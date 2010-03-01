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
package org.alfresco.jcr.item;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.alfresco.jcr.util.JCRProxyFactory;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.NodeService;

/**
 * Alfresco implementation of a JCR Lock
 * 
 * @author David Caruana
 */
public class LockImpl implements Lock
{
    
    private NodeImpl node;
    private Lock proxy = null;

    
    /**
     * Constructor
     *  
     * @param node  node holding lock
     */
    public LockImpl(NodeImpl node)
    {
        this.node = node;
    }

    /**
     * Create proxied JCR Lock
     * 
     * @return  lock
     */
    public Lock getProxy()
    {
        if (proxy == null)
        {
            proxy = (Lock)JCRProxyFactory.create(this, Lock.class, node.session); 
        }
        return proxy;
    }

    /*
     *  (non-Javadoc)
     * @see javax.jcr.lock.Lock#getLockOwner()
     */
    public String getLockOwner()
    {
        String lockOwner = null;
        NodeService nodeService = node.session.getRepositoryImpl().getServiceRegistry().getNodeService();
        if (nodeService.hasAspect(node.getNodeRef(), ContentModel.ASPECT_LOCKABLE))
        {
            lockOwner = (String)nodeService.getProperty(node.getNodeRef(), ContentModel.PROP_LOCK_OWNER);
        }
        return lockOwner;
    }

    /*
     *  (non-Javadoc)
     * @see javax.jcr.lock.Lock#isDeep()
     */
    public boolean isDeep()
    {
        return false;
    }

    /*
     *  (non-Javadoc)
     * @see javax.jcr.lock.Lock#getNode()
     */
    public Node getNode()
    {
        return node.getProxy();
    }

    /*
     *  (non-Javadoc)
     * @see javax.jcr.lock.Lock#getLockToken()
     */
    public String getLockToken()
    {
        LockService lockService = node.session.getRepositoryImpl().getServiceRegistry().getLockService();
        LockStatus lockStatus = lockService.getLockStatus(node.getNodeRef());
        return lockStatus.equals(LockStatus.LOCK_OWNER) ? node.getNodeRef().toString() : null;
    }

    /*
     *  (non-Javadoc)
     * @see javax.jcr.lock.Lock#isLive()
     */
    public boolean isLive() throws RepositoryException
    {
        return getLockToken() == null ? false : true;
    }

    /*
     *  (non-Javadoc)
     * @see javax.jcr.lock.Lock#isSessionScoped()
     */
    public boolean isSessionScoped()
    {
        return false;
    }

    /*
     *  (non-Javadoc)
     * @see javax.jcr.lock.Lock#refresh()
     */
    public void refresh() throws LockException, RepositoryException
    {
        // note: for now, this is a noop
    }

}
