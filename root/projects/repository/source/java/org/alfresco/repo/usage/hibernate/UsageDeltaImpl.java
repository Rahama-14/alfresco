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
package org.alfresco.repo.usage.hibernate;

import org.alfresco.repo.domain.Node;
import org.alfresco.repo.usage.UsageDelta;

/**
 * Usage Delta Implementation
 *
 */
public class UsageDeltaImpl implements UsageDelta
{
    private Long id;
    private Long version;
    
    private Node node;
    private long deltaSize; // +ve or -ve or 0 (in bytes)

    
    public Long getId()
    {
        return id;
    }

    /**
     * For Hibernate use
     */
    @SuppressWarnings("unused")
    private void setId(Long id)
    {
        this.id = id;
    }

    public Long getVersion()
    {
        return version;
    }

    /**
     * For Hibernate use
     */
    @SuppressWarnings("unused")
    private void setVersion(Long version)
    {
        this.version = version;
    }
 
    
    public Node getNode()
    {
        return node;
    }
    
    public void setNode(Node node)
    {
        this.node = node;
    }

    public long getDeltaSize()
    {
        return deltaSize;
    }

    public void setDeltaSize(long deltaSize)
    {
        this.deltaSize = deltaSize;
    }
}
