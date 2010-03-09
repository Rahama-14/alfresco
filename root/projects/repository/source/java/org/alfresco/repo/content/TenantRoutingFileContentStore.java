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
package org.alfresco.repo.content;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.tenant.Tenant;
import org.alfresco.repo.tenant.TenantDeployer;
import org.alfresco.repo.tenant.TenantService;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Content Store that supports tenant routing, if multi-tenancy is enabled.
 * 
 * Note: Need to initialise before the dictionary service, in the case that models are dynamically loaded for the tenant.
 */
public class TenantRoutingFileContentStore extends AbstractRoutingContentStore implements TenantDeployer
{
    // cache of tenant file stores
    Map<String, FileContentStore> tenantFileStores = new ConcurrentHashMap<String, FileContentStore>();
    
    private String defaultRootDirectory;
    private TenantService tenantService;
    private ApplicationEventPublisher applicationEventPublisher;
    
    
    public void setDefaultRootDir(String defaultRootDirectory)
    {
        this.defaultRootDirectory = defaultRootDirectory;
    }
    
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }
        
    /**
     * Sets the application event publisher.
     * 
     * @param applicationEventPublisher
     *            the new application event publisher
     */
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher)
    {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    protected ContentStore selectWriteStore(ContentContext ctx)
    {
        return getTenantFileStore(tenantService.getCurrentUserDomain());
    }
    
    @Override
    public List<ContentStore> getAllStores()
    {
        if (tenantService.isEnabled())
        {
            String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
            if ((currentUser == null) || (tenantService.getBaseNameUser(currentUser).equals(AuthenticationUtil.getSystemUserName())))
            {
                // return enabled stores across all tenants, if running as system/null user, for example, ContentStoreCleaner scheduled job      
                List<ContentStore> allEnabledStores = new ArrayList<ContentStore>();
                for (String tenantDomain : tenantFileStores.keySet())
                {
                    allEnabledStores.add(tenantFileStores.get(tenantDomain)); // note: cache should only contain enabled stores
                }
                return allEnabledStores;
            }
        }
        return Arrays.asList(getTenantFileStore(tenantService.getCurrentUserDomain()));
    }
    
    private ContentStore getTenantFileStore(String tenantDomain)
    {
        ContentStore cs = tenantFileStores.get(tenantDomain);
        if (cs == null)
        {
            init();
            cs = tenantFileStores.get(tenantDomain);
        }
        return cs;
    }
    
    private void putTenantFileStore(String tenantDomain, FileContentStore fileStore)
    {
        tenantFileStores.put(tenantDomain, fileStore);
    }
    
    private void removeTenantFileStore(String tenantDomain)
    {
        tenantFileStores.remove(tenantDomain);
    }
    
    public void init()
    {
        String tenantDomain = TenantService.DEFAULT_DOMAIN;
        String rootDir = defaultRootDirectory;
        
        Tenant tenant = tenantService.getTenant(tenantService.getCurrentUserDomain());
        if (tenant != null)
        {
            if (tenant.getRootContentStoreDir() != null)
            {
               rootDir = tenant.getRootContentStoreDir();
            }
            tenantDomain = tenant.getTenantDomain();
        }
        
        putTenantFileStore(tenantDomain, new FileContentStore(this.applicationEventPublisher, new File(rootDir)));
    }
    
    public void destroy()
    {
        removeTenantFileStore(tenantService.getCurrentUserDomain());
    }
    
    public void onEnableTenant()
    {
        init();
    }
    
    public void onDisableTenant()
    {
        destroy();
    }
    
    public String getDefaultRootDir()
    {
        return this.defaultRootDirectory;
    }
}
