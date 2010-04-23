/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.security.authority;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.security.permissions.impl.AclDaoComponent;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.ISO9075;
import org.alfresco.util.SearchLanguageConversion;
import org.alfresco.util.Pair;

public class AuthorityDAOImpl implements AuthorityDAO, NodeServicePolicies.BeforeDeleteNodePolicy, NodeServicePolicies.OnUpdatePropertiesPolicy
{
    private StoreRef storeRef;

    private NodeService nodeService;

    private NamespacePrefixResolver namespacePrefixResolver;

    private QName qnameAssocSystem;

    private QName qnameAssocAuthorities;

    private QName qnameAssocZones;

    private SearchService searchService;

    private DictionaryService dictionaryService;

    private PersonService personService;
    
    private TenantService tenantService;
    
    private SimpleCache<Pair<String, String>, NodeRef> authorityLookupCache;
    
    private SimpleCache<String, Set<String>> userAuthorityCache;
    
    /** System Container ref cache (Tennant aware) */
    private Map<String, NodeRef> systemContainerRefs = new ConcurrentHashMap<String, NodeRef>(4);
    
    private AclDaoComponent aclDao;

    private PolicyComponent policyComponent;


    public AuthorityDAOImpl()
    {
        super();
    }

    public void setStoreUrl(String storeUrl)
    {
        this.storeRef = new StoreRef(storeUrl);
    }

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespacePrefixResolver(NamespacePrefixResolver namespacePrefixResolver)
    {
        this.namespacePrefixResolver = namespacePrefixResolver;
        qnameAssocSystem = QName.createQName("sys", "system", namespacePrefixResolver);
        qnameAssocAuthorities = QName.createQName("sys", "authorities", namespacePrefixResolver);
        qnameAssocZones = QName.createQName("sys", "zones", namespacePrefixResolver);
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public void setAuthorityLookupCache(SimpleCache<Pair<String, String>, NodeRef> authorityLookupCache)
    {
        this.authorityLookupCache = authorityLookupCache;
    }
    
    public void setUserAuthorityCache(SimpleCache<String, Set<String>> userAuthorityCache)
    {
        this.userAuthorityCache = userAuthorityCache;
    }

    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }
    
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }
    
    public void setAclDao(AclDaoComponent aclDao)
    {
        this.aclDao = aclDao;
    }

    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    public boolean authorityExists(String name)
    {
        NodeRef ref = getAuthorityOrNull(name);
        return ref != null;
    }

    public void addAuthority(Collection<String> parentNames, String childName)
    {
        Set<NodeRef> parentRefs = new HashSet<NodeRef>(parentNames.size() * 2);
        AuthorityType authorityType = AuthorityType.getAuthorityType(childName);
        boolean isUser = authorityType.equals(AuthorityType.USER);
        boolean notUserOrGroup = !isUser && !authorityType.equals(AuthorityType.GROUP);
        for (String parentName : parentNames)
        {
            NodeRef parentRef = getAuthorityOrNull(parentName);
            if (parentRef == null)
            {
                throw new UnknownAuthorityException("An authority was not found for " + parentName);
            }
            if (notUserOrGroup
                    && !(authorityType.equals(AuthorityType.ROLE) && AuthorityType.getAuthorityType(parentName).equals(
                            AuthorityType.ROLE)))
            {
                throw new AlfrescoRuntimeException("Authorities of the type " + authorityType
                        + " may not be added to other authorities");
            }
            parentRefs.add(parentRef);
        }
        NodeRef childRef = getAuthorityOrNull(childName);
        if (childRef == null)
        {
            throw new UnknownAuthorityException("An authority was not found for " + childName);
        }
        nodeService.addChild(parentRefs, childRef, ContentModel.ASSOC_MEMBER, QName.createQName("cm", childName,
                namespacePrefixResolver));
        if (isUser)
        {
            userAuthorityCache.remove(childName);
        }
        else
        {
            userAuthorityCache.clear();
        }
    }

    public void createAuthority(String name, String authorityDisplayName, Set<String> authorityZones)
    {
        HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(ContentModel.PROP_AUTHORITY_NAME, name);
        props.put(ContentModel.PROP_AUTHORITY_DISPLAY_NAME, authorityDisplayName);
        NodeRef childRef;
        NodeRef authorityContainerRef = getAuthorityContainer();
        childRef = nodeService.createNode(authorityContainerRef, ContentModel.ASSOC_CHILDREN, QName.createQName("cm", name, namespacePrefixResolver),
                ContentModel.TYPE_AUTHORITY_CONTAINER, props).getChildRef();
        if (authorityZones != null)
        {
            Set<NodeRef> zoneRefs = new HashSet<NodeRef>(authorityZones.size() * 2);
            for (String authorityZone : authorityZones)
            {
                zoneRefs.add(getOrCreateZone(authorityZone));
            }
            nodeService.addChild(zoneRefs, childRef, ContentModel.ASSOC_IN_ZONE, QName.createQName("cm", name, namespacePrefixResolver));
        }
        authorityLookupCache.put(cacheKey(name), childRef);
    }

    private Pair<String, String> cacheKey(String authorityName)
    {
        String tenantDomain = AuthorityType.getAuthorityType(authorityName) == AuthorityType.USER ? tenantService.getDomain(authorityName) : tenantService.getCurrentUserDomain();
        return new Pair<String, String>(tenantDomain, authorityName);
    }

    public void deleteAuthority(String name)
    {
        NodeRef nodeRef = getAuthorityOrNull(name);
        if (nodeRef == null)
        {
            throw new UnknownAuthorityException("An authority was not found for " + name);
        }
        nodeService.deleteNode(nodeRef);
        authorityLookupCache.remove(cacheKey(name));
        userAuthorityCache.clear();
    }

    public Set<String> getAllAuthorities(AuthorityType type)
    {
        Set<String> authorities = new TreeSet<String>();

        // If all users are included, we use the person service to determine the complete set of names
        if (type == null || type == AuthorityType.USER)
        {
            for (NodeRef nodeRef : personService.getAllPeople())
            {
                authorities.add(DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef,
                        ContentModel.PROP_USERNAME)));
            }
        }

        // Look under the authority container for non-person authorities
        if (type != AuthorityType.USER)
        {
            NodeRef container = getAuthorityContainer();
            if (container != null)
            {
                for (ChildAssociationRef childRef : nodeService.getChildAssocs(container,
                        ContentModel.ASSOC_CHILDREN, RegexQNamePattern.MATCH_ALL, false))
                {
                    addAuthorityNameIfMatches(authorities, childRef.getQName().getLocalName(), type, null);
                }
            }
        }
        return authorities;
    }

    
    public Set<String> findAuthorities(AuthorityType type, String parentAuthority, boolean immediate,
            String displayNamePattern, String zoneName)
    {
        Pattern pattern = displayNamePattern == null ? null : Pattern.compile(SearchLanguageConversion.convert(
                SearchLanguageConversion.DEF_LUCENE, SearchLanguageConversion.DEF_REGEX, displayNamePattern),
                Pattern.CASE_INSENSITIVE);

        // Use SQL to determine root authorities
        Set<String> rootAuthorities = null;
        if (parentAuthority == null && immediate)
        {
            NodeRef container = zoneName == null ? getAuthorityContainer() : getZone(zoneName);
            if (container == null)
            {
                // The zone doesn't even exist so there are no root authorities
                return Collections.emptySet();
            }
            rootAuthorities = getRootAuthoritiesUnderContainer(container, type);
            if (pattern == null)
            {
                return rootAuthorities;
            }
        }

        // Use a Lucene search for other criteria
        Set<String> authorities = new TreeSet<String>();
        SearchParameters sp = new SearchParameters();
        sp.addStore(this.storeRef);
        sp.setLanguage("lucene");
        StringBuilder query = new StringBuilder(500);
        if (type == null || type == AuthorityType.USER)
        {
            if (type == null)
            {
                query.append("((");
            }
            query.append("TYPE:\"").append(ContentModel.TYPE_PERSON).append("\"");
            if (displayNamePattern != null)
            {
                query.append(" AND @").append(
                        LuceneQueryParser.escape("{" + ContentModel.PROP_USERNAME.getNamespaceURI() + "}"
                                + ISO9075.encode(ContentModel.PROP_USERNAME.getLocalName()))).append(":\"").append(
                                        LuceneQueryParser.escape(displayNamePattern)).append("\"");

            }
            if (type == null)
            {
                query.append(") OR (");
            }            
        }
        if (type != AuthorityType.USER)
        {
            query.append("TYPE:\"").append(ContentModel.TYPE_AUTHORITY_CONTAINER).append("\"");
            if (displayNamePattern != null)
            {
                query.append(" AND (@").append(
                        LuceneQueryParser.escape("{" + ContentModel.PROP_AUTHORITY_NAME.getNamespaceURI() + "}"
                                + ISO9075.encode(ContentModel.PROP_AUTHORITY_NAME.getLocalName()))).append(":\"");
                // Allow for the appropriate type prefix in the authority name
                if (type == null && !displayNamePattern.startsWith("*"))
                {
                    query.append("*").append(LuceneQueryParser.escape(displayNamePattern));
                }
                else
                {
                    query.append(getName(type, LuceneQueryParser.escape(displayNamePattern)));
                }
                query.append("\" OR @").append(
                        LuceneQueryParser.escape("{" + ContentModel.PROP_AUTHORITY_DISPLAY_NAME.getNamespaceURI() + "}"
                                + ISO9075.encode(ContentModel.PROP_AUTHORITY_DISPLAY_NAME.getLocalName()))).append(
                        ":\"").append(LuceneQueryParser.escape(displayNamePattern)).append("\")");
            }
            if (type == null)
            {
                query.append("))");
            }            
        }
        if (parentAuthority != null)
        {
           query.append(" AND PATH:\"/sys:system/sys:authorities/cm:").append(ISO9075.encode(parentAuthority));
           if (!immediate)
           {
              query.append('/');
           }
           query.append("/*\"");
        }
        if (zoneName != null)
        {
            query.append(" AND PATH:\"/sys:system/sys:zones/cm:").append(ISO9075.encode(zoneName)).append("/*\"");
        }
        sp.setQuery(query.toString());
        sp.setMaxItems(100);
        ResultSet rs = null;
        try
        {
            rs = searchService.query(sp);

            for (ResultSetRow row : rs)
            {
                NodeRef nodeRef = row.getNodeRef();
                QName idProp = type != AuthorityType.USER
                        || dictionaryService.isSubClass(nodeService.getType(nodeRef),
                                ContentModel.TYPE_AUTHORITY_CONTAINER) ? ContentModel.PROP_AUTHORITY_NAME
                        : ContentModel.PROP_USERNAME;
                addAuthorityNameIfMatches(authorities, DefaultTypeConverter.INSTANCE.convert(String.class, nodeService
                        .getProperty(nodeRef, idProp)), type, pattern);
            }

            // If we asked for root authorities, we must do an intersection with the set of root authorities
            if (rootAuthorities != null)
            {
                authorities.retainAll(rootAuthorities);
            }
            return authorities;
        }
        finally
        {
            if (rs != null)
            {
                rs.close();
            }
        }

    }
    
    public Set<String> getContainedAuthorities(AuthorityType type, String name, boolean immediate)
    {
        if (AuthorityType.getAuthorityType(name).equals(AuthorityType.USER))
        {
            return Collections.<String> emptySet();
        }
        else
        {
            NodeRef nodeRef = getAuthorityOrNull(name);
            if (nodeRef == null)
            {
                throw new UnknownAuthorityException("An authority was not found for " + name);
            }

            Set<String> authorities = new TreeSet<String>();
            findAuthorities(type, nodeRef, authorities, false, !immediate, false);
            return authorities;
        }
    }

    public void removeAuthority(String parentName, String childName)
    {
        NodeRef parentRef = getAuthorityOrNull(parentName);
        if (parentRef == null)
        {
            throw new UnknownAuthorityException("An authority was not found for " + parentName);
        }
        NodeRef childRef = getAuthorityOrNull(childName);
        if (childRef == null)
        {
            throw new UnknownAuthorityException("An authority was not found for " + childName);
        }
        nodeService.removeChild(parentRef, childRef);
        if (AuthorityType.getAuthorityType(childName) == AuthorityType.USER)
        {
            userAuthorityCache.remove(childName);
        }
        else
        {
            userAuthorityCache.clear();
        }
    }

    public Set<String> getContainingAuthorities(AuthorityType type, String name, boolean immediate)
    {
        // Optimize for the case where we want all the authorities that a user belongs to
        if (!immediate && AuthorityType.getAuthorityType(name) == AuthorityType.USER)
        {
            // Get the unfiltered set of authorities from the cache or generate it
            Set<String> authorities = userAuthorityCache.get(name);
            if (authorities == null)
            {
                authorities = new TreeSet<String>();
                findAuthorities(null, name, authorities, true, true);
                userAuthorityCache.put(name, authorities);            
            }
            // If we wanted the unfiltered set we are done
            if (type == null)
            {
                return authorities;
            }
            // Apply the filtering by type
            Set<String> filteredAuthorities = new TreeSet<String>();
            for (String authority : authorities)
            {
                addAuthorityNameIfMatches(filteredAuthorities, authority, type, null);
            }
            return filteredAuthorities;
        }
        // Otherwise, crawl the DB for the answer
        else
        {
            Set<String> authorities = new TreeSet<String>();        
            findAuthorities(type, name, authorities, true, !immediate);
            return authorities;
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

    private void addAuthorityNameIfMatches(Set<String> authorities, String authorityName, AuthorityType type,
            Pattern pattern)
    {
        if (type == null || AuthorityType.getAuthorityType(authorityName).equals(type))
        {
            if (pattern == null)
            {
                authorities.add(authorityName);
            }
            else
            {
                if (pattern.matcher(getShortName(authorityName)).matches())
                {
                    authorities.add(authorityName);
                }
                else
                {
                    String displayName = getAuthorityDisplayName(authorityName);
                    if (displayName != null && pattern.matcher(displayName).matches())
                    {
                        authorities.add(authorityName);
                    }
                }
            }
        }
    }

    private void findAuthorities(AuthorityType type, String name, Set<String> authorities, boolean parents, boolean recursive)
    {
        AuthorityType localType = AuthorityType.getAuthorityType(name);
        if (localType.equals(AuthorityType.GUEST))
        {
            // Nothing to do
        }
        else
        {
            NodeRef ref = getAuthorityOrNull(name);

            if (ref != null)
            {
                findAuthorities(type, ref, authorities, parents, recursive, false);
            }
            else if (!localType.equals(AuthorityType.USER))
            {
                // Don't worry about missing person objects. It might be the system user or a user yet to be
                // auto-created
                throw new UnknownAuthorityException("An authority was not found for " + name);
            }
        }
    }

    private void findAuthorities(AuthorityType type, NodeRef nodeRef, Set<String> authorities, boolean parents, boolean recursive, boolean includeNode)
    {
        if (includeNode)
        {
            String authorityName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService
                    .getProperty(nodeRef, dictionaryService.isSubClass(nodeService.getType(nodeRef),
                            ContentModel.TYPE_AUTHORITY_CONTAINER) ? ContentModel.PROP_AUTHORITY_NAME
                            : ContentModel.PROP_USERNAME));
            addAuthorityNameIfMatches(authorities, authorityName, type, null);
        }

        // Loop over children if we want immediate children or are in recursive mode
        if (!includeNode || recursive)
        {
            if (parents)
            {
                List<ChildAssociationRef> cars = nodeService.getParentAssocs(nodeRef, ContentModel.ASSOC_MEMBER, RegexQNamePattern.MATCH_ALL);
    
                for (ChildAssociationRef car : cars)
                {
                    findAuthorities(type, car.getParentRef(), authorities, true, recursive, true);
                }
            }
            else
            {
                List<ChildAssociationRef> cars = nodeService.getChildAssocs(nodeRef, RegexQNamePattern.MATCH_ALL,
                        RegexQNamePattern.MATCH_ALL, false);

                // Take advantage of the fact that the authority name is on the child association
                for (ChildAssociationRef car : cars)
                {
                    String childName = car.getQName().getLocalName();
                    AuthorityType childType = AuthorityType.getAuthorityType(childName);
                    addAuthorityNameIfMatches(authorities, childName, type, null);
                    if (recursive && childType != AuthorityType.USER)
                    {                    
                        findAuthorities(type, car.getChildRef(), authorities, false, true, false);
                    }
                }                
            }
        }
    }

    private NodeRef getAuthorityOrNull(String name)
    {
        try
        {
            if (AuthorityType.getAuthorityType(name).equals(AuthorityType.USER))
            {
                return personService.getPerson(name, false);
            }
            else if (AuthorityType.getAuthorityType(name).equals(AuthorityType.GUEST))
            {
                return personService.getPerson(name, false);
            }
            else if (AuthorityType.getAuthorityType(name).equals(AuthorityType.ADMIN))
            {
                return personService.getPerson(name, false);
            }
            else
            {
                Pair <String, String> cacheKey = cacheKey(name);
                NodeRef result = authorityLookupCache.get(cacheKey);
                if (result == null)
                {
                    List<ChildAssociationRef> results = nodeService.getChildAssocs(getAuthorityContainer(),
                            ContentModel.ASSOC_CHILDREN, QName.createQName("cm", name, namespacePrefixResolver), false);
                    if (!results.isEmpty())
                    {
                        result = results.get(0).getChildRef();
                        authorityLookupCache.put(cacheKey, result);
                    }
                }
                return result;
            }
        }
        catch (NoSuchPersonException e)
        {
            return null;
        }
    }

    /**
     * @return Returns the authority container, <b>which must exist</b>
     */
    private NodeRef getAuthorityContainer()
    {
        return getSystemContainer(qnameAssocAuthorities);
    }

    /**
     * @return Returns the zone container, <b>which must exist</b>
     */
    private NodeRef getZoneContainer()
    {
        return getSystemContainer(qnameAssocZones);
    }

    /**
     * Return the system container for the specified assoc name.
     * The containers are cached in a thread safe Tenant aware cache.
     *
     * @param assocQName
     *
     * @return System container, <b>which must exist</b>
     */
    private NodeRef getSystemContainer(QName assocQName)
    {
        final String cacheKey = tenantService.getCurrentUserDomain() + assocQName.toString();
        NodeRef systemContainerRef = systemContainerRefs.get(cacheKey);
        if (systemContainerRef == null)
        {
            NodeRef rootNodeRef = nodeService.getRootNode(this.storeRef);
            List<ChildAssociationRef> results = nodeService.getChildAssocs(rootNodeRef, RegexQNamePattern.MATCH_ALL, qnameAssocSystem, false);
            if (results.size() == 0)
            {
                throw new AlfrescoRuntimeException("Required system path not found: " + qnameAssocSystem);
            }
            NodeRef sysNodeRef = results.get(0).getChildRef();
            results = nodeService.getChildAssocs(sysNodeRef, RegexQNamePattern.MATCH_ALL, assocQName, false);
            if (results.size() == 0)
            {
                throw new AlfrescoRuntimeException("Required path not found: " + assocQName);
            }
            systemContainerRef = results.get(0).getChildRef();
            systemContainerRefs.put(cacheKey, systemContainerRef);
        }
        return systemContainerRef;
    }

    public NodeRef getAuthorityNodeRefOrNull(String name)
    {
        return getAuthorityOrNull(name);
    }

    public String getAuthorityName(NodeRef authorityRef)
    {
        String name = null;
        if (nodeService.exists(authorityRef))
        {
            QName type = nodeService.getType(authorityRef);
            if (dictionaryService.isSubClass(type, ContentModel.TYPE_AUTHORITY_CONTAINER))
            {
                name = (String) nodeService.getProperty(authorityRef, ContentModel.PROP_AUTHORITY_NAME);
            }
            else if (dictionaryService.isSubClass(type, ContentModel.TYPE_PERSON))
            {
                name = (String) nodeService.getProperty(authorityRef, ContentModel.PROP_USERNAME);
            }
        }
        return name;
    }

    public String getAuthorityDisplayName(String authorityName)
    {
        NodeRef ref = getAuthorityOrNull(authorityName);
        if (ref == null)
        {
            return null;
        }
        Serializable value = nodeService.getProperty(ref, ContentModel.PROP_AUTHORITY_DISPLAY_NAME);
        if (value == null)
        {
            return null;
        }
        return DefaultTypeConverter.INSTANCE.convert(String.class, value);
    }

    public void setAuthorityDisplayName(String authorityName, String authorityDisplayName)
    {
        NodeRef ref = getAuthorityOrNull(authorityName);
        if (ref == null)
        {
            return;
        }
        nodeService.setProperty(ref, ContentModel.PROP_AUTHORITY_DISPLAY_NAME, authorityDisplayName);

    }

    public NodeRef getOrCreateZone(String zoneName)
    {
        return getOrCreateZone(zoneName, true);
    }

    private NodeRef getOrCreateZone(String zoneName, boolean create)
    {
        NodeRef zoneContainerRef = getZoneContainer();
        QName zoneQName = QName.createQName("cm", zoneName, namespacePrefixResolver);
        List<ChildAssociationRef> results = nodeService.getChildAssocs(zoneContainerRef, ContentModel.ASSOC_CHILDREN, zoneQName, false);
        if (results.isEmpty())
        {
            if (create)
            {
                HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
                props.put(ContentModel.PROP_NAME, zoneName);
                return nodeService.createNode(zoneContainerRef, ContentModel.ASSOC_CHILDREN, zoneQName, ContentModel.TYPE_ZONE, props).getChildRef();
            }
            else
            {
                return null;
            }
        }
        else
        {
            return results.get(0).getChildRef();
        }
    }

    public NodeRef getZone(String zoneName)
    {
        return getOrCreateZone(zoneName, false);
    }

    public Set<String> getAuthorityZones(String name)
    {
        Set<String> zones = new TreeSet<String>();
        NodeRef childRef = getAuthorityOrNull(name);
        if (childRef == null)
        {
            return null;
        }
        List<ChildAssociationRef> results = nodeService.getParentAssocs(childRef, ContentModel.ASSOC_IN_ZONE, RegexQNamePattern.MATCH_ALL);
        if (results.isEmpty())
        {
            return zones;
        }

        for (ChildAssociationRef current : results)
        {
            NodeRef zoneRef = current.getParentRef();
            Serializable value = nodeService.getProperty(zoneRef, ContentModel.PROP_NAME);
            if (value == null)
            {
                continue;
            }
            else
            {
                String zone = DefaultTypeConverter.INSTANCE.convert(String.class, value);
                zones.add(zone);
            }
        }
        return zones;
    }

    public Set<String> getAllAuthoritiesInZone(String zoneName, AuthorityType type)
    {
        Set<String> authorities = new TreeSet<String>();
        NodeRef zoneRef = getZone(zoneName);
        if (zoneRef != null)
        {
            for (ChildAssociationRef childRef : nodeService.getChildAssocs(zoneRef, ContentModel.ASSOC_IN_ZONE, RegexQNamePattern.MATCH_ALL, false))
            {
                addAuthorityNameIfMatches(authorities, childRef.getQName().getLocalName(), type, null);
            }
        }
        return authorities;
    }

    public void addAuthorityToZones(String authorityName, Set<String> zones)
    {
        if ((zones != null) && (zones.size() > 0))
        {
            Set<NodeRef> zoneRefs = new HashSet<NodeRef>(zones.size() * 2);
            for (String authorityZone : zones)
            {
                zoneRefs.add(getOrCreateZone(authorityZone));
            }
            NodeRef authRef = getAuthorityOrNull(authorityName);
            if (authRef != null)
            {
                nodeService.addChild(zoneRefs, authRef, ContentModel.ASSOC_IN_ZONE, QName.createQName("cm", authorityName, namespacePrefixResolver));
            }
        }
    }

    public void removeAuthorityFromZones(String authorityName, Set<String> zones)
    {
        if ((zones != null) && (zones.size() > 0))
        {
            NodeRef authRef = getAuthorityOrNull(authorityName);
            List<ChildAssociationRef> results = nodeService.getParentAssocs(authRef, ContentModel.ASSOC_IN_ZONE, RegexQNamePattern.MATCH_ALL);
            for (ChildAssociationRef current : results)
            {
                NodeRef zoneRef = current.getParentRef();
                Serializable value = nodeService.getProperty(zoneRef, ContentModel.PROP_NAME);
                if (value == null)
                {
                    continue;
                }
                else
                {
                    String testZone = DefaultTypeConverter.INSTANCE.convert(String.class, value);
                    if (zones.contains(testZone))
                    {
                        nodeService.removeChildAssociation(current);
                    }
                }
            }
        }
    }
        
    private Set<String> getRootAuthoritiesUnderContainer(NodeRef container, AuthorityType type)
    {
        if (type != null && type.equals(AuthorityType.USER))
        {
            return Collections.<String> emptySet();
        }        
        Collection<ChildAssociationRef> childRefs = nodeService.getChildAssocsWithoutParentAssocsOfType(container, ContentModel.ASSOC_MEMBER);
        Set<String> authorities = new TreeSet<String>();
        for (ChildAssociationRef childRef : childRefs)
        {
            addAuthorityNameIfMatches(authorities, childRef.getQName().getLocalName(), type, null);
        }
        return authorities;        
    }

    // Listen out for person removals so that we can clear cached authorities
    public void beforeDeleteNode(NodeRef nodeRef)
    {
        userAuthorityCache.remove(getAuthorityName(nodeRef));        
    }

    public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after)
    {
        boolean isAuthority = dictionaryService.isSubClass(nodeService.getType(nodeRef),
                ContentModel.TYPE_AUTHORITY_CONTAINER);
        QName idProp = isAuthority ? ContentModel.PROP_AUTHORITY_NAME  : ContentModel.PROP_USERNAME;
        String authBefore = DefaultTypeConverter.INSTANCE.convert(String.class, before.get(idProp));
        if (authBefore == null)
        {
            // Node has just been created; nothing to do
            return;
        }
        String authAfter = DefaultTypeConverter.INSTANCE.convert(String.class, after.get(idProp));
        if (!EqualsHelper.nullSafeEquals(authBefore, authAfter))
        {
            if (authBefore.equalsIgnoreCase(authAfter))
            {
                if (isAuthority)
                {
                    // Fix any ACLs
                    aclDao.updateAuthority(authBefore, authAfter);

                    // Fix primary association local name
                    QName newAssocQName = QName.createQName("cm", authAfter, namespacePrefixResolver);
                    ChildAssociationRef assoc = nodeService.getPrimaryParent(nodeRef);
                    nodeService.moveNode(nodeRef, assoc.getParentRef(), assoc.getTypeQName(), newAssocQName);

                    // Fix other non-case sensitive parent associations
                    QName oldAssocQName = QName.createQName("cm", authBefore, namespacePrefixResolver);
                    newAssocQName = QName.createQName("cm", authAfter, namespacePrefixResolver);
                    for (ChildAssociationRef parent : nodeService.getParentAssocs(nodeRef))
                    {
                        if (!parent.isPrimary() && parent.getQName().equals(oldAssocQName))
                        {
                            nodeService.removeChildAssociation(parent);
                            nodeService.addChild(parent.getParentRef(), parent.getChildRef(), parent.getTypeQName(),
                                    newAssocQName);
                        }
                    }
                    authorityLookupCache.clear();

                    // Cache is out of date
                    userAuthorityCache.clear();
                }
                else
                {
                    userAuthorityCache.remove(authBefore);
                }

            }
            else
            {
                throw new UnsupportedOperationException("The name of an authority can not be changed");
            }
        }
    }

    public void init()
    {
        // Listen out for person removals so that we can clear cached authorities
        this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "beforeDeleteNode"), ContentModel.TYPE_PERSON, new JavaBehaviour(
                this, "beforeDeleteNode"));
        // Listen out for updates to persons and authority containers to handle renames
        this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateProperties"), ContentModel.TYPE_AUTHORITY, new JavaBehaviour(
                this, "onUpdateProperties"));
    }
}
