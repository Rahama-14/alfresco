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
package org.alfresco.repo.security.authentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.GrantedAuthorityImpl;
import net.sf.acegisecurity.UserDetails;
import net.sf.acegisecurity.providers.dao.User;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;

/**
 * This class abstract the support required to set up and query the Acegi context for security enforcement. There are
 * some simple default method implementations to support simple authentication.
 * 
 * @author Andy Hind
 */
public abstract class AbstractAuthenticationComponent implements AuthenticationComponent
{
    /**
     * The abstract class keeps track of support for guest login
     */
    private Boolean allowGuestLogin = null;

    private Set<String> defaultAdministratorUserNames = Collections.emptySet();

    private boolean autoCreatePeopleOnLogin = true;
    
    private AuthenticationContext authenticationContext;
    
    private PersonService personService;

    private NodeService nodeService;

    private TransactionService transactionService;

    public AbstractAuthenticationComponent()
    {
        super();
    }

    /**
     * Set if guest login is supported.
     * 
     * @param allowGuestLogin
     */
    public void setAllowGuestLogin(Boolean allowGuestLogin)
    {
        this.allowGuestLogin = allowGuestLogin;
    }

    public void setAuthenticationContext(AuthenticationContext authenticationContext)
    {
        this.authenticationContext = authenticationContext;
    }

    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    public TransactionService getTransactionService()
    {
        return transactionService;
    }

    public Boolean getAllowGuestLogin()
    {
        return allowGuestLogin;
    }

    public NodeService getNodeService()
    {
        return nodeService;
    }

    public PersonService getPersonService()
    {
        return personService;
    }

    public boolean isAutoCreatePeopleOnLogin()
    {
        return autoCreatePeopleOnLogin;
    }

    public void setAutoCreatePeopleOnLogin(boolean autoCreatePeopleOnLogin)
    {
        this.autoCreatePeopleOnLogin = autoCreatePeopleOnLogin;
    }
        
    public void authenticate(String userName, char[] password) throws AuthenticationException
    {
        // Support guest login from the login screen
        if (isGuestUserName(userName))
        {
            setGuestUserAsCurrentUser(getUserDomain(userName));
        }
        else
        {
            authenticateImpl(userName, password);
        }
    }

    /**
     * Default unsupported authentication implementation - as of 2.1 this is the best way to implement your own
     * authentication component as it will support guest login - prior to this direct over ride for authenticate(String ,
     * char[]) was used. This will still work.
     * 
     * @param userName
     * @param password
     */
    protected void authenticateImpl(String userName, char[] password)
    {
        throw new UnsupportedOperationException();
    }

    public Authentication setCurrentUser(String userName, UserNameValidationMode validationMode)
    {
        switch (validationMode)
        {
        case NONE:
            return setCurrentUserImpl(userName);
        case CHECK_AND_FIX:
        default:
            return setCurrentUser(userName);
        }
    }

    public Authentication setCurrentUser(final String userName) throws AuthenticationException
    {
        if (isSystemUserName(userName))
        {
            return setCurrentUserImpl(userName);
        }
        else
        {
            SetCurrentUserCallback callback = new SetCurrentUserCallback(userName);
            Authentication auth;
            // If the repository is read only, we have to settle for a read only transaction. Auto user creation will
            // not be possible.
            if (transactionService.isReadOnly())
            {
                auth = transactionService.getRetryingTransactionHelper().doInTransaction(callback, true, false);
            }
            // Otherwise, we want a writeable transaction, so if the current transaction is read only we set the
            // requiresNew flag to true
            else
            {
                auth = transactionService.getRetryingTransactionHelper().doInTransaction(callback, false,
                        AlfrescoTransactionSupport.getTransactionReadState() == TxnReadState.TXN_READ_ONLY);
            }
            if ((auth == null) || (callback.ae != null))
            {
                throw callback.ae;
            }
            return auth;
        }
    }

    /**
     * Explicitly set the current user to be authenticated.
     * 
     * @param userName
     *            String
     * @return Authentication
     */
    private Authentication setCurrentUserImpl(String userName) throws AuthenticationException
    {
        if (userName == null)
        {
            throw new AuthenticationException("Null user name");
        }

        if (isSystemUserName(userName))
        {
            return setSystemUserAsCurrentUser(getUserDomain(userName));
        }

        try
        {
            UserDetails ud = null;
            if (isGuestUserName(userName))
            {
                GrantedAuthority[] gas = new GrantedAuthority[0];
                ud = new User(getGuestUserName(getUserDomain(userName)), "", true, true, true, true, gas);
            }
            else
            {
                ud = getUserDetails(userName);
            }
            return setUserDetails(ud);
        }
        catch (net.sf.acegisecurity.AuthenticationException ae)
        {
            throw new AuthenticationException(ae.getMessage(), ae);
        }
    }

    /**
     * Default implementation that makes an ACEGI object on the fly
     * 
     * @param userName
     * @return
     */
    protected UserDetails getUserDetails(String userName)
    {
        GrantedAuthority[] gas = new GrantedAuthority[1];
        gas[0] = new GrantedAuthorityImpl("ROLE_AUTHENTICATED");
        UserDetails ud = new User(userName, "", true, true, true, true, gas);
        return ud;
    }

    /**
     * {@inheritDoc}
     */
    public Authentication setCurrentAuthentication(Authentication authentication)
    {
        return this.authenticationContext.setCurrentAuthentication(authentication);
    }

    /**
     * Get the current authentication context
     * 
     * @return Authentication
     * @throws AuthenticationException
     */
    public Authentication getCurrentAuthentication() throws AuthenticationException
    {
        return authenticationContext.getCurrentAuthentication();
    }

    /**
     * Get the current user name.
     * 
     * @return String
     * @throws AuthenticationException
     */
    public String getCurrentUserName() throws AuthenticationException
    {
        return authenticationContext.getCurrentUserName();
    }

    /**
     * Set the system user as the current user note: for MT, will set to default domain only
     * 
     * @return Authentication
     */
    public Authentication setSystemUserAsCurrentUser()
    {
        return authenticationContext.setSystemUserAsCurrentUser();
    }

    /**
     * Get the name of the system user note: for MT, will get system for default domain only
     * 
     * @return String
     */
    public String getSystemUserName()
    {
        return authenticationContext.getSystemUserName();
    }

    /**
     * Is this the system user ?
     * 
     * @return boolean
     */
    public boolean isSystemUserName(String userName)
    {
        return authenticationContext.isSystemUserName(userName);
    }

    /**
     * Is the current user the system user?
     * 
     * @return boolean
     */
    public boolean isCurrentUserTheSystemUser()
    {
        return authenticationContext.isCurrentUserTheSystemUser();
    }

    /**
     * Get the name of the Guest User note: for MT, will get guest for default domain only
     * 
     * @return String
     */
    public String getGuestUserName()
    {
        return authenticationContext.getGuestUserName();
    }

    public String getGuestUserName(String tenantDomain)
    {
        return authenticationContext.getGuestUserName(tenantDomain);
    }

    /**
     * Set the guest user as the current user. note: for MT, will set to default domain only
     */
    public Authentication setGuestUserAsCurrentUser() throws AuthenticationException
    {
        return setGuestUserAsCurrentUser(TenantService.DEFAULT_DOMAIN);
    }

    /**
     * Set the guest user as the current user.
     */
    private Authentication setGuestUserAsCurrentUser(String tenantDomain) throws AuthenticationException
    {
        if (allowGuestLogin == null)
        {
            if (implementationAllowsGuestLogin())
            {
                return setCurrentUser(getGuestUserName(tenantDomain));
            }
            else
            {
                throw new AuthenticationException("Guest authentication is not allowed");
            }
        }
        else
        {
            if (allowGuestLogin.booleanValue())
            {
                return setCurrentUser(getGuestUserName(tenantDomain));
            }
            else
{
                throw new AuthenticationException("Guest authentication is not allowed");
            }

        }
    }
    
    public boolean isGuestUserName(String userName)
    {
        return authenticationContext.isGuestUserName(userName);
    }
    
    
    protected abstract boolean implementationAllowsGuestLogin();

    
    /**
     * @return true if Guest user authentication is allowed, false otherwise
     */
    public boolean guestUserAuthenticationAllowed()
    {
        if (allowGuestLogin == null)
        {
            return (implementationAllowsGuestLogin());
        }
        else
        {
            return (allowGuestLogin.booleanValue());
        }
    }

    /**
     * Remove the current security information
     */
    public void clearCurrentSecurityContext()
    {
        authenticationContext.clearCurrentSecurityContext();
    }

    /**
     * The default is not to support Authentication token base authentication
     */
    public Authentication authenticate(Authentication token) throws AuthenticationException
    {
        throw new AlfrescoRuntimeException("Authentication via token not supported");
    }

    /**
     * The should only be supported if getNTLMMode() is NTLMMode.MD4_PROVIDER.
     */
    public String getMD4HashedPassword(String userName)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the NTML mode - none - supports MD4 hash to integrate - or it can asct as an NTLM authentication
     */
    public NTLMMode getNTLMMode()
    {
        return NTLMMode.NONE;
    }

    class SetCurrentUserCallback implements RetryingTransactionHelper.RetryingTransactionCallback<Authentication>
    {
        AuthenticationException ae = null;

        String userName;

        SetCurrentUserCallback(String userName)
        {
            this.userName = userName;
        }

        public Authentication execute() throws Throwable
        {
            try
            {
                String name = AuthenticationUtil.runAs(new RunAsWork<String>()
                {
                    public String doWork() throws Exception
                    {
                        if (personService.personExists(userName))
                        {
                            NodeRef userNode = personService.getPerson(userName);
                            if (userNode != null)
                            {
                                // Get the person name and use that as the current user to line up with permission
                                // checks
                                return (String) nodeService.getProperty(userNode, ContentModel.PROP_USERNAME);
                            }
                            else
                            {
                                // Get user name
                                return userName;
                            }
                        }
                        else
                        {
                            if (autoCreatePeopleOnLogin && (userName != null) && !userName.equals(AuthenticationUtil.getSystemUserName()))
                            {
                                if (personService.createMissingPeople())
                                {
                                    AuthorityType authorityType = AuthorityType.getAuthorityType(userName);
                                    if (authorityType == AuthorityType.USER)
                                    {
                                        personService.getPerson(userName);
                                    }
                                }
                            }
                            // Get user name
                            return userName;
                        }
                    }
                }, getSystemUserName(getUserDomain(userName)));

                return setCurrentUserImpl(name);
            }
            catch (AuthenticationException ae)
            {
                this.ae = ae;
                return null;
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.security.authentication.AuthenticationComponent#getDefaultAdministratorUserNames()
     */
    public Set<String> getDefaultAdministratorUserNames()
    {
        return this.defaultAdministratorUserNames;
    }

    /**
     * Sets the user names who for this particular authentication system should be considered administrators by default.
     * 
     * @param defaultAdministratorUserNames
     *            a set of user names
     */
    public void setDefaultAdministratorUserNames(Set<String> defaultAdministratorUserNames)
    {
        this.defaultAdministratorUserNames = defaultAdministratorUserNames;
    }
    
    /**
     * Convenience method to allow the administrator user names to be specified as a comma separated list
     * 
     * @param defaultAdministratorUserNames
     */
    public void setDefaultAdministratorUserNames(String defaultAdministratorUserNames)
    {
        Set<String> nameSet = new TreeSet<String>();
        if (defaultAdministratorUserNames.length() > 0)
        {
            nameSet.addAll(Arrays.asList(defaultAdministratorUserNames.split(",")));
        }
        setDefaultAdministratorUserNames(nameSet);
    }

    public String getSystemUserName(String tenantDomain)
    {
        return authenticationContext.getSystemUserName(tenantDomain);
    }

    public String getUserDomain(String userName)
    {
        return authenticationContext.getUserDomain(userName);
    }

    public Authentication setSystemUserAsCurrentUser(String tenantDomain)
    {
        return authenticationContext.setSystemUserAsCurrentUser(tenantDomain);
    }

    public Authentication setUserDetails(UserDetails ud)
    {
        return authenticationContext.setUserDetails(ud);
    }
}
