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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.alfresco.repo.audit.AuditMode;
import org.alfresco.repo.audit.AuditModel;
import org.alfresco.repo.audit.MethodAuditModel;
import org.alfresco.repo.audit.RecordOptions;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

public class ServiceAuditEntry extends AbstractNamedAuditEntry implements MethodAuditModel
{
    private static Log s_logger = LogFactory.getLog(ServiceAuditEntry.class);

    private Map<String, MethodAuditEntry> methods = new HashMap<String, MethodAuditEntry>();

    public ServiceAuditEntry()
    {
        super();
    }

    @Override
    void configure(AbstractAuditEntry parent, Element element, NamespacePrefixResolver namespacePrefixResolver)
    {
        super.configure(parent, element, namespacePrefixResolver);

        // Add Methods

        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Adding methods to service " + getName());
        }
        for (Iterator nsit = element.elementIterator(AuditModel.EL_METHOD); nsit.hasNext(); /**/)
        {
            Element methodElement = (Element) nsit.next();
            MethodAuditEntry method = new MethodAuditEntry();
            method.configure(this, methodElement, namespacePrefixResolver);
            methods.put(method.getName(), method);
        }
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("...added methods for service " + getName());
        }
    }

    public AuditMode beforeExecution(AuditMode auditMode, MethodInvocation mi)
    {
        String methodName = mi.getMethod().getName();
        MethodAuditEntry method = methods.get(methodName);
        if (method != null)
        {
            return method.beforeExecution(auditMode, mi);
        }
        else
        {
            if (s_logger.isDebugEnabled())
            {
                s_logger.debug("Evaluating if service is audited (no specific setting) for " + getName() + "." + methodName);
            }
            return getEffectiveAuditMode();
        }
    }

    public AuditMode afterExecution(AuditMode auditMode, MethodInvocation mi)
    {
        throw new UnsupportedOperationException();
    }

    public AuditMode onError(AuditMode auditMode, MethodInvocation mi)
    {
        throw new UnsupportedOperationException();
    }

    public RecordOptions getAuditRecordOptions(MethodInvocation mi)
    {
        return getEffectiveRecordOptions();
    }

    public TrueFalseUnset getAuditInternalServiceMethods(MethodInvocation mi)
    {
        String methodName = mi.getMethod().getName();
        MethodAuditEntry method = methods.get(methodName);
        if (method != null)
        {
            return method.getAuditInternalServiceMethods(mi);
        }
        else
        {
            if (s_logger.isDebugEnabled())
            {
                s_logger.debug("Evaluating if service is internally audited (no specific setting) for " + getName() + "." + methodName);
            }
            return getEffectiveAuditInternal();
        }
    }
    
    public MethodAuditEntry getMethodAuditEntry(String name)
    {
        return methods.get(name);
    }

}
