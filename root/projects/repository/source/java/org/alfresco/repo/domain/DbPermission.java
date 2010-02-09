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

import java.io.Serializable;

import org.alfresco.repo.domain.qname.QNameDAO;
import org.alfresco.service.namespace.QName;

/**
 * The interface against which permission references are persisted in hibernate.
 * 
 * @author andyh
 */
public interface DbPermission extends Serializable
{
    /**
     * Convenience method to get the type QName of the permission
     * 
     * @param qnameDAO          helper DAO
     * @return                  the permission's type QName
     */
    public QName getTypeQName(QNameDAO qnameDAO);
    
    /**
     * @return Returns the automatically assigned ID
     */
    public Long getId();
    
    /**
     * @return  Returns the version number for optimistic locking
     */
    public Long getVersion();
    
    /**
     * @return Returns the qualified name of this permission
     */
    public Long getTypeQNameId();
    
    /**
     * @param typeQNameId       the ID of the QName for this instance
     */
    public void setTypeQNameId(Long typeQNameId);

    /**
     * @return Returns the permission name
     */
    public String getName();
    
    /**
     * @param name the name of the permission
     */
    public void setName(String name);
    
    /**
     * @return Returns a key combining the {@link #getTypeQnameId() type}
     *      and {@link #getName() name}
     */
    public DbPermissionKey getKey();
}
