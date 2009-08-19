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
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.tenant;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.admin.RepoAdminService;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.CategoryService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.usage.UsageService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MultiTDemoTest extends TestCase
{
    private static Log logger = LogFactory.getLog(MultiTDemoTest.class);
    
    private static ApplicationContext ctx =new ClassPathXmlApplicationContext(
            new String[] {ApplicationContextHelper.CONFIG_LOCATIONS[0], "classpath:tenant/mt-*context.xml"}
            );
    
    private NodeService nodeService;
    private AuthenticationService authenticationService;
    private PersonService personService;
    private SearchService searchService;
    private ContentService contentService;
    private PermissionService permissionService;
    private OwnableService ownableService;
    private TenantAdminService tenantAdminService;
    private TenantService tenantService;
    private AuthorityService authorityService;
    private CategoryService categoryService;
    private CheckOutCheckInService cociService;
    private RepoAdminService repoAdminService;
    private DictionaryService dictionaryService;
    private UsageService usageService;
    
    public static int NUM_TENANTS = 2;
    
    public static final String TEST_RUN = System.currentTimeMillis()+"";
    public static final String TEST_TENANT_DOMAIN = TEST_RUN+".my.test";
    public static final String TEST_TENANT_DOMAIN2 = TEST_TENANT_DOMAIN+"2";
    
    public static List<String> tenants;
    
    static 
    {
        tenants = new ArrayList<String>(NUM_TENANTS);
        for (int i = 1; i <= NUM_TENANTS; i++)
        {
            tenants.add(TEST_TENANT_DOMAIN+i);
        }
    }
    
    public static final String ROOT_DIR = "./tenantstores";
    
    public static final String DEFAULT_ADMIN_PW = "admin";
    
    public static final String DEFAULT_GUEST_UN = "guest";
    public static final String DEFAULT_GUEST_PW = "thiscanbeanything";
    
    public static final String TEST_USER1 = "alice";
    public static final String TEST_USER2 = "bob";
    public static final String TEST_USER3 = "eve";
    public static final String TEST_USER4 = "fred";
    
    private static final int DEFAULT_STORE_COUNT = 7; // including siteStore
    
    public static StoreRef SPACES_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    
    
    public MultiTDemoTest()
    {
    }
    
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        nodeService = (NodeService) ctx.getBean("NodeService");
        authenticationService = (AuthenticationService) ctx.getBean("AuthenticationService");
        tenantAdminService = (TenantAdminService) ctx.getBean("tenantAdminService");
        tenantService = (TenantService) ctx.getBean("tenantService");
        personService = (PersonService) ctx.getBean("PersonService");
        searchService = (SearchService) ctx.getBean("SearchService");
        contentService = (ContentService) ctx.getBean("ContentService");
        permissionService = (PermissionService) ctx.getBean("PermissionService");
        ownableService = (OwnableService) ctx.getBean("OwnableService");
        authorityService = (AuthorityService) ctx.getBean("AuthorityService");
        categoryService = (CategoryService) ctx.getBean("CategoryService");
        cociService = (CheckOutCheckInService) ctx.getBean("CheckoutCheckinService");
        repoAdminService = (RepoAdminService) ctx.getBean("RepoAdminService");
        dictionaryService = (DictionaryService) ctx.getBean("DictionaryService");
        usageService = (UsageService) ctx.getBean("usageService");
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    
    public void test_ETHREEOH_2015()
    {
        final String tenantDomain1 = TEST_RUN+".one.ethreeoh2015";
        final String tenantDomain2 = TEST_RUN+".two.ethreeoh2015";
        
        clearUsage(AuthenticationUtil.getAdminUserName());
        
        createTenant(tenantDomain1);
        
        String tenantAdminName = tenantService.getDomainUser(AuthenticationUtil.getAdminUserName(), tenantDomain1);
        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                createUser(TEST_USER1, tenantDomain1, TEST_USER1+" "+tenantDomain1);
                
                return null;
            }
        }, tenantAdminName);
        
        createTenant(tenantDomain2);
    }
    
    private void clearUsage(String userName)
    {
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName()); // authenticate as super-admin
        
        // find person
        NodeRef personNodeRef = personService.getPerson(userName);
        // clear user usage
        nodeService.setProperty(personNodeRef, ContentModel.PROP_SIZE_CURRENT, null);
        usageService.deleteDeltas(personNodeRef);
    }
    
    public void testCreateTenants() throws Throwable
    {   
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName()); // authenticate as super-admin
        
        logger.info("Create tenants");
        
        Set<NodeRef> personRefs = personService.getAllPeople();
        //assertEquals(2, personRefs.size()); // super-tenant: admin, guest (note: checking for 2 assumes that this test is run in a fresh bootstrap env)
        for (NodeRef personRef : personRefs)
        {
            String userName = (String)nodeService.getProperty(personRef, ContentModel.PROP_USERNAME);
            for (final String tenantDomain : tenants)
            {
                assertFalse("Unexpected (tenant) user: "+userName, userName.endsWith(tenantDomain));
            }
        }
        
        try 
        {   
            for (final String tenantDomain : tenants)
            {
                createTenant(tenantDomain);
            }
        }   
        catch (Throwable t)
        {
            StringWriter stackTrace = new StringWriter();
            t.printStackTrace(new PrintWriter(stackTrace));
            System.err.println(stackTrace.toString());
            throw t;
        }
    }
    
    private void createTenant(final String tenantDomain)
    {
        // create tenants (if not already created)
        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                if (! tenantAdminService.existsTenant(tenantDomain))
                {
                    //tenantAdminService.createTenant(tenantDomain, DEFAULT_ADMIN_PW.toCharArray(), ROOT_DIR + "/" + tenantDomain);
                    tenantAdminService.createTenant(tenantDomain, (DEFAULT_ADMIN_PW+" "+tenantDomain).toCharArray(), null); // use default root dir
                    
                    logger.info("Created tenant " + tenantDomain);
                }
                
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }
    
    public void testCreateUsers() throws Throwable
    {
        logger.info("Create demo users");
        
        Set<NodeRef> personRefs = personService.getAllPeople();
        //assertEquals(2, personRefs.size()); // super-tenant: admin, guest (note: checking for 2 assumes that this test is run in a fresh bootstrap env)
        for (NodeRef personRef : personRefs)
        {
            String userName = (String)nodeService.getProperty(personRef, ContentModel.PROP_USERNAME);
            for (final String tenantDomain : tenants)
            {
                assertFalse("Unexpected (tenant) user: "+userName, userName.endsWith(tenantDomain));
            }
        }
        
        try
        {
            for (final String tenantDomain : tenants)
            {
                String tenantAdminName = tenantService.getDomainUser(AuthenticationUtil.getAdminUserName(), tenantDomain);
                
                AuthenticationUtil.runAs(new RunAsWork<Object>()
                {
                    public Object doWork() throws Exception
                    {
                        createUser(TEST_USER1, tenantDomain, TEST_USER1+" "+tenantDomain);
                        createUser(TEST_USER2, tenantDomain, TEST_USER2+" "+tenantDomain);
                        
                        if (tenantDomain.equals(TEST_TENANT_DOMAIN2))
                        {
                            createUser(TEST_USER3, tenantDomain, TEST_USER3+" "+tenantDomain);
                        }
                        
                        return null;
                    }
                }, tenantAdminName);
            }
            
            for (final String tenantDomain : tenants)
            {
                String tenantAdminName = tenantService.getDomainUser(AuthenticationUtil.getAdminUserName(), tenantDomain);
                
                AuthenticationUtil.runAs(new RunAsWork<Object>()
                {
                    public Object doWork() throws Exception
                    {
                        Set<NodeRef> personRefs = personService.getAllPeople();
                        
                        for (NodeRef personRef : personRefs)
                        {
                            String userName = (String)nodeService.getProperty(personRef, ContentModel.PROP_USERNAME); 
                            assertTrue(userName.endsWith(tenantDomain));
                            
                            logger.info("Create users: get all people - found user: "+userName);
                            
                            NodeRef homeSpaceRef = (NodeRef)nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);
                            assertNotNull(homeSpaceRef);
                        }
                        
                        if (tenantDomain.equals(TEST_TENANT_DOMAIN2))
                        {
                            assertEquals(5, personRefs.size()); // admin@tenant, guest@tenant, alice@tenant, bob@tenant, eve@tenant
                        }
                        else
                        {
                            assertEquals(4, personRefs.size()); // admin@tenant, guest@tenant, alice@tenant, bob@tenant
                        }
                        
                        return null;
                    }
                }, tenantAdminName);
            }
        }   
        catch (Throwable t)
        {
            StringWriter stackTrace = new StringWriter();
            t.printStackTrace(new PrintWriter(stackTrace));
            System.err.println(stackTrace.toString());
            throw t;
        } 
    }
    
    public void testLoginTenantUsers() throws Throwable
    {
        logger.info("Login tenant users");
        
        try
        {
            AuthenticationUtil.clearCurrentSecurityContext();
            
            for (final String tenantDomain : tenants)
            {
                loginLogoutUser(tenantService.getDomainUser(TEST_USER1, tenantDomain), TEST_USER1+" "+tenantDomain);
                
                loginLogoutUser(tenantService.getDomainUser(TEST_USER2, tenantDomain), TEST_USER2+" "+tenantDomain);
                
                if (tenantDomain.equals(TEST_TENANT_DOMAIN2))
                {
                    loginLogoutUser(tenantService.getDomainUser(TEST_USER3, tenantDomain), TEST_USER3+" "+tenantDomain);
                }
            }
        }   
        catch (Throwable t)
        {
            StringWriter stackTrace = new StringWriter();
            t.printStackTrace(new PrintWriter(stackTrace));
            System.err.println(stackTrace.toString());
            throw t;
        }
    }
    
    public void testLoginTenantGuests() throws Throwable
    {
        logger.info("Login tenant guests");
        
        try
        {
            AuthenticationUtil.clearCurrentSecurityContext();
            
            for (final String tenantDomain : tenants)
            {
                loginLogoutUser(tenantService.getDomainUser(DEFAULT_GUEST_UN, tenantDomain), DEFAULT_GUEST_UN);
            }
        }   
        catch (Throwable t)
        {
            StringWriter stackTrace = new StringWriter();
            t.printStackTrace(new PrintWriter(stackTrace));
            System.err.println(stackTrace.toString());
            throw t;
        }
    }
    
    public void testLoginTenantAdmin() throws Throwable
    {
        logger.info("Login tenant admins");
        
        try
        {
            AuthenticationUtil.clearCurrentSecurityContext();
            
            for (final String tenantDomain : tenants)
            {
                loginLogoutUser(tenantService.getDomainUser(AuthenticationUtil.getAdminUserName(), tenantDomain), DEFAULT_ADMIN_PW+" "+tenantDomain);
            }
        }   
        catch (Throwable t)
        {
            StringWriter stackTrace = new StringWriter();
            t.printStackTrace(new PrintWriter(stackTrace));
            System.err.println(stackTrace.toString());
            throw t;
        }
    }
    
    public void testCreateGroups()
    {
        logger.info("Create demo groups");
        
        for (final String tenantDomain : tenants)
        {
            String tenantAdminName = tenantService.getDomainUser(AuthenticationUtil.getAdminUserName(), tenantDomain);
            
            AuthenticationUtil.runAs(new RunAsWork<Object>()
                    {
                        public Object doWork() throws Exception
                        {
                            createGroup("GrpA-"+tenantDomain, null);
                            createGroup("SubGrpA-"+tenantDomain, "GrpA-"+tenantDomain);
                            
                            createGroup("GrpB-"+tenantDomain, null);
                            createGroup("SubGrpB-"+tenantDomain, "GrpB-"+tenantDomain);
                                                        
                            if (tenantDomain.equals(TEST_TENANT_DOMAIN2))
                            {
                                createGroup("GrpC-"+tenantDomain, null);
                                createGroup("SubGrpC-"+tenantDomain, "GrpC-"+tenantDomain);
                            }
                            
                            return null;
                        }
                    }, tenantAdminName);
            
        }
    }
    
    public void testCreateCategories()
    {
        logger.info("Create demo categories");
        
        // super admin
        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                logger.info("Create demo categories - super tenant");
                createCategoriesImpl("");
                
                return null;
            }
        }, AuthenticationUtil.getAdminUserName());
        
        for (final String tenantDomain : tenants)
        {
            String tenantAdminName = tenantService.getDomainUser(AuthenticationUtil.getAdminUserName(), tenantDomain);
            
            AuthenticationUtil.runAs(new RunAsWork<Object>()
            {
                public Object doWork() throws Exception
                {
                    logger.info("Create demo categories - "+tenantDomain);
                    
                    createCategoriesImpl(tenantDomain);
                    
                    return null;
                }
            }, tenantAdminName);
            
        }
    }
    
    @SuppressWarnings("unchecked")
    private void createCategoriesImpl(String tenantDomain)
    {
        if (tenantDomain.equals(TenantService.DEFAULT_DOMAIN))
        {
            Collection<ChildAssociationRef> childAssocs = categoryService.getRootCategories(SPACES_STORE, ContentModel.ASPECT_GEN_CLASSIFIABLE);
            
            for (ChildAssociationRef childAssoc : childAssocs)
            {
                if (nodeService.getProperty(childAssoc.getChildRef(), ContentModel.PROP_NAME).equals("CatA"))
                {
                    return; // re-runnable, else we need to delete the created categories
                }
            }
        }
        
        // Find all root categories
        String query = "PATH:\"/cm:generalclassifiable/*\"";
        ResultSet resultSet = searchService.query(SPACES_STORE, SearchService.LANGUAGE_LUCENE, query);
        int cnt = resultSet.length();
        
        NodeRef catA = createCategory(SPACES_STORE, null, "CatA", "CatA-"+tenantDomain);
        createCategory(SPACES_STORE, catA, "SubCatA", "SubCatA-"+tenantDomain); // ignore return
        
        NodeRef catB = createCategory(SPACES_STORE, null, "CatB", "CatB-"+tenantDomain);
        createCategory(SPACES_STORE, catB, "SubCatB", "SubCatB-"+tenantDomain); // ignore return
        
        cnt = cnt + 2;
        
        if (tenantDomain.equals(TEST_TENANT_DOMAIN2))
        {
            NodeRef catC = createCategory(SPACES_STORE, null, "CatC", "CatC-"+tenantDomain);
            createCategory(SPACES_STORE, catC, "SubCatC", "SubCatC-"+tenantDomain); // ignore return
            
            cnt = cnt + 1;
        }
        
        // Find all root categories
        resultSet = searchService.query(SPACES_STORE, SearchService.LANGUAGE_LUCENE, query);
        assertEquals(cnt, resultSet.length());
        
        String queryMembers = "PATH:\"/cm:generalclassifiable//cm:catA/member\"";
        resultSet = searchService.query(SPACES_STORE, SearchService.LANGUAGE_LUCENE, queryMembers);
        assertEquals(0, resultSet.length());
        
        NodeRef homeSpaceRef = getHomeSpaceFolderNode(AuthenticationUtil.getRunAsUser());
        NodeRef contentRef = addContent(homeSpaceRef, "tqbfjotld.txt", "The quick brown fox jumps over the lazy dog (tenant " + tenantDomain + ")", MimetypeMap.MIMETYPE_TEXT_PLAIN);
        
        assertFalse(nodeService.hasAspect(contentRef, ContentModel.ASPECT_GEN_CLASSIFIABLE));
        
        List<NodeRef> categories = (List<NodeRef>)nodeService.getProperty(contentRef, ContentModel.PROP_CATEGORIES);
        assertNull(categories);
        
        // Classify the node (ie. assign node to a particular category in a classification)
        categories = new ArrayList<NodeRef>(1);
        categories.add(catA);
        
        HashMap<QName, Serializable> catProps = new HashMap<QName, Serializable>();
        catProps.put(ContentModel.PROP_CATEGORIES, (Serializable)categories);
        nodeService.addAspect(contentRef, ContentModel.ASPECT_GEN_CLASSIFIABLE, catProps);
        
        assertTrue(nodeService.hasAspect(contentRef, ContentModel.ASPECT_GEN_CLASSIFIABLE));
        
        categories = (List<NodeRef>)nodeService.getProperty(contentRef, ContentModel.PROP_CATEGORIES);
        assertEquals(1, categories.size());
        
        // test ETHREEOH-210
        queryMembers = "PATH:\"/cm:generalclassifiable//cm:CatA/member\"";
        resultSet = searchService.query(SPACES_STORE, SearchService.LANGUAGE_LUCENE, queryMembers);
        assertEquals(1, resultSet.length());
    }
    
    public void testCreateFolders()
    {
        logger.info("Create demo folders");
        
        List<String> users = new ArrayList<String>(3);
        users.add(TEST_USER1);
        users.add(TEST_USER2);
        users.add(TEST_USER3);
        
        for (final String tenantDomain : tenants)
        {
            for (String baseUserName : users)
            {
                if ((! baseUserName.equals(TEST_USER3)) || (tenantDomain.equals(TEST_TENANT_DOMAIN2)))
                {
                    final String tenantUserName = tenantService.getDomainUser(baseUserName, tenantDomain);
                    
                    AuthenticationUtil.runAs(new RunAsWork<Object>()
                    {
                        public Object doWork() throws Exception
                        {
                            NodeRef homeSpaceRef = getHomeSpaceFolderNode(tenantUserName);
                            
                            NodeRef folderRef = createFolderNode(homeSpaceRef, "myfolder1");
                            createFolderNode(folderRef, "mysubfolder1"); // ignore return
                            
                            folderRef = createFolderNode(homeSpaceRef, "myfolder2"); 
                            createFolderNode(folderRef, "mysubfolder2"); // ignore return
                            
                            if (tenantDomain.equals(TEST_TENANT_DOMAIN2))
                            {
                                folderRef = createFolderNode(homeSpaceRef, "myfolder3"); 
                                createFolderNode(folderRef, "mysubfolder3"); // ignore return
                            }
                            
                            return null;
                        }
                    }, tenantUserName);
                }
            }
        }
    }
    
    public void testCreateVersionableUserContent()
    {
        logger.info("Create demo content");
        
        List<String> users = new ArrayList<String>(3);
        users.add(TEST_USER1);
        users.add(TEST_USER2);
        users.add(TEST_USER3);
        
        for (final String tenantDomain : tenants)
        {
            for (String baseUserName : users)
            {
                if ((! baseUserName.equals(TEST_USER3)) || (tenantDomain.equals(TEST_TENANT_DOMAIN2)))
                {
                    final String tenantUserName = tenantService.getDomainUser(baseUserName, tenantDomain);
                    
                    AuthenticationUtil.runAs(new RunAsWork<Object>()
                    {
                        public Object doWork() throws Exception
                        {
                            NodeRef homeSpaceRef = getHomeSpaceFolderNode(tenantUserName);
                            
                            NodeRef contentRef = addContent(homeSpaceRef, tenantUserName+" quick brown fox.txt", "The quick brown fox jumps over the lazy dog (tenant " + tenantDomain + ")", MimetypeMap.MIMETYPE_TEXT_PLAIN);
                            nodeService.addAspect(contentRef, ContentModel.ASPECT_VERSIONABLE, null);
                            
                            if (tenantDomain.equals(TEST_TENANT_DOMAIN2))
                            {
                                contentRef = addContent(homeSpaceRef, tenantUserName+" quick brown fox ANO.txt", "The quick brown fox jumps over the lazy dog ANO (tenant " + tenantDomain + ")", MimetypeMap.MIMETYPE_TEXT_PLAIN);
                                nodeService.addAspect(contentRef, ContentModel.ASPECT_VERSIONABLE, null);
                            }
                            
                            return null;
                        }
                    }, tenantUserName);
                }
            }
        }
    }
    
    public void testGetStores()
    {
        logger.info("Get tenant stores");
        
        // system
        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                for (StoreRef storeRef : nodeService.getStores())
                {
                    System.out.println("StoreRef: "+storeRef);
                }
                
                assertTrue("System: "+nodeService.getStores().size()+", "+(tenants.size()+1), (nodeService.getStores().size() >= (DEFAULT_STORE_COUNT * (tenants.size()+1))));
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
        
        // super admin
        AuthenticationUtil.runAs(new RunAsWork<Object>()
        {
            public Object doWork() throws Exception
            {
                assertTrue("Super admin: "+nodeService.getStores().size(), (nodeService.getStores().size() >= DEFAULT_STORE_COUNT));
                return null;
            }
        }, AuthenticationUtil.getAdminUserName());
        
        for (final String tenantDomain : tenants)
        {
            String tenantAdminName = tenantService.getDomainUser(AuthenticationUtil.getAdminUserName(), tenantDomain);
            
            AuthenticationUtil.runAs(new RunAsWork<Object>()
            {
                public Object doWork() throws Exception
                {
                    assertEquals("Tenant: "+tenantDomain, DEFAULT_STORE_COUNT, nodeService.getStores().size());
                    
                    return null;
                }
            }, tenantAdminName);
        }
    }
    
    public void testGetProperty()
    {
        logger.info("Test get property");
        
        for (final String tenantDomain : tenants)
        {
            String tenantAdminName = tenantService.getDomainUser(AuthenticationUtil.getAdminUserName(), tenantDomain);
            
            AuthenticationUtil.runAs(new RunAsWork<Object>()
            {
                public Object doWork() throws Exception
                {
                    NodeRef personNodeRef = createUser(TEST_USER4, tenantDomain, TEST_USER4+" "+tenantDomain);
                    
                    // Test nodeRef property
                    NodeRef homeFolderNodeRef = (NodeRef)nodeService.getProperty(personNodeRef, ContentModel.PROP_HOMEFOLDER);
                    assertFalse(homeFolderNodeRef.toString().contains(tenantDomain));
                    
                    Map<QName, Serializable> props = (Map<QName, Serializable>)nodeService.getProperties(personNodeRef);
                    assertFalse(props.get(ContentModel.PROP_HOMEFOLDER).toString().contains(tenantDomain));
                    
                    // Test "store-identifier" property
                    String storeId = (String)nodeService.getProperty(personNodeRef, ContentModel.PROP_STORE_IDENTIFIER);
                    assertFalse(storeId.contains(tenantDomain));
                    
                    assertFalse(props.get(ContentModel.PROP_STORE_IDENTIFIER).toString().contains(tenantDomain));
                    
                    return null;
                }
            }, tenantAdminName);
        }
    }
    
    public void testCOCIandSearch()
    {
        logger.info("Test checkout/checkin and search");
        
        for (final String tenantDomain : tenants)
        {
            final String tenantAdminName = tenantService.getDomainUser(AuthenticationUtil.getAdminUserName(), tenantDomain);
            
            AuthenticationUtil.runAs(new RunAsWork<Object>()
            {
                public Object doWork() throws Exception
                {
                    // search for local copy of bootstrapped file 'invite_user_email.ftl' (see email_templates.acp)
                    String origText = "You have been invited to";
                    String query = "+PATH:\"/app:company_home/app:dictionary/app:email_templates/app:invite_email_templates/*\" +TEXT:\""+origText+"\"";
                    ResultSet resultSet = searchService.query(SPACES_STORE, SearchService.LANGUAGE_LUCENE, query);
                    assertEquals(1, resultSet.length());
                    
                    NodeRef nodeRef = resultSet.getNodeRef(0);
                    
                    // checkout, update and checkin
                    
                    NodeRef workingCopyNodeRef = cociService.checkout(nodeRef);
                    
                    ContentWriter writer = contentService.getWriter(workingCopyNodeRef, ContentModel.PROP_CONTENT, true);
                    
                    writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                    writer.setEncoding("UTF-8");
                    
                    String updateText = "Updated by "+tenantAdminName;
                    writer.putContent(updateText);
                    
                    cociService.checkin(workingCopyNodeRef, null);
                    
                    query = "+PATH:\"/app:company_home/app:dictionary/app:email_templates/app:invite_email_templates/*\" +TEXT:\""+origText+"\"";
                    resultSet = searchService.query(SPACES_STORE, SearchService.LANGUAGE_LUCENE, query);
                    assertEquals(0, resultSet.length());
                    
                    query = "+PATH:\"/app:company_home/app:dictionary/app:email_templates/app:invite_email_templates/*\" +TEXT:\""+updateText+"\"";
                    resultSet = searchService.query(SPACES_STORE, SearchService.LANGUAGE_LUCENE, query);
                    assertEquals(1, resultSet.length());
                    
                    return null;
                }
            }, tenantAdminName);
        }
    }
    
    public void testDeleteArchiveAndRestoreContent()
    {
        logger.info("test delete/archive & restore content");
        
        for (final String tenantDomain : tenants)
        {
            final String tenantUserName = tenantService.getDomainUser(TEST_USER1, tenantDomain);
            
            AuthenticationUtil.runAs(new RunAsWork<Object>()
            {
                public Object doWork() throws Exception
                {
                    NodeRef homeSpaceRef = getHomeSpaceFolderNode(tenantUserName);
                    NodeRef contentRef = addContent(homeSpaceRef, tenantUserName+" tqbfjotld.txt", "The quick brown fox jumps over the lazy dog (tenant " + tenantDomain + ")", MimetypeMap.MIMETYPE_TEXT_PLAIN);
                    
                    NodeRef storeArchiveNode = nodeService.getStoreArchiveNode(contentRef.getStoreRef());
                    
                    nodeService.deleteNode(contentRef);
                    
                    // deduce archived nodeRef
                    StoreRef archiveStoreRef = storeArchiveNode.getStoreRef();
                    NodeRef archivedContentRef = new NodeRef(archiveStoreRef, contentRef.getId());
                    
                    nodeService.restoreNode(archivedContentRef, null, null, null);
                    
                    return null;
                }
            }, tenantUserName);
        }
    }
    
    public void testCustomModels()
    {
        logger.info("test custom models");
        
        final int defaultModelCnt = dictionaryService.getAllModels().size();
        
        for (final String tenantDomain : tenants)
        {
            final String tenantAdminName = tenantService.getDomainUser(AuthenticationUtil.getAdminUserName(), tenantDomain);
            
            AuthenticationUtil.runAs(new RunAsWork<Object>()
            {
                public Object doWork() throws Exception
                {
                    // no custom models should be deployed yet (note: will fail if re-run)
                    assertEquals(0, repoAdminService.getModels().size());
                    assertEquals(defaultModelCnt, dictionaryService.getAllModels().size());
                    assertNull(dictionaryService.getClass(QName.createQName("{my.new.model}sop")));
                    
                    // deploy custom model
                    InputStream modelStream = getClass().getClassLoader().getResourceAsStream("tenant/exampleModel.xml");
                    repoAdminService.deployModel(modelStream, "exampleModel.xml");
                    
                    assertEquals(1, repoAdminService.getModels().size());
                    assertEquals(defaultModelCnt+1, dictionaryService.getAllModels().size());
                    
                    ClassDefinition myType = dictionaryService.getClass(QName.createQName("{my.new.model}sop"));
                    assertNotNull(myType);
                    assertEquals(QName.createQName("{my.new.model}mynewmodel"),myType.getModel().getName());
                    
                    // deactivate model
                    repoAdminService.deactivateModel("exampleModel.xml");
                    
                    assertEquals(1, repoAdminService.getModels().size()); // still deployed, although not active
                    assertEquals(defaultModelCnt, dictionaryService.getAllModels().size());
                    assertNull(dictionaryService.getClass(QName.createQName("{my.new.model}sop")));
                    
                    // re-activate model
                    repoAdminService.activateModel("exampleModel.xml");
                    
                    assertEquals(1, repoAdminService.getModels().size());
                    assertEquals(defaultModelCnt+1, dictionaryService.getAllModels().size());
                    
                    myType = dictionaryService.getClass(QName.createQName("{my.new.model}sop"));
                    assertNotNull(myType);
                    assertEquals(QName.createQName("{my.new.model}mynewmodel"),myType.getModel().getName());
                    
                    return null;
                }
            }, tenantAdminName);
        }
    }
    
    public void testAddCustomWebClient()
    {
        // note: add as demo files - need to re-start Alfresco to see custom web client config / messages 
        logger.info("test add custom web client config");
        
        for (final String tenantDomain : tenants)
        {    
            final String tenantAdminName = tenantService.getDomainUser(AuthenticationUtil.getAdminUserName(), tenantDomain);
            
            AuthenticationUtil.runAs(new RunAsWork<Object>()
            {
                public Object doWork() throws Exception
                {
                    NodeRef webClientExtFolder = getWebClientExtensionNodeRef(SPACES_STORE);
                    
                    InputStream is = getClass().getClassLoader().getResourceAsStream("tenant/webclient.properties");
                    addContent(webClientExtFolder, "webclient.properties", is, MimetypeMap.MIMETYPE_TEXT_PLAIN);
                    
                    is = getClass().getClassLoader().getResourceAsStream("tenant/web-client-config-custom.xml");
                    addContent(webClientExtFolder, "web-client-config-custom.xml", is, MimetypeMap.MIMETYPE_XML);
                    
                    return null;
                }
            }, tenantAdminName);
        }
    }
    
    private void createGroup(String shortName, String parentShortName)
    {
        // create new Group using authority Service
        String groupName = this.authorityService.getName(AuthorityType.GROUP, shortName);
        if (this.authorityService.authorityExists(groupName) == false)
        {
           String parentGroupName = null;
           if (parentShortName != null)
           {
               parentGroupName = this.authorityService.getName(AuthorityType.GROUP, parentShortName);
               if (this.authorityService.authorityExists(parentGroupName) == false)
               {
                   logger.warn("Parent group does not exist: " + parentShortName);
                   return;
               }
           }
           
           this.authorityService.createAuthority(AuthorityType.GROUP, shortName);
           
           if (parentGroupName != null)
           {
               this.authorityService.addAuthority(parentGroupName, groupName);
           }
           
        }
        else
        {
            logger.warn("Group already exists: " + shortName);
        }
    }
    
    
    private NodeRef createUser(String baseUserName, String tenantDomain, String password)
    {
        String userName = tenantService.getDomainUser(baseUserName, tenantDomain);
        
        NodeRef personNodeRef = null;
        
        if (! this.authenticationService.authenticationExists(userName))
        {
            NodeRef baseHomeFolder = getUserHomesNodeRef(SPACES_STORE);
            
            // Create the users home folder
            NodeRef homeFolder = createHomeSpaceFolderNode(
                                                baseHomeFolder,
                                                baseUserName,
                                                userName);
            
            // Create the authentication
            this.authenticationService.createAuthentication(userName, password.toCharArray());
            
            // Create the person
            Map<QName, Serializable> personProperties = new HashMap<QName, Serializable>();
            personProperties.put(ContentModel.PROP_USERNAME, userName);
            personProperties.put(ContentModel.PROP_HOMEFOLDER, homeFolder);
            personProperties.put(ContentModel.PROP_FIRSTNAME, baseUserName);
            personProperties.put(ContentModel.PROP_LASTNAME, baseUserName+"-"+tenantDomain); // add domain suffix here for demo only
            personProperties.put(ContentModel.PROP_EMAIL, userName);
            
            personNodeRef = this.personService.createPerson(personProperties);
            
            // ensure the user can access their own Person object
            this.permissionService.setPermission(personNodeRef, userName, permissionService.getAllPermission(), true);
            
            NodeRef checkHomeSpaceRef = (NodeRef)nodeService.getProperty(personNodeRef, ContentModel.PROP_HOMEFOLDER);
            assertNotNull(checkHomeSpaceRef);
            
            logger.info("Created user " + userName);
        }
        else
        {
            personNodeRef = personService.getPerson(userName);
            
            logger.info("Found existing user " + userName);
        }
        
        return personNodeRef;
    }
    
    private void loginLogoutUser(String username, String password)
    {
        // authenticate via the authentication service
        authenticationService.authenticate(username, password.toCharArray());
        
        // set the user name as stored by the back end 
        username = authenticationService.getCurrentUserName();
        
        NodeRef personRef = personService.getPerson(username);
        NodeRef homeSpaceRef = (NodeRef)nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);
        
        // check that the home space node exists - else user cannot login
        if (nodeService.exists(homeSpaceRef) == false)
        {
           throw new InvalidNodeRefException(homeSpaceRef);
        }
        
        // logout
        authenticationService.clearCurrentSecurityContext();
    }
    
    private NodeRef getUserHomesNodeRef(StoreRef storeRef)
    {
        // get the "User Homes" location
        return findFolderNodeRef(storeRef, "/app:company_home/app:user_homes");
    }
    
    private NodeRef getWebClientExtensionNodeRef(StoreRef storeRef)
    {
        // get the "Web Client Extensions" location
        return findFolderNodeRef(storeRef, "/app:company_home/app:dictionary/app:webclient_extension");
    }
    
    private NodeRef findFolderNodeRef(StoreRef storeRef, String folderXPath)
    {
        ResultSet rs = this.searchService.query(storeRef, SearchService.LANGUAGE_XPATH, folderXPath);
        
        NodeRef folderNodeRef = null;
        if (rs.length() != 1)
        {
            throw new AlfrescoRuntimeException("Cannot find folder location: " + folderXPath);
        }
        else
        {
            folderNodeRef = rs.getNodeRef(0);
        }
        return folderNodeRef;
    }
    
    private NodeRef createFolderNode(NodeRef parentFolderNodeRef, String nameValue)
    {
        if (nameValue != null)
        {       
            Map<QName, Serializable> folderProps = new HashMap<QName, Serializable>();
            folderProps.put(ContentModel.PROP_NAME, nameValue);
            
            return this.nodeService.createNode(
                    parentFolderNodeRef, 
                    ContentModel.ASSOC_CONTAINS, 
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, nameValue),
                    ContentModel.TYPE_FOLDER,
                    folderProps).getChildRef();
        }
        
        return null;
    }
    
    private NodeRef createCategory(StoreRef storeRef, NodeRef parentCategoryRef, String name, String description)
    {
        // create category using categoryservice
        NodeRef ref;
        if (parentCategoryRef == null)
        {
           ref = this.categoryService.createRootCategory(storeRef, ContentModel.ASPECT_GEN_CLASSIFIABLE, name);
        }
        else
        {
           ref = categoryService.createCategory(parentCategoryRef, name);
        }
        
        // apply the titled aspect - for description
        Map<QName, Serializable> titledProps = new HashMap<QName, Serializable>(1, 1.0f);
        titledProps.put(ContentModel.PROP_DESCRIPTION, description);
        this.nodeService.addAspect(ref, ContentModel.ASPECT_TITLED, titledProps);
        
        return ref;
    }
    
    private NodeRef createHomeSpaceFolderNode(NodeRef folderNodeRef, String spaceName, String userName)
    {
        if (spaceName != null)
        {       
            Map<QName, Serializable> folderProps = new HashMap<QName, Serializable>();
            folderProps.put(ContentModel.PROP_NAME, spaceName);
            
            NodeRef nodeRef = this.nodeService.createNode(
                                                folderNodeRef, 
                                                ContentModel.ASSOC_CONTAINS, 
                                                QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, spaceName),
                                                ContentModel.TYPE_FOLDER,
                                                folderProps).getChildRef();
            
            // apply the uifacets aspect - icon and title props
            Map<QName, Serializable> uiFacetsProps = new HashMap<QName, Serializable>(3);
            uiFacetsProps.put(ApplicationModel.PROP_ICON, "space-icon-default");
            uiFacetsProps.put(ContentModel.PROP_TITLE, spaceName);
            this.nodeService.addAspect(nodeRef, ApplicationModel.ASPECT_UIFACETS, uiFacetsProps);
            
            setupHomeSpacePermissions(nodeRef, userName);
            
            return nodeRef;
        }
        
        return null;
    }
    
    private void setupHomeSpacePermissions(NodeRef homeSpaceRef, String userName)
    {
       // Admin Authority has full permissions by default (automatic - set in the permission config)
       // give full permissions to the new user
       this.permissionService.setPermission(homeSpaceRef, userName, permissionService.getAllPermission(), true);
       
       // by default other users will only have GUEST access to the space contents
       String permission = "Consumer";
       
       if (permission != null && permission.length() != 0)
       {
          this.permissionService.setPermission(homeSpaceRef, permissionService.getAllAuthorities(), permission, true);
       }
       
       // the new user is the OWNER of their own space and always has full permissions
       this.ownableService.setOwner(homeSpaceRef, userName);
       this.permissionService.setPermission(homeSpaceRef, permissionService.getOwnerAuthority(), permissionService.getAllPermission(), true);
       
       // now detach (if we did this first we could not set any permissions!)
       this.permissionService.setInheritParentPermissions(homeSpaceRef, false);
    }
    
    private NodeRef getHomeSpaceFolderNode(String userName)
    {
        return (NodeRef)this.nodeService.getProperty(personService.getPerson(userName), ContentModel.PROP_HOMEFOLDER);
    }
    
    private NodeRef addContent(NodeRef spaceRef, String name, String textData, String mimeType)
    {
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, name);
        
        ChildAssociationRef association = nodeService.createNode(spaceRef,
                ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name),
                ContentModel.TYPE_CONTENT,
                contentProps);
        
        NodeRef content = association.getChildRef();
        
        // add titled aspect (for Web Client display)
        Map<QName, Serializable> titledProps = new HashMap<QName, Serializable>();
        titledProps.put(ContentModel.PROP_TITLE, name);
        titledProps.put(ContentModel.PROP_DESCRIPTION, name);
        this.nodeService.addAspect(content, ContentModel.ASPECT_TITLED, titledProps);
        
        ContentWriter writer = contentService.getWriter(content, ContentModel.PROP_CONTENT, true);
        
        writer.setMimetype(mimeType);
        writer.setEncoding("UTF-8");
        
        writer.putContent(textData);
        
        return content;
    }
    
    private NodeRef addContent(NodeRef spaceRef, String name, InputStream is, String mimeType)
    {
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, name);
        
        ChildAssociationRef association = nodeService.createNode(spaceRef,
                ContentModel.ASSOC_CONTAINS, 
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name), 
                ContentModel.TYPE_CONTENT,
                contentProps);
        
        NodeRef content = association.getChildRef();
        
        // add titled aspect (for Web Client display)
        Map<QName, Serializable> titledProps = new HashMap<QName, Serializable>();
        titledProps.put(ContentModel.PROP_TITLE, name);
        titledProps.put(ContentModel.PROP_DESCRIPTION, name);
        this.nodeService.addAspect(content, ContentModel.ASPECT_TITLED, titledProps);
        
        ContentWriter writer = contentService.getWriter(content, ContentModel.PROP_CONTENT, true);
        
        writer.setMimetype(mimeType);
        writer.setEncoding("UTF-8");
        
        writer.putContent(is);
        
        return content;
    }
    
    /*
    public static void main(String args[]) 
    {
        System.out.println(new Date());
        junit.textui.TestRunner.run(MultiTDemoTest.class);
        System.out.println(new Date());
    }
    */
}
