/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.content.transform;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Converts any textual format to plain text.
 * <p>
 * The transformation is sensitive to the source and target string encodings.
 * 
 * @author Derek Hulley
 */
public class StringExtractingContentTransformer extends AbstractContentTransformer
{
    public static final String PREFIX_TEXT = "text/";
    
    private static final Log logger = LogFactory.getLog(StringExtractingContentTransformer.class);
    
    /**
     * Gives a high reliability for all translations from <i>text/sometype</i> to
     * <i>text/plain</i>.  As the text formats are already text, the characters
     * are preserved and no actual conversion takes place.
     * <p>
     * Extraction of text from binary data is wholly unreliable.
     */
    public double getReliability(String sourceMimetype, String targetMimetype)
    {
        if (!targetMimetype.equals(MimetypeMap.MIMETYPE_TEXT_PLAIN))
        {
            // can only convert to plain text
            return 0.0;
        }
        else if (sourceMimetype.startsWith(PREFIX_TEXT))
        {
            // transformations from any text to plain text is OK
            return 1.0;
        }
        else
        {
            // extracting text from binary is not useful
            return 0.0;
        }
    }

    /**
     * Text to text conversions are done directly using the content reader and writer string
     * manipulation methods.
     * <p>
     * Extraction of text from binary content attempts to take the possible character
     * encoding into account.  The text produced from this will, if the encoding was correct,
     * be unformatted but valid. 
     */
    @Override
    protected void transformInternal(ContentReader reader, ContentWriter writer) throws Exception
    {
        // is this a straight text-text transformation
        transformText(reader, writer);
    }
    
    /**
     * Transformation optimized for text-to-text conversion
     */
    private void transformText(ContentReader reader, ContentWriter writer) throws Exception
    {
        // get a char reader and writer
        Reader charReader = null;
        Writer charWriter = null;
        try
        {
            if (reader.getEncoding() == null)
            {
                charReader = new InputStreamReader(reader.getContentInputStream());
            }
            else
            {
                charReader = new InputStreamReader(reader.getContentInputStream(), reader.getEncoding());
            }
            if (writer.getEncoding() == null)
            {
                charWriter = new OutputStreamWriter(writer.getContentOutputStream());
            }
            else
            {
                charWriter = new OutputStreamWriter(writer.getContentOutputStream(), writer.getEncoding());
            }
            // copy from the one to the other
            char[] buffer = new char[1024];
            int readCount = 0;
            while (readCount > -1)
            {
                // write the last read count number of bytes
                charWriter.write(buffer, 0, readCount);
                // fill the buffer again
                readCount = charReader.read(buffer);
            }
        }
        finally
        {
            if (charReader != null)
            {
                try { charReader.close(); } catch (Throwable e) { logger.error(e); }
            }
            if (charWriter != null)
            {
                try { charWriter.close(); } catch (Throwable e) { logger.error(e); }
            }
        }
        // done
    }
}
