/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.util;


import junit.framework.TestCase;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.transaction.TransactionService;
import org.springframework.context.ApplicationContext;

/**
 * Base Alfresco test.
 * 
 * Creates a store and root node that can be used in the tests.
 * 
 * @author Roy Wetherall
 */
public abstract class BaseAlfrescoTestCase extends TestCase
{
    /**
     * This context will be fetched each time, but almost always
     *  will have been cached by {@link ApplicationContextHelper}
     */
    protected ApplicationContext ctx;

    /** the service registry */
    protected ServiceRegistry serviceRegistry;
    
    /** The node service */
    protected NodeService nodeService;
    
    /** The content service */
    protected ContentService contentService;
    
    /** The authentication component */
    protected AuthenticationComponent authenticationComponent;
    
    /** The store reference */
    protected StoreRef storeRef;
    
    /** The root node reference */
    protected NodeRef rootNodeRef;
    
    
    protected ActionService actionService;
    protected TransactionService transactionService;
    
    /**
     * By default will return the full context.
     * Override this if your test needs a different one.
     */
    protected void setUpContext()
    {
       // Fetch the default, full context
       ctx = ApplicationContextHelper.getApplicationContext();
    }
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        setUpContext();

        // get the service register
        this.serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        //Get a reference to the node service
        this.nodeService = serviceRegistry.getNodeService();
        this.contentService = serviceRegistry.getContentService();
        this.authenticationComponent = (AuthenticationComponent)ctx.getBean("authenticationComponent");
        this.actionService = (ActionService)ctx.getBean("actionService");
        this.transactionService = serviceRegistry.getTransactionService();
        
        // Authenticate as the system user - this must be done before we create the store
        authenticationComponent.setSystemUserAsCurrentUser();
        
        // Create the store and get the root node
        this.storeRef = this.nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
        this.rootNodeRef = this.nodeService.getRootNode(this.storeRef);
        
       
    }
    
    
    @Override
    protected void tearDown() throws Exception
    {
        try
        {
            authenticationComponent.clearCurrentSecurityContext();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            // Don't let this mask any previous exceptions
        }
        super.tearDown();
    }
    
}