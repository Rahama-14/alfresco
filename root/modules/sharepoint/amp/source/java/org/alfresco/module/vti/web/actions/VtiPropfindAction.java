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
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.vti.web.actions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.module.vti.handler.alfresco.VtiPathHelper;
import org.alfresco.module.vti.web.VtiAction;
import org.alfresco.module.vti.web.fp.PropfindMethod;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVHelper;
import org.alfresco.repo.webdav.WebDAVMethod;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
* <p>VtiPropfindAction is processor of WebDAV protocol. It provides 
* the back-end controller for dispatching among set of WebDAVMethods. 
* It selects and invokes a realization of {@link WebDAVMethod}
* to perform the requested method of WebDAV protocol.</p>
*
* @author PavelYur
*
*/
public class VtiPropfindAction implements VtiAction
{
    private static final long serialVersionUID = 8916126506309290108L;

    private VtiPathHelper pathHelper;

    private WebDAVHelper webDavHelper;

    private ServiceRegistry serviceRegistry;

    private AuthenticationService authenticationService;

    private static Log logger = LogFactory.getLog(VtiPropfindAction.class);

    /**
     * <p>Process WebDAV protocol request, dispatch among set of 
     * WebDAVMethods, selects and invokes a realization of {@link WebDAVMethod}
     * to perform the requested method of WebDAV protocol.</p> 
     *
     * @param request HTTP request
     * @param response HTTP response
     */
    public void execute(HttpServletRequest request, HttpServletResponse response)
    {
        if (webDavHelper == null)
        {
            webDavHelper = new VtiWebDavHelper(serviceRegistry, authenticationService);
        }

        if (WebDAV.METHOD_PROPFIND.equals(request.getMethod()))
        {
            WebDAVMethod method = new PropfindMethod(pathHelper.getAlfrescoContext());
            method.setDetails(request, response, webDavHelper, pathHelper.getRootNodeRef());
            try
            {
                method.execute();
            }
            catch (WebDAVServerException e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Exception while executing WebDAV method");
                }
            }
        }
    }

    /**
     * <p>VtiPathHelper setter.</p>
     *
     * @param pathHelper {@link VtiPathHelper}.    
     */
    public void setPathHelper(VtiPathHelper pathHelper)
    {
        this.pathHelper = pathHelper;
    }

    /**
     * <p>ServiceRegistry setter.</p>
     *
     * @param serviceRegistry {@link ServiceRegistry}.    
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }
    
    /**
     * <p>AuthenticationService setter.</p>
     *
     * @param authenticationService {@link AuthenticationService}.    
     */
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }

    private class VtiWebDavHelper extends WebDAVHelper
    {
        public VtiWebDavHelper(ServiceRegistry serviceRegistry, AuthenticationService authenticationService)
        {
            super(serviceRegistry, authenticationService);
        }
    };

}


