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
package org.alfresco.repo.security.authority;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.PermissionServiceSPI;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * The default implementation of the authority service.
 * 
 * @author Andy Hind
 */
public class AuthorityServiceImpl implements AuthorityService, InitializingBean
{
    private static Log logger = LogFactory.getLog(AuthorityServiceImpl.class);
    
    private PersonService personService;

    private NodeService nodeService;
    
    private TenantService tenantService;

    private AuthorityDAO authorityDAO;
    
    private AuthenticationService authenticationService;
    
    private PermissionServiceSPI permissionServiceSPI;

    private Set<String> adminSet = Collections.singleton(PermissionService.ADMINISTRATOR_AUTHORITY);

    private Set<String> guestSet = Collections.singleton(PermissionService.GUEST_AUTHORITY);

    private Set<String> allSet = Collections.singleton(PermissionService.ALL_AUTHORITIES);

    private Set<String> adminGroups = Collections.emptySet();
    
    public AuthorityServiceImpl()
    {
        super();
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }

    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    public void setAuthorityDAO(AuthorityDAO authorityDAO)
    {
        this.authorityDAO = authorityDAO;
    }
        
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }

    public void setPermissionServiceSPI(PermissionServiceSPI permissionServiceSPI)
    {
        this.permissionServiceSPI = permissionServiceSPI;
    }
    
    public void setAdminGroups(Set<String> adminGroups)
    {
        this.adminGroups = adminGroups;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception
    {
        // Fully qualify the admin group names
        if (!this.adminGroups.isEmpty())
        {
            Set<String> adminGroups = new HashSet<String>(this.adminGroups.size());
            for (String group : this.adminGroups)
            {
                adminGroups.add(getName(AuthorityType.GROUP, group));
            }
            this.adminGroups = adminGroups;
        }
    }

    public boolean hasAdminAuthority()
    {
        String currentUserName = AuthenticationUtil.getRunAsUser();
        
        // Determine whether the administrator role is mapped to this user or one of their groups
        return ((currentUserName != null) && getAuthoritiesForUser(currentUserName).contains(PermissionService.ADMINISTRATOR_AUTHORITY));
    }

    public boolean isAdminAuthority(String authorityName)
    {
        String canonicalName = personService.getUserIdentifier(authorityName);
        if (canonicalName == null)
        {
            canonicalName = authorityName;
        }
        
        // Determine whether the administrator role is mapped to this user or one of their groups
        return getAuthoritiesForUser(canonicalName).contains(PermissionService.ADMINISTRATOR_AUTHORITY);
    }

    public Set<String> getAuthorities()
    {
        String currentUserName = AuthenticationUtil.getRunAsUser();
        return getAuthoritiesForUser(currentUserName);
    }

    public Set<String> getAuthoritiesForUser(String currentUserName)
    {
        Set<String> authorities = new HashSet<String>();

        authorities.addAll(getContainingAuthorities(null, currentUserName, false));
        
        // Work out mapped roles
        
        // Check named admin users
        Set<String> adminUsers = this.authenticationService.getDefaultAdministratorUserNames();

        // note: for multi-tenancy, this currently relies on a naming convention which assumes that all tenant admins will 
        // have the same base name as the default non-tenant specific admin. Typically "admin" is the default required admin user, 
        // although, if for example "bob" is also listed as an admin then all tenant-specific bob's will also have admin authority
        String currentUserBaseName = tenantService.getBaseNameUser(currentUserName);
        boolean isAdminUser = (adminUsers.contains(currentUserName) || adminUsers.contains(currentUserBaseName));

        // Check named admin groups
        if (!isAdminUser && !adminGroups.isEmpty())
        {
            for (String authority : authorities)
            {
                if (adminGroups.contains(authority) || adminGroups.contains(tenantService.getBaseNameUser(authority)))
                {
                    isAdminUser = true;
                    break;
                }
            }
        }

        if (isAdminUser)
        {
            authorities.addAll(adminSet);
        }
        if (AuthorityType.getAuthorityType(currentUserBaseName) != AuthorityType.GUEST)
        {
           authorities.addAll(allSet);
        }
        return authorities;
    }
    
    public Set<String> getAllAuthorities(AuthorityType type)
    {
        Set<String> authorities = new HashSet<String>();
        switch (type)
        {
        case ADMIN:
            authorities.addAll(adminSet);
            break;
        case EVERYONE:
            authorities.addAll(allSet);
            break;
        case GUEST:
            authorities.addAll(guestSet);
            break;
        case GROUP:           
            authorities.addAll(authorityDAO.getAllAuthorities(type));
            break;
        case OWNER:
             break;
        case ROLE:
            authorities.addAll(authorityDAO.getAllAuthorities(type));
            break;
        case USER:
            for (NodeRef personRef : personService.getAllPeople())
            {
                authorities.add(DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(personRef,
                        ContentModel.PROP_USERNAME)));
            }
            break;
        default:
            break;
        }
        return authorities;
    }

    
    
    public Set<String> findAuthorities(AuthorityType type, String namePattern)
    {
        Set<String> authorities = new HashSet<String>();
        switch (type)
        {
        case ADMIN:
        case EVERYONE:
        case GUEST:
            throw new UnsupportedOperationException();
        case GROUP:           
            authorities.addAll(authorityDAO.findAuthorities(type, namePattern));
            break;
        case OWNER:
        case ROLE:
            throw new UnsupportedOperationException();
        case USER:
            throw new UnsupportedOperationException();
        default:
            break;
        }
        return authorities;
    }
    
    
    public Set<String> findAuthoritiesByShortName(AuthorityType type, String shortNamePattern)
    {
    	String fullNamePattern = getName(type, shortNamePattern);
    	return findAuthorities(type, fullNamePattern);
    }

    public void addAuthority(String parentName, String childName)
    {
        if (AuthorityType.getAuthorityType(childName).equals(AuthorityType.USER))
        {
            if(!personService.personExists(childName))
            {
                throw new AuthorityException("The person "+childName+" does not exist and can not be added to a group");
            }
        }
        authorityDAO.addAuthority(parentName, childName);
    }

    private void checkTypeIsMutable(AuthorityType type)
    {
        if((type == AuthorityType.GROUP) || (type == AuthorityType.ROLE))
        {
            return;
        }
        else
        {
            throw new AuthorityException("Trying to modify a fixed authority");
        }
    }
    
    public String createAuthority(AuthorityType type, String shortName)
    {
        return createAuthority(type, shortName, shortName, null);
    }

    public void deleteAuthority(String name)
    {
        AuthorityType type = AuthorityType.getAuthorityType(name);
        checkTypeIsMutable(type);
        authorityDAO.deleteAuthority(name);
        permissionServiceSPI.deletePermissions(name);
    }

    public Set<String> getAllRootAuthorities(AuthorityType type)
    {
        return authorityDAO.getAllRootAuthorities(type);
    }

    public Set<String> getContainedAuthorities(AuthorityType type, String name, boolean immediate)
    {
        return authorityDAO.getContainedAuthorities(type, name, immediate);
    }

    public Set<String> getContainingAuthorities(AuthorityType type, String name, boolean immediate)
    {
        return authorityDAO.getContainingAuthorities(type, name, immediate);
    }

    public String getName(AuthorityType type, String shortName)
    {
        if (type.isFixedString())
        {
            return type.getFixedString();
        }
        else if (type.isPrefixed())
        {
            return type.getPrefixString() + shortName;
        }
        else
        {
            return shortName;
        }
    }

    public String getShortName(String name)
    {
        AuthorityType type = AuthorityType.getAuthorityType(name);
        if (type.isFixedString())
        {
            return "";
        }
        else if (type.isPrefixed())
        {
            return name.substring(type.getPrefixString().length());
        }
        else
        {
            return name;
        }

    }

    public void removeAuthority(String parentName, String childName)
    {
        authorityDAO.removeAuthority(parentName, childName);
    }

    public boolean authorityExists(String name)
    {
       return authorityDAO.authorityExists(name);
    }

    public String createAuthority(AuthorityType type, String shortName, String authorityDisplayName,
            String authorityZone)
    {
        checkTypeIsMutable(type);
        String name = getName(type, shortName);
        authorityDAO.createAuthority(name, authorityDisplayName, authorityZone);
        return name;
    }

    public String getAuthorityDisplayName(String name)
    {
        String displayName = authorityDAO.getAuthorityDisplayName(name);
        if(displayName == null)
        {
            displayName = getShortName(name);
        }
        return displayName;
    }

    public void setAuthorityDisplayName(String authorityName, String authorityDisplayName)
    {
        AuthorityType type = AuthorityType.getAuthorityType(authorityName);
        checkTypeIsMutable(type);
        authorityDAO.setAuthorityDisplayName(authorityName, authorityDisplayName);
    }

    public String getAuthorityZone(String name)
    {
        return authorityDAO.getAuthorityZone(name);
    }

    public NodeRef getOrCreateZone(String zoneName)
    {
        return authorityDAO.getOrCreateZone(zoneName);
    }

    public Set<String> getAllAuthoritiesInZone(String zoneName, AuthorityType type)
    {
        return authorityDAO.getAllAuthoritiesInZone(zoneName, type);
    }
}
