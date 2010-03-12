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
package org.alfresco.module.vti.handler.alfresco;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.module.vti.handler.VtiHandlerException;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;
import org.springframework.extensions.surf.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;

/**
 * Helper for AlfrescoVtiMethodHandler. Help for path resolving and url formatting
 *
 * @author Dmitry Lazurkin
 *
 */
public class VtiPathHelper extends AbstractLifecycleBean
{
    private final static Log logger = LogFactory.getLog("org.alfresco.module.vti.handler");

    private NodeService nodeService;
    private FileFolderService fileFolderService;
    private PermissionService permissionService;
    private SearchService searchService;
    private NamespaceService namespaceService;
    private PersonService personService;
    private SysAdminParams sysAdminParams;

    private AuthenticationComponent authenticationComponent;

    private NodeRef rootNodeRef;

    private String rootPath;
    private String storePath;
    
    /**
     * Set authentication component
     * 
     * @param authenticationComponent the authentication component to set ({@link AuthenticationComponent})
     */
    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }

    /**
     * Set node service
     * 
     * @param nodeService the node service to set ({@link NodeService})
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * Set file-folder service
     * 
     * @param fileFolderService the file-folder service to set ({@link FileFolderService})
     */
    public void setFileFolderService(FileFolderService fileFolderService)
    {
        this.fileFolderService = fileFolderService;
    }

    /**
     * Set permission service
     * 
     * @param permissionService the permission service to set ({@link PermissionService})
     */
    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    /**
     * Set search service
     * 
     * @param searchService the search service to set ({@link SearchService})
     */
    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    /**
     * Set root path
     * 
     * @param rootPath the root path to set
     */
    public void setRootPath(String rootPath)
    {
        this.rootPath = rootPath;
    }

    /**
     * Set store path
     * 
     * @param storePath the store path to set
     */
    public void setStorePath(String storePath)
    {
        this.storePath = storePath;
    }
    
    /**
     * Get alfresco context
     * 
     * @return alfresco context
     */
    public String getAlfrescoContext()
    {
        return "/" + sysAdminParams.getAlfrescoContext();
    }
    
    /**
     * Set namespace service
     * 
     * @param namespaceService the namespace service to set ({@link NamespaceService})
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    /**
     * Resolve file info for file with URL path
     *
     * @param initialURL URL path
     * @return FileInfo file info null if file or folder doesn't exist
     */
    public FileInfo resolvePathFileInfo(String initialURL)
    {
        initialURL = removeSlashes(initialURL);
        
        FileInfo fileInfo = null;

        if (initialURL.length() == 0)
        {
            fileInfo = fileFolderService.getFileInfo(rootNodeRef);
        }
        else
        {
            try
            {
                List<String> splitPath = Arrays.asList(initialURL.split("/"));
                fileInfo = fileFolderService.resolveNamePath(rootNodeRef, splitPath);
            }
            catch (FileNotFoundException e)
            {
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Resolved file info for '" + initialURL + "' is " + fileInfo);
        }

        return fileInfo;
    }

    /**
     * Resolves file info for file with URL path in parent directory with file info
     *
     * @param parentFileInfo file info for parent directory ({@link FileInfo})
     * @param childName url path relative to parent directory
     * @return FileInfo resolved file info for childName or null, if it doesn't exist
     */
    public FileInfo resolvePathFileInfo(FileInfo parentFileInfo, String childName)
    {
        FileInfo childFileInfo = null;

        try
        {
            childFileInfo = fileFolderService.resolveNamePath(parentFileInfo.getNodeRef(), Collections.singletonList(childName));
        }
        catch (FileNotFoundException e)
        {
        }

        return childFileInfo;
    }

    /**
     * Split URL path to document name and path to parent folder of that document
     *
     * @param path URL path
     * @return Pair<String, String> first item of pair - path to parent folder, second item - document name
     */
    public static Pair<String, String> splitPathParentChild(String path)
    {
        path = removeSlashes(path);
        
        int indexOfName = path.lastIndexOf("/");

        String name = path.substring(indexOfName + 1);
        String parent = "";

        if (indexOfName != -1)
        {
            parent = path.substring(0, indexOfName);
        }

        return new Pair<String, String>(parent, name);
    }

    /**
     * Format FrontPageExtension URL path from file information
     *
     * @param fileInfo file information ({@link FileInfo})
     * @return URL path
     */
    public String toUrlPath(FileInfo fileInfo)
    {
        String urlPath ;
        if (fileInfo.getNodeRef().equals(rootNodeRef))
        {
            urlPath = "";
        }
        else
        {
            StringBuilder builder = new StringBuilder(nodeService.getPath(fileInfo.getNodeRef()).toDisplayPath(nodeService, permissionService));
            builder.delete(0, nodeService.getPath(rootNodeRef).toDisplayPath(nodeService, permissionService).length() +
                    ((String) nodeService.getProperty(rootNodeRef, ContentModel.PROP_NAME)).length() + 1);
            if (builder.length() != 0)
            {
                builder.deleteCharAt(0);
                builder.append("/");
            }
            builder.append(fileInfo.getName());
            urlPath = builder.toString();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Convert " + fileInfo + " to url path '" + urlPath + "'");
        }

        return urlPath;
    }

    /**
     * Get home space for current user
     * 
     * @return String url to home space
     */
    public String getUserHomeLocation()
    {
        NodeRef currentUser = personService.getPerson(authenticationComponent.getCurrentUserName());
        
        if (currentUser == null)
        {
            throw new AuthenticationException("No user have been authorized.");
        }
        
        NodeRef homeSpace = (NodeRef) nodeService.getProperty(currentUser, ContentModel.PROP_HOMEFOLDER);
        
        if (homeSpace == null)
        {
            throw new RuntimeException("No home space was found.");
        }
        
        return toUrlPath(fileFolderService.getFileInfo(homeSpace));        
    }
    
    /**
     * Get Document Workspace site from document name
     * 
     * @param document document
     * @param spaceType type of space
     * @return url to Document Workspace site
     */
    public String getDwsFromDocumentName(String document, final QName spaceType)
    {
        final String alfrescoContext = getContextFromDocumentName(document);
        final String uri = getPathFromDocumentName(document);
        String[] parts = AuthenticationUtil.runAs(new RunAsWork<String[]>()
                {

                    public String[] doWork() throws Exception
                    {
                        if (!uri.startsWith(alfrescoContext))
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("Url must start with alfresco context.");
                            }
                            throw new VtiHandlerException(VtiHandlerException.BAD_URL);
                        }
                        
                        if (uri.equalsIgnoreCase(alfrescoContext))
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("WebUrl: " + alfrescoContext + ", fileUrl: ''");
                            }
                            return new String[]{alfrescoContext, ""};
                        }
                        
                        String webUrl = "";
                        String fileUrl = "";        
                        
                        String[] splitPath = uri.replaceAll(alfrescoContext, "").substring(1).split("/");
                        
                        StringBuilder tempWebUrl = new StringBuilder();
                        
                        for (int i = splitPath.length; i > 0; i--)
                        {
                            tempWebUrl.delete(0, tempWebUrl.length());
                            
                            for (int j = 0; j < i; j++)
                            {
                                if ( j < i-1)
                                {
                                    tempWebUrl.append(splitPath[j] + "/");
                                }
                                else
                                {
                                    tempWebUrl.append(splitPath[j]);
                                }
                            }            
                            
                            FileInfo fileInfo = resolvePathFileInfo(tempWebUrl.toString());
                            
                            if (fileInfo != null)
                            {
                                if (nodeService.getType(fileInfo.getNodeRef()).equals(spaceType))
                                {
                                    webUrl = alfrescoContext + "/" + tempWebUrl;
                                    if (uri.replaceAll(webUrl, "").startsWith("/"))
                                    {
                                        fileUrl = uri.replaceAll(webUrl, "").substring(1);
                                    }
                                    else
                                    {
                                        fileUrl = uri.replaceAll(webUrl, "");                        
                                    }
                                    if (logger.isDebugEnabled())
                                    {
                                        logger.debug("WebUrl: " + webUrl + ", fileUrl: '" + fileUrl + "'");
                                    }
                                    return new String[]{webUrl, fileUrl};
                                }
                            }
                        }
                        if (webUrl.equals(""))
                        {
                            throw new VtiHandlerException(VtiHandlerException.BAD_URL);
                        }
                        return new String[]{webUrl, fileUrl};
                    }
            
                },
                authenticationComponent.getSystemUserName());   
        return parts[0].substring(parts[0].lastIndexOf("/"));
    }
    
    /**
     * Get host from document name
     * 
     * @param document document
     * @return host
     */
    public String getHostFromDocumentName(String document)
    {       
        try
        {
            URL url = new URL(document);            
            return url.getProtocol() + "://" + url.getHost()+ ":" + url.getPort();
        }
        catch (Exception e)
        {
            throw new VtiHandlerException(VtiHandlerException.BAD_URL);
        }        
    }
    
    /**
     * Get context form document name
     * 
     * @param document document
     * @return context
     */
    public String getContextFromDocumentName(String document)
    {                
        try
        {
            URL url = new URL(document);
            
            if (url.getPath().startsWith(getAlfrescoContext()))
            {
                return getAlfrescoContext();
            }
            else
            {
                throw new VtiHandlerException(VtiHandlerException.BAD_URL);
            }            
            
        }
        catch (Exception e)
        {
            throw new VtiHandlerException(VtiHandlerException.BAD_URL);
        }        
    }
    
    /**
     * Get path from document name
     * 
     * @param document document
     * @return path
     */
    public String getPathFromDocumentName(String document)    
    {
        try
        {            
            return new URL(document).getPath();
        }
        catch (Exception e)
        {
            throw new VtiHandlerException(VtiHandlerException.BAD_URL);
        }
    }
    
    /**
     * @see org.springframework.extensions.surf.util.AbstractLifecycleBean#onBootstrap(org.springframework.context.ApplicationEvent)
     */
	protected void onBootstrap(ApplicationEvent event) 
	{
		rootNodeRef = AuthenticationUtil.runAs(new RunAsWork<NodeRef>() 
		{
            public NodeRef doWork() throws Exception
            {
                StoreRef storeRef = new StoreRef(storePath);
                if (nodeService.exists(storeRef) == false)
                {
                    throw new RuntimeException("No store for path: " + storeRef);
                }

                NodeRef storeRootNodeRef = nodeService.getRootNode(storeRef);

                List<NodeRef> nodeRefs = searchService.selectNodes(storeRootNodeRef, rootPath, null, namespaceService, false);

                if (nodeRefs.size() > 1)
                {
                    throw new RuntimeException("Multiple possible roots for : \n" + "   root path: " + rootPath + "\n" + "   results: " + nodeRefs);
                }
                else if (nodeRefs.size() == 0)
                {
                    throw new RuntimeException("No root found for : \n" + "   root path: " + rootPath);
                }
                else
                {
                    return nodeRefs.get(0);
                }
            }
		}, authenticationComponent.getSystemUserName());
	}
	
	/**
	 * Remove slashes from string
	 * 
	 * @param value input string
	 * @return String output string
	 */
	public static String removeSlashes(String value)
	{	    
	    value = value.replaceAll("//", "/");
	    
	    if (value.startsWith("/"))
	        value = value.substring(1);
	    if (value.endsWith("/"))
	        value = value.substring(0, value.length() - 1);
	    return value;	    
	}

	/**
     * @see org.springframework.extensions.surf.util.AbstractLifecycleBean#onShutdown(org.springframework.context.ApplicationEvent)
     */
	protected void onShutdown(ApplicationEvent event) {
		// do nothing
	}

	/**
	 * @return
	 */
    public NodeRef getRootNodeRef()
    {
        return rootNodeRef;
    }

    public String getRootPath()
    {
        return rootPath;
    }

    public String getStorePath()
    {
        return storePath;
    }

    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    public void setSysAdminParams(SysAdminParams sysAdminParams)
    {
        this.sysAdminParams = sysAdminParams;
    }

    public FileFolderService getFileFolderService()
    {
        return fileFolderService;
    }
       
}
