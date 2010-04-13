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
package org.alfresco.filesys.repo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.UserTransaction;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.filesys.alfresco.AlfrescoContext;
import org.alfresco.filesys.alfresco.AlfrescoDiskDriver;
import org.alfresco.filesys.alfresco.AlfrescoNetworkFile;
import org.alfresco.filesys.state.FileState;
import org.alfresco.filesys.state.FileStateLockManager;
import org.alfresco.filesys.state.FileState.FileStateStatus;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.core.DeviceContext;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.server.filesys.AccessDeniedException;
import org.alfresco.jlan.server.filesys.AccessMode;
import org.alfresco.jlan.server.filesys.DirectoryNotEmptyException;
import org.alfresco.jlan.server.filesys.DiskFullException;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.FileName;
import org.alfresco.jlan.server.filesys.FileOpenParams;
import org.alfresco.jlan.server.filesys.FileSharingException;
import org.alfresco.jlan.server.filesys.FileStatus;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.server.filesys.SearchContext;
import org.alfresco.jlan.server.filesys.TreeConnection;
import org.alfresco.jlan.server.filesys.db.DBFileInfo;
import org.alfresco.jlan.server.filesys.pseudo.MemoryNetworkFile;
import org.alfresco.jlan.server.filesys.pseudo.PseudoFile;
import org.alfresco.jlan.server.filesys.pseudo.PseudoFileInterface;
import org.alfresco.jlan.server.filesys.pseudo.PseudoFileList;
import org.alfresco.jlan.server.filesys.pseudo.PseudoNetworkFile;
import org.alfresco.jlan.server.filesys.quota.QuotaManager;
import org.alfresco.jlan.server.filesys.quota.QuotaManagerException;
import org.alfresco.jlan.server.locking.FileLockingInterface;
import org.alfresco.jlan.server.locking.LockManager;
import org.alfresco.jlan.server.locking.OpLockInterface;
import org.alfresco.jlan.server.locking.OpLockManager;
import org.alfresco.jlan.smb.SharingMode;
import org.alfresco.jlan.smb.WinNT;
import org.alfresco.jlan.smb.server.SMBServer;
import org.alfresco.jlan.smb.server.SMBSrvSession;
import org.alfresco.jlan.util.WildCard;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.node.archive.NodeArchiveService;
import org.alfresco.repo.security.authentication.AuthenticationContext;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.config.ConfigElement;

/**
 * Content repository filesystem driver class
 * 
 * <p>Provides a filesystem interface for various protocols such as SMB/CIFS and FTP.
 * 
 * @author Derek Hulley
 */
public class ContentDiskDriver extends AlfrescoDiskDriver implements DiskInterface, FileLockingInterface, OpLockInterface
{
    // Logging
    
    private static final Log logger = LogFactory.getLog(ContentDiskDriver.class);
    
    // Configuration key names
    
    private static final String KEY_STORE = "store";
    private static final String KEY_ROOT_PATH = "rootPath";
    private static final String KEY_RELATIVE_PATH = "relativePath";
    
    // Services and helpers
    
    private CifsHelper cifsHelper;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private SearchService searchService;
    private ContentService contentService;
    private MimetypeService mimetypeService;
    private PermissionService permissionService;
    private FileFolderService fileFolderService;
    private NodeArchiveService nodeArchiveService;
    private LockService lockService;
    
    private AuthenticationContext authContext;
    private AuthenticationService authService;
    private SysAdminParams sysAdminParams;

    // Node monitor factory
    
    private NodeMonitorFactory m_nodeMonitorFactory;
    
	//	Lock manager
	
	private static FileStateLockManager _lockManager = new FileStateLockManager();
    
    /**
     * Class constructor
     * 
     * @param serviceRegistry to connect to the repository services
     */
    public ContentDiskDriver(CifsHelper cifsHelper)
    {
        this.cifsHelper = cifsHelper;
    }

    /**
     * Return the CIFS helper
     * 
     * @return CifsHelper
     */
    public final CifsHelper getCifsHelper()
    {
    	return this.cifsHelper;
    }
    
    /**
     * Return the authentication service
     * 
     * @return AuthenticationService
     */
    public final AuthenticationService getAuthenticationService()
    {
    	return authService;
    }

    /**
     * Return the authentication context
     * 
     * @return AuthenticationContext
     */
    public final AuthenticationContext getAuthenticationContext() {
    	return authContext;
    }
    
    /**
     * Return the node service
     * 
     * @return NodeService
     */
    public final NodeService getNodeService()
    {
    	return this.nodeService;
    }
    
    /**
     * Return the content service
     * 
     * @return ContentService
     */
    public final ContentService getContentService()
    {
    	return this.contentService;
    }

    /**
     * Return the namespace service
     * 
     * @return NamespaceService
     */
    public final NamespaceService getNamespaceService()
    {
    	return this.namespaceService;
    }
    
    /**
     * Return the search service
     * 
     * @return SearchService
     */
    public final SearchService getSearchService(){
    	return this.searchService;
    }

    /**
     * Return the file folder service
     * 
     * @return FileFolderService
     */
    public final FileFolderService getFileFolderService() {
    	return this.fileFolderService;
    }
    
    /**
     * Return the permission service
     * 
     * @return PermissionService
     */
    public final PermissionService getPermissionService() {
    	return this.permissionService;
    }

    /**
     * Return the node archive service
     * 
     * @param NodeArchiveService
     */
    public final NodeArchiveService getNodeArchiveService() {
    	return nodeArchiveService;
    }
    
    /**
     * Return the lock service
     * 
     * @return LockService
     */
    public final LockService getLockService() {
        return lockService;
    }
    
    /**
     * @param contentService the content service
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    /**
     * @param namespaceService the namespace service
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    /**
     * @param nodeService the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * @param searchService the search service
     */
    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }
    
    /**
     * Set the permission service
     * 
     * @param permissionService PermissionService
     */
    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }
    
    /**
     * Set the authentication context
     * 
     * @param authContext AuthenticationContext
     */
    public void setAuthenticationContext(AuthenticationContext authContext)
    {
        this.authContext = authContext;
    }

    /**
     * Set the authentication service
     * 
     * @param authService AuthenticationService
     */
    public void setAuthenticationService(AuthenticationService authService)
    {
    	this.authService = authService;
    }
    
    /**
     * Sets the sys admin params.
     * 
     * @param sysAdminParams
     *            the sys admin params
     */
    public void setSysAdminParams(SysAdminParams sysAdminParams)
    {
        this.sysAdminParams = sysAdminParams;
    }

    /**
     * Set the file folder service
     * 
     * @param fileService FileFolderService
     */
    public void setFileFolderService(FileFolderService fileService)
    {
    	fileFolderService = fileService;
    }
    
    /**
     * @param mimetypeService       service for helping with mimetypes and encoding
     */
    public void setMimetypeService(MimetypeService mimetypeService)
    {
        this.mimetypeService = mimetypeService;
    }

    /**
     * Set the node monitor factory
     * 
     * @param nodeMonitorFactory NodeMonitorFactory
     */
    public void setNodeMonitorFactory(NodeMonitorFactory nodeMonitorFactory) {
    	m_nodeMonitorFactory = nodeMonitorFactory;
    }
    
    /**
     * Set the node archive service
     * 
     * @param NodeArchiveService nodeArchiveService
     */
    public void setNodeArchiveService(NodeArchiveService nodeArchiveService) {
    	this.nodeArchiveService = nodeArchiveService;
    }
    
    /**
     * Set the lock service
     * 
     * @param lockService LockService
     */
    public void setLockService(LockService lockService) {
        this.lockService = lockService;
    }
    
    /**
     * Parse and validate the parameter string and create a device context object for this instance
     * of the shared device. The same DeviceInterface implementation may be used for multiple
     * shares.
     * 
     * @param shareName String
     * @param args ConfigElement
     * @return DeviceContext
     * @exception DeviceContextException
     */
    public DeviceContext createContext(String shareName, ConfigElement cfg) throws DeviceContextException
    {
        ContentContext context = null;
        
        try
        {
            
            // Get the store
            
            ConfigElement storeElement = cfg.getChild(KEY_STORE);
            if (storeElement == null || storeElement.getValue() == null || storeElement.getValue().length() == 0)
            {
                throw new DeviceContextException("Device missing init value: " + KEY_STORE);
            }
            String storeValue = storeElement.getValue();

            // Get the root path
            
            ConfigElement rootPathElement = cfg.getChild(KEY_ROOT_PATH);
            if (rootPathElement == null || rootPathElement.getValue() == null || rootPathElement.getValue().length() == 0)
            {
                throw new DeviceContextException("Device missing init value: " + KEY_ROOT_PATH);
            }
            String rootPath = rootPathElement.getValue();
            

            // Create the context
            
            context = new ContentContext();
            context.setDeviceName(shareName);
            context.setStoreName(storeValue);
            context.setRootPath(rootPath);
            context.setSysAdminParams(this.sysAdminParams);

            // Check if a relative path has been specified
            
            ConfigElement relativePathElement = cfg.getChild(KEY_RELATIVE_PATH);
            
            if ( relativePathElement != null)
            {
                // Make sure the path is in CIFS format
                
                String relPath = relativePathElement.getValue().replace( '/', FileName.DOS_SEPERATOR);
                context.setRelativePath(relPath);
            }


        }
        catch (Exception ex)
        {
            logger.error("Error during create context", ex);
        }

        // Check if URL link files are enabled
        
        ConfigElement urlFileElem = cfg.getChild( "urlFile");
        if ( urlFileElem != null)
        {
            // Get the pseudo file name and web prefix path
            
            ConfigElement pseudoName = urlFileElem.getChild( "filename");
            
            if ( pseudoName != null)
            {
                context.setURLFileName(pseudoName.getValue());
            }
        }
        
        // Check if locked files should be marked as offline
        
        ConfigElement offlineFiles = cfg.getChild( "offlineFiles");
        if ( offlineFiles != null)
        {
            context.setOfflineFiles(true);
        }
        
        // Install the node service monitor
        
        if ( cfg.getChild("disableNodeMonitor") == null) {
        	
        	// Create the node monitor
            context.setDisableNodeMonitor(true);
        }
        
        // Check if oplocks are enabled, if so then enable oplocks in the lock manager
        
        if ( cfg.getChild("disableOplocks") != null) {
        	context.setDisableOplocks( true);
        }
        
        // Register the device context
        
        registerContext(context);
        
        // Return the context for this shared filesystem
        
        return context;
    }

    
    /**
     * Registers a device context object for this instance
     * of the shared device. The same DeviceInterface implementation may be used for multiple
     * shares.
     * 
     * @param ctx the context
     * @exception DeviceContextException
     */
    @Override
    public void registerContext(DeviceContext ctx) throws DeviceContextException
    {
        super.registerContext(ctx);

        ContentContext context = (ContentContext)ctx;

        // Wrap the initialization in a transaction
        
        UserTransaction tx = getTransactionService().getUserTransaction(true);

        try
        {
            // Use the system user as the authenticated context for the filesystem initialization
            
            AuthenticationUtil.pushAuthentication();
            AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getSystemUserName());
            
            // Start the transaction
            
            if ( tx != null)
                tx.begin();
            
            // Get the store
            String storeValue = context.getStoreName();
            StoreRef storeRef = new StoreRef(storeValue);
            
            // Connect to the repo and ensure that the store exists
            
            if (! nodeService.exists(storeRef))
            {
                throw new DeviceContextException("Store not created prior to application startup: " + storeRef);
            }
            NodeRef storeRootNodeRef = nodeService.getRootNode(storeRef);
            
            // Get the root path
            String rootPath = context.getRootPath();
            
            // Find the root node for this device
            
            List<NodeRef> nodeRefs = searchService.selectNodes(storeRootNodeRef, rootPath, null, namespaceService, false);
            
            NodeRef rootNodeRef = null;
            
            if (nodeRefs.size() > 1)
            {
                throw new DeviceContextException("Multiple possible roots for device: \n" +
                        "   root path: " + rootPath + "\n" +
                        "   results: " + nodeRefs);
            }
            else if (nodeRefs.size() == 0)
            {
                // Nothing found
                
                throw new DeviceContextException("No root found for device: \n" +
                        "   root path: " + rootPath);
            }
            else
            {
                // We found a node
                
                rootNodeRef = nodeRefs.get(0);
            }

            // Check if a relative path has been specified
            
            String relPath = context.getRelativePath();
            
            if ( relPath != null && relPath.length() > 0)
            {
                // Find the node and validate that the relative path is to a folder
                
                NodeRef relPathNode = cifsHelper.getNodeRef( rootNodeRef, relPath);
                if ( cifsHelper.isDirectory( relPathNode) == false)
                    throw new DeviceContextException("Relative path is not a folder, " + relPath);
                
                // Use the relative path node as the root of the filesystem
                
                rootNodeRef = relPathNode;
            }
            else {
                
                // Make sure the default root node is a folder
                
                if ( cifsHelper.isDirectory( rootNodeRef) == false)
                    throw new DeviceContextException("Root node is not a folder type node");
            }
            
            // Commit the transaction
            
            tx.commit();
            tx = null;

            // Record the root node ref
            context.setRootNodeRef(rootNodeRef);
        }
        catch (Exception ex)
        {
            logger.error("Error during create context", ex);
        }
        finally
        {
            // Restore authentication context
            
            AuthenticationUtil.popAuthentication();
            
            // If there is an active transaction then roll it back
            
            if ( tx != null)
            {
                try
                {
                    tx.rollback();
                }
                catch (Exception ex)
                {
                    logger.warn("Failed to rollback transaction", ex);
                }
            }
        }

        // Check if locked files should be marked as offline
        if ( context.getOfflineFiles() )
        {
            // Enable marking locked files as offline
            
            cifsHelper.setMarkLockedFilesAsOffline( true);
            
            // Logging
            
            logger.info("Locked files will be marked as offline");
        }
        
        // Enable file state caching
        
        context.enableStateTable( true, getStateReaper());
        
        // Initialize the I/O control handler
        
        if ( context.hasIOHandler())
            context.getIOHandler().initialize( this, context);
        
        // Install the node service monitor
        
        if ( !context.getDisableNodeMonitor() && m_nodeMonitorFactory != null) {
            
            // Create the node monitor

            NodeMonitor nodeMonitor = m_nodeMonitorFactory.createNodeMonitor( this, context);
            context.setNodeMonitor( nodeMonitor);
        }
        
        // Check if oplocks are enabled
        
        if ( context.getDisableOplocks() == false) {
                
            // Enable oplock support
            	
            _lockManager.setStateTable( context.getStateTable());
        }
        else
        	logger.warn("Oplock support disabled for filesystem " + ctx.getDeviceName());
        
        // Start the quota manager, if enabled
        
        if ( context.hasQuotaManager()) {
            
            try {

                // Start the quota manager
                
                context.getQuotaManager().startManager( this, context);
                logger.info("Quota manager enabled for filesystem");
            }
            catch ( QuotaManagerException ex) {
                logger.error("Failed to start quota manager", ex);
            }
        }
    }

    /**
     * Check if pseudo file support is enabled
     * 
     * @param context ContentContext
     * @return boolean
     */
    public final boolean hasPseudoFileInterface(ContentContext context)
    {
    	return context.hasPseudoFileInterface();
    }
    
    /**
     * Return the pseudo file support implementation
     *
     * @param context ContentContext
     * @return PseudoFileInterface
     */
    public final PseudoFileInterface getPseudoFileInterface(ContentContext context)
    {
        return context.getPseudoFileInterface();
    }
    
    /**
     * Determine if the disk device is read-only.
     * 
     * @param sess Server session
     * @param ctx Device context
     * @return boolean
     * @exception java.io.IOException If an error occurs.
     */
    public boolean isReadOnly(SrvSession sess, DeviceContext ctx) throws IOException
    {
        if (cifsHelper.isReadOnly())
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Get the file information for the specified file.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param name File name/path that information is required for.
     * @return File information if valid, else null
     * @exception java.io.IOException The exception description.
     */
    public FileInfo getFileInformation(SrvSession session, TreeConnection tree, String path) throws IOException
    {
    	// Start a transaction
    	
    	beginReadTransaction( session);
        
        // Get the device root
        
        ContentContext ctx = (ContentContext) tree.getContext();
        NodeRef infoParentNodeRef = ctx.getRootNode();
        
        if ( path == null || path.length() == 0)
            path = FileName.DOS_SEPERATOR_STR;
        
        String infoPath = path;
        
        try
        {
            // Check if the path is to a pseudo file

            FileInfo finfo = null;
            
            if ( hasPseudoFileInterface(ctx))
            {
            	// Make sure the parent folder has a file state, and the path exists
        		
                String[] paths = FileName.splitPath( path);
                FileState fstate = ctx.getStateTable().findFileState( paths[0]);
                
                if ( fstate == null)
                {
                	NodeRef nodeRef = getNodeForPath(tree, paths[0]);
                        
                    if ( nodeRef != null)
                    {
                        // Get the file information for the node
                            
                        finfo = cifsHelper.getFileInformation(nodeRef);
                    }
                        
              		// Create the file state
                		
               		fstate = ctx.getStateTable().findFileState( paths[0], true, true);
                		
               		fstate.setFileStatus( FileStatus.DirectoryExists);
   	                fstate.setNodeRef( nodeRef);
                		
               		// Add pseudo files to the folder
                		
               		getPseudoFileInterface( ctx).addPseudoFilesToFolder( session, tree, paths[0]);
                		
               		// Debug
                		
              		if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
               			logger.debug( "Added file state for pseudo files folder (getinfo) - " + paths[0]);
                }
                else if ( fstate.hasPseudoFiles() == false)
                {
            		// Make sure the file state has the node ref
            		
            		if ( fstate.hasNodeRef() == false)
            		{
    	                // Get the node for the folder path
    	                
    	                fstate.setNodeRef( getNodeForPath( tree, paths[0]));
            		}
            		
                	// Add pseudo files for the parent folder
                	
            		getPseudoFileInterface( ctx).addPseudoFilesToFolder( session, tree, paths[0]);
            		
            		// Debug
            		
            		if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
            			logger.debug( "Added pseudo files for folder (exists) - " + paths[0]);
                }
            	
            	
                // Get the pseudo file
                
                PseudoFile pfile = getPseudoFileInterface(ctx).getPseudoFile( session, tree, path);
                if ( pfile != null)
                {
                    // DEBUG
                    if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                        logger.debug("getInfo using pseudo file info for " + path);
                    
                    FileInfo pseudoFileInfo = pfile.getFileInfo();
                    if (cifsHelper.isReadOnly())
                    {
                        int attr = pseudoFileInfo.getFileAttributes();
                        if (( attr & FileAttribute.ReadOnly) == 0)
                        {
                            attr += FileAttribute.ReadOnly;
                            pseudoFileInfo.setFileAttributes(attr);
                        }
                    }
                    return pfile.getFileInfo();
                }
            }
            
            // Get the node ref for the path, chances are there is a file state in the cache
            
            NodeRef nodeRef = getNodeForPath(tree, infoPath);
            
            if ( nodeRef != null)
            {
                // Get the file information for the node
                
                finfo = cifsHelper.getFileInformation(nodeRef);

                // DEBUG
                
                if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                    logger.debug("getInfo using cached noderef for path " + path);
            }
            
            // If the required node was not in the state cache, the parent folder node might be
            
            
            if ( finfo == null)
            {
                String[] paths = FileName.splitPath( path);
                
                if ( paths[0] != null && paths[0].length() > 1)
                {
                    // Find the node ref for the folder being searched
                    
                    nodeRef = getNodeForPath(tree, paths[0]);
                    
                    if ( nodeRef != null)
                    {
                        infoParentNodeRef = nodeRef;
                        infoPath          = paths[1];
                        
                        // DEBUG
                        
                        if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                            logger.debug("getInfo using cached noderef for parent " + path);
                    }
                }
            
                // Access the repository to get the file information
                
                finfo = cifsHelper.getFileInformation(infoParentNodeRef, infoPath);
                
                // DEBUG
                
                if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                    logger.debug("Getting file information: path=" + path + " file info: " + finfo);
            }

            // Set the file id for the file using the relative path
            
            if ( finfo != null) {
            	
            	// Set the file id
            
            	finfo.setFileId( path.hashCode());
            
            	// Copy cached timestamps, if available
            	
                FileState fstate = getStateForPath(tree, infoPath);
                if ( fstate != null) {
                	if ( fstate.hasAccessDateTime())
                		finfo.setAccessDateTime(fstate.getAccessDateTime());
                	if ( fstate.hasChangeDateTime())
                		finfo.setChangeDateTime(fstate.getChangeDateTime());
                	if ( fstate.hasModifyDateTime())
                		finfo.setModifyDateTime(fstate.getModifyDateTime());
                }
                else {
                	
                	// Create a file state for the file/folder
                	
                	fstate = ctx.getStateTable().findFileState( path, finfo.isDirectory(), true);
                	
                	fstate.setNodeRef( nodeRef);
                }
            }
            
            // Return the file information
            
            return finfo;
        }
        catch (FileNotFoundException e)
        {
            // Debug
        	
            if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                logger.debug("Get file info - file not found, " + path);
            throw e;
        }
        catch (org.alfresco.repo.security.permissions.AccessDeniedException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                logger.debug("Get file info - access denied, " + path);
            
            // Convert to a filesystem access denied status
            
            throw new AccessDeniedException("Get file information " + path);
        }
        catch (AlfrescoRuntimeException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                logger.debug("Get file info error", ex);
            
            // Convert to a general I/O exception
            
            throw new IOException("Get file information " + path);
        }
    }

    /**
     * Start a new search on the filesystem using the specified searchPath that may contain
     * wildcards.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param searchPath File(s) to search for, may include wildcards.
     * @param attrib Attributes of the file(s) to search for, see class SMBFileAttribute.
     * @return SearchContext
     * @exception java.io.FileNotFoundException If the search could not be started.
     */
    public SearchContext startSearch(SrvSession sess, TreeConnection tree, String searchPath, int attributes) throws FileNotFoundException
    {
        // Access the device context
        
        ContentContext ctx = (ContentContext) tree.getContext();

        try
        {
            String searchFileSpec = searchPath;
            NodeRef searchRootNodeRef = ctx.getRootNode();
            FileState searchFolderState = null;
            
            // Create the transaction
            
            beginReadTransaction( sess);
            
            // If the state table is available see if we can speed up the search using either cached
            // file information or find the folder node to be searched without having to walk the path

            String[] paths = FileName.splitPath(searchPath);
            
            if ( ctx.hasStateTable())
            {
                // See if the folder to be searched has a file state, we can avoid having to walk the path
                
                if ( paths[0] != null && paths[0].length() >= 1)
                {
                    // Find the node ref for the folder being searched
                    
                    NodeRef nodeRef = getNodeForPath(tree, paths[0]);
                    
                    // Get the file state for the folder being searched
                    
                    searchFolderState = getStateForPath(tree, paths[0]);
                    if ( searchFolderState == null)
                    {
                        // Create a file state for the folder

                        searchFolderState = ctx.getStateTable().findFileState( paths[0], true, true);
                    }
                    
                    // Make sure the associated node is set
                    
                    if ( searchFolderState.hasNodeRef() == false)
                    {
                        // Set the associated node for the folder
                        
                        searchFolderState.setNodeRef( nodeRef);
                    }
                    
                    // Add pseudo files to the folder being searched

                    if ( hasPseudoFileInterface(ctx))
                        getPseudoFileInterface(ctx).addPseudoFilesToFolder( sess, tree, paths[0]);

                    // Set the search node and file spec
                    
                    if ( nodeRef != null)
                    {
                        searchRootNodeRef = nodeRef;
                        searchFileSpec    = paths[1];
                        
                        // DEBUG
                        
                        if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_SEARCH))
                            logger.debug("Search using cached noderef for path " + searchPath);
                    }
                }
            }
            
            // Convert the all files wildcard
            
            if ( searchFileSpec.equals( "*.*"))
            	searchFileSpec = "*";
            
            // Debug
            
            long startTime = 0L;
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_SEARCH))
            	startTime = System.currentTimeMillis();
            
            // Perform the search
            
            List<NodeRef> results = cifsHelper.getNodeRefs(searchRootNodeRef, searchFileSpec);

            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_SEARCH)) {
            	long endTime = System.currentTimeMillis();
            	if (( endTime - startTime) > 500)
            		logger.debug("Search for searchPath=" + searchPath + ", searchSpec=" + searchFileSpec + ", searchRootNode=" + searchRootNodeRef + " took "
            				     + ( endTime - startTime) + "ms results=" + results.size());
            }
            
            // Check if there are any pseudo files for the folder being searched, for CIFS only
            
            PseudoFileList pseudoList = null;
            
            if ( sess instanceof SMBSrvSession && searchFolderState != null && searchFolderState.hasPseudoFiles())
            {
                // If it is a wildcard search use all pseudo files
                
                if ( WildCard.containsWildcards(searchFileSpec))
                {
                    // Get the list of pseudo files for the search path
                    
                    pseudoList = searchFolderState.getPseudoFileList();
                    
                    // Check if the wildcard is for all files or a subset
                   
                    if ( searchFileSpec.equals( "*") == false && pseudoList != null && pseudoList.numberOfFiles() > 0)
                    {
                        // Generate a subset of pseudo files that match the wildcard search pattern
                        
                        WildCard wildCard = new WildCard( searchFileSpec, false);
                        PseudoFileList filterList = null;
                        
                        for ( int i = 0; i > pseudoList.numberOfFiles(); i++)
                        {
                            PseudoFile pseudoFile = pseudoList.getFileAt( i);
                            if ( wildCard.matchesPattern( pseudoFile.getFileName()))
                            {
                                // Add the pseudo file to the filtered list
                                
                                if ( filterList == null)
                                    filterList = new PseudoFileList();
                                filterList.addFile( pseudoFile);
                            }
                        }
                        
                        // Use the filtered pseudo file list, or null if there were no matches
                        
                        pseudoList = filterList;
                    }
                }
                else if ( results == null || results.size() == 0)
                {
                    // Check if the required file is in the pseudo file list
                    
                    String fname = paths[1];
                    
                    if ( fname != null)
                    {
                        // Search for a matching pseudo file
                        
                        PseudoFile pfile = searchFolderState.getPseudoFileList().findFile( fname, true);
                        if ( pfile != null)
                        {
                            // Create a file list with the required file
                            
                            pseudoList = new PseudoFileList();
                            pseudoList.addFile( pfile);
                        }
                    }
                }
            }
            
            // Build the search context to store the results, use the cache lookup search for wildcard searches
            
            SearchContext searchCtx = null;
            
            if ( searchFileSpec.equals( "*"))
            {
            	// Use a cache lookup search context 

            	CacheLookupSearchContext cacheContext = new CacheLookupSearchContext(cifsHelper, results, searchFileSpec, pseudoList, paths[0], ctx.getStateTable());
            	searchCtx = cacheContext;
            	
            	// Set the '.' and '..' pseudo file entry details
            	
            	if ( searchFolderState != null && searchFolderState.hasNodeRef())
            	{
            		// Get the '.' pseudo entry file details
            	
            		FileInfo finfo = cifsHelper.getFileInformation( searchFolderState.getNodeRef());
            		
            		// Blend in any cached timestamps
            		
            		if ( searchFolderState != null) {
            			if ( searchFolderState.hasAccessDateTime())
            				finfo.setAccessDateTime( searchFolderState.getAccessDateTime());
            			
            			if ( searchFolderState.hasChangeDateTime())
            				finfo.setChangeDateTime( searchFolderState.getChangeDateTime());
            			
            			if ( searchFolderState.hasModifyDateTime())
            				finfo.setModifyDateTime( searchFolderState.getModifyDateTime());
            		}
            		
            		// Set the '.' pseudo entry details
            		
            		cacheContext.setDotInfo( finfo);
            		
            		// Check if the search folder has a parent, if we are at the root of the filesystem then re-use
            		// the file information
            		
            		if ( searchFolderState.getPath().equals( FileName.DOS_SEPERATOR_STR)) {
            			
            			// Searching the root folder, re-use the search folder file information for the '..' pseudo entry
            			
            			cacheContext.setDotDotInfo( finfo);
            		}
            		else {
            			
            			// Get the parent folder path
            			
            			String parentPath = searchFolderState.getPath();
            			if ( parentPath.endsWith( FileName.DOS_SEPERATOR_STR) && parentPath.length() > 1)
            				parentPath = parentPath.substring(0, parentPath.length() - 1);
            			
            			int pos = parentPath.lastIndexOf( FileName.DOS_SEPERATOR_STR);
            			if ( pos != -1)
            				parentPath = parentPath.substring(0, pos + 1);
            			
            			// Get the file state for the parent path, if available
            			
            			FileState parentState = ctx.getStateTable().findFileState( parentPath);
            			NodeRef parentNode = null;
            			
            			if ( parentState != null)
            				parentNode = parentState.getNodeRef();
            			
            			if ( parentState == null || parentNode == null)
            				parentNode = getNodeForPath( tree, parentPath);

            			// Get the file information for the parent folder
            			
            			finfo = cifsHelper.getFileInformation( parentNode);
            			
            			// Blend in any cached timestamps
            			
                		if ( parentState != null) {
                			if ( parentState.hasAccessDateTime())
                				finfo.setAccessDateTime( parentState.getAccessDateTime());
                			
                			if ( parentState.hasChangeDateTime())
                				finfo.setChangeDateTime( parentState.getChangeDateTime());
                			
                			if ( parentState.hasModifyDateTime())
                				finfo.setModifyDateTime( parentState.getModifyDateTime());
                		}

                		// Set the '..' pseudo entry details
                		
            			cacheContext.setDotDotInfo( finfo);
            		}
           		}
            }
            else
            	searchCtx = new ContentSearchContext(cifsHelper, results, searchFileSpec, pseudoList, paths[0]);
            
            // Debug
            
            if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_SEARCH))
                logger.debug("Started search: search path=" + searchPath + " attributes=" + attributes + ", ctx=" + searchCtx);
            
            // Return the search context
            
            return searchCtx;
        }
        catch (org.alfresco.repo.security.permissions.AccessDeniedException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_SEARCH))
                logger.debug("Start search - access denied, " + searchPath);
            
            // Convert to a file not found status
            
            throw new FileNotFoundException("Start search " + searchPath);
        }
        catch (AlfrescoRuntimeException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_SEARCH))
                logger.debug("Start search", ex);
            
            // Convert to a file not found status
            
            throw new FileNotFoundException("Start search " + searchPath);
        }
    }

    /**
     * Check if the specified file exists, and whether it is a file or directory.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param name java.lang.String
     * @return int
     * @see FileStatus
     */
    public int fileExists(SrvSession sess, TreeConnection tree, String name)
    {
        ContentContext ctx = (ContentContext) tree.getContext();
        int status = FileStatus.Unknown;
        
        try
        {
            // Check for a cached file state
            
            FileState fstate = null;
            
            if ( ctx.hasStateTable())
                ctx.getStateTable().findFileState(name);
            
            if ( fstate != null)
            {
                FileStateStatus fsts = fstate.getFileStatus();

                if ( fsts == FileStateStatus.FileExists)
                    status = FileStatus.FileExists;
                else if ( fsts == FileStateStatus.FolderExists)
                    status = FileStatus.DirectoryExists;
                else if ( fsts == FileStateStatus.NotExist || fsts == FileStateStatus.Renamed)
                    status = FileStatus.NotExist;
                
                // DEBUG
                
                if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                    logger.debug("Cache hit - fileExists() " + name + ", sts=" + status);
            }
            else
            {
                // Check if pseudo files are enabled

                if ( hasPseudoFileInterface(ctx))
                {
                	// Check if the file name is a pseudo file name
                	
                	if ( getPseudoFileInterface( ctx).isPseudoFile(sess, tree, name)) {
                		
    	            	// Make sure the parent folder has a file state, and the path exists
                		
    	                String[] paths = FileName.splitPath( name);
    	                fstate = ctx.getStateTable().findFileState( paths[0]);
    	                
    	                if ( fstate == null) {

    	                	// Check if the path exists
    	                	
    	                	if ( fileExists( sess, tree, paths[0]) == FileStatus.DirectoryExists)
    	                	{
    	                		// Create the file state
    	                		
    	                		fstate = ctx.getStateTable().findFileState( paths[0], true, true);
    	                		
    	                		fstate.setFileStatus( FileStatus.DirectoryExists);
    	                		
   	        	                // Get the node for the folder path
    	        	                
    	                		beginReadTransaction( sess);
   	        	                fstate.setNodeRef( getNodeForPath( tree, paths[0]));
    	                		
    	                		// Add pseudo files to the folder
    	                		
    	                		getPseudoFileInterface( ctx).addPseudoFilesToFolder( sess, tree, paths[0]);
    	                		
    	                		// Debug
    	                		
    	                		if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_PSEUDO))
    	                			logger.debug( "Added file state for pseudo files folder (exists) - " + paths[0]);
    	                	}
    	                }
    	                else if ( fstate.hasPseudoFiles() == false)
    	                {
	                		// Make sure the file state has the node ref
	                		
	                		if ( fstate.hasNodeRef() == false)
	                		{
	        	            	// Create the transaction
	        	                
	                			beginReadTransaction( sess);
	        	            
	        	                // Get the node for the folder path
	        	                
	        	                fstate.setNodeRef( getNodeForPath( tree, paths[0]));
	                		}
	                		
    	                	// Add pseudo files for the parent folder
    	                	
                    		getPseudoFileInterface( ctx).addPseudoFilesToFolder( sess, tree, paths[0]);
                    		
                    		// Debug
                    		
                    		if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_PSEUDO))
                    			logger.debug( "Added pseudo files for folder (exists) - " + paths[0]);
    	                }
    	            	
    	                // Check if the path is to a pseudo file
    	                
    	                PseudoFile pfile = getPseudoFileInterface(ctx).getPseudoFile( sess, tree, name);
    	                if ( pfile != null)
    	                {
    	                    // Indicate that the file exists
    	                    
    	                    status = FileStatus.FileExists;
    	                }
    	                else
    	                {
    	                	// Failed to find pseudo file
    	                	
    	                	if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_PSEUDO))
    	                		logger.debug( "Failed to find pseudo file (exists) - " + name);
    	                }
                	}
                }

                // If the file is not a pseudo file then search for the file
                
                if ( status == FileStatus.Unknown) 
                {
	            	// Create the transaction
	                
                	beginReadTransaction( sess);
	                
	                // Get the file information to check if the file/folder exists
	                
	                FileInfo info = getFileInformation(sess, tree, name);
	                if (info.isDirectory())
	                {
	                    status = FileStatus.DirectoryExists;
	                }
	                else
	                {
	                    status = FileStatus.FileExists;
	                }
                }
            }
        }
        catch (FileNotFoundException e)
        {
            status = FileStatus.NotExist;
        }
        catch (IOException e)
        {
            // Debug

        	if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
        		logger.debug("File exists error, " + name, e);
            
            status = FileStatus.NotExist;
        }

        // Debug
        
        if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
            logger.debug("File status determined: name=" + name + " status=" + FileStatus.asString(status));
        
        // Return the file/folder status
        
        return status;
    }
    
    /**
     * Open a file or folder
     * 
     * @param sess SrvSession
     * @param tree TreeConnection
     * @param params FileOpenParams
     * @return NetworkFile
     * @exception IOException
     */
    public NetworkFile openFile(SrvSession sess, TreeConnection tree, FileOpenParams params) throws IOException
    {
        // Create the transaction
        
    	beginReadTransaction( sess);
        ContentContext ctx = (ContentContext) tree.getContext();
        
        try
        {
            // Check if pseudo files are enabled
            
            if ( hasPseudoFileInterface(ctx))
            {
            	// Check if the file name is a pseudo file name
            	
            	String path = params.getPath();

            	if ( getPseudoFileInterface( ctx).isPseudoFile(sess, tree, path)) {
            		
	            	// Make sure the parent folder has a file state, and the path exists
	
	                String[] paths = FileName.splitPath( path);
	                FileState fstate = ctx.getStateTable().findFileState( paths[0]);
	                
	                if ( fstate == null) {

	                	// Check if the path exists
	                	
	                	if ( fileExists( sess, tree, paths[0]) == FileStatus.DirectoryExists)
	                	{
	                		// Create the file state and add any pseudo files
	                		
	                		fstate = ctx.getStateTable().findFileState( paths[0], true, true);
	                		
	                		fstate.setFileStatus( FileStatus.DirectoryExists);
	                		getPseudoFileInterface( ctx).addPseudoFilesToFolder( sess, tree, paths[0]);
	                		
	                		// Debug
	                		
	                		if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_PSEUDO))
	                			logger.debug( "Added file state for pseudo files folder (open) - " + paths[0]);
	                	}
	                }
	                else if ( fstate.hasPseudoFiles() == false)
	                {
	                	// Add pseudo files for the parent folder
	                	
                		getPseudoFileInterface( ctx).addPseudoFilesToFolder( sess, tree, paths[0]);
                		
                		// Debug
                		
                		if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_PSEUDO))
                			logger.debug( "Added pseudo files for folder (open) - " + paths[0]);
	                }
	            	
	                // Check if the path is to a pseudo file
	                
	                PseudoFile pfile = getPseudoFileInterface(ctx).getPseudoFile( sess, tree, params.getPath());
	                if ( pfile != null)
	                {
	                    // Create a network file to access the pseudo file data
	                    
	                    return pfile.getFile( params.getPath());
	                }
	                else
	                {
	                	// Failed to find pseudo file
	                	
	                	if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_PSEUDO))
	                		logger.debug( "Failed to find pseudo file (open) - " + params.getPath());
	                }
            	}
            }
            
            // Not a pseudo file, try and open a normal file/folder node
            
            NodeRef nodeRef = getNodeForPath(tree, params.getPath());
            
            // Check permissions on the file/folder node
            //
            // Check for read access
            
            if ( params.hasAccessMode(AccessMode.NTRead) &&
                    permissionService.hasPermission(nodeRef, PermissionService.READ) == AccessStatus.DENIED)
                throw new AccessDeniedException("No read access to " + params.getFullPath());
                
            // Check for write access
            
            if ( params.hasAccessMode(AccessMode.NTWrite) &&
                    permissionService.hasPermission(nodeRef, PermissionService.WRITE) == AccessStatus.DENIED)
                throw new AccessDeniedException("No write access to " + params.getFullPath());
            
            // Check for delete access
            
//            if ( params.hasAccessMode(AccessMode.NTDelete) &&
//                    permissionService.hasPermission(nodeRef, PermissionService.DELETE) == AccessStatus.DENIED)
//                throw new AccessDeniedException("No delete access to " + params.getFullPath());

            // Check if the file has a lock
            
            String lockTypeStr = (String) nodeService.getProperty( nodeRef, ContentModel.PROP_LOCK_TYPE);
            
            if ( params.hasAccessMode(AccessMode.NTWrite) && lockTypeStr != null)
                throw new AccessDeniedException("File is locked, no write access to " + params.getFullPath());
            
            //  Check if there is a file state for the file

            FileState fstate = null;
            
            if ( ctx.hasStateTable())
            {
                // Check if there is a file state for the file

                fstate = ctx.getStateTable().findFileState( params.getPath());
            
                if ( fstate != null)
                {                
                    // Check if the file exists
                    
                    if ( fstate.exists() == false)
                        throw new FileNotFoundException();
                }
                else {
                	
                	// Create a file state for the path
                	
                    fstate = ctx.getStateTable().findFileState( params.getPath(), false, true);
                }                	
                    
            	// Check if the current file open allows the required shared access
            	
            	boolean nosharing = false;

            	// TEST
            	
        		if ( params.getAccessMode() == AccessMode.NTFileGenericExecute && params.getPath().toLowerCase().endsWith( ".exe") == false) {
        			
            		// DEBUG
            		
            		if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE)) {
            			logger.debug( "Execute access mode, path" + params.getPath());
            			logger.debug( "  Fstate=" + fstate);
            		}
        		
        			throw new AccessDeniedException("Invalid access mode");
        		}
        		
            	if ( fstate.getOpenCount() > 0) {
            		
            		// Check for impersonation security level from the original process that opened the file
            		
            		if ( params.getSecurityLevel() == WinNT.SecurityImpersonation && params.getProcessId() == fstate.getProcessId())
            			nosharing = false;

            		// Check if the caller wants read access, check the sharing mode
            		// Check if the caller wants write access, check if the sharing mode allows write
            		
                	else if ( params.isReadOnlyAccess() && (fstate.getSharedAccess() & SharingMode.READ) != 0)
                		nosharing = false;
            		
            		// Check if the caller wants write access, check the sharing mode
            		
                	else if (( params.isReadWriteAccess() || params.isWriteOnlyAccess()) && (fstate.getSharedAccess() & SharingMode.WRITE) == 0)
                	{
                		// DEBUG
                		
                		if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                			logger.debug("Sharing mode disallows write access path=" + params.getPath());
                		
                		// Access not allowed
                		
                		throw new AccessDeniedException( "Sharing mode (write)");
                	}
                	
            		// Check if the file has been opened for exclusive access
            		
            		else if ( fstate.getSharedAccess() == SharingMode.NOSHARING)
            			nosharing = true;
            		
            		// Check if the required sharing mode is allowed by the current file open
            		
            		else if ( ( fstate.getSharedAccess() & params.getSharedAccess()) != params.getSharedAccess())
            			nosharing = true;
            		
            		// Check if the caller wants exclusive access to the file
            		
                	else if ( params.getSharedAccess() == SharingMode.NOSHARING)
                		nosharing = true;
            		
            	}
            	
            	// Check if the file allows shared access
            	
            	if ( nosharing == true)
                {
                	if ( params.getPath().equals( "\\") == false) {
                		
                		// DEBUG
                		
                		if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                			logger.debug("Sharing violation path=" + params.getPath() + ", sharing=0x" + Integer.toHexString(fstate.getSharedAccess()));
                	
                		// File is locked by another user
                		
                		throw new FileSharingException("File already open, " + params.getPath());
                	}
                }
            	
            	// Update the file sharing mode and process id, if this is the first file open
            	
            	fstate.setSharedAccess( params.getSharedAccess());
            	fstate.setProcessId( params.getProcessId());
            	
            	// DEBUG
            	
            	if ( logger.isDebugEnabled() && fstate.getOpenCount() == 0 && ctx.hasDebug(AlfrescoContext.DBG_FILE))
            		logger.debug("Path " + params.getPath() + ", sharing=0x" + Integer.toHexString(params.getSharedAccess()) + ", PID=" + params.getProcessId());
            }
            
            // Check if the node is a link node
            
            NodeRef linkRef = (NodeRef) nodeService.getProperty(nodeRef, ContentModel.PROP_LINK_DESTINATION);
            AlfrescoNetworkFile netFile = null;
            
            if ( linkRef == null)
            {
            	// Check if the file is already opened by this client/process
            	
            	if ( tree.openFileCount() > 1) {
            	
            		// Search the open file table for this session/virtual circuit
            		
            		int idx = 0;
            		
            		while ( idx < tree.getFileTableLength() && netFile == null) {
            			
            			// Get the current file from the open file table
            			
            			NetworkFile curFile = tree.findFile( idx);
            			if ( curFile != null && curFile instanceof ContentNetworkFile) {
            				
            				// Check if the file is the same path and process id
            				
            				ContentNetworkFile contentFile = (ContentNetworkFile) curFile;
            				if ( contentFile.getProcessId() == params.getProcessId() &&
            						contentFile.getFullName().equalsIgnoreCase( params.getFullPath())) {
            					
            					// Check that the access mode is the same
            					
            					if (( params.isReadWriteAccess() && contentFile.getGrantedAccess() == NetworkFile.READWRITE) ||
            							( params.isReadOnlyAccess() && contentFile.getGrantedAccess() == NetworkFile.READONLY)) {
            						
	            					// Found a match, re-use the open file
	            					
	            					netFile = contentFile;
	            					
	            					// Increment the file open count, last file close will actually close the file/stream
	            					
	            					contentFile.incrementOpenCount();
	
	            					// DEBUG
	            	            	
	            	            	if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
	            	            		logger.debug("Re-use existing file open Path " + params.getPath() + ", PID=" + params.getProcessId());
            					}
            				}
            			}
            			
            			// Update the file table index
            			
            			idx++;
            		}
            	}
            	
	            // Create the network file, if we could not match an existing file open
	            
            	if ( netFile == null)
            		netFile = ContentNetworkFile.createFile(nodeService, contentService, mimetypeService, cifsHelper, nodeRef, params);
            }
            else
            {
                // Get the CIFS server name
                
                String srvName = null;
                SMBServer cifsServer = (SMBServer) sess.getServer().getConfiguration().findServer( "CIFS");
                
                if ( cifsServer != null)
                {
                    // Use the CIFS server name in the URL
                
                    srvName = cifsServer.getServerName();
                }
                else
                {
                    // Use the local server name in the URL
                  
                    srvName = InetAddress.getLocalHost().getHostName();
                }
                
              	// Convert the target node to a path, convert to URL format
              	
              	String path = getPathForNode( tree, linkRef);
              	path = path.replace( FileName.DOS_SEPERATOR, '/');
            	
                // Build the URL file data
                
                StringBuilder urlStr = new StringBuilder();
            
                urlStr.append("[InternetShortcut]\r\n");
                urlStr.append("URL=file://");
                urlStr.append( srvName);
                urlStr.append("/");
                urlStr.append( tree.getSharedDevice().getName());
                urlStr.append( path);
                urlStr.append("\r\n");
    
                // Create the in memory pseudo file for the URL link
                
                byte[] urlData = urlStr.toString().getBytes();
                
                // Get the file information for the link node
                
                FileInfo fInfo = cifsHelper.getFileInformation( nodeRef);

                // Set the file size to the actual data length
                
                fInfo.setFileSize( urlData.length);
                
                // Create the network file using the in-memory file data
                
                netFile = new LinkMemoryNetworkFile( fInfo.getFileName(), urlData, fInfo, nodeRef);
                netFile.setFullName( params.getPath());
            }
            
            // Generate a file id for the file
            
            if ( netFile != null)
            	netFile.setFileId( params.getPath().hashCode());
            
            // If the file has been opened for overwrite then truncate the file to zero length, this will
            // also prevent the existing content data from being copied to the new version of the file
            
            if ( params.isOverwrite() && netFile != null)
            {
                // Truncate the file to zero length
                
                netFile.truncateFile( 0L);
            }
            
            // Create a file state for the open file
            
            if ( ctx.hasStateTable())
            {
                if ( fstate  == null)
                    fstate = ctx.getStateTable().findFileState(params.getPath(), params.isDirectory(), true);
            
                // Update the file state, cache the node
                
                fstate.incrementOpenCount();
                fstate.setNodeRef(nodeRef);
                
                // Store the state with the file
                
                netFile.setFileState( fstate);
                
                // Set the file access date/time, if available
                
                if ( fstate.hasAccessDateTime())
                	netFile.setAccessDate( fstate.getAccessDateTime());
            }
            
            // Debug
            
            if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Opened network file: path=" + params.getPath() + " file open parameters=" + params + " network file=" + netFile);

            // Return the network file
            
            return netFile;
        }
        catch (org.alfresco.repo.security.permissions.AccessDeniedException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Open file - access denied, " + params.getFullPath());
            
            // Convert to a filesystem access denied status
            
            throw new AccessDeniedException("Open file " + params.getFullPath());
        }
        catch (AlfrescoRuntimeException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Open file error", ex);
            
            // Convert to a general I/O exception
            
            throw new IOException("Open file " + params.getFullPath());
        }
    }
    
    /**
     * Create a new file on the file system.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param params File create parameters
     * @return NetworkFile
     * @exception java.io.IOException If an error occurs.
     */
    public NetworkFile createFile(SrvSession sess, TreeConnection tree, FileOpenParams params) throws IOException
    {
        // Create the transaction
        
    	beginWriteTransaction( sess);
        ContentContext ctx = (ContentContext) tree.getContext();
        
        try
        {
            // Get the device root

            NodeRef deviceRootNodeRef = ctx.getRootNode();
            
            String path = params.getPath();
            FileState parentState = null;
            
            // If the state table is available then try to find the parent folder node for the new file
            // to save having to walk the path
          
            if ( ctx.hasStateTable())
            {
                // See if the parent folder has a file state, we can avoid having to walk the path
                
                String[] paths = FileName.splitPath(path);
                if ( paths[0] != null && paths[0].length() > 1)
                {
                    // Find the node ref for the folder being searched
                    
                    NodeRef nodeRef = getNodeForPath(tree, paths[0]);
                    
                    if ( nodeRef != null)
                    {
                        deviceRootNodeRef = nodeRef;
                        path              = paths[1];
                        
                        // DEBUG
                        
                        if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                            logger.debug("Create file using cached noderef for path " + paths[0]);
                    }
                    
                    // Get the file state for the parent folder
                    
                    parentState = getStateForPath(tree, paths[0]);
                    if ( parentState == null && ctx.hasStateTable())
                    	parentState = ctx.getStateTable().findFileState( paths[0], true, true);
                }
            }
            
            // Create it - the path will be created, if necessary
            
            NodeRef nodeRef = cifsHelper.createNode(deviceRootNodeRef, path, true);
            nodeService.addAspect(nodeRef, ContentModel.ASPECT_NO_CONTENT, null);
            
            // Create the network file
            
            ContentNetworkFile netFile = ContentNetworkFile.createFile(nodeService, contentService, mimetypeService, cifsHelper, nodeRef, params);
            
            // Always allow write access to a newly created file
            
            netFile.setGrantedAccess(NetworkFile.READWRITE);
            
            // Set the owner process id for this open file
            
            netFile.setProcessId( params.getProcessId());
            
            // Truncate the file so that the content stream is created
            
            netFile.truncateFile( 0L);

            // Generate a file id for the file
            
            if ( netFile != null)
            	netFile.setFileId( params.getPath().hashCode());
            
            // Add a file state for the new file/folder
            
            if ( ctx.hasStateTable())
            {
                FileState fstate = ctx.getStateTable().findFileState(params.getPath(), false, true);
                if ( fstate != null)
                {
                    // Save the file sharing mode, needs to be done before the open count is incremented
                    
                    fstate.setSharedAccess( params.getSharedAccess());
                    fstate.setProcessId( params.getProcessId());
                    
                    // Indicate that the file is open
    
                    fstate.setFileStatus(FileStateStatus.FileExists);
                    fstate.incrementOpenCount();
                    fstate.setNodeRef(nodeRef);
                    
                    // Store the file state with the file
                    
                    netFile.setFileState( fstate);
                    
                    // DEBUG
                    
                    if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                        logger.debug("Create file, state=" + fstate);
                }
                
                // Update the parent folder file state
                
                if ( parentState != null)
                	parentState.updateModifyDateTime();
            }
            
            // Debug
            
            if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Created file: path=" + path + " file open parameters=" + params + " node=" + nodeRef + " network file=" + netFile);

            // Return the new network file
            
            return netFile;
        }
        catch (org.alfresco.repo.security.permissions.AccessDeniedException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Create file - access denied, " + params.getFullPath());
            
            // Convert to a filesystem access denied status
            
            throw new AccessDeniedException("Create file " + params.getFullPath());
        }
        catch (ContentIOException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Create file - content I/O error, " + params.getFullPath());
            
            // Convert to a filesystem disk full status
            
            throw new DiskFullException("Create file " + params.getFullPath());
        }
        catch (AlfrescoRuntimeException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Create file error", ex);
            
            // Convert to a general I/O exception
            
            throw new IOException("Create file " + params.getFullPath());
        }
        
    }

    /**
     * Create a new directory on this file system.
     * 
     * @param sess Server session
     * @param tree Tree connection.
     * @param params Directory create parameters
     * @exception java.io.IOException If an error occurs.
     */
    public void createDirectory(SrvSession sess, TreeConnection tree, FileOpenParams params) throws IOException
    {
        // Create the transaction
        
    	beginWriteTransaction( sess);
        ContentContext ctx = (ContentContext) tree.getContext();
        
        try
        {
            // get the device root
            
            NodeRef deviceRootNodeRef = ctx.getRootNode();
            
            String path = params.getPath(); 
            FileState parentState = null;
            
            // If the state table is available then try to find the parent folder node for the new folder
            // to save having to walk the path
          
            if ( ctx.hasStateTable())
            {
                // See if the parent folder has a file state, we can avoid having to walk the path
                
                String[] paths = FileName.splitPath(path);
                if ( paths[0] != null && paths[0].length() > 1)
                {
                    // Find the node ref for the folder being searched
                    
                    NodeRef nodeRef = getNodeForPath(tree, paths[0]);
                    
                    if ( nodeRef != null)
                    {
                        deviceRootNodeRef = nodeRef;
                        path              = paths[1];
                        
                        // DEBUG
                        
                        if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                            logger.debug("Create file using cached noderef for path " + paths[0]);
                    }
                    
                    // Get the file state for the parent folder
                    
                    parentState = getStateForPath(tree, paths[0]);
                    if ( parentState == null && ctx.hasStateTable())
                    	parentState = ctx.getStateTable().findFileState( paths[0], true, true);
                }
            }
            
            // Create it - the path will be created, if necessary
            
            NodeRef nodeRef = cifsHelper.createNode(deviceRootNodeRef, path, false);

            // Add a file state for the new folder
            
            if ( ctx.hasStateTable())
            {
                FileState fstate = ctx.getStateTable().findFileState( params.getPath(), true, true);
                if ( fstate != null)
                {
                    // Indicate that the file is open
    
                    fstate.setFileStatus(FileStateStatus.FolderExists);
                    fstate.setNodeRef(nodeRef);
                    
                    // DEBUG
                    
                    if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                        logger.debug("Create folder, state=" + fstate);
                }
                
                // Update the parent folder file state
                
                if ( parentState != null)
                	parentState.updateModifyDateTime();
            }
            
            // Debug
            
            if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Created directory: path=" + path + " file open params=" + params + " node=" + nodeRef);
        }
        catch (org.alfresco.repo.security.permissions.AccessDeniedException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Create directory - access denied, " + params.getFullPath());
            
            // Convert to a filesystem access denied status
            
            throw new AccessDeniedException("Create directory " + params.getFullPath());
        }
        catch (AlfrescoRuntimeException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Create directory error", ex);
            
            // Convert to a general I/O exception
            
            throw new IOException("Create directory " + params.getFullPath());
        }
    }

    /**
     * Delete the directory from the filesystem.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param dir Directory name.
     * @exception java.io.IOException The exception description.
     */
    public void deleteDirectory(SrvSession sess, TreeConnection tree, String dir) throws IOException
    {
        // Create the transaction
        
    	beginWriteTransaction( sess);
        
        // get the device root
        
        ContentContext ctx = (ContentContext) tree.getContext();
        NodeRef deviceRootNodeRef = ctx.getRootNode();
        
        try
        {
            // Get the node for the folder
        	
            NodeRef nodeRef = cifsHelper.getNodeRef(deviceRootNodeRef, dir);
            if (fileFolderService.exists(nodeRef))
            {
            	// Check if the folder is empty
            	
            	if ( cifsHelper.isFolderEmpty( nodeRef) == true) {
            		
	           		// Delete the folder node
	
	           		fileFolderService.delete(nodeRef);
	                
	                // Remove the file state
		                
	                if ( ctx.hasStateTable())
	                {
	                	// Remove the file state
	                
	                    ctx.getStateTable().removeFileState(dir);
	                
	                    // Update, or create, a parent folder file state
	                    
	                    String[] paths = FileName.splitPath(dir);
	                    if ( paths[0] != null && paths[0].length() > 1)
	                    {
	                        // Get the file state for the parent folder
	                        
	                        FileState parentState = getStateForPath(tree, paths[0]);
	                        if ( parentState == null && ctx.hasStateTable())
	                        	parentState = ctx.getStateTable().findFileState( paths[0], true, true);
	
	                        // Update the modification timestamp
	                        
	                        parentState.updateModifyDateTime();
	                    }
	                }
            	}
            	else
            		throw new DirectoryNotEmptyException( dir);
            }
            
            // Debug
            
            if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Deleted directory: directory=" + dir + " node=" + nodeRef);
        }
        catch (FileNotFoundException e)
        {
            // Debug
        	
            if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Delete directory - file not found, " + dir);
        }
        catch (org.alfresco.repo.security.permissions.AccessDeniedException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Delete directory - access denied, " + dir);
            
            // Convert to a filesystem access denied status
            
            throw new AccessDeniedException("Delete directory " + dir);
        }
        catch (AlfrescoRuntimeException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Delete directory", ex);
            
            // Convert to a general I/O exception
            
            throw new IOException("Delete directory " + dir);
        }
    }

    /**
     * Flush any buffered output for the specified file.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param file Network file context.
     * @exception java.io.IOException The exception description.
     */
    public void flushFile(SrvSession sess, TreeConnection tree, NetworkFile file) throws IOException
    {
    	// Debug
    	
    	ContentContext ctx = (ContentContext) tree.getContext();
    	
    	if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILEIO))
    		logger.debug("Flush file=" + file.getFullName());
    	
        // Flush the file data
        
        file.flushFile();
    }

    /**
     * Close the file.
     * 
     * @param sess Server session
     * @param tree Tree connection.
     * @param param Network file context.
     * @exception java.io.IOException If an error occurs.
     */
    public void closeFile(SrvSession sess, TreeConnection tree, NetworkFile file) throws IOException
    {
        // Create the transaction
        
    	beginWriteTransaction( sess);
        
        // Get the associated file state
        
        ContentContext ctx = (ContentContext) tree.getContext();
        
        if ( file instanceof ContentNetworkFile) {
        	
        	// Decrement the file open count
        	
        	ContentNetworkFile contentFile = (ContentNetworkFile) file;
        	
        	if ( contentFile.decrementOpenCount() > 0) {
        		
        		// DEBUG
                
                if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                    logger.debug("Deferred file close, path=" + file.getFullName() + ", openCount=" + contentFile.getOpenCount());
                
                // Defer the file close to the last reference
                
                return;
        	}
        }
        
        if ( ctx.hasStateTable())
        {
            FileState fstate = ctx.getStateTable().findFileState(file.getFullName());
            if ( fstate != null) {
            	
            	// If the file open count is now zero then reset the stored sharing mode
            
                if ( fstate.decrementOpenCount() == 0)
                	fstate.setSharedAccess( SharingMode.READWRITE + SharingMode.DELETE);
            
	            // Check if there is a cached modification timestamp to be written out
	            
	            if ( file.hasDeleteOnClose() == false && fstate.hasModifyDateTime() && fstate.hasNodeRef()) {
	            	
	            	// Update the modification date on the file/folder node
	
	            	Date modifyDate = new Date( fstate.getModifyDateTime());
	            	nodeService.setProperty( fstate.getNodeRef(), ContentModel.PROP_MODIFIED, modifyDate);
	
	            	// Debug
	                
	                if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
	                    logger.debug("Updated modifcation timestamp, " + file.getFullName() + ", modTime=" + modifyDate);
	            }
            }
        }
        
        // Check if there is a quota manager enabled
        
        long fileSize = 0L;
        
        if ( ctx.hasQuotaManager() && file.hasDeleteOnClose()) {
            
            // Make sure the content stream has been opened, to get the current file size
            
            if ( file instanceof ContentNetworkFile) {
                ContentNetworkFile contentFile = (ContentNetworkFile) file;
                if ( contentFile.hasContent() == false)
                    contentFile.openContent( false, false);
                
                // Save the current file size
                
                fileSize = contentFile.getFileSize();
            }
        }
        
        // Defer to the network file to close the stream and remove the content
           
        file.closeFile();
        
        // Remove the node if marked for delete
        
        if (file.hasDeleteOnClose())
        {
            // Check if the file is a noderef based file
            
            if ( file instanceof NodeRefNetworkFile)
            {
                NodeRefNetworkFile nodeNetFile = (NodeRefNetworkFile) file;
                NodeRef nodeRef = nodeNetFile.getNodeRef();
                
                // We don't know how long the network file has had the reference, so check for existence
                
                if (fileFolderService.exists(nodeRef))
                {
                    try
                    {
                    	boolean isVersionable = nodeService.hasAspect( nodeRef, ContentModel.ASPECT_VERSIONABLE);
                    	
                    	try
                    	{
	                        // Delete the file
	                        
	                        fileFolderService.delete(nodeRef);
	                        
	                        // Check if there is a quota manager enabled, release space back to the user quota
	                        
	                        if ( ctx.hasQuotaManager())
	                            ctx.getQuotaManager().releaseSpace(sess, tree, file.getFileId(), file.getFullName(), fileSize);
                    	}
                    	catch ( Exception ex)
                    	{
                    		if ( logger.isWarnEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                    			logger.warn("Error during delete on close, " + file.getFullName(), ex);
                    	}
    
                        // Set the file state to indicate a delete on close
                        
                        if ( ctx.hasStateTable())
                        {
                        	if ( isVersionable == true) {
                        		
	                        	// Get, or create, the file state
	                        	
	                        	FileState fState = ctx.getStateTable().findFileState(file.getFullName(), false, true);
	                        	
	                        	// Indicate that the file was deleted via a delete on close request
	                        	
	                        	fState.setFileStatus(FileStateStatus.DeleteOnClose);
	
	                        	// Make sure the file state is cached for a short while, save the noderef details
	        	                
	        	                fState.setExpiryTime(System.currentTimeMillis() + FileState.RenameTimeout);
	        	                fState.setNodeRef(nodeRef);
                        	}
                        	else {
                        		
                        		// Remove the file state
                        		
                        		ctx.getStateTable().removeFileState( file.getFullName());
                        	}
                        }
                    }
                    catch (org.alfresco.repo.security.permissions.AccessDeniedException ex)
                    {
                        // Debug
                        
                        if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                            logger.debug("Delete on close - access denied, " + file.getFullName());
                        
                        // Convert to a filesystem access denied exception
                        
                        throw new AccessDeniedException("Delete on close " + file.getFullName());
                    }
                }
            }
            else if ( file instanceof PseudoNetworkFile ||
                      file instanceof MemoryNetworkFile)
            {
                // Delete the pseudo file
                
                if ( hasPseudoFileInterface(ctx))
                {
                    // Delete the pseudo file
                    
                    getPseudoFileInterface(ctx).deletePseudoFile( sess, tree, file.getFullName());
                }
            }
        }
        
        // DEBUG
        
        if (logger.isDebugEnabled() && (ctx.hasDebug(AlfrescoContext.DBG_FILE) || ctx.hasDebug(AlfrescoContext.DBG_RENAME)))
            logger.debug("Closed file: network file=" + file + " delete on close=" + file.hasDeleteOnClose());
    }

    /**
     * Delete the specified file.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param file NetworkFile
     * @exception java.io.IOException The exception description.
     */
    public void deleteFile(SrvSession sess, TreeConnection tree, String name) throws IOException
    {
        // Create the transaction
        
    	beginWriteTransaction( sess);
        
        // Get the device context
        
        ContentContext ctx = (ContentContext) tree.getContext();
        
        try
        {
            // Check if there is a quota manager enabled, if so then we need to save the current file size
            
            QuotaManager quotaMgr = ctx.getQuotaManager();
            FileInfo fInfo = null;
            
            if ( quotaMgr != null) {
                
                // Get the size of the file being deleted
                
                fInfo = getFileInformation( sess, tree, name);
            }
            
            // Get the node
        	
            NodeRef nodeRef = getNodeForPath(tree, name);
            if (fileFolderService.exists(nodeRef))
            {
            	// Check if the node is versionable
            	
            	boolean isVersionable = nodeService.hasAspect( nodeRef, ContentModel.ASPECT_VERSIONABLE);
            	
                fileFolderService.delete(nodeRef);
                
                // Remove the file state
                
                if ( ctx.hasStateTable())
                {
                	// Check if the node is versionable, cache the node details for a short while
                	
                	if ( isVersionable == true) {
                		
	                    // Make sure the file state is cached for a short while, a new file may be renamed to the same name
	                	// in which case we can connect the file to the previous version history
	
	                	FileState delState = ctx.getStateTable().findFileState( name, false, true);
	                    
	                	delState.setExpiryTime(System.currentTimeMillis() + FileState.DeleteTimeout);
	                    delState.setFileStatus(FileStateStatus.DeleteOnClose);
	                    delState.setNodeRef( nodeRef);
                	}
                	else {
                		
                		// Remove the file state
                		
                		ctx.getStateTable().removeFileState( name);
                	}
                	
                    // Update, or create, a parent folder file state
                    
                    String[] paths = FileName.splitPath(name);
                    if ( paths[0] != null && paths[0].length() > 1)
                    {
                        // Get the file state for the parent folder
                        
                        FileState parentState = getStateForPath(tree, paths[0]);
                        if ( parentState == null && ctx.hasStateTable())
                        	parentState = ctx.getStateTable().findFileState( paths[0], true, true);

                        // Update the modification timestamp
                        
                        parentState.updateModifyDateTime();
                    }
                }
                
                // Release the space back to the users quota
                
                if ( quotaMgr != null)
                    quotaMgr.releaseSpace( sess, tree, fInfo.getFileId(), name, fInfo.getSize());
            }
            
            // Debug
            
            if (logger.isDebugEnabled() && (ctx.hasDebug(AlfrescoContext.DBG_FILE) || ctx.hasDebug(AlfrescoContext.DBG_RENAME)))
                logger.debug("Deleted file: " + name + ", node=" + nodeRef);
        }
        catch (NodeLockedException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Delete file - access denied (locked)");
            
            // Convert to a filesystem access denied status
            
            throw new AccessDeniedException("Delete " + name);
        }
        catch (org.alfresco.repo.security.permissions.AccessDeniedException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Delete file - access denied");
            
            // Convert to a filesystem access denied status
            
            throw new AccessDeniedException("Delete " + name);
        }
        catch (AlfrescoRuntimeException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILE))
                logger.debug("Delete file error", ex);
            
            // Convert to a general I/O exception
            
            throw new IOException("Delete file " + name);
        }
    }

    /**
     * Rename the specified file.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param oldName java.lang.String
     * @param newName java.lang.String
     * @exception java.io.IOException The exception description.
     */
    public void renameFile(SrvSession sess, TreeConnection tree, String oldName, String newName) throws IOException
    {
        // Create the transaction

        beginWriteTransaction( sess);
        
        // Get the device context
        
        ContentContext ctx = (ContentContext) tree.getContext();
        
        // DEBUG
        
        if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
            logger.debug("Rename oldName=" + oldName + ", newName=" + newName);
        
        try
        {
            // Get the file/folder to move
            
            NodeRef nodeToMoveRef = getNodeForPath(tree, oldName);
            
            // Check if the node is a link node

            if ( nodeToMoveRef != null && nodeService.getProperty(nodeToMoveRef, ContentModel.PROP_LINK_DESTINATION) != null)
                throw new AccessDeniedException("Cannot rename link nodes");
            
            // Get the new target folder - it must be a folder
            
            String[] splitPaths = FileName.splitPath(newName);
            NodeRef targetFolderRef = getNodeForPath(tree, splitPaths[0]);
            String name = splitPaths[1];

            // Check if this is a rename within the same folder
            
            String[] oldPaths = FileName.splitPath( oldName);
            
            boolean sameFolder = false;
            if ( splitPaths[0].equalsIgnoreCase( oldPaths[0]))
                sameFolder = true;
            
            // Get the file state for the old file, if available
            
            FileState oldState = ctx.getStateTable().findFileState( oldName);
            
            // Check if we are renaming a folder, or the rename is to a different folder
            
            boolean isFolder = cifsHelper.isDirectory( nodeToMoveRef);
            
            if ( isFolder == true || sameFolder == false) {
                
                // Update the old file state
                
                if ( oldState != null)
                {
                    // Update the file state index to use the new name
                    
                    ctx.getStateTable().renameFileState(newName, oldState);
                }
                
                // Rename or move the file/folder
                
                if ( sameFolder == true)
                    cifsHelper.rename(nodeToMoveRef, name);
                else
                    cifsHelper.move(nodeToMoveRef, targetFolderRef, name);
                
                // DEBUG
                
                if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                    logger.debug("  Renamed " + (isFolder ? "folder" : "file") + " using " + (sameFolder ? "rename" : "move"));
            }
            else {
                
                // Rename a file within the same folder
                //
                // Check if the target file already exists
                
                int newExists = fileExists( sess, tree, newName);
                FileState newState = ctx.getStateTable().findFileState( newName, false, true);
                
                NodeRef targetNodeRef = null;
                
                boolean isFromVersionable = nodeService.hasAspect( nodeToMoveRef, ContentModel.ASPECT_VERSIONABLE);
                
                if ( newExists == FileStatus.FileExists) {
                    
                    // Use the existing file as the target node
                    
                    targetNodeRef = getNodeForPath( tree, newName);
                }
                else {
                    
                    // Check if the target has a renamed or delete-on-close state
                    
                    if ( newState.getFileStatus() == FileStateStatus.Renamed) {
                        
                        // DEBUG
                        
                        if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                            logger.debug("  Using renamed node, " + newState);
                        
                        // Use the renamed node to clone aspects/state
                        
                        cloneNodeAspects( name, newState.getNodeRef(), nodeToMoveRef, ctx);
                    }
                    else if ( newState.getFileStatus() == FileStateStatus.DeleteOnClose) {
                        
                        // DEBUG
                        
                        if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                            logger.debug("  Restoring delete-on-close node, " + newState);
                        
                        // Restore the deleted node so we can relink the new version to the old history/properties
                        
                        NodeRef archivedNode = getNodeArchiveService().getArchivedNode( newState.getNodeRef());
                        
                        // DEBUG
                        
                        if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                            logger.debug("  Found archived node " + archivedNode);
                        
                        if ( archivedNode != null && getNodeService().exists( archivedNode))
                        {
                            // Restore the node
                            
                            targetNodeRef = getNodeService().restoreNode( archivedNode, null, null, null);
                            
                            // DEBUG
                            
                            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                                logger.debug("  Restored node " + targetNodeRef);
                            
                            // Check if the deleted file had a linked node, due to a rename
                            
                            if ( newState.hasLinkNode() && nodeService.exists( newState.getLinkNode())) {
                                
                                // Clone aspects from the linked node onto the restored node
                                
                                cloneNodeAspects( name, newState.getLinkNode(), targetNodeRef, ctx);

                                // DEBUG
                                
                                if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME)) {
                                    logger.debug("  Moved aspects from linked node " + newState.getLinkNode());

                                    // Check if the node is a working copy
                                    
                                    if ( nodeService.hasAspect( targetNodeRef, ContentModel.ASPECT_WORKING_COPY)) {
                                        
                                        // Check if the main document is still locked
                                        
                                        NodeRef mainNodeRef = (NodeRef) nodeService.getProperty( targetNodeRef, ContentModel.PROP_COPY_REFERENCE);
                                        if ( mainNodeRef != null) {
                                            LockType lockTyp = lockService.getLockType( mainNodeRef);
                                            logger.debug("  Main node ref lock type = " + lockTyp);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Check if the node being renamed is versionable
                    
                    else if ( isFromVersionable == true) {
                        
                        // Create a new node for the target
                        
                        targetNodeRef = cifsHelper.createNode(ctx.getRootNode(), newName, true);
                        
                        // DEBUG
                        
                        if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                            logger.debug("  Created new node for " + newName);

                        // Copy aspects from the original file
                        
                        cloneNodeAspects( name, nodeToMoveRef, targetNodeRef, ctx);
                    }
                }

                // If the original or target nodes are not versionable then just use a standard rename of the node
                
                if ( isFromVersionable == false &&
                        ( targetNodeRef == null || nodeService.hasAspect( targetNodeRef, ContentModel.ASPECT_VERSIONABLE) == false)) {

                    // Rename the file/folder
                    
                    cifsHelper.rename(nodeToMoveRef, name);
                    
                    // Mark the new file as existing
                    
                    newState.setFileStatus( FileStatus.FileExists);
                    newState.setNodeRef( nodeToMoveRef);

                    // Make sure the old file state is cached for a short while, the file may not be open so the
                    // file state could be expired
                    
                    oldState.setExpiryTime(System.currentTimeMillis() + FileState.DeleteTimeout);
                    
                    // Indicate that this is a renamed file state, set the node ref of the file that was renamed
                    
                    oldState.setFileStatus(FileStateStatus.Renamed);
                    oldState.setNodeRef(nodeToMoveRef);

                    // DEBUG
                    
                    if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                        logger.debug("  Use standard rename for " + name + "(versionable=" + isFromVersionable + ", targetNodeRef=" + targetNodeRef + ")");
                }
                else {
                    
                    // Make sure we have a valid target node
                    
                    if ( targetNodeRef == null) {
                        
                        // DEBUG
                        
                        if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                            logger.debug("  No target node for rename");
                        
                        // Throw an error
                        
                        throw new AccessDeniedException( "No target node for file rename");
                    }
                    
                    // Copy content data from the old file to the new file
                    
                    copyContentData( sess, tree, nodeToMoveRef, targetNodeRef);
                    
                    // Mark the new file as existing
                    
                    newState.setFileStatus( FileStatus.FileExists);
                    newState.setNodeRef( targetNodeRef);
                    
                    // Delete the old file
                    
                    nodeService.deleteNode( nodeToMoveRef);
                    
                    // Make sure the old file state is cached for a short while, the file may not be open so the
                    // file state could be expired
                    
                    oldState.setExpiryTime(System.currentTimeMillis() + FileState.DeleteTimeout);
                    
                    // Indicate that this is a deleted file state, set the node ref of the file that was renamed
                    
                    oldState.setFileStatus(FileStateStatus.DeleteOnClose);
                    oldState.setNodeRef(nodeToMoveRef);
                    
                    // Link to the new node, a new file may be renamed into place, we need to transfer aspect/locks
                    
                    oldState.setLinkNode( targetNodeRef);
                    
                    // DEBUG
                    
                    if ( logger.isDebugEnabled() && ctx.hasDebug( AlfrescoContext.DBG_RENAME))
                        logger.debug("  Cached delete state for " + oldName);
                }   
            }
        }
        catch (org.alfresco.repo.security.permissions.AccessDeniedException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                logger.debug("Rename file - access denied, " + oldName);
            
            // Convert to a filesystem access denied status
            
            throw new AccessDeniedException("Rename file " + oldName);
        }
        catch (NodeLockedException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                logger.debug("Rename file", ex);
            
            // Convert to an filesystem access denied exception
            
            throw new AccessDeniedException("Node locked " + oldName);
        }
        catch (AlfrescoRuntimeException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                logger.debug("Rename file", ex);
            
            // Convert to a general I/O exception
            
            throw new AccessDeniedException("Rename file " + oldName);
        }
    }

    /**
     * Set file information
     * 
     * @param sess SrvSession
     * @param tree TreeConnection
     * @param name String
     * @param info FileInfo
     * @exception IOException
     */
    public void setFileInformation(SrvSession sess, TreeConnection tree, String name, FileInfo info) throws IOException
    {
        // Get the device context
        
        ContentContext ctx = (ContentContext) tree.getContext();
        
        try
        {
            // Check if pseudo files are enabled
            
            if ( hasPseudoFileInterface(ctx) &&
                    getPseudoFileInterface(ctx).isPseudoFile( sess, tree, name))
            {
                // Allow the file information to be changed
                
                return;
            }
            
            // Get the file/folder node
            
            NodeRef nodeRef = getNodeForPath(tree, name);
            FileState fstate = getStateForPath(tree, name);
            
            // Check permissions on the file/folder node
            
            if ( permissionService.hasPermission(nodeRef, PermissionService.WRITE) == AccessStatus.DENIED)
                throw new AccessDeniedException("No write access to " + name);
            
            if ( permissionService.hasPermission(nodeRef, PermissionService.DELETE) == AccessStatus.DENIED)
                throw new AccessDeniedException("No delete access to " + name);
            
            // Check if the file is being marked for deletion, if so then check if the file is locked
            
            if ( info.hasSetFlag(FileInfo.SetDeleteOnClose) && info.hasDeleteOnClose())
            {
            	// Start a transaction
            	
            	beginReadTransaction( sess);
            	
                // Check if the node is locked
                
                if ( nodeService.hasAspect( nodeRef, ContentModel.ASPECT_LOCKABLE))
                {
                    // Get the lock type, if any
                    
                    String lockTypeStr = (String) nodeService.getProperty( nodeRef, ContentModel.PROP_LOCK_TYPE);
                    
                    if ( lockTypeStr != null)
                        throw new AccessDeniedException("Node locked, cannot mark for delete");
                }
                
                // Get the node for the folder
            	
                if (fileFolderService.exists(nodeRef))
                {
                	// Check if it is a folder that is being deleted, make sure it is empty
                	
                	boolean isFolder = true;
                	
                	if ( fstate != null)
                		isFolder = fstate.isDirectory();
                	else {
                		ContentFileInfo cInfo = cifsHelper.getFileInformation( nodeRef);
                		if ( cInfo != null && cInfo.isDirectory() == false)
                			isFolder = false;
                	}

                	// Check if the folder is empty
                	
                	if ( isFolder == true && cifsHelper.isFolderEmpty( nodeRef) == false)
                		throw new DirectoryNotEmptyException( name);
                }
                
                // Update the change date/time
                
                if ( fstate != null)
                	fstate.updateChangeDateTime();
                
                // DEBUG
                
                if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                	logger.debug("Set deleteOnClose=true file=" + name);
            }
            
            // Set the creation date/time
            
            if ( info.hasSetFlag(FileInfo.SetCreationDate)) {
            	
                // Create the transaction

            	beginWriteTransaction( sess);
            	
            	// Set the creation date on the file/folder node
            	
            	Date createDate = new Date( info.getCreationDateTime());
            	nodeService.setProperty( nodeRef, ContentModel.PROP_CREATED, createDate);

                // Update the change date/time
                
                if ( fstate != null)
                	fstate.updateChangeDateTime();
                
            	// DEBUG
                
                if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                	logger.debug("Set creationDate=" + createDate + " file=" + name);
            }

            // Set the modification date/time
            
            if ( info.hasSetFlag(FileInfo.SetModifyDate)) {
            	
                // Create the transaction

            	beginWriteTransaction( sess);
            	
            	// Set the modification date on the file/folder node

            	Date modifyDate = new Date( info.getModifyDateTime());
            	nodeService.setProperty( nodeRef, ContentModel.PROP_MODIFIED, modifyDate);

                // Update the change date/time, clear the cached modification date/time
                
                if ( fstate != null) {
                	fstate.updateChangeDateTime();
                	fstate.updateModifyDateTime( 0L);
                }
                
            	// DEBUG
                
                if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                	logger.debug("Set modifyDate=" + modifyDate + " file=" + name);
            }
        }
        catch (org.alfresco.repo.security.permissions.AccessDeniedException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                logger.debug("Set file information - access denied, " + name);
            
            // Convert to a filesystem access denied status
            
            throw new AccessDeniedException("Set file information " + name);
        }
        catch (AlfrescoRuntimeException ex)
        {
            // Debug
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_INFO))
                logger.debug("Open file error", ex);
            
            // Convert to a general I/O exception
            
            throw new IOException("Set file information " + name);
        }
    }

    /**
     * Truncate a file to the specified size
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param file Network file details
     * @param size New file length
     * @exception java.io.IOException The exception description.
     */
    public void truncateFile(SrvSession sess, TreeConnection tree, NetworkFile file, long size) throws IOException
    {
        //  Keep track of the allocation/release size in case the file resize fails
        
        ContentContext ctx = (ContentContext) tree.getContext();
        
        long allocSize   = 0L;
        long releaseSize = 0L;
        
        //  Check if there is a quota manager

        QuotaManager quotaMgr = ctx.getQuotaManager();
              
        if ( ctx.hasQuotaManager()) {
          
            // Check if the file content has been opened, we need the content to be opened to get the
            // current file size
            
            if ( file instanceof ContentNetworkFile) {
                ContentNetworkFile contentFile = (ContentNetworkFile) file;
                if ( contentFile.hasContent() == false)
                    contentFile.openContent( false, false);
            }
            else
                throw new IOException("Invalid file class type, " + file.getClass().getName());
            
            //  Determine if the new file size will release space or require space allocating
          
            if ( size > file.getFileSize()) {
            
                //  Calculate the space to be allocated
            
                allocSize = size - file.getFileSize();
            
                //  Allocate space to extend the file
            
                quotaMgr.allocateSpace(sess, tree, file, allocSize);
            }
            else {
            
              //  Calculate the space to be released as the file is to be truncated, release the space if
              //  the file truncation is successful
            
              releaseSize = file.getFileSize() - size;
            }
        }
        
        //  Set the file length

        try {
            file.truncateFile(size);
        }
        catch (IOException ex) {
          
            //  Check if we allocated space to the file
          
            if ( allocSize > 0 && quotaMgr != null)
                quotaMgr.releaseSpace(sess, tree, file.getFileId(), null, allocSize);

            //  Rethrow the exception
          
            throw ex;       
        }
        
        //  Check if space has been released by the file resizing
        
        if ( releaseSize > 0 && quotaMgr != null)
            quotaMgr.releaseSpace(sess, tree, file.getFileId(), null, releaseSize);

        // Debug

        if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILEIO))
            logger.debug("Truncated file: network file=" + file + " size=" + size);
    }

    /**
     * Read a block of data from the specified file.
     * 
     * @param sess Session details
     * @param tree Tree connection
     * @param file Network file
     * @param buf Buffer to return data to
     * @param bufPos Starting position in the return buffer
     * @param siz Maximum size of data to return
     * @param filePos File offset to read data
     * @return Number of bytes read
     * @exception java.io.IOException The exception description.
     */
    public int readFile(
            SrvSession sess, TreeConnection tree, NetworkFile file,
            byte[] buffer, int bufferPosition, int size, long fileOffset) throws IOException
    {
        // Check if the file is a directory
        
        if(file.isDirectory())
            throw new AccessDeniedException();
            
    	// If the content channel is not open for the file then start a transaction
    	
        if ( file instanceof ContentNetworkFile)
        {
	    	ContentNetworkFile contentFile = (ContentNetworkFile) file;
	    	
	    	if ( contentFile.hasContent() == false)
	    		beginReadTransaction( sess);
        }
        
        // Read a block of data from the file
        
        int count = file.readFile(buffer, size, bufferPosition, fileOffset);
        
        if ( count == -1)
        {
            // Read count of -1 indicates a read past the end of file
            
            count = 0;
        }
        
        // Debug

        ContentContext ctx = (ContentContext) tree.getContext();
        
        if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILEIO))
            logger.debug("Read bytes from file: network file=" + file + " buffer size=" + buffer.length + " buffer pos=" + bufferPosition +
                    " size=" + size + " file offset=" + fileOffset + " bytes read=" + count);
        
        return count;
    }

    /**
     * Seek to the specified file position.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param file Network file.
     * @param pos Position to seek to.
     * @param typ Seek type.
     * @return New file position, relative to the start of file.
     */
    public long seekFile(SrvSession sess, TreeConnection tree, NetworkFile file, long pos, int typ) throws IOException
    {
  	  	// Check if the file is a directory
    	
		if ( file.isDirectory())
			throw new AccessDeniedException();
      
    	// If the content channel is not open for the file then start a transaction
    	
    	ContentNetworkFile contentFile = (ContentNetworkFile) file;
    	
    	if ( contentFile.hasContent() == false)
    		beginReadTransaction( sess);
    	
		// Set the file position

		return file.seekFile(pos, typ);
    }

    /**
     * Write a block of data to the file.
     * 
     * @param sess Server session
     * @param tree Tree connection
     * @param file Network file details
     * @param buf byte[] Data to be written
     * @param bufoff Offset within the buffer that the data starts
     * @param siz int Data length
     * @param fileoff Position within the file that the data is to be written.
     * @return Number of bytes actually written
     * @exception java.io.IOException The exception description.
     */
    public int writeFile(SrvSession sess, TreeConnection tree, NetworkFile file,
            byte[] buffer, int bufferOffset, int size, long fileOffset) throws IOException
    {
    	// If the content channel is not open for the file then start a transaction
    	
    	if ( file instanceof ContentNetworkFile)
    	{
	    	ContentNetworkFile contentFile = (ContentNetworkFile) file;
	    	
	    	if ( contentFile.hasContent() == false)
	    		beginReadTransaction( sess);
    	}

        //  Check if there is a quota manager
        
        ContentContext ctx = (ContentContext) tree.getContext();
        QuotaManager quotaMgr = ctx.getQuotaManager();
        long curSize = file.getFileSize();
        
        if ( quotaMgr != null) {
          
          //  Check if the file requires extending
          
          long extendSize = 0L;
          long endOfWrite = fileOffset + size;
          
          if ( endOfWrite > curSize) {
            
            //  Calculate the amount the file must be extended

            extendSize = endOfWrite - file.getFileSize();
            
            //  Allocate space for the file extend
            
            quotaMgr.allocateSpace(sess, tree, file, extendSize);
          }
        }
    	
    	// Write to the file
        
        file.writeFile(buffer, size, bufferOffset, fileOffset);

        // Check if the file size was reduced by the write, may have been extended previously
        
        if ( quotaMgr != null) {
            
            // Check if the file size reduced
            
            if ( file.getFileSize() < curSize) {
                
                // Release space that was freed by the write
                
                quotaMgr.releaseSpace( sess, tree, file.getFileId(), file.getFullName(), curSize - file.getFileSize());
            }
        }
        
        // Debug

        if (logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_FILEIO))
            logger.debug("Wrote bytes to file: network file=" + file + " buffer size=" + buffer.length + " size=" + size + " file offset=" + fileOffset);

        return size;
    }

    /**
     * Get the node for the specified path
     * 
     * @param tree TreeConnection
     * @param path String
     * @return NodeRef
     * @exception FileNotFoundException
     */
    public NodeRef getNodeForPath(TreeConnection tree, String path)
        throws FileNotFoundException
    {
        // Check if there is a cached state for the path
        
        ContentContext ctx = (ContentContext) tree.getContext();
        
        if ( ctx.hasStateTable())
        {
            // Try and get the node ref from an in memory file state
            
            FileState fstate = ctx.getStateTable().findFileState(path);
            if ( fstate != null && fstate.hasNodeRef() && fstate.exists() )
            {
                // Check that the node exists
            	
                if (fileFolderService.exists(fstate.getNodeRef()))
                {
                	// Bump the file states expiry time
                	
                    fstate.setExpiryTime(System.currentTimeMillis() + FileState.DefTimeout);
                    
                    // Return the cached noderef
                    
                    return fstate.getNodeRef();
                }
                else
                {
                    ctx.getStateTable().removeFileState(path);
                }
            }
        }
        
        // Search the repository for the node
        
        return cifsHelper.getNodeRef(ctx.getRootNode(), path);
    }
    
    /**
     * Convert a node into a share relative path
     * 
     * @param tree TreeConnection
     * @param nodeRef NodeRef
     * @return String
     * @exception FileNotFoundException
     */
    public String getPathForNode( TreeConnection tree, NodeRef nodeRef)
    	throws FileNotFoundException
    {
    	// Convert the target node to a path
    	
        ContentContext ctx = (ContentContext) tree.getContext();
    	List<org.alfresco.service.cmr.model.FileInfo> linkPaths = null;
    	
    	try {
    		linkPaths = fileFolderService.getNamePath( ctx.getRootNode(), nodeRef);
    	}
    	catch ( org.alfresco.service.cmr.model.FileNotFoundException ex)
    	{
    		throw new FileNotFoundException();
    	}

    	// Build the share relative path to the node
    	
    	StringBuilder pathStr = new StringBuilder();
    	
    	for ( org.alfresco.service.cmr.model.FileInfo fInfo : linkPaths) {
    		pathStr.append( FileName.DOS_SEPERATOR);
    		pathStr.append( fInfo.getName());
    	}
    	
    	// Return the share relative path
    	
    	return pathStr.toString();
    }
    
    /**
     * Get the file state for the specified path
     * 
     * @param tree TreeConnection
     * @param path String
     * @return FileState
     * @exception FileNotFoundException
     */
    public FileState getStateForPath(TreeConnection tree, String path)
        throws FileNotFoundException
    {
        // Check if there is a cached state for the path
        
        ContentContext ctx = (ContentContext) tree.getContext();
        FileState fstate = null;
        
        if ( ctx.hasStateTable())
        {
            // Get the file state for a file/folder
            
            fstate = ctx.getStateTable().findFileState(path);
        }
        
        // Return the file state
        
        return fstate;
    }
    
    /**
     * Connection opened to this disk device
     * 
     * @param sess Server session
     * @param tree Tree connection
     */
    public void treeClosed(SrvSession sess, TreeConnection tree)
    {
        // Nothing to do
    }

    /**
     * Connection closed to this device
     * 
     * @param sess Server session
     * @param tree Tree connection
     */
    public void treeOpened(SrvSession sess, TreeConnection tree)
    {
        // Nothing to do
    }

	/**
	 * Return the lock manager used by this filesystem
	 * 
	 * @param sess SrvSession
	 * @param tree TreeConnection
	 * @return LockManager
	 */
	public LockManager getLockManager(SrvSession sess, TreeConnection tree) {
		return _lockManager;
	}
	
	/**
	 * Return the oplock manager implementation associated with this virtual filesystem
	 * 
	 * @param sess SrvSession
	 * @param tree TreeConnection
	 * @return OpLockManager
	 */
	public OpLockManager getOpLockManager(SrvSession sess, TreeConnection tree) {
		return _lockManager;
	}
	
	/**
	 * Enable/disable oplock support
	 * 
	 * @param sess SrvSession
	 * @param tree TreeConnection
	 * @return boolean
	 */
	public boolean isOpLocksEnabled(SrvSession sess, TreeConnection tree) {
		
        // Check if oplocks are enabled
        
        ContentContext ctx = (ContentContext) tree.getContext();
        return ctx.getDisableOplocks() ? false : true;
	}
	
	/**
	 * Copy content data from file to file
	 * 
	 * @param sess SrvSession
	 * @param tree TreeConnection
	 * @param fromNode NodeRef
	 * @param toNode NodeRef
	 * @exception IOException
	 */
	private void copyContentData( SrvSession sess, TreeConnection tree, NodeRef fromNode, NodeRef toNode)
		throws IOException {
		
		try {
			
			// Open the input and output files
			
			ContentReader fromReader = contentService.getReader( fromNode, ContentModel.PROP_CONTENT);
			ContentWriter toWriter   = contentService.getWriter( toNode, ContentModel.PROP_CONTENT, true);
			
			// Copy the content
			
			toWriter.putContent( fromReader);
		}
		catch ( Exception ex) {
			throw new IOException("Failed to copy content");
		}
	}
	
    
    /**
     * Clone/move aspects/properties between nodes
     * 
     * @param newName String
     * @param fromNode NodeRef
     * @param toNode NodeRef
     * @param ctx ContentContext
     */
    private void cloneNodeAspects( String newName, NodeRef fromNode, NodeRef toNode, ContentContext ctx)
    {
        // We need to remove various aspects/properties from the original file, and move them to the new file
        //
        // Check for the lockable aspect
        
        if ( nodeService.hasAspect( fromNode, ContentModel.ASPECT_LOCKABLE)) {
            
            // Remove the lockable aspect from the old working copy, add it to the new file
            
            nodeService.removeAspect( fromNode, ContentModel.ASPECT_LOCKABLE);
            nodeService.addAspect( toNode, ContentModel.ASPECT_LOCKABLE, null);

            // DEBUG
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                logger.debug("  Moved aspect " + ContentModel.ASPECT_LOCKABLE + " to new document");
        }
        
        // Check for the working copy aspect
        
        if ( nodeService.hasAspect( fromNode, ContentModel.ASPECT_WORKING_COPY)) {
            
            // Add the working copy aspect to the new file
            
            Map<QName, Serializable> workingCopyProperties = new HashMap<QName, Serializable>(1);
            workingCopyProperties.put(ContentModel.PROP_WORKING_COPY_OWNER, nodeService.getProperty( fromNode, ContentModel.PROP_WORKING_COPY_OWNER));
            
            nodeService.addAspect( toNode, ContentModel.ASPECT_WORKING_COPY, workingCopyProperties);
            
            // Remove the working copy aspect from old working copy file 
            
            nodeService.removeAspect( fromNode, ContentModel.ASPECT_WORKING_COPY);

            // DEBUG
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                logger.debug("  Moved aspect " + ContentModel.ASPECT_WORKING_COPY + " to new document");
        }
        
        // Check for the copied from aspect
        
        if ( nodeService.hasAspect( fromNode, ContentModel.ASPECT_COPIEDFROM)) {
            
            // Add the copied from aspect to the new file
            
            NodeRef copiedFromNode = (NodeRef) nodeService.getProperty( fromNode, ContentModel.PROP_COPY_REFERENCE);
            Map<QName, Serializable> copiedFromProperties = new HashMap<QName, Serializable>(1);
            copiedFromProperties.put(ContentModel.PROP_COPY_REFERENCE, copiedFromNode);
            
            nodeService.addAspect( toNode, ContentModel.ASPECT_COPIEDFROM, copiedFromProperties);
            
            // Remove the copied from aspect from old working copy file 
            
            nodeService.removeAspect( fromNode, ContentModel.ASPECT_COPIEDFROM);

            // DEBUG
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                logger.debug("  Moved aspect " + ContentModel.ASPECT_COPIEDFROM + " to new document");
            
            // Check if the original node is locked
            
            if ( lockService.getLockType( copiedFromNode) == null) {
                
                // Add the lock back onto the original file
                
                lockService.lock( copiedFromNode, LockType.READ_ONLY_LOCK);

                // DEBUG
                
                if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                    logger.debug("  Re-locked copied from node " + copiedFromNode);
            }
        }
        
        // Check if the new file name is a temporary file, remove any versionable aspect from it
        
        String newNameNorm = newName.toLowerCase();
        
        if ( newNameNorm.endsWith( ".tmp") || newNameNorm.endsWith( ".temp")) {
            
            // Remove the versionable aspect
            
            if ( nodeService.hasAspect( toNode, ContentModel.ASPECT_VERSIONABLE))
                nodeService.removeAspect( toNode, ContentModel.ASPECT_VERSIONABLE);

            // Add the temporary aspect, also prevents versioning
            
            nodeService.addAspect( toNode, ContentModel.ASPECT_TEMPORARY, null);

            // DEBUG
            
            if ( logger.isDebugEnabled() && ctx.hasDebug(AlfrescoContext.DBG_RENAME))
                logger.debug("  Removed versionable aspect from temp file");
        }
    }       
}
