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

import java.util.List;

import org.alfresco.cmis.dictionary.CMISMapping;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @see org.alfresco.repo.cmis.ws.ObjectServicePortDM
 *
 * @author Dmitry Lazurkin
 */
public class DMObjectServicePortTest extends BaseServicePortContentTest
{
    private ObjectServicePort objectServicePort;
    private RepositoryServicePort repositoryServicePort;

    @Override
    protected void onSetUp() throws Exception
    {
        super.onSetUp();

        repositoryServicePort = (RepositoryServicePort) applicationContext.getBean("dmRepositoryService");
        objectServicePort = (ObjectServicePort) applicationContext.getBean("dmObjectService");
    }

    public void testGetExistentFolderProperties() throws Exception
    {
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());

        GetProperties request = new GetProperties();
        request.setRepositoryId(repositoryId);
        request.setObjectId(L0_FOLDER_0_NODEREF.toString());

        GetPropertiesResponse response = objectServicePort.getProperties(request);
        assertNotNull(response);
        assertEquals(rootNodeRef.toString(), PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_PARENT_ID));
        assertEquals(L0_FOLDER_0, PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_NAME));
    }

    public void testGetExistentDocumentPropertiesThis() throws Exception
    {
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());

        GetProperties request = new GetProperties();
        request.setRepositoryId(repositoryId);
        request.setObjectId(L1_FILE_VERSION_1_0_NODEREF.toString());

        GetPropertiesResponse response = objectServicePort.getProperties(request);
        assertNotNull(response);
        assertEquals(PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_CHECKIN_COMMENT), "1.0");
        assertTrue((Boolean) PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_IS_MAJOR_VERSION));
    }

    public void testGetExistentDocumentPropertiesLatestMajor() throws Exception
    {
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());

        GetProperties request = new GetProperties();
        request.setRepositoryId(repositoryId);
        request.setReturnVersion(cmisObjectFactory.createGetPropertiesReturnVersion(EnumReturnVersion.LATESTMAJOR));
        request.setObjectId(L1_FILE_VERSION_1_0_NODEREF.toString());

        GetPropertiesResponse response = objectServicePort.getProperties(request);
        assertNotNull(response);
        assertEquals(PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_CHECKIN_COMMENT), "2.0");
        assertTrue((Boolean) PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_IS_MAJOR_VERSION));
    }

    public void testGetExistentDocumentPropertiesLatest() throws Exception
    {
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());

        GetProperties request = new GetProperties();
        request.setRepositoryId(repositoryId);
        request.setReturnVersion(cmisObjectFactory.createGetPropertiesReturnVersion(EnumReturnVersion.LATEST));
        request.setObjectId(L1_FILE_VERSION_2_0_NODEREF.toString());

        GetPropertiesResponse response = objectServicePort.getProperties(request);
        assertNotNull(response);
        assertEquals(PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_CHECKIN_COMMENT), "2.1");
        assertFalse((Boolean) PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_IS_MAJOR_VERSION));
    }

    public void testGetNonExistentObjectProperties() throws Exception
    {
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());

        GetProperties request = new GetProperties();
        request.setRepositoryId(repositoryId);
        request.setObjectId(L0_NONEXISTENT_NODEREF.toString());
        try
        {
            objectServicePort.getProperties(request);
        }
        catch (ObjectNotFoundException e)
        {
            return;
        }

        fail("Expects exception");
    }

    public void testGetPropertiesForInvalidOID() throws Exception
    {
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());

        GetProperties request = new GetProperties();
        request.setRepositoryId(repositoryId);
        request.setObjectId("invalid OID");

        try
        {
            objectServicePort.getProperties(request);
        }
        catch (InvalidArgumentException e)
        {
            return;
        }

        fail("Expects exception");
    }

    public void testGetPropertiesCheckedout() throws Exception
    {
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());
        NodeRef workingCopyNodeRef = checkOutCheckInService.checkout(L0_FILE_0_NODEREF);

        GetProperties request = new GetProperties();
        request.setRepositoryId(repositoryId);
        request.setObjectId(L0_FILE_0_NODEREF.toString());

        GetPropertiesResponse response = objectServicePort.getProperties(request);
        assertNotNull(response);

        assertNull(PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_CHECKIN_COMMENT));
        assertTrue((Boolean) PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_IS_VERSION_SERIES_CHECKED_OUT));
        assertEquals(PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_VERSION_SERIES_CHECKED_OUT_BY), authenticationComponent.getSystemUserName());
        assertEquals(PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_VERSION_SERIES_CHECKED_OUT_ID), workingCopyNodeRef.toString());

        request = new GetProperties();
        request.setRepositoryId(repositoryId);
        request.setObjectId(workingCopyNodeRef.toString());

        response = objectServicePort.getProperties(request);
        assertNotNull(response);
        assertNull(PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_CHECKIN_COMMENT));
        assertTrue((Boolean) PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_IS_VERSION_SERIES_CHECKED_OUT));
        assertEquals(PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_VERSION_SERIES_CHECKED_OUT_BY), authenticationComponent.getSystemUserName());
        assertEquals(PropertyUtil.getProperty(response.getObject().getProperties(), CMISMapping.PROP_VERSION_SERIES_CHECKED_OUT_ID), workingCopyNodeRef.toString());
    }

    public void testGetContentStream() throws Exception
    {
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());

        List<CmisRepositoryEntryType> repositories = repositoryServicePort.getRepositories();
        String documentId = L0_FILE_0_NODEREF.toString();
        CmisContentStreamType result = objectServicePort.getContentStream(repositories.get(0).getRepositoryID(), documentId);
        assertNotNull(result);
        if (result.getLength().intValue() != 9)
        {
            fail();
        }

        try
        {
            result = objectServicePort.getContentStream(repositories.get(0).getRepositoryID(), documentId + "s");
        }
        catch (ObjectNotFoundException e)
        {
        }
        catch (Throwable e)
        {
            fail();
        }
    }

}