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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationContext;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;

/**
 * The default implementation of the authority service.
 * 
 * @author Andy Hind
 */
public class SimpleAuthorityServiceImpl implements AuthorityService
{
    private PersonService personService;

    private NodeService nodeService;

    private Set<String> adminSet = Collections.singleton(PermissionService.ADMINISTRATOR_AUTHORITY);

    private Set<String> guestSet = Collections.singleton(PermissionService.GUEST_AUTHORITY);

    private Set<String> allSet = Collections.singleton(PermissionService.ALL_AUTHORITIES);

    private Set<String> adminUsers;

    private AuthenticationContext authenticationContext;

    private Set<String> guestUsers;
    
    private TenantService tenantService;
    

    public SimpleAuthorityServiceImpl()
    {
        super();
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }
    
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }
    

    public boolean hasAdminAuthority()
    {
        String currentUserName = authenticationContext.getCurrentUserName();

        // note: for MT, this currently relies on a naming convention which assumes that all tenant admins will 
        // have the same base name as the default non-tenant specific admin. Typically "admin" is the default required admin user, 
        // although, if for example "bob" is also listed as an admin then all tenant-specific bob's will also have admin authority 

        return ((currentUserName != null) && (adminUsers.contains(currentUserName) || adminUsers.contains(tenantService.getBaseNameUser(currentUserName))));
    }

    public boolean isAdminAuthority(String authorityName)
    {
        String canonicalName = personService.getUserIdentifier(authorityName);
        if (canonicalName == null)
        {
            canonicalName = authorityName;
        }
        return adminUsers.contains(canonicalName);
    }

    public boolean hasGuestAuthority()
    {
        String currentUserName = authenticationContext.getCurrentUserName();

        // note: for MT, this currently relies on a naming convention which assumes that all tenant admins will 
        // have the same base name as the default non-tenant specific guest. 

        return ((currentUserName != null) && (guestUsers.contains(currentUserName) || guestUsers.contains(tenantService.getBaseNameUser(currentUserName))));
    }

    public boolean isGuestAuthority(String authorityName)
    {
        String canonicalName = personService.getUserIdentifier(authorityName);
        if (canonicalName == null)
        {
            canonicalName = authorityName;
        }
        return guestUsers.contains(canonicalName);
    }

    // IOC

    public void setAuthenticationContext(AuthenticationContext authenticationContext)
    {
        this.authenticationContext = authenticationContext;
    }

    public void setAdminUsers(Set<String> adminUsers)
    {
        this.adminUsers = adminUsers;
    }
 
    public void setGuestUsers(Set<String> guestUsers)
    {
        this.guestUsers = guestUsers;
    }
 
    public Set<String> getAuthorities()
    {
        Set<String> authorities = new HashSet<String>();
        String currentUserName = authenticationContext.getCurrentUserName();
        if (adminUsers.contains(currentUserName))
        {
            authorities.addAll(adminSet);
        }
        else if (!guestUsers.contains(currentUserName))
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
            authorities.addAll(allSet);
            break;
        case OWNER:
             break;
        case ROLE:
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
       return Collections.<String>emptySet();
    }

    public void addAuthority(String parentName, String childName)
    {
        
    }

    public void addAuthority(Collection<String> parentNames, String childName)
    {

    }

    public String createAuthority(AuthorityType type, String shortName)
    {
       return "";
    }

    
    public void deleteAuthority(String name)
    {
      
    }

    public void deleteAuthority(String name, boolean cascade)
    {
        
    }

    public Set<String> getAllRootAuthorities(AuthorityType type)
    {
        return getAllAuthorities(type);
    }

    public Set<String> getContainedAuthorities(AuthorityType type, String name, boolean immediate)
    {
        return Collections.<String>emptySet();
    }

    public Set<String> getContainingAuthorities(AuthorityType type, String name, boolean immediate)
    {
        return Collections.<String>emptySet();
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
        
    }

    public boolean authorityExists(String name)
    {
        return false;
    }

    public Set<String> getAuthoritiesForUser(String currentUserName)
    {
        Set<String> authorities = new HashSet<String>();
        if (adminUsers.contains(currentUserName))
        {
            authorities.addAll(adminSet);
        }
        if(AuthorityType.getAuthorityType(currentUserName) != AuthorityType.GUEST)
        {
           authorities.addAll(allSet);
        }
        return authorities;
    }

    public String getAuthorityDisplayName(String name)
    {
        return "";
    }

    public void setAuthorityDisplayName(String authorityName, String authorityDisplayName)
    {
        
    }

	public Set<String> findAuthoritiesByShortName(AuthorityType type,
			String shortNamePattern) 
	{
    	String fullNamePattern = getName(type, shortNamePattern);
    	return findAuthorities(type, fullNamePattern);
	}

    public Set<String> getAllAuthoritiesInZone(String zoneName, AuthorityType type)
    {
        return Collections.<String>emptySet();
    }

    public NodeRef getOrCreateZone(String zoneName)
    {
        return null;
    }

    public void addAuthorityToZones(String authorityName, Set<String> zones)
    {
        
    }

    public String createAuthority(AuthorityType type, String shortName, String authorityDisplayName, Set<String> authorityZones)
    {
       return "";
    }

    public Set<String> getAllRootAuthoritiesInZone(String zoneName, AuthorityType type)
    {
        return Collections.<String>emptySet();
    }

    public Set<String> getAuthorityZones(String name)
    {
        return Collections.<String>emptySet();
    }

    public Set<String> getDefaultZones()
    {
        return Collections.<String>emptySet();
    }

    public void removeAuthorityFromZones(String authorityName, Set<String> zones)
    {
        
    }

    public Set<String> findAuthoritiesByShortNameInZone(AuthorityType type, String shortNamePattern, String zone)
    {
        return Collections.<String>emptySet();
    }

    public Set<String> findAuthoritiesInZone(AuthorityType type, String namePattern, String zone)
    {
        return Collections.<String>emptySet();
    }

    public NodeRef getZone(String zoneName)
    {
        return null;
    }
}
