/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Alfresco Network License. You may obtain a
 * copy of the License at
 *
 *   http://www.alfrescosoftware.com/legal/
 *
 * Please view the license relevant to your network subscription.
 *
 * BY CLICKING THE "I UNDERSTAND AND ACCEPT" BOX, OR INSTALLING,  
 * READING OR USING ALFRESCO'S Network SOFTWARE (THE "SOFTWARE"),  
 * YOU ARE AGREEING ON BEHALF OF THE ENTITY LICENSING THE SOFTWARE    
 * ("COMPANY") THAT COMPANY WILL BE BOUND BY AND IS BECOMING A PARTY TO 
 * THIS ALFRESCO NETWORK AGREEMENT ("AGREEMENT") AND THAT YOU HAVE THE   
 * AUTHORITY TO BIND COMPANY. IF COMPANY DOES NOT AGREE TO ALL OF THE   
 * TERMS OF THIS AGREEMENT, DO NOT SELECT THE "I UNDERSTAND AND AGREE"   
 * BOX AND DO NOT INSTALL THE SOFTWARE OR VIEW THE SOURCE CODE. COMPANY   
 * HAS NOT BECOME A LICENSEE OF, AND IS NOT AUTHORIZED TO USE THE    
 * SOFTWARE UNLESS AND UNTIL IT HAS AGREED TO BE BOUND BY THESE LICENSE  
 * TERMS. THE "EFFECTIVE DATE" FOR THIS AGREEMENT SHALL BE THE DAY YOU  
 * CHECK THE "I UNDERSTAND AND ACCEPT" BOX.
 */
package org.alfresco.filesys.server.auth.passthru;

import java.util.Hashtable;

import org.alfresco.config.ConfigElement;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.filesys.server.SessionListener;
import org.alfresco.filesys.server.SrvSession;
import org.alfresco.filesys.server.auth.ClientInfo;
import org.alfresco.filesys.server.auth.SrvAuthenticator;
import org.alfresco.filesys.server.auth.UserAccount;
import org.alfresco.filesys.server.config.InvalidConfigurationException;
import org.alfresco.filesys.server.config.ServerConfiguration;
import org.alfresco.filesys.server.core.SharedDevice;
import org.alfresco.filesys.smb.server.SMBServer;
import org.alfresco.filesys.smb.server.SMBSrvSession;
import org.alfresco.filesys.util.HexDump;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Passthru Authenticator Class
 * <p>
 * Authenticate users accessing the CIFS server by validating the user against a domain controller
 * or other server on the network.
 * 
 * @author GKSpencer
 */
public class PassthruAuthenticator extends SrvAuthenticator implements SessionListener
{
    // Debug logging

    private static final Log logger = LogFactory.getLog("org.alfresco.smb.protocol.auth");

    // Constants

    public final static int DefaultSessionTmo = 5000;   // 5 seconds
    public final static int MinSessionTmo = 2000;       // 2 seconds
    public final static int MaxSessionTmo = 30000;      // 30 seconds

    // Passthru servers used to authenticate users

    private PassthruServers m_passthruServers;

    // SMB server

    private SMBServer m_server;

    // Sessions that are currently in the negotiate/session setup state

    private Hashtable<String, PassthruDetails> m_sessions;

    /**
     * Passthru Authenticator Constructor
     * <p>
     * Default to user mode security with encrypted password support.
     */
    public PassthruAuthenticator()
    {
        setAccessMode(SrvAuthenticator.USER_MODE);
        setEncryptedPasswords(true);

        // Allocate the session table

        m_sessions = new Hashtable<String, PassthruDetails>();
    }

    /**
     * Authenticate the connection to a particular share, called when the SMB server is in share
     * security mode
     * 
     * @param client ClientInfo
     * @param share SharedDevice
     * @param sharePwd String
     * @param sess SrvSession
     * @return int
     */
    public int authenticateShareConnect(ClientInfo client, SharedDevice share, String sharePwd, SrvSession sess)
    {
        return SrvAuthenticator.Writeable;
    }

    /**
     * Authenticate a session setup by a user
     * 
     * @param client ClientInfo
     * @param sess SrvSession
     * @param alg int
     * @return int
     */
    public int authenticateUser(ClientInfo client, SrvSession sess, int alg)
    {

        // Find the active authentication session details for the server session

        int authSts = SrvAuthenticator.AUTH_DISALLOW;
        PassthruDetails passDetails = m_sessions.get(sess.getUniqueId());

        if (passDetails != null)
        {

            try
            {

                // Authenticate the user by passing the hashed password to the authentication server
                // using the session that has already been setup.

                AuthenticateSession authSess = passDetails.getAuthenticateSession();
                authSess.doSessionSetup(client.getUserName(), client.getANSIPassword(), client.getPassword());

                // Check if the user has been logged on as a guest

                if (authSess.isGuest())
                {

                    // Check if the local server allows guest access

                    if (allowGuest() == true)
                    {

                        // Allow the user access as a guest

                        authSts = SrvAuthenticator.AUTH_GUEST;

                        // Debug

                        if (logger.isDebugEnabled())
                            logger.debug("Passthru authenticate user=" + client.getUserName() + ", GUEST");
                    }
                }
                else
                {

                    // Allow the user full access to the server

                    authSts = SrvAuthenticator.AUTH_ALLOW;

                    // Debug

                    if (logger.isDebugEnabled())
                        logger.debug("Passthru authenticate user=" + client.getUserName() + ", FULL");
                }
            }
            catch (Exception ex)
            {

                // Debug

                logger.error("Passthru authenticator", ex);
            }

            // Keep the authentication session if the user session is an SMB session, else close the
            // session now

            if ((sess instanceof SMBSrvSession) == false)
            {

                // Remove the passthru session from the active list

                m_sessions.remove(sess.getUniqueId());

                // Close the passthru authentication session

                try
                {

                    // Close the authentication session

                    AuthenticateSession authSess = passDetails.getAuthenticateSession();
                    authSess.CloseSession();

                    // DEBUG

                    if (logger.isDebugEnabled())
                        logger.debug("Closed auth session, sessId=" + authSess.getSessionId());
                }
                catch (Exception ex)
                {

                    // Debug

                    logger.error("Passthru error closing session (auth user)", ex);
                }
            }
        }
        else
        {

            // DEBUG

            if (logger.isDebugEnabled())
                logger.debug("  No PassthruDetails for " + sess.getUniqueId());
        }

        // Return the authentication status

        return authSts;
    }

    /**
     * Get user account details for the specified user
     * 
     * @param user String
     * @return UserAccount
     */
    public UserAccount getUserDetails(String user)
    {

        // No user details to return

        return null;
    }

    /**
     * Get a challenge key for a new session
     * 
     * @param sess SrvSession
     * @return byte[]
     */
    public byte[] getChallengeKey(SrvSession sess)
    {

        // Check for an SMB session

        byte[] chKey = null;

        if (sess instanceof SMBSrvSession)
        {

            // Check if the SMB server listener has been initialized

            if (m_server == null)
            {

                // Initialize the SMB server session listener so we receive callbacks when sessions
                // are opened/closed on the SMB server

                SMBSrvSession smbSess = (SMBSrvSession) sess;
                m_server = smbSess.getSMBServer();

                m_server.addSessionListener(this);
            }
        }

        try
        {

            // Open a connection to the authentication server

            AuthenticateSession authSess = m_passthruServers.openSession();
            if (authSess != null)
            {

                // Create an entry in the active sessions table for the new session

                PassthruDetails passDetails = new PassthruDetails(sess, authSess);
                m_sessions.put(sess.getUniqueId(), passDetails);

                // Use the challenge key returned from the authentication server

                chKey = authSess.getEncryptionKey();

                // DEBUG

                if (logger.isDebugEnabled())
                    logger.debug("Passthru sessId=" + authSess.getSessionId() + ", negotiate key=["
                            + HexDump.hexString(chKey) + "]");
            }
        }
        catch (Exception ex)
        {

            // Debug

            logger.error("Passthru error getting challenge", ex);
        }

        // Return the challenge key

        return chKey;
    }

    /**
     * Initialzie the authenticator
     * 
     * @param config ServerConfiguration
     * @param params ConfigElement
     * @exception InvalidConfigurationException
     */
    public void initialize(ServerConfiguration config, ConfigElement params) throws InvalidConfigurationException
    {

        // Call the base class

        super.initialize(config, params);

        // Create the passthru authentication server list
        
        m_passthruServers = new PassthruServers();
        
        // Check if the session timeout has been specified

        ConfigElement sessTmoElem = params.getChild("Timeout");
        if (sessTmoElem != null)
        {

            try
            {

                // Validate the session timeout value

                int sessTmo = Integer.parseInt(sessTmoElem.getValue());
                
                // Range check the timeout
                
                if ( sessTmo < MinSessionTmo || sessTmo > MaxSessionTmo)
                    throw new InvalidConfigurationException("Invalid session timeout, valid range is " +
                                                            MinSessionTmo + " to " + MaxSessionTmo);
                
                // Set the session timeout for connecting to an authentication server
                
                m_passthruServers.setConnectionTimeout( sessTmo);
            }
            catch (NumberFormatException ex)
            {
                throw new InvalidConfigurationException("Invalid timeout value specified");
            }
        }

        // Check if the local server should be used

        String srvList = null;

        if (params.getChild("LocalServer") != null)
        {

            // Get the local server name, trim the domain name

            srvList = config.getLocalServerName(true);
            if(srvList == null)
                throw new AlfrescoRuntimeException("Passthru authenticator failed to get local server name");
        }

        // Check if a server name has been specified

        ConfigElement srvNamesElem = params.getChild("Server");

        if (srvNamesElem != null && srvNamesElem.getValue().length() > 0)
        {

            // Check if the server name was already set

            if (srvList != null)
                throw new AlfrescoRuntimeException("Set passthru server via local server or specify name");

            // Get the passthru authenticator server name

            srvList = srvNamesElem.getValue();
        }

        // If the passthru server name has been set initialize the passthru connection

        if (srvList != null)
        {
            // Initialize using a list of server names/addresses

            m_passthruServers.setServerList(srvList);
        }
        else
        {

            // Get the domain/workgroup name
            
            String domainName = null;
            
            // Check if the local domain/workgroup should be used
            
            if (params.getChild("LocalDomain") != null)
            {
                // Get the local domain/workgroup name
                
                domainName = config.getLocalDomainName();
            }
            
            // Check if a domain name has been specified

            ConfigElement domNameElem = params.getChild("Domain");

            if (domNameElem != null && domNameElem.getValue().length() > 0)
            {

                // Check if the authentication server has already been set, ie. server name was also specified
                
                if (srvList != null)
                    throw new AlfrescoRuntimeException("Specify server or domain name for passthru authentication");

                domainName = domNameElem.getValue();
            }
            
            // If the domain name has been set initialize the passthru connection
            
            if (domainName != null)
            {
                // Initialize using the domain
                
                m_passthruServers.setDomain(domainName);
            }
        }

        // Check if we have an authentication server

        if (m_passthruServers.getTotalServerCount() == 0)
            throw new AlfrescoRuntimeException("No valid authentication servers found for passthru");
    }

    /**
     * Close the authenticator, perform cleanup
     */
    public void closeAuthenticator()
    {
        // Close the passthru authentication server list
        
        if ( m_passthruServers != null)
            m_passthruServers.shutdown();
    }
    
    /**
     * SMB server session closed notification
     * 
     * @param sess SrvSession
     */
    public void sessionClosed(SrvSession sess)
    {

        // Check if there is an active session to the authentication server for this local
        // session

        PassthruDetails passDetails = m_sessions.get(sess.getUniqueId());

        if (passDetails != null)
        {

            // Remove the passthru session from the active list

            m_sessions.remove(sess.getUniqueId());

            // Close the passthru authentication session

            try
            {

                // Close the authentication session

                AuthenticateSession authSess = passDetails.getAuthenticateSession();
                authSess.CloseSession();

                // DEBUG

                if (logger.isDebugEnabled())
                    logger.debug("Closed auth session, sessId=" + authSess.getSessionId());
            }
            catch (Exception ex)
            {

                // Debug

                logger.error("Passthru error closing session (closed)", ex);
            }
        }
    }

    /**
     * SMB server session created notification
     * 
     * @param sess SrvSession
     */
    public void sessionCreated(SrvSession sess)
    {
    }

    /**
     * User successfully logged on notification
     * 
     * @param sess SrvSession
     */
    public void sessionLoggedOn(SrvSession sess)
    {

        // Check if the client information has an empty user name, if so then do not close the
        // authentication
        // session

        if (sess.hasClientInformation() && sess.getClientInformation().getUserName() != null
                && sess.getClientInformation().getUserName().length() > 0)
        {

            // Check if there is an active session to the authentication server for this local
            // session

            PassthruDetails passDetails = m_sessions.get(sess.getUniqueId());

            if (passDetails != null)
            {

                // Remove the passthru session from the active list

                m_sessions.remove(sess.getUniqueId());

                // Close the passthru authentication session

                try
                {

                    // Close the authentication session

                    AuthenticateSession authSess = passDetails.getAuthenticateSession();
                    authSess.CloseSession();

                    // DEBUG

                    if (logger.isDebugEnabled())
                        logger.debug("Closed auth session, sessId=" + authSess.getSessionId());
                }
                catch (Exception ex)
                {

                    // Debug

                    logger.error("Passthru error closing session (logon)", ex);
                }
            }
        }
    }
}
