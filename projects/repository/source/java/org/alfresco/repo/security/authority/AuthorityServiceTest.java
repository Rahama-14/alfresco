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
package org.alfresco.repo.security.authority;

import junit.framework.TestCase;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

public class AuthorityServiceTest extends TestCase
{
    private static ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
    
    private AuthenticationComponent authenticationComponent;
    
    private AuthenticationService authenticationService;
    
    private AuthorityService authorityService; 
    
    private AuthorityService pubAuthorityService; 
    
    private PersonService personService;

    public AuthorityServiceTest()
    {
        super();
        
    }
    
    public void setUp() throws Exception
    {
        authenticationComponent = (AuthenticationComponent) ctx.getBean("authenticationComponent");
        authenticationService = (AuthenticationService) ctx.getBean("authenticationService");
        authorityService = (AuthorityService) ctx.getBean("authorityService");
        pubAuthorityService = (AuthorityService) ctx.getBean("AuthorityService");
        personService = (PersonService) ctx.getBean("personService");
        
        if(!authenticationComponent.exists("andy"))
        {
            authenticationService.createAuthentication("andy", "andy".toCharArray());
        }
        
        if(!authenticationComponent.exists("admin"))
        {
            authenticationService.createAuthentication("admin", "admin".toCharArray());
        }
        
        if(!authenticationComponent.exists("administrator"))
        {
            authenticationService.createAuthentication("administrator", "administrator".toCharArray());
        }
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        authenticationService.clearCurrentSecurityContext();
        super.tearDown();
    }
    
    public void testNonAdminUser()
    {
        authenticationComponent.setCurrentUser("andy");
        assertFalse(authorityService.hasAdminAuthority());
        assertFalse(pubAuthorityService.hasAdminAuthority());
        assertEquals(0, authorityService.getAuthorities().size());
    }
    
    public void testAdminUser()
    {
        authenticationComponent.setCurrentUser("admin");
        assertTrue(authorityService.hasAdminAuthority());
        assertTrue(pubAuthorityService.hasAdminAuthority());
        assertEquals(1, authorityService.getAuthorities().size());
        
        authenticationComponent.setCurrentUser("administrator");
        assertTrue(authorityService.hasAdminAuthority());
        assertTrue(pubAuthorityService.hasAdminAuthority());
        assertEquals(1, authorityService.getAuthorities().size());
    }
    
    public void testAuthorities()
    {
        assertEquals(1, pubAuthorityService.getAllAuthorities(AuthorityType.ADMIN).size());
        assertTrue(pubAuthorityService.getAllAuthorities(AuthorityType.ADMIN).contains(PermissionService.ADMINISTRATOR_AUTHORITY));
        assertEquals(1, pubAuthorityService.getAllAuthorities(AuthorityType.EVERYONE).size());
        assertTrue(pubAuthorityService.getAllAuthorities(AuthorityType.EVERYONE).contains(PermissionService.ALL_AUTHORITIES));
        assertEquals(1, pubAuthorityService.getAllAuthorities(AuthorityType.GROUP).size());
        assertTrue(pubAuthorityService.getAllAuthorities(AuthorityType.GROUP).contains(PermissionService.ALL_AUTHORITIES));
        assertEquals(1, pubAuthorityService.getAllAuthorities(AuthorityType.GUEST).size());
        assertTrue(pubAuthorityService.getAllAuthorities(AuthorityType.GUEST).contains(PermissionService.GUEST));
        assertEquals(0, pubAuthorityService.getAllAuthorities(AuthorityType.OWNER).size());
        assertEquals(0, pubAuthorityService.getAllAuthorities(AuthorityType.ROLE).size());
        assertEquals(personService.getAllPeople().size(),  pubAuthorityService.getAllAuthorities(AuthorityType.USER).size());
//        assertTrue(pubAuthorityService.getAllAuthorities(AuthorityType.USER).contains("andy"));
//        assertTrue(pubAuthorityService.getAllAuthorities(AuthorityType.USER).contains("admin"));
       
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
