/*
 * Copyright (C) 2005 Jesper Steen Møller
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
package org.alfresco.repo.content.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

/**
 * Metadata extractor for the PDF documents.
 * <pre>
 *   <b>author:</b>                 --      cm:author
 *   <b>title:</b>                  --      cm:title
 *   <b>subject:</b>                --      cm:description
 *   <b>created:</b>                --      cm:created
 * </pre>
 * 
 * TIKA Note - all the fields (plus a few others) are present
 *  in the tika metadata.
 * 
 * @author Jesper Steen Møller
 * @author Derek Hulley
 */
public class PdfBoxMetadataExtracter extends AbstractMappingMetadataExtracter
{
    protected static Log pdfLogger = LogFactory.getLog(PdfBoxMetadataExtracter.class);

    private static final String KEY_AUTHOR = "author";
    private static final String KEY_TITLE = "title";
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_CREATED = "created";
    
    public static String[] SUPPORTED_MIMETYPES = new String[] {MimetypeMap.MIMETYPE_PDF };

    public PdfBoxMetadataExtracter()
    {
        super(new HashSet<String>(Arrays.asList(SUPPORTED_MIMETYPES)));
    }
    
    @Override
    public Map<String, Serializable> extractRaw(ContentReader reader) throws Throwable
    {
        Map<String, Serializable> rawProperties = newRawMap();
        
        PDDocument pdf = null;
        InputStream is = null;
        try
        {
            is = reader.getContentInputStream();
            // stream the document in
            pdf = PDDocument.load(is);
            if (!pdf.isEncrypted())
            {
                // Scoop out the metadata
                PDDocumentInformation docInfo = pdf.getDocumentInformation();
    
                putRawValue(KEY_AUTHOR, docInfo.getAuthor(), rawProperties);
                putRawValue(KEY_TITLE, docInfo.getTitle(), rawProperties);
                putRawValue(KEY_SUBJECT, docInfo.getSubject(), rawProperties);
    
                try
                {
                    Calendar created = docInfo.getCreationDate();
                    if (created != null)
                    {
                        // Work around https://issues.apache.org/jira/browse/PDFBOX-598
                        created.set(Calendar.MILLISECOND, 0);
                       
                        // Save
                        putRawValue(KEY_CREATED, created.getTime(), rawProperties);
                    }
                }
                catch (IOException iox)
                {
                    // This sometimes fails because the date is a string: ETHREEOH-1936
                    // Alfresco bug ETHREEOH-801 refers to a bug in PDFBox (http://issues.apache.org/jira/browse/PDFBOX-145)
                    // where the above call to docInfo.getCreationDate() throws an IOException for some PDFs.
                    //
                    // The code below is a workaround for that issue.
                    
                    // This creationDate has format: D:20080429+01'00'
                    String creationDate = docInfo.getCustomMetadataValue("CreationDate");
                    
                    if (pdfLogger.isWarnEnabled())
                    {
                        pdfLogger.warn("IOException caught when extracting metadata from pdf file.");
                        pdfLogger.warn("This may be caused by a PDFBox bug that can often be worked around. The stack trace below is provided for information purposes only.");
                        pdfLogger.warn("", iox);
                    }
                    
                    final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    if (creationDate != null && creationDate.length() > 10) // 10 allows for "D:yyyyMMdd"
                    {
                        String dateWithoutLeadingDColon = creationDate.substring(2);
                        Date parsedDate = sdf.parse(dateWithoutLeadingDColon);
                        putRawValue(KEY_CREATED, parsedDate, rawProperties);
                    }
                } 
            }
        }
        finally
        {
            if (is != null)
            {
                try { is.close(); } catch (IOException e) {}
            }
            if (pdf != null)
            {
                try { pdf.close(); } catch (Throwable e) { e.printStackTrace(); }
            }
        }
        // Done
        return rawProperties;
    }
}
