/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.service.cmr.security;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.Auditable;
import org.alfresco.service.NotAuditable;
import org.alfresco.service.PublicService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * This service encapsulates the management of people and groups.
 * <p>
 * <p>
 * People and groups may be managed entirely in the repository or entirely in
 * some other implementation such as LDAP or via NTLM. Some properties may in
 * the repository and some in another store. Individual properties may or may
 * not be mutable.
 * <p>
 * 
 * @author Andy Hind
 */
@PublicService
public interface PersonService
{
    /**
     * Get a person by userName. The person is store in the repository. The
     * person may be created as a side effect of this call, depending on the
     * setting to
     * {@link #setCreateMissingPeople(boolean) create missing people or not}.
     * 
     * @param userName -
     *            the userName key to find the person
     * @return Returns the person node, either existing or new
     * @throws NoSuchPersonException
     *             if the user doesn't exist and could not be created
     *             automatically
     * 
     * @see #setCreateMissingPeople(boolean)
     * @see #createMissingPeople()
     */
    @Auditable(parameters = {"userName"})
    public NodeRef getPerson(String userName);

    /**
     * Retrieve the person NodeRef for a username key. Depending on the <code>autoCreate</code> parameter and
     * configuration missing people will be created if not found, else a NoSuchPersonException exception will be thrown.
     * 
     * @param userName
     *            of the person NodeRef to retrieve
     * @param autoCreate
     *            should we auto-create the person node and home folder if they don't exist? (and configuration allows
     *            us to)
     * @return NodeRef of the person as specified by the username
     * @throws NoSuchPersonException
     *             if the person doesn't exist and can't be created
     */
    @Auditable(parameters = {"userName", "autoCreate"})
    public NodeRef getPerson(final String userName, final boolean autoCreate);

    /**
     * Check if a person exists.
     * 
     * @param userName
     *            the user name
     * @return Returns true if the user exists, otherwise false
     */
    @Auditable(parameters = {"userName"})
    public boolean personExists(String userName);

    /**
     * Does this service create people on demand if they are missing. If this is
     * true, a call to getPerson() will create a person if they are missing.
     * 
     * @return true if people are created on demand and false otherwise.
     */
    @Auditable
    public boolean createMissingPeople();

    /**
     * Set if missing people should be created.
     * 
     * @param createMissing
     *            set to true to create people
     * 
     * @see #getPerson(String)
     */
    @Auditable(parameters = {"createMissing"})
    public void setCreateMissingPeople(boolean createMissing);

    /**
     * Get the list of properties that are mutable. Some service may only allow
     * a limited list of properties to be changed. This may be those persisted
     * in the repository or those that can be changed in some other
     * implementation such as LDAP.
     * 
     * @return A set of QNames that identify properties that can be changed
     */
    @Auditable
    public Set<QName> getMutableProperties();

    /**
     * Set the properties on a person - some of these may be persisted in
     * different locations.
     * 
     * @param userName -
     *            the user for which the properties should be set.
     * @param properties -
     *            the map of properties to set (as the NodeService)
     */
    @Auditable(parameters = {"userName", "properties"})
    public void setPersonProperties(String userName, Map<QName, Serializable> properties);


    /**
     * Set the properties on a person - some of these may be persisted in different locations.
     * 
     * @param userName
     *            - the user for which the properties should be set.
     * @param properties
     *            - the map of properties to set (as the NodeService)
     * @param autoCreate
     *            should we auto-create the home folder if it doesn't exist? (and configuration allows us to)
     */
    @Auditable(parameters = {"userName", "properties", "autoCreate"})
    public void setPersonProperties(String userName, Map<QName, Serializable> properties, boolean autoCreate);
    
    /**
     * Can this service create, delete and update person information?
     * 
     * @return true if this service allows mutation to people.
     */
    @Auditable
    public boolean isMutable();

    /**
     * Create a new person with the given properties. The userName is one of the
     * properties. Users with duplicate userNames are not allowed.
     * 
     * @param properties
     * @return
     */
    @Auditable(parameters = {"properties"})
    public NodeRef createPerson(Map<QName, Serializable> properties);

    /**
     * Create a new person with the given properties, recording them against the given zone name (usually identifying an
     * external user registry from which the details were obtained). The userName is one of the properties. Users with
     * duplicate userNames are not allowed.
     * 
     * @param properties
     *            the properties
     * @param zones
     *            a set if zones including the identifier for the external user registry owning the person information, or <code>null</code> or an empty set
     * @return the node ref
     */
    @Auditable(parameters = {"properties", "zones"})
    public NodeRef createPerson(Map<QName, Serializable> properties, Set<String> zones);

    /**
     * Delete the person identified by the given user name.
     * 
     * @param userName
     */
    @Auditable(parameters = {"userName"})
    public void deletePerson(String userName);

    /**
     * Get all the people we know about.
     * 
     * @return a set of people in no specific order.
     */
    @Auditable
    public Set<NodeRef> getAllPeople();
    
    /**
     * Get people filtered by the given property name/value pair
     * 
     * @param propertyKey property key of property to filter people by 
     * @param propertyValue property value of property to filter people by
     * @return people filtered by the given property name/value pair
     */
    @Auditable
    public Set<NodeRef> getPeopleFilteredByProperty(QName propertyKey, Serializable propertyValue);

    /**
     * Return the container that stores people.
     * 
     * @return
     */
    @Auditable
    public NodeRef getPeopleContainer();
    
    /**
     * Are user names case sensitive?
     * 
     * @return
     */
    @Auditable
    public boolean getUserNamesAreCaseSensitive();

    /**
     * Given the case sensitive user name find the approriate identifier from the person service.
     * If the system is case sensitive it will return the same string.
     * If case insentive it will return the common object.
     * If the user does not exist it will return null;
     * 
     * @param caseSensitiveUserName
     * @return
     */
    @NotAuditable
    public String getUserIdentifier(String caseSensitiveUserName);

}
