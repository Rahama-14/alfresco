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
package org.alfresco.module.org_alfresco_module_dod5015.action.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.module.org_alfresco_module_dod5015.DispositionAction;
import org.alfresco.module.org_alfresco_module_dod5015.action.RMActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Edit review as of date action
 * 
 * @author Roy Wetherall
 */
public class EditDispositionActionAsOfDateAction extends RMActionExecuterAbstractBase
{
    @SuppressWarnings("unused")
    private static Log logger = LogFactory.getLog(EditDispositionActionAsOfDateAction.class);
    
    public static final String PARAM_AS_OF_DATE = "asOfDate";

    /**
     * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.action.Action,
     *      org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
    {
        if (this.nodeService.hasAspect(actionedUponNodeRef, ASPECT_DISPOSITION_LIFECYCLE) == true)
        {
            // Get the action parameter
            Date asOfDate = (Date)action.getParameterValue(PARAM_AS_OF_DATE);
            if (asOfDate == null)
            {
                throw new AlfrescoRuntimeException("Must specify a valid date when setting the disposition action as of date.");
            }
            
            // Set the dispostion action as of date
            DispositionAction da = recordsManagementService.getNextDispositionAction(actionedUponNodeRef);
            if (da != null)
            {                
                nodeService.setProperty(da.getNodeRef(), PROP_DISPOSITION_AS_OF, asOfDate);
            }
        }
    }

    /**
     * 
     * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
        // Intentionally empty
    }
    
    @Override
    public Set<QName> getProtectedProperties()
    {
        HashSet<QName> qnames = new HashSet<QName>();
        qnames.add(PROP_DISPOSITION_AS_OF);
        return qnames;
    }

    @Override
    protected boolean isExecutableImpl(NodeRef filePlanComponent, Map<String, Serializable> parameters, boolean throwException)
    {
        boolean result = false;
        if (this.nodeService.hasAspect(filePlanComponent, ASPECT_DISPOSITION_LIFECYCLE) == true)
        {
            result = true;
        }
        else
        {
            if (throwException == true)
            {
                throw new AlfrescoRuntimeException("Can only edit the disposition as of date of a record or record folder with a lifecycle applied.");
            }
        }
        return result;
    }
}
