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
package org.alfresco.util.remote.server;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.UUID;

import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.remoting.rmi.RmiServiceExporter;

/**
 * Concrete implementation of a remoting InputStream based on RMI.
 * 
 * @author <a href="mailto:Michael.Shavnev@effective-soft.com">Michael Shavnev</a>
 * @since Alfresco 2.2
 */
public class RmiRemoteInputStreamServer extends AbstractRemoteInputStreamServer
{
    private RmiServiceExporter rmiServiceExporter;

    public RmiRemoteInputStreamServer(InputStream inputStream)
    {
        super(inputStream);
    }

    public String start(String host, int port) throws RemoteException
    {
        String name = inputStream.getClass().getName() + UUID.randomUUID();
        rmiServiceExporter = new RmiServiceExporter();
        rmiServiceExporter.setServiceName(name);
        rmiServiceExporter.setRegistryPort(port);
        rmiServiceExporter.setRegistryHost(host);
        rmiServiceExporter.setServiceInterface(RemoteInputStreamServer.class);
        rmiServiceExporter.setService(this);
        rmiServiceExporter.afterPropertiesSet();
        return name;
    }

    /**
     * Closes the stream and the RMI connection to the peer.
     */
    public void close() throws IOException
    {
        try
        {
            inputStream.close();
        }
        finally
        {
            if (rmiServiceExporter != null)
            {
                try
                {
                    rmiServiceExporter.destroy();
                }
                catch (Throwable e)
                {
                    throw new IOException(e.getMessage());
                }
            }
        }
    }

    /**
     * Utility method to lookup a remote stream peer over RMI.
     */
    public static RemoteInputStreamServer obtain(String host, int port, String name) throws RemoteException
    {
        RmiProxyFactoryBean rmiProxyFactoryBean = new RmiProxyFactoryBean();
        rmiProxyFactoryBean.setServiceUrl("rmi://" + host + ":" + port + "/" + name);
        rmiProxyFactoryBean.setServiceInterface(RemoteInputStreamServer.class);
        rmiProxyFactoryBean.setRefreshStubOnConnectFailure(true);
        try
        {
            rmiProxyFactoryBean.afterPropertiesSet();
        }
        catch (Exception e)
        {
            throw new RemoteException("Error create rmi proxy");
        }
        return (RemoteInputStreamServer) rmiProxyFactoryBean.getObject();
    }
}
