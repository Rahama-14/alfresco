/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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

import java.util.Stack;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.GrantedAuthorityImpl;
import net.sf.acegisecurity.UserDetails;
import net.sf.acegisecurity.context.Context;
import net.sf.acegisecurity.context.ContextHolder;
import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import net.sf.acegisecurity.providers.dao.User;

import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.log.NDC;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility helper methods to change the authenticated context for threads.
 */
public abstract class AuthenticationUtil
{
    static Log s_logger = LogFactory.getLog(AuthenticationUtil.class);

    public interface RunAsWork<Result>
    {
        /**
         * Method containing the work to be done in the user transaction.
         * 
         * @return Return the result of the operation
         */
        Result doWork() throws Exception;
    }

    public static final String SYSTEM_USER_NAME = "System";

    private static boolean mtEnabled = false;
    
    public static void setMtEnabled(boolean mtEnabled)
    {
        if (!AuthenticationUtil.mtEnabled)
        {
            AuthenticationUtil.mtEnabled = mtEnabled;
        }
    }

    public static boolean isMtEnabled()
    {
        return AuthenticationUtil.mtEnabled;
    }

    private AuthenticationUtil()
    {
        super();
    }

    /**
     * Utility method to create an authentication token
     */
    private static UsernamePasswordAuthenticationToken getAuthenticationToken(String userName, UserDetails providedDetails)
    {
        UserDetails ud = null;
        if (userName.equals(SYSTEM_USER_NAME))
        {
            GrantedAuthority[] gas = new GrantedAuthority[1];
            gas[0] = new GrantedAuthorityImpl("ROLE_SYSTEM");
            ud = new User(SYSTEM_USER_NAME, "", true, true, true, true, gas);
        }
        else if (userName.equalsIgnoreCase(PermissionService.GUEST_AUTHORITY))
        {
            GrantedAuthority[] gas = new GrantedAuthority[0];
            ud = new User(PermissionService.GUEST_AUTHORITY.toLowerCase(), "", true, true, true, true, gas);
        }
        else
        {
            if (providedDetails.getUsername().equals(userName))
            {
                ud = providedDetails;
            }
            else
            {
                throw new AuthenticationException("Provided user details do not match the user name");
            }
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(ud, "", ud.getAuthorities());
        auth.setDetails(ud);
        auth.setAuthenticated(true);
        return auth;
    }

    /**
     * Default implementation that makes an ACEGI object on the fly
     */
    private static UserDetails getDefaultUserDetails(String userName)
    {
        GrantedAuthority[] gas = new GrantedAuthority[1];
        gas[0] = new GrantedAuthorityImpl("ROLE_AUTHENTICATED");
        UserDetails ud = new User(userName, "", true, true, true, true, gas);
        return ud;
    }

    /**
     * Extract the username from the authentication.
     */
    private static String getUserName(Authentication authentication)
    {
        if (authentication.getPrincipal() instanceof UserDetails)
        {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        else
        {
            return authentication.getPrincipal().toString();
        }
    }

    /**
     * Authenticate as the given user.  The user will be authenticated and all operations
     * with be run in the context of this user.
     * 
     * @param userName              the user name
     * @return                      the authentication token
     */
    public static Authentication setFullyAuthenticatedUser(String userName)
    {
        return setFullyAuthenticatedUser(userName, getDefaultUserDetails(userName));
    }
    
    private static Authentication setFullyAuthenticatedUser(String userName, UserDetails providedDetails) throws AuthenticationException
    {
        if (userName == null)
        {
            throw new AuthenticationException("Null user name");
        }

        try
        {
            UsernamePasswordAuthenticationToken auth = getAuthenticationToken(userName, providedDetails);
            return setFullAuthentication(auth);
        }
        catch (net.sf.acegisecurity.AuthenticationException ae)
        {
            throw new AuthenticationException(ae.getMessage(), ae);
        }
    }

    /**
     * Re-authenticate using a previously-created authentication.
     */
    public static Authentication setFullAuthentication(Authentication authentication)
    {
        if (authentication == null)
        {
            clearCurrentSecurityContext();
            return null;
        }
        else
        {
            Context context = ContextHolder.getContext();
            AlfrescoSecureContext sc = null;
            if ((context == null) || !(context instanceof AlfrescoSecureContext))
            {
                sc = new AlfrescoSecureContextImpl();
                ContextHolder.setContext(sc);
            }
            else
            {
                sc = (AlfrescoSecureContext) context;
            }
            authentication.setAuthenticated(true);
            // Sets real and effective
            sc.setRealAuthentication(authentication);
            sc.setEffectiveAuthentication(authentication);
            return authentication;
        }
    }
    
    /**
     * <b>WARN: Advanced usage only.</b><br/>
     * Set the system user as the currently running user for authentication purposes.
     * 
     * @return Authentication
     * 
     * @see #setRunAsUser(String)
     */
    public static Authentication setRunAsUserSystem()
    {
        return setRunAsUser(SYSTEM_USER_NAME);
    }

    /**
     * <b>WARN: Advanced usage only.</b><br/>
     * Switch to the given user for all authenticated operations.  The original, authenticated user
     * can still be found using {@link #getAuthenticatedUser()}.
     * 
     * @param userName          the user to run as
     * @return                  the new authentication
     */
    public static Authentication setRunAsUser(String userName)
    {
        return setRunAsUser(userName, getDefaultUserDetails(userName));
    }
    
    /*package*/ static Authentication setRunAsUser(String userName, UserDetails providedDetails) throws AuthenticationException
    {
        if (userName == null)
        {
            throw new AuthenticationException("Null user name");
        }

        try
        {
            UsernamePasswordAuthenticationToken auth = getAuthenticationToken(userName, providedDetails);
            return setRunAsAuthentication(auth);
        }
        catch (net.sf.acegisecurity.AuthenticationException ae)
        {
            throw new AuthenticationException(ae.getMessage(), ae);
        }
    }

    /*package*/ static Authentication setRunAsAuthentication(Authentication authentication)
    {
        if (authentication == null)
        {
            clearCurrentSecurityContext();
            return null;
        }
        else
        {
            Context context = ContextHolder.getContext();
            AlfrescoSecureContext sc = null;
            if ((context == null) || !(context instanceof AlfrescoSecureContext))
            {
                sc = new AlfrescoSecureContextImpl();
                ContextHolder.setContext(sc);
            }
            else
            {
                sc = (AlfrescoSecureContext) context;
            }
            authentication.setAuthenticated(true);
            if (sc.getRealAuthentication() == null)
            {
                // There is no authentication in action
                sc.setRealAuthentication(authentication);
            }
            sc.setEffectiveAuthentication(authentication);
            return authentication;
        }
    }
    
    /**
     * Get the current authentication for application of permissions.  This includes
     * the any overlay details set by {@link #setRunAsUser(String)}.
     * 
     * @return Authentication               Returns the running authentication
     * @throws AuthenticationException
     */
    public static Authentication getRunAsAuthentication() throws AuthenticationException
    {
        Context context = ContextHolder.getContext();
        if ((context == null) || !(context instanceof AlfrescoSecureContext))
        {
            return null;
        }
        return ((AlfrescoSecureContext) context).getEffectiveAuthentication();
    }
    
    /**
     * <b>WARN: Advanced usage only.</b><br/>
     * Get the authentication for that was set by an real authentication.
     * 
     * @return Authentication               Returns the real authentication
     * @throws AuthenticationException
     */
    public static Authentication getFullAuthentication() throws AuthenticationException
    {
        Context context = ContextHolder.getContext();
        if ((context == null) || !(context instanceof AlfrescoSecureContext))
        {
            return null;
        }
        return ((AlfrescoSecureContext) context).getRealAuthentication();
    }
    
    /**
     * Get the user that is currently in effect for purposes of authentication.  This includes
     * any overlays introduced by {@link #setRunAsUser(String) runAs}.
     * 
     * @return              Returns the name of the user
     * @throws AuthenticationException
     */
    public static String getRunAsUser() throws AuthenticationException
    {
        Context context = ContextHolder.getContext();
        if ((context == null) || !(context instanceof AlfrescoSecureContext))
        {
            return null;
        }
        AlfrescoSecureContext ctx = (AlfrescoSecureContext) context;
        if (ctx.getEffectiveAuthentication() == null)
        {
            return null;
        }
        return getUserName(ctx.getEffectiveAuthentication());
    }
    
    public static boolean isRunAsUserTheSystemUser()
    {
        String runAsUser = getRunAsUser();
        if ((runAsUser != null) && isMtEnabled())
        {
            // get base username
            int idx = runAsUser.indexOf(TenantService.SEPARATOR);
            if (idx != -1)
            {
                runAsUser = runAsUser.substring(0, idx);
            }
        }
        return EqualsHelper.nullSafeEquals(runAsUser, AuthenticationUtil.SYSTEM_USER_NAME);
    }
    
    /**
     * Get the fully authenticated user. 
     * It returns the name of the user that last authenticated and excludes any overlay authentication set
     * by {@link #runAs(org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork, String) runAs}.
     * 
     * @return              Returns the name of the authenticated user
     * @throws AuthenticationException
     */
    public static String getFullyAuthenticatedUser() throws AuthenticationException
    {
        Context context = ContextHolder.getContext();
        if ((context == null) || !(context instanceof AlfrescoSecureContext))
        {
            return null;
        }
        AlfrescoSecureContext ctx = (AlfrescoSecureContext) context;
        if (ctx.getRealAuthentication() == null)
        {
            return null;
        }
        return getUserName(ctx.getRealAuthentication());
    }
    
    /**
     * Get the name of the system user
     * 
     * @return String
     */
    public static String getSystemUserName()
    {
        return SYSTEM_USER_NAME;
    }

    /**
     * Get the name of the Guest User
     */
    public static String getGuestUserName()
    {
        return PermissionService.GUEST_AUTHORITY.toLowerCase();
    }

    /**
     * Remove the current security information
     */
    public static void clearCurrentSecurityContext()
    {
        ContextHolder.setContext(null);
        InMemoryTicketComponentImpl.clearCurrentSecurityContext();
    }

    /**
     * Execute a unit of work as a given user. The thread's authenticated user will be returned to its normal state
     * after the call.
     * 
     * @param runAsWork
     *            the unit of work to do
     * @param uid
     *            the user ID
     * @return Returns the work's return value
     */
    public static <R> R runAs(RunAsWork<R> runAsWork, String uid)
    {
        Authentication originalFullAuthentication = AuthenticationUtil.getFullAuthentication();
        Authentication originalRunAsAuthentication = AuthenticationUtil.getRunAsAuthentication();
        
        final R result;
        try
        {
            if (originalFullAuthentication == null)
            {
                AuthenticationUtil.setFullyAuthenticatedUser(uid);
            }
            else
            {
                if ((originalRunAsAuthentication != null) && (isMtEnabled()))
                {
                    String originalRunAsUserName = getUserName(originalRunAsAuthentication);
                    int idx = originalRunAsUserName.indexOf(TenantService.SEPARATOR);
                    if ((idx != -1) && (idx < (originalRunAsUserName.length() - 1)))
                    {
                        if (uid.equals(AuthenticationUtil.getSystemUserName()))
                        {
                            uid = uid + TenantService.SEPARATOR + originalRunAsUserName.substring(idx + 1);
                        }
                    }
                }
                AuthenticationUtil.setRunAsUser(uid);
            }
            result = runAsWork.doWork();
            return result;
        }
        catch (Throwable exception)
        {
            // Re-throw the exception
            if (exception instanceof RuntimeException)
            {
                throw (RuntimeException) exception;
            }
            else
            {
                throw new RuntimeException("Error during run as.", exception);
            }
        }
        finally
        {
            if (originalFullAuthentication == null)
            {
                AuthenticationUtil.clearCurrentSecurityContext();
            }
            else
            {
                AuthenticationUtil.setFullAuthentication(originalFullAuthentication);
                AuthenticationUtil.setRunAsAuthentication(originalRunAsAuthentication);
            }   
        }
    }
    
    static class ThreadLocalStack extends ThreadLocal<Stack<Authentication>> {

        /* (non-Javadoc)
         * @see java.lang.ThreadLocal#initialValue()
         */
        @Override
        protected Stack<Authentication> initialValue()
        {
            return new Stack<Authentication>();
        }
        
    }    
    private static ThreadLocal<Stack<Authentication>> threadLocalFullAuthenticationStack = new ThreadLocalStack();
    private static ThreadLocal<Stack<Authentication>> threadLocalRunAsAuthenticationStack = new ThreadLocalStack();
    
    /**
     * Push the current authentication context onto a threadlocal stack.
     */
    public static void pushAuthentication()
    {
        Authentication originalFullAuthentication = AuthenticationUtil.getFullAuthentication();
        Authentication originalRunAsAuthentication = AuthenticationUtil.getRunAsAuthentication();
        threadLocalFullAuthenticationStack.get().push(originalFullAuthentication);
        threadLocalRunAsAuthenticationStack.get().push(originalRunAsAuthentication);
    }
    
    /**
     * Pop the authentication context from a threadlocal stack.
     */
    public static void popAuthentication()
    {
        Authentication originalFullAuthentication = threadLocalFullAuthenticationStack.get().pop();
        Authentication originalRunAsAuthentication = threadLocalRunAsAuthenticationStack.get().pop();
        if (originalFullAuthentication == null)
        {
            AuthenticationUtil.clearCurrentSecurityContext();
        }
        else
        {
            AuthenticationUtil.setFullAuthentication(originalFullAuthentication);
            AuthenticationUtil.setRunAsAuthentication(originalRunAsAuthentication);
        }
    }

    /**
     * Logs the current authenticated users
     */
    public static void logAuthenticatedUsers()
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug(
                    "Authentication: \n" +
                    "   Fully authenticated: " + AuthenticationUtil.getFullyAuthenticatedUser() + "\n" +
                    "   Run as:              " + AuthenticationUtil.getRunAsUser());
        }
    }

    public static void logNDC(String userName)
    {
        NDC.remove();

        if (isMtEnabled())
        {
            int idx = userName.indexOf(TenantService.SEPARATOR);
            if ((idx != -1) && (idx < (userName.length() - 1)))
            {
                NDC.push("Tenant:" + userName.substring(idx + 1) + " User:" + userName.substring(0, idx));
            }
            else
            {
                NDC.push("User:" + userName);
            }
        }
        else
        {
            NDC.push("User:" + userName);
        }
    }
}
