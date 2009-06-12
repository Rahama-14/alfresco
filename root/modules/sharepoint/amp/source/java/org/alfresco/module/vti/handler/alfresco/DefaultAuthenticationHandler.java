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
package org.alfresco.module.vti.handler.alfresco;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.module.vti.handler.AuthenticationHandler;
import org.alfresco.module.vti.handler.MethodHandler;
import org.alfresco.module.vti.handler.UserGroupServiceHandler;
import org.alfresco.module.vti.handler.VtiHandlerException;
import org.alfresco.repo.SessionUser;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.web.sharepoint.auth.SiteMemberMapper;
import org.alfresco.web.sharepoint.auth.SiteMemberMappingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Default implementation of web authentication. Delegates to a authentication handler in the core alfresco
 * server authentication subsystem.
 * 
 * @author PavelYur
 */
public class DefaultAuthenticationHandler implements AuthenticationHandler, SiteMemberMapper
{

    private static Log logger = LogFactory.getLog(DefaultAuthenticationHandler.class);
        
    private MethodHandler vtiHandler;
    private UserGroupServiceHandler vtiUserGroupServiceHandler;
    private AuthenticationService authenticationService;
    private org.alfresco.web.sharepoint.auth.AuthenticationHandler delegate;

    public void forceClientToPromptLogonDetails(HttpServletResponse response)
    {
        delegate.forceClientToPromptLogonDetails(response);
    }

    @SuppressWarnings("unchecked")
    public void checkUserTicket(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String alfrescoContext, SessionUser user)
    {
        try
        {
            authenticationService.validate(user.getTicket());

            if (!isSiteMember(httpRequest, alfrescoContext, user.getUserName()))
            {
                Enumeration<String> attributes = httpRequest.getSession().getAttributeNames();
                while (attributes.hasMoreElements())
                {
                    String name = (String) attributes.nextElement();
                    httpRequest.getSession().removeAttribute(name);
                }
                throw new Exception("Not a member!!!!");
            }

            if (logger.isDebugEnabled())
                logger.debug("Ticket was validated");
        }
        catch (Exception ex)
        {
            forceClientToPromptLogonDetails(httpResponse);
            return;
        }           
    }
    
    public boolean isSiteMember(HttpServletRequest request, String alfrescoContext, final String username)
    {
        String uri = request.getRequestURI();

        if (request.getMethod().equalsIgnoreCase("OPTIONS"))
            return true;

        String targetUri = uri.startsWith(alfrescoContext) ? uri.substring(alfrescoContext.length()) : uri;

        if (targetUri.startsWith("/_vti_inf.html") || targetUri.startsWith("/_vti_bin/") || targetUri.startsWith("/resources/"))
            return true;

        String dwsName = null;

        try
        {
            String[] decompsedUrls = vtiHandler.decomposeURL(uri, alfrescoContext);
            dwsName = decompsedUrls[0].substring(decompsedUrls[0].lastIndexOf("/") + 1);
            
            final String buf = dwsName;
            
            RunAsWork<Boolean> isSiteMemberRunAsWork = new RunAsWork<Boolean>()            
            {

                public Boolean doWork() throws Exception
                {                    
                    return vtiUserGroupServiceHandler.isUserMember(buf, username);
                }
                
            };
            
            return AuthenticationUtil.runAs(isSiteMemberRunAsWork, AuthenticationUtil.SYSTEM_USER_NAME).booleanValue();
        }
        catch (Exception e)
        {
            if (dwsName == null)
                throw new SiteMemberMappingException(VtiHandlerException.DOESNOT_EXIST);
            else
                return false;
        }
    }
    
    public SessionUser authenticateRequest(HttpServletRequest request, HttpServletResponse httpResponse,
            String alfrescoContext)
    {
        return delegate.authenticateRequest(request, httpResponse, this, alfrescoContext);
    }
    
    public void setDelegate(org.alfresco.web.sharepoint.auth.AuthenticationHandler delegate)
    {
        this.delegate = delegate;
    }

    public void setVtiHandler(MethodHandler vtiHandler)
    {
        this.vtiHandler = vtiHandler;
    }
    
    public void setVtiUserGroupServiceHandler(UserGroupServiceHandler vtiUserGroupServiceHandler)
    {
        this.vtiUserGroupServiceHandler = vtiUserGroupServiceHandler;
    }
    
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }
}
