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
package org.alfresco.repo.audit;

import java.util.Date;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.PolicyScope;
import org.alfresco.repo.security.authentication.AuthenticationService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This aspect maintains the audit properties of the Auditable aspect.
 *  
 * @author David Caruana
 */
public class AuditableAspect
{
    // Logger
    private static final Log logger = LogFactory.getLog(AuditableAspect.class);

    // Unknown user, for when authentication has not occured
    private static final String USERNAME_UNKNOWN = "unknown";
    
    // Dependencies
    private NodeService nodeService;
    private AuthenticationService authenticationService;
    private PolicyComponent policyComponent;

    // Behaviours
    private Behaviour onCreateAudit;
    private Behaviour onAddAudit;
    private Behaviour onUpdateAudit;
    

    /**
     * @param nodeService  the node service to use for audit property maintenance
     */    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param policyComponent  the policy component
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    
    /**
     * @param authenticationService  the authentication service
     */
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService; 
    }
    
    /**
     * Initialise the Auditable Aspect
     */
    public void init()
    {
        // Create behaviours
        onCreateAudit =  new JavaBehaviour(this, "onCreateAudit");
        onAddAudit = new JavaBehaviour(this, "onAddAudit");
        onUpdateAudit = new JavaBehaviour(this, "onUpdateAudit");
        
        // Bind behaviours to node policies
        policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"), ContentModel.ASPECT_AUDITABLE, onCreateAudit);
        policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onAddAspect"), ContentModel.ASPECT_AUDITABLE, onAddAudit);
        policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateNode"), ContentModel.ASPECT_AUDITABLE, onUpdateAudit);
        
		// Register onCopy class behaviour
		policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onCopyNode"), ContentModel.ASPECT_AUDITABLE, new JavaBehaviour(this, "onCopy"));
    }

    /**
     * Maintain audit properties on creation of Node
     * 
     * @param childAssocRef  the association to the child created
     */
    public void onCreateAudit(ChildAssociationRef childAssocRef)
    {
        NodeRef nodeRef = childAssocRef.getChildRef();
        onAddAudit(nodeRef, null);
    }

    /**
     * Maintain audit properties on addition of audit aspect to a node
     * 
     * @param nodeRef  the node to which auditing has been added 
     * @param aspect  the aspect added
     */
    public void onAddAudit(NodeRef nodeRef, QName aspect)
    {
        try
        {
            onUpdateAudit.disable();
            
            // Set created / updated date
            Date now = new Date(System.currentTimeMillis());
            nodeService.setProperty(nodeRef, ContentModel.PROP_CREATED, now);
            nodeService.setProperty(nodeRef, ContentModel.PROP_MODIFIED, now);

            // Set creator (but do not override, if explicitly set)
            String creator = (String)nodeService.getProperty(nodeRef, ContentModel.PROP_CREATOR);
            if (creator == null || creator.length() == 0)
            {
                creator = getUsername();
                nodeService.setProperty(nodeRef, ContentModel.PROP_CREATOR, creator);
            }
            nodeService.setProperty(nodeRef, ContentModel.PROP_MODIFIER, creator);
            
            if (logger.isDebugEnabled())
                logger.debug("Auditable node " + nodeRef + " created [created,modified=" + now + ";creator,modifier=" + creator + "]");
        }
        finally
        {
            onUpdateAudit.enable();
        }
    }

    /**
     * Maintain audit properties on update of node
     * 
     * @param nodeRef  the updated node
     */
    public void onUpdateAudit(NodeRef nodeRef)
    {
        // Set updated date
        Date now = new Date(System.currentTimeMillis());
        nodeService.setProperty(nodeRef, ContentModel.PROP_MODIFIED, now);

        // Set modifier
        String modifier = getUsername();
        nodeService.setProperty(nodeRef, ContentModel.PROP_MODIFIER, modifier);
        
        if (logger.isDebugEnabled())
            logger.debug("Auditable node " + nodeRef + " updated [modified=" + now + ";modifier=" + modifier + "]");
    }

    /**
     * @return  the current username (or unknown, if unknown)
     */
    private String getUsername()
    {
        String currentUserName = authenticationService.getCurrentUserName();
        if (currentUserName != null)
        {
           return currentUserName;
        }
        return USERNAME_UNKNOWN;
    }
    
    /**
	 * OnCopy behaviour implementation for the lock aspect.
	 * <p>
	 * Ensures that the propety values of the lock aspect are not copied onto
	 * the destination node.
	 * 
	 * @see org.alfresco.repo.copy.CopyServicePolicies.OnCopyNodePolicy#onCopyNode(QName, NodeRef, StoreRef, boolean, PolicyScope)
	 */
	public void onCopy(
            QName sourceClassRef, 
            NodeRef sourceNodeRef, 
            StoreRef destinationStoreRef,
            boolean copyToNewNode,
            PolicyScope copyDetails)
	{
		// The auditable aspect should not be copied
	}    
}
