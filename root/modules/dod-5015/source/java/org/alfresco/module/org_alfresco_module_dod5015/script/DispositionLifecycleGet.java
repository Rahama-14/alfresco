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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.module.org_alfresco_module_dod5015.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.DispositionAction;
import org.alfresco.module.org_alfresco_module_dod5015.EventCompletionDetails;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.web.scripts.Cache;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.scripts.WebScriptException;
import org.alfresco.web.scripts.WebScriptRequest;

/**
 * Implementation for Java backed webscript to return full details
 * about a disposition lifecycle (next disposition action).
 * 
 * @author Gavin Cornwell
 */
public class DispositionLifecycleGet extends DispositionAbstractBase
{
    PersonService personService;
    
    /**
     * Sets the PersonService instance
     * 
     * @param personService The PersonService instance
     */
    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }
    
    /*
     * @see org.alfresco.web.scripts.DeclarativeWebScript#executeImpl(org.alfresco.web.scripts.WebScriptRequest, org.alfresco.web.scripts.Status, org.alfresco.web.scripts.Cache)
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        // parse the request to retrieve the next action
        NodeRef nodeRef = parseRequestForNodeRef(req);
        
        // make sure the node passed in has a next action attached
        DispositionAction nextAction = this.rmService.getNextDispositionAction(nodeRef);
        if (nextAction == null)
        {
            throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Node " + 
                        nodeRef.toString() + " does not have a disposition lifecycle");
        }

        // add all the next action data to Map
        Map<String, Object> nextActionModel = new HashMap<String, Object>(8);
        nextActionModel.put("url", req.getURL());
        nextActionModel.put("name", nextAction.getName());
        nextActionModel.put("label", nextAction.getLabel());
        nextActionModel.put("eventsEligible", this.rmService.isNextDispositionActionEligible(nodeRef));
        
        if (nextAction.getAsOfDate() != null)
        {
            nextActionModel.put("asOf", ISO8601DateFormat.format(nextAction.getAsOfDate()));
        }
        
        if (nextAction.getStartedAt() != null)
        {
            nextActionModel.put("startedAt", ISO8601DateFormat.format(nextAction.getStartedAt()));
        }
        
        String startedBy = nextAction.getStartedBy();
        if (startedBy != null)
        {
            nextActionModel.put("startedBy", startedBy);
            addUsersRealName(nextActionModel, startedBy, "startedBy");
        }
        
        if (nextAction.getCompletedAt() != null)
        {
            nextActionModel.put("completedAt", ISO8601DateFormat.format(nextAction.getCompletedAt()));
        }
        
        String completedBy = nextAction.getCompletedBy();
        if (completedBy != null)
        {
            nextActionModel.put("completedBy", completedBy);
            addUsersRealName(nextActionModel, completedBy, "completedBy");
        }
        
        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
        for (EventCompletionDetails event : nextAction.getEventCompletionDetails())
        {
            events.add(createEventModel(event));
        }
        nextActionModel.put("events", events);
        
        // create model object with just the schedule data
        Map<String, Object> model = new HashMap<String, Object>(1);
        model.put("nextaction", nextActionModel);
        return model;
    }
    
    /**
     * Helper to create a model to represent the given event execution.
     * 
     * @param event The event to create a model for
     * @return Map representing the model
     */
    protected Map<String, Object> createEventModel(EventCompletionDetails event)
    {
        Map<String, Object> model = new HashMap<String, Object>(8);
        
        model.put("name", event.getEventName());
        model.put("label", event.getEventLabel());
        model.put("automatic", event.isEventExecutionAutomatic());
        model.put("complete", event.isEventComplete());
        
        String completedBy = event.getEventCompletedBy();
        if (completedBy != null)
        {
            model.put("completedBy", completedBy);
            addUsersRealName(model, completedBy, "completedBy");
        }
        
        if (event.getEventCompletedAt() != null)
        {
            model.put("completedAt", ISO8601DateFormat.format(event.getEventCompletedAt()));
        }
        
        return model;
    }
    
    /**
     * Adds the given username's first and last name to the given model.
     * 
     * @param model The model to add the first and last name to
     * @param userName The username of the user to lookup
     * @param propertyPrefix The prefix of the property name to use when adding to the model
     */
    protected void addUsersRealName(Map<String, Object> model, String userName, String propertyPrefix)
    {
        NodeRef user = this.personService.getPerson(userName);
        if (user != null)
        {
            String firstName = (String)this.nodeService.getProperty(user, ContentModel.PROP_FIRSTNAME);
            if (firstName != null)
            {
                model.put(propertyPrefix + "FirstName", firstName);
            }
            
            String lastName = (String)this.nodeService.getProperty(user, ContentModel.PROP_LASTNAME);
            if (lastName != null)
            {
                model.put(propertyPrefix + "LastName", lastName);
            }
        }
    }
}