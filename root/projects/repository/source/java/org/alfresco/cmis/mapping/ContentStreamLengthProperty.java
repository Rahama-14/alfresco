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
package org.alfresco.cmis.mapping;

import java.io.Serializable;

import org.alfresco.cmis.CMISDictionaryModel;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;

/**
 * Accessor for CMIS content stream length property
 * 
 * @author andyh
 */
public class ContentStreamLengthProperty extends AbstractSimpleProperty
{
    /**
     * Construct
     * 
     * @param serviceRegistry
     */
    public ContentStreamLengthProperty(ServiceRegistry serviceRegistry)
    {
        super(serviceRegistry, CMISDictionaryModel.PROP_CONTENT_STREAM_LENGTH);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyAccessor#getValue(org.alfresco.service.cmr.repository.NodeRef)
     */
    public Serializable getValue(NodeRef nodeRef)
    {
        Serializable value = getServiceRegistry().getNodeService().getProperty(nodeRef, ContentModel.PROP_CONTENT);
        if (value != null)
        {
            ContentData contentData = DefaultTypeConverter.INSTANCE.convert(ContentData.class, value);
            return contentData.getSize();
        }
        else
        {
            return 0L;
        }
    }
    
    public String getLuceneFieldName()
    {
        StringBuilder field = new StringBuilder(128);
        field.append("@");
        field.append(ContentModel.PROP_CONTENT);
        field.append(".size");
        return field.toString();
    }

    protected String getValueAsString(Serializable value)
    {
        Object converted = DefaultTypeConverter.INSTANCE.convert(getServiceRegistry().getDictionaryService().getDataType(DataTypeDefinition.LONG), value);
        String asString = DefaultTypeConverter.INSTANCE.convert(String.class, converted);
        return asString;
    }

    protected QName getQNameForExists()
    {
        return ContentModel.PROP_CONTENT;
    }
   

    protected DataTypeDefinition getInDataType()
    {
        return getServiceRegistry().getDictionaryService().getDataType(DataTypeDefinition.LONG);
    }
}
