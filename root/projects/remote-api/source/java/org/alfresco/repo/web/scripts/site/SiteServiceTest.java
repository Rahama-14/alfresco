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
package org.alfresco.repo.web.scripts.site;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;
import org.alfresco.web.scripts.TestWebScriptServer.DeleteRequest;
import org.alfresco.web.scripts.TestWebScriptServer.GetRequest;
import org.alfresco.web.scripts.TestWebScriptServer.PostRequest;
import org.alfresco.web.scripts.TestWebScriptServer.PutRequest;
import org.alfresco.web.scripts.TestWebScriptServer.Response;
import org.htmlparser.parserapplications.SiteCapturer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Unit test to test site Web Script API
 * 
 * @author Roy Wetherall
 */
public class SiteServiceTest extends BaseWebScriptTest
{    
    private AuthenticationService authenticationService;
    private AuthenticationComponent authenticationComponent;
    private PersonService personService;
    private SiteService siteService;
    private NodeService nodeService;
    
    private static final String USER_ONE = "SiteTestOne";
    private static final String USER_TWO = "SiteTestTwo";
    private static final String USER_THREE = "SiteTestThree";
    
    private static final String URL_SITES = "/api/sites";
    private static final String URL_MEMBERSHIPS = "/memberships";
    
    private List<String> createdSites = new ArrayList<String>(5);
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        this.authenticationService = (AuthenticationService)getServer().getApplicationContext().getBean("AuthenticationService");
        this.authenticationComponent = (AuthenticationComponent)getServer().getApplicationContext().getBean("authenticationComponent");
        this.personService = (PersonService)getServer().getApplicationContext().getBean("PersonService");
        this.siteService = (SiteService)getServer().getApplicationContext().getBean("SiteService");
        this.nodeService = (NodeService)getServer().getApplicationContext().getBean("NodeService");
        
        this.authenticationComponent.setSystemUserAsCurrentUser();
        
        // Create users
        createUser(USER_ONE);
        createUser(USER_TWO);
        createUser(USER_THREE);
        
        // Do tests as user one
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
    
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        this.authenticationComponent.setCurrentUser("admin");
        
        // Tidy-up any site's create during the execution of the test
        for (String shortName : this.createdSites)
        {
            sendRequest(new DeleteRequest(URL_SITES + "/" + shortName), 0);
        }
        
        // Clear the list
        this.createdSites.clear();
    }
    
    public void testCreateSite() throws Exception
    {
        String shortName  = GUID.generate();
        
        // Create a new site
        JSONObject result = createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);        
        assertEquals("myPreset", result.get("sitePreset"));
        assertEquals(shortName, result.get("shortName"));
        assertEquals("myTitle", result.get("title"));
        assertEquals("myDescription", result.get("description"));
        assertNotNull(result.get("node"));
        assertNotNull(result.get("tagScope"));
        assertEquals(SiteVisibility.PUBLIC.toString(), result.get("visibility"));
        assertTrue(result.getBoolean("isPublic"));
        
        // Check for duplicate names
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 500); 
    }
    
    private JSONObject createSite(String sitePreset, String shortName, String title, String description, SiteVisibility visibility, int expectedStatus)
        throws Exception
    {
        JSONObject site = new JSONObject();
        site.put("sitePreset", sitePreset);
        site.put("shortName", shortName);
        site.put("title", title);
        site.put("description", description);
        site.put("visibility", visibility.toString());                
        Response response = sendRequest(new PostRequest(URL_SITES, site.toString(), "application/json"), expectedStatus); 
        this.createdSites.add(shortName);
        return new JSONObject(response.getContentAsString());
    }
    
    public void testGetSites() throws Exception
    {
        Response response = sendRequest(new GetRequest(URL_SITES), 200);        
        JSONArray result = new JSONArray(response.getContentAsString());        
        assertNotNull(result);
        assertEquals(0, result.length());
        
        createSite("myPreset", GUID.generate(), "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", GUID.generate(), "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", GUID.generate(), "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", GUID.generate(), "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", GUID.generate(), "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        response = sendRequest(new GetRequest(URL_SITES), 200);        
        result = new JSONArray(response.getContentAsString());        
        assertNotNull(result);
        assertEquals(5, result.length());
        
        response = sendRequest(new GetRequest(URL_SITES + "?size=3"), 200);        
        result = new JSONArray(response.getContentAsString());        
        assertNotNull(result);
        assertEquals(3, result.length());        

        response = sendRequest(new GetRequest(URL_SITES + "?size=13"), 200);        
        result = new JSONArray(response.getContentAsString());        
        assertNotNull(result);
        assertEquals(5, result.length());
    }
    
    public void testGetSite() throws Exception
    {
        // Get a site that doesn't exist
        Response response = sendRequest(new GetRequest(URL_SITES + "/" + "somerandomshortname"), 404);
        
        // Create a site and get it
        String shortName  = GUID.generate();
        JSONObject result = createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        response = sendRequest(new GetRequest(URL_SITES + "/" + shortName), 200);
       
    }
    
    public void testUpdateSite() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        JSONObject result = createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Update the site
        result.put("title", "abs123abc");
        result.put("description", "123abc123");
        result.put("visibility", SiteVisibility.PRIVATE.toString());
        Response response = sendRequest(new PutRequest(URL_SITES + "/" + shortName, result.toString(), "application/json"), 200);
        result = new JSONObject(response.getContentAsString());
        assertEquals("abs123abc", result.get("title"));
        assertEquals("123abc123", result.get("description"));
        assertFalse(result.getBoolean("isPublic"));
        assertEquals(SiteVisibility.PRIVATE.toString(), result.get("visibility"));
        
        // Try and get the site and double check it's changed
        response = sendRequest(new GetRequest(URL_SITES + "/" + shortName), 200);
        result = new JSONObject(response.getContentAsString());
        assertEquals("abs123abc", result.get("title"));
        assertEquals("123abc123", result.get("description"));
        assertFalse(result.getBoolean("isPublic"));
        assertEquals(SiteVisibility.PRIVATE.toString(), result.get("visibility"));
    }
    
    public void testDeleteSite() throws Exception
    {
        // Delete non-existent site
        Response response = sendRequest(new DeleteRequest(URL_SITES + "/" + "somerandomshortname"), 404);
        
        // Create a site
        String shortName  = GUID.generate();
        JSONObject result = createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Get the site
        response = sendRequest(new GetRequest(URL_SITES + "/" + shortName), 200);
        
        // Delete the site
        response = sendRequest(new DeleteRequest(URL_SITES + "/" + shortName), 200);
        
        // Get the site
        response = sendRequest(new GetRequest(URL_SITES + "/" + shortName), 404);
    }
    
    public void testGetMemeberships() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Check the memberships
        Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS), 200);
        JSONArray result = new JSONArray(response.getContentAsString());        
        assertNotNull(result);
        assertEquals(1, result.length());
        JSONObject membership = result.getJSONObject(0);
        assertEquals(SiteModel.SITE_MANAGER, membership.get("role"));
        assertEquals(USER_ONE, membership.getJSONObject("person").get("userName"));        
    }
    
    public void testPostMemberships() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Build the JSON membership object
        JSONObject membership = new JSONObject();
        membership.put("role", SiteModel.SITE_CONSUMER);
        JSONObject person = new JSONObject();
        person.put("userName", USER_TWO);
        membership.put("person", person);
        
        // Post the membership
        Response response = sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        JSONObject result = new JSONObject(response.getContentAsString());
        
        // Check the result
        assertEquals(SiteModel.SITE_CONSUMER, membership.get("role"));
        assertEquals(USER_TWO, membership.getJSONObject("person").get("userName")); 
        
        // Get the membership list
        response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS), 200);   
        JSONArray result2 = new JSONArray(response.getContentAsString());
        assertNotNull(result2);
        assertEquals(2, result2.length());
    }
    
    public void testGetMembership() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Test error conditions
        sendRequest(new GetRequest(URL_SITES + "/badsite" + URL_MEMBERSHIPS + "/" + USER_ONE), 404);
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/baduser"), 404);
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_TWO), 404);
        
        Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_ONE), 200);
        JSONObject result = new JSONObject(response.getContentAsString());
        
        // Check the result
        assertEquals(SiteModel.SITE_MANAGER, result.get("role"));
        assertEquals(USER_ONE, result.getJSONObject("person").get("userName")); 
    }
    
    public void testPutMembership() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Test error conditions
        // TODO
        
        // Build the JSON membership object
        JSONObject membership = new JSONObject();
        membership.put("role", SiteModel.SITE_CONSUMER);
        JSONObject person = new JSONObject();
        person.put("userName", USER_TWO);
        membership.put("person", person);
        
        // Post the membership
        Response response = sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        JSONObject newMember = new JSONObject(response.getContentAsString());
        
        // Update the role
        newMember.put("role", SiteModel.SITE_COLLABORATOR);
        response = sendRequest(new PutRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_TWO, newMember.toString(), "application/json"), 200);
        JSONObject result = new JSONObject(response.getContentAsString());
        
        // Check the result
        assertEquals(SiteModel.SITE_COLLABORATOR, result.get("role"));
        assertEquals(USER_TWO, result.getJSONObject("person").get("userName"));
        
        // Double check and get the membership for user two
        response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_TWO), 200);
        result = new JSONObject(response.getContentAsString());
        assertEquals(SiteModel.SITE_COLLABORATOR, result.get("role"));
        assertEquals(USER_TWO, result.getJSONObject("person").get("userName"));
    }
    
    public void testDeleteMembership() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
     
        // Build the JSON membership object
        JSONObject membership = new JSONObject();
        membership.put("role", SiteModel.SITE_CONSUMER);
        JSONObject person = new JSONObject();
        person.put("userName", USER_TWO);
        membership.put("person", person);
        
        // Post the membership
        sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        
        // Delete the membership
        sendRequest(new DeleteRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_TWO), 200);
        
        // Check that the membership has been deleted
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_TWO), 404);
        
    }
    
    public void testGetPersonSites() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        String shortName2  = GUID.generate();
        createSite("myPreset", shortName2, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        Response response = sendRequest(new GetRequest("/api/people/" + USER_TWO + "/sites"), 200);
        JSONArray result = new JSONArray(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(0, result.length());
        
        // Add some memberships
        JSONObject membership = new JSONObject();
        membership.put("role", SiteModel.SITE_CONSUMER);
        JSONObject person = new JSONObject();
        person.put("userName", USER_TWO);
        membership.put("person", person);
        sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        membership = new JSONObject();
        membership.put("role", SiteModel.SITE_CONSUMER);
        person = new JSONObject();
        person.put("userName", USER_TWO);
        membership.put("person", person);
        sendRequest(new PostRequest(URL_SITES + "/" + shortName2 + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);        
        
        response = sendRequest(new GetRequest("/api/people/" + USER_TWO + "/sites"), 200);
        result = new JSONArray(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(2, result.length());
        
        response = sendRequest(new GetRequest("/api/people/" + USER_ONE + "/sites"), 200);
        result = new JSONArray(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(2, result.length());
        
        response = sendRequest(new GetRequest("/api/people/" + USER_THREE + "/sites"), 200);
        result = new JSONArray(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(0, result.length());
        
        response = sendRequest(new GetRequest("/api/people/" + USER_ONE + "/sites?size=1"), 200);
        result = new JSONArray(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(1, result.length());
        
        response = sendRequest(new GetRequest("/api/people/" + USER_ONE + "/sites?size=5"), 200);
        result = new JSONArray(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(2, result.length());
    }   
    
    public void testSiteCustomProperties()
        throws Exception
    {
        // Create a site with a custom property
        SiteInfo siteInfo = this.siteService.createSite("testPreset", "mySiteWithCustomProperty2", "testTitle", "testDescription", SiteVisibility.PUBLIC);
        NodeRef siteNodeRef = siteInfo.getNodeRef();
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>(1);
        properties.put(QName.createQName(SiteModel.SITE_CUSTOM_PROPERTY_URL, "additionalInformation"), "information");
        this.nodeService.addAspect(siteNodeRef, QName.createQName(SiteModel.SITE_MODEL_URL, "customSiteProperties"), properties);
        this.createdSites.add("mySiteWithCustomProperty2");
        
        // Get the detail so of the site
        Response response = sendRequest(new GetRequest(URL_SITES + "/mySiteWithCustomProperty2"), 200);
        JSONObject result = new JSONObject(response.getContentAsString());
        assertNotNull(result);
        JSONObject customProperties = result.getJSONObject("customProperties");
        assertNotNull(customProperties);
        JSONObject addInfo = customProperties.getJSONObject("{http://www.alfresco.org/model/sitecustomproperty/1.0}additionalInformation");
        assertNotNull(addInfo);
        assertEquals("{http://www.alfresco.org/model/sitecustomproperty/1.0}additionalInformation", addInfo.get("name"));
        assertEquals("information", addInfo.get("value"));
        assertEquals("{http://www.alfresco.org/model/dictionary/1.0}text", addInfo.get("type"));
        assertEquals("Additional Site Information", addInfo.get("title"));
        
    }
}
