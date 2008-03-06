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
package org.alfresco.repo.domain.hibernate;

import java.io.Serializable;

import org.alfresco.repo.domain.DbAccessControlEntryContext;

public class DbAccessControlEntryContextImpl implements DbAccessControlEntryContext, Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = -4479587461724827683L;

    private String classContext;
    
    private String kvpContext;
    
    private String propertyContext;
    
    private Long id;
    
    private Long version;

    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(128);
        sb.append("DbAccessControlEntryContextImpl").append("[ id=").append(id).append(", version=").append(version).append(", classContext=").append(classContext).append(
                ", kvpContext=").append(kvpContext).append(", propertyContext=").append(propertyContext);
        return sb.toString();
    }
    
    @Override
    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((classContext == null) ? 0 : classContext.hashCode());
        result = PRIME * result + ((kvpContext == null) ? 0 : kvpContext.hashCode());
        result = PRIME * result + ((propertyContext == null) ? 0 : propertyContext.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DbAccessControlEntryContextImpl other = (DbAccessControlEntryContextImpl) obj;
        if (classContext == null)
        {
            if (other.classContext != null)
                return false;
        }
        else if (!classContext.equals(other.classContext))
            return false;
        if (kvpContext == null)
        {
            if (other.kvpContext != null)
                return false;
        }
        else if (!kvpContext.equals(other.kvpContext))
            return false;
        if (propertyContext == null)
        {
            if (other.propertyContext != null)
                return false;
        }
        else if (!propertyContext.equals(other.propertyContext))
            return false;
        return true;
    }

    public String getClassContext()
    {
      return classContext;
    }

    public Long getId()
    {
       return id;
    }

    public String getKvpContext()
    {
        return kvpContext;
    }

    public String getPropertyContext()
    {
       return propertyContext;
    }

    public Long getVersion()
    {
        return version;
    }

    public void setClassContext(String classContext)
    {
        this.classContext = classContext;
    }

    public void setKvpContext(String kvpContext)
    {
       this.kvpContext = kvpContext;

    }

    public void setPropertyContext(String propertyContext)
    {
        this.propertyContext = propertyContext;

    }

    /**
     * For Hibernate use
     */
    @SuppressWarnings("unused")
    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * For Hibernate use
     */
    @SuppressWarnings("unused")
    private void setVersion(Long version)
    {
        this.version = version;
    }

}
