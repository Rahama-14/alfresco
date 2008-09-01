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

package org.alfresco.deployment.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.deployment.FSDeploymentRunnable;
import org.alfresco.deployment.impl.DeploymentException;
import org.alfresco.deployment.impl.server.DeploymentReceiverServiceImpl;
import org.alfresco.deployment.impl.server.Target;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a class to hold deployment receiver side configuration.
 * It is initialized by the usual Spring mechanism.
 * @author britt
 */
public class Configuration
{
    private String fMetaDataDirectory;
    
    private Map<String, Map<String, Object>> fTargetData;
    
    private Map<String, Target> fTargets;

    private String fLogDirectory;
    
    private String fDataDirectory;
 
    private static Log logger = LogFactory.getLog(Configuration.class);
    
    public Configuration()
    {
        fTargets = new HashMap<String, Target>();
    }
    
    public void setTargetData(Map<String, Map<String, Object>> targetData)
    {
        fTargetData = targetData;
    }

    public void setMetaDataDirectory(String dir)
    {
        fMetaDataDirectory = dir;
    }
    
    public void setLogDirectory(String logDirectory)
    {
        fLogDirectory = logDirectory;
    }
    
    public void setDataDirectory(String dataDirectory)
    {
        fDataDirectory = dataDirectory;
    }
    
    @SuppressWarnings("unchecked")
    public void init()
    {
    	
        // Create the various necessary directories if they don't already exits.
        File meta = new File(fMetaDataDirectory);
        if (!meta.exists())
        {
        	logger.info("creating meta data directory:" + meta.toString());
            meta.mkdirs();
        }
        File log = new File(fLogDirectory);
        if (!log.exists())
        {
         	logger.info("creating log data directory:" + log.toString());
            log.mkdirs();
        }
        File data = new File(fDataDirectory);
        if (!data.exists())
        {
        	logger.info("creating data directory:" + data.toString());
            data.mkdirs();
        }
        for (Map.Entry<String, Map<String, Object>> entry : fTargetData.entrySet())
        {
            Map<String, Object> targetEntry = entry.getValue();
            String targetName = entry.getKey();
            String root = (String)targetEntry.get("root");
            if (root == null)
            {
                throw new DeploymentException("No root specification for target " +
                                              targetName);
            }
            String user = (String)targetEntry.get("user");
            if (user == null)
            {
                throw new DeploymentException("No user specification for target " +
                                              targetName);
            }
            String password = (String)targetEntry.get("password");
            if (password == null)
            {
                throw new DeploymentException("No password specification for target " +
                                              targetName);
            }
            List<FSDeploymentRunnable> runnables = (List<FSDeploymentRunnable>)targetEntry.get("runnables");
            if (runnables == null)
            {
                runnables = new ArrayList<FSDeploymentRunnable>();
            }
            
            // Create the root directory if it does not already exist
            File rootFile = new File(root); 
            if(!rootFile.exists()){
            	logger.info("creating root data directory:" + rootFile.toString());
            	rootFile.mkdirs(); 
            }
            
            fTargets.put(targetName, new Target(targetName,
                                                root,
                                                fMetaDataDirectory + File.separator + targetName + ".md",
                                                runnables,
                                                user,
                                                password));
        }
    }
    
    /**
     * Get the directory in which metadata 
     * @return
     */
    public String getMetaDataDirectory()
    {
        return fMetaDataDirectory;
    }
    
    /**
     * Get the Target with the given name.
     * @param targetName
     * @return
     */
    public Target getTarget(String targetName)
    {
        return fTargets.get(targetName);
    }
    
    /**
     * Get the names of all the configured targets.
     * @return
     */
    public Set<String> getTargetNames()
    {
        return fTargets.keySet();
    }
    
    /**
     * Get the directory to which log (as in journal) files will be written.
     * @return
     */
    public String getLogDirectory()
    {
        return fLogDirectory;
    }
    
    /**
     * Get the directory to which work phase files get written.
     * @return
     */
    public String getDataDirectory()
    {
        return fDataDirectory;
    }
}
