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
package org.alfresco.repo.ownable.impl;

import org.alfresco.repo.ownable.OwnableService;
import org.alfresco.repo.security.permissions.DynamicAuthority;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.EqualsHelper;
import org.springframework.beans.factory.InitializingBean;

public class OwnerDynamicAuthority implements DynamicAuthority, InitializingBean
{
    private OwnableService ownableService;

    public OwnerDynamicAuthority()
    {
        super();
    }

    public void setOwnableService(OwnableService ownableService)
    {
        this.ownableService = ownableService;
    }

    public void afterPropertiesSet() throws Exception
    {
        if (ownableService == null)
        {
            throw new IllegalArgumentException("There must be an ownable service");
        }
    }

    public boolean hasAuthority(NodeRef nodeRef, String userName)
    {
        return EqualsHelper.nullSafeEquals(ownableService.getOwner(nodeRef), userName);
    }

    public String getAuthority()
    {
       return PermissionService.OWNER_AUTHORITY;
    }

}
