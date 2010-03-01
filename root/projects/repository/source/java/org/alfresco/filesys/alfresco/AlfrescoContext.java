/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.filesys.alfresco;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.filesys.config.GlobalDesktopActionConfigBean;
import org.alfresco.filesys.state.FileStateReaper;
import org.alfresco.filesys.state.FileStateTable;
import org.alfresco.jlan.server.filesys.DiskDeviceContext;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.FileSystem;
import org.alfresco.jlan.server.filesys.SrvDiskInfo;
import org.alfresco.jlan.server.filesys.pseudo.PseudoFileInterface;


/**
 * Alfresco Filesystem Context Class
 * 
 * <p>Contains per filesystem context.
 * 
 * @author GKSpencer
 */
public abstract class AlfrescoContext extends DiskDeviceContext
{
    // Token name to substitute current servers DNS name or TCP/IP address into the webapp URL

    private static final String TokenLocalName = "${localname}";

	// Debug levels
	
	public final static int DBG_FILE		= 0x00000001;	// file/folder create/delete
	public final static int DBG_FILEIO		= 0x00000002;	// file read/write/truncate
	public final static int DBG_SEARCH  	= 0x00000004;	// folder search
	public final static int DBG_INFO        = 0x00000008;	// file/folder information
	public final static int DBG_LOCK        = 0x00000010;	// file byte range locking
	public final static int DBG_PSEUDO      = 0x00000020;	// pseudo files/folders
	public final static int DBG_RENAME      = 0x00000040;	// rename file/folder
	
	// Filesystem debug flag strings
	  
	private static final String m_filesysDebugStr[] = { "FILE", "FILEIO", "SEARCH", "INFO", "LOCK", "PSEUDO", "RENAME" };

    // File state table and associated file state reaper
    
    private FileStateTable m_stateTable;
    private FileStateReaper m_stateReaper;
    
    // URL pseudo file web path prefix (server/port/webapp) and link file name
    
    private String m_urlPathPrefix;
    private String m_urlFileName;
    
    // Pseudo file interface
    
    private PseudoFileInterface m_pseudoFileInterface;

    // Desktop actions
    
    private GlobalDesktopActionConfigBean m_globalDesktopActionConfig = new GlobalDesktopActionConfigBean();
    private DesktopActionTable m_desktopActions;
    private List<DesktopAction> m_desktopActionsToInitialize;
    
    // I/O control handler
    
    private IOControlHandler m_ioHandler;

    // Debug flags
    //
    // Requires the logger to be enabled for debug output
    
    public int m_debug;
    
    public AlfrescoContext()
    {
        // Default the filesystem to look like an 80Gb sized disk with 90% free space
        
        setDiskInformation(new SrvDiskInfo(2560000, 64, 512, 2304000));
        
        // Set parameters
        
        setFilesystemAttributes(FileSystem.CasePreservedNames + FileSystem.UnicodeOnDisk +
                FileSystem.CaseSensitiveSearch);        
    }
    

    public void setDisableChangeNotification(boolean disableChangeNotification)
    {
        enableChangeHandler(!disableChangeNotification);
    }
    
    /**
     * Complete initialization by registering with a disk driver
     */
    public void initialize(AlfrescoDiskDriver filesysDriver)
    {
        if (m_desktopActionsToInitialize != null)
        {
            for (DesktopAction desktopAction : m_desktopActionsToInitialize)
            {
                // Initialize the desktop action
                try
                {
                    desktopAction.initializeAction(filesysDriver, this);
                }
                catch (DesktopActionException ex)
                {
                    throw new AlfrescoRuntimeException("Failed to initialize desktop action", ex);
                }
                addDesktopAction(desktopAction);
            }
        }
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
     * Determine if the file state table is enabled
     * 
     * @return boolean
     */
    public final boolean hasStateTable()
    {
        return m_stateTable != null ? true : false;
    }
    
    /**
     * Return the file state table
     * 
     * @return FileStateTable
     */
    public final FileStateTable getStateTable()
    {
        return m_stateTable;
    }
    
    /**
     * Enable/disable the file state table
     * 
     * @param ena boolean
     * @param stateReaper FileStateReaper
     */
    public final void enableStateTable(boolean ena, FileStateReaper stateReaper)
    {
        if ( ena == false)
        {
        	// Remove the state table from the reaper
        	
        	stateReaper.removeStateTable( getShareName());
            m_stateTable = null;
        }
        else if ( m_stateTable == null)
        {
        	// Create the file state table

            m_stateTable = new FileStateTable();
            
            // Register with the file state reaper
            
            stateReaper.addStateTable( getShareName(), m_stateTable);
        }
        
        // Save the reaper, for deregistering when the filesystem is closed
        
        m_stateReaper = stateReaper;
    }
    
    /**
     * Determine if the pseudo file interface is enabled
     * 
     * @return boolean
     */
    public final boolean hasPseudoFileInterface()
    {
    	return m_pseudoFileInterface != null ? true : false;
    }
    
    /**
     * Return the pseudo file interface
     * 
     * @return PseudoFileInterface
     */
    public final PseudoFileInterface getPseudoFileInterface()
    {
    	return m_pseudoFileInterface;
    }

    /**
     * Enable the pseudo file interface for this filesystem
     */
    public final void enabledPseudoFileInterface()
    {
    	if ( m_pseudoFileInterface == null)
    		m_pseudoFileInterface = new PseudoFileImpl();
    }
    
    /**
     * Determine if there are desktop actins configured
     * 
     * @return boolean
     */
    public final boolean hasDesktopActions()
    {
    	return m_desktopActions != null ? true : false;
    }
    
    /**
     * Return the desktop actions table
     * 
     * @return DesktopActionTable
     */
    public final DesktopActionTable getDesktopActions()
    {
    	return m_desktopActions;
    }
    
    /**
     * Return the count of desktop actions
     * 
     * @return int
     */
    public final int numberOfDesktopActions()
    {
    	return m_desktopActions != null ? m_desktopActions.numberOfActions() : 0;
    }

    /**
     * Add a desktop action
     * 
     * @param action DesktopAction
     * @return boolean
     */
    public final boolean addDesktopAction(DesktopAction action)
    {
    	// Check if the desktop actions table has been created
    	
    	if ( m_desktopActions == null)
    	{
    		m_desktopActions = new DesktopActionTable();
    		
    		// Enable pseudo files
    		
    		enabledPseudoFileInterface();
    	}
    	
    	// Add the action
    	
    	return m_desktopActions.addAction(action);
    }

    /**
     * Determine if custom I/O control handling is enabled for this filesystem
     * 
     * @return boolean
     */
    public final boolean hasIOHandler()
    {
    	return m_ioHandler != null;
    }
    
    /**
     * Return the custom I/O control handler
     * 
     * @return IOControlHandler
     */
    public final IOControlHandler getIOHandler()
    {
    	return m_ioHandler;
    }
    
    /**
     * Determine if the URL pseudo file is enabled
     * 
     * @return boolean
     */
    public final boolean hasURLFile()
    {
        if ( m_urlPathPrefix != null && m_urlFileName != null)
            return true;
        return false;
    }
    
    /**
     * Return the URL pseudo file path prefix
     * 
     * @return String
     */
    public final String getURLPrefix()
    {
        return m_urlPathPrefix;
    }
    
    /**
     * Return the URL pseudo file name
     * 
     * @return String
     */
    public final String getURLFileName()
    {
        return m_urlFileName;
    }
    
    /**
     * Set the URL path prefix
     * 
     * @param urlPrefix String
     */
    public final void setURLPrefix(String urlPrefix)
    {
        m_urlPathPrefix = urlPrefix;

        if ( urlPrefix != null)
        {
            // Make sure the web prefix has a trailing slash
            
            if ( !urlPrefix.endsWith("/"))
                urlPrefix = urlPrefix + "/";
            
            // Check if the URL path name contains the local name token
    
            int pos = urlPrefix.indexOf(TokenLocalName);
            if (pos != -1)
            {
    
                // Get the local server name
    
                String srvName = "localhost";
                
                try
                {
                    srvName = InetAddress.getLocalHost().getHostName();
                }
                catch ( Exception ex)
                {
                }
    
                // Rebuild the host name substituting the token with the local server name
    
                StringBuilder hostStr = new StringBuilder();
    
                hostStr.append( urlPrefix.substring(0, pos));
                hostStr.append(srvName);
    
                pos += TokenLocalName.length();
                if (pos < urlPrefix.length())
                    hostStr.append( urlPrefix.substring(pos));
    
                m_urlPathPrefix = hostStr.toString();
            }
        
        	enabledPseudoFileInterface();
        }
    }
    
    /**
     * Set the URL pseudo file name
     * 
     * @param urlFileName String
     */
    public final void setURLFileName(String urlFileName)
    {
        m_urlFileName = urlFileName;

        // URL file name must end with .url
        if (urlFileName != null)
        {
            if (!urlFileName.endsWith(".url"))
                throw new AlfrescoRuntimeException("URL link file must end with .url, " + urlFileName);

            enabledPseudoFileInterface();
        }
    }

    /**
     * Set the desktop actions
     * 
     * @param desktopActions DesktopActionTable
     * @param filesysDriver DiskInterface
     */
    public final void setDesktopActions(DesktopActionTable desktopActions, DiskInterface filesysDriver)
    {
    	// Enumerate the desktop actions and add to this filesystem
    	
    	Enumeration<String> names = desktopActions.enumerateActionNames();
    	
    	while ( names.hasMoreElements())
    	{
    		addDesktopAction( desktopActions.getAction(names.nextElement()));
    	}
    	
    	// If there are desktop actions then create the custom I/O control handler
    	
    	if ( numberOfDesktopActions() > 0)
    	{
    		// Create the custom I/O control handler
    	
    		m_ioHandler = createIOHandler( filesysDriver);
    		if ( m_ioHandler != null)
    			m_ioHandler.initialize(( AlfrescoDiskDriver) filesysDriver, this);
    	}
    }
    

    /**
     * Set the desktop actions
     * 
     * @param desktopActions DesktopAction List
     */
    public final void setDesktopActionList(List<DesktopAction> desktopActions)
    {
        m_desktopActionsToInitialize = desktopActions;
    }

    public void setGlobalDesktopActionConfig(GlobalDesktopActionConfigBean desktopActionConfig)
    {
        m_globalDesktopActionConfig = desktopActionConfig;
    }


    protected GlobalDesktopActionConfigBean getGlobalDesktopActionConfig()
    {
        return m_globalDesktopActionConfig;
    }
    
    


    /**
     * Create the I/O control handler for this filesystem type
     * 
     * @param filesysDriver DiskInterface
     * @return IOControlHandler
     */
    protected abstract IOControlHandler createIOHandler( DiskInterface filesysDriver);
    
    /**
     * Set the I/O control handler
     * 
     * @param ioctlHandler IOControlHandler
     */
    protected void setIOHandler( IOControlHandler ioctlHandler)
    {
    	m_ioHandler = ioctlHandler;
    }
    
    /**
     * Set the debug flags, also requires the logger to be enabled for debug output
     * 
     * @param dbg int
     */
    public final void setDebug(String flagsStr)
    {
    	int filesysDbg = 0;
    	
    	if (flagsStr != null)
        {
	        // Parse the flags
	  
	        StringTokenizer token = new StringTokenizer(flagsStr.toUpperCase(), ",");
	  
	        while (token.hasMoreTokens())
	        {
	        	// Get the current debug flag token
  
                String dbg = token.nextToken().trim();
  
                // Find the debug flag name
  
                int idx = 0;
                boolean match = false;
  
                while (idx < m_filesysDebugStr.length && match == false)
                {
                	if ( m_filesysDebugStr[idx].equalsIgnoreCase(dbg) == true)
                		match = true;
                	else
                		idx++;
                }
  
                if (match == false)
                    throw new AlfrescoRuntimeException("Invalid filesystem debug flag, " + dbg);
  
                // Set the debug flag
  
                filesysDbg += 1 << idx;
            }
	        
	        // Set the debug flags
	        
	        m_debug = filesysDbg;
        }
    }
    
    /**
     * Check if a debug flag is enabled
     * 
     * @param flg int
     * @return boolean
     */
    public final boolean hasDebug(int flg)
    {
    	return (m_debug & flg) != 0 ? true : false;
    }
    
    /**
     * Close the filesystem context
     */
	public void CloseContext() {
		
		//	Deregister the file state table from the reaper
		
		if ( m_stateTable != null)
			enableStateTable( false, m_stateReaper);
		
		//	Call the base class
		
		super.CloseContext();
	}
}
