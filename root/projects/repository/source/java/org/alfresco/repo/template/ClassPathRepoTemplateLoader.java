/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.ApplicationContextHelper;

import freemarker.cache.TemplateLoader;

/**
 * Custom FreeMarker template loader to locate templates stored either from the ClassPath
 * or in a Alfresco Repository.
 * <p>
 * The template name should be supplied either as a NodeRef String or a ClassPath path String.
 * 
 * @author Kevin Roast
 */
public class ClassPathRepoTemplateLoader implements TemplateLoader
{
    private NodeService nodeService;
    private ContentService contentService;
    private String encoding;
    
    public ClassPathRepoTemplateLoader(NodeService nodeService, ContentService contentService, String encoding)
    {
        if (nodeService == null)
        {
            throw new IllegalArgumentException("NodeService is mandatory.");
        }
        if (contentService == null)
        {
            throw new IllegalArgumentException("ContentService is mandatory.");
        }
        this.nodeService = nodeService;
        this.contentService = contentService;
        this.encoding = encoding;
    }
    
    /**
     * Return an object wrapping a source for a template
     */
    public Object findTemplateSource(String name)
        throws IOException
    {
        if (name.indexOf(StoreRef.URI_FILLER) != -1)
        {
            NodeRef ref = new NodeRef(name);
            if (this.nodeService.exists(ref) == true)
            {
                return new RepoTemplateSource(ref);
            }
            else
            {
                return null;
            }
        }
        else
        {
            URL url = this.getClass().getClassLoader().getResource(name);
            return url == null ? null : new ClassPathTemplateSource(url, encoding);
        }
    }
    
    public long getLastModified(Object templateSource)
    {
        return ((BaseTemplateSource)templateSource).lastModified();
    }
    
    public Reader getReader(Object templateSource, String encoding) throws IOException
    {
        if (encoding != null)
        {
            return ((BaseTemplateSource)templateSource).getReader(encoding);
        }
        else
        {
            return ((BaseTemplateSource)templateSource).getReader(this.encoding);
        }
    }
    
    public void closeTemplateSource(Object templateSource) throws IOException
    {
        ((BaseTemplateSource)templateSource).close();
    }
    
    
    /**
     * Class used as a base for custom Template Source objects
     */
    abstract class BaseTemplateSource
    {
        public abstract Reader getReader(String encoding) throws IOException;
        
        public abstract void close() throws IOException;
        
        public abstract long lastModified();
    }
    
    
    /**
     * Class providing a ClassPath based Template Source
     */
    class ClassPathTemplateSource extends BaseTemplateSource
    {
        private final URL url;
        private URLConnection conn;
        private InputStream inputStream;
        private String encoding;
        
        ClassPathTemplateSource(URL url, String encoding) throws IOException
        {
            this.url = url;
            this.conn = url.openConnection();
            this.encoding = encoding;
        }
        
        public boolean equals(Object o)
        {
            if (o instanceof ClassPathTemplateSource)
            {
                return url.equals(((ClassPathTemplateSource)o).url);
            }
            else
            {
                return false;
            }
        }
        
        public int hashCode()
        {
            return url.hashCode();
        }
        
        public String toString()
        {
            return url.toString();
        }
        
        public long lastModified()
        {
            return conn.getLastModified();
        }
        
        public Reader getReader(String encoding) throws IOException
        {
            inputStream = conn.getInputStream();
            if (encoding != null)
            {
                return new InputStreamReader(inputStream, encoding);
            }
            else
            {
                return new InputStreamReader(inputStream);
            }
        }
        
        public void close() throws IOException
        {
            try
            {
                if (inputStream != null)
                {
                    inputStream.close();
                }
            }
            finally
            {
                inputStream = null;
                conn = null;
            }
        }
    }
    
    /**
     * Class providing a Repository based Template Source
     */
    class RepoTemplateSource extends BaseTemplateSource
    {
        private final NodeRef nodeRef;
        private InputStream inputStream;
        private ContentReader conn;
        
        RepoTemplateSource(NodeRef ref) throws IOException
        {
            this.nodeRef = ref;
            this.conn = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        }
        
        public boolean equals(Object o)
        {
            if (o instanceof RepoTemplateSource)
            {
                return nodeRef.equals(((RepoTemplateSource)o).nodeRef);
            }
            else
            {
                return false;
            }
        }
        
        public int hashCode()
        {
            return nodeRef.hashCode();
        }
        
        public String toString()
        {
            return nodeRef.toString();
        }
        
        public long lastModified()
        {
            return conn.getLastModified();
        }
        
        public Reader getReader(String encoding) throws IOException
        {
            inputStream = conn.getContentInputStream();
            return new InputStreamReader(inputStream, conn.getEncoding());
        }
        
        public void close() throws IOException
        {
            try
            {
                if (inputStream != null)
                {
                    inputStream.close();
                }
            }
            finally
            {
                inputStream = null;
                conn = null;
            }
        }
    }
}
