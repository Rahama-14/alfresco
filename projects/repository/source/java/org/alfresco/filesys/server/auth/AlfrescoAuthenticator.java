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
package org.alfresco.filesys.server.auth;

import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.alfresco.config.ConfigElement;
import org.alfresco.filesys.server.SrvSession;
import org.alfresco.filesys.server.config.InvalidConfigurationException;
import org.alfresco.filesys.server.config.ServerConfiguration;
import org.alfresco.filesys.server.core.ShareType;
import org.alfresco.filesys.server.core.SharedDevice;
import org.alfresco.filesys.smb.server.SMBSrvSession;
import org.alfresco.filesys.util.DataPacker;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.MD4PasswordEncoder;
import org.alfresco.repo.security.authentication.MD4PasswordEncoderImpl;
import org.alfresco.repo.security.authentication.NTLMMode;

/**
 * Alfresco Authenticator Class
 * 
 * <p>The Alfresco authenticator implementation enables user level security mode using the Alfresco authentication
 * manager.
 * 
 * <p>Note: Switching off encrypted password support will cause later NT4 service pack releases and
 * Win2000 to refuse to connect to the server without a registry update on the client.
 * 
 * @author GKSpencer
 */
public class AlfrescoAuthenticator extends SrvAuthenticator
{

    // Random number generator used to generate challenge keys

    private Random m_random = new Random(System.currentTimeMillis());

    // Server configuration

    private ServerConfiguration m_config;
    
    // Authentication component, used to access internal authentication functions
    
    private AuthenticationComponent m_authComponent;

    // MD4 hash decoder
    
    private MD4PasswordEncoder m_md4Encoder = new MD4PasswordEncoderImpl();
    
    /**
     * Default Constructor
     * 
     * <p>Default to user mode security with encrypted password support.
     */
    public AlfrescoAuthenticator()
    {
        setAccessMode(SrvAuthenticator.USER_MODE);
        setEncryptedPasswords(true);
    }

    /**
     * Authenticate the connection to a share
     * 
     * @param client ClienInfo
     * @param share SharedDevice
     * @param pwd Share level password.
     * @param sess Server session
     * @return Authentication status.
     */
    public int authenticateShareConnect(ClientInfo client, SharedDevice share, String pwd, SrvSession sess)
    {
        // Allow write access
        //
        // Main authentication is handled by authenticateUser()
        
        return SrvAuthenticator.Writeable;
    }

    /**
     * Authenticate a user
     * 
     * @param client Client information
     * @param sess Server session
     * @param alg Encryption algorithm
     */
    public int authenticateUser(ClientInfo client, SrvSession sess, int alg)
    {
        // Get the stored MD4 hashed password for the user, or null if the user does not exist
        
        String md4hash = m_authComponent.getMD4HashedPassword(client.getUserName());
        
        if ( md4hash != null)
        {
            // Check if the client has supplied an NTLM hashed password, if not then do not allow access
            
            if ( client.getPassword() == null)
                return SrvAuthenticator.AUTH_BADPASSWORD;
            
            try
            {
                // Generate the local encrypted password using the challenge that was sent to the client
                
                byte[] p21 = new byte[21];
                byte[] md4byts = m_md4Encoder.decodeHash(md4hash);
                System.arraycopy(md4byts, 0, p21, 0, 16);
                
                // Generate the local hash of the password using the same challenge
                
                byte[] localHash = getEncryptor().doNTLM1Encryption(p21, sess.getChallengeKey());
                
                // Validate the password
                
                byte[] clientHash = client.getPassword();

                if ( clientHash == null || clientHash.length != localHash.length)
                    return SrvAuthenticator.AUTH_BADPASSWORD;
                
                for ( int i = 0; i < clientHash.length; i++)
                {
                    if ( clientHash[i] != localHash[i])
                        return SrvAuthenticator.AUTH_BADPASSWORD;
                }
                
                // Set the current user to be authenticated, save the authentication token
                
                client.setAuthenticationToken( m_authComponent.setCurrentUser(client.getUserName()));
                
                // Passwords match, grant access
                
                return SrvAuthenticator.AUTH_ALLOW;
            }
            catch (NoSuchAlgorithmException ex)
            {
            }
            
            // Error during password check, do not allow access
            
            return SrvAuthenticator.AUTH_DISALLOW;
        }

        // Check if this is an SMB/CIFS null session logon.
        //
        // The null session will only be allowed to connect to the IPC$ named pipe share.

        if (client.isNullSession() && sess instanceof SMBSrvSession)
            return SrvAuthenticator.AUTH_ALLOW;
        
        // User does not exist, check if guest access is allowed
            
        return allowGuest() ? SrvAuthenticator.AUTH_GUEST : SrvAuthenticator.AUTH_DISALLOW;
    }

    /**
     * Generate a challenge key
     * 
     * @param sess SrvSession
     * @return byte[]
     */
    public byte[] getChallengeKey(SrvSession sess)
    {

        // Generate a new challenge key, pack the key and return

        byte[] key = new byte[8];

        DataPacker.putIntelLong(m_random.nextLong(), key, 0);
        return key;
    }

    /**
     * Search for the required user account details in the defined user list
     * 
     * @param user String
     * @return UserAccount
     */
    public UserAccount getUserDetails(String user)
    {
        return null;
    }

    /**
     * Initialize the authenticator
     * 
     * @param config ServerConfiguration
     * @param params ConfigElement
     * @exception InvalidConfigurationException
     */
    public void initialize(ServerConfiguration config, ConfigElement params) throws InvalidConfigurationException
    {
        // Save the server configuration so we can access the authentication component

        m_config = config;
        
        // Check that the required authentication classes are available

        m_authComponent = m_config.getAuthenticationComponent();
        
        if ( m_authComponent == null)
            throw new InvalidConfigurationException("Authentication component not available");
        
        if ( m_authComponent.getNTLMMode() != NTLMMode.MD4_PROVIDER)
            throw new InvalidConfigurationException("Required authentication mode not available");
    }
}