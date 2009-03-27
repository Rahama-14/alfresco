/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.repo.activities;

import java.util.HashMap;
import java.util.List;

import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.activities.ActivityService;
import org.alfresco.service.cmr.activities.FeedControl;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.util.BaseSpringTest;

/**
 * Activity Service Implementation unit test
 * 
 * @author janv
 */
public class ActivityServiceImplTest extends BaseSpringTest 
{
    private ActivityService activityService;
    private ScriptService scriptService;
    private AuthenticationService authenticationService;
    
    private static final String ADMIN_PW = "admin";
    
    private static final String USER_UN = "bob";
    private static final String USER_PW = "bob";
    
    protected void onSetUpInTransaction() throws Exception
    {
        super.onSetUpInTransaction();
        
        // Get the required services
        this.activityService = (ActivityService)this.applicationContext.getBean("activityService");
        this.scriptService = (ScriptService)this.applicationContext.getBean("ScriptService");
        
        this.authenticationService = (AuthenticationService)applicationContext.getBean("authenticationService");
        
        authenticationService.authenticate(AuthenticationUtil.getAdminUserName(), ADMIN_PW.toCharArray());
    }
    
    protected void onTearDownInTransaction() throws Exception
    {
        authenticationService.clearCurrentSecurityContext();
    }
	
    public void testPostValidActivities() throws Exception
    {
        this.activityService.postActivity("org.alfresco.testActivityType1", null, null, "");
        
        this.activityService.postActivity("org.alfresco.testActivityType2", "", "", "");
        
        this.activityService.postActivity("org.alfresco.testActivityType3", "site1", "appToolA", "{ \"var1\" : \"val1\" }");
    }
    
    public void testPostInvalidActivities() throws Exception
    {
        try
        {
            this.activityService.postActivity("", "", "", null, "");
            fail("invalid post activity");
        }
        catch (IllegalArgumentException iae)
        {
            assertTrue(iae.getMessage().contains("nodeRef is a mandatory parameter"));
        }
        
        try
        {
            this.activityService.postActivity("", "", "", "");
            fail("invalid post activity");
        }
        catch (IllegalArgumentException iae)
        {
            assertTrue(iae.getMessage().contains("activityType is a mandatory parameter"));
        }
    }   
        
    public void testGetEmptySiteFeed() throws Exception
    {
        authenticationService.clearCurrentSecurityContext();
        
        if(! authenticationService.authenticationExists(USER_UN))
        {
            authenticationService.createAuthentication(USER_UN, USER_PW.toCharArray());
        }
        authenticationService.authenticate(USER_UN, USER_PW.toCharArray());
        
        List<String> siteFeedEntries = this.activityService.getSiteFeedEntries("unknown site", "some format");
        
        assertNotNull(siteFeedEntries);
        assertTrue(siteFeedEntries.isEmpty());
    }
    
    public void testGetEmptyUserFeed() throws Exception
    {
        List<String> userFeedEntries = this.activityService.getUserFeedEntries("unknown user", "some format", null);
        
        assertNotNull(userFeedEntries);
        assertTrue(userFeedEntries.isEmpty());
        
        userFeedEntries = this.activityService.getUserFeedEntries("unknown user", "some format", "some site");
        
        assertNotNull(userFeedEntries);
        assertTrue(userFeedEntries.isEmpty());
        
        userFeedEntries = this.activityService.getUserFeedEntries("unknown user", "some format", "some site", true, false);
        
        assertNotNull(userFeedEntries);
        assertTrue(userFeedEntries.isEmpty());
        
        userFeedEntries = this.activityService.getUserFeedEntries("unknown user", "some format", "some site", false, true);
        
        assertNotNull(userFeedEntries);
        assertTrue(userFeedEntries.isEmpty());
        
        userFeedEntries = this.activityService.getUserFeedEntries("unknown user", "some format", "some site", true, true);
        
        assertNotNull(userFeedEntries);
        assertTrue(userFeedEntries.isEmpty());
    }
    
    public void testJSAPI() throws Exception
    {
        ScriptLocation location = new ClasspathScriptLocation("org/alfresco/repo/activities/script/test_activityService.js");
        String result = (String)this.scriptService.executeScript(location, new HashMap<String, Object>(0));
        
        // Check the result and fail if message returned
        if (result != null && result.length() != 0)
        {
            fail("The activity service test JS script failed: " + result);
        }
    }
    
    public void testFeedControls() throws Exception
    {
        List<FeedControl> feedControls = this.activityService.getFeedControls(USER_UN);
        assertNotNull(feedControls);
        assertTrue(feedControls.isEmpty());

        authenticationService.clearCurrentSecurityContext();
        
        if(! authenticationService.authenticationExists(USER_UN))
        {
            authenticationService.createAuthentication(USER_UN, USER_PW.toCharArray());
        }
        authenticationService.authenticate(USER_UN, USER_PW.toCharArray());
        
        feedControls = this.activityService.getFeedControls();
        assertNotNull(feedControls);
        assertTrue(feedControls.isEmpty());
        
        assertFalse(this.activityService.existsFeedControl(new FeedControl("mySite1", "appTool1")));
        
        this.activityService.setFeedControl(new FeedControl("mySite1", null));
        this.activityService.setFeedControl(new FeedControl("mySite1", "appTool1"));
        this.activityService.setFeedControl(new FeedControl(null, "appTool2"));
        
        feedControls = this.activityService.getFeedControls();
        assertEquals(3, feedControls.size());
        
        feedControls = this.activityService.getFeedControls(USER_UN);
        assertEquals(3, feedControls.size());
        
        assertTrue(this.activityService.existsFeedControl(new FeedControl("mySite1", "appTool1")));
        
        this.activityService.unsetFeedControl(new FeedControl("mySite1", "appTool1"));
        
        assertFalse(this.activityService.existsFeedControl(new FeedControl("mySite1", "appTool1")));
        
        feedControls = this.activityService.getFeedControls();
        assertEquals(2, feedControls.size());
        
        this.activityService.unsetFeedControl(new FeedControl("mySite1", null));
        this.activityService.unsetFeedControl(new FeedControl(null, "appTool2"));
        
        feedControls = this.activityService.getFeedControls();
        assertEquals(0, feedControls.size());
    }
}
