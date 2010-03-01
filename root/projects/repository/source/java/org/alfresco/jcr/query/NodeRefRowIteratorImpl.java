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
package org.alfresco.jcr.query;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.alfresco.jcr.session.SessionImpl;
import org.alfresco.jcr.util.AbstractRangeIterator;
import org.alfresco.jcr.util.JCRProxyFactory;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;


/**
 * Row Iterator based on a list of Node References
 * 
 * @author David Caruana
 */
public class NodeRefRowIteratorImpl extends AbstractRangeIterator implements RowIterator
{
    private SessionImpl session;
    private Map<QName, PropertyDefinition> columns;
    private List<NodeRef> nodeRefs;
    private RowIterator proxy = null;
    
    /**
     * Construct
     * 
     * @param session
     * @param columnNames
     * @param nodeRefs
     */
    public NodeRefRowIteratorImpl(SessionImpl session, Map<QName, PropertyDefinition> columns, List<NodeRef> nodeRefs)
    {
        this.session = session;
        this.columns = columns;
        this.nodeRefs = nodeRefs;
    }

    /**
     * Get proxied JCR Query
     * 
     * @return  proxy
     */
    public RowIterator getProxy()
    {
        if (proxy == null)
        {
            proxy = (RowIterator)JCRProxyFactory.create(this, RowIterator.class, session);
        }
        return proxy;
    }
    
    /* (non-Javadoc)
     * @see javax.jcr.query.RowIterator#nextRow()
     */
    public Row nextRow()
    {
        long position = skip();
        NodeRef nodeRef = nodeRefs.get((int)position);
        NodeService nodeService = session.getRepositoryImpl().getServiceRegistry().getNodeService();        
        Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
        return new PropertyMapRowImpl(session, columns, nodeRef, properties);
    }

    /* (non-Javadoc)
     * @see javax.jcr.RangeIterator#getSize()
     */
    public long getSize()
    {
        return nodeRefs.size();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next()
    {
        return nextRow();
    }
    
}
