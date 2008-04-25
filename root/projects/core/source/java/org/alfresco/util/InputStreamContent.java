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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.springframework.util.FileCopyUtils;


/**
 * Input Stream based Content
 */
public class InputStreamContent implements Content, Serializable
{
    private static final long serialVersionUID = -7729633986840536282L;
    
    private InputStream stream;    
    private String mimetype;
    private String encoding;
    
    
    /**
     * Constructor
     * 
     * @param stream    content input stream
     * @param mimetype  content mimetype
     */
    public InputStreamContent(InputStream stream, String mimetype, String encoding)
    {
        this.stream = stream;
        this.mimetype = mimetype;
        this.encoding = encoding;
    }

    /* (non-Javadoc)
     * @see org.alfresco.util.Content#getContent()
     */
    public String getContent()
        throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        FileCopyUtils.copy(stream, os);  // both streams are closed
        byte[] bytes = os.toByteArray();
        // get the encoding for the string
        String encoding = getEncoding();
        // create the string from the byte[] using encoding if necessary
        String content = (encoding == null) ? new String(bytes) : new String(bytes, encoding);
        // done
        return content;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.util.Content#getInputStream()
     */
    public InputStream getInputStream()
    {
        return stream;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.util.Content#getSize()
     */
    public long getSize()
    {
        return -1;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.util.Content#getMimetype()
     */
    public String getMimetype()
    {
        return mimetype;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.util.Content#getEncoding()
     */
    public String getEncoding()
    {
        return encoding;
    }

}