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

package org.alfresco.jlan.server.filesys.loader;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.server.filesys.TreeConnection;


/**
 * Write Request Class
 * 
 * <p>Contains the details of a write request to be performed using a background thread.
 *
 * @author gkspencer
 */
public class WriteRequest {

	//	Network file to write to
	
	private NetworkFile m_file;
	private TreeConnection m_conn;
	private DiskInterface m_disk;
	
	//	Write length and offset within the file
	
	private int m_writeLen;
	private long m_writeOff;
	
	//	Data buffer and offset of data within the buffer
	
	private byte[] m_buffer;
	private int m_dataOff;
	
	/**
	 * Class constructor
	 *
	 * @param file NetworkFile
	 * @param tree TreeConnection
	 * @param disk DiskInterface
	 * @param writeLen int
	 * @param writeOff long
	 * @param data byte[]
	 * @param dataOff int 
	 */
	public WriteRequest(NetworkFile file, TreeConnection tree, DiskInterface disk, int writeLen, long writeOff, byte[] data, int dataOff) {
		m_file = file;
		m_conn = tree;
		m_disk = disk;
		
		m_writeLen = writeLen;
		m_writeOff = writeOff;
		
		m_buffer  = data;
		m_dataOff = dataOff;
	}
	
	/**
	 * Return the network file
	 * 
	 * @return NetworkFile
	 */
	public final NetworkFile getFile() {
		return m_file;
	}

	/**
	 * Return the tree connection
	 * 
	 * @return TreeConnection
	 */
	public final TreeConnection getConnection() {
		return m_conn;
	}
	
	/**
	 * Return the disk interface
	 * 
	 * @return DiskInterface
	 */
	public final DiskInterface getDisk() {
		return m_disk;
	}
	
	/**
	 * Return the write length
	 * 
	 * @return int
	 */
	public final int getWriteLength() {
		return m_writeLen;
	}
	
	/**
	 * Return the file write position
	 * 
	 * @return long
	 */
	public final long getWriteOffset() {
		return m_writeOff;
	}
	
	/**
	 * Return the data buffer
	 * 
	 * @return byte[]
	 */
	public final byte[] getBuffer() {
		return m_buffer;
	}
	
	/**
	 * Return the data buffer offset
	 * 
	 * @return int
	 */
	public final int getDataOffset() {
		return m_dataOff;
	}
	
	/**
	 * Perform the write request, return the write length or -1 if an error occurs
	 * 
	 * @return int
	 */
	public final int doWrite() {
		
		int wlen = -1;
		
		try {
			
			//	Synchronize using the network file
			
			synchronized ( m_file) {
				
				//	Open the file
				
				m_file.openFile(false);
				
				//	Perform the write request
				
				wlen = m_disk.writeFile(null, m_conn, m_file, m_buffer, m_dataOff, m_writeLen, m_writeOff);
				
				//	Close the file
				
				m_file.closeFile();
			}
		}
		catch (Exception ex) {
			
			//	Debug
			
			Debug.println("ThreadedWriter error=" + ex.toString());
				
			//	Indicate that the write failed
			
			wlen = -1;
		}
		
		//	Check if there was a write error
		
		if ( wlen == -1)
			m_file.setDelayedWriteError(true);
		else
			m_file.incrementWriteCount();
			
		//	Return the actual write length
		
		return wlen;
	}
	
	/**
	 * Return the write request as a string
	 * 
	 * @return String
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		
		str.append("[");
		str.append(getFile().getFullName());
		str.append(":wlen=");
		str.append(getWriteLength());
		str.append(",woff=");
		str.append(getWriteOffset());
		str.append("]");
		
		return str.toString();
	}
}
