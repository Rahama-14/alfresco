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

package org.alfresco.repo.deploy;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.alfresco.deployment.DeploymentReceiverService;
import org.alfresco.deployment.DeploymentReceiverTransport;
import org.alfresco.deployment.DeploymentTransportOutputFilter;
import org.alfresco.deployment.FileDescriptor;
import org.alfresco.deployment.FileType;
import org.alfresco.repo.action.ActionServiceRemote;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.avm.util.SimplePath;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.remote.AVMRemoteImpl;
import org.alfresco.repo.remote.AVMSyncServiceRemote;
import org.alfresco.repo.remote.ClientTicketHolder;
import org.alfresco.repo.remote.ClientTicketHolderThread;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ActionServiceTransport;
import org.alfresco.service.cmr.avm.AVMException;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMNotFoundException;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avm.AVMStoreDescriptor;
import org.alfresco.service.cmr.avm.AVMWrongTypeException;
import org.alfresco.service.cmr.avm.deploy.DeploymentCallback;
import org.alfresco.service.cmr.avm.deploy.DeploymentEvent;
import org.alfresco.service.cmr.avm.deploy.DeploymentService;
import org.alfresco.service.cmr.avmsync.AVMDifference;
import org.alfresco.service.cmr.avmsync.AVMSyncService;
import org.alfresco.service.cmr.remote.AVMRemote;
import org.alfresco.service.cmr.remote.AVMRemoteTransport;
import org.alfresco.service.cmr.remote.AVMSyncServiceTransport;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.NameMatcher;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

/**
 * Implementation of DeploymentService.
 * @author britt
 */
public class DeploymentServiceImpl implements DeploymentService
{
    private static Log fgLogger = LogFactory.getLog(DeploymentServiceImpl.class);

    /**
     * Holds locks for all deployment destinations (alfresco->alfresco)
     */
    private Map<DeploymentDestination, DeploymentDestination> fDestinations;

    /**
     * The local AVMService Instance.
     */
    private AVMService fAVMService;

    /**
     * The local Transaction Service Instance
     */
    TransactionService trxService;
    
    /**
     * The Ticket holder.
     */
    private ClientTicketHolder fTicketHolder;
    
    /**
     * number of concurrent sending threads
     */
    private int numberOfSendingThreads = 3;

    /**
     * Default constructor.
     */
    public DeploymentServiceImpl()
    {
        fTicketHolder = new ClientTicketHolderThread();
        fDestinations = new HashMap<DeploymentDestination, DeploymentDestination>();
    }

    /**
     * Setter.
     * @param service The instance to set.
     */
    public void setAvmService(AVMService service)
    {
        fAVMService = service;
    }

    /**
     * Setter.
     * @param trxService The instance to set.
     */
    public void setTransactionService(TransactionService trxService)
    {
        this.trxService = trxService;
    }
    
    /* 
     * Deploy differences to an ASR 
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.avm.deploy.DeploymentService#deployDifference(int, java.lang.String, java.lang.String, int, java.lang.String, java.lang.String, java.lang.String, boolean, boolean)
     */
    public void deployDifference(int version, String srcPath, String hostName,
                                             int port, String userName, String password,
                                             String dstPath, NameMatcher matcher, boolean createDst,
                                             boolean dontDelete, boolean dontDo,
                                             List<DeploymentCallback> callbacks)
    {
        DeploymentDestination dest = getLock(hostName, port);
        synchronized (dest)
        {
            if (fgLogger.isDebugEnabled())
            {
                fgLogger.debug("Deploying to Remote Alfresco at " + dest);
            }
            try
            {
                AVMRemote remote = getRemote(hostName, port, userName, password);
                if (version < 0)
                {
                    String storeName = srcPath.substring(0, srcPath.indexOf(":"));
                    version = fAVMService.createSnapshot(storeName, null, null).get(storeName);
                }

                {
                	DeploymentEvent event = new DeploymentEvent(DeploymentEvent.Type.START,
                                                                new Pair<Integer, String>(version, srcPath),
                                                                dstPath);
                	processEvent(event, callbacks);
                }
                
                // Get the root of the deployment from this server.
                AVMNodeDescriptor srcRoot = fAVMService.lookup(version, srcPath);
                if (srcRoot == null)
                {
                    throw new AVMNotFoundException("Directory Not Found: " + srcPath);
                }
                if (!srcRoot.isDirectory())
                {
                    throw new AVMWrongTypeException("Not a directory: " + srcPath);
                }
                // Create a snapshot on the destination store.
                String [] storePath = dstPath.split(":");
                int snapshot = -1;
                AVMNodeDescriptor dstParent = null;
                if (!dontDo)
                {
                    String[] parentBase = AVMNodeConverter.SplitBase(dstPath);
                    dstParent = remote.lookup(-1, parentBase[0]);
                    if (dstParent == null)
                    {
                        if (createDst)
                        {
                            createDestination(remote, parentBase[0]);
                            dstParent = remote.lookup(-1, parentBase[0]);
                        }
                        else
                        {
                            throw new AVMNotFoundException("Node Not Found: " + parentBase[0]);
                        }
                    }
                    snapshot = remote.createSnapshot(storePath[0], "PreDeploy", "Pre Deployment Snapshot").get(storePath[0]);
                }
                // Get the root of the deployment on the destination server.
                AVMNodeDescriptor dstRoot = remote.lookup(-1, dstPath);
                if (dstRoot == null)
                {
                    // If it doesn't exist, do a copyDirectory to create it.
                    DeploymentEvent event =
                        new DeploymentEvent(DeploymentEvent.Type.COPIED,
                                            new Pair<Integer, String>(version, srcPath),
                                            dstPath);
                    if (fgLogger.isDebugEnabled())
                    {
                        fgLogger.debug(event);
                    }
                    processEvent(event, callbacks);
                    if (dontDo)
                    {
                        return;
                    }
                    copyDirectory(version, srcRoot, dstParent, remote, matcher);
                    remote.createSnapshot(storePath[0], "Deployment", "Post Deployment Snapshot.");
                    if (callbacks != null)
                    {
                        event = new DeploymentEvent(DeploymentEvent.Type.END,
                                                    new Pair<Integer, String>(version, srcPath),
                                                    dstPath);
                        if (fgLogger.isDebugEnabled())
                        {
                            fgLogger.debug(event);
                        }
                        processEvent(event, callbacks);

                    }
                    return;
                }
                if (!dstRoot.isDirectory())
                {
                    throw new AVMWrongTypeException("Not a Directory: " + dstPath);
                }
                // The corresponding directory exists so recursively deploy.
                try
                {
                    deployDirectoryPush(version, srcRoot, dstRoot, remote, matcher, dontDelete, dontDo, callbacks);
                    remote.createSnapshot(storePath[0], "Deployment", "Post Deployment Snapshot.");
                    if (callbacks != null)
                    {
                        DeploymentEvent event = new DeploymentEvent(DeploymentEvent.Type.END,
                                                                    new Pair<Integer, String>(version, srcPath),
                                                                    dstPath);
                        processEvent(event, callbacks);

                    }
                    return;
                }
                catch (AVMException e)
                {
                    if (callbacks != null)
                    {
                        DeploymentEvent event = new DeploymentEvent(DeploymentEvent.Type.FAILED,
                                                                    new Pair<Integer, String>(version, srcPath),
                                                                    dstPath, e.getMessage());
                        for (DeploymentCallback callback : callbacks)
                        {
                            callback.eventOccurred(event);
                        }
                    }
                    try
                    {
                        if (snapshot != -1)
                        {
                            AVMSyncService syncService = getSyncService(hostName, port);
                            List<AVMDifference> diffs = syncService.compare(snapshot, dstPath, -1, dstPath, null);
                            syncService.update(diffs, null, false, false, true, true, "Aborted Deployment", "Aborted Deployment");
                        }
                    }
                    catch (Exception ee)
                    {
                        throw new AVMException("Failed to rollback to version " + snapshot + " on " + hostName, ee);
                    }
                    throw new AVMException("Deployment to " + hostName + " failed.", e);
                }
            }
            catch (Exception e)
            {
                if (callbacks != null)
                {
                    DeploymentEvent event = new DeploymentEvent(DeploymentEvent.Type.FAILED,
                                                                new Pair<Integer, String>(version, srcPath),
                                                                dstPath, e.getMessage());
                    processEvent(event, callbacks);
                }
                throw new AVMException("Deployment to " + hostName + " failed.", e);
            }
            finally
            {
                fTicketHolder.setTicket(null);
            }
        }
    }

    /**
     * Deploy all the children of corresponding directories. (ASR version)
     * @param src The source directory.
     * @param dst The destination directory.
     * @param remote The AVMRemote instance.
     * @param dontDelete Flag for not deleting.
     * @param dontDo Flag for dry run.
     */
    private void deployDirectoryPush(int version,
                                     AVMNodeDescriptor src, 
                                     AVMNodeDescriptor dst,
                                     AVMRemote remote,
                                     NameMatcher matcher,
                                     boolean dontDelete, boolean dontDo,
                                     List<DeploymentCallback> callbacks)
    {
        if (src.getGuid().equals(dst.getGuid()))
        {
            return;
        }
        if (!dontDo && !dontDelete)
        {
            copyMetadata(version, src, dst, remote);
        }
        // Get the listing for the source.
        SortedMap<String, AVMNodeDescriptor> srcList = fAVMService.getDirectoryListing(src);
        // Get the listing for the destination.
        SortedMap<String, AVMNodeDescriptor> dstList = remote.getDirectoryListing(dst);
        for (Map.Entry<String, AVMNodeDescriptor> entry : srcList.entrySet())
        {
            String name = entry.getKey();
            AVMNodeDescriptor srcNode = entry.getValue();
            AVMNodeDescriptor dstNode = dstList.get(name);
            if (!excluded(matcher, srcNode.getPath(), dstNode != null ? dstNode.getPath() : null))
            {
                deploySinglePush(version, srcNode, dst, dstNode, remote, matcher, dontDelete, dontDo, callbacks);
            }
        }
        // Delete nodes that are missing in the source.
        if (dontDelete)
        {
            return;
        }
        for (String name : dstList.keySet())
        {
            if (!srcList.containsKey(name))
            {
                Pair<Integer, String> source =
                    new Pair<Integer, String>(version, AVMNodeConverter.ExtendAVMPath(src.getPath(), name));
                String destination = AVMNodeConverter.ExtendAVMPath(dst.getPath(), name);
                if (!excluded(matcher, null, destination))
                {
                    DeploymentEvent event =
                        new DeploymentEvent(DeploymentEvent.Type.DELETED,
                                            source,
                                            destination);
                    processEvent(event, callbacks);
                    if (dontDo)
                    {
                        continue;
                    }
                    remote.removeNode(dst.getPath(), name);
                }
            }
        }
    }

    /**
     * Push out a single node.
     * @param src The source node.
     * @param dstParent The destination parent.
     * @param dst The destination node. May be null.
     * @param remote The AVMRemote instance.
     * @param dontDelete Flag for whether deletions should happen.
     * @param dontDo Dry run flag.
     */
    private void deploySinglePush(int version,
                                  AVMNodeDescriptor src, AVMNodeDescriptor dstParent,
                                  AVMNodeDescriptor dst, AVMRemote remote,
                                  NameMatcher matcher,
                                  boolean dontDelete, boolean dontDo,
                                  List<DeploymentCallback> callbacks)
    {
        // Destination does not exist.
        if (dst == null)
        {
            if (src.isDirectory())
            {
                // Recursively copy a source directory.
                Pair<Integer, String> source =
                    new Pair<Integer, String>(version, src.getPath());
                String destination = AVMNodeConverter.ExtendAVMPath(dstParent.getPath(), src.getName());
                DeploymentEvent event = new DeploymentEvent(DeploymentEvent.Type.COPIED,
                                                      source,
                                                      destination);
                processEvent(event, callbacks);
                if (dontDo)
                {
                    return;
                }
                copyDirectory(version, src, dstParent, remote, matcher);
                return;
            }
            Pair<Integer, String> source =
                new Pair<Integer, String>(version, src.getPath());
            String destination = AVMNodeConverter.ExtendAVMPath(dstParent.getPath(), src.getName());
            DeploymentEvent event = new DeploymentEvent(DeploymentEvent.Type.COPIED,
                                                        source,
                                                        destination);
            processEvent(event, callbacks);
            if (dontDo)
            {
                return;
            }
            // Copy a source file.
            OutputStream out = remote.createFile(dstParent.getPath(), src.getName());
            InputStream in = fAVMService.getFileInputStream(src);
            copyStream(in, out);
            copyMetadata(version, src, remote.lookup(-1, dstParent.getPath() + '/' + src.getName()), remote);
            return;
        }
        // Destination exists.
        if (src.isDirectory())
        {
            // If the destination is also a directory, recursively deploy.
            if (dst.isDirectory())
            {
                deployDirectoryPush(version, src, dst, remote, matcher, dontDelete, dontDo, callbacks);
                return;
            }
            Pair<Integer, String> source =
                new Pair<Integer, String>(version, src.getPath());
            String destination = dst.getPath();
            DeploymentEvent event = new DeploymentEvent(DeploymentEvent.Type.COPIED,
                                                        source, destination);
            processEvent(event, callbacks);
            
            if (dontDo)
            {
                return;
            }
            remote.removeNode(dstParent.getPath(), src.getName());
            copyDirectory(version, src, dstParent, remote, matcher);
            return;
        }
        // Source is a file.
        if (dst.isFile())
        {
            // Destination is also a file. Overwrite if the GUIDS are different.
            if (src.getGuid().equals(dst.getGuid()))
            {
                return;
            }
            Pair<Integer, String> source =
                new Pair<Integer, String>(version, src.getPath());
            String destination = dst.getPath();
            DeploymentEvent event = new DeploymentEvent(DeploymentEvent.Type.UPDATED,
                                                        source,
                                                        destination);
            if (fgLogger.isDebugEnabled())
            {
                fgLogger.debug(event);
            }
            processEvent(event, callbacks);
            if (dontDo)
            {
                return;
            }
            InputStream in = fAVMService.getFileInputStream(src);
            OutputStream out = remote.getFileOutputStream(dst.getPath());
            copyStream(in, out);
            copyMetadata(version, src, dst, remote);
            return;
        }
        Pair<Integer, String> source =
            new Pair<Integer, String>(version, src.getPath());
        String destination = AVMNodeConverter.ExtendAVMPath(dstParent.getPath(), src.getName());
        DeploymentEvent event = new DeploymentEvent(DeploymentEvent.Type.UPDATED,
                                                    source,
                                                    destination);
        processEvent(event, callbacks);
        if (dontDo)
        {
            return;
        }
        // Destination is a directory and the source is a file.
        // Delete the destination directory and copy the file over.
        remote.removeNode(dstParent.getPath(), dst.getName());
        InputStream in = fAVMService.getFileInputStream(src);
        OutputStream out = remote.createFile(dstParent.getPath(), src.getName());
        copyStream(in, out);
        copyMetadata(version, src, remote.lookup(-1, dstParent.getPath() + '/' + dst.getName()), remote);
    }

    /**
     * Recursively copy a directory.
     * @param src
     * @param parent
     * @param remote
     */
    private void copyDirectory(int version, AVMNodeDescriptor src, AVMNodeDescriptor parent,
                               AVMRemote remote, NameMatcher matcher)
    {
        // Create the destination directory.
        remote.createDirectory(parent.getPath(), src.getName());
        AVMNodeDescriptor newParent = remote.lookup(-1, parent.getPath() + '/' + src.getName());
        copyMetadata(version, src, newParent, remote);
        SortedMap<String, AVMNodeDescriptor> list =
            fAVMService.getDirectoryListing(src);
        // For each child in the source directory.
        for (AVMNodeDescriptor child : list.values())
        {
            if (!excluded(matcher, child.getPath(), null))
            {
                // If it's a file, copy it over and move on.
                if (child.isFile())
                {
                    InputStream in = fAVMService.getFileInputStream(child);
                    OutputStream out = remote.createFile(newParent.getPath(), child.getName());
                    copyStream(in, out);
                    copyMetadata(version, child, remote.lookup(-1, newParent.getPath() + '/' + child.getName()), remote);
                    continue;
                }
                // Otherwise copy the child directory recursively.
                copyDirectory(version, child, newParent, remote, matcher);
            }
        }
    }

    /**
     * Utility for copying from one stream to another.
     * @param in The input stream.
     * @param out The output stream.
     */
    private void copyStream(InputStream in, OutputStream out)
    {
        byte[] buff = new byte[8192];
        int read = 0;
        try
        {
            while ((read = in.read(buff)) != -1)
            {
                out.write(buff, 0, read);
            }
            in.close();
            out.close();
        }
        catch (IOException e)
        {
            throw new AVMException("I/O Exception", e);
        }
    }

    private void copyMetadata(int version, AVMNodeDescriptor src, AVMNodeDescriptor dst, AVMRemote remote)
    {
        Map<QName, PropertyValue> props = fAVMService.getNodeProperties(version, src.getPath());
        remote.setNodeProperties(dst.getPath(), props);
        Set<QName> aspects = fAVMService.getAspects(version, src.getPath());
        for (QName aspect : aspects)
        {
            if (remote.hasAspect(-1, dst.getPath(), aspect))
            {
                continue;
            }
            remote.addAspect(dst.getPath(), aspect);
        }
        remote.setGuid(dst.getPath(), src.getGuid());
        if (src.isFile())
        {
            ContentData contData = fAVMService.getContentDataForRead(version, src.getPath());
            remote.setEncoding(dst.getPath(), contData.getEncoding());
            remote.setMimeType(dst.getPath(), contData.getMimetype());
        }
    }

    /**
     * Utility to get an AVMRemote from a remote Alfresco Server.
     * @param hostName
     * @param port
     * @param userName
     * @param password
     * @return
     */
    private AVMRemote getRemote(String hostName, int port, String userName, String password)
    {
        try
        {
            RmiProxyFactoryBean authFactory = new RmiProxyFactoryBean();
            authFactory.setRefreshStubOnConnectFailure(true);
            authFactory.setServiceInterface(AuthenticationService.class);
            authFactory.setServiceUrl("rmi://" + hostName + ":" + port + "/authentication");
            authFactory.afterPropertiesSet();
            AuthenticationService authService = (AuthenticationService)authFactory.getObject();
            authService.authenticate(userName, password.toCharArray());
            String ticket = authService.getCurrentTicket();
            fTicketHolder.setTicket(ticket);
            RmiProxyFactoryBean remoteFactory = new RmiProxyFactoryBean();
            remoteFactory.setRefreshStubOnConnectFailure(true);
            remoteFactory.setServiceInterface(AVMRemoteTransport.class);
            remoteFactory.setServiceUrl("rmi://" + hostName + ":" + port + "/avm");
            remoteFactory.afterPropertiesSet();
            AVMRemoteTransport transport = (AVMRemoteTransport)remoteFactory.getObject();
            AVMRemoteImpl remote = new AVMRemoteImpl();
            remote.setAvmRemoteTransport(transport);
            remote.setClientTicketHolder(fTicketHolder);
            return remote;
        }
        catch (Exception e)
        {
            throw new AVMException("Could not Initialize Remote Connection to " + hostName, e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.avm.deploy.DeploymentService#getRemoteActionService(java.lang.String, int, java.lang.String, java.lang.String)
     */
    public ActionService getRemoteActionService(String hostName, int port, String userName, String password)
    {
        try
        {
            RmiProxyFactoryBean authFactory = new RmiProxyFactoryBean();
            authFactory.setRefreshStubOnConnectFailure(true);
            authFactory.setServiceInterface(AuthenticationService.class);
            authFactory.setServiceUrl("rmi://" + hostName + ":" + port + "/authentication");
            authFactory.afterPropertiesSet();
            AuthenticationService authService = (AuthenticationService)authFactory.getObject();
            authService.authenticate(userName, password.toCharArray());
            String ticket = authService.getCurrentTicket();
            fTicketHolder.setTicket(ticket);
            RmiProxyFactoryBean remoteFactory = new RmiProxyFactoryBean();
            remoteFactory.setRefreshStubOnConnectFailure(true);
            remoteFactory.setServiceInterface(ActionServiceTransport.class);
            remoteFactory.setServiceUrl("rmi://" + hostName + ":" + port + "/action");
            remoteFactory.afterPropertiesSet();
            ActionServiceTransport transport = (ActionServiceTransport)remoteFactory.getObject();
            ActionServiceRemote remote = new ActionServiceRemote();
            remote.setActionServiceTransport(transport);
            remote.setClientTicketHolder(fTicketHolder);
            return remote;
        }
        catch (Exception e)
        {
            throw new AVMException("Could not Initialize Remote Connection to " + hostName, e);
        }
    }

    /**
     * Utility method to get the payload transformers for a named transport
     * 
     * The transport adapters are sprung into the deploymentReceiverTransportAdapters property
     * 
     * @return the transformers
     */
    private List<DeploymentTransportOutputFilter> getTransformers(String transportName)
    {    	
        
        DeploymentReceiverTransportAdapter adapter = deploymentReceiverTransportAdapters.get(transportName);
        	
        if(adapter == null) {
        		// Adapter does not exist
        		fgLogger.error("Deployment Receiver Transport adapter does not exist for transport. Name: " + transportName);
        		throw new AVMException("Deployment Receiver Transport adapter does not exist for transport. Name: " + transportName);
        }

        List<DeploymentTransportOutputFilter> transformers = adapter.getTransformers();
        return transformers;
    }

    
    
    /**
     * Utility method to get a connection to a remote file system receiver (FSR)
     * 
     * The transport adapters are sprung into the deploymentReceiverTransportAdapters property
     * @param transportName the name of the adapter for the transport 
     * @param hostName the hostname or IP address to connect to
     * @param port the port number
     * @param version the version of the website to deploy
     * @param srcPath the path of the website
     * 
     * @return an implementation of the service
     */
    private DeploymentReceiverService getDeploymentReceiverService(String transportName, String hostName, int port, int version, String srcPath)
    {    	
 
       DeploymentReceiverTransportAdapter adapter = deploymentReceiverTransportAdapters.get(transportName);
        	
        if(adapter == null) {
        	// Adapter does not exist
        	fgLogger.error("Deployment Receiver Transport adapter does not exist for transport. Name: " + transportName);
        		throw new AVMException("Deployment Receiver Transport adapter does not exist for transport. Name: " + transportName);
        }
        try
        {
        	DeploymentReceiverTransport transport = adapter.getTransport(hostName, port, version, srcPath);
        		
        	// Now decorate the transport with the service client
            DeploymentReceiverServiceClient service = new DeploymentReceiverServiceClient();
            service.setDeploymentReceiverTransport(transport);
            return service;
        }
        catch (Exception e)
        {
            throw new AVMException("Could not connect to remote FSR, transportName:" + transportName + ", hostName:" + hostName + ", port: " + port, e);
        }
    }

    /**
     * Utility to get the sync service for rolling back after a failed deployment.
     * @param hostName The target machine.
     * @param port The port.
     * @return An AVMSyncService instance.
     */
    private AVMSyncService getSyncService(String hostName, int port)
    {
        try
        {
            RmiProxyFactoryBean syncFactory = new RmiProxyFactoryBean();
            syncFactory.setRefreshStubOnConnectFailure(true);
            syncFactory.setServiceInterface(AVMSyncServiceTransport.class);
            syncFactory.setServiceUrl("rmi://" + hostName + ":" + port + "/avmsync");
            syncFactory.afterPropertiesSet();
            AVMSyncServiceTransport syncServiceTransport = (AVMSyncServiceTransport)syncFactory.getObject();
            AVMSyncServiceRemote remote = new AVMSyncServiceRemote();
            remote.setAvmSyncServiceTransport(syncServiceTransport);
            remote.setClientTicketHolder(fTicketHolder);
            return remote;
        }
        catch (Exception e)
        {
            throw new AVMException("Could not roll back failed deployment to " + hostName, e);
        }
    }

    /**
     * Helper function to create a non existent destination.
     * @param remote The AVMRemote instance.
     * @param dstPath The destination path to create.
     */
    private void createDestination(AVMRemote remote, String dstPath)
    {
        String[] storePath = dstPath.split(":");
        String storeName = storePath[0];
        String path = storePath[1];
        AVMStoreDescriptor storeDesc = remote.getStore(storeName);
        if (storeDesc == null)
        {
            remote.createStore(storeName);
        }
        SimplePath simpPath = new SimplePath(path);
        if (simpPath.size() == 0)
        {
            return;
        }
        String prevPath = storeName + ":/";
        for (int i = 0; i < simpPath.size(); i++)
        {
            String currPath = AVMNodeConverter.ExtendAVMPath(prevPath, simpPath.get(i));
            AVMNodeDescriptor desc = remote.lookup(-1, currPath);
            if (desc == null)
            {
                remote.createDirectory(prevPath, simpPath.get(i));
            }
            prevPath = currPath;
        }
    }

    /**
     * Deploy differences to a File System Receiver, FSR
     * 
     *  @param version snapshot version to deploy.  If 0 then a new snapshot is created.
     *  @param srcPath 
     *	@param adapterName
     *	@param hostName
     *  @param port
     *  @param userName 
     *  @param password
     *  @param target 
     *  @param matcher
     *  @param createDst 	Not implemented
     *  @param dontDelete   Not implemented
     *  @param dontDo		Not implemented
     *  @param callbacks	Event callbacks when a deployment Starts, Ends, Adds, Deletes etc.
     *  
     * @see org.alfresco.service.cmr.avm.deploy.DeploymentService#deployDifferenceFS(int, java.lang.String, java.lang.String, int, java.lang.String, java.lang.String, java.lang.String, boolean, boolean)
     */
	public void deployDifferenceFS(int version, 
			String srcPath,
			String adapterName, 
			String hostName, 
			int port, 
			String userName, 
			String password, 
			String target,
			NameMatcher matcher, 
			boolean createDst, 
			boolean dontDelete,
			boolean dontDo, 
			List<DeploymentCallback> callbacks) 
    {
        if (fgLogger.isDebugEnabled())
        {
            fgLogger.debug("Deploying To File System Reciever on " + hostName + " to target " + target);
        }
        
        DeploymentReceiverService service = null;
        List<DeploymentTransportOutputFilter>transformers = null;
        String ticket = null;
        
        String currentEffectiveUser = AuthenticationUtil.getRunAsUser();

        try
        {       
            // Kick off the event queue that will process deployment call-backs 
            LinkedBlockingQueue<DeploymentEvent> eventQueue = new LinkedBlockingQueue<DeploymentEvent>();
            EventQueueWorker eventQueueWorker = new EventQueueWorker(currentEffectiveUser, eventQueue, callbacks);
		    eventQueueWorker.setName(eventQueueWorker.getClass().getName());
			eventQueueWorker.setPriority(Thread.currentThread().getPriority());
            eventQueueWorker.start();
                
            try 
            {

                try {           
                
                	if (version < 0)
                	{
                		String storeName = srcPath.substring(0, srcPath.indexOf(':'));
                		version = fAVMService.createSnapshot(storeName, null, null).get(storeName);
                	}

                	transformers = getTransformers(adapterName);
                	service = getDeploymentReceiverService(adapterName, hostName, port, version, srcPath);
                } 
                catch (Exception e)
                {
                	// unable to get service
               	 	eventQueue.add(new DeploymentEvent(DeploymentEvent.Type.FAILED,
                         new Pair<Integer, String>(version, srcPath),
                         target, e.getMessage()));
               	 	throw e;
                }
                
           	 	eventQueue.add(new DeploymentEvent(DeploymentEvent.Type.START,
                     new Pair<Integer, String>(version, srcPath),
                     target));
            
                // Go parallel to reduce the problems of high network latency           

                LinkedBlockingQueue<DeploymentWork> sendQueue = new LinkedBlockingQueue<DeploymentWork>();
                List<Exception> errors = Collections.synchronizedList(new ArrayList<Exception>());

                SendQueueWorker[] workers = new SendQueueWorker[numberOfSendingThreads];
                for(int i = 0; i < numberOfSendingThreads; i++)
                {
       				workers[i] = new SendQueueWorker(currentEffectiveUser, service, fAVMService, trxService, errors, eventQueue, sendQueue, transformers);
       			    workers[i].setName(workers[i].getClass().getName());
       			    workers[i].setPriority(Thread.currentThread().getPriority());
                }
                
            	for(SendQueueWorker sender : workers) 
            	{
            		sender.start();
            	}
               
                try 
                {
                	ticket = service.begin(target, userName, password);
                	deployDirectoryPushFSR(service, ticket, version, srcPath, "/", matcher, eventQueue, sendQueue, errors);
                }
                catch (Exception e)
                {
                	errors.add(e);
                }
                finally
                {
                	// clean up senders thread pool
                	fgLogger.debug("closing deployment workers");
                	for(SendQueueWorker sender : workers) 
                	{
                		sender.stopMeWhenIdle();
                	}
                	for(SendQueueWorker sender : workers) 
                	{
                		sender.join();
                	}
                	fgLogger.debug("deployment workers closed");
                
                	if (errors.size() <= 0 && ticket != null)
                	{
                		try 
                		{
                			service.commit(ticket);
                		} 
                		catch (Exception e)
                		{
                			errors.add(e);
                		}
                	}
                
                	if(errors.size() > 0)
                	{
                		Exception firstError = errors.get(0);
                
                		eventQueue.add(new DeploymentEvent(DeploymentEvent.Type.FAILED,
                        new Pair<Integer, String>(version, srcPath),
                        target, firstError.getMessage()));

                		if (ticket != null)
                		{
                			try 
                			{
                				service.abort(ticket);
                			} 
                			catch (Exception ae)
                			{
                				// nothing we can do here
                				fgLogger.error("Unable to abort deployment.  Error in exception handler", ae);
                			}
                		}                	
                		// yes there were errors, throw the first exception that was saved
                		MessageFormat f = new MessageFormat("Error during deployment srcPath: {0}, version:{1}, adapterName:{2}, hostName:{3}, port:{4}, error:{5}");
                		Object[] objs = { srcPath, version, adapterName, hostName, port, firstError };
                	          	
                		throw new AVMException(f.format(objs), firstError);
                	}
                } // end of finally block
                
                // Success if we get here
                eventQueue.add(new DeploymentEvent(DeploymentEvent.Type.END,
                                        new Pair<Integer, String>(version, srcPath),
                                        target));
                
                fgLogger.debug("deployment completed successfully");
            }
            finally 
            {
            	// Now stutdown the event queue
            	fgLogger.debug("closing event queue");
            	eventQueueWorker.stopMeWhenIdle();
            	eventQueueWorker.join();
            	fgLogger.debug("event queue closed");
            }
        }
        catch (Exception e)
        {
        	// yes there were errors
    		MessageFormat f = new MessageFormat("Deployment exception, unable to deploy : srcPath:{0}, target:{1}, version:{2}, adapterName:{3}, hostName:{4}, port:{5}, error:{6}");
    		Object[] objs = { srcPath, target, version, adapterName, hostName, port, e };       
            throw new AVMException(f.format(objs), e);
        }
    }
    /**
     * deployDirectoryPush (FSR only)
     * 
     * Compares the source and destination listings and updates report with update events required to make 
     * dest similar to src. 
     * 
     * @param service
     * @param ticket
     * @param report 
     * @param callbacks
     * @param version
     * @param srcPath
     * @param dstPath
     * @param matcher
     */
    private void deployDirectoryPushFSR(DeploymentReceiverService service, 
    		String ticket,
            int version,
            String srcPath, 
            String dstPath, 
            NameMatcher matcher,
    		BlockingQueue<DeploymentEvent> eventQueue,
    		BlockingQueue<DeploymentWork> sendQueue,
    		List<Exception> errors)
    {
        Map<String, AVMNodeDescriptor> srcListing = fAVMService.getDirectoryListing(version, srcPath);
        List<FileDescriptor> dstListing = service.getListing(ticket, dstPath);
        Iterator<AVMNodeDescriptor> srcIter = srcListing.values().iterator();
        Iterator<FileDescriptor> dstIter = dstListing.iterator();
	    
        // Here with two sorted directory listings
        AVMNodeDescriptor src = null;
        FileDescriptor dst = null;
        
        // Step through both directory listings
        while ((srcIter.hasNext() || dstIter.hasNext() || src != null || dst != null) && errors.size() <= 0)
        {
            if (src == null)
            {
                if (srcIter.hasNext())
                {
                    src = srcIter.next();
                }
            }
            if (dst == null)
            {
                if (dstIter.hasNext())
                {
                    dst = dstIter.next();
                }
            }
            if (fgLogger.isDebugEnabled())
            {
                fgLogger.debug("comparing src:" + src + " dst:"+ dst);
            }
            // This means no entry on src so delete what is on dst.
            if (src == null)
            {
                String newDstPath = extendPath(dstPath, dst.getName());
                if (!excluded(matcher, null, newDstPath))
                {
//                    service.delete(ticket, newDstPath);
//                    eventQueue.add(new DeploymentEvent(DeploymentEvent.Type.DELETED,
//                                                                new Pair<Integer, String>(version, extendPath(srcPath, dst.getName())),
//                                                                newDstPath));
                	
                    sendQueue.add(new DeploymentWork(new DeploymentEvent(DeploymentEvent.Type.DELETED,
                            new Pair<Integer, String>(version, extendPath(srcPath, dst.getName())), 
                            newDstPath), ticket));
                }
                dst = null;
                continue;
            }
            // Nothing on the destination so copy over.
            if (dst == null)
            {
                if (!excluded(matcher, src.getPath(), null))
                {
                    createOnFSR(service, ticket, version, src, dstPath, matcher, sendQueue);
                }
                src = null;
                continue;
            }
            
            // Here with src and dst containing something
            int diff = src.getName().compareToIgnoreCase(dst.getName());
            if (diff < 0)
            {
            	// src is less than dst - must be new content in src
                if (!excluded(matcher, src.getPath(), null))
                {
                    createOnFSR(service, ticket, version, src, dstPath, matcher, sendQueue);
                }
                src = null;
                continue;
            }
            if (diff == 0)
            {
            	// src and dst have same file name
                if (src.getGuid().equals(dst.getGUID()))
                {
                    src = null;
                    dst = null;
                    continue;
                }
                if (src.isFile())
                {
                	// this is an update to a file
                    String extendedPath = extendPath(dstPath, dst.getName());
                    if (!excluded(matcher, src.getPath(), extendedPath))
                    {
                    	// Work in progress
                    	sendQueue.add(new DeploymentWork(
                    			new DeploymentEvent(DeploymentEvent.Type.UPDATED,
                                new Pair<Integer, String>(version, src.getPath()),                              
                                extendedPath), ticket, src));
                        // Work in progress
//                        copyFileToFSR(service, ticket, version, src,
//                                 extendedPath, false);
                    }
                    src = null;
                    dst = null;
                    continue;
                }
                // Source is a directory.
                if (dst.getType() == FileType.DIR)
                {
                    String extendedPath = extendPath(dstPath, dst.getName());
                    if (!excluded(matcher, src.getPath(), extendedPath))
                    {
                        deployDirectoryPushFSR(service, ticket, version, src.getPath(), extendedPath, matcher, eventQueue, sendQueue, errors);
                    }
                    service.setGuid(ticket, extendedPath, src.getGuid());
                    src = null;
                    dst = null;
                    continue;
                }
                if (!excluded(matcher, src.getPath(), null))
                {
                    createOnFSR(service, ticket, version, src, dstPath, matcher, sendQueue);
                }
                src = null;
                dst = null;
                continue;
            }
            // diff > 0
            // Destination is missing in source, delete it.
            String newDstPath = extendPath(dstPath, dst.getName());

            //            service.delete(ticket, newDstPath);
//            
//            eventQueue.add(new DeploymentEvent(DeploymentEvent.Type.DELETED,
//                                                        new Pair<Integer, String>(version, extendPath(srcPath, dst.getName())),
//                                                        newDstPath));
 
            //
            sendQueue.add(new DeploymentWork(new DeploymentEvent(DeploymentEvent.Type.DELETED,
                    new Pair<Integer, String>(version, extendPath(srcPath, dst.getName())), 
                    newDstPath), ticket));
            
            //

            dst = null;
        }
    }

    /**
     * Copy a file or directory to an empty destination on an FSR
     * @param service
     * @param ticket
     * @param report
     * @param callback
     * @param version
     * @param src
     * @param parentPath
     */
    private void createOnFSR(DeploymentReceiverService service, 
    		String ticket,
            int version, 
            AVMNodeDescriptor src, 
            String parentPath, 
            NameMatcher matcher,
    		BlockingQueue<DeploymentWork> sendQueue)
    {
        String dstPath = extendPath(parentPath, src.getName());
        
    	sendQueue.add(new DeploymentWork(
    			new DeploymentEvent(DeploymentEvent.Type.COPIED,
                new Pair<Integer, String>(version, src.getPath()),                              
                dstPath), ticket, src));
    	
        if (src.isFile())
        {
 //           copyFileToFSR(service, ticket, version, src, dstPath, true, transformers);
            return;
        }
        
        // Need to create directories in controlling thread since then need to be BEFORE any children are written
        
        // here if src is a directory.   
    	service.mkdir(ticket, dstPath, src.getGuid());

        // now copy the children over
        Map<String, AVMNodeDescriptor> listing = fAVMService.getDirectoryListing(src);
        for (AVMNodeDescriptor child : listing.values())
        {
            if (!excluded(matcher, child.getPath(), null))
            {
                createOnFSR(service, ticket, version, child, dstPath, matcher, sendQueue);
            }
        }
    }
    
    private void processEvent(DeploymentEvent event,  List<DeploymentCallback> callbacks)
    {
        if (fgLogger.isDebugEnabled())
        {
            fgLogger.debug(event);
        }
        if (callbacks != null)
        {
            for (DeploymentCallback callback : callbacks)
            {
                callback.eventOccurred(event);
            }
        }
    }

    /**
     * Extend a path.
     * @param path
     * @param name
     * @return
     */
    private String extendPath(String path, String name)
    {
        if (path.endsWith("/"))
        {
            return path + name;
        }
        return path + '/' + name;
    }

    /**
     * Returns true if either srcPath or dstPath are matched by matcher.
     * @param matcher
     * @param srcPath
     * @param dstPath
     * @return
     */
    private boolean excluded(NameMatcher matcher, String srcPath, String dstPath)
    {
        return matcher != null && ((srcPath != null && matcher.matches(srcPath)) || (dstPath != null && matcher.matches(dstPath)));
    }

    /**
     * Get the object to lock for an alfresco->alfresco target.
     * @param host
     * @param port
     * @return the lock
     */
    private synchronized DeploymentDestination getLock(String host, int port)
    {
        DeploymentDestination newDest = new DeploymentDestination(host, port);
        DeploymentDestination dest = fDestinations.get(newDest);
        if (dest == null)
        {
            dest = newDest;
            fDestinations.put(dest, dest);
        }
        return dest;
    }
    

    private Map<String, DeploymentReceiverTransportAdapter> deploymentReceiverTransportAdapters;
    /**
     * The deployment transport adapters provide the factories used to connect to a remote file system receiver.
     */
    public void setDeploymentReceiverTransportAdapters(Map<String, DeploymentReceiverTransportAdapter> adapters) {
    	this.deploymentReceiverTransportAdapters=adapters;
    }
    
    public Map<String, DeploymentReceiverTransportAdapter> getDeploymentReceiverTransportAdapters() {
    	return this.deploymentReceiverTransportAdapters;
    }

	public Set<String> getAdapterNames() 
	{
		if(deploymentReceiverTransportAdapters != null) {
			return(deploymentReceiverTransportAdapters.keySet());
		}	
		else 
		{
			Set<String> ret = new HashSet<String>(1);
			ret.add("default");
			return ret;
		}
	}
	
	public void setNumberOfSendingThreads(int numberOfSendingThreads) {
		this.numberOfSendingThreads = numberOfSendingThreads;
	}

	public int getNumberOfSendingThreads() {
		return numberOfSendingThreads;
	}

	/**
	 * This thread processes the event queue to do the callbacks
	 * @author mrogers
	 *
	 */
	private class EventQueueWorker extends Thread
	{
		private BlockingQueue<DeploymentEvent> eventQueue;
		private List<DeploymentCallback> callbacks;
		private String userName;
		
		private boolean stopMe = false;
		
		EventQueueWorker(String userName, BlockingQueue<DeploymentEvent> eventQueue, List<DeploymentCallback> callbacks)
		{
			this.eventQueue = eventQueue;
			this.callbacks = callbacks;
			this.userName = userName;
		}
		
		public void run()
		{
		    AuthenticationUtil.setFullyAuthenticatedUser(userName);
		    
			while (true)
			{
				DeploymentEvent event = null;
				try {
					event = eventQueue.poll(3, TimeUnit.SECONDS);
				} catch (InterruptedException e1) {
					fgLogger.debug("Interrupted ", e1);
				}
		
				if(event == null) 
				{
					if(stopMe) 
					{
						fgLogger.debug("Event Queue Closing Normally");
						break;
					}
					continue;
				}
				
				if (fgLogger.isDebugEnabled())
		        {
		            fgLogger.debug(event);
		        }
		        if (callbacks != null)
		        {
		            for (DeploymentCallback callback : callbacks)
		            {
		                callback.eventOccurred(event);
		            }
		        }
			}
		}
		
		public void stopMeWhenIdle() 
		{
			stopMe = true;
		}
		
	}
	
	/**
	 * This thread processes the send queue
	 * @author mrogers
	 *
	 */
	private class SendQueueWorker extends Thread
	{
		private BlockingQueue<DeploymentEvent> eventQueue;
		private BlockingQueue<DeploymentWork> sendQueue;
		private DeploymentReceiverService service;
		private String userName;
		private AVMService avmService;
		private TransactionService trxService;
		List<Exception> errors;
		List<DeploymentTransportOutputFilter> transformers;
		
		private boolean stopMe = false;
		
		SendQueueWorker(String userName,
				DeploymentReceiverService service,
				AVMService avmService,
				TransactionService trxService,
				List<Exception> errors,
				BlockingQueue<DeploymentEvent> eventQueue, 
				BlockingQueue<DeploymentWork> sendQueue,
				List<DeploymentTransportOutputFilter> transformers
				)
		{
			this.eventQueue = eventQueue;
			this.sendQueue = sendQueue;
			this.service = service;
			this.avmService = avmService;
			this.trxService = trxService;
			this.errors = errors;
			this.transformers = transformers;
			this.userName = userName;
		}
		
		public void run()
		{
		    AuthenticationUtil.setFullyAuthenticatedUser(userName);
            
			while (errors.size() <= 0)
			{
				DeploymentWork work = null;
				try {
					work = sendQueue.poll(3, TimeUnit.SECONDS);
				} catch (InterruptedException e1) {
					fgLogger.debug("Interrupted ", e1);
					continue;
				}
								
				if(work == null) 
				{
					if(stopMe) 
					{	
						fgLogger.debug("Send Queue Worker Closing Normally");
						eventQueue = null;
						sendQueue = null;
						service = null;
						errors = null;
						break;
					}
				}
				
				if(work != null)
				{
					DeploymentEvent event = work.getEvent();
					String ticket = work.getTicket();
					try 
					{
						if(event.getType().equals(DeploymentEvent.Type.DELETED))
						{
							service.delete(ticket, event.getDestination());
						} 
						else if (event.getType().equals(DeploymentEvent.Type.COPIED))
						{
							AVMNodeDescriptor src = work.getSrc();
							if(src.isFile())
							{
								copyFileToFSR(src, event.getDestination(), ticket);
							}
							else
							{
								// Do nothing. mkdir done on main thread. 
								//makeDirectoryOnFSR(src, event.getDestination(), ticket);
							}
						}
						else if (event.getType().equals(DeploymentEvent.Type.UPDATED))
						{
							copyFileToFSR(work.getSrc(), event.getDestination(), ticket);
						}
						// success, now put the event onto the event queue
						eventQueue.add(event);
					}
					catch (Exception e)
					{
						errors.add(e);
					}
				}
			}
			fgLogger.debug("Send Queue Worker finished");
		}
		
		public void stopMeWhenIdle() 
		{
			stopMe = true;
		}
		
		
	   /**
	     * Create or update a single file on a remote FSR. 
	     * @param ticket
	     * @param src which file to copy
	     * @param dstPath where to copy the file
	     */
	    private void copyFileToFSR(
	            final AVMNodeDescriptor src, 
	            final String dstPath,
	            final String ticket)
	    {
	        try
	        {
	            // Perform copy within 'read only' transaction
	            RetryingTransactionHelper trx = trxService.getRetryingTransactionHelper();
	            trx.setMaxRetries(1);
	            trx.doInTransaction(new RetryingTransactionCallback<Boolean>()
                {
                    public Boolean execute() throws Exception
                    {
        	        	InputStream in = avmService.getFileInputStream(src);
        	        
        	        	OutputStream out = service.send(ticket, dstPath, src.getGuid());
        	        	OutputStream baseStream = out; // finish send needs out, not a decorated stream
        	        
        	        	// Buffer the output, we don't want to send lots of small packets
        	        	out = new BufferedOutputStream(out, 10000);
        	        
        	        	// Call content transformers here to transform from local to network format
        	        	if(transformers != null && transformers.size() > 0) {
        	        		// yes we have pay-load transformers
        	        		for(DeploymentTransportOutputFilter transformer : transformers) 
        	        		{
        	        			out = transformer.addFilter(out, src.getPath());
        	        		}
        	        	}
        	        		        
        	            copyStream(in, out);
                        service.finishSend(ticket, baseStream);
                        return true;
                    }
                }, true);
	        }
	        catch (Exception e)
	        {
	            fgLogger.debug("Failed to copy dstPath:" + dstPath , e);
	            
	            // throw first exception - this is the root of the problem.
	            throw new AVMException("Failed to copy filename:" + dstPath, e);
	        }
	    }
	}
}
