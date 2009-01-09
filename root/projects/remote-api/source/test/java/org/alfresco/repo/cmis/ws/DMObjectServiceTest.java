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
package org.alfresco.repo.cmis.ws;

import java.math.BigInteger;
import java.util.List;

import javax.activation.DataHandler;
import javax.xml.ws.Holder;

import org.alfresco.cmis.dictionary.CMISMapping;
import org.alfresco.repo.content.MimetypeMap;

/**
 * @author Alexander Tsvetkov
 * 
 */

public class DMObjectServiceTest extends AbstractServiceTest
{
    GetPropertiesResponse propertiesResponse;

    public DMObjectServiceTest()
    {
        super();
    }

    public DMObjectServiceTest(String testCase, String username, String password)
    {
        super(testCase, username, password);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        createInitialContent();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        deleteInitialContent();
    }

    protected Object getServicePort()
    {
        return helper.objectServicePort;
    }

    public void testCreateDocument() throws Exception
    {

        documentName = "Test cmis document (" + System.currentTimeMillis() + ")";

        String content = "This is a test content";
        // Cmis Properties
        CmisPropertiesType properties = new CmisPropertiesType();
        List<CmisProperty> propertiesList = properties.getProperty();
        CmisPropertyString cmisProperty = new CmisPropertyString();
        cmisProperty.setName(CMISMapping.PROP_NAME);
        cmisProperty.setValue(documentName);
        propertiesList.add(cmisProperty);

        CmisContentStreamType cmisStream = new CmisContentStreamType();
        cmisStream.setFilename(documentName);
        cmisStream.setMimeType(MimetypeMap.MIMETYPE_TEXT_PLAIN);

        DataHandler dataHandler = new DataHandler(content, MimetypeMap.MIMETYPE_TEXT_PLAIN);
        cmisStream.setStream(dataHandler);

        // public String helper.createDocument(String repositoryId, String typeId, CmisPropertiesType properties, String folderId, CmisContentStreamType contentStream,
        // EnumVersioningState versioningState)

        String documentId;
        // MAJOR
        documentName = "Test cmis document (" + System.currentTimeMillis() + ")";
        documentId = helper.createDocument(documentName, folderId, CMISMapping.DOCUMENT_TYPE_ID, EnumVersioningState.MAJOR);
        propertiesResponse = helper.getObjectProperties(documentId);
        // assertTrue(getPropertyBooleanValue(propertiesResponse, CMISMapping.PROP_IS_MAJOR_VERSION));
        assertFalse(getPropertyBooleanValue(propertiesResponse, CMISMapping.PROP_IS_VERSION_SERIES_CHECKED_OUT));
        helper.deleteDocument(documentId);

        // MINOR
        documentName = "Test cmis document (" + System.currentTimeMillis() + ")";
        documentId = helper.createDocument(documentName, folderId, CMISMapping.DOCUMENT_TYPE_ID, EnumVersioningState.MINOR);
        propertiesResponse = helper.getObjectProperties(documentId);
        assertFalse(getPropertyBooleanValue(propertiesResponse, CMISMapping.PROP_IS_VERSION_SERIES_CHECKED_OUT));
        // assertTrue(getPropertyBooleanValue(propertiesResponse, CMISMapping.PROP_IS_MAJOR_VERSION));
        helper.deleteDocument(documentId);

    }

    public void testCreateDocument_Versioning() throws Exception
    {
        // CHECKEDOUT
        documentName = "Test cmis document (" + System.currentTimeMillis() + ")";
        String documentId = helper.createDocument(documentName, folderId, CMISMapping.DOCUMENT_TYPE_ID, EnumVersioningState.CHECKEDOUT);
        propertiesResponse = helper.getObjectProperties(documentId);
        assertNotNull(getPropertyValue(propertiesResponse, CMISMapping.PROP_VERSION_SERIES_ID));
        // Bug
        assertTrue(getPropertyBooleanValue(propertiesResponse, CMISMapping.PROP_IS_VERSION_SERIES_CHECKED_OUT));
        assertNotNull(getPropertyValue(propertiesResponse, CMISMapping.PROP_VERSION_SERIES_CHECKED_OUT_BY));
        assertTrue(getPropertyValue(propertiesResponse, CMISMapping.PROP_VERSION_SERIES_CHECKED_OUT_ID).equals(documentId));

        Holder<String> documentIdHolder = new Holder<String>(documentId);
        helper.checkIn(documentIdHolder, "checkin Comment", true);
        assertTrue(getPropertyValue(propertiesResponse, CMISMapping.PROP_VERSION_LABEL).equals("1.0"));

        // documentId = (String) PropertyUtil.getProperty(response.getObject().iterator().next().getProperties(), CMISMapping.PROP_OBJECT_ID);
        // deleteDocument(documentId);
    }

    public void testCreateDocument_Exceptions() throws Exception
    {
        // • If unfiling is not supported and a Folder is not specified, throw FolderNotValidException.
        try
        {
            documentName = "Test cmis document (" + System.currentTimeMillis() + ")";
            documentId = helper.createDocument(documentName, null, CMISMapping.DOCUMENT_TYPE_ID, EnumVersioningState.MAJOR);
            fail();
        }
        catch (FolderNotValidException e)
        {

        }
        catch (Exception e)
        { // Bug
            e.printStackTrace(); // org.alfresco.repo.cmis.ws.RuntimeException: Runtime error. Message: null
            fail(e.getMessage());
        }
    }

    public void testCreateFolder() throws Exception
    {
        String folderId1;
        folderName = "Test Cmis Folder (" + System.currentTimeMillis() + ")" + "testCreateFolder";
        folderId1 = helper.createFolder(folderName, folderId, CMISMapping.FOLDER_TYPE_ID);

        propertiesResponse = helper.getObjectProperties(folderId1);
        assertNotNull(getPropertyValue(propertiesResponse, CMISMapping.PROP_NAME));
        assertNotNull(getPropertyValue(propertiesResponse, CMISMapping.PROP_PARENT_ID));

        helper.deleteFolder(folderId1);
    }

    public void testGetDocumentProperties() throws Exception
    {
        String filter;
        filter = "*";
        propertiesResponse = helper.getObjectProperties(documentId, filter);
        assertNotNull(getPropertyValue(propertiesResponse, CMISMapping.PROP_NAME));
        assertNotNull(getPropertyValue(propertiesResponse, CMISMapping.PROP_CONTENT_STREAM_FILENAME));
        assertNotNull(getPropertyValue(propertiesResponse, CMISMapping.PROP_CONTENT_STREAM_MIME_TYPE));
        assertTrue(getPropertyBooleanValue(propertiesResponse, CMISMapping.PROP_IS_LATEST_VERSION));

        // A property filter is a string that contains either ‘*’ (to return all properties) or a comma-separated list of property names (to return selected properties). An
        // arbitrary number of spaces are allowed before or after each comma.

        // filter = "*Stream*";
        // propertiesResponse = helper.getObjectProperties(documentId, filter);
        // assertNotNull("filter test", getPropertyValue(propertiesResponse, CMISMapping.PROP_NAME));
        // assertNotNull("filter test", getPropertyValue(propertiesResponse, CMISMapping.PROP_CONTENT_STREAM_LENGTH));

    }

    public void testGetDocumentProperties_Versioning() throws Exception
    {
        GetPropertiesResponse response = helper.getObjectProperties(documentId);

        Holder<String> documentIdHolder = new Holder<String>(documentId);
        Holder<Boolean> contentCopied = new Holder<Boolean>();
        String checkinComment = "Test checkin" + System.currentTimeMillis();

        helper.checkOut(documentIdHolder, contentCopied);
        // new version of doc
        response = helper.getObjectProperties(documentIdHolder.value);
        assertNotNull(getPropertyValue(response, CMISMapping.PROP_NAME));
        assertNotNull(getPropertyValue(response, CMISMapping.PROP_CONTENT_STREAM_FILENAME));
        assertNotNull(getPropertyValue(response, CMISMapping.PROP_CONTENT_STREAM_MIME_TYPE));
        assertTrue(getPropertyBooleanValue(response, CMISMapping.PROP_IS_VERSION_SERIES_CHECKED_OUT));
        assertNotNull(getPropertyValue(response, CMISMapping.PROP_VERSION_SERIES_CHECKED_OUT_BY));

        helper.checkIn(documentIdHolder, checkinComment, true);

        response = helper.getObjectProperties(documentId);
        assertNotNull(getPropertyValue(response, CMISMapping.PROP_NAME));
        assertNotNull(getPropertyValue(response, CMISMapping.PROP_CONTENT_STREAM_FILENAME));
        assertNotNull(getPropertyValue(response, CMISMapping.PROP_CONTENT_STREAM_MIME_TYPE));
        assertTrue(getPropertyBooleanValue(response, CMISMapping.PROP_IS_LATEST_VERSION));
        assertTrue(getPropertyBooleanValue(response, CMISMapping.PROP_IS_LATEST_MAJOR_VERSION));
        assertTrue(getPropertyBooleanValue(response, CMISMapping.PROP_IS_MAJOR_VERSION));

        // Returns the list of all document versions for the specified version series, sorted by CREATION_DATE descending.
        GetAllVersionsResponse responseVersions = helper.getAllVersions(documentId);

        // Last version

        assertEquals(3, responseVersions.getObject().size());
        assertTrue("Initial version was not returned", isExistItemWithProperty(responseVersions.getObject(), CMISMapping.PROP_VERSION_LABEL, "1.0"));
        assertTrue("Invalid response ordering: First object is not latest version", (Boolean) PropertyUtil.getProperty(responseVersions.getObject().get(0).getProperties(), CMISMapping.PROP_IS_LATEST_VERSION));
        assertTrue("Invalid response ordering: Second object is not head version", (Boolean) PropertyUtil.getProperty(responseVersions.getObject().get(1).getProperties(), CMISMapping.PROP_IS_LATEST_VERSION));
    }

    // This test don't asserts until CMIS setProperty()/setProperties() logic is unimplemented
    public void testGetDocumentProperties_Other() throws Exception
    {
        // • If “includeAllowableActions” is TRUE, the repository will return the allowable actions for the current user for the object as part of the output.
        // • "IncludeRelationships" indicates whether relationships are also returned for the object. If it is set to "source" or "target", relationships for which the returned
        // object is a source, or respectively a target, will also be returned. If it is set to "both", relationships for which the returned object is either a source or a target
        // will be returned. If it is set to "none", relationships are not returned.

        GetPropertiesResponse response = helper.getObjectProperties(documentId);
        CmisObjectType object = response.getObject();
        // TODO: not implemented
        // assertNotNull(object.getAllowableActions());
        // assertNotNull(object.getRelationship());
    }

    public void testGetPropertiesForInvalidOID() throws Exception
    {
        GetProperties request = new GetProperties();
        request.setObjectId("invalid OID");
        request.setRepositoryId("invalid OID");
        try
        {
            ((ObjectServicePort) servicePort).getProperties(request);
        }
        catch (InvalidArgumentException e)
        {
            return;
        }

        fail("Expects exception");
    }

    public void testGetContentStream() throws Exception
    {
        GetContentStream contStream = new GetContentStream();
        contStream.setDocumentId(documentId);
        contStream.setRepositoryId(repositoryId);

        CmisContentStreamType result = ((ObjectServicePort) servicePort).getContentStream(repositoryId, documentId);
        if (result.getLength().intValue() == 0)
        {
            fail();
        }
        try
        {
            contStream.setDocumentId(documentId + "s");
            {
                result = ((ObjectServicePort) servicePort).getContentStream(repositoryId, documentId);
            }
        }
        catch (InvalidArgumentException e)
        {
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            fail();
        }

        // Content Stream of checked out doc
        Holder<String> documentIdHolder = new Holder<String>(documentId);
        Holder<Boolean> contentCopied = new Holder<Boolean>();
        String checkinComment = "Test checkin" + System.currentTimeMillis();

        helper.checkOut(documentIdHolder, contentCopied);

        result = ((ObjectServicePort) servicePort).getContentStream(repositoryId, documentIdHolder.value);
        if (result.getLength().intValue() == 0)
        {
            fail();
        }

        helper.checkIn(documentIdHolder, checkinComment, true);

    }

    // this method is not implemented yet
    public void testCreatePolicy() throws Exception
    {
        CreatePolicy request = cmisObjectFactory.createCreatePolicy();

        request.setRepositoryId(repositoryId);
        request.setFolderId(cmisObjectFactory.createCreatePolicyFolderId(companyHomeId));
        // there is no CMISMapping.POLICY_TYPE_ID
        request.setTypeId("policy");

        CmisPropertiesType properties = new CmisPropertiesType();
        List<CmisProperty> propertiesList = properties.getProperty();

        CmisPropertyString cmisProperty = new CmisPropertyString();
        cmisProperty.setName(CMISMapping.PROP_NAME);
        cmisProperty.setPropertyType(EnumPropertyType.STRING);
        cmisProperty.setIndex(BigInteger.valueOf(1));
        cmisProperty.setValue("Cmis Test Policy");
        propertiesList.add(cmisProperty);
        request.setProperties(properties);

        // TODO: not implemented
        // String createPolicy(String repositoryId, String typeId, CmisPropertiesType properties, String folderId)
        String response = ((ObjectServicePort) servicePort).createPolicy(repositoryId, request.getTypeId(), request.getProperties(), companyHomeId);

        // assertNotNull(response);
    }

    public void testCreateRelationship() throws Exception
    {
        // TODO: uncomment
        // String name = "Cmis Test Relationship";
        // String objectId = null;
        // try
        // {
        // objectId = helper.createRelationship(name, folderId, documentId);
        // }
        // catch (Exception e)
        // {
        // fail(e.getMessage());
        // }
        //
        // GetPropertiesResponse response = helper.getObjectProperties(objectId);
        // assertEquals(name, getPropertyValue(response, CMISMapping.PROP_NAME));
        //
        // helper.deleteFolder(folderId);
        //
        // response = helper.getObjectProperties(documentId);
        // assertNull(response);
    }

    public void testDeleteContentStream() throws Exception
    {
        // public void deleteContentStream(String repositoryId, String documentId)
        ((ObjectServicePort) servicePort).deleteContentStream(repositoryId, documentId);

        try
        {
            CmisContentStreamType result = ((ObjectServicePort) servicePort).getContentStream(repositoryId, documentId);
        }
        catch (StorageException e)
        {
        }
        catch (Exception e)
        {
            e.printStackTrace(); // org.alfresco.repo.cmis.ws.StorageException: The specified Document has no Content Stream
            fail(e.getMessage());
        }

        // on content update and on content delete new version should be created
        GetAllVersionsResponse responseVersions = helper.getAllVersions(documentId);
        assertTrue("new version of document should be created", responseVersions.getObject().size() > 1);

    }

    public void testDeleteObject() throws Exception
    {
        // public void deleteObject(String repositoryId, String objectId)
        ((ObjectServicePort) servicePort).deleteObject(repositoryId, documentId);
        assertNull(helper.getObjectProperties(documentId));
    }

    public void testDeleteObject_Exceptions() throws Exception
    {
        // • If the object is a Folder with at least one child, throw ConstraintViolationException.
        // • If the object is the Root Folder, throw OperationNotSupportedException.

        documentName = "Test cmis document (" + System.currentTimeMillis() + ")";
        String documentId = helper.createDocument(documentName, folderId);

        // Try to delere folder with child
        try
        {
            ((ObjectServicePort) servicePort).deleteObject(repositoryId, folderId);
            fail("Try to delere folder with child");
        }
        catch (ConstraintViolationException e)
        {
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getClass().getName() + ": " + e.getMessage());
        }

        // Try to delere root folder
        try
        {
            ((ObjectServicePort) servicePort).deleteObject(repositoryId, helper.getCompanyHomeId(repositoryId));
            fail("Try to delere root folder");
        }
        catch (OperationNotSupportedException e)
        {
        }
        catch (Exception e)
        {
            e.printStackTrace();// org.alfresco.repo.cmis.ws.ConstraintViolationException: Could not delete folder with at least one Child
            fail(e.getClass().getName() + ": " + e.getMessage());
        }

    }

    public void testDeleteTree() throws Exception
    {
        String folderName;
        String folderId1;
        String folderId2;
        String documentId2;

        folderName = "Test Cmis Folder (" + System.currentTimeMillis() + ")";
        folderId1 = helper.createFolder(folderName, folderId);

        folderName = "Test Cmis Folder (" + System.currentTimeMillis() + ")";
        folderId2 = helper.createFolder(folderName, folderId1);

        documentName = "Test cmis document (" + System.currentTimeMillis() + ")";
        documentId2 = helper.createDocument(documentName, folderId2);

        // public FailedToDelete deleteTree(String repositoryId, String folderId, EnumUnfileNonfolderObjects unfileNonfolderObjects, Boolean continueOnFailure)
        DeleteTreeResponse.FailedToDelete response = ((ObjectServicePort) servicePort).deleteTree(repositoryId, folderId1, EnumUnfileNonfolderObjects.DELETE, true);
        assertTrue("All objects should be deleted", response.getObjectId().size() == 0);

        assertNull("DELETE", helper.getObjectProperties(folderId1));
        assertNull("DELETE", helper.getObjectProperties(folderId2));
        assertNull("DELETE", helper.getObjectProperties(documentId2));

        // Check DELETESINGLEFILED
        folderName = "Test Cmis Folder (" + System.currentTimeMillis() + ")";
        folderId1 = helper.createFolder(folderName, folderId);

        folderName = "Test Cmis Folder (" + System.currentTimeMillis() + ")";
        folderId2 = helper.createFolder(folderName, folderId1);

        documentName = "Test cmis document (" + System.currentTimeMillis() + ")";
        documentId2 = helper.createDocument(documentName, folderId2);

        response = ((ObjectServicePort) servicePort).deleteTree(repositoryId, folderId1, EnumUnfileNonfolderObjects.DELETESINGLEFILED, true);
        // assertNotNull("DELETESINGLEFILED", response);
        assertTrue("All objects should not be deleted", response.getObjectId().size() != 0);
        assertNotNull("DELETESINGLEFILED", helper.getObjectProperties(folderId1));
        assertNotNull("DELETESINGLEFILED", helper.getObjectProperties(folderId2));
        assertNotNull("DELETESINGLEFILED", helper.getObjectProperties(documentId2));

        helper.deleteFolder(folderId1);

        /*        
        // on DELETESINGLEFILED deletes only relationships and folder. Primary parent folder and contend should not be deleted
        folderName = "Test Cmis Folder (" + System.currentTimeMillis() + ")";
        folderId1 = helper.createFolder(folderName, folderId);

        folderName = "Test Cmis Folder (" + System.currentTimeMillis() + ")";
        folderId2 = helper.createFolder(folderName, companyHomeId);

        documentName = "Test cmis document (" + System.currentTimeMillis() + ")";
        documentId2 = helper.createDocument(documentName, folderId2);

        String relationshipId = createRelationship("test relashionship", folderId1, documentId2);

        response = ((ObjectServicePort) servicePort).deleteTree(repositoryId, folderId1, EnumUnfileNonfolderObjects.DELETESINGLEFILED, true);
        assertNotNull("DELETESINGLEFILED", response);

        assertNull("DELETESINGLEFILED", helper.getObjectProperties(folderId1));
        assertNull("DELETESINGLEFILED", helper.getObjectProperties(relationshipId));

        assertNotNull("DELETESINGLEFILED", helper.getObjectProperties(folderId2));
        assertNotNull("DELETESINGLEFILED", helper.getObjectProperties(documentId2));

        deleteFolder(folderId2);

        // response = ((ObjectServicePort) servicePort).deleteTree(repositoryId, folderId, EnumUnfileNonfolderObjects.UNFILE, true);
        // assertNotNull("UNFILE", response);
        //
        // assertNull(getObjectProperties(folderId));

        // deleteFolder(folderId);
         * 
        */
    }

    public void testDeleteTree_Exceptions() throws Exception
    {
        // Try to delere root folder
        try
        {
            DeleteTreeResponse.FailedToDelete response = ((ObjectServicePort) servicePort).deleteTree(repositoryId, helper.getCompanyHomeId(repositoryId),
                    EnumUnfileNonfolderObjects.DELETE, true);
            fail("Try to delere root folder");
        }
        catch (OperationNotSupportedException e)
        {
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testGetAllowableActions() throws Exception
    {
        CmisAllowableActionsType response;
        // CmisAllowableActionsType getAllowableActions(String repositoryId, String objectId)
        response = ((ObjectServicePort) servicePort).getAllowableActions(repositoryId, documentId);
        assertNotNull(response);
        assertTrue(response.canGetProperties);
        assertTrue(response.canCheckout);

        response = ((ObjectServicePort) servicePort).getAllowableActions(repositoryId, folderId);
        assertNotNull(response);
        assertTrue(response.canGetProperties);
        assertNull(response.canCheckout);
    }

    public void testMoveObject() throws Exception
    {
        // public void moveObject(String repositoryId, String objectId, String targetFolderId, String sourceFolderId)
        ((ObjectServicePort) servicePort).moveObject(repositoryId, documentId, folderId, companyHomeId);

        GetPropertiesResponse response = helper.getObjectProperties(documentId);

        CmisObjectType objectType = response.getObject();

        assertNotNull(objectType);
        assertNotNull(getPropertyValue(response, CMISMapping.PROP_NAME));

        GetObjectParentsResponse parentsResponse = helper.getObjectParents(documentId, "*");
        assertTrue(parentsResponse.getObject().size() == 1);
        assertTrue(PropertyUtil.getProperty(parentsResponse.getObject().get(0).getProperties(), CMISMapping.PROP_NAME).equals(folderName));

    }

    // The moveObject() method must throw InvalidArgumentException for null SourceFolderId only if specified Object (Folder or Document) has SEVERAL parents.
    // In other case this parameter may be null.
    public void testMoveObject_Exceptions() throws Exception
    {
        // sourceFolderId is not specified - throw InvalidArgumentException
        // • If Object is multi-filed and source folder is not specified, throw InvalidArgumentException.

        helper.addObjectToFolder(documentId, folderId);

        try
        {
            ((ObjectServicePort) servicePort).moveObject(repositoryId, documentId, folderId, null);
            fail("sourceFolderId is not specified - should throw InvalidArgumentException");
        }
        catch (InvalidArgumentException e)
        {

        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public void testSetContentStream() throws Exception
    {
        String newFileName = "New file name (" + System.currentTimeMillis() + ")";
        String newContent = "New content test";

        CmisContentStreamType contentStream = new CmisContentStreamType();
        contentStream.setFilename(newFileName);
        contentStream.setMimeType(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        // contentStream.setLength(BigInteger.valueOf(256));
        // contentStream.setUri("test uri");
        // DataHandler dataHandler = new DataHandler(new FileDataSource("D:/test.txt"));
        DataHandler dataHandler = new DataHandler(newContent, MimetypeMap.MIMETYPE_TEXT_PLAIN);
        contentStream.setStream(dataHandler);

        Holder<String> documentIdHolder = new Holder<String>(documentId);
        // public void setContentStream(String repositoryId, Holder<String> documentId, Boolean overwriteFlag, CmisContentStreamType contentStream)
        ((ObjectServicePort) servicePort).setContentStream(repositoryId, documentIdHolder, true, contentStream);

        CmisContentStreamType result = ((ObjectServicePort) servicePort).getContentStream(repositoryId, documentId);
        if (result.getLength().intValue() == 0)
        {
            fail("Content Stream is empty");
        }
        assertEquals(newContent, result.getStream().getContent());

        // Alfresco create new version of document
        propertiesResponse = helper.getObjectProperties(documentIdHolder.value);
        assertFalse("new version of document should be created", getPropertyValue(propertiesResponse, CMISMapping.PROP_OBJECT_ID).equals(documentId));
        GetAllVersionsResponse responseVersions = helper.getAllVersions(documentId);
        assertTrue("new version of document should be created", responseVersions.getObject().size() > 1);

        // assertEquals(newFileName, result.getFilename());

        // GetPropertiesResponse response = helper.getObjectProperties(documentId);
        // assertNotNull(getObjectName(response));
        // assertEquals(newFileName, getContentStreamFilename(response));
    }

    public void testSetContentStream_Exceptions() throws Exception
    {
        String newFileName = "New file name (" + System.currentTimeMillis() + ")";
        String newContent = "New content test";

        CmisContentStreamType contentStream = new CmisContentStreamType();
        contentStream.setFilename(newFileName);
        contentStream.setMimeType(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        DataHandler dataHandler = new DataHandler(newContent, MimetypeMap.MIMETYPE_TEXT_PLAIN);
        contentStream.setStream(dataHandler);

        try
        {
            // public void setContentStream(String repositoryId, Holder<String> documentId, Boolean overwriteFlag, CmisContentStreamType contentStream)
            ((ObjectServicePort) servicePort).setContentStream(repositoryId, new Holder<String>(documentId), false, contentStream);
            fail("ContentAlreadyExists should be thrown");
        }
        catch (ContentAlreadyExistsException e)
        {

        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // now we can not set any property (beside name) when we create new document - so this case not working
        propertiesResponse = helper.getObjectProperties(documentId);
        Boolean contentStreamAllowed = getPropertyBooleanValue(propertiesResponse, CMISMapping.PROP_CONTENT_STREAM_ALLOWED);
        if (contentStreamAllowed != null && contentStreamAllowed == false)
        {
            try
            {
                // public void setContentStream(String repositoryId, Holder<String> documentId, Boolean overwriteFlag, CmisContentStreamType contentStream)
                ((ObjectServicePort) servicePort).setContentStream(repositoryId, new Holder<String>(documentId), true, contentStream);
                fail("ConstraintViolationException should be thrown");
            }
            catch (ConstraintViolationException e)
            {

            }
            catch (Exception e)
            {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }

    }

    public void testUpdateProperties() throws Exception
    {

        String newName = "New Cmis Test Node Name (" + System.currentTimeMillis() + ")";

        // Cmis Properties
        CmisPropertiesType properties = new CmisPropertiesType();
        List<CmisProperty> propertiesList = properties.getProperty();
        CmisPropertyString cmisProperty = new CmisPropertyString();
        cmisProperty.setName(CMISMapping.PROP_NAME);
        cmisProperty.setValue(newName);
        propertiesList.add(cmisProperty);

        // public void updateProperties(String repositoryId, Holder<String> objectId, String changeToken, CmisPropertiesType properties)
        ((ObjectServicePort) servicePort).updateProperties(repositoryId, new Holder<String>(documentId), new String(""), properties);

        GetPropertiesResponse response = helper.getObjectProperties(documentId);
        assertEquals(newName, getObjectName(response));
    }

    public void testUpdateProperties_Exceptions() throws Exception
    {
        // Now we can set up only name propery

        // try to update read only property
        CmisPropertiesType properties = new CmisPropertiesType();
        properties = new CmisPropertiesType();
        List<CmisProperty> propertiesList = properties.getProperty();
        CmisPropertyString cmisProperty = new CmisPropertyString();
        cmisProperty.setName(CMISMapping.PROP_OBJECT_ID);
        cmisProperty.setValue("new id value");
        propertiesList.add(cmisProperty);

        try
        {
            // public void updateProperties(String repositoryId, Holder<String> objectId, String changeToken, CmisPropertiesType properties)
            ((ObjectServicePort) servicePort).updateProperties(repositoryId, new Holder<String>(documentId), new String(""), properties);
            fail("should not update read only propery");
        }
        catch (ConstraintViolationException e)
        {

        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}