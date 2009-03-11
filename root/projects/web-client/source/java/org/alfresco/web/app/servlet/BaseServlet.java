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
package org.alfresco.web.app.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.URLDecoder;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.LoginBean;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.jsf.FacesContextUtils;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;


/**
 * Base servlet class containing useful constant values and common methods for Alfresco servlets.
 * 
 * @author Kevin Roast
 */
public abstract class BaseServlet extends HttpServlet
{
   public static final String FACES_SERVLET = "/faces";
   
   /** an existing Ticket can be passed to most servlet for non-session based authentication */
   private static final String ARG_TICKET   = "ticket";
   
   /** forcing guess access is available on most servlets */
   private static final String ARG_GUEST    = "guest";
   
   /** list of valid JSPs for redirect after a clean login */
   // TODO: make this list configurable
   private static Set<String> validRedirectJSPs = new HashSet<String>();
   static
   {
      validRedirectJSPs.add("/jsp/browse/browse.jsp");
      validRedirectJSPs.add("/jsp/admin/admin-console.jsp");
      validRedirectJSPs.add("/jsp/admin/avm-console.jsp");
      validRedirectJSPs.add("/jsp/admin/node-browser.jsp");
      validRedirectJSPs.add("/jsp/admin/store-browser.jsp");
      validRedirectJSPs.add("/jsp/users/user-console.jsp");
      validRedirectJSPs.add("/jsp/categories/categories.jsp");
      validRedirectJSPs.add("/jsp/dialog/about.jsp");
      validRedirectJSPs.add("/jsp/search/advanced-search.jsp");
      validRedirectJSPs.add("/jsp/admin/system-info.jsp");
      validRedirectJSPs.add("/jsp/forums/forums.jsp");
      validRedirectJSPs.add("/jsp/users/users.jsp");
      validRedirectJSPs.add("/jsp/trashcan/trash-list.jsp");
   }
   
   private static Log logger = LogFactory.getLog(BaseServlet.class);
   
   // Tenant service
   private static TenantService m_tenantService;
   
   /**
    * Return the ServiceRegistry helper instance
    * 
    * @param sc      ServletContext
    * 
    * @return ServiceRegistry
    */
   public static ServiceRegistry getServiceRegistry(ServletContext sc)
   {
      WebApplicationContext wc = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
      return (ServiceRegistry)wc.getBean(ServiceRegistry.SERVICE_REGISTRY);
   }
   
   /**
    * Perform an authentication for the servlet request URI. Processing any "ticket" or
    * "guest" URL arguments.
    * 
    * @return AuthenticationStatus
    * 
    * @throws IOException
    */
   public AuthenticationStatus servletAuthenticate(HttpServletRequest req, HttpServletResponse res) 
      throws IOException
   {
      return servletAuthenticate(req, res, true);
   }
   
   /**
    * Perform an authentication for the servlet request URI. Processing any "ticket" or
    * "guest" URL arguments.
    * 
    * @return AuthenticationStatus
    * 
    * @throws IOException
    */
   public AuthenticationStatus servletAuthenticate(HttpServletRequest req, HttpServletResponse res,
         boolean redirectToLoginPage) throws IOException
   {
      AuthenticationStatus status;
      
      // see if a ticket or a force Guest parameter has been supplied
      String ticket = req.getParameter(ARG_TICKET);
      if (ticket != null && ticket.length() != 0)
      {
         status = AuthenticationHelper.authenticate(getServletContext(), req, res, ticket);
      }
      else
      {
         boolean forceGuest = false;
         String guest = req.getParameter(ARG_GUEST);
         if (guest != null)
         {
            forceGuest = Boolean.parseBoolean(guest);
         }
         status = AuthenticationHelper.authenticate(getServletContext(), req, res, forceGuest);
      }
      if (status == AuthenticationStatus.Failure && redirectToLoginPage)
      {
         // authentication failed - now need to display the login page to the user, if asked to
         redirectToLoginPage(req, res, getServletContext());
      }
      
      return status;
   }
   
   /**
    * Redirect to the Login page - saving the current URL which can be redirected back later
    * once the user has successfully completed the authentication process.
    */
   public static void redirectToLoginPage(HttpServletRequest req, HttpServletResponse res, ServletContext sc)
      throws IOException
   {
      // authentication failed - so end servlet execution and redirect to login page
      res.sendRedirect(req.getContextPath() + FACES_SERVLET + Application.getLoginPage(sc));
      
      // save the full requested URL so the login page knows where to redirect too later
      String uri = req.getRequestURI();
      String url = uri;
      if (req.getQueryString() != null && req.getQueryString().length() != 0)
      {
         url += "?" + req.getQueryString();
      }
      if (uri.indexOf(req.getContextPath() + FACES_SERVLET) != -1)
      {
         // if we find a JSF servlet reference in the URI then we need to check if the rest of the
         // JSP specified is valid for a redirect operation after Login has occured.
         int jspIndex = uri.indexOf(BaseServlet.FACES_SERVLET) + BaseServlet.FACES_SERVLET.length();
         if (uri.length() > jspIndex && BaseServlet.validRedirectJSP(uri.substring(jspIndex)))
         {
            req.getSession().setAttribute(LoginBean.LOGIN_REDIRECT_KEY, url);
         }
      }
      else
      {
         req.getSession().setAttribute(LoginBean.LOGIN_REDIRECT_KEY, url);
      }
   }
   
   /**
    * Apply Client and Repository language locale based on the 'Accept-Language' request header
    */
   public static Locale setLanguageFromRequestHeader(HttpServletRequest req)
   {
      Locale locale = null;
      
      // set language locale from browser header
      String acceptLang = req.getHeader("Accept-Language");
      if (acceptLang != null && acceptLang.length() != 0)
      {
         StringTokenizer t = new StringTokenizer(acceptLang, ",; ");
         // get language and convert to java locale format
         String language = t.nextToken().replace('-', '_');
         Application.setLanguage(req.getSession(), language);
         locale = I18NUtil.parseLocale(language);
         I18NUtil.setLocale(locale);
      }
      
      return locale;
   }
   
   /**
    * Apply the headers required to disallow caching of the response in the browser
    */
   public static void setNoCacheHeaders(HttpServletResponse res)
   {
      res.setHeader("Cache-Control", "no-cache");
      res.setHeader("Pragma", "no-cache");
   }
   
   /**
    * Returns true if the specified JSP file is valid for a redirect after login.
    * Only a specific sub-set of the available JSPs are valid to jump directly too after a
    * clean login attempt - e.g. those that do not require JSF bean context setup. This is
    * a limitation of the JSP architecture. The ExternalAccessServlet provides a mechanism to
    * setup the JSF bean context directly for some specific cases.
    * 
    * @param jsp     Filename of JSP to check, for example "/jsp/browse/browse.jsp"
    * 
    * @return true if the JSP is in the list of valid direct URLs, false otherwise
    */
   public static boolean validRedirectJSP(String jsp)
   {
      return validRedirectJSPs.contains(jsp);
   }
   
   /**
    * Resolves the given path elements to a NodeRef in the current repository
    * 
    * @param context Faces context
    * @param args    The elements of the path to lookup
    */
   public static NodeRef resolveWebDAVPath(FacesContext context, String[] args)
   {
      WebApplicationContext wc = FacesContextUtils.getRequiredWebApplicationContext(context);
      return resolveWebDAVPath(wc, args, true);
   }
   
   /**
    * Resolves the given path elements to a NodeRef in the current repository
    * 
    * @param context Faces context
    * @param args    The elements of the path to lookup
    * @param decode  True to decode the arg from UTF-8 format, false for no decoding
    */
   public static NodeRef resolveWebDAVPath(FacesContext context, String[] args, boolean decode)
   {
      WebApplicationContext wc = FacesContextUtils.getRequiredWebApplicationContext(context);
      return resolveWebDAVPath(wc, args, decode);
   }
   
   /**
    * Resolves the given path elements to a NodeRef in the current repository
    * 
    * @param context ServletContext context
    * @param args    The elements of the path to lookup
    */
   public static NodeRef resolveWebDAVPath(ServletContext context, String[] args)
   {
      WebApplicationContext wc = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
      return resolveWebDAVPath(wc, args, true);
   }
   
   /**
    * Resolves the given path elements to a NodeRef in the current repository
    * 
    * @param context ServletContext context
    * @param args    The elements of the path to lookup
    * @param decode  True to decode the arg from UTF-8 format, false for no decoding
    */
   public static NodeRef resolveWebDAVPath(ServletContext context, String[] args, boolean decode)
   {
      WebApplicationContext wc = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
      return resolveWebDAVPath(wc, args, decode);
   }
   
   /**
    * Resolves the given path elements to a NodeRef in the current repository
    * 
    * @param WebApplicationContext Context
    * @param args    The elements of the path to lookup
    * @param decode  True to decode the arg from UTF-8 format, false for no decoding
    */
   private static NodeRef resolveWebDAVPath(WebApplicationContext wc, String[] args, boolean decode)
   {
      NodeRef nodeRef = null;

      List<String> paths = new ArrayList<String>(args.length - 1);
      
      FileInfo file = null;
      try
      {
         // create a list of path elements (decode the URL as we go)
         for (int x = 1; x < args.length; x++)
         {
            paths.add(decode ? URLDecoder.decode(args[x]) : args[x]);
         }
         
         if (logger.isDebugEnabled())
            logger.debug("Attempting to resolve webdav path: " + paths);
         
         // get the company home node to start the search from
         nodeRef = new NodeRef(Repository.getStoreRef(), Application.getCompanyRootId());
         
         m_tenantService = (TenantService) wc.getBean("tenantService");         
         if (m_tenantService !=null && m_tenantService.isEnabled())
         {
        	 if (logger.isDebugEnabled())
        	 {
        	     logger.debug("MT is enabled.");
        	 }
        	 
             NodeService nodeService = (NodeService) wc.getBean("NodeService");
             SearchService searchService = (SearchService) wc.getBean("SearchService");
             NamespaceService namespaceService = (NamespaceService) wc.getBean("NamespaceService");
             
             // TODO: since these constants are used more widely than just the WebDAVServlet, 
             // they should be defined somewhere other than in that servlet
             String m_rootPath = wc.getServletContext().getInitParameter(org.alfresco.repo.webdav.WebDAVServlet.KEY_ROOT_PATH);
             
             // note: rootNodeRef is required (for storeRef part)
             nodeRef = m_tenantService.getRootNode(nodeService, searchService, namespaceService, m_rootPath, nodeRef);
         }
         
         if (paths.size() != 0)
         {
            FileFolderService ffs = (FileFolderService)wc.getBean("FileFolderService");
            file = ffs.resolveNamePath(nodeRef, paths);
            nodeRef = file.getNodeRef();
         }
         
         if (logger.isDebugEnabled())
            logger.debug("Resolved webdav path to NodeRef: " + nodeRef);
      }
      catch (FileNotFoundException fne)
      {
         if (logger.isWarnEnabled())
            logger.warn("Failed to resolve webdav path", fne);
         
         nodeRef = null;
      }
      
      return nodeRef;
   }
   
   /**
    * Resolve a name based into a NodeRef and Filename string
    *  
    * @param sc      ServletContext
    * @param path    'cm:name' based path using the '/' character as a separator
    *  
    * @return PathRefInfo structure containing the resolved NodeRef and filename
    * 
    * @throws IllegalArgumentException
    */
   public final static PathRefInfo resolveNamePath(ServletContext sc, String path)
   {
      StringTokenizer t = new StringTokenizer(path, "/");
      int tokenCount = t.countTokens();
      String[] elements = new String[tokenCount];
      for (int i=0; i<tokenCount; i++)
      {
         elements[i] = t.nextToken();
      }
      
      // process name based path tokens using the webdav path resolving helper 
      NodeRef nodeRef = resolveWebDAVPath(sc, elements, false);
      if (nodeRef == null)
      {
         // unable to resolve path - output helpful error to the user
         throw new IllegalArgumentException("Unable to resolve item Path: " + path);
      }
      
      return new PathRefInfo(nodeRef, elements[tokenCount - 1]);
   }
   
   /**
    * Simple structure class for returning both a NodeRef and Filename String
    * @author Kevin Roast
    */
   public static class PathRefInfo
   {
      PathRefInfo(NodeRef ref, String filename)
      {
         this.NodeRef = ref;
         this.Filename = filename;
      }
      public NodeRef NodeRef;
      public String Filename;
   }
}
