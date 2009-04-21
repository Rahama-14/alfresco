/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.filesys.auth.cifs;

import javax.transaction.UserTransaction;

import net.sf.acegisecurity.Authentication;

import org.alfresco.config.ConfigElement;
import org.alfresco.filesys.AlfrescoConfigSection;
import org.alfresco.filesys.alfresco.AlfrescoClientInfo;
import org.alfresco.filesys.repo.ContentContext;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.auth.CifsAuthenticator;
import org.alfresco.jlan.server.auth.ClientInfo;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.jlan.server.core.SharedDevice;
import org.alfresco.jlan.server.filesys.DiskDeviceContext;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.DiskSharedDevice;
import org.alfresco.jlan.server.filesys.SrvDiskInfo;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.management.subsystems.ActivateableBean;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.MD4PasswordEncoder;
import org.alfresco.repo.security.authentication.MD4PasswordEncoderImpl;
import org.alfresco.repo.security.authentication.ntlm.NLTMAuthenticator;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * CIFS Authenticator Base Class
 * <p>
 * Base class for Alfresco CIFS authenticator implementations.
 * 
 * @author gkspencer
 */
public abstract class CifsAuthenticatorBase extends CifsAuthenticator implements ActivateableBean, InitializingBean, DisposableBean
{
    // Logging
    
    /** The Constant logger. */
    protected static final Log logger = LogFactory.getLog("org.alfresco.smb.protocol.auth");

    // MD4 hash decoder
    
    /** The m_md4 encoder. */
    protected MD4PasswordEncoder m_md4Encoder = new MD4PasswordEncoderImpl();

    /** The authentication component. */
    private AuthenticationComponent authenticationComponent;

    /** The authentication service. */
    private AuthenticationService authenticationService;

    /** The node service. */
    private NodeService nodeService;

    /** The person service. */
    private PersonService personService;

    /** The transaction service. */
    private TransactionService transactionService;

    /** The authority service. */
    private AuthorityService authorityService;

    /** The disk interface. */
    private DiskInterface diskInterface;
    
    /** Is this component active, i.e. should it be used? */
    private boolean active = true;
    
    // Alfresco configuration
    
    /**
     * Instantiates a new cifs authenticator base.
     */
    public CifsAuthenticatorBase()
    {
        // The default access mode
        setAccessMode(USER_MODE);
    }
    
    /**
     * Sets the authentication component.
     * 
     * @param authenticationComponent
     *            the authenticationComponent to set
     */
    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }

    /**
     * Sets the authentication service.
     * 
     * @param authenticationService
     *            the authenticationService to set
     */
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }

    /**
     * Sets the node service.
     * 
     * @param nodeService
     *            the nodeService to set
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * Sets the person service.
     * 
     * @param personService
     *            the personService to set
     */
    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    /**
     * Sets the transaction service.
     * 
     * @param transactionService
     *            the transactionService to set
     */
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * Sets the authority service.
     * 
     * @param authorityService
     *            the authorityService to set
     */
    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }

    /**
     * Set the filesystem driver for the node service based filesystem.
     * 
     * @param diskInterface
     *            DiskInterface
     */
    public void setDiskInterface(DiskInterface diskInterface)
    {
        this.diskInterface = diskInterface;
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.ActivateableBean#isActive()
     */
    public boolean isActive()
    {
        return active;
    }

    /**
     * Activates or deactivates the bean.
     * 
     * @param active
     *            <code>true</code> if the bean is active and initialization should complete
     */
    public void setActive(boolean active)
    {
        this.active = active;
    }

    /**
     * Initialize the authenticator.
     * 
     * @param config
     *            ServerConfiguration
     * @param params
     *            ConfigElement
     * @throws InvalidConfigurationException
     *             the invalid configuration exception
     * @exception InvalidConfigurationException
     */
    public void initialize(ServerConfiguration config, ConfigElement params) throws InvalidConfigurationException
    {
        // Get the alfresco configuration section, required to get hold of various services/components
        
        AlfrescoConfigSection alfrescoConfig = (AlfrescoConfigSection) config.getConfigSection( AlfrescoConfigSection.SectionName);
        
        // Copy over relevant bean properties for backward compatibility
        setAuthenticationComponent(alfrescoConfig.getAuthenticationComponent());
        setAuthenticationService(alfrescoConfig.getAuthenticationService());
        setNodeService(alfrescoConfig.getNodeService());
        setPersonService(alfrescoConfig.getPersonService());
        setTransactionService(alfrescoConfig.getTransactionService());
        setAuthorityService(alfrescoConfig.getAuthorityService());
        setDiskInterface(alfrescoConfig.getRepoDiskInterface());

        super.initialize(config, params);
    }

    
    /**
     * Initialize the authenticator.
     * 
     * @throws InvalidConfigurationException
     *             the invalid configuration exception
     * @exception InvalidConfigurationException
     */
    @Override
    public void initialize() throws InvalidConfigurationException
    {
        super.initialize();

        // Check that the required authentication classes are available

        if ( getAuthenticationComponent() == null)
            throw new InvalidConfigurationException("Authentication component not available");

        // Propagate the allow guest flag
        setAllowGuest(allowGuest() || getAuthenticationComponent().guestUserAuthenticationAllowed());
        
        // Set the guest user name
        
        setGuestUserName( getAuthenticationComponent().getGuestUserName());
        
        // Check that the authentication component is the required type for this authenticator
        
        if ( ! validateAuthenticationMode() )
            throw new InvalidConfigurationException("Required authentication mode not available");
    }
    
    

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public final void afterPropertiesSet() throws InvalidConfigurationException
    {
        // If the bean is active, call the overridable initialize method
        if (this.active)
        {
            initialize();
        }
    }

    /**
     * Validate that the authentication component supports the required mode.
     * 
     * @return boolean
     */
    protected boolean validateAuthenticationMode()
    {
        return true;
    }
    
    /**
     * Logon using the guest user account.
     * 
     * @param client
     *            ClientInfo
     * @param sess
     *            SrvSession
     */
    protected void doGuestLogon( ClientInfo client, SrvSession sess)
    {
        //  Check that the client is an Alfresco client
      
        if ( client instanceof AlfrescoClientInfo == false)
            return;
        
        AlfrescoClientInfo alfClient = (AlfrescoClientInfo) client;
        
        //  Get a guest authentication token
        
        getAuthenticationService().authenticateAsGuest();
        Authentication authToken = getAuthenticationComponent().getCurrentAuthentication();
        
        alfClient.setAuthenticationToken( authToken);
        
        // Set the home folder for the guest user
        
        client.setUserName( getGuestUserName());
        getHomeFolderForUser( client);
        
        // Mark the client as being a guest logon
        
        client.setGuest( true);

        // Create a dynamic share for the guest user, create the disk driver and context
        
        DiskDeviceContext diskCtx = new ContentContext(client.getUserName(), "", "", alfClient.getHomeFolder());

        //  Default the filesystem to look like an 80Gb sized disk with 90% free space

        diskCtx.setDiskInformation(new SrvDiskInfo(2560, 64, 512, 2304));
        
        //  Create a temporary shared device for the users home directory
        
        sess.addDynamicShare( new DiskSharedDevice( client.getUserName(), this.diskInterface, diskCtx, SharedDevice.Temporary));
    }
    
    /**
     * Get the home folder for the user.
     * 
     * @param client
     *            ClientInfo
     */
    protected final void getHomeFolderForUser(ClientInfo client)
    {
        // Check if the client is an Alfresco client, and not a null logon
      
        if ( client instanceof AlfrescoClientInfo == false ||
        		client.isNullSession() == true)
          return;
        
        AlfrescoClientInfo alfClient = (AlfrescoClientInfo) client;
        
        // Get the home folder for the user
        
        UserTransaction tx = getTransactionService().getUserTransaction();
        NodeRef homeSpaceRef = null;
        
        try
        {
            tx.begin();
            homeSpaceRef = (NodeRef) getNodeService().getProperty(getPersonService().getPerson(client.getUserName()), ContentModel.PROP_HOMEFOLDER);
            alfClient.setHomeFolder( homeSpaceRef);
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
            
             //          Re-throw the exception
            if (ex instanceof RuntimeException)
            {
                throw (RuntimeException) ex;
            }
            else
            {
                throw new RuntimeException("Error during execution of transaction.", ex);
            }
        }
    }
    
    /**
     * Map the case insensitive logon name to the internal person object user name.
     * 
     * @param userName
     *            String
     * @return String
     */
    protected final String mapUserNameToPerson(String userName)
    {
        // Get the home folder for the user
        
        UserTransaction tx = getTransactionService().getUserTransaction();
        String personName = null;
        
        try
        {
            tx.begin();
            personName = getPersonService().getUserIdentifier( userName);
            tx.commit();

            // Check if the person exists
            
            if (personName == null) {
                
                tx = getTransactionService().getNonPropagatingUserTransaction( false);
                tx.begin();
                
                getPersonService().getPerson( userName);
                personName = getPersonService().getUserIdentifier( userName);
                
                tx.commit();
            }
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
    
    /**
     * Set the current authenticated user context for this thread.
     * 
     * @param client
     *            ClientInfo
     */
    public void setCurrentUser(ClientInfo client) {

        // Check the account type and setup the authentication context

        if (client.isNullSession())
        {
            // Clear the authentication, null user should not be allowed to do any service calls

            getAuthenticationComponent().clearCurrentSecurityContext();
        }
        else if (client.isGuest() == false && client instanceof AlfrescoClientInfo)
        {
            // Set the authentication context for the request

            AlfrescoClientInfo alfClient = (AlfrescoClientInfo) client;
            getAuthenticationComponent().setCurrentAuthentication(alfClient.getAuthenticationToken());
        }
        else
        {
            // Enable guest access for the request

            getAuthenticationComponent().setGuestUserAsCurrentUser();
        }
    }
    
    /**
     * Return the authentication component.
     * 
     * @return AuthenticationComponent
     */
    protected final AuthenticationComponent getAuthenticationComponent()
    {
        return this.authenticationComponent;
    }
    

    /**
     * Returns an SSO-enabled authentication component.
     * 
     * @return NLTMAuthenticator
     */
    protected final NLTMAuthenticator getNTLMAuthenticator()
    {
        if (!(this.authenticationComponent instanceof NLTMAuthenticator))
        {
            throw new IllegalStateException("Attempt to use non SSO-enabled authentication component for SSO");
        }
        return (NLTMAuthenticator)this.authenticationComponent;
    }

    /**
     * Return the authentication service.
     * 
     * @return AuthenticationService
     */
    protected final AuthenticationService getAuthenticationService()
    {
        return this.authenticationService;
    }
    
    /**
     * Return the node service.
     * 
     * @return NodeService
     */
    protected final NodeService getNodeService()
    {
        return this.nodeService;
    }
    
    /**
     * Return the person service.
     * 
     * @return PersonService
     */
    protected final PersonService getPersonService()
    {
        return this.personService;
    }

    /**
     * Return the transaction service.
     * 
     * @return TransactionService
     */
    protected final TransactionService getTransactionService()
    {
        return this.transactionService;
    }
    
    /**
     * Return the authority service.
     * 
     * @return AuthorityService
     */
    protected final AuthorityService getAuthorityService() {
        return this.authorityService;
    }

    /**
     * Check if the user is an administrator user name.
     * 
     * @param cInfo
     *            ClientInfo
     */
    protected final void checkForAdminUserName(ClientInfo cInfo) {
        
        // Check if the user name is an administrator

        UserTransaction tx = createTransaction();

        try {
            tx.begin();

            if ( cInfo.getLogonType() == ClientInfo.LogonNormal && getAuthorityService().isAdminAuthority(cInfo.getUserName())) {
                
                // Indicate that this is an administrator logon

                cInfo.setLogonType(ClientInfo.LogonAdmin);
            }
            tx.commit();
        }
        catch (Throwable ex) {
            try {
                tx.rollback();
            }
            catch (Throwable ex2) {
                logger.error("Failed to rollback transaction", ex2);
            }

            // Re-throw the exception

            if ( ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            else {
                throw new RuntimeException("Error during execution of transaction.", ex);
            }
        }
    }
    
    /**
     * Create a transaction, this will be a wrteable transaction unless the system is in read-only mode. return
     * UserTransaction
     * 
     * @return the user transaction
     */
    protected final UserTransaction createTransaction()
    {
        // Get the transaction service
        
        TransactionService txService = getTransactionService();
        
        // DEBUG
        
        if ( logger.isDebugEnabled())
            logger.debug("Using " + (txService.isReadOnly() ? "ReadOnly" : "Write") + " transaction");
        
        // Create the transaction
        
        return txService.getUserTransaction( txService.isReadOnly() ? true : false);
    }

    /**
     * Handle tidy up on container shutdown.
     * 
     * @throws Exception
     *             the exception
     */
    public void destroy() throws Exception
    {
        closeAuthenticator();
    }
}