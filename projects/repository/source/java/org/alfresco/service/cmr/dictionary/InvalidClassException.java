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
package org.alfresco.service.cmr.dictionary;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.namespace.QName;

/**
 * Thrown when an operation cannot be performed because the dictionary class 
 * reference does not exist.
 * 
 */
public class InvalidClassException extends AlfrescoRuntimeException
{
    private static final long serialVersionUID = 3256722870754293558L;

    private QName className;
    
    public InvalidClassException(QName className)
    {
        this(null, className);
    }

    public InvalidClassException(String msg, QName className)
    {
        super(msg);
        this.className = className;
    }

    /**
     * @return Returns the offending class name
     */
    public QName getClassName()
    {
        return className;
    }
}
