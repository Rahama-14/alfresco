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

package org.alfresco.jlan.server.filesys.db;

import java.io.File;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.server.filesys.DiskDeviceContext;
import org.alfresco.jlan.server.filesys.DiskSharedDevice;
import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileSystem;
import org.alfresco.jlan.server.filesys.cache.FileState;
import org.alfresco.jlan.server.filesys.cache.FileStateCache;
import org.alfresco.jlan.server.filesys.loader.DeleteFileRequest;
import org.alfresco.jlan.server.filesys.loader.FileLoader;
import org.alfresco.jlan.server.filesys.loader.FileRequestQueue;
import org.alfresco.jlan.server.filesys.quota.QuotaManagerException;
import org.alfresco.jlan.util.MemorySize;
import org.alfresco.config.ConfigElement;

/**
 * Database Device Context Class
 *
 * @author gkspencer
 */
public class DBDeviceContext extends DiskDeviceContext {

	//	Default file state cache timeout
	
	public final static long DEFAULT_CACHETIMEOUT	= 5 * 60000L;		//	5 minutes
	public final static long MIN_CACHETIMEOUT			= 5000L;				//	5 seconds
	public final static long MAX_CACHETIMEOUT			= 60 * 60000L;	//	1 hour
	
	//	Default file state cache check interval
	
	public final static long DEFAULT_CACHECHECK		= 1 * 60000L;		//	1 minutes
	public final static long MIN_CACHECHECK  			= 2000L;				//	2 seconds
	public final static long MAX_CACHECHECK  			= 20 * 60000L;	//	20 minutes

	//	Milliseconds per day
	
	private static final long MillisecondsPerDay	= 24L * 60L * 60000L;

  //  Minimum allowed maximum file size
  
  private static final long MinimumMaxfileSize  = 512 * MemorySize.KILOBYTE;
  
	//	File state cache timeout, in milliseconds
	
	private long m_cacheTimer = DEFAULT_CACHETIMEOUT;
	private long m_cacheCheckInterval = DEFAULT_CACHECHECK;
	
	//	NTFS streams enable flag
	
	private boolean m_ntfsStreams = true;

	//	Database interface class
	//
	//	The database interface implementation may also provide the file loader interface. In this case
	//	database interface initialization must set the loader class.
	
	private String m_dbifClass;
	private DBInterface m_dbInterface;
	
	//	Database interface configuration values
	
	private ConfigElement m_dbifConfig;
	
	//	File loader class, used to load/save file data
	
	private String m_loaderClass;
	private FileLoader m_loader;
	
	//	Loader class configuration values
	
	private ConfigElement m_loaderConfig;
	
	//	Trash can enable, mark files as deleted rather than actually deleting
	
	private boolean m_trashCan;
	
	//	Retention period, milliseconds to add to current date/time value, or -1 to disable
	
	private long m_retentionPeriod = -1;
	
	//	Required database features
	
	private int m_dbFeatures;
	
	//	Root directory information
	
	private DBFileInfo m_rootInfo;
	
  //  Maximum file size to allow on the filesystem, zero indicates no limit
  
  private long m_maxFileSize;
  
  //  Mark files as offline, optionally above a certain size
  
  private boolean m_offlineFiles;
  private long m_offlineFileSize;
  
  //  List of files to be deleted when the database is back online
  
  private FileRequestQueue m_offlineDeleteList;
  
  //  File state cache
  
  private FileStateCache m_stateCache;
  
	//	Debug enable
	
	private boolean m_debug;
	
	/**
	 * Class constructor
	 * 
	 * @param args ConfigElement
	 * @exception DeviceContextException
	 */
	public DBDeviceContext(ConfigElement args)
		throws DeviceContextException {
	  super();
	  
		//	Initialize the database interface

	  initialize(args);
	}
	
	/**
	 * Initialize the database device context
	 * 
	 * @param args ConfigElement
	 * @exception DeviceContextException
	 */
	protected final void initialize(ConfigElement args)
		throws DeviceContextException {
	  
	  //	Set the filesystem attributes
	  
	  setFilesystemAttributes(FileSystem.CaseSensitiveSearch + FileSystem.CasePreservedNames);
	  
		//	Initialize the database interface
		
		ConfigElement dbParams = args.getChild("DatabaseInterface");

		if ( dbParams != null) {
		  
	    //	Get the database interface class name and create a new instance
	    
	    ConfigElement dbClass = dbParams.getChild("class");
	    if ( dbClass == null)
	      throw new DeviceContextException("Database interface class not specified");
	    
	    //	Create the database interface
	    
	    try {
	      
	      //	Create a database interface instance
	      
	      Object dbObj = Class.forName(dbClass.getValue()).newInstance();
	      
	      if ( dbObj instanceof DBInterface) {
	        
	        //	Set the database interface and parameters
	        
	        m_dbifClass   = dbClass.getValue();
	        m_dbInterface = (DBInterface) dbObj;
	        m_dbifConfig  = dbParams;
	      }
	      else
	        throw new DeviceContextException("Database interface class is not an instance of DBInterface");
	    }
	    catch (Exception ex) {
	      throw new DeviceContextException("Database interface error, " + ex.toString());
	    }
	  }
	  else
	    throw new DeviceContextException("Invalid Database Interface configuration");
		
		//	Get the file loader class name and arguments

		ConfigElement ldrParams = args.getChild("FileLoader");
		if ( ldrParams != null) {
			
			//	Set the loader configuration values
			
			m_loaderConfig = ldrParams;
			ConfigElement ldrClass = m_loaderConfig.getChild("class");
      
      if ( ldrClass == null || ldrClass.getValue().length() == 0)
        throw new DeviceContextException("Invalid File Loader configuration");
      
      //  Set the file loader class
        
			m_loaderClass = ldrClass.getValue();
		}
		else if ( m_dbInterface instanceof FileLoader) {
		  
		  //	Database interface is also the file loader
		  
		  m_loader = (FileLoader) m_dbInterface;
		}

		//	Get the file state cache timer, if specified
		
		ConfigElement nameVal = args.getChild("CacheTime");
		if ( nameVal != null) {
			
			//	Convert the cache timeout value
			
			String cacheTmo = nameVal.getValue();
			
			if ( cacheTmo != null) {
				
				//	Validate the cache time value
				
				try {
					
					//	Convert the cache timeout string to a numeric value
					
					m_cacheTimer = Long.valueOf(cacheTmo).longValue() * 1000L;
					
					//	Check that the value is within the valid range
					
					if ( m_cacheTimer < MIN_CACHETIMEOUT)
						m_cacheTimer = MIN_CACHETIMEOUT;
					else if ( m_cacheTimer > MAX_CACHETIMEOUT)
						m_cacheTimer = MAX_CACHETIMEOUT;
				}
				catch (NumberFormatException ex) {
				}
			}
		}
				
		//	Get the file state cache check interval, if specified

		nameVal = args.getChild("CacheCheckInterval");
		
		if ( nameVal != null) {
			
			//	Convert the cache check interval

			String cacheCheck = nameVal.getValue();
			
			if ( cacheCheck != null) {
				
				//	Validate the cache check interval value
				
				try {
					
					//	Convert the cache check interval string to a numeric value
					
					m_cacheCheckInterval = Long.valueOf(cacheCheck).longValue() * 1000L;
					
					//	Check that the value is within the valid range
					
					if ( m_cacheCheckInterval < MIN_CACHECHECK)
						m_cacheCheckInterval = MIN_CACHECHECK;
					else if ( m_cacheCheckInterval > MAX_CACHECHECK)
						m_cacheCheckInterval = MAX_CACHECHECK;
				}
				catch (NumberFormatException ex) {
				}
			}
		}
				
		//	Check if debug output is enabled
		
		if ( args.getChild("Debug") != null)
			m_debug = true;

		//	Check if NTFS streams are disabled
		
		if ( args.getChild("disableNTFSStreams") != null)
			m_ntfsStreams = false;

		//	Check if the trash can feature should be enabled
		
		if ( args.getChild("enableTrashCan") != null)
			m_trashCan = true;

    //  Check if files should be marked as offline
    
    if ( args.getChild( "offlineFiles") != null) {

      // Mark files as offline
      
      m_offlineFiles = true;
    }
    
    // Check if offline files are enabled above a specified size
    
    nameVal = args.getChild( "offlineFileSize");
    if ( nameVal != null) {
      try {
        m_offlineFileSize = MemorySize.getByteValue( nameVal.getValue());
        
        // Range check the offline file size
        
        if ( m_offlineFileSize < 0)
          throw new DeviceContextException( "Invalid offline files size, " + nameVal.getValue());
        
        //  Enable offline files
        
        m_offlineFiles = true;
      }
      catch ( NumberFormatException ex) {
        throw new DeviceContextException( "Invalid offline files size, " + nameVal.getValue());
      }
    }
    
    //  Check if a maximum file size has been specified
    
    nameVal = args.getChild( "MaxFileSize");
    if ( nameVal != null) {
    
      // Validate the maximum file size
      
      try {
        m_maxFileSize = MemorySize.getByteValue( nameVal.getValue());
        
        // Range check the maximum file size
        
        if ( m_maxFileSize < MinimumMaxfileSize)
          throw new DeviceContextException( "Maximum file size is below minimum allowed setting (" + MinimumMaxfileSize/MemorySize.KILOBYTE + "K)");
      }
      catch ( NumberFormatException ex) {
        throw new DeviceContextException( "Invalid maximum file size value, " + nameVal.getValue());
      }
    }
    
		//	Check if quota management should be enabled for this filesystem
		
		if ( args.getChild("QuotaManagement") != null) {

			//	Check if quota manager debug output is enabled
			
			boolean quotaDebug = args.getChild("QuotaDebug") != null ? true : false;
						
			//	Create the default quota manager
			
			setQuotaManager(new DBQuotaManager(this, quotaDebug));
		}

		//	Get the retention period in days, if specified
		
		nameVal = args.getChild("RetentionPeriod");
		if ( nameVal != null) {
			try {
				
				//	Convert the retention period in days value
				
				long retainDays = Long.parseLong(nameVal.getValue());
				
				//	Range check the retention period
				
				if ( retainDays < 0 || retainDays > 3650)
					throw new DeviceContextException("RetentionPeriod out of valid range (0 - 3650)");
					
				//	Convert the retention period to a milliseconds interval that can be added to the current date/time
				
				m_retentionPeriod = retainDays * MillisecondsPerDay;
			}
			catch (NumberFormatException ex) {
				throw new DeviceContextException("RetentionPeriod is invalid, " + nameVal.getValue());
			}
		}

		//	Check if the database interface supports retention, if retention has been enabled
		
		if ( hasRetentionPeriod() && getDBInterface().supportsFeature(DBInterface.FeatureRetention) == false)
		  throw new DeviceContextException("Database interface does not support retention");
		
		//	Create the file loader instance and get the database features required by the loader
		
		int dbFeatures = 0;

		if ( m_loaderClass != null) {
		  
			try {
			  
			  //	Create the file loader instance
			  
				m_loader = (FileLoader) Class.forName(m_loaderClass).newInstance();
	
				//	Get the database features that the database interface must implement to support this file loader
				
				dbFeatures = m_loader.getRequiredDBFeatures();
			}
			catch (Exception ex) {
			  
			  //	DEBUG
			  
			  if ( Debug.EnableError && hasDebug())
			    Debug.println(ex);
			  
			  //	Rethrow the exception
			  
			  throw new DeviceContextException(ex.getMessage());
			}
		}
		else if ( m_loader == null)
		  throw new DeviceContextException("File loader not specified");
		
		//	Create the root directory information
		
		m_rootInfo = new DBFileInfo("\\", "\\", 0, 0);
		
		m_rootInfo.setFileAttributes(FileAttribute.Directory);
		m_rootInfo.setMode( DBDiskDriver.DefaultNFSDirMode);
		
		m_rootInfo.setGid( 0);
		m_rootInfo.setUid( 0);

		long timeNow = System.currentTimeMillis();
		m_rootInfo.setCreationDateTime( timeNow);
		m_rootInfo.setAccessDateTime( timeNow);
		m_rootInfo.setModifyDateTime( timeNow);
		m_rootInfo.setChangeDateTime( timeNow);
		
	  //	Enable the file state cache
	  
	  enableStateCache(true);

		//	Set the enabled database features
		
		if ( hasNTFSStreamsEnabled() == true && m_loader.supportsStreams() == true)
		  dbFeatures |= DBInterface.FeatureNTFS;
		
		if ( hasRetentionPeriod() == true)
		  dbFeatures |= DBInterface.FeatureRetention;
		
    if ( getDBInterface().supportsFeature( DBInterface.FeatureSymLinks))
      dbFeatures |= DBInterface.FeatureSymLinks;
    
		try {
		  
		  //	Request the required database features to be enabled

		  getDBInterface().requestFeatures(dbFeatures);
		}
		catch ( DBException ex) {
		  throw new DeviceContextException("Failed to enable database features, " + ex.getMessage());
		}
		
		//	Initialize the database interface

		try {
		  
		  //	Initialize the database interface
		  
			getDBInterface().initializeDatabase(this, m_dbifConfig);
		}
		catch (InvalidConfigurationException ex) {
		  throw new DeviceContextException("Database interface initialization failure, " + ex.toString());
		}

		//	Set the default file state cache timeout
		
		getStateCache().setCacheTimer(m_cacheTimer);
		getStateCache().setCheckInterval(Math.max(5000,m_cacheTimer/4));
		
		//	Initialize the file loader, if it is a seperate class from the database interface

		if ( m_loaderClass != null) {
		  
			try {
			  
				//	Initialize the file loader
	
				m_loader.initializeLoader(m_loaderConfig, this);
			}
			catch (Exception ex) {
			  
			  //	DEBUG
			  
			  if ( Debug.EnableError && hasDebug())
			    Debug.println(ex);
			  
			  //	Rethrow the exception
			  
			  throw new DeviceContextException(ex.getMessage());
			}
		}
	}
	
	/**
	 * Return the database interface class name
	 * 
	 * @return String
	 */
	public final String getDBInterfaceClass() {
	  return m_dbifClass;
	}
	
	/**
	 * Return the database interface class
	 * 
	 * @return DBInterface
	 */
	public final DBInterface getDBInterface() {
	  return m_dbInterface;
	}

	/**
	 * Return the database interface configuration parameters
	 * 
	 * @return ConfigElement
	 */
	public final ConfigElement getDBInterfaceConfiguration() {
	  return m_dbifConfig;
	}
	
	/**
	 * Return the file loader class name
	 * 
	 * @return String
	 */
	public final String getFileLoaderClass() {
		return m_loaderClass;
	}
	
	/**
	 * Return the file data loader class
	 * 
	 * @return FileLoader
	 */
	public final FileLoader getFileLoader() {
		return m_loader;
	}
	
	/**
	 * Return the file loader class arguments list	
	 * 
	 * @return ConfigElement
	 */
	public final ConfigElement getLoaderConfiguration() {
		return m_loaderConfig;
	}
	
	/**
	 * Return the file state cache timeout, in milliseconds
	 * 
	 * @return long
	 */
	public final long getCacheTimeout() {
		return m_cacheTimer;
	}
	
	/**
	 * Determine if debug output is enabled
	 * 
	 * @return boolean
	 */
	public final boolean hasDebug() { 
	  return m_debug;
	}
	
  /**
   * Check if files should be marked as offline
   * 
   * @return boolean
   */
  public final boolean hasOfflineFiles() {
    return m_offlineFiles;
  }
  
  /**
   * Check if files are only to be marked offline above a certain size
   * 
   * @return boolean
   */
  public final boolean hasOfflineFileSize() {
    return m_offlineFileSize > 0;
  }
  
  /**
   * Return the file size to mark as offline
   * 
   * @return long
   */
  public final long getOfflineFileSize() {
    return m_offlineFileSize;
  }
  
	/**
	 * Determine if the retention period is enabled
	 * 
	 * @return boolean
	 */
	public final boolean hasRetentionPeriod() {
	  return m_retentionPeriod != -1L ? true : false;
	}
	
	/**
	 * Return the retention period, in milliseconds
	 * 
	 * @return long
	 */
	public final long getRetentionPeriod() {
	  return m_retentionPeriod;
	}
	
	/**
	 * Return the root directory file information
	 * 
	 * @return DBFileInfo
	 */
	public final DBFileInfo getRootDirectoryInfo() {
	  return m_rootInfo;
	}
	
	/**
	 * Check if NTFS streams are enabled
	 * 
	 * @return boolean
	 */
	public final boolean hasNTFSStreamsEnabled() {
		return m_ntfsStreams;
	}
	
	/**
	 * Determine if the trashcan feature is enabled
	 * 
	 * @return boolean
	 */
	public final boolean isTrashCanEnabled() {
		return m_trashCan;
	}
	
  /**
   * Check if a mamximum file size has been specified
   * 
   * @return boolean
   */
  public final boolean hasMaximumFileSize() {
    return m_maxFileSize != 0 ? true : false;
  }
  
  /**
   * Return the maximum file size allowed on this filesystem
   * 
   * @return long
   */
  public final long getMaximumFileSize() {
    return m_maxFileSize;
  }
  
  /**
   * Check if there are files to be deleted in the offline delete list
   * 
   * @return boolean
   */
  public final boolean hasOfflineFileDeletes() {
    return m_offlineDeleteList != null;
  }
  
  /**
   * Return the offline file delete list
   *
   * @param clearList boolean
   * @return FileRequestQueue
   */
  public synchronized final FileRequestQueue getOfflineFileDeletes(boolean clearList) {
    FileRequestQueue deleteList = m_offlineDeleteList;
    
    if ( clearList == true)
      m_offlineDeleteList = null;
    
    return deleteList;
  }

  /**
   * Add an offline file delete request
   * 
   * @param deleteReq DeleteFileRequest
   */
  public synchronized final void addOfflineFileDelete(DeleteFileRequest deleteReq) {
    if ( m_offlineDeleteList == null)
      m_offlineDeleteList = new FileRequestQueue();
    m_offlineDeleteList.addRequest( deleteReq);
  }
  
  /**
   * Determine if the connection has a file state cache
   * 
   * @return boolean
   */
  public final boolean hasStateCache() {
    return m_stateCache != null ? true : false;
  }

  /**
   * Return the file state cache
   * 
   * @return FileStateCache
   */
  public final FileStateCache getStateCache() {
    return m_stateCache;
  }

  /**
   * Enable/disable the file state cache
   * 
   * @param ena boolean
   */
  public final void enableStateCache(boolean ena) {
    if ( ena == true) {
     if ( m_stateCache == null)
        m_stateCache = new FileStateCache();
    }
    else
      m_stateCache = null;
  }
  
  /**
   * Remove expired file states from the state cache
   * 
   * @return int
   */
  public final int removeExpiredFileStates() {
    
    //  Check if there is a file state cache
    
    if ( hasStateCache() == false)
      return 0;
      
    //  Remove expired file states from the state cache
    
    return m_stateCache.removeExpiredFileStates();
  }
  
	/**
	 * File state has expired. The listener can control whether the file state is removed
	 * from the cache, or not.
	 * 
	 * @param state FileState
	 * @return true to remove the file state from the cache, or false to leave the file state in the cache
	 */
	public boolean fileStateExpired(FileState state) {
		
		//	Check if the file state has an associated file
		
		String tempName = (String) state.findAttribute(DBNetworkFile.DBCacheFile);
		
		if ( tempName != null) {
			
			//	Check if the file is open
			
			if ( state.getOpenCount() > 0) {
				
				//	Do not expire the file state yet as the temporary file is in use
				
				return false;
			}
			else {
				
				//	Delete the temporary file
				
				File tempFile = new File(tempName);
				tempFile.delete();
				
				//	Debug

				if ( m_debug)				
					Debug.println("$$ Deleted temporary file " + tempName + " (Expired) $$");
			}
		}
			
		//	File is not open or does not have a temporary file, file state can be expired
		
		return true;
	}
	
	/**
	 * File state cache is closing down, any resources attached to the file state must be released.
	 * 
	 * @param state FileState
	 */
	public void fileStateClosed(FileState state) {

		//	Check if the file state has an associated file
		
		String tempName = (String) state.findAttribute(DBNetworkFile.DBCacheFile);
		
		if ( tempName != null) {
			
			//	Delete the temporary file
			
			File tempFile = new File(tempName);
			tempFile.delete();
				
			//	Debug

			if ( m_debug)			
				Debug.println("$$ Deleted temporary file " + tempName + " (Closed) $$");
		}
	}
	
	/**
	 * Return the JDBC context as a string
	 * 
	 * @return String
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		
		str.append("[");
		str.append(getDBInterface().getDBInterfaceName());
		str.append(",");
		str.append(getFileLoaderClass());
		str.append("]");
		
		return str.toString();
	}
	
	/**
	 * Close the device context
	 */
	public void CloseContext() {

		//	Close the file loader
		
		if ( getFileLoader() != null)
			getFileLoader().shutdownLoader(false);

		//	Close the database interface
		
		if ( getDBInterface() != null)
		  getDBInterface().shutdownDatabase( this);
		
    //  Check if the file state cache is enabled, if so then release the file states and associated
    //  resources.
    
    if ( hasStateCache()) {
      m_stateCache.removeAllFileStates();
      getStateCache().shutdownRequest();
    }

		//	Call the base class
		
		super.CloseContext();
	}
	
	/**
	 * Start the shared filesystem, perform startup processing here.
	 * 
	 * @param disk DiskSharedDevice
	 * @throws DeviceContextException
	 */
	public void startFilesystem(DiskSharedDevice disk)
		throws DeviceContextException {
			
		//	Start the quota manager, if configured
		
		if ( hasQuotaManager()) {
			
			try {

				//	Start the quota manager
				
				getQuotaManager().startManager(disk.getDiskInterface(), this);
			}
			catch (QuotaManagerException ex) {
				throw new DeviceContextException(ex.toString());
			}
		}
	}

}
