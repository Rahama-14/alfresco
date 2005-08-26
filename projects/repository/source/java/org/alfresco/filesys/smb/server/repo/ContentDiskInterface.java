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

import org.alfresco.filesys.server.filesys.DiskInterface;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Extended {@link org.alfresco.filesys.server.filesys.DiskInterface disk interface} to
 * allow access to some of the internal configuration properties.
 * 
 * @author Derek Hulley
 */
public interface ContentDiskInterface extends DiskInterface
{
    /**
     * Get the name of the shared path within the server.  The share name is
     * equivalent in browse path to the {@link #getContextRootNodeRef() context root}.
     * 
     * @return Returns the share name
     */
    public String getShareName();
    
    /**
     * Get a reference to the node that all CIFS paths are relative to
     *    
     * @return Returns a node acting as the CIFS root
     */
    public NodeRef getContextRootNodeRef();
}
