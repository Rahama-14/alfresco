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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.impl.PermissionReferenceImpl;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 * Support to read and store the definition of a permission.
 * 
 * @author andyh
 */
public class Permission extends AbstractPermission implements XMLModelInitialisable
{
    // XML Constants
    
    private static final String GRANTED_TO_GROUP = "grantedToGroup";
    
    private static final String GTG_NAME = "permissionGroup";

    private static final String GTG_TYPE = "type";

    private Set<PermissionReference> grantedToGroups = new HashSet<PermissionReference>();
    
    private static final String DENY = "deny";
    
    private static final String ALLOW = "allow";
    
    private static final String DEFAULT_PERMISSION = "defaultPermission";

    private static final String EXPOSE = "expose";

    private AccessStatus defaultPermission;

    private boolean isExposed;

    public Permission(QName typeQName)
    {
        super(typeQName);
       
    }

    public void initialise(Element element, NamespacePrefixResolver nspr, PermissionModel permissionModel)
    {
        super.initialise(element, nspr, permissionModel);
        
        Attribute att = element.attribute(EXPOSE);
        if (att != null)
        {
            isExposed = Boolean.parseBoolean(att.getStringValue());
        }
        else
        {
            isExposed = true;
        }
        
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
        
        for (Iterator gtgit = element.elementIterator(GRANTED_TO_GROUP); gtgit.hasNext(); /**/)
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

    public boolean isExposed()
    {
        return isExposed;
    }

}
