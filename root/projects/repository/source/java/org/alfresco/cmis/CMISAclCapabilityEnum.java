/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.cmis;

/**
 * CMIS ACL capability
 * 
 * @author andyh
 */
public enum CMISAclCapabilityEnum implements EnumLabel
{
    /**
     * ACLs are not supported
     */
    NONE("none"),
    /**
     * ACLs can be read but not set
     */
    DISCOVER("discover"),
    /**
     * ACLs can be read and set
     */
    MANAGE("manage");

    private String label;

    /**
     * Construct
     * 
     * @param label
     */
    CMISAclCapabilityEnum(String label)
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
    public static EnumFactory<CMISAclCapabilityEnum> FACTORY = new EnumFactory<CMISAclCapabilityEnum>(CMISAclCapabilityEnum.class, MANAGE, true);

}
