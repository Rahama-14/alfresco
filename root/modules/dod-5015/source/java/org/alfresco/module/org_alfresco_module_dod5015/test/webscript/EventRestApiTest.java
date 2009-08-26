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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.org_alfresco_module_dod5015.test.webscript;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.DOD5015Model;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.module.org_alfresco_module_dod5015.capability.Capability;
import org.alfresco.module.org_alfresco_module_dod5015.event.RecordsManagementEventService;
import org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.GUID;
import org.alfresco.web.scripts.TestWebScriptServer.DeleteRequest;
import org.alfresco.web.scripts.TestWebScriptServer.GetRequest;
import org.alfresco.web.scripts.TestWebScriptServer.PostRequest;
import org.alfresco.web.scripts.TestWebScriptServer.PutRequest;
import org.alfresco.web.scripts.TestWebScriptServer.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * RM event REST API test
 * 
 * @author Roy Wetherall
 */
public class EventRestApiTest extends BaseWebScriptTest implements RecordsManagementModel
{
    protected static StoreRef SPACES_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    protected static final String GET_EVENTS_URL = "/api/rma/admin/rmevents";
    protected static final String SERVICE_URL_PREFIX = "/alfresco/service";
    protected static final String APPLICATION_JSON = "application/json";
    protected static final String DISPLAY_LABEL = "display label";
    protected static final String EVENT_TYPE = "rmEventType.simple";
    protected static final String KEY_EVENT_NAME = "eventName";
    protected static final String KEY_EVENT_TYPE = "eventType";
    protected static final String KEY_EVENT_DISPLAY_LABEL = "eventDisplayLabel";
    
    protected NodeService nodeService;
    protected RecordsManagementService rmService;
    protected RecordsManagementEventService rmEventService;
    
    private NodeRef rmRootNode;
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.nodeService = (NodeService) getServer().getApplicationContext().getBean("NodeService");
        this.rmService = (RecordsManagementService)getServer().getApplicationContext().getBean("RecordsManagementService");
        this.rmEventService = (RecordsManagementEventService)getServer().getApplicationContext().getBean("RecordsManagementEventService");
        
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getSystemUserName());        
    }    

    public void testGetEvents() throws Exception
    {
        String event1 = GUID.generate();
        String event2 = GUID.generate();
        
        // Create a couple or events by hand
        rmEventService.addEvent(EVENT_TYPE, event1, DISPLAY_LABEL);
        rmEventService.addEvent(EVENT_TYPE, event2, DISPLAY_LABEL);
        
        try
        {
            // Get the events
            Response rsp = sendRequest(new GetRequest(GET_EVENTS_URL),200);
            String rspContent = rsp.getContentAsString();
            
            JSONObject obj = new JSONObject(rspContent);
            JSONObject roles = obj.getJSONObject("data");
            assertNotNull(roles);
            
            JSONObject eventObj = roles.getJSONObject(event1);
            assertNotNull(eventObj);
            assertEquals(event1, eventObj.get(KEY_EVENT_NAME));
            assertEquals(DISPLAY_LABEL, eventObj.get(KEY_EVENT_DISPLAY_LABEL));
            assertEquals(EVENT_TYPE, eventObj.get(KEY_EVENT_TYPE));
            
            eventObj = roles.getJSONObject(event2);
            assertNotNull(eventObj);
            assertEquals(event2, eventObj.get(KEY_EVENT_NAME));
            assertEquals(DISPLAY_LABEL, eventObj.get(KEY_EVENT_DISPLAY_LABEL));
            assertEquals(EVENT_TYPE, eventObj.get(KEY_EVENT_TYPE));                    
        }
        finally
        {
            // Clean up 
            rmEventService.removeEvent(event1);
            rmEventService.removeEvent(event2);
        }
        
    }
    
    public void testPostEvents() throws Exception
    {        
        String eventName= GUID.generate();
        
        JSONObject obj = new JSONObject();
        obj.put(KEY_EVENT_NAME, eventName);
        obj.put(KEY_EVENT_DISPLAY_LABEL, DISPLAY_LABEL);
        obj.put(KEY_EVENT_TYPE, EVENT_TYPE);
        
        Response rsp = sendRequest(new PostRequest(GET_EVENTS_URL, obj.toString(), APPLICATION_JSON),200);
        try
        {
            String rspContent = rsp.getContentAsString();
            
            JSONObject resultObj = new JSONObject(rspContent);
            JSONObject eventObj = resultObj.getJSONObject("data");
            assertNotNull(eventObj);
            
            assertEquals(eventName, eventObj.get(KEY_EVENT_NAME));
            assertEquals(DISPLAY_LABEL, eventObj.get(KEY_EVENT_DISPLAY_LABEL));
            assertEquals(EVENT_TYPE, eventObj.get(KEY_EVENT_TYPE));
           
        }
        finally
        {
            rmEventService.removeEvent(eventName);
        }        
    }
    
    public void testPutRole() throws Exception
    {
        String eventName = GUID.generate();        
        rmEventService.addEvent(EVENT_TYPE, eventName, DISPLAY_LABEL);
        
        try
        {
            JSONObject obj = new JSONObject();
            obj.put(KEY_EVENT_NAME, eventName);
            obj.put(KEY_EVENT_DISPLAY_LABEL, "changed");
            obj.put(KEY_EVENT_TYPE, EVENT_TYPE);
            
            // Get the roles
            Response rsp = sendRequest(new PutRequest(GET_EVENTS_URL + "/" + eventName, obj.toString(), APPLICATION_JSON),200);
            String rspContent = rsp.getContentAsString();
            
            JSONObject result = new JSONObject(rspContent);
            JSONObject eventObj = result.getJSONObject("data");
            assertNotNull(eventObj);
            
            assertEquals(eventName, eventObj.get(KEY_EVENT_NAME));
            assertEquals("changed", eventObj.get(KEY_EVENT_DISPLAY_LABEL));
            assertEquals(EVENT_TYPE, eventObj.get(KEY_EVENT_TYPE));     
            
            // Bad requests
            sendRequest(new PutRequest(GET_EVENTS_URL + "/cheese", obj.toString(), APPLICATION_JSON), 404);   
        }
        finally
        {
            // Clean up 
            rmEventService.removeEvent(eventName);
        }
        
    }
    
    public void testGetRole() throws Exception
    {
        String eventName = GUID.generate();        
        rmEventService.addEvent(EVENT_TYPE, eventName, DISPLAY_LABEL);
        
        try
        {
            // Get the roles
            Response rsp = sendRequest(new GetRequest(GET_EVENTS_URL + "/" + eventName),200);
            String rspContent = rsp.getContentAsString();
            
            JSONObject obj = new JSONObject(rspContent);
            JSONObject eventObj = obj.getJSONObject("data");
            assertNotNull(eventObj);
            
            assertEquals(eventName, eventObj.get(KEY_EVENT_NAME));
            assertEquals(DISPLAY_LABEL, eventObj.get(KEY_EVENT_DISPLAY_LABEL));
            assertEquals(EVENT_TYPE, eventObj.get(KEY_EVENT_TYPE));      
            
            // Bad requests
            sendRequest(new GetRequest(GET_EVENTS_URL + "/cheese"), 404);
        }
        finally
        {
            // Clean up 
            rmEventService.removeEvent(eventName);
        }
        
    }
    
    public void testDeleteRole() throws Exception
    {
        String eventName = GUID.generate();
        assertFalse(rmEventService.existsEvent(eventName));        
        rmEventService.addEvent(EVENT_TYPE, eventName, DISPLAY_LABEL);       
        assertTrue(rmEventService.existsEvent(eventName));           
        sendRequest(new DeleteRequest(GET_EVENTS_URL + "/" + eventName),200);        
        assertFalse(rmEventService.existsEvent(eventName));    
        
        // Bad request
        sendRequest(new DeleteRequest(GET_EVENTS_URL + "/cheese"), 404);  
    }
    
}
