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
package org.alfresco.repo.domain.hibernate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.repo.domain.Node;
import org.alfresco.repo.domain.UsageDelta;
import org.alfresco.repo.domain.UsageDeltaDAO;
import org.alfresco.repo.node.db.NodeDaoService;
import org.alfresco.repo.transaction.TransactionalDao;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.alfresco.util.ParameterCheck;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Hibernate-specific implementation of the persistence-independent <b>Usage Delta</b> DAO interface
 * 
 */
public class HibernateUsageDeltaDAO extends HibernateDaoSupport implements UsageDeltaDAO, TransactionalDao
{
//    private static final String QUERY_GET_DELTAS = "usage.GetDeltas";
    private static final String QUERY_GET_TOTAL_DELTA_SIZE = "usage.GetTotalDeltaSize";
    private static final String QUERY_GET_USAGE_DELTA_NODES = "usage.GetUsageDeltaNodes";
    private static final String QUERY_DELETE_DELTAS_FOR_NODE = "usage.DeleteUsageDeltasForNode";
        
    /** a uuid identifying this unique instance */
    private final String uuid;
    private NodeDaoService nodeDaoService;
    
    public void setNodeDaoService(NodeDaoService nodeDaoService)
    {
        this.nodeDaoService = nodeDaoService;
    }

    /**
     * 
     */
    public HibernateUsageDeltaDAO()
    {
        this.uuid = GUID.generate();
    }

    /**
     * Checks equality by type and uuid
     */
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        else if (!(obj instanceof HibernateUsageDeltaDAO))
        {
            return false;
        }
        HibernateUsageDeltaDAO that = (HibernateUsageDeltaDAO) obj;
        return this.uuid.equals(that.uuid);
    }
    
    /**
     * @see #uuid
     */
    public int hashCode()
    {
        return uuid.hashCode();
    }

    /**
     * NO-OP
     */
    public void beforeCommit()
    {
    }   

    /**
     * Does this <tt>Session</tt> contain any changes which must be
     * synchronized with the store?
     * 
     * @return true => changes are pending
     */
    public boolean isDirty()
    {
        // create a callback for the task
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                return session.isDirty();
            }
        };
        // execute the callback
        return ((Boolean)getHibernateTemplate().execute(callback)).booleanValue();
    }

    /**
     * Just flushes the session
     */
    public void flush()
    {
        getSession().flush();
    }
    
    private Long getNodeIdNotNull(NodeRef nodeRef) throws InvalidNodeRefException
    {
        ParameterCheck.mandatory("nodeRef", nodeRef);
        
        Pair<Long, NodeRef> nodePair = nodeDaoService.getNodePair(nodeRef);
        if (nodePair == null)
        {
            throw new InvalidNodeRefException("Node does not exist: " + nodeRef, nodeRef);
        }
        return nodePair.getFirst();
    }

    public int deleteDeltas(NodeRef nodeRef)
    {
        Long nodeId = getNodeIdNotNull(nodeRef);
        return deleteDeltas(nodeId);
    }
    
    @SuppressWarnings("unchecked")
    public int deleteDeltas(final Long nodeId)
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_DELETE_DELTAS_FOR_NODE);
                query.setParameter("nodeId", nodeId);
                return query.executeUpdate();
            }
        };
        
        // execute
        Integer delCount = (Integer) getHibernateTemplate().execute(callback);
        
        return delCount.intValue();
    }
    
    @SuppressWarnings("unchecked")
    public long getTotalDeltaSize(NodeRef nodeRef)
    {
        final Long nodeId = getNodeIdNotNull(nodeRef);
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_TOTAL_DELTA_SIZE);
                query.setParameter("nodeId", nodeId);
                query.setReadOnly(true);
                return query.uniqueResult();
            }
        };
        // execute read-only tx
        Long queryResult = (Long)getHibernateTemplate().execute(callback);
       
        return (queryResult == null ? 0 : queryResult);
    }
    
    public void insertDelta(NodeRef usageNodeRef, long deltaSize)
    {
        Long nodeId = getNodeIdNotNull(usageNodeRef);
        Node node = (Node) getHibernateTemplate().get(NodeImpl.class, nodeId);
        
        UsageDelta delta = new UsageDeltaImpl();
        // delta properties
        delta.setNode(node);
        delta.setDeltaSize(deltaSize);

        // Save
        getSession().save(delta);
    }
    
    @SuppressWarnings("unchecked")
    public Set<NodeRef> getUsageDeltaNodes()
    {
        HibernateCallback callback = new HibernateCallback()
        {
            public Object doInHibernate(Session session)
            {
                Query query = session.getNamedQuery(QUERY_GET_USAGE_DELTA_NODES);
                query.setReadOnly(true);
                return query.list();
            }
        };
        // execute read-only tx
        List<Node> queryResults = (List<Node>)getHibernateTemplate().execute(callback);
        Set<NodeRef> results = new HashSet<NodeRef>(queryResults.size(), 1.0F);
        for (Node node : queryResults)
        {
            results.add(node.getNodeRef());
        }
        return results;
    }
}