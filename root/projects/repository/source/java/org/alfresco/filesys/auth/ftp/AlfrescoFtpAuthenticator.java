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
package org.alfresco.filesys.auth.ftp;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import net.sf.acegisecurity.Authentication;

import org.alfresco.config.ConfigElement;
import org.alfresco.filesys.AlfrescoConfigSection;
import org.alfresco.filesys.alfresco.AlfrescoClientInfo;
import org.alfresco.jlan.ftp.FTPAuthenticator;
import org.alfresco.jlan.ftp.FTPSrvSession;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.auth.ClientInfo;
import org.alfresco.jlan.server.auth.PasswordEncryptor;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.MD4PasswordEncoder;
import org.alfresco.repo.security.authentication.MD4PasswordEncoderImpl;
import org.alfresco.repo.security.authentication.NTLMMode;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Alfresco FTP Authenticator Class
 * 
 * @author gkspencer
 */
public class AlfrescoFtpAuthenticator extends FTPAuthenticatorBase {

  // MD4 hash decoder
  
  protected MD4PasswordEncoder m_md4Encoder = new MD4PasswordEncoderImpl();
  
  // Password encryptor, for MD4 hashing
  
  protected PasswordEncryptor m_encryptor = new PasswordEncryptor();
  
  /**
   * Authenticate the user
   * 
   * @param client ClientInfo
   * @param sess FTPSrvSession
   * @return boolean
   */
  public boolean authenticateUser( ClientInfo client, FTPSrvSession sess)
  {
    // Check that the client is an Alfresco client
    
    if ( client instanceof AlfrescoClientInfo == false)
        return false;
    
   // Check if this is a guest logon
    
    boolean authSts = false;
    UserTransaction tx = null;
    
    try
    {
        if ( client.isGuest())
        {
            //  Get a guest authentication token
            
            doGuestLogon((AlfrescoClientInfo) client, sess);
            
            // Indicate logged on as guest
            
            authSts = true;
            client.setLogonType( ClientInfo.LogonGuest);
            
            // DEBUG
            
            if ( logger.isDebugEnabled())
                logger.debug("Authenticated user " + client.getUserName() + " sts=" + authSts);
            
            // Return the guest status
            
            return authSts;
        }
        
        // Start a transaction
      
        tx = createTransaction();
        tx.begin();

        // Authenitcate using the authentication component, as we have the plaintext password
        
        getAuthenticationComponent().authenticate( client.getUserName(), client.getPasswordAsString().toCharArray());
        authSts = true;
        
        // Check if the user has been logged on successfully
        
        if ( authSts == true)
        	client.setLogonType( ClientInfo.LogonNormal);
        
        // Check if the logged on user is an administrator
        
        if ( client.getLogonType() == ClientInfo.LogonNormal)
        {
        	// Check for an administrator logon, update the logon type
        	
        	checkForAdminUserName( client);
        }
    }
    catch ( Exception ex)
    {
      if ( logger.isDebugEnabled())
        logger.debug( ex);
    }
    finally
    {
        // Commit the transaction
        
        if ( tx != null)
        {
            try
            {
                // Commit or rollback the transaction
                
                if ( tx.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                {
                    // Transaction is marked for rollback
                    
                    tx.rollback();
                }
                else
                {
                    // Commit the transaction
                    
                    tx.commit();
                }
            }
            catch ( Exception ex)
            {
            }
        }
    }
    
    // DEBUG
    
    if ( logger.isDebugEnabled())
        logger.debug("Authenticated user " + client.getUserName() + " sts=" + authSts +
                " via " + (getAuthenticationComponent().getNTLMMode() == NTLMMode.MD4_PROVIDER ? "MD4" : "Passthru"));
                
    // Return the authentication status
    
    return authSts;
  }

  /**
   * Logon using the guest user account
   * 
   * @param client AlfrescoClientInfo
   * @param sess SrvSession
   */
  protected void doGuestLogon( AlfrescoClientInfo client, SrvSession sess)
  {
      //  Get a guest authentication token
      
      getAuthenticationService().authenticateAsGuest();
      Authentication authToken = getAuthenticationComponent().getCurrentAuthentication();
      
      client.setAuthenticationToken( authToken);
      
      // Mark the client as being a guest logon
      
      client.setGuest( true);
  }
}
