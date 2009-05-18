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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.management.subsystems;

import java.util.Collections;
import java.util.Set;

import org.springframework.context.ApplicationContext;

/**
 * A configurable proxy for a set of {@link ApplicationContextFactory} beans that allows dynamic selection of one or
 * more alternative subsystems via a <code>sourceBeanName</code> property. As with other {@link PropertyBackedBean}s,
 * can be stopped, reconfigured, started and tested.
 */
public class SwitchableApplicationContextFactory extends AbstractPropertyBackedBean implements
        ApplicationContextFactory
{
    /**
     * 
     */
    private static final String SOURCE_BEAN_PROPERTY = "sourceBeanName";

    /** The bean name of the source {@link ApplicationContextFactory}. */
    private String sourceBeanName;

    /** The current source application context factory. */
    private ApplicationContextFactory sourceApplicationContextFactory;

    /**
     * Sets the bean name of the source {@link ApplicationContextFactory}.
     * 
     * @param sourceBeanName
     *            the bean name
     * @throws Exception
     *             on error
     */
    public synchronized void setSourceBeanName(String sourceBeanName)
    {
        if (this.sourceApplicationContextFactory != null)
        {
            stop();
            this.sourceBeanName = sourceBeanName;
            start();
        }
        else
        {
            this.sourceBeanName = sourceBeanName;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.enterprise.repo.management.ConfigurableBean#onStart()
     */
    public synchronized void start()
    {
        if (this.sourceApplicationContextFactory == null)
        {
            this.sourceApplicationContextFactory = (ApplicationContextFactory) getParent().getBean(this.sourceBeanName);
            this.sourceApplicationContextFactory.start();
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.SelfDescribingBean#onStop()
     */
    public void stop()
    {
        if (this.sourceApplicationContextFactory != null)
        {
            try
            {
                this.sourceApplicationContextFactory.stop();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        this.sourceApplicationContextFactory = null;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.ManagedApplicationContextFactory#getApplicationContext()
     */
    public synchronized ApplicationContext getApplicationContext()
    {
        if (this.sourceApplicationContextFactory == null)
        {
            start();
        }
        return this.sourceApplicationContextFactory.getApplicationContext();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#getProperty(java.lang.String)
     */
    public synchronized String getProperty(String name)
    {
        if (!name.equals(SOURCE_BEAN_PROPERTY))
        {
            return null;
        }
        return this.sourceBeanName;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#getPropertyNames()
     */
    public Set<String> getPropertyNames()
    {
        return Collections.singleton(SOURCE_BEAN_PROPERTY);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#setProperty(java.lang.String, java.lang.String)
     */
    public synchronized void setProperty(String name, String value)
    {
        if (!name.equals(SOURCE_BEAN_PROPERTY))
        {
            throw new IllegalStateException("Illegal attempt to write to property \"" + name + "\"");
        }
        if (!getParent().containsBean(value))
        {
            throw new IllegalStateException("\"" + value + "\" is not a valid bean name");
        }
        setSourceBeanName(value);
    }
}
