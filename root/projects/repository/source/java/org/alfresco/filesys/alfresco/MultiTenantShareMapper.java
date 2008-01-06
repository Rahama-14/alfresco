/*
 * Copyright (C) 2005-2006 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.filesys.alfresco;

import java.util.Enumeration;
import java.util.Hashtable;

import org.alfresco.config.ConfigElement;
import org.alfresco.filesys.AlfrescoConfigSection;
import org.alfresco.filesys.repo.ContentContext;
import org.alfresco.filesys.repo.ContentDiskDriver;
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
import org.alfresco.jlan.server.filesys.DiskDeviceContext;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.DiskSharedDevice;
import org.alfresco.jlan.server.filesys.FilesystemsConfigSection;
import org.alfresco.jlan.server.filesys.SrvDiskInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

/**
 * Multi Tenant Share Mapper Class
 * 
 * @author gkspencer
 */
public class MultiTenantShareMapper implements ShareMapper, ConfigurationListener {

	//	Server configuration and configuration sections
	
	private ServerConfiguration m_config;
	private FilesystemsConfigSection m_filesysConfig;
	private AlfrescoConfigSection m_alfrescoConfig;
  
	//  Share name for multi-tenant connections
	
	private String m_tenantShareName;
	
	//  Store name and root path from standard content filesystem share
	
	private String m_rootPath;
	private String m_storeName;
	
	//  Table of tenant share lists indexed by tenant domain
	
	private Hashtable<String, SharedDeviceList> m_tenantShareLists;
	
	//	Debug enable flag
	
	private boolean m_debug;
	
	/**
	 * Default constructor
	 */
	public MultiTenantShareMapper() {
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

		// 	Get the Alfresco configuration section
		
		m_alfrescoConfig = (AlfrescoConfigSection) m_config.getConfigSection( AlfrescoConfigSection.SectionName);

		if ( m_filesysConfig == null || m_alfrescoConfig == null)
			m_config.addListener( this);
		
		// Find the content filesystem details to be used for hte tenant shares

		if ( m_filesysConfig != null)
			findContentShareDetails();
		
		//  Check if a tenant share name has been specified
		
		ConfigElement tenantShareName = params.getChild( "TenantShareName");
		
		if ( tenantShareName != null)
		{
			// Validate the share name
			
			if ( tenantShareName.getValue() != null && tenantShareName.getValue().length() > 0)
				m_tenantShareName = tenantShareName.getValue();
			else
				throw new InvalidConfigurationException("Invalid tenant share name");
		}
		
		//	Check if debug is enabled
		
		if ( params.getChild("debug") != null)
			m_debug = true;
		
		// Create the tenant share lists table
		
		m_tenantShareLists = new Hashtable<String, SharedDeviceList>();
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
		
		//  Check if this is a tenant user
		
		if ( m_alfrescoConfig.getTenantService().isEnabled() && m_alfrescoConfig.getTenantService().isTenantUser())
			return findTenantShare(host, name, typ, sess, create);
		
		//	Find the required share by name/type. Use a case sensitive search first, if that fails use a case
		//	insensitive search.
		
		SharedDevice share = m_filesysConfig.getShares().findShare(name, typ, false);
		
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
    
		//  Check if this is a tenant user
		
		if ( m_alfrescoConfig.getTenantService().isEnabled() && m_alfrescoConfig.getTenantService().isTenantUser())
			return getTenantShareList(host, sess, allShares);
		
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
		
		// Close all the tenant shares
		
		// TODO:
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

			if (newVal instanceof FilesystemsConfigSection)
				m_filesysConfig = (FilesystemsConfigSection) newVal;

			// Or the Alfresco config

			else if (newVal instanceof AlfrescoConfigSection)
				m_alfrescoConfig = (AlfrescoConfigSection) newVal;

			// Return a dummy status

			return ConfigurationListener.StsAccepted;
		}

		// Check if the tenant share template details have been set
		
		if ( m_rootPath == null)
			findContentShareDetails();
		
		// Return a dummy status

		return ConfigurationListener.StsIgnored;
	}
  
	/**
	 * Find a share for a tenant
	 * 
	 * @param host String
	 * @param name String
	 * @param typ int
	 * @param sess SrvSession
	 * @param create boolean
	 * @return SharedDevice
	 * @exception InvalidUserException
	 */
	private final SharedDevice findTenantShare(String host, String name, int typ, SrvSession sess, boolean create)
	    throws InvalidUserException {

		// Get the share list for the tenant
		
		SharedDeviceList shareList = getTenantShareList(host, sess, true);
		if ( shareList == null)
			return null;
		
		// Search for the required share
		
		return shareList.findShare( name, typ, true);
	}
	
	/**
	 * Return the list of available shares for a particular tenant
	 * 
	 * @param host String
	 * @param sess SrvSession
	 * @param allShares boolean
	 * @return SharedDeviceList
	 */
	private final SharedDeviceList getTenantShareList(String host, SrvSession sess, boolean allShares) {
		
		// Get the tenant user domain
		
		String tenantDomain = m_alfrescoConfig.getTenantService().getCurrentUserDomain();

		// Get the share list for the current domain

		SharedDeviceList shareList = null;
		
		synchronized ( m_tenantShareLists)
		{
			// Get the tenant specific share list

			shareList = m_tenantShareLists.get( tenantDomain);
		
			if ( shareList == null)
			{
				// Create the tenant specific share list
				
				shareList = new SharedDeviceList();
				
				// Create a tenant specific share for this domain
				
				shareList.addShare( createTenantShare());
				
				// Store the list for use by other members of this domain
				
				m_tenantShareLists.put( tenantDomain, shareList);
			}
		}
		
		// Return the tenant specific share list
		
		return shareList;
	}
	
	/**
	 * Create a tenant domain specific share
	 */
	private final DiskSharedDevice createTenantShare()
	{
        StoreRef storeRef = new StoreRef(m_storeName);
        NodeRef rootNodeRef = new NodeRef(storeRef.getProtocol(), storeRef.getIdentifier(), "dummy"); 
        
        // Root nodeRef is required for storeRef part
        
        rootNodeRef = m_alfrescoConfig.getTenantService().getRootNode(m_alfrescoConfig.getNodeService(), m_alfrescoConfig.getSearchService(),
        		                                                      m_alfrescoConfig.getNamespaceService(), m_rootPath, rootNodeRef);

        //  Create the disk driver and context

        ContentDiskDriver diskDrv = (ContentDiskDriver) m_alfrescoConfig.getRepoDiskInterface();
        ContentContext diskCtx = new ContentContext(m_tenantShareName, "", m_rootPath, rootNodeRef);
        
        // Enable file state caching
        
        diskCtx.enableStateTable( true, diskDrv.getStateReaper());
        
        // Initialize the I/O control handler
        
        if ( diskCtx.hasIOHandler())
        	diskCtx.getIOHandler().initialize( diskDrv, diskCtx);
        
        //  Default the filesystem to look like an 80Gb sized disk with 90% free space

        diskCtx.setDiskInformation(new SrvDiskInfo(2560, 64, 512, 2304));

        //  Create a temporary shared device for the user to access the tenant company home directory

        return new DiskSharedDevice(m_tenantShareName, diskDrv, diskCtx);
	}
	
	/**
	 * Find the content filesystem driver details used for the tenant shares
	 */
 	private final void findContentShareDetails()
	{
		// Need the file system configuration to do the lookup
 		
 		if ( m_filesysConfig == null)
 			return;
 		
 		// Get the fixed share list
 		
 		SharedDeviceList shareList = m_filesysConfig.getShares();
 		Enumeration<SharedDevice> shareEnum = shareList.enumerateShares();
 		
 		while ( shareEnum.hasMoreElements())
 		{
 			// Get the current shared device
 			
 			SharedDevice share = shareEnum.nextElement();
 			if ( share.getContext() instanceof ContentContext)
 			{
 				// Found a content filesystem share

 				ContentContext ctx = (ContentContext) share.getContext();
 				
 				// Store the share details that are used for the tenant shares
 				
 				m_rootPath  = ctx.getRootPath();
 				m_storeName = ctx.getStoreName();
 				
 				if ( m_tenantShareName == null)
 					m_tenantShareName = ctx.getDeviceName();
 			}
 		}
	}
}
