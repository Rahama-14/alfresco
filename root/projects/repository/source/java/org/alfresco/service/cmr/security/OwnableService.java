/*
 * Copyright (C) 2005 Alfresco, Inc.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.alfresco.service.cmr.security;

import org.alfresco.service.Auditable;
import org.alfresco.service.PublicService;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Service support around managing ownership.
 * 
 * @author Andy Hind
 */
@PublicService
public interface OwnableService
{
    /**
     * Get the username of the owner of the given object.
     *  
     * @param nodeRef
     * @return the username or null if the object has no owner
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef"})
    public String getOwner(NodeRef nodeRef);
    
    /**
     * Set the owner of the object.
     * 
     * @param nodeRef
     * @param userName
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef", "userName"})
    public void setOwner(NodeRef nodeRef, String userName);
    
    /**
     * Set the owner of the object to be the current user.
     * 
     * @param nodeRef
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef"})
    public void takeOwnership(NodeRef nodeRef);
    
    /**
     * Does the given node have an owner?
     * 
     * @param nodeRef
     * @return
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef"})
    public boolean hasOwner(NodeRef nodeRef);
}
