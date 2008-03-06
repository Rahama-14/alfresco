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
 * http://www.alfresco.com/legal/licensing" */

package org.alfresco.repo.avm;

import java.io.Serializable;

import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.domain.QNameEntity;

/**
 * Simple bean to hold properties attached to AVMStores.
 * @author britt
 */
class AVMStorePropertyImpl implements AVMStoreProperty, Serializable
{
    private static final long serialVersionUID = -5419606158990318723L;

    /**
     * The Primary Key.
     */
    private Long fID;
    
    /**
     * The store that owns this property.
     */
    private AVMStore fStore;
    
    /**
     * The name of the property.
     */
    private QNameEntity name;
    
    /**
     * The actual PropertyValue.
     */
    private PropertyValue fValue;
    
    public AVMStorePropertyImpl()
    {
    }

    /**
     * {@inheritDoc}
     */
    public QNameEntity getName()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public void setName(QNameEntity name)
    {
        this.name = name;
    }

    /**
     * Get the store this property belongs to.
     * @return The AVMStore that owns this.
     */
    public AVMStore getStore()
    {
        return fStore;
    }

    /**
     * Set the store that this property belongs to.
     * @param store The AVMStore.
     */
    public void setStore(AVMStore store)
    {
        fStore = store;
    }

    /**
     * Get the actual property value.
     * @return A PropertyValue object.
     */
    public PropertyValue getValue()
    {
        return fValue;
    }

    /**
     * Set the actual property value.
     * @param value The PropertyValue to set.
     */
    public void setValue(PropertyValue value)
    {
        fValue = value;
    }
    
    /**
     * Set the primary key. (For Hibernate)
     * @param id The primary key.
     */
    protected void setId(Long id)
    {
        fID = id;
    }
    
    /**
     * Get the primary key. (For Hibernate)
     * @return The primary key.
     */
    protected Long getId()
    {
        return fID;
    }
    
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof AVMStoreProperty))
        {
            return false;
        }
        AVMStoreProperty o = (AVMStoreProperty)other;
        return fStore.equals(o.getStore()) && name.equals(o.getName());
    }
    
    @Override
    public int hashCode()
    {
        return fStore.hashCode() + name.hashCode();
    }
}

