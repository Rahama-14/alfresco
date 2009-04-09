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
package org.alfresco.repo.version;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.CopyServicePolicies;
import org.alfresco.repo.copy.DefaultCopyBehaviourCallback;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

/**
 * Class containing behaviour for the versionable aspect
 * 
 * @author Roy Wetherall
 */
public class VersionableAspect implements ContentServicePolicies.OnContentUpdatePolicy, 
                                          NodeServicePolicies.OnAddAspectPolicy,
                                          NodeServicePolicies.OnRemoveAspectPolicy,
                                          NodeServicePolicies.OnDeleteNodePolicy,
                                          VersionServicePolicies.AfterCreateVersionPolicy,
                                          CopyServicePolicies.OnCopyNodePolicy
{
    /** The i18n'ized messages */
    private static final String MSG_INITIAL_VERSION = "create_version.initial_version";
    private static final String MSG_AUTO_VERSION = "create_version.auto_version";
    
    /** Transaction resource key */
    private static final String KEY_VERSIONED_NODEREFS = "versioned_noderefs";
    
    /** The policy component */
    private PolicyComponent policyComponent;
    
    /** The node service */
    private NodeService nodeService;
    
    /** The Version service */
    private VersionService versionService;

    /** Auto version behaviour */
    private Behaviour autoVersionBehaviour;
    
    /**
     * Set the policy component
     * 
     * @param policyComponent   the policy component
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    
    /**
     * Set the version service
     * 
     * @param versionService    the version service
     */
    public void setVersionService(VersionService versionService) 
    {
        this.versionService = versionService;
    }
    
    /**
     * Set the node service
     * 
     * @param nodeService   the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Initialise the versionable aspect policies
     */
    public void init()
    {
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onAddAspect"), 
                ContentModel.ASPECT_VERSIONABLE, 
                new JavaBehaviour(this, "onAddAspect", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onRemoveAspect"), 
                ContentModel.ASPECT_VERSIONABLE, 
                new JavaBehaviour(this, "onRemoveAspect", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onDeleteNode"), 
                ContentModel.ASPECT_VERSIONABLE, 
                new JavaBehaviour(this, "onDeleteNode", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "afterCreateVersion"), 
                ContentModel.ASPECT_VERSIONABLE, 
                new JavaBehaviour(this, "afterCreateVersion", Behaviour.NotificationFrequency.EVERY_EVENT));
        
        autoVersionBehaviour = new JavaBehaviour(this, "onContentUpdate", Behaviour.NotificationFrequency.TRANSACTION_COMMIT);
        this.policyComponent.bindClassBehaviour(
                ContentServicePolicies.ON_CONTENT_UPDATE,
                ContentModel.ASPECT_VERSIONABLE,
                autoVersionBehaviour);
        
        // Register the copy behaviour
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "getCopyCallback"),
                ContentModel.ASPECT_VERSIONABLE,
                new JavaBehaviour(this, "getCopyCallback"));
    }
    
    /**
     * @see org.alfresco.repo.node.NodeServicePolicies.OnDeleteNodePolicy#onDeleteNode(org.alfresco.service.cmr.repository.ChildAssociationRef, boolean)
     */
    public void onDeleteNode(ChildAssociationRef childAssocRef, boolean isNodeArchived) 
    {
        if (isNodeArchived == false)
        {
            // If we are perminantly deleting the node then we need to remove the associated version history
            this.versionService.deleteVersionHistory(childAssocRef.getChildRef());
        }
        // otherwise we do nothing since we need to hold onto the version history in case the node is restored later
    }
    
    /**
     * @return          Returns the {@link VersionableAspectCopyBehaviourCallback}
     */
    public CopyBehaviourCallback getCopyCallback(QName classRef, CopyDetails copyDetails)
    {
        return VersionableAspectCopyBehaviourCallback.INSTANCE;
    }

    /**
     * Copy behaviour for the <b>cm:versionable</b> aspect
     * 
     * @author Derek Hulley
     * @since 3.2
     */
    private static class VersionableAspectCopyBehaviourCallback extends DefaultCopyBehaviourCallback
    {
        private static final CopyBehaviourCallback INSTANCE = new VersionableAspectCopyBehaviourCallback();

        /**
         * Copy the aspect, but only the {@link ContentModel#PROP_AUTO_VERSION} property
         */
        @Override
        public Map<QName, Serializable> getCopyProperties(
                QName classQName,
                CopyDetails copyDetails,
                Map<QName, Serializable> properties)
        {
            Serializable value = properties.get(ContentModel.PROP_AUTO_VERSION);
            if (value != null)
            {
                return Collections.singletonMap(ContentModel.PROP_AUTO_VERSION, value);
            }
            else
            {
                return Collections.emptyMap();
            }
        }
    }
    
    /**
     * On add aspect policy behaviour
     * 
     * @param nodeRef
     * @param aspectTypeQName
     */
    public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName)
    {
        if (this.nodeService.exists(nodeRef) == true && aspectTypeQName.equals(ContentModel.ASPECT_VERSIONABLE) == true)
        {
            boolean initialVersion = true;
            Boolean value = (Boolean)this.nodeService.getProperty(nodeRef, ContentModel.PROP_INITIAL_VERSION);
            if (value != null)
            {
                initialVersion = value.booleanValue();
            }
            // else this means that the default value has not been set the versionable aspect we applied pre-1.2
            
            if (initialVersion == true)
            {
                @SuppressWarnings("unchecked")
                Map<NodeRef, NodeRef> versionedNodeRefs = (Map<NodeRef, NodeRef>) AlfrescoTransactionSupport.getResource(KEY_VERSIONED_NODEREFS);
                if (versionedNodeRefs == null || versionedNodeRefs.containsKey(nodeRef) == false)           
                {
                    // Queue create version action
                    Map<String, Serializable> versionDetails = new HashMap<String, Serializable>(1);
                    versionDetails.put(Version.PROP_DESCRIPTION, I18NUtil.getMessage(MSG_INITIAL_VERSION));
                    
                    this.versionService.createVersion(nodeRef, versionDetails);
                }
            }
        }
    }
    
    /**
     * @see org.alfresco.repo.node.NodeServicePolicies.OnRemoveAspectPolicy#onRemoveAspect(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void onRemoveAspect(NodeRef nodeRef, QName aspectTypeQName) 
    {
        // When the versionable aspect is removed from a node, then delete the associatied verison history
        this.versionService.deleteVersionHistory(nodeRef);
    }
    
    /**
     * On content update policy bahaviour
     * 
     * @param nodeRef   the node reference
     */
    @SuppressWarnings("unchecked")
    public void onContentUpdate(NodeRef nodeRef, boolean newContent)
    {
        if (this.nodeService.exists(nodeRef) == true && this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE) == true
                && this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_TEMPORARY) == false)
        {
            Map<NodeRef, NodeRef> versionedNodeRefs = (Map)AlfrescoTransactionSupport.getResource(KEY_VERSIONED_NODEREFS);
            if (versionedNodeRefs == null || versionedNodeRefs.containsKey(nodeRef) == false)            
            {
                // Determine whether the node is auto versionable or not
                boolean autoVersion = false;
                Boolean value = (Boolean)this.nodeService.getProperty(nodeRef, ContentModel.PROP_AUTO_VERSION);
                if (value != null)
                {
                    // If the value is not null then 
                    autoVersion = value.booleanValue();
                }
                // else this means that the default value has not been set and the versionable aspect was applied pre-1.1
                
                if (autoVersion == true)
                {
                    // Create the auto-version
                    Map<String, Serializable> versionProperties = new HashMap<String, Serializable>(1);
                    versionProperties.put(Version.PROP_DESCRIPTION, I18NUtil.getMessage(MSG_AUTO_VERSION));
                    this.versionService.createVersion(nodeRef, versionProperties);
                }
            }
        }
    }

    /**
     * @see org.alfresco.repo.version.VersionServicePolicies.OnCreateVersionPolicy#onCreateVersion(org.alfresco.service.namespace.QName, org.alfresco.service.cmr.repository.NodeRef, java.util.Map, org.alfresco.repo.policy.PolicyScope)
     */
    @SuppressWarnings("unchecked")
    public void afterCreateVersion(NodeRef versionableNode, Version version) 
    {
        Map<NodeRef, NodeRef> versionedNodeRefs = (Map<NodeRef, NodeRef>)AlfrescoTransactionSupport.getResource(KEY_VERSIONED_NODEREFS);
        if (versionedNodeRefs == null)
        {
            versionedNodeRefs = new HashMap<NodeRef, NodeRef>();
            AlfrescoTransactionSupport.bindResource(KEY_VERSIONED_NODEREFS, versionedNodeRefs);
        }
        versionedNodeRefs.put(versionableNode, versionableNode);
    } 
}
