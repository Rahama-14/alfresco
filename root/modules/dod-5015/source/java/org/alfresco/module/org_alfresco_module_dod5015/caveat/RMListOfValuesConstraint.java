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
package org.alfresco.module.org_alfresco_module_dod5015.caveat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.ConstraintException;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;

/**
 * RM Constraint implementation that ensures the value is one of a constrained
 * <i>list of values</i>.  By default, this constraint is case-sensitive.
 * 
 * @see #setAllowedValues(List)
 * @see #setCaseSensitive(boolean)
 * 
 * @author janv
 */
public class RMListOfValuesConstraint extends ListOfValuesConstraint
{
    //private static final String ERR_NO_VALUES = "d_dictionary.constraint.list_of_values.no_values";
    private static final String ERR_NON_STRING = "d_dictionary.constraint.string_length.non_string";
    private static final String ERR_INVALID_VALUE = "d_dictionary.constraint.list_of_values.invalid_value";
    
    private List<String> allowedValues;
    private List<String> allowedValuesUpper;
    private MatchLogic matchLogic = MatchLogic.AND; // defined match logic used by caveat matching (default = "AND") 
    
    public enum MatchLogic
    {
        AND, // closed marking - all values must match
        OR;  // open marking   - at least one value must match
    }
    
    // note: alternative to static init could be to use 'registered' constraint
    private static RMCaveatConfigService caveatConfigService;
    
    public void setCaveatConfigService(RMCaveatConfigService caveatConfigService)
    {
        RMListOfValuesConstraint.caveatConfigService = caveatConfigService;
    }
    
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(80);
        sb.append("RMListOfValuesConstraint")
          .append("[allowedValues=").append(getAllowedValues())
          .append(", caseSensitive=").append(isCaseSensitive())
          .append(", matchLogic=").append(getMatchLogic())
          .append("]");
        return sb.toString();
    }

    /**
     * Get the allowed values.  Note that these are <tt>String</tt> instances, but may 
     * represent non-<tt>String</tt> values.  It is up to the caller to distinguish.
     * 
     * @return Returns the values allowed
     */
    @Override
    public List<String> getAllowedValues()
    {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        if ((runAsUser != null) && (! runAsUser.equals(AuthenticationUtil.getSystemUserName())) && (caveatConfigService != null))
        {
            List<String> allowedForUser = caveatConfigService.getRMAllowedValues(getShortName()); // get allowed values for current user
            
            List<String> filteredList = new ArrayList<String>(allowedForUser.size());
            for (String allowed : allowedForUser)
            {
                if (this.allowedValues.contains(allowed))
                {
                    filteredList.add(allowed);
                }
            }
            
            return filteredList;
        }
        else
        {
            return this.allowedValues;
        }
    }
    
    private List<String> getAllowedValuesUpper()
    {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        if ((runAsUser != null) && (! runAsUser.equals(AuthenticationUtil.getSystemUserName())) && (caveatConfigService != null))
        {
            List<String> allowedForUser = caveatConfigService.getRMAllowedValues(getType()); // get allowed values for current user
            
            List<String> filteredList = new ArrayList<String>(allowedForUser.size());
            for (String allowed : allowedForUser)
            {
                if (this.allowedValuesUpper.contains(allowed.toUpperCase()))
                {
                    filteredList.add(allowed);
                }
            }
            
            return filteredList;
        }
        else
        {
            return this.allowedValuesUpper;
        }
    }
    /**
     * Set the values that are allowed by the constraint.
     *  
     * @param values a list of allowed values
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setAllowedValues(List allowedValues)
    {
        if (allowedValues == null)
        {
            allowedValues = new ArrayList<String>(0);
        }
        int valueCount = allowedValues.size();
        this.allowedValues = Collections.unmodifiableList(allowedValues);
        
        // make the upper case versions
        this.allowedValuesUpper = new ArrayList<String>(valueCount);
        for (String allowedValue : this.allowedValues)
        {
            allowedValuesUpper.add(allowedValue.toUpperCase());
        }
    }
    
    @Override
    public void initialize()
    {
        checkPropertyNotNull("allowedValues", allowedValues);
    }
    
    @Override
    public Map<String, Object> getParameters()
    {
        Map<String, Object> params = new HashMap<String, Object>(2);
        
        params.put("caseSensitive", isCaseSensitive());
        params.put("allowedValues", getAllowedValues());
        
        return params;
    }
    
    public MatchLogic getMatchLogicEnum()
    {
        return matchLogic;
    }
    
    public String getMatchLogic()
    {
        return matchLogic.toString();
    }
    
    public void setMatchLogic(String matchLogicStr)
    {
        this.matchLogic = MatchLogic.valueOf(matchLogicStr);
    }
    
    @Override
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
        if (isCaseSensitive())
        {
            if (!getAllowedValues().contains(valueStr))
            {
                throw new ConstraintException(ERR_INVALID_VALUE, value);
            }
        }
        else
        {
            if (!getAllowedValuesUpper().contains(valueStr.toUpperCase()))
            {
                throw new ConstraintException(ERR_INVALID_VALUE, value);
            }
        }
    }
}
