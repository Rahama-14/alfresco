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

package org.alfresco.jlan.server.thread;

import java.util.Vector;

import org.alfresco.jlan.debug.Debug;

/**
 * Thread Request Pool Class
 * 
 * <p>
 * Thread pool that processes a queue of thread requests.
 * 
 * @author gkspencer
 */
public class ThreadRequestPool {

	// Default/minimum/maximum number of worker threads to use

	public static final int DefaultWorkerThreads = 25;
	public static final int MinimumWorkerThreads = 4;
	public static final int MaximumWorkerThreads = 250;

	// Queue of requests

	private ThreadRequestQueue m_queue;

	// Worker threads

	private ThreadWorker[] m_workers;

	// Debug enable flag

	protected boolean m_debug;

	/**
	 * Thread Worker Inner Class
	 */
	protected class ThreadWorker implements Runnable {

		// Worker thread

		private Thread mi_thread;

		// Shutdown flag

		private boolean mi_shutdown = false;

		/**
		 * Class constructor
		 * 
		 * @param name String
		 */
		public ThreadWorker(String name) {

			// Create the worker thread

			mi_thread = new Thread(this);
			mi_thread.setName(name);
			mi_thread.setDaemon(true);
			mi_thread.start();
		}

		/**
		 * Request the worker thread to shutdown
		 */
		public final void shutdownRequest() {
			mi_shutdown = true;
			try {
				mi_thread.interrupt();
			}
			catch (Exception ex) {
			}
		}

		/**
		 * Run the thread
		 */
		public void run() {

			// Loop until shutdown

			ThreadRequest threadReq = null;
			
			while (mi_shutdown == false) {

				try {

					// Wait for an request to be queued

					threadReq = m_queue.removeRequest();
				}
				catch (InterruptedException ex) {

					// Check for shutdown

					if ( mi_shutdown == true)
						break;
				}
				catch ( Throwable ex2) {
					ex2.printStackTrace();
				}

				// If the request is valid process it

				if ( threadReq != null) {

					// DEBUG
					
					if ( hasDebug())
						Debug.println("Worker " + Thread.currentThread().getName() + ": Req=" + threadReq);
					
					try {

						// Process the request

						threadReq.runRequest();
					}
					catch (Throwable ex) {

						// Do not display errors if shutting down

						if ( mi_shutdown == false) {
							Debug.println("Worker " + Thread.currentThread().getName() + ":");
							Debug.println(ex);
						}
					}
				}
			}
		}
	};

	/**
	 * Class constructor
	 * 
	 * @param threadName String
	 */
	public ThreadRequestPool(String threadName) {
		this(threadName, DefaultWorkerThreads);
	}

	/**
	 * Class constructor
	 * 
	 * @param threadName String
	 * @param poolSize int
	 */
	public ThreadRequestPool(String threadName, int poolSize) {

		// Create the request queue

		m_queue = new ThreadRequestQueue();

		// Check that we have at least minimum worker threads

		if ( poolSize < MinimumWorkerThreads)
			poolSize = MinimumWorkerThreads;

		// Create the worker threads

		m_workers = new ThreadWorker[poolSize];

		for (int i = 0; i < m_workers.length; i++)
			m_workers[i] = new ThreadWorker(threadName + (i + 1));
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
	 * Return the number of requests in the queue
	 * 
	 * @return int
	 */
	public final int getNumberOfRequests() {
		return m_queue.numberOfRequests();
	}

	/**
	 * Queue a request to the thread pool for processing
	 * 
	 * @param req ThreadRequest
	 */
	public final void queueRequest(ThreadRequest req) {
		m_queue.addRequest( req);
	}

	/**
	 * Queue a number of requests to the thread pool for processing
	 * 
	 * @param reqList Vector<ThreadRequest>
	 */
	public final void queueRequests( Vector<ThreadRequest> reqList) {
		m_queue.addRequests( reqList);
	}
	
	/**
	 * Shutdown the thread pool and release all resources
	 */
	public void shutdownThreadPool() {

		// Shutdown the worker threads

		if ( m_workers != null) {
			for (int i = 0; i < m_workers.length; i++)
				m_workers[i].shutdownRequest();
		}
	}
	
	/**
	 * Enable/dsiable debug output
	 * 
	 * @param ena boolean
	 */
	public final void setDebug( boolean ena) {
		m_debug = ena;
	}
}
