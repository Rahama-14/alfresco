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
package org.alfresco.jlan.smb.server;

import org.alfresco.jlan.server.thread.ThreadRequest;

/**
 * CIFS Thread Request Class
 * 
 * <p>Holds the details of a CIFS request for processing by a thread pool.
 * 
 * @author gkspencer
 */
public class CIFSThreadRequest implements ThreadRequest {

	// CIFS session and request packet
	
	private SMBSrvSession m_sess;
	private SMBSrvPacket m_smbPkt;
	
	/**
	 * Class constructor
	 * 
	 * @param sess SMBSrvSession
	 * @param smbPkt SMBSrvPacket
	 */
	public CIFSThreadRequest( SMBSrvSession sess, SMBSrvPacket smbPkt) {
		m_sess   = sess;
		m_smbPkt = smbPkt;
	}
	
	/**
	 * Run the CIFS request
	 */
	public void runRequest() {
		
		// Check if the session is still alive
		
		if ( m_sess.isShutdown() == false) {
			
			// Process the CIFS request
			
			m_sess.processPacket( m_smbPkt);
		}
		else {
			
			// Release the request back to the pool
			
			m_sess.getPacketPool().releasePacket( m_smbPkt);
		}
	}
	
	/**
	 * Return the CIFS request details as a string
	 * 
	 * @reurun String
	 */
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		str.append("[CIFS Sess=");
		str.append( m_sess.getUniqueId());
		str.append(", pkt=");
		str.append( m_smbPkt.toString());
		str.append("]");
		
		return str.toString();
	}
}
