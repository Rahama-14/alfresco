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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Data representation of a form to be displayed in the UI.
 *
 * @author Gavin Cornwell
 */
public class Form
{
    protected String item;
    protected String type;
    protected Collection<FieldDefinition> fieldDefinitions;
    protected Collection<FieldGroup> fieldGroups;
    protected FormData data;
    
    /**
     * Constructs a Form
     * 
     * @param item An identifier for the item the form is for
     */
    public Form(String item)
    {
        this.item = item;
    }

    /**
     * Returns an identifier for the item the form is for, in the case of a node
     * it will be a NodeRef, for a task, a task id etc.
     * 
     * @return The item
     */
    public String getItem()
    {
        return this.item;
    }  
    
    /**
     * Returns the type of the item the form is for, could be a content model type, a
     * workflow task type, an XML schema etc.
     * 
     * @return The type of the item
     */
    public String getType()
    {
        return this.type;
    }
    
    /**
     * Sets the type of the item this Form represents
     * 
     * @param type The type
     */
    public void setType(String type)
    {
        this.type = type;
    }
       
    /**
     * Returns the collection of field definitions for the form
     * 
     * @return Collection of FieldDefintion objects or null if there are no fields
     */
    public Collection<FieldDefinition> getFieldDefinitions()
    {
        return this.fieldDefinitions;
    }
    
    /**
     * Sets the collection of FieldDefintion objects representing the fields the
     * form is able to display
     * 
     * @param fieldDefinitions Collection of FieldDefinition objects
     */
    public void setFieldDefinitions(Collection<FieldDefinition> fieldDefinitions)
    {
        this.fieldDefinitions = fieldDefinitions;
    }
    
    /**
     * Adds the given FieldDefinition to the form.
     * <p>
     * NOTE: Multiple fields with the same name can be added to the list,
     *       it is therefore the form processor and the client of the 
     *       FormService responsibility to differentiate the fields in
     *       some way i.e. by type, property vs. association.
     * 
     * @param definition The FieldDefinition to add
     */
    public void addFieldDefinition(FieldDefinition definition)
    {
        if (this.fieldDefinitions == null)
        {
            this.fieldDefinitions = new ArrayList<FieldDefinition>(8);
        }
        
        this.fieldDefinitions.add(definition);
    }
    
    /**
     * Returns the collection of field groups for the form 
     * 
     * @return Collection of FieldGroup objects or null if there are no groups
     */
    public Collection<FieldGroup> getFieldGroups()
    {
        return this.fieldGroups;
    }
    
    /**
     * Sets the collection of FieldGroup objects representing the groups of
     * fields the form should display and maintain
     * 
     * @param fieldGroups Collection of FieldGroup objects
     */
    public void setFieldGroups(Collection<FieldGroup> fieldGroups)
    {
        this.fieldGroups = fieldGroups;
    }
    
    /**
     * Returns the data to display in the form
     * 
     * @return FormData object holding the data of the form or null
     *         if there is no data i.e. for a create form
     */
    public FormData getFormData()
    {
        return this.data;
    }

    /**
     * Sets the data this form should display
     * 
     * @param data FormData instance containing the data
     */
    public void setFormData(FormData data)
    {
        this.data = data;
    }
    
    /*
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuilder buffer = new StringBuilder(super.toString());
        buffer.append(" (");
        buffer.append("item=").append(this.item);
        buffer.append(", type=").append(this.type);
        buffer.append(", fieldGroups=").append(this.fieldGroups);
        buffer.append("\nfieldDefinitions=").append(this.fieldDefinitions);
        buffer.append("\nformData=").append(this.data);
        buffer.append(")");
        return buffer.toString();
    }
}




