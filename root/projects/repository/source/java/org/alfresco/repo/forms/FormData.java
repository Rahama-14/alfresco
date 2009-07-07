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
package org.alfresco.repo.forms;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

/**
 * Represents the data going to or coming from a Form.
 *
 * @author Gavin Cornwell
 */
public class FormData implements Iterable<FormData.FieldData>
{
    // TODO: Once we fully support file based FieldData add other methods
    //       in here to retrieve just file fields, just data fields etc.
    
    protected Map<String, FieldData> data;
    
    /**
     * Default constructor
     */
    public FormData()
    {
        this.data = new LinkedHashMap<String, FieldData>(8);
    }
    
    /**
     * Determines whether field data for the given item exists.
     * 
     * @param fieldName Name of field to look for
     * @return true if the field exists, false otherwise
     */
    public boolean hasFieldData(String fieldName)
    {
        return this.data.containsKey(fieldName);
    }
    
    /**
     * Returns the data for the given field.
     * 
     * @param fieldName Name of field to look for 
     * @return FieldData object representing the data for
     * the field or null if it doesn't exist
     */
    public FieldData getFieldData(String fieldName)
    {
        return this.data.get(fieldName);
    }
    
    /**
     * Adds the given data to the form.
     * <p>
     * NOTE: Adding the same named data will append the value and 
     * thereafter return a List containing all added values. 
     * </p>
     * 
     * @param fieldName The name of the field
     * @param fieldValue The value of the data
     */
    public void addFieldData(String fieldName, Object fieldValue)
    {
        this.addFieldData(fieldName, fieldValue, false);
    }
    
    /**
     * Adds the given data to the form. If data for the field is already
     * present the behaviour is controlled by the overwrite property.
     * <p>
     * If overwrite is true the provided value replaces the existing value
     * whereas false will force the creation of a List (if necessary) and the 
     * provided value will be added to the List.
     * </p>
     * 
     * @param fieldName The name of the field
     * @param fieldValue The value of the data
     * @param overwrite 
     */
    @SuppressWarnings("unchecked")
    public void addFieldData(String fieldName, Object fieldValue, boolean overwrite)
    {
        // check whether some data already exists
        if (this.data.containsKey(fieldName))
        {
            // if we are overwriting just replace with provided data
            if (overwrite)
            {
                this.data.put(fieldName, new FieldData(fieldName, fieldValue, false));
            }
            else
            {
                // pull out the existing value and create a List if necessary
                List currentValues = null;
                Object currentValue = this.data.get(fieldName).getValue();
                if (currentValue instanceof List)
                {
                    currentValues = (List)currentValue;
                }
                else
                {
                    // a non List value is present, create the new list
                    // and add the current value to it
                    currentValues = new ArrayList(4);
                    currentValues.add(currentValue);
                    this.data.put(fieldName, new FieldData(fieldName, currentValues, false));
                }
                
                // add the provided value to the list
                currentValues.add(fieldValue);
            }
        }
        else
        {
            this.data.put(fieldName, new FieldData(fieldName, fieldValue, false));
        }
    }
    
    /**
     * Removes the data associated with the given field
     * if it exists.
     * 
     * @param fieldName Name of the field to remove
     */
    public void removeFieldData(String fieldName)
    {
        this.data.remove(fieldName);
    }
    
    /**
     * Returns a list of the names of the fields held by this
     * object.
     * 
     * @return List of String objects
     */
    public Set<String> getFieldNames()
    {
        return this.data.keySet();
    }
    
    /**
     * Returns the number of fields data is being held for.
     * 
     * @return Number of fields
     */
    public int getNumberOfFields()
    {
        return this.data.size();
    }
    
    /**
     * Returns an Iterator over the FieldData objects
     * held by this object.
     * 
     * @return Iterator of FieldData
     */
    public Iterator<FormData.FieldData> iterator()
    {
        return this.data.values().iterator();
    }
    
    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder(super.toString());
        buffer.append(" (");
        buffer.append("data=").append(this.data);
        buffer.append(")");
        return buffer.toString();
    }

    /**
     * Inner class to represent the value of a field on a form
     *
     * @author Gavin Cornwell
     */
    public class FieldData
    {
        protected String name;
        protected Object value;
        protected boolean isFile = false;
        
        /**
         * Default Constructor 
         * 
         * @param name The name of the form field
         * @param value The value of the form field
         * @param isFile Whether the field data represents an uploaded file
         */
        public FieldData(String name, Object value, boolean isFile)
        {
            this.name = name;
            this.value = value;
            this.isFile = isFile;
        }

        /**
         * Returns the name of the form field that data represents
         * 
         * @return The name
         */
        public String getName()
        {
            return this.name;
        }

        /**
         * Returns the value of the form field that data represents
         * 
         * @return The value
         */
        public Object getValue()
        {
            return this.value;
        }

        /**
         * Determines whether the data represents a file
         * 
         * @return true if the data is a file
         */
        public boolean isFile()
        {
            return this.isFile;
        }
        
        /**
         * Returns an InputStream onto the content of the file,
         * throws IllegalStateException if this is called for
         * non file field data
         * 
         * @return An InputStream onto the file
         */
        public InputStream getInputStream()
        {
            // TODO: implement this
            
            throw new NotImplementedException();
        }
        
        /*
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            StringBuilder buffer = new StringBuilder(super.toString());
            buffer.append(" (");
            buffer.append("name=").append(this.name);
            buffer.append(", value=").append(this.value);
            buffer.append(", isFile=").append(this.isFile);
            buffer.append(")");
            return buffer.toString();
        }
    }
}
