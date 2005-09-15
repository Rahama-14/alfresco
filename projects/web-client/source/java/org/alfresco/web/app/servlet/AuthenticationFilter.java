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
package org.alfresco.web.app.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.app.Application;

/**
 * @author Kevin Roast
 * 
 * Servlet filter responsible for redirecting to the login page for the Web Client if the user
 * does not have a valid ticket.
 * <p>
 * The current ticker is validated for each page request and the login page is shown if the
 * ticker has expired.
 * <p>
 * Note that this filter is only active when the system is running in a servlet container -
 * the AlfrescoFacesPortlet will be used for a JSR-168 Portal environment.
 */
public class AuthenticationFilter implements Filter
{
   /**
    * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
    */
   public void init(FilterConfig config) throws ServletException
   {
      this.context = config.getServletContext();
   }

   /**
    * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
    */
   public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
         throws IOException, ServletException
   {
      // allow the login page to proceed
      if (((HttpServletRequest)req).getRequestURI().endsWith(getLoginPage()) == false)
      {
         if (AuthenticationHelper.authenticate(
               this.context, (HttpServletRequest)req, (HttpServletResponse)res))
         {
            // continue filter chaining
            chain.doFilter(req, res);
         }
      }
      else
      {
         // continue filter chaining
         chain.doFilter(req, res);
      }
   }

   /**
    * @see javax.servlet.Filter#destroy()
    */
   public void destroy()
   {
      // nothing to do
   }
   
   /**
    * @return The login page url
    */
   private String getLoginPage()
   {
      if (this.loginPage == null)
      {
         this.loginPage = Application.getLoginPage(this.context);
      }
      
      return this.loginPage;
   }
   
   
   private String loginPage = null;
   
   private ServletContext context;
}
