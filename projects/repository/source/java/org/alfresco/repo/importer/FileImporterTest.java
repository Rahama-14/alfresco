/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.importer;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.List;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.transform.AbstractContentTransformerTest;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

public class FileImporterTest extends TestCase
{
    static ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
    private NodeService nodeService;
    private SearchService searchService;
    private DictionaryService dictionaryService;
    private ContentService contentService;
    private AuthenticationService authenticationService;
    private MimetypeService mimetypeService;
    private NamespaceService namespaceService;

    private ServiceRegistry serviceRegistry;
    private NodeRef rootNodeRef;

    private SearchService searcher;

    public FileImporterTest()
    {
        super();
    }

    public FileImporterTest(String arg0)
    {
        super(arg0);
    }

    public void setUp()
    {
        serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);

        searcher = serviceRegistry.getSearchService();
        nodeService = serviceRegistry.getNodeService();
        searchService = serviceRegistry.getSearchService();
        dictionaryService = serviceRegistry.getDictionaryService();
        contentService = serviceRegistry.getContentService();
        authenticationService = (AuthenticationService) ctx.getBean("authenticationService");
        mimetypeService = serviceRegistry.getMimetypeService();
        namespaceService = serviceRegistry.getNamespaceService();

        StoreRef storeRef = nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
        rootNodeRef = nodeService.getRootNode(storeRef);
    }

    private FileImporter createFileImporter()
    {
        FileImporterImpl fileImporter = new FileImporterImpl();
        fileImporter.setAuthenticationService(authenticationService);
        fileImporter.setContentService(contentService);
        fileImporter.setMimetypeService(mimetypeService);
        fileImporter.setNodeService(nodeService);
        fileImporter.setDictionaryService(dictionaryService);
        return fileImporter;
    }

    public void testCreateFile() throws Exception
    {
        FileImporter fileImporter = createFileImporter();
        File file = AbstractContentTransformerTest.loadQuickTestFile("xml");
        fileImporter.loadFile(rootNodeRef, file);
    }

    public void testLoadRootNonRecursive1()
    {
        FileImporter fileImporter = createFileImporter();
        URL url = this.getClass().getClassLoader().getResource("");
        File root = new File(url.getFile());
        int count = fileImporter.loadFile(rootNodeRef, new File(url.getFile()));
        assertEquals("Expected to load a single file", 1, count);
    }

    public void testLoadRootNonRecursive2()
    {
        FileImporter fileImporter = createFileImporter();
        URL url = this.getClass().getClassLoader().getResource("");
        File root = new File(url.getFile());
        int count = fileImporter.loadFile(rootNodeRef, root, null, false);
        assertEquals("Expected to load a single file", 1, count);
    }

    public void testLoadXMLFiles()
    {
        FileImporter fileImporter = createFileImporter();
        URL url = this.getClass().getClassLoader().getResource("");
        FileFilter filter = new XMLFileFilter();
        fileImporter.loadFile(rootNodeRef, new File(url.getFile()), filter, true);
    }

    public void testLoadSourceTestResources()
    {
        FileImporter fileImporter = createFileImporter();
        URL url = this.getClass().getClassLoader().getResource("quick");
        FileFilter filter = new QuickFileFilter();
        fileImporter.loadFile(rootNodeRef, new File(url.getFile()), filter, true);
    }

    private static class XMLFileFilter implements FileFilter
    {
        public boolean accept(File file)
        {
            return file.getName().endsWith(".xml");
        }
    }

    private static class QuickFileFilter implements FileFilter
    {
        public boolean accept(File file)
        {
            return file.getName().startsWith("quick");
        }
    }

    /**
     * @param args
     *            <ol>
     *            <li>StoreRef
     *            <li>Store Path
     *            <li>Directory
     *            <li>Optional maximum time in seconds for node loading
     *            </ol>
     * @throws SystemException
     * @throws NotSupportedException
     * @throws HeuristicRollbackException
     * @throws HeuristicMixedException
     * @throws RollbackException
     * @throws IllegalStateException
     * @throws SecurityException
     */
    public static final void main(String[] args) throws Exception
    {

        int exitCode = 0;

        int grandTotal = 0;
        int count = 0;
        int target = 1000;
        while (count < target)
        {
            count++;
            FileImporterTest test = new FileImporterTest();
            test.setUp();

            TransactionService transactionService = test.serviceRegistry.getTransactionService();
            UserTransaction tx = transactionService.getUserTransaction(); 
            tx.begin();

            try
            {
                StoreRef spacesStore = new StoreRef(args[0]);
                if (!test.nodeService.exists(spacesStore))
                {
                    test.nodeService.createStore(spacesStore.getProtocol(), spacesStore.getIdentifier());
                }

                NodeRef storeRoot = test.nodeService.getRootNode(spacesStore);
                List<NodeRef> location = test.searchService.selectNodes(
                        storeRoot,
                        args[1],
                        null,
                        test.namespaceService,
                        false);
                if (location.size() == 0)
                {
                    throw new AlfrescoRuntimeException(
                            "Root node not found, " +
                            args[1] +
                            " not found in store, " +
                            storeRoot);
                }

                long start = System.nanoTime();
                int importCount = test.createFileImporter().loadFile(location.get(0), new File(args[2]), true);
                grandTotal += importCount;
                long end = System.nanoTime();
                long first = end-start;
                System.out.println("Created in: " + ((end - start) / 1000000.0) + "ms");
                start = System.nanoTime();

                tx.commit();
                end = System.nanoTime();
                long second = end-start;
                System.out.println("Committed in: " + ((end - start) / 1000000.0) + "ms");
                double total = ((first+second)/1000000.0);
                System.out.println("Grand Total: "+ grandTotal);
                System.out.println("Count: "+ count + "ms");
                System.out.println("Imported: " + importCount + " files or directories");
                System.out.println("Average: " + (importCount / (total / 1000.0)) + " files per second");
            }
            catch (Throwable e)
            {
                tx.rollback();
                e.printStackTrace();
                exitCode = 1;
            }
            //System.exit(exitCode);
        }
    }
}
