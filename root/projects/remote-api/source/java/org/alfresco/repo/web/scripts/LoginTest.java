/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.repo.web.scripts;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.util.PropertyMap;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.scripts.TestWebScriptServer.GetRequest;
import org.alfresco.web.scripts.TestWebScriptServer.DeleteRequest;
import org.alfresco.web.scripts.TestWebScriptServer.PostRequest;
import org.alfresco.web.scripts.TestWebScriptServer.Response;
import org.json.JSONObject;

/**
 * Junit test for login / logout and validate web scripts
 * 
 * testing uri /api/login
 */
public class LoginTest extends BaseWebScriptTest 
{
    private AuthenticationService authenticationService;
    private AuthenticationComponent authenticationComponent;
    private PersonService personService;
    
    private static final String USER_ONE = "AuthenticationTestOne";
    
    protected void setUp() throws Exception
    {
        super.setUp();
        this.authenticationService = (AuthenticationService)getServer().getApplicationContext().getBean("AuthenticationService");
        this.authenticationComponent = (AuthenticationComponent)getServer().getApplicationContext().getBean("authenticationComponent");
        this.personService = (PersonService)getServer().getApplicationContext().getBean("PersonService");

        this.authenticationComponent.setSystemUserAsCurrentUser();
    	createUser(USER_ONE, USER_ONE);
    }
    
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    private void createUser(String userName, String password)
    {
        if (this.authenticationService.authenticationExists(userName) == false)
        {
            this.authenticationService.createAuthentication(userName, password.toCharArray());
            
            PropertyMap ppOne = new PropertyMap(4);
            ppOne.put(ContentModel.PROP_USERNAME, userName);
            ppOne.put(ContentModel.PROP_FIRSTNAME, "firstName");
            ppOne.put(ContentModel.PROP_LASTNAME, "lastName");
            ppOne.put(ContentModel.PROP_EMAIL, "email@email.com");
            ppOne.put(ContentModel.PROP_JOBTITLE, "jobTitle");
            
            this.personService.createPerson(ppOne);
        }        
    }
    
    private String parseTicket(String ticketResult)
    {
       int startTag = ticketResult.indexOf("<ticket>");
       int endTag = ticketResult.indexOf("</ticket>");
       if ((startTag != -1) && (endTag != -1))
       {
              return ticketResult.substring(startTag+("<ticket>".length()), endTag);
       }
       return "";
    }
    
	/**
	 * Positive test - login and retrieve a ticket via get - return xml,
	 * - via get method 
	 * validate ticket
	 * logout
	 * fail to validate ticket
	 * fail to get ticket
	 */
    public void testAuthentication() throws Exception
    {	
    	/**
    	 * Login via get method to return xml
    	 */
        String loginURL = "/api/login?u=" + USER_ONE + "&pw=" + USER_ONE;
    	Response resp = sendRequest(new GetRequest(loginURL), Status.STATUS_OK);
    	String xmlFragment = resp.getContentAsString();
    	
    	assertNotNull("xmlFragment");
    	assertTrue("xmlFragment contains ticket", xmlFragment.contains("<ticket>"));
    	String ticket = parseTicket(xmlFragment);
 
    	String ticketURL = "/api/login/ticket/"+ticket;
    	
    	/**
    	 * Negative test - validate as AuthenticationUtil.getAdminUserName() - should fail with a 404
    	 */
      	setDefaultRunAs(AuthenticationUtil.getAdminUserName());
    	sendRequest(new GetRequest(ticketURL), Status.STATUS_NOT_FOUND);
      	
    	/**
    	 * Validate the ticket - should succeed
    	 */
    	setDefaultRunAs(USER_ONE);

    	sendRequest(new GetRequest(ticketURL), Status.STATUS_OK);
    	
    	/**
    	 * Logout
    	 */
    	sendRequest(new DeleteRequest(ticketURL), Status.STATUS_OK);

    	/**
    	 * Validate the ticket - should fail now
    	 */
    	sendRequest(new GetRequest(ticketURL), Status.STATUS_NOT_FOUND);	
    	
    }
    
	/**
	 * Positive test - login and retrieve a ticket,
	 * - via json method 
	 */
    public void testAuthenticationGetJSON() throws Exception
    {
     	/**
    	 * Login via get method to return json
    	 */
        String loginURL = "/api/login.json?u=" + USER_ONE + "&pw=" + USER_ONE ;
    	Response resp = sendRequest(new GetRequest(loginURL), Status.STATUS_OK);
    	JSONObject result = new JSONObject(resp.getContentAsString());
    	JSONObject data = result.getJSONObject("data");
    	String ticket = data.getString("ticket");
    	assertNotNull("ticket is null", ticket);
    	
    	/**
    	 * This is now testing the framework ... With a different format.
    	 */
        String login2URL = "/api/login?u=" + USER_ONE + "&pw=" + USER_ONE + "&format=json";
    	Response resp2 = sendRequest(new GetRequest(login2URL), Status.STATUS_OK);
    	JSONObject result2 = new JSONObject(resp2.getContentAsString());
    	JSONObject data2 = result2.getJSONObject("data");
    	String ticket2 = data2.getString("ticket");
    	assertNotNull("ticket is null", ticket2);
    	
    }
    
    /**
     * Authenticate via a POST
     * @throws Exception
     */
    public void testPostLogin() throws Exception
    {
        String loginURL = "/api/login";
        /**
         * logon via POST and JSON
         */
        {
        JSONObject req = new JSONObject();
        req.put("username", USER_ONE);
        req.put("password", USER_ONE);
        Response response = sendRequest(new PostRequest(loginURL, req.toString(), "application/json"), Status.STATUS_OK); 
        
        JSONObject result = new JSONObject(response.getContentAsString());
        JSONObject data = result.getJSONObject("data");
        String ticket = data.getString("ticket");
        assertNotNull("ticket null", ticket);	
        }     
        
        /**
         * Negative test - wrong password
         */
        {
            JSONObject req = new JSONObject();
            req.put("username", USER_ONE);
            req.put("password", "blurb");
            sendRequest(new PostRequest(loginURL, req.toString(), "application/json"), Status.STATUS_FORBIDDEN); 
        }
        /**
         * Negative test - missing username
         */
        {
            JSONObject req = new JSONObject();
            req.put("password", USER_ONE);
            sendRequest(new PostRequest(loginURL, req.toString(), "application/json"), Status.STATUS_BAD_REQUEST); 
        }
        
        /**
         * Negative test - missing password
         */
        {
            JSONObject req = new JSONObject();
            req.put("username", USER_ONE);
            sendRequest(new PostRequest(loginURL, req.toString(), "application/json"), Status.STATUS_BAD_REQUEST); 
        }
    }
    
	
	/**
	 * Negative tests - wrong password
	 */
    public void testWrongPassword() throws Exception
    {
    	/**
    	 * Login via get method and wrong password, should get FORBIDDEN
    	 */
        String loginURL = "/api/login?u=" + USER_ONE + "&pw=" + "crap";
    	sendRequest(new GetRequest(loginURL), Status.STATUS_FORBIDDEN);
    }
	
	/**
	 * Negative test - missing parameters
	 */
    public void testMissingParameters() throws Exception
    {
    	/**
    	 * Login via get method missing pw
    	 */
        String loginURL = "/api/login?u=" + USER_ONE;
    	sendRequest(new GetRequest(loginURL), Status.STATUS_BAD_REQUEST);
    	
    	/**
    	 * Login via get method missing u
    	 */
        String login2URL = "/api/login?&pw=" + USER_ONE;
    	sendRequest(new GetRequest(login2URL), Status.STATUS_BAD_REQUEST);
    	
    }
}
