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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.web.scripts.invite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.site.SiteInfo;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.repo.site.SiteService;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.util.PropertyMap;
import org.alfresco.util.URLEncoder;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.scripts.TestWebScriptServer.GetRequest;
import org.alfresco.web.scripts.TestWebScriptServer.PutRequest;
import org.alfresco.web.scripts.TestWebScriptServer.Response;
import org.apache.commons.lang.RandomStringUtils;
import org.json.JSONObject;

/**
 * Unit Test to test Invite Web Script API
 * 
 * @author Glen Johnson at Alfresco dot com
 */
public class InviteServiceTest extends BaseWebScriptTest
{
    // member variables for service instances
    private AuthorityService authorityService;
    private AuthenticationService authenticationService;
    private AuthenticationComponent authenticationComponent;
    private PersonService personService;
    private SiteService siteService;
    private NodeService nodeService;
    private WorkflowService workflowService;
    private MutableAuthenticationDao mutableAuthenticationDao;

    // stores invitee email addresses, one entry for each "start invite" operation
    // invoked, so that resources created for each invitee for each test
    // can be removed in the tearDown() method
    private List<String> inviteeEmailAddrs;

    private static final String WF_DEFINITION_INVITE = "jbpm$wf:invite";

    private static final String USER_INVITER = "InviterUser";
    private static final String USER_INVITER_2 = "InviterUser2";
    private static final String INVITEE_FIRSTNAME = "InviteeFirstName";
    private static final String INVITEE_LASTNAME = "InviteeLastName";
    private static final String INVITER_EMAIL = "FirstName123.LastName123@email.com";
    private static final String INVITER_EMAIL_2 = "FirstNameabc.LastNameabc@email.com";
    private static final String INVITEE_EMAIL_DOMAIN = "alfrescotesting.com";
    private static final String INVITEE_EMAIL_PREFIX = "invitee";
    private static final String INVITEE_SITE_ROLE = SiteModel.SITE_COLLABORATOR;
    private static final String SITE_SHORT_NAME_INVITE_1 = "SiteOneInviteTest";
    private static final String SITE_SHORT_NAME_INVITE_2 = "SiteTwoInviteTest";
    private static final String SITE_SHORT_NAME_INVITE_3 = "SiteThreeInviteTest";

    private static final String URL_INVITE = "/api/invite";
    private static final String URL_INVITES = "/api/invites";

    private static final String INVITE_ACTION_START = "start";
    private static final String INVITE_ACTION_CANCEL = "cancel";

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        // get references to services
        this.authorityService = (AuthorityService) getServer().getApplicationContext().getBean("AuthorityService");
        this.authenticationService = (AuthenticationService) getServer().getApplicationContext()
                .getBean("AuthenticationService");
        this.authenticationComponent = (AuthenticationComponent) getServer().getApplicationContext()
                .getBean("AuthenticationComponent");
        this.personService = (PersonService) getServer().getApplicationContext().getBean("PersonService");
        this.siteService = (SiteService) getServer().getApplicationContext().getBean("SiteService");
        this.nodeService = (NodeService) getServer().getApplicationContext().getBean("NodeService");
        this.workflowService = (WorkflowService) getServer().getApplicationContext().getBean("WorkflowService");
        this.mutableAuthenticationDao = (MutableAuthenticationDao) getServer().getApplicationContext()
                .getBean("authenticationDao");

        // Create new invitee email address list
        this.inviteeEmailAddrs = new ArrayList<String>();

        //
        // various setup operations which need to be run as system user
        //
        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                // Create inviter person
                createPerson(USER_INVITER, INVITER_EMAIL);
                
                // Create inviter2 person
                createPerson(USER_INVITER_2, INVITER_EMAIL_2);
                
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
        
        //
        // various setup operations which need to be run as inviter user
        //
        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                // Create first site for Inviter to invite Invitee to
                SiteInfo siteInfo = siteService.getSite(SITE_SHORT_NAME_INVITE_1);
                if (siteInfo == null)
                {
                    siteService.createSite("InviteSitePreset", SITE_SHORT_NAME_INVITE_1,
                            "InviteSiteTitle", "InviteSiteDescription", true);
                }
                
                // Create second site for inviter to invite invitee to
                siteInfo = siteService.getSite(SITE_SHORT_NAME_INVITE_2);
                if (siteInfo == null)
                {
                    siteService.createSite("InviteSitePreset", SITE_SHORT_NAME_INVITE_2,
                            "InviteSiteTitle", "InviteSiteDescription", true);
                }

                // Create third site for inviter to invite invitee to
                siteInfo = InviteServiceTest.this.siteService.getSite(SITE_SHORT_NAME_INVITE_3);
                if (siteInfo == null)
                {
                    siteService.createSite(
                        "InviteSitePreset", SITE_SHORT_NAME_INVITE_3,
                        "InviteSiteTitle", "InviteSiteDescription", true);
                }
                
                // set inviter2's role on third site to collaborator
                String inviterSiteRole = siteService.getMembersRole(SITE_SHORT_NAME_INVITE_3, USER_INVITER_2);
                if ((inviterSiteRole == null) || (inviterSiteRole.equals(SiteModel.SITE_COLLABORATOR) == false))
                {
                    siteService.setMembership(SITE_SHORT_NAME_INVITE_3, USER_INVITER_2, SiteModel.SITE_COLLABORATOR);
                }

                return null;
            }
        }, USER_INVITER);

        // Do tests as inviter user
        this.authenticationComponent.setCurrentUser(USER_INVITER);
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        //
        // run various teardown operations which need to be run as 'admin'
        //
        RunAsWork<Object> runAsWork = new RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                // delete the inviter
                deletePersonByUserName(USER_INVITER);

                // delete all invitee people
                for (String inviteeEmail : InviteServiceTest.this.inviteeEmailAddrs)
                {
                    //
                    // delete all people with given email address
                    //

                    Set<NodeRef> people = InviteServiceTest.this.personService
                            .getPeopleFilteredByProperty(
                                    ContentModel.PROP_EMAIL, inviteeEmail);
                    for (NodeRef person : people)
                    {
                        String userName = DefaultTypeConverter.INSTANCE
                                .convert(String.class, InviteServiceTest.this.nodeService.getProperty(
                                        person, ContentModel.PROP_USERNAME));

                        // delete person
                        deletePersonByUserName(userName);
                    }
                }

                // delete invite sites
                siteService.deleteSite(SITE_SHORT_NAME_INVITE_1);
                siteService.deleteSite(SITE_SHORT_NAME_INVITE_2);

                return null;
            }
        };
        AuthenticationUtil.runAs(runAsWork, AuthenticationUtil.getSystemUserName());

        // cancel all active invite workflows
        WorkflowDefinition wfDef = InviteServiceTest.this.workflowService
                .getDefinitionByName(WF_DEFINITION_INVITE);
        List<WorkflowInstance> workflowList = InviteServiceTest.this.workflowService
                .getActiveWorkflows(wfDef.id);
        for (WorkflowInstance workflow : workflowList)
        {
            InviteServiceTest.this.workflowService.cancelWorkflow(workflow.id);
        }
    }

    private void addUserToGroup(String groupName, String userName)
    {
        // get the full name for the group
        String fullGroupName = this.authorityService.getName(
                AuthorityType.GROUP, groupName);

        // create group if it does not exist
        if (this.authorityService.authorityExists(fullGroupName) == false)
        {
            this.authorityService.createAuthority(AuthorityType.GROUP, null,
                    fullGroupName);
        }

        // add the user to the group
        this.authorityService.addAuthority(fullGroupName, userName);
    }

    private void removeUserFromGroup(String groupName, String userName)
    {
        // get the full name for the group
        String fullGroupName = this.authorityService.getName(
                AuthorityType.GROUP, groupName);

        // remove user from the group
        this.authorityService.removeAuthority(fullGroupName, userName);

        // delete the group
        this.authorityService.deleteAuthority(fullGroupName);
    }

    private void createPerson(String userName, String emailAddress)
    {
        // if user with given user name doesn't already exist then create user
        if (this.authenticationService.authenticationExists(userName) == false)
        {
            // create user
            this.authenticationService.createAuthentication(userName,
                    "password".toCharArray());
        }

        // if person node with given user name doesn't already exist then create
        // person
        if (this.personService.personExists(userName) == false)
        {
            // create person properties
            PropertyMap personProps = new PropertyMap();
            personProps.put(ContentModel.PROP_USERNAME, userName);
            personProps.put(ContentModel.PROP_FIRSTNAME, "FirstName123");
            personProps.put(ContentModel.PROP_LASTNAME, "LastName123");
            personProps.put(ContentModel.PROP_EMAIL, emailAddress);
            personProps.put(ContentModel.PROP_JOBTITLE, "JobTitle123");
            personProps.put(ContentModel.PROP_JOBTITLE, "Organisation123");

            // create person node for user
            this.personService.createPerson(personProps);
        }
    }

    private void deletePersonByUserName(String userName)
    {
        // delete authentication if authentication exists for given user name
        if (this.authenticationService.authenticationExists(userName))
        {
            this.authenticationService.deleteAuthentication(userName);
        }
        
        // delete user account
        if (this.mutableAuthenticationDao.userExists(userName))
        {
            this.mutableAuthenticationDao.deleteUser(userName);
        }
        
        // delete person node associated with given user name
        // if one exists
        if (this.personService.personExists(userName))
        {
            this.personService.deletePerson(userName);
        }
    }

    private JSONObject startInvite(String inviteeFirstName, String inviteeLastName, String inviteeEmail, String inviteeSiteRole,
            String siteShortName, int expectedStatus)
            throws Exception
    {
        this.inviteeEmailAddrs.add(inviteeEmail);

        // Inviter sends invitation to Invitee to join a Site
        String startInviteUrl = URL_INVITE + "/" + INVITE_ACTION_START
                + "?inviteeFirstName=" + inviteeFirstName + "&inviteeLastName="
                + inviteeLastName + "&inviteeEmail="
                + URLEncoder.encode(inviteeEmail) + "&siteShortName="
                + siteShortName + "&inviteeSiteRole=" + inviteeSiteRole
                + "&serverPath=" + "http://localhost:8081/share/"
                + "&acceptUrl=" + "page/accept-invite"
                + "&rejectUrl=" + "page/reject-invite";

        Response response = sendRequest(new GetRequest(startInviteUrl), expectedStatus);

        JSONObject result = new JSONObject(response.getContentAsString());

        return result;
    }

    private JSONObject startInvite(String inviteeFirstName,
            String inviteeLastName, String inviteeSiteRole, String siteShortName, int expectedStatus)
            throws Exception
    {
        String inviteeEmail = INVITEE_EMAIL_PREFIX + RandomStringUtils.randomAlphanumeric(6)
                + "@" + INVITEE_EMAIL_DOMAIN;
        
        return startInvite(inviteeFirstName, inviteeLastName, inviteeEmail, inviteeSiteRole, siteShortName,
                expectedStatus);
    }

    private JSONObject getInvitesByInviteId(String inviteId, int expectedStatus)
            throws Exception
    {
        // construct get invites URL
        String getInvitesUrl = URL_INVITES + "?inviteId=" + inviteId;

        // invoke get invites web script
        Response response = sendRequest(new GetRequest(getInvitesUrl), expectedStatus);

        JSONObject result = new JSONObject(response.getContentAsString());

        return result;
    }

    private JSONObject getInvitesByInviterUserName(String inviterUserName,
            int expectedStatus) throws Exception
    {
        // construct get invites URL
        String getInvitesUrl = URL_INVITES + "?inviterUserName="
                + inviterUserName;

        // invoke get invites web script
        Response response = sendRequest(new GetRequest(getInvitesUrl), expectedStatus);

        JSONObject result = new JSONObject(response.getContentAsString());

        return result;
    }

    private JSONObject getInvitesByInviteeUserName(String inviteeUserName,
            int expectedStatus) throws Exception
    {
        // construct get invites URL
        String getInvitesUrl = URL_INVITES + "?inviteeUserName="
                + inviteeUserName;

        // invoke get invites web script
        Response response = sendRequest(new GetRequest(getInvitesUrl), expectedStatus);

        JSONObject result = new JSONObject(response.getContentAsString());

        return result;
    }

    private JSONObject getInvitesBySiteShortName(String siteShortName,
            int expectedStatus) throws Exception
    {
        // construct get invites URL
        String getInvitesUrl = URL_INVITES + "?siteShortName="
                + siteShortName;

        // invoke get invites web script
        Response response = sendRequest(new GetRequest(getInvitesUrl), expectedStatus);

        JSONObject result = new JSONObject(response.getContentAsString());

        return result;
    }

    public void testStartInvite() throws Exception
    {
        JSONObject result = startInvite(INVITEE_FIRSTNAME, INVITEE_LASTNAME, INVITEE_SITE_ROLE,
                SITE_SHORT_NAME_INVITE_1, Status.STATUS_OK);

        assertEquals(INVITE_ACTION_START, result.get("action"));
        assertEquals(INVITEE_FIRSTNAME, result.get("inviteeFirstName"));
        assertEquals(INVITEE_LASTNAME, result.get("inviteeLastName"));
        assertEquals(this.inviteeEmailAddrs
                .get(this.inviteeEmailAddrs.size() - 1), result
                .get("inviteeEmail"));
        assertEquals(SITE_SHORT_NAME_INVITE_1, result.get("siteShortName"));
    }
    
    public void testStartInviteWhenInviteeIsAlreadyMemberOfSite()
        throws Exception
    {
        //
        // add invitee as member of site: SITE_SHORT_NAME_INVITE
        //
        
        String randomStr = RandomStringUtils.randomNumeric(6);
        final String inviteeUserName = "inviteeUserName" + randomStr;
        final String inviteeEmailAddr = INVITEE_EMAIL_PREFIX + randomStr
            + "@" + INVITEE_EMAIL_DOMAIN;
        
        // create person with invitee user name and invitee email address 
        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                createPerson(inviteeUserName, inviteeEmailAddr);
                return null;
            }
    
        }, AuthenticationUtil.getSystemUserName());
        
        // add invitee person to site: SITE_SHORT_NAME_INVITE
        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                
                InviteServiceTest.this.siteService.setMembership(
                        SITE_SHORT_NAME_INVITE_1, inviteeUserName,
                        INVITEE_SITE_ROLE);
                return null;
            }
            
        }, USER_INVITER);
        
        JSONObject result = startInvite(INVITEE_FIRSTNAME, INVITEE_LASTNAME, inviteeEmailAddr, INVITEE_SITE_ROLE, 
                SITE_SHORT_NAME_INVITE_1, Status.STATUS_CONFLICT);
    }

//    public void testStartInviteWhenAlreadyInProgress()
//    throws Exception
//    {        
//        JSONObject result = startInvite(INVITEE_FIRSTNAME, INVITEE_LASTNAME, INVITEE_SITE_ROLE,
//                SITE_SHORT_NAME_INVITE_1, Status.STATUS_OK);
//        
//        String inviteeEmail = (String) result.get("inviteeEmail");
//        
//        startInvite(INVITEE_FIRSTNAME, INVITEE_LASTNAME, inviteeEmail, INVITEE_SITE_ROLE,
//                SITE_SHORT_NAME_INVITE_1,  Status.STATUS_CONFLICT);
//    }
//
    public void testStartInviteForSameInviteeButTwoDifferentSites()
        throws Exception
    {        
        JSONObject result = startInvite(INVITEE_FIRSTNAME, INVITEE_LASTNAME, INVITEE_SITE_ROLE,
                SITE_SHORT_NAME_INVITE_1, Status.STATUS_OK);
        
        String inviteeEmail = (String) result.get("inviteeEmail");
        
        startInvite(INVITEE_FIRSTNAME, INVITEE_LASTNAME, inviteeEmail, INVITEE_SITE_ROLE,
                SITE_SHORT_NAME_INVITE_2,  Status.STATUS_OK);
    }

    public void testCancelInvite() throws Exception
    {
        // inviter starts invite workflow
        JSONObject result = startInvite(INVITEE_FIRSTNAME, INVITEE_LASTNAME, INVITEE_SITE_ROLE,
                SITE_SHORT_NAME_INVITE_1, Status.STATUS_OK);

        // get hold of invite ID of started invite
        String inviteId = result.getString("inviteId");

        // Inviter cancels pending invitation
        String cancelInviteUrl = URL_INVITE + "/"
                + INVITE_ACTION_CANCEL + "?inviteId=" + inviteId;
        Response response = sendRequest(new GetRequest(cancelInviteUrl), Status.STATUS_OK);
    }

    public void testAcceptInvite() throws Exception
    {
        // inviter starts invite (sends out invitation)
        JSONObject result = startInvite(INVITEE_FIRSTNAME, INVITEE_LASTNAME, INVITEE_SITE_ROLE,
                SITE_SHORT_NAME_INVITE_1, Status.STATUS_OK);

        // get hold of invite ID and invite ticket of started invite
        String inviteId = result.getString("inviteId");
        String inviteTicket = result.getString("inviteTicket");

        // Invitee accepts invitation to a Site from Inviter
        String acceptInviteUrl = URL_INVITE + "/" + inviteId + "/" + inviteTicket + "/accept";
        Response response = sendRequest(new PutRequest(acceptInviteUrl, (byte[])null, null), Status.STATUS_OK);

        //
        // test that invitation represented by invite ID (of invitation started
        // above)
        // is no longer pending (as a result of the invitation having being
        // accepted)
        //

        // get pending invite matching inviteId from invite started above (run as inviter user)
        this.authenticationComponent.setCurrentUser(USER_INVITER);
        JSONObject getInvitesResult = getInvitesByInviteId(inviteId,
                Status.STATUS_OK);

        // there should no longer be any invites identified by invite ID pending
        assertEquals(0, getInvitesResult.getJSONArray("invites").length());
    }

    public void testRejectInvite() throws Exception
    {
        // inviter starts invite (sends out invitation)
        JSONObject result = startInvite(INVITEE_FIRSTNAME, INVITEE_LASTNAME, INVITEE_SITE_ROLE,
                SITE_SHORT_NAME_INVITE_1, Status.STATUS_OK);

        // get hold of invite ID of started invite
        String inviteId = result.getString("inviteId");
        String inviteTicket = result.getString("inviteTicket");

        // Invitee rejects invitation to a Site from Inviter
        String rejectInviteUrl = URL_INVITE + "/" + inviteId + "/" + inviteTicket + "/reject";
        Response response = sendRequest(new PutRequest(rejectInviteUrl, (byte[])null, null), Status.STATUS_OK);

        //
        // test that invite represented by invite ID (of invitation started
        // above)
        // is no longer pending (as a result of the invitation having being
        // rejected)
        //

        // get pending invite matching inviteId from invite started above (run as inviter user)
        this.authenticationComponent.setCurrentUser(USER_INVITER);
        JSONObject getInvitesResult = getInvitesByInviteId(inviteId, Status.STATUS_OK);

        // there should no longer be any invites identified by invite ID pending
        assertEquals(0, getInvitesResult.getJSONArray("invites").length());
    }

    public void testGetInvitesByInviteId() throws Exception
    {
        // inviter starts invite workflow
        JSONObject startInviteResult = startInvite(INVITEE_FIRSTNAME,
                INVITEE_LASTNAME, INVITEE_SITE_ROLE, SITE_SHORT_NAME_INVITE_1, Status.STATUS_OK);

        // get hold of workflow ID of started invite workflow instance

        String inviteId = startInviteResult.getString("inviteId");

        assertEquals(true, ((inviteId != null) && (inviteId.length() != 0)));

        // get pending invite matching inviteId from invite started above
        JSONObject getInvitesResult = getInvitesByInviteId(inviteId,
                Status.STATUS_OK);
        
        assertEquals(getInvitesResult.getJSONArray("invites").length(), 1);

        JSONObject inviteJSONObj = getInvitesResult.getJSONArray("invites").getJSONObject(0);

        assertEquals(inviteId, inviteJSONObj.get("inviteId"));
    }

    public void testGetInvitesByInviterUserName() throws Exception
    {
        // inviter starts invite workflow
        JSONObject startInviteResult = startInvite(INVITEE_FIRSTNAME,
                INVITEE_LASTNAME, INVITEE_SITE_ROLE, SITE_SHORT_NAME_INVITE_1, Status.STATUS_OK);

        // get pending invites matching inviter user name used in invite started
        // above
        JSONObject getInvitesResult = getInvitesByInviterUserName(USER_INVITER,
                Status.STATUS_OK);

        assertEquals(true, getInvitesResult.length() > 0);

        JSONObject inviteJSONObj = getInvitesResult.getJSONArray("invites").getJSONObject(0);

        assertEquals(USER_INVITER, inviteJSONObj.getJSONObject("inviter").get("userName"));
    }

    public void testGetInvitesByInviteeUserName() throws Exception
    {
        // inviter starts invite workflow
        JSONObject startInviteResult = startInvite(INVITEE_FIRSTNAME,
                INVITEE_LASTNAME, INVITEE_SITE_ROLE, SITE_SHORT_NAME_INVITE_1, Status.STATUS_OK);

        // get hold of invitee user name property of started invite workflow
        // instance
        String inviteeUserName = startInviteResult.getString("inviteeUserName");

        assertEquals(true, ((inviteeUserName != null) && (inviteeUserName
                .length() != 0)));

        // get pending invites matching invitee user name from invite started
        // above
        JSONObject getInvitesResult = getInvitesByInviteeUserName(
                inviteeUserName, Status.STATUS_OK);

        assertEquals(true, getInvitesResult.length() > 0);

        JSONObject inviteJSONObj = getInvitesResult.getJSONArray("invites").getJSONObject(0);

        assertEquals(inviteeUserName, inviteJSONObj.getJSONObject("invitee").get("userName"));
    }

    public void testGetInvitesBySiteShortName() throws Exception
    {
        // inviter starts invite workflow
        JSONObject startInviteResult = startInvite(INVITEE_FIRSTNAME,
                INVITEE_LASTNAME, INVITEE_SITE_ROLE, SITE_SHORT_NAME_INVITE_1, Status.STATUS_OK);

        // get hold of site short name property of started invite workflow
        // instance
        String siteShortName = startInviteResult.getString("siteShortName");

        assertEquals(true,
                ((siteShortName != null) && (siteShortName.length() != 0)));

        // get pending invites matching site short name from invite started
        // above
        JSONObject getInvitesResult = getInvitesBySiteShortName(siteShortName,
                Status.STATUS_OK);

        assertEquals(true, getInvitesResult.getJSONArray("invites").length() > 0);

        JSONObject inviteJSONObj = getInvitesResult.getJSONArray("invites").getJSONObject(0);

        assertEquals(siteShortName, inviteJSONObj.getJSONObject("site").get("shortName"));
    }
    
    public void testInviteForbiddenWhenInviterNotSiteManager() throws Exception
    {
        // inviter2 starts invite workflow, but he/she is not the site manager of the given site
        AuthenticationUtil.setCurrentUser(USER_INVITER_2);
        startInvite(INVITEE_FIRSTNAME,
                INVITEE_LASTNAME, INVITEE_SITE_ROLE, SITE_SHORT_NAME_INVITE_3, Status.STATUS_FORBIDDEN);
    }
}
