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
package org.alfresco.abdera.ext.cmis;

import java.util.ArrayList;
import java.util.List;

import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.ExtensibleElementWrapper;


/**
 * CMIS Version: 0.61
 *
 * CMIS Properties Element Wrapper for the Abdera ATOM library.
 * 
 * @author davidc
 */
public class CMISProperties extends ExtensibleElementWrapper
{
    /**
     * @param internal
     */
    public CMISProperties(Element internal)
    {
        super(internal);
    }

    /**
     * @param factory
     */
    public CMISProperties(Factory factory)
    {
        super(factory, CMISConstants.PROPERTIES);
    }

    /**
     * Gets all property ids
     * 
     * @return  list of property ids
     */
    public List<String> getIds()
    {
        List<CMISProperty> props = getElements();
        List<String> ids = new ArrayList<String>(props.size());
        for (CMISProperty prop : props)
        {
            ids.add(prop.getId());
        }
        return ids;
    }
    
    /**
     * Finds property by id
     * 
     * @param id  property id
     * @return  property
     */
    public CMISProperty find(String id)
    {
        List<Element> elements = getElements();
        for (Element element : elements)
        {
            if (element instanceof CMISProperty)
            {
                CMISProperty prop = (CMISProperty)element;
                if (id.equals(prop.getId()))
                {
                    return prop;
                }
            }
        }
        return null;
    }

}
