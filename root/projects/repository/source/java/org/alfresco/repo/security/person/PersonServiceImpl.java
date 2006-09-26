/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.security.person;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.permissions.PermissionServiceSPI;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;

public class PersonServiceImpl implements PersonService
{
    public static final String SYSTEM_FOLDER = "/sys:system";

    public static final String PEOPLE_FOLDER = SYSTEM_FOLDER + "/sys:people";

    // IOC

    private StoreRef storeRef;

    private NodeService nodeService;

    private SearchService searchService;

    private AuthorityService authorityService;

    private PermissionServiceSPI permissionServiceSPI;

    private NamespacePrefixResolver namespacePrefixResolver;

    private boolean createMissingPeople;

    private static Set<QName> mutableProperties;

    private boolean userNamesAreCaseSensitive = false;
    
    private String defaultHomeFolderProvider;

    static
    {
        Set<QName> props = new HashSet<QName>();
        props.add(ContentModel.PROP_HOMEFOLDER);
        props.add(ContentModel.PROP_FIRSTNAME);
        // Middle Name
        props.add(ContentModel.PROP_LASTNAME);
        props.add(ContentModel.PROP_EMAIL);
        props.add(ContentModel.PROP_ORGID);
        mutableProperties = Collections.unmodifiableSet(props);
    }

    public PersonServiceImpl()
    {
        super();
    }

    public boolean getUserNamesAreCaseSensitive()
    {
        return userNamesAreCaseSensitive;
    }

    public void setUserNamesAreCaseSensitive(boolean userNamesAreCaseSensitive)
    {
        this.userNamesAreCaseSensitive = userNamesAreCaseSensitive;
    }
    
    void setDefaultHomeFolderProvider(String defaultHomeFolderProvider)
    {
        this.defaultHomeFolderProvider = defaultHomeFolderProvider;
    }

    public NodeRef getPerson(String userName)
    {
        NodeRef personNode = getPersonOrNull(userName);
        if (personNode == null)
        {
            if (createMissingPeople())
            {
                return createMissingPerson(userName);
            }
            else
            {
                throw new NoSuchPersonException(userName);
            }

        }
        else
        {
            return personNode;
        }
    }

    public boolean personExists(String caseSensitiveUserName)
    {
        return getPersonOrNull(caseSensitiveUserName) != null;
    }

    public NodeRef getPersonOrNull(String searchUserName)
    {
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery("TYPE:\\{http\\://www.alfresco.org/model/content/1.0\\}person +@cm\\:userName:\"" + searchUserName
                + "\"");
        sp.addStore(storeRef);
        sp.excludeDataInTheCurrentTransaction(false);

        ResultSet rs = null;

        try
        {
            rs = searchService.query(sp);

            NodeRef returnRef = null;

            for (ResultSetRow row : rs)
            {

                NodeRef nodeRef = row.getNodeRef();
                if (nodeService.exists(nodeRef))
                {
                    String realUserName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(
                            nodeRef, ContentModel.PROP_USERNAME));

                    if (userNamesAreCaseSensitive)
                    {
                        if (realUserName.equals(searchUserName))
                        {
                            if (returnRef == null)
                            {
                                returnRef = nodeRef;
                            }
                            else
                            {
                                throw new AlfrescoRuntimeException("Found more than one user for " + searchUserName
                                        + " (case sensitive)");
                            }
                        }
                    }
                    else
                    {
                        if (realUserName.equalsIgnoreCase(searchUserName))
                        {
                            if (returnRef == null)
                            {
                                returnRef = nodeRef;
                            }
                            else
                            {
                                throw new AlfrescoRuntimeException("Found more than one user for " + searchUserName
                                        + " (case insensitive)");
                            }
                        }
                    }
                }
            }
            
            return returnRef;
        }
        finally
        {
            if (rs != null)
            {
                rs.close();
            }
        }
    }

    public boolean createMissingPeople()
    {
        return createMissingPeople;
    }

    public Set<QName> getMutableProperties()
    {
        return mutableProperties;
    }

    public void setPersonProperties(String userName, Map<QName, Serializable> properties)
    {
        NodeRef personNode = getPersonOrNull(userName);
        if (personNode == null)
        {
            if (createMissingPeople())
            {
                personNode = createMissingPerson(userName);
            }
            else
            {
                throw new PersonException("No person found for user name " + userName);
            }

        }
        else
        {
            String realUserName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(personNode,
                    ContentModel.PROP_USERNAME));
            properties.put(ContentModel.PROP_USERNAME, realUserName);
        }

        nodeService.setProperties(personNode, properties);
    }

    public boolean isMutable()
    {
        return true;
    }

    private NodeRef createMissingPerson(String userName)
    {
        HashMap<QName, Serializable> properties = getDefaultProperties(userName);
        return createPerson(properties);
    }

    private HashMap<QName, Serializable> getDefaultProperties(String userName)
    {
        HashMap<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ContentModel.PROP_USERNAME, userName);
        properties.put(ContentModel.PROP_FIRSTNAME, userName);
        properties.put(ContentModel.PROP_LASTNAME, "");
        properties.put(ContentModel.PROP_EMAIL, "");
        properties.put(ContentModel.PROP_ORGID, "");
        properties.put(ContentModel.PROP_HOME_FOLDER_PROVIDER, defaultHomeFolderProvider);
        return properties;
    }

    public NodeRef createPerson(Map<QName, Serializable> properties)
    {
        String userName = DefaultTypeConverter.INSTANCE.convert(String.class, properties
                .get(ContentModel.PROP_USERNAME));
        properties.put(ContentModel.PROP_USERNAME, userName);
        return nodeService.createNode(getPeopleContainer(), ContentModel.ASSOC_CHILDREN, ContentModel.TYPE_PERSON,
                ContentModel.TYPE_PERSON, properties).getChildRef();
    }

    public NodeRef getPeopleContainer()
    {
        NodeRef rootNodeRef = nodeService.getRootNode(storeRef);
        List<NodeRef> results = searchService.selectNodes(rootNodeRef, PEOPLE_FOLDER, null, namespacePrefixResolver,
                false);
        if (results.size() == 0)
        {
            throw new AlfrescoRuntimeException("Required people system path not found: " + PEOPLE_FOLDER);
        }
        else
        {
            return results.get(0);
        }
    }

    public void deletePerson(String userName)
    {
        NodeRef personNodeRef = getPersonOrNull(userName);

        // delete the person
        if (personNodeRef != null)
        {
            nodeService.deleteNode(personNodeRef);
        }

        // remove user from any containing authorities
        Set<String> containerAuthorities = authorityService.getContainingAuthorities(null, userName, true);
        for (String containerAuthority : containerAuthorities)
        {
            authorityService.removeAuthority(containerAuthority, userName);
        }

        // remove any user permissions
        permissionServiceSPI.deletePermissions(userName);
    }

    public Set<NodeRef> getAllPeople()
    {
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery("TYPE:\"" + ContentModel.TYPE_PERSON + "\"");
        sp.addStore(storeRef);
        sp.excludeDataInTheCurrentTransaction(false);

        LinkedHashSet<NodeRef> nodes = new LinkedHashSet<NodeRef>();
        ResultSet rs = null;

        try
        {
            rs = searchService.query(sp);

            for (ResultSetRow row : rs)
            {

                NodeRef nodeRef = row.getNodeRef();
                if (nodeService.exists(nodeRef))
                {
                    nodes.add(nodeRef);
                }
            }
        }
        finally
        {
            if (rs != null)
            {
                rs.close();
            }
        }
        return nodes;
    }

    public void setCreateMissingPeople(boolean createMissingPeople)
    {
        this.createMissingPeople = createMissingPeople;
    }

    public void setNamespacePrefixResolver(NamespacePrefixResolver namespacePrefixResolver)
    {
        this.namespacePrefixResolver = namespacePrefixResolver;
    }

    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }

    public void setPermissionServiceSPI(PermissionServiceSPI permissionServiceSPI)
    {
        this.permissionServiceSPI = permissionServiceSPI;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public void setStoreUrl(String storeUrl)
    {
        this.storeRef = new StoreRef(storeUrl);
    }

    public String getUserIdentifier(String caseSensitiveUserName)
    {
        NodeRef nodeRef = getPersonOrNull(caseSensitiveUserName);
        if ((nodeRef != null) && nodeService.exists(nodeRef))
        {
            String realUserName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef,
                    ContentModel.PROP_USERNAME));
            return realUserName;
        }
        return null;
    }

    // IOC Setters

}
