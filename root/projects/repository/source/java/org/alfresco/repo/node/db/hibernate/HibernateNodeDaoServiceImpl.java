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
package org.alfresco.repo.node.db.hibernate;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.CRC32;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.domain.AuditableProperties;
import org.alfresco.repo.domain.ChildAssoc;
import org.alfresco.repo.domain.DbAccessControlList;
import org.alfresco.repo.domain.LocaleDAO;
import org.alfresco.repo.domain.Node;
import org.alfresco.repo.domain.NodeAssoc;
import org.alfresco.repo.domain.NodePropertyValue;
import org.alfresco.repo.domain.PropertyMapKey;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.domain.QNameDAO;
import org.alfresco.repo.domain.Server;
import org.alfresco.repo.domain.Store;
import org.alfresco.repo.domain.Transaction;
import org.alfresco.repo.domain.UsageDeltaDAO;
import org.alfresco.repo.domain.hibernate.ChildAssocImpl;
import org.alfresco.repo.domain.hibernate.DMPermissionsDaoComponentImpl;
import org.alfresco.repo.domain.hibernate.DbAccessControlListImpl;
import org.alfresco.repo.domain.hibernate.DirtySessionMethodInterceptor;
import org.alfresco.repo.domain.hibernate.NodeAssocImpl;
import org.alfresco.repo.domain.hibernate.NodeImpl;
import org.alfresco.repo.domain.hibernate.ServerImpl;
import org.alfresco.repo.domain.hibernate.StoreImpl;
import org.alfresco.repo.domain.hibernate.TransactionImpl;
import org.alfresco.repo.node.db.NodeDaoService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.ACLType;
import org.alfresco.repo.security.permissions.AccessControlListProperties;
import org.alfresco.repo.security.permissions.SimpleAccessControlListProperties;
import org.alfresco.repo.security.permissions.impl.AclChange;
import org.alfresco.repo.security.permissions.impl.AclDaoComponent;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionAwareSingleton;
import org.alfresco.repo.transaction.TransactionalDao;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryException;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.InvalidTypeException;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationExistsException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.EntityRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.InvalidStoreRefException;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreExistsException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.cmr.repository.datatype.TypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Hibernate-specific implementation of the persistence-independent <b>node</b> DAO interface
 * 
 * @author Derek Hulley
 */
public class HibernateNodeDaoServiceImpl extends HibernateDaoSupport implements NodeDaoService, TransactionalDao
{
    private static final String QUERY_GET_STORE_BY_ALL = "store.GetStoreByAll";
    private static final String QUERY_GET_ALL_STORES = "store.GetAllStores";
    private static final String QUERY_GET_NODE_BY_STORE_ID_AND_UUID = "node.GetNodeByStoreIdAndUuid";
    private static final String QUERY_GET_CHILD_NODE_IDS = "node.GetChildNodeIds";
    private static final String QUERY_GET_CHILD_ASSOCS_BY_ALL = "node.GetChildAssocsByAll";
    private static final String QUERY_GET_CHILD_ASSOC_BY_TYPE_AND_NAME = "node.GetChildAssocByTypeAndName";
    private static final String QUERY_GET_CHILD_ASSOC_REFS = "node.GetChildAssocRefs";
    private static final String QUERY_GET_CHILD_ASSOC_REFS_BY_QNAME = "node.GetChildAssocRefsByQName";
    private static final String QUERY_GET_CHILD_ASSOC_REFS_BY_TYPEQNAMES = "node.GetChildAssocRefsByTypeQNames";
    private static final String QUERY_GET_CHILD_ASSOC_REFS_BY_TYPEQNAME_AND_QNAME = "node.GetChildAssocRefsByTypeQNameAndQName";
    private static final String QUERY_GET_CHILD_ASSOC_REFS_BY_CHILD_TYPEQNAME = "node.GetChildAssocRefsByChildTypeQName";
    private static final String QUERY_GET_PRIMARY_CHILD_ASSOCS = "node.GetPrimaryChildAssocs";
    private static final String QUERY_GET_PRIMARY_CHILD_ASSOCS_NOT_IN_SAME_STORE = "node.GetPrimaryChildAssocsNotInSameStore";
    private static final String QUERY_GET_NODES_WITH_CHILDREN_IN_DIFFERENT_STORES ="node.GetNodesWithChildrenInDifferentStores";
    private static final String QUERY_GET_NODES_WITH_ASPECT ="node.GetNodesWithAspect";
    private static final String QUERY_GET_PARENT_ASSOCS = "node.GetParentAssocs";
    private static final String QUERY_GET_NODE_ASSOC = "node.GetNodeAssoc";
    private static final String QUERY_GET_NODE_ASSOCS_TO_AND_FROM = "node.GetNodeAssocsToAndFrom";
    private static final String QUERY_GET_TARGET_ASSOCS = "node.GetTargetAssocs";
    private static final String QUERY_GET_SOURCE_ASSOCS = "node.GetSourceAssocs";
    private static final String QUERY_GET_NODES_WITH_PROPERTY_VALUES_BY_STRING_AND_STORE = "node.GetNodesWithPropertyValuesByStringAndStore";
    private static final String QUERY_GET_CONTENT_URLS_FOR_STORE = "node.GetContentUrlsForStore";
    private static final String QUERY_GET_USERS_WITHOUT_USAGE = "node.GetUsersWithoutUsage";
    private static final String QUERY_GET_USERS_WITH_USAGE = "node.GetUsersWithUsage";
    private static final String QUERY_GET_NODES_WITH_PROPERTY_VALUES_BY_ACTUAL_TYPE = "node.GetNodesWithPropertyValuesByActualType";
    private static final String QUERY_GET_SERVER_BY_IPADDRESS = "server.getServerByIpAddress";
    
    private static final Long NULL_CACHE_VALUE = new Long(-1);

    private static Log logger = LogFactory.getLog(HibernateNodeDaoServiceImpl.class);
    /** Log to trace parent association caching: <b>classname + .ParentAssocsCache</b> */
    private static Log loggerParentAssocsCache = LogFactory.getLog(HibernateNodeDaoServiceImpl.class.getName() + ".ParentAssocsCache");
    
    private QNameDAO qnameDAO;
    private UsageDeltaDAO usageDeltaDAO;
    private AclDaoComponent aclDaoComponent;
    private LocaleDAO localeDAO;
    private DictionaryService dictionaryService;
    /** A cache mapping StoreRef and NodeRef instances to the entity IDs (primary key) */
    private SimpleCache<EntityRef, Long> storeAndNodeIdCache;
    /** A cache for more performant lookups of the parent associations */
    private SimpleCache<Long, Set<Long>> parentAssocsCache;
    private boolean isDebugEnabled = logger.isDebugEnabled();
    private boolean isDebugParentAssocCacheEnabled = loggerParentAssocsCache.isDebugEnabled();
    
    /** a uuid identifying this unique instance */
    private final String uuid;
    
    private static TransactionAwareSingleton<Long> serverIdSingleton = new TransactionAwareSingleton<Long>();
    private final String ipAddress;

    /** used for debugging */
    private Set<String> changeTxnIdSet;

    /**
     * 
     */
    public HibernateNodeDaoServiceImpl()
    {
        this.uuid = GUID.generate();
        try
        {
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
            throw new AlfrescoRuntimeException("Failed to get server IP address", e);
        }
        
        changeTxnIdSet = new HashSet<String>(0);
    }

    /**
     * Checks equality by type and uuid
     */
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        else if (!(obj instanceof HibernateNodeDaoServiceImpl))
        {
            return false;
        }
        HibernateNodeDaoServiceImpl that = (HibernateNodeDaoServiceImpl) obj;
        return this.uuid.equals(that.uuid);
    }
    
    /**
     * @see #uuid
     */
    public int hashCode()
    {
        return uuid.hashCode();
    }

    /**
     * Set the component for creating QName entities.
     */
    public void setQnameDAO(QNameDAO qnameDAO)
    {
        this.qnameDAO = qnameDAO;
    }

    public void setUsageDeltaDAO(UsageDeltaDAO usageDeltaDAO)
    {
        this.usageDeltaDAO = usageDeltaDAO;
    }
    
    public void setAclDaoComponent(AclDaoComponent aclDaoComponent)
    {
        this.aclDaoComponent = aclDaoComponent;
    }

    /**
     * Set the component for creating Locale entities
     */
    public void setLocaleDAO(LocaleDAO localeDAO)
    {
        this.localeDAO = localeDAO;
    }

    /**
     * Set the component for querying the dictionary model
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     * Ste the transaction-aware cache to store Store and Root Node IDs by Store Reference
     * 
     * @param storeAndNodeIdCache          the cache
     */
    public void setStoreAndNodeIdCache(SimpleCache<EntityRef, Long> storeAndNodeIdCache)
    {
        this.storeAndNodeIdCache = storeAndNodeIdCache;
    }

    /**
     * Set the transaction-aware cache to store parent associations by child node id
     * 
     * @param parentAssocsCache     the cache
     */
    public void setParentAssocsCache(SimpleCache<Long, Set<Long>> parentAssocsCache)
    {
        this.parentAssocsCache = parentAssocsCache;
    }

    /**
     * @return          Returns the ID of this instance's <b>server</b> instance or <tt>null</tt>
     */
    private Long getServerIdOrNull()
    {
        Long serverId = serverIdSingleton.get();
        if (serverId != null)
        {
            return serverId;
        }
        // Query for it
        // The server already exists, so get it
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                        .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_SERVER_BY_IPADDRESS)
                        .setString("ipAddress", ipAddress);
                return query.uniqueResult();
            }
        };
        Server server = (Server) getHibernateTemplate().execute(callback);
        if (server != null)
        {
            // It exists, so just return the ID
            return server.getId();
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Gets/creates the <b>server</b> instance to use for the life of this instance
     */
    private Server getServer()
    {
        Long serverId = serverIdSingleton.get();
        Server server = null;
        if (serverId != null)
        {
            server = (Server) getSession().get(ServerImpl.class, serverId);
            if (server != null)
            {
                return server;
            }
        }
        try
        {
            HibernateCallback callback = new HibernateCallback()
            {
                public Object doInHibernate(Session session)
                {
                    Query query = session
                            .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_SERVER_BY_IPADDRESS)
                            .setString("ipAddress", ipAddress);
                    DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                    return query.uniqueResult();
                }
            };
            server = (Server) getHibernateTemplate().execute(callback);
            // create it if it doesn't exist
            if (server == null)
            {
                server = new ServerImpl();
                server.setIpAddress(ipAddress);
                try
                {
                    getSession().save(server);
                }
                catch (DataIntegrityViolationException e)
                {
                    // get it again
                    server = (Server) getHibernateTemplate().execute(callback);
                    if (server == null)
                    {
                        throw new AlfrescoRuntimeException("Unable to create server instance: " + ipAddress);
                    }
                }
            }
            // push the value into the singleton
            serverIdSingleton.put(server.getId());
            
            return server;
        }
        catch (Exception e)
        {
            throw new AlfrescoRuntimeException("Failed to create server instance", e);
        }
    }
    
    private static final String RESOURCE_KEY_TRANSACTION_ID = "hibernate.transaction.id";
    private Transaction getCurrentTransaction()
    {
        Transaction transaction = null;
        Serializable txnId = (Serializable) AlfrescoTransactionSupport.getResource(RESOURCE_KEY_TRANSACTION_ID);
        if (txnId == null)
        {
            String changeTxnId = AlfrescoTransactionSupport.getTransactionId();
            // no transaction instance has been bound to the transaction
            transaction = new TransactionImpl();
            transaction.setChangeTxnId(changeTxnId);
            transaction.setServer(getServer());
            txnId = getHibernateTemplate().save(transaction);
            // bind the id
            AlfrescoTransactionSupport.bindResource(RESOURCE_KEY_TRANSACTION_ID, txnId);
            
            if (isDebugEnabled)
            {
                if (!changeTxnIdSet.add(changeTxnId))
                {
                    // the txn id was already used!
                    logger.error("Change transaction ID already used: " + transaction);
                }
                logger.debug("Created new transaction: " + transaction);
            }
        }
        else
        {
            transaction = (Transaction) getHibernateTemplate().get(TransactionImpl.class, txnId);
            if (isDebugEnabled)
            {
                logger.debug("Using existing transaction: " + transaction);
            }
        }
        return transaction;
    }
    
    /**
     * Ensure that any transaction that might be present is updated to reflect the current time.
     */
    public void beforeCommit()
    {
        Serializable txnId = (Serializable) AlfrescoTransactionSupport.getResource(RESOURCE_KEY_TRANSACTION_ID);
        if (txnId != null)
        {
            // A write was done during the current transaction
            Transaction transaction = (Transaction) getHibernateTemplate().get(TransactionImpl.class, txnId);
            transaction.setCommitTimeMs(System.currentTimeMillis());
        }
    }

    /**
     * Does this <tt>Session</tt> contain any changes which must be
     * synchronized with the store?
     * 
     * @return true => changes are pending
     */
    public boolean isDirty()
    {
        // create a callback for the task
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                return session.isDirty();
            }
        };
        // execute the callback
        return ((Boolean)getHibernateTemplate().execute(callback)).booleanValue();
    }

    /**
     * Just flushes the session
     */
    public void flush()
    {
        getSession().flush();
    }

    /**
     * @return              Returns the <tt>Store</tt> entity or <tt>null</tt>
     */
    private Store getStore(final StoreRef storeRef)
    {
        // Look it up in the cache
        Long storeId = storeAndNodeIdCache.get(storeRef);
        // Load it
        if (storeId != null)
        {
            // Check for null persistence (previously missed value)
            if (storeId.equals(NULL_CACHE_VALUE))
            {
                // There is no such value matching
                return null;
            }
            // Don't use the method that throws an exception as the cache might be invalid.
            Store store = (Store) getSession().get(StoreImpl.class, storeId);
            if (store == null)
            {
                // It is not available, so we need to go the query route.
                // But first remove the cache entry
                storeAndNodeIdCache.remove(storeRef);
                // Recurse, but this time there is no cache entry
                return getStore(storeRef);
            }
            else
            {
                return store;
            }
        }
        // Query for it
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_STORE_BY_ALL)
                    .setString("protocol", storeRef.getProtocol())
                    .setString("identifier", storeRef.getIdentifier());
                return query.uniqueResult();
            }
        };
        Store store = (Store) getHibernateTemplate().execute(callback);
        if (store == null)
        {
            // Persist the null entry
            storeAndNodeIdCache.put(storeRef, NULL_CACHE_VALUE);
        }
        else
        {
            storeAndNodeIdCache.put(storeRef, store.getId());
        }
        // done
        return store;
    }

    private Store getStoreNotNull(StoreRef storeRef)
    {
        Store store = getStore(storeRef);
        if (store == null)
        {
            throw new InvalidStoreRefException(storeRef);
        }
        // done
        return store;
    }

    /**
     * Fetch the node.  If the ID is invalid, we assume that the state of the current session
     * is invalid i.e. the data is stale
     * 
     * @param nodeId        the node's ID
     * @return              the node
     * @throws              AlfrescoRuntimeException if the ID doesn't refer to a node.
     */
    private Node getNodeNotNull(Long nodeId)
    {
        Node node = (Node) getHibernateTemplate().get(NodeImpl.class, nodeId);
        if (node == null)
        {
            throw new AlfrescoRuntimeException("Node ID " + nodeId + " is invalid");
        }
        return node;
    }
    
    /**
     * Fetch the child assoc.  If the ID is invalid, we assume that the state of the current session
     * is invalid i.e. the data is stale
     * 
     * @param childAssocId  the assoc's ID
     * @return              the assoc
     * @throws              AlfrescoRuntimeException if the ID doesn't refer to an assoc.
     */
    private ChildAssoc getChildAssocNotNull(Long childAssocId)
    {
        ChildAssoc assoc = (ChildAssoc) getHibernateTemplate().get(ChildAssocImpl.class, childAssocId);
        if (assoc == null)
        {
            throw new AlfrescoRuntimeException("ChildAssoc ID " + childAssocId + " is invalid");
        }
        return assoc;
    }
    
//    /**
//     * Fetch the child assoc.  If the ID is invalid, we assume that the state of the current session
//     * is invalid i.e. the data is stale
//     * 
//     * @param nodeAssocId   the assoc's ID
//     * @return              the assoc
//     * @throws              AlfrescoRuntimeException if the ID doesn't refer to an assoc.
//     */
//    private NodeAssoc getNodeAssocNotNull(Long nodeAssocId)
//    {
//        NodeAssoc assoc = (NodeAssoc) getHibernateTemplate().get(NodeAssocImpl.class, nodeAssocId);
//        if (assoc == null)
//        {
//            throw new AlfrescoRuntimeException("NodeAssoc ID " + nodeAssocId + " is invalid");
//        }
//        return assoc;
//    }
//    
    /**
     * @see #QUERY_GET_ALL_STORES
     */
    @SuppressWarnings("unchecked")
    public List<StoreRef> getStoreRefs()
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_ALL_STORES);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.list();
            }
        };
        List<Store> stores = (List) getHibernateTemplate().execute(callback);
        List<StoreRef> storeRefs = new ArrayList<StoreRef>(stores.size());
        for (Store store : stores)
        {
            storeRefs.add(store.getStoreRef());
        }
        // done
        return storeRefs;
    }
    
    public Pair<Long, NodeRef> getRootNode(StoreRef storeRef)
    {
        Store store = getStore(storeRef);
        if (store == null)
        {
            return null;
        }
        Node rootNode = store.getRootNode();
        if (rootNode == null)
        {
            throw new InvalidStoreRefException("Store does not have a root node: " + storeRef, storeRef);
        }
        // done
        return new Pair<Long, NodeRef>(rootNode.getId(), rootNode.getNodeRef());
    }

    /**
     * Ensures that the store protocol/identifier combination is unique
     */
    public Pair<Long, NodeRef> createStore(StoreRef storeRef)
    {
        Store store = getStore(storeRef);
        if (store != null)
        {
            throw new StoreExistsException(storeRef);
        }
        
        store = new StoreImpl();
        // set key values
        store.setProtocol(storeRef.getProtocol());
        store.setIdentifier(storeRef.getIdentifier());
        // The root node may be null exactly because the Store needs an ID before it can be assigned to a node
        getHibernateTemplate().save(store);
        // create and assign a root node
        Node rootNode = newNode(
                store,
                null,
                ContentModel.TYPE_STOREROOT);
        store.setRootNode(rootNode);
        // Add the root aspect
        Pair<Long, QName> rootAspectQNamePair = qnameDAO.getOrCreateQName(ContentModel.ASPECT_ROOT);
        rootNode.getAspects().add(rootAspectQNamePair.getFirst());
        
        // Assign permissions to the root node
        SimpleAccessControlListProperties properties = DMPermissionsDaoComponentImpl.getDefaultProperties();
        Long id = aclDaoComponent.createAccessControlList(properties);
        DbAccessControlList acl = aclDaoComponent.getDbAccessControlList(id);
        rootNode.setAccessControlList(acl);
        
        // Cache the value
        storeAndNodeIdCache.put(storeRef, store.getId());
        // Done
        return new Pair<Long, NodeRef>(rootNode.getId(), rootNode.getNodeRef());
    }

    public NodeRef.Status getNodeRefStatus(NodeRef nodeRef)
    {
        // Get the store
        StoreRef storeRef = nodeRef.getStoreRef();
        Store store = getStore(storeRef);
        if (store == null)
        {
            // No such store therefore no such node reference
            return null;
        }
        Node node = getNodeOrNull(store, nodeRef.getId());
        if (node == null)     // node never existed
        {
            return null;
        }
        else
        {
            return new NodeRef.Status(
                    node.getTransaction().getChangeTxnId(),
                    node.getDeleted());
        }
    }
    
    private Node getNodeOrNull(final Store store, final String uuid)
    {
        NodeRef nodeRef = new NodeRef(store.getStoreRef(), uuid);
        // Look it up in the cache
        Long nodeId = storeAndNodeIdCache.get(nodeRef);
        // Load it
        if (nodeId != null)
        {
            // Check for null persistence (previously missed value)
            if (nodeId.equals(NULL_CACHE_VALUE))
            {
                // There is no such value matching
                return null;
            }
            // Don't use the method that throws an exception as the cache might be invalid.
            Node node = (Node) getSession().get(NodeImpl.class, nodeId);
            if (node == null)
            {
                // It is not available, so we need to go the query route.
                // But first remove the cache entry
                storeAndNodeIdCache.remove(nodeRef);
                // Recurse, but this time there is no cache entry
                return getNodeOrNull(store, uuid);
            }
            else
            {
                return node;
            }
        }
        // Query for it
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_NODE_BY_STORE_ID_AND_UUID)
                    .setLong("storeId", store.getId())
                    .setString("uuid", uuid);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.uniqueResult();
            }
        };
        Node node = (Node) getHibernateTemplate().execute(callback);
        // Cache the value
        if (node == null)
        {
            storeAndNodeIdCache.put(nodeRef, NULL_CACHE_VALUE);
        }
        else
        {
            storeAndNodeIdCache.put(nodeRef, node.getId());
        }
        // TODO: Fill cache here
        return node;
    }
    
    private void updateNodeStatus(Node node, boolean deleted)
    {
        Transaction currentTxn = getCurrentTransaction();
        // Update it if required
        if (!EqualsHelper.nullSafeEquals(node.getTransaction(), currentTxn))
        {
            // Txn has changed
            DirtySessionMethodInterceptor.setSessionDirty();
            node.setTransaction(currentTxn);
        }
        if (node.getDeleted() != deleted)
        {
            DirtySessionMethodInterceptor.setSessionDirty();
            node.setDeleted(deleted);
        }
    }
    
    private static final String UNKOWN_USER = "unkown";
    private String getCurrentUser()
    {
        String user = AuthenticationUtil.getCurrentUserName();
        return (user == null) ? UNKOWN_USER : user;
    }
    
    private void recordNodeCreate(Node node)
    {
        updateNodeStatus(node, false);
        // Handle cm:auditable
        if (hasNodeAspect(node, ContentModel.ASPECT_AUDITABLE))
        {
            String currentUser = getCurrentUser();
            Date currentDate = new Date();
            AuditableProperties auditableProperties = node.getAuditableProperties();
            auditableProperties.setAuditValues(currentUser, currentDate, false);
        }
    }

    private void recordNodeUpdate(Node node)
    {
        updateNodeStatus(node, false);
        // Handle cm:auditable
        if (hasNodeAspect(node, ContentModel.ASPECT_AUDITABLE))
        {
            String currentUser = getCurrentUser();
            Date currentDate = new Date();
            AuditableProperties auditableProperties = node.getAuditableProperties();
            auditableProperties.setAuditValues(currentUser, currentDate, false);
        }
    }

    private void recordNodeDelete(Node node)
    {
        updateNodeStatus(node, true);
        // Handle cm:auditable
        if (hasNodeAspect(node, ContentModel.ASPECT_AUDITABLE))
        {
            String currentUser = getCurrentUser();
            Date currentDate = new Date();
            AuditableProperties auditableProperties = node.getAuditableProperties();
            auditableProperties.setAuditValues(currentUser, currentDate, false);
        }
    }

    public Pair<Long, NodeRef> newNode(StoreRef storeRef, String uuid, QName nodeTypeQName) throws InvalidTypeException
    {
        Store store = (Store) getStoreNotNull(storeRef);
        Node newNode = newNode(store, uuid, nodeTypeQName);
        Long nodeId = newNode.getId();
        NodeRef nodeRef = newNode.getNodeRef();
        return new Pair<Long, NodeRef>(nodeId, nodeRef);
    }
    
    private Node newNode(Store store, String uuid, QName nodeTypeQName) throws InvalidTypeException
    {
        Node node = null;
        if (uuid != null)
        {
            // Get any existing Node.  A node with this UUID may have existed before, but must be marked
            // deleted; otherwise it will be considered live and valid
            node = getNodeOrNull(store, uuid);
        }
        else
        {
            uuid = GUID.generate();
        }
        if (node != null)
        {
            if (!node.getDeleted())
            {
                // If there is already an undeleted node, then there is a clash
                throw new InvalidNodeRefException("Live Node exists: " + node.getNodeRef(), node.getNodeRef());
            }
            // Set clean values
            node.setTypeQName(qnameDAO, nodeTypeQName);
            node.setDeleted(false);
            node.setAccessControlList(null);
            // Record node change
            recordNodeCreate(node);
        }
        else
        {
            // There is no existing node, deleted or otherwise.
            node = new NodeImpl();
            node.setStore(store);
            node.setUuid(uuid);
            node.setTypeQName(qnameDAO, nodeTypeQName);
            node.setDeleted(false);
            node.setAccessControlList(null);
            // Record node change
            recordNodeCreate(node);
            // Persist it
            getHibernateTemplate().save(node);
            
            // Update the cache
            storeAndNodeIdCache.put(node.getNodeRef(), node.getId());
        }
        
        // Done
        return node;
    }

    /**
     * This moves the entire node, ensuring that a trail is left behind.  It is more
     * efficient to move the node and recreate a deleted node in it's wake because of
     * the other properties and aspects that need to go with the node.
     */
    public Pair<Long, NodeRef> moveNodeToStore(Long nodeId, StoreRef storeRef)
    {
        Node node = getNodeNotNull(nodeId);
        // Update the node
        updateNode(nodeId, storeRef, null, null);
        NodeRef nodeRef = node.getNodeRef();
        
        return new Pair<Long, NodeRef>(node.getId(), nodeRef);
    }

    public Pair<Long, NodeRef> getNodePair(NodeRef nodeRef)
    {
        Store store = getStore(nodeRef.getStoreRef());
        if (store == null)
        {
            return null;
        }
        // Get the node: none, deleted or live
        Node node = getNodeOrNull(store, nodeRef.getId());
        if (node == null)
        {
            // The node doesn't exist even as a deleted reference
            return null;
        }
        else if (node.getDeleted())
        {
            // The reference exists, but only as a deleted node
            return null;
        }
        else
        {
            // The node is live
            return  new Pair<Long, NodeRef>(node.getId(), nodeRef);
        }
    }
    
    public Pair<Long, NodeRef> getNodePair(Long nodeId)
    {
        Node node = (Node) getHibernateTemplate().get(NodeImpl.class, nodeId);
        if (node == null)
        {
            return null;
        }
        else if (node.getDeleted())
        {
            // The node reference exists, but it is officially deleted
            return null;
        }
        else
        {
            return new Pair<Long, NodeRef>(nodeId, node.getNodeRef());
        }
    }

    public QName getNodeType(Long nodeId)
    {
        Node node = getNodeNotNull(nodeId);
        QName nodeTypeQName = node.getTypeQName(qnameDAO);
        return nodeTypeQName;
    }

    public void setNodeStatus(Long nodeId)
    {
        Node node = getNodeNotNull(nodeId);
        recordNodeUpdate(node);
    }

    public Long getNodeAccessControlList(Long nodeId)
    {
        Node node = getNodeNotNull(nodeId);
        DbAccessControlList acl = node.getAccessControlList();
        if (acl == null)
        {
            return null;
        }
        else
        {
            return acl.getId();
        }
    }

    public void setNodeAccessControlList(Long nodeId, Long aclId)
    {
        Node node = getNodeNotNull(nodeId);
        if (aclId == null)
        {
            node.setAccessControlList(null);
        }
        else
        {
            DbAccessControlList acl = (DbAccessControlList) getHibernateTemplate().get(DbAccessControlListImpl.class, aclId);
            if (acl == null)
            {
                throw new IllegalArgumentException("ACL with ID " + aclId + " doesn't exist.");
            }
            node.setAccessControlList(acl);
        }
    }

    public void updateNode(Long nodeId, StoreRef storeRefAfter, String uuidAfter, QName nodeTypeQName)
    {
        Node node = getNodeNotNull(nodeId);
        Store storeBefore = node.getStore();
        String uuidBefore = node.getUuid();
        NodeRef nodeRefBefore = node.getNodeRef();
        
        final Store storeAfter;
        if (storeRefAfter == null)
        {
            storeAfter = storeBefore;
            storeRefAfter = storeBefore.getStoreRef();
        }
        else
        {
            storeAfter = getStoreNotNull(storeRefAfter);
        }
        if (uuidAfter == null)
        {
            uuidAfter = uuidBefore;
        }
        
        NodeRef nodeRefAfter = new NodeRef(storeRefAfter, uuidAfter);
        if (!nodeRefAfter.equals(nodeRefBefore))
        {
            Node conflictingNode = getNodeOrNull(storeAfter, uuidAfter);
            if (conflictingNode != null)
            {
                if (!conflictingNode.getDeleted())
                {
                    throw new InvalidNodeRefException("Live Node exists: " + node.getNodeRef(), node.getNodeRef());
                }
                // It is a deleted node so just remove the conflict
                getHibernateTemplate().delete(conflictingNode);
                // Flush immediately to ensure that the record is deleted
                DirtySessionMethodInterceptor.flushSession(getSession(), true);
                // The cache entry will be overwritten so we don't need to do it here
            }
            
            // Change the store
            node.setStore(storeAfter);
            node.setUuid(uuidAfter);
            // We will need to record the change for the new node
            recordNodeUpdate(node);
            // Flush immediately to ensure that the record changes
            DirtySessionMethodInterceptor.flushSession(getSession(), true);
            
            // We need to create a dummy reference for the node that was just moved away
            Node oldNodeDummy = new NodeImpl();
            oldNodeDummy.setStore(storeBefore);
            oldNodeDummy.setUuid(uuidBefore);
            oldNodeDummy.setTypeQNameId(node.getTypeQNameId());
            recordNodeDelete(oldNodeDummy);
            // Persist
            getHibernateTemplate().save(oldNodeDummy);
            
            // Update cache entries
            NodeRef nodeRef = node.getNodeRef();
            storeAndNodeIdCache.put(nodeRef, node.getId());
            storeAndNodeIdCache.put(nodeRefBefore, oldNodeDummy.getId());
        }
        
        // Only update the node type if it is changing
        if (nodeTypeQName != null)
        {
            Long nodeTypeQNameId = qnameDAO.getOrCreateQName(nodeTypeQName).getFirst();
            if (!nodeTypeQNameId.equals(node.getTypeQNameId()))
            {
                node.setTypeQNameId(nodeTypeQNameId);
                // We will need to record the change
                recordNodeUpdate(node);
            }
        }
    }

    public Serializable getNodeProperty(Long nodeId, QName propertyQName)
    {
        Node node = getNodeNotNull(nodeId);

        // Handle cm:auditable
        if (AuditableProperties.isAuditableProperty(propertyQName))
        {
            // Only bother if the aspect is present
            if (hasNodeAspect(node, ContentModel.ASPECT_AUDITABLE))
            {
                AuditableProperties auditableProperties = node.getAuditableProperties();
                return auditableProperties.getAuditableProperty(propertyQName);
            }
            else
            {
                return null;
            }
        }
        
        Pair<Long, QName> propertyQNamePair = qnameDAO.getQName(propertyQName);
        if (propertyQNamePair == null)
        {
            return null;
        }
        
        Map<PropertyMapKey, NodePropertyValue> nodeProperties = node.getProperties();
        Serializable propertyValue = HibernateNodeDaoServiceImpl.getPublicProperty(
                nodeProperties,
                propertyQName,
                qnameDAO, localeDAO, dictionaryService);
        return propertyValue;
    }

    public Map<QName, Serializable> getNodeProperties(Long nodeId)
    {
        Node node = getNodeNotNull(nodeId);
        Map<PropertyMapKey, NodePropertyValue> nodeProperties = node.getProperties();
        
        // Convert the QName IDs
        Map<QName, Serializable> converted = HibernateNodeDaoServiceImpl.convertToPublicProperties(
                nodeProperties,
                qnameDAO,
                localeDAO,
                dictionaryService);
        
        // Handle cm:auditable
        if (hasNodeAspect(node, ContentModel.ASPECT_AUDITABLE))
        {
            AuditableProperties auditableProperties = node.getAuditableProperties();
            converted.putAll(auditableProperties.getAuditableProperties());
        }
        
        // Done
        return converted;
    }

    private void addNodePropertyImpl(Node node, QName qname, Serializable value, Long localeId)
    {
        // Handle cm:auditable
        if (AuditableProperties.isAuditableProperty(qname))
        {
            // This is never set manually
            return;
        }
        
        PropertyDefinition propertyDef = dictionaryService.getProperty(qname);
        Long qnameId = qnameDAO.getOrCreateQName(qname).getFirst();
        
        Map<PropertyMapKey, NodePropertyValue> persistableProperties = new HashMap<PropertyMapKey, NodePropertyValue>(3);
        
        HibernateNodeDaoServiceImpl.addValueToPersistedProperties(
                persistableProperties,
                propertyDef,
                (short)-1,
                qnameId,
                localeId,
                value,
                localeDAO);
        
        Map<PropertyMapKey, NodePropertyValue> nodeProperties = node.getProperties();
        
        Iterator<PropertyMapKey> oldPropertyKeysIterator = nodeProperties.keySet().iterator();
        while (oldPropertyKeysIterator.hasNext())
        {
            PropertyMapKey oldPropertyKey = oldPropertyKeysIterator.next();
            // If the qname doesn't match, then ignore
            if (!oldPropertyKey.getQnameId().equals(qnameId))
            {
                continue;
            }
            // The qname matches, but is the key present in the new values
            if (persistableProperties.containsKey(oldPropertyKey))
            {
                // The key is present in both maps so it'll be updated
                continue;
            }
            // Remove the entry from the node's properties
            oldPropertyKeysIterator.remove();
        }
        
        // Now add all the new properties.  The will overwrite and/or add values.
        nodeProperties.putAll(persistableProperties);
    }

    public void addNodeProperty(Long nodeId, QName qname, Serializable propertyValue)
    {        
        Node node = getNodeNotNull(nodeId);
        Long localeId = localeDAO.getOrCreateDefaultLocalePair().getFirst();
        addNodePropertyImpl(node, qname, propertyValue, localeId);
        
        // Record change ID
        recordNodeUpdate(node);
    }
    
    @SuppressWarnings("unchecked")
    public void addNodeProperties(Long nodeId, Map<QName, Serializable> properties)
    {
        Node node = getNodeNotNull(nodeId);
        
        Long localeId = localeDAO.getOrCreateDefaultLocalePair().getFirst();
        for (Map.Entry<QName, Serializable> entry : properties.entrySet())
        {
            QName qname = entry.getKey();
            Serializable value = entry.getValue();
            addNodePropertyImpl(node, qname, value, localeId);
        }
        
        // Record change ID
        recordNodeUpdate(node);
    }

    public void setNodeProperties(Long nodeId, Map<QName, Serializable> propertiesIncl)
    {
        Node node = getNodeNotNull(nodeId);
        
        // Handle cm:auditable.  These need to be removed from the properties.
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>(propertiesIncl.size());
        for (Map.Entry<QName, Serializable> entry : propertiesIncl.entrySet())
        {
            QName propertyQName = entry.getKey();
            Serializable value = entry.getValue();
            if (AuditableProperties.isAuditableProperty(propertyQName))
            {
                continue;
            }
            // The value was NOT an auditable value
            properties.put(propertyQName, value);
        }
        
        // Convert
        Map<PropertyMapKey, NodePropertyValue> persistableProperties = HibernateNodeDaoServiceImpl.convertToPersistentProperties(
                properties,
                qnameDAO,
                localeDAO,
                dictionaryService);

        // Get the persistent map attached to the node
        Map<PropertyMapKey, NodePropertyValue> nodeProperties = node.getProperties();
        
        // In order to make as few changes as possible, we need to update existing properties wherever possible.
        // This means keeping track of map keys that weren't updated
        Set<PropertyMapKey> toRemove = new HashSet<PropertyMapKey>(nodeProperties.keySet());
        
        // Loop over the converted values and update the persisted node properties map
        for (Map.Entry<PropertyMapKey, NodePropertyValue> entry : persistableProperties.entrySet())
        {
            PropertyMapKey key = entry.getKey();
            toRemove.remove(key);
            // Add the value to the node's map
            nodeProperties.put(key, entry.getValue());
        }
        
        // Now remove all untouched keys
        for (PropertyMapKey key : toRemove)
        {
            nodeProperties.remove(key);
        }
        
        // Record change ID
        recordNodeUpdate(node);
    }

    public void removeNodeProperties(Long nodeId, Set<QName> propertyQNamesIncl)
    {
        Node node = getNodeNotNull(nodeId);
        
        // Handle cm:auditable.  These need to be removed from the list.
        Set<QName> propertyQNames = new HashSet<QName>(propertyQNamesIncl.size());
        for (QName propertyQName : propertyQNamesIncl)
        {
            if (AuditableProperties.isAuditableProperty(propertyQName))
            {
                continue;
            }
            propertyQNames.add(propertyQName);
        }
        
        Map<PropertyMapKey, NodePropertyValue> nodeProperties = node.getProperties();

        Set<Long> propertyQNameIds = qnameDAO.convertQNamesToIds(propertyQNames, true);
        
        // Loop over the current properties and remove any that have the same qname.
        // Make a copy as we will modify the original map.
        Set<PropertyMapKey> entrySet = new HashSet<PropertyMapKey>(nodeProperties.keySet());
        for (PropertyMapKey propertyMapKey : entrySet)
        {
            Long propertyQNameId = propertyMapKey.getQnameId();
            if (propertyQNameIds.contains(propertyQNameId))
            {
                nodeProperties.remove(propertyMapKey);
            }
        }
        
        // Record change ID
        recordNodeUpdate(node);
    }

    public Set<QName> getNodeAspects(Long nodeId)
    {
        Node node = getNodeNotNull(nodeId);
        Set<Long> nodeAspects = node.getAspects();
        
        // Convert
        Set<QName> nodeAspectQNames = qnameDAO.convertIdsToQNames(nodeAspects);
        
        // Add sys:referenceable
        nodeAspectQNames.add(ContentModel.ASPECT_REFERENCEABLE);
        
        // Make immutable
        return nodeAspectQNames;
    }

    public void addNodeAspects(Long nodeId, Set<QName> aspectQNames)
    {
        Node node = getNodeNotNull(nodeId);

        aspectQNames = new HashSet<QName>(aspectQNames);
        // Remove sys:referenceable
        aspectQNames.remove(ContentModel.ASPECT_REFERENCEABLE);
        
        // Convert
        Set<Long> aspectQNameIds = qnameDAO.convertQNamesToIds(aspectQNames, true);

        // Add them
        Set<Long> nodeAspects = node.getAspects();
        nodeAspects.addAll(aspectQNameIds);
        
        // Record change ID
        recordNodeUpdate(node);
    }
    
    public void removeNodeAspects(Long nodeId, Set<QName> aspectQNames)
    {
        Node node = getNodeNotNull(nodeId);

        aspectQNames = new HashSet<QName>(aspectQNames);
        // Remove sys:referenceable
        aspectQNames.remove(ContentModel.ASPECT_REFERENCEABLE);
        // Handle cm:auditable
        aspectQNames.remove(ContentModel.ASPECT_AUDITABLE);

        // Convert
        Set<Long> aspectQNameIds = qnameDAO.convertQNamesToIds(aspectQNames, false);
        
        // Remove them
        Set<Long> nodeAspects = node.getAspects();
        nodeAspects.removeAll(aspectQNameIds);
        
        // Record change ID
        recordNodeUpdate(node);
    }

    public boolean hasNodeAspect(Long nodeId, QName aspectQName)
    {
        // Shortcut sys:referenceable
        if (aspectQName.equals(ContentModel.ASPECT_REFERENCEABLE))
        {
            return true;
        }
        Node node = getNodeNotNull(nodeId);
        return hasNodeAspect(node, aspectQName);
    }

    private boolean hasNodeAspect(Node node, QName aspectQName)
    {
        Pair<Long, QName> aspectQNamePair = qnameDAO.getQName(aspectQName);
        if (aspectQNamePair == null)
        {
            return false;
        }
        
        Set<Long> nodeAspects = node.getAspects();
        return nodeAspects.contains(aspectQNamePair.getFirst());
    }

    /**
     * Manually ensures that all cascading of associations is taken care of
     */
    public void deleteNode(Long nodeId)
    {
        Node node = getNodeNotNull(nodeId);
        Set<Long> deletedChildAssocIds = new HashSet<Long>(10);
        deleteNodeInternal(node, false, deletedChildAssocIds);
        
        // Record change ID
        recordNodeDelete(node);
    }

    private static final String QUERY_DELETE_PARENT_ASSOCS = "node.DeleteParentAssocs";
    private static final String QUERY_DELETE_CHILD_ASSOCS = "node.DeleteChildAssocs";
    private static final String QUERY_DELETE_NODE_ASSOCS = "node.DeleteNodeAssocs";
    
    /**
     * Does a full cleanup of the node if the <tt>deleted</tt> flag is off.  If
     * the node is marked as <tt>deleted</tt> then the cleanup is assumed to be
     * unnecessary and the node entry itself is cleaned up.
     * 
     * @param node                  the node to delete
     * @param cascade               true to cascade delete
     * @param deletedChildAssocIds  previously deleted child associations
     */
    private void deleteNodeInternal(Node node, boolean cascade, Set<Long> deletedChildAssocIds)
    {
        final Long nodeId = node.getId();

        // delete all parent assocs
        if (isDebugEnabled)
        {
            logger.debug("Deleting child assocs of node " + nodeId);
        }
        HibernateCallback getChildNodeIdsCallback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_CHILD_NODE_IDS)
                    .setLong("parentId", nodeId);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults childNodeIds = null;
        try
        {
            childNodeIds = (ScrollableResults) getHibernateTemplate().execute(getChildNodeIdsCallback);
        
            while (childNodeIds.next())
            {
                Long childNodeId = childNodeIds.getLong(0);
                parentAssocsCache.remove(childNodeId);
                if (isDebugParentAssocCacheEnabled)
                {
                    loggerParentAssocsCache.debug("\n" + "Parent associations cache - Removing entry: \n" + "   Node:   " + childNodeId);
                }
            }
        }
        finally
        {
            if(childNodeIds != null)
            {
                childNodeIds.close();
            }
        }
        HibernateCallback deleteParentAssocsCallback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_DELETE_CHILD_ASSOCS)
                    .setLong("parentId", nodeId);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.executeUpdate();
            }
        };
        getHibernateTemplate().execute(deleteParentAssocsCallback);
        
        // delete all child assocs
        if (isDebugEnabled)
        {
            logger.debug("Deleting parent assocs of node " + nodeId);
        }
        HibernateCallback deleteChildAssocsCallback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_DELETE_PARENT_ASSOCS)
                    .setLong("childId", nodeId);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.executeUpdate();
            }
        };
        getHibernateTemplate().execute(deleteChildAssocsCallback);
        
        // delete all node associations to and from
        if (isDebugEnabled)
        {
            logger.debug("Deleting source and target assocs of node " + node.getId());
        }
        HibernateCallback deleteNodeAssocsCallback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_DELETE_NODE_ASSOCS)
                    .setLong("nodeId", nodeId);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.executeUpdate();
            }
        };
        getHibernateTemplate().execute(deleteNodeAssocsCallback);
        
        // Delete deltas
        usageDeltaDAO.deleteDeltas(nodeId);
        
        // Wipe out properties and aspects
        node.getProperties().clear();
        node.getAspects().clear();
        
        // delete ACLs
        
        DbAccessControlList dbAcl = node.getAccessControlList();
        node.setAccessControlList(null);
        if(dbAcl != null)
        {
            if(dbAcl.getAclType() == ACLType.DEFINING)
            {
                getHibernateTemplate().delete(dbAcl);
            }
            if(dbAcl.getAclType() == ACLType.SHARED)
            {
                // check unused
                Long defining = dbAcl.getInheritsFrom();
                if(getHibernateTemplate().get(DbAccessControlListImpl.class, defining) == null)
                {
                    final Long id = dbAcl.getId();
                    HibernateCallback check = new HibernateCallback()
                    {
                        public Object doInHibernate(Session session)
                        {
                            Criteria criteria = getSession().createCriteria(NodeImpl.class, "n");
                            criteria.add(Restrictions.eq("n.accessControlList.id", id));
                            criteria.setProjection(Projections.rowCount());
                            return criteria.list();
                        }
                    };
                    List<Integer> list =  (List<Integer>)getHibernateTemplate().execute(check);
                    if(list.get(0).intValue() == 0)
                    {
                        getHibernateTemplate().delete(dbAcl);
                    }
                }
            }
        }
        
        // Mark the node as deleted
        node.setDeleted(true);
        
        // Remove node from cache
        parentAssocsCache.remove(nodeId);
        if (isDebugParentAssocCacheEnabled)
        {
            loggerParentAssocsCache.debug("\n" +
                    "Parent associations cache - Removing entry: \n" +
                    "   Node:   " + nodeId);
        }
        // done
    }
    
    private long getCrc(String str)
    {
        CRC32 crc = new CRC32();
        try
        {
            crc.update(str.getBytes("UTF-8"));              // https://issues.alfresco.com/jira/browse/ALFCOM-1335
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("UTF-8 encoding is not supported");
        }
        return crc.getValue();
    }
    
    private static final String TRUNCATED_NAME_INDICATOR = "~~~";
    private String getShortName(String str)
    {
        int length = str.length();
        if (length <= 50)
        {
            return str;
        }
        else
        {
            StringBuilder ret = new StringBuilder(50);
            ret.append(str.substring(0, 47)).append(TRUNCATED_NAME_INDICATOR);
            return ret.toString();
        }
    }
    
    public Pair<Long, ChildAssociationRef> newChildAssoc(
            Long parentNodeId,
            Long childNodeId,
            boolean isPrimary,
            QName assocTypeQName,
            QName assocQName)
    {
        Node parentNode = (Node) getSession().get(NodeImpl.class, parentNodeId);
        Node childNode = (Node) getSession().get(NodeImpl.class, childNodeId);
        
        // assign a random name to the node
        String name = GUID.generate();
        
        ChildAssoc assoc = new ChildAssocImpl();
        assoc.setTypeQName(qnameDAO, assocTypeQName);
        assoc.setChildNodeName(name);
        assoc.setChildNodeNameCrc(-1L);         // random names compete only with each other
        assoc.setQName(qnameDAO, assocQName);
        assoc.setIsPrimary(isPrimary);
        assoc.setIndex(-1);
        // maintain inverse sets
        assoc.buildAssociation(parentNode, childNode);
        // persist it
        Long assocId = (Long) getHibernateTemplate().save(assoc);
        // Add it to the cache
        Set<Long> oldParentAssocIds = parentAssocsCache.get(childNode.getId());
        if (oldParentAssocIds != null)
        {
            Set<Long> newParentAssocIds = new HashSet<Long>(oldParentAssocIds);
            newParentAssocIds.add(assocId);
            parentAssocsCache.put(childNodeId, newParentAssocIds);
            if (isDebugParentAssocCacheEnabled)
            {
                loggerParentAssocsCache.debug("\n" +
                        "Parent associations cache - Updating entry: \n" +
                        "   Node:   " + childNodeId +  "\n" +
                        "   Before: " + oldParentAssocIds + "\n" +
                        "   After:  " + newParentAssocIds);
            }
        }
        
        // If this is a primary association then update the permissions
        if (isPrimary)
        {
            DbAccessControlList inherited = parentNode.getAccessControlList();
            if (inherited == null)
            {
                // not fixde up yet or unset
            }
            else
            {
                // Get the parent's inherited ACLs
                DbAccessControlList inheritedAcl = aclDaoComponent.getDbAccessControlList(
                        aclDaoComponent.getInheritedAccessControlList(inherited.getId()));
                childNode.setAccessControlList(inheritedAcl);
            }
        }
        
        // Record change ID
        recordNodeUpdate(childNode);

        // done
        return new Pair<Long, ChildAssociationRef>(assocId, assoc.getChildAssocRef(qnameDAO));
    }
    
    public void setChildNameUnique(final Long childAssocId, String childName)
    {
        /*
         * Work out if there has been any change in the name
         */
        
        final ChildAssoc childAssoc = getChildAssocNotNull(childAssocId);
        final Node parentNode = childAssoc.getParent();
        
        String childNameNew = null;
        long crc = -1;
        if (childName == null)
        {
            // If the name assigned is null, then the name that will be assigned will
            // be random.  Ofcourse, if the association already has a random name assigned
            // to it then there is no reason to assign a new one.  The update of the child
            // association is only required if the existing CRC value is not -1.
            long existingCrc = childAssoc.getChildNodeNameCrc();
            if (existingCrc == -1L)
            {
                if (isDebugEnabled)
                {
                    logger.debug(
                            "Child association name assignment is already random-based (non-clashing): \n" +
                            "   Parent Node: " + parentNode.getId() + "\n" +
                            "   Child Assoc: " + childAssoc.getId());
                }
                // Shortcut here
                return;
            }
            
            // random names compete only with each other, i.e. not at all
            childNameNew = GUID.generate();
            // The CRC of -1 indicates that the cm:name equivalent is non-clashing, i.e. a GUID
            crc = -1L;
        }
        else
        {
            // assigned names compete exactly
            childNameNew = childName.toLowerCase();
            crc = getCrc(childNameNew);
        }

        final String childNameNewShort = getShortName(childNameNew);
        final long childNameNewCrc = crc;

        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                // Update the association
                childAssoc.setChildNodeName(childNameNewShort);
                childAssoc.setChildNodeNameCrc(childNameNewCrc);
                // Flush again to force a DB constraint here
                DirtySessionMethodInterceptor.flushSession(session, true);
                // Done
                return null;
            }
        };
        // Make sure that all changes to the session are persisted so that we know if any
        // failures are from the constraint or not
        DirtySessionMethodInterceptor.flushSession(getSession(false));
        try
        {
            getHibernateTemplate().execute(callback);
        }
        catch (Throwable e)
        {
            // There is already an entity
            if (isDebugEnabled)
            {
                logger.debug(
                        "Duplicate child association detected: \n" +
                        "   Parent Node: " + parentNode.getId() + "\n" +
                        "   Child Name:  " + childName);
            }
            throw new DuplicateChildNodeNameException(
                    parentNode.getNodeRef(),
                    childAssoc.getTypeQName(qnameDAO),
                    childName);
        }
        
        // Done
        if (isDebugEnabled)
        {
            logger.debug(
                    "Updated child association: \n" +
                    "   Parent:      " + parentNode + "\n" +
                    "   Child Assoc: " + childAssoc);
        }
    }
    
    public Pair<Long, ChildAssociationRef> updateChildAssoc(
            Long childAssocId,
            Long parentNodeId,
            Long childNodeId,
            QName assocTypeQName,
            QName assocQName,
            int index)
    {
        final ChildAssoc childAssoc = getChildAssocNotNull(childAssocId);
        final boolean isPrimary = childAssoc.getIsPrimary();
        final Node oldParentNode = childAssoc.getParent();
        final Node oldChildNode = childAssoc.getChild();
        final NodeRef oldChildNodeRef = childAssoc.getChild().getNodeRef();
        final Node newParentNode = getNodeNotNull(parentNodeId);
        final Node newChildNode = getNodeNotNull(childNodeId);
        final NodeRef newChildNodeRef = newChildNode.getNodeRef();
        
        // Reset the cm:name duplicate handling.  This has to be redone, if required.
        String name = GUID.generate();
        childAssoc.setChildNodeName(name);
        childAssoc.setChildNodeNameCrc(-1L);

        childAssoc.buildAssociation(newParentNode, newChildNode);
        childAssoc.setTypeQName(qnameDAO, assocTypeQName);
        childAssoc.setQName(qnameDAO, assocQName);
        if (index >= 0)
        {
            childAssoc.setIndex(index);
        }

        // Record change ID
        if (oldChildNodeRef.equals(newChildNodeRef))
        {
            recordNodeUpdate(newChildNode);
        }
        else
        {
            recordNodeUpdate(newChildNode);
        }
        
        // Update the inherited associations if either the parent or child nodes have changed and
        // the association is primary
        if (isPrimary && (
                !oldParentNode.getId().equals(parentNodeId) ||
                !oldChildNode.getId().equals(childNodeId))
                )
        {
            if (newChildNode.getAccessControlList() != null)
            {
                Long targetAclId = newChildNode.getAccessControlList().getId();
                AccessControlListProperties aclProperties = aclDaoComponent.getAccessControlListProperties(targetAclId);
                Boolean targetAclInherits = aclProperties.getInherits();
                if ((targetAclInherits != null) && (targetAclInherits.booleanValue()))
                {
                    if (newParentNode.getAccessControlList() != null)
                    {
                        Long parentAclId = newParentNode.getAccessControlList().getId();
                        Long inheritedAclId = aclDaoComponent.getInheritedAccessControlList(parentAclId);
                        if (aclProperties.getAclType() == ACLType.DEFINING)
                        {
                            aclDaoComponent.enableInheritance(targetAclId, parentAclId);
                        }
                        else if (aclProperties.getAclType() == ACLType.SHARED)
                        {
                            setFixedAcls(childNodeId, inheritedAclId, true);
                        }
                    }
                    else
                    {
                        if (aclProperties.getAclType() == ACLType.DEFINING)
                        {
                            // there is nothing to inherit from so clear out any inherited aces
                            aclDaoComponent.deleteInheritedAccessControlEntries(targetAclId);
                        }
                        else if (aclProperties.getAclType() == ACLType.SHARED)
                        {
                            // there is nothing to inherit
                            newChildNode.setAccessControlList(null);
                        }

                        // throw new IllegalStateException("Share bug");
                    }
                }
            }
            else
            {
                if (newChildNode.getAccessControlList() != null)
                {
                    Long parentAcl = newParentNode.getAccessControlList().getId();
                    Long inheritedAcl = aclDaoComponent.getInheritedAccessControlList(parentAcl);
                    setFixedAcls(childNodeId, inheritedAcl, true);
                } 
            }
        }

        // Done
        return new Pair<Long, ChildAssociationRef>(childAssocId, childAssoc.getChildAssocRef(qnameDAO));
    }

    /**
     * This code is here, and not in another DAO, in order to avoid unnecessary circular callbacks
     * and cyclical dependencies.  It would be nice if the ACL code could be separated (or combined)
     * but the node tree walking code is best done right here.
     * 
     * @param nodeRef
     * @param mergeFromAclId
     * @param set
     */
    private void setFixedAcls(
            final Long nodeId,
            final Long mergeFromAclId,
            final boolean set)
    {
        Node mergeFromNode = getNodeNotNull(nodeId);
        
        if (set)
        {
            DbAccessControlList mergeFromAcl = aclDaoComponent.getDbAccessControlList(mergeFromAclId);
            mergeFromNode.setAccessControlList(mergeFromAcl);
        }

        final List<Long> childNodeIds = new ArrayList<Long>(100);
        NodeDaoService.ChildAssocRefQueryCallback callback = new NodeDaoService.ChildAssocRefQueryCallback()
        {
            public boolean handle(
                    Pair<Long, ChildAssociationRef> childAssocPair,
                    Pair<Long, NodeRef> parentNodePair,
                    Pair<Long, NodeRef> childNodePair)
            {
                // Ignore non-primary nodes
                if (!childAssocPair.getSecond().isPrimary())
                {
                    return false;
                }
                childNodeIds.add(childNodePair.getFirst());
                return false;
            }
        };
        // Get all child associations with the specific qualified name
        getChildAssocs(nodeId, callback, false);
        for (Long childNodeId : childNodeIds)
        {
            Node childNode = getNodeNotNull(childNodeId);
            DbAccessControlList acl = childNode.getAccessControlList();

            if (acl == null)
            {
                setFixedAcls(childNodeId, mergeFromAclId, true);
            }
            else if (acl.getAclType() == ACLType.LAYERED)
            {
                logger.error("LAYERED ACL present on ADM node: " + childNode);
                continue;
            }
            else if (acl.getAclType() == ACLType.DEFINING)
            {
                @SuppressWarnings("unused")
                List<AclChange> newChanges = aclDaoComponent.mergeInheritedAccessControlList(mergeFromAclId, acl.getId());
            }
            else
            {
                    setFixedAcls(childNodeId, mergeFromAclId, true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void getChildAssocs(final Long parentNodeId, final ChildAssocRefQueryCallback resultsCallback, final boolean recurse)
    {
        Node parentNode = getNodeNotNull(parentNodeId);
        
        ChildAssocRefQueryCallback queryCallback = resultsCallback;
        final List<Long> childNodeIds = new ArrayList<Long>(100);
        if (recurse)
        {
            // In order to recurse, without loading the DB with nested scrollable queries, we have to
            // record the IDs of the children coming from the query.  This is done by adding our own
            // callback to the results iterator and passing values to the client's callback from there.
            queryCallback = new ChildAssocRefQueryCallback()
            {
                public boolean handle(
                        Pair<Long, ChildAssociationRef> childAssocPair,
                        Pair<Long, NodeRef> parentNodePair,
                        Pair<Long, NodeRef> childNodePair)
                {
                    // Pass the values to the client code
                    boolean recurseLocal = resultsCallback.handle(childAssocPair, parentNodePair, childNodePair);
                    if (recurseLocal)
                    {
                        childNodeIds.add(childNodePair.getFirst());
                    }
                    return false;
                }
            };
        }
        
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_CHILD_ASSOC_REFS)
                    .setLong("parentId", parentNodeId);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults queryResults = null;
        try
        {
            queryResults = (ScrollableResults) getHibernateTemplate().execute(callback);
            convertToChildAssocRefs(parentNode, queryResults, queryCallback);

            // Now recurse, if required
            if (recurse)
            {
                for (Long childNodeId : childNodeIds)
                {
                    getChildAssocs(childNodeId, resultsCallback, recurse);
                }
            }
            // Done
        }
        finally
        {
            if (queryResults != null)
            {
                queryResults.close();
            }
        }
        // Done
    }
    
    @SuppressWarnings("unchecked")
    public void getChildAssocs(final Long parentNodeId, final QName assocQName, ChildAssocRefQueryCallback resultsCallback)
    {
        final Pair<Long, String> assocQNameNamespacePair = qnameDAO.getNamespace(assocQName.getNamespaceURI());
        final String assocQNameLocalName = assocQName.getLocalName();
        if (assocQNameNamespacePair == null)
        {
            // There can't be any matches
            return;
        }
        Node parentNode = getNodeNotNull(parentNodeId);
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_CHILD_ASSOC_REFS_BY_QNAME)
                    .setLong("parentId", parentNodeId)
                    .setLong("qnameNamespaceId", assocQNameNamespacePair.getFirst())
                    .setString("qnameLocalName", assocQNameLocalName);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults queryResults = null;
        try
        {
            queryResults = (ScrollableResults) getHibernateTemplate().execute(callback);
            convertToChildAssocRefs(parentNode, queryResults, resultsCallback);
        }
        finally
        {
            if (queryResults != null)
            {
                queryResults.close();
            }
        }
        // Done
    }

    public void getChildAssocsByTypeQNames(
            final Long parentNodeId,
            final List<QName> assocTypeQNames,
            ChildAssocRefQueryCallback resultsCallback)
    {
        // Convert the type QNames to entities
        
        final Set<QName> assocTypeQNameSet = new HashSet<QName>(assocTypeQNames);
        final Set<Long> assocTypeQNameIds = qnameDAO.convertQNamesToIds(assocTypeQNameSet, false);
        // Shortcut if there are no assoc types
        if (assocTypeQNameIds.size() == 0)
        {
            return;
        }
        
        Node parentNode = getNodeNotNull(parentNodeId);
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_CHILD_ASSOC_REFS_BY_TYPEQNAMES)
                    .setLong("parentId", parentNodeId)
                    .setParameterList("childAssocTypeQNameIds", assocTypeQNameIds);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults queryResults = null;
        try
        {
            queryResults = (ScrollableResults) getHibernateTemplate().execute(callback);
            convertToChildAssocRefs(parentNode, queryResults, resultsCallback);
        }
        finally
        {
            if (queryResults != null)
            {
                queryResults.close();
            }
        }
        // Done
    }

    public void getChildAssocsByTypeQNameAndQName(
            final Long parentNodeId,
            final QName assocTypeQName,
            final QName assocQName,
            ChildAssocRefQueryCallback resultsCallback)
    {
        Node parentNode = getNodeNotNull(parentNodeId);

        final Pair<Long, QName> assocTypeQNamePair = qnameDAO.getQName(assocTypeQName);
        final Pair<Long, String> assocQNameNamespacePair = qnameDAO.getNamespace(assocQName.getNamespaceURI());
        final String assocQNameLocalName = assocQName.getLocalName();
        // Shortcut if possible
        if (assocTypeQNamePair == null || assocQNameNamespacePair == null)
        {
            return;
        }
        
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_CHILD_ASSOC_REFS_BY_TYPEQNAME_AND_QNAME)
                    .setLong("parentId", parentNodeId)
                    .setLong("typeQNameId", assocTypeQNamePair.getFirst())
                    .setLong("qnameNamespaceId", assocQNameNamespacePair.getFirst())
                    .setString("qnameLocalName", assocQNameLocalName);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults queryResults = null;
        try
        {
            queryResults = (ScrollableResults) getHibernateTemplate().execute(callback);
            convertToChildAssocRefs(parentNode, queryResults, resultsCallback);
        }
        finally
        {
            if (queryResults != null)
            {
                queryResults.close();
            }
        }
        // Done
    }

    public void getChildAssocsByChildTypes(
            final Long parentNodeId,
            Set<QName> childNodeTypeQNames,
            ChildAssocRefQueryCallback resultsCallback)
    {
        Node parentNode = getNodeNotNull(parentNodeId);

        // Get the IDs for all the QNames we are after
        final Set<Long> childNodeTypeQNameIds = qnameDAO.convertQNamesToIds(childNodeTypeQNames, false);
        // Shortcut if there are no QNames available
        if (childNodeTypeQNameIds.size() == 0)
        {
            return;
        }
        
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_CHILD_ASSOC_REFS_BY_CHILD_TYPEQNAME)
                    .setLong("parentId", parentNodeId)
                    .setParameterList("childTypeQNameIds", childNodeTypeQNameIds);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults queryResults = null;
        try
        {
            queryResults = (ScrollableResults) getHibernateTemplate().execute(callback);
            convertToChildAssocRefs(parentNode, queryResults, resultsCallback);
        }
        finally
        {
            if (queryResults != null)
            {
                queryResults.close();
            }
        }
        // Done
    }

    public void getPrimaryChildAssocs(final Long parentNodeId, ChildAssocRefQueryCallback resultsCallback)
    {
        Node parentNode = getNodeNotNull(parentNodeId);
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_PRIMARY_CHILD_ASSOCS)
                    .setLong("parentId", parentNodeId);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults queryResults = null;
        try
        {
            queryResults = (ScrollableResults) getHibernateTemplate().execute(callback);
            convertToChildAssocRefs(parentNode, queryResults, resultsCallback);
        }
        finally
        {
            if (queryResults != null)
            {
                queryResults.close();
            }
        }
        // Done
    }

    public void getPrimaryChildAssocsNotInSameStore(final Long parentNodeId, ChildAssocRefQueryCallback resultsCallback)
    {
        Node parentNode = getNodeNotNull(parentNodeId);
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_PRIMARY_CHILD_ASSOCS_NOT_IN_SAME_STORE)
                    .setLong("parentId", parentNodeId);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults queryResults = null;
        try
        {
            queryResults = (ScrollableResults) getHibernateTemplate().execute(callback);
            convertToChildAssocRefs(parentNode, queryResults, resultsCallback);
        }
        finally
        {
            if (queryResults != null)
            {
                queryResults.close();
            }
        }
        // Done
    }

    public Pair<Long, ChildAssociationRef> getChildAssoc(final Long parentNodeId, final QName assocTypeQName, final String childName)
    {
        final Pair<Long, QName> assocTypeQNamePair = qnameDAO.getQName(assocTypeQName);
        // Shortcut
        if (assocTypeQNamePair == null)
        {
            return null;
        }
        
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                String childNameLower = childName.toLowerCase();
                String childNameShort = getShortName(childNameLower);
                long childNameLowerCrc = getCrc(childNameLower);
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_CHILD_ASSOC_BY_TYPE_AND_NAME)
                    .setLong("parentId", parentNodeId)
                    .setLong("typeQNameId", assocTypeQNamePair.getFirst())
                    .setString("childNodeName", childNameShort)
                    .setLong("childNodeNameCrc", childNameLowerCrc);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.uniqueResult();
            }
        };
        ChildAssoc childAssoc = (ChildAssoc) getHibernateTemplate().execute(callback);
        if (childAssoc == null)
        {
            return null;
        }
        else
        {
            return new Pair<Long, ChildAssociationRef>(childAssoc.getId(), childAssoc.getChildAssocRef(qnameDAO));
        }
    }

    public Pair<Long, ChildAssociationRef> getChildAssoc(
            final Long parentNodeId,
            final Long childNodeId,
            final QName assocTypeQName,
            final QName assocQName)
    {

        final Pair<Long, QName> assocTypeQNamePair = qnameDAO.getQName(assocTypeQName);
        final Pair<Long, String> assocQNameNamespacePair = qnameDAO.getNamespace(assocQName.getNamespaceURI());
        final String assocQNameLocalName = assocQName.getLocalName();
        // Shortcut if possible
        if (assocTypeQNamePair == null || assocQNameNamespacePair == null)
        {
            return null;
        }
        
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_CHILD_ASSOCS_BY_ALL)
                    .setLong("parentId", parentNodeId)
                    .setLong("childId", childNodeId)
                    .setLong("typeQNameId", assocTypeQNamePair.getFirst())
                    .setParameter("qnameNamespaceId", assocQNameNamespacePair.getFirst())
                    .setParameter("qnameLocalName", assocQNameLocalName);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.uniqueResult();
            }
        };
        ChildAssoc childAssoc = (ChildAssoc) getHibernateTemplate().execute(callback);
        if (childAssoc == null)
        {
            return null;
        }
        else
        {
            return new Pair<Long, ChildAssociationRef>(childAssoc.getId(), childAssoc.getChildAssocRef(qnameDAO));
        }
    }
    
    /**
     * Columns returned are:
     * <pre>
         0 assoc.id,
         1 assoc.typeQName,
         2 assoc.qnameNamespace,
         3 assoc.qnameLocalName,
         4 assoc.isPrimary,
         5 assoc.index,
         6 child.id,
         7 child.store.key.protocol,
         8 child.store.key.identifier,
         9 child.uuid
     * </pre> 
     */
    private void convertToChildAssocRefs(Node parentNode, ScrollableResults results, ChildAssocRefQueryCallback resultsCallback)
    {
        Long parentNodeId = parentNode.getId();
        NodeRef parentNodeRef = parentNode.getNodeRef();
        Pair<Long, NodeRef> parentNodePair = new Pair<Long, NodeRef>(parentNodeId, parentNodeRef);
        while (results.next())
        {
            Object[] row = results.get();
            Long assocId = (Long) row[0];
            QName assocTypeQName = qnameDAO.getQName((Long) row[1]).getSecond();
            String assocQNameNamespace = qnameDAO.getNamespace((Long) row[2]).getSecond();
            String assocQNameLocalName = (String) row[3];
            QName assocQName = QName.createQName(assocQNameNamespace, assocQNameLocalName);
            Boolean assocIsPrimary = (Boolean) row[4];
            Integer assocIndex = (Integer) row[5];
            Long childNodeId = (Long) row[6];
            String childProtocol = (String) row[7];
            String childIdentifier = (String) row[8];
            String childUuid = (String) row[9];
            NodeRef childNodeRef = new NodeRef(new StoreRef(childProtocol, childIdentifier), childUuid);
            ChildAssociationRef assocRef = new ChildAssociationRef(
                    assocTypeQName,
                    parentNodeRef,
                    assocQName,
                    childNodeRef,
                    assocIsPrimary.booleanValue(),
                    assocIndex.intValue());
            Pair<Long, ChildAssociationRef> assocPair = new Pair<Long, ChildAssociationRef>(assocId, assocRef);
            Pair<Long, NodeRef> childNodePair = new Pair<Long, NodeRef>(childNodeId, childNodeRef);
            // Call back
            resultsCallback.handle(assocPair, parentNodePair, childNodePair);
        }
    }
    
    private Collection<Pair<Long, AssociationRef>> convertToAssocRefs(List<NodeAssoc> queryResults)
    {
        Collection<Pair<Long, AssociationRef>> refs = new ArrayList<Pair<Long, AssociationRef>>(queryResults.size());
        for (NodeAssoc nodeAssoc : queryResults)
        {
            Long nodeAssocId = nodeAssoc.getId();
            AssociationRef assocRef = nodeAssoc.getNodeAssocRef(qnameDAO);
            refs.add(new Pair<Long, AssociationRef>(nodeAssocId, assocRef));
        }
        return refs;
    }
    
    public void getNodesWithAspect(
            final QName aspectQName,
            final Long minNodeId,
            final int count,
            NodeRefQueryCallback resultsCallback)
    {
        final Pair<Long, QName> aspectQNamePair = qnameDAO.getQName(aspectQName);
        // Shortcut
        if (aspectQNamePair == null)
        {
            return;
        }
        
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_NODES_WITH_ASPECT)
                    .setLong("aspectQNameId", aspectQNamePair.getFirst())
                    .setLong("minNodeId", minNodeId)
                    .setMaxResults(count);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults queryResults = null;
        try
        {
            queryResults = (ScrollableResults) getHibernateTemplate().execute(callback);
            processNodeResults(queryResults, resultsCallback);
        }
        finally
        {
            if (queryResults != null)
            {
                queryResults.close();
            }
        }

        // Done
    }

    public void getNodesWithChildrenInDifferentStores(final Long minNodeId, final int count, NodeRefQueryCallback resultsCallback)
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_NODES_WITH_CHILDREN_IN_DIFFERENT_STORES)
                    .setLong("minNodeId", minNodeId)
                    .setMaxResults(count);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults queryResults = null;
        try
        {
            queryResults = (ScrollableResults) getHibernateTemplate().execute(callback);
            processNodeResults(queryResults, resultsCallback);
        }
        finally
        {
            if (queryResults != null)
            {
                queryResults.close();
            }
        }

        // Done
    }
    
    /**
     * <pre>
            Long parentId = (Long) row[0];
            String parentProtocol = (String) row[1];
            String parentIdentifier = (String) row[2];
            String parentUuid = (String) row[3];
     * </pre>
     */
    private void processNodeResults(ScrollableResults queryResults, NodeRefQueryCallback resultsCallback)
    {
        while (queryResults.next())
        {
            Object[] row = queryResults.get();
            Long parentId = (Long) row[0];
            String parentProtocol = (String) row[1];
            String parentIdentifier = (String) row[2];
            String parentUuid = (String) row[3];
            NodeRef parentNodeRef = new NodeRef(parentProtocol, parentIdentifier, parentUuid);
            Pair<Long, NodeRef> parentNodePair = new Pair<Long, NodeRef>(parentId, parentNodeRef);
            // Call back
            boolean moreRequired = resultsCallback.handle(parentNodePair);
            if (!moreRequired)
            {
                break;
            }
        }
    }

    public void deleteChildAssoc(Long assocId)
    {
        Set<Long> deletedChildAssocIds = new HashSet<Long>(10);
        ChildAssoc assoc = getChildAssocNotNull(assocId);
        deleteChildAssocInternal(assoc, false, deletedChildAssocIds);
    }

    @SuppressWarnings("unchecked")
    public boolean deleteChildAssoc(
            final Long parentNodeId,
            final Long childNodeId,
            final QName assocTypeQName,
            final QName assocQName)
    {
        final Pair<Long, QName> assocTypeQNamePair = qnameDAO.getQName(assocTypeQName);
        final Pair<Long, String> assocQNameNamespacePair = qnameDAO.getNamespace(assocQName.getNamespaceURI());
        final String assocQNameLocalName = assocQName.getLocalName();
        
        // Shortcut
        if (assocTypeQNamePair == null || assocQNameNamespacePair == null)
        {
            return false;
        }

        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_CHILD_ASSOCS_BY_ALL)
                    .setLong("parentId", parentNodeId)
                    .setLong("childId", childNodeId)
                    .setLong("typeQNameId", assocTypeQNamePair.getFirst())
                    .setParameter("qnameNamespaceId", assocQNameNamespacePair.getFirst())
                    .setParameter("qnameLocalName", assocQNameLocalName);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.list();
            }
        };
        List<ChildAssoc> childAssocs = (List<ChildAssoc>) getHibernateTemplate().execute(callback);
        // Remove each child association with full cascade
        for (ChildAssoc assoc : childAssocs)
        {
            deleteChildAssocInternal(assoc, false, new HashSet<Long>(0));
        }
        return (childAssocs.size() > 0);
    }

    /**
     * Cascade deletion of child associations, recording the IDs of deleted assocs.
     * 
     * @param assoc the association to delete
     * @param cascade true to cascade to the child node of the association
     * @param deletedChildAssocIds already-deleted associations
     */
    private void deleteChildAssocInternal(final ChildAssoc assoc, boolean cascade, Set<Long> deletedChildAssocIds)
    {
        Long childAssocId = assoc.getId();
        
        if (deletedChildAssocIds.contains(childAssocId))
        {
            if (isDebugEnabled)
            {
                logger.debug("Ignoring parent-child association " + assoc.getId());
            }
            return;
        }
        
        if (isDebugEnabled)
        {
            logger.debug(
                    "Deleting parent-child association " + assoc.getId() +
                    (cascade ? " with" : " without") + " cascade:" +
                    assoc.getParent().getId() + " -> " + assoc.getChild().getId());
        }

        Node childNode = assoc.getChild();
        Long childNodeId = childNode.getId();
        
        // Add remove the child association from the cache
        Set<Long> oldParentAssocIds = parentAssocsCache.get(childNodeId);
        if (oldParentAssocIds != null)
        {
            Set<Long> newParentAssocIds = new HashSet<Long>(oldParentAssocIds);
            newParentAssocIds.remove(childAssocId);
            parentAssocsCache.put(childNodeId, newParentAssocIds);
            loggerParentAssocsCache.debug("\n" +
                    "Parent associations cache - Updating entry: \n" +
                    "   Node:   " + childNodeId +  "\n" +
                    "   Before: " + oldParentAssocIds + "\n" +
                    "   After:  " + newParentAssocIds);
        }
        
        // maintain inverse association sets
        assoc.removeAssociation();
        // remove instance
        getHibernateTemplate().delete(assoc);
//        // ensure that we don't attempt to delete it twice
//        deletedChildAssocIds.add(childAssocId);
//        
//        if (cascade && assoc.getIsPrimary())   // the assoc is primary
//        {
//            // delete the child node
//            deleteNodeInternal(childNode, cascade, deletedChildAssocIds);
//            /*
//             * The child node deletion will cascade delete all assocs to
//             * and from it, but we have safely removed this one, so no
//             * duplicate call will be received to do this
//             */
//        }
    }

    /**
     * @param childNode         the child node
     * @return                  Returns the parent associations without any interpretation
     */
    @SuppressWarnings("unchecked")
    private Collection<ChildAssoc> getParentAssocsInternal(final Long childNodeId)
    {
        List<ChildAssoc> parentAssocs = null;
        // First check the cache
        Set<Long> parentAssocIds = parentAssocsCache.get(childNodeId);
        if (parentAssocIds != null)
        {
            if (isDebugParentAssocCacheEnabled)
            {
                loggerParentAssocsCache.debug("\n" +
                        "Parent associations cache - Hit: \n" +
                        "   Node:   " + childNodeId + "\n" +
                        "   Assocs: " + parentAssocIds);
            }
            parentAssocs = new ArrayList<ChildAssoc>(parentAssocIds.size());
            for (Long parentAssocId : parentAssocIds)
            {
                ChildAssoc parentAssoc = (ChildAssoc) getSession().get(ChildAssocImpl.class, parentAssocId);
                if (parentAssoc == null)
                {
                    // The cache is out of date, so just repopulate it
                    parentAssocs = null;
                    break;
                }
                else
                {
                    parentAssocs.add(parentAssoc);
                }
            }
        }
        // Did we manage to get the parent assocs
        if (parentAssocs == null)
        {
            if (isDebugParentAssocCacheEnabled)
            {
                loggerParentAssocsCache.debug("\n" +
                        "Parent associations cache - Miss: \n" +
                        "   Node:   " + childNodeId + "\n" +
                        "   Assocs: " + parentAssocIds);
            }
            HibernateCallback callback = new HibernateCallback()
            {
                public Object doInHibernate(Session session)
                {
                    Query query = session
                        .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_PARENT_ASSOCS)
                        .setLong("childId", childNodeId);
                    DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                    return query.list();
                }
            };
            List<Object[]> rows = (List<Object[]>) getHibernateTemplate().execute(callback);
            parentAssocs = new ArrayList<ChildAssoc>(rows.size());
            parentAssocIds = new HashSet<Long>(parentAssocs.size());
            for (Object[] row : rows)
            {
                ChildAssoc parentAssoc = (ChildAssoc) row[0];
                // Populate the results
                parentAssocs.add(parentAssoc);
                parentAssocIds.add(parentAssoc.getId());
            }
            // Populate the cache
            parentAssocsCache.put(childNodeId, parentAssocIds);
            if (isDebugParentAssocCacheEnabled)
            {
                loggerParentAssocsCache.debug("\n" +
                        "Parent associations cache - Adding entry: \n" +
                        "   Node:   " + childNodeId + "\n" +
                        "   Assocs: " + parentAssocIds);
            }
        }
        // Done
        return parentAssocs;
    }
    
    /**
     * {@inheritDoc}
     * 
     * This includes a check to ensuret that only root nodes don't have primary parents
     */
    public Collection<Pair<Long, ChildAssociationRef>> getParentAssocs(final Long childNodeId)
    {
        Collection<ChildAssoc> parentAssocs = getParentAssocsInternal(childNodeId);
        Collection<Pair<Long, ChildAssociationRef>> ret = new ArrayList<Pair<Long, ChildAssociationRef>>(parentAssocs.size());
        
        for (ChildAssoc childAssoc : parentAssocs)
        {
            Long childAssocId = childAssoc.getId();
            ChildAssociationRef childAssocRef = childAssoc.getChildAssocRef(qnameDAO);
            Pair<Long, ChildAssociationRef> childAssocPair = new Pair<Long, ChildAssociationRef>(childAssocId, childAssocRef);
            ret.add(childAssocPair);
        }
        // Done
        return ret;
    }

    private Set<Long> warnedDuplicateParents = new HashSet<Long>(3);
    /**
     * {@inheritDoc}
     * 
     * This method includes a check for multiple primary parent associations.
     * The check doesn't fail but will warn (once per instance) of the occurence of
     * the error.  It is up to the administrator to fix the issue at the moment, but
     * the server will not stop working.
     */
    public Pair<Long, ChildAssociationRef> getPrimaryParentAssoc(Long childNodeId)
    {
        // get the assocs pointing to the node
        Collection<ChildAssoc> parentAssocs = getParentAssocsInternal(childNodeId);
        ChildAssoc primaryAssoc = null;
        for (ChildAssoc assoc : parentAssocs)
        {
            // ignore non-primary assocs
            if (!assoc.getIsPrimary())
            {
                continue;
            }
            else if (primaryAssoc != null)
            {
                // We have found one already.
                synchronized(warnedDuplicateParents)
                {
                    boolean added = warnedDuplicateParents.add(childNodeId);
                    if (added)
                    {
                        logger.warn(
                                "Multiple primary associations: \n" +
                                "   first primary assoc: " + primaryAssoc + "\n" +
                                "   second primary assoc: " + assoc + "\n" +
                                "When running in a cluster, check that the caches are properly shared.");
                    }
                }
            }
            primaryAssoc = assoc;
            // we keep looping to hunt out data integrity issues
        }
        // done
        if (primaryAssoc == null)
        {
            return null;
        }
        else
        {
            return new Pair<Long, ChildAssociationRef>(primaryAssoc.getId(), primaryAssoc.getChildAssocRef(qnameDAO));
        }
    }

    public Pair<Long, AssociationRef> newNodeAssoc(Long sourceNodeId, Long targetNodeId, final QName assocTypeQName)
    {
        final Node sourceNode = getNodeNotNull(sourceNodeId);
        final Node targetNode = getNodeNotNull(targetNodeId);
        
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                // Force a flush here to ensure that the session is not dirty
                DirtySessionMethodInterceptor.flushSession(session, true);

                NodeAssoc assoc = new NodeAssocImpl();
                assoc.setTypeQName(qnameDAO, assocTypeQName);
                assoc.buildAssociation(sourceNode, targetNode);
                session.save(assoc);
                
                // Flush to catch integrity violations
                DirtySessionMethodInterceptor.flushSession(session, true);
                
                return assoc;
            }
        };
        
        // persist
        try
        {
            NodeAssoc assoc = (NodeAssoc) getHibernateTemplate().execute(callback);
            // done
            return new Pair<Long, AssociationRef>(assoc.getId(), assoc.getNodeAssocRef(qnameDAO));
        }
        catch (DataIntegrityViolationException e)
        {
            throw new AssociationExistsException(
                    sourceNode.getNodeRef(),
                    targetNode.getNodeRef(),
                    assocTypeQName,
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<Pair<Long, AssociationRef>> getNodeAssocsToAndFrom(final Long nodeId)
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                        .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_NODE_ASSOCS_TO_AND_FROM)
                        .setLong("nodeId", nodeId);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.list();
            }
        };
        List<NodeAssoc> results = (List<NodeAssoc>) getHibernateTemplate().execute(callback);
        Collection<Pair<Long, AssociationRef>> ret = convertToAssocRefs(results);
        return ret;
    }

    public Pair<Long, AssociationRef> getNodeAssoc(
            final Long sourceNodeId,
            final Long targetNodeId,
            final QName assocTypeQName)
    {
        final Pair<Long, QName> assocTypeQNamePair = qnameDAO.getQName(assocTypeQName);
        // Shortcut
        if (assocTypeQNamePair == null)
        {
            return null;
        }
        
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                        .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_NODE_ASSOC)
                        .setLong("sourceId", sourceNodeId)
                        .setLong("targetId", targetNodeId)
                        .setLong("assocTypeQNameId", assocTypeQNamePair.getFirst());
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.uniqueResult();
            }
        };
        NodeAssoc result = (NodeAssoc) getHibernateTemplate().execute(callback);
        Pair<Long, AssociationRef> ret = new Pair<Long, AssociationRef>(result.getId(), result.getNodeAssocRef(qnameDAO));
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Collection<Pair<Long, AssociationRef>> getTargetNodeAssocs(final Long sourceNodeId)
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_TARGET_ASSOCS)
                    .setLong("sourceId", sourceNodeId);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.list();
            }
        };
        List<NodeAssoc> results = (List<NodeAssoc>) getHibernateTemplate().execute(callback);
        Collection<Pair<Long, AssociationRef>> ret = convertToAssocRefs(results);
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Collection<Pair<Long, AssociationRef>> getSourceNodeAssocs(final Long targetNodeId)
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_SOURCE_ASSOCS)
                    .setLong("targetId", targetNodeId);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.list();
            }
        };
        List<NodeAssoc> results = (List<NodeAssoc>) getHibernateTemplate().execute(callback);
        Collection<Pair<Long, AssociationRef>> ret = convertToAssocRefs(results);
        return ret;
    }

    public void deleteNodeAssoc(Long assocId)
    {
        NodeAssoc assoc = (NodeAssoc) getHibernateTemplate().get(NodeAssocImpl.class, assocId);
        if (assoc != null)
        {
            getHibernateTemplate().delete(assoc);
        }
    }

    public void getPropertyValuesByPropertyAndValue(
            final StoreRef storeRef,
            final QName propertyQName,
            final String value,
            final NodePropertyHandler handler)
    {
	    Pair<Long, QName> propQNamePair = qnameDAO.getQName(propertyQName);
        // Shortcut
        if (propQNamePair == null)
        {
            return;
        }
        final Long propQNameEntityId = propQNamePair.getFirst();
        // Run the query
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                  .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_NODES_WITH_PROPERTY_VALUES_BY_STRING_AND_STORE)
                  .setString("storeProtocol", storeRef.getProtocol())
                  .setString("storeIdentifier", storeRef.getIdentifier())
                  .setParameter("propQNameID", propQNameEntityId)
                  .setString("propStringValue", value)
                  ;
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults results = null;
        try
        {
            results = (ScrollableResults) getHibernateTemplate().execute(callback);
            // Callback with the results
            Session session = getSession();
            while (results.next())
            {
                Node node = (Node) results.get(0);
                NodeRef nodeRef = node.getNodeRef();
                Long nodeTypeQNameId = node.getTypeQNameId();
				QName nodeTypeQName = qnameDAO.getQName(nodeTypeQNameId).getSecond();
                handler.handle(nodeRef, nodeTypeQName, propertyQName, value);
                // Flush if required
                DirtySessionMethodInterceptor.flushSession(session);
            }
        }
        finally
        {
            if (results != null)
            {
                results.close();
            }
        }
    }

    public void getContentUrlsForStore(
            final StoreRef storeRef,
            final ObjectArrayQueryCallback resultsCallback)
    {
        final Long contentTypeQNameEntityId = qnameDAO.getOrCreateQName(ContentModel.TYPE_CONTENT).getFirst();
        final Long ownerPropQNameEntityId = qnameDAO.getOrCreateQName(ContentModel.PROP_OWNER).getFirst();
        final Long contentPropQNameEntityId = qnameDAO.getOrCreateQName(ContentModel.PROP_CONTENT).getFirst();
        
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_CONTENT_URLS_FOR_STORE)
                    .setString("storeProtocol", storeRef.getProtocol())
                    .setString("storeIdentifier", storeRef.getIdentifier())
                    .setParameter("ownerPropQNameID", ownerPropQNameEntityId) // cm:owner
                    .setParameter("contentPropQNameID", contentPropQNameEntityId) // cm:content
                    .setParameter("contentTypeQNameID", contentTypeQNameEntityId) // cm:content
                    ;
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults results = null;
        try
        {
            results = (ScrollableResults) getHibernateTemplate().execute(callback);
            // Callback with the results
            Session session = getSession();
            while (results.next())
            {
                Object[] arr = new Object[3];
                arr[0] = (String)results.get(0); // owner (can be null)
                arr[1] = (String)results.get(1); // creator
                arr[2] = (String)results.get(2); // contentUrl
                resultsCallback.handle(arr);
                // Flush if required
                DirtySessionMethodInterceptor.flushSession(session);
            }
        }
        finally
        {
            if (results != null)
            {
                results.close();
            }
        }

        // Done
    }
    
    public void getUsersWithoutUsage(
            final StoreRef storeRef,
            final ObjectArrayQueryCallback resultsCallback)
    {
        final Long personTypeQNameEntityId = qnameDAO.getOrCreateQName(ContentModel.TYPE_PERSON).getFirst();
        final Long usernamePropQNameEntityId = qnameDAO.getOrCreateQName(ContentModel.PROP_USERNAME).getFirst();
        final Long sizeCurrentPropQNameEntityId = qnameDAO.getOrCreateQName(ContentModel.PROP_SIZE_CURRENT).getFirst();
        
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_USERS_WITHOUT_USAGE)
                    .setString("storeProtocol", storeRef.getProtocol())
                    .setString("storeIdentifier", storeRef.getIdentifier())
                    .setParameter("usernamePropQNameID", usernamePropQNameEntityId) // cm:username
                    .setParameter("sizeCurrentPropQNameID", sizeCurrentPropQNameEntityId) // cm:sizeCurrent
                    .setParameter("personTypeQNameID", personTypeQNameEntityId) // cm:person
                    ;
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults results = null;
        try
        {
            results = (ScrollableResults) getHibernateTemplate().execute(callback);
            // Callback with the results
            Session session = getSession();
            while (results.next())
            {
                Object[] arr = new Object[2];
                arr[0] = (String)results.get(0); // username
                arr[1] = (String)results.get(1); // node uuid
                resultsCallback.handle(arr);
                // Flush if required
                DirtySessionMethodInterceptor.flushSession(session);
            }
        }
        finally
        {
            if (results != null)
            {
                results.close();
            }
        }

        // Done
    }
    
    public void getUsersWithUsage(
            final StoreRef storeRef,
            final ObjectArrayQueryCallback resultsCallback)
    {
        final Long personTypeQNameEntityId = qnameDAO.getOrCreateQName(ContentModel.TYPE_PERSON).getFirst();
        final Long usernamePropQNameEntityId = qnameDAO.getOrCreateQName(ContentModel.PROP_USERNAME).getFirst();
        final Long sizeCurrentPropQNameEntityId = qnameDAO.getOrCreateQName(ContentModel.PROP_SIZE_CURRENT).getFirst();
        
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                    .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_USERS_WITH_USAGE)
                    .setString("storeProtocol", storeRef.getProtocol())
                    .setString("storeIdentifier", storeRef.getIdentifier())
                    .setParameter("usernamePropQNameID", usernamePropQNameEntityId) // cm:username
                    .setParameter("sizeCurrentPropQNameID", sizeCurrentPropQNameEntityId) // cm:sizeCurrent
                    .setParameter("personTypeQNameID", personTypeQNameEntityId) // cm:person
                    ;
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults results = null;
        try
        {
            results = (ScrollableResults) getHibernateTemplate().execute(callback);
            // Callback with the results
            Session session = getSession();
            while (results.next())
            {
                Object[] arr = new Object[2];
                arr[0] = (String)results.get(0); // username
                arr[1] = (String)results.get(1); // node uuid
                resultsCallback.handle(arr);
                // Flush if required
                DirtySessionMethodInterceptor.flushSession(session);
            }
        }
        finally
        {
            if (results != null)
            {
                results.close();
            }
        }

        // Done
    }
    
    public void getPropertyValuesByActualType(DataTypeDefinition actualDataTypeDefinition, NodePropertyHandler handler)
    {
        // get the in-database string representation of the actual type
        QName typeQName = actualDataTypeDefinition.getName();
        final int actualTypeOrdinal = PropertyValue.convertToTypeOrdinal(typeQName);
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session
                  .getNamedQuery(HibernateNodeDaoServiceImpl.QUERY_GET_NODES_WITH_PROPERTY_VALUES_BY_ACTUAL_TYPE)
                  .setInteger("actualType", actualTypeOrdinal);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.scroll(ScrollMode.FORWARD_ONLY);
            }
        };
        ScrollableResults results = null;
        try
        {
            results = (ScrollableResults) getHibernateTemplate().execute(callback);

            // Loop through, extracting content URLs
            TypeConverter converter = DefaultTypeConverter.INSTANCE;
            int unflushedCount = 0;
            while (results.next())
            {
                Node node = (Node) results.get()[0];
                Long nodeTypeQNameId = node.getTypeQNameId();
                QName nodeTypeQName = qnameDAO.getQName(nodeTypeQNameId).getSecond();
                // loop through all the node properties
                Map<PropertyMapKey, NodePropertyValue> properties = node.getProperties();
                for (Map.Entry<PropertyMapKey, NodePropertyValue> entry : properties.entrySet())
                {
				    PropertyMapKey propertyKey = entry.getKey();
					Long propertyQNameId = propertyKey.getQnameId();
                    QName propertyQName = qnameDAO.getQName(propertyQNameId).getSecond();
                    NodePropertyValue propertyValue = entry.getValue();
                    // ignore nulls
                    if (propertyValue == null)
                    {
                        continue;
                    }
                    // Get the actual value(s) as a collection
                    Collection<Serializable> values = propertyValue.getCollection(DataTypeDefinition.ANY);
                    // attempt to convert instance in the collection
                    for (Serializable value : values)
                    {
                        // ignore nulls (null entries in collections)
                        if (value == null)
                        {
                            continue;
                        }
                        Serializable convertedValue = null;
                        try
                        {
                            convertedValue = (Serializable) converter.convert(actualDataTypeDefinition, value);
                        }
                        catch (Throwable e)
                        {
                            // The value can't be converted - forget it
                        }
                        if (convertedValue != null)
                        {
                            NodeRef nodeRef = node.getNodeRef();
                            handler.handle(nodeRef, nodeTypeQName, propertyQName, convertedValue);
                        }
                    }
                }
                unflushedCount++;
                if (unflushedCount >= 1000)
                {
                    // evict all data from the session
                    getSession().clear();
                    unflushedCount = 0;
                }
            }
        }
        finally
        {
            if (results != null)
            {
                results.close();
            }
        }
    }
    
    /*
     * Queries for transactions
     */
    private static final String QUERY_GET_TXN_BY_ID = "txn.GetTxnById";
    private static final String QUERY_GET_MIN_COMMIT_TIME = "txn.GetMinCommitTime";
    private static final String QUERY_GET_MAX_COMMIT_TIME = "txn.GetMaxCommitTime";
    private static final String QUERY_GET_TXNS_BY_COMMIT_TIME_ASC = "txn.GetTxnsByCommitTimeAsc";
    private static final String QUERY_GET_TXNS_BY_COMMIT_TIME_DESC = "txn.GetTxnsByCommitTimeDesc";
    private static final String QUERY_GET_SELECTED_TXNS_BY_COMMIT_TIME_ASC = "txn.GetSelectedTxnsByCommitAsc";
    private static final String QUERY_GET_TXN_UPDATE_COUNT_FOR_STORE = "txn.GetTxnUpdateCountForStore";
    private static final String QUERY_GET_TXN_DELETE_COUNT_FOR_STORE = "txn.GetTxnDeleteCountForStore";
    private static final String QUERY_COUNT_TRANSACTIONS = "txn.CountTransactions";
    private static final String QUERY_GET_TXN_CHANGES_FOR_STORE = "txn.GetTxnChangesForStore";
    private static final String QUERY_GET_TXN_CHANGES = "txn.GetTxnChanges";
    
    public Transaction getTxnById(final long txnId)
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_TXN_BY_ID);
                query.setLong("txnId", txnId)
                     .setReadOnly(true);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.uniqueResult();
            }
        };
        Transaction txn = (Transaction) getHibernateTemplate().execute(callback);
        // done
        return txn;
    }
    
    public Long getMinTxnCommitTime()
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_MIN_COMMIT_TIME);
                query.setReadOnly(true);
                return query.uniqueResult();
            }
        };
        Long commitTime = (Long) getHibernateTemplate().execute(callback);
        // done
        return (commitTime == null) ? 0L : commitTime;
    }
    
    public Long getMaxTxnCommitTime()
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_MAX_COMMIT_TIME);
                query.setReadOnly(true);
                return query.uniqueResult();
            }
        };
        Long commitTime = (Long) getHibernateTemplate().execute(callback);
        // done
        return (commitTime == null) ? 0L : commitTime;
    }
    
    @SuppressWarnings("unchecked")
    public List<Transaction> getTxnsByMinCommitTime(final List<Long> includeTxnIds)
    {
        if (includeTxnIds.size() == 0)
        {
            return null;
        }
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_SELECTED_TXNS_BY_COMMIT_TIME_ASC);
                query.setParameterList("includeTxnIds", includeTxnIds)
                     .setReadOnly(true);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.list();
            }
        };
        List<Transaction> txns = (List<Transaction>) getHibernateTemplate().execute(callback);
        // done
        return txns;
    }

    @SuppressWarnings("unchecked")
    public int getTxnUpdateCount(final long txnId)
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_TXN_UPDATE_COUNT_FOR_STORE);
                query.setLong("txnId", txnId)
                     .setReadOnly(true);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.uniqueResult();
            }
        };
        Long count = (Long) getHibernateTemplate().execute(callback);
        // done
        return count.intValue();
    }
    
    @SuppressWarnings("unchecked")
    public int getTxnDeleteCount(final long txnId)
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_TXN_DELETE_COUNT_FOR_STORE);
                query.setLong("txnId", txnId)
                     .setReadOnly(true);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.uniqueResult();
            }
        };
        Long count = (Long) getHibernateTemplate().execute(callback);
        // done
        return count.intValue();
    }
    
    @SuppressWarnings("unchecked")
    public int getTransactionCount()
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_COUNT_TRANSACTIONS);
                query.setMaxResults(1)
                     .setReadOnly(true);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.uniqueResult();
            }
        };
        Long count = (Long) getHibernateTemplate().execute(callback);
        // done
        return count.intValue();
    }
    
    private static final Long TXN_ID_DUD = Long.valueOf(-1L);
    private static final Long SERVER_ID_DUD = Long.valueOf(-1L);
    private static final long MIN_TIME_QUERY_RANGE = 10L * 60L * 1000L;     // 10 minutes
    
    @SuppressWarnings("unchecked")
    public List<Transaction> getTxnsByCommitTimeAscending(
            long fromTimeInclusive,
            long toTimeExclusive,
            int count,
            List<Long> excludeTxnIds,
            boolean remoteOnly)
    {
        // Start with some sane defaults
        if (fromTimeInclusive < 0L)
        {
            fromTimeInclusive = getMinTxnCommitTime();
        }
        if (toTimeExclusive < 0L || toTimeExclusive == Long.MAX_VALUE)
        {
            toTimeExclusive = ((long)getMaxTxnCommitTime())+1L;
        }
        // Get the time difference required
        long diffTime = toTimeExclusive - fromTimeInclusive;
        if (diffTime <= 0)
        {
            // There can be no results
            return Collections.emptyList();
        }
        
        // Make sure that we have at least one entry in the exclude list
        final List<Long> excludeTxnIdsInner = new ArrayList<Long>(excludeTxnIds == null ? 1 : excludeTxnIds.size());
        if (excludeTxnIds == null || excludeTxnIds.isEmpty())
        {
            excludeTxnIdsInner.add(TXN_ID_DUD);
        }
        else
        {
            excludeTxnIdsInner.addAll(excludeTxnIds);
        }
        final List<Long> excludeServerIds = new ArrayList<Long>(1);
        if (remoteOnly)
        {
            // Get the current server ID.  This can be null if no transactions have been written by
            // a server with this IP address.
            Long serverId = getServerIdOrNull();
            if (serverId == null)
            {
                excludeServerIds.add(SERVER_ID_DUD);
            }
            else
            {
                excludeServerIds.add(serverId);
            }
        }
        else
        {
            excludeServerIds.add(SERVER_ID_DUD);
        }
        
        List<Transaction> results = new ArrayList<Transaction>(count);
        // Each query must be constrained in the time range,
        // so query larger and larger sets until enough results are retrieved.
        long iteration = 0L;
        long queryFromTimeInclusive = fromTimeInclusive;
        long queryToTimeExclusive = fromTimeInclusive;
        int queryCount = count;
        while ((results.size() < count) && (queryToTimeExclusive <= toTimeExclusive))
        {
            iteration++;
            queryFromTimeInclusive = queryToTimeExclusive;
            queryToTimeExclusive += (iteration * MIN_TIME_QUERY_RANGE);
            queryCount = count - results.size();
        
            final long innerQueryFromTimeInclusive = queryFromTimeInclusive;
            final long innerQueryToTimeExclusive = queryToTimeExclusive;
            final int innerQueryCount = queryCount;
            HibernateCallback callback = new HibernateCallback()
            {
                public Object doInHibernate(Session session)
                {
                    Query query = session.getNamedQuery(QUERY_GET_TXNS_BY_COMMIT_TIME_ASC);
                    query.setLong("fromTimeInclusive", innerQueryFromTimeInclusive)
                         .setLong("toTimeExclusive", innerQueryToTimeExclusive)
                         .setParameterList("excludeTxnIds", excludeTxnIdsInner)
                         .setParameterList("excludeServerIds", excludeServerIds)
                         .setMaxResults(innerQueryCount)
                         .setReadOnly(true);
                    return query.list();
                }
            };
            List<Transaction> queryResults = (List<Transaction>) getHibernateTemplate().execute(callback);
            // Copy results over
            results.addAll(queryResults);
        }
        // done
        return results;
    }
    
    @SuppressWarnings("unchecked")
    public List<Transaction> getTxnsByCommitTimeDescending(
            long fromTimeInclusive,
            long toTimeExclusive,
            int count,
            List<Long> excludeTxnIds,
            boolean remoteOnly)
    {
        // Start with some sane defaults
        if (fromTimeInclusive < 0L)
        {
            fromTimeInclusive = getMinTxnCommitTime();
        }
        if (toTimeExclusive < 0L || toTimeExclusive == Long.MAX_VALUE)
        {
            toTimeExclusive = ((long)getMaxTxnCommitTime())+1L;
        }
        // Get the time difference required
        long diffTime = toTimeExclusive - fromTimeInclusive;
        if (diffTime <= 0)
        {
            // There can be no results
            return Collections.emptyList();
        }
        
        // Make sure that we have at least one entry in the exclude list
        final List<Long> excludeTxnIdsInner = new ArrayList<Long>(excludeTxnIds == null ? 1 : excludeTxnIds.size());
        if (excludeTxnIds == null || excludeTxnIds.isEmpty())
        {
            excludeTxnIdsInner.add(TXN_ID_DUD);
        }
        else
        {
            excludeTxnIdsInner.addAll(excludeTxnIds);
        }
        final List<Long> excludeServerIds = new ArrayList<Long>(1);
        if (remoteOnly)
        {
            // Get the current server ID.  This can be null if no transactions have been written by
            // a server with this IP address.
            Long serverId = getServerIdOrNull();
            if (serverId == null)
            {
                excludeServerIds.add(SERVER_ID_DUD);
            }
            else
            {
                excludeServerIds.add(serverId);
            }
        }
        else
        {
            excludeServerIds.add(SERVER_ID_DUD);
        }
        
        

        List<Transaction> results = new ArrayList<Transaction>(count);
        // Each query must be constrained in the time range,
        // so query larger and larger sets until enough results are retrieved.
        long iteration = 0L;
        long queryFromTimeInclusive = toTimeExclusive;
        long queryToTimeExclusive = toTimeExclusive;
        int queryCount = count;
        while ((results.size() < count) && (queryFromTimeInclusive >= fromTimeInclusive))
        {
            iteration++;
            queryToTimeExclusive = queryFromTimeInclusive;
            queryFromTimeInclusive -= (iteration * MIN_TIME_QUERY_RANGE);
            queryCount = count - results.size();
        
            final long innerQueryFromTimeInclusive = queryFromTimeInclusive;
            final long innerQueryToTimeExclusive = queryToTimeExclusive;
            final int innerQueryCount = queryCount;
            HibernateCallback callback = new HibernateCallback()
            {
                public Object doInHibernate(Session session)
                {
                    Query query = session.getNamedQuery(QUERY_GET_TXNS_BY_COMMIT_TIME_DESC);
                    query.setLong("fromTimeInclusive", innerQueryFromTimeInclusive)
                         .setLong("toTimeExclusive", innerQueryToTimeExclusive)
                         .setParameterList("excludeTxnIds", excludeTxnIdsInner)
                         .setParameterList("excludeServerIds", excludeServerIds)
                         .setMaxResults(innerQueryCount)
                         .setReadOnly(true);
                    return query.list();
                }
            };
            List<Transaction> queryResults = (List<Transaction>) getHibernateTemplate().execute(callback);
            // Copy results over
            results.addAll(queryResults);
        }
        // done
        return results;
    }
    
    @SuppressWarnings("unchecked")
    public List<NodeRef> getTxnChangesForStore(final StoreRef storeRef, final long txnId)
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_TXN_CHANGES_FOR_STORE);
                query.setLong("txnId", txnId)
                     .setString("protocol", storeRef.getProtocol())
                     .setString("identifier", storeRef.getIdentifier())
                     .setReadOnly(true);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.list();
            }
        };
        List<Node> results = (List<Node>) getHibernateTemplate().execute(callback);
        // transform into a simpler form
        List<NodeRef> nodeRefs = new ArrayList<NodeRef>(results.size());
        for (Node node : results)
        {
            NodeRef nodeRef = node.getNodeRef();
            nodeRefs.add(nodeRef);
        }
        // done
        return nodeRefs;
    }
    
    @SuppressWarnings("unchecked")
    public List<NodeRef> getTxnChanges(final long txnId)
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_TXN_CHANGES);
                query.setLong("txnId", txnId)
                     .setReadOnly(true);
                DirtySessionMethodInterceptor.setQueryFlushMode(session, query);
                return query.list();
            }
        };
        List<Node> results = (List<Node>) getHibernateTemplate().execute(callback);
        // transform into a simpler form
        List<NodeRef> nodeRefs = new ArrayList<NodeRef>(results.size());
        for (Node node : results)
        {
            NodeRef nodeRef = node.getNodeRef();
            nodeRefs.add(nodeRef);
        }
        // done
        return nodeRefs;
    }
    
    //============ PROPERTY HELPER METHODS =================//
    
    public static Map<PropertyMapKey, NodePropertyValue> convertToPersistentProperties(
            Map<QName, Serializable> in,
            QNameDAO qnameDAO,
            LocaleDAO localeDAO,
            DictionaryService dictionaryService)
    {
        Map<PropertyMapKey, NodePropertyValue> propertyMap = new HashMap<PropertyMapKey, NodePropertyValue>(in.size() + 5);
        for (Map.Entry<QName, Serializable> entry : in.entrySet())
        {
            Serializable value = entry.getValue();
            // Get the qname ID
            QName propertyQName = entry.getKey();
            Long propertyQNameId = qnameDAO.getOrCreateQName(propertyQName).getFirst();
            // Get the locale ID
            Long propertylocaleId = localeDAO.getOrCreateDefaultLocalePair().getFirst();
            // Get the property definition, if available
            PropertyDefinition propertyDef = dictionaryService.getProperty(propertyQName);
            // Add it to the map
            HibernateNodeDaoServiceImpl.addValueToPersistedProperties(
                    propertyMap,
                    propertyDef,
                    HibernateNodeDaoServiceImpl.IDX_NO_COLLECTION,
                    propertyQNameId,
                    propertylocaleId,
                    value,
                    localeDAO);
        }
        // Done
        return propertyMap;
    }

    /**
     * The collection index used to indicate that the value is not part of a collection.
     * All values from zero up are used for real collection indexes.
     */
    private static final int IDX_NO_COLLECTION = -1;
    
    /**
     * A method that adds properties to the given map.  It copes with collections.
     *
     * @param propertyDef           the property definition (<tt>null</tt> is allowed)
     * @param collectionIndex       the index of the property in the collection or <tt>-1</tt> if
     *                              we are not yet processing a collection 
     */
    private static void addValueToPersistedProperties(
            Map<PropertyMapKey, NodePropertyValue> propertyMap,
            PropertyDefinition propertyDef,
            int collectionIndex,
            Long propertyQNameId,
            Long propertyLocaleId,
            Serializable value,
            LocaleDAO localeDAO)
    {
        if (value == null)
        {
            // The property is null.  Null is null and cannot be massaged any other way.
            NodePropertyValue npValue = HibernateNodeDaoServiceImpl.makeNodePropertyValue(propertyDef, null);
            PropertyMapKey npKey = new PropertyMapKey();
            npKey.setListIndex(collectionIndex);
            npKey.setQnameId(propertyQNameId);
            npKey.setLocaleId(propertyLocaleId);
            // Add it to the map
            propertyMap.put(npKey, npValue);
            // Done
            return;
        }
        
        // Get or spoof the property datatype
        QName propertyTypeQName;
        if (propertyDef == null)                // property not recognised
        {
            // allow it for now - persisting excess properties can be useful sometimes
            propertyTypeQName = DataTypeDefinition.ANY;
        }
        else
        {
            propertyTypeQName = propertyDef.getDataType().getName();
        }

        // A property may appear to be multi-valued if the model definition is loose and
        // an unexploded collection is passed in.  Otherwise, use the model-defined behaviour
        // strictly.
        boolean isMultiValued;
        if (propertyTypeQName.equals(DataTypeDefinition.ANY))
        {
            // It is multi-valued if required (we are not in a collection and the property is a new collection)
            isMultiValued = (value != null) && (value instanceof Collection) && (collectionIndex == IDX_NO_COLLECTION);
        }
        else
        {
            isMultiValued = propertyDef.isMultiValued();
        }
        
        // Handle different scenarios.
        // - Do we need to explode a collection?
        // - Does the property allow collections?
        if (collectionIndex == IDX_NO_COLLECTION && isMultiValued && !(value instanceof Collection))
        {
            // We are not (yet) processing a collection but the property should be part of a collection
            HibernateNodeDaoServiceImpl.addValueToPersistedProperties(
                    propertyMap,
                    propertyDef,
                    0,
                    propertyQNameId,
                    propertyLocaleId,
                    value,
                    localeDAO);
        }
        else if (collectionIndex == IDX_NO_COLLECTION && value instanceof Collection)
        {
            // We are not (yet) processing a collection and the property is a collection i.e. needs exploding
            // Check that multi-valued properties are supported if the property is a collection
            if (!isMultiValued)
            {
                throw new DictionaryException(
                        "A single-valued property of this type may not be a collection: \n" +
                        "   Property: " + propertyDef + "\n" +
                        "   Type: " + propertyTypeQName + "\n" +
                        "   Value: " + value);
            }
            // We have an allowable collection.
            @SuppressWarnings("unchecked")
            Collection<Object> collectionValues = (Collection<Object>) value;
            // Persist empty collections directly.  This is handled by the NodePropertyValue.
            if (collectionValues.size() == 0)
            {
                NodePropertyValue npValue = HibernateNodeDaoServiceImpl.makeNodePropertyValue(
                        null,
                        (Serializable) collectionValues);
                PropertyMapKey npKey = new PropertyMapKey();
                npKey.setListIndex(HibernateNodeDaoServiceImpl.IDX_NO_COLLECTION);
                npKey.setQnameId(propertyQNameId);
                npKey.setLocaleId(propertyLocaleId);
                // Add it to the map
                propertyMap.put(npKey, npValue);
            }
            // Break it up and recurse to persist the values.
            collectionIndex = -1;
            for (Object collectionValueObj : collectionValues)
            {
                collectionIndex++;
                if (collectionValueObj != null && !(collectionValueObj instanceof Serializable))
                {
                    throw new IllegalArgumentException(
                            "Node properties must be fully serializable, " +
                            "including values contained in collections. \n" +
                            "   Property: " + propertyDef + "\n" +
                            "   Index:    " + collectionIndex + "\n" +
                            "   Value:    " + collectionValueObj);
                }
                Serializable collectionValue = (Serializable) collectionValueObj;
                try
                {
                    HibernateNodeDaoServiceImpl.addValueToPersistedProperties(
                            propertyMap,
                            propertyDef,
                            collectionIndex,
                            propertyQNameId,
                            propertyLocaleId,
                            collectionValue,
                            localeDAO);
                }
                catch (Throwable e)
                {
                    throw new AlfrescoRuntimeException(
                            "Failed to persist collection entry: \n" +
                            "   Property: " + propertyDef + "\n" +
                            "   Index:    " + collectionIndex + "\n" +
                            "   Value:    " + collectionValue,
                            e);
                }
            }
        }
        else
        {
            // We are either processing collection elements OR the property is not a collection
            // Collections of collections are only supported by type d:any
            if (value instanceof Collection && !propertyTypeQName.equals(DataTypeDefinition.ANY))
            {
                throw new DictionaryException(
                        "Collections of collections (Serializable) are only supported by type 'd:any': \n" +
                        "   Property: " + propertyDef + "\n" +
                        "   Type: " + propertyTypeQName + "\n" +
                        "   Value: " + value);
            }
            // Handle MLText
            if (value instanceof MLText)
            {
                // This needs to be split up into individual strings
                MLText mlTextValue = (MLText) value;
                for (Map.Entry<Locale, String> mlTextEntry : mlTextValue.entrySet())
                {
                    Locale mlTextLocale = mlTextEntry.getKey();
                    String mlTextStr = mlTextEntry.getValue();
                    // Get the Locale ID for the text
                    Long mlTextLocaleId = localeDAO.getOrCreateLocalePair(mlTextLocale).getFirst();
                    // This is persisted against the current locale, but as a d:text instance
                    NodePropertyValue npValue = new NodePropertyValue(DataTypeDefinition.TEXT, mlTextStr);
                    PropertyMapKey npKey = new PropertyMapKey();
                    npKey.setListIndex(collectionIndex);
                    npKey.setQnameId(propertyQNameId);
                    npKey.setLocaleId(mlTextLocaleId);
                    // Add it to the map
                    propertyMap.put(npKey, npValue);
                }
            }
            else
            {
                NodePropertyValue npValue = HibernateNodeDaoServiceImpl.makeNodePropertyValue(propertyDef, value);
                PropertyMapKey npKey = new PropertyMapKey();
                npKey.setListIndex(collectionIndex);
                npKey.setQnameId(propertyQNameId);
                npKey.setLocaleId(propertyLocaleId);
                // Add it to the map
                propertyMap.put(npKey, npValue);
            }
        }
    }

    /**
     * Helper method to convert the <code>Serializable</code> value into a full, persistable {@link NodePropertyValue}.
     * <p>
     * Where the property definition is null, the value will take on the
     * {@link DataTypeDefinition#ANY generic ANY} value.
     * <p>
     * Collections are NOT supported.  These must be split up by the calling code before
     * calling this method.  Map instances are supported as plain serializable instances.
     * 
     * @param propertyDef       the property dictionary definition, may be null
     * @param value             the value, which will be converted according to the definition - may be null
     * @return                  Returns the persistable property value
     */
    private static NodePropertyValue makeNodePropertyValue(PropertyDefinition propertyDef, Serializable value)
    {
        // get property attributes
        final QName propertyTypeQName;
        if (propertyDef == null)                // property not recognised
        {
            // allow it for now - persisting excess properties can be useful sometimes
            propertyTypeQName = DataTypeDefinition.ANY;
        }
        else
        {
            propertyTypeQName = propertyDef.getDataType().getName();
        }
        try
        {
            NodePropertyValue propertyValue = new NodePropertyValue(propertyTypeQName, value);
            // done
            return propertyValue;
        }
        catch (TypeConversionException e)
        {
            throw new TypeConversionException(
                    "The property value is not compatible with the type defined for the property: \n" +
                    "   property: " + (propertyDef == null ? "unknown" : propertyDef) + "\n" +
                    "   value: " + value + "\n" +
                    "   value type: " + value.getClass(),
                    e);
        }
    }
    
    public static Serializable getPublicProperty(
            Map<PropertyMapKey, NodePropertyValue> propertyValues,
            QName propertyQName,
            QNameDAO qnameDAO,
            LocaleDAO localeDAO,
            DictionaryService dictionaryService)
    {
        // Get the qname ID
        Pair<Long, QName> qnamePair = qnameDAO.getQName(propertyQName);
        if (qnamePair == null)
        {
            // There is no persisted property with that QName, so we can't match anything
            return null;
        }
        Long qnameId = qnamePair.getFirst();
        // Now loop over the properties and extract those with the given qname ID
        SortedMap<PropertyMapKey, NodePropertyValue> scratch = new TreeMap<PropertyMapKey, NodePropertyValue>();
        for (Map.Entry<PropertyMapKey, NodePropertyValue> entry : propertyValues.entrySet())
        {
            PropertyMapKey propertyKey = entry.getKey();
            if (propertyKey.getQnameId().equals(qnameId))
            {
                scratch.put(propertyKey, entry.getValue());
            }
        }
        // If we found anything, then collapse the properties to a Serializable
        if (scratch.size() > 0)
        {
            PropertyDefinition propertyDef = dictionaryService.getProperty(propertyQName);
            Serializable collapsedValue = HibernateNodeDaoServiceImpl.collapsePropertiesWithSameQName(
                    propertyDef,
                    scratch,
                    localeDAO);
            return collapsedValue;
        }
        else
        {
            return null;
        }
    }

    public static Map<QName, Serializable> convertToPublicProperties(
            Map<PropertyMapKey, NodePropertyValue> propertyValues,
            QNameDAO qnameDAO,
            LocaleDAO localeDAO,
            DictionaryService dictionaryService)
    {
        Map<QName, Serializable> propertyMap = new HashMap<QName, Serializable>(propertyValues.size(), 1.0F);
        // Shortcut
        if (propertyValues.size() == 0)
        {
            return propertyMap;
        }
        // We need to process the properties in order
        SortedMap<PropertyMapKey, NodePropertyValue> sortedPropertyValues = new TreeMap<PropertyMapKey, NodePropertyValue>(propertyValues);
        // A working map.  Ordering is important.
        SortedMap<PropertyMapKey, NodePropertyValue> scratch = new TreeMap<PropertyMapKey, NodePropertyValue>();
        // Iterate (sorted) over the map entries and extract values with the same qname
        Long currentQNameId = Long.MIN_VALUE;
        Iterator<Map.Entry<PropertyMapKey, NodePropertyValue>> iterator = sortedPropertyValues.entrySet().iterator();
        while (true)
        {
            Long nextQNameId = null;
            PropertyMapKey nextPropertyKey = null;
            NodePropertyValue nextPropertyValue = null;
            // Record the next entry's values
            if (iterator.hasNext())
            {
                Map.Entry<PropertyMapKey, NodePropertyValue> entry = iterator.next();
                nextPropertyKey = entry.getKey();
                nextPropertyValue = entry.getValue();
                nextQNameId = nextPropertyKey.getQnameId();
            }
            // If the QName is going to change, and we have some entries to process, then process them.
            if (scratch.size() > 0 && (nextQNameId == null || !nextQNameId.equals(currentQNameId)))
            {
                QName currentQName = qnameDAO.getQName(currentQNameId).getSecond();
                PropertyDefinition currentPropertyDef = dictionaryService.getProperty(currentQName);
                // We have added something to the scratch properties but the qname has just changed
                Serializable collapsedValue = null;
                // We can shortcut if there is only one value
                if (scratch.size() == 1)
                {
                    // There is no need to collapse list indexes
                    collapsedValue = HibernateNodeDaoServiceImpl.collapsePropertiesWithSameQNameAndListIndex(
                            currentPropertyDef,
                            scratch,
                            localeDAO);
                }
                else
                {
                    // There is more than one value so the list indexes need to be collapsed
                    collapsedValue = HibernateNodeDaoServiceImpl.collapsePropertiesWithSameQName(
                            currentPropertyDef,
                            scratch,
                            localeDAO);
                }
                // If the property is multi-valued then the output property must be a collection
                if (currentPropertyDef != null && currentPropertyDef.isMultiValued())
                {
                    if (collapsedValue != null && !(collapsedValue instanceof Collection))
                    {
                        collapsedValue = (Serializable) Collections.singletonList(collapsedValue);
                    }
                }
                // Store the value
                propertyMap.put(currentQName, collapsedValue);
                // Reset
                scratch.clear();
            }
            if (nextQNameId != null)
            {
                // Add to the current entries
                scratch.put(nextPropertyKey, nextPropertyValue);
                currentQNameId = nextQNameId;
            }
            else
            {
                // There is no next value to process
                break;
            }
        }
        // Done
        return propertyMap;
    }
    
    private static Serializable collapsePropertiesWithSameQName(
            PropertyDefinition propertyDef,
            SortedMap<PropertyMapKey, NodePropertyValue> sortedPropertyValues,
            LocaleDAO localeDAO)
    {
        Serializable result = null;
        // A working map.  Ordering is not important for this map.
        Map<PropertyMapKey, NodePropertyValue> scratch = new HashMap<PropertyMapKey, NodePropertyValue>(3);
        // Iterate (sorted) over the map entries and extract values with the same list index
        Integer currentListIndex = Integer.MIN_VALUE;
        Iterator<Map.Entry<PropertyMapKey, NodePropertyValue>> iterator = sortedPropertyValues.entrySet().iterator();
        while (true)
        {
            Integer nextListIndex = null;
            PropertyMapKey nextPropertyKey = null;
            NodePropertyValue nextPropertyValue = null;
            // Record the next entry's values
            if (iterator.hasNext())
            {
                Map.Entry<PropertyMapKey, NodePropertyValue> entry = iterator.next();
                nextPropertyKey = entry.getKey();
                nextPropertyValue = entry.getValue();
                nextListIndex = nextPropertyKey.getListIndex();
            }
            // If the list index is going to change, and we have some entries to process, then process them.
            if (scratch.size() > 0 && (nextListIndex == null || !nextListIndex.equals(currentListIndex)))
            {
                // We have added something to the scratch properties but the index has just changed
                Serializable collapsedValue = HibernateNodeDaoServiceImpl.collapsePropertiesWithSameQNameAndListIndex(
                        propertyDef,
                        scratch,
                        localeDAO);
                // Store.  If there is a value already, then we must build a collection.
                if (result == null)
                {
                    result = collapsedValue;
                }
                else if (result instanceof Collection)
                {
                    @SuppressWarnings("unchecked")
                    Collection<Serializable> collectionResult = (Collection<Serializable>) result;
                    collectionResult.add(collapsedValue);
                }
                else
                {
                    Collection<Serializable> collectionResult = new ArrayList<Serializable>(20);
                    collectionResult.add(result);                   // Add the first result
                    collectionResult.add(collapsedValue);           // Add the new value
                    result = (Serializable) collectionResult;
                }
                // Reset
                scratch.clear();
            }
            if (nextListIndex != null)
            {
                // Add to the current entries
                scratch.put(nextPropertyKey, nextPropertyValue);
                currentListIndex = nextListIndex;
            }
            else
            {
                // There is no next value to process
                break;
            }
        }
        // Make sure that multi-valued properties are returned as a collection
        if (propertyDef != null && propertyDef.isMultiValued() && result != null && !(result instanceof Collection))
        {
            result = (Serializable) Collections.singletonList(result);
        }
        // Done
        return result;
    }
    
    /**
     * At this level, the properties have the same qname and list index.  They can only be separated
     * by locale.  Typically, MLText will fall into this category as only.
     * <p>
     * If there are multiple values then they can only be separated by locale.  If they are separated
     * by locale, then they have to be text-based.  This means that the only way to store them is via
     * MLText.  Any other multi-locale properties cannot be deserialized.
     */
    private static Serializable collapsePropertiesWithSameQNameAndListIndex(
            PropertyDefinition propertyDef,
            Map<PropertyMapKey, NodePropertyValue> propertyValues,
            LocaleDAO localeDAO)
    {
        int propertyValuesSize = propertyValues.size();
        Serializable value = null;
        if (propertyValuesSize == 0)
        {
            // Nothing to do
        }
        for (Map.Entry<PropertyMapKey, NodePropertyValue> entry : propertyValues.entrySet())
        {
            PropertyMapKey propertyKey = entry.getKey();
            NodePropertyValue propertyValue = entry.getValue();
            
            if (propertyValuesSize == 1 &&
                    (propertyDef == null || !propertyDef.getDataType().getName().equals(DataTypeDefinition.MLTEXT)))
            {
                // This is the only value and it is NOT to be converted to MLText
                value = HibernateNodeDaoServiceImpl.makeSerializableValue(propertyDef, propertyValue);
            }
            else
            {
                // There are multiple values, so add them to MLText
                MLText mltext = (value == null) ? new MLText() : (MLText) value;
                try
                {
                    String mlString = (String) propertyValue.getValue(DataTypeDefinition.TEXT);
                    // Get the locale
                    Long localeId = propertyKey.getLocaleId();
                    Locale locale = localeDAO.getLocalePair(localeId).getSecond();
                    // Add to the MLText object
                    mltext.addValue(locale, mlString);
                }
                catch (TypeConversionException e)
                {
                    // Ignore
                    logger.warn("Unable to add property value to MLText instance: " + propertyValue);
                }
                value = mltext;
            }
        }
        // Done
        return value;
    }
    
    /**
     * Extracts the externally-visible property from the persistable value.
     * 
     * @param propertyDef       the model property definition - may be <tt>null</tt>
     * @param propertyValue     the persisted property
     * @return                  Returns the value of the property in the format dictated by the property
     *                          definition, or null if the property value is null 
     */
    private static Serializable makeSerializableValue(PropertyDefinition propertyDef, NodePropertyValue propertyValue)
    {
        if (propertyValue == null)
        {
            return null;
        }
        // get property attributes
        final QName propertyTypeQName;
        if (propertyDef == null)
        {
            // allow this for now
            propertyTypeQName = DataTypeDefinition.ANY;
        }
        else
        {
            propertyTypeQName = propertyDef.getDataType().getName();
        }
        try
        {
            Serializable value = propertyValue.getValue(propertyTypeQName);
            // done
            return value;
        }
        catch (TypeConversionException e)
        {
            throw new TypeConversionException(
                    "The property value is not compatible with the type defined for the property: \n" +
                    "   property: " + (propertyDef == null ? "unknown" : propertyDef) + "\n" +
                    "   property value: " + propertyValue,
                    e);
        }
    }
}