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
package org.alfresco.repo.domain;

import org.alfresco.service.cmr.repository.StoreRef;

/**
 * Represents a store entity
 * 
 * @author Derek Hulley
 */
public interface Store
{
    /**
     * @return  Returns the current version number used for optimistic locking
     */
    public Long getVersion();
    
    /**
     * @return          Returns the unique ID of the object
     */
    public Long getId();

    /**
     * @return                  the store protocol
     */
    public String getProtocol();
    
    /**
     * @param protocol          the store protocol
     */
    public void setProtocol(String protocol);
    
    /**
     * @return                  the store identifier
     */
    public String getIdentifier();
    
    /**
     * @param identifier        the store identifier
     */
    public void setIdentifier(String identifier);

    /**
     * @return Returns the root of the store
     */
    public Node getRootNode();
    
    /**
     * @param rootNode mandatory association to the root of the store
     */
    public void setRootNode(Node rootNode);
    
    /**
     * Convenience method to access the reference
     * @return Returns the reference to the store
     */
    public StoreRef getStoreRef();
}
