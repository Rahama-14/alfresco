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
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>. */

package org.alfresco.repo.avm.wf;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.config.JNDIConstants;
import org.alfresco.linkvalidation.HrefValidationProgress;
import org.alfresco.linkvalidation.LinkValidationAction;
import org.alfresco.linkvalidation.LinkValidationReport;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.workflow.jbpm.JBPMNode;
import org.alfresco.repo.workflow.jbpm.JBPMSpringActionHandler;
import org.alfresco.wcm.sandbox.SandboxConstants;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.BeanFactory;

/**
 * Performs a link validaton check for the workflow sandbox being used
 * for a submisson process.
 * 
 * @author gavinc
 */
public class AVMSubmitLinkChecker extends JBPMSpringActionHandler
{
    private static final long serialVersionUID = 1442635948148675461L;

    private static Log    logger = LogFactory.getLog(AVMSubmitLinkChecker.class);
    
    /**
     * The AVMService.
     */
    private AVMService fAVMService;
    
    /**
     * The ActionService.
     */
    private ActionService fActionService;
    
    /**
     * Set any bean references necessary.
     * @param factory The BeanFactory from which to get beans.
     */
    @Override
    protected void initialiseHandler(BeanFactory factory)
    {
        this.fActionService = (ActionService)factory.getBean("ActionService");
        this.fAVMService = (AVMService)factory.getBean("AVMService");
    }

    /**
     * Do the actual link validation check.
     * 
     * @param executionContext The jBPM context.
     */
    public void execute(ExecutionContext executionContext) throws Exception
    {
        // retrieve the workflow sandbox (the workflow package)
        NodeRef pkg = ((JBPMNode)executionContext.getContextInstance().getVariable("bpm_package")).getNodeRef();
        
        // get the store name
        String storeName = pkg.getStoreRef().getIdentifier();

        // retrieve the webapp name from the workflow execution context
        String webappName = (String)executionContext.getContextInstance().getVariable("wcmwf_webapp");
        String webappPath = storeName + ":/" + JNDIConstants.DIR_DEFAULT_WWW + "/" +
                            JNDIConstants.DIR_DEFAULT_APPBASE + "/" + webappName;
        NodeRef webappPathRef = AVMNodeConverter.ToNodeRef(-1, webappPath);
        
        if (logger.isDebugEnabled())
            logger.debug("Checking links in workflow webapp: " + webappPath);

        // create and execute the action
        int brokenLinks = -1;
        
        try
        {
           HrefValidationProgress monitor = new HrefValidationProgress();
           Map<String, Serializable> args = new HashMap<String, Serializable>(1, 1.0f);
           args.put(LinkValidationAction.PARAM_MONITOR, monitor);
           args.put(LinkValidationAction.PARAM_COMPARE_TO_STAGING, Boolean.TRUE);
           Action action = this.fActionService.createAction(LinkValidationAction.NAME, args);
           this.fActionService.executeAction(action, webappPathRef, false, false);
           
           // retrieve the deployment report from the store property
           PropertyValue val = this.fAVMService.getStoreProperty(storeName, 
                  SandboxConstants.PROP_LINK_VALIDATION_REPORT);
           if (val != null)
           {
               LinkValidationReport report = (LinkValidationReport)val.getSerializableValue();
               if (report != null && report.wasSuccessful())
               {
                  brokenLinks = report.getNumberBrokenLinks();
               }
           }
           
           if (logger.isDebugEnabled())
               logger.debug("Link validation check found " + brokenLinks + " broken links");
        }
        catch (Throwable err)
        {
           logger.error(err);
        }
        
        // set the number of broken links in a variable, -1 indicates an error occured
        executionContext.setVariable("wcmwf_brokenLinks", brokenLinks);
    }
}
