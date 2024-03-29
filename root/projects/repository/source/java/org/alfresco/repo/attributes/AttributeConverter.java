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

package org.alfresco.repo.attributes;

import org.alfresco.repo.avm.AVMDAOs;

/**
 * Handles conversions between persistent and value based Attributes.
 * 
 * @see Attribute.Type#getAttributeImpl(Attribute)
 * @see Attribute.Type#getAttributeValue(Attribute)
 * 
 * @author britt
 */
public class AttributeConverter
{
    /**
     * Convert an Attribute (recursively) to a persistent attribute. This persists
     * the newly created Attribute immediately.
     * @param from The Attribute to clone.
     * @return The cloned persistent Attribute.
     */
    public Attribute toPersistent(Attribute from)
    {
        AttributeImpl attributeEntity = from.getAttributeImpl();
        // Done
        return attributeEntity;
    }

    public Attribute toValue(Attribute from)
    {
        AttributeValue attributeValue = from.getAttributeValue();
        AVMDAOs.Instance().fAttributeDAO.evictFlat(from);
        // Done
        return attributeValue;
    }
}
