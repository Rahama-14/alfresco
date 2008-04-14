/*
 * Copyright (C) 2006-2008 Alfresco Software Limited.
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

package org.alfresco.jlan.server.auth.acl;

import java.net.InetAddress;

import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.core.SharedDevice;
import org.alfresco.jlan.util.IPAddress;

/**
 * Ip Address Access Control Class
 * 
 * <p>Allow/disallow access by checking for a particular TCP/IP address or checking that the address is within
 * a specified subnet.
 *
 * @author gkspencer
 */
public class IpAddressAccessControl extends AccessControl {

	//	Subnet and network mask if the address specifies the subnet

	private String m_subnet;	
	private String m_netMask;
	
	/**
	 * Class constructor
	 *
	 * @param address String
	 * @param mask String
	 * @param type String
	 * @param access int 
	 */	
	protected IpAddressAccessControl(String address, String mask, String type, int access) {
		super(address, type, access);
		
		//	Save the subnet and network mask, if specified

		m_subnet  = address;		
		m_netMask = mask;
		
		//	Change the rule name if a network mask has been specified
		
		if ( m_netMask != null)
			setName(m_subnet + "/" + m_netMask);
	}

	/**
	 * Check if the TCP/IP address matches the specifed address or is within the subnet.
	 * 
	 * @param sess SrvSession
	 * @param share SharedDevice
	 * @param mgr AccessControlManager
	 * @return int
	 */
	public int allowsAccess(SrvSession sess, SharedDevice share, AccessControlManager mgr) {

		//	Check if the remote address is set for the session
		
		InetAddress remoteAddr = sess.getRemoteAddress();
		
		if ( remoteAddr == null)
			return Default;

		//	Get the remote address as a numeric IP address string
		
		String ipAddr = remoteAddr.getHostAddress();
		
		//	Check if the access control is a single TCP/IP address check
		
		int sts = Default;
		
		if ( m_netMask == null) {
			
			//	Check if the TCP/IP address matches the check address
			
			if ( IPAddress.parseNumericAddress(ipAddr) == IPAddress.parseNumericAddress(getName()))
				sts = getAccess();			
		}
		else {
			
			//	Check if the address is within the subnet range
			
			if ( IPAddress.isInSubnet(ipAddr, m_subnet, m_netMask) == true)
				sts = getAccess();
		}
		
		//	Return the access status
		
		return sts;
	}
}
