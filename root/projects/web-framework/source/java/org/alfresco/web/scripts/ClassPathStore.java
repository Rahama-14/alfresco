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
package org.alfresco.web.scripts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;


/**
 * ClassPath based Web Script Store
 * 
 * @author davidc
 */
public class ClassPathStore implements Store
{
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    protected boolean mustExist = false;
    protected String classPath;
    protected File fileDir;

    
    /**
     * Sets whether the class path must exist
     * 
     * If it must exist, but it doesn't exist, an exception is thrown
     * on initialisation of the store
     * 
     * @param mustExist
     */
    public void setMustExist(boolean mustExist)
    {
        this.mustExist = mustExist;
    }
    
    /**
     * Sets the class path
     * 
     * @param classPath  classpath
     */
    public void setClassPath(String classPath)
    {
        this.classPath = classPath;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#init()
     */
    public void init()
    {
        ClassPathResource resource = new ClassPathResource(classPath);
        if (resource.exists())
        {
            try
            {
				fileDir = resource.getFile();
			}
            catch (IOException e)
            {
            	throw new WebScriptException("Failed to initialise store " + classPath, e);
			}
        }
        else if (mustExist)
        {
            throw new WebScriptException("Web Script Store classpath:" + classPath + " must exist; it was not found");
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#exists()
     */
    public boolean exists()
    {
        return (fileDir != null);
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getBasePath()
     */
    public String getBasePath()
    {
        return "classpath:" + classPath;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getDescriptionDocumentPaths()
     */
    public String[] getDescriptionDocumentPaths()
    {
        String[] paths;

        try
        {
            List<String> documentPaths = new ArrayList<String>();
            Resource[] resources = resolver.getResources("classpath*:" + classPath + "/**/*.desc.xml");
            int filePathLength = fileDir.getAbsolutePath().length() +1;
            for (Resource resource : resources)
            {
                if (resource instanceof FileSystemResource)
                {
                    String resourcePath = resource.getFile().getAbsolutePath();
                    String documentPath = resourcePath.substring(filePathLength);
                    documentPath = documentPath.replace('\\', '/');
                    documentPaths.add(documentPath);
                }
            }
            paths = documentPaths.toArray(new String[documentPaths.size()]);
        }
        catch(IOException e)
        {
            // Note: Ignore: no service description documents found
            paths = new String[0];
        }
        
        return paths;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getScriptDocumentPaths(org.alfresco.web.scripts.WebScript)
     */
    public String[] getScriptDocumentPaths(WebScript script)
    {
        String[] paths;

        try
        {
            int filePathLength = fileDir.getAbsolutePath().length() +1;
            List<String> documentPaths = new ArrayList<String>();
            String scriptPaths = script.getDescription().getId() + ".*";
            Resource[] resources = resolver.getResources("classpath*:" + classPath + "/" + scriptPaths);
            for (Resource resource : resources)
            {
                if (resource instanceof FileSystemResource)
                {
                    String documentPath = resource.getFile().getAbsolutePath().substring(filePathLength);
                    documentPath = documentPath.replace('\\', '/');
                    documentPaths.add(documentPath);
                }
            }
            paths = documentPaths.toArray(new String[documentPaths.size()]);
        }
        catch(IOException e)
        {
            // Note: Ignore: no service description documents found
            paths = new String[0];
        }
        
        return paths;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#hasDocument(java.lang.String)
     */
    public boolean hasDocument(String documentPath)
    {
        File document = new File(fileDir, documentPath);
        return document.exists();
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getDocument(java.lang.String)
     */
    public InputStream getDocument(String documentPath)      
        throws IOException
    {
        File document = new File(fileDir, documentPath);
        if (!document.exists())
        {
            throw new IOException("Document " + documentPath + " does not exist within store " + getBasePath());
        }
        return new FileInputStream(document);
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#createDocument(java.lang.String, java.lang.String)
     */
    public void createDocument(String documentPath, String content) throws IOException
    {
        File document = new File(fileDir, documentPath);
        
        // create directory
        File path = document.getParentFile();
        path.mkdirs();
        
        // create file
        if (!document.createNewFile())
        {
            throw new IOException("Document " + documentPath + " already exists");
        }
        OutputStream output = new FileOutputStream(document);
        try
        {
            PrintWriter writer = new PrintWriter(output);
            writer.write(content);
            writer.flush();
        }
        finally
        {
            output.flush();
            output.close();
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getTemplateLoader()
     */
    public TemplateLoader getTemplateLoader()
    {
        FileTemplateLoader loader = null;
        try
        {
            loader = new FileTemplateLoader(fileDir);
        }
        catch (IOException e)
        {
            // Note: Can't establish loader, so return null
        }
        return loader;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getScriptLoader()
     */
    public ScriptLoader getScriptLoader()
    {
        return new ClassPathScriptLoader();
    }        
    
    /**
     * Class path based script loader
     * 
     * @author davidc
     */
    private class ClassPathScriptLoader implements ScriptLoader
    {

        /* (non-Javadoc)
         * @see org.alfresco.web.scripts.ScriptLoader#getScriptLocation(java.lang.String)
         */
        public ScriptContent getScript(String path)
        {
            ScriptContent location = null;
            File scriptPath = new File(fileDir, path);
            if (scriptPath.exists())
            {
                location = new ClassPathScriptLocation(scriptPath);
            }
            return location;
        }
    }

    /**
     * Class path script location
     * 
     * @author davidc
     */
    private static class ClassPathScriptLocation implements ScriptContent
    {
        private File location;

        /**
         * Construct
         * 
         * @param location
         */
        public ClassPathScriptLocation(File location)
        {
            this.location = location;
        }
        
        /* (non-Javadoc)
         * @see org.alfresco.service.cmr.repository.ScriptLocation#getInputStream()
         */
        public InputStream getInputStream()
        {
            try
            {
                return new FileInputStream(location);
            }
            catch (FileNotFoundException e)
            {
                throw new WebScriptException("Unable to retrieve input stream for script " + location.getAbsolutePath());
            }
        }

        /* (non-Javadoc)
         * @see org.alfresco.service.cmr.repository.ScriptLocation#getReader()
         */
        public Reader getReader()
        {
            try
            {
                return new InputStreamReader(getInputStream(), "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                throw new WebScriptException("Unsupported Encoding", e);
            }
        }

        /* (non-Javadoc)
         * @see org.alfresco.web.scripts.ScriptContent#getPath()
         */
		public String getPath()
		{
            return location.getAbsolutePath();
		}
    }
    
}
