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

import java.util.Collections;
import java.util.Set;

import org.alfresco.repo.management.subsystems.ActivateableBean;
import org.alfresco.repo.security.authentication.AuthenticationComponent.UserNameValidationMode;

public class AuthenticationServiceImpl extends AbstractAuthenticationService implements ActivateableBean
{
    AuthenticationComponent authenticationComponent;
    
    TicketComponent ticketComponent;
    
    private String domain;
    
    private boolean allowsUserCreation = true;
    
    private boolean allowsUserDeletion = true;
    
    private boolean allowsUserPasswordChange = true;
    
    public AuthenticationServiceImpl()
    {
        super();
    }
    
    public void setTicketComponent(TicketComponent ticketComponent)
    {
        this.ticketComponent = ticketComponent;
    }

    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.management.subsystems.ActivateableBean#isActive()
     */
    public boolean isActive()
    {
        return !(this.authenticationComponent instanceof ActivateableBean)
                || ((ActivateableBean) this.authenticationComponent).isActive();
    }


    public void authenticate(String userName, char[] password) throws AuthenticationException
    {
        try
        {
            // clear context - to avoid MT concurrency issue (causing domain mismatch) - see also 'validate' below
            clearCurrentSecurityContext();
            preAuthenticationCheck(userName);
        	authenticationComponent.authenticate(userName, password);
        }
        catch(AuthenticationException ae)
        {
            clearCurrentSecurityContext();
            throw ae;
        }
        ticketComponent.clearCurrentTicket();
        
        ticketComponent.getCurrentTicket(userName, null, true); // to ensure new ticket is created (even if client does not explicitly call getCurrentTicket)
    }
    
    public String getCurrentUserName() throws AuthenticationException
    {
        return authenticationComponent.getCurrentUserName();
    }

    public void invalidateUserSession(String userName) throws AuthenticationException
    {
        ticketComponent.invalidateTicketByUser(userName);
    }
    
    public Set<String> getUsersWithTickets(boolean nonExpiredOnly)
    {
    	return ticketComponent.getUsersWithTickets(nonExpiredOnly);
    }

    public void invalidateTicket(String ticket, String sessionId) throws AuthenticationException
    {
        ticketComponent.invalidateTicketById(ticket, sessionId);
    }
    
    public int countTickets(boolean nonExpiredOnly)
    {
    	return ticketComponent.countTickets(nonExpiredOnly);
    }
    
    public int invalidateTickets(boolean expiredOnly)
    {
    	return ticketComponent.invalidateTickets(expiredOnly);
    }
    

    public void validate(String ticket, String sessionId) throws AuthenticationException
    {
        String currentUser = null;
        try
        {

            // clear context - to avoid MT concurrency issue (causing domain mismatch) - see also 'authenticate' above
            clearCurrentSecurityContext();
            currentUser = ticketComponent.validateTicket(ticket, sessionId);
            authenticationComponent.setCurrentUser(currentUser, UserNameValidationMode.CHECK);
        }
        catch (AuthenticationException ae)
        {
            clearCurrentSecurityContext();
            throw ae;
        }
    }

    public String getCurrentTicket(String sessionId) throws AuthenticationException
    {
        String userName = getCurrentUserName();
        // So that preAuthenticationCheck can constrain the creation of new tickets, we first ask for the current ticket
        // without auto-creation
        String ticket = ticketComponent.getCurrentTicket(userName, sessionId, false);
        if (ticket == null)
        {
            try
            {
                preAuthenticationCheck(userName);
            }
            catch (AuthenticationException e)
            {
                clearCurrentSecurityContext();
                throw e;
            }
            // If we get through the authentication check then it's safe to issue a new ticket (e.g. for
            // SSO/external-based login)
            return ticketComponent.getCurrentTicket(userName, sessionId, true);
        }
        return ticket;
    }

    public String getNewTicket(String sessionId)
    {
        String userName = getCurrentUserName();
        try
        {
            preAuthenticationCheck(userName);
        }
        catch (AuthenticationException e)
        {
            clearCurrentSecurityContext();
            throw e;
        }
        return ticketComponent.getNewTicket(userName, sessionId);
    }
    
    public void clearCurrentSecurityContext()
    {
        authenticationComponent.clearCurrentSecurityContext();
        ticketComponent.clearCurrentTicket();
    }

    public boolean isCurrentUserTheSystemUser()
    {
        return authenticationComponent.isSystemUserName(getCurrentUserName());
    }

    public void authenticateAsGuest() throws AuthenticationException
    {
        String defaultGuestName = AuthenticationUtil.getGuestUserName();
        if (defaultGuestName == null || defaultGuestName.length() == 0)
        {
            throw new AuthenticationException("Guest authentication not supported");
        }
        preAuthenticationCheck(defaultGuestName);
        authenticationComponent.setGuestUserAsCurrentUser();
        String guestUser = authenticationComponent.getCurrentUserName();
        ticketComponent.clearCurrentTicket();
        ticketComponent.getCurrentTicket(guestUser, null, true); // to ensure new ticket is created (even if client does not explicitly call getCurrentTicket)
    }
    
    public boolean guestUserAuthenticationAllowed()
    {
        return authenticationComponent.guestUserAuthenticationAllowed();
    }

    public boolean getAllowsUserCreation()
    {
        return allowsUserCreation;
    }

    public void setAllowsUserCreation(boolean allowsUserCreation)
    {
        this.allowsUserCreation = allowsUserCreation;
    }

    public boolean getAllowsUserDeletion()
    {
        return allowsUserDeletion;
    }

    public void setAllowsUserDeletion(boolean allowsUserDeletion)
    {
        this.allowsUserDeletion = allowsUserDeletion;
    }

    public boolean getAllowsUserPasswordChange()
    {
        return allowsUserPasswordChange;
    }

    public void setAllowsUserPasswordChange(boolean allowsUserPasswordChange)
    {
        this.allowsUserPasswordChange = allowsUserPasswordChange;
    }

    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public Set<String> getDomains()
    {
       return Collections.singleton(getDomain());
    }

    public Set<String> getDomainsThatAllowUserCreation()
    {
        if(getAllowsUserCreation())
        {
            return Collections.singleton(getDomain()); 
        }
        else
        {
            return Collections.<String>emptySet();
        }
    }

    public Set<String> getDomainsThatAllowUserDeletion()
    {
        if(getAllowsUserDeletion())
        {
            return Collections.singleton(getDomain()); 
        }
        else
        {
            return Collections.<String>emptySet();
        }
    }

    public Set<String> getDomiansThatAllowUserPasswordChanges()
    {
        if(getAllowsUserPasswordChange())
        {
            return Collections.singleton(getDomain()); 
        }
        else
        {
            return Collections.<String>emptySet();
        }
    }

    @Override
    public Set<TicketComponent> getTicketComponents()
    {
        return Collections.singleton(ticketComponent);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDefaultAdministratorUserNames()
    {
        return authenticationComponent.getDefaultAdministratorUserNames();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDefaultGuestUserNames()
    {
        return authenticationComponent.getDefaultGuestUserNames();
    }

    /**
     * {@inheritDoc}
     */
    public boolean authenticationExists(String userName)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean getAuthenticationEnabled(String userName) throws AuthenticationException
    {
        return true;
    }        
}
