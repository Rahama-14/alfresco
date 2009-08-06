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
package org.alfresco.repo.security.sync;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.management.subsystems.ChildApplicationContextManager;
import org.alfresco.repo.security.authentication.AuthenticationContext;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.util.PropertyMap;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

/**
 * Tests the {@link ChainingUserRegistrySynchronizer} using a simulated {@link UserRegistry}.
 * 
 * @author dward
 */
public class ChainingUserRegistrySynchronizerTest extends TestCase
{

    /** The context locations, in reverse priority order. */
    private static final String[] CONFIG_LOCATIONS =
    {
        "classpath:alfresco/application-context.xml", "classpath:sync-test-context.xml"
    };

    /** The Spring application context */
    private static ApplicationContext context = new ClassPathXmlApplicationContext(
            ChainingUserRegistrySynchronizerTest.CONFIG_LOCATIONS);

    /** The synchronizer we are testing. */
    private UserRegistrySynchronizer synchronizer;

    /** The application context manager. */
    private MockApplicationContextManager applicationContextManager;

    /** The person service. */
    private PersonService personService;

    /** The authority service. */
    private AuthorityService authorityService;

    /** The node service. */
    private NodeService nodeService;

    /** The authentication context. */
    private AuthenticationContext authenticationContext;

    /** The retrying transaction helper. */
    private RetryingTransactionHelper retryingTransactionHelper;

    @Override
    protected void setUp() throws Exception
    {
        this.synchronizer = (UserRegistrySynchronizer) ChainingUserRegistrySynchronizerTest.context
                .getBean("testUserRegistrySynchronizer");
        this.applicationContextManager = (MockApplicationContextManager) ChainingUserRegistrySynchronizerTest.context
                .getBean("testApplicationContextManager");
        this.personService = (PersonService) ChainingUserRegistrySynchronizerTest.context.getBean("personService");
        this.authorityService = (AuthorityService) ChainingUserRegistrySynchronizerTest.context
                .getBean("authorityService");
        this.nodeService = (NodeService) ChainingUserRegistrySynchronizerTest.context.getBean("nodeService");

        this.authenticationContext = (AuthenticationContext) ChainingUserRegistrySynchronizerTest.context
                .getBean("authenticationContext");
        this.authenticationContext.setSystemUserAsCurrentUser();

        this.retryingTransactionHelper = (RetryingTransactionHelper) ChainingUserRegistrySynchronizerTest.context
                .getBean("retryingTransactionHelper");
    }

    @Override
    protected void tearDown() throws Exception
    {
        this.authenticationContext.clearCurrentSecurityContext();
    }

    /**
     * Sets up the test users and groups in three zones, "Z0", "Z1" and "Z2", by doing a forced synchronize with a Mock
     * user registry. Note that the zones have some overlapping entries. "Z0" is not used in subsequent synchronizations
     * and is used to test that users and groups in zones that aren't in the authentication chain get 're-zoned'
     * appropriately. The layout is as follows
     * 
     * <pre>
     * Z0
     * G1
     * U6
     * 
     * Z1
     * G2 - U1, G3 - U2, G4, G5
     * 
     * Z2
     * G2 - U1, U3, U4
     * G6 - U3, U4, G7 - U5
     * </pre>
     * 
     * @throws Exception
     *             the exception
     */
    private void setUpTestUsersAndGroups() throws Exception
    {
        this.applicationContextManager.setUserRegistries(new MockUserRegistry("Z0", new NodeDescription[]
        {
            newPerson("U6")
        }, new NodeDescription[]
        {
            newGroup("G1")
        }), new MockUserRegistry("Z1", new NodeDescription[]
        {
            newPerson("U1"), newPerson("U2")
        }, new NodeDescription[]
        {
            newGroup("G2", "U1", "G3"), newGroup("G3", "U2", "G4", "G5"), newGroup("G4"), newGroup("G5")
        }), new MockUserRegistry("Z2", new NodeDescription[]
        {
            newPerson("U1"), newPerson("U3"), newPerson("U4"), newPerson("U5")
        }, new NodeDescription[]
        {
            newGroup("G2", "U1", "U3", "U4"), newGroup("G6", "U3", "U4", "G7"), newGroup("G7", "U5")
        }));
        this.retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Object>()
        {

            public Object execute() throws Throwable
            {
                ChainingUserRegistrySynchronizerTest.this.synchronizer.synchronize(true, true);
                return null;
            }
        });
        this.retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Object>()
        {

            public Object execute() throws Throwable
            {
                assertExists("Z0", "U6");
                assertExists("Z0", "G1");
                assertExists("Z1", "U1");
                assertExists("Z1", "U2");
                assertExists("Z1", "G2", "U1", "G3");
                assertExists("Z1", "G3", "U2", "G4", "G5");
                assertExists("Z1", "G4");
                assertExists("Z1", "G5");
                assertExists("Z2", "U3");
                assertExists("Z2", "U4");
                assertExists("Z2", "U5");
                assertExists("Z2", "G6", "U3", "U4", "G7");
                assertExists("Z2", "G7", "U5");
                return null;
            }
        });
    }

    private void tearDownTestUsersAndGroups() throws Exception
    {
        // Wipe out everything that was in Z1 and Z2
        this.applicationContextManager.setUserRegistries(new MockUserRegistry("Z0", new NodeDescription[] {},
                new NodeDescription[] {}), new MockUserRegistry("Z1", new NodeDescription[] {},
                new NodeDescription[] {}), new MockUserRegistry("Z2", new NodeDescription[] {},
                new NodeDescription[] {}));
        this.retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Object>()
        {

            public Object execute() throws Throwable
            {
                ChainingUserRegistrySynchronizerTest.this.synchronizer.synchronize(true, true);
                return null;
            }
        });
        this.retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Object>()
        {

            public Object execute() throws Throwable
            {
                assertNotExists("U1");
                assertNotExists("U2");
                assertNotExists("U3");
                assertNotExists("U4");
                assertNotExists("U5");
                assertNotExists("U6");
                assertNotExists("G1");
                assertNotExists("G2");
                assertNotExists("G3");
                assertNotExists("G4");
                assertNotExists("G5");
                assertNotExists("G6");
                assertNotExists("G7");
                return null;
            }
        });
    }

    /**
     * Tests a differential update of the test users and groups. The layout is as follows
     * 
     * <pre>
     * Z1
     * G1 - U1, U6
     * G2 - U1
     * G3 - U2, G4, G5 - U6
     * 
     * Z2
     * G2 - U1, U3, U4, U6
     * G6 - U3, U4, G7
     * </pre>
     * 
     * @throws Exception
     *             the exception
     */
    public void testDifferentialUpdate() throws Exception
    {
        setUpTestUsersAndGroups();
        this.applicationContextManager.setUserRegistries(new MockUserRegistry("Z1", new NodeDescription[]
        {
            newPerson("U1", "changeofemail@alfresco.com"), newPerson("U6")
        }, new NodeDescription[]
        {
            newGroup("G1", "U1", "U6"), newGroup("G2", "U1"), newGroup("G5", "U6")
        }), new MockUserRegistry("Z2", new NodeDescription[]
        {
            newPerson("U1", "shouldbeignored@alfresco.com"), newPerson("U5", "u5email@alfresco.com"), newPerson("U6")
        }, new NodeDescription[]
        {
            newGroup("G2", "U1", "U3", "U4", "U6"), newGroup("G7")
        }));
        this.retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Object>()
        {

            public Object execute() throws Throwable
            {

                ChainingUserRegistrySynchronizerTest.this.synchronizer.synchronize(false, false);
                // Stay in the same transaction
                assertExists("Z1", "U1");
                assertEmailEquals("U1", "changeofemail@alfresco.com");
                assertExists("Z1", "U2");
                assertExists("Z1", "U6");
                assertExists("Z1", "G1", "U1", "U6");
                assertExists("Z1", "G2", "U1");
                assertExists("Z1", "G3", "U2", "G4", "G5");
                assertExists("Z1", "G4");
                assertExists("Z1", "G5", "U6");
                assertExists("Z2", "U3");
                assertExists("Z2", "U4");
                assertExists("Z2", "U5");
                assertEmailEquals("U5", "u5email@alfresco.com");
                assertExists("Z2", "G6", "U3", "U4", "G7");
                assertExists("Z2", "G7");
                return null;
            }
        });
        tearDownTestUsersAndGroups();
    }

    /**
     * Tests a forced update of the test users and groups. Also tests that groups and users that previously existed in
     * Z2 get moved when they appear in Z1. Also tests that 'dangling references' to removed users (U4, U5) do not cause
     * any problems. Also tests that case-sensitivity is not a problem when an occluded user is recreated with different
     * case. The layout is as follows
     * 
     * <pre>
     * Z1
     * G1 - U6
     * G2 - 
     * G3 - U2, G5 - U6
     * G6 - u3
     * 
     * Z2
     * G2 - U1, U3, U6
     * G6 - U3, G7
     * </pre>
     * 
     * @throws Exception
     *             the exception
     */
    public void testForcedUpdate() throws Exception
    {
        setUpTestUsersAndGroups();
        this.applicationContextManager.setUserRegistries(new MockUserRegistry("Z1", new NodeDescription[]
        {
            newPerson("U2"), newPerson("u3"), newPerson("U6")
        }, new NodeDescription[]
        {
            newGroup("G1", "U6"), newGroup("G2"), newGroup("G3", "U2", "G5"), newGroup("G5", "U6"),
            newGroup("G6", "u3")
        }), new MockUserRegistry("Z2", new NodeDescription[]
        {
            newPerson("U1", "somenewemail@alfresco.com"), newPerson("U3"), newPerson("U6")
        }, new NodeDescription[]
        {
            newGroup("G2", "U1", "U3", "U4", "U6"), newGroup("G6", "U3", "U4", "G7"), newGroup("G7", "U4", "U5")
        }));
        this.retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Object>()
        {

            public Object execute() throws Throwable
            {
                ChainingUserRegistrySynchronizerTest.this.synchronizer.synchronize(true, true);
                return null;
            }
        });
        this.retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Object>()
        {

            public Object execute() throws Throwable
            {
                assertExists("Z1", "U2");
                assertExists("Z1", "u3");
                assertExists("Z1", "U6");
                assertExists("Z1", "G1", "U6");
                assertExists("Z1", "G2");
                assertExists("Z1", "G3", "U2", "G5");
                assertNotExists("G4");
                assertExists("Z1", "G5", "U6");
                assertExists("Z1", "G6", "u3");
                assertExists("Z2", "U1");
                assertEmailEquals("U1", "somenewemail@alfresco.com");
                assertNotExists("U4");
                assertNotExists("U5");
                assertExists("Z2", "G7");
                return null;
            }
        });
        tearDownTestUsersAndGroups();
    }

    /**
     * Constructs a description of a test group
     * 
     * @param name
     *            the name
     * @param members
     *            the members
     * @return the node description
     */
    private NodeDescription newGroup(String name, String... members)
    {
        NodeDescription group = new NodeDescription();
        PropertyMap properties = group.getProperties();
        properties.put(ContentModel.PROP_AUTHORITY_NAME, longName(name));
        if (members.length > 0)
        {
            Set<String> assocs = group.getChildAssociations();
            for (String member : members)
            {
                assocs.add(longName(member));
            }
        }
        group.setLastModified(new Date());
        return group;
    }

    /**
     * Constructs a description of a test person with default email (userName@alfresco.com)
     * 
     * @param userName
     *            the user name
     * @return the node description
     */
    private NodeDescription newPerson(String userName)
    {
        return newPerson(userName, userName + "@alfresco.com");
    }

    /**
     * Constructs a description of a test person with a given email.
     * 
     * @param userName
     *            the user name
     * @param email
     *            the email
     * @return the node description
     */
    private NodeDescription newPerson(String userName, String email)
    {
        NodeDescription person = new NodeDescription();
        PropertyMap properties = person.getProperties();
        properties.put(ContentModel.PROP_USERNAME, userName);
        properties.put(ContentModel.PROP_FIRSTNAME, userName + "F");
        properties.put(ContentModel.PROP_LASTNAME, userName + "L");
        properties.put(ContentModel.PROP_EMAIL, email);
        person.setLastModified(new Date());
        return person;
    }

    /**
     * Perform all the necessary assertions to ensure that an authority and its members exist in the correct zone.
     * 
     * @param zone
     *            the zone
     * @param name
     *            the name
     * @param members
     *            the members
     */
    private void assertExists(String zone, String name, String... members)
    {
        String longName = longName(name);
        // Check authority exists
        assertTrue(this.authorityService.authorityExists(longName));

        // Check in correct zone
        assertTrue(this.authorityService.getAuthorityZones(longName).contains(
                AuthorityService.ZONE_AUTH_EXT_PREFIX + zone));
        if (AuthorityType.getAuthorityType(longName).equals(AuthorityType.GROUP))
        {
            // Check groups have expected members
            Set<String> memberSet = new HashSet<String>(members.length * 2);
            for (String member : members)
            {
                memberSet.add(longName(member));
            }
            assertEquals(memberSet, this.authorityService.getContainedAuthorities(null, longName, true));
        }
        else
        {
            // Check users exist as persons
            assertTrue(this.personService.personExists(name));
        }
    }

    /**
     * Perform all the necessary assertions to ensure that an authority does not exist.
     * 
     * @param name
     *            the name
     */
    private void assertNotExists(String name)
    {
        String longName = longName(name);
        // Check authority does not exist
        assertFalse(this.authorityService.authorityExists(longName));

        // Check there is no zone
        assertNull(this.authorityService.getAuthorityZones(longName));
        if (!AuthorityType.getAuthorityType(longName).equals(AuthorityType.GROUP))
        {
            // Check person does not exist
            assertFalse(this.personService.personExists(name));
        }
    }

    /**
     * Asserts that a person's email has the expected value
     * 
     * @param personName
     *            the person name
     * @param email
     *            the email
     */
    private void assertEmailEquals(String personName, String email)
    {
        NodeRef personRef = this.personService.getPerson(personName);
        assertEquals(email, this.nodeService.getProperty(personRef, ContentModel.PROP_EMAIL));
    }

    /**
     * Converts the given short name to a full authority name, assuming that those short names beginning with 'G'
     * correspond to groups and all others correspond to users.
     * 
     * @param shortName
     *            the short name
     * @return the full authority name
     */
    private String longName(String shortName)
    {
        return this.authorityService.getName(shortName.startsWith("G") ? AuthorityType.GROUP : AuthorityType.USER,
                shortName);
    }

    /**
     * A Mock {@link UserRegistry} that returns a fixed set of users and groups.
     */
    public static class MockUserRegistry implements UserRegistry
    {

        /** The zone id. */
        private String zoneId;

        /** The persons. */
        private NodeDescription[] persons;

        /** The groups. */
        private NodeDescription[] groups;

        /**
         * Instantiates a new mock user registry.
         * 
         * @param zoneId
         *            the zone id
         * @param persons
         *            the persons
         * @param groups
         *            the groups
         */
        public MockUserRegistry(String zoneId, NodeDescription[] persons, NodeDescription[] groups)
        {
            this.zoneId = zoneId;
            this.persons = persons;
            this.groups = groups;
        }

        /**
         * Gets the zone id.
         * 
         * @return the zoneId
         */
        public String getZoneId()
        {
            return this.zoneId;
        }

        /*
         * (non-Javadoc)
         * @see org.alfresco.repo.security.sync.UserRegistry#getGroups(java.util.Date)
         */
        public Iterator<NodeDescription> getGroups(Date modifiedSince)
        {
            return Arrays.asList(this.groups).iterator();
        }

        /*
         * (non-Javadoc)
         * @see org.alfresco.repo.security.sync.UserRegistry#getPersons(java.util.Date)
         */
        public Iterator<NodeDescription> getPersons(Date modifiedSince)
        {
            return Arrays.asList(this.persons).iterator();
        }
    }

    /**
     * An {@link ChildApplicationContextManager} for a chain of application contexts containing mock user registries.
     */
    public static class MockApplicationContextManager implements ChildApplicationContextManager
    {

        /** The contexts. */
        private Map<String, ApplicationContext> contexts = Collections.emptyMap();

        /**
         * Sets the user registries.
         * 
         * @param registries
         *            the new user registries
         */
        public void setUserRegistries(MockUserRegistry... registries)
        {
            this.contexts = new LinkedHashMap<String, ApplicationContext>(registries.length * 2);
            for (MockUserRegistry registry : registries)
            {
                StaticApplicationContext context = new StaticApplicationContext();
                context.getDefaultListableBeanFactory().registerSingleton("userRegistry", registry);
                this.contexts.put(registry.getZoneId(), context);
            }
        }

        /*
         * (non-Javadoc)
         * @see
         * org.alfresco.repo.management.subsystems.ChildApplicationContextManager#getApplicationContext(java.lang.String
         * )
         */
        public ApplicationContext getApplicationContext(String id)
        {
            return this.contexts.get(id);
        }

        /*
         * (non-Javadoc)
         * @see org.alfresco.repo.management.subsystems.ChildApplicationContextManager#getInstanceIds()
         */
        public Collection<String> getInstanceIds()
        {
            return this.contexts.keySet();
        }
    }
}
