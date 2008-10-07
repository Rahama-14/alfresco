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
 * http://www.alfresco.com/legal/licensing
 */

package org.alfresco.deployment;

import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.alfresco.deployment.impl.server.DeploymentReceiverServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Main class for Deployment Receiver.
 * @author britt
 */
public class Main
{
	 private static Log logger = LogFactory.getLog(Main.class);
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.err.println("Usage: org.alfresco.deployment.Main application-context.xml");
            System.exit(1);
        }

        try 
        {
        	logger.info("Alfresco File System Receiver Starting");
        	@SuppressWarnings("unused")
        	FileSystemXmlApplicationContext context =
        		new FileSystemXmlApplicationContext(args[0]);
        }
        catch (Exception e)
        {
        	logger.error("Unable to start file system receiver", e);
        	System.err.println("Unable to start file system receiver");
        	e.printStackTrace();
        }
    }
}
