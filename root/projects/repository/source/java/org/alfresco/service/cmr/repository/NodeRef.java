/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.service.cmr.repository;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.error.AlfrescoRuntimeException;

/**
 * Reference to a node
 * 
 * @author Derek Hulley
 */
public final class NodeRef implements EntityRef, Serializable
{
    private static final long serialVersionUID = 3760844584074227768L;
    private static final String URI_FILLER = "/";
    private static final Pattern nodeRefPattern = Pattern.compile(".+://.+/.+");
    
    private final StoreRef storeRef;
    private final String id;

    /**
     * @see #NodeRef(StoreRef, String)
     * @see StoreRef#StoreRef(String, String)
     */
    public NodeRef(String protocol, String identifier, String id)
    {
        this(new StoreRef(protocol, identifier), id);
    }
    
    /**
     * Construct a Node Reference from a Store Reference and Node Id
     * 
     * @param storeRef store reference
     * @param id the manually assigned identifier of the node
     */
    public NodeRef(StoreRef storeRef, String id)
    {
        if (storeRef == null)
        {
            throw new IllegalArgumentException("Store reference may not be null");
        }
        if (id == null)
        {
            throw new IllegalArgumentException("Node id may not be null");
        }

        this.storeRef = storeRef;
        this.id = id;
    }

    /**
     * Construct a Node Reference from a string representation of a Node Reference.
     * <p>
     * The string representation of a Node Reference is as follows:
     * <p>
     * <pre><storeref>/<nodeId></pre>
     * 
     * @param nodeRef the string representation of a node ref
     */
    public NodeRef(String nodeRef)
    {
        int lastForwardSlash = nodeRef.lastIndexOf('/');
        if(lastForwardSlash == -1)
        {
            throw new AlfrescoRuntimeException("Invalid node ref - does not contain forward slash: " + nodeRef);
        }
        this.storeRef = new StoreRef(nodeRef.substring(0, lastForwardSlash));
        this.id = nodeRef.substring(lastForwardSlash+1);
    }

    public String toString()
    {
        return storeRef.toString() + URI_FILLER + id;
    }

    /**
     * Override equals for this ref type
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj instanceof NodeRef)
        {
            NodeRef that = (NodeRef) obj;
            return (this.id.equals(that.id)
                    && this.storeRef.equals(that.storeRef));
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Hashes on ID alone.  As the number of copies of a particular node will be minimal, this is acceptable
     */
    public int hashCode()
    {
        return id.hashCode();
    }

    /**
     * @return The StoreRef part of this reference
     */
    public final StoreRef getStoreRef()
    {
        return storeRef;
    }

    /**
     * @return The Node Id part of this reference
     */
    public final String getId()
    {
        return id;
    }

    /**
     * Determine if passed string conforms to the pattern of a node reference
     * 
     * @param nodeRef  the node reference as a string
     * @return  true => it matches the pattern of a node reference
     */
    public static boolean isNodeRef(String nodeRef)
    {
    	Matcher matcher = nodeRefPattern.matcher(nodeRef);
    	return matcher.matches();
    }
    
    /**
     * Helper class to convey the status of a <b>node</b>.
     * 
     * @author Derek Hulley
     */
    public static class Status
    {
        private final String changeTxnId;
        private final Long dbTxnId;
        private final boolean deleted;
        
        public Status(String changeTxnId, Long dbTxnId, boolean deleted)
        {
            this.changeTxnId = changeTxnId;
            this.dbTxnId = dbTxnId;
            this.deleted = deleted;
        }
        /**
         * @return Returns the ID of the last transaction to change the node
         */
        public String getChangeTxnId()
        {
            return changeTxnId;
        }
        /**
         * @return Returns the db ID of the last transaction to change the node
         */
        public Long getDbTxnId()
        {
            return dbTxnId;
        }
        /**
         * @return Returns true if the node has been deleted, otherwise false
         */
        public boolean isDeleted()
        {
            return deleted;
        }
        
        // debug display string
        public String toString()
        {
            StringBuilder sb = new StringBuilder(50);
            
            sb.append("Status[")
              .append("changeTxnId=")
              .append(changeTxnId)
              .append(", dbTxnId=")
              .append(dbTxnId)
              .append(", deleted=")
              .append(deleted)
              .append("]");
            
            return sb.toString();
        }
    }
}