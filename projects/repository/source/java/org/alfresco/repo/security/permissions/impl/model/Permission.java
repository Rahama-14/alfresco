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
 * Created on 01-Aug-2005
 */
package org.alfresco.repo.security.permissions.impl.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.alfresco.repo.security.permissions.AccessStatus;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.impl.PermissionReferenceImpl;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.dom4j.Attribute;
import org.dom4j.Element;

public class Permission extends AbstractPermission implements XMLModelInitialisable
{

    private static final String GRANTED_TO_GROUPS = "grantedToGroups";
    
    private static final String GTG_NAME = "name";

    private static final String GTG_TYPE = "type";

    private Set<PermissionReference> grantedToGroups = new HashSet<PermissionReference>();
    
    private static final String DENY = "deny";
    
    private static final String ALLOW = "allow";
    
    private static final String DEFAULT_PERMISSION = "defaultPermission";

    private AccessStatus defaultPermission;

    public Permission(QName typeQName)
    {
        super(typeQName);
       
    }

    public void initialise(Element element, NamespacePrefixResolver nspr)
    {
        super.initialise(element, nspr);
        
        Attribute defaultPermissionAttribute = element.attribute(DEFAULT_PERMISSION);
        if(defaultPermissionAttribute != null)
        {
            if(defaultPermissionAttribute.getStringValue().equalsIgnoreCase(ALLOW))
            {
                defaultPermission = AccessStatus.ALLOWED;  
            }
            else if(defaultPermissionAttribute.getStringValue().equalsIgnoreCase(DENY))
            {
                defaultPermission = AccessStatus.DENIED;  
            }
            else
            {
                throw new PermissionModelException("The default permission must be deny or allow");
            }
        }
        else
        {
            defaultPermission = AccessStatus.DENIED;
        }
        
        for (Iterator gtgit = element.elementIterator(GRANTED_TO_GROUPS); gtgit.hasNext(); /**/)
        {
            QName qName;
            Element grantedToGroupsElement = (Element) gtgit.next();
            Attribute typeAttribute = grantedToGroupsElement.attribute(GTG_TYPE);
            if (typeAttribute != null)
            {
                qName = QName.createQName(typeAttribute.getStringValue(), nspr);
            }
            else
            {
                qName = getTypeQName();
            }

            String grantedName = grantedToGroupsElement.attributeValue(GTG_NAME);
            
            grantedToGroups.add(new PermissionReferenceImpl(qName, grantedName));
        }      
 
    }

    public AccessStatus getDefaultPermission()
    {
        return defaultPermission;
    }

    public Set<PermissionReference> getGrantedToGroups()
    {
        return Collections.unmodifiableSet(grantedToGroups);
    }
    
    

}
