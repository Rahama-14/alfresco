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
package org.alfresco.repo.domain.activities.ibatis;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.repo.domain.activities.ActivityPostDAO;
import org.alfresco.repo.domain.activities.ActivityPostEntity;

public class ActivityPostDAOImpl extends IBatisSqlMapper implements ActivityPostDAO
{
    @SuppressWarnings("unchecked")
    public List<ActivityPostEntity> selectPosts(ActivityPostEntity activityPost) throws SQLException 
    {
        if ((activityPost.getJobTaskNode() != -1) &&
            (activityPost.getMinId() != -1) &&
            (activityPost.getMaxId() != -1) &&
            (activityPost.getStatus() != null))
        {
            return (List<ActivityPostEntity>)getSqlMapClient().queryForList("alfresco.activities.select_activity_posts", activityPost);
        }
        else if (activityPost.getStatus() != null)
        {
            return (List<ActivityPostEntity>)getSqlMapClient().queryForList("alfresco.activities.select_activity_posts_by_status_only", activityPost);
        }
        else
        {
            return new ArrayList<ActivityPostEntity>(0);
        }
    }

    public Long getMaxActivitySeq() throws SQLException 
    {
        return (Long)getSqlMapClient().queryForObject("alfresco.activities.select_activity_post_max_seq");
    }
    
    public Long getMinActivitySeq() throws SQLException 
    {
        return (Long)getSqlMapClient().queryForObject("alfresco.activities.select_activity_post_min_seq");
    }
    
    public Integer getMaxNodeHash() throws SQLException 
    {
        return (Integer)getSqlMapClient().queryForObject("alfresco.activities.select_activity_post_max_jobtasknode");
    }

    public int updatePost(long id, String siteNetwork, String activityData, ActivityPostEntity.STATUS status) throws SQLException
    {
        ActivityPostEntity post = new ActivityPostEntity();
        post.setId(id);
        post.setSiteNetwork(siteNetwork);
        post.setActivityData(activityData);
        post.setStatus(status.toString());
        post.setLastModified(new Date());
        
        return getSqlMapClient().update("alfresco.activities.update_activity_post_data", post);
    }
    
    public int updatePostStatus(long id, ActivityPostEntity.STATUS status) throws SQLException
    {
        ActivityPostEntity post = new ActivityPostEntity();
        post.setId(id);
        post.setStatus(status.toString());
        post.setLastModified(new Date());
        
        return getSqlMapClient().update("alfresco.activities.update_activity_post_status", post);
    }
    
    public int deletePosts(Date keepDate, ActivityPostEntity.STATUS status) throws SQLException
    {
        ActivityPostEntity params = new ActivityPostEntity();
        params.setPostDate(keepDate);
        params.setStatus(status.toString());
        
        return getSqlMapClient().delete("alfresco.activities.delete_activity_posts_older_than_date", params);
    }
    
    public long insertPost(ActivityPostEntity activityPost) throws SQLException
    {
        Long id = (Long)getSqlMapClient().insert("alfresco.activities.insert_activity_post", activityPost);
        return (id != null ? id : -1);
    }
}
