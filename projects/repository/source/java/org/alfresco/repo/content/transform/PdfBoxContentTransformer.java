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
package org.alfresco.repo.content.transform;

import java.io.IOException;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

/**
 * Makes use of the {@link http://www.pdfbox.org/ PDFBox} library to
 * perform conversions from PDF files to text.
 * 
 * @author Derek Hulley
 */
public class PdfBoxContentTransformer extends AbstractContentTransformer
{
    private static final Log logger = LogFactory.getLog(PdfBoxContentTransformer.class);
    
    /**
     * Currently the only transformation performed is that of text extraction from PDF documents.
     */
    public double getReliability(String sourceMimetype, String targetMimetype)
    {
        // TODO: Expand PDFBox usage to convert images to PDF and investigate other conversions
        
        if (!MimetypeMap.MIMETYPE_PDF.equals(sourceMimetype) ||
                !MimetypeMap.MIMETYPE_TEXT_PLAIN.equals(targetMimetype))
        {
            // only support PDF -> Text
            return 0.0;
        }
        else
        {
            return 1.0;
        }
    }

    public void transformInternal(ContentReader reader, ContentWriter writer) throws Exception
    {
        PDDocument pdf = null;
        try
        {
            // stream the document in
            pdf = PDDocument.load(reader.getContentInputStream());
            // strip the text out
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdf);
            
            // dump it all to the writer
            writer.putContent(text);
        }
        catch (IOException e)
        {
            throw new ContentIOException("PDF text stripping failed: \n" +
                    "   reader: " + reader);
        }
        finally
        {
            if (pdf != null)
            {
                try { pdf.close(); } catch (Throwable e) {e.printStackTrace(); }
            }
        }
    }
}
