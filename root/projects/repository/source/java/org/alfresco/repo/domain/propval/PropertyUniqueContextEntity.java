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
package org.alfresco.repo.domain.propval;

/**
 * Entity bean for <b>alf_prop_unique_ctx</b> table.
 * 
 * @author Derek Hulley
 * @since 3.2
 */
public class PropertyUniqueContextEntity
{
    private Long id;
    private short version;
    private Long value1PropId;
    private Long value2PropId;
    private Long value3PropId;
    
    public PropertyUniqueContextEntity()
    {
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(512);
        sb.append("PropertyRootEntity")
          .append("[ ID=").append(id)
          .append(", version=").append(version)
          .append(", value1PropId=").append(value1PropId)
          .append(", value2PropId=").append(value2PropId)
          .append(", value3PropId=").append(value3PropId)
          .append("]");
        return sb.toString();
    }

    public void incrementVersion()
    {
        if (version >= Short.MAX_VALUE)
        {
            this.version = 0;
        }
        else
        {
            this.version++;
        }
    }
    
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public short getVersion()
    {
        return version;
    }

    public void setVersion(short version)
    {
        this.version = version;
    }

    public Long getValue1PropId()
    {
        return value1PropId;
    }

    public void setValue1PropId(Long value1PropId)
    {
        this.value1PropId = value1PropId;
    }

    public Long getValue2PropId()
    {
        return value2PropId;
    }

    public void setValue2PropId(Long value2PropId)
    {
        this.value2PropId = value2PropId;
    }

    public Long getValue3PropId()
    {
        return value3PropId;
    }

    public void setValue3PropId(Long value3PropId)
    {
        this.value3PropId = value3PropId;
    }
}
