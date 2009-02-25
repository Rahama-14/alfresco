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
package org.alfresco.repo.web.scripts.forms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.alfresco.web.scripts.TestWebScriptServer.Response;
import org.alfresco.web.scripts.json.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class FormRestApiGet_Test extends AbstractTestFormRestApi {

    public void testResponseContentType() throws Exception
    {
        Response rsp = sendGetReq(testNodeUrl, 200);
        assertEquals("application/json;charset=UTF-8", rsp.getContentType());
    }

    public void testGetFormForNonExistentNode() throws Exception
    {
    	// Replace all digits with an 'x' char - this should make for a non-existent node.
        Response rsp = sendGetReq(testNodeUrl.replaceAll("\\d", "x"), 404);
        assertEquals("application/json;charset=UTF-8", rsp.getContentType());
    }

    public void testJsonContentParsesCorrectly() throws Exception
    {
        Response rsp = sendGetReq(testNodeUrl, 200);
        String jsonResponseString = rsp.getContentAsString();
        
        Object jsonObject = new JSONUtils().toObject(jsonResponseString);
        assertNotNull("JSON object was null.", jsonObject);
    }

    public void testJsonUpperStructure() throws Exception
    {
        Response rsp = sendGetReq(testNodeUrl, 200);
        String jsonResponseString = rsp.getContentAsString();
        
        JSONObject jsonParsedObject = new JSONObject(new JSONTokener(jsonResponseString));
        assertNotNull(jsonParsedObject);
        
        Object dataObj = jsonParsedObject.get("data");
        assertEquals(JSONObject.class, dataObj.getClass());
        JSONObject rootDataObject = (JSONObject)dataObj;

        assertEquals(5, rootDataObject.length());
        String item = (String)rootDataObject.get("item");
        String submissionUrl = (String)rootDataObject.get("submissionUrl");
        String type = (String)rootDataObject.get("type");
        JSONObject definitionObject = (JSONObject)rootDataObject.get("definition");
        JSONObject formDataObject = (JSONObject)rootDataObject.get("formData");
        
        assertNotNull(item);
        assertNotNull(submissionUrl);
        assertNotNull(type);
        assertNotNull(definitionObject);
        assertNotNull(formDataObject);
    }

    @SuppressWarnings("unchecked")
    public void testJsonFormData() throws Exception
    {
        Response rsp = sendGetReq(testNodeUrl, 200);
        String jsonResponseString = rsp.getContentAsString();
        // At this point the formData names have underscores
        
        JSONObject jsonParsedObject = new JSONObject(new JSONTokener(jsonResponseString));
        assertNotNull(jsonParsedObject);
        
        JSONObject rootDataObject = (JSONObject)jsonParsedObject.get("data");
        
        JSONObject formDataObject = (JSONObject)rootDataObject.get("formData");
        List<String> keys = new ArrayList<String>();
        for (Iterator iter = formDataObject.keys(); iter.hasNext(); )
        {
        	// None of the formData keys should have a colon in them. We are
        	// replacing colons in field names with underscores.
            String nextFieldName = (String)iter.next();
            assertEquals("Did not expect to find a colon char in " + nextFieldName,
            		-1, nextFieldName.indexOf(':'));
			keys.add(nextFieldName);
        }
        // Threshold is a rather arbitrary number. I simply want to ensure that there
        // are *some* entries in the formData hash.
        final int threshold = 5;
        int actualKeyCount = keys.size();
        assertTrue("Expected more than " + threshold +
                " entries in formData. Actual: " + actualKeyCount, actualKeyCount > threshold);
    }
    
    @SuppressWarnings("unchecked")
	public void testJsonDefinitionFields() throws Exception
    {
        Response rsp = sendGetReq(testNodeUrl, 200);
        String jsonResponseString = rsp.getContentAsString();
        
        JSONObject jsonParsedObject = new JSONObject(new JSONTokener(jsonResponseString));
        assertNotNull(jsonParsedObject);
        
        JSONObject rootDataObject = (JSONObject)jsonParsedObject.get("data");
        
        JSONObject definitionObject = (JSONObject)rootDataObject.get("definition");
        
        JSONArray fieldsArray = (JSONArray)definitionObject.get("fields");
        
        for (int i = 0; i < fieldsArray.length(); i++)
        {
            Object nextObj = fieldsArray.get(i);
            
            JSONObject nextJsonObject = (JSONObject)nextObj;
            List<String> fieldKeys = new ArrayList<String>();
            for (Iterator iter2 = nextJsonObject.keys(); iter2.hasNext(); )
            {
                fieldKeys.add((String)iter2.next());
            }
            for (String s : fieldKeys)
            {
                if (s.equals("mandatory") || s.equals("protectedField"))
                {
                    assertEquals("JSON booleans should be actual booleans.", java.lang.Boolean.class, nextJsonObject.get(s).getClass());
                }
            }
        }
    }
}
