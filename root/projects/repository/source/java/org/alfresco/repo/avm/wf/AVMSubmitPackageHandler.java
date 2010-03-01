/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.avm.wf;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.workflow.jbpm.JBPMNode;
import org.alfresco.repo.workflow.jbpm.JBPMSpringActionHandler;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avm.locking.AVMLockingService;
import org.alfresco.service.cmr.avmsync.AVMDifference;
import org.alfresco.service.cmr.avmsync.AVMSyncService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.surf.util.Pair;
import org.alfresco.wcm.util.WCMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.BeanFactory;

public class AVMSubmitPackageHandler extends JBPMSpringActionHandler implements Serializable
{
    private static final long serialVersionUID = 4113360751217684995L;

    private static final Log logger = LogFactory.getLog(AVMSubmitPackageHandler.class);

    /** The AVMService instance. */
    private AVMService fAVMService;

    /** The AVMSyncService instance. */
    private AVMSyncService fAVMSyncService;

    /** The AVMLockingService instance. */
    private AVMLockingService fAVMLockingService;

    /**
     * The AVMSubmitTransactionListener instance (for JMX notification of virtualization server after commit/rollback).
     */
    private AVMSubmitTransactionListener fAVMSubmitTransactionListener;

    /**
     * Initialize service references.
     * 
     * @param factory
     *            The BeanFactory to get references from.
     */
    @Override
    protected void initialiseHandler(final BeanFactory factory)
    {
        fAVMService = (AVMService) factory.getBean(ServiceRegistry.AVM_SERVICE.getLocalName());
        fAVMSyncService = (AVMSyncService) factory.getBean(ServiceRegistry.AVM_SYNC_SERVICE.getLocalName());
        fAVMLockingService = (AVMLockingService) factory.getBean(ServiceRegistry.AVM_LOCKING_SERVICE.getLocalName());
        fAVMSubmitTransactionListener = (AVMSubmitTransactionListener) factory.getBean("AVMSubmitTransactionListener");

        AlfrescoTransactionSupport.bindListener(fAVMSubmitTransactionListener);
    }

    /**
     * Do the actual work.
     * 
     * @param executionContext
     *            The context to get stuff from.
     */
    public void execute(final ExecutionContext executionContext) throws Exception
    {
        // TODO: Allow submit parameters to be passed into this action handler
        // rather than pulling directly from execution context
        final NodeRef pkg = ((JBPMNode) executionContext.getContextInstance().getVariable("bpm_package")).getNodeRef();
        final Pair<Integer, String> pkgPath = AVMNodeConverter.ToAVMVersionPath(pkg);
        final AVMNodeDescriptor pkgDesc = fAVMService.lookup(pkgPath.getFirst(), pkgPath.getSecond());
        
        if (pkgDesc == null)
        {
            logger.warn("Submit skipped since workflow package does not exist: "+pkgPath);
        }
        else
        {
            final String from = (String) executionContext.getContextInstance().getVariable("wcmwf_fromPath");
            final String targetPath = pkgDesc.getIndirection();
            
            if (logger.isDebugEnabled())
            {
                logger.debug("handling submit of " + pkgPath.getSecond() + " from " + from + " to " + targetPath);
            }
            
            // submit the package changes
            final String description = (String) executionContext.getContextInstance().getVariable("bpm_workflowDescription");
            final String tag = (String) executionContext.getContextInstance().getVariable("wcmwf_label");
    
            final Map<QName, PropertyValue> dnsProperties = this.fAVMService.queryStorePropertyKey(targetPath.split(":")[0], QName.createQName(null, ".dns%"));
            String localName = dnsProperties.keySet().iterator().next().getLocalName();
            final String webProject = localName.substring(localName.lastIndexOf('.') + 1, localName.length());
            final List<AVMDifference> stagingDiffs = fAVMSyncService.compare(pkgPath.getFirst(), pkgPath.getSecond(), -1, targetPath, null);
    
            // Allow AVMSubmitTransactionListener to inspect the staging diffs
            // so it can notify the virtualization server via JMX if when this
            // submit succeeds or fails. This allows virtual webapps devoted
            // to the workarea to be destroyed, and staging to be updated in
            // the event that some of the files alter the behavior of the
            // webapp itself (e.g.: WEB-INF/web.xml, WEB-INF/lib/*.jar), etc.
    
            AlfrescoTransactionSupport.bindResource("staging_diffs", stagingDiffs);
    
            // Workflow does this as system as the staging area has restricted access and reviewers
            // may not have permission to flatten the store the workflow was submitted from
            AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
            {
                public Object doWork() throws Exception
                {
                    fAVMSyncService.update(stagingDiffs, null, false, false, true, true, tag, description);
                    fAVMSyncService.flatten(pkgPath.getSecond(), targetPath);
                    
                    for (final AVMDifference diff : stagingDiffs)
                    {
                        recursivelyRemoveLocks(webProject, -1, diff.getSourcePath());
                    }
                    
                    // flatten source folder where changes were submitted from
                    if (from != null && from.length() > 0)
                    {
                        // first, submit changes back to sandbox forcing addition of edits in workflow (and submission
                        // flag removal). second, flatten sandbox, removing modified items that have been submitted
                        // TODO: Without locking on the sandbox, it's possible that a change to a "submitted" item
                        // may get lost when the item is finally approved
                        final List<AVMDifference> sandboxDiffs = fAVMSyncService.compare(pkgPath.getFirst(), pkgPath.getSecond(), -1, from, null);
                        fAVMSyncService.update(sandboxDiffs, null, true, true, false, false, tag, description);
                        fAVMSyncService.flatten(from, targetPath);
                    }
                    
                    return null;
                }
            }, AuthenticationUtil.getSystemUserName());
        }
    }

    /**
     * Recursively remove locks from a path. Walking child folders looking for files to remove locks from.
     */
    private void recursivelyRemoveLocks(String wpStoreId, int version, String wfPath)
    {
        AVMNodeDescriptor desc = fAVMService.lookup(version, wfPath, true);
        if (desc.isFile() || desc.isDeletedFile())
        {
            String relativePath = WCMUtil.getStoreRelativePath(wfPath);
            
            if (logger.isDebugEnabled())
            {
                logger.debug("removing file lock on " + relativePath + " (store id: "+wpStoreId+")");
            }
            
            fAVMLockingService.removeLock(wpStoreId, relativePath);
        }
        else
        {
            if (desc.isDeletedDirectory() == false)
            {
                Map<String, AVMNodeDescriptor> list = fAVMService.getDirectoryListing(desc, true);
                for (AVMNodeDescriptor child : list.values())
                {
                    recursivelyRemoveLocks(wpStoreId, version, child.getPath());
                }
            }
        }
    }
}
