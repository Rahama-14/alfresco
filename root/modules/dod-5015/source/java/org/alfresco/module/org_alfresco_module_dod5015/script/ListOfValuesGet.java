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
import java.util.Set;

import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementAction;
import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService;
import org.alfresco.module.org_alfresco_module_dod5015.event.RecordsManagementEvent;
import org.alfresco.module.org_alfresco_module_dod5015.event.RecordsManagementEventService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.Period;
import org.alfresco.service.cmr.repository.PeriodProvider;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.scripts.Cache;
import org.alfresco.web.scripts.DeclarativeWebScript;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.scripts.WebScriptRequest;
import org.springframework.util.StringUtils;

/**
 * Implementation for Java backed webscript to return lists
 * of values for various records management services.
 * 
 * @author Gavin Cornwell
 */
public class ListOfValuesGet extends DeclarativeWebScript
{
    protected RecordsManagementActionService rmActionService;
    protected RecordsManagementEventService rmEventService;
    protected DictionaryService ddService;
    protected NamespaceService namespaceService;
    
    /**
     * Sets the RecordsManagementActionService instance
     * 
     * @param rmActionService The RecordsManagementActionService instance
     */
    public void setRecordsManagementActionService(RecordsManagementActionService rmActionService)
    {
        this.rmActionService = rmActionService;
    }
    
    /**
     * Sets the RecordsManagementEventService instance
     * 
     * @param rmEventService The RecordsManagementEventService instance
     */
    public void setRecordsManagementEventService(RecordsManagementEventService rmEventService)
    {
        this.rmEventService = rmEventService;
    }

    /**
     * Sets the DictionaryService instance
     * 
     * @param ddService The DictionaryService instance
     */
    public void setDictionaryService(DictionaryService ddService)
    {
        this.ddService = ddService;
    }
    
    /**
     * Sets the NamespaceService instance
     * 
     * @param namespaceService The NamespaceService instance
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }
    
    /*
     * @see org.alfresco.web.scripts.DeclarativeWebScript#executeImpl(org.alfresco.web.scripts.WebScriptRequest, org.alfresco.web.scripts.Status, org.alfresco.web.scripts.Cache)
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        // add all the lists data to a Map
        Map<String, Object> listsModel = new HashMap<String, Object>(4);
        String requestUrl = req.getURL();
        listsModel.put("dispositionActions", createDispositionActionsModel(requestUrl));
        listsModel.put("events", createEventsModel(requestUrl));
        listsModel.put("periodTypes", createPeriodTypesModel(requestUrl));
        listsModel.put("periodProperties", createPeriodPropertiesModel(requestUrl));
        
        // create model object with the lists model
        Map<String, Object> model = new HashMap<String, Object>(1);
        model.put("lists", listsModel);
        return model;
    }
    
    /**
     * Creates the model for the list of disposition actions.
     * 
     * @param baseUrl The base URL of the service
     * @return model of disposition actions list
     */
    protected Map<String, Object> createDispositionActionsModel(String baseUrl)
    {
        // iterate over the disposition actions
        List<RecordsManagementAction> dispositionActions = this.rmActionService.getDispositionActions();
        List<Map<String, String>> items = new ArrayList<Map<String, String>>(dispositionActions.size());
        for (RecordsManagementAction dispositionAction : dispositionActions)
        {
            Map<String, String> item = new HashMap<String, String>(2);
            item.put("label", dispositionAction.getLabel());
            item.put("value", dispositionAction.getName());
            items.add(item);
        }
        
        // create the model
        Map<String, Object> model = new HashMap<String, Object>(2);
        model.put("url", baseUrl + "/dispositionactions");
        model.put("items", items);
        
        return model;
    }
    
    /**
     * Creates the model for the list of events.
     * 
     * @param baseUrl The base URL of the service
     * @return model of events list
     */
    protected Map<String, Object> createEventsModel(String baseUrl)
    {
        // get all the events including their display labels from the event service
        List<RecordsManagementEvent> events = this.rmEventService.getEvents();
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>(events.size());
        for (RecordsManagementEvent event : events)
        {
            Map<String, Object> item = new HashMap<String, Object>(3);
            item.put("label", event.getDisplayLabel());
            item.put("value", event.getName());
            item.put("automatic", 
                        this.rmEventService.getEventType(event.getType()).isAutomaticEvent());
            items.add(item);
        }
        
        // create the model
        Map<String, Object> model = new HashMap<String, Object>(2);
        model.put("url", baseUrl + "/events");
        model.put("items", items);
        
        return model;
    }
    
    /**
     * Creates the model for the list of period types.
     * 
     * @param baseUrl The base URL of the service
     * @return model of period types list
     */
    protected Map<String, Object> createPeriodTypesModel(String baseUrl)
    {
        // iterate over all period provides, but ignore 'cron'
        Set<String> providers = Period.getProviderNames();
        List<Map<String, String>> items = new ArrayList<Map<String, String>>(providers.size());
        for (String provider : providers)
        {
            PeriodProvider pp = Period.getProvider(provider);
            if (!pp.getPeriodType().equals("cron"))
            {
                Map<String, String> item = new HashMap<String, String>(2);
                item.put("label", pp.getDisplayLabel());
                item.put("value", pp.getPeriodType());
                items.add(item);
            }
        }
        
        // create the model
        Map<String, Object> model = new HashMap<String, Object>(2);
        model.put("url", baseUrl + "/periodtypes");
        model.put("items", items);
        
        return model;
    }
    
    /**
     * Creates the model for the list of period properties.
     * 
     * @param baseUrl The base URL of the service
     * @return model of period properties list
     */
    protected Map<String, Object> createPeriodPropertiesModel(String baseUrl)
    {
        // TODO: make the list of period properties configurable
        List<QName> periodProperties = new ArrayList<QName>(3);
        periodProperties.add(RecordsManagementModel.PROP_CUT_OFF_DATE);
        periodProperties.add(RecordsManagementModel.PROP_DISPOSITION_AS_OF);
        periodProperties.add(RecordsManagementModel.PROP_DATE_FILED);
        
        // iterate over all period properties and get the label from their type definition
        List<Map<String, String>> items = new ArrayList<Map<String, String>>(periodProperties.size());
        for (QName periodProperty : periodProperties)
        {
            PropertyDefinition propDef = this.ddService.getProperty(periodProperty);
            
            if (propDef != null)
            {
                Map<String, String> item = new HashMap<String, String>(2);
                String propTitle = propDef.getTitle();
                if (propTitle == null || propTitle.length() == 0)
                {
                    propTitle = StringUtils.capitalize(periodProperty.getLocalName());
                }
                item.put("label", propTitle);
                item.put("value", periodProperty.toPrefixString(this.namespaceService));
                items.add(item);
            }
        }
        
        // create the model
        Map<String, Object> model = new HashMap<String, Object>(2);
        model.put("url", baseUrl + "/periodproperties");
        model.put("items", items);
        
        return model;
    }
}