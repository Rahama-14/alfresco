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
package org.alfresco.repo.exporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.alfresco.service.cmr.view.ExportPackageHandler;
import org.alfresco.service.cmr.view.ExporterException;
import org.alfresco.util.TempFileProvider;


/**
 * Handler for exporting Repository to file system files
 * 
 * @author David Caruana
 */
public class FileExportPackageHandler
    implements ExportPackageHandler
{
    protected File contentDir;
    protected File absContentDir;
    protected File absDataFile;
    protected boolean overwrite;
    protected OutputStream absDataStream = null;

    /**
     * Constuct Handler
     * 
     * @param destDir  destination directory
     * @param dataFile  filename of data file (relative to destDir)
     * @param packageDir  directory for content (relative to destDir)  
     * @param overwrite  force overwrite of existing package directory
     */
    public FileExportPackageHandler(File destDir, File dataFile, File contentDir, boolean overwrite)
    {
        this.contentDir = contentDir;
        this.absContentDir = new File(destDir, contentDir.getPath());
        this.absDataFile = new File(destDir, dataFile.getPath());
        this.overwrite = overwrite;
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.ExportPackageHandler#startExport()
     */
    public void startExport()
    {
        log("Exporting to package " + absDataFile.getAbsolutePath());
        
        if (absContentDir.exists())
        {
            if (overwrite == false)
            {
                throw new ExporterException("Package content dir " + absContentDir.getAbsolutePath() + " already exists.");
            }
            log("Warning: Overwriting existing package dir " + absContentDir.getAbsolutePath());
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.ExportPackageHandler#createDataStream()
     */
    public OutputStream createDataStream()
    {
        if (absDataFile.exists())
        {
            if (overwrite == false)
            {
                throw new ExporterException("Package data file " + absDataFile.getAbsolutePath() + " already exists.");
            }
            log("Warning: Overwriting existing package file " + absDataFile.getAbsolutePath());
            absDataFile.delete();
        }

        try
        {
            absDataFile.createNewFile();
            absDataStream = new FileOutputStream(absDataFile);
            return absDataStream;
        }
        catch(IOException e)
        {
            throw new ExporterException("Failed to create package file " + absDataFile.getAbsolutePath() + " due to " + e.getMessage());
        }
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.ExportStreamHandler#exportStream(java.io.InputStream)
     */
    public String exportStream(InputStream exportStream)
    {
        // Lazily create package directory
        try
        {
            absContentDir.mkdirs();
        }
        catch(SecurityException e)
        {
            throw new ExporterException("Failed to create package dir " + absContentDir.getAbsolutePath() + " due to " + e.getMessage());
        }
        
        // Create file in package directory to hold exported content
        File outputFile = TempFileProvider.createTempFile("export", ".bin", absContentDir);
        
        try
        {
            // Copy exported content from repository to exported file
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[2048 * 10];
            int read = exportStream.read(buffer, 0, 2048 *10);
            while (read != -1)
            {
                outputStream.write(buffer, 0, read);
                read = exportStream.read(buffer, 0, 2048 *10);
            }
            outputStream.close();
        }
        catch(FileNotFoundException e)
        {
            throw new ExporterException("Failed to create export package file due to " + e.getMessage());
        }
        catch(IOException e)
        {
            throw new ExporterException("Failed to export content due to " + e.getMessage());
        }
        
        // return relative path to exported content file (relative to xml export file) 
        return new File(contentDir, outputFile.getName()).getPath();
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.ExportPackageHandler#endExport()
     */
    public void endExport()
    {
        // close Export File
        if (absDataStream != null)
        {
            try
            {
                absDataStream.close();
            }
            catch(IOException e)
            {
                throw new ExporterException("Failed to close package data file " + absDataFile + " due to" + e.getMessage());
            }
        }            
    }
    
    /**
     * Log Export Message
     * 
     * @param message  message to log
     */
    protected void log(String message)
    {
    }
}
