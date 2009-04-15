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
package org.alfresco.repo.transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Helper class that will look up or create transactional resources.
 * This shortcuts some of the "<i>if not existing, then create</i>" code.
 * 
 * @author Derek Hulley
 * @since 3.2
 */
public abstract class TransactionalResourceHelper
{
    /**
     * Support method to retrieve or create and bind a <tt>HashMap</tt> to the current transaction.
     * 
     * @param <K>           the map key type
     * @param <V>           the map value type
     * @param resourceKey   the key under which the resource will be stored
     * @return              Returns an previously-bound <tt>Map</tt> or else a newly-bound <tt>HashMap</tt>
     */
    public static final <K,V> Map<K,V> getMap(Object resourceKey)
    {
        Map<K,V> map = AlfrescoTransactionSupport.<Map<K,V>>getResource(resourceKey);
        if (map == null)
        {
            map = new HashMap<K, V>(29);
            AlfrescoTransactionSupport.bindResource(resourceKey, map);
        }
        return map;
    }

    /**
     * Support method to retrieve or create and bind a <tt>HashSet</tt> to the current transaction.
     * 
     * @param <V>           the set value type
     * @param resourceKey   the key under which the resource will be stored
     * @return              Returns an previously-bound <tt>Set</tt> or else a newly-bound <tt>HashSet</tt>
     */
    public static final <V> Set<V> getSet(Object resourceKey)
    {
        Set<V> set = AlfrescoTransactionSupport.<Set<V>>getResource(resourceKey);
        if (set == null)
        {
            set = new HashSet<V>(29);
            AlfrescoTransactionSupport.bindResource(resourceKey, set);
        }
        return set;
    }

    /**
     * Support method to retrieve or create and bind a <tt>TreeSet</tt> to the current transaction.
     * 
     * @param <V>           the set value type
     * @param resourceKey   the key under which the resource will be stored
     * @return              Returns an previously-bound <tt>TreeSet</tt> or else a newly-bound <tt>TreeSet</tt>
     */
    public static final <V> TreeSet<V> getTreeSet(Object resourceKey)
    {
        TreeSet<V> set = AlfrescoTransactionSupport.<TreeSet<V>>getResource(resourceKey);
        if (set == null)
        {
            set = new TreeSet<V>();
            AlfrescoTransactionSupport.bindResource(resourceKey, set);
        }
        return set;
    }

    /**
     * Support method to retrieve or create and bind a <tt>ArrayList</tt> to the current transaction.
     * 
     * @param <V>           the list value type
     * @param resourceKey   the key under which the resource will be stored
     * @return              Returns an previously-bound <tt>List</tt> or else a newly-bound <tt>ArrayList</tt>
     */
    public static final <V> List<V> getList(Object resourceKey)
    {
        List<V> list = AlfrescoTransactionSupport.<List<V>>getResource(resourceKey);
        if (list == null)
        {
            list = new ArrayList<V>(29);
            AlfrescoTransactionSupport.bindResource(resourceKey, list);
        }
        return list;
    }
}
