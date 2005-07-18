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
import org.alfresco.repo.domain.StoreKey;
import org.alfresco.repo.domain.VersionCount;
import org.alfresco.service.cmr.repository.StoreRef;

/**
 * Hibernate-specific implementation of the domain entity <b>versioncounter</b>.
 * 
 * @author Derek Hulley
 */
public class VersionCountImpl implements VersionCount
{
	private StoreKey key;
    private int versionCount;
    private transient StoreRef storeRef;

    public VersionCountImpl()
    {
        versionCount = 0;
    }
    
    /**
     * @see #getKey()
     */
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
    
    /**
     * @see #getKey()
     */
    public int hashCode()
    {
        return getKey().hashCode();
    }
    
    /**
     * @see #getKey()
     */
    public String toString()
    {
        return getKey().toString();
    }

    public StoreKey getKey() {
		return key;
	}

	public synchronized void setKey(StoreKey key) {
		this.key = key;
        this.storeRef = null;
	}
    
    /**
     * For Hibernate use
     */
    private void setVersionCount(int versionCount)
    {
        this.versionCount = versionCount;
    }

    public int incrementVersionCount()
    {
        return ++versionCount;
    }

    /**
     * Reset back to 0
     */
    public void resetVersionCount()
    {
        setVersionCount(0);
    }

    public int getVersionCount()
    {
        return versionCount;
    }
}