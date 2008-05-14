/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.repo.web.scripts;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.filefolder.FileFolderServiceImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.tenant.TenantDeployer;
import org.alfresco.repo.tenant.TenantDeployerService;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.web.scripts.ScriptContent;
import org.alfresco.web.scripts.ScriptLoader;
import org.alfresco.web.scripts.Store;
import org.alfresco.web.scripts.WebScript;
import org.alfresco.web.scripts.WebScriptException;

import freemarker.cache.TemplateLoader;


/**
 * Repository based Web Script Store
 * 
 * @author davidc
 */
public class RepoStore implements Store, TenantDeployer
{
    protected boolean mustExist = false;
    protected StoreRef repoStore;
    protected String repoPath;
    protected Map<String, NodeRef> baseNodeRefs;

    // dependencies
    protected RetryingTransactionHelper retryingTransactionHelper;
    protected SearchService searchService;
    protected NodeService nodeService;
    protected ContentService contentService;
    protected FileFolderService fileService;
    protected NamespaceService namespaceService;
    protected PermissionService permissionService;
    protected TenantDeployerService tenantDeployerService;

    
    /**
     * Sets helper that provides transaction callbacks
     */
    public void setTransactionHelper(RetryingTransactionHelper retryingTransactionHelper)
    {
        this.retryingTransactionHelper = retryingTransactionHelper;
    }
    
    /**
     * Sets the search service
     */
    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }
    
    /**
     * Sets the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * Sets the content service
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    /**
     * Sets the file service
     */
    public void setFileFolderService(FileFolderService fileService)
    {
        this.fileService = fileService;
    }

    /**
     * Sets the namespace service
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }
    
    /**
     * Sets the permission service
     */
    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }
    
    /**
     * Sets the tenant deployer service
     */
    public void setTenantDeployerService(TenantDeployerService tenantDeployerService)
    {
        this.tenantDeployerService = tenantDeployerService;
    }

    /**
     * Sets whether the repo store must exist
     * 
     * @param mustExist
     */
    public void setMustExist(boolean mustExist)
    {
        this.mustExist = mustExist;
    }
    
    /**
     * Sets the repo store
     */
    public void setStore(String repoStore)
    {
        this.repoStore = new StoreRef(repoStore);
    }
    
    /**
     * Sets the repo path
     * 
     * @param repoPath  repoPath
     */
    public void setPath(String repoPath)
    {
        this.repoPath = repoPath;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#init()
     * @see org.alfresco.repo.tenant.TenantDeployer#init()
     */
    public void init()
    {
        if (baseNodeRefs == null)
        {
    		baseNodeRefs = new HashMap<String, NodeRef>(1);
    	}
    	
        getBaseNodeRef();
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.tenant.TenantDeployer#destroy()
     */
    public void destroy()
    {
        baseNodeRefs.remove(tenantDeployerService.getCurrentUserDomain());
    }
    
    private NodeRef getBaseNodeRef()
    {
        String tenantDomain = tenantDeployerService.getCurrentUserDomain();
        NodeRef baseNodeRef = baseNodeRefs.get(tenantDomain);
        if (baseNodeRef == null)
        {
            baseNodeRef = AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<NodeRef>()
            {
            	public NodeRef doWork() throws Exception
                {
    	            return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>()
    	            {
                        public NodeRef execute() throws Exception
                        {
                            String query = "PATH:\"" + repoPath + "\"";
                            ResultSet resultSet = searchService.query(repoStore, SearchService.LANGUAGE_LUCENE, query);
                            if (resultSet.length() == 1)
                            {
                                return resultSet.getNodeRef(0);
                            }
                            else if (mustExist)
                            {
                                throw new WebScriptException("Web Script Store " + repoStore.toString() + repoPath + " must exist; it was not found");
                            }
                            return null;
                        }
                    });
                }
    	    }, AuthenticationUtil.getSystemUserName());
    		
    		// TODO clear on deleteTenant
    		baseNodeRefs.put(tenantDomain, baseNodeRef);
    	}
    	return baseNodeRef;
    }
    
    private String getBaseDir()
    {
    	return getPath(getBaseNodeRef());
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#exists()
     */
    public boolean exists()
    {
        return (getBaseNodeRef() != null);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getBasePath()
     */
    public String getBasePath()
    {
        return repoStore.toString() + repoPath;
    }

    /**
     * Gets the display path for the specified node
     * 
     * @param nodeRef
     * @return  display path
     */
    protected String getPath(NodeRef nodeRef)
    {
        return nodeService.getPath(nodeRef).toDisplayPath(nodeService, permissionService) +
               "/" + nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
    }
    
    /**
     * Gets the node ref for the specified path within this repo store
     * 
     * @param documentPath
     * @return  node ref
     */
    protected NodeRef findNodeRef(String documentPath)
    {
        NodeRef node = null;
        try
        {
            String[] pathElements = documentPath.split("/");
            List<String> pathElementsList = Arrays.asList(pathElements);
            FileInfo file = fileService.resolveNamePath(getBaseNodeRef(), pathElementsList);
            node = file.getNodeRef();
        }
        catch (FileNotFoundException e)
        {
            // NOTE: return null
        }
        return node;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getScriptDocumentPaths(org.alfresco.web.scripts.WebScript)
     */
    public String[] getScriptDocumentPaths(final WebScript script)
    {
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<String[]>()
        {
            public String[] doWork() throws Exception
            {
                return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<String[]>()
                {
                    public String[] execute() throws Exception
                    {
                        int baseDirLength = getBaseDir().length() +1;
                        List<String> documentPaths = null;
                        String scriptPath = script.getDescription().getScriptPath();
                        NodeRef scriptNodeRef = (scriptPath.length() == 0) ? getBaseNodeRef() : findNodeRef(scriptPath);
                        if (scriptNodeRef != null)
                        {
                            org.alfresco.service.cmr.repository.Path repoScriptPath = nodeService.getPath(scriptNodeRef);
                            String id = script.getDescription().getId().substring(scriptPath.length() + (scriptPath.length() > 0 ? 1 : 0));
                            String query = "+PATH:\"" + repoScriptPath.toPrefixString(namespaceService) + "//*\" +QNAME:" + id + "*";
                            ResultSet resultSet = searchService.query(repoStore, SearchService.LANGUAGE_LUCENE, query);
                            documentPaths = new ArrayList<String>(resultSet.length());
                            List<NodeRef> nodes = resultSet.getNodeRefs();
                            for (NodeRef nodeRef : nodes)
                            {
                                String name = (String)nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
                                if (name.startsWith(id))
                                {
                                    String nodeDir = getPath(nodeRef);
                                    String documentPath = nodeDir.substring(baseDirLength);
                                    documentPaths.add(documentPath);
                                }
                            }
                        }
                        
                        return documentPaths != null ? documentPaths.toArray(new String[documentPaths.size()]) : new String[0];
                    }
                });
            }
        }, AuthenticationUtil.getSystemUserName());
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getDocumentPaths(java.lang.String, boolean, java.lang.String)
     */
    public String[] getDocumentPaths(String path, boolean includeSubPaths, String documentPattern)
    {
        if ((path == null) || (path.length() == 0))
        {
            path = "/";
        }
        
        if (! path.startsWith("/"))
        {
            path = "/" + path;
        }
        
        if (! path.endsWith("/"))
        {
            path = path + "/";
        }
        
        if ((documentPattern == null) || (documentPattern.length() == 0))
        {
            documentPattern = "*";
        }
        
        final String matcher = documentPattern.replace(".","\\.").replace("*",".*");
        
        final StringBuffer query = new StringBuffer();
        query.append("+PATH:\"").append(repoPath)
               .append(path)
               .append((includeSubPaths ? "/*\"" : ""))
               .append(" +QNAME:")
               .append(documentPattern);
        
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<String[]>()
        {
            public String[] doWork() throws Exception
            {
                return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<String[]>()
                {
                    public String[] execute() throws Exception
                    {
                        int baseDirLength = getBaseDir().length() +1;
                        
                        ResultSet resultSet = searchService.query(repoStore, SearchService.LANGUAGE_LUCENE, query.toString());
                        List<String> documentPaths = new ArrayList<String>(resultSet.length());
                        List<NodeRef> nodes = resultSet.getNodeRefs();
                        for (NodeRef nodeRef : nodes)
                        {
                            String name = (String)nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
                            if (Pattern.matches(matcher, name))
                            {
                                String nodeDir = getPath(nodeRef);
                                String documentPath = nodeDir.substring(baseDirLength);
                                documentPaths.add(documentPath);
                            }
                        }
                        
                        return documentPaths.toArray(new String[documentPaths.size()]);
                    }
                });
            }
        }, AuthenticationUtil.getSystemUserName());
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getDescriptionDocumentPaths()
     */
    public String[] getDescriptionDocumentPaths()
    {
        return getDocumentPaths("/", true, "*.desc.xml");
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getAllDocumentPaths()
     */
    public String[] getAllDocumentPaths()
    {
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<String[]>()
        {
            public String[] doWork() throws Exception
            {
                return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<String[]>()
                {
                    public String[] execute() throws Exception
                    {
                        int baseDirLength = getBaseDir().length() +1;
                        
                        String query = "+PATH:\"" + repoPath + "//*\" +TYPE:\"{http://www.alfresco.org/model/content/1.0}content\"";
                        ResultSet resultSet = searchService.query(repoStore, SearchService.LANGUAGE_LUCENE, query);
                        List<String> documentPaths = new ArrayList<String>(resultSet.length());
                        List<NodeRef> nodes = resultSet.getNodeRefs();
                        for (NodeRef nodeRef : nodes)
                        {
                            String nodeDir = getPath(nodeRef);
                            documentPaths.add(nodeDir.substring(baseDirLength));
                        }
                        
                        return documentPaths.toArray(new String[documentPaths.size()]);
                    }
                });
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#lastModified(java.lang.String)
     */
    public long lastModified(final String documentPath) throws IOException
    {
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Long>()
        {
            public Long doWork() throws Exception
            {
                return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Long>()
                {
                    public Long execute() throws Exception
                    {
                        ContentReader reader = contentService.getReader(
                                findNodeRef(documentPath), ContentModel.PROP_CONTENT);
                        return reader.getLastModified();
                    }
                });
            }
        }, AuthenticationUtil.getSystemUserName());            
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#hasDocument(java.lang.String)
     */
    public boolean hasDocument(final String documentPath)
    {
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Boolean>()
        {
            public Boolean doWork() throws Exception
            {
                return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Boolean>()
                {
                    public Boolean execute() throws Exception
                    {
                        NodeRef nodeRef = findNodeRef(documentPath);
                        return (nodeRef != null);
                    }
                });
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getDescriptionDocument(java.lang.String)
     */
    public InputStream getDocument(final String documentPath)      
        throws IOException
    {
        return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<InputStream>()
        {
            public InputStream doWork() throws Exception
            {
                return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<InputStream>()
                {
                    public InputStream execute() throws Exception
                    {
                        NodeRef nodeRef = findNodeRef(documentPath);
                        if (nodeRef == null)
                        {
                            throw new IOException("Document " + documentPath + " does not exist.");
                        }
                        ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
                        if (reader == null || !reader.exists())
                        {
                            throw new IOException("Failed to read content at " + documentPath + " (content reader does not exist)");
                        }
                        return reader.getContentInputStream();
                    }
                });
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#createDocument(java.lang.String, java.lang.String)
     */
    public void createDocument(String documentPath, String content) throws IOException
    {
        String[] pathElements = documentPath.split("/");
        String[] folderElements = new String[pathElements.length -1];
        System.arraycopy(pathElements, 0, folderElements, 0, pathElements.length -1);
        List<String> folderElementsList = Arrays.asList(folderElements);
        
        // create folder
        FileInfo pathInfo;
        if (folderElementsList.size() == 0)
        {
            pathInfo = fileService.getFileInfo(getBaseNodeRef());
        }
        else
        {
            pathInfo = FileFolderServiceImpl.makeFolders(fileService, getBaseNodeRef(), folderElementsList, ContentModel.TYPE_FOLDER);
        }

        // create file
        String fileName = pathElements[pathElements.length -1];
        if (fileService.searchSimple(pathInfo.getNodeRef(), fileName) != null)
        {
            throw new IOException("Document " + documentPath + " already exists");
        }
        FileInfo fileInfo = fileService.create(pathInfo.getNodeRef(), fileName, ContentModel.TYPE_CONTENT);
        ContentWriter writer = fileService.getWriter(fileInfo.getNodeRef());
        writer.putContent(content);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#updateDocument(java.lang.String, java.lang.String)
     */
    public void updateDocument(String documentPath, String content) throws IOException
    {
        String[] pathElements = documentPath.split("/");
        
        // get parent folder
        NodeRef parentRef;
        if (pathElements.length == 1)
        {
            parentRef = getBaseNodeRef();
        }
        else
        {
            parentRef = findNodeRef(documentPath.substring(0, documentPath.lastIndexOf('/')));
        }

        // update file
        String fileName = pathElements[pathElements.length -1];
        if (fileService.searchSimple(parentRef, fileName) == null)
        {
            throw new IOException("Document " + documentPath + " does not exists");
        }
        FileInfo fileInfo = fileService.create(parentRef, fileName, ContentModel.TYPE_CONTENT);
        ContentWriter writer = fileService.getWriter(fileInfo.getNodeRef());
        writer.putContent(content);
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getTemplateLoader()
     */
    public TemplateLoader getTemplateLoader()
    {
        return new RepoTemplateLoader();
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Store#getScriptLoader()
     */
    public ScriptLoader getScriptLoader()
    {
        return new RepoScriptLoader();
    }        
    
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.tenant.TenantDeployer#onEnableTenant()
     */
    public void onEnableTenant()
    {
        init();
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.tenant.TenantDeployer#onDisableTenant()
     */
    public void onDisableTenant()
    {
        destroy();
    }
    
    /**
     * Repository path based template loader
     * 
     * @author davidc
     */
    private class RepoTemplateLoader implements TemplateLoader
    {
        /* (non-Javadoc)
         * @see freemarker.cache.TemplateLoader#findTemplateSource(java.lang.String)
         */
        public Object findTemplateSource(final String name)
            throws IOException
        {
            return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>()
            {
                public Object doWork() throws Exception
                {
                    return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Object>()
                    {
                        public Object execute() throws Exception
                        {
                            RepoTemplateSource source = null;
                            NodeRef nodeRef = findNodeRef(name);
                            if (nodeRef != null)
                            {
                                source = new RepoTemplateSource(nodeRef);
                            }
                            return source;
                        }
                    });
                }
            }, AuthenticationUtil.getSystemUserName());
        }

        /* (non-Javadoc)
         * @see freemarker.cache.TemplateLoader#getLastModified(java.lang.Object)
         */
        public long getLastModified(Object templateSource)
        {
            return ((RepoTemplateSource)templateSource).lastModified();
        }
        
        /* (non-Javadoc)
         * @see freemarker.cache.TemplateLoader#getReader(java.lang.Object, java.lang.String)
         */
        public Reader getReader(Object templateSource, String encoding) throws IOException
        {
            return ((RepoTemplateSource)templateSource).getReader();
        }

        /* (non-Javadoc)
         * @see freemarker.cache.TemplateLoader#closeTemplateSource(java.lang.Object)
         */
        public void closeTemplateSource(Object arg0) throws IOException
        {
        }
    }

    /**
     * Repository (content) node template source
     *
     * @author davidc
     */
    private class RepoTemplateSource
    {
        protected final NodeRef nodeRef;

        /**
         * Construct
         * 
         * @param ref
         */
        private RepoTemplateSource(NodeRef ref)
        {
            this.nodeRef = ref;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
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
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode()
        {
            return nodeRef.hashCode();
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            return nodeRef.toString();
        }
        
        /**
         * Gets the last modified time of the content
         * 
         * @return  last modified time
         */
        public long lastModified()
        {
            return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Long>()
            {
                public Long doWork() throws Exception
                {
                    return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Long>()
                    {
                        public Long execute() throws Exception
                        {
                            ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
                            return reader.getLastModified();
                        }
                    });
                }
            }, AuthenticationUtil.getSystemUserName());            
        }
        
        /**
         * Gets the content reader
         * 
         * @return  content reader
         * @throws IOException
         */
        public Reader getReader() throws IOException
        {
            return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Reader>()
            {
                public Reader doWork() throws Exception
                {
                    return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Reader>()
                    {
                        public Reader execute() throws Exception
                        {
                            ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
                            return new InputStreamReader(reader.getContentInputStream(), reader.getEncoding());
                        }
                    });
                }
            }, AuthenticationUtil.getSystemUserName());            
        }
    }
        
    /**
     * Repository path based script loader
     * 
     * @author davidc
     */
    private class RepoScriptLoader implements ScriptLoader
    {
        /* (non-Javadoc)
         * @see org.alfresco.web.scripts.ScriptLoader#getScriptLocation(java.lang.String)
         */
        public ScriptContent getScript(final String path)
        {
            return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<ScriptContent>()
            {
                public ScriptContent doWork() throws Exception
                {
                    return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<ScriptContent>()
                    {
                        public ScriptContent execute() throws Exception
                        {
                            ScriptContent location = null;
                            NodeRef nodeRef = findNodeRef(path);
                            if (nodeRef != null)
                            {
                                location = new RepoScriptContent(path, nodeRef);
                            }
                            return location;
                        }
                    });
                }
            }, AuthenticationUtil.getSystemUserName());            
        }
    }
    
    /**
     * Repo path script location
     * 
     * @author davidc
     */
    private class RepoScriptContent implements ScriptContent
    {
        protected String path;
        protected NodeRef nodeRef;

        /**
         * Construct
         * 
         * @param location
         */
        public RepoScriptContent(String path, NodeRef nodeRef)
        {
            this.path = path;
            this.nodeRef = nodeRef;
        }
        
        /* (non-Javadoc)
         * @see org.alfresco.service.cmr.repository.ScriptLocation#getInputStream()
         */
        public InputStream getInputStream()
        {
            return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<InputStream>()
            {
                public InputStream doWork() throws Exception
                {
                    return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<InputStream>()
                    {
                        public InputStream execute() throws Exception
                        {
                            ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
                            return reader.getContentInputStream();
                        }
                    });
                }
            }, AuthenticationUtil.getSystemUserName());            
        }

        /* (non-Javadoc)
         * @see org.alfresco.service.cmr.repository.ScriptLocation#getReader()
         */
        public Reader getReader()
        {
            ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
            try
            {
                return new InputStreamReader(getInputStream(), reader.getEncoding());
            }
            catch (UnsupportedEncodingException e)
            {
                throw new AlfrescoRuntimeException("Unsupported Encoding", e);
            }
        }

        /* (non-Javadoc)
         * @see org.alfresco.web.scripts.ScriptContent#getPath()
         */
		public String getPath()
		{
            return repoStore + getBaseDir() + "/" + path;
		}
		
		/* (non-Javadoc)
		 * @see org.alfresco.web.scripts.ScriptContent#getPathDescription()
		 */
		public String getPathDescription()
		{
		    return "/" + path + " (in repository store " + repoStore.toString() + getBaseDir() + ")";
		}

        /* (non-Javadoc)
         * @see org.alfresco.web.scripts.ScriptContent#isSecure()
         */
        public boolean isSecure()
        {
            return false;
        }
    }

}
