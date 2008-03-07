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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.repo.security.permissions.impl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.providers.dao.User;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.avm.AVMRepository;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessControlEntry;
import org.alfresco.repo.security.permissions.AccessControlList;
import org.alfresco.repo.security.permissions.DynamicAuthority;
import org.alfresco.repo.security.permissions.NodePermissionEntry;
import org.alfresco.repo.security.permissions.PermissionEntry;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.PermissionServiceSPI;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionContext;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * The Alfresco implementation of a permissions service against our APIs for the permissions model and permissions
 * persistence.
 * 
 * @author andyh
 */
public class PermissionServiceImpl implements PermissionServiceSPI, InitializingBean
{

    static SimplePermissionReference OLD_ALL_PERMISSIONS_REFERENCE = new SimplePermissionReference(QName.createQName("", PermissionService.ALL_PERMISSIONS),
            PermissionService.ALL_PERMISSIONS);

    private static Log log = LogFactory.getLog(PermissionServiceImpl.class);

    /** a transactionally-safe cache to be injected */
    private SimpleCache<Serializable, AccessStatus> accessCache;

    /*
     * Access to the model
     */
    private ModelDAO modelDAO;

    /*
     * Access to permissions
     */
    private PermissionsDaoComponent permissionsDaoComponent;

    /*
     * Access to the node service
     */
    private NodeService nodeService;

    /*
     * Access to the tenant service
     */
    private TenantService tenantService;

    /*
     * Access to the data dictionary
     */
    private DictionaryService dictionaryService;

    /*
     * Access to the authentication component
     */
    private AuthenticationComponent authenticationComponent;

    /*
     * Access to the authority component
     */
    private AuthorityService authorityService;

    /*
     * Dynamic authorities providers
     */
    private List<DynamicAuthority> dynamicAuthorities;

    private PolicyComponent policyComponent;

    private AclDaoComponent aclDaoComponent;

    /*
     * Standard spring construction.
     */
    public PermissionServiceImpl()
    {
        super();
    }

    //
    // Inversion of control
    //

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    public void setModelDAO(ModelDAO modelDAO)
    {
        this.modelDAO = modelDAO;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }

    public void setPermissionsDaoComponent(PermissionsDaoComponent permissionsDaoComponent)
    {
        this.permissionsDaoComponent = permissionsDaoComponent;
    }

    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }

    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }

    public void setDynamicAuthorities(List<DynamicAuthority> dynamicAuthorities)
    {
        this.dynamicAuthorities = dynamicAuthorities;
    }

    public void setAclDaoComponent(AclDaoComponent aclDaoComponent)
    {
        this.aclDaoComponent = aclDaoComponent;
    }

    /**
     * Set the permissions access cache.
     * 
     * @param accessCache
     *            a transactionally safe cache
     */
    public void setAccessCache(SimpleCache<Serializable, AccessStatus> accessCache)
    {
        this.accessCache = accessCache;
    }

    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    public void onMoveNode(ChildAssociationRef oldChildAssocRef, ChildAssociationRef newChildAssocRef)
    {
        accessCache.clear();
    }

    public void afterPropertiesSet() throws Exception
    {
        if (dictionaryService == null)
        {
            throw new IllegalArgumentException("Property 'dictionaryService' has not been set");
        }
        if (modelDAO == null)
        {
            throw new IllegalArgumentException("Property 'modelDAO' has not been set");
        }
        if (nodeService == null)
        {
            throw new IllegalArgumentException("Property 'nodeService' has not been set");
        }
        if (permissionsDaoComponent == null)
        {
            throw new IllegalArgumentException("Property 'permissionsDAO' has not been set");
        }
        if (authenticationComponent == null)
        {
            throw new IllegalArgumentException("Property 'authenticationComponent' has not been set");
        }
        if (authorityService == null)
        {
            throw new IllegalArgumentException("Property 'authorityService' has not been set");
        }
        if (accessCache == null)
        {
            throw new IllegalArgumentException("Property 'accessCache' has not been set");
        }
        if (policyComponent == null)
        {
            throw new IllegalArgumentException("Property 'policyComponent' has not been set");
        }
        if (aclDaoComponent == null)
        {
            throw new IllegalArgumentException("Property 'aclDaoComponent' has not been set");
        }

        policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onMoveNode"), ContentModel.TYPE_BASE, new JavaBehaviour(this, "onMoveNode"));

    }

    //
    // Permissions Service
    //

    public String getOwnerAuthority()
    {
        return OWNER_AUTHORITY;
    }

    public String getAllAuthorities()
    {
        return ALL_AUTHORITIES;
    }

    public String getAllPermission()
    {
        return ALL_PERMISSIONS;
    }

    public Set<AccessPermission> getPermissions(NodeRef nodeRef)
    {
        return getAllPermissionsImpl(nodeRef, true, true);
    }

    public Set<AccessPermission> getAllSetPermissions(NodeRef nodeRef)
    {
        HashSet<AccessPermission> accessPermissions = new HashSet<AccessPermission>();
        NodePermissionEntry nodePremissionEntry = getSetPermissions(nodeRef);
        for (PermissionEntry pe : nodePremissionEntry.getPermissionEntries())
        {
            accessPermissions.add(new AccessPermissionImpl(getPermission(pe.getPermissionReference()), pe.getAccessStatus(), pe.getAuthority()));
        }
        return accessPermissions;
    }

    public Set<AccessPermission> getAllSetPermissions(StoreRef storeRef)
    {
        HashSet<AccessPermission> accessPermissions = new HashSet<AccessPermission>();
        NodePermissionEntry nodePremissionEntry = getSetPermissions(storeRef);
        for (PermissionEntry pe : nodePremissionEntry.getPermissionEntries())
        {
            accessPermissions.add(new AccessPermissionImpl(getPermission(pe.getPermissionReference()), pe.getAccessStatus(), pe.getAuthority()));
        }
        return accessPermissions;
    }

    private Set<AccessPermission> getAllPermissionsImpl(NodeRef nodeRef, boolean includeTrue, boolean includeFalse)
    {
        String userName = authenticationComponent.getCurrentUserName();
        HashSet<AccessPermission> accessPermissions = new HashSet<AccessPermission>();
        for (PermissionReference pr : getSettablePermissionReferences(nodeRef))
        {
            if (hasPermission(nodeRef, pr) == AccessStatus.ALLOWED)
            {
                accessPermissions.add(new AccessPermissionImpl(getPermission(pr), AccessStatus.ALLOWED, userName));
            }
            else
            {
                if (includeFalse)
                {
                    accessPermissions.add(new AccessPermissionImpl(getPermission(pr), AccessStatus.DENIED, userName));
                }
            }
        }
        return accessPermissions;
    }

    public Set<String> getSettablePermissions(NodeRef nodeRef)
    {
        Set<PermissionReference> settable = getSettablePermissionReferences(nodeRef);
        Set<String> strings = new HashSet<String>(settable.size());
        for (PermissionReference pr : settable)
        {
            strings.add(getPermission(pr));
        }
        return strings;
    }

    public Set<String> getSettablePermissions(QName type)
    {
        Set<PermissionReference> settable = getSettablePermissionReferences(type);
        Set<String> strings = new LinkedHashSet<String>(settable.size());
        for (PermissionReference pr : settable)
        {
            strings.add(getPermission(pr));
        }
        return strings;
    }

    public NodePermissionEntry getSetPermissions(NodeRef nodeRef)
    {
        return permissionsDaoComponent.getPermissions(tenantService.getName(nodeRef));
    }

    public NodePermissionEntry getSetPermissions(StoreRef storeRef)
    {
        return permissionsDaoComponent.getPermissions(storeRef);
    }

    public AccessStatus hasPermission(final NodeRef nodeRefIn, final PermissionReference permIn)
    {
        // If the node ref is null there is no sensible test to do - and there
        // must be no permissions
        // - so we allow it
        if (nodeRefIn == null)
        {
            return AccessStatus.ALLOWED;
        }

        final NodeRef nodeRef = tenantService.getName(nodeRefIn);

        // If the permission is null we deny
        if (permIn == null)
        {
            return AccessStatus.DENIED;
        }

        // AVM nodes - test for existence underneath
        if (nodeRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_AVM))
        {
            return doAvmCan(nodeRef, permIn);
        }
        
        // Allow permissions for nodes that do not exist
        if (!nodeService.exists(nodeRef))
        {
            return AccessStatus.ALLOWED;
        }

      

        final PermissionReference perm;
        if (permIn.equals(OLD_ALL_PERMISSIONS_REFERENCE))
        {
            perm = getAllPermissionReference();
        }
        else
        {
            perm = permIn;
        }

        if(AuthenticationUtil.getCurrentEffectiveUserName() == null)
        {
            return AccessStatus.DENIED;
        }
        
        if(AuthenticationUtil.getCurrentEffectiveUserName().equals(AuthenticationUtil.getSystemUserName()))
        {
            return AccessStatus.ALLOWED;
        }
        
        // Get the current authentications
        // Use the smart authentication cache to improve permissions performance
        Authentication auth = AuthenticationUtil.getCurrentEffectiveAuthentication();
        final Set<String> authorisations = getAuthorisations(auth, nodeRef);

        // If the node does not support the given permission there is no point
        // doing the test
        Set<PermissionReference> available = AuthenticationUtil.runAs(new RunAsWork<Set<PermissionReference>>()
        {
            public Set<PermissionReference> doWork() throws Exception
            {
                return modelDAO.getAllPermissions(nodeRef);
            }

        }, AuthenticationUtil.getSystemUserName());

        available.add(getAllPermissionReference());
        available.add(OLD_ALL_PERMISSIONS_REFERENCE);

        final Serializable key = generateKey(authorisations, nodeRef, perm, CacheType.HAS_PERMISSION);
        if (!(available.contains(perm)))
        {
            accessCache.put(key, AccessStatus.DENIED);
            return AccessStatus.DENIED;
        }

        if (authenticationComponent.getCurrentUserName().equals(authenticationComponent.getSystemUserName()))
        {
            return AccessStatus.ALLOWED;
        }

        return AuthenticationUtil.runAs(new RunAsWork<AccessStatus>()
        {

            public AccessStatus doWork() throws Exception
            {

                AccessStatus status = accessCache.get(key);
                if (status != null)
                {
                    return status;
                }

                //
                // TODO: Dynamic permissions via evaluators
                //

                /*
                 * Does the current authentication have the supplied permission on the given node.
                 */

                QName typeQname = nodeService.getType(nodeRef);
                Set<QName> aspectQNames = nodeService.getAspects(nodeRef);

                NodeTest nt = new NodeTest(perm, typeQname, aspectQNames);
                boolean result = nt.evaluate(authorisations, nodeRef);
                if (log.isDebugEnabled())
                {
                    log.debug("Permission <"
                            + perm + "> is " + (result ? "allowed" : "denied") + " for " + authenticationComponent.getCurrentUserName() + " on node "
                            + nodeService.getPath(nodeRef));
                }

                status = result ? AccessStatus.ALLOWED : AccessStatus.DENIED;
                accessCache.put(key, status);
                return status;
            }
        }, AuthenticationUtil.getSystemUserName());

    }

    private AccessStatus doAvmCan(NodeRef nodeRef, PermissionReference permission)
    {
        org.alfresco.util.Pair<Integer, String> avmVersionPath = AVMNodeConverter.ToAVMVersionPath(nodeRef);
        int version = avmVersionPath.getFirst();
        String path = avmVersionPath.getSecond();
        boolean result = AVMRepository.GetInstance().can(nodeRef.getStoreRef().getIdentifier(), version, path, permission.getName());
        AccessStatus status = result ? AccessStatus.ALLOWED : AccessStatus.DENIED;
        return status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.service.cmr.security.PermissionService#hasPermission(java.lang.Long, java.lang.String,
     *      java.lang.String)
     */
    public AccessStatus hasPermission(Long aclID, PermissionContext context, String permission)
    {
        return hasPermission(aclID, context, getPermissionReference(permission));
    }

    public AccessStatus hasPermission(Long aclId, PermissionContext context, PermissionReference permission)
    {
        if (aclId == null)
        {
            return AccessStatus.ALLOWED;
        }

        if (permission == null)
        {
            return AccessStatus.DENIED;
        }

        if(AuthenticationUtil.getCurrentEffectiveUserName() == null)
        {
            return AccessStatus.DENIED;
        }
        
        if(AuthenticationUtil.getCurrentEffectiveUserName().equals(AuthenticationUtil.getSystemUserName()))
        {
            return AccessStatus.ALLOWED;
        }
        
        // Get the current authentications
        // Use the smart authentication cache to improve permissions performance
        Authentication auth = AuthenticationUtil.getCurrentEffectiveAuthentication();
        if (auth == null)
        {
            throw new IllegalStateException("Unauthenticated");
        }

        Set<String> authorisations = getAuthorisations(auth, context);

        // If the node does not support the given permission there is no point
        // doing the test

        QName typeQname = context.getType();
        Set<QName> aspectQNames = context.getAspects();

        Set<PermissionReference> available = modelDAO.getAllPermissions(typeQname, aspectQNames);
        available.add(getAllPermissionReference());
        available.add(OLD_ALL_PERMISSIONS_REFERENCE);

        if (!(available.contains(permission)))
        {
            return AccessStatus.DENIED;
        }

        if (authenticationComponent.getCurrentUserName().equals(authenticationComponent.getSystemUserName()))
        {
            return AccessStatus.ALLOWED;
        }

        if (permission.equals(OLD_ALL_PERMISSIONS_REFERENCE))
        {
            permission = getAllPermissionReference();
        }

        boolean result;
        if (context.getStoreAcl() == null)
        {
            AclTest aclTest = new AclTest(permission, typeQname, aspectQNames);
            result = aclTest.evaluate(authorisations, aclId);
        }
        else
        {
            Set<String> storeAuthorisations = getAuthorisations(auth, (PermissionContext)null);
            AclTest aclTest = new AclTest(permission, typeQname, aspectQNames);
            result = aclTest.evaluate(authorisations, aclId) && aclTest.evaluate(storeAuthorisations, context.getStoreAcl());
        }
        AccessStatus status = result ? AccessStatus.ALLOWED : AccessStatus.DENIED;
        return status;

    }

    enum CacheType
    {
        HAS_PERMISSION, SINGLE_PERMISSION, SINGLE_PERMISSION_GLOBAL;
    }

    /**
     * Key for a cache object is built from all the known Authorities (which can change dynamically so they must all be
     * used) the NodeRef ID and the permission reference itself. This gives a unique key for each permission test.
     */
    static Serializable generateKey(Set<String> auths, NodeRef nodeRef, PermissionReference perm, CacheType type)
    {
        LinkedHashSet<Serializable> key = new LinkedHashSet<Serializable>();
        key.add(perm.toString());
        key.addAll(auths);
        key.add(nodeRef);
        key.add(type);
        return key;
    }

    /**
     * Get the authorisations for the currently authenticated user
     * 
     * @param auth
     * @return
     */
    private Set<String> getAuthorisations(Authentication auth, NodeRef nodeRef)
    {
        nodeRef = tenantService.getName(nodeRef);

        HashSet<String> auths = new HashSet<String>();
        // No authenticated user then no permissions
        if (auth == null)
        {
            return auths;
        }
        // TODO: Refactor and use the authentication service for this.
        User user = (User) auth.getPrincipal();
        
        String username = user.getUsername();
        auths.add(username);
        
        if (tenantService.getBaseNameUser(username).equalsIgnoreCase(PermissionService.GUEST_AUTHORITY))
        {
        	auths.add(PermissionService.GUEST_AUTHORITY);
        }
        
        for (GrantedAuthority authority : auth.getAuthorities())
        {
            auths.add(authority.getAuthority());
        }
        if (nodeRef != null)
        {
            if (dynamicAuthorities != null)
            {
                for (DynamicAuthority da : dynamicAuthorities)
                {
                    if (da.hasAuthority(nodeRef, username))
                    {
                        auths.add(da.getAuthority());
                    }
                }
            }
        }
        auths.addAll(authorityService.getAuthorities());
        return auths;
    }

    private Set<String> getAuthorisations(Authentication auth, PermissionContext context)
    {
        HashSet<String> auths = new HashSet<String>();
        // No authenticated user then no permissions
        if (auth == null)
        {
            return auths;
        }
        // TODO: Refactor and use the authentication service for this.
        User user = (User) auth.getPrincipal();
        auths.add(user.getUsername());
        for (GrantedAuthority authority : auth.getAuthorities())
        {
            auths.add(authority.getAuthority());
        }
        auths.addAll(authorityService.getAuthorities());

        if (context != null)
        {
            Map<String, Set<String>> dynamicAuthorityAssignments = context.getDynamicAuthorityAssignment();
            HashSet<String> dynAuths = new HashSet<String>();
            for (String current : auths)
            {
                Set<String> dynos = dynamicAuthorityAssignments.get(current);
                if (dynos != null)
                {
                    dynAuths.addAll(dynos);
                }
            }
            auths.addAll(dynAuths);
        }

        return auths;
    }

    public NodePermissionEntry explainPermission(NodeRef nodeRef, PermissionReference perm)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void clearPermission(StoreRef storeRef, String authority)
    {
        permissionsDaoComponent.deletePermissions(storeRef, authority);
        accessCache.clear();

    }

    public void deletePermission(StoreRef storeRef, String authority, String perm)
    {
        deletePermission(storeRef, authority, getPermissionReference(perm));
    }

    public void deletePermission(StoreRef storeRef, String authority, PermissionReference perm)
    {
        permissionsDaoComponent.deletePermission(storeRef, authority, perm);
        accessCache.clear();
    }

    public void deletePermissions(StoreRef storeRef)
    {
        permissionsDaoComponent.deletePermissions(storeRef);
        accessCache.clear();

    }

    public void setPermission(StoreRef storeRef, String authority, String perm, boolean allow)
    {
        setPermission(storeRef, authority, getPermissionReference(perm), allow);
    }

    public void setPermission(StoreRef storeRef, String authority, PermissionReference permission, boolean allow)
    {
        permissionsDaoComponent.setPermission(storeRef, authority, permission, allow);
        accessCache.clear();

    }

    public void deletePermissions(NodeRef nodeRef)
    {
        permissionsDaoComponent.deletePermissions(tenantService.getName(nodeRef));
        accessCache.clear();
    }

    public void deletePermissions(NodePermissionEntry nodePermissionEntry)
    {
        permissionsDaoComponent.deletePermissions(tenantService.getName(nodePermissionEntry.getNodeRef()));
        accessCache.clear();
    }

    /**
     * @see #deletePermission(NodeRef, String, PermissionReference)
     */
    public void deletePermission(PermissionEntry permissionEntry)
    {
        NodeRef nodeRef = permissionEntry.getNodeRef();
        String authority = permissionEntry.getAuthority();
        PermissionReference permission = permissionEntry.getPermissionReference();
        deletePermission(nodeRef, authority, permission);
    }

    public void deletePermission(NodeRef nodeRef, String authority, PermissionReference perm)
    {
        permissionsDaoComponent.deletePermission(tenantService.getName(nodeRef), authority, perm);
        accessCache.clear();
    }

    public void clearPermission(NodeRef nodeRef, String authority)
    {
        permissionsDaoComponent.deletePermissions(tenantService.getName(nodeRef), authority);
        accessCache.clear();
    }

    public void setPermission(NodeRef nodeRef, String authority, PermissionReference perm, boolean allow)
    {
        permissionsDaoComponent.setPermission(tenantService.getName(nodeRef), authority, perm, allow);
        accessCache.clear();
    }

    public void setPermission(PermissionEntry permissionEntry)
    {
    	// TODO - not MT-enabled nodeRef - currently only used by tests
        permissionsDaoComponent.setPermission(permissionEntry);
        accessCache.clear();
    }

    public void setPermission(NodePermissionEntry nodePermissionEntry)
    {
    	// TODO - not MT-enabled nodeRef- currently only used by tests
        permissionsDaoComponent.setPermission(nodePermissionEntry);
        accessCache.clear();
    }

    public void setInheritParentPermissions(NodeRef nodeRef, boolean inheritParentPermissions)
    {
        permissionsDaoComponent.setInheritParentPermissions(tenantService.getName(nodeRef), inheritParentPermissions);
        accessCache.clear();
    }

    /**
     * @see org.alfresco.service.cmr.security.PermissionService#getInheritParentPermissions(org.alfresco.service.cmr.repository.NodeRef)
     */
    public boolean getInheritParentPermissions(NodeRef nodeRef)
    {
        return permissionsDaoComponent.getInheritParentPermissions(tenantService.getName(nodeRef));
    }

    public PermissionReference getPermissionReference(QName qname, String permissionName)
    {
        return modelDAO.getPermissionReference(qname, permissionName);
    }

    public PermissionReference getAllPermissionReference()
    {
        return getPermissionReference(ALL_PERMISSIONS);
    }

    public String getPermission(PermissionReference permissionReference)
    {
        if (modelDAO.isUnique(permissionReference))
        {
            return permissionReference.getName();
        }
        else
        {
            return permissionReference.toString();
        }
    }

    public PermissionReference getPermissionReference(String permissionName)
    {
        return modelDAO.getPermissionReference(null, permissionName);
    }

    public Set<PermissionReference> getSettablePermissionReferences(QName type)
    {
        return modelDAO.getExposedPermissions(type);
    }

    public Set<PermissionReference> getSettablePermissionReferences(NodeRef nodeRef)
    {
        return modelDAO.getExposedPermissions(tenantService.getName(nodeRef));
    }

    public void deletePermission(NodeRef nodeRef, String authority, String perm)
    {
        deletePermission(nodeRef, authority, getPermissionReference(perm));
    }

    public AccessStatus hasPermission(NodeRef nodeRef, String perm)
    {
        return hasPermission(nodeRef, getPermissionReference(perm));
    }

    public void setPermission(NodeRef nodeRef, String authority, String perm, boolean allow)
    {
        setPermission(nodeRef, authority, getPermissionReference(perm), allow);
    }

    public void deletePermissions(String recipient)
    {
        permissionsDaoComponent.deletePermissions(recipient);
        accessCache.clear();
    }

    //
    // SUPPORT CLASSES
    //

    /**
     * Support class to test the permission on a node.
     * 
     * @author Andy Hind
     */
    private class NodeTest
    {
        /*
         * The required permission.
         */
        PermissionReference required;

        /*
         * Granters of the permission
         */
        Set<PermissionReference> granters;

        /*
         * The additional permissions required at the node level.
         */
        Set<PermissionReference> nodeRequirements = new HashSet<PermissionReference>();

        /*
         * The additional permissions required on the parent.
         */
        Set<PermissionReference> parentRequirements = new HashSet<PermissionReference>();

        /*
         * The permissions required on all children .
         */
        Set<PermissionReference> childrenRequirements = new HashSet<PermissionReference>();

        /*
         * The type name of the node.
         */
        QName typeQName;

        /*
         * The aspects set on the node.
         */
        Set<QName> aspectQNames;

        /*
         * Constructor just gets the additional requirements
         */
        NodeTest(PermissionReference required, QName typeQName, Set<QName> aspectQNames)
        {
            this.required = required;
            this.typeQName = typeQName;
            this.aspectQNames = aspectQNames;

            // Set the required node permissions
            if (required.equals(getPermissionReference(ALL_PERMISSIONS)))
            {
                nodeRequirements = modelDAO.getRequiredPermissions(getPermissionReference(PermissionService.FULL_CONTROL), typeQName, aspectQNames, RequiredPermission.On.NODE);
            }
            else
            {
                nodeRequirements = modelDAO.getRequiredPermissions(required, typeQName, aspectQNames, RequiredPermission.On.NODE);
            }

            parentRequirements = modelDAO.getRequiredPermissions(required, typeQName, aspectQNames, RequiredPermission.On.PARENT);

            childrenRequirements = modelDAO.getRequiredPermissions(required, typeQName, aspectQNames, RequiredPermission.On.CHILDREN);

            // Find all the permissions that grant the allowed permission
            // All permissions are treated specially.
            granters = new LinkedHashSet<PermissionReference>(128, 1.0f);
            granters.addAll(modelDAO.getGrantingPermissions(required));
            granters.add(getAllPermissionReference());
            granters.add(OLD_ALL_PERMISSIONS_REFERENCE);
        }

        /**
         * External hook point
         * 
         * @param authorisations
         * @param nodeRef
         * @return
         */
        boolean evaluate(Set<String> authorisations, NodeRef nodeRef)
        {
            Set<Pair<String, PermissionReference>> denied = new HashSet<Pair<String, PermissionReference>>();
            return evaluate(authorisations, nodeRef, denied, null);
        }

        /**
         * Internal hook point for recursion
         * 
         * @param authorisations
         * @param nodeRef
         * @param denied
         * @param recursiveIn
         * @return
         */
        boolean evaluate(Set<String> authorisations, NodeRef nodeRef, Set<Pair<String, PermissionReference>> denied, MutableBoolean recursiveIn)
        {
            // Do we defer our required test to a parent (yes if not null)
            MutableBoolean recursiveOut = null;

            Set<Pair<String, PermissionReference>> locallyDenied = new HashSet<Pair<String, PermissionReference>>();
            locallyDenied.addAll(denied);
            locallyDenied.addAll(getDenied(nodeRef));

            // Start out true and "and" all other results
            boolean success = true;

            // Check the required permissions but not for sets they rely on
            // their underlying permissions
            if (modelDAO.checkPermission(required))
            {
                if (parentRequirements.contains(required))
                {
                    if (checkGlobalPermissions(authorisations) || checkRequired(authorisations, nodeRef, locallyDenied))
                    {
                        // No need to do the recursive test as it has been found
                        if (recursiveIn != null)
                        {
                            recursiveIn.setValue(true);
                        }
                    }
                    else
                    {
                        // Much cheaper to do this as we go then check all the
                        // stack values for each parent
                        recursiveOut = new MutableBoolean(false);
                    }
                }
                else
                {
                    // We have to do the test as no parent will help us out
                    success &= hasSinglePermission(authorisations, nodeRef);
                }
                if (!success)
                {
                    return false;
                }
            }

            // Check the other permissions required on the node
            for (PermissionReference pr : nodeRequirements)
            {
                // Build a new test
                NodeTest nt = new NodeTest(pr, typeQName, aspectQNames);
                success &= nt.evaluate(authorisations, nodeRef, locallyDenied, null);
                if (!success)
                {
                    return false;
                }
            }

            // Check the permission required of the parent

            if (success)
            {
                ChildAssociationRef car = nodeService.getPrimaryParent(nodeRef);
                if (car.getParentRef() != null)
                {

                    NodePermissionEntry nodePermissions = permissionsDaoComponent.getPermissions(car.getChildRef());
                    if ((nodePermissions == null) || (nodePermissions.inheritPermissions()))
                    {

                        locallyDenied.addAll(getDenied(car.getParentRef()));
                        for (PermissionReference pr : parentRequirements)
                        {
                            if (pr.equals(required))
                            {
                                // Recursive permission
                                success &= this.evaluate(authorisations, car.getParentRef(), locallyDenied, recursiveOut);
                                if ((recursiveOut != null) && recursiveOut.getValue())
                                {
                                    if (recursiveIn != null)
                                    {
                                        recursiveIn.setValue(true);
                                    }
                                }
                            }
                            else
                            {
                                NodeTest nt = new NodeTest(pr, typeQName, aspectQNames);
                                success &= nt.evaluate(authorisations, car.getParentRef(), locallyDenied, null);
                            }

                            if (!success)
                            {
                                return false;
                            }
                        }
                    }
                }
            }

            if ((recursiveOut != null) && (!recursiveOut.getValue()))
            {
                // The required authentication was not resolved in recursion
                return false;
            }

            // Check permissions required of children
            if (childrenRequirements.size() > 0)
            {
                List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(nodeRef);
                for (PermissionReference pr : childrenRequirements)
                {
                    for (ChildAssociationRef child : childAssocRefs)
                    {
                        success &= (hasPermission(child.getChildRef(), pr) == AccessStatus.ALLOWED);
                        if (!success)
                        {
                            return false;
                        }
                    }
                }
            }

            return success;
        }

        public boolean hasSinglePermission(Set<String> authorisations, NodeRef nodeRef)
        {
        	nodeRef = tenantService.getName(nodeRef);
        	
            Serializable key = generateKey(authorisations, nodeRef, this.required, CacheType.SINGLE_PERMISSION_GLOBAL);

            AccessStatus status = accessCache.get(key);
            if (status != null)
            {
                return status == AccessStatus.ALLOWED;
            }

            // Check global permission

            if (checkGlobalPermissions(authorisations))
            {
                accessCache.put(key, AccessStatus.ALLOWED);
                return true;
            }

            Set<Pair<String, PermissionReference>> denied = new HashSet<Pair<String, PermissionReference>>();

            return hasSinglePermission(authorisations, nodeRef, denied);

        }

        public boolean hasSinglePermission(Set<String> authorisations, NodeRef nodeRef, Set<Pair<String, PermissionReference>> denied)
        {
        	nodeRef = tenantService.getName(nodeRef);
        	
            // Add any denied permission to the denied list - these can not
            // then
            // be used to given authentication.
            // A -> B -> C
            // If B denies all permissions to any - allowing all permissions
            // to
            // andy at node A has no effect

            denied.addAll(getDenied(nodeRef));

            // Cache non denied
            Serializable key = null;
            if (denied.size() == 0)
            {
                key = generateKey(authorisations, nodeRef, this.required, CacheType.SINGLE_PERMISSION);
            }
            if (key != null)
            {
                AccessStatus status = accessCache.get(key);
                if (status != null)
                {
                    return status == AccessStatus.ALLOWED;
                }
            }

            // If the current node allows the permission we are done
            // The test includes any parent or ancestor requirements
            if (checkRequired(authorisations, nodeRef, denied))
            {
                if (key != null)
                {
                    accessCache.put(key, AccessStatus.ALLOWED);
                }
                return true;
            }

            // Permissions are only evaluated up the primary parent chain
            // TODO: Do not ignore non primary permissions
            ChildAssociationRef car = nodeService.getPrimaryParent(nodeRef);

            // Build the next element of the evaluation chain
            if (car.getParentRef() != null)
            {
                NodePermissionEntry nodePermissions = permissionsDaoComponent.getPermissions(nodeRef);
                if ((nodePermissions == null) || (nodePermissions.inheritPermissions()))
                {
                    if (hasSinglePermission(authorisations, car.getParentRef(), denied))
                    {
                        if (key != null)
                        {
                            accessCache.put(key, AccessStatus.ALLOWED);
                        }
                        return true;
                    }
                    else
                    {
                        if (key != null)
                        {
                            accessCache.put(key, AccessStatus.DENIED);
                        }
                        return false;
                    }
                }
                else
                {
                    if (key != null)
                    {
                        accessCache.put(key, AccessStatus.DENIED);
                    }
                    return false;
                }
            }
            else
            {
                if (key != null)
                {
                    accessCache.put(key, AccessStatus.DENIED);
                }
                return false;
            }
        }

        /**
         * Check if we have a global permission
         * 
         * @param authorisations
         * @return
         */
        private boolean checkGlobalPermissions(Set<String> authorisations)
        {
            for (PermissionEntry pe : modelDAO.getGlobalPermissionEntries())
            {
                if (isGranted(pe, authorisations, null))
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * Get the list of permissions denied for this node.
         * 
         * @param nodeRef
         * @return
         */
        Set<Pair<String, PermissionReference>> getDenied(NodeRef nodeRef)
        {
            Set<Pair<String, PermissionReference>> deniedSet = new HashSet<Pair<String, PermissionReference>>();

            // Loop over all denied permissions
            NodePermissionEntry nodeEntry = permissionsDaoComponent.getPermissions(nodeRef);
            if (nodeEntry != null)
            {
                for (PermissionEntry pe : nodeEntry.getPermissionEntries())
                {
                    if (pe.isDenied())
                    {
                        // All the sets that grant this permission must be
                        // denied
                        // Note that granters includes the orginal permission
                        Set<PermissionReference> granters = modelDAO.getGrantingPermissions(pe.getPermissionReference());
                        for (PermissionReference granter : granters)
                        {
                            deniedSet.add(new Pair<String, PermissionReference>(pe.getAuthority(), granter));
                        }

                        // All the things granted by this permission must be
                        // denied
                        Set<PermissionReference> grantees = modelDAO.getGranteePermissions(pe.getPermissionReference());
                        for (PermissionReference grantee : grantees)
                        {
                            deniedSet.add(new Pair<String, PermissionReference>(pe.getAuthority(), grantee));
                        }

                        // All permission excludes all permissions available for
                        // the node.
                        if (pe.getPermissionReference().equals(getAllPermissionReference()) || pe.getPermissionReference().equals(OLD_ALL_PERMISSIONS_REFERENCE))
                        {
                            for (PermissionReference deny : modelDAO.getAllPermissions(nodeRef))
                            {
                                deniedSet.add(new Pair<String, PermissionReference>(pe.getAuthority(), deny));
                            }
                        }
                    }
                }
            }
            return deniedSet;
        }

        /**
         * Check that a given authentication is available on a node
         * 
         * @param authorisations
         * @param nodeRef
         * @param denied
         * @return
         */
        boolean checkRequired(Set<String> authorisations, NodeRef nodeRef, Set<Pair<String, PermissionReference>> denied)
        {
            NodePermissionEntry nodeEntry = permissionsDaoComponent.getPermissions(nodeRef);

            // No permissions set - short cut to deny
            if (nodeEntry == null)
            {
                return false;
            }

            // Check if each permission allows - the first wins.
            // We could have other voting style mechanisms here
            for (PermissionEntry pe : nodeEntry.getPermissionEntries())
            {
                if (isGranted(pe, authorisations, denied))
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * Is a permission granted
         * 
         * @param pe -
         *            the permissions entry to consider
         * @param granters -
         *            the set of granters
         * @param authorisations -
         *            the set of authorities
         * @param denied -
         *            the set of denied permissions/authority pais
         * @return
         */
        private boolean isGranted(PermissionEntry pe, Set<String> authorisations, Set<Pair<String, PermissionReference>> denied)
        {
            // If the permission entry denies then we just deny
            if (pe.isDenied())
            {
                return false;
            }

            // The permission is allowed but we deny it as it is in the denied
            // set

            if (denied != null)
            {
                Pair<String, PermissionReference> specific = new Pair<String, PermissionReference>(pe.getAuthority(), required);
                if (denied.contains(specific))
                {
                    return false;
                }
            }

            // any deny denies

            if (false)
            {
                if (denied != null)
                {
                    for (String auth : authorisations)
                    {
                        Pair<String, PermissionReference> specific = new Pair<String, PermissionReference>(auth, required);
                        if (denied.contains(specific))
                        {
                            return false;
                        }
                        for (PermissionReference perm : granters)
                        {
                            specific = new Pair<String, PermissionReference>(auth, perm);
                            if (denied.contains(specific))
                            {
                                return false;
                            }
                        }
                    }
                }
            }

            // If the permission has a match in both the authorities and
            // granters list it is allowed
            // It applies to the current user and it is granted
            if (authorisations.contains(pe.getAuthority()) && granters.contains(pe.getPermissionReference()))
            {
                {
                    return true;
                }
            }

            // Default deny
            return false;
        }

    }

    /**
     * Test a permission in the context of the new ACL implementation. All components of the ACL are in the object -
     * there is no need to walk up the parent chain. Parent conditions cna not be applied as there is no context to do
     * this. Child conditions can not be applied as there is no context to do this
     * 
     * @author andyh
     */

    private class AclTest
    {
        /*
         * The required permission.
         */
        PermissionReference required;

        /*
         * Granters of the permission
         */
        Set<PermissionReference> granters;

        /*
         * The additional permissions required at the node level.
         */
        Set<PermissionReference> nodeRequirements = new HashSet<PermissionReference>();

        /*
         * The type name of the node.
         */
        QName typeQName;

        /*
         * The aspects set on the node.
         */
        Set<QName> aspectQNames;

        /*
         * Constructor just gets the additional requirements
         */
        AclTest(PermissionReference required, QName typeQName, Set<QName> aspectQNames)
        {
            this.required = required;
            this.typeQName = typeQName;
            this.aspectQNames = aspectQNames;

            // Set the required node permissions
            if (required.equals(getPermissionReference(ALL_PERMISSIONS)))
            {
                nodeRequirements = modelDAO.getRequiredPermissions(getPermissionReference(PermissionService.FULL_CONTROL), typeQName, aspectQNames, RequiredPermission.On.NODE);
            }
            else
            {
                nodeRequirements = modelDAO.getRequiredPermissions(required, typeQName, aspectQNames, RequiredPermission.On.NODE);
            }

            if (modelDAO.getRequiredPermissions(required, typeQName, aspectQNames, RequiredPermission.On.PARENT).size() > 0)
            {
                throw new IllegalStateException("Parent permissions can not be checked for an acl");
            }

            if (modelDAO.getRequiredPermissions(required, typeQName, aspectQNames, RequiredPermission.On.CHILDREN).size() > 0)
            {
                throw new IllegalStateException("Child permissions can not be checked for an acl");
            }

            // Find all the permissions that grant the allowed permission
            // All permissions are treated specially.
            granters = new LinkedHashSet<PermissionReference>(128, 1.0f);
            granters.addAll(modelDAO.getGrantingPermissions(required));
            granters.add(getAllPermissionReference());
            granters.add(OLD_ALL_PERMISSIONS_REFERENCE);
        }

        /**
         * Internal hook point for recursion
         * 
         * @param authorisations
         * @param nodeRef
         * @param denied
         * @param recursiveIn
         * @return
         */
        boolean evaluate(Set<String> authorisations, Long aclId)
        {
            // Do we defer our required test to a parent (yes if not null)
            MutableBoolean recursiveOut = null;

            // Start out true and "and" all other results
            boolean success = true;

            // Check the required permissions but not for sets they rely on
            // their underlying permissions
            if (modelDAO.checkPermission(required))
            {

                // We have to do the test as no parent will help us out
                success &= hasSinglePermission(authorisations, aclId);

                if (!success)
                {
                    return false;
                }
            }

            // Check the other permissions required on the node
            for (PermissionReference pr : nodeRequirements)
            {
                // Build a new test
                AclTest nt = new AclTest(pr, typeQName, aspectQNames);
                success &= nt.evaluate(authorisations, aclId);
                if (!success)
                {
                    return false;
                }
            }

            return success;
        }

        public boolean hasSinglePermission(Set<String> authorisations, Long aclId)
        {
            // Check global permission

            if (checkGlobalPermissions(authorisations))
            {
                return true;
            }

            return checkRequired(authorisations, aclId);

        }

        /**
         * Check if we have a global permission
         * 
         * @param authorisations
         * @return
         */
        private boolean checkGlobalPermissions(Set<String> authorisations)
        {
            for (PermissionEntry pe : modelDAO.getGlobalPermissionEntries())
            {
                if (isGranted(pe, authorisations))
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * Check that a given authentication is available on a node
         * 
         * @param authorisations
         * @param nodeRef
         * @param denied
         * @return
         */
        boolean checkRequired(Set<String> authorisations, Long aclId)
        {
            AccessControlList acl = aclDaoComponent.getAccessControlList(aclId);

            if (acl == null)
            {
                return false;
            }

            Set<Pair<String, PermissionReference>> denied = new HashSet<Pair<String, PermissionReference>>();

            // Check if each permission allows - the first wins.
            // We could have other voting style mechanisms here
            for (AccessControlEntry ace : acl.getEntries())
            {
                if (isGranted(ace, authorisations, denied))
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * Is a permission granted
         * 
         * @param pe -
         *            the permissions entry to consider
         * @param granters -
         *            the set of granters
         * @param authorisations -
         *            the set of authorities
         * @param denied -
         *            the set of denied permissions/authority pais
         * @return
         */
        private boolean isGranted(AccessControlEntry ace, Set<String> authorisations, Set<Pair<String, PermissionReference>> denied)
        {
            // If the permission entry denies then we just deny
            if (ace.getAccessStatus() == AccessStatus.DENIED)
            {
                denied.add(new Pair<String, PermissionReference>(ace.getAuthority(), ace.getPermission()));
                return false;
            }

            // The permission is allowed but we deny it as it is in the denied
            // set

            if (denied != null)
            {
                Pair<String, PermissionReference> specific = new Pair<String, PermissionReference>(ace.getAuthority(), required);
                if (denied.contains(specific))
                {
                    return false;
                }
            }

            // any deny denies

            if (false)
            {
                if (denied != null)
                {
                    for (String auth : authorisations)
                    {
                        Pair<String, PermissionReference> specific = new Pair<String, PermissionReference>(auth, required);
                        if (denied.contains(specific))
                        {
                            return false;
                        }
                        for (PermissionReference perm : granters)
                        {
                            specific = new Pair<String, PermissionReference>(auth, perm);
                            if (denied.contains(specific))
                            {
                                return false;
                            }
                        }
                    }
                }
            }

            // If the permission has a match in both the authorities and
            // granters list it is allowed
            // It applies to the current user and it is granted
            if (authorisations.contains(ace.getAuthority()) && granters.contains(ace.getPermission()))
            {
                {
                    return true;
                }
            }

            // Default deny
            return false;
        }

        private boolean isGranted(PermissionEntry pe, Set<String> authorisations)
        {
            // If the permission entry denies then we just deny
            if (pe.isDenied())
            {
                return false;
            }

            // If the permission has a match in both the authorities and
            // granters list it is allowed
            // It applies to the current user and it is granted
            if (authorisations.contains(pe.getAuthority()) && granters.contains(pe.getPermissionReference()))
            {
                {
                    return true;
                }
            }

            // Default deny
            return false;
        }

    }

    /**
     * Helper class to store a pair of objects which may be null
     * 
     * @author Andy Hind
     */
    private static class Pair<A, B>
    {
        A a;

        B b;

        Pair(A a, B b)
        {
            this.a = a;
            this.b = b;
        }

        A getA()
        {
            return a;
        }

        B getB()
        {
            return b;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(this instanceof Pair))
            {
                return false;
            }
            Pair other = (Pair) o;
            return EqualsHelper.nullSafeEquals(this.getA(), other.getA()) && EqualsHelper.nullSafeEquals(this.getB(), other.getB());
        }

        @Override
        public int hashCode()
        {
            return (((a == null) ? 0 : a.hashCode()) * 37) + ((b == null) ? 0 : b.hashCode());
        }

    }

    private static class MutableBoolean
    {
        private boolean value;

        MutableBoolean(boolean value)
        {
            this.value = value;
        }

        void setValue(boolean value)
        {
            this.value = value;
        }

        boolean getValue()
        {
            return value;
        }
    }

    public Map<NodeRef, Set<AccessPermission>> getAllSetPermissionsForCurrentUser()
    {
        String currentUser = authenticationComponent.getCurrentUserName();
        return getAllSetPermissionsForAuthority(currentUser);
    }

    public Map<NodeRef, Set<AccessPermission>> getAllSetPermissionsForAuthority(String authority)
    {
        return permissionsDaoComponent.getAllSetPermissions(authority);
    }

    public Set<NodeRef> findNodesByAssignedPermissionForCurrentUser(String permission, boolean allow, boolean includeContainingAuthorities, boolean exactPermissionMatch)
    {
        String currentUser = authenticationComponent.getCurrentUserName();
        return findNodesByAssignedPermission(currentUser, permission, allow, includeContainingAuthorities, exactPermissionMatch);
    }

    public Set<NodeRef> findNodesByAssignedPermission(String authority, String permission, boolean allow, boolean includeContainingAuthorities, boolean includeContainingPermissions)
    {
        // TODO: owned nodes and add owner rights ??
        // Does not include dynamic permissions (they would have to be done by query - e.g. owership and OWNER rights)
        // Does not include ACEGI auth object authorities
        Set<String> authorities = new HashSet<String>();
        authorities.add(authority);
        if (includeContainingAuthorities)
        {
            authorities.addAll(authorityService.getAuthoritiesForUser(authority));
        }

        HashSet<NodeRef> answer = new HashSet<NodeRef>();

        PermissionReference pr = getPermissionReference(permission);
        Set<PermissionReference> permissions = new HashSet<PermissionReference>();
        permissions.add(pr);

        if (includeContainingPermissions)
        {
            permissions.addAll(modelDAO.getGrantingPermissions(pr));
        }

        for (PermissionReference perm : permissions)
        {
            for (String auth : authorities)
            {
                answer.addAll(permissionsDaoComponent.findNodeByPermission(auth, perm, allow));
            }
        }
        return answer;
    }
}
