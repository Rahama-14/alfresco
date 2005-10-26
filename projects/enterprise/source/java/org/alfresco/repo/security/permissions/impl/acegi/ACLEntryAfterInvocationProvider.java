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
package org.alfresco.repo.security.permissions.impl.acegi;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import net.sf.acegisecurity.AccessDeniedException;
import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.ConfigAttribute;
import net.sf.acegisecurity.ConfigAttributeDefinition;
import net.sf.acegisecurity.afterinvocation.AfterInvocationProvider;

import org.alfresco.repo.security.permissions.impl.SimplePermissionReference;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

public class ACLEntryAfterInvocationProvider implements AfterInvocationProvider, InitializingBean
{
    private static Log log = LogFactory.getLog(ACLEntryAfterInvocationProvider.class);

    private static final String AFTER_ACL_NODE = "AFTER_ACL_NODE";

    private static final String AFTER_ACL_PARENT = "AFTER_ACL_PARENT";

    private PermissionService permissionService;

    private NamespacePrefixResolver nspr;

    private NodeService nodeService;

    private AuthenticationService authenticationService;

    public ACLEntryAfterInvocationProvider()
    {
        super();
    }

    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    public PermissionService getPermissionService()
    {
        return permissionService;
    }

    public NamespacePrefixResolver getNamespacePrefixResolver()
    {
        return nspr;
    }

    public void setNamespacePrefixResolver(NamespacePrefixResolver nspr)
    {
        this.nspr = nspr;
    }

    public NodeService getNodeService()
    {
        return nodeService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public AuthenticationService getAuthenticationService()
    {
        return authenticationService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }

    public void afterPropertiesSet() throws Exception
    {
        if (permissionService == null)
        {
            throw new IllegalArgumentException("There must be a permission service");
        }
        if (nspr == null)
        {
            throw new IllegalArgumentException("There must be a namespace service");
        }
        if (nodeService == null)
        {
            throw new IllegalArgumentException("There must be a node service");
        }
        if (authenticationService == null)
        {
            throw new IllegalArgumentException("There must be an authentication service");
        }

    }

    public Object decide(Authentication authentication, Object object, ConfigAttributeDefinition config,
            Object returnedObject) throws AccessDeniedException
    {
        if (log.isDebugEnabled())
        {
            MethodInvocation mi = (MethodInvocation) object;
            log.debug("Method: " + mi.getMethod().toString());
        }
        try
        {
            if (authenticationService.isCurrentUserTheSystemUser())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Allowing system user access");
                }
                return returnedObject;
            }
            else if (returnedObject == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Allowing null object access");
                }
                return null;
            }
            else if (StoreRef.class.isAssignableFrom(returnedObject.getClass()))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Store access");
                }
                return decide(authentication, object, config, nodeService.getRootNode((StoreRef) returnedObject))
                        .getStoreRef();
            }
            else if (NodeRef.class.isAssignableFrom(returnedObject.getClass()))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Node access");
                }
                return decide(authentication, object, config, (NodeRef) returnedObject);
            }
            else if (ChildAssociationRef.class.isAssignableFrom(returnedObject.getClass()))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Child Association access");
                }
                return decide(authentication, object, config, (ChildAssociationRef) returnedObject);
            }
            else if (ResultSet.class.isAssignableFrom(returnedObject.getClass()))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Result Set access");
                }
                return decide(authentication, object, config, (ResultSet) returnedObject);
            }
            else if (Collection.class.isAssignableFrom(returnedObject.getClass()))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Collection Access");
                }
                return decide(authentication, object, config, (Collection) returnedObject);
            }
            else if (returnedObject.getClass().isArray())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Array Access");
                }
                return decide(authentication, object, config, (Object[]) returnedObject);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Uncontrolled object - access allowed for " + object.getClass().getName());
                }
                return returnedObject;
            }
        }
        catch (AccessDeniedException ade)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Access denied");
                ade.printStackTrace();
            }
            throw ade;
        }
        catch (RuntimeException re)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Access denied by runtime exception");
                re.printStackTrace();
            }
            throw re;
        }

    }

    public NodeRef decide(Authentication authentication, Object object, ConfigAttributeDefinition config,
            NodeRef returnedObject) throws AccessDeniedException

    {
        if (returnedObject == null)
        {
            return null;
        }

        List<ConfigAttributeDefintion> supportedDefinitions = extractSupportedDefinitions(config);

        if (supportedDefinitions.size() == 0)
        {
            return returnedObject;
        }

        for (ConfigAttributeDefintion cad : supportedDefinitions)
        {
            NodeRef testNodeRef = null;

            if (cad.typeString.equals(AFTER_ACL_NODE))
            {
                testNodeRef = returnedObject;
            }
            else if (cad.typeString.equals(AFTER_ACL_PARENT))
            {
                testNodeRef = nodeService.getPrimaryParent(returnedObject).getParentRef();
            }

            if ((testNodeRef != null)
                    && (permissionService.hasPermission(testNodeRef, cad.required.toString()) == AccessStatus.DENIED))
            {
                throw new AccessDeniedException("Access Denied");
            }

        }

        return returnedObject;
    }

    private List<ConfigAttributeDefintion> extractSupportedDefinitions(ConfigAttributeDefinition config)
    {
        List<ConfigAttributeDefintion> definitions = new ArrayList<ConfigAttributeDefintion>();
        Iterator iter = config.getConfigAttributes();

        while (iter.hasNext())
        {
            ConfigAttribute attr = (ConfigAttribute) iter.next();

            if (this.supports(attr))
            {
                definitions.add(new ConfigAttributeDefintion(attr));
            }

        }
        return definitions;
    }

    public ChildAssociationRef decide(Authentication authentication, Object object, ConfigAttributeDefinition config,
            ChildAssociationRef returnedObject) throws AccessDeniedException

    {
        if (returnedObject == null)
        {
            return null;
        }

        List<ConfigAttributeDefintion> supportedDefinitions = extractSupportedDefinitions(config);

        if (supportedDefinitions.size() == 0)
        {
            return returnedObject;
        }

        for (ConfigAttributeDefintion cad : supportedDefinitions)
        {
            NodeRef testNodeRef = null;

            if (cad.typeString.equals(AFTER_ACL_NODE))
            {
                testNodeRef = ((ChildAssociationRef) returnedObject).getChildRef();
            }
            else if (cad.typeString.equals(AFTER_ACL_PARENT))
            {
                testNodeRef = ((ChildAssociationRef) returnedObject).getParentRef();
            }

            if ((testNodeRef != null)
                    && (permissionService.hasPermission(testNodeRef, cad.required.toString()) == AccessStatus.DENIED))
            {
                throw new AccessDeniedException("Access Denied");
            }

        }

        return returnedObject;
    }

    public ResultSet decide(Authentication authentication, Object object, ConfigAttributeDefinition config,
            ResultSet returnedObject) throws AccessDeniedException

    {
        FilteringResultSet filteringResultSet = new FilteringResultSet((ResultSet) returnedObject);

        if (returnedObject == null)
        {
            return null;
        }

        List<ConfigAttributeDefintion> supportedDefinitions = extractSupportedDefinitions(config);

        if (supportedDefinitions.size() == 0)
        {
            return returnedObject;
        }

        for (int i = 0; i < returnedObject.length(); i++)
        {
            for (ConfigAttributeDefintion cad : supportedDefinitions)
            {
                filteringResultSet.setIncluded(i, true);
                NodeRef testNodeRef = null;
                if (cad.typeString.equals(AFTER_ACL_NODE))
                {
                    testNodeRef = returnedObject.getNodeRef(i);
                }
                else if (cad.typeString.equals(AFTER_ACL_PARENT))
                {
                    testNodeRef = returnedObject.getChildAssocRef(i).getParentRef();
                }

                if (filteringResultSet.getIncluded(i)
                        && (testNodeRef != null)
                        && (permissionService.hasPermission(testNodeRef, cad.required.toString()) == AccessStatus.DENIED))
                {
                    filteringResultSet.setIncluded(i, false);
                }
            }
        }

        return filteringResultSet;
    }

    public Collection decide(Authentication authentication, Object object, ConfigAttributeDefinition config,
            Collection returnedObject) throws AccessDeniedException

    {
        if (returnedObject == null)
        {
            return null;
        }

        List<ConfigAttributeDefintion> supportedDefinitions = extractSupportedDefinitions(config);

        if (supportedDefinitions.size() == 0)
        {
            return returnedObject;
        }

        Set<Object> removed = new HashSet<Object>();

        if (log.isDebugEnabled())
        {
            log.debug("Entries are " + supportedDefinitions);
        }

        for (Object nextObject : returnedObject)
        {
            boolean allowed = true;
            for (ConfigAttributeDefintion cad : supportedDefinitions)
            {
                NodeRef testNodeRef = null;

                if (cad.typeString.equals(AFTER_ACL_NODE))
                {
                    if (StoreRef.class.isAssignableFrom(nextObject.getClass()))
                    {
                        testNodeRef = nodeService.getRootNode((StoreRef) nextObject);
                        if (log.isDebugEnabled())
                        {
                            log.debug("\tNode Test on store " + nodeService.getPath(testNodeRef));
                        }
                    }
                    else if (NodeRef.class.isAssignableFrom(nextObject.getClass()))
                    {
                        testNodeRef = (NodeRef) nextObject;
                        if (log.isDebugEnabled())
                        {
                            log.debug("\tNode Test on node " + nodeService.getPath(testNodeRef));
                        }
                    }
                    else if (ChildAssociationRef.class.isAssignableFrom(nextObject.getClass()))
                    {
                        testNodeRef = ((ChildAssociationRef) nextObject).getChildRef();
                        if (log.isDebugEnabled())
                        {
                            log.debug("\tNode Test on child association ref using " + nodeService.getPath(testNodeRef));
                        }
                    }
                    else
                    {
                        throw new ACLEntryVoterException(
                                "The specified parameter is not a collection of NodeRefs or ChildAssociationRefs");
                    }
                }
                else if (cad.typeString.equals(AFTER_ACL_PARENT))
                {
                    if (StoreRef.class.isAssignableFrom(nextObject.getClass()))
                    {
                        // Will be allowed
                        testNodeRef = null;
                        if (log.isDebugEnabled())
                        {
                            log.debug("\tParent Test on store ");
                        }
                    }
                    else if (NodeRef.class.isAssignableFrom(nextObject.getClass()))
                    {
                        testNodeRef = nodeService.getPrimaryParent((NodeRef) nextObject).getParentRef();
                        if (log.isDebugEnabled())
                        {
                            log.debug("\tParent test on node " + nodeService.getPath(testNodeRef));
                        }
                    }
                    else if (ChildAssociationRef.class.isAssignableFrom(nextObject.getClass()))
                    {
                        testNodeRef = ((ChildAssociationRef) nextObject).getParentRef();
                        if (log.isDebugEnabled())
                        {
                            log.debug("\tParent Test on child association ref using "
                                    + nodeService.getPath(testNodeRef));
                        }
                    }
                    else
                    {
                        throw new ACLEntryVoterException(
                                "The specified parameter is not a collection of NodeRefs or ChildAssociationRefs");
                    }
                }

                if (allowed
                        && (testNodeRef != null)
                        && (permissionService.hasPermission(testNodeRef, cad.required.toString()) == AccessStatus.DENIED))
                {
                    allowed = false;
                }
            }
            if (!allowed)
            {
                removed.add(nextObject);
            }
        }
        for (Object toRemove : removed)
        {
            while (returnedObject.remove(toRemove))
                ;
        }
        return returnedObject;
    }

    public Object[] decide(Authentication authentication, Object object, ConfigAttributeDefinition config,
            Object[] returnedObject) throws AccessDeniedException

    {
        BitSet incudedSet = new BitSet(returnedObject.length);

        if (returnedObject == null)
        {
            return null;
        }

        List<ConfigAttributeDefintion> supportedDefinitions = extractSupportedDefinitions(config);

        if (supportedDefinitions.size() == 0)
        {
            return returnedObject;
        }

        for (int i = 0, l = returnedObject.length; i < l; i++)
        {
            Object current = returnedObject[i];
            for (ConfigAttributeDefintion cad : supportedDefinitions)
            {
                incudedSet.set(i, true);
                NodeRef testNodeRef = null;
                if (cad.typeString.equals(AFTER_ACL_NODE))
                {
                    if (StoreRef.class.isAssignableFrom(current.getClass()))
                    {
                        testNodeRef = nodeService.getRootNode((StoreRef) current);
                    }
                    else if (NodeRef.class.isAssignableFrom(current.getClass()))
                    {
                        testNodeRef = (NodeRef) current;
                    }
                    else if (ChildAssociationRef.class.isAssignableFrom(current.getClass()))
                    {
                        testNodeRef = ((ChildAssociationRef) current).getChildRef();
                    }
                    else
                    {
                        throw new ACLEntryVoterException("The specified array is not of NodeRef or ChildAssociationRef");
                    }
                }

                else if (cad.typeString.equals(AFTER_ACL_PARENT))
                {
                    if (StoreRef.class.isAssignableFrom(current.getClass()))
                    {
                        testNodeRef = null;
                    }
                    else if (NodeRef.class.isAssignableFrom(current.getClass()))
                    {
                        testNodeRef = nodeService.getPrimaryParent((NodeRef) current).getParentRef();
                    }
                    else if (ChildAssociationRef.class.isAssignableFrom(current.getClass()))
                    {
                        testNodeRef = ((ChildAssociationRef) current).getParentRef();
                    }
                    else
                    {
                        throw new ACLEntryVoterException("The specified array is not of NodeRef or ChildAssociationRef");
                    }
                }

                if (incudedSet.get(i)
                        && (testNodeRef != null)
                        && (permissionService.hasPermission(testNodeRef, cad.required.toString()) == AccessStatus.DENIED))
                {
                    incudedSet.set(i, false);
                }

            }
        }

        if (incudedSet.cardinality() == returnedObject.length)
        {
            return returnedObject;
        }
        else
        {
            Object[] answer = new Object[incudedSet.cardinality()];
            for (int i = incudedSet.nextSetBit(0), p = 0; i >= 0; i = incudedSet.nextSetBit(++i), p++)
            {
                answer[p] = returnedObject[i];
            }
            return answer;
        }
    }

    public boolean supports(ConfigAttribute attribute)
    {
        if ((attribute.getAttribute() != null)
                && (attribute.getAttribute().startsWith(AFTER_ACL_NODE) || attribute.getAttribute().startsWith(
                        AFTER_ACL_PARENT)))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean supports(Class clazz)
    {
        return (MethodInvocation.class.isAssignableFrom(clazz));
    }

    private class ConfigAttributeDefintion
    {

        String typeString;

        SimplePermissionReference required;

        ConfigAttributeDefintion(ConfigAttribute attr)
        {

            StringTokenizer st = new StringTokenizer(attr.getAttribute(), ".", false);
            if (st.countTokens() != 3)
            {
                throw new ACLEntryVoterException("There must be three . separated tokens in each config attribute");
            }
            typeString = st.nextToken();
            String qNameString = st.nextToken();
            String permissionString = st.nextToken();

            if (!(typeString.equals(AFTER_ACL_NODE) || typeString.equals(AFTER_ACL_PARENT)))
            {
                throw new ACLEntryVoterException("Invalid type: must be ACL_NODE or ACL_PARENT");
            }

            QName qName = QName.createQName(qNameString, nspr);

            required = new SimplePermissionReference(qName, permissionString);
        }
    }
}
