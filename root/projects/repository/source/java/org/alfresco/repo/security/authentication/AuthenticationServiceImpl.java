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

import java.util.Collections;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationComponent.UserNameValidationMode;
import org.alfresco.service.Managed;
import org.alfresco.service.cmr.security.PermissionService;

public class AuthenticationServiceImpl extends AbstractAuthenticationService
{
    MutableAuthenticationDao authenticationDao;

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
    
    @Managed(category="Security")
    public void setAuthenticationDao(MutableAuthenticationDao authenticationDao)
    {
        this.authenticationDao = authenticationDao;
    }

    public void setTicketComponent(TicketComponent ticketComponent)
    {
        this.ticketComponent = ticketComponent;
    }

    @Managed(category="Security")
    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }

    public void createAuthentication(String userName, char[] password) throws AuthenticationException
    {
        authenticationDao.createUser(userName, password);
    }

    public void updateAuthentication(String userName, char[] oldPassword, char[] newPassword)
            throws AuthenticationException
    {
        // Need to preserve the run-as user
        String currentUser = AuthenticationUtil.getRunAsUser();
        try
        {
            authenticate(userName, oldPassword);
        }
        finally
        {
            AuthenticationUtil.setRunAsUser(currentUser);
        }
        authenticationDao.updateUser(userName, newPassword);
    }

    public void setAuthentication(String userName, char[] newPassword) throws AuthenticationException
    {
        authenticationDao.updateUser(userName, newPassword);
    }

    public void deleteAuthentication(String userName) throws AuthenticationException
    {
        authenticationDao.deleteUser(userName);
    }

    public boolean getAuthenticationEnabled(String userName) throws AuthenticationException
    {
        return authenticationDao.getEnabled(userName);
    }

    public void setAuthenticationEnabled(String userName, boolean enabled) throws AuthenticationException
    {
        authenticationDao.setEnabled(userName, enabled);
    }

    @SuppressWarnings("unchecked")
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
        
        ticketComponent.getCurrentTicket(userName); // to ensure new ticket is created (even if client does not explicitly call getCurrentTicket)
    }
    
    public boolean authenticationExists(String userName)
    {
        return authenticationDao.userExists(userName);
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

    public void invalidateTicket(String ticket) throws AuthenticationException
    {
        ticketComponent.invalidateTicketById(ticket);
    }
    
    public int countTickets(boolean nonExpiredOnly)
    {
    	return ticketComponent.countTickets(nonExpiredOnly);
    }
    
    public int invalidateTickets(boolean expiredOnly)
    {
    	return ticketComponent.invalidateTickets(expiredOnly);
    }
    

    public void validate(String ticket) throws AuthenticationException
    {
        try
        {

           // clear context - to avoid MT concurrency issue (causing domain mismatch) - see also 'authenticate' above
           clearCurrentSecurityContext();
           authenticationComponent.setCurrentUser(ticketComponent.validateTicket(ticket), UserNameValidationMode.NONE);
        }
        catch(AuthenticationException ae)
        {
            clearCurrentSecurityContext();
            throw ae;
        } 
    }

    public String getCurrentTicket()
    {
        return ticketComponent.getCurrentTicket(getCurrentUserName());
    }

    public String getNewTicket()
    {
        return ticketComponent.getNewTicket(getCurrentUserName());
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

    @SuppressWarnings("unchecked")
    public void authenticateAsGuest() throws AuthenticationException
    {
        preAuthenticationCheck(PermissionService.GUEST_AUTHORITY);
        authenticationComponent.setGuestUserAsCurrentUser();
        ticketComponent.clearCurrentTicket();
        ticketComponent.getCurrentTicket(PermissionService.GUEST_AUTHORITY); // to ensure new ticket is created (even if client does not explicitly call getCurrentTicket)
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
}
