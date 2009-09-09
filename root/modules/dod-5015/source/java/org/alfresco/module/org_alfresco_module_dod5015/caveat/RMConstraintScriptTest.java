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
package org.alfresco.module.org_alfresco_module_dod5015.caveat;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.util.PropertyMap;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.scripts.TestWebScriptServer.DeleteRequest;
import org.alfresco.web.scripts.TestWebScriptServer.GetRequest;
import org.alfresco.web.scripts.TestWebScriptServer.PostRequest;
import org.alfresco.web.scripts.TestWebScriptServer.PutRequest;
import org.alfresco.web.scripts.TestWebScriptServer.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Test of GET RM Constraint  (User facing scripts)
 *
 * @author Mark Rogers
 */
public class RMConstraintScriptTest extends BaseWebScriptTest
{
    private AuthenticationService authenticationService;
    private RMCaveatConfigService caveatConfigService;
    private PersonService personService;
    
    protected final static String RM_LIST = "rmc:smList";
    
    private static final String URL_RM_CONSTRAINTS = "/api/rma/rmconstraints";
  
    @Override
    protected void setUp() throws Exception
    {
        this.caveatConfigService = (RMCaveatConfigService)getServer().getApplicationContext().getBean("CaveatConfigService");
        this.authenticationService = (AuthenticationService)getServer().getApplicationContext().getBean("AuthenticationService");
        this.personService = (PersonService)getServer().getApplicationContext().getBean("PersonService");
        super.setUp();
    }
        
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    /**
     * 
     * @throws Exception
     */
    public void testGetRMConstraint() throws Exception
    {
        // Set the current security context as admin
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());

        /**
         * Delete the list to remove any junk then recreate it.
         */
        caveatConfigService.deleteRMConstraint(RM_LIST);
        caveatConfigService.addRMConstraint(RM_LIST, "my title", new String[0]);
        
        
        createUser("fbloggs");
        createUser("jrogers");
        createUser("jdoe");
      
        
        List<String> values = new ArrayList<String>();
        values.add("NOFORN");
        values.add("FGI");
        caveatConfigService.updateRMConstraintListAuthority(RM_LIST, "fbloggs", values);
        caveatConfigService.updateRMConstraintListAuthority(RM_LIST, "jrogers", values);
        caveatConfigService.updateRMConstraintListAuthority(RM_LIST, "jdoe", values);
        
        AuthenticationUtil.setFullyAuthenticatedUser("jdoe");
        /**
         * Positive test Get the constraint 
         */
        {
            String url = URL_RM_CONSTRAINTS + "/" + "rmc_smList";
            Response response = sendRequest(new GetRequest(url), Status.STATUS_OK);
            JSONObject top = new JSONObject(response.getContentAsString());
            
            JSONObject data = top.getJSONObject("data");
            System.out.println(response.getContentAsString());
            
            JSONArray allowedValues = data.getJSONArray("allowedValuesForCurrentUser");
            
//            assertTrue("values not correct", compare(array, allowedValues));
            
//            JSONArray constraintDetails = data.getJSONArray("constraintDetails");
//           
//            assertTrue("details array does not contain 3 elements", constraintDetails.length() == 3);
//            for(int i =0; i < constraintDetails.length(); i++)
//            {
//                JSONObject detail = constraintDetails.getJSONObject(i);
//            }
        }
        
        /**
         * 
         * @throws Exception
         */
         
//        /**
//         * Negative test - Attempt to get a constraint that does exist
//         */
//        {
//            String url = URL_RM_CONSTRAINTS + "/" + "rmc_wibble";
//            sendRequest(new GetRequest(url), Status.STATUS_NOT_FOUND); 
//        }
//      
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        personService.deletePerson("fbloggs");
        personService.deletePerson("jrogers");
        personService.deletePerson("jdoe");
                
    }
    
    private void createUser(String userName)
    {
        if (this.authenticationService.authenticationExists(userName) == false)
        {
            this.authenticationService.createAuthentication(userName, "PWD".toCharArray());
            
            PropertyMap ppOne = new PropertyMap(4);
            ppOne.put(ContentModel.PROP_USERNAME, userName);
            ppOne.put(ContentModel.PROP_AUTHORITY_DISPLAY_NAME, "title" + userName);
            ppOne.put(ContentModel.PROP_FIRSTNAME, "firstName");
            ppOne.put(ContentModel.PROP_LASTNAME, "lastName");
            ppOne.put(ContentModel.PROP_EMAIL, "email@email.com");
            ppOne.put(ContentModel.PROP_JOBTITLE, "jobTitle");
            
            this.personService.createPerson(ppOne);
        }        
    }
}
    
     
 