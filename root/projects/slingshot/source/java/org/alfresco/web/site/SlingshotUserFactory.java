/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.web.site;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.extensions.surf.FrameworkUtil;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.exception.ConnectorServiceException;
import org.springframework.extensions.surf.exception.UserFactoryException;
import org.springframework.extensions.surf.site.AlfrescoUser;
import org.springframework.extensions.surf.site.AuthenticationUtil;
import org.springframework.extensions.surf.support.AlfrescoUserFactory;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.surf.util.StringBuilderWriter;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.connector.Connector;
import org.springframework.extensions.webscripts.connector.ConnectorContext;
import org.springframework.extensions.webscripts.connector.HttpMethod;
import org.springframework.extensions.webscripts.connector.Response;
import org.springframework.extensions.webscripts.json.JSONWriter;

/**
 * Slingshot User Factory makes use of the slingshot REST API to persist
 * modified user details back to the repo.  
 * 
 * @author kevinr
 */
public class SlingshotUserFactory extends AlfrescoUserFactory
{
    /**
     * @see org.alfresco.web.site.UserFactory#authenticate(org.alfresco.web.site.RequestContext, javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.String)
     */
    @Override
    public boolean authenticate(HttpServletRequest request, String username, String password)
    {
        // special case to disallow Guest user authentication in Share
        // TODO: add basic Guest user support
        boolean authenticated = false;
        if (!AuthenticationUtil.isGuest(username))
        {
            authenticated = super.authenticate(request, username, password);
        }
        return authenticated;
    }

    
    /**
     * @see org.alfresco.web.site.AlfrescoUserFactory#constructUser(java.lang.String, Map<String, Boolean>)
     */
    protected AlfrescoUser constructUser(String userId, Map<String, Boolean> capabilities)
    {
        return new SlingshotUser(userId, capabilities);
    }

    /**
     * Persist the user back to the Alfresco repository
     * 
     * @param user  to persist
     * 
     * @throws IOException
     */
    public void saveUser(AlfrescoUser user) throws UserFactoryException
    {
        RequestContext context = (RequestContext)ThreadLocalRequestContext.getRequestContext();
        if (!context.getUserId().equals(user.getId()))
        {
            throw new UserFactoryException("Unable to persist user with different Id that current Id.");
        }
        
        StringBuilderWriter buf = new StringBuilderWriter(512);
        JSONWriter writer = new JSONWriter(buf);
        
        try
        {
            writer.startObject();
            
            writer.writeValue("username", user.getId());
            
            writer.startValue("properties");
            writer.startObject();
            writer.writeValue(CM_FIRSTNAME, user.getFirstName());
            writer.writeValue(CM_LASTNAME, user.getLastName());
            writer.writeValue(CM_JOBTITLE, user.getJobTitle());
            writer.writeValue(CM_ORGANIZATION, user.getOrganization());
            writer.writeValue(CM_LOCATION, user.getLocation());
            writer.writeValue(CM_EMAIL, user.getEmail());
            writer.writeValue(CM_TELEPHONE, user.getTelephone());
            writer.writeValue(CM_MOBILE, user.getMobilePhone());
            writer.writeValue(CM_SKYPE, user.getSkype());
            writer.writeValue(CM_INSTANTMSG, user.getInstantMsg());
            writer.writeValue(CM_COMPANYADDRESS1, user.getCompanyAddress1());
            writer.writeValue(CM_COMPANYADDRESS2, user.getCompanyAddress2());
            writer.writeValue(CM_COMPANYADDRESS3, user.getCompanyAddress3());
            writer.writeValue(CM_COMPANYPOSTCODE, user.getCompanyPostcode());
            writer.writeValue(CM_COMPANYFAX, user.getCompanyFax());
            writer.writeValue(CM_COMPANYEMAIL, user.getCompanyEmail());
            writer.writeValue(CM_COMPANYTELEPHONE, user.getCompanyTelephone());
            writer.endObject();
            writer.endValue();
            
            writer.startValue("content");
            writer.startObject();
            writer.writeValue(CM_PERSONDESCRIPTION, user.getBiography());
            writer.endObject();
            writer.endValue();
            
            writer.endObject();
            
            Connector conn = FrameworkUtil.getConnector(context, ALFRESCO_ENDPOINT_ID);
            ConnectorContext c = new ConnectorContext(HttpMethod.POST);
            c.setContentType("application/json");
            Response res = conn.call("/slingshot/profile/userprofile", c,
                    new ByteArrayInputStream(buf.toString().getBytes()));
            if (Status.STATUS_OK != res.getStatus().getCode())
            {
                throw new UserFactoryException("Remote error during User save: " + res.getStatus().getMessage());
            }
        }
        catch (IOException ioErr)
        {
            throw new UserFactoryException("IO error during User save: " + ioErr.getMessage(), ioErr);
        }
        catch (ConnectorServiceException cse)
        {
            throw new UserFactoryException("Configuration error during User save: " + cse.getMessage(), cse);
        }
    }
}
