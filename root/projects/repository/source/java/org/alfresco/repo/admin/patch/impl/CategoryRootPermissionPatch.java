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

import java.util.List;

import org.springframework.extensions.surf.util.I18NUtil;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.admin.PatchException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;

/**
 * Grant <b>Consumer</b> role to <b>Guest</b> in <b>Category Root</b> folder.
 * <p>
 * This patch expects the folder to be present.
 */
public class CategoryRootPermissionPatch extends AbstractPatch
{
    private static final String MSG_RESULT = "patch.categoryRootPermission.result";
    private static final String ERR_NOT_FOUND = "patch.categoryRootPermission.err.not_found";
    
    private PermissionService permissionService;
    private ImporterBootstrap spacesBootstrap;
    
    
    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    public void setSpacesBootstrap(ImporterBootstrap spacesBootstrap)
    {
        this.spacesBootstrap = spacesBootstrap;
    }
    
    @Override
    protected String applyInternal() throws Exception
    {
        String categoryRootPath = "/cm:categoryRoot";

        // find category root
        NodeRef rootNodeRef = nodeService.getRootNode(spacesBootstrap.getStoreRef());
        List<NodeRef> nodeRefs = searchService.selectNodes(rootNodeRef, categoryRootPath, null, namespaceService, false);
        if (nodeRefs.size() == 0)
        {
            String msg = I18NUtil.getMessage(ERR_NOT_FOUND, categoryRootPath);
            throw new PatchException(msg);
        }
        NodeRef categoryRootRef = nodeRefs.get(0);
        
        // apply permission
        permissionService.setPermission(
                categoryRootRef,
                AuthenticationUtil.getGuestUserName(),
                PermissionService.READ,
                true);

        // done
        String msg = I18NUtil.getMessage(MSG_RESULT, categoryRootPath);
        return msg;
    }
}
