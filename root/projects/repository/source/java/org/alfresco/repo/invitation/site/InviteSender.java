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

package org.alfresco.repo.invitation.site;

import static org.alfresco.repo.invitation.WorkflowModelNominatedInvitation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.repo.i18n.MessageService;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.search.SearcherException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.invitation.InvitationException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.springframework.extensions.surf.util.ParameterCheck;
import org.springframework.extensions.surf.util.URLEncoder;

/**
 * This class is responsible for sending email invitations, allowing nominated
 * user's to join a Site.
 * 
 * @author Nick Smith
 */
public class InviteSender
{
    public static final String WF_INSTANCE_ID = "wf_instanceId";
    public static final String WF_PACKAGE = "wf_package";

    private static final List<String> expectedProperties = Arrays.asList(wfVarInviteeUserName,//
                wfVarResourceName,//
                wfVarInviterUserName,//
                wfVarInviteeUserName,//
                wfVarRole,//
                wfVarInviteeGenPassword,//
                wfVarResourceName,//
                wfVarInviteTicket,//
                wfVarServerPath,//
                wfVarAcceptUrl,//
                wfVarRejectUrl, WF_INSTANCE_ID,//
                WF_PACKAGE);

    private final ActionService actionService;
    private final NodeService nodeService;
    private final PersonService personService;
    private final SearchService searchService;
    private final SiteService siteService;
    private final TemplateService templateService;
    private final Repository repository;
    private final MessageService messageService;
    
    public InviteSender(ServiceRegistry services, Repository repository, MessageService messageService)
    {
        this.actionService = services.getActionService();
        this.nodeService = services.getNodeService();
        this.personService = services.getPersonService();
        this.searchService = services.getSearchService();
        this.siteService = services.getSiteService();
        this.templateService = services.getTemplateService();
        this.repository = repository;
        this.messageService = messageService;
    }

    /**
     * Sends an invitation email.
     * 
     * @param properties A Map containing the properties needed to send the
     *            email.
     */
    public void sendMail(Map<String, String> properties)
    {
        checkProperties(properties);
        ParameterCheck.mandatory("Properties", properties);
        NodeRef inviter = personService.getPerson(properties.get(wfVarInviterUserName));
        String inviteeName = properties.get(wfVarInviteeUserName);
        NodeRef invitee = personService.getPerson(inviteeName);
        Action mail = actionService.createAction(MailActionExecuter.NAME);
        mail.setParameterValue(MailActionExecuter.PARAM_FROM, getEmail(inviter));
        mail.setParameterValue(MailActionExecuter.PARAM_TO, getEmail(invitee));
        mail.setParameterValue(MailActionExecuter.PARAM_SUBJECT, buildSubject(properties));
        String mailText = buildMailText(properties, inviter, invitee);
        mail.setParameterValue(MailActionExecuter.PARAM_TEXT, mailText);
        actionService.executeAction(mail, getWorkflowPackage(properties));
    }

    /**
     * @param properties
     */
    private void checkProperties(Map<String, String> properties)
    {
        Set<String> keys = properties.keySet();
        if (!keys.containsAll(expectedProperties))
        {
            LinkedList<String> missingProperties = new LinkedList<String>(expectedProperties);
            missingProperties.removeAll(keys);
            throw new InvitationException("The following mandatory properties are missing:\n" + missingProperties);
        }
    }

    private String buildSubject(Map<String, String> properties)
    {
    	return messageService.getMessage("invitation.invitesender.email.subject", getSiteName(properties));
    }

    private String buildMailText(Map<String, String> properties, NodeRef inviter, NodeRef invitee)
    {
        String template = getEmailTemplate();
        Map<String, Object> model = makeDefaultModel();
        Map<String, String> args = buildArgs(properties, inviter, invitee);
        model.put("args", args);
        return templateService.processTemplate(template, model);
    }

    private String getEmailTemplate()
    {
        NodeRef template = getEmailTemplateNodeRef();
        return template.toString();
    }

    private Map<String, String> buildArgs(Map<String, String> properties, NodeRef inviter, NodeRef invitee)
    {
        String params = buildUrlParamString(properties);
        String serverPath = properties.get(wfVarServerPath);
        String acceptLink = serverPath + properties.get(wfVarAcceptUrl) + params;
        String rejectLink = serverPath + properties.get(wfVarRejectUrl) + params;

        Map<String, String> args = new HashMap<String, String>();
        args.put("inviteePersonRef", invitee.toString());
        args.put("inviterPersonRef", inviter.toString());
        args.put("siteName", getSiteName(properties));
        args.put("inviteeSiteRole", getRoleName(properties));
        args.put("inviteeUserName", properties.get(wfVarInviteeUserName));
        args.put("inviteeGenPassword", properties.get(wfVarInviteeGenPassword));
        args.put("acceptLink", acceptLink);
        args.put("rejectLink", rejectLink);
        return args;
    }

    private String getRoleName(Map<String, String> properties) {
    	String roleName = properties.get(wfVarRole);
    	String role = messageService.getMessage("invitation.invitesender.email.role."+roleName);
    	if(role == null)
    	{
			role = roleName;
		}
    	return role;
	}

	private Map<String, Object> makeDefaultModel()
    {
        NodeRef person = repository.getPerson();
        NodeRef companyHome = repository.getCompanyHome();
        NodeRef userHome = repository.getUserHome(person);
        Map<String, Object> model = templateService.buildDefaultModel(person, companyHome, userHome, null, null);
        return model;
    }

    private String getEmail(NodeRef person)
    {
        return (String) nodeService.getProperty(person, ContentModel.PROP_EMAIL);
    }

    private NodeRef getWorkflowPackage(Map<String, String> properties)
    {
        String packageRef = properties.get(WF_PACKAGE);
        return new NodeRef(packageRef);
    }

    private NodeRef getEmailTemplateNodeRef()
    {
        StoreRef spacesStore = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
        String query = " PATH:\"app:company_home/app:dictionary/app:email_templates/cm:invite/cm:invite-email.ftl\"";

        SearchParameters searchParams = new SearchParameters();
        searchParams.addStore(spacesStore);
        searchParams.setLanguage(SearchService.LANGUAGE_LUCENE);
        searchParams.setQuery(query);

        ResultSet results = null;
        try
        {
            results = searchService.query(searchParams);
            List<NodeRef> nodeRefs = results.getNodeRefs();
            if (nodeRefs.size() == 1)
                return nodeRefs.get(0);
            else
                throw new InvitationException("Cannot find the email templatte!");
        }
        catch (SearcherException e)
        {
            throw new InvitationException("Cannot find the email templatte!", e);
        }
        finally
        {
            if (results != null)
                results.close();
        }
    }

    private String buildUrlParamString(Map<String, String> properties)
    {
        StringBuilder params = new StringBuilder("?inviteId=");
        params.append(properties.get(WF_INSTANCE_ID));
        params.append("&inviteeUserName=");
        params.append(URLEncoder.encode(properties.get(wfVarInviteeUserName)));
        params.append("&siteShortName=");
        params.append(properties.get(wfVarResourceName));
        params.append("&inviteTicket=");
        params.append(properties.get(wfVarInviteTicket));
        return params.toString();
    }

    private String getSiteName(Map<String, String> properties)
    {
        String siteFullName = properties.get(wfVarResourceName);
        SiteInfo site = siteService.getSite(siteFullName);
        if (site == null)
            throw new InvitationException("The site " + siteFullName + " could not be found.");

        String siteName = site.getShortName();
        String siteTitle = site.getTitle();
        if (siteTitle != null && siteTitle.length() > 0)
        {
            siteName = siteTitle;
        }
        return siteName;
    }
}
