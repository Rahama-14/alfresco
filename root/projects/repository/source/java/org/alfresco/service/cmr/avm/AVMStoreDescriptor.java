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

package org.alfresco.service.cmr.avm;

import java.io.Serializable;
import java.util.Date;

import org.springframework.extensions.surf.util.ISO8601DateFormat;

/**
 * A value class for Data about an AVMStore.
 * @author britt
 */
public class AVMStoreDescriptor implements Serializable
{
    private static final long serialVersionUID = -4401863082685362175L;

    /**
     * The name.
     */
    private String fName;
    
    /**
     * The creator.
     */
    private String fCreator;
    
    /**
     * The create date.
     */
    private long fCreateDate;
    
    public AVMStoreDescriptor(String name,
                                String creator,
                                long createDate)
    {
        fName = name;
        fCreator = creator;
        fCreateDate = createDate;
    }

    /**
     * @return the fCreateDate
     */
    public long getCreateDate()
    {
        return fCreateDate;
    }

    /**
     * @return the fCreator
     */
    public String getCreator()
    {
        return fCreator;
    }

    /**
     * @return the fName
     */
    public String getName()
    {
        return fName;
    }
    
    public String toString()
    {
        return "[" + fName + ":" + fCreator + ":" + ISO8601DateFormat.format(new Date(fCreateDate)) + "]";
    }
}
