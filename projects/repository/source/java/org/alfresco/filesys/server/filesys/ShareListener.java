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

import org.alfresco.filesys.server.SrvSession;

/**
 * <p>
 * The share listener interface provides a hook into the server so that an application is notified
 * when a session connects/disconnects from a particular share.
 */
public interface ShareListener
{

    /**
     * Called when a session connects to a share
     * 
     * @param sess SrvSession
     * @param tree TreeConnection
     */
    public void shareConnect(SrvSession sess, TreeConnection tree);

    /**
     * Called when a session disconnects from a share
     * 
     * @param sess SrvSession
     * @param tree TreeConnection
     */
    public void shareDisconnect(SrvSession sess, TreeConnection tree);
}
