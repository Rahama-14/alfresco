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
package org.alfresco.cmis.search;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.alfresco.cmis.dictionary.CMISDictionaryService;
import org.alfresco.cmis.property.CMISPropertyService;
import org.alfresco.repo.search.impl.querymodel.Query;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;

/**
 * @author andyh
 */
public class CMISResultSetImpl implements CMISResultSet, Serializable
{
    private static final long serialVersionUID = 2014688399588268994L;

    private Map<String, ResultSet> wrapped;

    CMISQueryOptions options;

    NodeService nodeService;
    
    Query query;
    
    CMISDictionaryService cmisDictionaryService;
    
    CMISPropertyService cmisPropertyService;

    public CMISResultSetImpl(Map<String, ResultSet> wrapped, CMISQueryOptions options, NodeService nodeService, Query query, CMISDictionaryService cmisDictionaryService, CMISPropertyService cmisPropertyService)
    {
        this.wrapped = wrapped;
        this.options = options;
        this.nodeService = nodeService;
        this.query = query;
        this.cmisDictionaryService = cmisDictionaryService;
        this.cmisPropertyService = cmisPropertyService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISResultSet#close()
     */
    public void close()
    {
        for (ResultSet resultSet : wrapped.values())
        {
            resultSet.close();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISResultSet#getMetaData()
     */
    public CMISResultSetMetaData getMetaData()
    {
        return new CMISResultSetMetaDataImpl(options, query, cmisDictionaryService);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISResultSet#getRow(int)
     */
    public CMISResultSetRow getRow(int i)
    {
        return new CMISResultSetRowImpl(this, i, getScores(i), nodeService, getNodeRefs(i), query, cmisPropertyService, cmisDictionaryService);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISResultSet#hasMore()
     */
    public boolean hasMore()
    {
        for (ResultSet resultSet : wrapped.values())
        {
            if(resultSet.getResultSetMetaData().getLimitedBy() != LimitBy.UNLIMITED)
            {
                return true;
            }
        }
       return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISResultSet#length()
     */
    public int getLength()
    {
        for (ResultSet resultSet : wrapped.values())
        {
            return resultSet.length();
        }
        throw new IllegalStateException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISResultSet#start()
     */
    public int getStart()
    {
        return options.getSkipCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<CMISResultSetRow> iterator()
    {
        return new CMISResultSetRowIteratorImpl(this);
    }

    private Map<String, NodeRef> getNodeRefs(int i)
    {
        HashMap<String, NodeRef> refs = new HashMap<String, NodeRef>();
        for (String selector : wrapped.keySet())
        {
            ResultSet rs = wrapped.get(selector);
            refs.put(selector, rs.getNodeRef(i));
        }
        return refs;
    }

    private Map<String, Float> getScores(int i)
    {
        HashMap<String, Float> scores = new HashMap<String, Float>();
        for (String selector : wrapped.keySet())
        {
            ResultSet rs = wrapped.get(selector);
            scores.put(selector, Float.valueOf(rs.getScore(i)));
        }
        return scores;
    }

}
