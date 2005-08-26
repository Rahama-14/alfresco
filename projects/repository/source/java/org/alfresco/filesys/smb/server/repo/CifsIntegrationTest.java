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
package org.alfresco.filesys.smb.server.repo;

import junit.framework.TestCase;

import org.alfresco.filesys.CIFSServer;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

/**
 * Checks that the required configuration details are obtainable from the CIFS components.
 * 
 * @author Derek Hulley
 */
public class CifsIntegrationTest extends TestCase
{
    /** the context to keep between tests */
    public static ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
    
    public void testGetServerName()
    {
        CIFSServer cifsServer = (CIFSServer) ctx.getBean("cifsServer");
        assertNotNull("No CIFS server available", cifsServer);
        // the server might, quite legitimately, not start
        if (!cifsServer.isStarted())
        {
            return;
        }
        
        // get the server name
        String serverName = cifsServer.getConfiguration().getServerName();
        assertNotNull("No server name available", serverName);
        assertTrue("No server name available (zero length)", serverName.length() > 0);

        // check the disk interface
        ContentDiskInterface diskInterface = (ContentDiskInterface) ctx.getBean("contentDiskDriver");
        assertNotNull("No content disk interface found", diskInterface);
        
        // get the root share name
        String shareName = diskInterface.getShareName();
        assertNotNull("No share name available", shareName);
        assertTrue("No share name available (zero length)", shareName.length() > 0);
        
        NodeService nodeService = (NodeService) ctx.getBean(ServiceRegistry.NODE_SERVICE.getLocalName());
        // get the share root node and check that it exists
        NodeRef shareNodeRef = diskInterface.getContextRootNodeRef();
        assertNotNull("No share root node available", shareNodeRef);
        assertTrue("Share root node doesn't exist", nodeService.exists(shareNodeRef));
    }
}
