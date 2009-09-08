package org.alfresco.module.org_alfresco_module_dod5015.email;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.scripts.TestWebScriptServer.GetRequest;
import org.alfresco.web.scripts.TestWebScriptServer.PostRequest;
import org.alfresco.web.scripts.TestWebScriptServer.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class EmailMapScriptTest extends BaseWebScriptTest
{

    public final static String URL_RM_EMAILMAP = "/api/rma/admin/emailmap";
    
    AuthenticationService authenticationService;
    
    @Override
    protected void setUp() throws Exception
    {
        this.authenticationService = (AuthenticationService)getServer().getApplicationContext().getBean("AuthenticationService");
//        setCurrentUser(AuthenticationUtil.getAdminUserName());
        super.setUp();
        
        // Set the current security context as admin
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
 
    }
    
    public void testGetEmailMap() throws Exception
    {
        {
            Response response = sendRequest(new GetRequest(URL_RM_EMAILMAP), Status.STATUS_OK);    
    
            JSONObject top = new JSONObject(response.getContentAsString());
            System.out.println(response.getContentAsString());
            //JSONArray data = top.getJSONArray("data");
        }
    } 
    
    public void testUpdateEmailMap() throws Exception
    {
        /**
         * Update the list by adding two values  
         */
        {
           JSONObject obj = new JSONObject();
           
           JSONArray add = new JSONArray();  
           JSONObject val = new JSONObject();
           val.put("from", "whatever");
           val.put("to", "rmc:Wibble");
           add.put(val);
           JSONObject val2 = new JSONObject();
           val2.put("from", "whatever");
           val2.put("to", "rmc:wobble");
           add.put(val2);
 
           obj.put("add", add);
           
           System.out.println(obj.toString());
           
           /**
             * Now do a post to add a couple of values
             */
           Response response = sendRequest(new PostRequest(URL_RM_EMAILMAP, obj.toString(), "application/json"), Status.STATUS_OK); 
           System.out.println(response.getContentAsString());
           // Check the response
           
           
           JSONArray delete = new JSONArray(); 
           delete.put(val2);
           
        }
        
        /**
         * Update the list by deleting a value
         * 
         * "whatever" has two mappings, delete one of them
         */
        {
            JSONObject obj = new JSONObject();
            JSONObject val2 = new JSONObject();
            JSONArray delete = new JSONArray();  
            val2.put("from", "whatever");
            val2.put("to", "rmc:wobble");
            delete.put(val2);
            obj.put("delete", delete);
            
            /**
             * Now do a post to delete a couple of values
             */
           Response response = sendRequest(new PostRequest(URL_RM_EMAILMAP, obj.toString(), "application/json"), Status.STATUS_OK); 
           System.out.println(response.getContentAsString());

           JSONObject top = new JSONObject(response.getContentAsString());
           JSONObject data = top.getJSONObject("data");
           JSONArray mappings = data.getJSONArray("mappings");
           
           boolean wibbleFound = false; 
           for(int i = 0; i < mappings.length(); i++)
           {
               JSONObject mapping = mappings.getJSONObject(i);
               
        
               if(mapping.get("from").equals("whatever"))
               {
                   if(mapping.get("to").equals("rmc:Wibble"))
                   {
                       wibbleFound = true;
                   }
                   else
                   {
                       fail("custom mapping for field not deleted");
                   }
               }
           }
           assertTrue(wibbleFound);
        }
    }
}

