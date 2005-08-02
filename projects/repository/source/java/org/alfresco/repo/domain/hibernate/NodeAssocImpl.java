/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.domain.hibernate;

import org.alfresco.repo.domain.Node;
import org.alfresco.repo.domain.NodeAssoc;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;

/**
 * Hibernate-specific implementation of the generic node association
 * 
 * @author Derek Hulley
 */
public class NodeAssocImpl implements NodeAssoc
{
    private long id;
    private Node source;
    private Node target;
    private String typeNamespaceUri;
    private String typeLocalName;
    private transient AssociationRef nodeAssocRef;

    public NodeAssocImpl()
    {
    }

    public void buildAssociation(Node sourceNode, Node targetNode)
    {
        // add the forward associations
        this.setTarget(targetNode);
        this.setSource(sourceNode);
        // add the inverse associations
        sourceNode.getTargetNodeAssocs().add(this);
        targetNode.getSourceNodeAssocs().add(this);
    }
    
    public void removeAssociation()
    {
        // maintain inverse assoc from source node to this instance
        this.getSource().getTargetNodeAssocs().remove(this);
        // maintain inverse assoc from target node to this instance
        this.getTarget().getSourceNodeAssocs().remove(this);
    }
    
    public synchronized AssociationRef getNodeAssocRef()
    {
        if (nodeAssocRef == null)
        {
            nodeAssocRef = new AssociationRef(getSource().getNodeRef(),
                    QName.createQName(getTypeNamespaceUri(), getTypeLocalName()),
                    getTarget().getNodeRef());
        }
        return nodeAssocRef;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer(32);
        sb.append("NodeAssoc")
          .append("[ source=").append(source)
          .append(", target=").append(target)
          .append(", name=").append(getTypeQName())
          .append("]");
        return sb.toString();
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
        else if (!(obj instanceof NodeAssoc))
        {
            return false;
        }
        NodeAssoc that = (NodeAssoc) obj;
        return (EqualsHelper.nullSafeEquals(this.getTypeQName(), that.getTypeQName())
                && EqualsHelper.nullSafeEquals(this.getTarget(), that.getTarget())
                && EqualsHelper.nullSafeEquals(this.getSource(), that.getSource()));
    }
    
    public int hashCode()
    {
        return (this.getTypeLocalName() == null ? 0 : getTypeLocalName().hashCode());
    }

    public long getId()
    {
        return id;
    }

    /**
     * For Hibernate use
     */
    private void setId(long id)
    {
        this.id = id;
    }

    public Node getSource()
    {
        return source;
    }

    /**
     * For internal use
     */
    private void setSource(Node source)
    {
        this.source = source;
    }

    public Node getTarget()
    {
        return target;
    }

    /**
     * For internal use
     */
    private void setTarget(Node target)
    {
        this.target = target;
    }

    /**
     * @see #getNamespaceUri()
     * @see #getLocalName()
     */
    public QName getTypeQName()
    {
        return QName.createQName(getTypeNamespaceUri(), getTypeLocalName());
    }

    /**
     * @see #setNamespaceUri(String)
     * @see #setLocalName(String)
     */
    public void setTypeQName(QName qname)
    {
        setTypeNamespaceUri(qname.getNamespaceURI());
        setTypeLocalName(qname.getLocalName());
    }

    /**
     * For Hibernate use only
     */
    private String getTypeNamespaceUri()
    {
        return typeNamespaceUri;
    }

    /**
     * For Hibernate use only
     */
    private void setTypeNamespaceUri(String typeNamespaceUri)
    {
        this.typeNamespaceUri = typeNamespaceUri;
    }

    /**
     * For Hibernate use only
     */
    private String getTypeLocalName()
    {
        return typeLocalName;
    }

    /**
     * For Hibernate use only
     */
    private void setTypeLocalName(String name)
    {
        this.typeLocalName = name;
    }
}
