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
package org.alfresco.web.scripts;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.scripts.Description.FormatStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Basic Implementation of a Web Script Request
 * 
 * @author davidc
 */
public abstract class WebScriptRequestImpl implements WebScriptRequest
{
    protected static final Log logger = LogFactory.getLog(WebScriptRequestImpl.class);
    private Runtime runtime;

    // parsed request content
    private Object requestContent = this;


    /**
     * Construct
     * 
     * @param runtime
     */
    public WebScriptRequestImpl(Runtime runtime)
    {
        this.runtime = runtime;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebScriptRequest#getRuntime()
     */
    public Runtime getRuntime()
    {
        return runtime;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebScriptRequest#getExtensionPath()
     */
    public String getExtensionPath()
    {
        String extensionPath = "";
        Match match = getServiceMatch();
        if (match != null)
        {
            String matchPath = getServiceMatch().getPath();
            String pathInfo = getPathInfo();
            if (pathInfo != null)
            {
                extensionPath = pathInfo.substring(matchPath.length());
            }
        }
        return extensionPath;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebScriptRequest#isGuest()
     */
    public boolean isGuest()
    {
        return Boolean.valueOf(getParameter("guest"));
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebScriptRequest#getFormat()
     */
    public String getFormat()
    {
        String format = null;
        Match match = getServiceMatch();
        if (match != null)
        {
            Description desc = match.getWebScript().getDescription();
            FormatStyle style = desc.getFormatStyle();
            
            // extract format from extension
            if (style == FormatStyle.extension || style == FormatStyle.any)
            {
                String pathInfo = getPathInfo();
                if (pathInfo != null)
                {
                    int extIdx = pathInfo.lastIndexOf('.');
                    if (extIdx != -1)
                    {
                        format = pathInfo.substring(extIdx +1);
                    }
                }
            }
            
            // extract format from argument
            if (style == FormatStyle.argument || style == FormatStyle.any)
            {
                String argFormat = getParameter("format");
                if (argFormat != null)
                {
                    if (format != null && format.length() > 0)
                    {
                        throw new WebScriptException("Format specified both in extension and format argument");
                    }
                    format = argFormat;
                }
            }
            
            // negotiate format, if necessary
            if (format == null || format.length() == 0)
            {
                String accept = getHeader("Accept");
                NegotiatedFormat[] negotiatedFormats = desc.getNegotiatedFormats();
                if (accept != null && negotiatedFormats != null)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Negotiating format for " + accept);
                        
                    format = NegotiatedFormat.negotiateFormat(accept, negotiatedFormats);
                    if (format == null)
                    {
                        throw new WebScriptException(HttpServletResponse.SC_NOT_ACCEPTABLE, "Cannot negotiate appropriate response format for Accept: " + accept);
                    }
                }
            }
            
            // fallback to default
            if (format == null || format.length() == 0)
            {
                format = desc.getDefaultFormat();
            }
        }
        
        return (format == null || format.length() == 0) ? "" : format;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebScriptRequest#getFormatStyle()
     */
    public FormatStyle getFormatStyle()
    {
        Match match = getServiceMatch();
        if (match == null)
        {
            return FormatStyle.any;
        }
        FormatStyle style = match.getWebScript().getDescription().getFormatStyle();
        if (style != FormatStyle.any)
        {
            return style;
        }
        else
        {
            String argFormat = getParameter("format");
            if (argFormat != null && argFormat.length() > 0)
            {
                return FormatStyle.argument;
            }
            else
            {
                return FormatStyle.extension;
            }
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebScriptRequest#getJSONCallback()
     */
    public String getJSONCallback()
    {
        return getParameter("alf_callback");
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebScriptRequest#forceSuccessStatus()
     */
    public boolean forceSuccessStatus()
    {
        return false;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebScriptRequest#getContentType()
     */
    public String getContentType()
    {
        String contentType = getHeader("Content-Type");
        if (contentType != null && contentType.startsWith("multipart/form-data"))
        {
            contentType = "multipart/form-data";
        }
        return contentType;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebScriptRequest#parseContent()
     */
    public Object parseContent()
    {
        if (requestContent == this)
        {
            requestContent = null;
            String contentType = getContentType();
            if (contentType != null && contentType.length() > 0)
            {
                FormatRegistry formatRegistry = getRuntime().getContainer().getFormatRegistry();
                FormatReader<Object> reader = formatRegistry.getReader(contentType);
                if (reader != null)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Converting request (mimetype: " + contentType + ") to " + reader.getDestinationClass().getName());
                    
                    requestContent = reader.read(this);
                }
            }
        }
        return requestContent;
    }
    
}
