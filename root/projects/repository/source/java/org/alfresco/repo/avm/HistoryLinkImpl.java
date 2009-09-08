/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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

/**
 * Holds a ancestor-descendent relationship.
 * @author britt
 */
public class HistoryLinkImpl implements HistoryLink, Serializable
{
    private static final long serialVersionUID = -430859344980137718L;

    /**
     * The ancestor.
     */
    private AVMNode fAncestor;
    
    /**
     * The descendent.
     */
    private AVMNode fDescendent;
    
    /**
     * Set the ancestor part of this.
     * @param ancestor
     */
    public void setAncestor(AVMNode ancestor)
    {
        fAncestor = ancestor;
    }

    /**
     * Get the ancestor part of this.
     * @return The ancestor.
     */
    public AVMNode getAncestor()
    {
        return fAncestor;
    }

    /**
     * Set the descendent part of this.
     * @param descendent
     */
    public void setDescendent(AVMNode descendent)
    {
        fDescendent = descendent;
    }

    /**
     * Get the descendent part of this.
     * @return The descendent.
     */
    public AVMNode getDescendent()
    {
        return fDescendent;
    }

    /**
     * Equals override.
     * @param obj
     * @return Equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof HistoryLink))
        {
            return false;
        }
        HistoryLink o = (HistoryLink)obj;
        return fAncestor.equals(o.getAncestor()) && fDescendent.equals(o.getDescendent());
    }

    /**
     * Get the hashcode.
     * @return The hashcode.
     */
    @Override
    public int hashCode()
    {
        return fAncestor.hashCode() + fDescendent.hashCode();
    }
}
