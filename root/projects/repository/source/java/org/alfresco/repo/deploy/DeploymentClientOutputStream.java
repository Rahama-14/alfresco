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
 * http://www.alfresco.com/legal/licensing
 */

package org.alfresco.repo.deploy;

import java.io.IOException;
import java.io.OutputStream;

import org.alfresco.deployment.DeploymentReceiverTransport;

/**
 * OutputStream used by client side to talk to 
 * the deployment receiver.
 * @author britt
 */
public class DeploymentClientOutputStream extends OutputStream
{
    private DeploymentReceiverTransport fTransport;
    
    private String fTicket;
    
    private String fOutputToken;
    
    /**
     * Make one up.
     * @param transport
     * @param ticket
     * @param outputToken
     */
    public DeploymentClientOutputStream(DeploymentReceiverTransport transport,
                                        String ticket,
                                        String outputToken)
    {
        fTransport = transport;
        fTicket = ticket;
        fOutputToken = outputToken;
    }
    
    /* (non-Javadoc)
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int b) throws IOException
    {
        byte[] buff = new byte[1];
        buff[0] = (byte)b;
        write(buff);
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException
    {
        // NO OP
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#flush()
     */
    @Override
    public void flush() throws IOException
    {
        // NO OP
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        fTransport.write(fTicket, fOutputToken, b, off, len);
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(byte[] b) throws IOException
    {
        write(b, 0, b.length);
    }
    
    /**
     * Get the deployment ticket.
     * @return
     */
    public String getTicket()
    {
        return fTicket;
    }
    
    /**
     * Get the output token.
     * @return
     */
    public String getOutputToken()
    {
        return fOutputToken;
    }
}
