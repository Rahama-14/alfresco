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
package org.alfresco.repo.site;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.cmr.tagging.TaggingService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseAlfrescoSpringTest;
import org.alfresco.util.PropertyMap;

/**
 * Thumbnail service implementation unit test
 * 
 * @author Roy Wetherall
 */
public class SiteServiceImplTest extends BaseAlfrescoSpringTest 
{
    private static final String TEST_SITE_PRESET = "testSitePreset";
    private static final String TEST_SITE_PRESET_2 = "testSitePreset2";
    private static final String TEST_TITLE = "This is my title";
    private static final String TEST_DESCRIPTION = "This is my description";
    
    private static final String USER_ONE = "UserOne_SiteServiceImplTest";
    private static final String USER_TWO = "UserTwo_SiteServiceImplTest";
    private static final String USER_THREE = "UserThree_SiteServiceImplTest";
    private static final String GROUP_ONE = "GrpOne_SiteServiceImplTest";
    private static final String GROUP_TWO = "GrpTwo_SiteServiceImplTest";
    
    private SiteService siteService;    
    private ScriptService scriptService;
    private NodeService nodeService;
    private AuthenticationComponent authenticationComponent;
    private TaggingService taggingService;
    private PersonService personService;
    private AuthorityService authorityService;
    private FileFolderService fileFolderService;

    private String groupOne;
    private String groupTwo;
    
    /**
     * Called during the transaction setup
     */
    protected void onSetUpInTransaction() throws Exception
    {
        super.onSetUpInTransaction();
        
        // Get the required services
        this.siteService = (SiteService)this.applicationContext.getBean("SiteService");
        this.scriptService = (ScriptService)this.applicationContext.getBean("ScriptService");
        this.nodeService = (NodeService)this.applicationContext.getBean("NodeService");
        this.authenticationComponent = (AuthenticationComponent)this.applicationContext.getBean("authenticationComponent");
        this.taggingService = (TaggingService)this.applicationContext.getBean("TaggingService");
        this.personService = (PersonService)this.applicationContext.getBean("PersonService");
        this.authorityService = (AuthorityService)this.applicationContext.getBean("AuthorityService");
        this.fileFolderService = (FileFolderService)this.applicationContext.getBean("FileFolderService");
        
        // Create the test users
        createUser(USER_ONE);
        createUser(USER_TWO);
        createUser(USER_THREE);
     
        // Create the test groups
        this.groupOne = this.authorityService.createAuthority(AuthorityType.GROUP, null, GROUP_ONE);
        this.authorityService.addAuthority(this.groupOne, USER_TWO);
        this.groupTwo = this.authorityService.createAuthority(AuthorityType.GROUP, null, GROUP_TWO);
        this.authorityService.addAuthority(this.groupTwo, USER_TWO);
        this.authorityService.addAuthority(this.groupTwo, USER_THREE);

        // Set the current authentication
        this.authenticationComponent.setCurrentUser(USER_ONE);
    }
    
    private void createUser(String userName)
    {
        if (this.authenticationService.authenticationExists(userName) == false)
        {
            this.authenticationService.createAuthentication(userName, "PWD".toCharArray());
            
            PropertyMap ppOne = new PropertyMap(4);
            ppOne.put(ContentModel.PROP_USERNAME, userName);
            ppOne.put(ContentModel.PROP_FIRSTNAME, "firstName");
            ppOne.put(ContentModel.PROP_LASTNAME, "lastName");
            ppOne.put(ContentModel.PROP_EMAIL, "email@email.com");
            ppOne.put(ContentModel.PROP_JOBTITLE, "jobTitle");
            
            this.personService.createPerson(ppOne);
        }        
    }
	
    public void testCreateSite() throws Exception
    {
        // Create a public site
        SiteInfo siteInfo = this.siteService.createSite(TEST_SITE_PRESET, "mySiteTest", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "mySiteTest", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);     
        
        String name = "!£$%^&*()_+=-[]{}";
        siteInfo = this.siteService.createSite(TEST_SITE_PRESET, name, TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, name, TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC); 
        siteInfo = this.siteService.getSite(name);
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, name, TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC); 
        
        name = "éíóúÁÉÍÓÚ";
        siteInfo = this.siteService.createSite(TEST_SITE_PRESET, name, TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, name, TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC); 
        siteInfo = this.siteService.getSite(name);
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, name, TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC); 
        
        // Test for duplicate site error
        try
        {
            this.siteService.createSite(TEST_SITE_PRESET, "mySiteTest", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
            fail("Shouldn't allow duplicate site short names.");
        }
        catch (AlfrescoRuntimeException exception)
        {
            // Expected
        }
    }
    
    public void testETHREEOH_15() throws Exception
    {
        SiteInfo siteInfo = this.siteService.createSite(TEST_SITE_PRESET, "mySiteTest", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "mySiteTest", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        
        authenticationComponent.setCurrentUser("admin");
        this.siteService.setMembership(siteInfo.getShortName(), USER_TWO, SiteModel.SITE_MANAGER);
        
        authenticationComponent.setCurrentUser(USER_TWO);
        this.siteService.setMembership(siteInfo.getShortName(), USER_THREE, SiteModel.SITE_CONTRIBUTOR);
        this.siteService.removeMembership(siteInfo.getShortName(), USER_THREE);
        
        authenticationComponent.setCurrentUser("admin");
        this.siteService.removeMembership(siteInfo.getShortName(), USER_TWO);
        
        authenticationComponent.setSystemUserAsCurrentUser();
        this.siteService.setMembership(siteInfo.getShortName(), USER_THREE, SiteModel.SITE_CONTRIBUTOR);
        
        authenticationComponent.setCurrentUser(USER_THREE);
        try
        {
            this.siteService.setMembership(siteInfo.getShortName(), USER_TWO, SiteModel.SITE_CONTRIBUTOR);
            fail("Shouldn't be able to do this cos you don't have permissions");
        }
        catch (Exception exception) {}
        try
        {
            this.siteService.removeMembership(siteInfo.getShortName(), USER_ONE);
            fail("Shouldn't be able to do this cos you don't have permissions");
        }
        catch (Exception exception) {}        
        this.siteService.removeMembership(siteInfo.getShortName(), USER_THREE);
    }
    
    private void checkSiteInfo(SiteInfo siteInfo, 
                               String expectedSitePreset, 
                               String expectedShortName, 
                               String expectedTitle, 
                               String expectedDescription, 
                               SiteVisibility expectedVisibility)
    {
        assertNotNull(siteInfo);
        assertEquals(expectedSitePreset, siteInfo.getSitePreset());
        assertEquals(expectedShortName, siteInfo.getShortName());
        assertEquals(expectedTitle, siteInfo.getTitle());
        assertEquals(expectedDescription, siteInfo.getDescription());
        assertEquals(expectedVisibility, siteInfo.getVisibility());
        assertNotNull(siteInfo.getNodeRef());
        
        // Check that the site is a tag scope
        assertTrue(this.taggingService.isTagScope(siteInfo.getNodeRef()));
    }
    
    public void testListSites() throws Exception
    {
        // TODO
        // - check filters
        // - check private excluded when not owner (or admin)
        
        // Check for no sites
        List<SiteInfo> sites = this.siteService.listSites(null, null);
        assertNotNull(sites);
        assertTrue(sites.isEmpty());
        
        // Create some sites
        this.siteService.createSite(TEST_SITE_PRESET, "mySiteOne", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        this.siteService.createSite(TEST_SITE_PRESET, "mySiteTwo", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PRIVATE);
        this.siteService.createSite(TEST_SITE_PRESET_2, "mySiteThree", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        this.siteService.createSite(TEST_SITE_PRESET_2, "mySiteFour", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PRIVATE);
        
        // Get all the sites
        sites = this.siteService.listSites(null, null);
        assertNotNull(sites);
        assertEquals(4, sites.size());
        // Do detailed check of the site info objects
        for (SiteInfo site : sites)
        {
            String shortName = site.getShortName();
            if (shortName.equals("mySiteOne") == true)
            {
                checkSiteInfo(site, TEST_SITE_PRESET, "mySiteOne", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
            }
            else if (shortName.equals("mySiteTwo") == true)
            {
                checkSiteInfo(site, TEST_SITE_PRESET, "mySiteTwo", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PRIVATE);
            }
            else if (shortName.equals("mySiteThree") == true)
            {
                checkSiteInfo(site, TEST_SITE_PRESET_2, "mySiteThree", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
            }
            else if (shortName.equals("mySiteFour") == true)
            {
                checkSiteInfo(site, TEST_SITE_PRESET_2, "mySiteFour", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PRIVATE);                
            }
            else
            {
                fail("The shortname " + shortName + " is not recognised");
            }
        }
        
        sites = this.siteService.listSites(USER_TWO);
        assertNotNull(sites);
        assertEquals(0, sites.size());
        
        this.siteService.setMembership("mySiteOne", USER_TWO, SiteModel.SITE_CONSUMER);
        this.siteService.setMembership("mySiteTwo", USER_TWO, SiteModel.SITE_CONSUMER);
        
        sites = this.siteService.listSites(USER_TWO);
        assertNotNull(sites);
        assertEquals(2, sites.size());
        
        sites = this.siteService.listSites(USER_ONE);
        assertNotNull(sites);
        assertEquals(4, sites.size());
        
    }
    
    public void testGetSite()
    {
        // Get a site that isn't there
        SiteInfo siteInfo = this.siteService.getSite("testGetSite");
        assertNull(siteInfo);
        
        // Create a test site
        this.siteService.createSite(TEST_SITE_PRESET, "testGetSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        
        // Get the test site
        siteInfo = this.siteService.getSite("testGetSite");
        assertNotNull(siteInfo);
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testGetSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC); 
    }
    
    public void testUpdateSite()
    {
        SiteInfo siteInfo = new SiteInfoImpl(TEST_SITE_PRESET, "testUpdateSite", "changedTitle", "changedDescription", SiteVisibility.PRIVATE, null);
        
        // update a site that isn't there
        try
        {
            this.siteService.updateSite(siteInfo);
            fail("Shouldn't be able to update a site that does not exist");
        }
        catch (AlfrescoRuntimeException exception)
        {
            // Expected
        }
        
        // Create a test site
        this.siteService.createSite(TEST_SITE_PRESET, "testUpdateSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        
        // Update the details of the site
        this.siteService.updateSite(siteInfo);
        siteInfo = this.siteService.getSite("testUpdateSite");
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testUpdateSite", "changedTitle", "changedDescription", SiteVisibility.PRIVATE); 
        
        // Update the permission again
        siteInfo.setVisibility(SiteVisibility.PUBLIC);
        this.siteService.updateSite(siteInfo);
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testUpdateSite", "changedTitle", "changedDescription", SiteVisibility.PUBLIC);         
    }
    
    public void testDeleteSite()
    {
        // delete a site that isn't there
        try
        {
            this.siteService.deleteSite("testDeleteSite");
            fail("Shouldn't be able to delete a site that does not exist");
        }
        catch (AlfrescoRuntimeException exception)
        {
            // Expected
        }
        
        // Create a test site
        this.siteService.createSite(TEST_SITE_PRESET, "testUpdateSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        assertNotNull(this.siteService.getSite("testUpdateSite"));
        
        // Delete the site
        this.siteService.deleteSite("testUpdateSite");
        assertNull(this.siteService.getSite("testUpdateSite"));
    }
    
    public void testIsPublic()
    {
        // Create a couple of sites as user one
        this.siteService.createSite(TEST_SITE_PRESET, "isPublicTrue", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        this.siteService.createSite(TEST_SITE_PRESET, "isPublicFalse", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PRIVATE);
        
        // Get the sites as user one
        List<SiteInfo> sites = this.siteService.listSites(null, null);
        assertNotNull(sites);
        assertEquals(2, sites.size());
        
        // Now get the sites as user two
        this.authenticationComponent.setCurrentUser(USER_TWO);
        sites = this.siteService.listSites(null, null);
        assertNotNull(sites);
        assertEquals(1, sites.size());
        checkSiteInfo(sites.get(0), TEST_SITE_PRESET, "isPublicTrue", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        
        // Make user 2 a member of the site
        //TestWithUserUtils.authenticateUser(USER_ONE, "PWD", this.authenticationService, this.authenticationComponent);
        this.authenticationComponent.setCurrentUser(USER_ONE);
        this.siteService.setMembership("isPublicFalse", USER_TWO, SiteModel.SITE_CONSUMER);
        
        // Now get the sites as user two
        this.authenticationComponent.setCurrentUser(USER_TWO);
        sites = this.siteService.listSites(null, null);
        assertNotNull(sites);
        assertEquals(2, sites.size());
    }
    
    public void testMembership()
    {
        // Create a site as user one
        this.siteService.createSite(TEST_SITE_PRESET, "testMembership", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PRIVATE);
        
        // Get the members of the site and check that user one is a manager
        Map<String, String> members = this.siteService.listMembers("testMembership", null, null);
        assertNotNull(members);
        assertEquals(1, members.size());
        assertTrue(members.containsKey(USER_ONE));
        assertEquals(SiteModel.SITE_MANAGER, members.get(USER_ONE));
     
        // Add user two as a consumer and user three as a collaborator
        this.siteService.setMembership("testMembership", USER_TWO, SiteModel.SITE_CONSUMER);
        this.siteService.setMembership("testMembership", USER_THREE, SiteModel.SITE_COLLABORATOR);
        
        // Get the members of the site
        members = this.siteService.listMembers("testMembership", null, null);
        assertNotNull(members);
        assertEquals(3, members.size());
        assertTrue(members.containsKey(USER_ONE));
        assertEquals(SiteModel.SITE_MANAGER, members.get(USER_ONE));
        assertTrue(members.containsKey(USER_TWO));
        assertEquals(SiteModel.SITE_CONSUMER, members.get(USER_TWO));
        assertTrue(members.containsKey(USER_THREE));
        assertEquals(SiteModel.SITE_COLLABORATOR, members.get(USER_THREE));
        
        // Get only the site managers
        members = this.siteService.listMembers("testMembership", null, SiteModel.SITE_MANAGER);
        assertNotNull(members);
        assertEquals(1, members.size());
        assertTrue(members.containsKey(USER_ONE));
        assertEquals(SiteModel.SITE_MANAGER, members.get(USER_ONE));
        
        // Get only user two
        members = this.siteService.listMembers("testMembership", USER_TWO, null);
        assertNotNull(members);
        assertEquals(1, members.size());
        assertTrue(members.containsKey(USER_TWO));
        assertEquals(SiteModel.SITE_CONSUMER, members.get(USER_TWO));
        
        // Change the membership of user two
        this.siteService.setMembership("testMembership", USER_TWO, SiteModel.SITE_COLLABORATOR);
        
        // Check the members of the site
        members = this.siteService.listMembers("testMembership", null, null);
        assertNotNull(members);
        assertEquals(3, members.size());
        assertTrue(members.containsKey(USER_ONE));
        assertEquals(SiteModel.SITE_MANAGER, members.get(USER_ONE));
        assertTrue(members.containsKey(USER_TWO));
        assertEquals(SiteModel.SITE_COLLABORATOR, members.get(USER_TWO));
        assertTrue(members.containsKey(USER_THREE));
        assertEquals(SiteModel.SITE_COLLABORATOR, members.get(USER_THREE));
        
        // Remove user two's membership
        this.siteService.removeMembership("testMembership", USER_TWO);
        
        // Check the members of the site
        members = this.siteService.listMembers("testMembership", null, null);
        assertNotNull(members);
        assertEquals(2, members.size());
        assertTrue(members.containsKey(USER_ONE));
        assertEquals(SiteModel.SITE_MANAGER, members.get(USER_ONE));
        assertTrue(members.containsKey(USER_THREE));
        assertEquals(SiteModel.SITE_COLLABORATOR, members.get(USER_THREE));
        
        // Check that a non-manager and non-member cannot edit the memberships
        this.authenticationComponent.setCurrentUser(USER_TWO);
        try
        {
            this.siteService.setMembership("testMembership", USER_TWO, SiteModel.SITE_COLLABORATOR);
            fail("A non member shouldnt be able to set memberships");
        }
        catch (AlfrescoRuntimeException e)
        {
            // As expected
        }
        try
        {
            this.siteService.removeMembership("testMembership", USER_THREE);
            fail("A non member shouldnt be able to remove a membership");
        }
        catch (AlfrescoRuntimeException e)
        {
            // As expected            
        }
        this.authenticationComponent.setCurrentUser(USER_THREE);
        try
        {
            this.siteService.setMembership("testMembership", USER_TWO, SiteModel.SITE_COLLABORATOR);
            fail("A member who isn't a manager shouldnt be able to set memberships");
        }
        catch (AlfrescoRuntimeException e)
        {
            // As expected
        }
        this.siteService.removeMembership("testMembership", USER_THREE);
        
        this.authenticationComponent.setCurrentUser(USER_ONE);        
        // Try and change the permissions of the only site manager
        this.siteService.setMembership("testMembership", USER_TWO, SiteModel.SITE_MANAGER);
        this.siteService.setMembership("testMembership", USER_TWO, SiteModel.SITE_COLLABORATOR);
        try
        {
            this.siteService.setMembership("testMembership", USER_ONE, SiteModel.SITE_COLLABORATOR);
            fail("You can not change the role of the last site memnager");
        }
        catch (AlfrescoRuntimeException exception)
        {
            // Expected
            //exception.printStackTrace();
        }
        
        // Try and remove the only site manager and should get a failure
        this.siteService.setMembership("testMembership", USER_TWO, SiteModel.SITE_MANAGER);
        this.siteService.removeMembership("testMembership", USER_ONE);
        try
        {
            this.siteService.removeMembership("testMembership", USER_TWO);
            fail("You can not remove the last site memnager from a site");
        }
        catch (AlfrescoRuntimeException exception)
        {
            // Expected
            //exception.printStackTrace();
        }
    }
    
    public void testJoinLeave()
    {
        // Create a site as user one
        this.siteService.createSite(TEST_SITE_PRESET, "testMembership", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        this.siteService.createSite(TEST_SITE_PRESET, "testMembershipPrivate", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PRIVATE);
        
        // Become user two
        //TestWithUserUtils.authenticateUser(USER_TWO, "PWD", this.authenticationService, this.authenticationComponent);
        this.authenticationComponent.setCurrentUser(USER_TWO);
        
        // As user two try and add self as contributor
        try
        {
            this.siteService.setMembership("testMembership", USER_TWO, SiteModel.SITE_COLLABORATOR);
            fail("This should have failed because you don't have permissions");
        }
        catch (AlfrescoRuntimeException exception)
        {
            // Ignore because as expected
        }
        
        // As user two try and add self as consumer to public site
        this.siteService.setMembership("testMembership", USER_TWO, SiteModel.SITE_CONSUMER);
        
        // As user two try and add self as consumer to private site
        try
        {
            this.siteService.setMembership("testMembershipPrivate", USER_TWO, SiteModel.SITE_CONSUMER);
            fail("This should have failed because you can't do this to a private site unless you are site manager");
        }
        catch (AlfrescoRuntimeException exception)
        {
            // Ignore because as expected
        }
        
        // As user two try and add user three as a consumer to a public site
        try
        {
            this.siteService.setMembership("testMembership", USER_THREE, SiteModel.SITE_CONSUMER);
            fail("This should have failed because you can't add another user as a consumer of a public site");
        }
        catch (AlfrescoRuntimeException exception)
        {
            // Ignore because as expected
        }
        
        
        // add some members use in remove tests
        this.authenticationComponent.setCurrentUser(USER_ONE);
        this.siteService.setMembership("testMembership", USER_THREE, SiteModel.SITE_COLLABORATOR);
        this.siteService.setMembership("testMembershipPrivate", USER_TWO, SiteModel.SITE_CONSUMER);
        this.authenticationComponent.setCurrentUser(USER_TWO);
        
        // Try and remove user threes membership from public site
        try
        {
            this.siteService.removeMembership("testMembership", USER_THREE);
            fail("Cannot remove membership");
        }
        catch (Exception exception)
        {
            // Ignore because as expected
        }
        
        // Try and remove own membership
        this.siteService.removeMembership("testMembership", USER_TWO);
    }
        
    public void testContainer()
    {
        // Create a couple of sites as user one
        SiteInfo siteInfo = this.siteService.createSite(TEST_SITE_PRESET, "testContainer", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);

        boolean hasContainer = this.siteService.hasContainer(siteInfo.getShortName(), "folder.component");
        assertFalse(hasContainer);
        NodeRef container1 = this.siteService.getContainer(siteInfo.getShortName(), "folder.component");
        assertNull(container1);
        container1 = this.siteService.createContainer(siteInfo.getShortName(), "folder.component", null, null);
        assertTrue(this.taggingService.isTagScope(container1));
        NodeRef container2 = this.siteService.getContainer(siteInfo.getShortName(), "folder.component");
        assertNotNull(container2);
        assertTrue(this.taggingService.isTagScope(container2));
        assertTrue(container1.equals(container2));
        boolean hasContainer2 = this.siteService.hasContainer(siteInfo.getShortName(), "folder.component");
        assertTrue(hasContainer2);
        boolean hasContainer3 = this.siteService.hasContainer(siteInfo.getShortName(), "folder.component2");
        assertFalse(hasContainer3);
        
        NodeRef container3 = this.siteService.getContainer(siteInfo.getShortName(), "folder.component2");
        assertNull(container3);
        container3 = this.siteService.createContainer(siteInfo.getShortName(), "folder.component2", null, null);
        assertNotNull(container3);
        assertTrue(this.taggingService.isTagScope(container3));        
        assertFalse(container1.equals(container3));
        
        boolean hasContainer4 = this.siteService.hasContainer(siteInfo.getShortName(), "folder.component2");
        assertTrue(hasContainer4);
        boolean hasContainer5 = this.siteService.hasContainer(siteInfo.getShortName(), "folder.component3");
        assertFalse(hasContainer5);
        NodeRef container5 = this.siteService.getContainer(siteInfo.getShortName(), "folder.component3");
        assertNull(container5);
        container5 = this.siteService.createContainer(siteInfo.getShortName(), "folder.component3", ContentModel.TYPE_FOLDER, null);
        assertNotNull(container5);
        
        NodeRef container6 = this.siteService.getContainer(siteInfo.getShortName(), "folder.component3");
        assertNotNull(container6);
        container6 = this.siteService.createContainer(siteInfo.getShortName(), "folder.component3", null, null);
        assertNotNull(container6);
        assertTrue(container5.equals(container6));
        assertEquals(ContentModel.TYPE_FOLDER, nodeService.getType(container6));
        NodeRef container7 = this.siteService.getContainer(siteInfo.getShortName(), "folder.component3");
        assertNotNull(container7);
        container7 = this.siteService.createContainer(siteInfo.getShortName(), "folder.component3", ForumModel.TYPE_FORUM, null);
        assertNotNull(container7);
        assertTrue(container5.equals(container7));
        assertEquals(ContentModel.TYPE_FOLDER, nodeService.getType(container7));
        NodeRef container8 = this.siteService.getContainer(siteInfo.getShortName(), "folder.component4");
        assertNull(container8);
        container8 = this.siteService.createContainer(siteInfo.getShortName(), "folder.component4", ForumModel.TYPE_FORUM, null);
        assertNotNull(container8);
        assertEquals(ForumModel.TYPE_FORUM, nodeService.getType(container8));
    }
    
    public void testSiteGetRoles()
    {
        List<String> roles = this.siteService.getSiteRoles();
        assertNotNull(roles);
        assertFalse(roles.isEmpty());
        
//        for (String role : roles)
//        {
//            System.out.println("Role: " + role);
//        }
    }
    
    public void testCustomSiteProperties()
    {
        QName additionalInformationQName = QName.createQName(SiteModel.SITE_CUSTOM_PROPERTY_URL, "additionalInformation");
        
        // Create a site
        SiteInfo siteInfo = this.siteService.createSite(TEST_SITE_PRESET, "mySiteTest", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "mySiteTest", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        assertNull(siteInfo.getCustomProperty(additionalInformationQName));
        assertNotNull(siteInfo.getCustomProperties());
        assertTrue(siteInfo.getCustomProperties().isEmpty());
        
        // Add an aspect with a custom property
        NodeRef siteNodeRef = siteInfo.getNodeRef();
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>(1);
        properties.put(additionalInformationQName, "information");
        this.nodeService.addAspect(siteNodeRef, QName.createQName(SiteModel.SITE_MODEL_URL, "customSiteProperties"), properties);
        
        // Get the site again
        siteInfo = this.siteService.getSite("mySiteTest");
        assertNotNull(siteInfo);
        assertEquals("information", siteInfo.getCustomProperty(additionalInformationQName));
        assertNotNull(siteInfo.getCustomProperties());
        assertFalse(siteInfo.getCustomProperties().isEmpty());
        assertEquals(1, siteInfo.getCustomProperties().size());
        assertEquals("information", siteInfo.getCustomProperties().get(additionalInformationQName));
        
    }
    
    public void testGroupMembership()
    {
        // USER_ONE - SiteAdmin
        // GROUP_ONE - USER_TWO
        // GROUP_TWO - USER_TWO, USER_THREE
        
        // Create a site as user one
        this.siteService.createSite(TEST_SITE_PRESET, "testMembership", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PRIVATE);

        // Get the members of the site and check that user one is a manager
        Map<String, String> members = this.siteService.listMembers("testMembership", null, null);
        assertNotNull(members);
        assertEquals(1, members.size());
        assertTrue(members.containsKey(USER_ONE));
        assertEquals(SiteModel.SITE_MANAGER, members.get(USER_ONE));

        // Add a group
        this.siteService.setMembership("testMembership", this.groupTwo, SiteModel.SITE_CONSUMER);        
        //   - is the group in the list of all members?
        members = this.siteService.listMembers("testMembership", null, null);
        assertNotNull(members);
        assertEquals(2, members.size());
        assertTrue(members.containsKey(USER_ONE));
        assertEquals(SiteModel.SITE_MANAGER, members.get(USER_ONE));
        assertTrue(members.containsKey(this.groupTwo));
        assertEquals(SiteModel.SITE_CONSUMER, members.get(this.groupTwo));        
        //   - is the user in the expanded list?      
        members = this.siteService.listMembers("testMembership", null, null, true);
        assertNotNull(members);
        assertEquals(3, members.size());
        assertTrue(members.containsKey(USER_ONE));
        assertEquals(SiteModel.SITE_MANAGER, members.get(USER_ONE));
        assertTrue(members.containsKey(USER_TWO));
        assertEquals(SiteModel.SITE_CONSUMER, members.get(USER_TWO));
        assertTrue(members.containsKey(USER_THREE));
        assertEquals(SiteModel.SITE_CONSUMER, members.get(USER_THREE));
        //   - is the user a member?
        assertTrue(this.siteService.isMember("testMembership", USER_ONE));
        assertTrue(this.siteService.isMember("testMembership", USER_TWO));
        assertTrue(this.siteService.isMember("testMembership", USER_THREE));
        //   - is the group a member?
        assertTrue(this.siteService.isMember("testMembership", this.groupTwo));
        //   - can we get the roles for the various members directly
        assertEquals(SiteModel.SITE_MANAGER, this.siteService.getMembersRole("testMembership", USER_ONE));
        assertEquals(SiteModel.SITE_CONSUMER, this.siteService.getMembersRole("testMembership", USER_TWO));
        assertEquals(SiteModel.SITE_CONSUMER, this.siteService.getMembersRole("testMembership", USER_THREE));
        assertEquals(SiteModel.SITE_CONSUMER, this.siteService.getMembersRole("testMembership", this.groupTwo));
        
        // Add a group member as an explicit member
        this.siteService.setMembership("testMembership", USER_THREE, SiteModel.SITE_COLLABORATOR);
        //   - check the explicit members list
        members = this.siteService.listMembers("testMembership", null, null);
        assertNotNull(members);
        assertEquals(3, members.size());
        assertTrue(members.containsKey(USER_ONE));
        assertEquals(SiteModel.SITE_MANAGER, members.get(USER_ONE));
        assertTrue(members.containsKey(USER_THREE));
        assertEquals(SiteModel.SITE_COLLABORATOR, members.get(USER_THREE));
        assertTrue(members.containsKey(this.groupTwo));
        assertEquals(SiteModel.SITE_CONSUMER, members.get(this.groupTwo));        
        //   - check the expanded members list      
        members = this.siteService.listMembers("testMembership", null, null, true);
        assertNotNull(members);
        assertEquals(3, members.size());
        assertTrue(members.containsKey(USER_ONE));
        assertEquals(SiteModel.SITE_MANAGER, members.get(USER_ONE));
        assertTrue(members.containsKey(USER_TWO));
        assertEquals(SiteModel.SITE_CONSUMER, members.get(USER_TWO));
        assertTrue(members.containsKey(USER_THREE));
        assertEquals(SiteModel.SITE_COLLABORATOR, members.get(USER_THREE));
        //   - check is member
        assertTrue(this.siteService.isMember("testMembership", USER_ONE));
        assertTrue(this.siteService.isMember("testMembership", USER_TWO));
        assertTrue(this.siteService.isMember("testMembership", USER_THREE));
        //   - is the group a member?
        assertTrue(this.siteService.isMember("testMembership", this.groupTwo));
        //   - check get role directly
        assertEquals(SiteModel.SITE_MANAGER, this.siteService.getMembersRole("testMembership", USER_ONE));
        assertEquals(SiteModel.SITE_CONSUMER, this.siteService.getMembersRole("testMembership", USER_TWO));
        assertEquals(SiteModel.SITE_COLLABORATOR, this.siteService.getMembersRole("testMembership", USER_THREE));
        assertEquals(SiteModel.SITE_CONSUMER, this.siteService.getMembersRole("testMembership", this.groupTwo));
                
        // Check permissions of added group
        
        // Update the permissions of the group

        // Add other group with higher role
        //  - is group in list?
        //  - is new user a member?
        //  - does redefined user have highest role?

        // Add group user as a specific user with higher role
        //  - check that the user's role is higher that the group?

        // Add a group with a sub-group

        // Remove groups
    }
    
    /**
     * Tests the visibility of a site
     * 
     * See https://issues.alfresco.com/jira/browse/JAWS-291
     */
    public void testSiteVisibility()
    {
        // Create a public site
        SiteInfo siteInfo = createTestSiteWithContent("testSiteVisibilityPublicSite", "testComp", SiteVisibility.PUBLIC);        
        //   - is the value on the site nodeRef correct?
        assertEquals(SiteVisibility.PUBLIC.toString(), this.nodeService.getProperty(siteInfo.getNodeRef(), SiteModel.PROP_SITE_VISIBILITY));
        //   - is the site info correct?
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testSiteVisibilityPublicSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        siteInfo = this.siteService.getSite("testSiteVisibilityPublicSite");
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testSiteVisibilityPublicSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        //   - are the permissions correct for non-members?
        testVisibilityPermissions("Testing visibility of public site", USER_TWO, siteInfo, true, true);
        
        // Create a moderated site
        siteInfo = createTestSiteWithContent("testSiteVisibilityModeratedSite", "testComp", SiteVisibility.MODERATED);
        //  - is the value on the site nodeRef correct?
        assertEquals(SiteVisibility.MODERATED.toString(), this.nodeService.getProperty(siteInfo.getNodeRef(), SiteModel.PROP_SITE_VISIBILITY));
        //  - is the site info correct?
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testSiteVisibilityModeratedSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.MODERATED);
        siteInfo = this.siteService.getSite("testSiteVisibilityModeratedSite");
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testSiteVisibilityModeratedSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.MODERATED);
        //  - are the permissions correct for non-members?
        testVisibilityPermissions("Testing visibility of moderated site", USER_TWO, siteInfo, true, false);
        
        // Create a private site
        siteInfo = createTestSiteWithContent("testSiteVisibilityPrivateSite", "testComp", SiteVisibility.PRIVATE);
        //  - is the value on the site nodeRef correct?
        assertEquals(SiteVisibility.PRIVATE.toString(), this.nodeService.getProperty(siteInfo.getNodeRef(), SiteModel.PROP_SITE_VISIBILITY));
        //  - is the site info correct?
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testSiteVisibilityPrivateSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PRIVATE);
        siteInfo = this.siteService.getSite("testSiteVisibilityPrivateSite");
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testSiteVisibilityPrivateSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PRIVATE);
        //  - are the permissions correct for non-members?
        testVisibilityPermissions("Testing visibility of private site", USER_TWO, siteInfo, false, false);
        
        SiteInfo changeSite = createTestSiteWithContent("testSiteVisibilityChangeSite", "testComp", SiteVisibility.PUBLIC);        
        // Switch from public -> moderated
        changeSite.setVisibility(SiteVisibility.MODERATED);
        this.siteService.updateSite(changeSite);
        //  - check the updated sites visibility
        siteInfo = this.siteService.getSite("testSiteVisibilityChangeSite");
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testSiteVisibilityChangeSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.MODERATED);
        testVisibilityPermissions("Testing visibility of moderated site", USER_TWO, siteInfo, true, false);
        
        // Switch from moderated -> private
        changeSite.setVisibility(SiteVisibility.PRIVATE);
        this.siteService.updateSite(changeSite);
        //  - check the updated sites visibility
        siteInfo = this.siteService.getSite("testSiteVisibilityChangeSite");
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testSiteVisibilityChangeSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PRIVATE);
        testVisibilityPermissions("Testing visibility of moderated site", USER_TWO, siteInfo, false, false);
        
        // Switch from private -> public
        changeSite.setVisibility(SiteVisibility.PUBLIC);
        this.siteService.updateSite(changeSite);
        //  - check the updated sites visibility
        siteInfo = this.siteService.getSite("testSiteVisibilityChangeSite");
        checkSiteInfo(siteInfo, TEST_SITE_PRESET, "testSiteVisibilityChangeSite", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        testVisibilityPermissions("Testing visibility of moderated site", USER_TWO, siteInfo, true, true);
    }
    
    private SiteInfo createTestSiteWithContent(String shortName, String compName, SiteVisibility visibility)
    {
        // Create a public site
        SiteInfo siteInfo = this.siteService.createSite(TEST_SITE_PRESET, 
                                                        shortName, 
                                                        TEST_TITLE, 
                                                        TEST_DESCRIPTION, 
                                                        visibility);
        NodeRef foldeRef = this.siteService.createContainer(shortName, compName, ContentModel.TYPE_FOLDER, null);
        FileInfo fileInfo = this.fileFolderService.create(foldeRef, "test.txt", ContentModel.TYPE_CONTENT);
        ContentWriter writer = this.fileFolderService.getWriter(fileInfo.getNodeRef());
        writer.putContent("Just some old content that doesn't mean anything");
        
        return siteInfo;
    }
    
    private void testVisibilityPermissions(String message, String userName, SiteInfo siteInfo, boolean listSite, boolean readSite)
    {
        String holdUser = this.authenticationComponent.getCurrentUserName();
        this.authenticationComponent.setCurrentUser(userName);
        try
        {
            // Can the site be seen in the list sites by the user?
            List<SiteInfo> sites = this.siteService.listSites(null, null);
            boolean siteInList = sites.contains(siteInfo);
            if (listSite == true && siteInList == false)
            {
                fail(message + ":  The site '" + siteInfo.getShortName() + "' was expected in the list of sites for user '" + userName + "'");
            }
            else if (listSite == false && siteInList == true)
            {
                fail(message + ":  The site '" + siteInfo.getShortName() + "' was NOT expected in the list of sites for user '" + userName + "'");
            }
            
            if (siteInList == true)
            {
                // Can site content be read by the user?
                NodeRef folder = this.siteService.getContainer(siteInfo.getShortName(), "testComp");
                List<FileInfo> files = null;  
                try
                {
                    files = this.fileFolderService.listFiles(folder);
                    if (readSite == false)
                    {
                        fail(message + ":  Content of the site '" + siteInfo.getShortName() + "' was NOT expected to be read by user '" + userName + "'");
                    }
                }
                catch (Exception exception)
                {
                    if (readSite == true)
                    {
                        fail(message + ":  Content of the site '" + siteInfo.getShortName() + "' was expected to be read by user '" + userName + "'");
                    }
                }
            }
        }
        finally
        {
            this.authenticationComponent.setCurrentUser(holdUser);
        }
    }
    
    // == Test the JavaScript API ==
    
    public void testJSAPI() throws Exception
    {
        // Create a site with a custom property
        SiteInfo siteInfo = this.siteService.createSite(TEST_SITE_PRESET, "mySiteWithCustomProperty", TEST_TITLE, TEST_DESCRIPTION, SiteVisibility.PUBLIC);
        NodeRef siteNodeRef = siteInfo.getNodeRef();
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>(1);
        properties.put(QName.createQName(SiteModel.SITE_CUSTOM_PROPERTY_URL, "additionalInformation"), "information");
        this.nodeService.addAspect(siteNodeRef, QName.createQName(SiteModel.SITE_MODEL_URL, "customSiteProperties"), properties);
        
        // Create a model to pass to the unit test scripts
        Map<String, Object> model = new HashMap<String, Object>(1);
        model.put("customSiteName", "mySiteWithCustomProperty");
        
        // Execute the unit test script
        ScriptLocation location = new ClasspathScriptLocation("org/alfresco/repo/site/script/test_siteService.js");
        this.scriptService.executeScript(location, model);
    }

}
