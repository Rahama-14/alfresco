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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.module.org_alfresco_module_dod5015.event.RecordsManagementEventService;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.BeanNameAware;

/**
 * Records management action executer base class
 * 
 * @author Roy Wetherall
 */
public abstract class RMActionExecuterAbstractBase  extends ActionExecuterAbstractBase
                                                    implements RecordsManagementAction,
                                                               RecordsManagementModel,
                                                               BeanNameAware
{
    /** Action name */
    protected String name;
    
    /** Node service */
    protected NodeService nodeService;
    
    /** Dictionary service */
    protected DictionaryService dictionaryService;
    
    /** Content service */
    protected ContentService contentService;
    
    /** Namespace service */
    protected NamespaceService namespaceService;
    
    /** Action service */
    protected ActionService actionService;
    
    /** Records management action service */
    protected RecordsManagementActionService recordsManagementActionService;
    
    /** Records management service */
    protected RecordsManagementService recordsManagementService;
    
    /** Records management event service */
    protected RecordsManagementEventService recordsManagementEventService;
    
    /**
     * Set node service
     * 
     * @param nodeService   node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Set the dictionary service
     * 
     * @param dictionaryService
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }
    
    /**
     * Set the content service
     * 
     * @param contentService
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }
    
    /**
     * Set the namespace service
     * 
     * @param namespaceService
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }
    
    /**
     * Set action service 
     * 
     * @param actionService
     */
    public void setActionService(ActionService actionService)
    {
        this.actionService = actionService;
    }
    
    /**
     * Set records management service
     * 
     * @param recordsManagementActionService
     */
    public void setRecordsManagementActionService(RecordsManagementActionService recordsManagementActionService)
    {
        this.recordsManagementActionService = recordsManagementActionService;
    }
    
    /**
     * Set records management service
     * 
     * @param recordsManagementService      records management service
     */
    public void setRecordsManagementService(RecordsManagementService recordsManagementService)
    {
        this.recordsManagementService = recordsManagementService;
    }    
    
    /** 
     * Set records management event service
     * 
     * @param recordsManagementEventService records management event service
     */
    public void setRecordsManagementEventService(RecordsManagementEventService recordsManagementEventService)
    {
        this.recordsManagementEventService = recordsManagementEventService;
    }
    
    /**
     * Init method
     */
    @Override
    public void init()
    {
        super.init();
        
        // Do the RM action registration
        this.recordsManagementActionService.register(this);
    }
    
    /**
     * @see org.alfresco.repo.action.CommonResourceAbstractBase#setBeanName(java.lang.String)
     */
    @Override
    public void setBeanName(String name)
    {
        this.name = name;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementAction#getName()
     */
    public String getName()
    {
        return this.name;
    }
    
    /**
     * By default an action is not a disposition action
     * 
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementAction#isDispositionAction()
     */
    public boolean isDispositionAction()
    {
        return false;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementAction#execute(org.alfresco.service.cmr.repository.NodeRef, java.util.Map)
     */
    public void execute(NodeRef filePlanComponent, Map<String, Serializable> parameters)
    {
        isExecutableImpl(filePlanComponent, parameters, true);
        
        // Create the action
        Action action = this.actionService.createAction(name);
        action.setParameterValues(parameters);
        
        // Execute the action
        this.actionService.executeAction(action, filePlanComponent);          
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
    
    /**
     * By default there are no parameters.
     * 
     * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
        // No parameters
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementAction#getProtectedProperties()
     */
    public Set<QName> getProtectedProperties()
    {
       return Collections.<QName>emptySet();
    }
    

    /*
     * (non-Javadoc)
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementAction#getProtectedAspects()
     */
    public Set<QName> getProtectedAspects()
    {
        return Collections.<QName>emptySet();
    }

    /* (non-Javadoc)
     * @see org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementAction#isExecutable(org.alfresco.service.cmr.repository.NodeRef, java.util.Map)
     */
    public boolean isExecutable(NodeRef filePlanComponent, Map<String, Serializable> parameters)
    {
        return isExecutableImpl(filePlanComponent, parameters, false);
    }
    
    protected abstract boolean isExecutableImpl(NodeRef filePlanComponent, Map<String, Serializable> parameters, boolean throwException);
}
