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
package org.alfresco.repo.node;

import java.io.InputStream;
import java.util.Map;

import junit.framework.TestCase;

import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.search.impl.lucene.fts.FullTextSearchIndexer;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.DynamicNamespacePrefixResolver;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

/**
 * @author Andy Hind
 */
@SuppressWarnings("unused")
public class ConcurrentNodeServiceTest extends TestCase
{
    public static final String NAMESPACE = "http://www.alfresco.org/test/BaseNodeServiceTest";

    public static final String TEST_PREFIX = "test";

    public static final QName TYPE_QNAME_TEST_CONTENT = QName.createQName(NAMESPACE, "content");

    public static final QName ASPECT_QNAME_TEST_TITLED = QName.createQName(NAMESPACE, "titled");

    public static final QName PROP_QNAME_TEST_TITLE = QName.createQName(NAMESPACE, "title");

    public static final QName PROP_QNAME_TEST_MIMETYPE = QName.createQName(NAMESPACE, "mimetype");

    public static final int COUNT = 10;

    public static final int REPEATS = 20;

    static ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();

    private NodeService nodeService;

    private TransactionService transactionService;
    private RetryingTransactionHelper retryingTransactionHelper;

    private NodeRef rootNodeRef;

    private FullTextSearchIndexer luceneFTS;

    private AuthenticationComponent authenticationComponent;

    public ConcurrentNodeServiceTest()
    {
        super();
    }

    protected void setUp() throws Exception
    {
        DictionaryDAO dictionaryDao = (DictionaryDAO) ctx.getBean("dictionaryDAO");
        // load the system model
        ClassLoader cl = BaseNodeServiceTest.class.getClassLoader();
        InputStream modelStream = cl.getResourceAsStream("alfresco/model/systemModel.xml");
        assertNotNull(modelStream);
        M2Model model = M2Model.createModel(modelStream);
        dictionaryDao.putModel(model);
        // load the test model
        modelStream = cl.getResourceAsStream("org/alfresco/repo/node/BaseNodeServiceTest_model.xml");
        assertNotNull(modelStream);
        model = M2Model.createModel(modelStream);
        dictionaryDao.putModel(model);

        nodeService = (NodeService) ctx.getBean("dbNodeService");
        transactionService = (TransactionService) ctx.getBean("transactionComponent");
        retryingTransactionHelper = (RetryingTransactionHelper) ctx.getBean("retryingTransactionHelper");
        luceneFTS = (FullTextSearchIndexer) ctx.getBean("LuceneFullTextSearchIndexer");
        this.authenticationComponent = (AuthenticationComponent) ctx.getBean("authenticationComponent");

        this.authenticationComponent.setSystemUserAsCurrentUser();

        // create a first store directly
        RetryingTransactionCallback<Object> createRootNodeCallback =  new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                StoreRef storeRef = nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_"
                        + System.currentTimeMillis());
                rootNodeRef = nodeService.getRootNode(storeRef);
                return null;
            }
        };
        retryingTransactionHelper.doInTransaction(createRootNodeCallback);
    }

    @Override
    protected void tearDown() throws Exception
    {
        authenticationComponent.clearCurrentSecurityContext();
        super.tearDown();
    }

    protected Map<QName, ChildAssociationRef> buildNodeGraph() throws Exception
    {
        return BaseNodeServiceTest.buildNodeGraph(nodeService, rootNodeRef);
    }

    protected Map<QName, ChildAssociationRef> commitNodeGraph() throws Exception
    {
        RetryingTransactionCallback<Map<QName, ChildAssociationRef>> buildGraphCallback =
            new RetryingTransactionCallback<Map<QName, ChildAssociationRef>>()
        {
            public Map<QName, ChildAssociationRef> execute() throws Exception
            {

                Map<QName, ChildAssociationRef> answer = buildNodeGraph();
                return answer;
            }
        };
        return retryingTransactionHelper.doInTransaction(buildGraphCallback);
    }

    public void xtest1() throws Exception
    {
        testConcurrent();
    }

    public void xtest2() throws Exception
    {
        testConcurrent();
    }

    public void xtest3() throws Exception
    {
        testConcurrent();
    }

    public void xtest4() throws Exception
    {
        testConcurrent();
    }

    public void xtest5() throws Exception
    {
        testConcurrent();
    }

    public void xtest6() throws Exception
    {
        testConcurrent();
    }

    public void xtest7() throws Exception
    {
        testConcurrent();
    }

    public void xtest8() throws Exception
    {
        testConcurrent();
    }

    public void xtest9() throws Exception
    {
        testConcurrent();
    }

    public void xtest10() throws Exception
    {
        testConcurrent();
    }

    public void testConcurrent() throws Exception
    {
        luceneFTS.pause();
        // TODO: LUCENE UPDATE ISSUE fix commit lock time out
        // IndexWriter.COMMIT_LOCK_TIMEOUT = 100000;

        Map<QName, ChildAssociationRef> assocRefs = commitNodeGraph();
        Thread runner = null;

        for (int i = 0; i < COUNT; i++)
        {
            runner = new Nester("Concurrent-" + i, runner, REPEATS);
        }
        if (runner != null)
        {
            runner.start();

            try
            {
                runner.join();
                System.out.println("Query thread has waited for " + runner.getName());
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        /*
         * Builds a graph of child associations as follows:
         * <pre>
         * Level 0:     root
         * Level 1:     root_p_n1   root_p_n2
         * Level 2:     n1_p_n3     n2_p_n4     n1_n4       n2_p_n5     n1_n8
         * Level 3:     n3_p_n6     n4_n6       n5_p_n7
         * Level 4:     n6_p_n8     n7_n8
         * </pre>
         */
        RetryingTransactionCallback<Object> testCallback = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                // There are two nodes at the base level in each test
                assertEquals(2 * ((COUNT * REPEATS) + 1), nodeService.getChildAssocs(rootNodeRef).size());

                SearchService searcher = (SearchService) ctx.getBean(ServiceRegistry.SEARCH_SERVICE.getLocalName());
                assertEquals(
                        2 * ((COUNT * REPEATS) + 1),
                        searcher.selectNodes(rootNodeRef, "/*", null, getNamespacePrefixReolsver(""), false).size());
                ResultSet results = null;

                results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*\"");
                // n6 has root aspect - there are three things at the root level in the index
                assertEquals(3 * ((COUNT * REPEATS) + 1), results.length());
                results.close();

                results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*\"");
                // n6 has root aspect - there are three things at the root level in the index
                assertEquals(4 * ((COUNT * REPEATS) + 1), results.length());
                results.close();

                results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*/*\"");
                // n6 has root aspect - there are three things at the root level in the index
                assertEquals(2 * ((COUNT * REPEATS) + 1), results.length());
                results.close();

                results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*/*/*\"");
                // n6 has root aspect - there are three things at the root level in the index
                assertEquals(1 * ((COUNT * REPEATS) + 1), results.length());
                results.close();

                results = searcher.query(rootNodeRef.getStoreRef(), "lucene", "PATH:\"/*/*/*/*/*\"");
                // n6 has root aspect - there are three things at the root level in the index
                assertEquals(0 * ((COUNT * REPEATS) + 1), results.length());
                results.close();

                return null;
            }
        };
        retryingTransactionHelper.doInTransaction(testCallback);
    }

    /**
     * Daemon thread
     */
    private class Nester extends Thread
    {
        Thread waiter;

        int repeats;

        Nester(String name, Thread waiter, int repeats)
        {
            super(name);
            this.setDaemon(true);
            this.waiter = waiter;
            this.repeats = repeats;
        }

        public void run()
        {
            authenticationComponent.setSystemUserAsCurrentUser();

            if (waiter != null)
            {
                System.out.println("Starting " + waiter.getName());
                waiter.start();
            }
            try
            {
                System.out.println("Start " + this.getName());
                for (int i = 0; i < repeats; i++)
                {
                    Map<QName, ChildAssociationRef> assocRefs = commitNodeGraph();
                    System.out.println(" " + this.getName() + " " + i);
                }
                System.out.println("End " + this.getName());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            if (waiter != null)
            {
                try
                {
                    waiter.join();
                    System.out.println("Thread "
                            + this.getName() + " has waited for " + (waiter == null ? "null" : waiter.getName()));
                }
                catch (InterruptedException e)
                {
                    System.err.println(e);
                }
            }
        }

    }

    private NamespacePrefixResolver getNamespacePrefixReolsver(String defaultURI)
    {
        DynamicNamespacePrefixResolver nspr = new DynamicNamespacePrefixResolver(null);
        nspr.registerNamespace(NamespaceService.SYSTEM_MODEL_PREFIX, NamespaceService.SYSTEM_MODEL_1_0_URI);
        nspr.registerNamespace(NamespaceService.CONTENT_MODEL_PREFIX, NamespaceService.CONTENT_MODEL_1_0_URI);
        nspr.registerNamespace(NamespaceService.APP_MODEL_PREFIX, NamespaceService.APP_MODEL_1_0_URI);
        nspr.registerNamespace("namespace", "namespace");
        nspr.registerNamespace(NamespaceService.DEFAULT_PREFIX, defaultURI);
        return nspr;
    }
}
