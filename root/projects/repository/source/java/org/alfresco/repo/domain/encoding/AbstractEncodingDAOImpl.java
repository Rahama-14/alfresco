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
package org.alfresco.repo.domain.encoding;

import java.io.Serializable;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.util.Pair;
import org.springframework.extensions.surf.util.ParameterCheck;

/**
 * Abstract implementation for Encoding DAO.
 * <p>
 * This provides basic services such as caching, but defers to the underlying implementation
 * for CRUD operations. 
 * 
 * @author Derek Hulley
 * @since 3.2
 */
public abstract class AbstractEncodingDAOImpl implements EncodingDAO
{
    private static final Long CACHE_NULL_LONG = Long.MIN_VALUE;
    private SimpleCache<Serializable, Serializable> encodingEntityCache;

    /**
     * 
     * @param encodingEntityCache           the cache of IDs to mimetypes
     */
    public void setEncodingEntityCache(SimpleCache<Serializable, Serializable> encodingEntityCache)
    {
        this.encodingEntityCache = encodingEntityCache;
    }

    public Pair<Long, String> getEncoding(Long id)
    {
        // Check the cache
        String encoding = (String) encodingEntityCache.get(id);
        if (encoding != null)
        {
            return new Pair<Long, String>(id, encoding);
        }
        // Get it from the DB
        EncodingEntity mimetypeEntity = getEncodingEntity(id);
        if (mimetypeEntity == null)
        {
            throw new AlfrescoRuntimeException("The MimetypeEntity ID " + id + " doesn't exist.");
        }
        encoding = mimetypeEntity.getEncoding();
        // Cache it
        encodingEntityCache.put(encoding, id);
        encodingEntityCache.put(id, encoding);
        // Done
        return new Pair<Long, String>(id, encoding);
    }

    public Pair<Long, String> getEncoding(String encoding)
    {
        ParameterCheck.mandatory("encoding", encoding);
        
        // Check the cache
        Long id = (Long) encodingEntityCache.get(encoding);
        if (id != null)
        {
            if (id.equals(CACHE_NULL_LONG))
            {
                return null;
            }
            else
            {
                return new Pair<Long, String>(id, encoding);
            }
        }
        // It's not in the cache, so query
        EncodingEntity result = getEncodingEntity(encoding);
        if (result == null)
        {
            // Cache it
            encodingEntityCache.put(encoding, CACHE_NULL_LONG);
            // Done
            return null;
        }
        else
        {
            id = result.getId();
            // Cache it
            encodingEntityCache.put(id, encoding);
            encodingEntityCache.put(encoding, id);
            // Done
            return new Pair<Long, String>(id, encoding);
        }
    }

    public Pair<Long, String> getOrCreateEncoding(String encoding)
    {
        ParameterCheck.mandatory("encoding", encoding);
        
        Pair<Long, String> result = getEncoding(encoding);
        if (result == null)
        {
            EncodingEntity encodingEntity = createEncodingEntity(encoding);
            Long id = encodingEntity.getId();
            result = new Pair<Long, String>(id, encoding);
            // Cache it
            encodingEntityCache.put(id, encoding);
            encodingEntityCache.put(encoding, id);
        }
        return result;
    }
    
    /**
     * @param id            the ID of the encoding entity
     * @return              Return the entity or <tt>null</tt> if it doesn't exist
     */
    protected abstract EncodingEntity getEncodingEntity(Long id);
    protected abstract EncodingEntity getEncodingEntity(String encoding);
    protected abstract EncodingEntity createEncodingEntity(String encoding);
}
