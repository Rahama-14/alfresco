/*
 * Copyright (C) 2005-2008 Alfresco, Inc.
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
package org.alfresco.jlan.smb.server;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.server.core.NoPooledMemoryException;
import org.alfresco.jlan.server.memory.ByteBufferPool;

/**
 * CIFs Packet Pool Class
 * 
 * <p>Allocates buffers from the main byte buffer pool and wraps them in a CIFS specific packet.
 * 
 * @author gkspencer
 */
public class CIFSPacketPool {

	// Constants
	
	public static final long CIFSAllocateWaitTime	= 250;	// milliseconds
	
	// Main byte buffer pool
	
	private ByteBufferPool m_bufferPool;
	
	// Debug enable
	
	private boolean m_debug;
	
	// Allow over sized packet allocations, maximum over sized packet size to allow
	
	private boolean m_allowOverSize = true;
	private int m_maxOverSize = 128 * 1024;		// 128K
	
	// Maximum buffer size that the pool provides
	
	private int m_maxPoolBufSize;
	
	/**
	 * Class constructor
	 * 
	 * @param bufPool byteBufferPool
	 */
	public CIFSPacketPool( ByteBufferPool bufPool) {
		m_bufferPool = bufPool;
		
		// Set the maximum pooled buffer size
		
		m_maxPoolBufSize = m_bufferPool.getLargestSize();
	}
	
	/**
	 * Allocate a CIFS packet with the specified buffer size
	 * 
	 * @param reqSiz int
	 * @return SMBSrvPacket
	 * @exception NoPooledMemoryException
	 */
	public final SMBSrvPacket allocatePacket( int reqSiz)
		throws NoPooledMemoryException {
		
		// Check if the buffer can be allocated from the pool

		byte[] buf = null;
		
		if ( reqSiz <= m_maxPoolBufSize) {
			
			// Allocate the byte buffer for the CIFS packet
			
			buf = m_bufferPool.allocateBuffer( reqSiz, CIFSAllocateWaitTime);
		}

		// Check if over sized allocations are allowed
		
		else if ( allowsOverSizedAllocations() && reqSiz <= getMaximumOverSizedAllocation()) {
				
			// DEBUG
			
			if ( Debug.EnableDbg && hasDebug())
				Debug.println("[SMB] Allocating an over-sized packet, reqSiz=" + reqSiz);
			
			// Allocate an over sized packet
			
			buf = new byte[reqSiz];
		}

		// Check if the buffer was allocated
		
		if ( buf == null) {
			
			// DEBUG
		
			if ( Debug.EnableDbg && hasDebug())
				Debug.println("[SMB] CIFS Packet allocate failed, reqSiz=" + reqSiz);
			
			// Throw an exception, no memory available
			
			throw new NoPooledMemoryException( "Request size " + reqSiz);
		}
		
		// Create the CIFS packet
		
		return new SMBSrvPacket( buf);
	}
	
	/**
	 * Allocate a CIFS packet with the specified buffer size, copy the header from the
	 * request packet
	 * 
	 * @param reqSiz int
	 * @param reqPkt SMBSrvPacket
	 * @return SMBSrvPacket
	 * @exception NoPooledMemoryException
	 */
	public final SMBSrvPacket allocatePacket( int reqSiz, SMBSrvPacket reqPkt)
		throws NoPooledMemoryException {

		// Allocate a new packet, copy the standard header length
		
		return allocatePacket( reqSiz, reqPkt, -1);
	}
	
	/**
	 * Allocate a CIFS packet with the specified buffer size, copy the header from the
	 * request packet
	 * 
	 * @param reqSiz int
	 * @param reqPkt SMBSrvPacket
	 * @param copyLen int
	 * @return SMBSrvPacket
	 * @exception NoPooledMemoryException
	 */
	public final SMBSrvPacket allocatePacket( int reqSiz, SMBSrvPacket reqPkt, int copyLen)
		throws NoPooledMemoryException {

		// Allocate the response packet
		
		SMBSrvPacket respPkt = allocatePacket( reqSiz);

		// Copy the header from the request to the response
		
		System.arraycopy( reqPkt.getBuffer(), 4, respPkt.getBuffer(), 4, copyLen == -1 ? SMBSrvPacket.HeaderLength : copyLen);

		// Attach the response packet to the request
		
		reqPkt.setAssociatedPacket( respPkt);
		
		// DEBUG
		
		if ( Debug.EnableDbg && hasDebug())
			Debug.println("[SMB]  Associated packet reqSiz=" + reqSiz + " with pktSiz=" + reqPkt.getBuffer().length);
		
		// Return the new packet
		
		return respPkt;
	}
	
	/**
	 * Release a CIFS packet buffer back to the pool
	 * 
	 * @param smbPkt SMBSrvPacket
	 */
	public final void releasePacket( SMBSrvPacket smbPkt) {
		
		// TEST
		//
		// Check if the packet is queued for async I/O
		
		if ( smbPkt.isQueuedForAsyncIO())
			Debug.println("*** Packet queued for async I/O, pkt=" + smbPkt);

		// Check if the packet is an over sized packet, just let the garbage collector pick it up
		
		if ( smbPkt.getBuffer().length <= m_maxPoolBufSize) {
			
			// Release the buffer from the CIFS packet back to the pool
			
			m_bufferPool.releaseBuffer( smbPkt.getBuffer());
		
			// DEBUG
			
			if ( Debug.EnableDbg && hasDebug() && smbPkt.hasAssociatedPacket() == false)
				Debug.println("[SMB] CIFS Packet released bufSiz=" + smbPkt.getBuffer().length);
		}
		else if ( Debug.EnableDbg && hasDebug())
			Debug.println("[SMB] Over sized packet left for garbage collector");
		
		// Check if the packet has an associated packet which also needs releasing
		
		if ( smbPkt.hasAssociatedPacket()) {

			// Check if the associated packet is using an over sized packet
			
			byte[] assocBuf = smbPkt.getAssociatedPacket().getBuffer();
			if ( assocBuf.length <= m_maxPoolBufSize) {
				
				// Release the associated packets buffer back to the pool
				
				m_bufferPool.releaseBuffer( smbPkt.getAssociatedPacket().getBuffer());
	
				// DEBUG
				
				if ( Debug.EnableDbg && hasDebug())
					Debug.println("[SMB] CIFS Packet released bufSiz=" + smbPkt.getBuffer().length + " and assoc packet, bufSiz=" + smbPkt.getAssociatedPacket().getBuffer().length);
			}
			else if ( Debug.EnableDbg && hasDebug())
				Debug.println("[SMB] Over sized associated packet left for garbage collector");
			
			// Clear the associated packet
			
			smbPkt.clearAssociatedPacket();
		}
	}
	
	/**
	 * Return the length of the smallest packet size available
	 * 
	 * @return int
	 */
	public final int getSmallestSize() {
		return m_bufferPool.getSmallestSize();
	}
	
	/**
	 * Return the length of the largest packet size available
	 * 
	 * @return int
	 */
	public final int getLargestSize() {
		return m_bufferPool.getLargestSize();
	}

	/**
	 * Check if over sized packet allocations are allowed
	 * 
	 * @return boolean
	 */
	public final boolean allowsOverSizedAllocations() {
		return m_allowOverSize;
	}
	
	/**
	 * Return the maximum size of over sized packet that is allowed
	 * 
	 * @return int
	 */
	public final int getMaximumOverSizedAllocation() {
		return m_maxOverSize;
	}
	
	/**
	 * Enable/disable debug output
	 * 
	 * @param ena boolean
	 */
	public final void setDebug( boolean ena) {
		m_debug = ena;
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
	 * Enable/disable over sized packet allocations
	 * 
	 * @param ena
	 */
	public final void setAllowOverSizedAllocations(boolean ena) {
		m_allowOverSize = ena;
	}
	
	/**
	 * Return the packet pool details as a string
	 * 
	 * @return String
	 */
	public String toString() {
		return m_bufferPool.toString();
	}
 }
 
