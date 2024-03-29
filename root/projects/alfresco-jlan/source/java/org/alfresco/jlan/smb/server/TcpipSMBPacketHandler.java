/*
 * Copyright (C) 2006-2010 Alfresco Software Limited.
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

package org.alfresco.jlan.smb.server;

import java.io.IOException;
import java.net.Socket;

import org.alfresco.jlan.netbios.RFCNetBIOSProtocol;
import org.alfresco.jlan.util.DataPacker;

/**
 * Tcpip SMB Packet Handler Class
 * 
 * @author gkspencer
 */
public class TcpipSMBPacketHandler extends SocketPacketHandler {

	// Buffer to read the request header
	
	private byte[] m_headerBuf = new byte[4];
	
	/**
	 * Class constructor
	 * 
	 * @param sock Socket
	 * @param packetPool CIFSPacketPool
	 * @exception IOException If a network error occurs
	 */
	public TcpipSMBPacketHandler(Socket sock, CIFSPacketPool packetPool) throws IOException {
		super(sock, SMBSrvPacket.PROTOCOL_TCPIP, "TCP-SMB", "T", packetPool);
	}

	/**
	 * Read a packet from the input stream
	 * 
	 * @return SMBSrvPacket
	 * @exception IOexception If a network error occurs
	 */
	public SMBSrvPacket readPacket()
		throws IOException {

		// Read the packet header

		int len = readBytes( m_headerBuf, 0, 4);

		// Check if the connection has been closed, read length equals -1

		if ( len == -1)
			throw new IOException("Connection closed (header read)");

		// Check if we received a valid header

		if ( len < RFCNetBIOSProtocol.HEADER_LEN)
			throw new IOException("Invalid header, len=" + len);

		// Get the packet type from the header

//		int typ = (int) ( m_headerBuf[0] & 0xFF);
		int dlen = (int) DataPacker.getShort( m_headerBuf, 2);

		// Check for a large packet, add to the data length

		if ( m_headerBuf[1] != 0) {
			int llen = (int) m_headerBuf[1];
			dlen += (llen << 16);
		}

		// Get a packet from the pool to hold the request data, allow for the NetBIOS header length
		// so that the CIFS request lines up with other implementations.
		
		SMBSrvPacket pkt = getPacketPool().allocatePacket( dlen + RFCNetBIOSProtocol.HEADER_LEN);
		
		// Read the data part of the packet into the users buffer, this may take
		// several reads

		int offset = RFCNetBIOSProtocol.HEADER_LEN;
		int totlen = offset;

		try {
			
			while (dlen > 0) {
	
				// Read the data
	
				len = readBytes( pkt.getBuffer(), offset, dlen);
	
				// Check if the connection has been closed
	
				if ( len == -1)
					throw new IOException("Connection closed (request read)");
	
				// Update the received length and remaining data length
	
				totlen += len;
				dlen -= len;
	
				// Update the user buffer offset as more reads will be required
				// to complete the data read
	
				offset += len;
	
			}
		}
		catch (IOException ex) {
			
			// Release the packet back to the pool
			
			getPacketPool().releasePacket( pkt);
			
			// Rethrow the exception
			
			throw ex;
		}

		// Set the received request length
		
		pkt.setReceivedLength( totlen);
		
		// Return the received packet

		return pkt;
	}

	/**
	 * Send a packet to the output stream
	 * 
	 * @param pkt SMBSrvPacket
	 * @param len int
	 * @param writeRaw boolean
	 * @exception IOexception If a network error occurs
	 */
	public void writePacket(SMBSrvPacket pkt, int len, boolean writeRaw)
		throws IOException {

		// Fill in the TCP SMB message header, this is already allocated as
		// part of the users buffer.

		byte[] buf = pkt.getBuffer();
		DataPacker.putInt(len, buf, 0);

		// Output the data packet

		int bufSiz = len + RFCNetBIOSProtocol.HEADER_LEN;
		writeBytes(buf, 0, bufSiz);
	}
}
