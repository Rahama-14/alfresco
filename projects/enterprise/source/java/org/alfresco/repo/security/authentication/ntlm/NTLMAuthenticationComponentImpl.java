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

package org.alfresco.repo.security.authentication.ntlm;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;
import java.util.Hashtable;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthenticationServiceException;
import net.sf.acegisecurity.BadCredentialsException;
import net.sf.acegisecurity.CredentialsExpiredException;
import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.GrantedAuthorityImpl;
import net.sf.acegisecurity.UserDetails;
import net.sf.acegisecurity.context.Context;
import net.sf.acegisecurity.context.ContextHolder;
import net.sf.acegisecurity.context.security.SecureContext;
import net.sf.acegisecurity.context.security.SecureContextImpl;
import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import net.sf.acegisecurity.providers.dao.User;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.filesys.server.auth.PasswordEncryptor;
import org.alfresco.filesys.server.auth.passthru.AuthenticateSession;
import org.alfresco.filesys.server.auth.passthru.PassthruServers;
import org.alfresco.filesys.smb.SMBException;
import org.alfresco.filesys.smb.SMBStatus;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.NTLMMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * NTLM Authentication Component Class
 * 
 * <p>Provides authentication using passthru to a Windows server(s)/domain controller(s) using the accounts
 * defined on the passthru server to validate users.
 * 
 * @author GKSpencer
 */
public class NTLMAuthenticationComponentImpl implements AuthenticationComponent
{
    // Logging
    
    private static final Log logger = LogFactory.getLog("org.alfresco.passthru.auth");

    // Name of the system user
    
    private static final String SYSTEM_USER_NAME = "System";

    // Constants
    //
    // Standard authorities
    
    public static final String NTLMAuthorityGuest         = "Guest";
    public static final String NTLMAuthorityAdministrator = "Administrator";
    
    // Active session timeout
    
    private static final long DefaultSessionTimeout = 60000L;   // 1 minute
    private static final long MinimumSessionTimeout = 5000L;    // 5 seconds
    
    // Passthru authentication servers
    
    private PassthruServers m_passthruServers;
    
    // Password encryptor for generating password hash for local authentication
    
    private PasswordEncryptor m_encryptor;
    
    // Allow guest access
    
    private boolean m_allowGuest;
    
    // Table of currently active passthru authentications and the associated authentication session
    //
    // If the two authentication stages are not completed within a reasonable time the authentication
    // session will be closed by the reaper thread.
    
    private Hashtable<NTLMPassthruToken,AuthenticateSession> m_passthruSessions;
    
    // Active authentication session timeout, in milliseconds
    
    private long m_passthruSessTmo = DefaultSessionTimeout;
    
    // Authentication session reaper thread
    
    private PassthruReaperThread m_reaperThread;
    
    /**
     * Passthru Session Repear Thread
     */
    class PassthruReaperThread extends Thread
    {
        // Thread shutdown request flag
        
        private boolean m_ishutdown;

        // Reaper wakeup interval, in milliseconds
        
        private long m_wakeupInterval = m_passthruSessTmo / 2;
        
        /**
         * Default constructor
         */
        PassthruReaperThread()
        {
            setDaemon(true);
            setName("PassthruReaper");
            start();
        }
        
        /**
         * Set the wakeup interval
         * 
         * @param wakeup long
         */
        public final void setWakeup(long wakeup)
        {
            m_wakeupInterval = wakeup;
        }
        
        /**
         * Main thread code
         */
        public void run()
        {
            // Loop until shutdown
            
            m_ishutdown = false;
            
            while ( m_ishutdown == false)
            {
                // Sleep for a while
                
                try
                {
                    sleep( m_wakeupInterval);
                }
                catch ( InterruptedException ex)
                {
                }
                
                // Check if there are any active sessions to check
                
                if ( m_passthruSessions.size() > 0)
                {
                    // Enumerate the active sessions
                    
                    Enumeration<NTLMPassthruToken> tokenEnum = m_passthruSessions.keys();
                    long timeNow = System.currentTimeMillis();
                    
                    while (tokenEnum.hasMoreElements())
                    {
                        // Get the current NTLM token and check if it has expired
                        
                        NTLMPassthruToken ntlmToken = tokenEnum.nextElement();
                        
                        if ( ntlmToken != null && ntlmToken.getAuthenticationExpireTime() < timeNow)
                        {
                            // Authentication token has expired, close the associated authentication session
                            
                            AuthenticateSession authSess = m_passthruSessions.get(ntlmToken);
                            if ( authSess != null)
                            {
                                try
                                {
                                    // Close the authentication session
                                    
                                    authSess.CloseSession();
                                }
                                catch ( Exception ex)
                                {
                                    // Debug
                                    
                                    if(logger.isDebugEnabled())
                                        logger.debug("Error closing expired authentication session", ex);
                                }
                            }
                            
                            // Remove the expired token from the active list
                            
                            m_passthruSessions.remove(ntlmToken);
                            
                            // Debug
                            
                            if(logger.isDebugEnabled())
                                logger.debug("Removed expired NTLM token " + ntlmToken);
                        }
                    }
                }
            }
            
            // Debug
            
            if(logger.isDebugEnabled())
                logger.debug("Passthru reaper thread shutdown");
        }
        
        /**
         * Shutdown the reaper thread
         */
        public final void shutdownRequest()
        {
            m_ishutdown = true;
            this.interrupt();
        }
    }

    /**
     * Class constructor
     */
    public NTLMAuthenticationComponentImpl() {
        
        // Create the passthru authentication server list
        
        m_passthruServers = new PassthruServers();
        
        // Create the password encryptor for local password hashing
        
        m_encryptor = new PasswordEncryptor();
        
        // Create the active session list and reaper thread
        
        m_passthruSessions = new Hashtable<NTLMPassthruToken,AuthenticateSession>();
        m_reaperThread = new PassthruReaperThread();
    }

    /**
     * Determine if guest logons are allowed
     * 
     * @return boolean
     */
    public final boolean allowsGuest()
    {
        return m_allowGuest;
    }
    
    /**
     * Set the domain to authenticate against
     * 
     * @param domain String
     */
    public void setDomain(String domain) {
        
        // Check if the passthru server list is already configured
        
        if ( m_passthruServers.getTotalServerCount() > 0)
            throw new AlfrescoRuntimeException("Passthru server list already configured");
        
        // Configure the passthru authentication server list using the domain controllers
        
        m_passthruServers.setDomain(domain);
    }
    
    /**
     * Set the server(s) to authenticate against
     * 
     * @param servers String
     */
    public void setServers(String servers) {
        
        // Check if the passthru server list is already configured
        
        if ( m_passthruServers.getTotalServerCount() > 0)
            throw new AlfrescoRuntimeException("Passthru server list already configured");
        
        // Configure the passthru authenticaiton list using a list of server names/addresses
        
        m_passthruServers.setServerList(servers);
    }
    
    /**
     * Use the local server as the authentication server
     * 
     * @param useLocal String
     */
    public void setUseLocalServer(String useLocal)
    {
        // Check if the local server should be used for authentication
        
        if ( Boolean.parseBoolean(useLocal) == true)
        {
            // Check if the passthru server list is already configured
            
            if ( m_passthruServers.getTotalServerCount() > 0)
                throw new AlfrescoRuntimeException("Passthru server list already configured");

            try
            {
                // Get the list of local network addresses
                
                InetAddress[] localAddrs = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
                
                // Build the list of local addresses
                
                if ( localAddrs != null && localAddrs.length > 0)
                {
                    StringBuilder addrStr = new StringBuilder();
                
                    for ( InetAddress curAddr  : localAddrs)
                    {
                        if ( curAddr.isLoopbackAddress() == false)
                        {
                            addrStr.append(curAddr.getHostAddress());
                            addrStr.append(",");
                        }
                    }
                    
                    if ( addrStr.length() > 0)
                        addrStr.setLength(addrStr.length() - 1);
                    
                    // Set the server list using the local address list
                    
                    m_passthruServers.setServerList(addrStr.toString());
                }
                else
                    throw new AlfrescoRuntimeException("No local server address(es)");
            }
            catch ( UnknownHostException ex)
            {
                throw new AlfrescoRuntimeException("Failed to get local address list");
            }
        }
    }
    
    /**
     * Allow guest access
     * 
     * @param guest String
     */
    public void setGuestAccess(String guest)
    {
        m_allowGuest = Boolean.parseBoolean(guest);
    }
    
    /**
     * Set the JCE provider
     * 
     * @param providerClass String
     */
    public void setJCEProvider(String providerClass)
    {
        // Set the JCE provider, required to provide various encryption/hashing algorithms not available
        // in the standard Sun JDK/JRE
        
        try
        {

            // Load the JCE provider class and validate

            Object jceObj = Class.forName(providerClass).newInstance();
            if (jceObj instanceof java.security.Provider)
            {

                // Inform listeners, validate the configuration change

                Provider jceProvider = (Provider) jceObj;

                // Add the JCE provider

                Security.addProvider(jceProvider);
                
                // Debug
                
                if ( logger.isDebugEnabled())
                    logger.debug("Using JCE provider " + providerClass);
            }
            else
            {
                throw new AlfrescoRuntimeException("JCE provider class is not a valid Provider class");
            }
        }
        catch (ClassNotFoundException ex)
        {
            throw new AlfrescoRuntimeException("JCE provider class " + providerClass + " not found");
        }
        catch (Exception ex)
        {
            throw new AlfrescoRuntimeException("JCE provider class error", ex);
        }
    }
    
    /**
     * Set the authentication session timeout, in seconds
     * 
     * @param sessTmo String
     */
    public void setSessionTimeout(String sessTmo)
    {
        // Convert to an integer value and range check the timeout value
        
        try
        {
            // Convert to an integer value
            
            long sessTmoMilli = Long.parseLong(sessTmo) * 1000L;
            
            if ( sessTmoMilli < MinimumSessionTimeout)
                throw new AlfrescoRuntimeException("Authentication session timeout too low, " + sessTmo);
            
            // Set the authentication session timeout value
            
            m_passthruSessTmo = sessTmoMilli;
            
            // Set the reaper thread wakeup interval
            
            m_reaperThread.setWakeup( sessTmoMilli / 2);
        }
        catch(NumberFormatException ex)
        {
            throw new AlfrescoRuntimeException("Invalid authenication session timeout value");
        }
    }
    
    /**
     * Return the authentication session timeout, in milliseconds
     * 
     * @return long
     */
    private final long getSessionTimeout()
    {
        return m_passthruSessTmo;
    }
    
    /**
     * Authenticate
     * 
     * @param userName String
     * @param password char[]
     * @throws AuthenticationException
     */     
    public void authenticate(String userName, char[] password) throws AuthenticationException
    {
        // Debug
        
        if ( logger.isDebugEnabled())
            logger.debug("Authenticate user=" + userName + " via local credentials");
        
        // Create a local authentication token
        
        NTLMLocalToken authToken = new NTLMLocalToken(userName, new String(password));
        
        // Authenticate using the token
        
        authenticate( authToken);
        setCurrentUser( userName);
    }

    /**
     * Authenticate using a token
     * 
     * @param token Authentication
     * @return Authentication
     * @throws AuthenticationException
     */
    public Authentication authenticate(Authentication auth) throws AuthenticationException
    {
        // DEBUG
        
        if ( logger.isDebugEnabled())
            logger.debug("Authenticate " + auth + " via token");
        
        // Check if the token is for passthru authentication
        
        if( auth instanceof NTLMPassthruToken)
        {
            // Access the NTLM passthru token
            
            NTLMPassthruToken ntlmToken = (NTLMPassthruToken) auth;
            
            // Authenticate using passthru
            
            authenticatePassthru(ntlmToken);
        }

        // Check for a local authentication token
        
        else if( auth instanceof NTLMLocalToken)
        {
            AuthenticateSession authSess = null;
            
            try
            {

                // Access the NTLM token
                
                NTLMLocalToken ntlmToken = (NTLMLocalToken) auth;
    
                // Open a session to an authentication server
                
                authSess = m_passthruServers.openSession();
                
                // Authenticate using the credentials supplied
                    
                authenticateLocal(ntlmToken, authSess);
            }
            finally
            {
                // Make sure the authentication session is closed
                
                if ( authSess != null)
                {
                    try
                    {
                        authSess.CloseSession();
                    }
                    catch ( Exception ex)
                    {
                    }
                }
            }
        }
        else
        {
            // Unsupported authentication token
            
            throw new AuthenticationException("Unsupported authentication token type");
        }

        // Return the updated authentication token
        
        return auth;
    }
    
    /**
     * Explicitly set the current user to be authenticated.
     * 
     * @param userName String
     * @return Authentication
     */
    public Authentication setCurrentUser(String userName)
    {
        try
        {
            UserDetails ud = null;
            if (userName.equals(SYSTEM_USER_NAME))
            {
                GrantedAuthority[] gas = new GrantedAuthority[1];
                gas[0] = new GrantedAuthorityImpl("ROLE_SYSTEM");
                ud = new User(SYSTEM_USER_NAME, "", true, true, true, true, gas);
            }
            else
            {
                GrantedAuthority[] gas = new GrantedAuthority[1];
                gas[0] = new GrantedAuthorityImpl("ROLE_AUTHENTICATED");
                
                ud = new User(userName, "", true, true, true, true, gas);
            }
            
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(ud, "", ud
                    .getAuthorities());
            auth.setDetails(ud);
            auth.setAuthenticated(true);
            return setCurrentAuthentication(auth);
        }
        catch (net.sf.acegisecurity.AuthenticationException ae)
        {
            throw new AuthenticationException(ae.getMessage(), ae);
        }
    }

    /**
     * Remove the current security information
     */
    public void clearCurrentSecurityContext()
    {
        ContextHolder.setContext(null);
    }

    /**
     * Explicitly set the current suthentication.
     * 
     * @param authentication Authentication
     */
    public Authentication setCurrentAuthentication(Authentication authentication)
    {
        Context context = ContextHolder.getContext();
        SecureContext sc = null;
        if ((context == null) || !(context instanceof SecureContext))
        {
            sc = new SecureContextImpl();
            ContextHolder.setContext(sc);
        }
        else
        {
            sc = (SecureContext) context;
        }
        authentication.setAuthenticated(true);
        sc.setAuthentication(authentication);
        return authentication;
    }

    /**
     * Get the current authentication context
     *  
     * @return Authentication
     * @throws AuthenticationException
     */
    public Authentication getCurrentAuthentication() throws AuthenticationException
    {
        Context context = ContextHolder.getContext();
        if ((context == null) || !(context instanceof SecureContext))
        {
            return null;
        }
        return ((SecureContext) context).getAuthentication();
    }

    /**
     * Set the system user as the current user.
     * 
     * @return Authentication
     */
    public Authentication setSystemUserAsCurrentUser()
    {
        return setCurrentUser(SYSTEM_USER_NAME);
    }

    /**
     * Get the name of the system user
     * 
     * @return String
     */
    public String getSystemUserName()
    {
        return SYSTEM_USER_NAME;
    }

    /**
     * Get the current user name.
     * 
     * @return String
     * @throws AuthenticationException
     */
    public String getCurrentUserName() throws AuthenticationException
    {
        Context context = ContextHolder.getContext();
        if ((context == null) || !(context instanceof SecureContext))
        {
            return null;
        }
        return getUserName(((SecureContext) context).getAuthentication());
    }

    /**
     * Get the enum that describes NTLM integration
     * 
     * @return NTLMMode
     */
    public NTLMMode getNTLMMode()
    {
        return NTLMMode.PASS_THROUGH;
    }

    /**
     * Get the MD4 password hash, as required by NTLM based authentication methods.
     * 
     * @param userName String
     * @return String
     */
    public String getMD4HashedPassword(String userName)
    {
        // Do not support MD4 hashed password
        
        throw new AlfrescoRuntimeException("MD4 passwords not supported");
    }
    
    /**
     * Authenticate a user using local credentials
     * 
     * @param ntlmToken NTLMLocalToken
     * @param authSess AuthenticateSession
     */
    private void authenticateLocal(NTLMLocalToken ntlmToken, AuthenticateSession authSess)
    {
        try
        {
            // Get the plaintext password and generate an NTLM1 password hash
            
            String username = (String) ntlmToken.getPrincipal();
            String plainPwd = (String) ntlmToken.getCredentials();
            byte[] ntlm1Pwd = m_encryptor.generateEncryptedPassword( plainPwd, authSess.getEncryptionKey(), PasswordEncryptor.NTLM1);
            
            // Send the logon request to the authentication server
            //
            // Note: Only use the stronger NTLM hash, we do not send the LM hash
            
            authSess.doSessionSetup(username, null, ntlm1Pwd);
            
            // Check if the session has logged on as a guest
            
            if ( authSess.isGuest() || username.equalsIgnoreCase("GUEST"))
            {
                // If guest access is enabled add a guest authority to the token
                
                if ( allowsGuest())
                {
                    // Set the guest authority
                    
                    GrantedAuthority[] authorities = new GrantedAuthority[2];
                    authorities[0] = new GrantedAuthorityImpl(NTLMAuthorityGuest);
                    authorities[1] = new GrantedAuthorityImpl("ROLE_AUTHENTICATED");
                    
                    ntlmToken.setAuthorities(authorities);
                }
                else
                {
                    // Guest access not allowed
                    
                    throw new AuthenticationException("Guest logons disabled");
                }
            }
            else
            {
                // Set authorities
                
                GrantedAuthority[] authorities = new GrantedAuthority[1];
                authorities[0] = new GrantedAuthorityImpl("ROLE_AUTHENTICATED");
                
                ntlmToken.setAuthorities(authorities);
            }
            
            // Indicate that the token is authenticated
            
            ntlmToken.setAuthenticated(true);
            setCurrentUser( username);
            
            // Debug
            
            if ( logger.isDebugEnabled())
                logger.debug("Authenticated token=" + ntlmToken);
        }
        catch (NoSuchAlgorithmException ex)
        {
            // JCE provider does not have the required encryption/hashing algorithms
            
            throw new AuthenticationServiceException("JCE provider error", ex);
        }
        catch (IOException ex)
        {
            // Error connecting to the authentication server
            
            throw new AuthenticationServiceException("I/O error", ex);
        }
        catch (SMBException ex)
        {
            // Check the returned status code to determine why the logon failed and throw an appropriate exception
            
            if ( ex.getErrorClass() == SMBStatus.NTErr)
            {
                AuthenticationException authEx = null;
                
                switch( ex.getErrorCode())
                {
                case SMBStatus.NTLogonFailure:
                    authEx = new AuthenticationException("Logon failure");
                    break;
                case SMBStatus.NTAccountDisabled:
                    authEx = new AuthenticationException("Account disabled");
                    break;
                default:
                    authEx = new AuthenticationException("Logon failure");
                break;
                }
                
                throw authEx;
            }
            else
                throw new AuthenticationException("Logon failure");
        }
    }
    
    /**
     * Authenticate using passthru authentication with a client
     * 
     * @param ntlmToken NTLMPassthruToken
     */
    private void authenticatePassthru(NTLMPassthruToken ntlmToken)
    {
        // Check if the token has an authentication session, if not then it is either a new token
        // or the session has been timed out
        
        AuthenticateSession authSess = m_passthruSessions.get(ntlmToken);
        
        if ( authSess == null)
        {
            // Check if the token has a challenge, if it does then the associated session has been
            // timed out
            
            if ( ntlmToken.getChallenge() != null)
                throw new CredentialsExpiredException("Authentication session expired");
            
            // Open an authentication session for the new token and add to the active session list
            
            authSess = m_passthruServers.openSession();
            
            ntlmToken.setAuthenticationExpireTime(System.currentTimeMillis() + getSessionTimeout());
            
            // Get the challenge from the initial session negotiate stage
            
            ntlmToken.setChallenge(new NTLMChallenge(authSess.getEncryptionKey()));

            StringBuilder details = new StringBuilder();

            // Build a details string with the authentication session details
            
            details.append(authSess.getDomain());
            details.append("\\");
            details.append(authSess.getPCShare().getNodeName());
            details.append(",");
            details.append(authSess.getSession().getProtocolName());
            
            ntlmToken.setDetails(details.toString());

            // Put the token/session into the active session list
            
            m_passthruSessions.put(ntlmToken, authSess);
            
            // Debug
            
            if ( logger.isDebugEnabled())
                logger.debug("Passthru stage 1 token " + ntlmToken);
        }
        else
        {
            try
            {
                // Stage two of the authentication, send the hashed password to the authentication server
            
                byte[] lmPwd = null;
                byte[] ntlmPwd = null;
                
                if ( ntlmToken.getPasswordType() == PasswordEncryptor.LANMAN)
                    lmPwd = ntlmToken.getHashedPassword();
                else if ( ntlmToken.getPasswordType() == PasswordEncryptor.NTLM1)
                    ntlmPwd = ntlmToken.getHashedPassword();
                
                String username = (String) ntlmToken.getPrincipal();
                
                authSess.doSessionSetup(username, lmPwd, ntlmPwd);
                
                // Check if the session has logged on as a guest
                
                if ( authSess.isGuest() || username.equalsIgnoreCase("GUEST"))
                {
                    // If guest access is enabled add a guest authority to the token
                    
                    if ( allowsGuest())
                    {
                        // Set the guest authority
                        
                        GrantedAuthority[] authorities = new GrantedAuthority[1];
                        authorities[0] = new GrantedAuthorityImpl(NTLMAuthorityGuest);
                        
                        ntlmToken.setAuthorities(authorities);
                    }
                    else
                    {
                        // Guest access not allowed
                        
                        throw new BadCredentialsException("Guest logons disabled");
                    }
                }
                
                // Indicate that the token is authenticated
                
                ntlmToken.setAuthenticated(true);
                setCurrentUser( username);
                
                // Debug
                
                if ( logger.isDebugEnabled())
                    logger.debug("Authenticated token=" + ntlmToken);
            }                
            catch (IOException ex)
            {
                // Error connecting to the authentication server
                
                throw new AuthenticationServiceException("I/O error", ex);
            }
            catch (SMBException ex)
            {
                // Check the returned status code to determine why the logon failed and throw an appropriate exception
                
                if ( ex.getErrorClass() == SMBStatus.NTErr)
                {
                    AuthenticationException authEx = null;
                    
                    switch( ex.getErrorCode())
                    {
                    case SMBStatus.NTLogonFailure:
                        authEx = new AuthenticationException("Logon failure");
                        break;
                    case SMBStatus.NTAccountDisabled:
                        authEx = new AuthenticationException("Account disabled");
                        break;
                    default:
                        authEx = new AuthenticationException("Logon failure");
                    break;
                    }
                    
                    throw authEx;
                }
                else
                    throw new BadCredentialsException("Logon failure");
            }
            finally
            {
                // Make sure the authentication session is closed
                
                if ( authSess != null)
                {
                    try
                    {
                        // Remove the session from the active list
                        
                        m_passthruSessions.remove(ntlmToken);
                        
                        // Close the session to the authentication server
                        
                        authSess.CloseSession();
                    }
                    catch (Exception ex)
                    {
                    }
                }
            }
        }
    }

    /**
     * Get the current user name
     * 
     * @param authentication Authentication
     * @return String
     */
    private String getUserName(Authentication authentication)
    {
        String username = authentication.getPrincipal().toString();

        if (authentication.getPrincipal() instanceof UserDetails)
        {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        }

        return username;
    }
    
}
