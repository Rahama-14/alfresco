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
package org.alfresco.module.recordsManagement.ui;

import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.module.recordsManagement.RecordsManagementModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Repository;


/**
 * 
 * @author Roy Wetherall
 */
public abstract class BaseEvaluator extends BaseActionEvaluator
{
    protected ServiceRegistry getServiceRegistry()
    {
        return Repository.getServiceRegistry(FacesContext.getCurrentInstance());
    }
    
    protected boolean isRecordsManager()
    {
        boolean result = false;
        String currentUser = getServiceRegistry().getAuthenticationService().getCurrentUserName();
        Set<String> authorities = getServiceRegistry().getAuthorityService().getContainingAuthorities(null, currentUser, false);
        if (getServiceRegistry().getAuthorityService().hasAdminAuthority() == true || 
            authorities.contains(RecordsManagementModel.RM_GROUP) == true)
        {
            result = true;
        }        
        return result;
    }
}
