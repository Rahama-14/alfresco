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
package org.alfresco.filesys.smb.server;

/**
 * Find First Flags Class
 */
class Find
{
    // Find first flags

    protected static final int CloseSearch = 0x01;
    protected static final int CloseSearchAtEnd = 0x02;
    protected static final int ResumeKeysRequired = 0x04;
    protected static final int ContinuePrevious = 0x08;
    protected static final int BackupIntent = 0x10;
}
