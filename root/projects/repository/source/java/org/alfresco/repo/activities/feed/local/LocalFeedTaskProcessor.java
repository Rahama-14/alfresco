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
package org.alfresco.repo.activities.feed.local;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.activities.feed.FeedTaskProcessor;
import org.alfresco.repo.activities.feed.RepoCtx;
import org.alfresco.repo.domain.activities.ActivityFeedDAO;
import org.alfresco.repo.domain.activities.ActivityFeedEntity;
import org.alfresco.repo.domain.activities.ActivityPostDAO;
import org.alfresco.repo.domain.activities.ActivityPostEntity;
import org.alfresco.repo.domain.activities.FeedControlDAO;
import org.alfresco.repo.domain.activities.FeedControlEntity;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.template.ClassPathRepoTemplateLoader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.ibatis.sqlmap.client.SqlMapClient;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;

/**
 * The local (ie. not grid) feed task processor is responsible for processing the individual feed job
 */
public class LocalFeedTaskProcessor extends FeedTaskProcessor implements ApplicationContextAware
{
    private static final Log logger = LogFactory.getLog(LocalFeedTaskProcessor.class);
    
    private ActivityPostDAO postDAO;
    private ActivityFeedDAO feedDAO;
    private FeedControlDAO feedControlDAO;
    
    // can call locally (instead of remote repo callback)
    private SiteService siteService;
    private NodeService nodeService;
    private ContentService contentService;
    private String defaultEncoding;
    private List<String> templateSearchPaths;
    private boolean useRemoteCallbacks;
    private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();    
    
    // used to start/end/commit transaction
    // note: currently assumes that all dao services are configured with this mapper / data source
    private SqlMapClient sqlMapper;

    public void setPostDAO(ActivityPostDAO postDAO)
    {
        this.postDAO = postDAO;
    }
    
    public void setFeedDAO(ActivityFeedDAO feedDAO)
    {
        this.feedDAO = feedDAO;
    }
    
    public void setFeedControlDAO(FeedControlDAO feedControlDAO)
    {
        this.feedControlDAO = feedControlDAO;
    }
    
    public void setSiteService(SiteService siteService)
    {
        this.siteService = siteService;
    }
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }
    
    public void setDefaultEncoding(String defaultEncoding)
    {
        this.defaultEncoding = defaultEncoding;
    }
    
    public void setTemplateSearchPaths(List<String> templateSearchPaths)
    {
        this.templateSearchPaths = templateSearchPaths;
    }
    
    public void setUseRemoteCallbacks(boolean useRemoteCallbacks)
    {
        this.useRemoteCallbacks = useRemoteCallbacks;
    }
    
    public void setSqlMapClient(SqlMapClient sqlMapper)
    {
        this.sqlMapper = sqlMapper;
    }
            
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.resolver = applicationContext;
    }

    public void startTransaction() throws SQLException
    {
        sqlMapper.startTransaction();
    }
    
    public void commitTransaction() throws SQLException
    {
        sqlMapper.commitTransaction();
    }
    
    public void endTransaction() throws SQLException
    {
        sqlMapper.endTransaction();
    }
    
    public List<ActivityPostEntity> selectPosts(ActivityPostEntity selector) throws SQLException
    {
        return postDAO.selectPosts(selector);
    }
    
    public long insertFeedEntry(ActivityFeedEntity feed) throws SQLException
    {
        return feedDAO.insertFeedEntry(feed);
    }
    
    public int updatePostStatus(long id, ActivityPostEntity.STATUS status) throws SQLException
    {
        return postDAO.updatePostStatus(id, status);
    }
    
    public List<FeedControlEntity> selectUserFeedControls(String userId) throws SQLException
    {
       return feedControlDAO.selectFeedControls(userId);
    }
    
    @Override
    protected Set<String> getSiteMembers(final RepoCtx ctx, final String siteId) throws Exception
    {
        if (useRemoteCallbacks)
        {
            // as per 3.0, 3.1
            return super.getSiteMembers(ctx, siteId);
        }
        else
        {
            // optimise for non-remote implementation - override remote repo callback (to "List Site Memberships" web script) with embedded call
            return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Set<String>>()
            {
                public Set<String> doWork() throws Exception
                {
                    Set<String> members = new HashSet<String>();
                    if ((siteId != null) && (siteId.length() != 0))
                    {
                        Map<String, String> mapResult = siteService.listMembers(siteId, null, null, 0, true);
                        
                        if ((mapResult != null) && (mapResult.size() != 0))
                        {
                            for (String userName : mapResult.keySet())
                            {
                                if (! ctx.isUserNamesAreCaseSensitive())
                                {
                                    userName = userName.toLowerCase();
                                }
                                members.add(userName);
                            }
                        }
                    }
                    
                    return members;
                }
            }, AuthenticationUtil.getSystemUserName());
        }
    }
    
    @Override
    protected Map<String, List<String>> getActivityTypeTemplates(String repoEndPoint, String ticket, String subPath) throws Exception
    {
        if (useRemoteCallbacks)
        {
            // as per 3.0, 3.1
            return super.getActivityTypeTemplates(repoEndPoint, ticket, subPath);
        }
        else
        {
            // optimisation - override remote repo callback (to "Activities Templates" web script) with local/embedded call
            
            String path = "/";
            String templatePattern = "*.ftl";
            
            if ((subPath != null) && (subPath.length() > 0))
            {
                subPath = subPath + "*";
                
                int idx = subPath.lastIndexOf("/");
                if (idx != -1)
                {
                    path = subPath.substring(0, idx);
                    templatePattern = subPath.substring(idx+1) + ".ftl";
                }
            }
            
            List<String> allTemplateNames = getDocumentPaths(path, false, templatePattern);
            
            return getActivityTemplates(allTemplateNames);
        }
    }
    
    @Override
    protected Configuration getFreemarkerConfiguration(RepoCtx ctx)
    {
        if (useRemoteCallbacks)
        {
            // as per 3.0, 3.1
            return super.getFreemarkerConfiguration(ctx);
        }
        else
        {
            Configuration cfg = new Configuration();
            cfg.setObjectWrapper(new DefaultObjectWrapper());
            
            cfg.setTemplateLoader(new ClassPathRepoTemplateLoader(nodeService, contentService, defaultEncoding));
            
            // TODO review i18n
            cfg.setLocalizedLookup(false);
            
            return cfg;
        }
    }
    
    // Helper to get template document paths
    private List<String> getDocumentPaths(String path, boolean includeSubPaths, String documentPattern)
    {
        if ((path == null) || (path.length() == 0))
        {
            path = "/";
        }
        
        if (! path.startsWith("/"))
        {
            path = "/" + path;
        }
        
        if (! path.endsWith("/"))
        {
            path = path + "/";
        }
        
        if ((documentPattern == null) || (documentPattern.length() == 0))
        {
            documentPattern = "*";
        }
        
        List<String> documentPaths = new ArrayList<String>(0);
        
        for (String classPath : templateSearchPaths)
        {
            final StringBuilder pattern = new StringBuilder(128);
            pattern.append("classpath*:").append(classPath)
                   .append(path)
                   .append((includeSubPaths ? "**/" : ""))
                   .append(documentPattern);
            
            try
            {
                documentPaths.addAll(getPaths(pattern.toString(), classPath));
            }
            catch (IOException e)
            {
                // Note: Ignore: no documents found
            }
        }
        
        return documentPaths;
    }
    
    // Helper to return a list of resource document paths based on a search pattern.
    private List<String> getPaths(String pattern, String classPath) throws IOException
    {
        Resource[] resources = resolver.getResources(pattern);
        List<String> documentPaths = new ArrayList<String>(resources.length);
        for (Resource resource : resources)
        {
            String resourcePath = resource.getURL().toExternalForm();
            
            int idx = resourcePath.lastIndexOf(classPath);
            if (idx != -1)
            {
                String documentPath = resourcePath.substring(idx);
                documentPath = documentPath.replace('\\', '/');
                if (logger.isTraceEnabled())
                {
                    logger.trace("Item resource path: " + resourcePath + " , item path: " + documentPath);
                }
                documentPaths.add(documentPath);
            }
        }
        return documentPaths;
    }
}
