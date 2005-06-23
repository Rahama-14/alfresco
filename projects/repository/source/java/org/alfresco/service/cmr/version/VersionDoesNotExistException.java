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
package org.alfresco.service.cmr.version;

import java.text.MessageFormat;


/**
 * Version does not exist exception class.
 * 
 * @author Roy Wetherall
 */
public class VersionDoesNotExistException extends VersionServiceException
{
    private static final long serialVersionUID = 3258133548417233463L;
    private static final String ERROR_MESSAGE = "The version with label {0} does not exisit in the version store.";

    /**
     * Constructor
     */
    public VersionDoesNotExistException(String versionLabel)
    {
        super(MessageFormat.format(ERROR_MESSAGE, new Object[]{versionLabel}));
    }
}
