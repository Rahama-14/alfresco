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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * A default {@link ChildApplicationContextManager} implementation that manages a 'chain' of
 * {@link ChildApplicationContextFactory} objects, perhaps corresponding to the components of a chained subsystem such
 * as authentication. As with other {@link PropertyBackedBean}s, can be stopped, reconfigured, started and tested. Its
 * one special <code>chain</code> property allows an ordered list of {@link ChildApplicationContextFactory} objects to
 * be managed. This property is a comma separated list with the format:
 * <ul>
 * <li>&lt;id1>:&lt;typeName1>,&lt;id2>:&lt;typeName2>,...,&lt;id<i>n</i>>:&lt;typeName<i>n</i>>
 * </ul>
 * See {@link ChildApplicationContextManager} for the meanings of &lt;id> and &lt;typeName>. In the enterprise edition,
 * this property is editable at runtime via JMX. If a new &lt;id> is included in the list then a new
 * {@link ChildApplicationContextFactory} will be brought into existence. Similarly, if one is removed from the list,
 * then the corresponding instance will be destroyed. For Alfresco community edition, the chain is best configured
 * through the {@link #setDefaultChain(String)} method via Spring configuration.
 * 
 * @author dward
 */
public class DefaultChildApplicationContextManager extends AbstractPropertyBackedBean implements
        ApplicationContextAware, ApplicationListener, ChildApplicationContextManager
{

    /** The name of the special property that holds the ordering of child instance names. */
    private static final String ORDER_PROPERTY = "chain";

    /** The parent. */
    private ApplicationContext parent;

    /** The default type name. */
    private String defaultTypeName;

    /** The default chain. */
    private String defaultChain;

    /** The instance ids. */
    private List<String> instanceIds = new ArrayList<String>(10);

    /** The child application contexts. */
    private Map<String, ChildApplicationContextFactory> childApplicationContexts = new TreeMap<String, ChildApplicationContextFactory>();

    /** The auto start. */
    private boolean autoStart;

    /**
     * Instantiates a new default child application context manager.
     */
    public DefaultChildApplicationContextManager()
    {
        setId("manager");
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.
     * ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.parent = applicationContext;
    }

    /**
     * Sets the default type name. This is used when a type name is not included after an instance ID in a chain string.
     * 
     * @param defaultTypeName
     *            the new default type name
     */
    public void setDefaultTypeName(String defaultTypeName)
    {
        this.defaultTypeName = defaultTypeName;
    }

    /**
     * Configures the default chain of {@link ChildApplicationContextFactory} instances. May be set on initialization by
     * the Spring container.
     * 
     * @param defaultChain
     *            a comma separated list in the following format:
     *            <ul>
     *            <li>&lt;id1>:&lt;typeName1>,&lt;id2>:&lt;typeName2>,...,&lt;id<i>n</i>>:&lt;typeName<i>n</i>>
     *            </ul>
     */
    public void setDefaultChain(String defaultChain)
    {
        this.defaultChain = defaultChain;
    }

    /**
     * Indicates whether all child application contexts should be started on startup of the parent application context.
     * 
     * @param autoStart
     *            <code>true</code> if all child application contexts should be started on startup of the parent
     *            application context
     */
    public void setAutoStart(boolean autoStart)
    {
        this.autoStart = autoStart;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.AbstractPropertyBackedBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        if (this.defaultChain != null && this.defaultChain.length() > 0)
        {
            // Use the first type as the default, unless one is specified explicitly
            if (this.defaultTypeName == null)
            {
                updateOrder(this.defaultChain, AbstractPropertyBackedBean.DEFAULT_ID);
                this.defaultTypeName = this.childApplicationContexts.get(this.instanceIds.get(0)).getTypeName();
            }
            else
            {
                updateOrder(this.defaultChain, this.defaultTypeName);
            }
        }
        else if (this.defaultTypeName == null)
        {
            setDefaultTypeName(AbstractPropertyBackedBean.DEFAULT_ID);
        }

        super.afterPropertiesSet();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#start()
     */
    public void start()
    {
        // Nothing to do
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#stop()
     */
    public void stop()
    {
        // Nothing to do
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.AbstractPropertyBackedBean#destroy(boolean)
     */
    @Override
    public void destroy(boolean permanent)
    {
        super.destroy(permanent);

        // Cascade the destroy / shutdown
        for (String id : this.instanceIds)
        {
            ChildApplicationContextFactory factory = this.childApplicationContexts.get(id);
            factory.destroy(permanent);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#getProperty(java.lang.String)
     */
    public synchronized String getProperty(String name)
    {
        if (!name.equals(DefaultChildApplicationContextManager.ORDER_PROPERTY))
        {
            return null;
        }
        return getOrderString();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#getPropertyNames()
     */
    public Set<String> getPropertyNames()
    {
        return Collections.singleton(DefaultChildApplicationContextManager.ORDER_PROPERTY);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#setProperty(java.lang.String, java.lang.String)
     */
    public synchronized void setProperty(String name, String value)
    {
        if (!name.equals(DefaultChildApplicationContextManager.ORDER_PROPERTY))
        {
            throw new IllegalStateException("Illegal attempt to write to property \"" + name + "\"");
        }
        updateOrder(value, this.defaultTypeName);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.ChildApplicationContextManager#getInstanceIds()
     */
    public synchronized Collection<String> getInstanceIds()
    {
        return Collections.unmodifiableList(this.instanceIds);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.ChildApplicationContextManager#getApplicationContext(java.lang.String)
     */
    public synchronized ApplicationContext getApplicationContext(String id)
    {
        ChildApplicationContextFactory child = this.childApplicationContexts.get(id);
        return child == null ? null : child.getApplicationContext();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
     */
    public void onApplicationEvent(ApplicationEvent event)
    {
        if (this.autoStart && event instanceof ContextRefreshedEvent && event.getSource() == this.parent)
        {
            for (String instance : getInstanceIds())
            {
                getApplicationContext(instance);
            }
        }
    }

    /**
     * Gets the order string.
     * 
     * @return the order string
     */
    private String getOrderString()
    {
        StringBuilder orderString = new StringBuilder(100);
        for (String id : this.instanceIds)
        {
            if (orderString.length() > 0)
            {
                orderString.append(",");
            }
            orderString.append(id).append(':').append(this.childApplicationContexts.get(id).getTypeName());
        }
        return orderString.toString();
    }

    /**
     * Updates the order from a comma or whitespace separated string.
     * 
     * @param orderString
     *            the order as a comma or whitespace separated string
     * @param defaultTypeName
     *            the default type name
     */
    private void updateOrder(String orderString, String defaultTypeName)
    {
        try
        {
            StringTokenizer tkn = new StringTokenizer(orderString, ", \t\n\r\f");
            List<String> newInstanceIds = new ArrayList<String>(tkn.countTokens());
            while (tkn.hasMoreTokens())
            {
                String instance = tkn.nextToken();
                int sepIndex = instance.indexOf(':');
                String id = sepIndex == -1 ? instance : instance.substring(0, sepIndex);
                String typeName = sepIndex == -1 || sepIndex + 1 >= instance.length() ? defaultTypeName : instance
                        .substring(sepIndex + 1);
                newInstanceIds.add(id);

                // Look out for new or updated children
                ChildApplicationContextFactory factory = this.childApplicationContexts.get(id);

                // If we have the same instance ID but a different type, treat that as a destroy and remove
                if (factory != null && !factory.getTypeName().equals(typeName))
                {
                    factory.destroy(true);
                    factory = null;
                }
                if (factory == null)
                {
                    this.childApplicationContexts.put(id, new ChildApplicationContextFactory(this.parent,
                            getRegistry(), getCategory(), typeName, "managed$" + id));
                }
            }

            // Destroy any children that have been removed
            Set<String> idsToRemove = new TreeSet<String>(this.childApplicationContexts.keySet());
            idsToRemove.removeAll(newInstanceIds);
            for (String id : idsToRemove)
            {
                ChildApplicationContextFactory factory = this.childApplicationContexts.remove(id);
                factory.destroy(true);
            }
            this.instanceIds = newInstanceIds;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
