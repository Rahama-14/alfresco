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
package org.alfresco.repo.tenant;

import java.io.File;
import java.util.List;

import org.apache.commons.logging.Log;


/**
 * Tenant Admin Service interface.
 * <p>
 * This interface provides administrative methods to provision and administer tenants.
 *
 */

public interface TenantAdminService extends TenantUserService
{
    public void startTenants();
    
    public void stopTenants();
    
    /*
     * Deployer methods
     */

    public void deployTenants(final TenantDeployer deployer, Log logger);
    
    public void undeployTenants(final TenantDeployer deployer, Log logger);

    public void register(TenantDeployer tenantDeployer);
    
    public void unregister(TenantDeployer tenantDeployer);
    
    public List<Tenant> getAllTenants();
    
    /*
     * Deployer methods
     */
    
    public void createTenant(String tenantDomain, char[] adminRawPassword);

    public void createTenant(String tenantDomain, char[] adminRawPassword, String rootContentStoreDir);
    
    public void exportTenant(String tenantDomain, File directoryDestination);
    
    public void importTenant(String tenantDomain, File directorySource, String rootContentStoreDir);
    
    public boolean existsTenant(String tenantDomain);
    
    public void deleteTenant(String tenantDomain);
    
    public void enableTenant(String tenantDomain);
    
    public void disableTenant(String tenantDomain);
    
    public Tenant getTenant(String tenantDomain);
    
    public boolean isEnabledTenant(String tenantDomain);
}
