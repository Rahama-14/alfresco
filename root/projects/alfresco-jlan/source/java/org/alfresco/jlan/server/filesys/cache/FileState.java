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

package org.alfresco.jlan.server.filesys.cache;

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.locking.FileLock;
import org.alfresco.jlan.locking.FileLockList;
import org.alfresco.jlan.locking.LockConflictException;
import org.alfresco.jlan.locking.NotLockedException;
import org.alfresco.jlan.server.filesys.FileName;
import org.alfresco.jlan.server.filesys.FileOpenParams;
import org.alfresco.jlan.server.filesys.FileStatus;
import org.alfresco.jlan.smb.SharingMode;

/**
 * File State Class
 * 
 * <p>Caches information about a file/directory so that the core server does not need
 * to make calls to the shared device driver.
 *
 * @author gkspencer
 */
public class FileState {

	//	File state constants
	
	public final static long NoTimeout			= -1L;
	public final static long DefTimeout			= 5 * 60000L;	// 5 minutes

	public final static int UnknownFileId				= -1;
	public final static int UnknownStreamCount	=	-1;
			
	//	File status codes

	public final static int FILE_LOADWAIT	= 0;
	public final static int FILE_LOADING	= 1;
	public final static int FILE_AVAILABLE= 2;
	public final static int FILE_UPDATED	= 3;
	public final static int FILE_SAVEWAIT	= 4;
	public final static int FILE_SAVING		= 5;
	public final static int FILE_SAVED		= 6;
	public final static int FILE_DELETED	= 7;

	//	File state names
	
	private static final String[] _fileStates = { "LoadWait", "Loading", "Available", "Updated", "SaveWait", "Saving", "Saved", "Deleted" };
		
	//	Standard file information keys
	
	public static final String FileInformation	= "FileInfo";
	public static final String StreamsList			= "StreamsList";
	
	//	File name/path
	
	private String m_path;
	
	//	File identifier
	
	private int m_fileId = UnknownFileId;
	
	//	File state timeout, -1 indicates no timeout
	
	private long m_tmo;
	
	//	File status, indicates if the file/folder exists and if it is a file or folder.
	//	Constants are defined in the FileStatus class.
	
	private int m_fileStatus;
	
	//	File data status
	
	private int m_status = FILE_AVAILABLE;
	
	//	Open file count
	
	private int m_openCount;
	
	//	Sharing mode
	
	private int m_sharedAccess = SharingMode.READWRITE;
	
	//	Cache of various file information
	
	private Hashtable m_cache;
	
	//	Count of streams associated with this file, -1 if not known
	
	private int m_streamCount = UnknownStreamCount;

	//	File lock list, allocated once there are active locks on this file
	
	private FileLockList m_lockList;
	
	//	Retention period expiry date/time
	
	private long m_retainUntil = -1L;
		
	/**
	 * Class constructor
	 * 
	 * @param fname String
	 */
	public FileState(String fname) {
	  
	  //	Normalize the file path
	  
	  setPath(fname);
	  setExpiryTime(System.currentTimeMillis() + DefTimeout);
	  
	  //	Set the file/folder status
	  
	  m_fileStatus = FileStatus.Unknown;
	}

	/**
	 * Class constructor
	 * 
	 * @param fname String
	 * @param status int
	 */
	public FileState(String fname, int status) {
	  
	  //	Normalize the file path
	  
	  setPath(fname);
	  setExpiryTime(System.currentTimeMillis() + DefTimeout);
	  
	  //	Set the file/folder status
	  
	  m_fileStatus = status;
	}

	/**
	 * Return the file name/path
	 * 
	 * @return String
	 */
	public final String getPath() {
	  return m_path;
	}
	
	/**
	 * 	Return the file exists state
	 * 
	 * @return boolean
	 */
	public final boolean fileExists() {
	  if ( m_fileStatus == FileStatus.FileExists || m_fileStatus == FileStatus.DirectoryExists)
	    return true;
	  return false;
	}
	
	/**
	 * Return the file status
	 * 
	 * @return int
	 */
	public final int getFileStatus() {
	  return m_fileStatus;
	}
	
	/**
	 * Return the directory state
	 * 
	 * @return boolean
	 */
	public final boolean isDirectory() {
	  return m_fileStatus == FileStatus.DirectoryExists ? true : false;
	}
	
	/**
	 * Return the file open count
	 * 
	 * @return int
	 */
	public final int getOpenCount() {
		return m_openCount;
	}

	/**
	 * Get the file id
	 * 
	 * @return int
	 */
	public final int getFileId() {
		return m_fileId;
	}

	/**
	 * Return the shared access mode
	 * 
	 * @return int
	 */
	public final int getSharedAccess() {
	  return m_sharedAccess;
	}
	
	/**
	 * Return the file status
	 * 
	 * @return int
	 */
	public final int getStatus() {
		return m_status;
	}
	
	/**
	 * Return the count of streams associated with this file, or -1 if not known
	 * 
	 * @return int
	 */
	public final int getStreamCount() {
		return m_streamCount;
	}

	/**
	 * Check if there are active locks on this file
	 * 
	 * @return boolean
	 */
	public final boolean hasActiveLocks() {
		if ( m_lockList != null && m_lockList.numberOfLocks() > 0)
			return true;
		return false;
	}
	
	/**
	 * Check if this file state does not expire
	 * 
	 * @return boolean
	 */
	public final boolean hasNoTimeout() {
		return m_tmo == NoTimeout ? true : false;
	}

	/**
	 * Check if the file/folder is under retention
	 * 
	 * @return boolean
	 */
	public final boolean hasActiveRetentionPeriod() {
	  if ( m_retainUntil == -1L)
	    return false;
	  return System.currentTimeMillis() < m_retainUntil ? true : false;
	}
	
	/**
	 * Get the retention period expiry date/time for the file/folder
	 * 
	 * @return long
	 */
	public final long getRetentionExpiryDateTime() {
	  return m_retainUntil;
	}
	
	/**
	 * Check if the file can be opened depending on any current file opens and the sharing mode of the
	 * first file open
	 * 
	 * @param params FileOpenParams
	 * @return boolean
	 */
	public final boolean allowsOpen( FileOpenParams params) {
	
	  //	If the file is not currently open then allow the file open
	  
	  if ( getOpenCount() == 0)
	    return true;
	  
	  //	Check the shared access mode
	  
	  if ( getSharedAccess() == SharingMode.READWRITE &&
	       params.getSharedAccess() == SharingMode.READWRITE)
	    return true;
	  else if (( getSharedAccess() & SharingMode.READ) != 0 &&
	      params.isReadOnlyAccess())
	    return true;
	  else if(( getSharedAccess() & SharingMode.WRITE) != 0 &&
	      params.isWriteOnlyAccess())
	    return true;
	  
	  //	Sharing violation, do not allow the file open
	  
	  return false;
	}
	
	/**
	 * Increment the file open count
	 * 
	 * @return int
	 */
	public final synchronized int incrementOpenCount() {
		m_openCount++;
		
		//	Debug
		
//		if ( m_openCount > 1)
//			Debug.println("@@@@@ File open name=" + getPath() + ", count=" + m_openCount);
		return m_openCount;
	}
	
	/**
	 * Decrement the file open count
	 * 
	 * @return int
	 */
	public final synchronized int decrementOpenCount() {
		
		//	Debug
		
		if ( m_openCount <= 0)
			Debug.println("@@@@@ File close name=" + getPath() + ", count=" + m_openCount + " <<ERROR>>");
		else
			m_openCount--;
			
		return m_openCount;
	}
	
	/**
	 * Check if the file state has expired
	 * 
	 * @param curTime long
	 * @return boolean
	 */
	public final boolean hasExpired(long curTime) {
	  if ( m_tmo == NoTimeout)
	  	return false;
	  if ( curTime > m_tmo)
	  	return true;
	  return false;
	}
	
	/**
	 * Return the number of seconds left before the file state expires
	 * 
	 * @param curTime long
	 * @return long
	 */
	public final long getSecondsToExpire(long curTime) {
		if ( m_tmo == NoTimeout)
			return -1;
		return ( m_tmo - curTime)/1000L;
	}
	
	/**
	 * Return a file status code as a string
	 * 
	 * @return String
	 */
	public final String getStatusAsString() {
		if ( m_status >= 0 && m_status < _fileStates.length)
			return _fileStates[m_status];
		return "Unknown";
	}
	
	/**
	 * Set the file status
	 * 
	 * @param status int
	 */
	public final void setFileStatus(int status) {
	  m_fileStatus = status;
	}
	
	/**
	 * Set the file identifier
	 * 
	 * @param id int
	 */
	public final void setFileId(int id) {
		m_fileId = id;
	}
	
	/**
	 * Set the file state expiry time
	 * 
	 * @param expire long
	 */
	public final void setExpiryTime(long expire) {
		m_tmo = expire;
	}

	/**
	 * Set the retention preiod expiry date/time
	 * 
	 * @param expires long
	 */
	public final void setRetentionExpiryDateTime(long expires) {
	  m_retainUntil = expires;
	}
	
	/**
	 * Set the shared access mode, from the first file open
	 * 
	 * @param mode int
	 */
	public final void setSharedAccess( int mode) {
	  if ( getOpenCount() == 0)
	    m_sharedAccess = mode;
	}
	
	/**
	 * Set the file status
	 * 
	 * @param sts int
	 */
	public final void setStatus(int sts) {
		m_status = sts;
	}

	/**
	 * Set the associated stream count
	 * 
	 * @param cnt int
	 */
	public final synchronized void setStreamCount(int cnt) {
		m_streamCount = cnt;			
	}
	
	/**
	 * Add an attribute to the file state
	 * 
	 * @param name String
	 * @param attr Object
	 */
	public final synchronized void addAttribute(String name, Object attr) {
	  if ( m_cache == null)
	  	m_cache = new Hashtable();
	  m_cache.put(name,attr);
	}
	
	/**
	 * Find an attribute
	 * 
	 * @param name String
	 * @return Object
	 */
	public final Object findAttribute(String name) {
	  if ( m_cache == null)
	  	return null;
	  return m_cache.get(name);
	}
	
	/**
	 * Remove an attribute from the file state
	 * 
	 * @param name String
	 * @return Object
	 */
	public final synchronized Object removeAttribute(String name) {
	  if ( m_cache == null)
	  	return null;
	  return m_cache.remove(name);
	}
	
	/**
	 * Remove all attributes from the file state
	 */
	public final synchronized void removeAllAttributes() {
	  if ( m_cache != null)
	  	m_cache.clear();
	  m_cache = null;
	}

	/**
	 * Set the file path
	 * 
	 * @param path String
	 */
	public final void setPath(String path) {
		
		//	Split the path into directories and file name, only uppercase the directories to normalize
		//	the path.

		m_path = normalizePath(path);		
	}

	/**
	 * Return the count of active locks on this file
	 *
	 * @return int
	 */	
	public final int numberOfLocks() {
		if ( m_lockList != null)
			return m_lockList.numberOfLocks();
		return 0;
	}
	
	/**
	 * Add a lock to this file
	 *
	 * @param lock FileLock
	 * @exception LockConflictException
	 */
	public final void addLock(FileLock lock)
		throws LockConflictException {
			
		//	Check if the lock list has been allocated
		
		if ( m_lockList == null) {
			
			synchronized (this) {
				
				//	Allocate the lock list, check if the lock list has been allocated elsewhere
				//	as we may have been waiting for the lock
				
				if ( m_lockList == null)
					m_lockList = new FileLockList();
			}
		}
		
		//	Add the lock to the list, check if there are any lock conflicts
		
		synchronized (m_lockList) {
			
			//	Check if the new lock overlaps with any existing locks
			
			if ( m_lockList.allowsLock(lock)) {
				
				//	Add the new lock to the list
				
				m_lockList.addLock(lock);
			}
			else
				throw new LockConflictException();
		}
	}
	
	/**
	 * Remove a lock on this file
	 * 
	 * @param lock FileLock
	 * @exception NotLockedException
	 */
	public final void removeLock(FileLock lock)
		throws NotLockedException {
			
		//	Check if the lock list has been allocated
		
		if ( m_lockList == null)
			throw new NotLockedException();
			
		//	Remove the lock from the active list
		
		synchronized ( m_lockList) {
			
			//	Remove the lock, check if we found the matching lock
			
			if ( m_lockList.removeLock(lock) == null)
				throw new NotLockedException();
		}
	}

	/**
	 * Check if the file is readable for the specified section of the file and process id
	 * 
	 * @param offset long
	 * @param len long
	 * @param pid int
	 * @return boolean
	 */
	public final boolean canReadFile(long offset, long len, int pid) {
		
		//	Check if the lock list is valid
		
		if ( m_lockList == null)
			return true;
			
		//	Check if the file section is readable by the specified process

		boolean readOK = false;
				
		synchronized ( m_lockList) {

			//	Check if the file section is readable
			
			readOK = m_lockList.canReadFile(offset, len, pid);						
		}
		
		//	Return the read status
		
		return readOK;
	}
	
	/**
	 * Check if the file is writeable for the specified section of the file and process id
	 * 
	 * @param offset long
	 * @param len long
	 * @param pid int
	 * @return boolean
	 */
	public final boolean canWriteFile(long offset, long len, int pid) {
		
		//	Check if the lock list is valid
		
		if ( m_lockList == null)
			return true;
			
		//	Check if the file section is writeable by the specified process

		boolean writeOK = false;
				
		synchronized ( m_lockList) {

			//	Check if the file section is writeable
			
			writeOK = m_lockList.canWriteFile(offset, len, pid);						
		}
		
		//	Return the write status
		
		return writeOK;
	}

	/**
	 * Normalize the path to uppercase the directory names and keep the case of the file name.
	 * 
	 * @param path String
	 * @return String
	 */
	public final static String normalizePath(String path) {	
		
		//	Split the path into directories and file name, only uppercase the directories to normalize
		//	the path.

		String normPath = path;
    
		if ( path.length() > 3) {
			
			//	Split the path to seperate the folders/file name
			
			int pos = path.lastIndexOf(FileName.DOS_SEPERATOR);
			if ( pos != -1) {
				
				//	Get the path and file name parts, normalize the path
				
				String pathPart = path.substring(0, pos).toUpperCase();
				String namePart = path.substring(pos);
				
				//	Rebuild the path string
				
				normPath = pathPart + namePart;
			}
		}
		
		//	Return the normalized path
		
		return normPath;
	}

	/**
	 * Dump the attributes that are attached to the file state
	 * 
	 * @param out PrintStream
	 */
	public final void DumpAttributes(PrintStream out) {
	
	  //	Check if there are any attributes
	  
	  if ( m_cache != null) {

	    //	Enumerate the available attribute objects
	    
	    Enumeration names = m_cache.keys();
	    
	    while ( names.hasMoreElements()) {
	      
	      //	Get the current attribute name
	      
	      String name = (String) names.nextElement();
	      
	      //	Get the associated attribute object
	      
	      Object attrib = m_cache.get(name);
	      
	      //	Output the attribute details
	      
	      out.println("++    " + name + " : " + attrib);
	    }
	  }
	  else
	    out.println("++    No Attributes");
	}
	
	/**
	 * Return the file state as a string
	 * 
	 * @return String
	 */
	public String toString() {
	  StringBuffer str = new StringBuffer();
	  
	  str.append("[");
	  str.append(getPath());
	  str.append(",");
	  str.append(FileStatus.asString(getFileStatus()));
	  str.append(":Opn=");
	  str.append(getOpenCount());
	  str.append(",Str=");
	  str.append(getStreamCount());
	  str.append(":");
	  
	  str.append(",Fid=");
	  str.append(getFileId());

		str.append(",Expire=");
		str.append(getSecondsToExpire(System.currentTimeMillis()));
		
		str.append(",Sts=");
	  str.append(_fileStates[getStatus()]);

		str.append(",Locks=");
		str.append(numberOfLocks());
		
	  str.append("]");
	  
	  return str.toString();
	}
}

