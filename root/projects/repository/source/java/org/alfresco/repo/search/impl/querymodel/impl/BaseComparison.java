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
package org.alfresco.repo.search.impl.querymodel.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.alfresco.repo.search.impl.querymodel.Argument;
import org.alfresco.repo.search.impl.querymodel.ArgumentDefinition;
import org.alfresco.repo.search.impl.querymodel.FunctionArgument;
import org.alfresco.repo.search.impl.querymodel.Multiplicity;
import org.alfresco.repo.search.impl.querymodel.PropertyArgument;
import org.alfresco.repo.search.impl.querymodel.QueryModelException;
import org.alfresco.repo.search.impl.querymodel.StaticArgument;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Lower;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Upper;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.namespace.QName;

/**
 * @author andyh
 */
public abstract class BaseComparison extends BaseFunction
{
    /**
     * Left hand side
     */
    public final static String ARG_LHS = "LHS";

    /**
     * Right hand side
     */
    public final static String ARG_RHS = "RHS";

    /**
     * Args
     */
    public static LinkedHashMap<String, ArgumentDefinition> ARGS;

    private PropertyArgument propertyArgument;

    private StaticArgument staticArgument;

    private FunctionArgument functionArgument;

    static
    {
        ARGS = new LinkedHashMap<String, ArgumentDefinition>();
        ARGS.put(ARG_LHS, new BaseArgumentDefinition(Multiplicity.ANY, ARG_LHS, DataTypeDefinition.ANY, true));
        ARGS.put(ARG_RHS, new BaseArgumentDefinition(Multiplicity.ANY, ARG_RHS, DataTypeDefinition.ANY, true));
    }

    /**
     * @param name
     * @param returnType
     * @param argumentDefinitions
     */
    public BaseComparison(String name, QName returnType, LinkedHashMap<String, ArgumentDefinition> argumentDefinitions)
    {
        super(name, returnType, argumentDefinitions);
    }

    protected void setPropertyAndStaticArguments(Map<String, Argument> functionArgs)
    {
        Argument lhs = functionArgs.get(ARG_LHS);
        Argument rhs = functionArgs.get(ARG_RHS);

        if (lhs instanceof PropertyArgument)
        {
            if ((rhs instanceof PropertyArgument) || (rhs instanceof FunctionArgument))
            {
                throw new QueryModelException("Implicit join is not supported");
            }
            else if (rhs instanceof StaticArgument)
            {
                propertyArgument = (PropertyArgument) lhs;
                staticArgument = (StaticArgument) rhs;
            }
            else
            {
                throw new QueryModelException("Argument of type " + rhs.getClass().getName() + " is not supported");
            }
        }
        else if (lhs instanceof FunctionArgument)
        {
            if ((rhs instanceof PropertyArgument) || (rhs instanceof FunctionArgument))
            {
                throw new QueryModelException("Implicit join is not supported");
            }
            else if (rhs instanceof StaticArgument)
            {
                functionArgument = (FunctionArgument) lhs;
                staticArgument = (StaticArgument) rhs;
            }
            else
            {
                throw new QueryModelException("Argument of type " + rhs.getClass().getName() + " is not supported");
            }
        }
        else if (rhs instanceof PropertyArgument)
        {
            if ((lhs instanceof PropertyArgument) || (lhs instanceof FunctionArgument))
            {
                throw new QueryModelException("Implicit join is not supported");
            }
            else if (lhs instanceof StaticArgument)
            {
                propertyArgument = (PropertyArgument) rhs;
                staticArgument = (StaticArgument) lhs;
            }
            else
            {
                throw new QueryModelException("Argument of type " + lhs.getClass().getName() + " is not supported");
            }
        }
        else if (rhs instanceof FunctionArgument)
        {
            if ((lhs instanceof PropertyArgument) || (lhs instanceof FunctionArgument))
            {
                throw new QueryModelException("Implicit join is not supported");
            }
            else if (lhs instanceof StaticArgument)
            {
                functionArgument = (FunctionArgument) rhs;
                staticArgument = (StaticArgument) lhs;
            }
            else
            {
                throw new QueryModelException("Argument of type " + lhs.getClass().getName() + " is not supported");
            }
        }
        else
        {
            throw new QueryModelException("Equals must have one property argument");
        }
    }

    /**
     * @return the propertyArgument - there must be a property argument of a function argument
     */
    protected PropertyArgument getPropertyArgument()
    {
        return propertyArgument;
    }

    /**
     * @return the staticArgument - must be set
     */
    protected StaticArgument getStaticArgument()
    {
        return staticArgument;
    }

    /**
     * @return the functionArgument
     */
    protected FunctionArgument getFunctionArgument()
    {
        return functionArgument;
    }

    protected String getPropertyName()
    {
        if (propertyArgument != null)
        {
            return propertyArgument.getPropertyName();
        }
        else if (functionArgument != null)
        {
            String functionName = functionArgument.getFunction().getName();
            if (functionName.equals(Upper.NAME))
            {
                Argument arg = functionArgument.getFunctionArguments().get(Upper.ARG_ARG);
                if (arg instanceof PropertyArgument)
                {
                    return ((PropertyArgument) arg).getPropertyName();
                }
                else
                {
                    throw new QueryModelException("Upper must have a column argument " + arg);
                }
            }
            else if (functionName.equals(Lower.NAME))
            {
                Argument arg = functionArgument.getFunctionArguments().get(Lower.ARG_ARG);
                if (arg instanceof PropertyArgument)
                {
                    return ((PropertyArgument) arg).getPropertyName();
                }
                else
                {
                    throw new QueryModelException("Lower must have a column argument " + arg);
                }
            }
            else
            {
                throw new QueryModelException("Unsupported function: " + functionName);
            }
        }
        else
        {
            throw new QueryModelException("A property of function argument must be provided");
        }
    }

}
