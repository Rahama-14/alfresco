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
package org.alfresco.repo.security.authentication.ntlm;

import java.util.Date;

import net.sf.acegisecurity.UserDetails;
import net.sf.acegisecurity.providers.dao.UsernameNotFoundException;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.springframework.dao.DataAccessException;

/**
 * Null Mutable Authentication Dao Class
 * 
 * <p>Mutable authentication implementation that does nothing.
 * 
 * @author GKSpencer
 */
public class NullMutableAuthenticationDao implements MutableAuthenticationDao
{
    // Node service
    
    private NodeService m_nodeService;
    
    // User store reference
    
    private StoreRef m_userStoreRef;

    /**
     * Set the node service
     * 
     * @param nodeService NodeService
     */
    public void setNodeService(NodeService nodeService)
    {
        m_nodeService = nodeService;
    }
    
    /**
     * Create a user with the given userName and password
     * 
     * @param userName
     * @param rawPassword
     * @throws AuthenticationException
     */
    public void createUser(String userName, char[] rawPassword) throws AuthenticationException
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
        // Nothing to do
    }
    
    /**
     * Update a user's password.
     * 
     * @param userName
     * @param rawPassword
     * @throws AuthenticationException
     */
    public void updateUser(String userName, char[] rawPassword) throws AuthenticationException
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
        // Nothing to do
    }
    
    /**
     * Delete a user.
     * 
     * @param userName
     * @throws AuthenticationException
     */
    public void deleteUser(String userName) throws AuthenticationException
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
        // Nothing to do
    }
    
    /**
     * Check is a user exists.
     * 
     * @param userName
     * @return
     */
    public boolean userExists(String userName)
    {
        return true;
    }
    
    /**
     * Get the store ref where user objects are persisted.
     * 
     * @return
     */
    public StoreRef getUserStoreRef()
    {
        throw new AlfrescoRuntimeException("Not implemented");
/**        
        if (m_userStoreRef == null)
        {
            m_userStoreRef = new StoreRef("user", "alfrescoUserStore");
        }
        if (m_nodeService.exists(m_userStoreRef) == false)
        {
            m_nodeService.createStore(m_userStoreRef.getProtocol(), m_userStoreRef.getIdentifier());
        }

        return m_userStoreRef;
**/        
    }
    
    /**
     * Enable/disable a user.
     * 
     * @param userName
     * @param enabled
     */
    public void setEnabled(String userName, boolean enabled)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
        // Nothing to do
    }
    
    /**
     * Getter for user enabled
     * 
     * @param userName
     * @return
     */
    public boolean getEnabled(String userName)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return true;
    }
    
    /**
     * Set if the account should expire
     * 
     * @param userName
     * @param expires
     */
    public void setAccountExpires(String userName, boolean expires)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
        // Nothing to do
    }
    
    /**
     * Does the account expire?
     * 
     * @param userName
     * @return
     */
            
    public boolean getAccountExpires(String userName)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return false;
    }
    
    /**
     * Has the account expired?
     * 
     * @param userName
     * @return
     */
    public boolean getAccountHasExpired(String userName)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return false;
    }
  
    /**
     * Set if the password expires.
     * 
     * @param userName
     * @param expires
     */
    public void setCredentialsExpire(String userName, boolean expires)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
        // Nothing to do
    }
  
    /**
     * Do the credentials for the user expire?
     * 
     * @param userName
     * @return
     */
    public boolean getCredentialsExpire(String userName)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return false;
    }
    
    /**
     * Have the credentials for the user expired?
     * 
     * @param userName
     * @return
     */
    public boolean getCredentialsHaveExpired(String userName)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return false;
    }
    
    /**
     * Set if the account is locked.
     * 
     * @param userName
     * @param locked
     */
    public void setLocked(String userName, boolean locked)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
        // Nothing to do
    }
    
    /**
     * Is the account locked?
     * 
     * @param userName
     * @return
     */
    public boolean getAccountlocked(String userName)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return false;
    }
    
    /**
     * Set the date on which the account expires
     * 
     * @param userName
     * @param exipryDate
     */
    public void setAccountExpiryDate(String userName, Date exipryDate)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
        // Nothing to do
    }
    
    /** 
     * Get the date when this account expires.
     * 
     * @param userName
     * @return
     */
    public Date getAccountExpiryDate(String userName)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return null;
    }
    
    /**
     * Set the date when credentials expire.
     * 
     * @param userName
     * @param exipryDate
     */
    public void setCredentialsExpiryDate(String userName, Date exipryDate)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
        // Nothing to do
    }
    
    /**
     * Get the date when the credentials/password expire.
     * 
     * @param userName
     * @return
     */
    public Date getCredentialsExpiryDate(String userName)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return null;
    }
    
    /**
     * Get the MD4 password hash
     * 
     * @param userName
     * @return
     */
    public String getMD4HashedPassword(String userName)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return null;
    }
    
    /**
     * Are user names case sensitive?
     * 
     * @return
     */
    public boolean getUserNamesAreCaseSensitive()
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return false;
    }

    /**
     * Return the user details for the specified user
     * 
     * @param user String
     * @return UserDetails
     * @exception UsernameNotFoundException
     * @exception DataAccessException
     */
    public UserDetails loadUserByUsername(String arg0) throws UsernameNotFoundException, DataAccessException
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return null;
    }

    /**
     * Return salt for user
     * 
     * @param user UserDetails
     * @return Object
     */
    public Object getSalt(UserDetails user)
    {
        throw new AlfrescoRuntimeException("Not implemented");
        
//        return null;
    }
}
