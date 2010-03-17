/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
package org.alfresco.repo.lotus.server;

import javax.servlet.http.HttpServlet;

import org.alfresco.error.AlfrescoRuntimeException;
//import org.alfresco.util.AbstractLifecycleBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationEvent;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;

/**
 * Embedded HTTP server that process all Quickr requests.
 * 
 * @author EugeneZh
 */
public class QuickrServer extends AbstractLifecycleBean
{
    private static Log logger = LogFactory.getLog(QuickrServer.class);

    private Server server;
    private Connector connector;
    private HttpServlet servlet;

    /**
     * Set the HTTP connector
     * 
     * @param connector HTTP Connector
     */
    public void setConnector(Connector connector)
    {
        this.connector = connector;
    }

    /**
     * Set the main QuickrServlet. All the requests will be processed by it.
     * 
     * @param servlet HTTP Servlet
     */
    public void setServlet(HttpServlet servlet)
    {
        this.servlet = servlet;
    }

    /**
     * Method checks that all mandatory fiedls are set.
     * 
     * @throws RuntimeException Exception is thrown if at least one mandatory field isn't set.
     */
    private void check()
    {
        if (servlet == null)
        {
            throw new AlfrescoRuntimeException("Error start QuickrServer, cause: Property 'servlet' not set");
        }

        if (connector == null)
        {
            throw new AlfrescoRuntimeException("Error start QuickrServer, cause: Property 'connector' not set");
        }
    }

    /**
     * Method starts the server.
     * 
     * @see org.alfresco.util.AbstractLifecycleBean#onBootstrap(org.springframework.context.ApplicationEvent)
     */
    @Override
    protected void onBootstrap(ApplicationEvent event)
    {
        check();

        server = new Server();
        server.setStopAtShutdown(true);
        server.setConnectors(new Connector[] { connector });

        Context context = new Context(server, "/", Context.SESSIONS);
        context.addServlet(new ServletHolder(servlet), "/dm/*");

        try
        {
            server.start();

            if (logger.isInfoEnabled())
                logger.info("Quickr server started successfully on port: " + this.connector.getLocalPort());
        }
        catch (Exception e)
        {
            throw new AlfrescoRuntimeException("Error start QuickrServer, cause: ", e);
        }
    }

    /**
     * Method stops the server.
     * 
     * @see org.alfresco.util.AbstractLifecycleBean#onBootstrap(org.springframework.context.ApplicationEvent)
     */
    @Override
    protected void onShutdown(ApplicationEvent event)
    {
        try
        {
            server.stop();
        }
        catch (Exception e)
        {
            throw new AlfrescoRuntimeException("Error stop QuickrServer, cause: ", e);
        }
    }
}
