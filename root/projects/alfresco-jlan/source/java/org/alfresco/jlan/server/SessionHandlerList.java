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

package org.alfresco.jlan.server;

import java.util.Vector;

/**
 * Session Handler List Class
 * 
 * @author gkspencer
 */
public class SessionHandlerList {

	// List of session handlers

	private Vector<SessionHandlerInterface> m_handlers;

	/**
	 * Default constructor
	 */
	public SessionHandlerList() {
		m_handlers = new Vector<SessionHandlerInterface>();
	}

	/**
	 * Add a handler to the list
	 * 
	 * @param handler SessionHandlerInterface
	 */
	public final void addHandler(SessionHandlerInterface handler) {
		m_handlers.add(handler);
	}

	/**
	 * Return the number of handlers in the list
	 * 
	 * @return int
	 */
	public final int numberOfHandlers() {
		return m_handlers.size();
	}

	/**
	 * Return the specified handler
	 * 
	 * @param idx int
	 * @return SessionHandlerInterface
	 */
	public final SessionHandlerInterface getHandlerAt(int idx) {

		// Range check the index

		if ( idx < 0 || idx >= m_handlers.size())
			return null;
		return m_handlers.get(idx);
	}

	/**
	 * Find the required handler by name
	 * 
	 * @param name String
	 * @return SessionHandlerInterface
	 */
	public final SessionHandlerInterface findHandler(String name) {

		// Search for the required handler

		for (int i = 0; i < m_handlers.size(); i++) {

			// Get the current handler

			SessionHandlerInterface handler = m_handlers.get(i);

			if ( handler.getHandlerName().equals(name))
				return handler;
		}

		// Handler not found

		return null;
	}

	/**
	 * Remove a handler from the list
	 * 
	 * @param idx int
	 * @return SessionHandlerInterface
	 */
	public final SessionHandlerInterface remoteHandler(int idx) {

		// Range check the index

		if ( idx < 0 || idx >= m_handlers.size())
			return null;

		// Remove the handler, and return it

		return m_handlers.remove(idx);
	}

	/**
	 * Remove a handler from the list
	 * 
	 * @param name String
	 * @return SessionHandlerInterface
	 */
	public final SessionHandlerInterface remoteHandler(String name) {

		// Search for the required handler

		for (int i = 0; i < m_handlers.size(); i++) {

			// Get the current handler

			SessionHandlerInterface handler = m_handlers.get(i);

			if ( handler.getHandlerName().equals(name)) {

				// Remove the handler from the list

				m_handlers.removeElementAt(i);
				return handler;
			}
		}

		// Handler not found

		return null;
	}

	/**
	 * Remove all handlers from the list
	 */
	public final void removeAllHandlers() {
		m_handlers.removeAllElements();
	}
	
	/**
	 * Wait for a session handler to be added to the list
	 * 
	 * @exception InterruptedException
	 */
	public final synchronized void waitWhileEmpty()
		throws InterruptedException {

		// Wait until a session handler is added to the list

		while (m_handlers.size() == 0)
			wait();
	}
}
