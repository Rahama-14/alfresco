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
package org.alfresco.repo.content.transform;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Allows direct streaming from source to target when the respective mimetypes
 * are identical, except where the mimetype is text.
 * <p>
 * Text has to be transformed based on the encoding even if the mimetypes don't
 * reflect it. 
 * 
 * @see org.alfresco.repo.content.transform.StringExtractingContentTransformer
 * 
 * @author Derek Hulley
 */
public class BinaryPassThroughContentTransformer extends AbstractContentTransformer2
{
    @SuppressWarnings("unused")
    private static final Log logger = LogFactory.getLog(BinaryPassThroughContentTransformer.class);

    @Override
    protected void transformInternal(ContentReader reader,
            ContentWriter writer, TransformationOptions options)
            throws Exception
    {
        // just stream it
        writer.putContent(reader.getContentInputStream());
        
    }

    public boolean isTransformable(String sourceMimetype,
            String targetMimetype, TransformationOptions options)
    {
        if (sourceMimetype.startsWith(StringExtractingContentTransformer.PREFIX_TEXT))
        {
            // we can only stream binary content through
            return false;
        }
        else if (!sourceMimetype.equals(targetMimetype))
        {
            // no transformation is possible so formats must be exact
            return false;
        }
        else
        {
            // formats are the same and are not text
            return true;
        }
    }
}
