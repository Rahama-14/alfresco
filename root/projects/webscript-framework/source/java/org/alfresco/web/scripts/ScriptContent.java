/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.scripts;

import java.io.InputStream;
import java.io.Reader;


/**
 * Web Script Content
 * 
 * @author davidc
 */
public interface ScriptContent
{
    /**
     * Gets an input stream to the contents of the script
     * 
     * @return  the input stream
     */
    InputStream getInputStream();
    
    /**
     * Gets a reader to the contents of the script
     * 
     * @return  the reader
     */
    Reader getReader();

    /**
     * Gets the path to the content
     * 
     * @return  path
     */
    public String getPath();
    
    /**
     * Gets path description
     * 
     * @return  human readable version of path
     */
    public String getPathDescription();
    
    /**
     * Returns true if the script content is considered cachedable - i.e. classpath located or similar.
     * Else the content will be compiled/interpreted on every execution i.e. repo content.
     * 
     * @return true if the script content is considered cachedable, false otherwise
     */
    boolean isCachable();
    
    /**
     * Returns true if the script location is considered secure - i.e. on the app-server classpath.
     * Secure scripts may access java.* libraries and instantiate pure Java objects directly. Unsecure
     * scripts only have access to pre-configure host objects and cannot access java.* libs.
     * 
     * @return true if the script location is considered secure
     */
    boolean isSecure();
}
