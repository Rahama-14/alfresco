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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.studio.servlet;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.site.servlet.BaseServlet;
import org.alfresco.web.studio.OverlayUtil;
import org.alfresco.web.studio.WebStudio;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO: Auto-generated Javadoc
/**
 * Retrieves JavaScript on behalf of the currently logged in user so
 * as to render the in-context displays.
 * 
 * The JavaScript is requested using the following urls:
 * 
 * /_js/static -> retrieves static javascript no-cache headers
 * /_js/dynamic -> retrieves dynamic javascript future expires headers
 * /_js -> retrieves static + dynamic javascript no-cache headers
 * 
 * A static retrieval involves pulling in the content generated by
 * "core.js". If Web Studio has been configured to cache this, it will
 * be stored in a session cache.
 * 
 * A dynamic retrieval involves pulling in the content generated by
 * "web-studio-init.js". This is never cached.
 * 
 * Static Javascript refers to javascript with no runtime dependency.
 * These would include things like MooTools, jQuery and most of the
 * Web Studio Javascript code.
 * 
 * Dynamic Javascript refers to javascript which changes based on the
 * runtime state. Web Studio requires that some javascript change on
 * each request to reflect the current user's authentication state and
 * interesting environmental considerations (such as the source
 * location of the incoming request).
 * 
 * @author muzquiano
 */
public class WebStudioJsServlet extends BaseServlet
{

    /** The Constant REQUEST_DYNAMIC_JS. */
    public static final String REQUEST_DYNAMIC_JS = "dynamic";

    /** The Constant REQUEST_STATIC_JS. */
    public static final String REQUEST_STATIC_JS = "static";

    /** The logger. */
    public static Log logger = LogFactory.getLog(WebStudioJsServlet.class);

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.GenericServlet#init()
     */
    public void init() throws ServletException
    {
        super.init();
    }

    /**
     * Sets the HTTP headers to reflect "dynamic" javascript.
     * 
     * This means that the content should never cache on the browser.
     * 
     * @param response the response
     */
    public void setDynamicHeaders(HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");
    }

    /**
     * Sets the HTTP headers to reflect "static" javascript.
     * 
     * This means that the content should always cache on the browser.
     * 
     * @param response the response
     */
    public void setStaticHeaders(HttpServletResponse response)
    {
        response.setHeader("Expires", "Thu, 15 Apr 2010 20:00:00 GMT");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        long bufferCommitSize = 0;
        long t1 = System.currentTimeMillis();

        String uri = request.getRequestURI();

        // skip server context path and build the path to the resource
        // we are looking for
        uri = uri.substring(request.getContextPath().length());

        // validate and return the resource path - stripping the
        // servlet context
        StringTokenizer t = new StringTokenizer(uri, "/");
        String servletName = t.nextToken();

        boolean dynamics = false;
        boolean statics = false;

        if (t.hasMoreTokens())
        {
            String switcher = (String) t.nextToken();
            if (REQUEST_STATIC_JS.equals(switcher))
            {
                // If we are in developer mode, then we'll return
                // dynamic headers since we want stuff to refresh on
                // every page hit.
                if (WebStudio.getConfig().isDeveloperMode())
                {
                    this.setDynamicHeaders(response);
                }
                else
                {
                    // Otherwise, we'll send static headers.
                    // This will allow the browser to cache the
                    // "heavy"
                    // part of the static JS which will help pages to
                    // render faster for end users
                    this.setStaticHeaders(response);
                }

                statics = true;
            }
            else if (REQUEST_DYNAMIC_JS.equals(switcher))
            {
                this.setDynamicHeaders(response);

                dynamics = true;
            }
        }
        else
        {
            // assume they want both static and dynamic javascript

            this.setDynamicHeaders(response);

            statics = true;
            dynamics = true;
        }

        // handle statics
        if (statics)
        {
            String cacheKey = "JS_" + request.getRequestURI()
                    + request.getQueryString();

            StringBuilder buffer = null;

            // load from cache (if not in developer mode)
            if (!WebStudio.getConfig().isDeveloperMode())
            {
                buffer = OverlayUtil.getCachedResource(request, cacheKey);
            }

            if (buffer == null)
            {
                buffer = new StringBuilder(512000);

                // 
                // By default, include a JSP from disk so that we can
                // at least be pretty flexible
                // about some of the core stuff that gets included
                OverlayUtil.include(request, buffer,
                        "/overlay/default/core.js.jsp");

                // cache back (if not in developer mode)
                if (!WebStudio.getConfig().isDeveloperMode())
                {
                    OverlayUtil.setCachedResource(request, cacheKey, buffer);
                }
            }

            // commit to response
            bufferCommitSize += buffer.length();
            response.getWriter().println(buffer.toString());
        }

        // handle dynamics
        if (dynamics)
        {
            // we want these to load on every request since they
            // contain
            // user state information which can change between
            // requests
            StringBuilder dynamicBuffer = new StringBuilder(65536);
            OverlayUtil.include(request, dynamicBuffer,
                    "/overlay/default/web-studio-init.js.jsp");

            // commit to response
            bufferCommitSize += dynamicBuffer.length();
            response.getWriter().println(dynamicBuffer.toString());
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("URI: " + uri);
            logger.debug("Buffer: " + bufferCommitSize);
            logger.debug("Gentime: " + (System.currentTimeMillis() - t1));
        }
    }
}