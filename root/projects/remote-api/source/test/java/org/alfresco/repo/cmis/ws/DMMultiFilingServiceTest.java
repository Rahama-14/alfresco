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

import javax.xml.namespace.QName;

import org.alfresco.cmis.dictionary.CMISMapping;

public class DMMultiFilingServiceTest extends AbstractServiceTest
{

    public final static String SERVICE_WSDL_LOCATION = CmisServiceTestHelper.ALFRESCO_URL + "/cmis/MultiFilingService?wsdl";
    public final static QName SERVICE_NAME = new QName("http://www.cmis.org/ns/1.0", "MultiFilingService");
    private String anotherFolderId;

    public DMMultiFilingServiceTest()
    {
        super();
    }

    public DMMultiFilingServiceTest(String testCase, String username, String password)
    {
        super(testCase, username, password);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        createInitialContent();
        anotherFolderId = helper.createFolder("Test Cmis Folder (" + System.currentTimeMillis() + ")", companyHomeId);

    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        deleteInitialContent();
        helper.deleteFolder(anotherFolderId);
    }

    protected Object getServicePort()
    {
        return helper.multiFilingServicePort;
    }

    public void testAddObjectToFolder() throws Exception
    {
        ((MultiFilingServicePort) servicePort).addObjectToFolder(repositoryId, documentId, anotherFolderId);
        boolean found = false;
        for (CmisObjectType cmisObjectType : helper.getChildren(anotherFolderId, EnumTypesOfFileableObjects.DOCUMENTS, 0, CMISMapping.PROP_OBJECT_ID).getObject())
        {
            if ((found = PropertyUtil.getProperty(cmisObjectType.getProperties(), CMISMapping.PROP_OBJECT_ID).equals(documentId)))
            {
                break;
            }
        }
        assertTrue("Document was not added to folder", found);
    }

    public void testRemoveObjectFromFolder() throws Exception
    {

        helper.addObjectToFolder(documentId, anotherFolderId);

        try
        {
            // remove object from all folders expects Exception
            ((MultiFilingServicePort) servicePort).removeObjectFromFolder(repositoryId, documentId, null);
            fail("Expects exception");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof OperationNotSupportedException);
        }

        helper.removeObjectFromFolder(documentId, anotherFolderId);

        try
        {
            // remove object from folder where it is not situated expects Exception
            ((MultiFilingServicePort) servicePort).removeObjectFromFolder(repositoryId, documentId, folderId);
            fail("Expected exception");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof NotInFolderException);
        }

        try
        {
            // remove object from last folder expects Exception
            ((MultiFilingServicePort) servicePort).removeObjectFromFolder(repositoryId, documentId, companyHomeId);
            fail("Expected exception");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof OperationNotSupportedException);
        }
    }
}
