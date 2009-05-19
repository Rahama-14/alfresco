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
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.org_alfresco_module_dod5015;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Records Management Service Implementation
 * 
 * @author Roy Wetherall
 */
public class RecordsManagementServiceImpl implements RecordsManagementService
{
    private static Log logger = LogFactory.getLog(RecordsManagementServiceImpl.class);

    private Map<String, RecordsManagementAction> rmActions;
    
    private ActionService actionService;
    private ServiceRegistry services;

    public void setActionService(ActionService actionService)
    {
        this.actionService = actionService;
    }
    
    public void setServiceRegistry(ServiceRegistry services)
    {
        this.services = services;
    }
    
    public void setRecordsManagementActions(List<RecordsManagementAction> rmActions)
    {
        // Clear the existing map of states
        this.rmActions = new HashMap<String, RecordsManagementAction>(rmActions.size());
        for (RecordsManagementAction recordState : rmActions)
        {
            this.rmActions.put(recordState.getName(), recordState);
        }
    }    
    
    public void executeRecordAction(NodeRef filePlanComponent, String name, Map<String, Serializable> parameters)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Executing record management action on " + filePlanComponent);
            logger.debug("    actionName = " + name);
            logger.debug("    parameters = " + parameters);
        }
        
        //TODO Fix this up after the demo.
        // Replace the noderef String in parameters with a ScriptNodeRef
        Serializable existingString = parameters.get("recordFolder");
        ScriptNode scrNode = new ScriptNode(new NodeRef(existingString.toString()), this.services);
        parameters.put("recordFolder", scrNode);
        logger.debug("    new parameters = " + parameters);
        
        // Get the state
        RecordsManagementAction rmAction = rmActions.get(name);
        if (rmAction == null)
        {
            throw new AlfrescoRuntimeException("The record management action '" + name + "' has not been defined");
        }
        
        // Create the action
        Action action = this.actionService.createAction(rmAction.getActionName());
        action.setParameterValues(parameters);
        
        // Execute the action
        this.actionService.executeAction(action, filePlanComponent);        
    }

    public List<String> getRecordActions()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
