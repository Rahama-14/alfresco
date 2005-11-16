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
package org.alfresco.util;


import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthenticationService;

/**
 * Base Alfresco test.
 * 
 * Creates a store and root node that can be used in the tests.
 * 
 * @author Roy Wetherall
 */
public class BaseAlfrescoTest extends BaseSpringTest
{
    /** The node service */
    protected NodeService nodeService;
    
    /** The content service */
    protected ContentService contentService;
    
    /** The authentication service */
    protected AuthenticationService authenticationService;
    
    /** The store reference */
    protected StoreRef storeRef;
    
    /** The root node reference */
    protected NodeRef rootNodeRef;
    
    /**
     * On setup in transaction override
     */
    @Override
    protected void onSetUpInTransaction() throws Exception
    {
        super.onSetUpInTransaction();
        
        // Get a reference to the node service
        this.nodeService = (NodeService)this.applicationContext.getBean("nodeService");
        this.contentService = (ContentService)this.applicationContext.getBean("contentService");
        this.authenticationService = (AuthenticationService)this.applicationContext.getBean("authenticationService");
        
        // Create the store and get the root node
        this.storeRef = this.nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
        this.rootNodeRef = this.nodeService.getRootNode(this.storeRef);
        
        // Authenticate as the admin user
        
        try
        {
           authenticationService.authenticate("admin", "admin".toCharArray());
        }
        catch(AuthenticationException e)
        {
            authenticationService.createAuthentication("admin", "admin".toCharArray());
            authenticationService.authenticate("admin", "admin".toCharArray());
        }
    }

    @Override
    protected void onTearDownInTransaction()
    {
        authenticationService.clearCurrentSecurityContext();
        super.onTearDownInTransaction();
    }
    
    
    
    
}
