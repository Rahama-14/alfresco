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
package org.alfresco.web.scripts.servlet;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.extensions.config.Config;
import org.springframework.extensions.config.ConfigService;
import org.springframework.extensions.surf.util.I18NUtil;
import org.alfresco.web.config.ServerConfigElement;
import org.alfresco.web.config.ServerProperties;
import org.alfresco.web.scripts.RuntimeContainer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;


/**
 * Entry point for Web Scripts
 * 
 * @author davidc
 */
public class WebScriptServlet extends HttpServlet
{
    private static final long serialVersionUID = 4209892938069597860L;

    // Logger
    private static final Log logger = LogFactory.getLog(WebScriptServlet.class);

    // Component Dependencies
    protected RuntimeContainer container;
    protected ServletAuthenticatorFactory authenticatorFactory;
    protected ConfigService configService;

    /** Host Server Configuration */
    protected static ServerProperties serverProperties;

    /* (non-Javadoc)
     * @see javax.servlet.GenericServlet#init()
     */
    @Override
    public void init() throws ServletException
    {
        super.init();
        ApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        configService = (ConfigService)context.getBean("web.config");
        String containerName = getServletConfig().getInitParameter("container");
        if (containerName == null)
        {
            containerName = "webscripts.container";
        }
        container = (RuntimeContainer)context.getBean(containerName);
        
        // retrieve authenticator factory
        String authenticatorId = getInitParameter("authenticator");
        if (authenticatorId != null && authenticatorId.length() > 0)
        {
            Object bean = context.getBean(authenticatorId);
            if (bean == null || !(bean instanceof ServletAuthenticatorFactory))
            {
                throw new ServletException("Initialisation parameter 'authenticator' does not refer to a servlet authenticator factory (" + authenticatorId + ")");
            }
            authenticatorFactory = (ServletAuthenticatorFactory)bean;
        }
        
        // retrieve host server configuration 
        Config config = configService.getConfig("Server");
        serverProperties = (ServerConfigElement)config.getConfigElement(ServerConfigElement.CONFIG_ELEMENT_ID);
        
        // servlet specific initialisation
        initServlet(context);
        
        if (logger.isDebugEnabled())
            logger.debug("Initialised Web Script Servlet (authenticator='" + authenticatorId + "')");
    }

    /* (non-Javadoc) 
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        if (logger.isDebugEnabled())
            logger.debug("Processing request ("  + req.getMethod() + ") " + req.getRequestURL() + (req.getQueryString() != null ? "?" + req.getQueryString() : ""));
        
        if (req.getCharacterEncoding() == null)
        {
            req.setCharacterEncoding("UTF-8");
        }
        
        setLanguageFromRequestHeader(req);
        
        WebScriptServletRuntime runtime = new WebScriptServletRuntime(container, authenticatorFactory, req, res, serverProperties);
        runtime.executeScript();
    }
    
    /**
     * Servlet specific initialisation
     * 
     * @param context
     */
    protected void initServlet(ApplicationContext context)
    {
        // NOOP
    }
    
    /**
     * Apply Client and Repository language locale based on the 'Accept-Language' request header
     */
    public static void setLanguageFromRequestHeader(HttpServletRequest req)
    {
        // set language locale from browser header
        String acceptLang = req.getHeader("Accept-Language");
        if (acceptLang != null && acceptLang.length() != 0)
        {
            StringTokenizer t = new StringTokenizer(acceptLang, ",; ");
            // get language and convert to java locale format
            String language = t.nextToken().replace('-', '_');
            I18NUtil.setLocale(I18NUtil.parseLocale(language));
        }
    }
}
