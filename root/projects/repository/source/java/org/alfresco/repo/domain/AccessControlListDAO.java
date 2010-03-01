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
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>. */

package org.alfresco.repo.domain;

import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.permissions.ACLType;
import org.alfresco.repo.security.permissions.impl.AclChange;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

/**
 * This abstracts the reading and writing of ACLs on nodes from particular node implementations.
 * 
 * @author britt
 */
public interface AccessControlListDAO
{
    /**
     * Get the ACL from a node.
     * 
     * @param nodeRef
     *            The reference to the node.
     * @return The ACL.
     * @throws InvalidNodeRefException
     */
    public DbAccessControlList getAccessControlList(NodeRef nodeRef);

    /**
     * Set the ACL on a node.
     * 
     * @param nodeRef
     *            The reference to the node.
     * @param acl
     *            The ACL.
     * @throws InvalidNodeRefException
     */
    public void setAccessControlList(NodeRef nodeRef, DbAccessControlList acl);

    /**
     * Set the ACL on a node.
     * 
     * @param nodeRef
     *            The reference to the node.
     * @param aclId
     *            The ID of the ACL entity.
     * @throws InvalidNodeRefException          if the noderef is invalid
     */
    public void setAccessControlList(NodeRef nodeRef, Long aclId);

    /**
     * Update any associated ACLs
     * 
     * @param startingPoint
     * @param chnages
     */
    public void updateChangedAcls(NodeRef startingPoint, List<AclChange> changes);

    /**
     * Update inheritance
     * 
     * @param parent
     * @param inheritFrom
     * @param previousId
     * @return
     */
    public List<AclChange> setInheritanceForChildren(NodeRef parent, Long inheritFrom);

    public Long getIndirectAcl(NodeRef nodeRef);

    public Long getInheritedAcl(NodeRef nodeRef);

    public void forceCopy(NodeRef nodeRef);
    
    public Map<ACLType, Integer> patchAcls();

    public DbAccessControlList getAccessControlList(StoreRef storeRef);
    
    public void setAccessControlList(StoreRef storeRef, DbAccessControlList acl);
}
