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
package org.alfresco.repo.search.impl.querymodel.impl;

import org.alfresco.repo.search.impl.querymodel.Argument;

/**
 * @author andyh
 *
 */
public abstract class BaseArgument implements Argument
{
    private String name;
    
    private boolean queryable;
    
    private boolean orderable;
    
    
    public BaseArgument(String name, boolean queryable, boolean orderable)
    {
        this.name = name;
        this.queryable = queryable;
        this.orderable = orderable;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.querymodel.Argument#getName()
     */
    public String getName()
    {
        return name;
    }

    public boolean isOrderable()
    {
        return orderable;
    }

    public boolean isQueryable()
    {
       return queryable;
    }
}
