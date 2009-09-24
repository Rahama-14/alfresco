/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.module.org_alfresco_module_dod5015.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.module.org_alfresco_module_dod5015.capability.Capability;
import org.alfresco.module.org_alfresco_module_dod5015.capability.RMEntryVoter;
import org.alfresco.module.org_alfresco_module_dod5015.capability.RMPermissionModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Records management permission service implementation
 * 
 * @author Roy Wetherall
 */
public class RecordsManagementSecurityServiceImpl implements RecordsManagementSecurityService, 
                                                             RecordsManagementModel
                                                             
{
    /** Entry voter for capability related support */
    private RMEntryVoter voter;
    
    /** Authority service */
    private AuthorityService authorityService;
    
    /** Permission service */
    private PermissionService permissionService;
    
    /** Policy component */
    private PolicyComponent policyComponent;
    
    private RecordsManagementService recordsManagementService;
    
    /** Node service */
    private NodeService nodeService;
    
    /** Records management role zone */
    public static final String RM_ROLE_ZONE_PREFIX = "rmRoleZone";
    
    /**
     * Set the RMEntryVoter
     * 
     * @param voter
     */
    public void setVoter(RMEntryVoter voter)
    {
        this.voter = voter;       
    }
    
    /**
     * Set the authortiy service
     * 
     * @param authorityService
     */
    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }
    
    /**
     * Set the permission service
     * 
     * @param permissionService
     */
    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }  
    
    /**
     * Set the policy component
     * 
     * @param policyComponent
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    
    /**
     * Set records management service
     * 
     * @param recordsManagementService  records management service
     */
    public void setRecordsManagementService(RecordsManagementService recordsManagementService)
    {
        this.recordsManagementService = recordsManagementService;
    }
    
    /**
     * Set the node service
     * 
     * @param nodeService
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Initialisation method
     */
    public void init()
    {
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, 
                ASPECT_RECORDS_MANAGEMENT_ROOT, 
                new JavaBehaviour(this, "onCreateRootNode", NotificationFrequency.TRANSACTION_COMMIT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, 
                TYPE_RECORDS_MANAGEMENT_CONTAINER, 
                new JavaBehaviour(this, "onCreateRMContainer", NotificationFrequency.TRANSACTION_COMMIT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME,  
                TYPE_RECORD_FOLDER, 
                new JavaBehaviour(this, "onCreateRecordFolder", NotificationFrequency.TRANSACTION_COMMIT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeDeleteNodePolicy.QNAME, 
                ASPECT_FROZEN, 
                new JavaBehaviour(this, "beforeDeleteFrozenNode", NotificationFrequency.TRANSACTION_COMMIT));
    }
    
    public void beforeDeleteFrozenNode(NodeRef nodeRef)
    {
        throw new AccessDeniedException("Frozen nodes can not be deleted");
    }
    
    /**
     * Create root node behaviour
     * 
     * @param childAssocRef
     */
    public void onCreateRootNode(ChildAssociationRef childAssocRef)
    {
        final NodeRef rmRootNode = childAssocRef.getChildRef();
        if (nodeService.exists(rmRootNode) == true)
        {
            AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
            {
                public Object doWork()
                {
                    // Create "all" role group for root node
                    String allRoles = authorityService.createAuthority(AuthorityType.GROUP, getAllRolesGroupShortName(rmRootNode), "All Roles", null);                    
                    
                    // Set the permissions
                    permissionService.setInheritParentPermissions(rmRootNode, false);
                    permissionService.setPermission(rmRootNode, allRoles, RMPermissionModel.READ_RECORDS, true);
                    return null;
                }
            }, AuthenticationUtil.getAdminUserName());
                        
            // Bootstrap in the default set of roles for the newly created root node
            bootstrapDefaultRoles(rmRootNode);
        }
    }
    
    /**
     * @param rmRootNode
     * @return
     */
    private String getAllRolesGroupShortName(NodeRef rmRootNode)
    {
        return "AllRoles" + rmRootNode.getId();
    }
    
    /**
     * @param childAssocRef
     */
    public void onCreateRMContainer(ChildAssociationRef childAssocRef)
    {
        setUpPermissions(childAssocRef.getChildRef());
    }
    
    /**
     * @param childAssocRef
     */
    public void onCreateRecordFolder(ChildAssociationRef childAssocRef)
    {
    	final NodeRef folderNodeRef = childAssocRef.getChildRef();
        setUpPermissions(folderNodeRef);
        
        // Pull any permissions found on the parent (ie the record category)
        final NodeRef catNodeRef = childAssocRef.getParentRef();
        if (nodeService.exists(catNodeRef) == true)
        {
        	AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
            {
                public Object doWork()
                {
                	Set<AccessPermission> perms = permissionService.getAllSetPermissions(catNodeRef);
                	for (AccessPermission perm : perms) 
                	{
                		AccessStatus accessStatus = perm.getAccessStatus();
                		boolean allow = false;
                		if (AccessStatus.ALLOWED.equals(accessStatus) == true)
                		{
                			allow = true;
                		}
                		permissionService.setPermission(
                				folderNodeRef, 
                				perm.getAuthority(), 
                				perm.getPermission(), 
                				allow);
        			}
                	
                    return null;
                }
            }, AuthenticationUtil.getAdminUserName());	
        }
    }
    
    /**
     * 
     * @param nodeRef
     */
    public void setUpPermissions(final NodeRef nodeRef)
    {
        if (nodeService.exists(nodeRef) == true)
        {        
            AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
            {
                public Object doWork()
                {
                    // Break inheritance 
                    permissionService.setInheritParentPermissions(nodeRef, false);
                                    
                    return null;
                }
            }, AuthenticationUtil.getAdminUserName());         
        }
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#getCapabilities()
     */
    public Set<Capability> getCapabilities()
    {
        Collection<Capability> caps = voter.getAllCapabilities();
        Set<Capability> result = new HashSet<Capability>(caps.size());
        for (Capability cap : caps)
        {
            if (cap.isGroupCapability() == false)
            {
                result.add(cap);
            }
        }
        return result;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#getCapabilities(org.alfresco.service.cmr.repository.NodeRef)
     */
    public Map<Capability, AccessStatus> getCapabilities(NodeRef nodeRef)
    {
        return voter.getCapabilities(nodeRef);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#getCapability(java.lang.String)
     */
    public Capability getCapability(String name)
    {
        return voter.getCapability(name);
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#getProtectedAspects()
     */
    public Set<QName> getProtectedAspects()
    {
        return voter.getProtetcedAscpects();
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#getProtectedProperties()
     */
    public Set<QName> getProtectedProperties()
    {
       return voter.getProtectedProperties();
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#bootstrapDefaultRoles(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void bootstrapDefaultRoles(final NodeRef rmRootNode)
    {
        AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
        {
            public Object doWork()
            {
                try
                {
                    JSONArray array = null;
                    try
                    {
                        // Load up the default roles from JSON
                        InputStream is = getClass().getClassLoader().getResourceAsStream("alfresco/module/org_alfresco_module_dod5015/security/rm-default-roles-bootstrap.json");
                        if  (is == null)
                        {
                            throw new AlfrescoRuntimeException("Could not load default bootstrap roles configuration");
                        }
                        array = new JSONArray(convertStreamToString(is));
                    }
                    catch (IOException ioe)
                    {
                        throw new AlfrescoRuntimeException("Unable to load rm-default-roles-bootstrap.json configuration file.", ioe);
                    }
                    
                    // Add each role to the rm root node
                    for (int i = 0; i < array.length(); i++)
                    {
                        JSONObject object = array.getJSONObject(i);
                        
                        // Get the name of the role
                        String name = null;
                        if (object.has("name") == true)
                        {
                            name = object.getString("name");
                            if (existsRole(rmRootNode, name) == true)
                            {
                                throw new AlfrescoRuntimeException("The bootstrap role " + name + " already exists on the rm root node " + rmRootNode.toString());
                            }
                        }
                        else
                        {
                            throw new AlfrescoRuntimeException("No name given to default bootstrap role.  Check json configuration file.");
                        }
                        
                                                
                        // Get the role's display label
                        String displayLabel = name;
                        if (object.has("displayLabel") == true)
                        {
                            displayLabel = object.getString("displayLabel");
                        }
                        
                        // Determine whether the role is an admin role or not
                        boolean isAdmin = false;
                        if (object.has("isAdmin") == true)
                        {
                            isAdmin = object.getBoolean("isAdmin");
                        }
                                                
                        // Get the roles capabilities
                        Set<Capability> capabilities = new HashSet<Capability>(30);
                        if (object.has("capabilities") == true)
                        {
                            JSONArray arrCaps = object.getJSONArray("capabilities");
                            for (int index = 0; index < arrCaps.length(); index++)
                            {
                                String capName = arrCaps.getString(index);
                                Capability capability = getCapability(capName);
                                if (capability == null)
                                {
                                    throw new AlfrescoRuntimeException("The capability '" + capName + "' configured for the deafult boostrap role '" + name + "' is invalid.");
                                }
                                capabilities.add(capability);
                            }
                        }
                        
                        // Create the role
                        Role role = createRole(rmRootNode, name, displayLabel, capabilities);
                        
                        // Add any additional admin permissions
                        if (isAdmin == true)
                        {
                            permissionService.setPermission(rmRootNode, role.getRoleGroupName(), RMPermissionModel.FILING, true);
                        }
                    }
                }
                catch (JSONException exception)
                {
                    throw new AlfrescoRuntimeException("Error loading json configuration file rm-default-roles-bootstrap.json", exception);
                }
                
                return null;
            }
        }, AuthenticationUtil.getAdminUserName());
    }
    
    public String convertStreamToString(InputStream is) throws IOException
    {
        /*
        * To convert the InputStream to String we use the BufferedReader.readLine()
        * method. We iterate until the BufferedReader return null which means
        * there's no more data to read. Each line will appended to a StringBuilder
        * and returned as String.
        */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
         
        String line = null;
        try 
        {
            while ((line = reader.readLine()) != null) 
            {
                sb.append(line + "\n");
            }
        }
        finally 
        {
            try {is.close();} catch (IOException e) {}
        }
         
        return sb.toString();
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#getRoles()
     */
    public Set<Role> getRoles(final NodeRef rmRootNode)
    {  
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Set<Role>>()
        {
            public Set<Role> doWork() throws Exception
            {
                Set<Role> result = new HashSet<Role>(13);
                
                Set<String> roleAuthorities = authorityService.getAllAuthoritiesInZone(getZoneName(rmRootNode), AuthorityType.GROUP);        
                for (String roleAuthority : roleAuthorities)
                {
                    String name = getShortRoleName(authorityService.getShortName(roleAuthority), rmRootNode);
                    String displayLabel = authorityService.getAuthorityDisplayName(roleAuthority);
                    Set<String> capabilities = getCapabilities(rmRootNode, roleAuthority);
                    
                    Role role = new Role(name, displayLabel, capabilities, roleAuthority);
                    result.add(role);            
                }
                
                return result;
            }
        }, AuthenticationUtil.getAdminUserName());
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#getRolesByUser(org.alfresco.service.cmr.repository.NodeRef, java.lang.String)
     */
    public Set<Role> getRolesByUser(final NodeRef rmRootNode, final String user)
    {
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Set<Role>>()
        {
            public Set<Role> doWork() throws Exception
            {
                Set<Role> result = new HashSet<Role>(13);
                
                Set<String> roleAuthorities = authorityService.getAllAuthoritiesInZone(getZoneName(rmRootNode), AuthorityType.GROUP);        
                for (String roleAuthority : roleAuthorities)
                {
                    Set<String> users = authorityService.getContainedAuthorities(AuthorityType.USER, roleAuthority, false);
                    if (users.contains(user) == true)
                    {                    
                        String name = getShortRoleName(authorityService.getShortName(roleAuthority), rmRootNode);
                        String displayLabel = authorityService.getAuthorityDisplayName(roleAuthority);
                        Set<String> capabilities = getCapabilities(rmRootNode, roleAuthority);
                        
                        Role role = new Role(name, displayLabel, capabilities, roleAuthority);
                        result.add(role);  
                    }
                }
                
                return result;
            }
        }, AuthenticationUtil.getAdminUserName());
    }
    
    /**
     * 
     * @param rmRootNode
     * @return
     */
    private String getZoneName(NodeRef rmRootNode)
    {
        return RM_ROLE_ZONE_PREFIX + rmRootNode.getId();
    }
    
    /**
     * Get the full role name
     * 
     * @param role
     * @param rmRootNode
     * @return
     */
    private String getFullRoleName(String role, NodeRef rmRootNode)
    {
        return role + rmRootNode.getId();
    }
    
    /**
     * Get the short role name
     * 
     * @param fullRoleName
     * @param rmRootNode
     * @return
     */
    private String getShortRoleName(String fullRoleName, NodeRef rmRootNode)
    {
        return fullRoleName.replaceAll(rmRootNode.getId(), "");
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#getRole(org.alfresco.service.cmr.repository.NodeRef, java.lang.String)
     */
    public Role getRole(final NodeRef rmRootNode, final String role)
    {
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Role>()
        {
            public Role doWork() throws Exception
            {                
                Role result = null;
                
                String roleAuthority = authorityService.getName(AuthorityType.GROUP, getFullRoleName(role, rmRootNode));
                if (authorityService.authorityExists(roleAuthority) == true)
                {
                    String name = getShortRoleName(authorityService.getShortName(roleAuthority), rmRootNode);
                    String displayLabel = authorityService.getAuthorityDisplayName(roleAuthority);                
                    Set<String> capabilities = getCapabilities(rmRootNode, roleAuthority);
                    
                    result = new Role(name, displayLabel, capabilities, roleAuthority);
                }
                
                return result;
            }
        }, AuthenticationUtil.getAdminUserName());
    }
    
    private Set<String> getCapabilities(NodeRef rmRootNode, String roleAuthority)
    {
        Set<AccessPermission> permissions = permissionService.getAllSetPermissions(rmRootNode);
        Set<String> capabilities = new HashSet<String>(52);
        for (AccessPermission permission : permissions)

        {
            if (permission.getAuthority().equals(roleAuthority) == true)
            {
                String capabilityName = permission.getPermission();
                if (getCapability(capabilityName) != null)
                {
                    capabilities.add(permission.getPermission());
                }
            }

        }

        return capabilities;
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#existsRole(java.lang.String)
     */
    public boolean existsRole(final NodeRef rmRootNode, final String role)
    {
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Boolean>()
        {
            public Boolean doWork() throws Exception
            {                                
                String fullRoleName = authorityService.getName(AuthorityType.GROUP, getFullRoleName(role, rmRootNode));
            
                String zone = getZoneName(rmRootNode);
                Set<String> roles = authorityService.getAllAuthoritiesInZone(zone, AuthorityType.GROUP);
                return new Boolean(roles.contains(fullRoleName));
            }
        }, AuthenticationUtil.getAdminUserName()).booleanValue();
    }
    
    /*
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#hasRMAdminRole(org.alfresco.service.cmr.repository.NodeRef, java.lang.String)
     */
    public boolean hasRMAdminRole(NodeRef rmRootNode, String user)
    {
        boolean isRMAdmin = false;
        
        Set<Role> userRoles = this.getRolesByUser(rmRootNode, user);
        if (userRoles != null)
        {
            for (Role role : userRoles)
            {
                if (role.getName().equals("Administrator"))
                {
                    isRMAdmin = true;
                    break;
                }
            }
        }
        
        return isRMAdmin;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#createRole(org.alfresco.service.cmr.repository.NodeRef, java.lang.String, java.lang.String, java.util.Set)
     */
    public Role createRole(final NodeRef rmRootNode, final String role, final String roleDisplayLabel, final Set<Capability> capabilities)
    {
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Role>()
        {
            public Role doWork() throws Exception
            {
                String fullRoleName = getFullRoleName(role, rmRootNode);
                
                // Check that the role does not already exist for the rm root node
                Set<String> exists = authorityService.findAuthoritiesByShortName(AuthorityType.GROUP, fullRoleName);
                if (exists.size() != 0)
                {
                    throw new AlfrescoRuntimeException("The role " + role + " already exists for root rm node " + rmRootNode.getId());
                }
                
                // Create a group that relates to the records management role
                Set<String> zones = new HashSet<String>(2);
                zones.add(getZoneName(rmRootNode));
                zones.add(AuthorityService.ZONE_APP_DEFAULT);
                String roleGroup = authorityService.createAuthority(AuthorityType.GROUP, fullRoleName, roleDisplayLabel, zones);
                
                // Add the roleGroup to the "all" role group
                String allRoleGroup = authorityService.getName(AuthorityType.GROUP, getAllRolesGroupShortName(rmRootNode));
                authorityService.addAuthority(allRoleGroup, roleGroup);
                
                // Assign the various capabilities to the group on the root records management node
                for (Capability capability : capabilities)
                {
                    permissionService.setPermission(rmRootNode, roleGroup, capability.getName(), true);
                }
                
                // Create the role
                Set<String> capStrings = new HashSet<String>(capabilities.size());
                for (Capability capability : capabilities)
                {
                    capStrings.add(capability.getName());
                }
                return new Role(role, roleDisplayLabel, capStrings, roleGroup);
            }
        }, AuthenticationUtil.getAdminUserName());
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#updateRole(org.alfresco.service.cmr.repository.NodeRef, java.lang.String, java.lang.String, java.util.Set)
     */
    public Role updateRole(final NodeRef rmRootNode, final String role, final String roleDisplayLabel, final Set<Capability> capabilities)
    {
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Role>()
        {
            public Role doWork() throws Exception
            {                                
                String roleAuthority = authorityService.getName(AuthorityType.GROUP, getFullRoleName(role, rmRootNode));
                
                // Reset the role display name
                authorityService.setAuthorityDisplayName(roleAuthority, roleDisplayLabel);

                // TODO this needs to be improved, removing all and readding is not ideal
                
                // Clear the current capabilities
                permissionService.clearPermission(rmRootNode, roleAuthority);
                
                // Re-add the provided capabilities
                for (Capability capability : capabilities)
                {
                    permissionService.setPermission(rmRootNode, roleAuthority, capability.getName(), true);
                }
                
                Set<String> capStrings = new HashSet<String>(capabilities.size());
                for (Capability capability : capabilities)
                {
                    capStrings.add(capability.getName());
                }
                return new Role(role, roleDisplayLabel, capStrings, roleAuthority);
                
            }
        }, AuthenticationUtil.getAdminUserName());
    }

    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#deleteRole(java.lang.String)
     */
    public void deleteRole(final NodeRef rmRootNode, final String role)
    {
        AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
        {
            public Boolean doWork() throws Exception
            {                                
                String roleAuthority = authorityService.getName(AuthorityType.GROUP, getFullRoleName(role, rmRootNode));
                authorityService.deleteAuthority(roleAuthority);                
                return null;
                
            }
        }, AuthenticationUtil.getAdminUserName());
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#assignRoleToAuthority(org.alfresco.service.cmr.repository.NodeRef, java.lang.String, java.lang.String)
     */
    public void assignRoleToAuthority(final NodeRef rmRootNode, final String role, final String authorityName)
    {
        AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
        {
            public Boolean doWork() throws Exception
            {                                
                String roleAuthority = authorityService.getName(AuthorityType.GROUP, getFullRoleName(role, rmRootNode));
                authorityService.addAuthority(roleAuthority, authorityName);             
                return null;
                
            }
        }, AuthenticationUtil.getAdminUserName());
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#setPermission(org.alfresco.service.cmr.repository.NodeRef, java.lang.String, java.lang.String, boolean)
     */
    public void setPermission(final NodeRef nodeRef, final String authority, final String permission)
    {
        AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
        {
            public Boolean doWork() throws Exception
            { 
                if (nodeService.hasAspect(nodeRef, ASPECT_RECORDS_MANAGEMENT_ROOT) == false &&
                    recordsManagementService.isRecordsManagementContainer(nodeRef) == true)
                {
                    setReadPermissionUp(nodeRef, authority);
                    setPermissionDown(nodeRef, authority, permission);
                }
                else if (recordsManagementService.isRecordFolder(nodeRef) == true)
                {
                    setReadPermissionUp(nodeRef, authority);
                    setPermissionImpl(nodeRef, authority, permission);
                }
                
                return null;
            }
        }, AuthenticationUtil.getAdminUserName());
    }
    
    /**
     * Helper method to set the read permission up the hierarchy
     * 
     * @param nodeRef
     * @param authority
     */
    private void setReadPermissionUp(NodeRef nodeRef, String authority)
    {
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        if (parent != null &&
            nodeService.hasAspect(parent, ASPECT_RECORDS_MANAGEMENT_ROOT) == false)
        {
            setPermissionImpl(parent, authority, RMPermissionModel.READ_RECORDS);
            setReadPermissionUp(parent, authority);
        }
    }
    
    /**
     * Helper method to set the permission down the hierarchy
     * 
     * @param nodeRef
     * @param authority
     * @param permission
     */
    private void setPermissionDown(NodeRef nodeRef, String authority, String permission)
    {
        setPermissionImpl(nodeRef, authority, permission);
        if (recordsManagementService.isRecordsManagementContainer(nodeRef) == true)
        {
            List<ChildAssociationRef> assocs = nodeService.getChildAssocs(nodeRef, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
            for (ChildAssociationRef assoc : assocs)
            {
                NodeRef child = assoc.getChildRef();
                if (recordsManagementService.isRecordsManagementContainer(child) == true ||
                    recordsManagementService.isRecordFolder(child) == true)
                {
                    setPermissionDown(child, authority, permission);
                }
            }
        }
    }
    
    /**
     * Set the permission, taking into account that filing is a superset of read
     * 
     * @param nodeRef
     * @param authority
     * @param permission
     */
    private void setPermissionImpl(NodeRef nodeRef, String authority, String permission)
    {
        if (RMPermissionModel.FILING.equals(permission) == true)
        {
            // Remove record read permission before adding filing permission
            permissionService.deletePermission(nodeRef, authority, RMPermissionModel.READ_RECORDS);
        }
        
        permissionService.setPermission(nodeRef, authority, permission, true);
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.security.RecordsManagementSecurityService#deletePermission(org.alfresco.service.cmr.repository.NodeRef, java.lang.String, java.lang.String)
     */
    public void deletePermission(final NodeRef nodeRef, final String authority, final String permission)
    {
        AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
        {
            public Boolean doWork() throws Exception
            { 
                // Delete permission on this node
                permissionService.deletePermission(nodeRef, authority, permission);
                
                if (recordsManagementService.isRecordsManagementContainer(nodeRef) == true)
                {
                    List<ChildAssociationRef> assocs = nodeService.getChildAssocs(nodeRef, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
                    for (ChildAssociationRef assoc : assocs)
                    {
                        NodeRef child = assoc.getChildRef();
                        if (recordsManagementService.isRecordsManagementContainer(child) == true ||
                            recordsManagementService.isRecordFolder(child) == true)
                        {
                            deletePermission(child, authority, permission);
                        }
                    }
                }
                
                return null;
            }
        }, AuthenticationUtil.getAdminUserName());        
    }
}
