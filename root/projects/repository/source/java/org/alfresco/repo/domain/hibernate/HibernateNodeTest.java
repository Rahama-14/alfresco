/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.repo.domain.hibernate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.ChildAssoc;
import org.alfresco.repo.domain.NamespaceEntity;
import org.alfresco.repo.domain.Node;
import org.alfresco.repo.domain.NodeKey;
import org.alfresco.repo.domain.NodeStatus;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.domain.QNameDAO;
import org.alfresco.repo.domain.QNameEntity;
import org.alfresco.repo.domain.Server;
import org.alfresco.repo.domain.Store;
import org.alfresco.repo.domain.StoreKey;
import org.alfresco.repo.domain.Transaction;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.BaseSpringTest;
import org.alfresco.util.GUID;
import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.GenericJDBCException;

/**
 * Test persistence and retrieval of Hibernate-specific implementations of the
 * {@link org.alfresco.repo.domain.Node} interface
 * 
 * @author Derek Hulley
 */
@SuppressWarnings("unused")
public class HibernateNodeTest extends BaseSpringTest
{
    private static final String TEST_NAMESPACE = "http://www.alfresco.org/test/HibernateNodeTest";
    
    private Store store;
    private Server server;
    private Transaction transaction;
    private NamespaceEntity cmNamespaceEntity;
    private NamespaceEntity emptyNamespaceEntity;
    private QNameEntity cmObjectQNameEntity;
    private QNameEntity containerQNameEntity;
    private QNameEntity contentQNameEntity;
    private QNameEntity type1QNameEntity;
    private QNameEntity type2QNameEntity;
    private QNameEntity type3QNameEntity;
    private QNameEntity aspect1QNameEntity;
    private QNameEntity aspect2QNameEntity;
    private QNameEntity aspect3QNameEntity;
    private QNameEntity aspect4QNameEntity;
    private QNameEntity prop1QNameEntity;
    private QNameEntity propNameQNameEntity;
    private QNameEntity propAuthorQNameEntity;
    private QNameEntity propArchivedByQNameEntity;
    private QNameEntity aspectAuditableQNameEntity;
    
    public HibernateNodeTest()
    {
    }
    
    protected void onSetUpInTransaction() throws Exception
    {
        store = new StoreImpl();
		StoreKey storeKey = new StoreKey(StoreRef.PROTOCOL_WORKSPACE,
                "TestWorkspace@" + getName() + " - " + System.currentTimeMillis());
		store.setKey(storeKey);
        // persist so that it is present in the hibernate cache
        getSession().save(store);
        
        server = (Server) getSession().get(ServerImpl.class, new Long(1));
        if (server == null)
        {
            server = new ServerImpl();
            server.setIpAddress("" + "i_" + System.currentTimeMillis());
            getSession().save(server);
        }
        transaction = new TransactionImpl();
        transaction.setServer(server);
        transaction.setChangeTxnId(AlfrescoTransactionSupport.getTransactionId());
        getSession().save(transaction);
        
        // Create a QName for node type
        QNameDAO qnameDAO = (QNameDAO) applicationContext.getBean("qnameDAO");
        
        cmNamespaceEntity = qnameDAO.getOrCreateNamespaceEntity(NamespaceService.CONTENT_MODEL_1_0_URI);
        emptyNamespaceEntity = qnameDAO.getOrCreateNamespaceEntity("");
        cmObjectQNameEntity = qnameDAO.getOrCreateQNameEntity(ContentModel.TYPE_CMOBJECT);
        containerQNameEntity = qnameDAO.getOrCreateQNameEntity(ContentModel.TYPE_CONTAINER);
        contentQNameEntity = qnameDAO.getOrCreateQNameEntity(ContentModel.TYPE_CONTENT);
        type1QNameEntity = qnameDAO.getOrCreateQNameEntity(QName.createQName(TEST_NAMESPACE, "type1"));
        type2QNameEntity = qnameDAO.getOrCreateQNameEntity(QName.createQName(TEST_NAMESPACE, "type2"));
        type3QNameEntity = qnameDAO.getOrCreateQNameEntity(QName.createQName(TEST_NAMESPACE, "type3"));
        aspect1QNameEntity = qnameDAO.getOrCreateQNameEntity(QName.createQName(TEST_NAMESPACE, "aspect1"));
        aspect2QNameEntity = qnameDAO.getOrCreateQNameEntity(QName.createQName(TEST_NAMESPACE, "aspect2"));
        aspect3QNameEntity = qnameDAO.getOrCreateQNameEntity(QName.createQName(TEST_NAMESPACE, "aspect3"));
        aspect4QNameEntity = qnameDAO.getOrCreateQNameEntity(QName.createQName(TEST_NAMESPACE, "aspect4"));
        prop1QNameEntity = qnameDAO.getOrCreateQNameEntity(QName.createQName(TEST_NAMESPACE, "prop1"));
        propNameQNameEntity = qnameDAO.getOrCreateQNameEntity(ContentModel.PROP_NAME);
        propAuthorQNameEntity = qnameDAO.getOrCreateQNameEntity(ContentModel.PROP_AUTHOR);
        propArchivedByQNameEntity = qnameDAO.getOrCreateQNameEntity(ContentModel.PROP_ARCHIVED_BY);
        aspectAuditableQNameEntity = qnameDAO.getOrCreateQNameEntity(ContentModel.ASPECT_AUDITABLE);
    }
    
    protected void onTearDownInTransaction()
    {
        // force a flush to ensure that the database updates succeed
        getSession().flush();
        getSession().clear();
    }

    public void testSetUp() throws Exception
    {
        assertNotNull("Workspace not initialised", store);
    }
    
	public void testGetStore() throws Exception
	{
        // create a new Node
        Node node = new NodeImpl();
        node.setStore(store);
        node.setUuid(GUID.generate());
        node.setTypeQName(containerQNameEntity);

        // now it should work
		Serializable id = getSession().save(node);

        // throw the reference away and get the a new one for the id
        node = (Node) getSession().load(NodeImpl.class, id);
        assertNotNull("Node not found", node);
		// check that the store has been loaded
		Store loadedStore = node.getStore();
		assertNotNull("Store not present on node", loadedStore);
		assertEquals("Incorrect store key", store, loadedStore);
	}
    
    public void testNodeStatus()
    {
        NodeKey key = new NodeKey(store.getKey(), "AAA");
        // create the node status
        NodeStatus nodeStatus = new NodeStatusImpl();
        nodeStatus.setKey(key);
        nodeStatus.setTransaction(transaction);
        getSession().save(nodeStatus);
        
        // create a new Node
        Node node = new NodeImpl();
        node.setStore(store);
        node.setUuid(GUID.generate());
        node.setTypeQName(containerQNameEntity);
        Serializable nodeId = getSession().save(node);

        // This should all be fine.  The node does not HAVE to have a status.
        flushAndClear();

        // set the node
        nodeStatus = (NodeStatus) getSession().get(NodeStatusImpl.class, key);
        nodeStatus.setNode(node);
        flushAndClear();

        // is the node retrievable?
        nodeStatus = (NodeStatus) getSession().get(NodeStatusImpl.class, key);
        node = nodeStatus.getNode();
        assertNotNull("Node was not attached to status", node);
        // change the values
        transaction.setChangeTxnId("txn:456");
        // delete the node
        getSession().delete(node);
        
        try
        {
            flushAndClear();
            fail("Node status may not refer to non-existent node");
        }
        catch(ConstraintViolationException e)
        {
            // expected
        }
        catch(GenericJDBCException e)
        {
           //  Sybase
            // expected
        }
        // Just clear out any pending changes
        getSession().clear();
    }

    /**
     * Check that properties can be persisted and retrieved
     */
    public void testProperties() throws Exception
    {
        // create a new Node
        Node node = new NodeImpl();
        node.setStore(store);
        node.setUuid(GUID.generate());
        node.setTypeQName(containerQNameEntity);
        // give it a property map
        Map<Long, PropertyValue> propertyMap = new HashMap<Long, PropertyValue>(5);
        QName propertyQName = QName.createQName("{}A");
        PropertyValue propertyValue = new PropertyValue(DataTypeDefinition.TEXT, "AAA");
        propertyMap.put(prop1QNameEntity.getId(), propertyValue);
        node.getProperties().putAll(propertyMap);
        // persist it
        Serializable id = getSession().save(node);

        // throw the reference away and get the a new one for the id
        node = (Node) getSession().load(NodeImpl.class, id);
        assertNotNull("Node not found", node);
        // extract the Map
        propertyMap = node.getProperties();
        assertNotNull("Map not persisted", propertyMap);
        // ensure that the value is present
        assertNotNull("Property value not present in map", propertyMap.get(prop1QNameEntity.getId()));
    }

    /**
     * Check that aspect qnames can be added and removed from a node and that they
     * are persisted correctly 
     */
    public void testAspects() throws Exception
    {
        // make a real node
        Node node = new NodeImpl();
        node.setStore(store);
        node.setUuid(GUID.generate());
        node.setTypeQName(cmObjectQNameEntity);
        
        // add some aspects
        Set<Long> aspects = node.getAspects();
        aspects.add(aspect1QNameEntity.getId());
        aspects.add(aspect2QNameEntity.getId());
        aspects.add(aspect3QNameEntity.getId());
        aspects.add(aspect4QNameEntity.getId());
        assertFalse("Set did not eliminate duplicate aspect qname", aspects.add(aspect4QNameEntity.getId()));
        
        // persist
        Serializable id = getSession().save(node);
        
        // flush and clear
        flushAndClear();
        
        // get node and check aspects
        node = (Node) getSession().get(NodeImpl.class, id);
        assertNotNull("Node not persisted", node);
        aspects = node.getAspects();
        assertEquals("Not all aspects persisted", 4, aspects.size());
    }
    
    public void testChildAssoc() throws Exception
    {
        // make a content node
        Node contentNode = new NodeImpl();
        contentNode.setStore(store);
        contentNode.setUuid(GUID.generate());
        contentNode.setTypeQName(contentQNameEntity);
        Serializable contentNodeId = getSession().save(contentNode);

        // make a container node
        Node containerNode = new NodeImpl();
        containerNode.setStore(store);
        containerNode.setUuid(GUID.generate());
        containerNode.setTypeQName(containerQNameEntity);
        Serializable containerNodeId = getSession().save(containerNode);
        // create an association to the content
        ChildAssoc assoc1 = new ChildAssocImpl();
        assoc1.setIsPrimary(true);
        assoc1.setTypeQName(type1QNameEntity);
        assoc1.setQnameNamespace(emptyNamespaceEntity);
        assoc1.setQnameLocalName("number1");
        assoc1.setChildNodeName("number1");
        assoc1.setChildNodeNameCrc(1);
        assoc1.buildAssociation(containerNode, contentNode);
        getSession().save(assoc1);

        // make another association between the same two parent and child nodes
        ChildAssoc assoc2 = new ChildAssocImpl();
        assoc2.setIsPrimary(true);
        assoc2.setTypeQName(type2QNameEntity);
        assoc2.setQnameNamespace(emptyNamespaceEntity);
        assoc2.setQnameLocalName("number2");
        assoc2.setChildNodeName("number2");
        assoc2.setChildNodeNameCrc(2);
        assoc2.buildAssociation(containerNode, contentNode);
        getSession().save(assoc2);
        
        assertFalse("Hashcode incorrent", assoc2.hashCode() == 0);
        assertNotSame("Assoc equals failure", assoc1, assoc2);

        // reload the container
        containerNode = (Node) getSession().get(NodeImpl.class, containerNodeId);
        assertNotNull("Node not found", containerNode);

        // check that we can traverse the association from the child
        Collection<ChildAssoc> parentAssocs = getParentAssocs(contentNode);
        assertEquals("Expected exactly 2 parent assocs", 2, parentAssocs.size());
        parentAssocs = new HashSet<ChildAssoc>(parentAssocs);
        for (ChildAssoc assoc : parentAssocs)
        {
            // maintain inverse assoc sets
            assoc.removeAssociation();
            // remove the assoc
            getSession().delete(assoc);
        }
        
        // check that the child now has zero parents
        parentAssocs = getParentAssocs(contentNode);
        assertEquals("Expected exactly 0 parent assocs", 0, parentAssocs.size());
    }
    
    @SuppressWarnings("unchecked")
    private List<ChildAssoc> getParentAssocs(final Node childNode)
    {
        Query query = getSession()
                .createQuery(
                    "select assoc from org.alfresco.repo.domain.hibernate.ChildAssocImpl as assoc " +
                    "where " +
                    "   assoc.child.id = :childId " +
                    "order by " +
                    "assoc.index, assoc.id")
                .setLong("childId", childNode.getId());
        List<ChildAssoc> parentAssocs = query.list();
        return parentAssocs;
    }
    
    /**
     * Allows tracing of L2 cache
     */
    public void testCaching() throws Exception
    {
        // make a node
        Node node = new NodeImpl();
        node.setStore(store);
        node.setUuid(GUID.generate());
        node.setTypeQName(contentQNameEntity);
        Serializable nodeId = getSession().save(node);
        
        // add some aspects to the node
        Set<Long> aspects = node.getAspects();
        aspects.add(aspectAuditableQNameEntity.getId());
        
        // add some properties
        Map<Long, PropertyValue> properties = node.getProperties();
        properties.put(propNameQNameEntity.getId(), new PropertyValue(DataTypeDefinition.TEXT, "ABC"));
        
        // check that the session hands back the same instance
        Node checkNode = (Node) getSession().get(NodeImpl.class, nodeId);
        assertNotNull(checkNode);
        assertTrue("Node retrieved was not same instance", checkNode == node);
        
        Set<Long> checkAspects = checkNode.getAspects();
        assertTrue("Aspect set retrieved was not the same instance", checkAspects == aspects);
        assertEquals("Incorrect number of aspects", 1, checkAspects.size());
        Long checkQNameId = (Long) checkAspects.toArray()[0];
        assertEquals("QName retrieved was not the same instance", aspectAuditableQNameEntity.getId(), checkQNameId);
        
        Map<Long, PropertyValue> checkProperties = checkNode.getProperties();
        assertTrue("Propery map retrieved was not the same instance", checkProperties == properties);
        assertTrue("Property not found", checkProperties.containsKey(propNameQNameEntity.getId()));

        flushAndClear();
        // commit the transaction
        setComplete();
        endTransaction();
        
        TransactionService transactionService = (TransactionService) applicationContext.getBean("transactionComponent");
        UserTransaction txn = transactionService.getUserTransaction();
        try
        {
            txn.begin();
            
            // check that the L2 cache hands back the same instance
            checkNode = (Node) getSession().get(NodeImpl.class, nodeId);
            assertNotNull(checkNode);
            checkAspects = checkNode.getAspects();
    
            txn.commit();
        }
        catch (Throwable e)
        {
            txn.rollback();
        }
    }

    /**
     * This test demonstrates how entities are effectively rendered useless when the session
     * is cleared.  The object itself will appear to behave properly, but it is only when
     * it comes to retrieving the associated values that one discovers that they were not
     * persisted at all.  Uncomment at <b>UNCOMMENT FOR FAILURE</b> to see the effect in action.
     */
    public void testPostCommitClearIssue() throws Exception
    {
        // commit the transaction
        setComplete();
        endTransaction();
        // Start a transaction explicitly
        TransactionService transactionService = (TransactionService) applicationContext.getBean("transactionComponent");
        UserTransaction txn = transactionService.getUserTransaction();
        
        // We need a listener
        TestPostCommitClearIssueHelper listener = new TestPostCommitClearIssueHelper();
        try
        {
            txn.begin();
            
            // Bind the listener
            AlfrescoTransactionSupport.bindListener(listener);
            
            // Bind a list of node IDs into the transaction
            List<Long> nodeIds = new ArrayList<Long>(100);
            AlfrescoTransactionSupport.bindResource("node_ids", nodeIds);
            // Bind the session in, too
            Session session = getSession();
            AlfrescoTransactionSupport.bindResource("session", session);
            
            // Make a whole lot of nodes with aspects and properties
            for (int i = 0; i < 100; i++)
            {
                // make a node
                Node node = new NodeImpl();
                node.setStore(store);
                node.setUuid(GUID.generate());
                node.setTypeQName(contentQNameEntity);
                Long nodeId = (Long) getSession().save(node);
                
                // Record the ID
                nodeIds.add(nodeId);
                
                // Now flush and clear
                /* UNCOMMENT FOR FAILURE */
                /* flushAndClear(); */

                // add some aspects to the node
                Set<Long> aspects = node.getAspects();
                aspects.add(aspectAuditableQNameEntity.getId());
                
                // add some properties
                Map<Long, PropertyValue> properties = node.getProperties();
                properties.put(propNameQNameEntity.getId(), new PropertyValue(DataTypeDefinition.TEXT, "ABC"));
            }
            // Commit the transaction
            txn.commit();
        }
        catch (Throwable e)
        {
            try { txn.rollback(); } catch (Throwable ee) {}
        }
        // Did the listener find any issues?
        if (listener.err != null)
        {
            fail(listener.err);
        }
    }
    /** Helper class to test entities during transaction wind-down */
    private class TestPostCommitClearIssueHelper extends TransactionListenerAdapter
    {
        public String err = null;
        @SuppressWarnings("unchecked")
        @Override
        public void beforeCommit(boolean readOnly)
        {
            // Get the session
            Session session = (Session) AlfrescoTransactionSupport.getResource("session");
            // Get the node IDs
            List<Long> nodeIds = (List<Long>) AlfrescoTransactionSupport.getResource("node_ids");
            // Check each node for the aspects and properties required
            int incorrectAspectCount = 0;
            int incorrectPropertyCount = 0;
            for (Long nodeId : nodeIds)
            {
                Node node = (Node) session.get(NodeImpl.class, nodeId);
                Set<Long> aspects = node.getAspects();
                Map<Long, PropertyValue> properties = node.getProperties();
                if (!aspects.contains(aspectAuditableQNameEntity.getId()))
                {
                    // Missing the aspect
                    incorrectAspectCount++;
                }
                if (!properties.containsKey(propNameQNameEntity.getId()))
                {
                    // Missing property
                    incorrectPropertyCount++;
                }
            }
            // What is the outcome?
            if (incorrectAspectCount > 0 || incorrectPropertyCount > 0)
            {
                this.err =
                    "Checked " + nodeIds.size() + " nodes and found: \n" +
                    "   " + incorrectAspectCount + " missing aspects and \n" +
                    "   " + incorrectPropertyCount + " missing properties.";
                
            }
            // Force a rollback anyway, just to stop an explosion of data
            throw new RuntimeException("ROLLBACK");
        }
    }
    
    /**
     * Create some simple parent-child relationships and flush them.  Then read them back in without
     * using the L2 cache.
     */
    public void testQueryJoins() throws Exception
    {
        getSession().setCacheMode(CacheMode.IGNORE);
        
        // make a container node
        Node containerNode = new NodeImpl();
        containerNode.setStore(store);
        containerNode.setUuid(GUID.generate());
        containerNode.setTypeQName(containerQNameEntity);
        containerNode.getProperties().put(propAuthorQNameEntity.getId(), new PropertyValue(DataTypeDefinition.TEXT, "ABC"));
        containerNode.getProperties().put(propArchivedByQNameEntity.getId(), new PropertyValue(DataTypeDefinition.TEXT, "ABC"));
        containerNode.getAspects().add(aspectAuditableQNameEntity.getId());
        Serializable containerNodeId = getSession().save(containerNode);
        NodeKey containerNodeKey = new NodeKey(containerNode.getNodeRef());
        NodeStatus containerNodeStatus = new NodeStatusImpl();
        containerNodeStatus.setKey(containerNodeKey);
        containerNodeStatus.setNode(containerNode);
        containerNodeStatus.setTransaction(transaction);
        getSession().save(containerNodeStatus);
        // make content node 1
        Node contentNode1 = new NodeImpl();
        contentNode1.setStore(store);
        contentNode1.setUuid(GUID.generate());
        contentNode1.setTypeQName(contentQNameEntity);
        contentNode1.getProperties().put(propAuthorQNameEntity.getId(), new PropertyValue(DataTypeDefinition.TEXT, "ABC"));
        contentNode1.getProperties().put(propArchivedByQNameEntity.getId(), new PropertyValue(DataTypeDefinition.TEXT, "ABC"));
        contentNode1.getAspects().add(aspectAuditableQNameEntity.getId());
        Serializable contentNode1Id = getSession().save(contentNode1);
        NodeKey contentNodeKey1 = new NodeKey(contentNode1.getNodeRef());
        NodeStatus contentNodeStatus1 = new NodeStatusImpl();
        contentNodeStatus1.setKey(contentNodeKey1);
        contentNodeStatus1.setNode(contentNode1);
        contentNodeStatus1.setTransaction(transaction);
        getSession().save(contentNodeStatus1);
        // make content node 2
        Node contentNode2 = new NodeImpl();
        contentNode2.setStore(store);
        contentNode2.setUuid(GUID.generate());
        contentNode2.setTypeQName(contentQNameEntity);
        Serializable contentNode2Id = getSession().save(contentNode2);
        contentNode2.getProperties().put(propAuthorQNameEntity.getId(), new PropertyValue(DataTypeDefinition.TEXT, "ABC"));
        contentNode2.getProperties().put(propArchivedByQNameEntity.getId(), new PropertyValue(DataTypeDefinition.TEXT, "ABC"));
        contentNode2.getAspects().add(aspectAuditableQNameEntity.getId());
        NodeKey contentNodeKey2 = new NodeKey(contentNode2.getNodeRef());
        NodeStatus contentNodeStatus2 = new NodeStatusImpl();
        contentNodeStatus2.setKey(contentNodeKey2);
        contentNodeStatus2.setNode(contentNode2);
        contentNodeStatus2.setTransaction(transaction);
        getSession().save(contentNodeStatus2);
        // create an association to content 1
        ChildAssoc assoc1 = new ChildAssocImpl();
        assoc1.setIsPrimary(true);
        assoc1.setTypeQName(type1QNameEntity);
        assoc1.setQnameNamespace(emptyNamespaceEntity);
        assoc1.setQnameLocalName("number1");
        assoc1.setChildNodeName("number1");
        assoc1.setChildNodeNameCrc(1);
        assoc1.buildAssociation(containerNode, contentNode1);
        getSession().save(assoc1);
        // create an association to content 2
        ChildAssoc assoc2 = new ChildAssocImpl();
        assoc2.setIsPrimary(true);
        assoc2.setTypeQName(type2QNameEntity);
        assoc2.setQnameNamespace(emptyNamespaceEntity);
        assoc2.setQnameLocalName("number2");
        assoc2.setChildNodeName("number2");
        assoc2.setChildNodeNameCrc(2);
        assoc2.buildAssociation(containerNode, contentNode2);
        getSession().save(assoc2);
        
        // make sure that there are no entities cached in either L1 or L2
        getSession().flush();
        getSession().clear();

        // now read the structure back in from the container down
        containerNodeStatus = (NodeStatus) getSession().get(NodeStatusImpl.class, containerNodeKey);
        containerNode = containerNodeStatus.getNode();
        
        // clear out again
        getSession().clear();

        // expect that just the specific property gets removed in the delete statement
        getSession().flush();
        getSession().clear();
        
        // Create a second association to content 2
        // create an association to content 2
        containerNodeStatus = (NodeStatus) getSession().get(NodeStatusImpl.class, containerNodeKey);
        containerNode = containerNodeStatus.getNode();
        contentNodeStatus2 = (NodeStatus) getSession().get(NodeStatusImpl.class, contentNodeKey2);
        contentNode2 = contentNodeStatus2.getNode();
        ChildAssoc assoc3 = new ChildAssocImpl();
        assoc3.setIsPrimary(false);
        assoc3.setTypeQName(type3QNameEntity);
        assoc3.setQnameNamespace(emptyNamespaceEntity);
        assoc3.setQnameLocalName("number3");
        assoc3.setChildNodeName("number3");
        assoc3.setChildNodeNameCrc(2);
        assoc3.buildAssociation(containerNode, contentNode2);  // check whether the children are pulled in for this
        getSession().save(assoc3);

        // flush it
        getSession().flush();
        getSession().clear();
    }
    
    public void testDeletesAndFlush() throws Exception
    {
        // Create parent node
        Node parentNode = new NodeImpl();
        parentNode.setStore(store);
        parentNode.setUuid(GUID.generate());
        parentNode.setTypeQName(containerQNameEntity);
        Long nodeIdOne = (Long) getSession().save(parentNode);
        // Create child node
        Node childNode = new NodeImpl();
        childNode.setStore(store);
        childNode.setUuid(GUID.generate());
        childNode.setTypeQName(contentQNameEntity);
        Long nodeIdTwo = (Long) getSession().save(childNode);
        // Get them into the database
        getSession().flush();
        
        // Now create a loads of associations
        int assocCount = 1000;
        List<Long> assocIds = new ArrayList<Long>(assocCount);
        for (int i = 0; i < assocCount; i++)
        {
            ChildAssoc assoc = new ChildAssocImpl();
            assoc.buildAssociation(parentNode, childNode);
            assoc.setIsPrimary(false);
            assoc.setTypeQName(type1QNameEntity);
            assoc.setQnameNamespace(emptyNamespaceEntity);
            assoc.setQnameLocalName("" + System.nanoTime());
            assoc.setChildNodeName(GUID.generate());                        // It must be unique
            assoc.setChildNodeNameCrc(-1L);
            Long assocId = (Long) getSession().save(assoc);
            assocIds.add(assocId);
        }
        // Flush and clear the lot
        getSession().flush();
        getSession().clear();
        
        // Now we delete the entities, flushing and clearing every 100 deletes
        int count = 0;
        for (Long assocId : assocIds)
        {
            // Load the entity
            ChildAssoc assoc = (ChildAssoc) getSession().get(ChildAssocImpl.class, assocId);
            assertNotNull("Entity should exist", assoc);
            getSession().delete(assoc);
            // Do we flush and clear
            if (count % 100 == 0)
            {
                getSession().flush();
                getSession().clear();
            }
            count++;
        }
    }
    
    private static final String GET_NODE =
    "      select"+
    "         node" +
    "      from" +
    "         org.alfresco.repo.domain.hibernate.NodeImpl as node" +
    "      where" +
    "         node.id in (:nodeIds)";
    @SuppressWarnings("unchecked")
    public void testPropertiesViaJoin() throws Exception
    {
        getSession().setCacheMode(CacheMode.IGNORE);
        
        List<Long> nodeIds = new ArrayList<Long>(10);
        
        for (int i = 0; i < 100; i++)
        {
            // make a container node
            Node node = new NodeImpl();
            node.setStore(store);
            node.setUuid(GUID.generate());
            node.setTypeQName(containerQNameEntity);
            node.getProperties().put(propAuthorQNameEntity.getId(), new PropertyValue(DataTypeDefinition.TEXT, "ABC"));
            node.getProperties().put(propArchivedByQNameEntity.getId(), new PropertyValue(DataTypeDefinition.TEXT, "ABC"));
            Long nodeId = (Long) getSession().save(node);
            // Keep the ID
            nodeIds.add(nodeId);
        }
        getSession().flush();
        getSession().clear();
        
        // Now select it
        Query query = getSession()
            .createQuery(GET_NODE)
            .setParameterList("nodeIds", nodeIds)
            .setCacheMode(CacheMode.IGNORE);
        List<Node> queryList = (List<Node>) query.list();
        
        for (Node node : queryList)
        {
            // Get the node properties - this should not execute a query to retrieve
            node.getProperties().size();
        }
    }
}