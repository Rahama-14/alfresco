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
package org.alfresco.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;


/**
 * Content
 * 
 * @author dcaruana
 */
public interface Content
{
	/**
	 * Gets content as a string
	 * 
	 * @return  content as a string
	 * @throws IOException
	 */
    public String getContent() throws IOException;

    /**
     * Gets the content mimetype
     * 
     * @return mimetype
     */
    public String getMimetype();
    
    /**
     * Gets the content encoding
     * 
     * @return  encoding
     */
    public String getEncoding();
    
    /**
     * Gets the content length (in bytes)
     * 
     * @return  length
     */
    public long getSize();

    /**
     * Gets the content input stream
     * 
     * @return  input stream
     */
    public InputStream getInputStream();

    /**
     * Gets the content reader (which is sensitive to encoding)
     * 
     * @return
     */
    public Reader getReader() throws IOException;
}
