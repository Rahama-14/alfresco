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
package org.alfresco.repo.audit.model;

import org.alfresco.repo.audit.AuditModel;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

public class ParameterFilter extends KeyFilter implements XMLModelElement 
{
    private static Log s_logger = LogFactory.getLog(ParameterFilter.class);
    
    private QName parameterName;
    
    public ParameterFilter()
    {
        super();
    }

    @Override
    public void configure(Element element, NamespacePrefixResolver namespacePrefixResolver)
    {
        super.configure(element, namespacePrefixResolver);
        
        Element parameterNameElement = element.element(AuditModel.EL_PARAMETER_NAME);
        if(parameterNameElement == null)
        {
            throw new AuditModelException("A parameter is mandatory for a parameter filter");
        }
        else
        {
            String stringQName = parameterNameElement.getStringValue();
            if (stringQName.charAt(1) == '{')
            {
                parameterName = QName.createQName(stringQName);
            }
            else
            {
                parameterName = QName.createQName(stringQName);
            }
        }
    }

    
}
