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
package org.alfresco.module.org_alfresco_module_dod5015;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Period;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Search Behaviour class.
 * 
 * Manages the collapse of data onto the supporting aspect on the record/record folder
 * 
 * @author Roy Wetherall
 */
public class RecordsManagementSearchBehaviour implements RecordsManagementModel
{
    private static Log logger = LogFactory.getLog(RecordsManagementSearchBehaviour.class);
    
    /** Search specific elements of the RM model */
    public static final QName ASPECT_RM_SEARCH = QName.createQName(RM_URI, "recordSearch");
    public static final QName PROP_RS_DISPOSITION_ACTION_NAME = QName.createQName(RM_URI, "recordSearchDispositionActionName");
    public static final QName PROP_RS_DISPOSITION_ACTION_AS_OF = QName.createQName(RM_URI, "recordSearchDispositionActionAsOf");
    public static final QName PROP_RS_DISPOSITION_EVENTS_ELIGIBLE = QName.createQName(RM_URI, "recordSearchDispositionEventsEligible");
    public static final QName PROP_RS_DISPOSITION_EVENTS = QName.createQName(RM_URI, "recordSearchDispositionEvents");
    public static final QName PROP_RS_VITAL_RECORD_REVIEW_PERIOD = QName.createQName(RM_URI, "recordSearchVitalRecordReviewPeriod");
    public static final QName PROP_RS_VITAL_RECORD_REVIEW_PERIOD_EXPRESSION = QName.createQName(RM_URI, "recordSearchVitalRecordReviewPeriodExpression");
    public static final QName PROP_RS_DISPOSITION_PERIOD = QName.createQName(RM_URI, "recordSearchDispositionPeriod");
    public static final QName PROP_RS_DISPOSITION_PERIOD_EXPRESSION = QName.createQName(RM_URI, "recordSearchDispositionPeriodExpression");
    public static final QName PROP_RS_HAS_DISPOITION_SCHEDULE = QName.createQName(RM_URI, "recordSearchHasDispositionSchedule");
    public static final QName PROP_RS_DISPOITION_INSTRUCTIONS = QName.createQName(RM_URI, "recordSearchDispositionInstructions");
    public static final QName PROP_RS_DISPOITION_AUTHORITY = QName.createQName(RM_URI, "recordSearchDispositionAuthority");
    public static final QName PROP_RS_HOLD_REASON = QName.createQName(RM_URI, "recordSearchHoldReason");
    
    /** Policy component */
    private PolicyComponent policyComponent;
    
    /** Node service */
    private NodeService nodeService;
    
    /**  Records management service */
    private RecordsManagementService recordsManagementService;
    
    /** Records management service registry */
    private RecordsManagementServiceRegistry recordsManagementServiceRegistry;

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
    
    /**
     * @param recordsManagementService  the records management service
     */
    public void setRecordsManagementService(RecordsManagementService recordsManagementService)
    {
        this.recordsManagementService = recordsManagementService;
    }
    
    /**
     * @param recordsManagementServiceRegistry  the records management service registry
     */
    public void setRecordsManagementServiceRegistry(RecordsManagementServiceRegistry recordsManagementServiceRegistry)
    {
        this.recordsManagementServiceRegistry = recordsManagementServiceRegistry;
    }
    
    /** Java behaviour */
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

        this.policyComponent.bindClassBehaviour(
                    QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateProperties"), 
                    TYPE_DISPOSITION_SCHEDULE, 
                    new JavaBehaviour(this, "dispositionSchedulePropertiesUpdate", NotificationFrequency.TRANSACTION_COMMIT));
        
        this.policyComponent.bindAssociationBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateChildAssociation"), 
                TYPE_DISPOSITION_ACTION, 
                ASSOC_EVENT_EXECUTIONS,
                new JavaBehaviour(this, "eventExecutionUpdate", NotificationFrequency.TRANSACTION_COMMIT));

        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onDeleteNode"), 
                TYPE_EVENT_EXECUTION, 
                new JavaBehaviour(this, "eventExecutionDelete", NotificationFrequency.TRANSACTION_COMMIT));
        
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onAddAspect"), 
                ASPECT_RM_SEARCH, 
                onAddSearchAspect);  
        
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onAddAspect"), 
                ASPECT_RECORD, 
                new JavaBehaviour(this, "onAddRecordAspect", NotificationFrequency.TRANSACTION_COMMIT));  
        
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"), 
                TYPE_RECORD_FOLDER, 
                new JavaBehaviour(this, "recordFolderCreate", NotificationFrequency.TRANSACTION_COMMIT));
        
        // Vital Records Review Details Rollup
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onAddAspect"), 
                ASPECT_VITAL_RECORD_DEFINITION, 
                new JavaBehaviour(this, "vitalRecordDefintionAddAspect", NotificationFrequency.TRANSACTION_COMMIT));
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateProperties"), 
                ASPECT_VITAL_RECORD_DEFINITION, 
                new JavaBehaviour(this, "vitalRecordDefintionUpdateProperties", NotificationFrequency.TRANSACTION_COMMIT));
        
        // Hold reason rollup
        this.policyComponent.bindClassBehaviour(
                    QName.createQName(NamespaceService.ALFRESCO_URI, "onRemoveAspect"), 
                    ASPECT_FROZEN, 
                    new JavaBehaviour(this, "onRemoveFrozenAspect", NotificationFrequency.TRANSACTION_COMMIT));
        
        this.policyComponent.bindClassBehaviour(
                    QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateProperties"), 
                    TYPE_HOLD, 
                    new JavaBehaviour(this, "frozenAspectUpdateProperties", NotificationFrequency.TRANSACTION_COMMIT));
    }

    /**
     * Ensures the search aspect for the given node is present, complete and correct.
     * 
     * @param recordOrFolder
     */
    public void fixupSearchAspect(NodeRef recordOrFolder)
    {
        // for now only deal with record folders
        if (recordsManagementService.isRecordFolder(recordOrFolder))
        {
            // ensure the search aspect is applied
            applySearchAspect(recordOrFolder);
            
            // setup the properties relating to the disposition schedule
            setupDispositionScheduleProperties(recordOrFolder);
            
            // setup the properties relating to the disposition lifecycle
            DispositionAction da = recordsManagementService.getNextDispositionAction(recordOrFolder);
            if (da != null)
            {
                updateDispositionActionProperties(recordOrFolder, da.getNodeRef());
                setupDispositionActionEvents(recordOrFolder, da);
            }
            
            // setup the properties relating to the vital record indicator
            setVitalRecordDefintionDetails(recordOrFolder);
        }
    }
    
    /**
     * Updates the disposition action properties
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
                
                if (logger.isDebugEnabled())
                    logger.debug("Added search aspect to node: " + nodeRef);
            }
        }
        finally
        {
            onAddSearchAspect.enable();
        }
    }
    
    public void onAddRecordAspect(NodeRef nodeRef, QName aspectTypeQName)
    {
        if (nodeService.exists(nodeRef) == true)
        {
            applySearchAspect(nodeRef);
            setupDispositionScheduleProperties(nodeRef);
        }
    }
    
    public void recordFolderCreate(ChildAssociationRef childAssocRef)
    {
        NodeRef nodeRef = childAssocRef.getChildRef();
        if (nodeService.exists(nodeRef) == true)
        {
            applySearchAspect(nodeRef);
            setupDispositionScheduleProperties(nodeRef);
        }
    }
    
    private void setupDispositionScheduleProperties(NodeRef recordOrFolder)
    {
        DispositionSchedule ds = recordsManagementService.getDispositionSchedule(recordOrFolder);
        if (ds == null)
        {
            nodeService.setProperty(recordOrFolder, PROP_RS_HAS_DISPOITION_SCHEDULE, false);
        }
        else
        {
            nodeService.setProperty(recordOrFolder, PROP_RS_HAS_DISPOITION_SCHEDULE, true);
            setDispositionScheduleProperties(recordOrFolder, ds);
        }
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Set rma:recordSearchHasDispositionSchedule for node " + recordOrFolder + 
                        " to: " + (ds != null));
        }
    }
    
    public void dispositionActionCreate(ChildAssociationRef childAssocRef)
    {
        NodeRef child = childAssocRef.getChildRef();
        if (nodeService.exists(child) == true &&
            childAssocRef.getTypeQName().equals(ASSOC_NEXT_DISPOSITION_ACTION) == true)
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
        Map<QName, Serializable> props = nodeService.getProperties(record);
        
        DispositionAction da = new DispositionActionImpl(recordsManagementServiceRegistry, dispositionAction);
        
        props.put(PROP_RS_DISPOSITION_ACTION_NAME, da.getName()); 
        props.put(PROP_RS_DISPOSITION_ACTION_AS_OF, da.getAsOfDate()); 
        props.put(PROP_RS_DISPOSITION_EVENTS_ELIGIBLE, this.nodeService.getProperty(dispositionAction, PROP_DISPOSITION_EVENTS_ELIGIBLE));
        
        DispositionActionDefinition daDefinition = da.getDispositionActionDefinition();        
        Period period = daDefinition.getPeriod();
        if (period != null)
        {
            props.put(PROP_RS_DISPOSITION_PERIOD, period.getPeriodType());
            props.put(PROP_RS_DISPOSITION_PERIOD_EXPRESSION, period.getExpression());            
        }
        else
        {
            props.put(PROP_RS_DISPOSITION_PERIOD, null);
            props.put(PROP_RS_DISPOSITION_PERIOD_EXPRESSION, null);
        }
        
        nodeService.setProperties(record, props);
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Set rma:recordSearchDispositionActionName for node " + record + " to: " + 
                        props.get(PROP_RS_DISPOSITION_ACTION_NAME));
            logger.debug("Set rma:recordSearchDispositionActionAsOf for node " + record + " to: " +
                        props.get(PROP_RS_DISPOSITION_ACTION_AS_OF));
            logger.debug("Set rma:recordSearchDispositionEventsEligible for node " + record + " to: " + 
                        props.get(PROP_RS_DISPOSITION_EVENTS_ELIGIBLE));
            logger.debug("Set rma:recordSearchDispositionPeriod for node " + record + " to: " +
                        props.get(PROP_RS_DISPOSITION_PERIOD));
            logger.debug("Set rma:recordSearchDispositionPeriodExpression for node " + record + " to: " +
                        props.get(PROP_RS_DISPOSITION_PERIOD_EXPRESSION));
        }
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
    
    public void eventExecutionDelete(ChildAssociationRef childAssocRef, boolean isNodeArchived)
    {
        NodeRef dispositionActionNode = childAssocRef.getParentRef();
        
        if (this.nodeService.exists(dispositionActionNode))
        {
            ChildAssociationRef assoc = this.nodeService.getPrimaryParent(dispositionActionNode);
            if (assoc.getTypeQName().equals(ASSOC_NEXT_DISPOSITION_ACTION) == true)
            {
                // Get the record (or record folder)
                NodeRef record = assoc.getParentRef();

                // Apply the search aspect
                applySearchAspect(record);
                
                // make sure the list of events match the action definition
                setupDispositionActionEvents(record, recordsManagementService.getNextDispositionAction(record));
            }
        }
    }
    
    private void setupDispositionActionEvents(NodeRef nodeRef, DispositionAction da)
    {
        if (da != null)
        {
            List<String> eventNames = null;
            List<EventCompletionDetails> eventsList = da.getEventCompletionDetails();
            if (eventsList.size() > 0)
            {
                eventNames = new ArrayList<String>(eventsList.size());
                for (EventCompletionDetails event : eventsList)
                {
                    eventNames.add(event.getEventName());
                }
            }
            
            // set the property
            this.nodeService.setProperty(nodeRef, PROP_RS_DISPOSITION_EVENTS, (Serializable)eventNames);
            
            if (logger.isDebugEnabled())
            {
                logger.debug("Set rma:recordSearchDispositionEvents for node " + nodeRef + " to: " + eventNames);
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
        // ensure the folder itself reflects the correct details
        applySearchAspect(nodeRef);
        setVitalRecordDefintionDetails(nodeRef);
        
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
            
            if (logger.isDebugEnabled())
            {
                logger.debug("Set rma:recordSearchVitalRecordReviewPeriod for node " + nodeRef + " to: " +
                            vrd.getReviewPeriod().getPeriodType());
                logger.debug("Set rma:recordSearchVitalRecordReviewPeriodExpression for node " + nodeRef + " to: " +
                            vrd.getReviewPeriod().getExpression());
            }
        }
    }
    
    public void onRemoveFrozenAspect(NodeRef nodeRef, QName aspectTypeQName)
    {
        if (nodeService.exists(nodeRef) == true &&
            nodeService.hasAspect(nodeRef, ASPECT_RM_SEARCH))
        {
            nodeService.setProperty(nodeRef, PROP_RS_HOLD_REASON, null);
        }
    }
    
    public void frozenAspectUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after)
    {
        if (nodeService.exists(nodeRef))
        {
            // get the changed hold reason
            String holdReason = (String)nodeService.getProperty(nodeRef, PROP_HOLD_REASON);
            
            // get all the frozen items the hold node has and change the hold reason
            List<ChildAssociationRef> holdAssocs = this.nodeService.getChildAssocs(
                        nodeRef, ASSOC_FROZEN_RECORDS, RegexQNamePattern.MATCH_ALL);
            for (ChildAssociationRef assoc : holdAssocs)
            {
                NodeRef frozenItem = assoc.getChildRef();
                
                // ensure the search aspect is applied and set the hold reason
                applySearchAspect(frozenItem);
                nodeService.setProperty(frozenItem, PROP_RS_HOLD_REASON, holdReason);
            }
        }
    }
    
    /**
     * Updates the disposition schedule properties
     * 
     * @param nodeRef
     * @param before
     * @param after
     */
    public void dispositionSchedulePropertiesUpdate(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after)
    {
        if (this.nodeService.exists(nodeRef) == true)
        {
            // create the schedule object and get the record category for it
            DispositionSchedule schedule = new DispositionScheduleImpl(this.recordsManagementServiceRegistry, this.nodeService, nodeRef);
            NodeRef recordCategoryNode = this.nodeService.getPrimaryParent(schedule.getNodeRef()).getParentRef();
            
            if (schedule.isRecordLevelDisposition())
            {
                for (NodeRef recordFolder : this.getRecordFolders(recordCategoryNode))
                {
                    for (NodeRef record : this.recordsManagementService.getRecords(recordFolder))
                    {
                        applySearchAspect(record);
                        setDispositionScheduleProperties(record, schedule);
                    }
                }
            }
            else
            {
                for (NodeRef recordFolder : this.getRecordFolders(recordCategoryNode))
                {
                    applySearchAspect(recordFolder);
                    setDispositionScheduleProperties(recordFolder, schedule);
                }
            }
        }
    }
    
    private void setDispositionScheduleProperties(NodeRef recordOrFolder, DispositionSchedule schedule)
    {
        if (schedule != null)
        {
            this.nodeService.setProperty(recordOrFolder, PROP_RS_DISPOITION_AUTHORITY, schedule.getDispositionAuthority());
            this.nodeService.setProperty(recordOrFolder, PROP_RS_DISPOITION_INSTRUCTIONS, schedule.getDispositionInstructions());
            
            if (logger.isDebugEnabled())
            {
                logger.debug("Set rma:recordSearchDispositionAuthority for node " + recordOrFolder + " to: " + schedule.getDispositionAuthority());
                logger.debug("Set rma:recordSearchDispositionInstructions for node " + recordOrFolder + " to: " + schedule.getDispositionInstructions());
            }
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
    
    private List<NodeRef> getRecordFolders(NodeRef recordCategoryNode)
    {
        List<NodeRef> results = new ArrayList<NodeRef>(8);
        
        List<ChildAssociationRef> folderAssocs = nodeService.getChildAssocs(recordCategoryNode, 
                    ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
        for (ChildAssociationRef folderAssoc : folderAssocs)
        {
            NodeRef folder = folderAssoc.getChildRef();
            if (this.recordsManagementService.isRecordFolder(folder))
            {
                results.add(folder);
            }
        }
        
        return results;
    }
}
