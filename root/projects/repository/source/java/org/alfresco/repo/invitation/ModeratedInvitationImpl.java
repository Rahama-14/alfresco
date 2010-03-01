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
package org.alfresco.repo.invitation;

import org.alfresco.service.cmr.invitation.ModeratedInvitation;
import org.alfresco.service.namespace.QName;

import java.io.Serializable;
import java.util.Map;

/**
 * InvitationRequestImpl is a basic InvitationRequest that is processed by the 
 * InvitationService 
 */
/*package scope */ class ModeratedInvitationImpl extends InvitationImpl implements ModeratedInvitation, Serializable 
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -5557544865169876451L;
	
	private String roleName;
	private String inviteeComments;
	private String inviteeUserName;
	
	public ModeratedInvitationImpl()
	{
		super();
	}
	
	public ModeratedInvitationImpl(Map<QName, Serializable> workflowProps)
	{
		 super();
		 
		 setInviteeUserName((String)workflowProps.get(WorkflowModelModeratedInvitation.WF_PROP_INVITEE_USER_NAME));
		 setResourceName((String)workflowProps.get(WorkflowModelModeratedInvitation.WF_PROP_RESOURCE_NAME));
	     if(workflowProps.containsKey(WorkflowModelModeratedInvitation.WF_PROP_RESOURCE_TYPE))
	     {
	     	 setResourceType(ResourceType.valueOf((String)workflowProps.get(WorkflowModelModeratedInvitation.WF_PROP_RESOURCE_TYPE)));
	     }
	     roleName = (String)workflowProps.get(WorkflowModelModeratedInvitation.WF_PROP_INVITEE_ROLE);
	     inviteeComments = (String)workflowProps.get(WorkflowModelModeratedInvitation.WF_PROP_INVITEE_COMMENTS);
	}
	

	public void setRoleName(String roleName) 
	{
		this.roleName = roleName;
	}

	public String getRoleName() 
	{
		return roleName;
	}

	public String getInviteeComments() 
	{
		return inviteeComments;
	}
	
	public void setInviteeComments(String inviteeComments)
	{
		this.inviteeComments = inviteeComments;
	}

	public InvitationType getInvitationType() {
		return InvitationType.MODERATED;
	}
	
	public void setInviteeUserName(String inviteeUserName) {
		this.inviteeUserName = inviteeUserName;
	}

	public String getInviteeUserName() {
		return inviteeUserName;
	}

}
