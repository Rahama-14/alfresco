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
package org.alfresco.repo.remote;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.encoding.ContentCharsetFinder;
import org.alfresco.repo.node.db.NodeDaoService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.remote.FileFolderRemote;
import org.alfresco.service.cmr.remote.LoaderRemote;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.PropertyMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server side implementation of the <code>LoaderServiceTransport</code> transport
 * layer.  This is the class that gets exported remotely as it contains the
 * explicit ticket arguments.
 * 
 * @author Derek Hulley
 * @since 2.2
 */
public class LoaderRemoteServer implements LoaderRemote
{
    private static Log logger = LogFactory.getLog(LoaderRemoteServer.class);
    
    /** The association for the working root node: <b>sys:LoaderServiceWorkingRoot</b> */
    private static final QName ASSOC_WORKING_ROOT = QName.createQName(
            NamespaceService.SYSTEM_MODEL_1_0_URI,
            "LoaderServiceWorkingRoot");
    
    private RetryingTransactionHelper retryingTransactionHelper;
    private AuthenticationService authenticationService;
    private NodeService nodeService;
    private NodeDaoService nodeDaoService;
    private FileFolderService fileFolderService;
    private FileFolderRemote fileFolderRemote;
    private MimetypeService mimetypeService;
    private CheckOutCheckInService checkOutCheckInService;

    /**
     * @param transactionService            provides transactional support and retrying
     */
    public void setTransactionService(TransactionService transactionService)
    {
        this.retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
    }

    /**
     * @param authenticationService         the service that will validate the tickets
     */
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }

    /**
     * @param nodeService                   the service that will do the work
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param nodeDaoService                the DAO for node queries
     */
    public void setNodeDaoService(NodeDaoService nodeDaoService)
    {
        this.nodeDaoService = nodeDaoService;
    }

    /**
     * @param fileFolderService             the file-specific service
     */
    public void setFileFolderService(FileFolderService fileFolderService)
    {
        this.fileFolderService = fileFolderService;
    }

    public void setFileFolderRemote(FileFolderRemote fileFolderRemote) 
    {
        this.fileFolderRemote = fileFolderRemote;
    }

    /**
     * @param mimetypeService               used to determine encoding, etc
     */
    public void setMimetypeService(MimetypeService mimetypeService)
    {
        this.mimetypeService = mimetypeService;
    }

    public void setCheckOutCheckInService(CheckOutCheckInService checkOutCheckInService)
    {
        this.checkOutCheckInService = checkOutCheckInService;
    }

    /**
     * {@inheritDoc}
     */
    public String authenticate(String username, String password)
    {
        // We need to authenticate
        AuthenticationUtil.pushAuthentication();
        try
        {
            authenticationService.authenticate(username, password.toCharArray());
            String ticket = authenticationService.getCurrentTicket();
            // Done
            if (logger.isDebugEnabled())
            {
                logger.debug("Authenticated: " + username);
            }
            return ticket;
        }
        finally
        {
            AuthenticationUtil.popAuthentication();
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeRef getOrCreateWorkingRoot(String ticket, final StoreRef storeRef)
    {
        AuthenticationUtil.pushAuthentication();
        try
        {
            authenticationService.validate(ticket, null);
            // Make the call
            RetryingTransactionCallback<NodeRef> callback = new RetryingTransactionCallback<NodeRef>()
            {
                public NodeRef execute() throws Throwable
                {
                    NodeRef rootNodeRef = null;
                    // Check if the store exists
                    if (!nodeService.exists(storeRef))
                    {
                        nodeService.createStore(storeRef.getProtocol(), storeRef.getIdentifier());
                    }
                    rootNodeRef = nodeService.getRootNode(storeRef);
                    // Look for the working root
                    List<ChildAssociationRef> assocRefs = nodeService.getChildAssocs(
                            rootNodeRef,
                            ContentModel.ASSOC_CHILDREN,
                            ASSOC_WORKING_ROOT);
                    NodeRef workingRootNodeRef = null;
                    if (assocRefs.size() > 0)
                    {
                        // Just use the first one
                        workingRootNodeRef = assocRefs.get(0).getChildRef();
                    }
                    else
                    {
                        String username = authenticationService.getCurrentUserName();
                        // We have to make it.  Touch the root node to act as an optimistic lock.
                        nodeService.setProperty(rootNodeRef, ContentModel.PROP_AUTHOR, username);
                        // Add a cm:folder below the root
                        PropertyMap properties = new PropertyMap();
                        properties.put(ContentModel.PROP_NAME, "Loader Application Root");
                        workingRootNodeRef = nodeService.createNode(
                                rootNodeRef,
                                ContentModel.ASSOC_CHILDREN,
                                ASSOC_WORKING_ROOT,
                                ContentModel.TYPE_FOLDER,
                                properties).getChildRef();
                    }
                    // Done
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Got working root node: " + workingRootNodeRef);
                    }
                    return workingRootNodeRef;
                }
            };
            return retryingTransactionHelper.doInTransaction(callback, false, true);
        }
        finally
        {
            AuthenticationUtil.popAuthentication();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getNodeCount(String ticket)
    {
        throw new UnsupportedOperationException("getNodeCount cannot be accurately determined at low cost.");
    }

    /**
     * {@inheritDoc}
     */
    public int getNodeCount(String ticket, final StoreRef storeRef)
    {
        throw new UnsupportedOperationException("getNodeCount cannot be accurately determined at low cost.");
    }

    public FileInfo[] uploadContent(
            String ticket,
            final NodeRef folderNodeRef,
            final String[] filenames,
            final byte[][] bytes)
    {
        if (filenames.length < bytes.length)
        {
            throw new IllegalArgumentException("The number of files must match the number of binary byte arrays given.");
        }
        
        AuthenticationUtil.pushAuthentication();
        try
        {
            authenticationService.validate(ticket, null);
            
            // Make the call
            RetryingTransactionCallback<FileInfo[]> callback = new RetryingTransactionCallback<FileInfo[]>()
            {
                public FileInfo[] execute() throws Throwable
                {
                    FileInfo[] results = new FileInfo[filenames.length];
                    // Create each file
                    for (int i = 0; i < filenames.length; i++)
                    {
                        // Create the file
                        FileInfo newFileInfo = fileFolderService.create(
                                folderNodeRef,
                                filenames[i],
                                ContentModel.TYPE_CONTENT);
                        results[i] = newFileInfo;
                        NodeRef newFileNodeRef = newFileInfo.getNodeRef();

                        // Guess the mimetype
                        String mimetype = mimetypeService.guessMimetype(filenames[i]);
                        // Get a writer
                        ContentWriter writer = fileFolderService.getWriter(newFileNodeRef);
                        // Make a stream
                        ByteArrayInputStream is = new ByteArrayInputStream(bytes[i]);
                        // Guess the encoding
                        ContentCharsetFinder charsetFinder = mimetypeService.getContentCharsetFinder();
                        Charset charset = charsetFinder.getCharset(is, mimetype);
                        // Set metadata
                        writer.setEncoding(charset.name());
                        writer.setMimetype(mimetype);
                        // Write the stream
                        writer.putContent(is);
                    }
                    // Done
                    return results;
                }
            };
            return retryingTransactionHelper.doInTransaction(callback, false, true);
        }
        finally
        {
            AuthenticationUtil.popAuthentication();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void coci(String ticket, final NodeRef[]  nodeRef, byte[][] bytes, List<HashMap<String, Serializable>> versionProperties)
    {
        FileInfo[] workingCopy = checkout(ticket, nodeRef);
        String[] fna = new String[bytes.length];
        for (int i = 0; i < workingCopy.length; i++)
        {
            fna[i] = workingCopy[i].getName();
            versionProperties.add(new HashMap<String, Serializable>());
        }
        fileFolderRemote.putContent(ticket, getNodesRef(workingCopy), bytes, fna);
        checkin(ticket, getNodesRef(workingCopy), versionProperties);
    }

    NodeRef[] getNodesRef(FileInfo[] filesInfoList)
    {
        NodeRef[] nr = new NodeRef[filesInfoList.length];
        for (int i = 0; i < filesInfoList.length; i++)
        {
            nr[i] = (filesInfoList[i].getNodeRef());
        }
        return nr;
    }

   /**
     * {@inheritDoc}
     */
    public NodeRef checkout(String ticket,final NodeRef nodeRef)
    {

        AuthenticationUtil.pushAuthentication();
        try
        {
            authenticationService.validate(ticket, null);
            RetryingTransactionCallback<NodeRef> callback = new RetryingTransactionCallback<NodeRef>()
            {
                public NodeRef execute() throws Throwable
                {
                    //check out current document
                    return checkOutCheckInService.checkout(nodeRef);
                }

            };
           return retryingTransactionHelper.doInTransaction(callback, false, true);

        }
        finally
        {
            AuthenticationUtil.popAuthentication();
        }
    }

    /**
     * {@inheritDoc}
     */
    public FileInfo[] checkout(String ticket, final NodeRef[]  nodeRef)
    {
        AuthenticationUtil.pushAuthentication();
        try
        {
            authenticationService.validate(ticket, null);
            RetryingTransactionCallback<FileInfo[]> callback = new RetryingTransactionCallback<FileInfo[]>()
            {
                public FileInfo[] execute() throws Throwable
                {
                    FileInfo[] arr = new FileInfo[nodeRef.length];

                    for(int i = 0; i < nodeRef.length; i++)
                    {
                        //check out current document
                        arr[i] = fileFolderService.getFileInfo(checkOutCheckInService.checkout(nodeRef[i]));
                    }
                    //Done
                    return arr;
                }

            };
           return retryingTransactionHelper.doInTransaction(callback, false, true);

        }
        finally
        {
            AuthenticationUtil.popAuthentication();
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeRef checkin(String ticket,
                           final NodeRef workingCopyNodeRef,
                           final Map<String, Serializable> versionProperties)
    {
        AuthenticationUtil.pushAuthentication();
        try
        {
            authenticationService.validate(ticket, null);
            RetryingTransactionCallback<NodeRef> callback = new RetryingTransactionCallback<NodeRef>()
            {
                public NodeRef execute() throws Throwable
                {
                    //check in current document
                    return checkOutCheckInService.checkin(workingCopyNodeRef,versionProperties);
                }
            };
           return retryingTransactionHelper.doInTransaction(callback, false, true);
        }
        finally
        {
            AuthenticationUtil.popAuthentication();
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeRef [] checkin(String ticket, final NodeRef [] workingCopyNodeRef,
			final List<HashMap<String, Serializable>> versionProperties)
    {
         AuthenticationUtil.pushAuthentication();

        try
        {
            authenticationService.validate(ticket, null);
            RetryingTransactionCallback<NodeRef[]> callback = new RetryingTransactionCallback<NodeRef[]>()
            {
                public NodeRef [] execute() throws Throwable
                {
                    NodeRef [] nr = new NodeRef[workingCopyNodeRef.length];
                    for(int i = 0; i < workingCopyNodeRef.length;i++)
                    {
                        //check in current document
                        nr[i] = checkOutCheckInService.checkin(workingCopyNodeRef[i],versionProperties.get(i));
                    }
                    //Done
                    return nr;
                }

            };
           return retryingTransactionHelper.doInTransaction(callback, false, true);

        }
        finally
        {
            AuthenticationUtil.popAuthentication();
        }
    }
}
