/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.repo.activities.feed.cleanup;

import java.sql.SQLException;
import java.util.Date;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.domain.activities.ActivityFeedDAO;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.VmShutdownListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionException;

/**
 * The feed cleaner component is responsible for purging 'obsolete' activity feed entries
 */
public class FeedCleaner
{
    private static Log logger = LogFactory.getLog(FeedCleaner.class);
    
    private static VmShutdownListener vmShutdownListener = new VmShutdownListener(FeedCleaner.class.getName());
    
    private int maxAgeMins = 0;
    
    private ActivityFeedDAO feedDAO;
    
    public void setFeedDAO(ActivityFeedDAO feedDAO)
    {
        this.feedDAO = feedDAO;
    }
    
    public void setMaxAgeMins(int mins)
    {
        this.maxAgeMins = mins;
    }
    
    /**
     * Perform basic checks to ensure that the necessary dependencies were injected.
     */
    private void checkProperties()
    {
        PropertyCheck.mandatory(this, "feedDAO", feedDAO);
        
        // check the max age
        if (maxAgeMins <= 0)
        {
            throw new AlfrescoRuntimeException("Property 'maxAgeMins' must be greater than 0");
        }
    }
        
    public void execute() throws JobExecutionException
    {
        checkProperties();
        try
        { 
            long nowTimeOffset = new Date().getTime();
            long keepTimeOffset = nowTimeOffset - ((long)maxAgeMins*60000L); // millsecs = mins * 60 secs * 1000 msecs
            Date keepDate = new Date(keepTimeOffset);
             
            // clean old entries   
            int deletedCount = feedDAO.deleteFeedEntries(keepDate);
            
            if (logger.isDebugEnabled())
            {
                
                logger.debug("Cleaned " + deletedCount + " entries (upto " + keepDate + ", max age " + maxAgeMins + " mins)");
            }
        }
        catch (SQLException e)
        {
            logger.error("Exception during cleanup of feeds", e);
            throw new JobExecutionException(e);
        }
        catch (Throwable e)
        {
            // If the VM is shutting down, then ignore
            if (vmShutdownListener.isVmShuttingDown())
            {
                // Ignore
            }
            else
            {
                logger.error("Exception during cleanup of feeds", e);
            }
        }
    }
}
