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
 * http://www.alfresco.com/legal/licensing" */

package org.alfresco.repo.domain.hibernate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.domain.AccessControlListDAO;
import org.alfresco.repo.domain.DbAccessControlList;
import org.alfresco.repo.node.db.NodeDaoService;
import org.alfresco.repo.security.permissions.ACLType;
import org.alfresco.repo.security.permissions.impl.AclChange;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.Pair;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * The Node implementation for getting and setting ACLs.
 * 
 * @author britt
 */
public class NodeAccessControlListDAO extends HibernateDaoSupport implements AccessControlListDAO
{
    /**
     * The DAO for Nodes.
     */
    private NodeDaoService fNodeDAOService;

    /**
     * Default constructor.
     */
    public NodeAccessControlListDAO()
    {
    }

    public void setNodeDaoService(NodeDaoService nodeDAOService)
    {
        fNodeDAOService = nodeDAOService;
    }

    private Pair<Long, NodeRef> getNodePairNotNull(NodeRef nodeRef)
    {
        Pair<Long, NodeRef> nodePair = fNodeDAOService.getNodePair(nodeRef);
        if (nodePair == null)
        {
            throw new InvalidNodeRefException(nodeRef);
        }
        return nodePair;
    }
    
    public DbAccessControlList getAccessControlList(NodeRef nodeRef)
    {
        Pair<Long, NodeRef> nodePair = getNodePairNotNull(nodeRef);
        Long aclId = fNodeDAOService.getNodeAccessControlList(nodePair.getFirst());
        // Now get the entity
        DbAccessControlList acl;
        if (aclId == null)
        {
            return null;
        }
        else
        {
            acl = (DbAccessControlList) getHibernateTemplate().get(DbAccessControlListImpl.class, aclId);
        }
        return acl;
    }

    public void setAccessControlList(NodeRef nodeRef, DbAccessControlList acl)
    {
        Pair<Long, NodeRef> nodePair = getNodePairNotNull(nodeRef);
        Long aclId = (acl == null) ? null : acl.getId();
        fNodeDAOService.setNodeAccessControlList(nodePair.getFirst(), aclId);
    }

    public void updateChangedAcls(NodeRef startingPoint, List<AclChange> changes)
    {
        // Nothing to do here
    }

    public List<AclChange> setInheritanceForChildren(NodeRef parent, Long inheritFrom)
    {
        // Nothing to do here
        return Collections.<AclChange> emptyList();
    }

    public Long getIndirectAcl(NodeRef nodeRef)
    {
        DbAccessControlList acl = getAccessControlList(nodeRef);
        return (acl == null) ? null : acl.getId();
    }

    public Long getInheritedAcl(NodeRef nodeRef)
    {
        Pair<Long, NodeRef> nodePair = fNodeDAOService.getNodePair(nodeRef);
        if (nodePair == null)
        {
            throw new InvalidNodeRefException(nodeRef);
        }
        Pair<Long, ChildAssociationRef> caPair = fNodeDAOService.getPrimaryParentAssoc(nodePair.getFirst());
        if ((caPair != null) && (caPair.getSecond().getParentRef() != null))
        {
            Long aclId = fNodeDAOService.getNodeAccessControlList(caPair.getFirst());
            return aclId;
        }
        else
        {
            return null;
        }
    }

    public void forceCopy(NodeRef nodeRef)
    {
        // nothing to do;
    }

    public   Map<ACLType, Integer> patchAcls()
    {
        throw new UnsupportedOperationException();
    }

    public DbAccessControlList getAccessControlList(StoreRef storeRef)
    {
        throw new UnsupportedOperationException();
    }

    public void setAccessControlList(StoreRef storeRef, DbAccessControlList acl)
    {
        throw new UnsupportedOperationException();
    }

    public void setAccessControlList(NodeRef nodeRef, Long aclId)
    {
        throw new UnsupportedOperationException();
    }
}
