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
package org.alfresco.repo.ownable.impl;

import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

public class OwnableServiceTest extends TestCase
{
    private static ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();

    private NodeService nodeService;

    private AuthenticationService authenticationService;
    
    private AuthenticationComponent authenticationComponent;

    private OwnableService ownableService;

    private NodeRef rootNodeRef;

    private UserTransaction userTransaction;
    
    private PermissionService permissionService;
    
    private OwnerDynamicAuthority dynamicAuthority;
    
    public OwnableServiceTest()
    {
        super();
    }

    public OwnableServiceTest(String arg0)
    {
        super(arg0);
    }
    
    public void setUp() throws Exception
    {
        nodeService = (NodeService) ctx.getBean("nodeService");
        authenticationService = (AuthenticationService) ctx.getBean("authenticationService");
        authenticationComponent = (AuthenticationComponent) ctx.getBean("authenticationComponent");
        ownableService = (OwnableService) ctx.getBean("ownableService");
        permissionService = (PermissionService) ctx.getBean("permissionService");
    
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());
        
        TransactionService transactionService = (TransactionService) ctx.getBean(ServiceRegistry.TRANSACTION_SERVICE.getLocalName());
        userTransaction = transactionService.getUserTransaction();
        userTransaction.begin();
        
        StoreRef storeRef = nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
        rootNodeRef = nodeService.getRootNode(storeRef);
        permissionService.setPermission(rootNodeRef, PermissionService.ALL_AUTHORITIES, PermissionService.ADD_CHILDREN, true);
        
        if(authenticationComponent.exists("andy"))
        {
            authenticationService.deleteAuthentication("andy");
        }
        authenticationService.createAuthentication("andy", "andy".toCharArray());
        
        dynamicAuthority = new OwnerDynamicAuthority();
        dynamicAuthority.setOwnableService(ownableService);
       
        authenticationComponent.clearCurrentSecurityContext();
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        authenticationComponent.clearCurrentSecurityContext();
        userTransaction.rollback();
        super.tearDown();
    }
    
    public void testSetup()
    {
        assertNotNull(nodeService);
        assertNotNull(authenticationService);
        assertNotNull(ownableService);
    }
    
    public void testUnSet()
    {
        assertNull(ownableService.getOwner(rootNodeRef));
        assertFalse(ownableService.hasOwner(rootNodeRef));
    }
    
    public void testCMObject()
    {
        authenticationService.authenticate("andy", "andy".toCharArray());
        NodeRef testNode = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, ContentModel.TYPE_PERSON, ContentModel.TYPE_CMOBJECT, null).getChildRef();
        permissionService.setPermission(rootNodeRef, "andy", PermissionService.TAKE_OWNERSHIP, true);
        assertEquals("andy", ownableService.getOwner(testNode));
        assertTrue(ownableService.hasOwner(testNode));
        assertTrue(nodeService.hasAspect(testNode, ContentModel.ASPECT_AUDITABLE));
        assertFalse(nodeService.hasAspect(testNode, ContentModel.ASPECT_OWNABLE));
        assertTrue(dynamicAuthority.hasAuthority(testNode, "andy"));
        
        assertEquals(AccessStatus.ALLOWED, permissionService.hasPermission(rootNodeRef, PermissionService.TAKE_OWNERSHIP));
        assertEquals(AccessStatus.ALLOWED, permissionService.hasPermission(rootNodeRef, PermissionService.SET_OWNER));
        
        ownableService.setOwner(testNode, "muppet");
        assertEquals("muppet", ownableService.getOwner(testNode));
        ownableService.takeOwnership(testNode);
        assertEquals("andy", ownableService.getOwner(testNode));
        assertTrue(nodeService.hasAspect(testNode, ContentModel.ASPECT_AUDITABLE));
        assertTrue(nodeService.hasAspect(testNode, ContentModel.ASPECT_OWNABLE));
        assertTrue(dynamicAuthority.hasAuthority(testNode, "andy"));
    }
    
    public void testContainer()
    {  
        authenticationService.authenticate("andy", "andy".toCharArray());
        NodeRef testNode = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, ContentModel.TYPE_PERSON, ContentModel.TYPE_CONTAINER, null).getChildRef();
        assertNull(ownableService.getOwner(testNode));
        assertFalse(ownableService.hasOwner(testNode));
        assertFalse(nodeService.hasAspect(testNode, ContentModel.ASPECT_AUDITABLE));
        assertFalse(nodeService.hasAspect(testNode, ContentModel.ASPECT_OWNABLE));
        assertFalse(dynamicAuthority.hasAuthority(testNode, "andy"));
        
        assertFalse(permissionService.hasPermission(testNode, PermissionService.READ) == AccessStatus.ALLOWED);
        assertFalse(permissionService.hasPermission(testNode, permissionService.getAllPermission()) == AccessStatus.ALLOWED);
        
        permissionService.setPermission(rootNodeRef, permissionService.getOwnerAuthority(), permissionService.getAllPermission(), true);
        
        ownableService.setOwner(testNode, "muppet");
        assertEquals("muppet", ownableService.getOwner(testNode));
        ownableService.takeOwnership(testNode);
        assertEquals("andy", ownableService.getOwner(testNode));
        assertFalse(nodeService.hasAspect(testNode, ContentModel.ASPECT_AUDITABLE));
        assertTrue(nodeService.hasAspect(testNode, ContentModel.ASPECT_OWNABLE));
        assertTrue(dynamicAuthority.hasAuthority(testNode, "andy"));
        
        assertTrue(permissionService.hasPermission(testNode, PermissionService.READ) == AccessStatus.ALLOWED);
        assertTrue(permissionService.hasPermission(testNode, permissionService.getAllPermission())== AccessStatus.ALLOWED);
        
        
    }
    
}
