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
package org.alfresco.repo.domain;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.namespace.QName;

/**
 * Represents a generic association between two nodes.  The association is named
 * and bidirectional by default.
 * 
 * @author Derek Hulley
 */
public interface NodeAssoc
{
    /**
     * Wires up the necessary bits on the source and target nodes so that the association
     * is immediately bidirectional.
     * <p>
     * The association attributes still have to be set.
     * 
     * @param sourceNode
     * @param targetNode
     * 
     * @see #setName(String)
     */
    public void buildAssociation(Node sourceNode, Node targetNode);

    /**
     * Convenience method to retrieve the association's reference
     * 
     * @param qnameDAO          helper DAO
     * @return                  the association's reference
     */
    public AssociationRef getNodeAssocRef(QNameDAO qnameDAO);
    
    /**
     * Convenience method to retrieve the association's type QName
     * 
     * @param qnameDAO          helper DAO
     * @return                  the association's type QName
     */
    public QName getTypeQName(QNameDAO qnameDAO);
    
    /**
     * Convenience method to set the association's type
     * 
     * @param qnameDAO      the helper DAO
     * @param typeQName     the association's type QName
     */
    public void setTypeQName(QNameDAO qnameDAO, QName typeQName);
    
    public Long getId();
    
    /**
     * @return  Returns the current version number
     */
    public Long getVersion();

    public Node getSource();

    public Node getTarget();

    /**
     * @return              Returns the type of the association
     */
    public Long getTypeQNameId();
    
    /**
     * @param typeQNameId   the association's dictionary type
     */
    public void setTypeQNameId(Long typeQNameId);
}
