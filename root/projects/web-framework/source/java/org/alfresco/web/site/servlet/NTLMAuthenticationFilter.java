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
package org.alfresco.web.site.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.config.ConfigService;
import org.alfresco.connector.Connector;
import org.alfresco.connector.ConnectorService;
import org.alfresco.connector.Response;
import org.alfresco.connector.exception.RemoteConfigException;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.jlan.server.auth.ntlm.NTLM;
import org.alfresco.jlan.server.auth.ntlm.NTLMLogonDetails;
import org.alfresco.jlan.server.auth.ntlm.NTLMMessage;
import org.alfresco.jlan.server.auth.ntlm.Type1NTLMMessage;
import org.alfresco.jlan.server.auth.ntlm.Type2NTLMMessage;
import org.alfresco.jlan.server.auth.ntlm.Type3NTLMMessage;
import org.alfresco.util.Base64;
import org.alfresco.web.config.RemoteConfigElement;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.site.AuthenticationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * NTLM Authentication Filter Class for web-tier.
 * 
 * @see org.alfresco.web.app.servlet.NTLMAuthenticationFilter
 * 
 * @author Kevin Roast
 */
public class NTLMAuthenticationFilter implements Filter 
{
    private static Log logger = LogFactory.getLog(NTLMAuthenticationFilter.class);
    
    // NTLM authentiation request/response headers
    private static final String AUTH_NTLM = "NTLM";
    private static final String HEADER_WWWAUTHENTICATE = "WWW-Authenticate";
    
    // NTLM authentication session object names
    private static final String NTLM_AUTH_SESSION = "_alfNTLMAuthSess";
    private static final String NTLM_AUTH_DETAILS = "_alfNTLMDetails";
    private static final String LOGIN_PAGE_PASSTHROUGH = "_alfLoginPassthrough";
    
    private RemoteConfigElement config;
    private ConnectorService connectorService;
    private String endpoint;
    
    
    /**
     * Initialize the filter
     */
    public void init(FilterConfig args) throws ServletException
    {
        ApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(args.getServletContext());
        ConfigService configService = (ConfigService)context.getBean("web.config");
        
        // retrieve the remote configuration
        this.config = (RemoteConfigElement)configService.getConfig("Remote").getConfigElement("remote");
        
        // retrieve the connector service
        this.connectorService = (ConnectorService)context.getBean("connector.service");
        
        // get the endpoint id to use
        this.endpoint = args.getInitParameter("endpoint");
        
        if (logger.isInfoEnabled())
            logger.info("NTLMAuthenticationFilter initialised.");
    }

    /**
     * Run the filter
     * 
     * @param sreq ServletRequest
     * @param sresp ServletResponse
     * @param chain FilterChain
     * @exception IOException
     * @exception ServletException
     */
    public void doFilter(ServletRequest sreq, ServletResponse sresp, FilterChain chain)
        throws IOException, ServletException
    {
        // Get the HTTP request/response/session
        HttpServletRequest req = (HttpServletRequest) sreq;
        HttpServletResponse res = (HttpServletResponse) sresp;
        HttpSession session = req.getSession(true);
        
        // Check if there is an authorization header with an NTLM security blob
        String authHdr = req.getHeader("Authorization");
        boolean reqAuth = (authHdr != null && authHdr.startsWith(AUTH_NTLM));
        
        //
        // TODO: validate ticket via REST call? reqAuth=true failure...
        //
        
        // If user exists and we do not require re-authentication then continue to next filter
        if (!reqAuth && AuthenticationUtil.isAuthenticated(req))
        {
            if (logger.isDebugEnabled())
                logger.debug("Authentication not required, chaining ...");
            
            // Chain to the next filter
            chain.doFilter(sreq, sresp);
            return;
        }
        
        // Check if the login page is being accessed, do not intercept the login page
        if (session.getAttribute(LOGIN_PAGE_PASSTHROUGH) != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Login page requested, chaining ...");
            
            // Chain to the next filter
            chain.doFilter(sreq, sresp);
            return;
        }
        
        // Check if the browser is Opera, if so then display the login page as Opera does not
        // support NTLM and displays an error page if a request to use NTLM is sent to it
        String userAgent = req.getHeader("user-agent");
        if (userAgent != null && userAgent.indexOf("Opera ") != -1)
        {
            if (logger.isDebugEnabled())
                logger.debug("Opera detected, redirecting to login page");

            redirectToLoginPage(req, res);
            return;
        }
        
        // Check the authorization header
        if (authHdr == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("New NTLM auth request from " + req.getRemoteHost() + " (" +
                             req.getRemoteAddr() + ":" + req.getRemotePort() + ")");
            
            // Send back a request for NTLM authentication
            res.setHeader(HEADER_WWWAUTHENTICATE, AUTH_NTLM);
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.flushBuffer();
        }
        else
        {
            // Decode the received NTLM blob and validate
            final byte[] authHdrByts = authHdr.substring(5).getBytes();
            final byte[] ntlmByts = Base64.decode(authHdrByts);
            int ntlmTyp = NTLMMessage.isNTLMType(ntlmByts);
            
            if (ntlmTyp == NTLM.Type1)
            {
                // Process the type 1 NTLM message
                Type1NTLMMessage type1Msg = new Type1NTLMMessage(ntlmByts);
                processType1(type1Msg, req, res, session);
            }
            else if (ntlmTyp == NTLM.Type3)
            {
                // Process the type 3 NTLM message
                Type3NTLMMessage type3Msg = new Type3NTLMMessage(ntlmByts);
                processType3(type3Msg, req, res, session, chain);
            }
            else
            {
                if (logger.isDebugEnabled())
                    logger.debug("NTLM not handled, redirecting to login page");
                
                redirectToLoginPage(req, res);
            }
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy()
    {
    }

    /**
     * Process a type 1 NTLM message
     * 
     * @param type1Msg Type1NTLMMessage
     * @param req HttpServletRequest
     * @param res HttpServletResponse
     * @param session HttpSession
     * 
     * @exception IOException
     */
    private void processType1(Type1NTLMMessage type1Msg, HttpServletRequest req, HttpServletResponse res,
            HttpSession session) throws IOException
    {
        if (logger.isDebugEnabled())
            logger.debug("Received type1 " + type1Msg);
        
        // Get the existing NTLM details
        NTLMLogonDetails ntlmDetails = (NTLMLogonDetails)session.getAttribute(NTLM_AUTH_DETAILS);
        
        // Check if cached logon details are available
        if (ntlmDetails != null && ntlmDetails.hasType2Message() && ntlmDetails.hasNTLMHashedPassword())
        {
            // Get the authentication server type2 response
            Type2NTLMMessage cachedType2 = ntlmDetails.getType2Message();
            
            byte[] type2Bytes = cachedType2.getBytes();
            String ntlmBlob = "NTLM " + new String(Base64.encodeBytes(type2Bytes, Base64.DONT_BREAK_LINES));
            
            if (logger.isDebugEnabled())
                logger.debug("Sending cached NTLM type2 to client - " + cachedType2);
            
            // Send back a request for NTLM authentication
            res.setHeader(HEADER_WWWAUTHENTICATE, ntlmBlob);
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.flushBuffer();
        }
        else
        {
            // Clear any cached logon details
            session.removeAttribute(NTLM_AUTH_DETAILS);
            
            try
            {
                Connector conn = connectorService.getConnector(this.endpoint, session);
                Response remoteRes = conn.call("/touch", null, req, null);
                if (Status.STATUS_UNAUTHORIZED == remoteRes.getStatus().getCode())
                {
                    String authHdr = remoteRes.getStatus().getHeaders().get(HEADER_WWWAUTHENTICATE);
                    if (authHdr.startsWith(AUTH_NTLM))
                    {
                        // Decode the received NTLM blob and validate
                        final byte[] authHdrByts = authHdr.substring(5).getBytes();
                        final byte[] ntlmByts = Base64.decode(authHdrByts);
                        int ntlmType = NTLMMessage.isNTLMType(ntlmByts);
                        if (ntlmType == NTLM.Type2)
                        {
                            // Retrieve the type2 NTLM message
                            Type2NTLMMessage type2Msg = new Type2NTLMMessage(ntlmByts);
                            
                            // Store the NTLM logon details, cache the type2 message, and token if using passthru
                            ntlmDetails = new NTLMLogonDetails();
                            ntlmDetails.setType2Message(type2Msg);
                            //ntlmDetails.setAuthenticationToken(authToken);
                            session.setAttribute(NTLM_AUTH_DETAILS, ntlmDetails);
                            
                            if (logger.isDebugEnabled())
                                logger.debug("Sending NTLM type2 to client - " + type2Msg);
                            
                            // Send back a request for NTLM authentication
                            byte[] type2Bytes = type2Msg.getBytes();
                            String ntlmBlob = "NTLM " + new String(Base64.encodeBytes(type2Bytes, Base64.DONT_BREAK_LINES));
                            
                            res.setHeader(HEADER_WWWAUTHENTICATE, ntlmBlob);
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.flushBuffer();
                        }
                        else
                        {
                            if (logger.isDebugEnabled())
                                logger.debug("Unexpected NTLM message type from repository: NTLMType" + ntlmType);
                            redirectToLoginPage(req, res);
                        }
                    }
                    else
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("Unexpected response from repository: WWW-Authenticate:" + authHdr);
                        redirectToLoginPage(req, res);
                    }
                }
                else
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Unexpected response from repository: " + remoteRes.getStatus().getMessage());
                    redirectToLoginPage(req, res);
                }
            }
            catch (RemoteConfigException rerr)
            {
                throw new AlfrescoRuntimeException("Incorrectly configured endpoint ID: " + this.endpoint);
            }
        }
    }
    
    /**
     * Process a type 3 NTLM message
     * 
     * @param type3Msg Type3NTLMMessage
     * @param req HttpServletRequest
     * @param res HttpServletResponse
     * @param session HttpSession
     * @param chain FilterChain
     * @exception IOException
     * @exception ServletException
     */
    private void processType3(Type3NTLMMessage type3Msg, HttpServletRequest req, HttpServletResponse res,
            HttpSession session, FilterChain chain) throws IOException, ServletException
    {
        if (logger.isDebugEnabled())
            logger.debug("Received type3 " + type3Msg);
        
        // Get the existing NTLM details
        NTLMLogonDetails ntlmDetails = (NTLMLogonDetails) session.getAttribute(NTLM_AUTH_DETAILS);
        String userId = AuthenticationUtil.getUserId(req);
        
        // Get the NTLM logon details
        String userName = type3Msg.getUserName();
        String workstation = type3Msg.getWorkstation();
        String domain = type3Msg.getDomain();
        
        boolean authenticated = false;
        
        // Check if we are using cached details for the authentication
        if (userId != null && ntlmDetails != null && ntlmDetails.hasNTLMHashedPassword())
        {
            // Check if the received NTLM hashed password matches the cached password
            byte[] ntlmPwd = type3Msg.getNTLMHash();
            byte[] cachedPwd = ntlmDetails.getNTLMHashedPassword();
            
            if (ntlmPwd != null)
            {
                if (ntlmPwd.length == cachedPwd.length)
                {
                    authenticated = true;
                    for (int i = 0; i < ntlmPwd.length; i++)
                    {
                        if (ntlmPwd[i] != cachedPwd[i])
                        {
                            authenticated = false;
                            break;
                        }
                    }
                }
            }
            
            if (logger.isDebugEnabled())
                logger.debug("Using cached NTLM hash, authenticated = " + authenticated);
            
            //
            // TODO: validate ticket via REST call? Redirect to login on failure...
            //
            
            // Allow the user to access the requested page
            chain.doFilter( req, res);
            return;
        }
        else
        {
            try
            {
                Connector conn = connectorService.getConnector(this.endpoint, session);
                Response remoteRes = conn.call("/touch", null, req, null);
                if (Status.STATUS_UNAUTHORIZED == remoteRes.getStatus().getCode())
                {
                    String authHdr = remoteRes.getStatus().getHeaders().get(HEADER_WWWAUTHENTICATE);
                    if (authHdr.equals(AUTH_NTLM))
                    {
                        // authentication failed on repo side - being login process again
                        res.setHeader(HEADER_WWWAUTHENTICATE, AUTH_NTLM);
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.flushBuffer();
                    }
                    else
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("Unexpected response from repository: WWW-Authenticate:" + authHdr);
                        redirectToLoginPage(req, res);
                    }
                }
                else if (Status.STATUS_OK == remoteRes.getStatus().getCode() ||
                         Status.STATUS_TEMPORARY_REDIRECT == remoteRes.getStatus().getCode())
                {
                    // Update the NTLM logon details in the session
                    if (ntlmDetails == null)
                    {
                        // No cached NTLM details
                        ntlmDetails = new NTLMLogonDetails(userName, workstation, domain, false, null);
                        ntlmDetails.setNTLMHashedPassword(type3Msg.getNTLMHash());
                        session.setAttribute(NTLM_AUTH_DETAILS, ntlmDetails);
                        
                        if (logger.isDebugEnabled())
                            logger.debug("No cached NTLM details, created");
                    }
                    else
                    {
                        // Update the cached NTLM details
                        ntlmDetails.setDetails(userName, workstation, domain, false, null);
                        ntlmDetails.setNTLMHashedPassword(type3Msg.getNTLMHash());
                        
                        if (logger.isDebugEnabled())
                            logger.debug("Updated cached NTLM details");
                    }
                    
                    if (logger.isDebugEnabled())
                        logger.debug("User logged on via NTLM, " + ntlmDetails);
                    
                    // Create User ID in session so the web-framework dispatcher knows we have logged in
                    AuthenticationUtil.login(req, userName);
                    
                    // Allow the user to access the requested page
                    chain.doFilter(req, res);
                }
                else
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Unexpected response from repository: " + remoteRes.getStatus().getMessage());
                    redirectToLoginPage(req, res);
                }
            }
            catch (RemoteConfigException rerr)
            {
                throw new AlfrescoRuntimeException("Incorrectly configured endpoint: " + this.endpoint);
            }
        }
    }
    
    /**
     * Redirect to the root of the website - ignore further NTLM auth requests
     */
    private void redirectToLoginPage(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        // Redirect to the root of the website - mark the session for login passthrough
        // as we ignore requests for login page during NTLM processing.
        req.getSession().setAttribute(LOGIN_PAGE_PASSTHROUGH, Boolean.TRUE);
        res.sendRedirect(req.getContextPath());
    }
}
