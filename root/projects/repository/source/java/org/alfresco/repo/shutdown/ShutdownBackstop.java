/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.repo.shutdown;

import org.alfresco.util.AbstractLifecycleBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * The Shutdown backstop is a spring bean that will ensure that shutdown completes 
 * within a given time.
 * 
 * If alfresco is blocked - for example waiting for a remote resource or a long 
 * running actionbackstop will wait for a timeout and then call System.exit()
 * 
 * @author mrogers
 *
 */
public class ShutdownBackstop extends AbstractLifecycleBean
{
	/**
	 * How long to go before shutdown (in ms)
	 * Default 10 Seconds.
	 */
	private int timeout = 10000;
	
	/**
	 * is the backstop enabled?
	 */
	private boolean enabled = true;
	
    protected final static Log log = LogFactory.getLog(ShutdownBackstop.class); 

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getTimeout() {
		return timeout;
	}

	@Override
	protected void onBootstrap(ApplicationEvent event) 
	{
		// Do logging here for side effect of initialising log object.
		if (log.isDebugEnabled())
		{
			log.debug("Shutdown backstop onBootstrap");
		}	
		
		// Nothing to do here
	}

	@Override
	protected void onShutdown(ApplicationEvent event) 
	{
		if(isEnabled())
		{
			log.info("Shutdown backstop timer started");
			Thread selfDestructThread = new ShutdownBackstopThread(timeout);
			selfDestructThread.start();
		}
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * This is a dangerous class!   It will kill the JVM after sleeping 
	 * for timeout ms.
	 * 
	 * It also dumps information to the system console prior to termination. 
	 */
	private class ShutdownBackstopThread extends Thread
	{
		int timeout;
		
		public ShutdownBackstopThread(int timeout)
		{
			this.timeout = timeout;
			this.setDaemon(true);
			this.setName("Alfresco Shutdown Backstop Thread (Self Destruct)");
		}
		
		public void run() 
        { 
			try 
			{
				sleep(timeout);
			} 
			catch (InterruptedException e) 
			{
				// nothing to do here
			}
			
			try 
			{
				log.error("Alfresco terminating via Shutdown Backstop");	
			} 
			catch (Throwable t)
			{
				// Do nothing
			}
			
			try 
			{
				// Try to dump the status of all threads
				SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
				String timeStr = sf.format(new Date());
				File f = new File("alf-backstop-" + timeStr + ".dmp");
				f.createNewFile();
				BufferedWriter w = new BufferedWriter(new FileWriter(f));
				w.write("Alfresco Shutdown Backstop Dump, time:" + timeStr);
				w.newLine();
				listAllThreads(w);
				w.close();
			} 
			catch (Throwable t)
			{
				System.out.println(t.toString());
				t.printStackTrace();
        	}

			try 
			{
				log.error("Alfresco terminated");
			}
			catch (Throwable t)
			{
				// Do nothing
			}
            System.exit(1);
        }
		
		private void printThreadInfo(Thread t, String indent, BufferedWriter w) throws IOException 
		{
			if (t == null){
				return;
			}
		
			w.write(indent + "Thread: " + t.getName() + "  Priority: "
			        + t.getPriority() + (t.isDaemon() ? " Daemon" : "")
			        + (t.isAlive() ? "" : " Not Alive"));
			w.newLine();
		}

	    /** 
	     * Display info about a thread group 
	     * @throws IOException 
	     */
		private void printGroupInfo(ThreadGroup g, String indent, BufferedWriter w) throws IOException 
		{
			if (g == null)
			{
			   return;
			}
			int numThreads = g.activeCount();
			int numGroups = g.activeGroupCount();
			Thread[] threads = new Thread[numThreads];
			ThreadGroup[] groups = new ThreadGroup[numGroups];

			g.enumerate(threads, false);
			g.enumerate(groups, false);

			w.append(indent + "Thread Group: " + g.getName()
			   + "  Max Priority: " + g.getMaxPriority()
			   + (g.isDaemon() ? " Daemon" : ""));
			w.newLine();

			for (int i = 0; i < numThreads; i++)
			{
			      printThreadInfo(threads[i], indent + "    ", w);
			}
			for (int i = 0; i < numGroups; i++)
			{
			      printGroupInfo(groups[i], indent + "    ", w);
			}
		}

	    /** 
	     * Find the root thread group and list it recursively 
	     * @throws IOException 
		 */
		public void listAllThreads(BufferedWriter w) throws IOException 
		{
			
	        ThreadGroup currentThreadGroup;
			ThreadGroup rootThreadGroup;
			ThreadGroup parent;

			// Get the current thread group
			currentThreadGroup = Thread.currentThread().getThreadGroup();

			// Now go find the root thread group
			rootThreadGroup = currentThreadGroup;
			parent = rootThreadGroup.getParent();
			while (parent != null) {
			      rootThreadGroup = parent;
			      parent = parent.getParent();
			}
			printGroupInfo(rootThreadGroup, "", w);
		}
	}
}
