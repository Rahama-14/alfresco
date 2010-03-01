/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.security.authentication;

import java.util.Date;

import net.sf.acegisecurity.UserDetails;
import net.sf.acegisecurity.providers.dao.UsernameNotFoundException;

import org.alfresco.error.AlfrescoRuntimeException;
import org.springframework.dao.DataAccessException;

/**
 * An authority DAO that has no implementation.
 * 
 * By default it will throw an exception if any method is called.
 * 
 * Any of the getter/setter methods can be enabled with a no action implementation.
 * 
 * This can support deleting users via the UI for LDAP and NTLM. The Alfresco person object is deleted from the UI.
 * The call to delete the user will return with no action. 
 * 
 * The following methods will always fail.
 * 
 * getMD4HashedPassword(String userName)
 * loadUserByUsername(String arg0) 
 * getSalt(UserDetails user)
 * 
 * @author Andy Hind
 */
public class DefaultMutableAuthenticationDao implements MutableAuthenticationDao
{
    private boolean allowCreateUser = false;

    private boolean allowUpdateUser = false;

    private boolean allowDeleteUser = false;

    private boolean allowSetEnabled = false;

    private boolean allowGetEnabled = false;

    private boolean allowSetAccountExpires = false;

    private boolean allowGetAccountHasExpired = false;

    private boolean allowSetCredentialsExpire = false;

    private boolean allowGetCredentialsExpire = false;

    private boolean allowGetCredentialsHaveExpired = false;

    private boolean allowSetAccountLocked = false;

    private boolean allowGetAccountLocked = false;

    private boolean allowSetAccountExpiryDate = false;

    private boolean allowGetAccountExpiryDate = false;

    private boolean allowSetCredentialsExpiryDate = false;

    private boolean allowGetCredentialsExpiryDate = false;
    
    /**
     * Create a user with the given userName and password
     * 
     * If enabled does nothing.
     * 
     * @param userName
     * @param rawPassword
     * @throws AuthenticationException
     */
    public void createUser(String userName, char[] rawPassword) throws AuthenticationException
    {
        if (!allowCreateUser)
        {
            throw new AlfrescoRuntimeException("Create User is not supported");
        }
    }

    /**
     * Update a user's password.
     * 
     * If enabled does nothing.
     * 
     * @param userName
     * @param rawPassword
     * @throws AuthenticationException
     */
    public void updateUser(String userName, char[] rawPassword) throws AuthenticationException
    {
        if (!allowUpdateUser)
        {
            throw new AlfrescoRuntimeException("Update user is not supported");
        }
    }

    /**
     * Delete a user.
     * 
     * If enabled does nothing.
     * 
     * @param userName
     * @throws AuthenticationException
     */
    public void deleteUser(String userName) throws AuthenticationException
    {
        if (!allowDeleteUser)
        {
            throw new AlfrescoRuntimeException("Delete user is not supported");
        }
    }

    /**
     * Check is a user exists.
     * 
     * If enabled returns true.
     * 
     * @param userName
     * @return
     */
    public boolean userExists(String userName)
    {
        // All users may exist
        return true;
    }

    /**
     * Enable/disable a user.
     * 
     * If enabled does nothing.
     * 
     * @param userName
     * @param enabled
     */
    public void setEnabled(String userName, boolean enabled)
    {
        if (!allowSetEnabled)
        {
            throw new AlfrescoRuntimeException("Set enabled is not supported");
        }
    }

    /**
     * Getter for user enabled
     * 
     * If enabled returns true.
     * 
     * @param userName
     * @return
     */
    public boolean getEnabled(String userName)
    {
        if (!allowGetEnabled)
        {
            throw new AlfrescoRuntimeException("Get enabled is not supported");
        }
        return true;
    }

    /**
     * Set if the account should expire
     * 
     * If enabled does nothing.
     * 
     * @param userName
     * @param expires
     */
    public void setAccountExpires(String userName, boolean expires)
    {
        if (!allowSetAccountExpires)
        {
            throw new AlfrescoRuntimeException("Set account expires is not supported");
        }
    }

    /**
     * Does the account expire?
     * 
     * If enabled returns false.
     * 
     * @param userName
     * @return
     */

    public boolean getAccountExpires(String userName)
    {
        if (!allowSetAccountExpires)
        {
            throw new AlfrescoRuntimeException("Get account expires is not supported");
        }
        return false;
    }

    /**
     * Has the account expired?
     * 
     * If enabled returns false.
     * 
     * @param userName
     * @return
     */
    public boolean getAccountHasExpired(String userName)
    {
        if (!allowGetAccountHasExpired)
        {
            throw new AlfrescoRuntimeException("Get account has expired is not supported");
        }
        return false;
    }

    /**
     * Set if the password expires.
     * 
     * If enabled does nothing.
     * 
     * @param userName
     * @param expires
     */
    public void setCredentialsExpire(String userName, boolean expires)
    {
        if (!allowSetCredentialsExpire)
        {
            throw new AlfrescoRuntimeException("Set credentials expire is not supported");
        }
    }

    /**
     * Do the credentials for the user expire?
     * 
     * If enabled returns false.
     * 
     * @param userName
     * @return
     */
    public boolean getCredentialsExpire(String userName)
    {
        if (!allowGetCredentialsExpire)
        {
            throw new AlfrescoRuntimeException("Get credentials expire is not supported");
        }
        return false;
    }

    /**
     * Have the credentials for the user expired?
     * 
     * If enabled returns false.
     * 
     * @param userName
     * @return
     */
    public boolean getCredentialsHaveExpired(String userName)
    {
        if (!allowGetCredentialsHaveExpired)
        {
            throw new AlfrescoRuntimeException("Get credentials have expired is not supported");
        }
        return false;
    }

    /**
     * Set if the account is locked.
     * 
     * If enabled does nothing.
     * 
     * @param userName
     * @param locked
     */
    public void setLocked(String userName, boolean locked)
    {
        if (!allowSetAccountLocked)
        {
            throw new AlfrescoRuntimeException("Set account locked is not supported");
        }
    }

    /**
     * Is the account locked?
     * 
     * If enabled returns false.
     * 
     * @param userName
     * @return
     */
    public boolean getAccountlocked(String userName)
    {
        if (!allowGetAccountLocked)
        {
            throw new AlfrescoRuntimeException("Get account locked is not supported");
        }
        return false;
    }

    /**
     * Set the date on which the account expires
     * 
     * If enabled does nothing.
     * 
     * @param userName
     * @param exipryDate
     */
    public void setAccountExpiryDate(String userName, Date exipryDate)
    {
        if (!allowSetAccountExpiryDate)
        {
            throw new AlfrescoRuntimeException("Set account expiry date is not supported");
        }
    }

    /**
     * Get the date when this account expires.
     * 
     * If enabled returns null.
     * 
     * @param userName
     * @return
     */
    public Date getAccountExpiryDate(String userName)
    {
        if (!allowGetAccountExpiryDate)
        {
            throw new AlfrescoRuntimeException("Get account expiry date is not supported");
        }
        return null;
    }

    /**
     * Set the date when credentials expire.
     * 
     * If enabled does nothing.
     * 
     * @param userName
     * @param exipryDate
     */
    public void setCredentialsExpiryDate(String userName, Date exipryDate)
    {
        if (!allowSetCredentialsExpiryDate)
        {
            throw new AlfrescoRuntimeException("Set credentials expiry date is not supported");
        }
    }

    /**
     * Get the date when the credentials/password expire.
     * 
     * If enabled returns null.
     * 
     * @param userName
     * @return
     */
    public Date getCredentialsExpiryDate(String userName)
    {
        if (!allowGetCredentialsExpiryDate)
        {
            throw new AlfrescoRuntimeException("Get credentials expiry date is not supported");
        }
        return null;
    }

    /**
     * Get the MD4 password hash
     * 
     * Always throws an exception.
     * 
     * @param userName
     * @return
     */
    public String getMD4HashedPassword(String userName)
    {
        throw new AlfrescoRuntimeException("Not implemented");
    }

    /**
     * Return the user details for the specified user
     * 
     *  Always throws an exception.
     * 
     * @param user
     *            String
     * @return UserDetails
     * @exception UsernameNotFoundException
     * @exception DataAccessException
     */
    public UserDetails loadUserByUsername(String arg0) throws UsernameNotFoundException, DataAccessException
    {
        throw new AlfrescoRuntimeException("Not implemented");
    }

    /**
     * Return salt for user
     * 
     * Always throws an exception.
     * 
     * @param user
     *            UserDetails
     * @return Object
     */
    public Object getSalt(UserDetails user)
    {
        throw new AlfrescoRuntimeException("Not implemented");
    }

    
    // -------- //
    // Bean IOC //
    // -------- //
    
    public void setAllowCreateUser(boolean allowCreateUser)
    {
        this.allowCreateUser = allowCreateUser;
    }

    public void setAllowDeleteUser(boolean allowDeleteUser)
    {
        this.allowDeleteUser = allowDeleteUser;
    }

    public void setAllowGetAccountExpiryDate(boolean allowGetAccountExpiryDate)
    {
        this.allowGetAccountExpiryDate = allowGetAccountExpiryDate;
    }

    public void setAllowGetAccountHasExpired(boolean allowGetAccountHasExpired)
    {
        this.allowGetAccountHasExpired = allowGetAccountHasExpired;
    }

    public void setAllowGetAccountLocked(boolean allowGetAccountLocked)
    {
        this.allowGetAccountLocked = allowGetAccountLocked;
    }

    public void setAllowGetCredentialsExpire(boolean allowGetCredentialsExpire)
    {
        this.allowGetCredentialsExpire = allowGetCredentialsExpire;
    }

    public void setAllowGetCredentialsExpiryDate(boolean allowGetCredentialsExpiryDate)
    {
        this.allowGetCredentialsExpiryDate = allowGetCredentialsExpiryDate;
    }

    public void setAllowGetCredentialsHaveExpired(boolean allowGetCredentialsHaveExpired)
    {
        this.allowGetCredentialsHaveExpired = allowGetCredentialsHaveExpired;
    }

    public void setAllowGetEnabled(boolean allowGetEnabled)
    {
        this.allowGetEnabled = allowGetEnabled;
    }

    public void setAllowSetAccountExpires(boolean allowSetAccountExpires)
    {
        this.allowSetAccountExpires = allowSetAccountExpires;
    }

    public void setAllowSetAccountExpiryDate(boolean allowSetAccountExpiryDate)
    {
        this.allowSetAccountExpiryDate = allowSetAccountExpiryDate;
    }

    public void setAllowSetAccountLocked(boolean allowSetAccountLocked)
    {
        this.allowSetAccountLocked = allowSetAccountLocked;
    }

    public void setAllowSetCredentialsExpire(boolean allowSetCredentialsExpire)
    {
        this.allowSetCredentialsExpire = allowSetCredentialsExpire;
    }

    public void setAllowSetCredentialsExpiryDate(boolean allowSetCredentialsExpiryDate)
    {
        this.allowSetCredentialsExpiryDate = allowSetCredentialsExpiryDate;
    }

    public void setAllowSetEnabled(boolean allowSetEnabled)
    {
        this.allowSetEnabled = allowSetEnabled;
    }

    public void setAllowUpdateUser(boolean allowUpdateUser)
    {
        this.allowUpdateUser = allowUpdateUser;
    }
}
