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
package org.alfresco.repo.web.scripts.blog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.util.PropertyMap;
import org.springframework.extensions.webscripts.TestWebScriptServer.DeleteRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.GetRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PostRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PutRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Unit Test to test Blog Web Script API
 * 
 * @author mruflin
 */
public class BlogServiceTest extends BaseWebScriptTest
{
	@SuppressWarnings("unused")
    private static Log logger = LogFactory.getLog(BlogServiceTest.class);
	
    private MutableAuthenticationService authenticationService;
    private AuthenticationComponent authenticationComponent;
    private PersonService personService;
    private SiteService siteService;
    
    private static final String USER_ONE = "UserOneSecondToo";
    private static final String USER_TWO = "UserTwoSecondToo";
    private static final String SITE_SHORT_NAME_BLOG = "BlogSiteShortNameTest";
    private static final String COMPONENT_BLOG = "blog";

    private static final String URL_BLOG_POST = "/api/blog/post/site/" + SITE_SHORT_NAME_BLOG + "/" + COMPONENT_BLOG + "/";
    private static final String URL_BLOG_POSTS = "/api/blog/site/" + SITE_SHORT_NAME_BLOG + "/" + COMPONENT_BLOG + "/posts";
    
    private List<String> posts = new ArrayList<String>(5);
    private List<String> drafts = new ArrayList<String>(5);

    
    // General methods

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        this.authenticationService = (MutableAuthenticationService)getServer().getApplicationContext().getBean("AuthenticationService");
        this.authenticationComponent = (AuthenticationComponent)getServer().getApplicationContext().getBean("authenticationComponent");
        this.personService = (PersonService)getServer().getApplicationContext().getBean("PersonService");
        this.siteService = (SiteService)getServer().getApplicationContext().getBean("SiteService");
        
        // Authenticate as user
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        
        // Create test site
        // - only create the site if it doesn't already exist
        SiteInfo siteInfo = this.siteService.getSite(SITE_SHORT_NAME_BLOG);
        if (siteInfo == null)
        {
            this.siteService.createSite("BlogSitePreset", SITE_SHORT_NAME_BLOG, "BlogSiteTitle", "BlogSiteDescription", SiteVisibility.PUBLIC);
        }
        
        // Create users
        createUser(USER_ONE, SiteModel.SITE_COLLABORATOR);
        createUser(USER_TWO, SiteModel.SITE_COLLABORATOR);

        // Do tests as inviter user
        this.authenticationComponent.setCurrentUser(USER_ONE);
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        // admin user required to delete user
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        
        // TODO don't delete them as it seems they don't get cleaned up correctly
        // delete the inviter user
      //  personService.deletePerson(USER_ONE);
      //  this.authenticationService.deleteAuthentication(USER_ONE);
      //  personService.deletePerson(USER_TWO);
      //  this.authenticationService.deleteAuthentication(USER_TWO);
        
        // delete invite site
        siteService.deleteSite(SITE_SHORT_NAME_BLOG);
    }
    
    private void createUser(String userName, String role)
    {
        // if user with given user name doesn't already exist then create user
        if (this.authenticationService.authenticationExists(userName) == false)
        {
            // create user
            this.authenticationService.createAuthentication(userName, "password".toCharArray());
            
            // create person properties
            PropertyMap personProps = new PropertyMap();
            personProps.put(ContentModel.PROP_USERNAME, userName);
            personProps.put(ContentModel.PROP_FIRSTNAME, "FirstName123");
            personProps.put(ContentModel.PROP_LASTNAME, "LastName123");
            personProps.put(ContentModel.PROP_EMAIL, "FirstName123.LastName123@email.com");
            personProps.put(ContentModel.PROP_JOBTITLE, "JobTitle123");
            personProps.put(ContentModel.PROP_JOBTITLE, "Organisation123");
            
            // create person node for user
            this.personService.createPerson(personProps);
        }
        
        // add the user as a member with the given role
        this.siteService.setMembership(SITE_SHORT_NAME_BLOG, userName, role);
    }
    
    
    // Test helper methods 
    
    private JSONObject getRequestObject(String title, String content, String[] tags, boolean isDraft)
    throws Exception
    {
        JSONObject post = new JSONObject();
        if (title != null)
        {
        	post.put("title", title);
        }
        if (content != null)
        {
        	post.put("content", content);
        }
        if (tags != null)
        {
        	JSONArray arr = new JSONArray();
        	for(String s : tags)
        	{
        		arr.put(s);
        	}
        	post.put("tags", arr);
        }
        post.put("draft", isDraft);
        return post;
    }
    
    private JSONObject createPost(String title, String content, String[] tags, boolean isDraft, int expectedStatus)
    throws Exception
    {
        JSONObject post = getRequestObject(title, content, tags, isDraft);
	    Response response = sendRequest(new PostRequest(URL_BLOG_POSTS, post.toString(), "application/json"), expectedStatus);
	    
	    if (expectedStatus != 200)
	    {
	    	return null;
	    }
	    
    	//logger.debug(response.getContentAsString());
    	JSONObject result = new JSONObject(response.getContentAsString());
    	JSONObject item = result.getJSONObject("item");
    	if (isDraft)
    	{
    		this.drafts.add(item.getString("name"));
    	}
    	else
    	{
    	    this.posts.add(item.getString("name"));
    	}
    	return item;
    }
    
    private JSONObject updatePost(String name, String title, String content, String[] tags, boolean isDraft, int expectedStatus)
    throws Exception
    {
    	JSONObject post = getRequestObject(title, content, tags, isDraft);
	    Response response = sendRequest(new PutRequest(URL_BLOG_POST + name, post.toString(), "application/json"), expectedStatus);
	    
	    if (expectedStatus != 200)
	    {
	    	return null;
	    }

    	JSONObject result = new JSONObject(response.getContentAsString());
    	return result.getJSONObject("item");
    }
    
    private JSONObject getPost(String name, int expectedStatus)
    throws Exception
    {
    	Response response = sendRequest(new GetRequest(URL_BLOG_POST + name), expectedStatus);
    	if (expectedStatus == 200)
    	{
    		JSONObject result = new JSONObject(response.getContentAsString());
    		return result.getJSONObject("item");
    	}
    	else
    	{
    		return null;
    	}
    }
    
    private String getCommentsUrl(String nodeRef)
    {
    	return "/api/node/" + nodeRef.replace("://", "/") + "/comments";
    }
    
    private String getCommentUrl(String nodeRef)
    {
    	return "/api/comment/node/" + nodeRef.replace("://", "/");
    }
    
    private JSONObject createComment(String nodeRef, String title, String content, int expectedStatus)
    throws Exception
    {
        JSONObject comment = new JSONObject();
        comment.put("title", title);
        comment.put("content", content);
	    Response response = sendRequest(new PostRequest(getCommentsUrl(nodeRef), comment.toString(), "application/json"), expectedStatus);
	    
	    if (expectedStatus != 200)
	    {
	    	return null;
	    }
	    
	    //logger.debug("Comment created: " + response.getContentAsString());
    	JSONObject result = new JSONObject(response.getContentAsString());
    	return result.getJSONObject("item");
    }
    
    private JSONObject updateComment(String nodeRef, String title, String content, int expectedStatus)
    throws Exception
    {
    	JSONObject comment = new JSONObject();
        comment.put("title", title);
        comment.put("content", content);
	    Response response = sendRequest(new PutRequest(getCommentUrl(nodeRef), comment.toString(), "application/json"), expectedStatus);
	    
	    if (expectedStatus != 200)
	    {
	    	return null;
	    }
	    
	    //logger.debug("Comment updated: " + response.getContentAsString());
    	JSONObject result = new JSONObject(response.getContentAsString());
    	return result.getJSONObject("item");
    }
    
    
    // Tests
    
    public void testCreateDraftPost() throws Exception
    {
    	String title = "test";
    	String content = "test";
    	JSONObject item = createPost(title, content, null, true, 200);
    	
    	// check that the values
    	assertEquals(title, item.get("title"));
    	assertEquals(content, item.get("content"));
    	assertEquals(true, item.get("isDraft"));
    	
    	// check that other user doesn't have access to the draft
    	this.authenticationComponent.setCurrentUser(USER_TWO);
    	getPost(item.getString("name"), 404);
    	this.authenticationComponent.setCurrentUser(USER_ONE);
    }
    
    public void testCreatePublishedPost() throws Exception
    {
    	String title = "published";
    	String content = "content";
    	
    	JSONObject item = createPost(title, content, null, false, 200);
    	
    	// check the values
    	assertEquals(title, item.get("title"));
    	assertEquals(content, item.get("content"));
    	assertEquals(false, item.get("isDraft"));
    	
    	// check that user two has access to it as well
    	this.authenticationComponent.setCurrentUser(USER_TWO);
    	getPost(item.getString("name"), 200);
    	this.authenticationComponent.setCurrentUser(USER_ONE);
    }
    
    public void testCreateEmptyPost() throws Exception
    {
    	JSONObject item = createPost(null, null, null, false, 200);
    	
    	// check the values
    	assertEquals("", item.get("title"));
    	assertEquals("", item.get("content"));
    	assertEquals(false, item.get("isDraft"));
    	
    	// check that user two has access to it as well
    	this.authenticationComponent.setCurrentUser(USER_TWO);
    	getPost(item.getString("name"), 200);
    	this.authenticationComponent.setCurrentUser(USER_ONE);
    }
    
    public void testUpdated() throws Exception
    {
    	JSONObject item = createPost("test", "test", null, false, 200);
    	String name = item.getString("name");
    	assertEquals(false, item.getBoolean("isUpdated"));
    	
    	item = updatePost(name, "new title", "new content", null, false, 200);
    	assertEquals(true, item.getBoolean("isUpdated"));
    	assertEquals("new title", item.getString("title"));
    	assertEquals("new content", item.getString("content"));
    }
    
    public void testUpdateWithEmptyValues() throws Exception
    {
    	JSONObject item = createPost("test", "test", null, false, 200);
    	String name = item.getString("name");
    	assertEquals(false, item.getBoolean("isUpdated"));
    	
    	item = updatePost(item.getString("name"), null, null, null, false, 200);
    	assertEquals("", item.getString("title"));
    	assertEquals("", item.getString("content"));
    }
    
    public void testPublishThroughUpdate() throws Exception
    {
    	JSONObject item = createPost("test", "test", null, true, 200);
    	String name = item.getString("name");
    	assertEquals(true, item.getBoolean("isDraft"));
    	
    	// check that user two does not have access
    	this.authenticationComponent.setCurrentUser(USER_TWO);
    	getPost(name, 404);
    	this.authenticationComponent.setCurrentUser(USER_ONE);
    	
    	item = updatePost(name, "new title", "new content", null, false, 200);
    	assertEquals("new title", item.getString("title"));
    	assertEquals("new content", item.getString("content"));
    	assertEquals(false, item.getBoolean("isDraft"));
    	
    	// check that user two does have access
    	this.authenticationComponent.setCurrentUser(USER_TWO);
    	getPost(name, 200);
    	this.authenticationComponent.setCurrentUser(USER_ONE);
    }

    public void testCannotDoUnpublish() throws Exception
    {
    	JSONObject item = createPost("test", "test", null, false, 200);
    	String name = item.getString("name");
    	assertEquals(false, item.getBoolean("isDraft"));
    	
    	item = updatePost(name, "new title", "new content", null, true, 400); // should return bad request
    }
    
    public void testGetAll() throws Exception
    {
    	String url = URL_BLOG_POSTS;
    	Response response = sendRequest(new GetRequest(url), 200);
    	JSONObject result = new JSONObject(response.getContentAsString());
    	
    	// we should have posts.size + drafts.size together
    	assertEquals(this.posts.size() + this.drafts.size(), result.getInt("total"));
    }
    
    public void testGetNew() throws Exception
    {
    	String url = URL_BLOG_POSTS + "/new";
    	Response response = sendRequest(new GetRequest(url), 200);
    	JSONObject result = new JSONObject(response.getContentAsString());
    	
    	// we should have posts.size
    	assertEquals(this.posts.size(), result.getInt("total"));
    }
    
    public void _testGetDrafts() throws Exception
    {
    	String url = URL_BLOG_POSTS + "/mydrafts";
    	Response response = sendRequest(new GetRequest(URL_BLOG_POSTS), 200);
    	JSONObject result = new JSONObject(response.getContentAsString());
    	
    	// we should have drafts.size resultss
    	assertEquals(this.drafts.size(), result.getInt("total"));
    	
    	// the second user should have zero
    	this.authenticationComponent.setCurrentUser(USER_TWO);
    	response = sendRequest(new GetRequest(url), 200);
    	result = new JSONObject(response.getContentAsString());
    	assertEquals(0, result.getInt("total"));
    	this.authenticationComponent.setCurrentUser(USER_ONE);
    	
    }
    
    public void _testMyPublished() throws Exception
    {
    	String url = URL_BLOG_POSTS + "/mypublished";
    	Response response = sendRequest(new GetRequest(url), 200);
    	JSONObject result = new JSONObject(response.getContentAsString());
    	
    	// we should have posts.size results
    	assertEquals(this.drafts.size(), result.getInt("total"));
    	
    	// the second user should have zero
    	this.authenticationComponent.setCurrentUser(USER_TWO);
    	response = sendRequest(new GetRequest(url), 200);
    	result = new JSONObject(response.getContentAsString());
    	assertEquals(0, result.getInt("total"));
    	this.authenticationComponent.setCurrentUser(USER_ONE);
    }

    public void testComments() throws Exception
    {
    	JSONObject item = createPost("test", "test", null, false, 200);
    	String name = item.getString("name");
    	String nodeRef = item.getString("nodeRef");
    	
    	JSONObject commentOne = createComment(nodeRef, "comment", "content", 200);
    	JSONObject commentTwo = createComment(nodeRef, "comment", "content", 200);
    	
    	// fetch the comments
    	Response response = sendRequest(new GetRequest(getCommentsUrl(nodeRef)), 200);
    	JSONObject result = new JSONObject(response.getContentAsString());
    	assertEquals(2, result.getInt("total"));
    	
    	// add another one
    	JSONObject commentThree = createComment(nodeRef, "comment", "content", 200);
    	
    	response = sendRequest(new GetRequest(getCommentsUrl(nodeRef)), 200);
    	result = new JSONObject(response.getContentAsString());
    	assertEquals(3, result.getInt("total"));
    	
    	// delete the last comment
    	response = sendRequest(new DeleteRequest(getCommentUrl(commentThree.getString("nodeRef"))), 200);
    	
    	response = sendRequest(new GetRequest(getCommentsUrl(nodeRef)), 200);
    	result = new JSONObject(response.getContentAsString());
    	assertEquals(2, result.getInt("total"));
    	
    	JSONObject commentTwoUpdated = updateComment(commentTwo.getString("nodeRef"), "new title", "new content", 200);
    	assertEquals("new title", commentTwoUpdated.getString("title"));
    	assertEquals("new content", commentTwoUpdated.getString("content"));
    }
 
    /**
     * Does some stress tests.
     * 
     * Currently observed errors:
     * 1. [repo.action.AsynchronousActionExecutionQueueImpl] Failed to execute asynchronous action: Action[ id=485211db-f117-4976-9530-ab861a19f563, node=null ]
     * org.alfresco.repo.security.permissions.AccessDeniedException: Access Denied.  You do not have the appropriate permissions to perform this operation. 
     * 
     * 2. JSONException, but with root cause being
     *   get(assocs) failed on instance of org.alfresco.repo.template.TemplateNode
     *   The problematic instruction:	
     *   ----------
	 *   ==> if person.assocs["cm:avatar"]?? [on line 4, column 7 in org/alfresco/repository/blogs/blogpost.lib.ftl]
	 *   
     * @throws Exception
     */
    public void _testTagsStressTest() throws Exception
    {
    	final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
    	List<Thread> threads = new ArrayList<Thread>();
    	
        System.err.println("Creating and starting threads...");
    	for (int x=0; x < 3; x++)
    	{
    		Thread t = new Thread(new Runnable() {
    			public void run() {
    				// set the correct user
    				authenticationComponent.setCurrentUser(USER_ONE);
    				
    				// now do some requests
    				try {
	    				for (int y=0; y < 3; y++)
	    				{
	    					_testPostTags();
	    					_testClearTags();
	    					
	    				}
	    				System.err.println("------------- SUCCEEDED ---------------");
    				} catch (Exception e)
    				{
    					System.err.println("------------- ERROR ---------------");
    					exceptions.add(e);
    					e.printStackTrace();
    					return;
    				}
    			}}
    		);
    		threads.add(t);
    		t.start();
    	} 
    	/*for (Thread t : threads)
    	{
    		t.start();
    	}*/
    	
    	for (Thread t : threads)
    	{
    		t.join();
    	}
    	
    	System.err.println("------------- STACK TRACES ---------------");
    	for (Exception e : exceptions)
    	{
    		e.printStackTrace();
    	}
    	System.err.println("------------- STACK TRACES END ---------------");
    	if (exceptions.size() > 0)
    	{
    		throw exceptions.get(0);
    	}
    }
    
    public void _testPostTags() throws Exception
    {
    	String[] tags = { "first", "test" };
    	JSONObject item = createPost("tagtest", "tagtest", tags, false, 200);
    	assertEquals(2, item.getJSONArray("tags").length());
    	assertEquals("first", item.getJSONArray("tags").get(0));
    	assertEquals("test", item.getJSONArray("tags").get(1));
    	
    	item = updatePost(item.getString("name"), null, null, new String[] { "First", "Test", "Second" }, false, 200);
    	assertEquals(3, item.getJSONArray("tags").length());
    	assertEquals("first", item.getJSONArray("tags").get(0));
    	assertEquals("test", item.getJSONArray("tags").get(1));
    	assertEquals("second", item.getJSONArray("tags").get(2));
    }
    
    public void _testClearTags() throws Exception
    {
    	String[] tags = { "abc", "def"};
    	JSONObject item = createPost("tagtest", "tagtest", tags, false, 200);
    	assertEquals(2, item.getJSONArray("tags").length());
    	
    	item = updatePost(item.getString("name"), null, null, new String[0], false, 200);
    	assertEquals(0, item.getJSONArray("tags").length());
    }

}