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
package org.alfresco.repo.search;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.alfresco.repo.node.BaseNodeServiceTest;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryParameterDefinition;
import org.alfresco.service.namespace.DynamicNamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

/**
 * @see org.alfresco.repo.search.SearcherComponent
 * 
 * @author Derek Hulley
 */
public class SearcherComponentTest extends TestCase
{
    private static ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
    
    private ServiceRegistry serviceRegistry;
    private TransactionService transactionService;
    private DictionaryService dictionaryService;
    private SearcherComponent searcher;
    private NodeService nodeService;
    
    private NodeRef rootNodeRef;
    private UserTransaction txn;
    
    public void setUp() throws Exception
    {
        serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        transactionService = serviceRegistry.getTransactionService();
        dictionaryService = BaseNodeServiceTest.loadModel(ctx);
        nodeService = serviceRegistry.getNodeService();
        // get the indexer and searcher factory
        IndexerAndSearcher indexerAndSearcher = (IndexerAndSearcher) ctx.getBean("indexerAndSearcherFactory");
        searcher = new SearcherComponent();
        searcher.setIndexerAndSearcherFactory(indexerAndSearcher);
        // create a test workspace
        StoreRef storeRef = nodeService.createStore(
                StoreRef.PROTOCOL_WORKSPACE,
                getName() + "_" + System.currentTimeMillis());
        rootNodeRef = nodeService.getRootNode(storeRef);
        // begin a transaction
        txn = transactionService.getUserTransaction();
        txn.begin();
    }
    
    public void tearDown() throws Exception
    {
        if (txn.getStatus() == Status.STATUS_ACTIVE)
        {
            txn.rollback();
        }
    }

    public void testNodeXPath() throws Exception
    {
        Map<QName, ChildAssociationRef> assocRefs = BaseNodeServiceTest.buildNodeGraph(nodeService, rootNodeRef);
        QName qname = QName.createQName(BaseNodeServiceTest.NAMESPACE, "n2_p_n4");
        
        NodeServiceXPath xpath;
        String xpathStr;
        QueryParameterDefImpl paramDef;
        List list;
        
        DynamicNamespacePrefixResolver namespacePrefixResolver = new DynamicNamespacePrefixResolver(null);
        namespacePrefixResolver.addDynamicNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        // create the document navigator
        DocumentNavigator documentNavigator = new DocumentNavigator(
                dictionaryService,
                nodeService,
                searcher,
                namespacePrefixResolver,
                false);
        
        xpath = new NodeServiceXPath("//.[@test:animal='monkey']", documentNavigator, null);
        xpath.addNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
        assertEquals(1, list.size());
      
        xpath = new NodeServiceXPath("*", documentNavigator, null);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
        assertEquals(2, list.size());
        
        xpath = new NodeServiceXPath("*/*", documentNavigator, null);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
        assertEquals(4, list.size());
        
        xpath = new NodeServiceXPath("*/*/*", documentNavigator, null);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
        assertEquals(3, list.size());
        
        xpath = new NodeServiceXPath("*/*/*/*", documentNavigator, null);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
        assertEquals(2, list.size());
        
        xpath = new NodeServiceXPath("*/*/*/*/..", documentNavigator, null);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
        assertEquals(2, list.size());
        
        xpath = new NodeServiceXPath("*//.", documentNavigator, null);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
//        assertEquals(11, list.size());
        assertEquals(13, list.size());   // 13 unique paths through the graph - duplicates not being removed
        
        xpathStr = "test:root_p_n1";
        xpath = new NodeServiceXPath(xpathStr, documentNavigator, null);
        xpath.addNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
        assertEquals(1, list.size());
        
        xpathStr = "*//.[@test:animal]";
        xpath = new NodeServiceXPath(xpathStr, documentNavigator, null);
        xpath.addNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
        assertEquals(1, list.size());
        
        xpathStr = "*//.[@test:animal='monkey']";
        xpath = new NodeServiceXPath(xpathStr, documentNavigator, null);
        xpath.addNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
        assertEquals(1, list.size());
        
        xpathStr = "//.[@test:animal='monkey']";
        xpath = new NodeServiceXPath(xpathStr, documentNavigator, null);
        xpath.addNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
        assertEquals(1, list.size());
        
        paramDef = new QueryParameterDefImpl(
                QName.createQName("test:test", namespacePrefixResolver),
                dictionaryService.getDataType(DataTypeDefinition.TEXT),
                true,
                "monkey");
        xpathStr = "//.[@test:animal=$test:test]";
        xpath = new NodeServiceXPath(xpathStr, documentNavigator, new QueryParameterDefinition[]{paramDef});
        xpath.addNamespace(BaseNodeServiceTest.TEST_PREFIX,   BaseNodeServiceTest.NAMESPACE);
        list = xpath.selectNodes(new ChildAssociationRef(null, null, null, rootNodeRef));
        assertEquals(1, list.size());
        
        xpath = new NodeServiceXPath(".", documentNavigator, null);
        list = xpath.selectNodes(assocRefs.get(qname));
        assertEquals(1, list.size());
        
        xpath = new NodeServiceXPath("..", documentNavigator, null);
        list = xpath.selectNodes(assocRefs.get(qname));
        assertEquals(1, list.size());
        
        // follow all parent links now
        documentNavigator.setFollowAllParentLinks(true);
        
        xpath = new NodeServiceXPath("..", documentNavigator, null);
        list = xpath.selectNodes(assocRefs.get(qname));
        assertEquals(2, list.size());
        
        xpathStr = "//@test:animal";
        xpath = new NodeServiceXPath(xpathStr, documentNavigator, null);
        xpath.addNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        list = xpath.selectNodes(assocRefs.get(qname));
        assertEquals(1, list.size());
        assertTrue(list.get(0) instanceof DocumentNavigator.Property);
        
        xpathStr = "//@test:reference";
        xpath = new NodeServiceXPath(xpathStr, documentNavigator, null);
        xpath.addNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        list = xpath.selectNodes(assocRefs.get(qname));
        assertEquals(1, list.size());
        
        // stop following parent links
        documentNavigator.setFollowAllParentLinks(false);
        
        xpathStr = "deref(/test:root_p_n1/test:n1_p_n3/@test:reference, '')";
        xpath = new NodeServiceXPath(xpathStr, documentNavigator, null);
        xpath.addNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        list = xpath.selectNodes(assocRefs.get(qname));
        assertEquals(1, list.size());
        
        // test 'subtypeOf' function
        paramDef = new QueryParameterDefImpl(
                QName.createQName("test:type", namespacePrefixResolver),
                dictionaryService.getDataType(DataTypeDefinition.QNAME),
                true,
                BaseNodeServiceTest.TYPE_QNAME_TEST_CONTENT.toString());
        xpathStr = "//.[subtypeOf($test:type)]";
        xpath = new NodeServiceXPath(xpathStr, documentNavigator, new QueryParameterDefinition[]{paramDef});
        xpath.addNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        list = xpath.selectNodes(assocRefs.get(qname));
        assertEquals(2, list.size());   // 2 distinct paths to node n8, which is of type content

        xpath = new NodeServiceXPath("/", documentNavigator, null);
        xpath.addNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        list = xpath.selectNodes(assocRefs.get(qname));
        assertEquals(1, list.size());
    }
    
    
    public void testSelectAPI() throws Exception
    {
        Map<QName, ChildAssociationRef> assocRefs = BaseNodeServiceTest.buildNodeGraph(nodeService, rootNodeRef);
        NodeRef n6Ref = assocRefs.get(QName.createQName(BaseNodeServiceTest.NAMESPACE, "n3_p_n6")).getChildRef();
        
        DynamicNamespacePrefixResolver namespacePrefixResolver = new DynamicNamespacePrefixResolver(null);
        namespacePrefixResolver.addDynamicNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        
        List<NodeRef> answer =  searcher.selectNodes(rootNodeRef, "/test:root_p_n1/test:n1_p_n3/*", null, namespacePrefixResolver, false);
        assertEquals(1, answer.size());
        assertTrue(answer.contains(n6Ref));
        
        //List<ChildAssocRef> 
        answer =  searcher.selectNodes(rootNodeRef, "*", null, namespacePrefixResolver, false);
        assertEquals(2, answer.size());
        
        List<Serializable> attributes = searcher.selectProperties(rootNodeRef, "//@test:animal", null, namespacePrefixResolver, false);
        assertEquals(1, attributes.size());
    }
    
    /**
     * Tests the <b>like</b> and <b>contains</b> functions (FTS functions) within a currently executing
     * transaction
     */
    public void testLikeAndContains() throws Exception
    {
        Map<QName, ChildAssociationRef> assocRefs = BaseNodeServiceTest.buildNodeGraph(nodeService, rootNodeRef);
        // commit the node graph
        txn.commit();
        
        txn = transactionService.getUserTransaction();
        txn.begin();
        
        DynamicNamespacePrefixResolver namespacePrefixResolver = new DynamicNamespacePrefixResolver(null);
        namespacePrefixResolver.addDynamicNamespace(BaseNodeServiceTest.TEST_PREFIX, BaseNodeServiceTest.NAMESPACE);
        
        List<NodeRef> answer =  searcher.selectNodes(
                rootNodeRef,
                "//*[like(@test:animal, 'm__k%', false)]",
                null,
                namespacePrefixResolver, false);
        assertEquals(1, answer.size());
        
        answer =  searcher.selectNodes(
                rootNodeRef,
                "//*[like(@test:animal, 'M__K%', false)]",
                null,
                namespacePrefixResolver, false);
        assertEquals(1, answer.size());
        
        answer =  searcher.selectNodes(
                rootNodeRef,
                "//*[like(@test:UPPERANIMAL, 'm__k%', false)]",
                null,
                namespacePrefixResolver, false);
        assertEquals(1, answer.size());
        
        answer =  searcher.selectNodes(
                rootNodeRef,
                "//*[like(@test:UPPERANIMAL, 'M__K%', false)]",
                null,
                namespacePrefixResolver, false);
        assertEquals(1, answer.size());
        
        answer =  searcher.selectNodes(rootNodeRef, "//*[contains('monkey')]", null, namespacePrefixResolver, false);
        assertEquals(1, answer.size());
        
        answer =  searcher.selectNodes(rootNodeRef, "//*[contains('MONKEY')]", null, namespacePrefixResolver, false);
        assertEquals(1, answer.size());
        
        answer =  searcher.selectNodes(rootNodeRef, "//*[contains(lower-case('MONKEY'))]", null, namespacePrefixResolver, false);
        assertEquals(1, answer.size());

        // select the monkey node in the second level
        QueryParameterDefinition[] paramDefs = new QueryParameterDefinition[2];
        paramDefs[0] = new QueryParameterDefImpl(
                QName.createQName("test:animal", namespacePrefixResolver),
                dictionaryService.getDataType(DataTypeDefinition.TEXT),
                true,
                "monkey%");
        paramDefs[1] = new QueryParameterDefImpl(
                QName.createQName("test:type", namespacePrefixResolver),
                dictionaryService.getDataType(DataTypeDefinition.TEXT),
                true,
                BaseNodeServiceTest.TYPE_QNAME_TEST_CONTENT.toString());
        answer = searcher.selectNodes(
                rootNodeRef,
                "./*/*[like(@test:animal, $test:animal, false) or subtypeOf($test:type)]",
                paramDefs,
                namespacePrefixResolver,
                false);
        assertEquals(1, answer.size());
        
        // select the monkey node again, but use the first level as the starting poing
        NodeRef n1Ref = assocRefs.get(QName.createQName(BaseNodeServiceTest.NAMESPACE, "root_p_n1")).getChildRef();
        NodeRef n3Ref = assocRefs.get(QName.createQName(BaseNodeServiceTest.NAMESPACE, "n1_p_n3")).getChildRef();
        // fist time go too deep
        answer = searcher.selectNodes(
                n1Ref,
                "./*/*[like(@test:animal, $test:animal, false) or subtypeOf($test:type)]",
                paramDefs,
                namespacePrefixResolver,
                false);
        assertEquals(0, answer.size());
        // second time get it right
        answer = searcher.selectNodes(
                n1Ref,
                "./*[like(@test:animal, $test:animal, false) or subtypeOf($test:type)]",
                paramDefs,
                namespacePrefixResolver,
                false);
        assertEquals(1, answer.size());
        assertFalse("Incorrect result: search root node pulled back", answer.contains(n1Ref));
        assertTrue("Incorrect result: incorrect node retrieved", answer.contains(n3Ref));
   }
}
