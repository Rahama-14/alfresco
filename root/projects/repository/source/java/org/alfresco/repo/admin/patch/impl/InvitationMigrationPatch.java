/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.repo.admin.patch.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.invitation.InvitationServiceImpl;
import org.alfresco.repo.invitation.WorkflowModelNominatedInvitation;
import org.alfresco.repo.invitation.site.AcceptInviteAction;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.invitation.Invitation;
import org.alfresco.service.cmr.invitation.InvitationService;
import org.alfresco.service.cmr.invitation.Invitation.ResourceType;
import org.alfresco.service.cmr.workflow.WorkflowException;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.cmr.workflow.WorkflowTaskQuery;
import org.alfresco.service.cmr.workflow.WorkflowTaskState;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Invitation service changes from Labs3D 3.1.0E
 * 
 * wf:invite becomes wf:invitation-nominated
 * 
 * Create new invitations
 * Cancel wf:invite workflows.
 * 
 * @author mrogers
 */
public class InvitationMigrationPatch extends AbstractPatch
{
	private WorkflowService workflowService;
	private InvitationService invitationService;
    private static final String MSG_SUCCESS = "patch.invitationMigration.result";
    private static final String MSG_NO_INVITES = "patch.invitationMigration.no_invites";
	
	private static final Log logger = LogFactory.getLog(InvitationMigrationPatch.class);
	
	/**
	 * Old invite model from V3.0 E / Labs 3 D
	 */
	private static class OldInviteModel
	{
		// process name
		public static final QName WF_PROCESS_INVITE = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "invite");

		// workflow definition name
		public static final String WORKFLOW_DEFINITION_NAME = "jbpm$wf:invite";
    
		// tasks
		public static final QName WF_INVITE_TASK_INVITE_TO_SITE = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "inviteToSiteTask");
		public static final QName WF_INVITE_TASK_INVITE_PENDING = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "invitePendingTask");
		public static final QName WF_TASK_ACCEPT_INVITE = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "acceptInviteTask");
		public static final QName WF_TASK_REJECT_INVITE = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "rejectInviteTask");
    
		// transition names
		public static final String WF_TRANSITION_SEND_INVITE = "sendInvite";
		public static final String WF_TRANSITION_ACCEPT = "accept";
		public static final String WF_TRANSITION_REJECT = "reject";
		public static final String WF_TRANSITION_CANCEL = "cancel";
		public static final String WF_TRANSITION_ACCEPT_INVITE_END = "end";
		public static final String WF_TRANSITION_REJECT_INVITE_END = "end";
    
		// workflow properties
		public static final QName WF_PROP_SERVER_PATH = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "serverPath");
		public static final QName WF_PROP_ACCEPT_URL = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "acceptUrl");
		public static final QName WF_PROP_REJECT_URL = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "rejectUrl");
		public static final QName WF_PROP_INVITE_TICKET = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "inviteTicket");
		public static final QName WF_PROP_INVITER_USER_NAME = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "inviterUserName");
		public static final QName WF_PROP_INVITEE_USER_NAME = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "inviteeUserName");
		public static final QName WF_PROP_INVITEE_FIRSTNAME = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "inviteeFirstName");
		public static final QName WF_PROP_INVITEE_LASTNAME = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "inviteeLastName");
		public static final QName WF_PROP_SITE_SHORT_NAME = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "siteShortName");
		public static final QName WF_PROP_INVITEE_SITE_ROLE = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "inviteeSiteRole");
		public static final QName WF_PROP_SENT_INVITE_DATE = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "sentInviteDate");
		public static final QName WF_PROP_INVITEE_GEN_PASSWORD = QName.createQName(NamespaceService.WORKFLOW_MODEL_1_0_URI, "inviteeGenPassword");

	}

	@Override
	protected String applyInternal() throws Exception 
	{
		List<WorkflowInstance> currentInstances = null;
		try 
		{
			// Get the process properties.
			currentInstances = workflowService.getActiveWorkflows(OldInviteModel.WF_PROCESS_INVITE.toPrefixString());
		}
		catch (WorkflowException we)
		{
				logger.debug("no invites to cancel" , we);	
		        String msg = I18NUtil.getMessage(MSG_NO_INVITES);
		        return msg;
		}		
		int count = 0;
		
		for(WorkflowInstance oldInstance : currentInstances)
		{
			String oldWorkflowId = oldInstance.id;
			
			try 
			{
				convertOldInvite(oldWorkflowId);
			} 
			catch (Exception e)
			{
				// swallow error - a lost invitation is not a show stopper
				logger.error("unable to re-invite oldInstance:" + oldWorkflowId, e);	
			}
			
			// Cancel the old workflow instance
			workflowService.cancelWorkflow(oldWorkflowId);
			
			count++;
		}
			
	    // build the result message
	    String msg = I18NUtil.getMessage(MSG_SUCCESS, count);
	    return msg;
	}

	public void setWorkflowService(WorkflowService workflowService) 
	{
		this.workflowService = workflowService;
	}

	public WorkflowService getWorkflowService() 
	{
		return workflowService;
	}

	public void setInvitationService(InvitationService invitationService) {
		this.invitationService = invitationService;
	}

	public InvitationService getInvitationService() {
		return invitationService;
	}
	
	/**
	 * Converts old invite to new service
	 * @param oldWorkflowId
	 */
	private void convertOldInvite(String oldWorkflowId)
	{
	
		WorkflowTaskQuery query = new WorkflowTaskQuery();
		query.setProcessId(oldWorkflowId);
		query.setProcessName(OldInviteModel.WF_PROCESS_INVITE);
		query.setTaskName(OldInviteModel.WF_INVITE_TASK_INVITE_TO_SITE);

		// query for invite workflow task associate
		List<WorkflowTask> inviteStartTasks = workflowService.queryTasks(query);

		// should also be 0 or 1
		if (inviteStartTasks.size() < 1)
		{
			// task not found - can't do anything
		}
		else
		{
			WorkflowTask oldTask = inviteStartTasks.get(0);
        
			Map<QName, Serializable> workflowProps = oldTask.properties;
		 	final String inviteeUserName = (String)workflowProps.get(OldInviteModel.WF_PROP_INVITEE_USER_NAME);
		 	final String inviterUserName = (String)workflowProps.get(OldInviteModel.WF_PROP_INVITER_USER_NAME);
		 	final String resourceName = (String)workflowProps.get(OldInviteModel.WF_PROP_SITE_SHORT_NAME);
		 	final String roleName =  (String)workflowProps.get(OldInviteModel.WF_PROP_INVITEE_SITE_ROLE);
		 	final String serverPath =   (String)workflowProps.get(OldInviteModel.WF_PROP_SERVER_PATH);
		 	final String acceptUrl =  (String)workflowProps.get(OldInviteModel.WF_PROP_ACCEPT_URL);
		 	final String rejectUrl =   (String)workflowProps.get(OldInviteModel.WF_PROP_REJECT_URL);
            
	        // add Invitee to Site with the site role that the inviter "started" the invite process with
	        AuthenticationUtil.runAs(new RunAsWork<Object>()
	        {
	            public Object doWork() throws Exception
	            {
	    		 	// Create a new invitation.
	    		 	invitationService.inviteNominated(inviteeUserName, Invitation.ResourceType.WEB_SITE, resourceName, roleName, serverPath, acceptUrl, rejectUrl);
	                return null;
	            }
	            
	        }, inviterUserName);   
		}
	}
}
