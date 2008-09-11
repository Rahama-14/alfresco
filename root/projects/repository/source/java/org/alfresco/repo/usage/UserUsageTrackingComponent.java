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
package org.alfresco.repo.usage;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.db.NodeDaoService;
import org.alfresco.repo.node.db.NodeDaoService.NodePropertyHandler;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.tenant.Tenant;
import org.alfresco.repo.tenant.TenantAdminService;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.TransactionServiceImpl;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.usage.UsageService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * User Usage Tracking Component - to allow user usages to be collapsed or re-calculated
 * 
 * - used by UserUsageCollapseJob to collapse usage deltas.
 * - used by UserUsageBootstrapJob to either clear all usages or (re-)calculate all missing usages.
 */
public class UserUsageTrackingComponent
{
    private static Log logger = LogFactory.getLog(UserUsageTrackingComponent.class);
    
    private static boolean busy = false;
    
    private TransactionServiceImpl transactionService;
    private ContentUsageImpl contentUsageImpl;
    
    private PersonService personService;
    private NodeService nodeService;
    private NodeDaoService nodeDaoService;
    private UsageService usageService;
    private TenantAdminService tenantAdminService;
    
    private boolean enabled = true;
    
    public void setTransactionService(TransactionServiceImpl transactionService)
    {
        this.transactionService = transactionService;
    }
    
    public void setContentUsageImpl(ContentUsageImpl contentUsageImpl)
    {
        this.contentUsageImpl = contentUsageImpl;
    }
    
    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    public void setNodeDaoService(NodeDaoService nodeDaoService)
    {
        this.nodeDaoService = nodeDaoService;
    }

    public void setUsageService(UsageService usageService)
    {
        this.usageService = usageService;
    }
    
    public void setTenantAdminService(TenantAdminService tenantAdminService)
    {
        this.tenantAdminService = tenantAdminService;
    }
    
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
      
    public void execute()
    {
        if (enabled == true)
        {
        	if (! busy)
        	{
        		try
        		{
            		busy = true;      
            		
            		// collapse usages - note: for MT environment, will collapse for all tenants
            		collapseUsages();
	            }
	            finally
	            {
	                busy = false;
	            }
        	}
        }
	}
    
    // called once on startup
    public void bootstrap()
    {
    	// default domain
    	bootstrapInternal(); 
		
		if (tenantAdminService.isEnabled())
		{
			List<Tenant> tenants = tenantAdminService.getAllTenants();	                            	
            for (Tenant tenant : tenants)
            {          
            	AuthenticationUtil.runAs(new RunAsWork<Object>()
                {
            		public Object doWork() throws Exception
                    {
            			bootstrapInternal();
            			return null;
                    }
                }, tenantAdminService.getDomainUser(AuthenticationUtil.getSystemUserName(), tenant.getTenantDomain()));
            }
		}
    }
    
    public void bootstrapInternal()
    {
    	if (! busy)
    	{
    		try
    		{
    			busy = true;
    		
            	if (enabled)
            	{
            		// enabled - calculate missing usages
            		calculateMissingUsages();
            	}
            	else
            	{
            		// disabled - remove all usages
                	clearAllUsages();
            	}
            }
            finally
            {
                busy = false;
            }
    	}
    }
    
    public void clearAllUsages()
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("Disabled - clear usages for all users ...");
        }

        RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
        
        // wrap to make the request in a transaction
        RetryingTransactionCallback<Integer> clearAllUsages = new RetryingTransactionCallback<Integer>()
        {
            public Integer execute() throws Throwable
            {
                Set<NodeRef> allPeople = personService.getAllPeople();
                
                for (NodeRef personNodeRef : allPeople)
                {
                    nodeService.setProperty(personNodeRef, ContentModel.PROP_SIZE_CURRENT, null);
                    usageService.deleteDeltas(personNodeRef);
                }
                return allPeople.size();
            }
        };
        // execute in txn
        int count = txnHelper.doInTransaction(clearAllUsages, false);

        if (logger.isDebugEnabled()) 
        {
            logger.debug("... cleared usages for " + count + " users");
        }   	
    }
    
    public void calculateMissingUsages()
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("Enabled - calculate missing usages ...");
        }

        RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
        
        // wrap to make the request in a transaction
        RetryingTransactionCallback<Set<String>> getAllPeople = new RetryingTransactionCallback<Set<String>>()
        {
            public Set<String> execute() throws Throwable
            {
                Set<NodeRef> allPeople = personService.getAllPeople();
                Set<String> userNames = new HashSet<String>();
                
                for (NodeRef personNodeRef : allPeople)
                {
                    // Cater for Lucene indexes being stale
                    if (!nodeService.exists(personNodeRef))
                    {
                        continue;
                    }
                    Long currentUsage = (Long)nodeService.getProperty(personNodeRef, ContentModel.PROP_SIZE_CURRENT);
                    if (currentUsage == null)
                    {
                        String userName = (String)nodeService.getProperty(personNodeRef, ContentModel.PROP_USERNAME);
                        userNames.add(userName);
                    }
                }
                return userNames;
            }
        };
        // execute in READ-ONLY txn
        final Set<String> userNames = txnHelper.doInTransaction(getAllPeople, true);
        
        for (final String userName : userNames)
        {
        	AuthenticationUtil.runAs(new RunAsWork<Object>()
            {
        		public Object doWork() throws Exception
                {
        			recalculateUsage(userName);
        			return null;
                }
            }, tenantAdminService.getDomainUser(AuthenticationUtil.getSystemUserName(), tenantAdminService.getUserDomain(userName)));
        }

        if (logger.isDebugEnabled()) 
        {
            logger.debug("... calculated missing usages for " + userNames.size() + " users");
        }
    }

    /**
     * Recalculate content usage for given user. Required if upgrading an existing Alfresco, for users that
     * have not had their initial usage calculated. In a future release, could also be called explicitly by
     * a SysAdmin, eg. via a JMX operation.
     * 
     * @param username          the username to for which calcualte usages
     */
    public void recalculateUsage(final String username)
    { 
        final RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
        
        // wrap to make the request in a transaction
        RetryingTransactionCallback<Long> calculatePersonCurrentUsage = new RetryingTransactionCallback<Long>()
        {
            public Long execute() throws Throwable
            {
//                QNameEntity ownerQnameEntity = qnameDAO.getQNameEntity(ContentModel.PROP_OWNER);
//                QNameEntity contentQnameEntity = qnameDAO.getQNameEntity(ContentModel.PROP_CONTENT);
//                
                List<String> stores = contentUsageImpl.getStores();
                final MutableLong totalUsage = new MutableLong(0L);
                
                for (String store : stores)
                {
                    final StoreRef storeRef = new StoreRef(store);
                    
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Recalc usage (" + username + ") store=" + storeRef);
                    }

                    NodePropertyHandler propOwnerHandler = new NodePropertyHandler()
                    {
                        public void handle(NodeRef nodeRef, QName nodeTypeQName, QName propertyQName, Serializable value)
                        {
                            if (nodeTypeQName.equals(ContentModel.TYPE_CONTENT))
                            {
                                // It is not content
                            }
                            ContentData contentData = DefaultTypeConverter.INSTANCE.convert(
                                    ContentData.class,
                                    nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT));
                            if (contentData != null)
                            {
                                long currentTotalUsage = totalUsage.longValue();
                                totalUsage.setValue(currentTotalUsage + contentData.getSize());
                            }
                        }
                    };
                    nodeDaoService.getPropertyValuesByPropertyAndValue(storeRef, ContentModel.PROP_OWNER, username, propOwnerHandler);
                    
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Recalc usage (" + username + ") store=" + storeRef);
                    }

                    NodePropertyHandler propCreatorHandler = new NodePropertyHandler()
                    {
                        public void handle(NodeRef nodeRef, QName nodeTypeQName, QName propertyQName, Serializable value)
                        {
                            if (!nodeTypeQName.equals(ContentModel.TYPE_CONTENT))
                            {
                                // It is not content
                                return;
                            }
                            if (nodeService.getProperty(nodeRef, ContentModel.PROP_OWNER) != null)
                            {
                                // There is an owner property so we will have process this already
                                return;
                            }
                            ContentData contentData = DefaultTypeConverter.INSTANCE.convert(
                                    ContentData.class,
                                    nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT));
                            if (contentData != null)
                            {
                                long currentTotalUsage = totalUsage.longValue();
                                totalUsage.setValue(currentTotalUsage + contentData.getSize());
                            }
                        }
                    };
                    nodeDaoService.getPropertyValuesByPropertyAndValue(storeRef, ContentModel.PROP_OWNER, username, propCreatorHandler);
                }
                
                if (logger.isDebugEnabled()) 
                {
                    long quotaSize = contentUsageImpl.getUserQuota(username);
                    logger.debug("Recalc usage ("+ username+") totalUsage="+totalUsage+", quota="+quotaSize);
                }
                		
                return totalUsage.longValue();
            }
        };
        // execute in READ-ONLY txn
        final Long currentUsage = txnHelper.doInTransaction(calculatePersonCurrentUsage, true);
        
        // wrap to make the request in a transaction
        RetryingTransactionCallback<Object> updatePersonCurrentUsage = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Throwable
            {
                NodeRef personNodeRef = personService.getPerson(username);
                if (!nodeService.exists(personNodeRef))
                {
                    // Ignore
                    return null;
                }
                contentUsageImpl.setUserStoredUsage(personNodeRef, currentUsage);
                usageService.deleteDeltas(personNodeRef);
                return null;
            }
        };
        
        // execute in txn
        txnHelper.doInTransaction(updatePersonCurrentUsage, false);
    }
    
    /**
     * Collapse usages - note: for MT environment, will collapse all tenants
     */
    private void collapseUsages()
    {
        // Collapse usage deltas (if a person has initial usage set)
        final RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
        
        // wrap to make the request in a transaction
        RetryingTransactionCallback<Object> collapseUsages = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Throwable
            {
                // Get distinct candidates
                Set<NodeRef> usageNodeRefs = usageService.getUsageDeltaNodes();
                
                for(final NodeRef usageNodeRef : usageNodeRefs)
                {                            
                	AuthenticationUtil.runAs(new RunAsWork<Object>()
                    {
                		public Object doWork() throws Exception
                        {
                            QName nodeType = nodeService.getType(usageNodeRef);
                            
                            if (nodeType.equals(ContentModel.TYPE_PERSON))
                            {
                                NodeRef personNodeRef = usageNodeRef;
                                String userName = (String)nodeService.getProperty(personNodeRef, ContentModel.PROP_USERNAME);
                                
                                long currentUsage = contentUsageImpl.getUserStoredUsage(personNodeRef);
                                if (currentUsage != -1)
                                {
                                    // collapse the usage deltas
                                    currentUsage = contentUsageImpl.getUserUsage(userName);                                 
                                    usageService.deleteDeltas(personNodeRef);
                                    contentUsageImpl.setUserStoredUsage(personNodeRef, currentUsage);
                                    
                                    if (logger.isDebugEnabled()) 
                                    {
                                        logger.debug("Collapsed usage: username=" + userName + ", usage=" + currentUsage);
                                    }
                                }
                                else
                                {
                                    if (logger.isWarnEnabled())
                                    {
                                        logger.warn("Initial usage for user has not yet been calculated: " + userName);
                                    }
                                }
                            }
                			return null;
                        }
                    }, tenantAdminService.getDomainUser(AuthenticationUtil.getSystemUserName(), tenantAdminService.getDomain(usageNodeRef.getStoreRef().getIdentifier())));
                }  
                return null;
            }
        };
        
    	// execute in txn
    	txnHelper.doInTransaction(collapseUsages, false);
    }
}
