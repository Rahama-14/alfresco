/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.filesys.server.filesys;

import java.io.FileNotFoundException;

import org.alfresco.filesys.server.SrvSession;

/**
 * File Id Interface
 * <p>
 * Optional interface that a DiskInterface driver can implement to provide file id to path
 * conversion.
 */
public interface FileIdInterface
{

    /**
     * Convert a file id to a share relative path
     * 
     * @param sess SrvSession
     * @param tree TreeConnection
     * @param dirid int
     * @param fileid
     * @return String
     * @exception FileNotFoundException
     */
    public String buildPathForFileId(SrvSession sess, TreeConnection tree, int dirid, int fileid)
            throws FileNotFoundException;
}
