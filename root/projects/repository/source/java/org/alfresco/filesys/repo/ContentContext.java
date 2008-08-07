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
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.filesys.repo;

import org.alfresco.filesys.alfresco.AlfrescoContext;
import org.alfresco.filesys.alfresco.IOControlHandler;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.DiskSharedDevice;
import org.alfresco.jlan.server.filesys.FileSystem;
import org.alfresco.jlan.smb.server.notify.NotifyChangeHandler;
import org.alfresco.service.cmr.repository.*;

/**
 * Content Filesystem Context Class
 * 
 * <p>Contains per filesystem context.
 * 
 * @author GKSpencer
 */
public class ContentContext extends AlfrescoContext
{
    // Store and root path
    
    private String m_storeName;
    private String m_rootPath;
    
    // Root node
    
    private NodeRef m_rootNodeRef;
    
    // Node monitor
    
    private NodeMonitor m_nodeMonitor;
    
    /**
     * Class constructor
     *
     *@param filesysName String
     * @param storeName String
     * @param rootPath String
     * @param rootNodeRef NodeRef
     */
    public ContentContext(String filesysName, String storeName, String rootPath, NodeRef rootNodeRef)
    {
        super(filesysName, rootNodeRef.toString());
        
        m_storeName = storeName;
        m_rootPath  = rootPath;
        
        m_rootNodeRef = rootNodeRef;
        
        // Create the I/O control handler
        
        setIOHandler( createIOHandler( null));
    }
    
    /**
     * Return the filesystem type, either FileSystem.TypeFAT or FileSystem.TypeNTFS.
     * 
     * @return String
     */
    public String getFilesystemType()
    {
        return FileSystem.TypeNTFS;
    }
    
    /**
     * Return the store name
     * 
     * @return String
     */
    public final String getStoreName()
    {
        return m_storeName;
    }
    
    /**
     * Return the root path
     * 
     * @return String
     */
    public final String getRootPath()
    {
        return m_rootPath;
    }
    
    /**
     * Return the root node
     * 
     * @return NodeRef
     */
    public final NodeRef getRootNode()
    {
        return m_rootNodeRef;
    }

    /**
     * Close the filesystem context
     */
	public void CloseContext() {

		// Stop the node monitor, if enabled
		
		if ( m_nodeMonitor != null)
			m_nodeMonitor.shutdownRequest();
		
		//	Call the base class
		
		super.CloseContext();
	}
    
    /**
     * Create the I/O control handler for this filesystem type
     * 
     * @param filesysDriver DiskInterface
     * @return IOControlHandler
     */
    protected IOControlHandler createIOHandler( DiskInterface filesysDriver)
    {
    	return new ContentIOControlHandler();
    }
    
    /**
     * Create the node monitor
     * 
     * @param filesysDriver ContentDiskDriver
     */
    protected void createNodeMonitor( ContentDiskDriver filesysDriver) {
    	m_nodeMonitor = new NodeMonitor( filesysDriver, this);
    }

	/**
	 * Start the filesystem
	 * 
	 * @param share DiskSharedDevice
	 * @exception DeviceContextException
	 */
	public void startFilesystem(DiskSharedDevice share)
		throws DeviceContextException {

		// Call the base class
		
		super.startFilesystem(share);
		
		// Start the node monitor, if enabled
		
		if ( m_nodeMonitor != null)
			m_nodeMonitor.startMonitor();
	}
}
