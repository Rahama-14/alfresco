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
package org.alfresco.repo.activities.script;

import java.util.List;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.service.cmr.activities.ActivityService;
import org.alfresco.service.cmr.activities.FeedControl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * Scripted Activity Service for posting activities.
 */

public final class Activity extends BaseScopableProcessorExtension
{
    private ActivityService activityService;

   /**
    * Set the activity service
    * 
    * @param activityService  the activity service
    */
   public void setActivityService(ActivityService activityService)
   {
      this.activityService = activityService;
   }
   
   
   /*
    * Post Activity
    */

   /**
    * Post a custom activity type
    *
    * @param activityType - required
    * @param siteId - optional, if null will be stored as empty string
    * @param appTool - optional, if null will be stored as empty string
    * @param jsonActivityData - required
    */
   public void postActivity(String activityType, String siteId, String appTool, String jsonActivityData)
   {
       activityService.postActivity(activityType, siteId, appTool, jsonActivityData);
   }
   
   /**
    * Post a pre-defined activity type - activity data will be looked-up asynchronously, including:
    * 
    *   name
    *   displayPath
    *   typeQName
    *   firstName (of posting user)
    *   lastName  (of posting user)
    * 
    * @param activityType - required
    * @param siteId - optional, if null will be stored as empty string
    * @param appTool - optional, if null will be stored as empty string
    * @param nodeRef - required - do not use for deleted (or about to be deleted) nodeRef
    */
   public void postActivity(String activityType, String siteId, String appTool, NodeRef nodeRef)
   {
       activityService.postActivity(activityType, siteId, appTool, nodeRef);
   }
   
   /**
    * Post a pre-defined activity type - eg. for checked-out nodeRef or renamed nodeRef
    * 
    * @param activityType - required
    * @param siteId - optional, if null will be stored as empty string
    * @param appTool - optional, if null will be stored as empty string
    * @param nodeRef - required - do not use deleted (or about to be deleted) nodeRef
    * @param beforeName - optional - name of node (eg. prior to name change)
    */
   public void postActivity(String activityType, String siteId, String appTool, NodeRef nodeRef, String beforeName)
   {
       activityService.postActivity(activityType, siteId, appTool, nodeRef, beforeName);
   }
   
   /**
    * Post a pre-defined activity type - eg. for deleted nodeRef
    *
    * @param activityType - required
    * @param siteId - optional, if null will be stored as empty string
    * @param appTool - optional, if null will be stored as empty string
    * @param nodeRef - required - can be a deleted (or about to be deleted) nodeRef
    * @param name - optional - name of name
    * @param typeQName - optional - type of node
    * @param parentNodeRef - required - used to lookup path/displayPath
    */
   public void postActivity(String activityType, String siteId, String appTool,  NodeRef nodeRef, String name, QName typeQName, NodeRef parentNodeRef)
   {
       activityService.postActivity(activityType, siteId, appTool, nodeRef, name, typeQName, parentNodeRef);
   }
   
   
   /*
    * Manage User Feed Controls
    */
   
   /**
    * For current user, get feed controls
    *
    * @return array of user feed controls
    */
   public FeedControl[] getFeedControls()
   {
       List<FeedControl> feedControls = activityService.getFeedControls();
       return (FeedControl[])feedControls.toArray(new FeedControl[feedControls.size()]);
   }
   
   /**
    * For current user, set feed control (opt-out) for a site or an appTool or a site/appTool combination
    *
    * @param siteId - required (optional, if appToolId is supplied)
    * @param appToolId - required (optional, if siteId is supplied)
    */
   public void setFeedControl(String siteId, String appToolId)
   {
       activityService.setFeedControl(new FeedControl(siteId, appToolId));
   }
   
   /**
    * For current user, unset feed control
    *
    * @param siteId - required (optional, if appToolId is supplied)
    * @param appToolId - required (optional, if siteId is supplied)
    */
   public void unsetFeedControl(String siteId, String appToolId)
   {
       activityService.unsetFeedControl(new FeedControl(siteId, appToolId));
   }
}
