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
package org.alfresco.repo.webdav.auth;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.Vector;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.sasl.RealmCallback;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.UserTransaction;

import org.alfresco.filesys.ServerConfigurationBean;
import org.alfresco.jlan.server.auth.kerberos.KerberosDetails;
import org.alfresco.jlan.server.auth.kerberos.SessionSetupPrivilegedAction;
import org.alfresco.jlan.server.auth.spnego.NegTokenInit;
import org.alfresco.jlan.server.auth.spnego.NegTokenTarg;
import org.alfresco.jlan.server.auth.spnego.OID;
import org.alfresco.jlan.server.auth.spnego.SPNEGO;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.NTLMMode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ietf.jgss.Oid;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * WebDAV Kerberos Authentication Filter Class
 * 
 * @author GKSpencer
 */
public class KerberosAuthenticationFilter implements Filter, CallbackHandler
{
    // Constants
    //
    // Default login configuration entry name
    
    private static final String LoginConfigEntry    = "AlfrescoHTTP";
    
    // Authenticated user session object name

    public final static String AUTHENTICATION_USER = "_alfDAVAuthTicket";
    
    // Allow an authenitcation ticket to be passed as part of a request to bypass authentication
    
    private static final String ARG_TICKET = "ticket";
    
    // Debug logging
    
    private static Log logger = LogFactory.getLog(KerberosAuthenticationFilter.class);
    
    // Servlet context, required to get authentication service
    
    private ServletContext m_context;
    
    // File server configuration
    
    private ServerConfigurationBean m_srvConfig;
    
    // Various services required by the Kerberos authenticator
    
    private AuthenticationService m_authService;
    private AuthenticationComponent m_authComponent;
    private PersonService m_personService;
    private NodeService m_nodeService;
    private TransactionService m_transactionService;
    
    // Login page address
    
    private String m_loginPage;

    // Local server name, from either the file servers config or DNS host name
    
    private String m_srvName;
    
    // Kerberos settings
    //
    // Account name and password for server ticket
    //
    // The account name must be built from the HTTP server name, in the format :-
    //
    //      HTTP/<server_name>@<realm>
    
    private String m_accountName;
    private String m_password;
    
    // Kerberos realm and KDC address
    
    private String m_krbRealm;
    private String m_krbKDC;
    
    // Login configuration entry name
    
    private String m_loginEntryName = LoginConfigEntry; 

    // Server login context
    
    private LoginContext m_loginContext;
    
    // SPNEGO NegTokenInit blob, sent to the client in the SMB negotiate response
    
    private byte[] m_negTokenInit;
    
    /**
     * Initialize the filter
     * 
     * @param args FilterConfig
     * @exception ServletException
     */
    public void init(FilterConfig args) throws ServletException
    {
        // Save the servlet context, needed to get hold of the authentication service
        
        m_context = args.getServletContext();

        // Setup the authentication context

        WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(m_context);
        
        ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        m_nodeService = serviceRegistry.getNodeService();
        m_transactionService = serviceRegistry.getTransactionService();

        m_authService = (AuthenticationService) ctx.getBean("AuthenticationService");
        m_authComponent = (AuthenticationComponent) ctx.getBean("AuthenticationComponent");
        m_personService = (PersonService) ctx.getBean("personService");
        
        m_srvConfig = (ServerConfigurationBean) ctx.getBean(ServerConfigurationBean.SERVER_CONFIGURATION);
        
        // Check that the authentication component supports the required mode
        
        if ( m_authComponent.getNTLMMode() != NTLMMode.MD4_PROVIDER &&
                m_authComponent.getNTLMMode() != NTLMMode.PASS_THROUGH)
        {
            throw new ServletException("Required authentication mode not available");
        }
        
        // Get the local server name, try the file server config first
        
        if ( m_srvConfig != null)
        {
            m_srvName = m_srvConfig.getServerName();
            
            if ( m_srvName == null)
            {
                // CIFS server may not be running so the local server name has not been set, generate
                // a server name
                
                m_srvName = m_srvConfig.getLocalServerName(true) + "_A";
            }
        }
        else
        {
            // Get the host name
            
            try
            {
                // Get the local host name
                
                m_srvName = InetAddress.getLocalHost().getHostName();
                
                // Strip any domain name
                
                int pos = m_srvName.indexOf(".");
                if ( pos != -1)
                    m_srvName = m_srvName.substring(0, pos - 1);
            }
            catch (UnknownHostException ex)
            {
                // Log the error
                
                if ( logger.isErrorEnabled())
                    logger.error("Kerberos filter, error getting local host name", ex);
            }
            
        }
        
        // Check if the server name is valid
        
        if ( m_srvName == null || m_srvName.length() == 0)
            throw new ServletException("Failed to get local server name");

        // Check if Kerberos is enabled, get the Kerberos KDC address

        String kdcAddress = args.getInitParameter("KDC");
        
        if (kdcAddress != null && kdcAddress.length() > 0)
        {
            // Set the Kerberos KDC address
            
            m_krbKDC = kdcAddress;
        
            // Get the Kerberos realm
            
            String krbRealm = args.getInitParameter("Realm");
            if ( krbRealm != null && krbRealm.length() > 0)
            {
                // Set the Kerberos realm
                
                m_krbRealm = krbRealm;
            }
            else
                throw new ServletException("Kerberos realm not specified");
            
            // Get the HTTP service account password
            
            String srvPassword = args.getInitParameter("Password");
            if ( srvPassword != null && srvPassword.length() > 0)
            {
                // Set the HTTP service account password
                
                m_password = srvPassword;
            }
            else
                throw new ServletException("HTTP service account password not specified");
            
            // Get the login configuration entry name
            
            String loginEntry = args.getInitParameter("LoginEntry");
            
            if ( loginEntry != null)
            {
                if ( loginEntry.length() > 0)
                {
                    // Set the login configuration entry name to use
                    
                    m_loginEntryName = loginEntry;
                }
                else
                    throw new ServletException("Invalid login entry specified");
            }
            
            // Get the local host name
            
            String localName = null;
            
            try
            {
            	localName = InetAddress.getLocalHost().getCanonicalHostName();
            }
            catch ( UnknownHostException ex)
            {
            	throw new ServletException( "Failed to get local host name");
            }
            
            // Create a login context for the HTTP server service
            
            try
            {
                // Login the HTTP server service
                
                m_loginContext = new LoginContext( m_loginEntryName, this);
                m_loginContext.login();
                
                // DEBUG
                
                if ( logger.isDebugEnabled())
                	logger.debug( "HTTP Kerberos login successful");
            }
            catch ( LoginException ex)
            {
                // Debug
                
                if ( logger.isErrorEnabled())
                    logger.error("HTTP Kerberos web filter error", ex);
                
                throw new ServletException("Failed to login HTTP server service");
            }
            
            // Get the HTTP service account name from the subject
            
            Subject subj = m_loginContext.getSubject();
            Principal princ = subj.getPrincipals().iterator().next();
            
            m_accountName = princ.getName();
            
            // DEBUG
            
            if ( logger.isDebugEnabled())
            	logger.debug("Logged on using principal " + m_accountName);
            
            // Create the Oid list for the SPNEGO NegTokenInit, include NTLMSSP for fallback
            
            Vector<Oid> mechTypes = new Vector<Oid>();

            mechTypes.add(OID.KERBEROS5);
            mechTypes.add(OID.MSKERBEROS5);
        
            // Build the SPNEGO NegTokenInit blob
    
            try
            {
                // Build the mechListMIC principle
                //
                // Note: This field is not as specified
                
                String mecListMIC = null;
                
                StringBuilder mic = new StringBuilder();
                mic.append( localName);
                mic.append("$@");
                mic.append( m_krbRealm);
                
                mecListMIC = mic.toString();
                
                // Build the SPNEGO NegTokenInit that contains the authentication types that the HTTP server accepts
                
                NegTokenInit negTokenInit = new NegTokenInit(mechTypes, mecListMIC);
                
                // Encode the NegTokenInit blob
                
                m_negTokenInit = negTokenInit.encode();
            }
            catch (IOException ex)
            {
                // Debug
                
                if ( logger.isErrorEnabled())
                    logger.error("Error creating SPNEGO NegTokenInit blob", ex);
                
                throw new ServletException("Failed to create SPNEGO NegTokenInit blob");
            }
        }        
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
    public void doFilter(ServletRequest sreq, ServletResponse sresp, FilterChain chain) throws IOException,
            ServletException
    {
        // Get the HTTP request/response/session
        
        HttpServletRequest req = (HttpServletRequest) sreq;
        HttpServletResponse resp = (HttpServletResponse) sresp;
        
        HttpSession httpSess = req.getSession(true);

        // Check if there is an authorization header with an SPNEGO security blob
        
        String authHdr = req.getHeader("Authorization");
        boolean reqAuth = false;
        
        if ( authHdr != null && authHdr.startsWith("Negotiate"))
            reqAuth = true;
        
        // Check if the user is already authenticated
        
        WebDAVUser user = (WebDAVUser) httpSess.getAttribute( AUTHENTICATION_USER);
        
        if ( user != null && reqAuth == false)
        {
            try
            {
                // Debug
                
                if ( logger.isDebugEnabled())
                    logger.debug("User " + user.getUserName() + " validate ticket");
                
                // Validate the user ticket
                
                m_authService.validate( user.getTicket());
                reqAuth = false;
            }
            catch (AuthenticationException ex)
            {
                if ( logger.isErrorEnabled())
                    logger.error("Failed to validate user " + user.getUserName(), ex);
                
                reqAuth = true;
            }
        }

        // If the user has been validated and we do not require re-authentication then continue to
        // the next filter
        
        if ( reqAuth == false && user != null)
        {
            // Debug
            
            if ( logger.isDebugEnabled())
                logger.debug("Authentication not required, chaining ...");
            
            // Chain to the next filter
            
            chain.doFilter(sreq, sresp);
            return;
        }

        // Check the authorization header
        
        if ( authHdr == null) {

        	// Check if the request includes an authentication ticket
            
        	String ticket = req.getParameter( ARG_TICKET);
        	
        	if ( ticket != null &&  ticket.length() > 0)
        	{
            	// Debug
                
                if ( logger.isDebugEnabled())
                    logger.debug("Logon via ticket from " + req.getRemoteHost() + " (" +
                            req.getRemoteAddr() + ":" + req.getRemotePort() + ")" + " ticket=" + ticket);
                
        		UserTransaction tx = null;
        	    try
        	    {
        	    	// Validate the ticket
        	    	  
        	    	m_authService.validate(ticket);

        	    	// Need to create the User instance if not already available
        	    	  
        	        String currentUsername = m_authService.getCurrentUserName();

        	        // Start a transaction
        	          
      	            tx = m_transactionService.getUserTransaction();
        	        tx.begin();
        	            
        	        NodeRef personRef = m_personService.getPerson(currentUsername);
        	        user = new WebDAVUser( currentUsername, m_authService.getCurrentTicket(), personRef);
        	        NodeRef homeRef = (NodeRef) m_nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);
        	            
        	        // Check that the home space node exists - else Login cannot proceed
        	            
        	        if (m_nodeService.exists(homeRef) == false)
        	        {
        	        	throw new InvalidNodeRefException(homeRef);
        	        }
        	        user.setHomeNode(homeRef);
        	            
        	        tx.commit();
        	        tx = null; 
        	            
        	        // Store the User object in the Session - the authentication servlet will then proceed
        	            
        	        req.getSession().setAttribute( AUTHENTICATION_USER, user);

        	        // Chain to the next filter
                    
                    chain.doFilter(sreq, sresp);
        	        return;
        	    }
            	catch (AuthenticationException authErr)
            	{
            		// Clear the user object to signal authentication failure
            		
            		user = null;
            	}
            	catch (Throwable e)
            	{
            		// Clear the user object to signal authentication failure
            		
            		user = null;
            	}
            	finally
            	{
            		try
            	    {
            			if (tx != null)
            	        {
            				tx.rollback();
           	        	}
            	    }
            	    catch (Exception tex)
            	    {
            	    }
            	}
        	}
        	
            // Debug
            
            if ( logger.isDebugEnabled())
                logger.debug("New Kerberos auth request from " + req.getRemoteHost() + " (" +
                        req.getRemoteAddr() + ":" + req.getRemotePort() + ")");
            
            // Send back a request for SPNEGO authentication
            
            resp.setHeader("WWW-Authenticate", "Negotiate");
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            
            resp.flushBuffer();
        }
        else
        {
            // Decode the received SPNEGO blob and validate
            
            final byte[] spnegoByts = Base64.decodeBase64( authHdr.substring(10).getBytes());
         
            //  Check the received SPNEGO token type

            int tokType = -1;
            
            try
            {
                tokType = SPNEGO.checkTokenType( spnegoByts, 0, spnegoByts.length);
            }
            catch ( IOException ex)
            {
            }

            // Check for a NegTokenInit blob
            
            if ( tokType == SPNEGO.NegTokenInit)
            {
                //  Parse the SPNEGO security blob to get the Kerberos ticket
                
                NegTokenInit negToken = new NegTokenInit();
                
                try
                {
                    // Decode the security blob
                    
                    negToken.decode( spnegoByts, 0, spnegoByts.length);

                    //  Determine the authentication mechanism the client is using and logon
                    
                    String oidStr = null;
                    if ( negToken.numberOfOids() > 0)
                        oidStr = negToken.getOidAt( 0).toString();
                    
                    if (  oidStr != null && (oidStr.equals( OID.ID_MSKERBEROS5) || oidStr.equals(OID.ID_KERBEROS5)))
                    {
                        //  Kerberos logon
                        
                        if ( doKerberosLogon( negToken, req, resp, httpSess) != null)
                        {
                            // Allow the user to access the requested page
                                
                            chain.doFilter( req, resp);
                        }
                        else
                        {
                            // Send back a request for SPNEGO authentication
                            
                            resp.setHeader("WWW-Authenticate", "Negotiate");
                            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            
                            resp.flushBuffer();
                        }	
                    }
                }
                catch ( IOException ex)
                {
                    // Log the error
                    
                	if ( logger.isDebugEnabled())
                    	logger.debug(ex);
                }
            }
            else
            {
                //  Unknown SPNEGO token type
                
            	if ( logger.isDebugEnabled())
            		logger.debug( "Unknown SPNEGO token type");

                // Send back a request for SPNEGO authentication
                
                resp.setHeader("WWW-Authenticate", "Negotiate");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                
                resp.flushBuffer();
            }
        }
    }

    /**
     * Delete the servlet filter
     */
    public void destroy()
    {
    }
    
    /**
     * JAAS callback handler
     * 
     * @param callbacks Callback[]
     * @exception IOException
     * @exception UnsupportedCallbackException
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
        // Process the callback list
        
        for (int i = 0; i < callbacks.length; i++)
        {
            // Request for user name
            
            if (callbacks[i] instanceof NameCallback)
            {
                NameCallback cb = (NameCallback) callbacks[i];
                cb.setName(m_accountName);
            }
            
            // Request for password
            else if (callbacks[i] instanceof PasswordCallback)
            {
                PasswordCallback cb = (PasswordCallback) callbacks[i];
                cb.setPassword(m_password.toCharArray());
            }
            
            // Request for realm
            
            else if (callbacks[i] instanceof RealmCallback)
            {
                RealmCallback cb = (RealmCallback) callbacks[i];
                cb.setText(m_krbRealm);
            }
            else
            {
                throw new UnsupportedCallbackException(callbacks[i]);
            }
        }
    }
    
    /**
     * Perform a Kerberos login and return an SPNEGO response
     * 
     * @param negToken NegTokenInit
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @param httpSess HttpSession
     * @return NegTokenTarg
     */
    private final NegTokenTarg doKerberosLogon( NegTokenInit negToken, HttpServletRequest req, HttpServletResponse resp, HttpSession httpSess)
    {
        //  Authenticate the user
        
        KerberosDetails krbDetails = null;
        NegTokenTarg negTokenTarg = null;
        
        UserTransaction tx = null;
        
        try
        {
            //  Run the session setup as a privileged action
            
            SessionSetupPrivilegedAction sessSetupAction = new SessionSetupPrivilegedAction( m_accountName, negToken.getMechtoken());
            Object result = Subject.doAs( m_loginContext.getSubject(), sessSetupAction);
    
            if ( result != null)
            {
                // Access the Kerberos response
                
                krbDetails = (KerberosDetails) result;
                
                // Create the NegTokenTarg response blob
                
                negTokenTarg = new NegTokenTarg( SPNEGO.AcceptCompleted, OID.KERBEROS5, krbDetails.getResponseToken());
                
                // Check if the user has been authenticated, if so then setup the user environment
                
                if ( negTokenTarg != null)
                {
                	// Create a read transaction
                
                    tx = m_transactionService.getUserTransaction();
                    
                    NodeRef homeSpaceRef = null;
                    WebDAVUser user = null;
                    String userName = null;
                    
                    try
                    {
                    	// Start the transaction
                    
                        tx.begin();
                        
                        // Setup User object and Home space ID etc.
                        
                        NodeRef personNodeRef = m_personService.getPerson( krbDetails.getUserName());

                        // Use the system user to do the user name lookup
                        
                        m_authComponent.setSystemUserAsCurrentUser();
                        
                        // User name should match the uid in the person entry found
                        
                        userName = (String) m_nodeService.getProperty(personNodeRef, ContentModel.PROP_USERNAME);
                        AuthenticationUtil.setCurrentUser( userName);
                        String currentTicket = m_authService.getCurrentTicket();
                        user = new WebDAVUser(userName, currentTicket, personNodeRef);
                        
                        homeSpaceRef = (NodeRef) m_nodeService.getProperty( personNodeRef, ContentModel.PROP_HOMEFOLDER);
                        user.setHomeNode( homeSpaceRef);
                        
                        // Commit
                        
                        tx.commit();
                    }
                    catch (Throwable ex)
                    {
                        try
                        {
                            tx.rollback();
                        }
                        catch (Exception ex2)
                        {
                            logger.error("Failed to rollback transaction", ex2);
                        }
                        if(ex instanceof RuntimeException)
                        {
                            throw (RuntimeException)ex;
                        }
                        else if(ex instanceof IOException)
                        {
                            throw (IOException)ex;
                        }
                        else if(ex instanceof ServletException)
                        {
                            throw (ServletException)ex;
                        }
                        else
                        {
                            throw new RuntimeException("Authentication setup failed", ex);
                        }
                    }
                    
                    // Store the user
                    
                    httpSess.setAttribute(AUTHENTICATION_USER, user);

                    // Debug
                    
                    if ( logger.isDebugEnabled())
                        logger.debug("User " + userName + " logged on via Kerberos");

                }
            }
            else
            {
            	// Debug
            	
            	if ( logger.isDebugEnabled())
            		logger.debug( "No SPNEGO response, Kerberos logon failed");
            }
        }
        catch (Exception ex)
        {
            // Log the error

        	if ( logger.isDebugEnabled())
        		logger.debug("Kerberos logon error", ex);
        }
    
        // Return the response SPNEGO blob
        
        return negTokenTarg;
    }
    
    /**
     * Map the case insensitive logon name to the internal person object user name
     * 
     * @param userName String
     * @return String
     */
    protected final String mapUserNameToPerson(String userName)
    {
        // Get the home folder for the user
        
        UserTransaction tx = m_transactionService.getUserTransaction();
        String personName = null;
        
        try
        {
            tx.begin();
            personName = m_personService.getUserIdentifier( userName);
            tx.commit();
        }
        catch (Throwable ex)
        {
            try
            {
                tx.rollback();
            }
            catch (Throwable ex2)
            {
                logger.error("Failed to rollback transaction", ex2);
            }
            
            // Re-throw the exception
            
            if (ex instanceof RuntimeException)
            {
                throw (RuntimeException) ex;
            }
            else
            {
                throw new RuntimeException("Error during execution of transaction.", ex);
            }
        }
        
        // Return the person name
        
        return personName;
    }
}
