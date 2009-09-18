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
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.domain.propval;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.alfresco.repo.domain.CrcHelper;
import org.alfresco.util.Pair;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * DAO services for <b>alf_prop_XXX</b> tables.
 * 
 * @author Derek Hulley
 * @since 3.2
 */
public interface PropertyValueDAO
{
    //================================
    // 'alf_prop_class' accessors
    //================================
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_class</b> accessor
     * 
     * @param id                the ID (may not be <tt>null</tt>)
     */
    Pair<Long, Class<?>> getPropertyClassById(Long id);
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_class</b> accessor
     * 
     * @param value             the value to find the ID for (may not be <tt>null</tt>)
     */
    Pair<Long, Class<?>> getPropertyClass(Class<?> value);
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_class</b> accessor
     * 
     * @param value             the value to find the ID for (may not be <tt>null</tt>)
     */
    Pair<Long, Class<?>> getOrCreatePropertyClass(Class<?> value);

    //================================
    // 'alf_prop_date_value' accessors
    //================================
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_date_value</b> accessor
     * 
     * @param id                the ID (may not be <tt>null</tt>)
     */
    Pair<Long, Date> getPropertyDateValueById(Long id);
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_date_value</b> accessor
     * 
     * @param value             the value to find the ID for (may not be <tt>null</tt>)
     */
    Pair<Long, Date> getPropertyDateValue(Date value);
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_date_value</b> accessor
     * 
     * @param value             the value to find the ID for (may not be <tt>null</tt>)
     */
    Pair<Long, Date> getOrCreatePropertyDateValue(Date value);
    
    //================================
    // 'alf_prop_string_value' accessors
    //================================
    /**
     * Utility method to get query parameters for case-sensitive string searching
     * @see CrcHelper
     */
    Pair<String, Long> getPropertyStringCaseSensitiveSearchParameters(String value);
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_string_value</b> accessor
     * 
     * @param id                the ID (may not be <tt>null</tt>)
     */
    Pair<Long, String> getPropertyStringValueById(Long id);
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_string_value</b> accessor
     * 
     * @param value             the value to find the ID for (may not be <tt>null</tt>)
     */
    Pair<Long, String> getPropertyStringValue(String value);
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_string_value</b> accessor
     * 
     * @param value             the value to find the ID for (may not be <tt>null</tt>)
     */
    Pair<Long, String> getOrCreatePropertyStringValue(String value);

    //================================
    // 'alf_prop_double_value' accessors
    //================================
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_double_value</b> accessor
     * 
     * @param id                the ID (may not be <tt>null</tt>)
     */
    Pair<Long, Double> getPropertyDoubleValueById(Long id);
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_double_value</b> accessor
     * 
     * @param value             the value to find the ID for (may not be <tt>null</tt>)
     */
    Pair<Long, Double> getPropertyDoubleValue(Double value);
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_double_value</b> accessor
     * 
     * @param value             the value to find the ID for (may not be <tt>null</tt>)
     */
    Pair<Long, Double> getOrCreatePropertyDoubleValue(Double value);
    
    //================================
    // 'alf_prop_serializable_value' accessors
    //================================
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_serializable_value</b> accessor
     * 
     * @param id                the ID (may not be <tt>null</tt>)
     */
    Pair<Long, Serializable> getPropertySerializableValueById(Long id);
    /**
     * <b>FOR INTERNAL USE ONLY</b>: Do not use directly; see interface comments.
     * <p/>
     * <b>alf_prop_serializable_value</b> accessor
     * 
     * @param value             the value to find the ID for (may not be <tt>null</tt>)
     */
    Pair<Long, Serializable> createPropertySerializableValue(Serializable value);
    
    //================================
    // 'alf_prop_value' accessors
    //================================
    /**
     * Use for accessing unique properties; see interface comments.
     * <p/>
     * <b>alf_prop_value</b> accessor: get a property based on the database ID
     * 
     * @param id                the ID (may not be <tt>null</tt>)
     */
    Pair<Long, Serializable> getPropertyValueById(Long id);
    /**
     * Use for accessing unique properties; see interface comments.
     * <p/>
     * <b>alf_prop_value</b> accessor: find a property based on the value
     * 
     * @param value             the value to find the ID for (may be <tt>null</tt>)
     */
    Pair<Long, Serializable> getPropertyValue(Serializable value);
    /**
     * Use for accessing unique properties; see interface comments.
     * <p/>
     * <b>alf_prop_value</b> accessor: find or create a property based on the value.
     * <b>Note:</b> This method will not recurse into maps or collections.  Use the
     * dedicated methods if you want recursion; otherwise maps and collections will
     * be serialized and probably stored as BLOB values.
     * <p/>
     * All collections and maps will be opened up to any depth.  To limit this behaviour,
     * use {@link #getOrCreatePropertyValue(Serializable, int)}.
     * 
     * @param value             the value to find the ID for (may be <tt>null</tt>)
     */
    Pair<Long, Serializable> getOrCreatePropertyValue(Serializable value);
    
    //================================
    // 'alf_prop_root' accessors
    //================================
    /**
     * Use for accessing non-unique, exploded properties; see interface comments.
     * <p/>
     * <b>alf_prop_root</b> accessor: get a property based on the database ID
     * 
     * @param id                the ID (may not be <tt>null</tt>)
     * @return                  Returns the value of the property (never <tt>null</tt>)
     * @throws DataIntegrityViolationException if the ID is invalid
     */
    Serializable getPropertyById(Long id);
    /**
     * Use for accessing non-unique, exploded properties; see interface comments.
     * <p/>
     * <b>alf_prop_root</b> accessor: find or create a property based on the value.
     * <p/>
     * All collections and maps will be opened up to any depth.
     * 
     * @param value             the value to create (may be <tt>null</tt>)
     * @return                  Returns the new property's ID
     */
    Long createProperty(Serializable value);
    
    /**
     * Use for accessing non-unique, exploded properties; see interface comments.
     * <p/>
     * <b>alf_prop_root</b> accessor: update the property root to contain a new value.
     * 
     * @param id                the ID of the root property to change
     * @param value             the new property value
     */
    void updateProperty(Long id, Serializable value);
    
    /**
     * Use for accessing non-unique, exploded properties; see interface comments.
     * <p/>
     * <b>alf_prop_root</b> accessor: delete a property root completely
     * 
     * @param id                the ID of the root property to delete
     */
    void deleteProperty(Long id);
    
    /**
     * Utility method to convert property query results into the original value.  Note
     * that the rows must all share the same root property ID.
     * <p/>
     * If the rows passed in don't constitute a valid, full property - they don't contain all
     * the link entities for the property - then the result may be <tt>null</tt>.
     * 
     * @param rows              the search results for a single root property
     * @return                  Returns the root property as originally persisted, or <tt>null</tt>
     *                          if the rows don't represent a complete property
     * @throws IllegalArgumentException     if rows don't all share the same root property ID
     */
    Serializable convertPropertyIdSearchRows(List<PropertyIdSearchRow> rows);
}
