/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.avm.actions;

import java.util.List;
import java.util.Map;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avm.locking.AVMLockingService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Undos a list of changed nodes in a user sandbox. The set of nodes to undo is
 * passed in as a packed string (Obtained by VersionPathStuffer).
 * The actionedUponNodeRef is a dummy and can be null.
 * @author britt
 * 
 * @deprecated see org.alfresco.wcm.actions.WCMSandboxRevertListAction or org.alfresco.wcm.SandboxService.revert
 */
public class AVMUndoSandboxListAction extends ActionExecuterAbstractBase 
{
    private static Log    fgLogger = LogFactory.getLog(AVMUndoSandboxListAction.class);
    
    public static final String NAME = "avm-undo-list";
    // The encoded list of nodes.
    public static final String PARAM_NODE_LIST = "node-list";
    
    /**
     * The AVM Service reference.
     */
    private AVMService fAVMService;
   
    /**
     * The AVM Locking Service reference.
     */
    private AVMLockingService fAVMLockingService;
    
    public void setAvmService(AVMService service)
    {
        fAVMService = service;
    }

    public void setAvmLockingService(AVMLockingService service)
    {
        fAVMLockingService = service;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef) 
    {
        List<Pair<Integer, String>> versionPaths = 
            (List<Pair<Integer, String>>)action.getParameterValue(PARAM_NODE_LIST);
        for (Pair<Integer, String> item : versionPaths)
        {
            AVMNodeDescriptor desc = fAVMService.lookup(-1, item.getSecond(), true);
            if (desc == null)
            {
                continue;
            }
            String [] parentChild = AVMNodeConverter.SplitBase(item.getSecond());
            if (parentChild.length != 2)
            {
                continue;
            }
            AVMNodeDescriptor parent = fAVMService.lookup(-1, parentChild[0], true);
            if (parent.isLayeredDirectory())
            {
                if (fgLogger.isDebugEnabled())
                   fgLogger.debug("reverting " + parentChild[1] + " in " + parentChild[0]);
                fAVMService.makeTransparent(parentChild[0], parentChild[1]);
            }

            if (desc.isFile() || desc.isDeletedFile())
            {
                final Map<QName, PropertyValue> dnsProperties = fAVMService.queryStorePropertyKey(item.getSecond().split(":")[0], QName.createQName(null, ".dns%"));
                if (dnsProperties.size() == 1)
                {
                    String webProject = dnsProperties.keySet().iterator().next().getLocalName();
                    webProject = webProject.substring(webProject.lastIndexOf('.') + 1, webProject.length());
                    String path = item.getSecond().substring(item.getSecond().indexOf(":") + 1);
                    if (fgLogger.isDebugEnabled())
                        fgLogger.debug("unlocking file " + path + " in web project " + webProject);
    
                    if (fAVMLockingService.getLock(webProject, path) != null)
                    {
                        fAVMLockingService.removeLock(webProject, path);
                    }
                    else
                    {
                        fgLogger.warn("expected file " + path + " in " + webProject + " to be locked");
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) 
    {
        paramList.add(
                new ParameterDefinitionImpl(PARAM_NODE_LIST,
                                            DataTypeDefinition.ANY,
                                            true,
                                            getParamDisplayLabel(PARAM_NODE_LIST)));
    }
}
