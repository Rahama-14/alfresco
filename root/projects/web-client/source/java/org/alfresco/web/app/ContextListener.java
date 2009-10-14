/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.app;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.transaction.UserTransaction;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.SessionUser;
import org.alfresco.repo.cache.InternalEhCacheManagerFactoryBean;
import org.alfresco.repo.security.authentication.AuthenticationContext;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.app.servlet.AuthenticationHelper;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;


/**
 * ServletContextListener implementation that initialises the application.
 * 
 * NOTE: This class must appear after the Spring context loader listener
 * 
 * @author gavinc
 */
public class ContextListener implements ServletContextListener, HttpSessionListener
{
   private static Log logger = LogFactory.getLog(ContextListener.class);

   private ServletContext servletContext;

   /**
    * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
    */
   public void contextInitialized(ServletContextEvent event)
   {
       // make sure that the spaces store in the repository exists
      this.servletContext = event.getServletContext();
      WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
      
      // If no context has been initialised, exit silently so config changes can be made
      if (ctx == null)
      {
          return;
      }
      
      ServiceRegistry registry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
      TransactionService transactionService = registry.getTransactionService();
      NodeService nodeService = registry.getNodeService();
      SearchService searchService = registry.getSearchService();
      NamespaceService namespaceService = registry.getNamespaceService();
      AuthenticationContext authenticationContext = (AuthenticationContext) ctx
            .getBean("authenticationContext");

      // repo bootstrap code for our client
      UserTransaction tx = null;
      NodeRef companySpaceNodeRef = null;
      try
      {
         tx = transactionService.getUserTransaction();
         tx.begin();
         authenticationContext.setSystemUserAsCurrentUser();

         // get and setup the initial store ref and root path from config
         StoreRef storeRef = Repository.getStoreRef(servletContext);
         
         // get root path
         String rootPath = Application.getRootPath(servletContext);

         // Extract company space id and store it in the Application object
         companySpaceNodeRef = Repository.getCompanyRoot(nodeService, searchService, namespaceService, storeRef, rootPath);
         Application.setCompanyRootId(companySpaceNodeRef.getId());
         
         // commit the transaction
         tx.commit();
      }
      catch (Throwable e)
      {
         // rollback the transaction
         try
         {
            if (tx != null)
            {
               tx.rollback();
            }
         }
         catch (Exception ex) {}
         
         logger.error("Failed to initialise ", e);
         throw new AlfrescoRuntimeException("Failed to initialise ", e);
      }
      finally
      {
          try
          {
             authenticationContext.clearCurrentSecurityContext();
          }
          catch (Exception ex) {}
      }
   }

   /**
    * {@inheritDoc}
    * <p>
    * Forcibly kills Alfresco's EHCache CacheManager
    */
   public void contextDestroyed(ServletContextEvent event)
   {
      InternalEhCacheManagerFactoryBean.getInstance().shutdown();
   }

   /**
    * Session created listener
    */
   public void sessionCreated(HttpSessionEvent event)
   {
      if (logger.isDebugEnabled())
         logger.debug("HTTP session created: " + event.getSession().getId());
   }

   /**
    * Session destroyed listener
    */
   public void sessionDestroyed(HttpSessionEvent event)
   {
      if (logger.isDebugEnabled())
         logger.debug("HTTP session destroyed: " + event.getSession().getId());
      
      String userKey = null;
      if (Application.inPortalServer() == false)
      {
         userKey = AuthenticationHelper.AUTHENTICATION_USER;
      }
      else
      {
         // search for the user object in the portlet wrapped session keys
         // each vendor uses a different naming scheme so we search by hand
         String userKeyPostfix = "?" + AuthenticationHelper.AUTHENTICATION_USER; 
         Enumeration enumNames = event.getSession().getAttributeNames();
         while (enumNames.hasMoreElements())
         {
            String name = (String)enumNames.nextElement();
            if (name.endsWith(userKeyPostfix))
            {
               userKey = name;
               break;
            }
         }
      }
      if (userKey != null)
      {
         SessionUser user = (SessionUser)event.getSession().getAttribute(userKey);
         if (user != null)
         {
            // invalidate ticket and clear the Security context for this thread
            WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
            AuthenticationService authService = (AuthenticationService)ctx.getBean("authenticationService");
            authService.invalidateTicket(user.getTicket());
            authService.clearCurrentSecurityContext();
            event.getSession().removeAttribute(userKey);
         }
      }
   }
}
