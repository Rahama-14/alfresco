/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.domain.hibernate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.domain.ChildAssoc;
import org.alfresco.repo.domain.Node;
import org.alfresco.repo.domain.NodeAssoc;
import org.alfresco.repo.domain.NodeKey;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.domain.Store;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * Bean containing all the persistence data representing a <b>node</b>.
 * <p>
 * This implementation of the {@link org.alfresco.repo.domain.Node Node} interface is
 * Hibernate specific.
 * 
 * @author Derek Hulley
 * 
 */
public class NodeImpl implements Node
{
    private NodeKey key;
    private Store store;
    private QName typeQName;
    private Set<QName> aspects;
    private Set<NodeAssoc> sourceNodeAssocs;
    private Set<NodeAssoc> targetNodeAssocs;
    private Set<ChildAssoc> parentAssocs;
    private Set<ChildAssoc> childAssocs;
    private Map<QName, PropertyValue> properties;
    private transient NodeRef nodeRef;

    public NodeImpl()
    {
        aspects = new HashSet<QName>(5);
        sourceNodeAssocs = new HashSet<NodeAssoc>(3);
        targetNodeAssocs = new HashSet<NodeAssoc>(3);
        parentAssocs = new HashSet<ChildAssoc>(3);
        childAssocs = new HashSet<ChildAssoc>(3, 0.75F);
        properties = new HashMap<QName, PropertyValue>(5);
    }
    
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        else if (obj == this)
        {
            return true;
        }
        else if (!(obj instanceof Node))
        {
            return false;
        }
        Node that = (Node) obj;
        return (this.getKey().equals(that.getKey()));
    }
    
    public int hashCode()
    {
        return getKey().hashCode();
    }

    public NodeKey getKey() {
		return key;
	}

	public void setKey(NodeKey key) {
		this.key = key;
	}
    
    public Store getStore()
    {
        return store;
    }

    public synchronized void setStore(Store store)
    {
        this.store = store;
        this.nodeRef = null;
    }

    public QName getTypeQName()
    {
        return typeQName;
    }

    public void setTypeQName(QName typeQName)
    {
        this.typeQName = typeQName;
    }

    public Set<QName> getAspects()
    {
        return aspects;
    }
    
    /**
     * For Hibernate use
     */
    private void setAspects(Set<QName> aspects)
    {
        this.aspects = aspects;
    }

    public Set<NodeAssoc> getSourceNodeAssocs()
    {
        return sourceNodeAssocs;
    }

    /**
     * For Hibernate use
     */
    private void setSourceNodeAssocs(Set<NodeAssoc> sourceNodeAssocs)
    {
        this.sourceNodeAssocs = sourceNodeAssocs;
    }

    public Set<NodeAssoc> getTargetNodeAssocs()
    {
        return targetNodeAssocs;
    }

    /**
     * For Hibernate use
     */
    private void setTargetNodeAssocs(Set<NodeAssoc> targetNodeAssocs)
    {
        this.targetNodeAssocs = targetNodeAssocs;
    }
    
    public Set<ChildAssoc> getParentAssocs()
    {
        return parentAssocs;
    }

    /**
     * For Hibernate use
     */
    private void setParentAssocs(Set<ChildAssoc> parentAssocs)
    {
        this.parentAssocs = parentAssocs;
    }

    public Set<ChildAssoc> getChildAssocs()
    {
        return childAssocs;
    }

    /**
     * For Hibernate use
     */
    private void setChildAssocs(Set<ChildAssoc> childAssocs)
    {
        this.childAssocs = childAssocs;
    }

    public Map<QName, PropertyValue> getProperties()
    {
        return properties;
    }

    /**
     * For Hibernate use
     */
    private void setProperties(Map<QName, PropertyValue> properties)
    {
        this.properties = properties;
    }

    /**
     * Thread-safe caching of the reference is provided
     */
    public synchronized NodeRef getNodeRef()
    {
        if (nodeRef == null && key != null)
        {
            nodeRef = new NodeRef(getStore().getStoreRef(), getKey().getGuid());
        }
        return nodeRef;
    }
    
    /**
     * @see #getNodeRef()
     */
    public String toString()
    {
        return getNodeRef().toString();
    }
}
