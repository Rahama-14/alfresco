/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
package org.alfresco.repo.lotus.ws.impl.helper;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
//import org.alfresco.util.AbstractLifecycleBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;

/**
 * @author PavelYur
 */
public class AlfrescoQuickrPathHelper extends AbstractLifecycleBean
{

    private static Log logger = LogFactory.getLog(AlfrescoQuickrPathHelper.class);

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static DatatypeFactory datatypeFactory;

    public FileFolderService fileFolderService;

    public void setFileFolderService(FileFolderService fileFolderService)
    {
        this.fileFolderService = fileFolderService;
    }

    static
    {
        try
        {
            datatypeFactory = DatatypeFactory.newInstance();
        }
        catch (DatatypeConfigurationException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Datatype factory was not properly initialized!!!");
            }
        }
    }

    private NodeRef rootNodeRef;

    private String rootPath;
    private StoreRef libraryStoreRef;
    private String lotusUrl;
    private String shareDocumentUrl;
    private String shareFolderUrl;
    private String shareSiteUrl;

    private NodeService nodeService;
    private SearchService searchService;
    private NamespaceService namespaceService;
    private AuthenticationComponent authenticationComponent;
    private PermissionService permissionService;
    private CheckOutCheckInService checkOutCheckInService;

    public void setRootPath(String rootPath)
    {
        this.rootPath = rootPath;
    }

    public void setLibraryStoreRef(StoreRef libraryStoreRef)
    {
        this.libraryStoreRef = libraryStoreRef;
    }

    public void setLotusUrl(String lotusUrl)
    {
        this.lotusUrl = lotusUrl;
    }

    public String getLotusUrl()
    {
        return lotusUrl;
    }

    public void setShareDocumentUrl(String shareDocumentUrl)
    {
        this.shareDocumentUrl = shareDocumentUrl;
    }

    public void setShareFolderUrl(String shareFolderUrl)
    {
        this.shareFolderUrl = shareFolderUrl;
    }

    public void setShareSiteUrl(String shareSiteUrl)
    {
        this.shareSiteUrl = shareSiteUrl;
    }

    public StoreRef getLibraryStoreRef()
    {
        return libraryStoreRef;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }

    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    public void setCheckOutCheckInService(CheckOutCheckInService checkOutCheckInService)
    {
        this.checkOutCheckInService = checkOutCheckInService;
    }

    @Override
    protected void onBootstrap(ApplicationEvent event)
    {
        rootNodeRef = AuthenticationUtil.runAs(new RunAsWork<NodeRef>()
        {
            public NodeRef doWork() throws Exception
            {
                if (nodeService.exists(libraryStoreRef) == false)
                {
                    throw new RuntimeException("No store for path: " + libraryStoreRef);
                }

                NodeRef storeRootNodeRef = nodeService.getRootNode(libraryStoreRef);

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

    @Override
    protected void onShutdown(ApplicationEvent event)
    {
        // do nothing
    }

    /**
     * @param nodeRef NodeRef of the document/folder.
     * @return Path to the document/folder.
     */
    public String getNodePath(NodeRef nodeRef)
    {
        String urlPath;
        if (nodeRef.equals(rootNodeRef))
        {
            urlPath = "";
        }
        else
        {
            StringBuilder builder = new StringBuilder(nodeService.getPath(nodeRef).toDisplayPath(nodeService, permissionService));
            builder.delete(0, nodeService.getPath(rootNodeRef).toDisplayPath(nodeService, permissionService).length()
                    + ((String) nodeService.getProperty(rootNodeRef, ContentModel.PROP_NAME)).length() + 1);
            if (builder.length() != 0)
            {
                builder.deleteCharAt(0);
                builder.append("/");
            }
            String nodeName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
            builder.append(nodeName);
            urlPath = builder.toString();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Convert " + nodeRef + " to url path '" + urlPath + "'");
        }

        return urlPath;
    }

    private NodeRef getNodeRef(NodeRef parentNodeRef, String nodePath) throws FileNotFoundException
    {
        if (parentNodeRef == null)
        {
            parentNodeRef = rootNodeRef;
        }

        nodePath = removeSlashes(nodePath);

        FileInfo fileInfo = null;

        if (nodePath.length() == 0)
        {
            fileInfo = fileFolderService.getFileInfo(parentNodeRef);
        }
        else
        {
            List<String> splitPath = Arrays.asList(nodePath.split("/"));
            fileInfo = fileFolderService.resolveNamePath(parentNodeRef, splitPath);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Resolved file info for '" + nodePath + "' is " + fileInfo);
        }

        return fileInfo.getNodeRef();
    }

    public static String removeSlashes(String value)
    {
        value = value.replaceAll("//", "/");

        if (value.startsWith("/"))
            value = value.substring(1);
        if (value.endsWith("/"))
            value = value.substring(0, value.length() - 1);
        return value;
    }

    public NodeRef getRootNodeRef()
    {
        return rootNodeRef;
    }

    public XMLGregorianCalendar getXmlDate(Date date)
    {
        if (date == null)
        {
            return null;
        }
        return datatypeFactory.newXMLGregorianCalendar(dateFormat.format(date));
    }

    /**
     * @return url of share application document details page.
     */
    public String getShareDocumentUrl()
    {
        return shareDocumentUrl;
    }

    /**
     * @return url of share application folder page.
     */
    public String getShareFolderUrl()
    {
        return shareFolderUrl;
    }

    public String getShareSiteUrl()
    {
        return shareSiteUrl;
    }

    /**
     * Resolve NodeRef using id and path. If only an id is provided, the id must be the uuid of the document. If only a path is provided, that path must be the absolute path to the
     * document. If an id and path are both provided, the id must be the uuid of a folder that is a parent to the document and the path must be the relative path from the parent
     * folder.
     * 
     * @param id The uuid of the document if no path is provided. If both id and path are provided, the id must be the uuid of the parent folder to the document.
     * @param path The absolute path to the document if no id is provided. If both id and path are provided, the path must be the relative path from the id provided.
     * @return NodeRef of the document.
     * @throws FileNotFoundException
     */
    public NodeRef resolveNodeRef(String id, String path) throws FileNotFoundException
    {
        if (id != null)
        {
            if (path == null)
            {
                NodeRef nodeRef = new NodeRef(libraryStoreRef, id);
                if (!nodeService.exists(nodeRef))
                {
                    throw new FileNotFoundException(nodeRef);
                }
                return nodeRef;
            }
            else
            {
                NodeRef parent = new NodeRef(libraryStoreRef, id);
                return getNodeRef(parent, path);
            }
        }
        else
        {
            if (path != null)
            {
                return getNodeRef(null, path);
            }
            else
            {
                throw new FileNotFoundException("id=null  path=null");
            }
        }
    }

    /**
     * Return the name of site where given content is stored.
     * 
     * @param nodeRef the NodeRef of the content
     * @return
     */
    public String getNodeRefSiteName(NodeRef nodeRef)
    {
        boolean found = false;
        NodeRef currentNodeRef = nodeRef;
        String siteName = null;

        while (!found)
        {
            NodeRef parentNodeRef = nodeService.getPrimaryParent(currentNodeRef).getParentRef();

            if (nodeService.getType(parentNodeRef).equals(SiteModel.TYPE_SITE))
            {
                siteName = (String) nodeService.getProperty(parentNodeRef, ContentModel.PROP_NAME);
                found = true;
            }
            else
            {
                currentNodeRef = parentNodeRef;
            }
        }

        return siteName;
    }

    /**
     * Return working copy of document. If working copy doesn't exist return original document.
     * 
     * @param nodeRef
     * @return
     */
    public NodeRef getDocumentForWork(NodeRef nodeRef)
    {

        if (isInRmSite(nodeRef))
        {
            return nodeRef;
        }
        NodeRef workingCopy = checkOutCheckInService.getWorkingCopy(nodeRef);

        if (workingCopy != null)
        {
            return workingCopy;
        }

        return nodeRef;
    }

    /**
     * Check, if provided content lie in Records Management site.
     * 
     * @param nodeRef NodeRef
     * @return true if Node is in Records Management site.
     */
    public boolean isInRmSite(NodeRef nodeRef)
    {
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        while (!nodeService.getType(parent).equals(SiteModel.TYPE_SITE))
        {
            nodeRef = parent;
            parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        }

        if (nodeService.getType(nodeRef).equals(ContentModel.TYPE_FOLDER))
        {
            return false;
        }
        return true;
    }
}
