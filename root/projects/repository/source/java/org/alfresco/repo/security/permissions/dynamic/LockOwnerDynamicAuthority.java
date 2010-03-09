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
package org.alfresco.repo.security.permissions.dynamic;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.DynamicAuthority;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.impl.ModelDAO;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.PermissionService;
import org.springframework.context.ApplicationEvent;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;
import org.springframework.extensions.surf.util.PropertyCheck;

/**
 * LockOwnerDynamicAuthority
 */
public class LockOwnerDynamicAuthority extends AbstractLifecycleBean implements DynamicAuthority
{
    private LockService lockService;
    
    private NodeService nodeService;
    
    private ModelDAO modelDAO;
    
    private List<String> requiredFor;
    
    private Set<PermissionReference> whenRequired;
    
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

    @Override
    protected void onBootstrap(ApplicationEvent event)
    {
        PropertyCheck.mandatory(this, "lockService", lockService);
        PropertyCheck.mandatory(this, "nodeService", nodeService);
        PropertyCheck.mandatory(this, "modelDAO", modelDAO);

        // Build the permission set
        if(requiredFor != null)
        {
            whenRequired = new HashSet<PermissionReference>();
            for(String permission : requiredFor)
            {
                PermissionReference permissionReference = modelDAO.getPermissionReference(null, permission);
                whenRequired.addAll(modelDAO.getGranteePermissions(permissionReference));
                whenRequired.addAll(modelDAO.getGrantingPermissions(permissionReference));
            }
        }
    }

    /**
     * No-op
     */
    @Override
    protected void onShutdown(ApplicationEvent event)
    {
    }

    /**
     * Set the lock service
     * @param lockService
     */
    public void setLockService(LockService lockService)
    {
        this.lockService = lockService;
    }
    
    /**
     * Set the node service
     * @param nodeService
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Set the permissions model dao
     * @param modelDAO
     */
    public void setModelDAO(ModelDAO modelDAO)
    {
        this.modelDAO = modelDAO;
    }
    
    /**
     * Set the permissions for which this dynamic authority is required
     * @param requiredFor
     */
    public void setRequiredFor(List<String> requiredFor)
    {
        this.requiredFor = requiredFor;
    }
    
    
    public Set<PermissionReference> requiredFor()
    {
        return whenRequired;
    }
}
