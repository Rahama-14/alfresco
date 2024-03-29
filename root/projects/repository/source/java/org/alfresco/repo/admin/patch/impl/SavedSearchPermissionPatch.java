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
package org.alfresco.repo.admin.patch.impl;

import org.springframework.extensions.surf.util.I18NUtil;
import org.alfresco.service.cmr.admin.PatchException;
import org.alfresco.service.cmr.security.PermissionService;

/**
 * Grant <b>CONTRIBUTOR</b> role to <b>EVERYONE</b> in <b>savedsearches</b> folder.
 * <p>
 * This patch expects the folder to be present.
 * <p>
 * JIRA: {@link http://www.alfresco.org/jira/browse/AWC-487 AR-487}
 * 
 * @see org.alfresco.repo.admin.patch.impl.SavedSearchFolderPatch
 * @author Derek Hulley
 */
public class SavedSearchPermissionPatch extends SavedSearchFolderPatch
{
    private static final String MSG_CREATED = "patch.savedSearchesPermission.result.applied";
    private static final String ERR_NOT_FOUND = "patch.savedSearchesPermission.err.not_found";
    
    private PermissionService permissionService;
    
    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    @Override
    protected String applyInternal() throws Exception
    {
        // properties must be set
        checkCommonProperties();
        if (permissionService == null)
        {
            throw new PatchException("'permissionService' property has not been set");
        }
        
        // get useful values
        setUp();
        
        if (savedSearchesFolderNodeRef == null)
        {
            // it doesn't exist
            String msg = I18NUtil.getMessage(ERR_NOT_FOUND);
            throw new PatchException(msg);
        }
        // apply permission
        permissionService.setPermission(
                savedSearchesFolderNodeRef,
                PermissionService.ALL_AUTHORITIES,
                PermissionService.CONTRIBUTOR,
                true);
        String msg = I18NUtil.getMessage(MSG_CREATED, savedSearchesFolderNodeRef);

        // done
        return msg;
    }
}
