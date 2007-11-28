package org.alfresco.jlan.server.filesys;

/*
 * DiskInterface.java
 *
 * Copyright (c) 2004 Starlasoft. All rights reserved.
 */
 
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.core.DeviceContext;
import org.alfresco.jlan.server.core.DeviceInterface;

/**
 * The disk interface is implemented by classes that provide an interface for a disk type shared
 * device.
 */
public interface DiskInterface extends DeviceInterface {

  /**
   * Close the file.
   *
   * @param sess			Server session
   * @param tree     	Tree connection.
   * @param param   	Network file context.
   * @exception java.io.IOException    If an error occurs.
   */
  public void closeFile(SrvSession sess, TreeConnection tree, NetworkFile param)
  	throws java.io.IOException;

  /**
   * Create a new directory on this file system.
   *
   * @param sess			Server session
   * @param tree     	Tree connection.
   * @param params   	Directory create parameters
   * @exception java.io.IOException    If an error occurs.
   */
	public void createDirectory(SrvSession sess, TreeConnection tree, FileOpenParams params)
  	throws java.io.IOException;

  /**
   * Create a new file on the file system.
   *
   * @param sess			Server session
   * @param tree      Tree connection
   * @param params    File create parameters
   * @return NetworkFile
   * @exception java.io.IOException   If an error occurs.
   */
  public NetworkFile createFile(SrvSession sess, TreeConnection tree, FileOpenParams params)
  	throws java.io.IOException;

  /**
   * Delete the directory from the filesystem.
   *
   * @param sess			Server session
   * @param tree     	Tree connection
   * @param dir     	Directory name.
   * @exception java.io.IOException The exception description.
   */
  public void deleteDirectory(SrvSession sess, TreeConnection tree, String dir)
  	throws java.io.IOException;

  /**
   * Delete the specified file.
   *
   * @param sess			Server session
   * @param tree      Tree connection
   * @param name 			File name
   * @exception java.io.IOException The exception description.
   */
  public void deleteFile(SrvSession sess, TreeConnection tree, String name)
  	throws java.io.IOException;

  /**
   * Check if the specified file exists, and whether it is a file or directory.
   *
   * @param sess			Server session
   * @param tree 			Tree connection
   * @param name 			java.lang.String
   * @return int
   * @see FileStatus
   */
  int fileExists(SrvSession sess, TreeConnection tree, String name);

  /**
   * Flush any buffered output for the specified file.
   *
   * @param sess			Server session
   * @param tree      Tree connection
   * @param file     	Network file context.
   * @exception java.io.IOException The exception description.
   */
  public void flushFile(SrvSession sess, TreeConnection tree, NetworkFile file)
  	throws java.io.IOException;

  /**
   * Get the file information for the specified file.
   *
   * @param sess			Server session
   * @param tree     	Tree connection
   * @param name     	File name/path that information is required for.
   * @return         	File information if valid, else null
   * @exception java.io.IOException The exception description.
   */
  public FileInfo getFileInformation(SrvSession sess, TreeConnection tree, String name)
  	throws java.io.IOException;

  /**
   * Determine if the disk device is read-only.
   *
   * @param sess			Server session
   * @param ctx				Device context
   * @return boolean
   * @exception java.io.IOException  If an error occurs.
   */
  boolean isReadOnly(SrvSession sess, DeviceContext ctx)
  	throws java.io.IOException;

  /**
   * Open a file on the file system.
   *
   * @param sess			Server session
   * @param tree     	Tree connection
   * @param params 		File open parameters
   * @return NetworkFile
   * @exception java.io.IOException If an error occurs.
   */
  public NetworkFile openFile(SrvSession sess, TreeConnection tree, FileOpenParams params)
    throws java.io.IOException;

  /**
   * Read a block of data from the specified file.
   *
   * @param sess		Session details
   * @param tree		Tree connection
   * @param file		Network file
   * @param buf			Buffer to return data to
   * @param bufPos 	Starting position in the return buffer
   * @param siz			Maximum size of data to return
   * @param filePos	File offset to read data
   * @return Number of bytes read
   * @exception java.io.IOException The exception description.
   */
  public int readFile(SrvSession sess, TreeConnection tree, NetworkFile file, byte[] buf, int bufPos, int siz, long filePos)
    throws java.io.IOException;

  /**
   * Rename the specified file.
   *
   * @param sess			Server session
   * @param tree     	Tree connection
   * @param oldName 	java.lang.String
   * @param newName 	java.lang.String
   * @exception java.io.IOException The exception description.
   */
  public void renameFile(SrvSession sess, TreeConnection tree, String oldName, String newName)
    throws java.io.IOException;

  /**
   * Seek to the specified file position.
   *
   * @param sess			Server session
   * @param tree			Tree connection
   * @param file     	Network file.
   * @param pos     	Position to seek to.
   * @param typ      	Seek type.
   * @return         	New file position, relative to the start of file.
   */
  long seekFile(SrvSession sess, TreeConnection tree, NetworkFile file, long pos, int typ)
  	throws java.io.IOException;

  /**
   * Set the file information for the specified file.
   *
   * @param sess			Server session
   * @param tree     	Tree connection
   * @param name 			java.lang.String
   * @param info 			FileInfo
   * @exception java.io.IOException The exception description.
   */
  public void setFileInformation(SrvSession sess, TreeConnection tree, String name, FileInfo info)
    throws java.io.IOException;

  /**
   * Start a new search on the filesystem using the specified searchPath that may contain
   * wildcards.
   *
   * @return SearchContext
   * @param sess				Server session
   * @param tree     		Tree connection
   * @param searchPath  File(s) to search for, may include wildcards.
   * @param attrib      Attributes of the file(s) to search for, see class SMBFileAttribute.
   * @exception java.io.FileNotFoundException    If the search could not be started.
   */
  public SearchContext startSearch(SrvSession sess, TreeConnection tree, String searchPath, int attrib)
    throws java.io.FileNotFoundException;

	/**
	 * Truncate a file to the specified size
	 * 
   * @param sess	 Server session
   * @param tree   Tree connection
   * @param file   Network file details
   * @param siz    New file length
   * @exception java.io.IOException The exception description.
   */
  public void truncateFile(SrvSession sess, TreeConnection tree, NetworkFile file, long siz)
    throws java.io.IOException;
    
  /**
   * Write a block of data to the file.
   *
   * @param sess					Server session
   * @param tree         	Tree connection
   * @param file         	Network file details
   * @param buf byte[]  	Data to be written
   * @param bufoff      	Offset within the buffer that the data starts
   * @param siz int      	Data length
   * @param fileoff      	Position within the file that the data is to be written.
   * @return            	Number of bytes actually written
   * @exception java.io.IOException The exception description.
   */
  public int writeFile(SrvSession sess, TreeConnection tree, NetworkFile file, byte[] buf, int bufoff, int siz,
    									 long fileoff)
    throws java.io.IOException;
}