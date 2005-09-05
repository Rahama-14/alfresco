/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.web.app;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.faces.context.FacesContext;
import javax.portlet.PortletContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigService;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.app.servlet.AuthenticationFilter;
import org.alfresco.web.bean.ErrorBean;
import org.alfresco.web.bean.repository.User;
import org.alfresco.web.config.ServerConfigElement;
import org.apache.commons.logging.Log;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * Utilities class
 * 
 * @author gavinc
 */
public class Application
{
   private static final String LOCALE = "locale";
   
   public static final String BEAN_CONFIG_SERVICE = "configService";
   public static final String BEAN_DATA_DICTIONARY = "dataDictionary";
   
   public static final String MESSAGE_BUNDLE = "alfresco.messages";
   
   private static boolean inPortalServer = true;
   private static String repoStoreUrl;
   private static String rootPath;
   private static String companyRootId;
   private static String companyRootDescription;
   private static String glossaryFolderName;
   private static String templatesFolderName;
   
   /**
    * Private constructor to prevent instantiation of this class 
    */
   private Application()
   {
   }
   
   /**
    * Sets whether this application is running inside a portal server
    * 
    * @param inPortal true to indicate the application is running as a portlet 
    */
   public static void setInPortalServer(boolean inPortal)
   {
      inPortalServer = inPortal;
   }
   
   /**
    * Determines whether the server is running in a portal
    * 
    * @return true if we are running inside a portal server
    */
   public static boolean inPortalServer()
   {
      return inPortalServer;
   }
   
   /**
    * Handles errors thrown from servlets
    * 
    * @param servletContext The servlet context
    * @param request The HTTP request
    * @param response The HTTP response
    * @param error The exception
    * @param logger The logger
    */
   public static void handleServletError(ServletContext servletContext, HttpServletRequest request,
         HttpServletResponse response, Throwable error, Log logger, String returnPage)
      throws IOException, ServletException
   {
      // get the error bean from the session and set the error that occurred.
      HttpSession session = request.getSession();
      ErrorBean errorBean = (ErrorBean)session.getAttribute(ErrorBean.ERROR_BEAN_NAME);
      if (errorBean == null)
      {
         errorBean = new ErrorBean();
         session.setAttribute(ErrorBean.ERROR_BEAN_NAME, errorBean);
      }
      errorBean.setLastError(error);
      errorBean.setReturnPage(returnPage);
      
      // try and find the configured error page
      boolean errorShown = false;
      String errorPage = getErrorPage(servletContext);
      
      if (errorPage != null)
      {
         if (logger.isDebugEnabled())
            logger.debug("An error has occurred, redirecting to error page: " + errorPage);
         
         if (response.isCommitted() == false)
         {
            errorShown = true;
            response.sendRedirect(request.getContextPath() + errorPage);
         }
         else
         {
            if (logger.isDebugEnabled())
               logger.debug("Response is already committed, re-throwing error");
         }
      }
      else
      {
         if (logger.isDebugEnabled())
            logger.debug("No error page defined, re-throwing error");
      }
      
      // if we could not show the error page for whatever reason, re-throw the error
      if (!errorShown)
      {
         if (error instanceof IOException)
         {
            throw (IOException)error;
         }
         else if (error instanceof ServletException)
         {
            throw (ServletException)error;
         }
         else
         {
            throw new ServletException(error);
         }
      }
   }
   
   /**
    * Retrieves the configured error page for the application
    * 
    * @param servletContext The servlet context
    * @return The configured error page or null if the configuration is missing
    */
   public static String getErrorPage(ServletContext servletContext)
   {
      return getErrorPage(WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext));
   }
   
   /**
    * Retrieves the configured error page for the application
    * 
    * @param portletContext The portlet context
    * @return
    */
   public static String getErrorPage(PortletContext portletContext)
   {
      return getErrorPage((WebApplicationContext)portletContext.getAttribute(
            WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE));
   }
   
   /**
    * Retrieves the configured login page for the application
    * 
    * @param servletContext The servlet context
    * @return The configured login page or null if the configuration is missing
    */
   public static String getLoginPage(ServletContext servletContext)
   {
      return getLoginPage(WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext));
   }
   
   /**
    * Retrieves the configured login page for the application
    * 
    * @param portletContext The portlet context
    * @return
    */
   public static String getLoginPage(PortletContext portletContext)
   {
      return getLoginPage((WebApplicationContext)portletContext.getAttribute(
            WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE));
   }
   
   /**
    * @return Returns the User object representing the currently logged in user
    */
   public static User getCurrentUser(ServletContext context)
   {
      return (User)context.getAttribute(AuthenticationFilter.AUTHENTICATION_USER);
   }
   
   /**
    * @return Returns the User object representing the currently logged in user
    */
   public static User getCurrentUser(FacesContext context)
   {
      return (User)context.getExternalContext().getSessionMap().get(AuthenticationFilter.AUTHENTICATION_USER);
   }
   
   /**
    * @return Returns the repository store URL (retrieved from config service)
    */
   public static String getRepositoryStoreUrl(ServletContext context)
   {
      return getRepositoryStoreUrl(WebApplicationContextUtils.getRequiredWebApplicationContext(context));
   }
   
   /**
    * @return Returns the repository store URL (retrieved from config service)
    */
   public static String getRepositoryStoreUrl(FacesContext context)
   {
      return getRepositoryStoreUrl(FacesContextUtils.getRequiredWebApplicationContext(context));
   }
   
   /**
    * @return Returns id of the company root 
    */
   public static String getCompanyRootId()
   {
      return companyRootId;
   }
   
   /**
    * Sets the company root id. This is setup by the ContextListener.
    * 
    * @param id The company root id
    */
   public static void setCompanyRootId(String id)
   {
      companyRootId = id;
   }
   
   /**
    * @return Returns the root path for the application (retrieved from config service)
    */
   public static String getRootPath(ServletContext context)
   {
      return getRootPath(WebApplicationContextUtils.getRequiredWebApplicationContext(context));
   }
   
   /**
    * @return Returns the root path for the application (retrieved from config service)
    */
   public static String getRootPath(FacesContext context)
   {
      return getRootPath(FacesContextUtils.getRequiredWebApplicationContext(context));
   }
   
   /**
    * @return Returns the glossary folder name (retrieved from config service)
    */
   public static String getGlossaryFolderName(ServletContext context)
   {
      return getGlossaryFolderName(WebApplicationContextUtils.getRequiredWebApplicationContext(context));
   }
   
   /**
    * @return Returns the glossary folder name (retrieved from config service)
    */
   public static String getGlossaryFolderName(FacesContext context)
   {
      return getGlossaryFolderName(FacesContextUtils.getRequiredWebApplicationContext(context));
   }
   
   /**
    * @return Returns the templates folder name (retrieved from config service)
    */
   public static String getTemplatesFolderName(ServletContext context)
   {
      return getTemplatesFolderName(WebApplicationContextUtils.getRequiredWebApplicationContext(context));
   }
   
   /**
    * @return Returns the templates folder name (retrieved from config service)
    */
   public static String getTemplatesFolderName(FacesContext context)
   {
      return getTemplatesFolderName(FacesContextUtils.getRequiredWebApplicationContext(context));
   }
   
   /**
    * Set the language locale for the current user context
    * 
    * @param context        FacesContext for current user
    * @param code           The ISO locale code to set
    */
   public static void setLanguage(FacesContext context, String code)
   {
      Locale locale = Locale.getDefault();
      
      StringTokenizer t = new StringTokenizer(code, "_");
      int tokens = t.countTokens();
      if (tokens == 1)
      {
         locale = new Locale(code);
      }
      else if (tokens == 2)
      {
         locale = new Locale(t.nextToken(), t.nextToken());
      }
      else if (tokens == 3)
      {
         locale = new Locale(t.nextToken(), t.nextToken(), t.nextToken());
      }
      
      // set locale for JSF framework usage
      context.getViewRoot().setLocale(locale);
      
      // set locale for our framework usage
      context.getExternalContext().getSessionMap().put(LOCALE, locale);
      
      // clear the current message bundle - so it's reloaded with new locale
      context.getExternalContext().getSessionMap().remove(MESSAGE_BUNDLE);
   }
   
   /**
    * Return the language Locale for the current user context
    * 
    * @param context        FacesContext for the current user
    * 
    * @return Current language Locale set or null if none set 
    */
   public static Locale getLanguage(FacesContext context)
   {
      return (Locale)context.getExternalContext().getSessionMap().get(LOCALE);
   }
   
   /**
    * Return the language Locale for the current user Session.
    * 
    * @param session        HttpSession for the current user
    * 
    * @return Current language Locale set or null if none set 
    */
   public static Locale getLanguage(HttpSession session)
   {
      return (Locale)session.getAttribute(LOCALE);
   }
   
   /**
    * Get the specified I18N message string from the default message bundle for this user
    * 
    * @param context        FacesContext
    * @param msg            Message ID
    * 
    * @return String from message bundle or null if not found
    */
   public static String getMessage(FacesContext context, String msg)
   {
      return getBundle(context).getString(msg);
   }
   
   /**
    * Get the specified I18N message string from the default message bundle for this user
    * 
    * @param session        HttpSession
    * @param msg            Message ID
    * 
    * @return String from message bundle or null if not found
    */
   public static String getMessage(HttpSession session, String msg)
   {
      ResourceBundle bundle = (ResourceBundle)session.getAttribute(MESSAGE_BUNDLE);
      if (bundle == null)
      {
         // get Locale from language selected by each user on login
         Locale locale = (Locale)session.getAttribute(LOCALE);
         if (locale == null)
         {
            locale = Locale.getDefault();
         }
         bundle = ResourceBundle.getBundle(MESSAGE_BUNDLE, locale);
         if (bundle == null)
         {
            throw new AlfrescoRuntimeException("Unable to load Alfresco messages bundle: " + MESSAGE_BUNDLE);
         }
         
         // apply our wrapper to catch MissingResourceException
         bundle = new ResourceBundleWrapper(bundle);
         
         session.setAttribute(MESSAGE_BUNDLE, bundle);
      }
      
      return bundle.getString(msg);
   }
   
   /**
    * Get the specified the default message bundle for this user
    * 
    * @param context        FacesContext
    * 
    * @return ResourceBundle for this user
    */
   public static ResourceBundle getBundle(FacesContext context)
   {
      // get the resource bundle for the current locale
      // we store the bundle in the users session
      // this makes it easy to add a locale per user support later
      Map session = context.getExternalContext().getSessionMap();
      ResourceBundle bundle = (ResourceBundle)session.get(MESSAGE_BUNDLE);
      if (bundle == null)
      {
         // get Locale from language selected by each user on login
         Locale locale = (Locale)session.get(LOCALE);
         if (locale == null)
         {
            locale = Locale.getDefault();
         }
         bundle = ResourceBundle.getBundle(MESSAGE_BUNDLE, locale);
         if (bundle == null)
         {
            throw new AlfrescoRuntimeException("Unable to load Alfresco messages bundle: " + MESSAGE_BUNDLE);
         }
         
         // apply our wrapper to catch MissingResourceException
         bundle = new ResourceBundleWrapper(bundle);
         
         session.put(MESSAGE_BUNDLE, bundle);
      }
      
      return bundle;
   }
   
   /**
    * Returns the repository store URL (retrieved from config service)
    * 
    * @param context The spring context
    * @return The repository store URL to use
    */
   private static String getRepositoryStoreUrl(WebApplicationContext context)
   {
      if (repoStoreUrl == null)
      {
         ConfigService configService = (ConfigService)context.getBean(BEAN_CONFIG_SERVICE);
         ConfigElement repoConfig = configService.getGlobalConfig().getConfigElement("repository");
         ConfigElement storeUrlConfig = repoConfig.getChild("store-url");
         if (storeUrlConfig != null)
         {
             repoStoreUrl = storeUrlConfig.getValue();
         }
      }
      
      return repoStoreUrl;
   }
   
   /**
    * Returns the root path for the application (retrieved from config service)
    * 
    * @param context The spring context
    * @return The application root path
    */
   private static String getRootPath(WebApplicationContext context)
   {
      if (rootPath == null)
      {
         ConfigService configService = (ConfigService)context.getBean(BEAN_CONFIG_SERVICE);
         ConfigElement repoConfig = configService.getGlobalConfig().getConfigElement("repository");
         ConfigElement rootPathConfig = repoConfig.getChild("root-path");
         if (rootPathConfig != null)
         {
             rootPath = rootPathConfig.getValue();
         }
      }
      
      return rootPath;
   }
   
   /**
    * Returns the glossary folder name (retrieved from config service)
    * 
    * @param context The spring context
    * @return The glossary folder name
    */
   private static String getGlossaryFolderName(WebApplicationContext context)
   {
      if (glossaryFolderName == null)
      {
         ConfigService configService = (ConfigService)context.getBean(BEAN_CONFIG_SERVICE);
         ConfigElement repoConfig = configService.getGlobalConfig().getConfigElement("repository");
         for (ConfigElement child : repoConfig.getChildren())
         {
            if (child.getName().equals("glossary-folder-name"))
            {
               glossaryFolderName = child.getValue();
            }
         }
      }
      
      return glossaryFolderName;
   }
   
   /**
    * Returns the templates folder name (retrieved from config service)
    * 
    * @param context The spring context
    * @return The templates folder name
    */
   private static String getTemplatesFolderName(WebApplicationContext context)
   {
      if (templatesFolderName == null)
      {
         ConfigService configService = (ConfigService)context.getBean(BEAN_CONFIG_SERVICE);
         ConfigElement repoConfig = configService.getGlobalConfig().getConfigElement("repository");
         for (ConfigElement child : repoConfig.getChildren())
         {
            if (child.getName().equals("templates-folder-name"))
            {
               templatesFolderName = child.getValue();
            }
         }
      }
      
      return templatesFolderName;
   }
   
   /**
    * Retrieves the configured error page for the application
    * 
    * @param context The Spring contexr
    * @return The configured error page or null if the configuration is missing
    */
   private static String getErrorPage(WebApplicationContext context)
   {
      String errorPage = null;
      
      ConfigService svc = (ConfigService)context.getBean(BEAN_CONFIG_SERVICE);
      ServerConfigElement serverConfig = (ServerConfigElement)svc.getGlobalConfig().getConfigElement("server");
      
      if (serverConfig != null)
      {
         errorPage = serverConfig.getErrorPage();
      }
      
      return errorPage;
   }
   
   /**
    * Retrieves the configured login page for the application
    * 
    * @param context The Spring contexr
    * @return The configured login page or null if the configuration is missing
    */
   private static String getLoginPage(WebApplicationContext context)
   {
      String loginPage = null;
      
      ConfigService svc = (ConfigService)context.getBean(BEAN_CONFIG_SERVICE);
      ServerConfigElement serverConfig = (ServerConfigElement)svc.getGlobalConfig().getConfigElement("server");
      
      if (serverConfig != null)
      {
         loginPage = serverConfig.getLoginPage();
      }
      
      return loginPage;
   }
}
