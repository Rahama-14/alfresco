package org.alfresco.web.util;

import java.io.IOException;

import javax.portlet.PortletContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.config.ConfigService;
import org.alfresco.web.bean.ErrorBean;
import org.alfresco.web.config.ServerConfigElement;
import org.apache.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Utilities class
 * 
 * @author gavinc
 */
public class Utils
{
   /**
    * Private constructor to prevent instantiation of this class 
    */
   private Utils()
   {
   }
   
   /**
    * Determines whether the server is running in a portal
    * 
    * @param servletContext The servlet context
    * @return true if we are running inside a portal server
    */
   public static boolean inPortalServer(ServletContext servletContext)
   {
      boolean inPortal = true;
      
      ConfigService svc = (ConfigService)WebApplicationContextUtils.getRequiredWebApplicationContext(
            servletContext).getBean("configService");
      ServerConfigElement serverConfig = (ServerConfigElement)svc.getGlobalConfig().getConfigElement("server");
      
      if (serverConfig != null)
      {
         inPortal = serverConfig.isPortletMode();
      }
      
      return inPortal;
   }
   
   /**
    * Retrieves the configured error page for the application
    * 
    * @param servletContext The servlet context
    * @return The configured error page or null if the configuration is missing
    */
   public static String getErrorPage(ServletContext servletContext)
   {
      String errorPage = null;
      
      ConfigService svc = (ConfigService)WebApplicationContextUtils.getRequiredWebApplicationContext(
            servletContext).getBean("configService");
      ServerConfigElement serverConfig = (ServerConfigElement)svc.getGlobalConfig().getConfigElement("server");
      
      if (serverConfig != null)
      {
         errorPage = serverConfig.getErrorPage();
      }
      
      return errorPage;
   }
   
   /**
    * Retrieves the configured error page for the application
    * 
    * @param portletContext The portlet context
    * @return
    */
   public static String getErrorPage(PortletContext portletContext)
   {
      String errorPage = null;
      
      WebApplicationContext webAppCtx = (WebApplicationContext)portletContext.getAttribute(
            WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
      if (webAppCtx != null)
      {
         ConfigService svc = (ConfigService)webAppCtx.getBean("configService");
         ServerConfigElement serverConfig = (ServerConfigElement)svc.getGlobalConfig().getConfigElement("server");
         
         if (serverConfig != null)
         {
            errorPage = serverConfig.getErrorPage();
         }
      }
      
      return errorPage;
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
         HttpServletResponse response, Throwable error, Logger logger, String returnPage)
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
      
      // if we could show the error page for whatever reason, re-throw the error
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
}
