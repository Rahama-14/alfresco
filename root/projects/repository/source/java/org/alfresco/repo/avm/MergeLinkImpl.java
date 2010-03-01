/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>. */

package org.alfresco.repo.avm;

import java.io.Serializable;

/**
 * This contains a single merged from-to relationship.
 * @author britt
 */
public class MergeLinkImpl implements MergeLink, Serializable
{
    private static final long serialVersionUID = 6672271083042424944L;

    /**
     * The node that was merged from.
     */
    private AVMNode fFrom;
    
    /**
     * The node that was merged to.
     */
    private AVMNode fTo;
    
    /**
     * Set the from part.
     * @param from
     */
    public void setMfrom(AVMNode from)
    {
        fFrom = from;
    }

    /**
     * Get the from part.
     * @return The from part.
     */
    public AVMNode getMfrom()
    {
        return fFrom;
    }

    /**
     * Set the to part.
     * @param to
     */
    public void setMto(AVMNode to)
    {
        fTo = to;
    }

    /**
     * Get the to part.
     * @return The to part.
     */
    public AVMNode getMto()
    {
        return fTo;
    }

    /**
     * Override of equals.
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
        if (!(obj instanceof MergeLink))
        {
            return false;
        }
        MergeLink o = (MergeLink)obj;
        return fFrom.equals(o.getMfrom()) && fTo.equals(o.getMto());
    }

    /**
     * Get the hash code.
     * @return The hash code.
     */
    @Override
    public int hashCode()
    {
        return fFrom.hashCode() + fTo.hashCode();
    }
}
