/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
import java.util.List;

import org.alfresco.repo.domain.activities.ActivityFeedDAO;
import org.alfresco.repo.domain.activities.ActivityFeedEntity;
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
    
    private int maxFeedSize = -1; //unlimited
    
    private ActivityFeedDAO feedDAO;
    
    public void setFeedDAO(ActivityFeedDAO feedDAO)
    {
        this.feedDAO = feedDAO;
    }
    
    public void setMaxAgeMins(int mins)
    {
        this.maxAgeMins = mins;
    }
    
    // note: this relates to user feed size (across all sites) or site feed size - for each format
    public void setMaxFeedSize(int size)
    {
        this.maxFeedSize = size;
    }
    
    /**
     * Perform basic checks to ensure that the necessary dependencies were injected.
     */
    private void checkProperties()
    {
        PropertyCheck.mandatory(this, "feedDAO", feedDAO);
        
        // check the max age and max feed size
        if ((maxAgeMins <= 0) && (maxFeedSize <= 0))
        {
            logger.warn("Neither maxAgeMins or maxFeedSize set - feeds will not be cleaned");
        }
    }
        
    public int execute() throws JobExecutionException
    {
        checkProperties();
        
        int maxAgeDeletedCount = 0;
        int maxSizeDeletedCount = 0;
        
        try
        {
            if (maxAgeMins > 0)
            {
                // clean old entries based on maxAgeMins
                
                long nowTimeOffset = new Date().getTime();
                long keepTimeOffset = nowTimeOffset - ((long)maxAgeMins*60000L); // millsecs = mins * 60 secs * 1000 msecs
                Date keepDate = new Date(keepTimeOffset);
                
                maxAgeDeletedCount = feedDAO.deleteFeedEntries(keepDate);
                
                if (maxAgeDeletedCount > 0)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Cleaned " + maxAgeDeletedCount + " entries (upto " + keepDate + ", max age " + maxAgeMins + " mins)");
                    }
                }
                else
                {
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Cleaned " + maxAgeDeletedCount + " entries (upto " + keepDate + ", max age " + maxAgeMins + " mins)");
                    }
                }
            }
            
            if (maxFeedSize > 0)
            {
                // clean old entries based on maxFeedSize
                
                // return candidate feeds to clean - either site+format or user+format
                List<ActivityFeedEntity> feeds = feedDAO.selectFeedsToClean(maxFeedSize);
                
                int feedCount = 0;
                
                for (ActivityFeedEntity feed : feeds)
                {
                    String siteId = feed.getSiteNetwork();
                    String feedUserId = feed.getFeedUserId();
                    String format = feed.getActivitySummaryFormat();
                    
                    List<ActivityFeedEntity> feedToClean;
                    
                    if ((feedUserId == null) || (feedUserId.length() == 0))
                    {
                        feedToClean = feedDAO.selectSiteFeedEntries(siteId, format);
                    }
                    else
                    {
                        feedToClean = feedDAO.selectUserFeedEntries(feedUserId, format, null, false, false);
                    }
                    
                    if (feedToClean.size() > maxFeedSize)
                    {
                        Date oldestFeedEntry = feedToClean.get(maxFeedSize-1).getPostDate();
                        
                        int deletedCount = 0;
                        
                        if ((feedUserId == null) || (feedUserId.length() == 0))
                        {
                            deletedCount = feedDAO.deleteUserFeedEntries(feedUserId, format, oldestFeedEntry);
                        }
                        else
                        {
                            deletedCount = feedDAO.deleteSiteFeedEntries(siteId, format, oldestFeedEntry);
                        }
                        
                        
                        if (deletedCount > 0)
                        {
                            maxSizeDeletedCount = maxSizeDeletedCount + deletedCount;
                            feedCount++;
                            
                            if (logger.isTraceEnabled())
                            {
                                logger.trace("Cleaned " + deletedCount + " entries for ["+feed.getSiteNetwork()+", "+feed.getFeedUserId()+", "+feed.getActivitySummaryFormat()+"] (upto " + oldestFeedEntry + ")");
                            }
                        }
                    }
                }
                
                if (maxSizeDeletedCount > 0)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Cleaned " + maxSizeDeletedCount + " entries across " + feedCount + " feeds (max feed size "+maxFeedSize+" entries)");
                    }
                }
                else
                {
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Cleaned " + maxSizeDeletedCount + " entries across " + feedCount + " feeds (max feed size "+maxFeedSize+" entries)");
                    }
                }
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
        
        return (maxAgeDeletedCount + maxSizeDeletedCount);
    }
}
