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

import java.io.IOException;
import java.io.InputStream;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.textmining.extraction.TextExtractor;
import org.textmining.extraction.word.WordTextExtractorFactory;

/**
 * Makes use of the {@link http://www.textmining.org/ TextMining} library to
 * perform conversions from MSWord documents to text.
 * 
 * @author Derek Hulley
 */
public class TextMiningContentTransformer extends AbstractContentTransformer2
{
    private WordTextExtractorFactory wordExtractorFactory;
    
    public TextMiningContentTransformer()
    {
        this.wordExtractorFactory = new WordTextExtractorFactory();
    }
    
    /**
     * Currently the only transformation performed is that of text extraction from Word documents.
     */
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        if (!MimetypeMap.MIMETYPE_WORD.equals(sourceMimetype) ||
                !MimetypeMap.MIMETYPE_TEXT_PLAIN.equals(targetMimetype))
        {
            // only support DOC -> Text
            return false;
        }
        else
        {
            return true;
        }
    }

    public void transformInternal(ContentReader reader, ContentWriter writer,  TransformationOptions options)
            throws Exception
    {
        InputStream is = null;
        String text = null;
        try
        {
            is = reader.getContentInputStream();
            TextExtractor te = wordExtractorFactory.textExtractor(is);
            text = te.getText();
        }
        catch (IOException e)
        {
            // check if this is an error caused by the fact that the .doc is in fact
            // one of Word's temp non-documents
            if (e.getMessage().contains("Unable to read entire header"))
            {
                // just assign an empty string
                text = "";
            }
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }
        // dump the text out.  This will close the writer automatically.
        writer.putContent(text);
    }
}
