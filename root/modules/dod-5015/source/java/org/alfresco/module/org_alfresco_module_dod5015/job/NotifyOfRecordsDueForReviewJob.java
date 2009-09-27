/*
 * Copyright (C) 2009-2009 Alfresco Software Limited.
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.module.org_alfresco_module_dod5015.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.module.org_alfresco_module_dod5015.notification.RecordsManagementNotificationService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This job finds all Vital Records which are due for review, optionally
 * excluding those for which notification has already been issued.
 * 
 * @author Neil McErlean
 */
public class NotifyOfRecordsDueForReviewJob implements Job
{
    private static Log logger = LogFactory.getLog(NotifyOfRecordsDueForReviewJob.class);
    
    /**
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        final RecordsManagementService rmService = (RecordsManagementService)context.getJobDetail().getJobDataMap().get("recordsManagementService");
        final RecordsManagementNotificationService notificaitonService = (RecordsManagementNotificationService)context.getJobDetail().getJobDataMap().get("recordsManagementNotificationService");        
        final NodeService nodeService = (NodeService) context.getJobDetail().getJobDataMap().get("nodeService");
        final SearchService searchService = (SearchService) context.getJobDetail().getJobDataMap().get("searchService");
        final TransactionService trxService = (TransactionService) context.getJobDetail().getJobDataMap().get("transactionService");       
        final String subject = (String)context.getJobDetail().getJobDataMap().get("subject");
        final String role = (String)context.getJobDetail().getJobDataMap().get("role");
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Job " + this.getClass().getSimpleName() + " starting.");
        }

        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                // Query is for all records that are due for review and for which
                // notification has not been sent.
                StringBuilder queryBuffer = new StringBuilder();
                queryBuffer.append("+ASPECT:\"rma:vitalRecord\" ");                
                queryBuffer.append("+(@rma\\:reviewAsOf:[MIN TO NOW] ) ");
                queryBuffer.append("+( ");
                queryBuffer.append("@rma\\:notificationIssued:false "); 
                queryBuffer.append("OR ISNULL:\"rma:notificationIssued\" ");
                queryBuffer.append(") ");                
                String query = queryBuffer.toString();

                ResultSet results = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, query);
             
                final List<NodeRef> resultNodes = results.getNodeRefs();                
                if (logger.isDebugEnabled() == true)
                {
                    logger.debug("Found " + resultNodes.size() + " nodes due for review and without notification.");
                }                
                    
                //If we have something to do and a template to do it with
                if(resultNodes.size() != 0)
                {
                    RetryingTransactionHelper trn = trxService.getRetryingTransactionHelper();
                    
                    //Send the email message - but we must not retry since email is not transactional
                    RetryingTransactionCallback<Boolean> txCallbackSendEmail = new RetryingTransactionCallback<Boolean>()
                    {
                        // Set the notification issued property.
                        public Boolean execute() throws Throwable
                        {
                            // Find the root
                            NodeRef root = rmService.getRecordsManagementRoot(resultNodes.get(0));
                            
                            // Send the notification to the role specified
                            Map<String, Object> model = new HashMap<String, Object>(8, 1.0f);
                            model.put("records", resultNodes);   
                            model.put("subject", subject);
                            notificaitonService.sendNotificationToRole(
                                    RecordsManagementNotificationService.NE_DUE_FOR_REVIEW, 
                                    RecordsManagementNotificationService.NT_EMAIL, 
                                    root, 
                                    role, 
                                    model);   
                            
                            return null;
                        }
                    };
                    
                    RetryingTransactionCallback<Boolean> txUpdateNodesCallback = new RetryingTransactionCallback<Boolean>()
                    {
                        // Set the notification issued property.
                        public Boolean execute() throws Throwable
                        {
                            for (NodeRef node : resultNodes)
                            {
                                nodeService.setProperty(node, RecordsManagementModel.PROP_NOTIFICATION_ISSUED, "true");
                            }
                            return Boolean.TRUE;
                        }
                    };
      
                    /**
                     * Now do the work, one action in each transaction
                     */
                    trn.setMaxRetries(0);   // don't retry the send email
                    trn.doInTransaction(txCallbackSendEmail);
                    trn.setMaxRetries(10);
                    trn.doInTransaction(txUpdateNodesCallback);
                }
                return null;
            }
            
        }, AuthenticationUtil.getSystemUserName());

        if (logger.isDebugEnabled())
        {
            logger.debug("Job " + this.getClass().getSimpleName() + " finished");
        }     
    }  // end of execute method
        
}


