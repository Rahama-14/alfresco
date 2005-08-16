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
 *
 * Created on 02-Aug-2005
 */
package org.alfresco.repo.security.permissions.impl.hibernate;

import java.util.HashSet;
import java.util.Set;

/**
 * The persisted class for recipients.
 * 
 * @author andyh
 */
public class RecipientImpl implements Recipient
{
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = -5582068692208928127L;

    private String recipient;
    
    private Set<String> externalKeys = new HashSet<String>();

    public RecipientImpl()
    {
        super();
    }
    
    public String getRecipient()
    {
        return recipient;
    }

    public void setRecipient(String recipient)
    {
       this.recipient = recipient;
    }

    public Set<String> getExternalKeys()
    {
        return externalKeys;
    }

    // Hibernate
    /* package */ void setExternalKeys(Set<String> externalKeys)
    {
        this.externalKeys = externalKeys;
    }
    
    // Hibernate pattern
    
    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof Recipient))
        {
            return false;
        }
        Recipient other = (Recipient)o;
        return this.getRecipient().equals(other.getRecipient());
    }

    @Override
    public int hashCode()
    {
        return getRecipient().hashCode();
    }
    
    
}
