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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.sf.jooreports.converter.DocumentFamily;
import net.sf.jooreports.converter.DocumentFormat;
import net.sf.jooreports.converter.DocumentFormatRegistry;
import net.sf.jooreports.converter.XmlDocumentFormatRegistry;
import net.sf.jooreports.openoffice.connection.OpenOfficeConnection;
import net.sf.jooreports.openoffice.connection.OpenOfficeException;
import net.sf.jooreports.openoffice.converter.OpenOfficeDocumentConverter;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.TempFileProvider;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * Makes use of the {@link http://sourceforge.net/projects/joott/ JOOConverter} library to
 * perform OpenOffice-drive conversions.
 * 
 * @author Derek Hulley
 */
public class OpenOfficeContentTransformer extends AbstractContentTransformer2
{
    private OpenOfficeConnection connection;
    private OpenOfficeDocumentConverter converter;
    private String documentFormatsConfiguration;
    private DocumentFormatRegistry formatRegistry;
    
    public OpenOfficeContentTransformer()
    {
    }
    
    public void setConnection(OpenOfficeConnection connection)
    {
        this.connection = connection;
    }
    
    /**
     * Set a non-default location from which to load the document format mappings.
     * 
     * @param path a resource location supporting the <b>file:</b> or <b>classpath:</b> prefixes
     */
    public void setDocumentFormatsConfiguration(String path)
    {
        this.documentFormatsConfiguration = path;
    }

    public boolean isConnected()
    {
        return connection.isConnected();
    }

    @Override
    public void register()
    {
        PropertyCheck.mandatory("OpenOfficeContentTransformer", "connection", connection);
        
        // load the document conversion configuration
        if (documentFormatsConfiguration != null)
        {
            DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
            try
            {
                InputStream is = resourceLoader.getResource(documentFormatsConfiguration).getInputStream();
                formatRegistry = new XmlDocumentFormatRegistry(is);
            }
            catch (IOException e)
            {
                throw new AlfrescoRuntimeException(
                        "Unable to load document formats configuration file: " + documentFormatsConfiguration);
            }
        }
        else
        {
            formatRegistry = new XmlDocumentFormatRegistry();
        }
        
        // set up the converter
        converter = new OpenOfficeDocumentConverter(connection);
        
        // Register
        super.register();
    }

    /**
     * @see DocumentFormatRegistry
     */
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        if (!isConnected())
        {
            // The connection management is must take care of this
            return false;
        }
        
        // there are some conversions that fail, despite the converter believing them possible
        if (targetMimetype.equals(MimetypeMap.MIMETYPE_XHTML))
        {
            return false;
        }
        else if (targetMimetype.equals(MimetypeMap.MIMETYPE_WORDPERFECT))
        {
            return false;
        }
        
        MimetypeService mimetypeService = getMimetypeService();
        String sourceExtension = mimetypeService.getExtension(sourceMimetype);
        String targetExtension = mimetypeService.getExtension(targetMimetype);
        // query the registry for the source format
        DocumentFormat sourceFormat = formatRegistry.getFormatByFileExtension(sourceExtension);
        if (sourceFormat == null)
        {
            // no document format
            return false;
        }
        // query the registry for the target format
        DocumentFormat targetFormat = formatRegistry.getFormatByFileExtension(targetExtension);
        if (targetFormat == null)
        {
            // no document format
            return false;
        }

        // get the family of the target document
        DocumentFamily sourceFamily = sourceFormat.getFamily();
        // does the format support the conversion
        if (!targetFormat.isExportableFrom(sourceFamily))
        {
            // unable to export from source family of documents to the target format
            return false;
        }
        else
        {
            return true;
        }
    }

    protected void transformInternal(
            ContentReader reader,
            ContentWriter writer,
            TransformationOptions options) throws Exception
    {
        String sourceMimetype = getMimetype(reader);
        String targetMimetype = getMimetype(writer);

        MimetypeService mimetypeService = getMimetypeService();
        String sourceExtension = mimetypeService.getExtension(sourceMimetype);
        String targetExtension = mimetypeService.getExtension(targetMimetype);
        // query the registry for the source format
        DocumentFormat sourceFormat = formatRegistry.getFormatByFileExtension(sourceExtension);
        if (sourceFormat == null)
        {
            // source format is not recognised
            throw new ContentIOException("No OpenOffice document format for source extension: " + sourceExtension);
        }
        // query the registry for the target format
        DocumentFormat targetFormat = formatRegistry.getFormatByFileExtension(targetExtension);
        if (targetFormat == null)
        {
            // target format is not recognised
            throw new ContentIOException("No OpenOffice document format for target extension: " + targetExtension);
        }
        // get the family of the target document
        DocumentFamily sourceFamily = sourceFormat.getFamily();
        // does the format support the conversion
        if (!targetFormat.isExportableFrom(sourceFamily))
        {
            throw new ContentIOException(
                    "OpenOffice conversion not supported: \n" +
                    "   reader: " + reader + "\n" +
                    "   writer: " + writer);
        }

        // create temporary files to convert from and to
        File tempFromFile = TempFileProvider.createTempFile(
                "OpenOfficeContentTransformer-source-",
                "." + sourceExtension);
        File tempToFile = TempFileProvider.createTempFile(
                "OpenOfficeContentTransformer-target-",
                "." + targetExtension);
        // download the content from the source reader
        reader.getContent(tempFromFile);
        
        try
        {
            converter.convert(tempFromFile, sourceFormat, tempToFile, targetFormat);
            // conversion success
        }
        catch (OpenOfficeException e)
        {
            throw new ContentIOException("OpenOffice server conversion failed: \n" +
                    "   reader: " + reader + "\n" +
                    "   writer: " + writer + "\n" +
                    "   from file: " + tempFromFile + "\n" +
                    "   to file: " + tempToFile,
                    e);
        }
        
        // upload the temp output to the writer given us
        writer.putContent(tempToFile);
    }
}
