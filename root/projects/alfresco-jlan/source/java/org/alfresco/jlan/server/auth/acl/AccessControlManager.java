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

import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.jlan.server.core.SharedDevice;
import org.alfresco.jlan.server.core.SharedDeviceList;
import org.alfresco.config.ConfigElement;


/**
 * Access Control Manager Interface
 * 
 * <p>Used to control access to shared filesystems.
 *
 * @author gkspencer
 */
public interface AccessControlManager {

	/**
	 * Initialize the access control manager
	 * 
	 * @param config ServerConfiguration
	 * @param params ConfigElement
	 * @exception InvalidConfigurationException
	 */
	public void initialize(ServerConfiguration config, ConfigElement params)
		throws InvalidConfigurationException;
		
	/**
	 * Check access to the shared filesystem for the specified session
	 *
	 * @param sess SrvSession
	 * @param share SharedDevice
	 * @return int
	 */
	public int checkAccessControl(SrvSession sess, SharedDevice share);
	
	/**
	 * Filter a shared device list to remove shares that are not visible or the session does
	 * not have access to.
	 *
	 * @param sess SrvSession
	 * @param shares SharedDeviceList
	 * @return SharedDeviceList
	 */
	public SharedDeviceList filterShareList(SrvSession sess, SharedDeviceList shares);
	
	/**
	 * Create an access control
	 * 
	 * @param type String
	 * @param params ConfigElement
	 * @return AccessControl
	 * @exception ACLParseException
	 * @exception InvalidACLTypeException
	 */
	public AccessControl createAccessControl(String type, ConfigElement params)
		throws ACLParseException, InvalidACLTypeException;
		
	/**
	 * Add an access control parser to the list of available access control types.
	 * 
	 * @param parser AccessControlParser
	 */
	public void addAccessControlType(AccessControlParser parser);
}
