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

import java.util.List;
import java.util.Map;

/**
 * A property field definition.
 *
 * @author Gavin Cornwell
 */
public class PropertyFieldDefinition extends FieldDefinition
{
    protected String dataType;
    protected boolean mandatory = false;
    protected boolean repeats = false;
    protected List<FieldConstraint> constraints;
    
    /**
     * Default constructor
     * 
     * @param name              The name of the property
     * @param dataType          The data type of the property
     */
    public PropertyFieldDefinition(String name, String dataType)
    {
        super(name);
        
        this.dataType = dataType;
    }
    
    /**
     * Returns the dataType for the property, this is a value from the 
     * Alfresco data dictionary i.e. d:text, d:int etc.
     * 
     * @return The field's data type
     */
    public String getDataType()
    {
        return this.dataType;
    }

    /**
     * Determines if the property is mandatory
     * 
     * @return true if the field is mandatory
     */
    public boolean isMandatory()
    {
        return this.mandatory;
    }
    
    /**
     * Sets whether the property is mandatory
     * 
     * @param mandatory true if it is mandatory
     */
    public void setMandatory(boolean mandatory)
    {
        this.mandatory = mandatory;
    }

    /**
     * Determines if the property can contain multiple values
     * 
     * @return true if the field can contain multiple values
     */
    public boolean isRepeating()
    {
        return this.repeats;
    }

    /**
     * Sets whether the property can contain multiple values
     * 
     * @param repeats true if the field can contain multiple values
     */
    public void setRepeating(boolean repeats)
    {
        this.repeats = repeats;
    }

    /**
     * Returns a list of constraints the property may have
     * 
     * @return List of FieldContstraint objects or null if there are
     *         no constraints for the field
     */
    public List<FieldConstraint> getConstraints()
    {
        return this.constraints;
    }

    /**
     * Sets the list of FieldConstraint objects for the property
     * 
     * @param constraints List of FieldConstraint objects
     */
    public void setConstraints(List<FieldConstraint> constraints)
    {
        this.constraints = constraints;
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuilder buffer = new StringBuilder(super.toString());
        buffer.append(" (");
        buffer.append("name=").append(this.name);
        buffer.append(", dataType=").append(this.dataType);
        buffer.append(", label=").append(this.label);
        buffer.append(", description=").append(this.description);
        buffer.append(", binding=").append(this.binding);
        buffer.append(", defaultValue=").append(this.defaultValue);
        buffer.append(", group=").append(this.group);
        buffer.append(", protectedField=").append(this.protectedField);
        buffer.append(", mandatory=").append(this.mandatory);
        buffer.append(", repeats=").append(this.repeats);
        buffer.append(", constraints=").append(this.constraints);
        buffer.append(")");
        return buffer.toString();
    }
    
    /**
     * Represents a constraint on a property field
     */
    public class FieldConstraint
    {
        protected String type;
        protected Map<String, String> params;
        
        /**
         * Constructs a FieldConstraint
         * 
         * @param type      The type of the constraint
         * @param params    Map of parameters for the constraint
         */
        public FieldConstraint(String type, Map<String, String> params)
        {
            super();
            this.type = type;
            this.params = params;
        }

        /**
         * Returns the type of the constraint
         * 
         * @return The constraint type
         */
        public String getType()
        {
            return this.type;
        }

        /**
         * Returns the parameters for the constraint
         * 
         * @return Map of parameters for the constraint or null if
         *         there are no parameters
         */
        public Map<String, String> getParams()
        {
            return this.params;
        }
    }
}