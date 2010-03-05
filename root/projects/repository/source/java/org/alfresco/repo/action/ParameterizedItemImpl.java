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
package org.alfresco.repo.action;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.action.ParameterizedItem;

/**
 * Rule item instance implementation class.
 * 
 * @author Roy Wetherall
 */
@SuppressWarnings("serial")
public abstract class ParameterizedItemImpl implements ParameterizedItem, Serializable
{
	/**
	 * The id
	 */
	private String id;
	
    /**
     * The parameter values
     */
    private Map<String, Serializable> parameterValues = new HashMap<String, Serializable>();

    /**
     * Constructor
     * 
     * @param ruleItem  the rule item
     */
    public ParameterizedItemImpl(String id)
    {
        this(id, null);
    }
    
    /**
     * Constructor
     * 
     * @param ruleItem          the rule item
     * @param parameterValues   the parameter values
     */
    public ParameterizedItemImpl(String id, Map<String, Serializable> parameterValues)
    {     
    	// Set the action id
    	this.id = id;
    	
        if (parameterValues != null)
		{
	        // TODO need to check that the parameter values being set correspond
	        // correctly to the parameter definions on the rule item defintion
	        this.parameterValues = parameterValues;
		}
    }
    
    /**
     * @see org.alfresco.service.cmr.action.ParameterizedItem#getId()
     */
    public String getId()
    {
	    return this.id;
    }

    /**
     * @see org.alfresco.service.cmr.action.ParameterizedItem#getParameterValues()
     */
    public Map<String, Serializable> getParameterValues()
    {
        Map<String, Serializable> result = this.parameterValues;
        if (result == null)
        {
            result = new HashMap<String, Serializable>();
        }
        return result;
    }
	
	/**
	 * @see org.alfresco.service.cmr.action.ParameterizedItem#getParameterValue(String)
	 */
	public Serializable getParameterValue(String name)
	{
		return this.parameterValues.get(name);
	}
	
	/**
     * @see org.alfresco.service.cmr.action.ParameterizedItem#setParameterValues(java.util.Map)
     */
    public void setParameterValues(Map<String, Serializable> parameterValues)
    {
		if (parameterValues != null)
		{
			// TODO need to check that the parameter values being set correspond
			//      correctly to the parameter definions on the rule item defintion
			this.parameterValues = parameterValues;
		}
    }
	
	/**
	 * @see org.alfresco.service.cmr.action.ParameterizedItem#setParameterValue(String, Serializable)
	 */
	public void setParameterValue(String name, Serializable value)
	{
		this.parameterValues.put(name, value);
	}
	
	/**
	 * Hash code implementation
	 */
	@Override
	public int hashCode()
	{
		return this.id.hashCode(); 
	}
	
	/**
	 * Equals implementation
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
        {
            return true;
        }
        if (obj instanceof ParameterizedItemImpl)
        {
        	ParameterizedItemImpl that = (ParameterizedItemImpl) obj;
            return (this.id.equals(that.id));
        }
        else
        {
            return false;
        }
	}
}
