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
package org.alfresco.repo.audit;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.alfresco.service.PublicService;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;

/**
 * Identify public services by method invocation. Look ups are cached on a thread local as they are quite expensive.
 * All public service names end with "Service" and start with capital letter.
 * This pattern is used to filter bean names. TODO: Look at pulling out all the mappings at start up.
 * 
 * @author Andy Hind
 */
public class PublicServiceIdentifierImpl implements PublicServiceIdentifier, BeanFactoryAware
{
    private static Log s_logger = LogFactory.getLog(PublicServiceIdentifierImpl.class);
    private static ThreadLocal<HashMap<Method, String>> methodToServiceMap = new ThreadLocal<HashMap<Method, String>>();

    private ListableBeanFactory beanFactory;

    public PublicServiceIdentifierImpl()
    {
        super();
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = (ListableBeanFactory)beanFactory;
    }

    public String getPublicServiceName(MethodInvocation mi)
    {
        return getServiceName(mi);
    }

    /**
     * {@inheritDoc}
     * Cache service name look up.
     */
    private String getServiceName(MethodInvocation mi) throws BeansException
    {
        if (methodToServiceMap.get() == null)
        {
            methodToServiceMap.set(new HashMap<Method, String>());
        }
        Method method = mi.getMethod();
        String serviceName = methodToServiceMap.get().get(method);
        if (serviceName == null)
        {
            serviceName = getServiceNameImpl(mi);
            methodToServiceMap.get().put(method, serviceName);
        }
        else
        {
            if (s_logger.isDebugEnabled())
            {
                s_logger.debug("Cached look up for " + serviceName + "." + method.getName());
            }
        }
        return serviceName;
    }

    /**
     * Do the look up by interface type.
     * 
     * @return              Returns the name of the service or <tt>null</tt> if not found
     */
    @SuppressWarnings("unchecked")
    private String getServiceNameImpl(MethodInvocation mi) throws BeansException
    {
        Class clazz = mi.getThis().getClass();
        while (clazz != null)
        {
            Class[] interfaces = clazz.getInterfaces();
            for (Class iFace : interfaces)
            {
                Class publicServiceInterface = findPublicService(iFace);
                if (publicServiceInterface != null)
                {
                    Map beans = beanFactory.getBeansOfType(publicServiceInterface);
                    Iterator iter = beans.entrySet().iterator();
                    while (iter.hasNext())
                    {
                        Map.Entry entry = (Map.Entry) iter.next();
                        String serviceName = (String) entry.getKey();
                        if ((serviceName.endsWith("Service"))
                                && (Character.isUpperCase(serviceName.charAt(0)))
                                && !serviceName.equals("DescriptorService"))
                        {
                            return serviceName;
                        }
                    }
                }

            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * We use a marker annotation to identify public interfaces.
     * The interfaces have to be walked to determine if a public interface is implemented.
     * 
     * Only one public service interface is expected.
     * 
     * @param clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    private Class findPublicService(Class clazz)
    {
        if (!clazz.isInterface())
        {
            return null;
        }
        
        if (clazz.isAnnotationPresent(PublicService.class))
        {
            return clazz;
        }

        Class[] classes = clazz.getInterfaces();
        for(Class implemented: classes)
        {
            Class answer = findPublicService(implemented);
            if(answer != null)
            {
                return answer;
            }
        }
        return null;

    }
}
