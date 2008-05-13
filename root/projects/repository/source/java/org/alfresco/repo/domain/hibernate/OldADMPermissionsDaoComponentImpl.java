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
package org.alfresco.repo.domain.hibernate;

import java.util.Collections;

import org.alfresco.repo.domain.DbAccessControlList;
import org.alfresco.repo.security.permissions.ACLType;
import org.alfresco.repo.security.permissions.SimpleAccessControlListProperties;
import org.alfresco.repo.security.permissions.impl.AclChange;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Support for accessing persisted permission information. This class maps between persisted objects and the external
 * API defined in the PermissionsDAO interface.
 * 
 * @author andyh
 */
public class OldADMPermissionsDaoComponentImpl extends AbstractPermissionsDaoComponentImpl
{
    private static Log logger = LogFactory.getLog(OldADMPermissionsDaoComponentImpl.class);

    /**
     * 
     */
    public OldADMPermissionsDaoComponentImpl()
    {
        super();
    }

    /**
     * Creates an access control list for the node and removes the entry from the nullPermsionCache.
     */
    protected AbstractPermissionsDaoComponentImpl.CreationReport createAccessControlList(NodeRef nodeRef, boolean inherit, DbAccessControlList existing)
    {
        SimpleAccessControlListProperties properties = new SimpleAccessControlListProperties();
        properties.setAclType(ACLType.OLD);
        properties.setInherits(inherit);
        Long id = aclDaoComponent.createAccessControlList(properties);
        DbAccessControlList acl = aclDaoComponent.getDbAccessControlList(id);

        // maintain inverse
        getACLDAO(nodeRef).setAccessControlList(nodeRef, acl);

        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Created Access Control List: \n" + "   node: " + nodeRef + "\n" + "   list: " + acl);
        }

        AbstractPermissionsDaoComponentImpl.CreationReport report = new AbstractPermissionsDaoComponentImpl.CreationReport(acl, Collections
                .<AclChange> singletonList(new AclDaoComponentImpl.AclChangeImpl(null, id, null, acl.getAclType())));
        return report;

    }

    public void deletePermissions(NodeRef nodeRef)
    {
        DbAccessControlList acl = null;
        try
        {
            acl = getAccessControlList(nodeRef);
        }
        catch (InvalidNodeRefException e)
        {
            return;
        }
        if (acl != null)
        {
            // maintain referencial integrity
            getACLDAO(nodeRef).setAccessControlList(nodeRef, null);
            aclDaoComponent.deleteAccessControlList(acl.getId());
        }
    }
}
