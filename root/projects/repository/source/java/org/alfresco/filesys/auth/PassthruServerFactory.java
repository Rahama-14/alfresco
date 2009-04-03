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
package org.alfresco.filesys.auth;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.jlan.server.auth.passthru.AuthSessionFactory;
import org.alfresco.jlan.server.auth.passthru.PassthruServers;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.smb.Protocol;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * A Factory for {@link PassthruServers} objects, allowing setting of the server list via local server, individual
 * servers or domain name.
 * 
 * @author dward
 */
public class PassthruServerFactory implements FactoryBean, InitializingBean, DisposableBean
{
    private static final Log logger = LogFactory.getLog("org.alfresco.smb.protocol.auth");

    public final static int DefaultSessionTmo = 5000; // 5 seconds
    public final static int MinSessionTmo = 2000; // 2 seconds
    public final static int MaxSessionTmo = 30000; // 30 seconds

    public final static int MinCheckInterval = 10; // 10 seconds
    public final static int MaxCheckInterval = 15 * 60; // 15 minutes

    private Integer timeout;

    private boolean localServer;

    private String server;

    private String domain;

    private Integer offlineCheckInterval;

    private PassthruServers passthruServers;

    /**
     * Sets the timeout for opening a session to an authentication server
     * 
     * @param timeout
     *            a time period in milliseconds
     */
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    /**
     * Indicates whether the local server should be used as the authentication server
     * 
     * @param localServer
     *            <code>true</code> if the local server should be used as the authentication server
     */
    public void setLocalServer(boolean localServer)
    {
        this.localServer = localServer;
    }

    /**
     * Sets the server(s) to authenticate against.
     * 
     * @param server
     *            comma-delimited list of server names
     */
    public void setServer(String server)
    {
        this.server = server;
    }

    /**
     * Sets the domain to authenticate against
     * 
     * @param domain
     *            a domain name
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    /**
     * Sets the offline server check interval in seconds
     * 
     * @param offlineCheckInterval
     *            a time interval in seconds
     */
    public void setOfflineCheckInterval(Integer offlineCheckInterval)
    {
        this.offlineCheckInterval = offlineCheckInterval;
    }

    /**
     * Set the protocol order for passthru connections
     * 
     * @param protoOrder
     *            a comma-delimited list containing one or more of "NetBIOS" and "TCPIP" in any order
     */
    public void setProtocolOrder(String protoOrder)
    {
        // Parse the protocol order list

        StringTokenizer tokens = new StringTokenizer(protoOrder, ",");
        int primaryProto = Protocol.None;
        int secondaryProto = Protocol.None;

        // There should only be one or two tokens

        if (tokens.countTokens() > 2)
            throw new AlfrescoRuntimeException("Invalid protocol order list, " + protoOrder);

        // Get the primary protocol

        if (tokens.hasMoreTokens())
        {
            // Parse the primary protocol

            String primaryStr = tokens.nextToken();

            if (primaryStr.equalsIgnoreCase("TCPIP"))
                primaryProto = Protocol.NativeSMB;
            else if (primaryStr.equalsIgnoreCase("NetBIOS"))
                primaryProto = Protocol.TCPNetBIOS;
            else
                throw new AlfrescoRuntimeException("Invalid protocol type, " + primaryStr);

            // Check if there is a secondary protocol, and validate

            if (tokens.hasMoreTokens())
            {
                // Parse the secondary protocol

                String secondaryStr = tokens.nextToken();

                if (secondaryStr.equalsIgnoreCase("TCPIP") && primaryProto != Protocol.NativeSMB)
                    secondaryProto = Protocol.NativeSMB;
                else if (secondaryStr.equalsIgnoreCase("NetBIOS") && primaryProto != Protocol.TCPNetBIOS)
                    secondaryProto = Protocol.TCPNetBIOS;
                else
                    throw new AlfrescoRuntimeException("Invalid secondary protocol, " + secondaryStr);
            }
        }

        // Set the protocol order used for passthru authentication sessions

        AuthSessionFactory.setProtocolOrder(primaryProto, secondaryProto);

        // DEBUG

        if (logger.isDebugEnabled())
            logger.debug("Protocol order primary=" + Protocol.asString(primaryProto) + ", secondary="
                    + Protocol.asString(secondaryProto));
    }

    public void afterPropertiesSet() throws InvalidConfigurationException
    {
        // Check if the offline check interval has been specified
        if (this.offlineCheckInterval != null)
        {
            // Range check the value

            if (this.offlineCheckInterval < MinCheckInterval || this.offlineCheckInterval > MaxCheckInterval)
                throw new InvalidConfigurationException("Invalid offline check interval, valid range is "
                        + MinCheckInterval + " to " + MaxCheckInterval);

            // Set the offline check interval for offline passthru servers

            passthruServers = new PassthruServers(this.offlineCheckInterval);

            // DEBUG

            if (logger.isDebugEnabled())
                logger.debug("Using offline check interval of " + this.offlineCheckInterval + " seconds");
        }
        else
        {
            // Create the passthru server list with the default offline check interval

            passthruServers = new PassthruServers();
        }

        // Propagate the debug setting

        if (logger.isDebugEnabled())
            passthruServers.setDebug(true);

        // Check if the session timeout has been specified

        if (this.timeout != null)
        {

            // Range check the timeout

            if (this.timeout < MinSessionTmo || this.timeout > MaxSessionTmo)
                throw new InvalidConfigurationException("Invalid session timeout, valid range is " + MinSessionTmo
                        + " to " + MaxSessionTmo);

            // Set the session timeout for connecting to an authentication server

            passthruServers.setConnectionTimeout(this.timeout);
        }

        // Check if a server name has been specified

        String srvList = null;
        if (localServer)
        {
            try
            {
                // Get the list of local network addresses

                InetAddress[] localAddrs = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());

                // Build the list of local addresses

                if (localAddrs != null && localAddrs.length > 0)
                {
                    StringBuilder addrStr = new StringBuilder();

                    for (InetAddress curAddr : localAddrs)
                    {
                        if (curAddr.isLoopbackAddress() == false)
                        {
                            addrStr.append(curAddr.getHostAddress());
                            addrStr.append(",");
                        }
                    }

                    if (addrStr.length() > 0)
                        addrStr.setLength(addrStr.length() - 1);

                    // Set the server list using the local address list

                    srvList = addrStr.toString();
                }
                else
                    throw new AlfrescoRuntimeException("No local server address(es)");
            }
            catch (UnknownHostException ex)
            {
                throw new AlfrescoRuntimeException("Failed to get local address list");
            }
        }

        if (this.server != null && this.server.length() > 0)
        {

            // Check if the server name was already set

            if (srvList != null)
                throw new AlfrescoRuntimeException("Set passthru server via local server or specify name");

            // Get the passthru authenticator server name

            srvList = this.server;
        }

        // If the passthru server name has been set initialize the passthru connection

        if (srvList != null)
        {
            // Initialize using a list of server names/addresses

            passthruServers.setServerList(srvList);
        }
        else
        {

            // Get the domain/workgroup name

            String domainName = null;

            // Check if a domain name has been specified

            if (this.domain != null && this.domain.length() > 0)
            {

                // Check if the authentication server has already been set, ie. server name was also specified

                if (srvList != null)
                    throw new AlfrescoRuntimeException("Specify server or domain name for passthru authentication");

                domainName = this.domain;
            }

            // If the domain name has been set initialize the passthru connection

            if (domainName != null)
            {
                try
                {
                    // Initialize using the domain

                    passthruServers.setDomain(domainName);
                }
                catch (IOException ex)
                {
                    throw new AlfrescoRuntimeException("Error setting passthru domain, " + ex.getMessage());
                }
            }
        }

        // Check if we have an authentication server

        if (passthruServers.getTotalServerCount() == 0)
            throw new AlfrescoRuntimeException("No valid authentication servers found for passthru");
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#getObject()
     */
    public Object getObject()
    {
        return passthruServers;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
    public Class<?> getObjectType()
    {
        return PassthruServers.class;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    public boolean isSingleton()
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    public void destroy() throws Exception
    {
        passthruServers.shutdown();
    }
}
