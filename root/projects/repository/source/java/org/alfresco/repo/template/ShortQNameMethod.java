/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.repo.template;

import java.util.List;

import org.alfresco.repo.processor.BaseProcessorExtension;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.QName;

import freemarker.ext.beans.BeanModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

/**
 * @author Mike Hatfield
 * 
 * FreeMarker custom method to return the short (prefix) version of a QName.
 * <p>
 * Usage: String shortQname(String longQName)
 */
public final class ShortQNameMethod extends BaseProcessorExtension implements TemplateMethodModelEx
{
    private final static String NAMESPACE_BEGIN = "" + QName.NAMESPACE_BEGIN;

    /* Repository Service Registry */
    private ServiceRegistry services;

    /**
     * Set the service registry
     * 
     * @param serviceRegistry	the service registry
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
    	this.services = serviceRegistry;
    }

    /**
     * @see freemarker.template.TemplateMethodModel#exec(java.util.List)
     */
    public Object exec(List args) throws TemplateModelException
    {
        String result = null;
        
        if (args.size() == 1)
        {
            // arg 0 can be either wrapped QName object or a String 
            String arg0String = null;
            Object arg0 = args.get(0);
            if (arg0 instanceof BeanModel)
            {
                arg0String = ((BeanModel)arg0).getWrappedObject().toString();
            }
            else if (arg0 instanceof TemplateScalarModel)
            {
                arg0String = ((TemplateScalarModel)arg0).getAsString();
            }

            result = createQName(arg0String).toPrefixString(services.getNamespaceService());
        }
        
        return result != null ? result : "";
    }

    /**
     * Helper to create a QName from either a fully qualified or short-name QName string
     * 
     * @param s    Fully qualified or short-name QName string
     * 
     * @return QName
     */
    private QName createQName(String s)
    {
        QName qname;
        if (s.indexOf(NAMESPACE_BEGIN) != -1)
        {
            qname = QName.createQName(s);
        }
        else
        {
            qname = QName.createQName(s, this.services.getNamespaceService());
        }
        return qname;
    }
}
