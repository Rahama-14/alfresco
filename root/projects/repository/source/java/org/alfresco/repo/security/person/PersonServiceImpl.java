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
package org.alfresco.repo.security.person;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.permissions.PermissionServiceSPI;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PersonServiceImpl extends TransactionListenerAdapter implements PersonService, NodeServicePolicies.OnCreateNodePolicy, NodeServicePolicies.BeforeDeleteNodePolicy
{
    private static Log s_logger = LogFactory.getLog(PersonServiceImpl.class);

    private static final String DELETE = "DELETE";

    private static final String SPLIT = "SPLIT";

    private static final String LEAVE = "LEAVE";

    public static final String SYSTEM_FOLDER_SHORT_QNAME = "sys:system";

    public static final String PEOPLE_FOLDER_SHORT_QNAME = "sys:people";

    // IOC

    private StoreRef storeRef;

    private TransactionService transactionService;

    private NodeService nodeService;
    
    private TenantService tenantService;

    private SearchService searchService;

    private AuthorityService authorityService;
    
    private DictionaryService dictionaryService;

    private PermissionServiceSPI permissionServiceSPI;

    private NamespacePrefixResolver namespacePrefixResolver;

    private PolicyComponent policyComponent;

    private boolean createMissingPeople;

    private static Set<QName> mutableProperties;

    private boolean userNamesAreCaseSensitive = false;

    private String defaultHomeFolderProvider;

    private boolean processDuplicates = true;

    private String duplicateMode = LEAVE;

    private boolean lastIsBest = true;

    private boolean includeAutoCreated = false;

    private PersonDao personDao;

    /** a transactionally-safe cache to be injected */
    private SimpleCache<String, NodeRef> personCache;

    static
    {
        Set<QName> props = new HashSet<QName>();
        props.add(ContentModel.PROP_HOMEFOLDER);
        props.add(ContentModel.PROP_FIRSTNAME);
        // Middle Name
        props.add(ContentModel.PROP_LASTNAME);
        props.add(ContentModel.PROP_EMAIL);
        props.add(ContentModel.PROP_ORGID);
        mutableProperties = Collections.unmodifiableSet(props);
    }

    @Override
    public boolean equals(Object obj)
    {
        return this == obj;
    }

    @Override
    public int hashCode()
    {
        return 1;
    }

    /**
     * Spring bean init method
     */
    public void init()
    {
        PropertyCheck.mandatory(this, "storeUrl", storeRef);
        PropertyCheck.mandatory(this, "transactionService", transactionService);
        PropertyCheck.mandatory(this, "nodeService", nodeService);
        PropertyCheck.mandatory(this, "searchService", searchService);
        PropertyCheck.mandatory(this, "permissionServiceSPI", permissionServiceSPI);
        PropertyCheck.mandatory(this, "authorityService", authorityService);
        PropertyCheck.mandatory(this, "namespacePrefixResolver", namespacePrefixResolver);
        PropertyCheck.mandatory(this, "policyComponent", policyComponent);
        PropertyCheck.mandatory(this, "personCache", personCache);
        PropertyCheck.mandatory(this, "personDao", personDao);

        this.policyComponent
                .bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"), ContentModel.TYPE_PERSON, new JavaBehaviour(this, "onCreateNode"));
        this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "beforeDeleteNode"), ContentModel.TYPE_PERSON, new JavaBehaviour(this,
                "beforeDeleteNode"));
    }

    public boolean getUserNamesAreCaseSensitive()
    {
        return userNamesAreCaseSensitive;
    }

    public void setUserNamesAreCaseSensitive(boolean userNamesAreCaseSensitive)
    {
        this.userNamesAreCaseSensitive = userNamesAreCaseSensitive;
    }

    void setDefaultHomeFolderProvider(String defaultHomeFolderProvider)
    {
        this.defaultHomeFolderProvider = defaultHomeFolderProvider;
    }

    public void setDuplicateMode(String duplicateMode)
    {
        this.duplicateMode = duplicateMode;
    }

    public void setIncludeAutoCreated(boolean includeAutoCreated)
    {
        this.includeAutoCreated = includeAutoCreated;
    }

    public void setLastIsBest(boolean lastIsBest)
    {
        this.lastIsBest = lastIsBest;
    }

    public void setProcessDuplicates(boolean processDuplicates)
    {
        this.processDuplicates = processDuplicates;
    }
    
    public void setPersonDao(PersonDao personDao)
    {
        this.personDao = personDao;
    }

    /**
     * Set the username to person cache.
     * 
     * @param personCache
     *            a transactionally safe cache
     */
    public void setPersonCache(SimpleCache<String, NodeRef> personCache)
    {
        this.personCache = personCache;
    }

    /**
     * Retrieve the person NodeRef for a username key. Depending on configuration missing people will be created if not
     * found, else a NoSuchPersonException exception will be thrown.
     * 
     * @param userName
     *            of the person NodeRef to retrieve
     * @return NodeRef of the person as specified by the username
     * @throws NoSuchPersonException
     */
    public NodeRef getPerson(String userName)
    {
        NodeRef personNode = getPersonOrNull(userName);
        if (personNode == null)
        {
            TxnReadState txnReadState = AlfrescoTransactionSupport.getTransactionReadState();
            if (createMissingPeople() && txnReadState == TxnReadState.TXN_READ_WRITE)
            {
                // We create missing people AND are in a read-write txn
                return createMissingPerson(userName);
            }
            else
            {
                throw new NoSuchPersonException(userName);
            }
        }
        else
        {
            return personNode;
        }
    }

    public boolean personExists(String caseSensitiveUserName)
    {
        return getPersonOrNull(caseSensitiveUserName) != null;
    }

    private NodeRef getPersonOrNull(String searchUserName)
    {
        NodeRef returnRef = this.personCache.get(searchUserName);
        if (returnRef == null)
        {
            List<NodeRef> refs = personDao.getPersonOrNull(searchUserName, userNamesAreCaseSensitive);
            if (refs.size() > 1)
            {
                returnRef = handleDuplicates(refs, searchUserName);
            }
            else if (refs.size() == 1)
            {
                returnRef = refs.get(0);
            }

            // add to cache
            this.personCache.put(searchUserName, returnRef);
        }
        return returnRef;
    }

    private NodeRef handleDuplicates(List<NodeRef> refs, String searchUserName)
    {
        if (processDuplicates)
        {
            NodeRef best = findBest(refs);
            HashSet<NodeRef> toHandle = new HashSet<NodeRef>();
            toHandle.addAll(refs);
            toHandle.remove(best);
            addDuplicateNodeRefsToHandle(toHandle);
            return best;
        }
        else
        {
            if (userNamesAreCaseSensitive)
            {
                throw new AlfrescoRuntimeException("Found more than one user for " + searchUserName + " (case sensitive)");
            }
            else
            {
                throw new AlfrescoRuntimeException("Found more than one user for " + searchUserName + " (case insensitive)");
            }
        }
    }

    private static final String KEY_POST_TXN_DUPLICATES = "PersonServiceImpl.KEY_POST_TXN_DUPLICATES";

    /**
     * Get the txn-bound usernames that need cleaning up
     */
    private Set<NodeRef> getPostTxnDuplicates()
    {
        @SuppressWarnings("unchecked")
        Set<NodeRef> postTxnDuplicates = (Set<NodeRef>) AlfrescoTransactionSupport.getResource(KEY_POST_TXN_DUPLICATES);
        if (postTxnDuplicates == null)
        {
            postTxnDuplicates = new HashSet<NodeRef>();
            AlfrescoTransactionSupport.bindResource(KEY_POST_TXN_DUPLICATES, postTxnDuplicates);
        }
        return postTxnDuplicates;
    }

    /**
     * Flag a username for cleanup after the transaction.
     */
    private void addDuplicateNodeRefsToHandle(Set<NodeRef> refs)
    {
        // Firstly, bind this service to the transaction
        AlfrescoTransactionSupport.bindListener(this);
        // Now get the post txn duplicate list
        Set<NodeRef> postTxnDuplicates = getPostTxnDuplicates();
        postTxnDuplicates.addAll(refs);
    }

    /**
     * Process clean up any duplicates that were flagged during the transaction.
     */
    @Override
    public void afterCommit()
    {
        // Get the duplicates in a form that can be read by the transaction work anonymous instance
        final Set<NodeRef> postTxnDuplicates = getPostTxnDuplicates();

        RetryingTransactionCallback<Object> processDuplicateWork = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Throwable
            {

                if (duplicateMode.equalsIgnoreCase(SPLIT))
                {
                    split(postTxnDuplicates);
                    s_logger.info("Split duplicate person objects");
                }
                else if (duplicateMode.equalsIgnoreCase(DELETE))
                {
                    delete(postTxnDuplicates);
                    s_logger.info("Deleted duplicate person objects");
                }
                else
                {
                    if (s_logger.isDebugEnabled())
                    {
                        s_logger.debug("Duplicate person objects exist");
                    }
                }

                // Done
                return null;
            }
        };
        transactionService.getRetryingTransactionHelper().doInTransaction(processDuplicateWork, false, true);
    }

    private void delete(Set<NodeRef> toDelete)
    {
        for (NodeRef nodeRef : toDelete)
        {
            nodeService.deleteNode(nodeRef);
        }
    }

    private void split(Set<NodeRef> toSplit)
    {
        for (NodeRef nodeRef : toSplit)
        {
            String userName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME));
            nodeService.setProperty(nodeRef, ContentModel.PROP_USERNAME, userName + GUID.generate());
        }
    }

    private NodeRef findBest(List<NodeRef> refs)
    {
        if (lastIsBest)
        {
            Collections.sort(refs, new CreationDateComparator(nodeService, false));
        }
        else
        {
            Collections.sort(refs, new CreationDateComparator(nodeService, true));
        }

        NodeRef fallBack = null;

        for (NodeRef nodeRef : refs)
        {
            if (fallBack == null)
            {
                fallBack = nodeRef;
            }

            if (includeAutoCreated || !wasAutoCreated(nodeRef))
            {
                return nodeRef;
            }
        }

        return fallBack;
    }

    private boolean wasAutoCreated(NodeRef nodeRef)
    {
        String userName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME));

        String testString = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_FIRSTNAME));
        if ((testString == null) || !testString.equals(userName))
        {
            return false;
        }

        testString = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_LASTNAME));
        if ((testString == null) || !testString.equals(""))
        {
            return false;
        }

        testString = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_EMAIL));
        if ((testString == null) || !testString.equals(""))
        {
            return false;
        }

        testString = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_ORGID));
        if ((testString == null) || !testString.equals(""))
        {
            return false;
        }

        testString = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_HOME_FOLDER_PROVIDER));
        if ((testString == null) || !testString.equals(defaultHomeFolderProvider))
        {
            return false;
        }

        return true;
    }

    public boolean createMissingPeople()
    {
        return createMissingPeople;
    }

    public Set<QName> getMutableProperties()
    {
        return mutableProperties;
    }

    public void setPersonProperties(String userName, Map<QName, Serializable> properties)
    {
        NodeRef personNode = getPersonOrNull(userName);
        if (personNode == null)
        {
            if (createMissingPeople())
            {
                personNode = createMissingPerson(userName);
            }
            else
            {
                throw new PersonException("No person found for user name " + userName);
            }

        }
        else
        {
            String realUserName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(personNode, ContentModel.PROP_USERNAME));
            properties.put(ContentModel.PROP_USERNAME, realUserName);
        }

        nodeService.setProperties(personNode, properties);
    }

    public boolean isMutable()
    {
        return true;
    }

    private NodeRef createMissingPerson(String userName)
    {
        HashMap<QName, Serializable> properties = getDefaultProperties(userName);
        return createPerson(properties);
    }

    private HashMap<QName, Serializable> getDefaultProperties(String userName)
    {
        HashMap<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ContentModel.PROP_USERNAME, userName);
        properties.put(ContentModel.PROP_FIRSTNAME, tenantService.getBaseNameUser(userName));
        properties.put(ContentModel.PROP_LASTNAME, "");
        properties.put(ContentModel.PROP_EMAIL, "");
        properties.put(ContentModel.PROP_ORGID, "");
        properties.put(ContentModel.PROP_HOME_FOLDER_PROVIDER, defaultHomeFolderProvider);
        
        properties.put(ContentModel.PROP_SIZE_CURRENT, 0L);
        properties.put(ContentModel.PROP_SIZE_QUOTA, -1L); // no quota
        
        return properties;
    }

    public NodeRef createPerson(Map<QName, Serializable> properties)
    {
        String userName = DefaultTypeConverter.INSTANCE.convert(String.class, properties.get(ContentModel.PROP_USERNAME));
        
        tenantService.checkDomainUser(userName);
        
        properties.put(ContentModel.PROP_USERNAME, userName);
        properties.put(ContentModel.PROP_SIZE_CURRENT, 0L);
        
        return nodeService.createNode(
                getPeopleContainer(),
                ContentModel.ASSOC_CHILDREN,
                QName.createQName("cm", userName, namespacePrefixResolver),
                ContentModel.TYPE_PERSON, properties).getChildRef();
    }

    public NodeRef getPeopleContainer()
    {
        NodeRef rootNodeRef = nodeService.getRootNode(tenantService.getName(storeRef));
        List<ChildAssociationRef> children = nodeService.getChildAssocs(rootNodeRef, RegexQNamePattern.MATCH_ALL, QName.createQName(SYSTEM_FOLDER_SHORT_QNAME, namespacePrefixResolver));
       
        if (children.size() == 0)
        {
            throw new AlfrescoRuntimeException("Required people system path not found: " + SYSTEM_FOLDER_SHORT_QNAME);
        }
       
        NodeRef systemNodeRef = children.get(0).getChildRef();
        
        children = nodeService.getChildAssocs(systemNodeRef, RegexQNamePattern.MATCH_ALL, QName.createQName(PEOPLE_FOLDER_SHORT_QNAME, namespacePrefixResolver));
        
          if (children.size() == 0)
        {
            throw new AlfrescoRuntimeException("Required people system path not found: " + PEOPLE_FOLDER_SHORT_QNAME);
        }
        
        NodeRef peopleNodeRef = children.get(0).getChildRef();
        return peopleNodeRef;
    }

    public void deletePerson(String userName)
    {
        NodeRef personNodeRef = getPersonOrNull(userName);

        // delete the person
        if (personNodeRef != null)
        {
            nodeService.deleteNode(personNodeRef);
        }

        // remove user from any containing authorities
        Set<String> containerAuthorities = authorityService.getContainingAuthorities(null, userName, true);
        for (String containerAuthority : containerAuthorities)
        {
            authorityService.removeAuthority(containerAuthority, userName);
        }

        // remove any user permissions
        permissionServiceSPI.deletePermissions(userName);
    }

    public Set<NodeRef> getAllPeople()
    {
        return personDao.getAllPeople();
    }

    public Set<NodeRef> getPeopleFilteredByProperty(QName propertyKey, Serializable propertyValue)
    {
        // check that given property key is defined for content model type 'cm:person'
        // and throw exception if it isn't
        if (this.dictionaryService.getProperty(ContentModel.TYPE_PERSON, propertyKey) == null)
        {
            throw new AlfrescoRuntimeException("Property '" + propertyKey + "' is not defined "
                    + "for content model type cm:person");
        }
        
        LinkedHashSet<NodeRef> people = new LinkedHashSet<NodeRef>();
        
        //
        // Search for people using the given property
        //
        
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery("@cm\\:" + propertyKey.getLocalName() + ":\"" + propertyValue + "\"");
        sp.addStore(tenantService.getName(storeRef));
        sp.excludeDataInTheCurrentTransaction(false);

        ResultSet rs = null;

        try
        {
            rs = searchService.query(sp);

            for (ResultSetRow row : rs)
            {
                NodeRef nodeRef = row.getNodeRef();
                if (nodeService.exists(nodeRef))
                {
                    people.add(nodeRef);
                }
            }
        }
        finally
        {
            if (rs != null)
            {
                rs.close();
            }
        }
        
        return people;
    }
    
    // Policies

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy#onCreateNode(org.alfresco.service.cmr.repository.ChildAssociationRef)
     */
    public void onCreateNode(ChildAssociationRef childAssocRef)
    {
        NodeRef personRef = childAssocRef.getChildRef();
        String username = (String) this.nodeService.getProperty(personRef, ContentModel.PROP_USERNAME);
        this.personCache.put(username, personRef);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy#beforeDeleteNode(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void beforeDeleteNode(NodeRef nodeRef)
    {
        String username = (String) this.nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME);
        this.personCache.remove(username);
    }

    // IOC Setters

    public void setCreateMissingPeople(boolean createMissingPeople)
    {
        this.createMissingPeople = createMissingPeople;
    }

    public void setNamespacePrefixResolver(NamespacePrefixResolver namespacePrefixResolver)
    {
        this.namespacePrefixResolver = namespacePrefixResolver;
    }

    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }
    
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    public void setPermissionServiceSPI(PermissionServiceSPI permissionServiceSPI)
    {
        this.permissionServiceSPI = permissionServiceSPI;
    }

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }    

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }
    
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    public void setStoreUrl(String storeUrl)
    {
        this.storeRef = new StoreRef(storeUrl);
    }

    public String getUserIdentifier(String caseSensitiveUserName)
    {
        NodeRef nodeRef = getPersonOrNull(caseSensitiveUserName);
        if ((nodeRef != null) && nodeService.exists(nodeRef))
        {
            String realUserName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME));
            return realUserName;
        }
        return null;
    }

    public static class CreationDateComparator implements Comparator<NodeRef>
    {
        private NodeService nodeService;

        boolean ascending;

        CreationDateComparator(NodeService nodeService, boolean ascending)
        {
            this.nodeService = nodeService;
            this.ascending = ascending;
        }

        public int compare(NodeRef first, NodeRef second)
        {
            Date firstDate = DefaultTypeConverter.INSTANCE.convert(Date.class, nodeService.getProperty(first, ContentModel.PROP_CREATED));
            Date secondDate = DefaultTypeConverter.INSTANCE.convert(Date.class, nodeService.getProperty(second, ContentModel.PROP_CREATED));

            if (firstDate != null)
            {
                if (secondDate != null)
                {
                    return firstDate.compareTo(secondDate) * (ascending ? 1 : -1);
                }
                else
                {
                    return ascending ? -1 : 1;
                }
            }
            else
            {
                if (secondDate != null)
                {
                    return ascending ? 1 : -1;
                }
                else
                {
                    return 0;
                }
            }

        }
    }
}
