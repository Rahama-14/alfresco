/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.jcr.query;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.version.VersionException;

import org.alfresco.jcr.session.SessionImpl;
import org.alfresco.jcr.util.JCRProxyFactory;


/**
 * Alfresco implementation of JCR Query
 * 
 * @author David Caruana
 */
public abstract class QueryImpl implements Query
{
    /** Session */
    private SessionImpl session;

    /** Query Statement */
    private String statement;
    
    /** Proxy */
    private Query proxy = null;
    
    
    /**
     * Construct
     * 
     * @param statement  query language
     */
    public QueryImpl(SessionImpl session, String statement)
    {
        this.session = session;
        this.statement = statement;
    }

    /**
     * Get proxied JCR Query
     * 
     * @return  proxy
     */
    public Query getProxy()
    {
        if (proxy == null)
        {
            proxy = (Query)JCRProxyFactory.create(this, Query.class, session);
        }
        return proxy;
    }
    
    /**
     * Get Session
     * 
     * @return  session
     */
    public SessionImpl getSession()
    {
        return session;
    }
    
    /**
     * Is the statement valid?
     *
     * @throws InvalidQueryException
     */
    public abstract void isValidStatement() throws InvalidQueryException;
    
    /* (non-Javadoc)
     * @see javax.jcr.query.Query#getStatement()
     */
    public String getStatement()
    {
        return statement;
    }

    /* (non-Javadoc)
     * @see javax.jcr.query.Query#getStoredQueryPath()
     */
    public String getStoredQueryPath() throws ItemNotFoundException, RepositoryException
    {
        throw new ItemNotFoundException("This query has not been saved to the Repository");
    }

    /* (non-Javadoc)
     * @see javax.jcr.query.Query#storeAsNode(java.lang.String)
     */
    public Node storeAsNode(String absPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, UnsupportedRepositoryOperationException, RepositoryException
    {
        throw new UnsupportedRepositoryOperationException();
    }

}
