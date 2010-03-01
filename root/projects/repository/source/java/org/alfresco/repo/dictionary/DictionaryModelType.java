/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.dictionary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.tenant.Tenant;
import org.alfresco.repo.tenant.TenantAdminService;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.repo.workflow.BPMEngineRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryException;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.NamespaceDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTaskDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Dictionary model type behaviour.
 * 
 * @author Roy Wetherall, janv
 */
public class DictionaryModelType implements ContentServicePolicies.OnContentUpdatePolicy,
                                            NodeServicePolicies.OnUpdatePropertiesPolicy,
                                            NodeServicePolicies.BeforeDeleteNodePolicy,
                                            NodeServicePolicies.OnDeleteNodePolicy,
                                            NodeServicePolicies.OnCreateNodePolicy,
                                            NodeServicePolicies.OnRemoveAspectPolicy
{
    // Logger
    private static Log logger = LogFactory.getLog(DictionaryModelType.class);
    
    /** Key to the pending models */
    private static final String KEY_PENDING_MODELS = "dictionaryModelType.pendingModels";
    
    /** Key to the pending deleted models */
    private static final String KEY_PENDING_DELETE_MODELS = "dictionaryModelType.pendingDeleteModels";
    
    /** Key to the removed "workingcopy" aspect */
    private static final String KEY_WORKING_COPY = "dictionaryModelType.workingCopy";
    
    /** Key to the removed "archived" aspect */
    private static final String KEY_ARCHIVED = "dictionaryModelType.archived";
    
    /** The dictionary DAO */
    private DictionaryDAO dictionaryDAO;
    
    /** The namespace DAO */
    private NamespaceDAO namespaceDAO;
    
    /** The node service */
    private NodeService nodeService;
    
    /** The content service */
    private ContentService contentService;
    
    /** The policy component */
    private PolicyComponent policyComponent;
    
    /** The workflow service */
    private WorkflowService workflowService;
    
    /** The search service */
    private SearchService searchService;
    
    /** The namespace service */
    private NamespaceService namespaceService;
    
    /** The tenant service */
    private TenantService tenantService;
    
    /** The tenant deployer service */
    private TenantAdminService tenantAdminService;
    
    /** Transaction listener */
    private DictionaryModelTypeTransactionListener transactionListener;
        
    private List<String> storeUrls; // stores against which model deletes should be validated
    
    
    /**
     * Set the dictionary DAO
     */
    public void setDictionaryDAO(DictionaryDAO dictionaryDAO)
    {
        this.dictionaryDAO = dictionaryDAO;
    }
    
    /**
     * Set the namespace DOA
     */
    public void setNamespaceDAO(NamespaceDAO namespaceDAO)
    {
        this.namespaceDAO = namespaceDAO;
    }
    
    /**
     * Set the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Set the content service
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }
    
    /**
     * Set the policy component
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    
    /**
     * Set the workflow service
     */
    public void setWorkflowService(WorkflowService workflowService)
    {
        this.workflowService = workflowService;
    }
    
    /**
     * Set the search service
     */
    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }
    
    /**
     * Set the namespace service
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }
    
    /**
     * Set the tenant service
     */
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }
    
    /**
     * Set the tenant admin service
     */
    public void setTenantAdminService(TenantAdminService tenantAdminService)
    {
        this.tenantAdminService = tenantAdminService;
    }
    
    public void setStoreUrls(List<String> storeUrls)
    {
        this.storeUrls = storeUrls;
    }
    
    
    /**
     * The initialise method
     */
    public void init()
    {
        // Register interest in the onContentUpdate policy for the dictionary model type
        policyComponent.bindClassBehaviour(
                ContentServicePolicies.ON_CONTENT_UPDATE, 
                ContentModel.TYPE_DICTIONARY_MODEL, 
                new JavaBehaviour(this, "onContentUpdate"));
        
        // Register interest in the onUpdateProperties policy for the dictionary model type
        policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateProperties"), 
                ContentModel.TYPE_DICTIONARY_MODEL, 
                new JavaBehaviour(this, "onUpdateProperties"));
        
        // Register interest in the beforeDeleteNode policy for the dictionary model type
        policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "beforeDeleteNode"), 
                ContentModel.TYPE_DICTIONARY_MODEL, 
                new JavaBehaviour(this, "beforeDeleteNode"));
        
        // Register interest in the onDeleteNode policy for the dictionary model type
        policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onDeleteNode"), 
                ContentModel.TYPE_DICTIONARY_MODEL, 
                new JavaBehaviour(this, "onDeleteNode"));
        
        // Register interest in the onRemoveAspect policy
        policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onRemoveAspect"), 
                this, 
                new JavaBehaviour(this, "onRemoveAspect"));
        
        // Register interest in the onCreateNode policy
        policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"),
                this, 
                new JavaBehaviour(this, "onCreateNode"));
        
        // Create the transaction listener
        this.transactionListener = new DictionaryModelTypeTransactionListener(this.nodeService, this.contentService);
    }
    
    /**
     * On content update behaviour implementation
     * 
     * @param nodeRef   the node reference whose content has been updated
     */
    public void onContentUpdate(NodeRef nodeRef, boolean newContent)
    {
        queueModel(nodeRef);
    }
    
    @SuppressWarnings("unchecked")
    private void queueModel(NodeRef nodeRef)
    {
        Set<NodeRef> pendingModelUpdates = (Set<NodeRef>)AlfrescoTransactionSupport.getResource(KEY_PENDING_MODELS);
        if (pendingModelUpdates == null)
        {
            pendingModelUpdates = new HashSet<NodeRef>();
            AlfrescoTransactionSupport.bindResource(KEY_PENDING_MODELS, pendingModelUpdates);
        }
        pendingModelUpdates.add(tenantService.getName(nodeRef));
        
        AlfrescoTransactionSupport.bindListener(this.transactionListener);
    }
    
    /**
     * On update properties behaviour implementation
     * 
     * @param nodeRef   the node reference
     * @param before    the values of the properties before update
     * @param after     the values of the properties after the update
     */
    public void onUpdateProperties(
            NodeRef nodeRef,
            Map<QName, Serializable> before,
            Map<QName, Serializable> after)
    {
        Boolean beforeValue = (Boolean)before.get(ContentModel.PROP_MODEL_ACTIVE);
        Boolean afterValue = (Boolean)after.get(ContentModel.PROP_MODEL_ACTIVE);
        
        if (beforeValue == null && afterValue != null)
        {
            queueModel(nodeRef);
        }
        else if (afterValue == null && beforeValue != null)
        {
            // Remove the model since the value has been cleared
            queueModel(nodeRef);
        }
        else if (beforeValue != null && afterValue != null && beforeValue.equals(afterValue) == false)
        {
            queueModel(nodeRef);
        }
    }
    
    public void onRemoveAspect(NodeRef nodeRef, QName aspect)
    {
    	// undo/cancel checkout removes the "workingcopy" aspect prior to deleting the node - hence need to track here
    	if (aspect.equals(ContentModel.ASPECT_WORKING_COPY))
    	{
    		AlfrescoTransactionSupport.bindResource(KEY_WORKING_COPY, nodeRef);
    	}
    	
        // restore removes the "archived" aspect prior to restoring (via delete/move) the node - hence need to track here
        if (aspect.equals(ContentModel.ASPECT_ARCHIVED))
        {
            AlfrescoTransactionSupport.bindResource(KEY_ARCHIVED, nodeRef);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void beforeDeleteNode(NodeRef nodeRef)
    {
    	boolean workingCopy = nodeService.hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY);
    	NodeRef wcNodeRef = (NodeRef)AlfrescoTransactionSupport.getResource(KEY_WORKING_COPY);
    	if ((wcNodeRef != null) && (wcNodeRef.equals(nodeRef)))
    	{
    		workingCopy = true;
    	}
    	
        boolean archived = nodeService.hasAspect(nodeRef, ContentModel.ASPECT_ARCHIVED);
        NodeRef aNodeRef = (NodeRef)AlfrescoTransactionSupport.getResource(KEY_ARCHIVED);
        if ((aNodeRef != null) && (aNodeRef.equals(nodeRef)))
        {
            archived = true;
        }
        
        // Ignore if the node is a working copy or archived
        if (! (workingCopy || archived))
        {
            QName modelName = (QName)this.nodeService.getProperty(nodeRef, ContentModel.PROP_MODEL_NAME);
            if (modelName != null)
            {
                // Validate model delete against usages - content and/or workflows
                validateModelDelete(modelName);
                
                if (logger.isDebugEnabled())
                {
                    logger.debug("beforeDeleteNode: modelName="+modelName+" ("+nodeRef+")");
                }
                
                Set<NodeRef> pendingModelDeletes = (Set<NodeRef>)AlfrescoTransactionSupport.getResource(KEY_PENDING_DELETE_MODELS);
                if (pendingModelDeletes == null)
                {
                    pendingModelDeletes = new HashSet<NodeRef>();
                    AlfrescoTransactionSupport.bindResource(KEY_PENDING_DELETE_MODELS, pendingModelDeletes);
                }
                pendingModelDeletes.add(tenantService.getName(nodeRef));
                
                AlfrescoTransactionSupport.bindListener(this.transactionListener);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public void onDeleteNode(ChildAssociationRef childAssocRef, boolean isNodeArchived)
    {
        NodeRef nodeRef = childAssocRef.getChildRef();
        
        Set<NodeRef> pendingDeleteModels = (Set<NodeRef>)AlfrescoTransactionSupport.getResource(KEY_PENDING_DELETE_MODELS);
        
        if (pendingDeleteModels != null)
        {
            if (pendingDeleteModels.contains(nodeRef))
            {
                String tenantDomain = tenantService.getDomain(nodeRef.getStoreRef().getIdentifier());
                String tenantSystemUserName = tenantService.getDomainUser(AuthenticationUtil.getSystemUserName(), tenantDomain);
                
                AuthenticationUtil.runAs(new RunAsWork<Object>()
                {
                    public Object doWork()
                    {
                        // invalidate - to force lazy re-init
                        dictionaryDAO.destroy();
                        
                        return null; 
                    }
                }, tenantSystemUserName);
            }
        }
    }
    
    public void onCreateNode(ChildAssociationRef childAssocRef)
    {
        NodeRef nodeRef = childAssocRef.getChildRef();
        if (nodeService.getType(nodeRef).equals(ContentModel.TYPE_DICTIONARY_MODEL))
        {
	        Boolean value = (Boolean)nodeService.getProperty(nodeRef, ContentModel.PROP_MODEL_ACTIVE);
	        if ((value != null) && (value == true))
	        {
	            queueModel(nodeRef);
	        }
        }
    }
    
    /**
     * Dictionary model type transaction listener class.
     */
    public class DictionaryModelTypeTransactionListener extends TransactionListenerAdapter
    {
        /**
         * Id used in equals and hash
         */
        private String id = GUID.generate();
        
        private NodeService nodeService;
        private ContentService contentService;
        
        public DictionaryModelTypeTransactionListener(NodeService nodeService, ContentService contentService)
        {
            this.nodeService = nodeService;
            this.contentService = contentService;
        }
        
        /**
         * @see org.alfresco.repo.transaction.TransactionListener#beforeCommit(boolean)
         */
        @SuppressWarnings("unchecked")
        @Override
        public void beforeCommit(boolean readOnly)
        { 
            Set<NodeRef> pendingModels = (Set<NodeRef>)AlfrescoTransactionSupport.getResource(KEY_PENDING_MODELS);
            
            if (pendingModels != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("beforeCommit: pendingModelsCnt="+pendingModels.size());
                }
                
                // unbind the resource from the transaction
                AlfrescoTransactionSupport.unbindResource(KEY_PENDING_MODELS);
                
                for (NodeRef pendingNodeRef : pendingModels)
                {
                    String tenantDomain = tenantService.getDomain(pendingNodeRef.getStoreRef().getIdentifier());
                    String tenantSystemUserName = tenantService.getDomainUser(AuthenticationUtil.getSystemUserName(), tenantDomain);
                    
                    final NodeRef nodeRef = tenantService.getBaseName(pendingNodeRef);
                    
                    AuthenticationUtil.runAs(new RunAsWork<Object>()
                    {
                        public Object doWork()
                        {
                            // Find out whether the model is active
                            boolean isActive = false;
                            Boolean value = (Boolean)nodeService.getProperty(nodeRef, ContentModel.PROP_MODEL_ACTIVE);
                            if (value != null)
                            {
                                isActive = value.booleanValue();
                            }
                            
                            // Ignore if the node is a working copy or archived
                            if (! (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY) || nodeService.hasAspect(nodeRef, ContentModel.ASPECT_ARCHIVED)))
                            {
                                if (isActive == true)
                                {
                                    // 1. Compile the model and update the details on the node            
                                    // 2. Re-put the model
                                    
                                    ContentReader contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
                                    if (contentReader != null)
                                    {
                                        // Create a model from the current content
                                        M2Model m2Model = M2Model.createModel(contentReader.getContentInputStream());
                                        
                                        // Try and compile the model
                                        ModelDefinition modelDefinition = m2Model.compile(dictionaryDAO, namespaceDAO).getModelDefinition();
                                        
                                        // Update the meta data for the model
                                        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
                                        props.put(ContentModel.PROP_MODEL_NAME, modelDefinition.getName());
                                        props.put(ContentModel.PROP_MODEL_DESCRIPTION, modelDefinition.getDescription());
                                        props.put(ContentModel.PROP_MODEL_AUTHOR, modelDefinition.getAuthor());
                                        props.put(ContentModel.PROP_MODEL_PUBLISHED_DATE, modelDefinition.getPublishedDate());
                                        props.put(ContentModel.PROP_MODEL_VERSION, modelDefinition.getVersion());
                                        nodeService.setProperties(nodeRef, props);
                                        
                                        ArrayList<NodeRef> modelNodeRefs = getModelNodes(nodeRef.getStoreRef(), modelDefinition.getName());
                                        for (NodeRef existingNodeRef : modelNodeRefs)
                                        {
                                        	if (! existingNodeRef.equals(nodeRef))
                                        	{
                                        		// check if existing model node is active
                                                Boolean existingValue = (Boolean)nodeService.getProperty(existingNodeRef, ContentModel.PROP_MODEL_ACTIVE);
                                                if ((existingValue != null) && (existingValue.booleanValue() == true))
                                                {
                                                	String name = (String)nodeService.getProperty(existingNodeRef, ContentModel.PROP_NAME);
                                                	
                                                	// for MT import, model may have been activated by DictionaryRepositoryBootstrap
                                                	if (logger.isDebugEnabled())
                                                	{
                                                		logger.debug("Re-activating '"+modelDefinition.getName()+"' - existing active model: " + name);
                                                	}
                                                    //throw new AlfrescoRuntimeException("Cannot activate '"+modelDefinition.getName()+"' - existing active model: " + name);
                                                }
                                        	}
                                        }
                                        
                                        // Validate model against dictionary - could be new, unchanged or updated
                                        dictionaryDAO.validateModel(m2Model);
                                        
                                        // invalidate - to force lazy re-init
                                        dictionaryDAO.destroy();
                                    }
                                }
                                else
                                {
                                    QName modelName = (QName)nodeService.getProperty(nodeRef, ContentModel.PROP_MODEL_NAME);
                                    if (modelName != null)
                                    {
                                        // Validate model delete against usages - content and/or workflows
                                        validateModelDelete(modelName);
                                        
                                        // invalidate - to force lazy re-init
                                        dictionaryDAO.destroy();
                                    }
                                }
                            }
                            
                            return null; 
                        }
                    }, tenantSystemUserName);
                }
            }
        }
        
        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj instanceof DictionaryModelTypeTransactionListener)
            {
                DictionaryModelTypeTransactionListener that = (DictionaryModelTypeTransactionListener) obj;
                return (this.id.equals(that.id));
            }
            else
            {
                return false;
            }
        }
    }
    
    /**
     * validate against repository contents / workflows (e.g. when deleting an existing model)
     * 
     * @param modelName
     */
    private void validateModelDelete(final QName modelName)
    {
        // TODO add model locking during delete (would need to be tenant-aware & cluster-aware) to avoid potential 
    	//      for concurrent addition of new content/workflow as model is being deleted
        
        try
        {
        	dictionaryDAO.getModel(modelName); // ignore returned model definition
        }
        catch (DictionaryException e)
        {
        	logger.warn("Model ' + modelName + ' does not exist ... skip delete validation : " + e);
        	return;
        }
        
        // TODO - in case of MT we do not currently allow deletion of an overridden model (with usages) ... but could allow if (re-)inherited model is equivalent to an incremental update only ?
        validateModelDelete(modelName, false);
        
        if (tenantService.isEnabled() && tenantService.isTenantUser() == false)
        {
            // shared model - need to check all tenants (whether enabled or disabled) unless they have overridden
            List<Tenant> tenants = tenantAdminService.getAllTenants();
            for (Tenant tenant : tenants)
            {
                // validate model delete within context of tenant domain
                AuthenticationUtil.runAs(new RunAsWork<Object>()
                {
                    public Object doWork()
                    {
                        if (dictionaryDAO.isModelInherited(modelName))
                        {
                            validateModelDelete(modelName, true);
                        }
                        return null;
                    }
                }, tenantService.getDomainUser(AuthenticationUtil.getSystemUserName(), tenant.getTenantDomain()));
            }
        }
    }
    
    private void validateModelDelete(QName modelName, boolean sharedModel)
    {
        String tenantDomain = TenantService.DEFAULT_DOMAIN;   
        if (sharedModel)
        {
            tenantDomain = " for tenant [" + tenantService.getCurrentUserDomain() + "]";
        }
        
        // check workflow namespace usage
        for (WorkflowDefinition workflowDef : workflowService.getDefinitions())
        {
            String workflowDefName = workflowDef.getName();
            String workflowNamespaceURI = QName.createQName(BPMEngineRegistry.getLocalId(workflowDefName), namespaceService).getNamespaceURI();
            for (NamespaceDefinition namespace : dictionaryDAO.getNamespaces(modelName))
            {
                if (workflowNamespaceURI.equals(namespace.getUri()))
                {
                    throw new AlfrescoRuntimeException("Failed to validate model delete" + tenantDomain + " - found workflow process definition " + workflowDefName + " using model namespace '" + namespace.getUri() + "'");
                }
            }
        }
 
        // check for type usages
        for (TypeDefinition type : dictionaryDAO.getTypes(modelName))
        {
        	validateClass(tenantDomain, type);
        }

        // check for aspect usages
        for (AspectDefinition aspect : dictionaryDAO.getAspects(modelName))
        {
        	validateClass(tenantDomain, aspect);
        }
    }
    
    private void validateClass(String tenantDomain, ClassDefinition classDef)
    {
    	QName className = classDef.getName();
        
        String classType = "TYPE";
        if (classDef instanceof AspectDefinition)
        {
        	classType = "ASPECT";
        }
        
        for (String storeUrl : this.storeUrls)
        {
            StoreRef store = new StoreRef(storeUrl);
            
            // search for TYPE or ASPECT - TODO - alternative would be to extract QName and search by namespace ...
            ResultSet rs = searchService.query(store, SearchService.LANGUAGE_LUCENE, classType+":\""+className+"\"");
            try
            {
                if (rs.length() > 0)
                {
                    throw new AlfrescoRuntimeException("Failed to validate model delete" + tenantDomain + " - found " + rs.length() + " nodes in store " + store + " with " + classType + " '" + className + "'" );
                }
            }
            finally
            {
                rs.close();
            }
        }
        
        // check against workflow task usage
        for (WorkflowDefinition workflowDef : workflowService.getDefinitions())
        {
            for (WorkflowTaskDefinition workflowTaskDef : workflowService.getTaskDefinitions(workflowDef.getId()))
            {
                TypeDefinition workflowTypeDef = workflowTaskDef.metadata;
                if (workflowTypeDef.getName().toString().equals(className))
                {
                    throw new AlfrescoRuntimeException("Failed to validate model delete" + tenantDomain + " - found task definition in workflow " + workflowDef.getName() + " with " + classType + " '" + className + "'");
                }
            }
        }
    }
    
	private ArrayList<NodeRef> getModelNodes(StoreRef storeRef, QName modelName)
	{
		ArrayList<NodeRef> nodeRefs = new ArrayList<NodeRef>();
		
	    ResultSet rs = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, "TYPE:\""+ContentModel.TYPE_DICTIONARY_MODEL+"\"");
	    try
	    {
    	    if (rs.length() > 0)
    	    {
    	    	for (NodeRef modelNodeRef : rs.getNodeRefs())
    	        {
    	    		QName name = (QName)nodeService.getProperty(modelNodeRef, ContentModel.PROP_MODEL_NAME);
    	    		if ((name != null) && (name.equals(modelName)))
    	    		{
    	    			nodeRefs.add(modelNodeRef);
    	    		}
    	        }
    	    }
	    }
	    finally
	    {
	        rs.close();
	    }
	    return nodeRefs;
	}
}
