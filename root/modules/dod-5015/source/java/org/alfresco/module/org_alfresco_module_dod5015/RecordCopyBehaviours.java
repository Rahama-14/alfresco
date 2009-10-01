/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.org_alfresco_module_dod5015;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService;
import org.alfresco.repo.copy.AbstractCopyBehaviourCallback;
import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.DoNothingCopyBehaviourCallback;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

/**
 * Class containing behaviour for the vitalRecordDefinition aspect.
 * 
 * @author neilm
 */
public class RecordCopyBehaviours implements RecordsManagementModel
{
    /** The policy component */
    private PolicyComponent policyComponent;
    
    /** The rm service registry */
    private RecordsManagementServiceRegistry rmServiceRegistry;
    
    /** List of aspects to remove during move and copy */
    private List<QName> unwantedAspects = new ArrayList<QName>(5);
    
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
     * Set the rm service registry.
     * 
     * @param recordsManagementServiceRegistry   the rm service registry.
     */
    public void setRecordsManagementServiceRegistry(RecordsManagementServiceRegistry recordsManagementServiceRegistry)
    {
        this.rmServiceRegistry = recordsManagementServiceRegistry;
    }

    /**
     * Initialise the vitalRecord aspect policies
     */
    public void init()
    {
        // Set up list of unwanted aspects
        unwantedAspects.add(ASPECT_VITAL_RECORD);
        unwantedAspects.add(ASPECT_DISPOSITION_LIFECYCLE);
        unwantedAspects.add(RecordsManagementSearchBehaviour.ASPECT_RM_SEARCH);
        
        // Do not copy any of the Alfresco-internal 'state' aspects
        for (QName aspect : unwantedAspects)
        {
            this.policyComponent.bindClassBehaviour(
                    QName.createQName(NamespaceService.ALFRESCO_URI, "getCopyCallback"),
                    aspect,
                    new JavaBehaviour(this, "getDoNothingCopyCallback"));
        }
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "getCopyCallback"),
                ASPECT_RECORD_COMPONENT_ID,
                new JavaBehaviour(this, "getIdCallback"));
        
        // Move behaviour 
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onMoveNode"),
                RecordsManagementModel.ASPECT_RECORD, 
                new JavaBehaviour(this, "onMoveRecordNode", NotificationFrequency.FIRST_EVENT));
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onMoveNode"),
                RecordsManagementModel.TYPE_RECORD_FOLDER, 
                new JavaBehaviour(this, "onMoveRecordFolderNode", NotificationFrequency.FIRST_EVENT));        
    }
    
    /**
     * onMove record behaviour
     * 
     * @param oldChildAssocRef
     * @param newChildAssocRef
     */
    public void onMoveRecordNode(ChildAssociationRef oldChildAssocRef, ChildAssociationRef newChildAssocRef)
    {
        final NodeRef newNodeRef = newChildAssocRef.getChildRef();
        final NodeService nodeService = rmServiceRegistry.getNodeService();
        
        AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                if (nodeService.exists(newNodeRef) == true)
                {
                    // Remove unwanted aspects
                    removeUnwantedAspects(nodeService, newNodeRef);
                }
                
                return null;
            }
        }, AuthenticationUtil.getAdminUserName());
    }
    
    /**
     * onMove record folder behaviour
     * 
     * @param oldChildAssocRef
     * @param newChildAssocRef
     */
    public void onMoveRecordFolderNode(ChildAssociationRef oldChildAssocRef, ChildAssociationRef newChildAssocRef)
    {
        final NodeRef newNodeRef = newChildAssocRef.getChildRef();
        final NodeService nodeService = rmServiceRegistry.getNodeService();
        final RecordsManagementService rmService = rmServiceRegistry.getRecordsManagementService();
        final RecordsManagementActionService rmActionService = rmServiceRegistry.getRecordsManagementActionService();
        
        AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                if (nodeService.exists(newNodeRef) == true)
                {
                    // Remove unwanted aspects
                    removeUnwantedAspects(nodeService, newNodeRef);
                    
                    // Trigger folder setup
                    rmActionService.executeRecordsManagementAction(newNodeRef, "setupRecordFolder");
                    
                    // Sort out the child records
                    for (NodeRef record : rmService.getRecords(newNodeRef))
                    {
                        removeUnwantedAspects(nodeService, record);
                        rmActionService.executeRecordsManagementAction(record, "file");
                    }
                }
                
                return null;
            }
        }, AuthenticationUtil.getAdminUserName());
    }
    
    /**
     * Removes unwanted aspects
     * 
     * @param nodeService
     * @param nodeRef
     */
    private void removeUnwantedAspects(NodeService nodeService, NodeRef nodeRef)
    {
        // Remove unwanted aspects
        for (QName aspect : unwantedAspects)
        {
            if (nodeService.hasAspect(nodeRef, aspect) == true)
            {
                nodeService.removeAspect(nodeRef, aspect);
            }
        }
    }
    
    /**
     * Get the "do nothing" call back behaviour
     * 
     * @param classRef
     * @param copyDetails
     * @return
     */
    public CopyBehaviourCallback getDoNothingCopyCallback(QName classRef, CopyDetails copyDetails)
    {
        return new DoNothingCopyBehaviourCallback();
    }
    
    public CopyBehaviourCallback getIdCallback(QName classRef, CopyDetails copyDetails)
    {
        return new AbstractCopyBehaviourCallback()
        {
            public ChildAssocCopyAction getChildAssociationCopyAction(
                    QName classQName,
                    CopyDetails copyDetails,
                    CopyChildAssociationDetails childAssocCopyDetails)
            {
                return null;
            }

            public Map<QName, Serializable> getCopyProperties(
                    QName classQName,
                    CopyDetails copyDetails,
                    Map<QName, Serializable> properties)
            {
                properties.put(PROP_IDENTIFIER, properties.get(PROP_IDENTIFIER) + "1");
                return properties;
            }

            public boolean getMustCopy(QName classQName, CopyDetails copyDetails)
            {
                return true;
            }
            
        };
    }
    
    /**
     * Function to pad a string with zero '0' characters to the required length
     * 
     * @param s     String to pad with leading zero '0' characters
     * @param len   Length to pad to
     * 
     * @return padded string or the original if already at >=len characters 
     */
    protected String padString(String s, int len)
    {
       String result = s;
       for (int i=0; i<(len - s.length()); i++)
       {
           result = "0" + result;
       }
       return result;
    } 
}
