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
package org.alfresco.repo.importer;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;

/**
 * Importer Location
 * 
 * @author David Caruana
 */
public class Location
{
    private StoreRef storeRef = null;
    private NodeRef nodeRef = null;
    private String path = null;
    private QName childAssocType = null;
    

    /**
     * Construct
     * 
     * @param nodeRef
     */
    public Location(NodeRef nodeRef)
    {
        ParameterCheck.mandatory("Node Ref", nodeRef);
        this.storeRef = nodeRef.getStoreRef();
        this.nodeRef = nodeRef;
    }

    /**
     * Construct
     * 
     * @param storeRef
     */
    public Location(StoreRef storeRef)
    {
        ParameterCheck.mandatory("Store Ref", storeRef);
        this.storeRef = storeRef;
    }

    /**
     * @return  the store ref
     */
    public StoreRef getStoreRef()
    {
        return storeRef;
    }
    
    /**
     * @return  the node ref
     */
    public NodeRef getNodeRef()
    {
        return nodeRef;
    }
    
    /**
     * Sets the import location to the specified path
     *  
     * @param path  path relative to store or node reference
     */
    public void setPath(String path)
    {
        this.path = path;
    }
    
    /**
     * @return  the import location
     */
    public String getPath()
    {
        return path;
    }

    /**
     * Sets the child association type to import within
     * 
     * @param childAssocType  child association type
     */
    public void setChildAssocType(QName childAssocType)
    {
        this.childAssocType = childAssocType;
    }
    
    /**
     * @return  the child association type
     */
    public QName getChildAssocType()
    {
        return childAssocType;
    }
}
