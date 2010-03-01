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
package org.alfresco.cmis.acl;

import org.alfresco.cmis.EnumFactory;
import org.alfresco.cmis.EnumLabel;

/**
 * Part two of the allowable action key for the permission mappings
 * @author andyh
 *
 */
public enum CMISAllowedActionKeyTypeEnum implements EnumLabel
{
    /**
     * Folder
     */
    FOLDER("Folder"),
    /**
     * Object
     */
    OBJECT("Object"),
    /**
     * Source
     */
    SOURCE("Source"),
    /**
     * Target
     */
    TARGET("Target"),
    /**
     * Document
     */
    DOCUMENT("Document"),
    /**
     * Policy
     */
    POLICY("Policy");
    
    private String label;

    /**
     * Construct
     * 
     * @param label
     */
    CMISAllowedActionKeyTypeEnum(String label)
    {
        this.label = label;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.EnumLabel#label()
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Factory for CMISAclCapabilityEnum
     */
    public static EnumFactory<CMISAllowedActionKeyTypeEnum> FACTORY = new EnumFactory<CMISAllowedActionKeyTypeEnum>(CMISAllowedActionKeyTypeEnum.class, null, true);

    
    
}
