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

package org.alfresco.jlan.oncrpc.nfs;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * NFS Server Session Table Class
 *
 * @author gkspencer
 */
public class NFSSessionTable {

	//	Session list
	
	private Hashtable<Object, NFSSrvSession> m_sessions;
	
	/**
	 * Class constructor
	 */
	public NFSSessionTable() {
		m_sessions = new Hashtable<Object, NFSSrvSession>();
	}
	
	/**
	 * Return the number of sessions in the list
	 * 
	 * @return int
	 */
	public final int numberOfSessions() {
		return m_sessions.size();
	}
	
	/**
	 * Add a session to the list
	 * 
	 * @param sess NFSSrvSession
	 */
	public final void addSession(NFSSrvSession sess) {
		m_sessions.put(sess.getAuthIdentifier(), sess);
	}
	
	/**
	 * Find the session using the authentication identifier
	 * 
	 * @param authIdent Object
	 * @return NFSSrvSession
	 */
	public final NFSSrvSession findSession(Object authIdent) {
		return m_sessions.get(authIdent);
	}
	
	/**
	 * Remove a session from the list
	 * 
	 * @param sess NFSSrvSession
	 * @return NFSSrvSession
	 */
	public final NFSSrvSession removeSession(NFSSrvSession sess) {
		return removeSession(sess.getAuthIdentifier());
	}
	
	/**
	 * Remove a session from the list
	 * 
	 * @param authIdent Object
	 * @return NFSSrvSession
	 */
	public final NFSSrvSession removeSession(Object authIdent) {
	  
		//	Find the required session
	  
		NFSSrvSession sess = findSession(authIdent);
	  
		//	Remove the session and return the removed session
	  
		m_sessions.remove(authIdent);
		return sess;
	}
	
	/**
	 * Enumerate the session ids
	 * 
	 * @return Enumeration
	 */
	public final Enumeration enumerate() {
		return m_sessions.keys();
	}
}
