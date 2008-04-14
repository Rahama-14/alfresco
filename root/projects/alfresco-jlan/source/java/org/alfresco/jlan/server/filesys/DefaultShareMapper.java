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

package org.alfresco.jlan.server.filesys;

import java.util.Enumeration;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.auth.InvalidUserException;
import org.alfresco.jlan.server.config.ConfigId;
import org.alfresco.jlan.server.config.ConfigurationListener;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.jlan.server.core.ShareMapper;
import org.alfresco.jlan.server.core.SharedDevice;
import org.alfresco.jlan.server.core.SharedDeviceList;
import org.alfresco.config.ConfigElement;

/**
 * Default Share Mapper Class
 * 
 * <p>Maps disk and print share lookup requests to the list of shares defined in the server configuration.
 *
 * @author gkspencer
 */
public class DefaultShareMapper implements ShareMapper, ConfigurationListener {

	//	Server configuration and configuration sections
	
	private ServerConfiguration m_config;
	private FilesystemsConfigSection m_filesysConfig;
  
	//	Debug enable flag
	
	private boolean m_debug;
	
	/**
	 * Default constructor
	 */
	public DefaultShareMapper() {
	}
	
	/**
	 * Initialize the share mapper
	 * 
	 * @param config ServerConfiguration
	 * @param params ConfigElement
	 * @exception InvalidConfigurationException
	 */
	public void initializeMapper(ServerConfiguration config, ConfigElement params)
		throws InvalidConfigurationException {
		
		//	Save the server configuration
		
		m_config = config;

		//  Filesystem configuration will usually be initialized after the security configuration so we need to plug in
		//  a listener to initialize it later
    
		m_filesysConfig  = (FilesystemsConfigSection) m_config.getConfigSection( FilesystemsConfigSection.SectionName);
		if ( m_filesysConfig == null)
			m_config.addListener( this);
    
		//	Check if debug is enabled
		
		if ( params.getChild("debug") != null)
			m_debug = true;
	}

	/**
	 * Check if debug output is enabled
	 * 
	 * @return boolean
	 */
	public final boolean hasDebug() {
		return m_debug;
	}
	
	/**
	 * Find a share using the name and type for the specified client.
	 * 
	 * @param host String
	 * @param name String
	 * @param typ int
	 * @param sess SrvSession
	 * @param create boolean
	 * @return SharedDevice
	 * @exception InvalidUserException
	 */
	public SharedDevice findShare(String host, String name, int typ, SrvSession sess, boolean create)
		throws InvalidUserException {
		
		//	Check for the special HOME disk share
		
		SharedDevice share = null;
		
		//	Find the required share by name/type. Use a case sensitive search first, if that fails use a case
		//	insensitive search.
		
		share = m_filesysConfig.getShares().findShare(name, typ, false);
		
		if ( share == null) {
			
			//	Try a case insensitive search for the required share
			
			share = m_filesysConfig.getShares().findShare(name, typ, true);
		}
		
		//	Check if the share is available
		
		if ( share != null && share.getContext() != null && share.getContext().isAvailable() == false)
		    share = null;
		
		//	Return the shared device, or null if no matching device was found
		
		return share;
	}

	/**
	 * Delete temporary shares for the specified session
	 * 
	 * @param sess SrvSession
	 */
	public void deleteShares(SrvSession sess) {

		//	Check if the session has any dynamic shares
		
		if ( sess.hasDynamicShares() == false)
			return;
			
		//	Delete the dynamic shares
		
		SharedDeviceList shares = sess.getDynamicShareList();
		Enumeration<SharedDevice> enm = shares.enumerateShares();
		
		while ( enm.hasMoreElements()) {

			//	Get the current share from the list
			
			SharedDevice shr = enm.nextElement();
			
			//	Close the shared device
			
			shr.getContext().CloseContext();
			
			//	DEBUG
			
			if ( Debug.EnableInfo && hasDebug())
				Debug.println("Deleted dynamic share " + shr);
		}
	}
	
	/**
	 * Return the list of available shares.
	 * 
	 * @param host String
	 * @param sess SrvSession
	 * @param allShares boolean
	 * @return SharedDeviceList
	 */
	public SharedDeviceList getShareList(String host, SrvSession sess, boolean allShares) {

    //  Check that the filesystems configuration is valid
    
    if ( m_filesysConfig == null)
      return null;
    
		//	Make a copy of the global share list and add the per session dynamic shares
		
	  SharedDeviceList shrList = new SharedDeviceList( m_filesysConfig.getShares());
		
		if ( sess != null && sess.hasDynamicShares()) {
			
			//	Add the per session dynamic shares
			
			shrList.addShares(sess.getDynamicShareList());
		}
		  
		//	Remove unavailable shares from the list and return the list

		if ( allShares == false)
		  shrList.removeUnavailableShares();
		return shrList;
	}
	
	/**
	 * Close the share mapper, release any resources.
	 */
	public void closeMapper() {
	}

  /**
   * Configuration changed
   * 
   * @param id int
   * @param config Serverconfiguration
   * @param newVal Object
   * @return int
   * @throws InvalidConfigurationException
   */
  public int configurationChanged(int id, ServerConfiguration config, Object newVal)
    throws InvalidConfigurationException {

    // Check if the filesystems configuration section has been added
    
    if ( id == ConfigId.ConfigSection) {
      
      // Check if the section added is the filesystems config
      
      if ( newVal instanceof FilesystemsConfigSection)
        m_filesysConfig = (FilesystemsConfigSection) newVal;

      // Return a dummy status
      
      return ConfigurationListener.StsAccepted;
    }
    
    // Return a dummy status
    
    return ConfigurationListener.StsIgnored;
  }
}
