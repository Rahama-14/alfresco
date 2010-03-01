/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.model.filefolder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.alfresco.error.AlfrescoRuntimeException;
import org.springframework.extensions.surf.util.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.dictionary.M2Type;
import org.alfresco.repo.node.integrity.IntegrityChecker;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileFolderServiceType;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.CyclicChildRelationshipException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

/**
 * @see org.alfresco.repo.model.filefolder.FileFolderServiceImpl
 * @author Derek Hulley
 */
public class FileFolderServiceImplTest extends TestCase
{
    private static final String IMPORT_VIEW = "filefolder/filefolder-test-import.xml";

    private static final String NAME_L0_FILE_A = "L0- File A";
    private static final String NAME_L0_FILE_B = "L0- File B";
    private static final String NAME_L0_FOLDER_A = "L0- Folder A";
    private static final String NAME_L0_FOLDER_B = "L0- Folder B";
    private static final String NAME_L0_FOLDER_C = "L0- Folder C";
    private static final String NAME_L1_FOLDER_A = "L1- Folder A";
    private static final String NAME_L1_FOLDER_B = "L1- Folder B";
    private static final String NAME_L1_FILE_A = "L1- File A";
    private static final String NAME_L1_FILE_B = "L1- File B";
    private static final String NAME_L1_FILE_C = "L1- File C (%_)";
    private static final String NAME_CHECK_FILE = "CHECK_FILE";
    private static final String NAME_CHECK_FOLDER = "CHECK_FOLDER";

    private static final ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();

    private TransactionService transactionService;
    private NodeService nodeService;
    private FileFolderService fileFolderService;
    private DictionaryDAO dictionaryDAO;
    private UserTransaction txn;
    private NodeRef rootNodeRef;
    private NodeRef workingRootNodeRef;

    @Override
    public void setUp() throws Exception
    {
        ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean("ServiceRegistry");
        transactionService = serviceRegistry.getTransactionService();
        nodeService = serviceRegistry.getNodeService();
        fileFolderService = serviceRegistry.getFileFolderService();
        dictionaryDAO = (DictionaryDAO) ctx.getBean("dictionaryDAO");
        AuthenticationComponent authenticationComponent = (AuthenticationComponent) ctx
                .getBean("authenticationComponent");

        // start the transaction
        txn = transactionService.getUserTransaction();
        txn.begin();

        // downgrade integrity
        IntegrityChecker.setWarnInTransaction();

        // authenticate
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());

        // create a test store
        StoreRef storeRef = nodeService
                .createStore(StoreRef.PROTOCOL_WORKSPACE, getName() + System.currentTimeMillis());
        rootNodeRef = nodeService.getRootNode(storeRef);

        // create a folder to import into
        workingRootNodeRef = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN,
                QName.createQName(NamespaceService.ALFRESCO_URI, "working root"), ContentModel.TYPE_FOLDER)
                .getChildRef();

        // import the test data
        ImporterService importerService = serviceRegistry.getImporterService();
        Location importLocation = new Location(workingRootNodeRef);
        InputStream is = getClass().getClassLoader().getResourceAsStream(IMPORT_VIEW);
        if (is == null)
        {
            throw new NullPointerException("Test resource not found: " + IMPORT_VIEW);
        }
        Reader reader = new InputStreamReader(is);
        importerService.importView(reader, importLocation, null, null);
    }

    public void tearDown() throws Exception
    {
        try
        {
            txn.rollback();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Checks that the names and numbers of files and folders in the provided list is correct
     * 
     * @param files the list of files
     * @param expectedFileCount the number of uniquely named files expected
     * @param expectedFolderCount the number of uniquely named folders expected
     * @param expectedNames the names of the files and folders expected
     */
    private void checkFileList(
            List<FileInfo> files,
            int expectedFileCount,
            int expectedFolderCount,
            String[] expectedNames)
    {
        int fileCount = 0;
        int folderCount = 0;
        List<String> check = new ArrayList<String>(8);
        for (String filename : expectedNames)
        {
            check.add(filename);
        }
        for (FileInfo file : files)
        {
            if (file.isFolder())
            {
                folderCount++;
            }
            else
            {
                fileCount++;
            }
            check.remove(file.getName());
        }
        assertTrue("Name list was not exact - remaining: " + check, check.size() == 0);
        assertEquals("Incorrect number of files", expectedFileCount, fileCount);
        assertEquals("Incorrect number of folders", expectedFolderCount, folderCount);
    }

    public void testShallowFilesAndFoldersList() throws Exception
    {
        List<FileInfo> files = fileFolderService.list(workingRootNodeRef);
        // check
        String[] expectedNames = new String[]
        { NAME_L0_FILE_A, NAME_L0_FILE_B, NAME_L0_FOLDER_A, NAME_L0_FOLDER_B, NAME_L0_FOLDER_C };
        checkFileList(files, 2, 3, expectedNames);
    }

    public void testShallowFilesOnlyList() throws Exception
    {
        List<FileInfo> files = fileFolderService.listFiles(workingRootNodeRef);
        // check
        String[] expectedNames = new String[]
        { NAME_L0_FILE_A, NAME_L0_FILE_B };
        checkFileList(files, 2, 0, expectedNames);
    }

    public void testShallowFoldersOnlyList() throws Exception
    {
        List<FileInfo> files = fileFolderService.listFolders(workingRootNodeRef);
        // check
        String[] expectedNames = new String[]
        { NAME_L0_FOLDER_A, NAME_L0_FOLDER_B, NAME_L0_FOLDER_C };
        checkFileList(files, 0, 3, expectedNames);
    }

    public void testShallowFileSearch() throws Exception
    {
        List<FileInfo> files = fileFolderService.search(workingRootNodeRef, NAME_L0_FILE_B, true, false, false);
        // check
        String[] expectedNames = new String[]
        { NAME_L0_FILE_B };
        checkFileList(files, 1, 0, expectedNames);
    }

    public void testDeepFilesAndFoldersSearch() throws Exception
    {
        List<FileInfo> files = fileFolderService.search(workingRootNodeRef, "?1-*", true, true, true);
        // check
        String[] expectedNames = new String[]
        { NAME_L1_FOLDER_A, NAME_L1_FOLDER_B, NAME_L1_FILE_A, NAME_L1_FILE_B, NAME_L1_FILE_C };
        checkFileList(files, 3, 2, expectedNames);
    }

    public void testDeepFilesOnlySearch() throws Exception
    {
        List<FileInfo> files = fileFolderService.search(workingRootNodeRef, "?1-*", true, false, true);
        // check
        String[] expectedNames = new String[]
        { NAME_L1_FILE_A, NAME_L1_FILE_B, NAME_L1_FILE_C };
        checkFileList(files, 3, 0, expectedNames);
    }

    /**
     * Helper to fetch a file or folder by name
     * 
     * @param name the name of the file or folder
     * @param isFolder true if we want a folder, otherwise false if we want a file
     * @return Returns the info for the file or folder
     */
    private FileInfo getByName(String name, boolean isFolder) throws Exception
    {
        List<FileInfo> results = fileFolderService.search(workingRootNodeRef, name, !isFolder, isFolder, true);
        if (results.size() > 1)
        {
            throw new AlfrescoRuntimeException("Name is not unique in hierarchy: \n" + "   name: " + name + "\n"
                    + "   is folder: " + isFolder);
        }
        else if (results.size() == 0)
        {
            return null;
        }
        else
        {
            return results.get(0);
        }
    }

    /**
     * Ensure that an internal method is working - it gets used extensively by following tests
     * 
     * @see #getByName(String, boolean)
     */
    public void testGetByName() throws Exception
    {
        FileInfo fileInfo = getByName(NAME_CHECK_FOLDER, true);
        assertNotNull(fileInfo);
        assertTrue(fileInfo.isFolder());

        fileInfo = getByName(NAME_CHECK_FILE, false);
        assertNotNull(fileInfo);
        assertFalse(fileInfo.isFolder());
    }

    public void testRenameNormal() throws Exception
    {
        FileInfo folderInfo = getByName(NAME_L0_FOLDER_A, true);
        assertNotNull(folderInfo);
        // rename normal
        String newName = "DUPLICATE - renamed";
        folderInfo = fileFolderService.rename(folderInfo.getNodeRef(), newName);
        // check it
        FileInfo checkInfo = getByName(NAME_L0_FOLDER_A, true);
        assertNull("Folder info should have been renamed away", checkInfo);
        checkInfo = getByName(newName, true);
        assertNotNull("Folder info for new name is not present", checkInfo);
    }

    public void testRenameWithoutAssocQNameChange() throws Exception
    {
        FileInfo folderInfo = getByName(NAME_L0_FOLDER_A, true);
        assertNotNull(folderInfo);
        NodeRef folderNodeRef = folderInfo.getNodeRef();
        // Create a child file
        QName assocQName = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "abc");
        NodeRef newFileNodeRef = fileFolderService.create(folderNodeRef, "AnotherFile.txt", ContentModel.TYPE_CONTENT,
                assocQName).getNodeRef();
        // Make sure that the correct association QName was used
        QName checkQName = nodeService.getPrimaryParent(newFileNodeRef).getQName();
        assertEquals("The given assoc QName was not used for the path", assocQName, checkQName);
        // Rename
        String newName = "AnotherFile-new.txt";
        folderInfo = fileFolderService.rename(newFileNodeRef, newName);
        // Make sure that the association QName did not change
        checkQName = nodeService.getPrimaryParent(newFileNodeRef).getQName();
        assertEquals("The given assoc QName was not used for the path after a rename", assocQName, nodeService
                .getPrimaryParent(newFileNodeRef).getQName());
    }

    public void testRenameDuplicate() throws Exception
    {
        FileInfo folderInfo = getByName(NAME_L0_FOLDER_A, true);
        assertNotNull(folderInfo);
        // rename duplicate. A file with that name already exists
        String newName = NAME_L0_FILE_A;
        try
        {
            folderInfo = fileFolderService.rename(folderInfo.getNodeRef(), newName);
            fail("Existing file not detected");
        }
        catch (FileExistsException e)
        {
            // expected
        }
    }

    public void testMove() throws Exception
    {
        // we are testing failures as well
        txn.commit();
        // start a new one
        txn = transactionService.getNonPropagatingUserTransaction();
        txn.begin();
        
        FileInfo folderToMoveInfo = getByName(NAME_L1_FOLDER_A, true);
        assertNotNull(folderToMoveInfo);
        NodeRef folderToMoveRef = folderToMoveInfo.getNodeRef();
        // move it to the root
        fileFolderService.move(folderToMoveRef, workingRootNodeRef, null);
        // make sure that it is an immediate child of the root
        List<FileInfo> checkFileInfos = fileFolderService.search(workingRootNodeRef, NAME_L1_FOLDER_A, false);
        assertEquals("Folder not moved to root", 1, checkFileInfos.size());
        // rename properly
        FileInfo checkFileInfo = fileFolderService.move(folderToMoveRef, null, "new name");
        checkFileInfos = fileFolderService.search(workingRootNodeRef, checkFileInfo.getName(), false);
        assertEquals("Folder not renamed in root", 1, checkFileInfos.size());
        // attempt illegal rename (existing)
        try
        {
            fileFolderService.move(folderToMoveRef, null, NAME_L0_FOLDER_A);
            fail("Existing folder not detected");
        }
        catch (FileExistsException e)
        {
            // expected
        }
        
        txn.rollback();
        txn = transactionService.getNonPropagatingUserTransaction();
        txn.begin();

        // Move a file to a new location
        FileInfo fileA = getByName(NAME_L1_FILE_A, false);
        FileInfo folderB = getByName(NAME_L0_FOLDER_B, true);
        fileFolderService.copy(fileA.getNodeRef(), folderB.getNodeRef(), null);
        try
        {
            // Move to a target folder without a rename and expecting a name clash
            fileFolderService.move(fileA.getNodeRef(), folderB.getNodeRef(), null);
            fail("Duplicately-named file in target folder was not detected");
        }
        catch (FileExistsException e)
        {
            // Expected
        }

        txn.rollback();
        txn = transactionService.getNonPropagatingUserTransaction();
        txn.begin();

        // Move to a target folder but with a rename to avoid the name clash
        fileFolderService.move(fileA.getNodeRef(), folderB.getNodeRef(), NAME_L1_FILE_B);
    }

    public void testCopy() throws Exception
    {
        FileInfo folderToCopyInfo = getByName(NAME_L1_FOLDER_A, true);
        assertNotNull(folderToCopyInfo);
        NodeRef folderToCopyRef = folderToCopyInfo.getNodeRef();
        // copy it to the root
        folderToCopyInfo = fileFolderService.copy(folderToCopyRef, workingRootNodeRef, null);
        folderToCopyRef = folderToCopyInfo.getNodeRef();
        // make sure that it is an immediate child of the root
        List<FileInfo> checkFileInfos = fileFolderService.search(workingRootNodeRef, NAME_L1_FOLDER_A, false);
        assertEquals("Folder not copied to root", 1, checkFileInfos.size());
        // copy properly
        FileInfo checkFileInfo = fileFolderService.copy(folderToCopyRef, null, "new name");
        checkFileInfos = fileFolderService.search(workingRootNodeRef, checkFileInfo.getName(), false);
        assertEquals("Folder not renamed in root", 1, checkFileInfos.size());
        // attempt illegal copy (existing)
        try
        {
            fileFolderService.copy(folderToCopyRef, null, NAME_L0_FOLDER_A);
            fail("Existing folder not detected");
        }
        catch (FileExistsException e)
        {
            // expected
        }
    }

    public void testCreateFolder() throws Exception
    {
        // we are testing failures as well
        txn.commit();
        // start a new one
        txn = transactionService.getNonPropagatingUserTransaction();
        txn.begin();

        FileInfo parentFolderInfo = getByName(NAME_L0_FOLDER_A, true);
        assertNotNull(parentFolderInfo);
        NodeRef parentFolderRef = parentFolderInfo.getNodeRef();
        // create a file that already exists
        UserTransaction rollbackTxn = null;
        try
        {
            rollbackTxn = transactionService.getNonPropagatingUserTransaction();
            rollbackTxn.begin();
            fileFolderService.create(parentFolderRef, NAME_L1_FILE_A, ContentModel.TYPE_CONTENT);
            fail("Failed to detect duplicate filename");
        }
        catch (FileExistsException e)
        {
            // expected
        }
        finally
        {
            rollbackTxn.rollback();
        }
        // create folder of illegal type
        try
        {
            rollbackTxn = transactionService.getNonPropagatingUserTransaction();
            rollbackTxn.begin();
            fileFolderService.create(parentFolderRef, "illegal folder", ContentModel.TYPE_SYSTEM_FOLDER);
            fail("Illegal type not detected");
        }
        catch (RuntimeException e)
        {
            // expected
        }
        finally
        {
            rollbackTxn.rollback();
        }
        // Create a cm:folder derived type
        try
        {
            rollbackTxn = transactionService.getNonPropagatingUserTransaction();
            rollbackTxn.begin();
            // Create a new model
            String testNs = "http://www.alfresco.org/model/test111/1.0";
            M2Model testModel = M2Model.createModel("t111:filefolderserviceimpltest");
            testModel.createNamespace(testNs, "t111");
            testModel.createImport(NamespaceService.DICTIONARY_MODEL_1_0_URI, NamespaceService.DICTIONARY_MODEL_PREFIX);
            testModel.createImport(NamespaceService.SYSTEM_MODEL_1_0_URI, NamespaceService.SYSTEM_MODEL_PREFIX);
            testModel.createImport(NamespaceService.CONTENT_MODEL_1_0_URI, NamespaceService.CONTENT_MODEL_PREFIX);

            M2Type testType = testModel.createType("t111:subfolder");
            testType.setParentName("cm:" + ContentModel.TYPE_FOLDER.getLocalName());
            dictionaryDAO.putModel(testModel);
            fileFolderService
                    .create(parentFolderRef, "Legal subtype of folder", QName.createQName(testNs, "subfolder"));
        }
        catch (Throwable e)
        {
            throw new Exception("Legal subtype of cm:folder not allowed.", e);
        }
        finally
        {
            rollbackTxn.rollback();
        }

        // create a file
        FileInfo fileInfo = fileFolderService.create(parentFolderRef, "newFile", ContentModel.TYPE_CONTENT);
        // check
        assertTrue("Node not created", nodeService.exists(fileInfo.getNodeRef()));
        assertFalse("File type expected", fileInfo.isFolder());
    }

    public void testCreateFile() throws Exception
    {

    }

    public void testCreateInRoot() throws Exception
    {
        fileFolderService.create(rootNodeRef, "New Folder", ContentModel.TYPE_FOLDER);
    }

    public void testMakeFolders() throws Exception
    {
        // create a completely new path below the root
        List<String> namePath = new ArrayList<String>(4);
        namePath.add("AAA");
        namePath.add("BBB");
        namePath.add("CCC");
        namePath.add("DDD");

        FileInfo lastFileInfo = FileFolderServiceImpl.makeFolders(fileFolderService, rootNodeRef, namePath,
                ContentModel.TYPE_FOLDER);
        assertNotNull("First makeFolder failed", lastFileInfo);
        // check that a repeat works

        FileInfo lastFileInfoAgain = FileFolderServiceImpl.makeFolders(fileFolderService, rootNodeRef, namePath,
                ContentModel.TYPE_FOLDER);
        assertNotNull("Repeat makeFolders failed", lastFileInfoAgain);
        assertEquals("Repeat created new leaf", lastFileInfo.getNodeRef(), lastFileInfoAgain.getNodeRef());
        // check that it worked
        List<FileInfo> checkInfos = fileFolderService.search(rootNodeRef, "DDD", false, true, true);
        assertEquals("Expected to find a result", 1, checkInfos.size());
        // get the path
        List<FileInfo> checkPathInfos = fileFolderService.getNamePath(rootNodeRef, checkInfos.get(0).getNodeRef());
        assertEquals("Path created is incorrect", namePath.size(), checkPathInfos.size());
        int i = 0;
        for (FileInfo checkInfo : checkPathInfos)
        {
            assertEquals("Path mismatch", namePath.get(i), checkInfo.getName());
            i++;
        }
    }

    /**
     * Lucene only indexes terms that are 3 characters or more
     */
    public void testMakeFoldersShortNames() throws Exception
    {
        // create a completely new path below the root
        List<String> namePath = new ArrayList<String>(4);
        namePath.add("A");
        namePath.add("B");
        namePath.add("C");
        namePath.add("D");

        FileInfo lastFileInfo = FileFolderServiceImpl.makeFolders(fileFolderService, rootNodeRef, namePath,
                ContentModel.TYPE_FOLDER);
        assertNotNull("First makeFolder failed", lastFileInfo);
        // check that a repeat works

        FileInfo lastFileInfoAgain = FileFolderServiceImpl.makeFolders(fileFolderService, rootNodeRef, namePath,
                ContentModel.TYPE_FOLDER);
        assertNotNull("Repeat makeFolders failed", lastFileInfoAgain);
        assertEquals("Repeat created new leaf", lastFileInfo.getNodeRef(), lastFileInfoAgain.getNodeRef());
    }

    public void testGetNamePath() throws Exception
    {
        FileInfo fileInfo = getByName(NAME_L1_FILE_A, false);
        assertNotNull(fileInfo);
        NodeRef nodeRef = fileInfo.getNodeRef();

        List<FileInfo> infoPaths = fileFolderService.getNamePath(workingRootNodeRef, nodeRef);
        assertEquals("Not enough elements", 2, infoPaths.size());
        assertEquals("First level incorrent", NAME_L0_FOLDER_A, infoPaths.get(0).getName());
        assertEquals("Second level incorrent", NAME_L1_FILE_A, infoPaths.get(1).getName());

        // pass in a null root and make sure that it still works
        infoPaths = fileFolderService.getNamePath(null, nodeRef);
        assertEquals("Not enough elements", 3, infoPaths.size());
        assertEquals("First level incorrent", workingRootNodeRef.getId(), infoPaths.get(0).getName());
        assertEquals("Second level incorrent", NAME_L0_FOLDER_A, infoPaths.get(1).getName());
        assertEquals("Third level incorrent", NAME_L1_FILE_A, infoPaths.get(2).getName());

        // check that a non-aligned path is detected
        NodeRef startRef = getByName(NAME_L0_FOLDER_B, true).getNodeRef();
        try
        {
            fileFolderService.getNamePath(startRef, nodeRef);
            fail("Failed to detect non-aligned path from root to target node");
        }
        catch (FileNotFoundException e)
        {
            // expected
        }
    }

    public void testSearchSimple() throws Exception
    {
        FileInfo folderInfo = getByName(NAME_L0_FOLDER_A, true);
        assertNotNull(folderInfo);
        NodeRef folderNodeRef = folderInfo.getNodeRef();
        // search for a file that is not there
        NodeRef phantomNodeRef = fileFolderService.searchSimple(folderNodeRef, "aaaaaaa");
        assertNull("Found non-existent node by name", phantomNodeRef);
        // search for a file that is there
        NodeRef fileNodeRef = fileFolderService.searchSimple(folderNodeRef, NAME_L1_FILE_A);
        assertNotNull("Didn't find file", fileNodeRef);
        // double check
        FileInfo checkInfo = getByName(NAME_L1_FILE_A, false);
        assertEquals("Incorrect node found", checkInfo.getNodeRef(), fileNodeRef);
    }

    public void testResolveNamePath() throws Exception
    {
        FileInfo fileInfo = getByName(NAME_L1_FILE_A, false);
        List<String> pathElements = new ArrayList<String>(3);
        pathElements.add(NAME_L0_FOLDER_A);
        pathElements.add(NAME_L1_FILE_A);

        FileInfo fileInfoCheck = fileFolderService.resolveNamePath(workingRootNodeRef, pathElements);
        assertNotNull("File info not found", fileInfoCheck);
        assertEquals("Path not resolved to correct node", fileInfo.getNodeRef(), fileInfoCheck.getNodeRef());
    }

    public void testGetReaderWriter() throws Exception
    {
        // testing a failure
        txn.commit();
        txn = transactionService.getUserTransaction();
        txn.begin();

        FileInfo dirInfo = getByName(NAME_L0_FOLDER_A, true);

        UserTransaction rollbackTxn = null;
        try
        {
            rollbackTxn = transactionService.getNonPropagatingUserTransaction();
            rollbackTxn.begin();
            fileFolderService.getWriter(dirInfo.getNodeRef());
            fail("Failed to detect content write to folder");
        }
        catch (RuntimeException e)
        {
            // expected
        }
        finally
        {
            rollbackTxn.rollback();
        }

        FileInfo fileInfo = getByName(NAME_L1_FILE_A, false);

        ContentWriter writer = fileFolderService.getWriter(fileInfo.getNodeRef());
        assertNotNull("Writer is null", writer);
        // write some content
        String content = "ABC";
        writer.putContent(content);
        // read the content
        ContentReader reader = fileFolderService.getReader(fileInfo.getNodeRef());
        assertNotNull("Reader is null", reader);
        String checkContent = reader.getContentString();
        assertEquals("Content mismatch", content, checkContent);
    }

    public void testLongFileNames() throws Exception
    {
        String fileName = "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890";
        FileInfo fileInfo = fileFolderService.create(workingRootNodeRef, fileName, ContentModel.TYPE_CONTENT);
        // see if we can get it again
        NodeRef fileNodeRef = fileFolderService.searchSimple(workingRootNodeRef, fileName);
        assertNotNull("Long filename not found", fileNodeRef);
        assertEquals(fileInfo.getNodeRef(), fileNodeRef);
    }

    /**
     * Validates <a href="https://issues.alfresco.com/jira/browse/ALFCOM-2655">ACT-7225</a>
     */
    public void testGetType() throws Exception
    {
        I18NUtil.setContentLocale(Locale.CANADA);
        FileFolderServiceType type = fileFolderService.getType(ContentModel.TYPE_FOLDER);
        assertEquals("Type incorrect for folder", FileFolderServiceType.FOLDER, type);
    }
    
    public void testETHREEOH_3088_MoveIntoSelf() throws Exception
    {
        FileInfo folderInfo = fileFolderService.create(workingRootNodeRef, "NotGood.txt", ContentModel.TYPE_FOLDER);
        NodeRef folderNodeRef = folderInfo.getNodeRef();
        // Move into self
        try
        {
            fileFolderService.move(folderNodeRef, folderNodeRef, null);
            fail("Failed to detect cyclic relationship");
        }
        catch (CyclicChildRelationshipException e)
        {
            // Expected
        }
    }
}
