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
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;

/**
 * Change Guest Person permission from Guest to Read
 * 
 * Guest (now Consumer) permission is not valid for cm:person type.
 */
public class GuestPersonPermissionPatch extends AbstractPatch
{
    private static final String MSG_SUCCESS = "patch.guestPersonPermission.result";

    private PersonService personService;

    private PermissionService permissionService;

    public GuestPersonPermissionPatch()
    {
        super();
    }

    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    @Override
    protected String applyInternal() throws Exception
    {
        String guestId = AuthenticationUtil.getGuestUserName();
        if (personService.personExists(guestId))
        {
            NodeRef personRef = personService.getPerson(guestId);
            permissionService.setInheritParentPermissions(personRef, false);
            permissionService.deletePermission(personRef, guestId, PermissionService.CONSUMER);
            permissionService.setPermission(personRef, guestId, PermissionService.READ, true);
        }

        return I18NUtil.getMessage(MSG_SUCCESS);
    }

}
