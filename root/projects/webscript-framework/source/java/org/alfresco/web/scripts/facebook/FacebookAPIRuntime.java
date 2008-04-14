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
package org.alfresco.web.scripts.facebook;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.config.ServerProperties;
import org.alfresco.web.scripts.Match;
import org.alfresco.web.scripts.RuntimeContainer;
import org.alfresco.web.scripts.WebScriptRequest;
import org.alfresco.web.scripts.servlet.ServletAuthenticatorFactory;
import org.alfresco.web.scripts.servlet.WebScriptServletRuntime;


/**
 * Runtime to support requests from Facebook
 * 
 * @author davidc
 */
public class FacebookAPIRuntime extends WebScriptServletRuntime
{

    /**
     * Construct
     * 
     * @param container
     * @param authFactory
     * @param req
     * @param res
     * @param serverProperties
     */
    public FacebookAPIRuntime(RuntimeContainer container, ServletAuthenticatorFactory authFactory, HttpServletRequest req, HttpServletResponse res, ServerProperties serverProperties)
    {
        super(container, authFactory, req, res, serverProperties);
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.servlet.WebScriptServletRuntime#createRequest(org.alfresco.web.scripts.Match)
     */
    @Override
    protected WebScriptRequest createRequest(Match match)
    {
        servletReq = new FacebookServletRequest(this, req, match, serverProperties, getScriptUrl());
        return servletReq;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.servlet.WebScriptServletRuntime#getScriptParameters()
     */
    @Override
    public Map<String, Object> getScriptParameters()
    {
        Map<String, Object> model = new HashMap<String, Object>();
        model.putAll(super.getScriptParameters());
        model.put("facebook", new FacebookModel((FacebookServletRequest)servletReq));
        return model;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.AbstractRuntime#getTemplateParameters()
     */
    @Override
    public Map<String, Object> getTemplateParameters()
    {
        Map<String, Object> model = new HashMap<String, Object>();
        model.putAll(super.getTemplateParameters());
        model.put("facebook", new FacebookModel((FacebookServletRequest)servletReq));
        return model;
    }

}
