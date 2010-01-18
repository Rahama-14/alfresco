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
package org.alfresco.repo.security.authentication;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;

/**
 * A base class for chaining authentication services. Where appropriate, methods will 'chain' across multiple
 * {@link AuthenticationService} instances, as returned by {@link #getUsableAuthenticationServices()}.
 * 
 * @author dward
 */
public abstract class AbstractChainingAuthenticationService extends AbstractAuthenticationService implements
        MutableAuthenticationService
{
    /**
     * Instantiates a new abstract chaining authentication service.
     */
    public AbstractChainingAuthenticationService()
    {
        super();
    }

    /**
     * Gets the mutable authentication service.
     * 
     * @return the mutable authentication service
     */
    public abstract MutableAuthenticationService getMutableAuthenticationService();

    /**
     * Gets the authentication services across which methods will chain.
     * 
     * @return the usable authentication services
     */
    protected abstract List<AuthenticationService> getUsableAuthenticationServices();

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.security.AuthenticationService#createAuthentication(java.lang.String, char[])
     */
    public void createAuthentication(String userName, char[] password) throws AuthenticationException
    {
        if (getMutableAuthenticationService() == null)
        {
            throw new AuthenticationException(
                    "Unable to create authentication as there is no suitable authentication service.");
        }
        getMutableAuthenticationService().createAuthentication(userName, password);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.security.AuthenticationService#updateAuthentication(java.lang.String, char[],
     * char[])
     */
    public void updateAuthentication(String userName, char[] oldPassword, char[] newPassword)
            throws AuthenticationException
    {
        if (getMutableAuthenticationService() == null)
        {
            throw new AuthenticationException(
                    "Unable to update authentication as there is no suitable authentication service.");
        }
        getMutableAuthenticationService().updateAuthentication(userName, oldPassword, newPassword);

    }

    /**
     * {@inheritDoc}
     */
    public void setAuthentication(String userName, char[] newPassword) throws AuthenticationException
    {
        if (getMutableAuthenticationService() == null)
        {
            throw new AuthenticationException(
                    "Unable to set authentication as there is no suitable authentication service.");
        }
        getMutableAuthenticationService().setAuthentication(userName, newPassword);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAuthentication(String userName) throws AuthenticationException
    {
        if (getMutableAuthenticationService() == null)
        {
            throw new AuthenticationException(
                    "Unable to delete authentication as there is no suitable authentication service.");
        }
        getMutableAuthenticationService().deleteAuthentication(userName);

    }

    /**
     * {@inheritDoc}
     */
    public void setAuthenticationEnabled(String userName, boolean enabled) throws AuthenticationException
    {
        if (getMutableAuthenticationService() == null)
        {
            throw new AuthenticationException(
                    "Unable to set authentication enabled as there is no suitable authentication service.");
        }
        getMutableAuthenticationService().setAuthenticationEnabled(userName, enabled);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAuthenticationMutable(String userName)
    {
        MutableAuthenticationService mutableAuthenticationService = getMutableAuthenticationService();
        return mutableAuthenticationService == null ? false : mutableAuthenticationService
                .isAuthenticationMutable(userName);
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isAuthenticationCreationAllowed()
    {
        MutableAuthenticationService mutableAuthenticationService = getMutableAuthenticationService();
        return mutableAuthenticationService == null ? false : mutableAuthenticationService
                .isAuthenticationCreationAllowed();
    }

    /**
     * {@inheritDoc}
     */
    public boolean getAuthenticationEnabled(String userName) throws AuthenticationException
    {
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            try
            {
                if (authService.getAuthenticationEnabled(userName))
                {
                    return true;
                }
            }
            catch (AuthenticationException e)
            {
                // Ignore and chain
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void authenticate(String userName, char[] password) throws AuthenticationException
    {
        preAuthenticationCheck(userName);
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            try
            {
                authService.authenticate(userName, password);
                return;
            }
            catch (AuthenticationException e)
            {
                // Ignore and chain
            }
        }
        throw new AuthenticationException("Failed to authenticate");

    }

    /**
     * {@inheritDoc}
     */
    public void authenticateAsGuest() throws AuthenticationException
    {
        String defaultGuestName = AuthenticationUtil.getGuestUserName();
        if (defaultGuestName != null && defaultGuestName.length() > 0)
        {
            preAuthenticationCheck(defaultGuestName);
        }
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            try
            {
                authService.authenticateAsGuest();
                return;
            }
            catch (AuthenticationException e)
            {
                // Ignore and chain
            }
        }
        throw new AuthenticationException("Guest authentication not supported");
    }

    /**
     * {@inheritDoc}
     */
    public boolean guestUserAuthenticationAllowed()
    {
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            if (authService.guestUserAuthenticationAllowed())
            {
                return true;
            }
        }
        // it isn't allowed in any of the authentication components
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean authenticationExists(String userName)
    {
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            if (authService.authenticationExists(userName))
            {
                return true;
            }
        }
        // it doesn't exist in any of the authentication components
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getCurrentUserName() throws AuthenticationException
    {
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            try
            {
                return authService.getCurrentUserName();
            }
            catch (AuthenticationException e)
            {
                // Ignore and chain
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void invalidateUserSession(String userName) throws AuthenticationException
    {
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            try
            {
                authService.invalidateUserSession(userName);
                return;
            }
            catch (AuthenticationException e)
            {
                // Ignore and chain
            }
        }
        throw new AuthenticationException("Unable to invalidate user session");

    }

    /**
     * {@inheritDoc}
     */
    public void invalidateTicket(String ticket, String sessionId) throws AuthenticationException
    {
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            try
            {
                authService.invalidateTicket(ticket, sessionId);
                return;
            }
            catch (AuthenticationException e)
            {
                // Ignore and chain
            }
        }
        throw new AuthenticationException("Unable to invalidate ticket");

    }

    /**
     * {@inheritDoc}
     */
    public void validate(String ticket, String sessionId) throws AuthenticationException
    {
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            try
            {
                authService.validate(ticket, sessionId);
                return;
            }
            catch (AuthenticationException e)
            {
                // Ignore and chain
            }
        }
        throw new AuthenticationException("Unable to validate ticket");

    }

    /**
     * {@inheritDoc}
     */
    public String getCurrentTicket(String sessionId)
    {
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            try
            {
                return authService.getCurrentTicket(sessionId);
            }
            catch (AuthenticationException e)
            {
                // Ignore and chain
            }
        }
        throw new AuthenticationException("Unable to issue ticket");
    }

    /**
     * {@inheritDoc}
     */
    public String getNewTicket(String sessionId)
    {
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            try
            {
                return authService.getNewTicket(sessionId);
            }
            catch (AuthenticationException e)
            {
                // Ignore and chain
            }
        }
        throw new AuthenticationException("Unable to issue ticket");
    }

    /**
     * {@inheritDoc}
     */
    public void clearCurrentSecurityContext()
    {
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            try
            {
                authService.clearCurrentSecurityContext();
                return;
            }
            catch (AuthenticationException e)
            {
                // Ignore and chain
            }
        }
        throw new AuthenticationException("Failed to clear security context");

    }

    /**
     * {@inheritDoc}
     */
    public boolean isCurrentUserTheSystemUser()
    {
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            try
            {
                return authService.isCurrentUserTheSystemUser();
            }
            catch (AuthenticationException e)
            {
                // Ignore and chain
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDomains()
    {
        HashSet<String> domains = new HashSet<String>();
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            domains.addAll(authService.getDomains());
        }
        return domains;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDomainsThatAllowUserCreation()
    {
        HashSet<String> domains = new HashSet<String>();
        if (getMutableAuthenticationService() != null)
        {
            domains.addAll(getMutableAuthenticationService().getDomainsThatAllowUserCreation());
        }
        return domains;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDomainsThatAllowUserDeletion()
    {
        HashSet<String> domains = new HashSet<String>();
        if (getMutableAuthenticationService() != null)
        {
            domains.addAll(getMutableAuthenticationService().getDomainsThatAllowUserDeletion());
        }
        return domains;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDomiansThatAllowUserPasswordChanges()
    {
        HashSet<String> domains = new HashSet<String>();
        if (getMutableAuthenticationService() != null)
        {
            domains.addAll(getMutableAuthenticationService().getDomiansThatAllowUserPasswordChanges());
        }
        return domains;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getUsersWithTickets(boolean nonExpiredOnly)
    {
        HashSet<String> users = new HashSet<String>();
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            if (authService instanceof AbstractAuthenticationService)
            {
                users.addAll(((AbstractAuthenticationService) authService).getUsersWithTickets(nonExpiredOnly));
            }
        }
        return users;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countTickets(boolean nonExpiredOnly)
    {
        int count = 0;
        for (TicketComponent tc : getTicketComponents())
        {
            count += tc.countTickets(nonExpiredOnly);
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int invalidateTickets(boolean nonExpiredOnly)
    {
        int count = 0;
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            if (authService instanceof AbstractAuthenticationService)
            {
                count += ((AbstractAuthenticationService) authService).invalidateTickets(nonExpiredOnly);
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<TicketComponent> getTicketComponents()
    {
        Set<TicketComponent> tcs = new HashSet<TicketComponent>();
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            if (authService instanceof AbstractAuthenticationService)
            {
                tcs.addAll(((AbstractAuthenticationService) authService).getTicketComponents());
            }
        }
        return tcs;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDefaultAdministratorUserNames()
    {
        Set<String> defaultAdministratorUserNames = new TreeSet<String>();
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            defaultAdministratorUserNames.addAll(authService.getDefaultAdministratorUserNames());
        }
        return defaultAdministratorUserNames;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDefaultGuestUserNames()
    {
        Set<String> defaultGuestUserNames = new TreeSet<String>();
        for (AuthenticationService authService : getUsableAuthenticationServices())
        {
            defaultGuestUserNames.addAll(authService.getDefaultGuestUserNames());
        }
        return defaultGuestUserNames;
    }

}