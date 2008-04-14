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

package org.alfresco.jlan.oncrpc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.server.NetworkServer;
import org.alfresco.jlan.server.PacketHandlerInterface;
import org.alfresco.jlan.server.SocketSessionHandler;


/**
 * TCP RPC Session Handler Class
 * 
 * <p>Receives session requests via a TCP socketRPC requests via a datagram and passes the request to the registered RPC server.
 *
 * @author gkspencer
 */
public class TcpRpcSessionHandler extends SocketSessionHandler {

  //	RPC server implementation that handles the RPC processing
  
  private RpcProcessor m_rpcProcessor;
  
  //	Maximum request size allowed
  
  private int m_maxRpcSize;

  //	List of active sessions
  
  private Hashtable<Integer, TcpRpcPacketHandler> m_sessions;
  
  /**
   * Class constructor
   * 
   * @param name String
   * @param protocol String
   * @param rpcServer RpcProcessor
   * @param server NetworkServer
   * @param addr InetAddress
   * @param port int
   * @param maxSize int
   */
  public TcpRpcSessionHandler(String name, String protocol, RpcProcessor rpcServer, NetworkServer server,
      												 InetAddress addr, int port, int maxSize) {
    super(name, protocol, server, addr, port);

    //	Set the RPC server implementation that will handle the actual requests
    
    m_rpcProcessor = rpcServer;
    
    //	Set the maximum RPC request size allowed
    
    m_maxRpcSize = maxSize;
    
    //	Create the active session list
    
    m_sessions = new Hashtable<Integer, TcpRpcPacketHandler>();
  }
  
  /**
   * Return the maximum RPC size allowed
   * 
   * @return int
   */
  protected int getMaximumRpcSize() {
    return m_maxRpcSize;
  }

  /**
   * Return the RPC server used to process the requests
   * 
   * @return RpcProcessor
   */
  protected final RpcProcessor getRpcProcessor() {
    return m_rpcProcessor;
  }
  
  /**
   * Accept an incoming session request
   * 
   * @param sock Socket
   */
  protected void acceptConnection(Socket sock) {
    
    try {
      
      //	Set the socket for no delay
      
      sock.setTcpNoDelay(true);
      
	    //	Create a packet handler for the new session and add to the active session list
	    
	    int sessId = getNextSessionId();
	    TcpRpcPacketHandler pktHandler = createPacketHandler(sessId, sock);

	    //	Add the packet handler to the active session table
	    
	    m_sessions.put(new Integer(sessId), pktHandler);
	    
	    //	DEBUG
	    
	    if ( Debug.EnableInfo && hasDebug())
	      Debug.println("[" + getProtocolName() + "] Created new session id = " + sessId + ", from = " + sock.getInetAddress().getHostAddress() + ":" + sock.getPort());
    }
    catch (IOException ex) {
    }
  }
  
  /**
   * Remove a session from the active session list
   * 
   * @param sessId int
   */
  protected final void closeSession(int sessId) {
  
    //	Remove the specified session from the active session table
    
    PacketHandlerInterface pktHandler = m_sessions.remove(new Integer(sessId));
    if ( pktHandler != null) {
      
      //	Close the session
      
      pktHandler.closePacketHandler();
    }
  }
  
  /**
   * Close the session handler, close all active sessions.
   * 
   * @param server NetworkServer
   */
  public void closeSessionHandler(NetworkServer server) {
    super.closeSessionHandler(server);
    
    //	Close all active sessions
    
    if ( m_sessions.size() > 0) {

      //	Enumerate the sessions
      
      Enumeration<TcpRpcPacketHandler> enm = m_sessions.elements();
      
      while ( enm.hasMoreElements()) {
        
        //	Get the current packet handler
        
        PacketHandlerInterface handler = enm.nextElement();
        handler.closePacketHandler();
      }
      
      //	Clear the session list
      
      m_sessions.clear();
    }
  }
  
  /**
   * Create a packet handler for a new session
   * 
   * @param sessId int
   * @param sock Socket
   * @return TcpRpcPacketHandler
   * @exception IOException
   */
  protected TcpRpcPacketHandler createPacketHandler(int sessId, Socket sock)
  	throws IOException {
    
    //	Create a single threaded TCP RPC packet handler
    
    return new TcpRpcPacketHandler(this, sessId, m_rpcProcessor, sock, getMaximumRpcSize());    
  }
}
