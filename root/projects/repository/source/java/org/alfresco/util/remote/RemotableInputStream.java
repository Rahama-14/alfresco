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
package org.alfresco.util.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;

import org.alfresco.util.remote.server.RemoteInputStreamServer;
import org.alfresco.util.remote.server.RmiRemoteInputStreamServer;

/**
 * The data consuming side of the remote connection that the <code>InputStream</code> spans.
 * 
 * @author <a href="mailto:Michael.Shavnev@effective-soft.com">Michael Shavnev</a>
 * @since Alfresco 2.2
 */
public class RemotableInputStream extends InputStream implements Serializable
{
    private static final long serialVersionUID = 2434858590717000057L;

    private int port;
    private String host;
    private String name;

    transient private RemoteInputStreamServer inputStreamServer;

    public RemotableInputStream(String host, int port, InputStream inputStream)
    {
        this.host = host;
        this.port = port;
        this.inputStreamServer = new RmiRemoteInputStreamServer(inputStream);
    }

    public void close() throws IOException
    {
        inputStreamServer.close();
    }

    public int read() throws IOException
    {
        return inputStreamServer.read();
    }

    public int read(byte[] bytes) throws IOException
    {
        return inputStreamServer.read(bytes);
    }

    public int read(byte[] bytes, int off, int len) throws IOException
    {
        return inputStreamServer.read(bytes, off, len);
    }

    public long skip(long n) throws IOException
    {
        return inputStreamServer.skip(n);
    }

    public int available() throws IOException
    {
        return inputStreamServer.available();
    }

    public void mark(int readlimit)
    {
        inputStreamServer.mark(readlimit);
    }

    public boolean markSupported()
    {
        return inputStreamServer.markSupported();
    }

    public void reset() throws IOException
    {
        inputStreamServer.reset();
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        name = inputStreamServer.start(host, port);
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        inputStreamServer = (RemoteInputStreamServer) RmiRemoteInputStreamServer.obtain(host, port, name);
    }

    public static void main(String[] args) throws Exception
    {
        RemotableInputStream remotableInputStream = new RemotableInputStream(InetAddress.getLocalHost().getHostName(), 7777, new ByteArrayInputStream("test".getBytes()));

        for (int b = -1; (b = remotableInputStream.read()) != -1;)
        {
            System.out.println((char) b);
        }

        remotableInputStream = new RemotableInputStream(InetAddress.getLocalHost().getHostName(), 7777, new ByteArrayInputStream("test".getBytes()));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(remotableInputStream);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        remotableInputStream = (RemotableInputStream) ois.readObject();

        for (int b = -1; (b = remotableInputStream.read()) != -1;)
        {
            System.out.println((char) b);
        }
        remotableInputStream.close();
    }
}
