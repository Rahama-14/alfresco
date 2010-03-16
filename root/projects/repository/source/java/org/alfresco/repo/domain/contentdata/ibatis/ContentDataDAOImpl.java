/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.domain.contentdata.ibatis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.ibatis.IdsEntity;
import org.alfresco.repo.domain.contentdata.AbstractContentDataDAOImpl;
import org.alfresco.repo.domain.contentdata.ContentDataEntity;
import org.alfresco.repo.domain.contentdata.ContentUrlEntity;
import org.alfresco.service.cmr.repository.ContentData;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.alfresco.util.Pair;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import com.ibatis.sqlmap.client.event.RowHandler;

/**
 * iBatis-specific implementation of the ContentData DAO.
 * 
 * @author Derek Hulley
 * @since 3.2
 */
public class ContentDataDAOImpl extends AbstractContentDataDAOImpl
{
    private static final String SELECT_CONTENT_URL_BY_ID = "alfresco.content.select_ContentUrlById";
    private static final String SELECT_CONTENT_URL_BY_KEY = "alfresco.content.select_ContentUrlByKey";
    private static final String SELECT_CONTENT_URL_BY_KEY_UNREFERENCED = "alfresco.content.select_ContentUrlByKeyUnreferenced";
    private static final String SELECT_CONTENT_URLS_BY_ORPHAN_TIME = "alfresco.content.select_ContentUrlByOrphanTime";
    private static final String SELECT_CONTENT_DATA_BY_ID = "alfresco.content.select_ContentDataById";
    private static final String SELECT_CONTENT_DATA_BY_NODE_AND_QNAME = "alfresco.content.select_ContentDataByNodeAndQName";
    private static final String INSERT_CONTENT_URL = "alfresco.content.insert_ContentUrl";
    private static final String INSERT_CONTENT_DATA = "alfresco.content.insert_ContentData";
    private static final String UPDATE_CONTENT_URL_ORPHAN_TIME = "alfresco.content.update_ContentUrlOrphanTime";
    private static final String UPDATE_CONTENT_DATA = "alfresco.content.update_ContentData";
    private static final String DELETE_CONTENT_DATA = "alfresco.content.delete_ContentData";
    private static final String DELETE_CONTENT_URLS = "alfresco.content.delete_ContentUrls";
    
    private SqlMapClientTemplate template;

    public void setSqlMapClientTemplate(SqlMapClientTemplate sqlMapClientTemplate)
    {
        this.template = sqlMapClientTemplate;
    }

    public Pair<Long, String> createContentUrlOrphaned(String contentUrl)
    {
        ContentUrlEntity contentUrlEntity = new ContentUrlEntity();
        contentUrlEntity.setContentUrl(contentUrl);
        contentUrlEntity.setSize(0L);
        contentUrlEntity.setOrphanTime(System.currentTimeMillis());
        Long id = (Long) template.insert(INSERT_CONTENT_URL, contentUrlEntity);
        // Done
        return new Pair<Long, String>(id, contentUrl);
    }

    @Override
    protected ContentUrlEntity createContentUrlEntity(String contentUrl, long size)
    {
        ContentUrlEntity contentUrlEntity = new ContentUrlEntity();
        contentUrlEntity.setContentUrl(contentUrl);
        contentUrlEntity.setSize(size);
        contentUrlEntity.setOrphanTime(null);
        /* Long id = (Long) */ template.insert(INSERT_CONTENT_URL, contentUrlEntity);
        /*contentUrlEntity.setId(id);*/
        // Register the url as new
        registerNewContentUrl(contentUrl);
        // Done
        return contentUrlEntity;
    }

    @Override
    protected ContentUrlEntity getContentUrlEntity(Long id)
    {
        ContentUrlEntity contentUrlEntity = new ContentUrlEntity();
        contentUrlEntity.setId(id);
        contentUrlEntity = (ContentUrlEntity) template.queryForObject(SELECT_CONTENT_URL_BY_ID, contentUrlEntity);
        // Done
        return contentUrlEntity;
    }

    @Override
    protected ContentUrlEntity getContentUrlEntity(String contentUrl)
    {
        ContentUrlEntity contentUrlEntity = new ContentUrlEntity();
        contentUrlEntity.setContentUrl(contentUrl);
        if (contentUrlEntity.getContentUrlShort() != null)
        {
            contentUrlEntity.setContentUrlShort(contentUrlEntity.getContentUrlShort().toLowerCase());
        }
        contentUrlEntity = (ContentUrlEntity) template.queryForObject(SELECT_CONTENT_URL_BY_KEY, contentUrlEntity);
        // Done
        return contentUrlEntity;
    }

    public void getContentUrlsOrphaned(final ContentUrlHandler contentUrlHandler, long maxOrphanTime)
    {
        RowHandler rowHandler = new RowHandler()
        {
            public void handleRow(Object valueObject)
            {
                ContentUrlEntity contentUrlEntity = (ContentUrlEntity) valueObject;
                contentUrlHandler.handle(
                        contentUrlEntity.getId(),
                        contentUrlEntity.getContentUrl(),
                        contentUrlEntity.getOrphanTime());
            }
        };
        ContentUrlEntity contentUrlEntity = new ContentUrlEntity();
        contentUrlEntity.setOrphanTime(maxOrphanTime);
        template.queryWithRowHandler(SELECT_CONTENT_URLS_BY_ORPHAN_TIME, contentUrlEntity, rowHandler);
    }
    
    @SuppressWarnings("unchecked")
    public void getContentUrlsOrphaned(final ContentUrlHandler contentUrlHandler, long maxOrphanTime, int maxResults)
    {
        ContentUrlEntity contentUrlEntity = new ContentUrlEntity();
        contentUrlEntity.setOrphanTime(maxOrphanTime);
        List<ContentUrlEntity> results = template.queryForList(
                SELECT_CONTENT_URLS_BY_ORPHAN_TIME,
                contentUrlEntity, 0, maxResults);
        // Pass the result to the callback
        for (ContentUrlEntity result : results)
        {
            contentUrlHandler.handle(
                    result.getId(),
                    result.getContentUrl(),
                    result.getOrphanTime());
        }
    }
    
    public int updateContentUrlOrphanTime(Long id, Long orphanTime)
    {
        ContentUrlEntity contentUrlEntity = new ContentUrlEntity();
        contentUrlEntity.setId(id);
        contentUrlEntity.setOrphanTime(orphanTime);
        return template.update(UPDATE_CONTENT_URL_ORPHAN_TIME, contentUrlEntity);
    }

    /**
     * {@inheritDoc}
     */
    public int deleteContentUrls(List<Long> ids)
    {
        return template.delete(DELETE_CONTENT_URLS, ids);
    }

    @Override
    protected ContentUrlEntity getContentUrlEntityUnreferenced(String contentUrl)
    {
        ContentUrlEntity contentUrlEntity = new ContentUrlEntity();
        contentUrlEntity.setContentUrl(contentUrl);
        if (contentUrlEntity.getContentUrlShort() != null)
        {
            contentUrlEntity.setContentUrlShort(contentUrlEntity.getContentUrlShort().toLowerCase());
        }
        contentUrlEntity = (ContentUrlEntity) template.queryForObject(SELECT_CONTENT_URL_BY_KEY_UNREFERENCED, contentUrlEntity);
        // Done
        return contentUrlEntity;
    }

    @Override
    protected ContentDataEntity createContentDataEntity(
            Long contentUrlId,
            Long mimetypeId,
            Long encodingId,
            Long localeId)
    {
        ContentDataEntity contentDataEntity = new ContentDataEntity();
        contentDataEntity.setVersion(ContentDataEntity.CONST_LONG_ZERO);
        contentDataEntity.setContentUrlId(contentUrlId);
        contentDataEntity.setMimetypeId(mimetypeId);
        contentDataEntity.setEncodingId(encodingId);
        contentDataEntity.setLocaleId(localeId);
        try
        {
            template.insert(INSERT_CONTENT_DATA, contentDataEntity);
        }
        catch (Throwable e)
        {
            throw new AlfrescoRuntimeException("Failed to insert ContentData: " + contentDataEntity, e);
        }
        // Done
        return contentDataEntity;
    }

    @Override
    protected ContentDataEntity getContentDataEntity(Long id)
    {
        Map<String, Object> params = new HashMap<String, Object>(11);
        params.put("id", id);
        ContentDataEntity contentDataEntity = (ContentDataEntity) template.queryForObject(SELECT_CONTENT_DATA_BY_ID, params);
        // Done
        return contentDataEntity;
    }

    @Override
    protected int updateContentDataEntity(ContentDataEntity entity)
    {
        entity.incrementVersion();
        return template.update(UPDATE_CONTENT_DATA, entity);
    }

    @Override
    protected int deleteContentDataEntity(Long id)
    {
        // Get the content urls
        try
        {
            ContentData contentData = getContentData(id).getSecond();
            String contentUrl = contentData.getContentUrl();
            if (contentUrl != null)
            {
                // It has been dereferenced and may be orphaned - we'll check later
                registerDereferencedContentUrl(contentUrl);
            }
        }
        catch (DataIntegrityViolationException e)
        {
            // Doesn't exist.  The node doesn't enforce a FK constraint, so we protect against this.
        }
        // Issue the delete statement
        Map<String, Object> params = new HashMap<String, Object>(11);
        params.put("id", id);
        return template.delete(DELETE_CONTENT_DATA, params);
    }

    public void deleteContentDataForNode(Long nodeId, Set<Long> qnameIds)
    {
        if (qnameIds.size() == 0)
        {
            // There will be no results
            return;
        }
        IdsEntity idsEntity = new IdsEntity();
        idsEntity.setIdOne(nodeId);
        idsEntity.setIds(new ArrayList<Long>(qnameIds));
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) template.queryForList(SELECT_CONTENT_DATA_BY_NODE_AND_QNAME, idsEntity);
        // Delete each one
        for (Long id : ids)
        {
            try
            {
                // Delete the ContentData entity
                deleteContentData(id);
            }
            catch (ConcurrencyFailureException e)
            {
                // The DB may return results even though the row has just been
                // deleted.  Since we are deleting the row, it doesn't matter
                // if it is deleted here or not.
            }
        }
    }
}
