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
package org.alfresco.repo.tenant;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.alfresco.repo.workflow.WorkflowDeployer;
import org.apache.commons.logging.Log;

/**
 * Empty Tenant Deployer Service implementation (for Single-Tenant / Single-Instance)
 */

public class SingleTAdminServiceImpl implements TenantAdminService
{
    /**
     * NO-OP
     */
    public void startTenants()
    {
    }

    /**
     * NO-OP
     */
    public void stopTenants()
    {
    }

    /**
     * @return          Returns <tt>false</tt> always
     */
    public boolean isEnabled()
    {
        return false;
    }

    /**
     * NO-OP
     */
    public void initialiseTenants()
    {
    }

    /**
     * NO-OP
     */
    public void deployTenants(final TenantDeployer deployer, Log logger)
    {
    }
    
    /**
     * NO-OP
     */
    public void undeployTenants(final TenantDeployer deployer, Log logger)
    {
    }
    
    /**
     * NO-OP
     */
    public void register(TenantDeployer tenantDeployer)
    {
    }
    
    /**
     * NO-OP
     */
    public void unregister(TenantDeployer tenantDeployer)
    {
    }

    /**
     * NO-OP
     */
    public void register(WorkflowDeployer workflowDeployer)
    {
    }

    /**
     * @return          Returns an empty list always
     */
    public List<Tenant> getAllTenants()
    {
        return Collections.emptyList();
    }
    
    /**
     * @return          Returns {@link TenantService#DEFAULT_DOMAIN} always
     */
    public String getCurrentUserDomain()
    {
        return TenantService.DEFAULT_DOMAIN;
    }
    
    /**
     * @return          Returns {@link TenantService#DEFAULT_DOMAIN} always
     */
    public String getUserDomain(String username)
    {
        return TenantService.DEFAULT_DOMAIN;
    }
    
    /**
     * @return          Returns the given <tt>username</tt> always
     */
    public String getBaseNameUser(String username)
    {
        return username;
    }
    
    /**
     * @return          Returns the given <tt>baseUserName</tt> always
     */
    public String getDomainUser(String baseUsername, String tenantDomain)
    {
        return baseUsername;
    }
    
    /**
     * @return          Returns {@link TenantService#DEFAULT_DOMAIN} always
     */
    public String getDomain(String name)
    {
        return TenantService.DEFAULT_DOMAIN;
    }

    /**
     * @throws UnsupportedOperationException        always
     */
    public void createTenant(String tenantDomain, char[] adminRawPassword, String rootContentStoreDir)
    {
        throw new UnsupportedOperationException("Single tenant mode is active.");
    }

    /**
     * @throws UnsupportedOperationException        always
     */
    public void createTenant(String tenantDomain, char[] adminRawPassword)
    {
        throw new UnsupportedOperationException("Single tenant mode is active.");
    }

    /**
     * @throws UnsupportedOperationException        always
     */
    public void deleteTenant(String tenantDomain)
    {
        throw new UnsupportedOperationException("Single tenant mode is active.");
    }

    /**
     * @throws UnsupportedOperationException        always
     */
    public void disableTenant(String tenantDomain)
    {
        throw new UnsupportedOperationException("Single tenant mode is active.");
    }

    /**
     * @throws UnsupportedOperationException        always
     */
    public void enableTenant(String tenantDomain)
    {
        throw new UnsupportedOperationException("Single tenant mode is active.");
    }

    /**
     * @throws UnsupportedOperationException        always
     */
    public boolean existsTenant(String tenantDomain)
    {
        throw new UnsupportedOperationException("Single tenant mode is active.");
    }

    /**
     * @throws UnsupportedOperationException        always
     */
    public void exportTenant(String tenantDomain, File directoryDestination)
    {
        throw new UnsupportedOperationException("Single tenant mode is active.");
    }

    /**
     * @throws UnsupportedOperationException        always
     */
    public Tenant getTenant(String tenantDomain)
    {
        throw new UnsupportedOperationException("Single tenant mode is active.");
    }

    /**
     * @throws UnsupportedOperationException        always
     */
    public void importTenant(String tenantDomain, File directorySource, String rootContentStoreDir)
    {
        throw new UnsupportedOperationException("Single tenant mode is active.");
    }

    /**
     * @throws UnsupportedOperationException        always
     */
    public boolean isEnabledTenant(String tenantDomain)
    {
        throw new UnsupportedOperationException("Single tenant mode is active.");
    }
}