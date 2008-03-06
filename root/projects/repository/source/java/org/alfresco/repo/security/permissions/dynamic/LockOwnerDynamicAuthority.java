/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.repo.security.permissions.dynamic;

import java.io.Serializable;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.DynamicAuthority;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.EqualsHelper;
import org.springframework.beans.factory.InitializingBean;

/**
 * LockOwnerDynamicAuthority
 */
public class LockOwnerDynamicAuthority implements DynamicAuthority, InitializingBean
{
    private LockService lockService;
    
    private NodeService nodeService;
    
    
    public boolean hasAuthority(final NodeRef nodeRef, final String userName)
    {
        return AuthenticationUtil.runAs(new RunAsWork<Boolean>(){

            public Boolean doWork() throws Exception
            {
                if (lockService.getLockStatus(nodeRef, userName) == LockStatus.LOCK_OWNER)
                {
                    return true;
                }
                if (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY))
                {
                    NodeRef original = null;
                    Serializable reference = nodeService.getProperty(nodeRef, ContentModel.PROP_COPY_REFERENCE);
                    if (reference != null)
                    {
                        original = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, reference);
                    }
                    if (original != null && nodeService.exists(original))
                    {
                        return (lockService.getLockStatus(original, userName) == LockStatus.LOCK_OWNER);
                    }
                    else
                    {
                        return false;
                    }
                }
                else
                {
                    return false;
                }
            }}, AuthenticationUtil.getSystemUserName());
        
        
        
    }

    public String getAuthority()
    {
        return PermissionService.LOCK_OWNER_AUTHORITY;
    }

    public void afterPropertiesSet() throws Exception
    {
        if(lockService == null)
        {
            throw new IllegalStateException("The LockService must be set");
        }
        if(nodeService == null)
        {
            throw new IllegalStateException("The NodeService service must be set");
        }
        
    }

    public void setLockService(LockService lockService)
    {
        this.lockService = lockService;
    }
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
}
