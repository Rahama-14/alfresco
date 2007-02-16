/*
 * Copyright (C) 2005 Alfresco, Inc.
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
package org.alfresco.repo.node.integrity;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Component that tags {@link org.alfresco.model.ContentModel#ASPECT_INCOMPLETE incomplete} nodes. 
 * 
 * @author Derek Hulley
 */
public class IncompleteNodeTagger
        extends     TransactionListenerAdapter
        implements  NodeServicePolicies.OnCreateNodePolicy,
                    NodeServicePolicies.OnUpdatePropertiesPolicy,
                    NodeServicePolicies.OnAddAspectPolicy,
                    NodeServicePolicies.OnRemoveAspectPolicy,
                    NodeServicePolicies.OnCreateChildAssociationPolicy,
                    NodeServicePolicies.OnDeleteChildAssociationPolicy,
                    NodeServicePolicies.OnCreateAssociationPolicy,
                    NodeServicePolicies.OnDeleteAssociationPolicy
{
    private static Log logger = LogFactory.getLog(IncompleteNodeTagger.class);
    
    /** key against which the set of nodes to check is stored in the current transaction */
    private static final String KEY_NODES = "IncompleteNodeTagger.Nodes";
    
    private PolicyComponent policyComponent;
    private DictionaryService dictionaryService;
    private NodeService nodeService;
    
    public IncompleteNodeTagger()
    {
    }

    /**
     * @param policyComponent the component to register behaviour with
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    /**
     * @param dictionaryService the dictionary against which to confirm model details
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     * @param nodeService the node service to use for browsing node structures
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * Registers the system-level policy behaviours
     */
    public void init()
    {
        // check that required properties have been set
        PropertyCheck.mandatory("IncompleteNodeTagger", "dictionaryService", dictionaryService);
        PropertyCheck.mandatory("IncompleteNodeTagger", "nodeService", nodeService);
        PropertyCheck.mandatory("IncompleteNodeTagger", "policyComponent", policyComponent);

        // register behaviour
        policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"),
                this,
                new JavaBehaviour(this, "onCreateNode"));   
        policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateProperties"),
                this,
                new JavaBehaviour(this, "onUpdateProperties"));   
        policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onAddAspect"),
                this,
                new JavaBehaviour(this, "onAddAspect"));   
        policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onRemoveAspect"),
                this,
                new JavaBehaviour(this, "onRemoveAspect"));   
        policyComponent.bindAssociationBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateChildAssociation"),
                this,
                new JavaBehaviour(this, "onCreateChildAssociation"));   
        policyComponent.bindAssociationBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onDeleteChildAssociation"),
                this,
                new JavaBehaviour(this, "onDeleteChildAssociation"));   
        policyComponent.bindAssociationBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateAssociation"),
                this,
                new JavaBehaviour(this, "onCreateAssociation"));   
        policyComponent.bindAssociationBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onDeleteAssociation"),
                this,
                new JavaBehaviour(this, "onDeleteAssociation"));   
    }
    
    /**
     * @return Returns the set of nodes to check properties, or null if none were registered
     */
    @SuppressWarnings("unchecked")
    private Map<NodeRef, Set<QName>> getNodes()
    {
        return (Map<NodeRef, Set<QName>>) AlfrescoTransactionSupport.getResource(KEY_NODES);
    }

    /**
     * Ensures that this service is registered with the transaction and saves the node
     * reference for use (property check) later.
     * 
     * @param nodeRef
     */
    private Set<QName> save(NodeRef nodeRef)
    {
        Set<QName> assocs = null;
        
        // register this service
        AlfrescoTransactionSupport.bindListener(this);
        
        // get the event list
        Map<NodeRef, Set<QName>> nodes = getNodes();
        if (nodes == null)
        {
            nodes = new HashMap<NodeRef, Set<QName>>(31, 0.75F);
            AlfrescoTransactionSupport.bindResource(KEY_NODES, nodes);
        }
        // add node to the set
        if (nodes.containsKey(nodeRef))
        {
            assocs = nodes.get(nodeRef);
        }
        else
        {
            nodes.put(nodeRef, null);
        }
        
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Added node reference to property set: " + nodeRef);
        }
        
        return assocs;
    }

    /**
     * Ensures that this service is registered with the transaction and saves the node
     * reference for use (association check) later.
     * 
     * @param nodeRef
     * @param assocType
     */
    private void saveAssoc(NodeRef nodeRef, QName assocType)
    {
        // register this service
        AlfrescoTransactionSupport.bindListener(this);

        Set<QName> assocs = save(nodeRef);
        if (assocs == null)
        {
            assocs = new HashSet<QName>(7, 0.75f);
            Map<NodeRef, Set<QName>> nodes = getNodes();
            nodes.put(nodeRef, assocs);
        }
        if (assocType != null)
        {
            assocs.add(assocType);
        }
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Added association to node: " + nodeRef + ", " + assocType);
        }
    }
    
    public void onCreateNode(ChildAssociationRef childAssocRef)
    {
        NodeRef nodeRef = childAssocRef.getChildRef();
        save(nodeRef);
        saveAssoc(nodeRef, null);
    }

    public void onUpdateProperties(
            NodeRef nodeRef,
            Map<QName, Serializable> before,
            Map<QName, Serializable> after)
    {
        save(nodeRef);
    }

    /**
     * Save the node for checking of properties.
     * The {@link org.alfresco.model.ContentModel#ASPECT_INCOMPLETE incomplete} aspect is
     * not processed.
     */
    public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName)
    {
        if (aspectTypeQName.equals(ContentModel.ASPECT_INCOMPLETE))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Ignoring aspect addition: " + ContentModel.ASPECT_INCOMPLETE);
            }
        }
        save(nodeRef);
    }

    /**
     * Recheck the node as an aspect was removed.
     */
    public void onRemoveAspect(NodeRef nodeRef, QName aspectTypeQName)
    {
        if (aspectTypeQName.equals(ContentModel.ASPECT_INCOMPLETE))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Ignoring aspect removal: " + ContentModel.ASPECT_INCOMPLETE);
            }
        }
        save(nodeRef);
    }
    
    /**
     * @see AssocSourceTypeIntegrityEvent
     * @see AssocTargetTypeIntegrityEvent
     * @see AssocSourceMultiplicityIntegrityEvent
     * @see AssocTargetMultiplicityIntegrityEvent
     * @see AssocTargetRoleIntegrityEvent
     */
    public void onCreateChildAssociation(ChildAssociationRef childAssocRef)
    {
        saveAssoc(childAssocRef.getParentRef(), childAssocRef.getTypeQName());
    }

    /**
     * @see AssocSourceMultiplicityIntegrityEvent
     * @see AssocTargetMultiplicityIntegrityEvent
     */
    public void onDeleteChildAssociation(ChildAssociationRef childAssocRef)
    {
        saveAssoc(childAssocRef.getParentRef(), childAssocRef.getTypeQName());
    }

    /**
     * @see AssocSourceTypeIntegrityEvent
     * @see AssocTargetTypeIntegrityEvent
     * @see AssocSourceMultiplicityIntegrityEvent
     * @see AssocTargetMultiplicityIntegrityEvent
     */
    public void onCreateAssociation(AssociationRef nodeAssocRef)
    {
        saveAssoc(nodeAssocRef.getSourceRef(), nodeAssocRef.getTypeQName());
    }

    /**
     * @see AssocSourceMultiplicityIntegrityEvent
     * @see AssocTargetMultiplicityIntegrityEvent
     */
    public void onDeleteAssociation(AssociationRef nodeAssocRef)
    {
        saveAssoc(nodeAssocRef.getSourceRef(), nodeAssocRef.getTypeQName());
    }
        
    /**
     * Process all the nodes that require checking within the transaction.
     */
    @Override
    public void beforeCommit(boolean readOnly)
    {
        Map<NodeRef, Set<QName>> nodes = getNodes();
        // clear the set out of the transaction
        // there may be processes that react to the addition/removal of the aspect,
        //    and these will, in turn, lead to further events
        AlfrescoTransactionSupport.unbindResource(KEY_NODES);
        // process each node
        for (Map.Entry<NodeRef, Set<QName>> entry : nodes.entrySet())
        {
            if (nodeService.exists(entry.getKey()))
            {
                processNode(entry.getKey(), entry.getValue());
            }
        }
    }

    private void processNode(NodeRef nodeRef, Set<QName> assocTypes)
    {
        // ignore the node if the marker aspect is already present
        boolean isTagged = nodeService.hasAspect(nodeRef, ContentModel.ASPECT_INCOMPLETE);
        
        // get the node properties
        Map<QName, Serializable> nodeProperties = nodeService.getProperties(nodeRef);
        // get the node type
        QName nodeTypeQName = nodeService.getType(nodeRef);
        // get property definitions for the node type
        TypeDefinition typeDef = dictionaryService.getType(nodeTypeQName);
        if (typeDef == null)
        {
            throw new AlfrescoRuntimeException("Node type is not recognised: " + nodeTypeQName);
        }
        Collection<PropertyDefinition> propertyDefs = typeDef.getProperties().values();
        // check them
        boolean classPropertiesOK = checkProperties(propertyDefs, nodeProperties);
        
        // were there outstanding properties to check?
        if (!classPropertiesOK)
        {
            addOrRemoveTag(nodeRef, true, isTagged);
            // no further checking required
            return;
        }
        
        // get the node aspects
        Set<QName> aspectTypeQNames = nodeService.getAspects(nodeRef);
        for (QName aspectTypeQName : aspectTypeQNames)
        {
            // get property definitions for the aspect
            AspectDefinition aspectDef = dictionaryService.getAspect(aspectTypeQName);
            propertyDefs = aspectDef.getProperties().values();
            // check them
            boolean aspectPropertiesOK = checkProperties(propertyDefs, nodeProperties);
            // were there outstanding properties to check?
            if (!aspectPropertiesOK)
            {
                addOrRemoveTag(nodeRef, true, isTagged);
                // no further checking required
                return;
            }
        }
        
        // test associations
        if (assocTypes != null)
        {
            Map<QName, AssociationDefinition> assocDefs = typeDef.getAssociations();
            if (assocTypes.size() > 0)
            {
                // check only those associations that have changed
                for (QName assocType : assocTypes)
                {
                    AssociationDefinition assocDef = assocDefs.get(assocType);
                    if (assocDef != null)
                    {
                        if (!checkAssociation(nodeRef, assocDef))
                        {
                            addOrRemoveTag(nodeRef, true, isTagged);
                            return;
                        }
                    }
                }
            }
            else
            {
                // check all associations (typically for new objects)
                for (QName assocType : assocDefs.keySet())
                {
                    AssociationDefinition assocDef = assocDefs.get(assocType);
                    if (assocDef != null)
                    {
                        if (!checkAssociation(nodeRef, assocDef))
                        {
                            addOrRemoveTag(nodeRef, true, isTagged);
                            return;
                        }
                    }
                }
            }
        }
        
        // all properties and associations passed (both class- and aspect-defined) - remove aspect
        addOrRemoveTag(nodeRef, false, isTagged);
    }
    
    /**
     * @param propertyDefs the property definitions to check
     * @param properties the properties
     * @return Returns true if the property definitions were all satisified
     */
    private boolean checkProperties(
            Collection<PropertyDefinition> propertyDefs,
            Map<QName, Serializable> properties)
    {
        for (PropertyDefinition propertyDef : propertyDefs)
        {
            if (!propertyDef.isMandatory())
            {
                // The property isn't mandatory in any way
                continue;
            }
            else if (propertyDef.isMandatoryEnforced())
            {
                // The mandatory nature of the property is fully enforced
                // Leave these for integrity
                continue;
            }
            // The mandatory nature of the property is 'soft' a.k.a. 'required'
            // Check that the property value has been supplied
            if (properties.get(propertyDef.getName()) == null)
            {
                // property NOT supplied
                return false;
            }
        }
        // all properties were present
        return true;
    }

    /**
     * @param nodeRef
     * @param assocDef
     * @return
     */
    private boolean checkAssociation(NodeRef nodeRef, AssociationDefinition assocDef)
    {
        boolean complete = true;
        
        if (assocDef.isTargetMandatory() && !assocDef.isTargetMandatoryEnforced())
        {
            int actualSize = 0;
            if (assocDef.isChild())
            {
                // check the child assocs present
                List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(
                        nodeRef,
                        assocDef.getName(),
                        RegexQNamePattern.MATCH_ALL);
                actualSize = childAssocRefs.size();
            }
            else
            {
                // check the target assocs present
                List<AssociationRef> targetAssocRefs = nodeService.getTargetAssocs(nodeRef, assocDef.getName());
                actualSize = targetAssocRefs.size();
            }
            if (assocDef.isTargetMandatory() && actualSize == 0)
            {
                complete = false;
            }
        }
        return complete;
    }
    
    /**
     * Adds or removes the {@link ContentModel#ASPECT_INCOMPLETE incomplete} marker aspect.
     * This only performs the operation if the tag aspect is or is not present, depending
     * on the operation required.
     * 
     * @param nodeRef the node to apply the change to
     * @param addTag <tt>true</tt> to add the tag and <tt>false</tt> to remove the tag
     * @param isTagged <tt>true</tt> if the node already has the tag aspect applied,
     *      otherwise <tt>false</tt>
     */
    private void addOrRemoveTag(NodeRef nodeRef, boolean addTag, boolean isTagged)
    {
        if (addTag && !isTagged)
        {
            nodeService.addAspect(nodeRef, ContentModel.ASPECT_INCOMPLETE, null);
            // done
            if (logger.isDebugEnabled())
            {
                logger.debug("Tagged node as INCOMPLETE: " + nodeRef);
            }
        }
        else if (!addTag && isTagged)
        {
            nodeService.removeAspect(nodeRef, ContentModel.ASPECT_INCOMPLETE);
            // done
            if (logger.isDebugEnabled())
            {
                logger.debug("Untagged node as INCOMPLETE: " + nodeRef);
            }
        }
    }
}
