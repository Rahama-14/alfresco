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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.jscript;

import java.util.Set;
import java.util.StringTokenizer;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.repo.security.authentication.PasswordGenerator;
import org.alfresco.repo.security.authentication.UserNameGenerator;
import org.alfresco.repo.security.authority.AuthorityDAO;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyMap;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Scripted People service for describing and executing actions against People & Groups.
 * 
 * @author davidc
 */
public final class People extends BaseScopableProcessorExtension
{
    /** Repository Service Registry */
    private ServiceRegistry services;
    private AuthorityDAO authorityDAO;
    private AuthorityService authorityService;
    private PersonService personService;
    private MutableAuthenticationDao mutableAuthenticationDao;
    private UserNameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;
    private StoreRef storeRef;
    
    
    /**
     * Set the default store reference
     * 
     * @param   storeRef the default store reference
     */
    public void setStoreUrl(String storeRef)
    {
        // ensure this is not set again by a script instance
        if (this.storeRef != null)
        {
            throw new IllegalStateException("Default store URL can only be set once.");
        }
        this.storeRef = new StoreRef(storeRef);
    }
    
    /**
     * Set the mutable authentication dao
     * 
     * @param mutableAuthenticationDao Mutable Authentication DAO 
     */
    public void setMutableAuthenticationDao(MutableAuthenticationDao mutableAuthenticationDao)
    {
        this.mutableAuthenticationDao = mutableAuthenticationDao;
    }

    /**
     * Set the service registry
     * 
     * @param serviceRegistry	the service registry
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
    	this.services = serviceRegistry;
    }

    /**
     * Set the authority DAO
     *
     * @param authorityDAO  authority dao
     */
    public void setAuthorityDAO(AuthorityDAO authorityDAO)
    {
        this.authorityDAO = authorityDAO;
    }
    
    /**
     * Set the authority service
     * 
     * @param authorityService The authorityService to set.
     */
    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }
    
    /**
     * Set the person service
     * 
     * @param personService The personService to set.
     */
    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }
    
    /**
     * Set the user name generator service
     * 
     * @param userNameGenerator the user name generator 
     */
    public void setUserNameGenerator(UserNameGenerator userNameGenerator)
    {
        this.usernameGenerator = userNameGenerator;
    }
    
    /**
     * Set the password generator service
     * 
     * @param passwordGenerator the password generator
     */
    public void setPasswordGenerator(PasswordGenerator passwordGenerator)
    {
        this.passwordGenerator = passwordGenerator;
    }
    
    /**
     * Delete a Person with the given username
     * 
     * @param username the username of the person to delete
     */
    public void deletePerson(String username)
    {
        personService.deletePerson(username);
    }
    
    /**
     * Create a Person with a generated user name
     * 
     * @param createUserAccount
     *            set to 'true' to create a user account for the person with the
     *            generated user name and a generated password
     * @param setAccountEnabled
     *            set to 'true' to create enabled user account, or 'false' to
     *            create disabled user account for created person.
     * @return the person node (type cm:person) created or null if the person
     *         could not be created
     */
    public ScriptNode createPerson(boolean createUserAccount,
            boolean setAccountEnabled)
    {
        ParameterCheck.mandatory("createUserAccount", createUserAccount);
        ParameterCheck.mandatory("setAccountEnabled", setAccountEnabled);

        ScriptNode person = null;

        // generate user name
        String userName = usernameGenerator.generateUserName();

        // create person if user name does not already exist
        if (!personService.personExists(userName))
        {
            person = createPerson(userName);

            if (createUserAccount)
            {
                // generate password
                char[] password = passwordGenerator.generatePassword().toCharArray();

                // create account for person with generated userName and
                // password
                mutableAuthenticationDao.createUser(userName, password);
                mutableAuthenticationDao.setEnabled(userName, setAccountEnabled);
                
                // TODO glen johnson at alfresco dot com -
                // find a more secure way of making generated password
                // available. I need to make it available for the invite
                // workflow/service
                person.getProperties().put("generatedPassword", new String(password));
                person.save();
            }
        }

        return person;
    }

    /**
     * Enable person's user account
     * 
     * @param userName user name of person for which to enable user account
     */
    public void enablePerson(String userName)
    {
        mutableAuthenticationDao.setEnabled(userName, true);
    }

    /**
     * Create a Person with the given user name
     * 
     * @param userName the user name of the person to create
     * @return the person node (type cm:person) created or null if the user name already exists
     */
    public ScriptNode createPerson(String userName)
    {
        ParameterCheck.mandatoryString("userName", userName);
        
        ScriptNode person = null;
        
        PropertyMap properties = new PropertyMap();
        properties.put(ContentModel.PROP_USERNAME, userName);
        
        if (!personService.personExists(userName))
        {
            NodeRef personRef = personService.createPerson(properties); 
            person = new ScriptNode(personRef, services, getScope()); 
        }
        
        return person;
    }
    
        
    /**
     * Get the collection of people stored in the repository.
     * An optional filter query may be provided by which to filter the people collection.
     * Space separate the query terms i.e. "john bob" will find all users who's first or
     * second names contain the strings "john" or "bob".
     * 
     * @param filter filter query string by which to filter the collection of people.
     *          If <pre>null</pre> then all people stored in the repository are returned
     *          
     * @return people collection as a JavaScript array
     */
    public Scriptable getPeople(String filter)
    {
        return getPeople(filter, 0);
    }
    
    /**
     * Get the collection of people stored in the repository.
     * An optional filter query may be provided by which to filter the people collection.
     * Space separate the query terms i.e. "john bob" will find all users who's first or
     * second names contain the strings "john" or "bob".
     * 
     * @param filter filter query string by which to filter the collection of people.
     *          If <pre>null</pre> then all people stored in the repository are returned
     * @param maxResults maximum results to return or all if <= 0
     * 
     * @return people collection as a JavaScript array
     */
    public Scriptable getPeople(String filter, int maxResults)
    {
        Object[] people = null;
        
        if (filter == null || filter.length() == 0)
        {
            people = personService.getAllPeople().toArray();
        }
        else
        {
            filter = filter.trim();
            if (filter.length() != 0)
            {
                // define the query to find people by their first or last name
                StringBuilder query = new StringBuilder(128);
                for (StringTokenizer t = new StringTokenizer(filter, " "); t.hasMoreTokens(); /**/)
                {
                    String term = LuceneQueryParser.escape(t.nextToken().replace('"', ' '));
                    query.append("@").append(NamespaceService.CONTENT_MODEL_PREFIX).append("\\:firstName:\"*");
                    query.append(term);
                    query.append("*\" @").append(NamespaceService.CONTENT_MODEL_PREFIX).append("\\:lastName:\"*");
                    query.append(term);
                    query.append("*\" ");
                }
                
                // define the search parameters
                SearchParameters params = new SearchParameters();
                params.setLanguage(SearchService.LANGUAGE_LUCENE);
                params.addStore(this.storeRef);
                params.setQuery(query.toString());
                if (maxResults > 0)
                {
                    params.setLimitBy(LimitBy.FINAL_SIZE);
                    params.setLimit(maxResults);
                }
                
                ResultSet results = null;
                try
                {
                    results = services.getSearchService().query(params);
                    people = results.getNodeRefs().toArray();
                }
                finally
                {
                    if (results != null)
                    {
                        results.close();
                    }
                }
            }
        }
        
        if (people == null)
        {
            people = new Object[0];
        }
        
        return Context.getCurrentContext().newArray(getScope(), people);
    }
    
    /**
     * Gets the Person given the username
     * 
     * @param username  the username of the person to get
     * @return the person node (type cm:person) or null if no such person exists 
     */
    public ScriptNode getPerson(String username)
    {
        ParameterCheck.mandatoryString("Username", username);
        ScriptNode person = null;
        if (personService.personExists(username))
        {
            NodeRef personRef = personService.getPerson(username);
            person = new ScriptNode(personRef, services, getScope());
        }
        return person;
    }

    /**
     * Gets the Group given the group name
     * 
     * @param groupName  name of group to get
     * @return  the group node (type usr:authorityContainer) or null if no such group exists
     */
    public ScriptNode getGroup(String groupName)
    {
        ParameterCheck.mandatoryString("GroupName", groupName);
        ScriptNode group = null;
        NodeRef groupRef = authorityDAO.getAuthorityNodeRefOrNull(groupName);
        if (groupRef != null)
        {
            group = new ScriptNode(groupRef, services, getScope());
        }
        return group;
    }
    
    /**
     * Deletes a group from the system.
     * 
     * @param group     The group to delete
     */
    public void deleteGroup(ScriptNode group)
    {
        ParameterCheck.mandatory("Group", group);
        if (group.getQNameType().equals(ContentModel.TYPE_AUTHORITY_CONTAINER))
        {
            String groupName = (String)group.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
            authorityService.deleteAuthority(groupName);
        }
    }
    
    /**
     * Create a new root level group with the specified unique name
     * 
     * @param groupName     The unique group name to create - NOTE: do not prefix with "GROUP_"
     * 
     * @return the group reference if successful or null if failed
     */
    public ScriptNode createGroup(String groupName)
    {
        return createGroup(null, groupName);
    }
    
    /**
     * Create a new group with the specified unique name
     * 
     * @param parentGroup   The parent group node - can be null for a root level group
     * @param groupName     The unique group name to create - NOTE: do not prefix with "GROUP_"
     * 
     * @return the group reference if successful or null if failed
     */
    public ScriptNode createGroup(ScriptNode parentGroup, String groupName)
    {
        ParameterCheck.mandatoryString("GroupName", groupName);
        
        ScriptNode group = null;
        
        String actualName = services.getAuthorityService().getName(AuthorityType.GROUP, groupName);
        if (authorityService.authorityExists(actualName) == false)
        {
            String parentGroupName = null;
            if (parentGroup != null)
            {
                parentGroupName = (String)parentGroup.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
            }
            String result = authorityService.createAuthority(AuthorityType.GROUP, parentGroupName, groupName);
            group = getGroup(result);
        }
        
        return group;
    }
    
    /**
     * Add an authority (a user or group) to a group container as a new child
     * 
     * @param parentGroup   The parent container group
     * @param authority     The authority (user or group) to add
     */
    public void addAuthority(ScriptNode parentGroup, ScriptNode authority)
    {
        ParameterCheck.mandatory("Authority", authority);
        ParameterCheck.mandatory("ParentGroup", parentGroup);
        if (parentGroup.getQNameType().equals(ContentModel.TYPE_AUTHORITY_CONTAINER))
        {
            String parentGroupName = (String)parentGroup.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
            String authorityName;
            if (authority.getQNameType().equals(ContentModel.TYPE_AUTHORITY_CONTAINER))
            {
                authorityName = (String)authority.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
            }
            else
            {
                authorityName = (String)authority.getProperties().get(ContentModel.PROP_USERNAME);
            }
            authorityService.addAuthority(parentGroupName, authorityName);
        }
    }
    
    /**
     * Remove an authority (a user or group) from a group
     * 
     * @param parentGroup   The parent container group
     * @param authority     The authority (user or group) to remove
     */
    public void removeAuthority(ScriptNode parentGroup, ScriptNode authority)
    {
        ParameterCheck.mandatory("Authority", authority);
        ParameterCheck.mandatory("ParentGroup", parentGroup);
        if (parentGroup.getQNameType().equals(ContentModel.TYPE_AUTHORITY_CONTAINER))
        {
            String parentGroupName = (String)parentGroup.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
            String authorityName;
            if (authority.getQNameType().equals(ContentModel.TYPE_AUTHORITY_CONTAINER))
            {
                authorityName = (String)authority.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
            }
            else
            {
                authorityName = (String)authority.getProperties().get(ContentModel.PROP_USERNAME);
            }
            authorityService.removeAuthority(parentGroupName, authorityName);
        }
    }
    
    /**
     * Gets the members (people) of a group (including all sub-groups)
     * 
     * @param group        the group to retrieve members for
     * @param recurse      recurse into sub-groups
     * 
     * @return members of the group as a JavaScript array
     */
    public Scriptable getMembers(ScriptNode group)
    {
        ParameterCheck.mandatory("Group", group);
        Object[] members = getContainedAuthorities(group, AuthorityType.USER, true);
        return Context.getCurrentContext().newArray(getScope(), members);
    }

    /**
     * Gets the members (people) of a group
     * 
     * @param group        the group to retrieve members for
     * @param recurse      recurse into sub-groups
     * 
     * @return the members of the group as a JavaScript array
     */
    public Scriptable getMembers(ScriptNode group, boolean recurse)
    {
        ParameterCheck.mandatory("Group", group);
        Object[] members = getContainedAuthorities(group, AuthorityType.USER, recurse);
        return Context.getCurrentContext().newArray(getScope(), members);
    }
    
    /**
     * Gets the groups that contain the specified authority
     * 
     * @param person       the user (cm:person) to get the containing groups for
     * 
     * @return the containing groups as a JavaScript array, can be null
     */
    public Scriptable getContainerGroups(ScriptNode person)
    {
        ParameterCheck.mandatory("Person", person);
        Object[] parents = null;
        Set<String> authorities = this.authorityService.getContainingAuthorities(
                AuthorityType.GROUP,
                (String)person.getProperties().get(ContentModel.PROP_USERNAME),
                false);
        parents = new Object[authorities.size()];
        int i = 0;
        for (String authority : authorities)
        {
            ScriptNode group = getGroup(authority);
            if (group != null)
            {
                parents[i++] = group; 
            }
        }
        return Context.getCurrentContext().newArray(getScope(), parents);
    }
    
    /**
     * Return true if the specified user is an Administrator authority.
     * 
     * @param person to test
     * 
     * @return true if an admin, false otherwise
     */
    public boolean isAdmin(ScriptNode person)
    {
        ParameterCheck.mandatory("Person", person);
        return this.authorityService.isAdminAuthority((String)person.getProperties().get(ContentModel.PROP_USERNAME));
    }

    /**
     * Get Contained Authorities
     * 
     * @param container  authority containers
     * @param type       authority type to filter by
     * @param recurse    recurse into sub-containers
     * 
     * @return contained authorities
     */
    private Object[] getContainedAuthorities(ScriptNode container, AuthorityType type, boolean recurse)
    {
        Object[] members = null;
        
        if (container.getQNameType().equals(ContentModel.TYPE_AUTHORITY_CONTAINER))
        {
            String groupName = (String)container.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
            Set<String> authorities = authorityService.getContainedAuthorities(type, groupName, !recurse);
            members = new Object[authorities.size()];
            int i = 0;
            for (String authority : authorities)
            {
                AuthorityType authorityType = AuthorityType.getAuthorityType(authority);
                if (authorityType.equals(AuthorityType.GROUP))
                {
                    ScriptNode group = getGroup(authority);
                    if (group != null)
                    {
                        members[i++] = group; 
                    }
                }
                else if (authorityType.equals(AuthorityType.USER))
                {
                    ScriptNode person = getPerson(authority);
                    if (person != null)
                    {
                        members[i++] = person; 
                    }
                }
            }
        }
        
        return members != null ? members : new Object[0];
    }
}
