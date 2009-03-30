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
package org.alfresco.repo.web.scripts.forms;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.scripts.TestWebScriptServer.GetRequest;
import org.alfresco.web.scripts.TestWebScriptServer.PostRequest;
import org.alfresco.web.scripts.TestWebScriptServer.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class FormRestApiJsonPost_Test extends AbstractTestFormRestApi
{
    private static final String PROP_CM_DESCRIPTION = "prop_cm_description";
    private static final String PROP_MIMETYPE = "prop_mimetype";
    private static final String APPLICATION_JSON = "application/json";
    private static final String ASSOC_CM_REFERENCES = "assoc_cm_references";
    private static final String ASSOC_CM_REFERENCES_ADDED = "assoc_cm_references_added";
    private static final String ASSOC_CM_REFERENCES_REMOVED = "assoc_cm_references_removed";

    public void testSimpleJsonPostRequest() throws IOException, JSONException
    {
        // Retrieve and store the original property value.
        Serializable originalDescription =
            nodeService.getProperty(referencingDocNodeRef, ContentModel.PROP_DESCRIPTION);
        assertEquals(TEST_FORM_DESCRIPTION, originalDescription);
        
        // get the original mimetype
        String originalMimetype = null;
        ContentData content = (ContentData)this.nodeService.getProperty(referencingDocNodeRef, ContentModel.PROP_CONTENT);
        if (content != null)
        {
            originalMimetype = content.getMimetype();
        }
        
        // Construct some JSON to represent a new value.
        JSONObject jsonPostData = new JSONObject();
        final String proposedNewDescription = "Modified Description";
        jsonPostData.put(PROP_CM_DESCRIPTION, proposedNewDescription);
        jsonPostData.put(PROP_MIMETYPE, MimetypeMap.MIMETYPE_HTML);
        
        // Submit the JSON request.
        String jsonPostString = jsonPostData.toString();
        Response ignoredRsp = sendRequest(new PostRequest(referencingNodeUrl, jsonPostString,
                APPLICATION_JSON), 200);

        // The nodeService should give us the modified property.
        Serializable modifiedDescription =
            nodeService.getProperty(referencingDocNodeRef, ContentModel.PROP_DESCRIPTION);
        assertEquals(proposedNewDescription, modifiedDescription);
        
        // get the original mimetype
        String modifiedMimetype = null;
        content = (ContentData)this.nodeService.getProperty(referencingDocNodeRef, ContentModel.PROP_CONTENT);
        if (content != null)
        {
            modifiedMimetype = content.getMimetype();
        }
        assertEquals(MimetypeMap.MIMETYPE_HTML, modifiedMimetype);

        // The Rest API should also give us the modified property.
        Response response = sendRequest(new GetRequest(referencingNodeUrl), 200);
        JSONObject jsonGetResponse = new JSONObject(response.getContentAsString());
        JSONObject jsonDataObj = (JSONObject)jsonGetResponse.get("data");
        assertNotNull(jsonDataObj);

        JSONObject formData = (JSONObject)jsonDataObj.get("formData");
        assertNotNull(formData);
        String retrievedValue = (String)formData.get(PROP_CM_DESCRIPTION);
        assertEquals(modifiedDescription, retrievedValue);
        String retrievedMimetype = (String)formData.get(PROP_MIMETYPE);
        assertEquals(MimetypeMap.MIMETYPE_HTML, modifiedMimetype);
    }
    
    /**
     * This test method attempts to add new associations between existing nodes.
     */
    public void testAddNewAssociationsToNode() throws Exception
    {
        List<NodeRef> associatedNodes;
        checkOriginalAssocsBeforeChanges();
        
        // Add three additional associations
        JSONObject jsonPostData = new JSONObject();
        String assocsToAdd = associatedDoc_C + "," + associatedDoc_D + "," + associatedDoc_E;
        jsonPostData.put(ASSOC_CM_REFERENCES_ADDED, assocsToAdd);
        String jsonPostString = jsonPostData.toString();

        sendRequest(new PostRequest(referencingNodeUrl, jsonPostString, APPLICATION_JSON), 200);

        // Check the now updated associations via the node service
        List<AssociationRef> modifiedAssocs = nodeService.getTargetAssocs(referencingDocNodeRef, RegexQNamePattern.MATCH_ALL);
        assertEquals(5, modifiedAssocs.size());

        // Extract the target nodeRefs to make them easier to examine
        associatedNodes = new ArrayList<NodeRef>(5);
        for (AssociationRef assocRef : modifiedAssocs)
        {
            associatedNodes.add(assocRef.getTargetRef());
        }

        assertTrue(associatedNodes.contains(associatedDoc_A));
        assertTrue(associatedNodes.contains(associatedDoc_B));
        assertTrue(associatedNodes.contains(associatedDoc_C));
        assertTrue(associatedNodes.contains(associatedDoc_D));
        assertTrue(associatedNodes.contains(associatedDoc_E));
        
        // The Rest API should also give us the modified assocs.
        Response response = sendRequest(new GetRequest(referencingNodeUrl), 200);
        String jsonRspString = response.getContentAsString();
        JSONObject jsonGetResponse = new JSONObject(jsonRspString);
        JSONObject jsonData = (JSONObject)jsonGetResponse.get("data");
        assertNotNull(jsonData);

        JSONObject jsonFormData = (JSONObject)jsonData.get("formData");
        assertNotNull(jsonFormData);
        
        String jsonAssocs = (String)jsonFormData.get(ASSOC_CM_REFERENCES);
        
        // We expect exactly 5 assocs on the test node
        assertEquals(5, jsonAssocs.split(",").length);
        for (AssociationRef assocRef : modifiedAssocs)
        {
            assertTrue(jsonAssocs.contains(assocRef.getTargetRef().toString()));
        }
    }

    /**
     * This test method attempts to remove an existing association between two existing
     * nodes.
     */
    public void testRemoveAssociationsFromNode() throws Exception
    {
        List<NodeRef> associatedNodes;
        checkOriginalAssocsBeforeChanges();

        // Remove an association
        JSONObject jsonPostData = new JSONObject();
        String assocsToRemove = associatedDoc_B.toString();
        jsonPostData.put(ASSOC_CM_REFERENCES_REMOVED, assocsToRemove);
        String jsonPostString = jsonPostData.toString();

        sendRequest(new PostRequest(referencingNodeUrl, jsonPostString, APPLICATION_JSON), 200);

        // Check the now updated associations via the node service
        List<AssociationRef> modifiedAssocs = nodeService.getTargetAssocs(referencingDocNodeRef, RegexQNamePattern.MATCH_ALL);
        assertEquals(1, modifiedAssocs.size());

        // Extract the target nodeRefs to make them easier to examine
        associatedNodes = new ArrayList<NodeRef>(5);
        for (AssociationRef assocRef : modifiedAssocs)
        {
            associatedNodes.add(assocRef.getTargetRef());
        }

        assertTrue(associatedNodes.contains(associatedDoc_A));
        
        // The Rest API should also give us the modified assocs.
        Response response = sendRequest(new GetRequest(referencingNodeUrl), 200);
        String jsonRspString = response.getContentAsString();
        JSONObject jsonGetResponse = new JSONObject(jsonRspString);
        JSONObject jsonData = (JSONObject)jsonGetResponse.get("data");
        assertNotNull(jsonData);

        JSONObject jsonFormData = (JSONObject)jsonData.get("formData");
        assertNotNull(jsonFormData);
        
        String jsonAssocs = (String)jsonFormData.get(ASSOC_CM_REFERENCES);
        
        // We expect exactly 1 assoc on the test node
        assertEquals(1, jsonAssocs.split(",").length);
        for (AssociationRef assocRef : modifiedAssocs)
        {
            assertTrue(jsonAssocs.contains(assocRef.getTargetRef().toString()));
        }
    }

    /**
     * This test method attempts to add the same association twice. This attempt should
     * not succeed.
     */
    public void off_testAddAssocThatAlreadyExists() throws Exception
    {
        //TODO This test has been switched off as the current implementation of assoc
        //     persistence does not handle this -ve test case. Currently a 500 error
        //     is thrown from within the repo.
        checkOriginalAssocsBeforeChanges();

        // Add an association
        JSONObject jsonPostData = new JSONObject();
        String assocsToAdd = associatedDoc_C.toString();
        jsonPostData.put(ASSOC_CM_REFERENCES_ADDED, assocsToAdd);
        String jsonPostString = jsonPostData.toString();

        sendRequest(new PostRequest(referencingNodeUrl, jsonPostString, APPLICATION_JSON), 200);

        // Try to add the same association again
        sendRequest(new PostRequest(referencingNodeUrl, jsonPostString, APPLICATION_JSON), 200);
        
        fail("This TC not finished.");
    }
    
    /**
     * This test method attempts to remove an association that does not exist. This
     * attempt should not succeed.
     */
    public void off_testRemoveAssocThatDoesNotExist() throws Exception
    {
        //TODO This test has been switched off as the current implementation of assoc
        //     persistence does not handle this -ve test case. Currently a 500 error
        //     is thrown from within the repo.
        checkOriginalAssocsBeforeChanges();

        // Remove a non-existent association
        JSONObject jsonPostData = new JSONObject();
        String assocsToRemove = associatedDoc_E.toString();
        jsonPostData.put(ASSOC_CM_REFERENCES_REMOVED, assocsToRemove);
        String jsonPostString = jsonPostData.toString();

        sendRequest(new PostRequest(referencingNodeUrl, jsonPostString, APPLICATION_JSON), 200);
        
        fail("This TC not finished.");
    }

    private void checkOriginalAssocsBeforeChanges()
    {
        List<AssociationRef> originalAssocs = nodeService.getTargetAssocs(referencingDocNodeRef, RegexQNamePattern.MATCH_ALL);
        assertEquals(2, originalAssocs.size());

        List<NodeRef> associatedNodes = new ArrayList<NodeRef>(2);
        associatedNodes.add(originalAssocs.get(0).getTargetRef());
        associatedNodes.add(originalAssocs.get(1).getTargetRef());
        
        assertTrue(associatedNodes.contains(associatedDoc_A));
        assertTrue(associatedNodes.contains(associatedDoc_B));
    }
}
