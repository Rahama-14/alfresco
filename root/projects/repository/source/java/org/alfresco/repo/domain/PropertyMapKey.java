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

import org.alfresco.util.EqualsHelper;

/**
 * Compound key for persistence of {@link org.alfresco.repo.domain.Node}
 * 
 * @author Derek Hulley
 */
public class PropertyMapKey implements Serializable, Comparable<PropertyMapKey>
{
    private static final long serialVersionUID = 3258695403221300023L;
    
    private Long qnameId;
    private Long localeId;
    private Integer listIndex;
    
    public PropertyMapKey()
    {
    }
    
	public String toString()
	{
		return ("PropertymapKey[" +
				" qnameId=" + qnameId +
				", localeId=" + localeId +
				", listIndex=" + listIndex +
				"]");
	}
    
    public int hashCode()
    {
        return
                (qnameId == null ? 0 : qnameId.hashCode()) +
                (listIndex == null ? 0 : listIndex.hashCode());
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        else if (!(obj instanceof PropertyMapKey))
        {
            return false;
        }
        PropertyMapKey that = (PropertyMapKey) obj;
        return (EqualsHelper.nullSafeEquals(this.qnameId, that.qnameId) &&
                EqualsHelper.nullSafeEquals(this.listIndex, that.listIndex) &&
                EqualsHelper.nullSafeEquals(this.localeId, that.localeId)
                );
    }

    /**
     * throws ClassCastException        if the object is not of the correct type
     */
    public int compareTo(PropertyMapKey that)
    {
        // Comparision by priority: qnameId, listIndex, localeId 
        if (this.qnameId.equals(that.qnameId))
        {
            if (this.listIndex.equals(that.listIndex))
            {
                return this.localeId.compareTo(that.localeId);
            }
            else
            {
                return this.listIndex.compareTo(that.listIndex);
            }
        }
        else
        {
            return this.qnameId.compareTo(that.qnameId);
        }
    }

    public Long getQnameId()
    {
        return qnameId;
    }

    public void setQnameId(Long qnameId)
    {
        this.qnameId = qnameId;
    }

    public Long getLocaleId()
    {
        return localeId;
    }

    public void setLocaleId(Long localeId)
    {
        this.localeId = localeId;
    }

    public Integer getListIndex()
    {
        return listIndex;
    }

    public void setListIndex(Integer listIndex)
    {
        this.listIndex = listIndex;
    }
}
