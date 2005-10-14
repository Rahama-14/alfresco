/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Alfresco Network License. You may obtain a
 * copy of the License at
 *
 *   http://www.alfrescosoftware.com/legal/
 *
 * Please view the license relevant to your network subscription.
 *
 * BY CLICKING THE "I UNDERSTAND AND ACCEPT" BOX, OR INSTALLING,  
 * READING OR USING ALFRESCO'S Network SOFTWARE (THE "SOFTWARE"),  
 * YOU ARE AGREEING ON BEHALF OF THE ENTITY LICENSING THE SOFTWARE    
 * ("COMPANY") THAT COMPANY WILL BE BOUND BY AND IS BECOMING A PARTY TO 
 * THIS ALFRESCO NETWORK AGREEMENT ("AGREEMENT") AND THAT YOU HAVE THE   
 * AUTHORITY TO BIND COMPANY. IF COMPANY DOES NOT AGREE TO ALL OF THE   
 * TERMS OF THIS AGREEMENT, DO NOT SELECT THE "I UNDERSTAND AND AGREE"   
 * BOX AND DO NOT INSTALL THE SOFTWARE OR VIEW THE SOURCE CODE. COMPANY   
 * HAS NOT BECOME A LICENSEE OF, AND IS NOT AUTHORIZED TO USE THE    
 * SOFTWARE UNLESS AND UNTIL IT HAS AGREED TO BE BOUND BY THESE LICENSE  
 * TERMS. THE "EFFECTIVE DATE" FOR THIS AGREEMENT SHALL BE THE DAY YOU  
 * CHECK THE "I UNDERSTAND AND ACCEPT" BOX.
 */
package org.alfresco.repo.security.permissions.impl.model;

import java.util.Set;

import org.alfresco.repo.security.permissions.PermissionEntry;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.impl.AbstractPermissionTest;
import org.alfresco.repo.security.permissions.impl.SimplePermissionReference;
import org.alfresco.service.namespace.QName;

public class PermissionModelTest extends AbstractPermissionTest
{
    
    public PermissionModelTest()
    {
        super();
    }

    public void testIncludePermissionGroups()
    {
        Set<PermissionReference> grantees = permissionModelDAO.getGranteePermissions(new SimplePermissionReference(QName.createQName("cm", "folder",
                namespacePrefixResolver), "Guest"));

        assertEquals(5, grantees.size());
    }
    
    public void testGetGrantingPermissions()
    {
        Set<PermissionReference> granters = permissionModelDAO.getGrantingPermissions(new SimplePermissionReference(QName.createQName("sys", "base",
                namespacePrefixResolver), "ReadProperties"));
        assertEquals(8, granters.size());
    }
    
    public void testGlobalPermissions()
    {
        Set<? extends PermissionEntry> globalPermissions = permissionModelDAO.getGlobalPermissionEntries();
        assertEquals(2, globalPermissions.size());
    }
}
