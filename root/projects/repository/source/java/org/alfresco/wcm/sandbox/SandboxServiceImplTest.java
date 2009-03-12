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
package org.alfresco.wcm.sandbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.wcm.AbstractWCMServiceImplTest;
import org.alfresco.wcm.asset.AssetInfo;
import org.alfresco.wcm.asset.AssetService;
import org.alfresco.wcm.util.WCMUtil;
import org.alfresco.wcm.webproject.WebProjectInfo;

/**
 * Sandbox Service implementation unit test
 * 
 * @author janv
 */
public class SandboxServiceImplTest extends AbstractWCMServiceImplTest
{   
    // base sandbox
    private static final String TEST_SANDBOX = TEST_WEBPROJ_DNS+"-sandbox";
    
    private static final int SCALE_USERS = 5;
    private static final int SCALE_WEBPROJECTS = 2;
    
    //
    // services
    //
    
    private SandboxService sbService;
    private AssetService assetService;
    
    // TODO: temporary - remove from here when r13170 is merged from V3.1->HEAD
    private TransactionService transactionService;
    
    private AVMService avmService; // non-locking-aware
    
    //private AVMService avmLockingAwareService;
    //private AVMService avmNonLockingAwareService;

    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        // Get the required services
        sbService = (SandboxService)ctx.getBean("SandboxService");
        assetService = (AssetService)ctx.getBean("AssetService");
        
        avmService = (AVMService)ctx.getBean("AVMService");
        
        // TODO: temporary - remove from here when r13170 is merged from V3.1->HEAD
        transactionService = (TransactionService)ctx.getBean("TransactionService");
        
        // WCM locking
        //avmLockingAwareService = (AVMService)ctx.getBean("AVMLockingAwareService");
        
        // without WCM locking
        //avmNonLockingAwareService = (AVMService)ctx.getBean("AVMService");
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        if (CLEAN)
        {
            // Switch back to Admin
            AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
            
            List<WebProjectInfo> webProjects = wpService.listWebProjects();
            for (final WebProjectInfo wpInfo : webProjects)
            {
                if (wpInfo.getStoreId().startsWith(TEST_WEBPROJ_DNS))
                {
                    // TODO: temporary - remove from here when r13170 is merged from V3.1->HEAD
                    
                    // note: added retry for now, due to intermittent concurrent update (during tearDown) possibly due to OrphanReaper ?
                    // org.hibernate.StaleObjectStateException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect): [org.alfresco.repo.avm.PlainFileNodeImpl#3752]
                    RetryingTransactionCallback<Object> deleteWebProjectWork = new RetryingTransactionCallback<Object>()
                    {
                        public Object execute() throws Exception
                        {
                            wpService.deleteWebProject(wpInfo.getNodeRef());
                            return null;
                        }
                    };
                    transactionService.getRetryingTransactionHelper().doInTransaction(deleteWebProjectWork);

                }
            }

            deleteUser(USER_ONE);
            deleteUser(USER_TWO);
            deleteUser(USER_THREE);
            deleteUser(USER_FOUR);
        }
        
        AuthenticationUtil.clearCurrentSecurityContext();
        super.tearDown();
    }
    
    public void testSimple()
    {
        int storeCnt = avmService.getStores().size();
        
        // create web project (also creates staging sandbox and admin's author sandbox)
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-sandboxSimple", TEST_WEBPROJ_NAME+"-sandboxSimple", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION, TEST_WEBPROJ_DEFAULT_WEBAPP, TEST_WEBPROJ_DONT_USE_AS_TEMPLATE, null);
        String wpStoreId = wpInfo.getStoreId();
        
        // list 2 sandboxes
        assertEquals(2, sbService.listSandboxes(wpStoreId).size());
        
        // list 4 extra AVM stores (2 per sandbox)
        assertEquals(storeCnt+4, avmService.getStores().size()); // 2x stating (main,preview), 2x admin author (main, preview)
        
        // get admin's sandbox
        SandboxInfo sbInfo = sbService.getAuthorSandbox(wpStoreId);
        assertNotNull(sbInfo);
        
        // get staging sandbox
        sbInfo = sbService.getStagingSandbox(wpStoreId);
        assertNotNull(sbInfo);

        // invite user one to the web project and do not implicitly create user one's sandbox
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_PUBLISHER, false);
        assertEquals(2, sbService.listSandboxes(wpStoreId).size());
        
        sbInfo = sbService.createAuthorSandbox(wpStoreId, USER_TWO);
        assertEquals(3, sbService.listSandboxes(wpStoreId).size());
        
        sbInfo = sbService.getSandbox(sbInfo.getSandboxId());
        sbService.deleteSandbox(sbInfo.getSandboxId());
        assertEquals(2, sbService.listSandboxes(wpStoreId).size());
        
        // delete admin's sandbox
        sbService.deleteSandbox(sbService.getAuthorSandbox(wpStoreId).getSandboxId());
        assertEquals(1, sbService.listSandboxes(wpStoreId).size());
        
        // delete web project (also deletes staging sandbox)
        wpService.deleteWebProject(wpStoreId);
        
        assertEquals(storeCnt, avmService.getStores().size());
    }
	
    public void testCreateAuthorSandbox()
    {
        // Create a web project
        WebProjectInfo wpInfo1 = wpService.createWebProject(TEST_SANDBOX+"-create-author", TEST_WEBPROJ_NAME+"-author", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION, TEST_WEBPROJ_DEFAULT_WEBAPP, TEST_WEBPROJ_DONT_USE_AS_TEMPLATE, null);
        
        String expectedUserSandboxId = TEST_SANDBOX+"-create-author" + "--" + AuthenticationUtil.getAdminUserName();
        
        SandboxInfo sbInfo1 = sbService.getAuthorSandbox(wpInfo1.getStoreId());
        checkSandboxInfo(sbInfo1, expectedUserSandboxId, AuthenticationUtil.getAdminUserName(), AuthenticationUtil.getAdminUserName(), expectedUserSandboxId, SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN);
        
        sbInfo1 = sbService.getAuthorSandbox(wpInfo1.getStoreId(), USER_ONE);
        assertNull(sbInfo1);
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        sbInfo1 = sbService.getAuthorSandbox(wpInfo1.getStoreId());
        assertNull(sbInfo1);
        
        // Switch back to admin
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        // Invite web user
        wpService.inviteWebUser(wpInfo1.getStoreId(), USER_ONE, WCMUtil.ROLE_CONTENT_MANAGER);
        
        // Create author sandbox for user one - admin is the creator
        sbInfo1 = sbService.createAuthorSandbox(wpInfo1.getStoreId(), USER_ONE);

        expectedUserSandboxId = TEST_SANDBOX+"-create-author" + "--" + USER_ONE;
        
        sbInfo1 = sbService.getAuthorSandbox(wpInfo1.getStoreId(), USER_ONE);
        checkSandboxInfo(sbInfo1, expectedUserSandboxId, USER_ONE, AuthenticationUtil.getAdminUserName(), expectedUserSandboxId, SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN);
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        // Get author sandbox
        sbInfo1 = sbService.getAuthorSandbox(wpInfo1.getStoreId());
        checkSandboxInfo(sbInfo1, expectedUserSandboxId, USER_ONE, AuthenticationUtil.getAdminUserName(), expectedUserSandboxId, SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN);
        
        String userSandboxId = sbInfo1.getSandboxId();
        
        // Get (author) sandbox
        sbInfo1 = sbService.getSandbox(userSandboxId);
        checkSandboxInfo(sbInfo1, expectedUserSandboxId, USER_ONE, AuthenticationUtil.getAdminUserName(), expectedUserSandboxId, SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN);
        
        // Should return same as before
        sbInfo1 = sbService.createAuthorSandbox(wpInfo1.getStoreId());
        checkSandboxInfo(sbInfo1, expectedUserSandboxId, USER_ONE, AuthenticationUtil.getAdminUserName(), expectedUserSandboxId, SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN);
        
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        try
        {
            // Try to create author sandbox as a non-web user (-ve test)
            sbService.createAuthorSandbox(wpInfo1.getStoreId()); // ignore return
            fail("Shouldn't be able to create author store since not a web user");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        
        // Switch back to admin
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        // Invite web user
        wpService.inviteWebUser(wpInfo1.getStoreId(), USER_TWO, WCMUtil.ROLE_CONTENT_REVIEWER);
        
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        // Get author sandbox
        sbInfo1 = sbService.getAuthorSandbox(wpInfo1.getStoreId());
        assertNull(sbInfo1);
        
        expectedUserSandboxId = TEST_SANDBOX+"-create-author" + "--" + USER_TWO;
        
        // Create own sandbox - user two is the creator
        sbInfo1 = sbService.createAuthorSandbox(wpInfo1.getStoreId());
        checkSandboxInfo(sbInfo1, expectedUserSandboxId, USER_TWO, USER_TWO, expectedUserSandboxId, SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN);
    }
    
    private void checkSandboxInfo(SandboxInfo sbInfo, String expectedStoreId, String expectedName, String expectedCreator, String expectedMainStoreName, QName expectedSandboxType)
    {
        assertNotNull(sbInfo);
        assertEquals(expectedStoreId, sbInfo.getSandboxId());
        assertEquals(expectedName, sbInfo.getName());
        assertEquals(expectedCreator, sbInfo.getCreator());
        assertNotNull(sbInfo.getCreatedDate());
        assertEquals(expectedMainStoreName, sbInfo.getMainStoreName());
        assertEquals(expectedSandboxType, sbInfo.getSandboxType());
    }
    
    public void testListSandboxes() throws Exception
    {
        // Create web project - implicitly creates staging sandbox and also author sandbox for web project creator (in this case, admin)
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-list", TEST_WEBPROJ_NAME+" list", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        String wpStoreId = wpInfo.getStoreId();
        
        List<SandboxInfo> sbInfos = sbService.listSandboxes(wpInfo.getStoreId());
        assertEquals(2, sbInfos.size()); // staging sandbox, author sandbox (for admin)
        
        String expectedUserSandboxId = TEST_SANDBOX+"-list" + "--" + AuthenticationUtil.getAdminUserName();
        
        // Do detailed check of the sandbox info objects
        for (SandboxInfo sbInfo : sbInfos)
        {
            QName sbType = sbInfo.getSandboxType();
            
            if (sbType.equals(SandboxConstants.PROP_SANDBOX_STAGING_MAIN) == true)
            {
                checkSandboxInfo(sbInfo, TEST_SANDBOX+"-list", TEST_SANDBOX+"-list", AuthenticationUtil.getAdminUserName(), TEST_SANDBOX+"-list", SandboxConstants.PROP_SANDBOX_STAGING_MAIN);
            }
            else if (sbType.equals(SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN) == true)
            {
                checkSandboxInfo(sbInfo, expectedUserSandboxId, AuthenticationUtil.getAdminUserName(), AuthenticationUtil.getAdminUserName(), expectedUserSandboxId, SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN);
            }
            else
            {
                fail("The sandbox store id " + sbInfo.getSandboxId() + " is not recognised");
            }
        }
        
        // test roles
        
        // Invite web users
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_MANAGER, true);
        wpService.inviteWebUser(wpStoreId, USER_TWO, WCMUtil.ROLE_CONTENT_PUBLISHER, true);
        wpService.inviteWebUser(wpStoreId, USER_THREE, WCMUtil.ROLE_CONTENT_REVIEWER, true);
        wpService.inviteWebUser(wpStoreId, USER_FOUR, WCMUtil.ROLE_CONTENT_CONTRIBUTOR, true);
        
        // admin can list all sandboxes
        sbInfos = sbService.listSandboxes(wpInfo.getStoreId());
        assertEquals(6, sbInfos.size());
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        // content manager can list all sandboxes
        sbInfos = sbService.listSandboxes(wpInfo.getStoreId());
        assertEquals(6, sbInfos.size());
        
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);

        // Content publisher - can only list own sandbox and staging
        sbInfos = sbService.listSandboxes(wpInfo.getStoreId());
        assertEquals(2, sbInfos.size());
        
        // Switch to USER_THREE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_THREE);
        
        // Content reviewer - can only list own sandbox and staging
        sbInfos = sbService.listSandboxes(wpInfo.getStoreId());
        assertEquals(2, sbInfos.size());
               
        // Switch to USER_FOUR
        AuthenticationUtil.setFullyAuthenticatedUser(USER_FOUR);
        
        // Content contributor - can only list own sandbox and staging
        sbInfos = sbService.listSandboxes(wpInfo.getStoreId());
        assertEquals(2, sbInfos.size());
    }
    
    public void testGetSandbox()
    {
        // Get a sandbox that isn't there
        SandboxInfo sbInfo = sbService.getSandbox(TEST_SANDBOX+"-get");
        assertNull(sbInfo);
        
        // Create web project - implicitly creates staging sandbox and also admin sandbox (author sandbox for web project creator)
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-get", TEST_WEBPROJ_NAME+" get", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        String wpStoreId = wpInfo.getStoreId();
        
        // Get staging sandbox
        sbInfo = sbService.getStagingSandbox(wpInfo.getStoreId());
        checkSandboxInfo(sbInfo, TEST_SANDBOX+"-get", TEST_SANDBOX+"-get", AuthenticationUtil.getAdminUserName(), TEST_SANDBOX+"-get", SandboxConstants.PROP_SANDBOX_STAGING_MAIN);
        
        // Get (staging) sandbox
        String stagingSandboxId = wpInfo.getStagingStoreName();
        sbInfo = sbService.getSandbox(stagingSandboxId);
        checkSandboxInfo(sbInfo, TEST_SANDBOX+"-get", TEST_SANDBOX+"-get", AuthenticationUtil.getAdminUserName(), TEST_SANDBOX+"-get", SandboxConstants.PROP_SANDBOX_STAGING_MAIN);

        // Get (author) sandbox
        sbInfo = sbService.getAuthorSandbox(wpStoreId);      
        sbInfo = sbService.getSandbox(sbInfo.getSandboxId());       
        String userSandboxId = TEST_SANDBOX+"-get" + "--" + AuthenticationUtil.getAdminUserName();
        checkSandboxInfo(sbInfo, userSandboxId, AuthenticationUtil.getAdminUserName(), AuthenticationUtil.getAdminUserName(), userSandboxId, SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN);

        // test roles
        
        // Invite web users
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_MANAGER, true);
        wpService.inviteWebUser(wpStoreId, USER_TWO, WCMUtil.ROLE_CONTENT_PUBLISHER, true);
        wpService.inviteWebUser(wpStoreId, USER_THREE, WCMUtil.ROLE_CONTENT_REVIEWER, true);
        wpService.inviteWebUser(wpStoreId, USER_FOUR, WCMUtil.ROLE_CONTENT_CONTRIBUTOR, true);
        
        // admin can get any sandbox
        userSandboxId = TEST_SANDBOX+"-get" + "--" + USER_THREE;
        sbInfo = sbService.getSandbox(userSandboxId);
        checkSandboxInfo(sbInfo, userSandboxId, USER_THREE, AuthenticationUtil.getAdminUserName(), userSandboxId, SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN);
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        // content manager can get any (author) sandbox
        userSandboxId = TEST_SANDBOX+"-get" + "--" + USER_THREE;
        sbInfo = sbService.getSandbox(userSandboxId);
        checkSandboxInfo(sbInfo, userSandboxId, USER_THREE, AuthenticationUtil.getAdminUserName(), userSandboxId, SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN);
     
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        try
        {
            // Content publisher - try to get another user's sandbox (-ve test)
            userSandboxId = TEST_SANDBOX+"-get" + "--" + USER_THREE;
            sbInfo = sbService.getSandbox(userSandboxId);
            fail("Shouldn't be able to get another author's sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        
        // Switch to USER_THREE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_THREE);
        
        try
        {
            // Content reviewer - try to get another user's sandbox (-ve test)
            userSandboxId = TEST_SANDBOX+"-get" + "--" + USER_TWO;
            sbInfo = sbService.getSandbox(userSandboxId);
            fail("Shouldn't be able to get another author's sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        
        // Switch to USER_FOUR
        AuthenticationUtil.setFullyAuthenticatedUser(USER_FOUR);
        
        try
        {
            // Content contributor - try to get another user's sandbox (-ve test)
            userSandboxId = TEST_SANDBOX+"-get" + "--" + USER_THREE;
            sbInfo = sbService.getSandbox(userSandboxId);
            fail("Shouldn't be able to get another author's sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
    }
    
    public void testIsSandboxType()
    {
        // Get a sandbox that isn't there
        SandboxInfo sbInfo = sbService.getSandbox(TEST_SANDBOX+"-is");
        assertNull(sbInfo);
        
        // Create web project - implicitly creates staging sandbox and also admin sandbox (author sandbox for web project creator)
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-is", TEST_WEBPROJ_NAME+" is", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);

        // Get staging sandbox
        sbInfo = sbService.getStagingSandbox(wpInfo.getStoreId());
        
        assertTrue(sbService.isSandboxType(sbInfo.getSandboxId(), SandboxConstants.PROP_SANDBOX_STAGING_MAIN));
        assertFalse(sbService.isSandboxType(sbInfo.getSandboxId(), SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN));
     
        // Get author sandbox
        sbInfo = sbService.getAuthorSandbox(wpInfo.getStoreId());
        
        assertTrue(sbService.isSandboxType(sbInfo.getSandboxId(), SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN));
        assertFalse(sbService.isSandboxType(sbInfo.getSandboxId(), SandboxConstants.PROP_SANDBOX_STAGING_MAIN));
    }
    
    public void testDeleteSandbox()
    {
        // Create web project - implicitly creates staging sandbox and also admin sandbox (author sandbox for web project creator)
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-delete", TEST_WEBPROJ_NAME+" delete", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        String wpStoreId = wpInfo.getStoreId();
        
        assertEquals(2, sbService.listSandboxes(wpStoreId).size());
        
        // Get staging sandbox
        SandboxInfo sbInfo = sbService.getStagingSandbox(wpStoreId);
        
        try
        {
            // Try to delete staging sandbox (-ve test)
            sbService.deleteSandbox(sbInfo.getSandboxId());
            fail("Shouldn't be able to delete staging sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        
        try
        {
            // Try to delete non-existant sandbox (-ve test)
            sbService.deleteSandbox("some-random-staging-sandbox");
            fail("Shouldn't be able to delete non-existant sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        
        // Get admin author sandbox
        sbInfo = sbService.getAuthorSandbox(wpInfo.getStoreId());
        sbService.deleteSandbox(sbInfo.getSandboxId());
        
        assertEquals(1, sbService.listSandboxes(wpInfo.getStoreId()).size());
        
        // Invite web users
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_MANAGER);
        wpService.inviteWebUser(wpStoreId, USER_TWO, WCMUtil.ROLE_CONTENT_PUBLISHER);
        wpService.inviteWebUser(wpStoreId, USER_THREE, WCMUtil.ROLE_CONTENT_REVIEWER, true);
        
        assertEquals(2, sbService.listSandboxes(wpStoreId).size());
        
        sbService.createAuthorSandbox(wpStoreId, USER_ONE);
        sbService.createAuthorSandbox(wpStoreId, USER_TWO);
        
        assertEquals(4, sbService.listSandboxes(wpStoreId).size());
        
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        assertEquals(2, sbService.listSandboxes(wpStoreId).size());
        
        sbInfo = sbService.getAuthorSandbox(wpStoreId);
        assertNotNull(sbInfo);
        
        // can delete own sandbox
        sbService.deleteSandbox(sbInfo.getSandboxId());
        
        assertEquals(1, sbService.listSandboxes(wpStoreId).size());
        
        sbInfo = sbService.getAuthorSandbox(wpStoreId);
        assertNull(sbInfo);
        
        // but not others
        try
        {
            // Try to delete another author's sandbox as a non-content manager (-ve test)
            sbService.deleteSandbox(wpInfo.getStoreId()+"--"+USER_THREE);
            fail("Shouldn't be able to delete another author's sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }  
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        assertEquals(3, sbService.listSandboxes(wpStoreId).size());
        
        // content manager can delete others
        sbInfo = sbService.getAuthorSandbox(wpStoreId, USER_THREE);
        sbService.deleteSandbox(sbInfo.getSandboxId());
        
        assertEquals(2, sbService.listSandboxes(wpStoreId).size());
    }
    
    // list changed (in this test, new) assets in user sandbox compared to staging sandbox
    public void testListNewItems1()
    {
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-listNewItems1", TEST_WEBPROJ_NAME+" listNewItems1", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        String wpStoreId = wpInfo.getStoreId();
        
        assertEquals(2, sbService.listSandboxes(wpStoreId).size());
        
        // add web app (in addition to default ROOT web app)
        String myWebApp = "myWebApp";
        wpService.createWebApp(wpStoreId, myWebApp, "this is my web app");
        
        // Invite web users
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_CONTRIBUTOR);
        SandboxInfo sbInfo = sbService.createAuthorSandbox(wpStoreId, USER_ONE);
        String userOneSandboxId = sbInfo.getSandboxId();
        
        wpService.inviteWebUser(wpStoreId, USER_TWO, WCMUtil.ROLE_CONTENT_PUBLISHER);
        sbInfo = sbService.createAuthorSandbox(wpStoreId, USER_TWO);
        String userTwoSandboxId = sbInfo.getSandboxId();
        
        wpService.inviteWebUser(wpStoreId, USER_THREE, WCMUtil.ROLE_CONTENT_MANAGER);
        sbService.createAuthorSandbox(wpStoreId, USER_THREE);
        
        wpService.inviteWebUser(wpStoreId, USER_FOUR, WCMUtil.ROLE_CONTENT_REVIEWER, true);
   
        assertEquals(6, sbService.listSandboxes(wpStoreId).size());
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        assertEquals(2, sbService.listSandboxes(wpStoreId).size());
        
        sbInfo = sbService.getAuthorSandbox(wpStoreId);
        String sbStoreId = sbInfo.getSandboxId();
        
        // no changes yet
        List<AssetInfo> assets = sbService.listChangedAll(sbStoreId, true);
        assertEquals(0, assets.size());
      
        String authorSandboxMyWebAppRelativePath = sbInfo.getSandboxRootPath() + "/" + myWebApp; // in this case, my web app is 'myWebApp'
        String authorSandboxDefaultWebAppRelativePath = sbInfo.getSandboxRootPath() + "/" + wpInfo.getDefaultWebApp(); // in this case, default web app is 'ROOT'

        assetService.createFile(sbStoreId, authorSandboxMyWebAppRelativePath, "myFile1", null);
        
        assets = sbService.listChangedAll(sbStoreId, false);
        assertEquals(1, assets.size());
        assertEquals("myFile1", assets.get(0).getName());
        
        assetService.createFolder(sbStoreId, authorSandboxDefaultWebAppRelativePath, "myDir1", null);
        assetService.createFile(sbStoreId, authorSandboxDefaultWebAppRelativePath+"/myDir1", "myFile2", null);
        assetService.createFolder(sbStoreId, authorSandboxDefaultWebAppRelativePath+"/myDir1", "myDir2", null);
        assetService.createFile(sbStoreId, authorSandboxDefaultWebAppRelativePath+"/myDir1/myDir2", "myFile3", null);
        assetService.createFile(sbStoreId, authorSandboxDefaultWebAppRelativePath+"/myDir1/myDir2", "myFile4", null);
        assetService.createFolder(sbStoreId, authorSandboxDefaultWebAppRelativePath+"/myDir1", "myDir3", null);
        
        assets = sbService.listChangedAll(sbStoreId, false);
        assertEquals(2, assets.size()); // new dir with new dirs/files is returned as single change
        
        for (AssetInfo asset : assets)
        {
            if (asset.getName().equals("myFile1") && asset.isFile())
            {
                continue;
            }
            else if (asset.getName().equals("myDir1") && asset.isFolder())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
        
        assets = sbService.listChangedWebApp(sbStoreId, wpInfo.getDefaultWebApp(), false);
        assertEquals(1, assets.size());
        
        for (AssetInfo asset : assets)
        {
            if (asset.getName().equals("myDir1") && asset.isFolder())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
        
        assets = sbService.listChanged(sbStoreId, authorSandboxDefaultWebAppRelativePath+"/myDir1", false);
        assertEquals(1, assets.size());
        
        for (AssetInfo asset : assets)
        {
            if (asset.getName().equals("myDir1") && asset.isFolder())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
      
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        assertEquals(2, sbService.listSandboxes(wpStoreId).size());
        
        try
        {
            // Content Contributor should not be able to list another user's changes (-ve test)
            assets = sbService.listChangedAll(userOneSandboxId, true);
            fail("Shouldn't allow non-content-manager to get modified list for another sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        
        // test roles
        
        // Switch to AuthenticationUtil.getAdminUserName()
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        assertEquals(6, sbService.listSandboxes(wpStoreId).size());
        
        // admin (Content Manager) should be able to list another user's changes
        assets = sbService.listChangedAll(userOneSandboxId, true);
        assertEquals(2, assets.size());
        
        // Switch to USER_THREE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_THREE);
        
        assertEquals(6, sbService.listSandboxes(wpStoreId).size());
        
        // Content Manager should be able to list another user's changes
        assets = sbService.listChangedAll(userOneSandboxId, true);
        assertEquals(2, assets.size());
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        try
        {
            // Content publisher - try to list changes in another user's sandbox (-ve test)
            assets = sbService.listChangedAll(userTwoSandboxId, true);
            fail("Shouldn't be able to list another author's sandbox changes");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        try
        {
            // Content contributor - try to list changes in another user's sandbox (-ve test)
            assets = sbService.listChangedAll(userOneSandboxId, true);
            fail("Shouldn't be able to list another author's sandbox changes");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        
        // Switch to USER_FOUR
        AuthenticationUtil.setFullyAuthenticatedUser(USER_FOUR);
        
        try
        {
            // Content reviewer - try to list changes in another user's sandbox (-ve test)
            assets = sbService.listChangedAll(userOneSandboxId, true);
            fail("Shouldn't be able to list another author's sandbox changes");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
    }
    
    // list changed (in this test, new) assets in two different user sandboxes compared to each other
    public void testListNewItems2()
    {
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-listNewItems2", TEST_WEBPROJ_NAME+" listNewItems2", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        String wpStoreId = wpInfo.getStoreId();

        // Invite web users
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_CONTRIBUTOR, true);
        wpService.inviteWebUser(wpStoreId, USER_TWO, WCMUtil.ROLE_CONTENT_CONTRIBUTOR, true);
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        SandboxInfo sbInfo1 = sbService.getAuthorSandbox(wpStoreId);
        String sbStoreId = sbInfo1.getSandboxId();
        
        List<AssetInfo> assets = sbService.listChangedAll(sbStoreId, true);
        assertEquals(0, assets.size());
        
        assetService.createFile(sbStoreId, sbInfo1.getSandboxRootPath(), "myFile1", null);
        
        assets = sbService.listChangedAll(sbStoreId, false);
        assertEquals(1, assets.size());
        assertEquals("myFile1", assets.get(0).getName());
        
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        SandboxInfo sbInfo2 = sbService.getAuthorSandbox(wpStoreId);
        sbStoreId = sbInfo2.getSandboxId();
        
        assets = sbService.listChangedAll(sbStoreId, true);
        assertEquals(0, assets.size());
        
        assetService.createFile(sbStoreId, sbInfo2.getSandboxRootPath(), "myFile2", null);
        assetService.createFile(sbStoreId, sbInfo2.getSandboxRootPath(), "myFile3", null);
        
        assets = sbService.listChangedAll(sbStoreId, false);
        assertEquals(2, assets.size());

        for (AssetInfo asset : assets)
        {
            if (asset.getName().equals("myFile2") && asset.isFile())
            {
                continue;
            }
            else if (asset.getName().equals("myFile3") && asset.isFile())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
        
        // Switch back to admin
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        sbInfo1 = sbService.getAuthorSandbox(wpStoreId, USER_ONE);
        sbInfo2 = sbService.getAuthorSandbox(wpStoreId, USER_TWO);
        
        assets = sbService.listChanged(sbInfo1.getSandboxId(), sbInfo1.getSandboxRootPath(), sbInfo2.getSandboxId(), sbInfo2.getSandboxRootPath(), false);
        assertEquals(1, assets.size());
        assertEquals("myFile1", assets.get(0).getName());
        
        assets = sbService.listChanged(sbInfo2.getSandboxId(), sbInfo2.getSandboxRootPath(), sbInfo1.getSandboxId(), sbInfo1.getSandboxRootPath(), false);
        assertEquals(2, assets.size());
        
        for (AssetInfo asset : assets)
        {
            if (asset.getName().equals("myFile2") && asset.isFile())
            {
                continue;
            }
            else if (asset.getName().equals("myFile3") && asset.isFile())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
    }
    
    /*
    // list changed (in this test, new) assets in two different user sandboxes compared to each other - without locking
    public void testListNewItems3()
    {
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-listNewItems2", TEST_WEBPROJ_NAME+" listNewItems2", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        String wpStoreId = wpInfo.getStoreId();

        // Invite web users
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_CONTRIBUTOR, true);
        wpService.inviteWebUser(wpStoreId, USER_TWO, WCMUtil.ROLE_CONTENT_CONTRIBUTOR, true);
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        SandboxInfo sbInfo1 = sbService.getAuthorSandbox(wpStoreId);
        String sbStoreId = sbInfo1.getSandboxId();
        
        List<AssetInfo> assets = sbService.listChangedAll(sbStoreId, true);
        assertEquals(0, assets.size());
      
        String authorSandboxRootPath = sbStoreId + AVM_STORE_SEPARATOR + sbInfo1.getSandboxRootPath();

        avmNonLockingAwareService.createFile(authorSandboxRootPath, "myFile1");
        
        assets = sbService.listChangedAll(sbStoreId, false);
        assertEquals(1, assets.size());
        assertEquals("myFile1", assets.get(0).getName());
        
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        SandboxInfo sbInfo2 = sbService.getAuthorSandbox(wpStoreId);
        sbStoreId = sbInfo2.getSandboxId();
        
        assets = sbService.listChangedAll(sbStoreId, true);
        assertEquals(0, assets.size());
      
        authorSandboxRootPath = sbStoreId + AVM_STORE_SEPARATOR + sbInfo2.getSandboxRootPath();

        avmNonLockingAwareService.createFile(authorSandboxRootPath, "myFile1"); // allowed, since using base (non-locking-aware) AVM service
        avmNonLockingAwareService.createFile(authorSandboxRootPath, "myFile2");
        avmNonLockingAwareService.createFile(authorSandboxRootPath, "myFile3");
        
        assets = sbService.listChangedAll(sbStoreId, false);
        assertEquals(3, assets.size());

        for (AssetInfo asset : assets)
        {
            if (asset.getName().equals("myFile1") && asset.isFile())
            {
                continue;
            }
            else if (asset.getName().equals("myFile2") && asset.isFile())
            {
                continue;
            }
            else if (asset.getName().equals("myFile3") && asset.isFile())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
        
        // Switch back to admin
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        sbInfo1 = sbService.getAuthorSandbox(wpStoreId, USER_ONE);
        sbInfo2 = sbService.getAuthorSandbox(wpStoreId, USER_TWO);
        
        assets = sbService.listChanged(sbInfo1.getSandboxId(), sbInfo1.getSandboxRootPath(), sbInfo2.getSandboxId(), sbInfo2.getSandboxRootPath(), false);
        assertEquals(1, assets.size());
        assertEquals("myFile1", assets.get(0).getName());
        
        assets = sbService.listChanged(sbInfo2.getSandboxId(), sbInfo1.getSandboxRootPath(), sbInfo1.getSandboxId(), sbInfo2.getSandboxRootPath(), false);
        assertEquals(3, assets.size());
        
        for (AssetInfo asset : assets)
        {
            if (asset.getName().equals("myFile1") && asset.isFile())
            {
                continue;
            }
            else if (asset.getName().equals("myFile2") && asset.isFile())
            {
                continue;
            }
            else if (asset.getName().equals("myFile3") && asset.isFile())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
    }
    */
    
    // submit new assets in user sandbox to staging sandbox
    public void testSubmitNewItems1() throws InterruptedException
    {
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-submitNewItems1", TEST_WEBPROJ_NAME+" submitNewItems1", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        
        String wpStoreId = wpInfo.getStoreId();
        String webApp = wpInfo.getDefaultWebApp();
        String stagingSandboxId = wpInfo.getStagingStoreName();
        
        // Invite web users
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_CONTRIBUTOR, true);
        wpService.inviteWebUser(wpStoreId, USER_TWO, WCMUtil.ROLE_CONTENT_PUBLISHER, true);
        wpService.inviteWebUser(wpStoreId, USER_THREE, WCMUtil.ROLE_CONTENT_MANAGER, true);
        wpService.inviteWebUser(wpStoreId, USER_FOUR, WCMUtil.ROLE_CONTENT_REVIEWER, true);
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        SandboxInfo sbInfo = sbService.getAuthorSandbox(wpStoreId);
        String authorSandboxId = sbInfo.getSandboxId();
        
        // no changes yet
        List<AssetInfo> assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
      
        String authorSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        
        assetService.createFile(authorSandboxId, authorSandboxPath, "myFile1", null);
        assetService.createFolder(authorSandboxId, authorSandboxPath, "myDir1", null);
        assetService.createFile(authorSandboxId, authorSandboxPath+"/myDir1", "myFile2", null);
        assetService.createFolder(authorSandboxId, authorSandboxPath+"/myDir1", "myDir2", null);
        assetService.createFile(authorSandboxId, authorSandboxPath+"/myDir1/myDir2", "myFile3", null);
        assetService.createFile(authorSandboxId, authorSandboxPath+"/myDir1/myDir2", "myFile4", null);
        assetService.createFolder(authorSandboxId, authorSandboxPath+"/myDir1", "myDir3", null);
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(2, assets.size()); // new dir with new dirs/files is returned as single change
        
        // check staging before
        String stagingSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        assertEquals(0, assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false).size());
        
        // submit (new assets) !
        sbService.submitWebApp(authorSandboxId, webApp, "a submit label", "a submit comment");
        
        Thread.sleep(SUBMIT_DELAY);
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(0, assets.size());
        
        // check staging after
        List<AssetInfo> listing = assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false);
        assertEquals(2, listing.size());
        
        for (AssetInfo asset : listing)
        {
            if (asset.getName().equals("myFile1") && asset.isFile())
            {
                continue;
            }
            else if (asset.getName().equals("myDir1") && asset.isFolder())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
        
        // test roles
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        // admin (content manager) can submit any sandbox
        String userSandboxId = wpStoreId + "--" + USER_THREE;
        sbService.submitAll(userSandboxId, "my submit", null);
        
        // Switch to USER_THREE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_THREE);
        
        // content manager can submit any (author) sandbox
        userSandboxId = wpStoreId + "--" + USER_ONE;
        sbService.submitAll(userSandboxId, "my submit", null);
     
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        try
        {
            // Content contributor - try to submit another user's sandbox (-ve test)
            userSandboxId = wpStoreId + "--" + USER_THREE;
            List<AssetInfo> noAssets = new ArrayList<AssetInfo>(0);
            sbService.submitListAssets(userSandboxId, noAssets, "my submit", null);
            fail("Shouldn't be able to submit another author's sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        try
        {
            // Content publisher - try to submit another user's sandbox (-ve test)
            userSandboxId = wpStoreId + "--" + USER_ONE;
            sbService.submitAll(userSandboxId, "my submit", null);
            fail("Shouldn't be able to submit another author's sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        
        // Switch to USER_FOUR
        AuthenticationUtil.setFullyAuthenticatedUser(USER_FOUR);
        
        try
        {
            // Content reviewer - try to submit another user's sandbox (-ve test)
            userSandboxId = TEST_SANDBOX+"-get" + "--" + USER_THREE;
            sbService.submitAll(userSandboxId, "my submit", null);
            fail("Shouldn't be able to submit another author's sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
    }
    
    // submit changed assets in user sandbox to staging sandbox
    public void testSubmitChangedAssets1() throws IOException, InterruptedException
    {
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-submitChangedAssets1", TEST_WEBPROJ_NAME+" submitChangedAssets1", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        
        final String wpStoreId = wpInfo.getStoreId();
        final String webApp = wpInfo.getDefaultWebApp();
        final String stagingSandboxId = wpInfo.getStagingStoreName();
        
        // Invite web users
        
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_CONTRIBUTOR, true);
        wpService.inviteWebUser(wpStoreId, USER_TWO, WCMUtil.ROLE_CONTENT_PUBLISHER, true);
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        SandboxInfo sbInfo = sbService.getAuthorSandbox(wpStoreId);
        String authorSandboxId = sbInfo.getSandboxId();
        
        // no changes yet
        List<AssetInfo> assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
        
        final String MYFILE1 = "This is myFile1";
        ContentWriter writer = assetService.createFileWebApp(authorSandboxId, webApp, "/", "myFile1");
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE1);
        
        assetService.createFolderWebApp(authorSandboxId, webApp, "/", "myDir1");
        
        final String MYFILE2 = "This is myFile2";
        writer = assetService.createFileWebApp(authorSandboxId, webApp, "/myDir1", "myFile2");
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE2);
                
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(2, assets.size());
        
        // check staging before
        String stagingSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        assertEquals(0, assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false).size());
        
        // submit (new assets) !
        sbService.submitWebApp(authorSandboxId, webApp, "a submit label", "a submit comment");
        
        Thread.sleep(SUBMIT_DELAY);
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(0, assets.size());
        
        // check staging after
        List<AssetInfo> listing = assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false);
        assertEquals(2, listing.size());
        
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        sbInfo = sbService.getAuthorSandbox(wpStoreId);
        authorSandboxId = sbInfo.getSandboxId();
        
        // no changes yet
        assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
        
        final String MYFILE1_MODIFIED = "This is myFile1 ... modified by "+USER_TWO;
        
        writer = assetService.getContentWriter(assetService.getAssetWebApp(authorSandboxId, webApp, "/myFile1"));
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE1_MODIFIED);

        final String MYFILE2_MODIFIED = "This is myFile2 ... modified by "+USER_TWO;
        writer = assetService.getContentWriter(assetService.getAssetWebApp(authorSandboxId, webApp, "/myDir1/myFile2"));
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE2_MODIFIED);
                
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(2, assets.size());
        
        // check staging before
        stagingSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        
        ContentReader reader = assetService.getContentReader(assetService.getAsset(stagingSandboxId, -1, stagingSandboxPath+"/myFile1", false));
        InputStream in = reader.getContentInputStream();
        byte[] buff = new byte[1024];
        in.read(buff);
        in.close();
        assertEquals(MYFILE1, new String(buff, 0, MYFILE1.length())); // assumes 1byte=1char
        
        reader = assetService.getContentReader(assetService.getAsset(stagingSandboxId, -1, stagingSandboxPath+"/myDir1/myFile2", false));
        in = reader.getContentInputStream();
        buff = new byte[1024];
        in.read(buff);
        in.close();
        assertEquals(MYFILE2, new String(buff, 0, MYFILE2.length()));
        
        // submit (modified assets) !
        sbService.submitWebApp(authorSandboxId, webApp, "my label", null);
        
        Thread.sleep(SUBMIT_DELAY);
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(0, assets.size());
        
        // check staging after
        reader = assetService.getContentReader(assetService.getAsset(stagingSandboxId, -1, stagingSandboxPath+"/myFile1", false));
        in = reader.getContentInputStream();
        buff = new byte[1024];
        in.read(buff);
        in.close();
        assertEquals(MYFILE1_MODIFIED, new String(buff, 0, MYFILE1_MODIFIED.length()));
        
        reader = assetService.getContentReader(assetService.getAsset(stagingSandboxId, -1, stagingSandboxPath+"/myDir1/myFile2", false));
        in = reader.getContentInputStream();
        buff = new byte[1024];
        in.read(buff);
        in.close();
        assertEquals(MYFILE2_MODIFIED, new String(buff, 0, MYFILE1_MODIFIED.length()));
    }
    
    // submit "all" changed assets in user sandbox to staging sandbox (not using default webapp)
    public void testSubmitChangedAssets2() throws IOException, InterruptedException
    {
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-submitChangedAssets1", TEST_WEBPROJ_NAME+" submitChangedAssets1", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        
        final String wpStoreId = wpInfo.getStoreId();
        final String stagingSandboxId = wpInfo.getStagingStoreName();
        
        SandboxInfo sbInfo = sbService.getAuthorSandbox(wpStoreId);
        String authorSandboxId = sbInfo.getSandboxId();
        
        String rootPath = sbInfo.getSandboxRootPath(); // currently /www/avm_webapps
        
        // no changes yet
        List<AssetInfo> assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
        
        assetService.createFolder(authorSandboxId, rootPath, "a", null);
        assetService.createFolder(authorSandboxId, rootPath+"/a", "b", null);
        assetService.createFolder(authorSandboxId, rootPath+"/a/b", "c", null);

        final String MYFILE1 = "This is foo";
        ContentWriter writer = assetService.createFile(authorSandboxId, rootPath+"/a/b/c", "foo", null);
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE1);
        
        final String MYFILE2 = "This is bar";
        writer = assetService.createFile(authorSandboxId, rootPath+"/a/b/c", "bar", null);
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE2);
                
        assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(1, assets.size());
        
        // check staging before
        assertEquals(1, assetService.listAssets(stagingSandboxId, -1, rootPath, false).size()); // note: currently includes default webapp ('ROOT')
        
        // submit (new assets) !
        sbService.submitAll(authorSandboxId, "a submit label", "a submit comment");
        
        Thread.sleep(SUBMIT_DELAY);
        
        // check staging after
        List<AssetInfo> listing = assetService.listAssets(stagingSandboxId, -1, rootPath, false);
        assertEquals(2, listing.size()); // 'a' and 'ROOT'
        
        // no changes in sandbox
        assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
        
        final String MYFILE3 = "This is figs";
        writer = assetService.createFile(authorSandboxId, rootPath, "figs", null);
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE3);
        
        final String MYFILE1_MODIFIED = "This is foo ... modified";
        writer = assetService.getContentWriter(assetService.getAsset(authorSandboxId, rootPath+"/a/b/c/foo"));
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE1_MODIFIED);
        
        assetService.deleteAsset(assetService.getAsset(authorSandboxId, rootPath+"/a/b/c/bar"));
                
        assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(3, assets.size());
        
        // check staging before
        listing = assetService.listAssets(stagingSandboxId, -1, rootPath, false);
        assertEquals(2, listing.size());  // 'a' and 'ROOT'
        
        // submit all (modified assets) !
        sbService.submitAll(authorSandboxId, "my label", null);
        
        Thread.sleep(SUBMIT_DELAY);
        
        assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
       
        // check staging after
        listing = assetService.listAssets(stagingSandboxId, -1, rootPath, false);
        assertEquals(3, listing.size());  // 'figs', 'a' and 'ROOT'
    }
    
    // submit deleted assets in user sandbox to staging sandbox
    public void testSubmitDeletedItems1() throws IOException, InterruptedException
    {
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-submitDeletedItems1", TEST_WEBPROJ_NAME+" submitDeletedItems1", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        
        final String wpStoreId = wpInfo.getStoreId();
        final String webApp = wpInfo.getDefaultWebApp();
        final String stagingSandboxId = wpInfo.getStagingStoreName();
        
        // Invite web users
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_CONTRIBUTOR, true);
        wpService.inviteWebUser(wpStoreId, USER_TWO, WCMUtil.ROLE_CONTENT_MANAGER, true); // note: publisher does not have permission to delete
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        SandboxInfo sbInfo = sbService.getAuthorSandbox(wpStoreId);
        String authorSandboxId = sbInfo.getSandboxId();
        
        // no changes yet
        List<AssetInfo> assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
      
        String authorSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        
        final String MYFILE1 = "This is myFile1";
        ContentWriter writer = assetService.createFile(authorSandboxId, authorSandboxPath, "myFile1", null);
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE1);
        
        assetService.createFolder(authorSandboxId, authorSandboxPath, "myDir1", null);
        assetService.createFolder(authorSandboxId, authorSandboxPath+"/myDir1", "myDir2", null);
        
        final String MYFILE2 = "This is myFile2";
        writer = assetService.createFile(authorSandboxId, authorSandboxPath+"/myDir1", "myFile2", null);
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE2);
                
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(2, assets.size());
        
        // check staging before
        String stagingSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        assertEquals(0, assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false).size());
        
        // submit (new assets) !
        sbService.submitWebApp(authorSandboxId, webApp, "a submit label", "a submit comment");
        
        Thread.sleep(SUBMIT_DELAY);
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(0, assets.size());
        
        // check staging after
        List<AssetInfo> listing = assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false);
        assertEquals(2, listing.size());
        
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        sbInfo = sbService.getAuthorSandbox(wpStoreId);
        authorSandboxId = sbInfo.getSandboxId();
        
        // no changes yet
        assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
      
        //authorSandboxWebppPath = authorSandboxId + AVM_STORE_SEPARATOR + sbInfo.getSandboxRootPath() + "/" + webApp;
        
        
        assetService.deleteAsset(assetService.getAssetWebApp(authorSandboxId, webApp, "myFile1"));
        assetService.deleteAsset(assetService.getAssetWebApp(authorSandboxId, webApp, "/myDir1/myDir2"));
                
        // do not list deleted
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(0, assets.size());
        
        // do list deleted
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, true);
        assertEquals(2, assets.size());
        
        // check staging before
        //stagingSandboxWebppPath = stagingSandboxId + AVM_STORE_SEPARATOR + sbInfo.getSandboxRootPath() + "/" + webApp;
        
        assertNotNull(assetService.getAssetWebApp(stagingSandboxId, webApp, "/myFile1"));
        assertNotNull(assetService.getAssetWebApp(stagingSandboxId, webApp, "/myDir1"));
        assertNotNull(assetService.getAssetWebApp(stagingSandboxId, webApp, "/myDir1/myDir2"));
        assertNotNull(assetService.getAssetWebApp(stagingSandboxId, webApp, "/myDir1/myFile2"));
        
        // submit (deleted assets) !
        sbService.submitWebApp(authorSandboxId, webApp, "my label", null);
        
        Thread.sleep(SUBMIT_DELAY);
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(0, assets.size());
        
        // check staging after
        assertNull(assetService.getAssetWebApp(stagingSandboxId, webApp, "/myFile1"));
        assertNull(assetService.getAssetWebApp(stagingSandboxId, webApp, "/myDir1/myDir2"));
        
        assertNotNull(assetService.getAssetWebApp(stagingSandboxId, webApp, "/myDir1"));
        assertNotNull(assetService.getAssetWebApp(stagingSandboxId, webApp, "/myDir1/myFile2"));
    }
    
    // revert (changed) assets in user sandbox
    public void testRevert() throws IOException, InterruptedException
    {
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-revertChangedAssets", TEST_WEBPROJ_NAME+" revertChangedAssets", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        
        final String wpStoreId = wpInfo.getStoreId();
        final String webApp = wpInfo.getDefaultWebApp();
        final String stagingSandboxId = wpInfo.getStagingStoreName();
        
        // Invite web users
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_CONTRIBUTOR, true);
        
        // TODO - pending fix for ETWOTWO-981
        //wpService.inviteWebUser(wpStoreId, USER_TWO, WCMUtil.ROLE_CONTENT_PUBLISHER, true);
        
        wpService.inviteWebUser(wpStoreId, USER_TWO, WCMUtil.ROLE_CONTENT_MANAGER, true);
        
        wpService.inviteWebUser(wpStoreId, USER_THREE, WCMUtil.ROLE_CONTENT_MANAGER, true);
        wpService.inviteWebUser(wpStoreId, USER_FOUR, WCMUtil.ROLE_CONTENT_REVIEWER, true);
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        SandboxInfo sbInfo = sbService.getAuthorSandbox(wpStoreId);
        String authorSandboxId = sbInfo.getSandboxId();
        
        // no changes yet
        List<AssetInfo> assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
      
        String authorSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        
        final String MYFILE1 = "This is myFile1";
        ContentWriter writer = assetService.createFile(authorSandboxId, authorSandboxPath, "myFile1", null);
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE1);
        
        assetService.createFolder(authorSandboxId, authorSandboxPath, "myDir1", null);
        
        final String MYFILE2 = "This is myFile2";
        writer = assetService.createFile(authorSandboxId, authorSandboxPath+"/myDir1", "myFile2", null);
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE2);
                
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(2, assets.size());
        
        // check staging before
        String stagingSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        assertEquals(0, assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false).size());
        
        // submit (new assets) !
        sbService.submitWebApp(authorSandboxId, webApp, "a submit label", "a submit comment");
        
        Thread.sleep(SUBMIT_DELAY);
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(0, assets.size());
        
        // check staging after
        List<AssetInfo> listing = assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false);
        assertEquals(2, listing.size());
        
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        sbInfo = sbService.getAuthorSandbox(wpStoreId);
        authorSandboxId = sbInfo.getSandboxId();
        
        // no changes yet
        assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
      
        //authorSandboxWebppPath = authorSandboxId + AVM_STORE_SEPARATOR + sbInfo.getSandboxRootPath() + "/" + webApp;
        
        final String MYFILE1_MODIFIED = "This is myFile1 ... modified by "+USER_TWO;
        writer = assetService.getContentWriter(assetService.getAssetWebApp(authorSandboxId, webApp, "/myFile1"));
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE1_MODIFIED);

        final String MYFILE2_MODIFIED = "This is myFile2 ... modified by "+USER_TWO;
        writer = assetService.getContentWriter(assetService.getAssetWebApp(authorSandboxId, webApp, "/myDir1/myFile2"));
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(MYFILE2_MODIFIED);
                
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(2, assets.size());
        
        // check staging before
        stagingSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        
        ContentReader reader = assetService.getContentReader(assetService. getAsset(stagingSandboxId, -1, stagingSandboxPath+"/myFile1", false));
        InputStream in = reader.getContentInputStream();
        byte[] buff = new byte[1024];
        in.read(buff);
        in.close();
        assertEquals(MYFILE1, new String(buff, 0, MYFILE1.length())); // assumes 1byte = 1char
        
        reader = assetService.getContentReader(assetService. getAsset(stagingSandboxId, -1, stagingSandboxPath+"/myDir1/myFile2", false));
        in = reader.getContentInputStream();
        buff = new byte[1024];
        in.read(buff);
        in.close();
        assertEquals(MYFILE2, new String(buff, 0, MYFILE2.length()));
        
        // revert (modified assets) !
        sbService.revertWebApp(authorSandboxId, webApp);
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(0, assets.size());
        
        // check staging after
        reader = assetService.getContentReader(assetService.getAsset(stagingSandboxId, -1, stagingSandboxPath+"/myFile1", false));
        in = reader.getContentInputStream();
        buff = new byte[1024];
        in.read(buff);
        in.close();
        assertEquals(MYFILE1, new String(buff, 0, MYFILE1.length()));
        
        reader = assetService.getContentReader(assetService.getAsset(stagingSandboxId, -1, stagingSandboxPath+"/myDir1/myFile2", false));
        in = reader.getContentInputStream();
        buff = new byte[1024];
        in.read(buff);
        in.close();
        assertEquals(MYFILE2, new String(buff, 0, MYFILE2.length()));
        
        // test roles
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        // admin (content manager) can revert any sandbox
        String userSandboxId = wpStoreId + "--" + USER_THREE;
        sbService.revertAll(userSandboxId);
        
        // Switch to USER_THREE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_THREE);
        
        // content manager can revert any (author) sandbox
        userSandboxId = wpStoreId + "--" + USER_ONE;
        sbService.revertAll(userSandboxId);
     
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        try
        {
            // Content contributor - try to revert another user's sandbox (-ve test)
            userSandboxId = wpStoreId + "--" + USER_THREE;
            List<AssetInfo> noAssets = new ArrayList<AssetInfo>(0);
            sbService.revertListAssets(userSandboxId, noAssets);
            fail("Shouldn't be able to revert another author's sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        
        // TODO - pending fix for ETWOTWO-981 - see above
        /*
        // Switch to USER_TWO
        AuthenticationUtil.setFullyAuthenticatedUser(USER_TWO);
        
        try
        {
            // Content publisher - try to revert another user's sandbox (-ve test)
            userSandboxId = wpStoreId + "--" + USER_ONE;
            sbService.revertAll(userSandboxId);
            fail("Shouldn't be able to revert another author's sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
        */
        
        // Switch to USER_FOUR
        AuthenticationUtil.setFullyAuthenticatedUser(USER_FOUR);
        
        try
        {
            // Content reviewer - try to revert another user's sandbox (-ve test)
            userSandboxId = TEST_SANDBOX+"-get" + "--" + USER_THREE;
            sbService.revertAll(userSandboxId);
            fail("Shouldn't be able to revert another author's sandbox");
        }
        catch (AccessDeniedException exception)
        {
            // Expected
        }
    }
    
    public void testListSnapshots() throws IOException, InterruptedException
    {
        Date fromDate = new Date();
        
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-listSnapshots", TEST_WEBPROJ_NAME+" listSnapshots", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        
        final String wpStoreId = wpInfo.getStoreId();
        final String webApp = wpInfo.getDefaultWebApp();
        final String stagingSandboxId = wpInfo.getStagingStoreName();
        
        // Invite web users
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_MANAGER, true);
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        SandboxInfo sbInfo = sbService.getAuthorSandbox(wpStoreId);
        String authorSandboxId = sbInfo.getSandboxId();
        
        // no changes yet
        List<AssetInfo> assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
      
        assetService.createFolderWebApp(authorSandboxId, webApp, "/", "myDir1");
        assetService.createFolderWebApp(authorSandboxId, webApp, "/", "myDir2");
        assetService.createFolderWebApp(authorSandboxId, webApp, "/", "myDir3");
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(3, assets.size());
        
        // check staging before
        String stagingSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        assertEquals(0, assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false).size());
        
        List<SandboxVersion> sbVersions = sbService.listSnapshots(stagingSandboxId, fromDate, new Date(), false);
        assertEquals(0, sbVersions.size());
        
        // submit (new assets) !
        sbService.submitWebApp(authorSandboxId, webApp, "a submit label", "a submit comment");
        
        Thread.sleep(SUBMIT_DELAY);
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(0, assets.size());
        
        // check staging after
        List<AssetInfo> listing = assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false);
        assertEquals(3, listing.size());
        
        sbVersions = sbService.listSnapshots(stagingSandboxId, fromDate, new Date(), false);
        assertEquals(1, sbVersions.size());
        
        // more changes ...
        assetService.createFolderWebApp(authorSandboxId, webApp, "/", "myDir4");

        // submit (new assets) !
        sbService.submitWebApp(authorSandboxId, webApp, "a submit label", "a submit comment");
        
        Thread.sleep(SUBMIT_DELAY);
        
        // check staging after
        listing = assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false);
        assertEquals(4, listing.size());
        
        sbVersions = sbService.listSnapshots(stagingSandboxId, fromDate, new Date(), false);
        assertEquals(2, sbVersions.size());
    }
   
    public void testRevertSnapshot() throws IOException, InterruptedException
    {
        Date fromDate = new Date();
        
        WebProjectInfo wpInfo = wpService.createWebProject(TEST_SANDBOX+"-revertSnapshot", TEST_WEBPROJ_NAME+" revertSnapshot", TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION);
        
        final String wpStoreId = wpInfo.getStoreId();
        final String webApp = wpInfo.getDefaultWebApp();
        final String stagingSandboxId = wpInfo.getStagingStoreName();
        
        // Start: Test ETWOTWO-817
        
        // Invite web users
        wpService.inviteWebUser(wpStoreId, USER_ONE, WCMUtil.ROLE_CONTENT_MANAGER, true);
        
        // Switch to USER_ONE
        AuthenticationUtil.setFullyAuthenticatedUser(USER_ONE);
        
        // Finish: Test ETWOTWO-817
        
        SandboxInfo sbInfo = sbService.getAuthorSandbox(wpStoreId);
        String authorSandboxId = sbInfo.getSandboxId();
        
        // no changes yet
        List<AssetInfo> assets = sbService.listChangedAll(authorSandboxId, true);
        assertEquals(0, assets.size());
      
        String authorSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        
        assetService.createFolder(authorSandboxId, authorSandboxPath, "myDir1", null);
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(1, assets.size());
        
        // check staging before
        String stagingSandboxPath = sbInfo.getSandboxRootPath() + "/" + webApp;
        assertEquals(0, assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false).size());
        
        List<SandboxVersion> sbVersions = sbService.listSnapshots(stagingSandboxId, fromDate, new Date(), false);
        assertEquals(0, sbVersions.size());
        
        // submit (new assets) !
        sbService.submitWebApp(authorSandboxId, webApp, "a submit label", "a submit comment");
        
        Thread.sleep(SUBMIT_DELAY);
        
        assets = sbService.listChangedWebApp(authorSandboxId, webApp, false);
        assertEquals(0, assets.size());
        
        // check staging after
        List<AssetInfo> listing = assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false);
        assertEquals(1, listing.size());
        for (AssetInfo asset : listing)
        {
            if (asset.getName().equals("myDir1") && asset.isFolder())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
        
        sbVersions = sbService.listSnapshots(stagingSandboxId, fromDate, new Date(), false);
        assertEquals(1, sbVersions.size());
        
        // more changes ...
        assetService.createFolder(authorSandboxId, authorSandboxPath, "myDir2", null);

        // submit (new assets) !
        sbService.submitWebApp(authorSandboxId, webApp, "a submit label", "a submit comment");
        
        Thread.sleep(SUBMIT_DELAY);
        
        // check staging after
        listing = assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false);
        assertEquals(2, listing.size());
        for (AssetInfo asset : listing)
        {
            if (asset.getName().equals("myDir1") && asset.isFolder())
            {
                continue;
            }
            else if (asset.getName().equals("myDir2") && asset.isFolder())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
        
        sbVersions = sbService.listSnapshots(stagingSandboxId, fromDate, new Date(), false);
        assertEquals(2, sbVersions.size());
        
        // more changes ...
        assetService.createFolderWebApp(authorSandboxId, webApp, "/", "myDir3");

        // submit (new assets) !
        sbService.submitWebApp(authorSandboxId, webApp, "a submit label", "a submit comment");
        
        Thread.sleep(SUBMIT_DELAY);
        
        // check staging after
        listing = assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false);
        assertEquals(3, listing.size());
        for (AssetInfo asset : listing)
        {
            if (asset.getName().equals("myDir1") && asset.isFolder())
            {
                continue;
            }
            else if (asset.getName().equals("myDir2") && asset.isFolder())
            {
                continue;
            }
            else if (asset.getName().equals("myDir3") && asset.isFolder())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
        
        sbVersions = sbService.listSnapshots(stagingSandboxId, fromDate, new Date(), false);
        assertEquals(3, sbVersions.size());
        
        // revert to snapshot ...
        
        SandboxVersion version = sbVersions.get(1);
        int versionId = version.getVersion();
        
        sbService.revertSnapshot(stagingSandboxId, versionId);
        
        sbVersions = sbService.listSnapshots(stagingSandboxId, fromDate, new Date(), false);
        assertEquals(4, sbVersions.size());
        
        // check staging after
        listing = assetService.listAssets(stagingSandboxId, -1, stagingSandboxPath, false);
        assertEquals(2, listing.size());
        for (AssetInfo asset : listing)
        {
            if (asset.getName().equals("myDir1") && asset.isFolder())
            {
                continue;
            }
            else if (asset.getName().equals("myDir2") && asset.isFolder())
            {
                continue;
            }
            else
            {
                fail("The asset '" + asset.getName() + "' is not recognised");
            }
        }
    }
    
    public void testPseudoScaleTest()
    {
        long start = System.currentTimeMillis();
        
        long split = start;
        
        for (int i = 1; i <= SCALE_USERS; i++)
        {
            createUser(TEST_USER+"-"+i);
        }
        
        System.out.println("testPseudoScaleTest: created "+SCALE_USERS+" users in "+(System.currentTimeMillis()-split)+" msecs");
        
        split = System.currentTimeMillis();
        
        for (int i = 1; i <= SCALE_WEBPROJECTS; i++)
        {
            wpService.createWebProject(TEST_SANDBOX+"-"+i, TEST_WEBPROJ_NAME+"-"+i, TEST_WEBPROJ_TITLE, TEST_WEBPROJ_DESCRIPTION); // ignore return
        }
        
        System.out.println("testPseudoScaleTest: created "+SCALE_WEBPROJECTS+" web projects in "+(System.currentTimeMillis()-split)+" msecs");
        
        split = System.currentTimeMillis();
        
        for (int i = 1; i <= SCALE_WEBPROJECTS; i++)
        {
            WebProjectInfo wpInfo = wpService.getWebProject(TEST_SANDBOX+"-"+i);
            Map<String, String> userRoles = new HashMap<String, String>(SCALE_USERS);
            for (int j = 1; j <= SCALE_USERS; j++)
            {
                userRoles.put(TEST_USER+"-"+j, WCMUtil.ROLE_CONTENT_MANAGER);
            }
            wpService.inviteWebUsersGroups(wpInfo.getNodeRef(), userRoles, true);
        }
        
        System.out.println("testPseudoScaleTest: invited "+SCALE_USERS+" content managers (and created user sandboxes) to each of "+SCALE_WEBPROJECTS+" web projects in "+(System.currentTimeMillis()-split)+" msecs");
        
        split = System.currentTimeMillis();
        
        for (int i = 1; i <= SCALE_WEBPROJECTS; i++)
        {
            WebProjectInfo wpInfo = wpService.getWebProject(TEST_SANDBOX+"-"+i);
            assertEquals(SCALE_USERS+2, sbService.listSandboxes(wpInfo.getStoreId()).size()); // including staging sandbox and admin sandbox (web project creator)
        }

        System.out.println("testPseudoScaleTest: list sandboxes for admin for each of "+SCALE_WEBPROJECTS+" web projects in "+(System.currentTimeMillis()-split)+" msecs");

        split = System.currentTimeMillis();
        
        for (int i = 1; i <= SCALE_WEBPROJECTS; i++)
        {
            WebProjectInfo wpInfo = wpService.getWebProject(TEST_SANDBOX+"-"+i);
            assertEquals(SCALE_USERS+1, wpService.listWebUsers(wpInfo.getStoreId()).size()); // including admin user (web project creator)
        }

        System.out.println("testPseudoScaleTest: list web users for admin for each of "+SCALE_WEBPROJECTS+" web projects in "+(System.currentTimeMillis()-split)+" msecs");

        split = System.currentTimeMillis();

        for (int i = 1; i <= SCALE_WEBPROJECTS; i++)
        {
            WebProjectInfo wpInfo = wpService.getWebProject(TEST_SANDBOX+"-"+i);
            
            for (int j = 1; j <= SCALE_USERS; j++)
            {
                AuthenticationUtil.setFullyAuthenticatedUser(TEST_USER+"-"+j);
                assertEquals(SCALE_USERS+2, sbService.listSandboxes(wpInfo.getStoreId()).size()); // including staging sandbox and admin sandbox (web project creator)
            }
            AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        } 
        
        System.out.println("testPseudoScaleTest: list sandboxes for "+SCALE_USERS+" content managers for each of "+SCALE_WEBPROJECTS+" web projects in "+(System.currentTimeMillis()-split)+" msecs");
        
        split = System.currentTimeMillis();

        for (int i = 1; i <= SCALE_WEBPROJECTS; i++)
        {
            WebProjectInfo wpInfo = wpService.getWebProject(TEST_SANDBOX+"-"+i);
            
            for (int j = 1; j <= SCALE_USERS; j++)
            {
                AuthenticationUtil.setFullyAuthenticatedUser(TEST_USER+"-"+j);
                assertEquals(SCALE_USERS+1, wpService.listWebUsers(wpInfo.getStoreId()).size()); // including admin user (web project creator)
            }
            AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        }
        
        System.out.println("testPseudoScaleTest: list web users for "+SCALE_USERS+" content managers for each of "+SCALE_WEBPROJECTS+" web projects in "+(System.currentTimeMillis()-split)+" msecs");
      
        split = System.currentTimeMillis();
        
        for (int i = 1; i <= SCALE_WEBPROJECTS; i++)
        {
            WebProjectInfo wpInfo = wpService.getWebProject(TEST_SANDBOX+"-"+i);
            
            for (int j = 1; j <= SCALE_USERS; j++)
            {
                SandboxInfo sbInfo = sbService.getAuthorSandbox(wpInfo.getStoreId(), TEST_USER+"-"+j);
                sbService.deleteSandbox(sbInfo.getSandboxId());
            }
        }
        
        System.out.println("testPseudoScaleTest: deleted "+SCALE_USERS+" author sandboxes for each of "+SCALE_WEBPROJECTS+" web projects in "+(System.currentTimeMillis()-split)+" msecs");

        split = System.currentTimeMillis();
        
        for (int i = 1; i <= SCALE_WEBPROJECTS; i++)
        {
            WebProjectInfo wpInfo = wpService.getWebProject(TEST_SANDBOX+"-"+i);
            wpService.deleteWebProject(wpInfo.getNodeRef());
        }
        
        System.out.println("testPseudoScaleTest: deleted "+SCALE_WEBPROJECTS+" web projects in "+(System.currentTimeMillis()-split)+" msecs");

        split = System.currentTimeMillis();
        
        for (int i = 1; i <= SCALE_USERS; i++)
        {
            deleteUser(TEST_USER+"-"+i);
        }
        
        System.out.println("testPseudoScaleTest: deleted "+SCALE_USERS+" users in "+(System.currentTimeMillis()-split)+" msecs");
    }
    
        
    /*
    // == Test the JavaScript API ==
    
    public void testJSAPI() throws Exception
    {
        ScriptLocation location = new ClasspathScriptLocation("org/alfresco/wcm/script/test_sandboxService.js");
        scriptService.executeScript(location, new HashMap<String, Object>(0));
    }
    */
}
