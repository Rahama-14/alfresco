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
package org.alfresco.module.org_alfresco_module_dod5015;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Period;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

/**
 * Search Behaviour class.
 * 
 * Manages the collapse of data onto the supporting aspect on the record/record folder
 * 
 * @author Roy Wetherall
 */
public class RecordsManagementSearchBehaviour implements RecordsManagementModel
{
    /** Search specific elements of the RM model */
    public static final QName ASPECT_RM_SEARCH = QName.createQName(RM_URI, "recordSearch");
    public static final QName PROP_RS_DISPOSITION_ACTION_NAME = QName.createQName(RM_URI, "recordSearchDispositionActionName");
    public static final QName PROP_RS_DISPOSITION_ACTION_AS_OF = QName.createQName(RM_URI, "recordSearchDispositionActionAsOf");
    public static final QName PROP_RS_DISPOSITION_EVENTS_ELIGIBLE = QName.createQName(RM_URI, "recordSearchDispositionEventsEligible");
    public static final QName PROP_RS_DISPOSITION_EVENTS = QName.createQName(RM_URI, "recordSearchDispositionEvents");
    public static final QName PROP_RS_VITAL_RECORD_REVIEW_PERIOD = QName.createQName(RM_URI, "recordSearchVitalRecordReviewPeriod");
    public static final QName PROP_RS_VITAL_RECORD_REVIEW_PERIOD_EXPRESSION = QName.createQName(RM_URI, "recordSearchVitalRecordReviewPeriodExpression");
    
    /** Policy component */
    private PolicyComponent policyComponent;
    
    /** Node service */
    private NodeService nodeService;
    
    /**  Records management service */
    private RecordsManagementService recordsManagementService;

    /**
     * @param nodeService the nodeService to set
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param policyComponent the policyComponent to set
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    
    public void setRecordsManagementService(RecordsManagementService recordsManagementService)
    {
        this.recordsManagementService = recordsManagementService;
    }
    
    private JavaBehaviour onAddSearchAspect = new JavaBehaviour(this, "rmSearchAspectAdd", NotificationFrequency.TRANSACTION_COMMIT);
    
    /**
     * Initialisation method
     */
    public void init()
    {
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"), 
                TYPE_DISPOSITION_ACTION, 
                new JavaBehaviour(this, "dispositionActionCreate", NotificationFrequency.TRANSACTION_COMMIT));
        
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateProperties"), 
                TYPE_DISPOSITION_ACTION, 
                new JavaBehaviour(this, "dispositionActionPropertiesUpdate", NotificationFrequency.TRANSACTION_COMMIT));

        this.policyComponent.bindAssociationBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateChildAssociation"), 
                TYPE_DISPOSITION_ACTION, 
                ASSOC_EVENT_EXECUTIONS,
                new JavaBehaviour(this, "eventExecutionUpdate", NotificationFrequency.TRANSACTION_COMMIT));
        
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onAddAspect"), 
                ASPECT_RM_SEARCH, 
                onAddSearchAspect);
        
        
        // Vital Records Review Details Rollup
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onAddAspect"), 
                ASPECT_VITAL_RECORD_DEFINITION, 
                new JavaBehaviour(this, "vitalRecordDefintionAddAspect", NotificationFrequency.TRANSACTION_COMMIT));
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateProperties"), 
                ASPECT_VITAL_RECORD_DEFINITION, 
                new JavaBehaviour(this, "vitalRecordDefintionUpdateProperties", NotificationFrequency.TRANSACTION_COMMIT));
    }

    /**
     * 
     * @param nodeRef
     * @param before
     * @param after
     */
    public void dispositionActionPropertiesUpdate(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after)
    {
        if (this.nodeService.exists(nodeRef) == true)
        {
            ChildAssociationRef assoc = this.nodeService.getPrimaryParent(nodeRef);
            if (assoc.getTypeQName().equals(ASSOC_NEXT_DISPOSITION_ACTION) == true)
            {
                // Get the record (or record folder)
                NodeRef record = assoc.getParentRef();
                 
                // Apply the search aspect
                applySearchAspect(record);
                
                // Update disposition properties
                updateDispositionActionProperties(record, nodeRef);
            }
        }
    }
    
    private void applySearchAspect(NodeRef nodeRef)
    {
        onAddSearchAspect.disable();
        try
        {
            if (this.nodeService.hasAspect(nodeRef, ASPECT_RM_SEARCH) == false)
            {
                this.nodeService.addAspect(nodeRef, ASPECT_RM_SEARCH , null);
            }
        }
        finally
        {
            onAddSearchAspect.enable();
        }
    }
    
    public void dispositionActionCreate(ChildAssociationRef childAssocRef)
    {
        if (childAssocRef.getTypeQName().equals(ASSOC_NEXT_DISPOSITION_ACTION) == true)
        {
            // Get the record (or record folder)
            NodeRef record = childAssocRef.getParentRef();
             
            // Apply the search aspect
            applySearchAspect(record);
            
            // Update disposition properties
            updateDispositionActionProperties(record, childAssocRef.getChildRef());
            
            // Clear the events
            this.nodeService.setProperty(record, PROP_RS_DISPOSITION_EVENTS, null);
        }        
    }
    
    /**
     * 
     * @param record
     * @param dispositionAction
     */
    private void updateDispositionActionProperties(NodeRef record, NodeRef dispositionAction)
    {
        this.nodeService.setProperty(record, PROP_RS_DISPOSITION_ACTION_NAME, this.nodeService.getProperty(dispositionAction, PROP_DISPOSITION_ACTION));
        this.nodeService.setProperty(record, PROP_RS_DISPOSITION_ACTION_AS_OF, this.nodeService.getProperty(dispositionAction, PROP_DISPOSITION_AS_OF));
        this.nodeService.setProperty(record, PROP_RS_DISPOSITION_EVENTS_ELIGIBLE, this.nodeService.getProperty(dispositionAction, PROP_DISPOSITION_EVENTS_ELIGIBLE));
       
    }

    public void eventExecutionUpdate(ChildAssociationRef childAssocRef, boolean isNewNode)
    {
        NodeRef dispositionAction = childAssocRef.getParentRef();
        NodeRef eventExecution = childAssocRef.getChildRef();
        
        if (this.nodeService.exists(dispositionAction) == true &&
            this.nodeService.exists(eventExecution) == true)
        {        
            ChildAssociationRef assoc = this.nodeService.getPrimaryParent(dispositionAction);
            if (assoc.getTypeQName().equals(ASSOC_NEXT_DISPOSITION_ACTION) == true)
            {
                // Get the record (or record folder)
                NodeRef record = assoc.getParentRef();

                // Apply the search aspect
                applySearchAspect(record);
                
                Collection<String> events = (List<String>)this.nodeService.getProperty(record, PROP_RS_DISPOSITION_EVENTS);
                if (events == null)
                {
                    events = new ArrayList<String>(1);
                }
                events.add((String)this.nodeService.getProperty(eventExecution, PROP_EVENT_EXECUTION_NAME));
                this.nodeService.setProperty(record, PROP_RS_DISPOSITION_EVENTS, (Serializable)events);
            }
        }
    }
    
    public void rmSearchAspectAdd(NodeRef nodeRef, QName aspectTypeQName)
    {
        if (nodeService.exists(nodeRef) == true)
        {
            // Initialise the search parameteres as required
            setVitalRecordDefintionDetails(nodeRef);
        }        
    }

    public void vitalRecordDefintionAddAspect(NodeRef nodeRef, QName aspectTypeQName)
    {
        // Only care about record folders
        if (recordsManagementService.isRecordFolder(nodeRef) == true)
        {
            updateVitalRecordDefinitionValues(nodeRef);         
        }
    }
    
    public void vitalRecordDefintionUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after)
    {
        // Only care about record folders
        if (recordsManagementService.isRecordFolder(nodeRef) == true)
        {
            Set<QName> props = new HashSet<QName>(1);
            props.add(PROP_REVIEW_PERIOD);
            Set<QName> changed = determineChangedProps(before, after);
            changed.retainAll(props);
            if (changed.isEmpty() == false)
            {
                updateVitalRecordDefinitionValues(nodeRef);
            }
            
        }
    }
    
    private void updateVitalRecordDefinitionValues(NodeRef nodeRef)
    {
        List<NodeRef> records = recordsManagementService.getRecords(nodeRef);
        for (NodeRef record : records)
        {
            // Apply the search aspect
            applySearchAspect(record);
            
            // Set the vital record definition details
            setVitalRecordDefintionDetails(record);
        }
    }
    
    private void setVitalRecordDefintionDetails(NodeRef nodeRef)
    {
        VitalRecordDefinition vrd = recordsManagementService.getVitalRecordDefinition(nodeRef);
        
        if (vrd != null)
        {
            // Set the property values
            nodeService.setProperty(nodeRef, PROP_RS_VITAL_RECORD_REVIEW_PERIOD, vrd.getReviewPeriod().getPeriodType());
            nodeService.setProperty(nodeRef, PROP_RS_VITAL_RECORD_REVIEW_PERIOD_EXPRESSION, vrd.getReviewPeriod().getExpression());
        }
    }
    
    /**
     * This method compares the oldProps map against the newProps map and returns
     * a set of QNames of the properties that have changed. Changed here means one of
     * <ul>
     * <li>the property has been removed</li>
     * <li>the property has had its value changed</li>
     * <li>the property has been added</li>
     * </ul>
     */
    private Set<QName> determineChangedProps(Map<QName, Serializable> oldProps, Map<QName, Serializable> newProps)
    {
        Set<QName> result = new HashSet<QName>();
        for (QName qn : oldProps.keySet())
        {
            if (newProps.get(qn) == null ||
                newProps.get(qn).equals(oldProps.get(qn)) == false)
            {
                result.add(qn);
            }
        }
        for (QName qn : newProps.keySet())
        {
            if (oldProps.get(qn) == null)
            {
                result.add(qn);
            }
        }
        
        return result;
    }
}
