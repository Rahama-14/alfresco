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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.alfresco.config.JBossEnabledResourcePatternResolver;
import org.alfresco.config.JndiPropertiesFactoryBean;
import org.alfresco.util.config.RepositoryPathConfigBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ListFactoryBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * A factory allowing initialization of an entire 'subsystem' in a child application context. As with other
 * {@link PropertyBackedBean}s, can be stopped, reconfigured, started and tested. Each instance possesses a
 * <code>typeName</code> property, that determines where the factory will look for default configuration for the
 * application context. In addition, its <code>id</code> property will determine where the factory will look for
 * override configuration in the extension classpath.
 * <p>
 * The factory will search for a Spring application context in the classpath using the following patterns in order:
 * <ul>
 * <li>alfresco/subsystems/&lt;category>/&lt;typeName>/*-context.xml</li>
 * <li>alfresco/extension/subsystems/&lt;category>/&lt;typeName>/&lt;id>/*-context.xml</li>
 * </ul>
 * The child application context may use <code>${}</code> placeholders, and will be configured with a
 * {@link PropertyPlaceholderConfigurer} initialized with properties files found in classpath matching the following
 * patterns in order:
 * <ul>
 * <li>alfresco/subsystems/&lt;category>/&lt;typeName>/*.properties</li>
 * <li>alfresco/extension/subsystems/&lt;category>/&lt;typeName>/&lt;id>/*.properties</li>
 * </ul>
 * This means that the extension classpath can be used to provide instance-specific overrides to product default
 * settings. Of course, if you are using the Enterprise edition, you might want to use a JMX client such as JConsole to
 * edit the settings instead!
 * <p>
 * For advanced purposes, the class also allows management of 'composite' properties, that is properties that can be
 * populated with a sequence of zero or more objects, themselves registered as property-backed beans. Using the
 * <code>compositePropertyTypes</code> property you can register a Map of property names to Java Bean classes. Each
 * property named in this map will then be materialized as a 'composite' property.
 * <p>
 * Composite property settings are best controlled either through a JMX console (Enterprise edition only) or
 * /alfresco-global.properties in the classpath (the replacement to /alfresco/extension/custom-repository.properties).
 * For example, suppose "imap.server.mountPoints" was registered as a composite property of type {@link RepositoryPathConfigBean}.
 * You can then use the property to configure a list of {@link RepositoryPathConfigBean}s. First you specify in the property's
 * value a list of zero or more 'instance names'. Each name must be unique within the property.
 * <p>
 * <code>imap.server.mountPoints=Repository_virtual,Repository_archive</code>
 * <p>
 * Then, by magic you have two separate instances of {@link RepositoryPathConfigBean} whose properties you can address through an
 * extended set of properties prefixed by "imap.server.mountPoints".
 * <p>
 * To set a property on one of the instances, you append ".value.&lt;instance name>.&lt;bean property name>" to the
 * parent property name. For example:
 * <p>
 * <code>imap.server.mountPoints.value.Repository_virtual.store=${spaces.store}</code><br/>
 * <code>imap.server.mountPoints.value.Repository_virtual.path=/${spaces.company_home.childname}</code>
 * <p>
 * To specify a default value for a property on all instances of the bean, you append ".default.&lt;bean property name>"
 * to the parent property name. For example:
 * <p>
 * <code>imap.server.mountPoints.default.store=${spaces.store}</code><br/>
 * <code>imap.server.mountPoints.default.path=/${spaces.company_home.childname}</code>
 * <p>
 * Note that it's perfectly valid to use placeholders in property values that will be resolved from other global
 * properties.
 * <p>
 * In order to actually utilize this configurable list of beans in your child application context, you simply need to
 * declare a {@link ListFactoryBean} whose ID is the same name as the property. For example:
 * <pre>
 * &lt;bean id=&quot;imap.server.mountPoints&quot; class=&quot;org.springframework.beans.factory.config.ListFactoryBean&quot;&gt;
 *    &lt;property name=&quot;sourceList&quot;&gt;
 *       &lt;!-- Whatever you declare in here will get replaced by the property value list --&gt;
 *       &lt;!-- This property is not actually required at all --&gt;
 *    &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * Then, when the application context is started and before that bean is initialized, it will be given the current
 * configured list of values for the composite property. Magic! This all sounds like a complex, yet primitive
 * replacement for Spring, but it means you can do powerful things to reconfigure the system through an admin UI rather
 * than editing XML.
 * 
 * @author dward
 */
public class ChildApplicationContextFactory extends AbstractPropertyBackedBean implements ApplicationContextFactory
{

    /** The name of the special read-only property containing the type name. */
    private static final String TYPE_NAME_PROPERTY = "$type";

    /** The suffix to the property file search path. */
    private static final String PROPERTIES_SUFFIX = "/*.properties";

    /** The suffix to the context file search path. */
    private static final String CONTEXT_SUFFIX = "/*-context.xml";

    /** The prefix to default file search paths. */
    private static final String CLASSPATH_PREFIX = "classpath*:alfresco/subsystems/";

    /** The prefix to extension file search paths. */
    private static final String EXTENSION_CLASSPATH_PREFIX = "classpath*:alfresco/extension/subsystems/";

    /** The logger. */
    private static Log logger = LogFactory.getLog(ChildApplicationContextFactory.class);

    /** The properties to be used in placeholder expansion. */
    private Properties properties;

    /** The child application context. */
    private ClassPathXmlApplicationContext applicationContext;

    /** The type name. */
    private String typeName;

    /** The registered composite propertes and their types. */
    private Map<String, Class<?>> compositePropertyTypes = Collections.emptyMap();

    /** The composite property values. */
    private Map<String, Map<String, CompositeDataBean>> compositeProperties = new TreeMap<String, Map<String, CompositeDataBean>>();

    /**
     * Default constructor for container construction.
     */
    protected ChildApplicationContextFactory()
    {
    }

    /**
     * Constructor for dynamically created instances, e.g. through {@link DefaultChildApplicationContextManager}.
     * 
     * @param parent
     *            the parent application context
     * @param registry
     *            the registry of property backed beans
     * @param propertyDefaults
     *            property defaults provided by the installer or System properties
     * @param category
     *            the category
     * @param typeName
     *            the type name
     * @param id
     *            the instance id
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public ChildApplicationContextFactory(ApplicationContext parent, PropertyBackedBeanRegistry registry,
            Properties propertyDefaults, String category, String typeName, List<String> id) throws IOException
    {
        setApplicationContext(parent);
        setRegistry(registry);
        setPropertyDefaults(propertyDefaults);
        setBeanName(category);
        setTypeName(typeName);
        setId(id);

        try
        {
            afterPropertiesSet();
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

    /**
     * Sets the type name.
     * 
     * @param typeName
     *            the typeName to set
     */
    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }

    /**
     * Gets the type name.
     * 
     * @return the type name
     */
    public String getTypeName()
    {
        return this.typeName;
    }

    /**
     * Registers a set of composite propertes and their types.
     * 
     * @param compositePropertyTypes
     *            a map of property names to Java classes. The classes should follow standard Java Bean conventions. If
     *            the class implements {@link BeanNameAware} the instance name will be propagated to the
     *            <code>beanName</code> property automatically.
     */
    public void setCompositePropertyTypes(Map<String, Class<?>> compositePropertyTypes)
    {
        this.compositePropertyTypes = compositePropertyTypes;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        if (getTypeName() == null)
        {
            setTypeName(getId().get(0));
        }

        // Load the property defaults
        PropertiesFactoryBean factory = new PropertiesFactoryBean();
        factory.setLocations(getParent().getResources(
                ChildApplicationContextFactory.CLASSPATH_PREFIX + getCategory() + '/' + getTypeName()
                        + ChildApplicationContextFactory.PROPERTIES_SUFFIX));
        factory.afterPropertiesSet();
        this.properties = (Properties) factory.getObject();

        // Now let the superclass propagate default settings from the global properties and register us
        super.afterPropertiesSet();

        // Apply any property overrides from the extension classpath and also allow system properties and JNDI to
        // override
        JndiPropertiesFactoryBean overrideFactory = new JndiPropertiesFactoryBean();
        overrideFactory.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
        overrideFactory.setLocations(getParent().getResources(
                ChildApplicationContextFactory.EXTENSION_CLASSPATH_PREFIX + getCategory() + '/' + getTypeName() + '/'
                        + getId() + '/' + ChildApplicationContextFactory.PROPERTIES_SUFFIX));
        overrideFactory.setProperties(this.properties);
        overrideFactory.afterPropertiesSet();
        this.properties = (Properties) overrideFactory.getObject();

    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#getPropertyNames()
     */
    @SuppressWarnings("unchecked")
    public synchronized Set<String> getPropertyNames()
    {
        Set<String> result = new TreeSet<String>(((Map) this.properties).keySet());
        result.add(ChildApplicationContextFactory.TYPE_NAME_PROPERTY);
        result.addAll(this.compositePropertyTypes.keySet());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#getProperty(java.lang.String)
     */
    public synchronized String getProperty(String name)
    {
        if (name.equals(ChildApplicationContextFactory.TYPE_NAME_PROPERTY))
        {
            return getTypeName();
        }
        else if (this.compositePropertyTypes.containsKey(name))
        {
            Map<String, CompositeDataBean> beans = this.compositeProperties.get(name);
            if (beans != null)
            {
                StringBuilder list = new StringBuilder(100);
                for (String id : beans.keySet())
                {
                    if (list.length() > 0)
                    {
                        list.append(',');
                    }
                    list.append(id);
                }
                return list.toString();
            }
            return "";
        }
        else
        {
            return this.properties.getProperty(name);
        }

    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#setProperty(java.lang.String, java.lang.String)
     */
    public void setProperty(String name, String value)
    {
        if (name.equals(ChildApplicationContextFactory.TYPE_NAME_PROPERTY))
        {
            throw new IllegalStateException("Illegal write to property \""
                    + ChildApplicationContextFactory.TYPE_NAME_PROPERTY + "\"");
        }
        Class<?> type = this.compositePropertyTypes.get(name);
        if (type != null)
        {
            updateCompositeProperty(name, value, type);
        }
        else
        {
            this.properties.setProperty(name, value);
        }
    }

    /**
     * Updates a composite property with a new list of instance names. Properties of those instances that existed
     * previously will be preserved. Instances that no longer exist will be destroyed. New instances will be brought
     * into life with default values, as described in the class description.
     * 
     * @param name
     *            the composite property name
     * @param value
     *            a list of bean instance IDs
     * @param type
     *            the bean class
     */
    private void updateCompositeProperty(String name, String value, Class<?> type)
    {
        // Retrieve the map of existing values of this property
        Map<String, CompositeDataBean> propertyValues = this.compositeProperties.get(name);
        if (propertyValues == null)
        {
            propertyValues = Collections.emptyMap();
        }

        try
        {
            Map<String, CompositeDataBean> newPropertyValues = new LinkedHashMap<String, CompositeDataBean>(11);
            StringTokenizer tkn = new StringTokenizer(value, ", \t\n\r\f");
            while (tkn.hasMoreTokens())
            {
                String id = tkn.nextToken();

                // Generate a unique ID within the category
                List<String> childId = new ArrayList<String>(3);
                childId.addAll(getId());
                childId.add(name);
                childId.add(id);

                // Look out for new or updated children
                CompositeDataBean child = propertyValues.get(id);

                if (child == null)
                {
                    child = new CompositeDataBean(getParent(), this, getRegistry(), getPropertyDefaults(),
                            getCategory(), type, childId);
                }
                newPropertyValues.put(id, child);
            }

            // Destroy any children that have been removed
            Set<String> idsToRemove = new TreeSet<String>(propertyValues.keySet());
            idsToRemove.removeAll(newPropertyValues.keySet());
            for (String id : idsToRemove)
            {
                CompositeDataBean child = propertyValues.get(id);
                child.destroy(true);
            }
            this.compositeProperties.put(name, newPropertyValues);
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

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.AbstractPropertyBackedBean#isUpdateable(java.lang.String)
     */
    @Override
    public boolean isUpdateable(String name)
    {
        // Only the type property is read only
        return !name.equals(ChildApplicationContextFactory.TYPE_NAME_PROPERTY);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.AbstractPropertyBackedBean#getDescription(java.lang.String)
     */
    @Override
    public String getDescription(String name)
    {
        return name.equals(ChildApplicationContextFactory.TYPE_NAME_PROPERTY) ? "Read-only subsystem type name"
                : this.compositePropertyTypes.containsKey(name) ? "Comma separated list of child object names" : super
                        .getDescription(name);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#start()
     */
    public synchronized void start()
    {
        // This is where we actually create and start a child application context based on the configured properties.
        if (this.applicationContext == null)
        {
            ChildApplicationContextFactory.logger.info("Starting '" + getCategory() + "' subsystem, ID: " + getId());
            this.applicationContext = new ChildApplicationContext();
            this.applicationContext.refresh();
            ChildApplicationContextFactory.logger.info("Startup of '" + getCategory() + "' subsystem, ID: " + getId()
                    + " complete");
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.PropertyBackedBean#stop()
     */
    public void stop()
    {
        if (this.applicationContext != null)
        {
            ChildApplicationContextFactory.logger.info("Stopping '" + getCategory() + "' subsystem, ID: " + getId());
            try
            {
                this.applicationContext.close();
            }
            catch (Exception e)
            {
                ChildApplicationContextFactory.logger.error(e);
                // Continue anyway. Perhaps it didn't start properly
            }
            this.applicationContext = null;
            ChildApplicationContextFactory.logger.info("Stopped '" + getCategory() + "' subsystem, ID: " + getId());
        }
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
        for (Map<String, CompositeDataBean> beans : this.compositeProperties.values())
        {
            for (CompositeDataBean bean : beans.values())
            {
                bean.destroy(permanent);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.ManagedApplicationContextFactory#getApplicationContext()
     */
    public synchronized ApplicationContext getApplicationContext()
    {
        start();
        return this.applicationContext;
    }

    /**
     * A specialized application context class with the power to propagate simple and composite property values into
     * beans before they are initialized.
     * 
     * @author dward
     */
    private class ChildApplicationContext extends ClassPathXmlApplicationContext
    {

        /**
         * The Constructor.
         * 
         * @throws BeansException
         *             the beans exception
         */
        private ChildApplicationContext() throws BeansException
        {
            super(new String[]
            {
                ChildApplicationContextFactory.CLASSPATH_PREFIX + getCategory() + '/' + getTypeName()
                        + ChildApplicationContextFactory.CONTEXT_SUFFIX,
                ChildApplicationContextFactory.EXTENSION_CLASSPATH_PREFIX + getCategory() + '/' + getTypeName() + '/'
                        + getId() + '/' + ChildApplicationContextFactory.CONTEXT_SUFFIX
            }, false, ChildApplicationContextFactory.this.getParent());

            // Add a property placeholder configurer, with the subsystem-scoped default properties
            PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
            configurer.setProperties(ChildApplicationContextFactory.this.properties);
            configurer.setIgnoreUnresolvablePlaceholders(true);
            addBeanFactoryPostProcessor(configurer);

            // Add all the post processors of the parent, e.g. to make sure system placeholders get expanded properly
            for (Object postProcessor : getParent().getBeansOfType(BeanFactoryPostProcessor.class).values())
            {
                addBeanFactoryPostProcessor((BeanFactoryPostProcessor) postProcessor);
            }

            setClassLoader(ChildApplicationContextFactory.this.getParent().getClassLoader());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.context.support.AbstractApplicationContext#getResourcePatternResolver()
         */
        @Override
        protected ResourcePatternResolver getResourcePatternResolver()
        {
            // Ensure we can resolve resourced on JBoss 5
            return new JBossEnabledResourcePatternResolver(this);
        }

        /*
         * (non-Javadoc)
         * @see
         * org.springframework.context.support.AbstractApplicationContext#postProcessBeanFactory(org.springframework
         * .beans.factory.config.ConfigurableListableBeanFactory)
         */
        @Override
        protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
        {
            // Propagate composite properties to list factories of the corresponding name
            beanFactory.addBeanPostProcessor(new BeanPostProcessor()
            {

                public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
                {
                    return bean;
                }

                public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
                {
                    if (bean instanceof ListFactoryBean
                            && ChildApplicationContextFactory.this.compositePropertyTypes.containsKey(beanName))
                    {
                        Map<String, CompositeDataBean> beans = ChildApplicationContextFactory.this.compositeProperties
                                .get(beanName);
                        List<Object> beanList;
                        if (beans != null)
                        {
                            beanList = new ArrayList<Object>(beans.size());
                            for (CompositeDataBean wrapped : beans.values())
                            {
                                beanList.add(wrapped.getBean());
                            }
                        }
                        else
                        {
                            beanList = Collections.emptyList();
                        }
                        ((ListFactoryBean) bean).setSourceList(beanList);
                    }
                    return bean;
                }
            });
        }
    }
}
