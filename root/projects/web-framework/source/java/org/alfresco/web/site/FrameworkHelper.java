/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 * As a special exception to the terms and conditions of version 2.0 of the GPL,
 * you may redistribute this Program in connection with Free/Libre and Open
 * Source Software ("FLOSS") applications as described in Alfresco's FLOSS
 * exception. You should have recieved a copy of the text describing the FLOSS
 * exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.site;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.alfresco.config.Config;
import org.alfresco.config.ConfigService;
import org.alfresco.connector.Connector;
import org.alfresco.connector.ConnectorService;
import org.alfresco.connector.ConnectorSession;
import org.alfresco.connector.CredentialVault;
import org.alfresco.connector.exception.RemoteConfigException;
import org.alfresco.util.ReflectionHelper;
import org.alfresco.web.config.RemoteConfigElement;
import org.alfresco.web.config.WebFrameworkConfigElement;
import org.alfresco.web.config.RemoteConfigElement.EndpointDescriptor;
import org.alfresco.web.framework.PresetsManager;
import org.alfresco.web.framework.WebFrameworkManager;
import org.alfresco.web.site.exception.FrameworkInitializationException;
import org.alfresco.web.site.exception.RequestContextException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

/**
 * Static utility functions for starting up the Framework
 * 
 * @author muzquiano
 */
public final class FrameworkHelper
{
    private static final String WEB_CONFIG_ID = "web.config";
    private static final String WEBFRAMEWORK_MANAGER_ID = "webframework.manager";
    private static final String CONNECTOR_SERVICE_ID = "connector.service";
    private static final String PRESETS_MANAGER_ID = "webframework.presets.manager";
    
    private static final String FRAMEWORK_TITLE = "Alfresco Surf";
    private static final String FRAMEWORK_VERSION = "3.0";
    
    private static Log logger = LogFactory.getLog(FrameworkHelper.class);
    
    private static ApplicationContext applicationContext = null;
    private static WebFrameworkManager webFrameworkManager = null;
    private static ConnectorService connectorService = null;
    private static PresetsManager presetManager = null;
    private static ConfigService configService = null;
    private static RemoteConfigElement remoteConfig = null;
    private static WebFrameworkConfigElement webFrameworkConfig = null;
    private static UserFactory userFactory = null;
    private static RequestContextFactory requestContextFactory = null;
    private static boolean isInitialized = false;
    private static String realPath;
    
    
    /**
     * Initializes the Web Framework.  This method must be called once
     * in order to instruct the framework to walk through its appropriate
     * startup steps.  This involves locating the configuration service,
     * connecting the appropriate model stores and the like.
     * 
     * This should be very quick to do.  The objects loaded here are
     * classloader scoped and should then be available to all future
     * requests.
     */
    public synchronized static void initFramework(ServletContext servletContext, ApplicationContext context)
        throws FrameworkInitializationException
    {
        if (!isInitialized())
        {
            // Store the application context
            applicationContext = context;
            
            
            // Get the WebFramework spring bean services
            webFrameworkManager = (WebFrameworkManager)applicationContext.getBean(WEBFRAMEWORK_MANAGER_ID);
            connectorService = (ConnectorService)applicationContext.getBean(CONNECTOR_SERVICE_ID);
            presetManager = (PresetsManager)applicationContext.getBean(PRESETS_MANAGER_ID);
            
            
            // init config caches
            configService = (ConfigService)applicationContext.getBean(WEB_CONFIG_ID);
            Config config = configService.getConfig("Remote");
            remoteConfig = (RemoteConfigElement)config.getConfigElement("remote");
            config = configService.getConfig("WebFramework");
            webFrameworkConfig = (WebFrameworkConfigElement)config.getConfigElement("web-framework");
            
            
            // store the real path to the servlet context
            realPath = servletContext.getRealPath("/");
            if (realPath != null && realPath.endsWith(java.io.File.separator))
            {
                realPath = realPath.substring(0, realPath.length() - 1);
            }
            
            
            /**
             * Init the User Factory for the framework.
             */
            String className = "org.alfresco.web.site.DefaultUserFactory";
            
            // check the config for an override
            String defaultId = webFrameworkConfig.getDefaultUserFactoryId();
            if (defaultId != null)
            {
                String _className = webFrameworkConfig.getUserFactoryDescriptor(defaultId).getImplementationClass();
                if (_className != null)
                {
                    className = _className;
                }
            }
            UserFactory factory = (UserFactory)ReflectionHelper.newObject(className);
            if (factory == null)
            {
                throw new FrameworkInitializationException(
                        "Unable to create user factory for class name: " + className);
            }
            
            if (logger.isDebugEnabled())
                logger.debug("Created User Factory: " + className);
            
            userFactory = factory;
            
            
            /**
             * Retrieve the configured RequestContextFactory implementation.
             */
            try
            {
                requestContextFactory = RequestContextFactoryBuilder.newFactory();
            }
            catch (RequestContextException re)
            {
                throw new FrameworkInitializationException("RequestContextFactory failed.", re);
            }
                        
            
            isInitialized = true;
            logger.info("Successfully Initialized Web Framework");
        }
    }
    
    /**
     * Creates and initializes a single request context instance.
     * 
     * This method is called once at the top of the request processing chain.
     * It routes through the configured RequestContextFactory implementation
     * so as to create a RequestContext implementation.
     * 
     * @param request
     * @throws RequestContextException
     */
    public static RequestContext initRequestContext(ServletRequest request)
        throws RequestContextException
    {
        /**
         * Using the factory, produce a new RequestContext instance for the given request.
         */
        RequestContext context = requestContextFactory.newInstance(request);
        
        /**
         * Bind the new request context instance to the request.
         */
        RequestUtil.setRequestContext(request, context);
        
        return context;
    }
    
    public static boolean isInitialized()
    {
        return isInitialized;
    }
    
    public static WebFrameworkManager getWebFrameworkManager()
    {
        return webFrameworkManager;
    }
    
    public static String getRealPath(String path)
    {
        String newPath = path;
        if (realPath != null)
        {
            newPath = realPath + path;
            
            // clean up the paths
            newPath = newPath.replace("/", java.io.File.separator);
            newPath = newPath.replace("\\", java.io.File.separator);            
        }
        return newPath;
    }
    
    public static Log getLogger()
    {
        return logger;
    }
    
    public static RemoteConfigElement getRemoteConfig()
    {
        return remoteConfig;
    }
    
    public static WebFrameworkConfigElement getConfig()
    {
        return webFrameworkConfig;
    }
    
    public static ConfigService getConfigService()
    {
        return configService;
    }
    
    public static ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }
    
    public static ConnectorService getConnectorService()
    {
        return connectorService;
    }
    
    public static PresetsManager getPresetsManager()
    {
        return presetManager;
    }
    
    public static EndpointDescriptor getEndpoint(String endpointId)
    {
        return getRemoteConfig().getEndpointDescriptor(endpointId);
    }
    
    public static Connector getConnector(String endpointId)
        throws RemoteConfigException
    {
        return getConnectorService().getConnector(endpointId);
    }
    
    public static Connector getConnector(RequestContext context, String endpointId)
        throws RemoteConfigException
    {
        HttpSession httpSession = context.getRequest().getSession();
        return getConnector(httpSession, context.getUserId(), endpointId);
    }
    
    public static Connector getConnector(HttpSession httpSession, String userId, String endpointId)
        throws RemoteConfigException
    {
        return getConnectorService().getConnector(endpointId, userId, httpSession);
    }
    
    public static UserFactory getUserFactory()
    {
        return userFactory;
    }
    
    public static CredentialVault getCredentialVault(HttpSession httpSession, String userId)
    {
        CredentialVault vault = null;
        try
        {
            vault = getConnectorService().getCredentialVault(httpSession, userId);
        }
        catch (RemoteConfigException rce)
        {
            logger.error("Unable to retrieve credential vault for user: " + userId, rce);
        }
        
        return vault;
    }
    
    public static CredentialVault getCredentialVault(RequestContext context, String userId)
    {
        HttpSession httpSession = context.getRequest().getSession();
        
        return getCredentialVault(httpSession, userId);
    }
    
    public static ConnectorSession getConnectorSession(RequestContext context, String endpointId)
    {
        HttpSession httpSession = context.getRequest().getSession();
        return getConnectorSession(httpSession, endpointId);
    }

    public static ConnectorSession getConnectorSession(HttpSession httpSession, String endpointId)
    {
        return getConnectorService().getConnectorSession(httpSession, endpointId);
    }
    
    public static void removeConnectorSessions(RequestContext context)
    {
        try
        {
            HttpSession httpSession = context.getRequest().getSession();
            
            String[] endpointIds = FrameworkHelper.getRemoteConfig().getEndpointIds();
            for (int i = 0; i < endpointIds.length; i++)
            {
                getConnectorService().removeConnectorSession(httpSession, endpointIds[i]);
            }
        }
        catch (Exception ex)
        {
            logger.error("Unable to remove connector sessions", ex);
        }
    }
    
    public static String getFrameworkTitle()
    {
        return FRAMEWORK_TITLE;
    }
    
    public static String getFrameworkVersion()
    {
        return FRAMEWORK_VERSION;
    }
}
