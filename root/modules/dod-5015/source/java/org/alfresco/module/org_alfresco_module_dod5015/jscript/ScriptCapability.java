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
package org.alfresco.module.org_alfresco_module_dod5015.jscript;

/**
 * @author Roy Wetherall
 */
public class ScriptCapability
{
    private String name;
    private String displayLabel;
    private String[] actions;
    
    /**
     * @param name
     * @param displayLabel
     * @param actions
     */
    protected ScriptCapability(String name, String displayLabel, String[] actions)
    {
        this.name = name;
        this.displayLabel = displayLabel;
        this.actions = actions;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * @return the displayLabel
     */
    public String getDisplayLabel()
    {
        return displayLabel;
    }
    
    /**
     * @return the actions
     */
    public String[] getActions()
    {
        return actions;
    }
}
