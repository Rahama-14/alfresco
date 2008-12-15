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
package org.alfresco.repo.dictionary.constraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.ConstraintException;
import org.alfresco.service.cmr.dictionary.DictionaryException;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;

/**
 * Constraint implementation that ensures the value is one of a constrained
 * <i>list of values</i>.  By default, this constraint is case-sensitive.
 * 
 * @see #setAllowedValues(List)
 * @see #setCaseSensitive(boolean)
 * 
 * @author Derek Hulley
 */
public class ListOfValuesConstraint extends AbstractConstraint
{
    private static final String ERR_NO_VALUES = "d_dictionary.constraint.list_of_values.no_values";
    private static final String ERR_NON_STRING = "d_dictionary.constraint.string_length.non_string";
    private static final String ERR_INVALID_VALUE = "d_dictionary.constraint.list_of_values.invalid_value";

    private List<String> allowedValues;
    private List<String> allowedValuesUpper;
    private boolean caseSensitive;
    
    public ListOfValuesConstraint()
    {
        caseSensitive = true;
    }
    
    /*
     * @see org.alfresco.service.cmr.dictionary.Constraint#getType()
     */
    @Override
    public String getType()
    {
        return "LIST";
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(80);
        sb.append("ListOfValuesConstraint")
          .append("[ allowedValues=").append(allowedValues)
          .append(", caseSensitive=").append(caseSensitive)
          .append("]");
        return sb.toString();
    }

    /**
     * Get the allowed values.  Note that these are <tt>String</tt> instances, but may 
     * represent non-<tt>String</tt> values.  It is up to the caller to distinguish.
     * 
     * @return Returns the values allowed
     */
    public List<String> getAllowedValues()
    {
        return allowedValues;
    }
    
    /**
     * Set the values that are allowed by the constraint.
     *  
     * @param values a list of allowed values
     */
    @SuppressWarnings("unchecked")
    public void setAllowedValues(List allowedValues)
    {
        if (allowedValues == null)
        {
            throw new DictionaryException(ERR_NO_VALUES);
        }
        int valueCount = allowedValues.size();
        if (valueCount == 0)
        {
            throw new DictionaryException(ERR_NO_VALUES);
        }
        this.allowedValues = Collections.unmodifiableList(allowedValues);
        // make the upper case versions
        this.allowedValuesUpper = new ArrayList<String>(valueCount);
        for (String allowedValue : this.allowedValues)
        {
            allowedValuesUpper.add(allowedValue.toUpperCase());
        }
    }

    /**
     * @return Returns <tt>true</tt> if this constraint is case-sensitive (default)
     */
    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }

    /**
     * Set the handling of case checking.
     * 
     * @param caseSensitive <tt>true</tt> if the constraint is case-sensitive (default),
     *      or <tt>false</tt> for case-insensitive.
     */
    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }

    public void initialize()
    {
        checkPropertyNotNull("allowedValues", allowedValues);
    }

    protected void evaluateSingleValue(Object value)
    {
        // convert the value to a String
        String valueStr = null;
        try
        {
            valueStr = DefaultTypeConverter.INSTANCE.convert(String.class, value);
        }
        catch (TypeConversionException e)
        {
            throw new ConstraintException(ERR_NON_STRING, value);
        }
        // check that the value is in the set of allowed values
        if (caseSensitive)
        {
            if (!allowedValues.contains(valueStr))
            {
                throw new ConstraintException(ERR_INVALID_VALUE, value);
            }
        }
        else
        {
            if (!allowedValuesUpper.contains(valueStr.toUpperCase()))
            {
                throw new ConstraintException(ERR_INVALID_VALUE, value);
            }
        }
    }
    
    /*
     * @see org.alfresco.service.cmr.dictionary.Constraint#getParameters()
     */
    public Map<String, Object> getParameters()
    {
        Map<String, Object> params = new HashMap<String, Object>(2);
        
        params.put("caseSensitive", this.caseSensitive);
        params.put("allowedValues", this.allowedValues);
        
        return params;
    }
}
