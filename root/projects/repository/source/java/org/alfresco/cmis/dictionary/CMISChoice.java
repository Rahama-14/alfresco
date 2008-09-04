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
package org.alfresco.cmis.dictionary;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Choice for property definitions
 * 
 * @author andyh
 *
 */
public class CMISChoice
{
    private String name;
    
    Serializable value;
    
    private int index;
    
    private Collection<CMISChoice> choices = new HashSet<CMISChoice>();
    
    public CMISChoice(String name, Serializable value, int index)
    {
        this.name = name;
        this.value = value;
        this.index = index;
    }
    
    /**
     * Get the name of the choice
     * @return
     */
    public String getName()
    {
       return name;
    }
    
    /**
     * Get the value when chosen
     * @return
     */
    public Serializable getValue()
    {
        return value;
    }
    
    /**
     * Get the index that determined the choices position amongst it siblings
     * @return
     */
    public int getIndex()
    {
       return index;
    }
    
    /**
     * Get sub-choices
     * @return
     */
    public Collection<CMISChoice> getChildren()
    {
        return choices;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + index;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        final CMISChoice other = (CMISChoice) obj;
        if (index != other.index)
            return false;
        if (name == null)
        {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        return true;
    }
    
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("[Choice ");
        builder.append("name=").append(getName()).append(",");
        builder.append("index=").append(getIndex()).append(",");
        builder.append("value=").append(getValue()).append(",");
        builder.append("children=").append(getChildren());
        builder.append("]");
        return builder.toString();
    }
}
