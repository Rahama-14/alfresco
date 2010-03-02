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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.RegexQNamePattern;

/**
 * Disposition instructions implementation
 * 
 * @author Roy Wetherall
 */
public class DispositionScheduleImpl implements DispositionSchedule,
                                                RecordsManagementModel
{
    private NodeService nodeService;
    private RecordsManagementServiceRegistry services;
    private NodeRef dispositionDefinitionNodeRef;
    
    private List<DispositionActionDefinition> actions;
    private Map<String, DispositionActionDefinition> actionsById;
    
    public DispositionScheduleImpl(RecordsManagementServiceRegistry services, NodeService nodeService,  NodeRef nodeRef)
    {
        // TODO check that we have a disposition definition node reference
        
        this.dispositionDefinitionNodeRef = nodeRef;
        this.nodeService = nodeService;
        this.services = services;
    }

    /*
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionSchedule#getNodeRef()
     */
    public NodeRef getNodeRef()
    {
        return this.dispositionDefinitionNodeRef;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionSchedule#getDispositionAuthority()
     */
    public String getDispositionAuthority()
    {
        return (String)this.nodeService.getProperty(this.dispositionDefinitionNodeRef, PROP_DISPOSITION_AUTHORITY);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionSchedule#getDispositionInstructions()
     */
    public String getDispositionInstructions()
    {
        return (String)this.nodeService.getProperty(this.dispositionDefinitionNodeRef, PROP_DISPOSITION_INSTRUCTIONS);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.DispositionSchedule#isRecordLevelDisposition()
     */
    public boolean isRecordLevelDisposition()
    {
        boolean result = false;
        Boolean value = (Boolean)this.nodeService.getProperty(this.dispositionDefinitionNodeRef, PROP_RECORD_LEVEL_DISPOSITION);
        if (value != null)
        {
            result = value.booleanValue();
        }            
        return result;
    }

    public DispositionActionDefinition getDispositionActionDefinition(String id)
    {
        if (this.actions == null)
        {
            getDispositionActionsImpl();
        }
        
        return this.actionsById.get(id);
    }

    public List<DispositionActionDefinition> getDispositionActionDefinitions()
    {
        if (this.actions == null)
        {
            getDispositionActionsImpl();
        }
        
        return this.actions;
    }
    
    private void getDispositionActionsImpl()
    {
        List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(
                                                      this.dispositionDefinitionNodeRef, 
                                                      ASSOC_DISPOSITION_ACTION_DEFINITIONS, 
                                                      RegexQNamePattern.MATCH_ALL);
        this.actions = new ArrayList<DispositionActionDefinition>(assocs.size());
        this.actionsById = new HashMap<String, DispositionActionDefinition>(assocs.size()); 
        int index = 0;
        for (ChildAssociationRef assoc : assocs)
        {            
            DispositionActionDefinition da = new DispositionActionDefinitionImpl(services, assoc.getChildRef(), index); 
            actions.add(da);
            actionsById.put(da.getId(), da);
            index++;
        }
    }
    
    
    
    
}
