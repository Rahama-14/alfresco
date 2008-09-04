/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.cmis;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


/**
 * Enum Factory for managing mapping between Enum name and Enum label
 * 
 * @author dcaruana
 *
 * @param <E>
 */
public class EnumFactory<E extends Enum<E>>
{
    private Enum<E> defaultEnum;
    private Map<String, Enum<E>> labelMap = new HashMap<String, Enum<E>>(10);
    
    /**
     * @param enumClass
     */
    public EnumFactory(Class<E> enumClass)
    {
        this(enumClass, null, false);
    }

    /**
     * @param enumClass
     * @param defaultEnum
     */
    public EnumFactory(Class<E> enumClass, Enum<E> defaultEnum)
    {
        this(enumClass, defaultEnum, false);
    }
    
    /**
     * @param enumClass
     * @param defaultEnum
     * @param caseSensitive  case-sensitive lookup for Enum label
     */
    public EnumFactory(Class<E> enumClass, Enum<E> defaultEnum, boolean caseSensitive)
    {
        this.defaultEnum = defaultEnum;

        // setup label map
        labelMap = caseSensitive ? new HashMap<String, Enum<E>>(10) : new TreeMap<String, Enum<E>>(String.CASE_INSENSITIVE_ORDER);
        EnumSet<E> enumSet = EnumSet.allOf(enumClass);
        Iterator<E> iter = enumSet.iterator();
        while(iter.hasNext())
        {
            Enum<E> e = iter.next();
            if (e instanceof EnumLabel)
            {
                labelMap.put(((EnumLabel)e).getLabel(), e);
            }
        }
    }

    /**
     * Gets the default enum
     * 
     * @return  default enum (or null, if no default specified)
     */
    public Enum<E> defaultEnum()
    {
        return defaultEnum;
    }

    /**
     * Gets the default label
     * 
     * @return  label of default enum (or null, if no default specified)
     */
    public String defaultLabel()
    {
        return label(defaultEnum);
    }
    
    /**
     * Gets the label for the specified enum
     * 
     * @param e  enum
     * @return  label (or null, if no label specified)
     */
    public String label(Enum<E> e)
    {
        if (e instanceof EnumLabel)
        {
            return ((EnumLabel)e).getLabel();
        }
        return null;
    }

    /**
     * Is valid label?
     * 
     * @param label
     * @return  true => valid, false => does not exist for this enum
     */
    public boolean validLabel(String label)
    {
        return fromLabel(label) == null ? false : true;
    }

    /**
     * Gets enum from label
     * 
     * @param label
     * @return  enum (or null, if no enum has specified label)
     */
    public Enum<E> fromLabel(String label)
    {
        return labelMap.get(label);
    }
    
    /**
     * Gets enum from label
     * 
     * NOTE: If specified label is invalid, the default enum is returned
     *       
     * @param label
     * @return  enum (or default enum, if label is invalid)
     */
    public Enum<E> toEnum(String label)
    {
        Enum<E> e = fromLabel(label);
        if (e == null)
        {
            e = defaultEnum;
        }
        return e;
    }
}