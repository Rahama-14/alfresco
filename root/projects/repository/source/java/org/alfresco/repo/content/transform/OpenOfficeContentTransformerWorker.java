/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
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
import net.sf.jooreports.openoffice.converter.AbstractOpenOfficeDocumentConverter;
import net.sf.jooreports.openoffice.converter.OpenOfficeDocumentConverter;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.TempFileProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * Makes use of the {@link http://sourceforge.net/projects/joott/JOOConverter} library to perform OpenOffice-drive
 * conversions.
 * 
 * @author Derek Hulley
 */
public class OpenOfficeContentTransformerWorker extends OOoContentTransformerHelper implements ContentTransformerWorker, InitializingBean
{
    private OpenOfficeConnection connection;
    private AbstractOpenOfficeDocumentConverter converter;
    private String documentFormatsConfiguration;
    private DocumentFormatRegistry formatRegistry;

    /**
     * @param connection
     *            the connection that the converter uses
     */
    public void setConnection(OpenOfficeConnection connection)
    {
        this.connection = connection;
    }

    /**
     * Explicitly set the converter to be used. The converter must use the same connection set in
     * {@link #setConnection(OpenOfficeConnection)}.
     * <p>
     * If not set, then the <code>OpenOfficeDocumentConverter</code> will be used.
     * 
     * @param converter
     *            the converter to use.
     */
    public void setConverter(AbstractOpenOfficeDocumentConverter converter)
    {
        this.converter = converter;
    }

    /**
     * Set a non-default location from which to load the document format mappings.
     * 
     * @param path
     *            a resource location supporting the <b>file:</b> or <b>classpath:</b> prefixes
     */
    public void setDocumentFormatsConfiguration(String path)
    {
        this.documentFormatsConfiguration = path;
    }

    public boolean isAvailable()
    {
        return this.connection.isConnected();
    }

    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory("OpenOfficeContentTransformerWorker", "connection", this.connection);

        // load the document conversion configuration
        if (this.documentFormatsConfiguration != null)
        {
            DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
            try
            {
                InputStream is = resourceLoader.getResource(this.documentFormatsConfiguration).getInputStream();
                this.formatRegistry = new XmlDocumentFormatRegistry(is);
            }
            catch (IOException e)
            {
                throw new AlfrescoRuntimeException("Unable to load document formats configuration file: "
                        + this.documentFormatsConfiguration);
            }
        }
        else
        {
            this.formatRegistry = new XmlDocumentFormatRegistry();
        }

        // set up the converter
        if (this.converter == null)
        {
            this.converter = new OpenOfficeDocumentConverter(this.connection);
        }
    }

    /**
     * @see DocumentFormatRegistry
     */
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        if (!isAvailable())
        {
            // The connection management is must take care of this
            return false;
        }

        if (isTransformationBlocked(sourceMimetype, targetMimetype))
        {
            return false;
        }
        
        MimetypeService mimetypeService = getMimetypeService();
        String sourceExtension = mimetypeService.getExtension(sourceMimetype);
        String targetExtension = mimetypeService.getExtension(targetMimetype);
        // query the registry for the source format
        DocumentFormat sourceFormat = this.formatRegistry.getFormatByFileExtension(sourceExtension);
        if (sourceFormat == null)
        {
            // no document format
            return false;
        }
        // query the registry for the target format
        DocumentFormat targetFormat = this.formatRegistry.getFormatByFileExtension(targetExtension);
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

    public void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception
    {
        String sourceMimetype = getMimetype(reader);
        String targetMimetype = getMimetype(writer);

        MimetypeService mimetypeService = getMimetypeService();
        String sourceExtension = mimetypeService.getExtension(sourceMimetype);
        String targetExtension = mimetypeService.getExtension(targetMimetype);
        // query the registry for the source format
        DocumentFormat sourceFormat = this.formatRegistry.getFormatByFileExtension(sourceExtension);
        if (sourceFormat == null)
        {
            // source format is not recognised
            throw new ContentIOException("No OpenOffice document format for source extension: " + sourceExtension);
        }
        // query the registry for the target format
        DocumentFormat targetFormat = this.formatRegistry.getFormatByFileExtension(targetExtension);
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
            throw new ContentIOException("OpenOffice conversion not supported: \n" + "   reader: " + reader + "\n"
                    + "   writer: " + writer);
        }

        // create temporary files to convert from and to
        File tempFromFile = TempFileProvider.createTempFile("OpenOfficeContentTransformer-source-", "."
                + sourceExtension);
        File tempToFile = TempFileProvider
                .createTempFile("OpenOfficeContentTransformer-target-", "." + targetExtension);
        // download the content from the source reader
        reader.getContent(tempFromFile);

        try
        {
            this.converter.convert(tempFromFile, sourceFormat, tempToFile, targetFormat);
            // conversion success
        }
        catch (OpenOfficeException e)
        {
            throw new ContentIOException("OpenOffice server conversion failed: \n" + "   reader: " + reader + "\n"
                    + "   writer: " + writer + "\n" + "   from file: " + tempFromFile + "\n" + "   to file: "
                    + tempToFile, e);
        }

        // upload the temp output to the writer given us
        writer.putContent(tempToFile);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.content.transform.ContentTransformerWorker#getVersionString()
     */
    public String getVersionString()
    {
        // Actual version information owned by OpenOfficeConnectionTester
        return "";
    }
}
