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
package org.alfresco.module.org_alfresco_module_dod5015.action;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.module.org_alfresco_module_dod5015.DispositionSchedule;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;

/**
 * @author Roy Wetherall
 */
public abstract class RMDispositionActionExecuterAbstractBase extends RMActionExecuterAbstractBase
{
    /** Indicates whether the eligibility of the record should be checked or not */
    // TODO add the capability to override this value using a property on the action
    protected boolean checkEligibility = true;

    /**
     * All children of this implementation are disposition actions.
     * 
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RMActionExecuterAbstractBase#isDispositionAction()
     */
    @Override
    public boolean isDispositionAction()
    {
        return true;
    }

    /**
     * Indicates whether the disposition is marked complete
     * 
     * @return boolean true if marked complete, false otherwise
     */
    public boolean getSetDispositionActionComplete()
    {
        return true;
    }

    /**
     * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.action.Action,
     *      org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
    {
    	//
        NodeRef nextDispositionActionNodeRef = getNextDispostionAction(actionedUponNodeRef);
        
        // Check the validity of the action (is it the next action, are we dealing with the correct type of object for
        // the disposition level?
        DispositionSchedule di = checkDispositionActionExecutionValidity(actionedUponNodeRef, nextDispositionActionNodeRef, true);

        // Check the eligibility of the action
        if (checkEligibility == false || this.recordsManagementService.isNextDispositionActionEligible(actionedUponNodeRef) == true)
        {
            if (di.isRecordLevelDisposition() == true)
            {
                // Check that we do indeed have a record
                if (this.recordsManagementService.isRecord(actionedUponNodeRef) == true)
                {
                    // Can only execute disposition action on record if declared
                    if (this.recordsManagementService.isRecordDeclared(actionedUponNodeRef) == true)
                    {
                        // Indicate that the disposition action is underway
                        this.nodeService.setProperty(nextDispositionActionNodeRef, PROP_DISPOSITION_ACTION_STARTED_AT, new Date());
                        this.nodeService.setProperty(nextDispositionActionNodeRef, PROP_DISPOSITION_ACTION_STARTED_BY, AuthenticationUtil.getRunAsUser());

                        // Execute record level disposition
                        executeRecordLevelDisposition(action, actionedUponNodeRef);

                        if (this.nodeService.exists(nextDispositionActionNodeRef) == true &&
                            getSetDispositionActionComplete() == true)
                        {
                            this.nodeService.setProperty(nextDispositionActionNodeRef, PROP_DISPOSITION_ACTION_COMPLETED_AT, new Date());
                            this.nodeService.setProperty(nextDispositionActionNodeRef, PROP_DISPOSITION_ACTION_COMPLETED_BY, AuthenticationUtil.getRunAsUser());
                        }
                    }
                    else
                    {
                        throw new AlfrescoRuntimeException("Unable to execute disposition action"
                                + getName() + ", because the record is not declared. (" + actionedUponNodeRef.toString() + ")");
                    }
                }
                else
                {
                    throw new AlfrescoRuntimeException("Unable to execute disposition action"
                            + getName() + ", because disposition is expected at the record level and this node is not a record. (" + actionedUponNodeRef.toString() + ")");
                }
            }
            else
            {
                if (this.recordsManagementService.isRecordFolder(actionedUponNodeRef) == true)
                {
                    if (this.recordsManagementService.isRecordFolderDeclared(actionedUponNodeRef) == true)
                    {
                        // Indicate that the disposition action is underway
                        this.nodeService.setProperty(nextDispositionActionNodeRef, PROP_DISPOSITION_ACTION_STARTED_AT, new Date());
                        this.nodeService.setProperty(nextDispositionActionNodeRef, PROP_DISPOSITION_ACTION_STARTED_BY, AuthenticationUtil.getRunAsUser());

                        executeRecordFolderLevelDisposition(action, actionedUponNodeRef);
                        
                        // Indicate that the disposition action is compelte
                        if (this.nodeService.exists(nextDispositionActionNodeRef) == true &&
                            getSetDispositionActionComplete() == true)
                        {
                            this.nodeService.setProperty(nextDispositionActionNodeRef, PROP_DISPOSITION_ACTION_COMPLETED_AT, new Date());
                            this.nodeService.setProperty(nextDispositionActionNodeRef, PROP_DISPOSITION_ACTION_COMPLETED_BY, AuthenticationUtil.getRunAsUser());
                        }

                    }
                    else
                    {
                        throw new AlfrescoRuntimeException("Unable to execute disposition action"
                                + getName() + ", because not all the records in the record are declared. (" + actionedUponNodeRef.toString() + ")");
                    }
                }
                else
                {
                    throw new AlfrescoRuntimeException("Unable to execute disposition action"
                            + getName() + ", because disposition is expected at the record folder level and this node is not a record folder. (" + actionedUponNodeRef.toString()
                            + ")");
                }

            }

            if (this.nodeService.exists(actionedUponNodeRef) == true && getSetDispositionActionComplete() == true)
            {
                // Update the disposition schedule
                updateNextDispositionAction(actionedUponNodeRef);
            }
        }
        else
        {
            throw new AlfrescoRuntimeException("Unable to execute disposition action "
                    + getName() + ", because the next disposition action on the record or record folder is not eligiable. (" + actionedUponNodeRef.toString() + ")");
        }
    }

    /**
     * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
        // TODO add the "checkEligibility" parameter
    }

    /**
     * @param action
     * @param record
     */
    protected abstract void executeRecordLevelDisposition(Action action, NodeRef record);

    /**
     * @param action
     * @param recordFolder
     */
    protected abstract void executeRecordFolderLevelDisposition(Action action, NodeRef recordFolder);

    /**
     * @param nodeRef
     * @return
     */
    protected DispositionSchedule checkDispositionActionExecutionValidity(NodeRef nodeRef, NodeRef nextDispositionActionNodeRef, boolean throwError)
    {
        // Check the node has associated disposition instructions
        DispositionSchedule di = this.recordsManagementService.getDispositionSchedule(nodeRef);
        if (di == null)
        {
            if (throwError)
            {
                throw new AlfrescoRuntimeException("Unable to find disposition instructions for node.  Can not execute disposition action "
                        + getName() + ". (" + nodeRef.toString() + ")");
            }
            else
            {
                return null;
            }
        }

        // Check the node has the disposition schedule aspect applied
        if (this.nodeService.hasAspect(nodeRef, ASPECT_DISPOSITION_LIFECYCLE) == false)
        {
            if (throwError)
            {
                throw new AlfrescoRuntimeException("Unable to execute disposition action"
                        + getName() + ", because node does not have a disposition lifecycle set. (" + nodeRef.toString() + ")");
            }
            else
            {
                return null;
            }
        }

        // Check this the next disposition action
        
        NodeRef nextDispositionAction = nextDispositionActionNodeRef;
        if (nextDispositionAction == null)
        {
            if (throwError)
            {
                throw new AlfrescoRuntimeException("Unable to execute disposition action"
                        + getName() + ", because the next disposition action is not set. (" + nodeRef.toString() + ")");
            }
            else
            {
                return null;
            }
        }
        String actionName = (String) this.nodeService.getProperty(nextDispositionAction, PROP_DISPOSITION_ACTION);
        if (actionName == null || actionName.equals(getName()) == false)
        {
            if (throwError)
            {
                throw new AlfrescoRuntimeException("Unable to execute disposition action"
                        + getName() + ", because this is not the next disposition action for this record or record folder. (" + nodeRef.toString() + ")");
            }
            else
            {
                return null;
            }
        }

        return di;
    }

    /**
     * Get the next disposition action node. Null if none present.
     * 
     * @param nodeRef
     *            the disposable node reference
     * @return NodeRef the next disposition action, null if none
     */
    private NodeRef getNextDispostionAction(NodeRef nodeRef)
    {
        NodeRef result = null;
        List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(nodeRef, ASSOC_NEXT_DISPOSITION_ACTION, RegexQNamePattern.MATCH_ALL);
        if (assocs.size() != 0)
        {
            result = assocs.get(0).getChildRef();
        }
        return result;
    }

    @Override
    public Set<QName> getProtectedProperties()
    {
        HashSet<QName> qnames = new HashSet<QName>();
        qnames.add(PROP_DISPOSITION_ACTION_STARTED_AT);
        qnames.add(PROP_DISPOSITION_ACTION_STARTED_BY);
        qnames.add(PROP_DISPOSITION_ACTION_COMPLETED_AT);
        qnames.add(PROP_DISPOSITION_ACTION_COMPLETED_BY);
        return qnames;
    }

    @Override
    protected boolean isExecutableImpl(NodeRef filePlanComponent, Map<String, Serializable> parameters, boolean throwException)
    {
        // Check the validity of the action (is it the next action, are we dealing with the correct type of object for
        // the disposition level?
    	//
        NodeRef nextDispositionActionNodeRef = getNextDispostionAction(filePlanComponent);
        
        DispositionSchedule di = checkDispositionActionExecutionValidity(filePlanComponent, nextDispositionActionNodeRef, throwException);

        // Check the eligibility of the action
        if (checkEligibility == false || this.recordsManagementService.isNextDispositionActionEligible(filePlanComponent) == true)
        {
            if (di.isRecordLevelDisposition() == true)
            {
                // Check that we do indeed have a record
                if (this.recordsManagementService.isRecord(filePlanComponent) == true)
                {
                    // Can only execute disposition action on record if declared
                    if (this.recordsManagementService.isRecordDeclared(filePlanComponent) == true)
                    {
                        return true;
                    }
                    else
                    {
                        if (throwException)
                        {
                            throw new AlfrescoRuntimeException("Unable to execute disposition action"
                                    + getName() + ", because the record is not declared. (" + filePlanComponent.toString() + ")");
                        }
                        else
                        {
                            return false;
                        }
                    }
                }
                else
                {
                    if (throwException)
                    {
                        throw new AlfrescoRuntimeException("Unable to execute disposition action"
                                + getName() + ", because disposition is expected at the record level and this node is not a record. (" + filePlanComponent.toString() + ")");
                    }
                    else
                    {
                        return false;
                    }
                }
            }
            else
            {
                if (this.recordsManagementService.isRecordFolder(filePlanComponent) == true)
                {
                    if (this.recordsManagementService.isRecordFolderDeclared(filePlanComponent) == true)
                    {
                        return true;
                    }
                    else
                    {
                        if (throwException)
                        {
                            throw new AlfrescoRuntimeException("Unable to execute disposition action"
                                    + getName() + ", because not all the records in the record are declared. (" + filePlanComponent.toString() + ")");
                        }
                        else
                        {
                            return false;
                        }
                    }
                }
                else
                {
                    if (throwException)
                    {
                        throw new AlfrescoRuntimeException("Unable to execute disposition action"
                                + getName() + ", because disposition is expected at the record folder level and this node is not a record folder. (" + filePlanComponent.toString()
                                + ")");
                    }
                    else
                    {
                        return false;
                    }
                }

            }

        }
        else
        {
            if (throwException)
            {
                throw new AlfrescoRuntimeException("Unable to execute disposition action "
                        + getName() + ", because the next disposition action on the record or record folder is not eligiable. (" + filePlanComponent.toString() + ")");
            }
            else
            {
                return false;
            }
        }
    }
 

}
