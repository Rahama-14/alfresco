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

import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService;
import org.alfresco.module.org_alfresco_module_dod5015.audit.RecordsManagementAuditService;
import org.alfresco.module.org_alfresco_module_dod5015.event.RecordsManagementEventService;
import org.alfresco.module.org_alfresco_module_dod5015.notification.RecordsManagementNotificationService;
import org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService;
import org.alfresco.repo.service.ServiceDescriptorRegistry;

/**
 * Records management service registry implementation
 * 
 * @author Roy Wetherall
 */
public class RecordsManagementServiceRegistryImpl extends ServiceDescriptorRegistry 
                                                  implements RecordsManagementServiceRegistry
{
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementServiceRegistry#getRecordsManagementActionService()
     */
    public RecordsManagementActionService getRecordsManagementActionService()
    {
        return (RecordsManagementActionService)getService(RECORDS_MANAGEMENT_ACTION_SERVICE);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementServiceRegistry#getRecordsManagementAdminService()
     */
    public RecordsManagementAdminService getRecordsManagementAdminService()
    {
        return (RecordsManagementAdminService)getService(RECORDS_MANAGEMENT_ADMIN_SERVICE);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementServiceRegistry#getRecordsManagementEventService()
     */
    public RecordsManagementEventService getRecordsManagementEventService()
    {
        return (RecordsManagementEventService)getService(RECORDS_MANAGEMENT_EVENT_SERVICE);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementServiceRegistry#getRecordsManagementService()
     */
    public RecordsManagementService getRecordsManagementService()
    {
        return (RecordsManagementService)getService(RECORDS_MANAGEMENT_SERVICE);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementServiceRegistry#getRecordsManagementSecurityService()
     */
    public RecordsManagementSecurityService getRecordsManagementSecurityService()
    {
        return (RecordsManagementSecurityService)getService(RECORDS_MANAGEMENT_SECURITY_SERVICE);
    }

    /*
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementServiceRegistry#getRecordsManagementAuditService()
     */
    public RecordsManagementAuditService getRecordsManagementAuditService()
    {
        return (RecordsManagementAuditService)getService(RECORDS_MANAGEMENT_AUDIT_SERVICE);
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementServiceRegistry#getRecordsManagementNotificationService()
     */
    public RecordsManagementNotificationService getRecordsManagementNotificationService()
    {
        return (RecordsManagementNotificationService)getService(RECORDS_MANAGEMENT_NOTIFICATION_SERVICE);
    }
}
