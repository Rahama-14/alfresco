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
package org.alfresco.util;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

/**
 * Property map helper class.  
 * <p>
 * This class can be used as a short hand when a class of type
 * Map<QName, Serializable> is required.
 * 
 * @author Roy Wetherall
 */
public class PropertyMap extends HashMap<QName, Serializable>
{
    private static final long serialVersionUID = 8052326301073209645L;
    
    /**
     * A static empty map to us when having to deal with nulls
     */
    public static final Map<QName, Serializable> EMPTY_MAP = Collections.<QName, Serializable>emptyMap();
    
    /**
     * @see HashMap#HashMap(int, float)
     */
    public PropertyMap(int initialCapacity, float loadFactor)
    {
        super(initialCapacity, loadFactor);
    }
    
    /**
     * @see HashMap#HashMap(int)
     */
    public PropertyMap(int initialCapacity)
    {
        super(initialCapacity);
    }
    
    /**
     * @see HashMap#HashMap()
     */
    public PropertyMap()
    {
        super();
    }
    
    /**
     * Utility method to remove unchanged entries from each map.
     * 
     * @param before                the properties before (may be <tt>null</tt>)
     * @param after                 the properties after (may be <tt>null</tt>)
     * @return                      Return a map of values that <b>changed</b> from before to after.
     *                              The before value is first and the after value is second.
     * 
     * @since 3.2
     */
    public static Pair<Map<QName, Serializable>, Map<QName, Serializable>> getBeforeAndAfterMapsForChanges(
            Map<QName, Serializable> before,
            Map<QName, Serializable> after)
    {
        // Shortcuts
        if (before == null)
        {
            before = Collections.emptyMap();
        }
        if (after == null)
        {
            after = Collections.emptyMap();
        }
        
        // Get after values that changed
        Map<QName, Serializable> afterDelta = new HashMap<QName, Serializable>(after);
        afterDelta.entrySet().removeAll(before.entrySet());
        // Get before values that changed
        Map<QName, Serializable> beforeDelta = new HashMap<QName, Serializable>(before);
        beforeDelta.entrySet().removeAll(after.entrySet());
        
        // Done
        return new Pair<Map<QName, Serializable>, Map<QName, Serializable>>(beforeDelta, afterDelta);
    }
}
