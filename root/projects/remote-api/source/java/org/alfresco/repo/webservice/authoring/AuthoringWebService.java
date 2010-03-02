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
package org.alfresco.repo.webservice.authoring;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.repo.webservice.AbstractWebService;
import org.alfresco.repo.webservice.Utils;
import org.alfresco.repo.webservice.types.ContentFormat;
import org.alfresco.repo.webservice.types.NamedValue;
import org.alfresco.repo.webservice.types.ParentReference;
import org.alfresco.repo.webservice.types.Predicate;
import org.alfresco.repo.webservice.types.Reference;
import org.alfresco.repo.webservice.types.VersionHistory;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Web service implementation of the AuthoringService. The WSDL for this service
 * can be accessed from
 * http://localhost:8080/alfresco/wsdl/authoring-service.wsdl
 * 
 * @author gavinc
 */
public class AuthoringWebService extends AbstractWebService implements
        AuthoringServiceSoapPort
{
    /**
     * Logger
     */
    private static Log logger = LogFactory.getLog(AuthoringWebService.class);

    /**
     * The check in check out service
     */
    private CheckOutCheckInService cociService;
    
    /**
     * The lock service
     */
    private LockService lockService;
    
    /**
     * The version service
     */
    private VersionService versionService;

    /**
     * The transaction service
     */
    private TransactionService transactionService;

    /**
     * Sets the CheckInCheckOutService to use
     * 
     * @param cociService
     *            The CheckInCheckOutService
     */
    public void setCheckOutCheckinService(CheckOutCheckInService cociService)
    {
        this.cociService = cociService;
    }
    
    /**
     * Sets the LockService to use
     * 
     * @param lockService   the lock service
     */
    public void setLockService(LockService lockService)
    {
        this.lockService = lockService;
    }
    
    /**
     * Set the version service
     * 
     * @param versionService    the version service
     */
    public void setVersionService(VersionService versionService)
    {
        this.versionService = versionService;
    }

    /**
     * Set the transaction service
     * 
     * @param transactionService
     *            the transaction service
     */
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * @see org.alfresco.repo.webservice.authoring.AuthoringServiceSoapPort#checkout(org.alfresco.repo.webservice.types.Predicate,
     *      org.alfresco.repo.webservice.types.ParentReference)
     */
    public CheckoutResult checkout(final Predicate items, final ParentReference destination) throws RemoteException,
            AuthoringFault
    {
        try
        {
            return transactionService.getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<CheckoutResult>()
                    {
                        public CheckoutResult execute()
                        {
                            List<NodeRef> nodes = Utils.resolvePredicate(items,
                                    AuthoringWebService.this.nodeService,
                                    AuthoringWebService.this.searchService,
                                    AuthoringWebService.this.namespaceService);
                            CheckoutResult checkoutResult = new CheckoutResult();
                            Reference[] originals = new Reference[nodes.size()];
                            Reference[] workingCopies = new Reference[nodes.size()];
    
                            // get a repository NodeRef for the destination (if
                            // there is one)
                            NodeRef destinationRef = null;
                            if (destination != null)
                            {
                                destinationRef = Utils.convertToNodeRef(
                                        destination,
                                        AuthoringWebService.this.nodeService,
                                        AuthoringWebService.this.searchService,
                                        AuthoringWebService.this.namespaceService);
                            }
    
                            for (int x = 0; x < nodes.size(); x++)
                            {
                                // get the current node
                                NodeRef original = nodes.get(x);
    
                                // call the appropriate service method depending on
                                // whether a destination has been provided
                                NodeRef workingCopy = null;
                                if (destinationRef != null)
                                {                                    
                                     workingCopy = AuthoringWebService.this.cociService
                                            .checkout(
                                                    original, 
                                                    destinationRef,
                                                    QName.createQName(destination.getAssociationType()),
                                                    QName.createQName(destination.getChildName()));
                                } else
                                {
                                    workingCopy = AuthoringWebService.this.cociService
                                            .checkout(original);
                                }
    
                                // store the results
                                originals[x] = Utils.convertToReference(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, original);
                                workingCopies[x] = Utils.convertToReference(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, workingCopy);
                            }
    
                            // setup the result object
                            checkoutResult.setOriginals(originals);
                            checkoutResult.setWorkingCopies(workingCopies);
    
                            return checkoutResult;
                        }
                    });
        }
        catch (Throwable e)
        {
           if (logger.isDebugEnabled())
           {
              logger.error("Unexpected error occurred", e);
           }
         
           throw new AuthoringFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.authoring.AuthoringServiceSoapPort#checkin(org.alfresco.repo.webservice.types.Predicate,
     *      org.alfresco.repo.webservice.types.NamedValue[], boolean)
     */
    public CheckinResult checkin(final Predicate items,
            final NamedValue[] comments, final boolean keepCheckedOut)
            throws RemoteException, AuthoringFault
    {
        try
        {
            return transactionService.getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<CheckinResult>()
                    {
                        public CheckinResult execute()
                        {
                            // Get the passed nodes
                            List<NodeRef> nodes = Utils.resolvePredicate(
                                    items,
                                    AuthoringWebService.this.nodeService,
                                    AuthoringWebService.this.searchService,
                                    AuthoringWebService.this.namespaceService);
    
                            // Map the comments into the expected map
                            Map<String, Serializable> mapComments = convertCommnets(comments);
                            
                            Reference[] checkedIn = new Reference[nodes.size()];
                            List<Reference> listWorkingCopies = new ArrayList<Reference>(nodes.size());
                            int iIndex = 0;
    
                            // Execute checkin for each node
                            // TODO should be able to do this as a batch so that all the nodes are versioned together
                            for (NodeRef node : nodes)
                            {
                                // Checkin the node
                                NodeRef checkedInNode = AuthoringWebService.this.cociService.checkin(node, mapComments, null, keepCheckedOut);
                                
                                // Add the results to the array
                                checkedIn[iIndex] = Utils.convertToReference(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, checkedInNode);
                                
                                // Only return the working copies if the node is keep checked out otherwise the working copies have been deleted
                                if (keepCheckedOut == true)
                                {
                                    listWorkingCopies.add(Utils.convertToReference(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, node));
                                }                                
                                iIndex++;
                            }
                            
                            // Sort out the working copy list
                            Reference[] workingCopies = listWorkingCopies.toArray(new Reference[listWorkingCopies.size()]);
                            if (workingCopies == null)
                            {
                                workingCopies = new Reference[0];
                            }
                            
                            // Create the result object
                            CheckinResult result = new CheckinResult();
                            result.setCheckedIn(checkedIn);
                            result.setWorkingCopies(workingCopies);
                            
                            return result;
                        }
                    });
        }
        catch (Throwable e)
        {
           if (logger.isDebugEnabled())
           {
              logger.error("Unexpected error occurred", e);
           }
         
           throw new AuthoringFault(0, e.getMessage());
        }
    }
    
    /**
     * Takes the named value array that contains the comments and converts it to a Map that the underlying
     * services can understand.
     * 
     * @param comments						the comments named value array
     * @return Map<String, Serializable>	Map that can be used with the underlying services
     */
    private Map<String, Serializable> convertCommnets(NamedValue[] comments)
    {
    	// Map the comments into the expected map
    	Map<String, Serializable> mapComments = new HashMap<String, Serializable>(comments.length);
    	for (NamedValue value : comments)
    	{
    		if (value.getName().equals(VersionModel.PROP_VERSION_TYPE) == true)
    		{
    			mapComments.put(value.getName(), VersionType.valueOf(value.getValue()));
    		}
    		else
    		{
    			mapComments.put(value.getName(), value.getValue());
    		}
    	}    	
    	return mapComments;
    }

    /**
     * @see org.alfresco.repo.webservice.authoring.AuthoringServiceSoapPort#checkinExternal(org.alfresco.repo.webservice.types.Reference, org.alfresco.repo.webservice.types.NamedValue[], boolean, org.alfresco.repo.webservice.types.ContentFormat, byte[])
     */
    public Reference checkinExternal(final Reference node, final NamedValue[] comments, final boolean keepCheckedOut, final ContentFormat format, final byte[] content)
            throws RemoteException, AuthoringFault
    {
        try
        {
            return transactionService.getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<Reference>()
                    {
                        public Reference execute()
                        {
                            // Get the passed nodes
                            NodeRef nodeRef = Utils.convertToNodeRef(
                                    node,
                                    AuthoringWebService.this.nodeService,
                                    AuthoringWebService.this.searchService,
                                    AuthoringWebService.this.namespaceService);
                            
                            // Write the content to the server
                            // TODO: Need to get the property QName into this method
                            ContentWriter contentWriter = AuthoringWebService.this.contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, false);
                            if (contentWriter == null)
                            {
                                throw new RuntimeException("Unable to write external content before checkin.");
                            }
                            InputStream is = new ByteArrayInputStream(content);
                            contentWriter.setEncoding(format.getEncoding());
                            contentWriter.setMimetype(format.getMimetype());
                            contentWriter.putContent(is);
                            String contentUrl = contentWriter.getContentUrl();
                            
                            // Get the version properties map
                            Map<String, Serializable> versionProperties = convertCommnets(comments);
                            
                            // CheckIn the content
                            NodeRef origNodeRef = AuthoringWebService.this.cociService.checkin(
                                                                        nodeRef, 
                                                                        versionProperties, 
                                                                        contentUrl, 
                                                                        keepCheckedOut);
                            // Return the orig node ref
                            return Utils.convertToReference(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, origNodeRef);
                        }
                    });
        }
        catch (Throwable e)
        {
           if (logger.isDebugEnabled())
           {
              logger.error("Unexpected error occurred", e);
           }
         
           throw new AuthoringFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.authoring.AuthoringServiceSoapPort#cancelCheckout(org.alfresco.repo.webservice.types.Predicate)
     */
    public CancelCheckoutResult cancelCheckout(final Predicate items)
            throws RemoteException, AuthoringFault
    {
        try
        {
            return transactionService.getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<CancelCheckoutResult>()
                    {
                        public CancelCheckoutResult execute()
                        {
                            // Get the passed nodes
                            List<NodeRef> nodes = Utils.resolvePredicate(
                                    items,
                                    AuthoringWebService.this.nodeService,
                                    AuthoringWebService.this.searchService,
                                    AuthoringWebService.this.namespaceService);
                            
                            // Create the arrays to hold results
                            Reference[] origNodes = new Reference[nodes.size()];
                            Reference[] workingCopies = new Reference[nodes.size()];
                            int iIndex = 0;
                            
                            for (NodeRef node : nodes)
                            {
                                // Cancel the checkout
                                NodeRef origNode = AuthoringWebService.this.cociService.cancelCheckout(node);
                                
                                // Set the value in the arrays
                                origNodes[iIndex] = Utils.convertToReference(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, origNode);
                                workingCopies[iIndex] = Utils.convertToReference(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, node);
                                iIndex ++;
                            }
                            
                            CancelCheckoutResult result = new CancelCheckoutResult();
                            result.setOriginals(origNodes);
                            result.setWorkingCopies(workingCopies);
                            
                            return result;
                        }
                    });
        }
        catch (Throwable e)
        {
           if (logger.isDebugEnabled())
           {
              logger.error("Unexpected error occurred", e);
           }
         
           throw new AuthoringFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.authoring.AuthoringServiceSoapPort#lock(org.alfresco.repo.webservice.types.Predicate,
     *      boolean, org.alfresco.repo.webservice.authoring.LockTypeEnum)
     */
    public Reference[] lock(final Predicate items, final boolean lockChildren, final LockTypeEnum lockType) 
            throws RemoteException, AuthoringFault
    {
        try
        {
            return transactionService.getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<Reference[]>()
                    {
                        public Reference[] execute()
                        {
                            // Get the passed nodes
                            List<NodeRef> nodes = Utils.resolvePredicate(
                                    items,
                                    AuthoringWebService.this.nodeService,
                                    AuthoringWebService.this.searchService,
                                    AuthoringWebService.this.namespaceService);
                            
                            // Gather together the results
                            Reference[] result = new Reference[nodes.size()];
                            int iIndex = 0;
                            for (NodeRef node : nodes)
                            {
                                LockType convertedLockType = convertToLockType(lockType);
                                AuthoringWebService.this.lockService.lock(node, convertedLockType, 0, lockChildren);
                                result[iIndex] = Utils.convertToReference(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, node);
                                iIndex++;
                            }                        
                            
                            return result;
                        }
                    });
        }
        catch (Throwable e)
        {
           if (logger.isDebugEnabled())
           {
              logger.error("Unexpected error occurred", e);
           }
           
           throw new AuthoringFault(0, e.getMessage());
        }
    }
    
    /**
     * Convert from the web service lock type to the Lock type enum value used by the service interface
     * 
     * @param lockTypeEnum  web service lock type value
     * @return              lock type enum value used by the service interface
     */
    private LockType convertToLockType(LockTypeEnum lockTypeEnum)
    {
        LockType lockType = null;
        if (lockTypeEnum.equals(LockTypeEnum.write) == true)
        {
            lockType = LockType.WRITE_LOCK;
        }
        else
        {
            lockType = LockType.READ_ONLY_LOCK;
        }
        return lockType;
    }

    /**
     * @see org.alfresco.repo.webservice.authoring.AuthoringServiceSoapPort#unlock(org.alfresco.repo.webservice.types.Predicate,
     *      boolean)
     */
    public Reference[] unlock(final Predicate items, final boolean unlockChildren)
            throws RemoteException, AuthoringFault
    {
        try
        {
            return transactionService.getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<Reference[]>()
                    {
                        public Reference[] execute()
                        {
                            // Get the passed nodes
                            List<NodeRef> nodes = Utils.resolvePredicate(
                                    items,
                                    AuthoringWebService.this.nodeService,
                                    AuthoringWebService.this.searchService,
                                    AuthoringWebService.this.namespaceService);
                            
                            // Gather together the results
                            Reference[] result = new Reference[nodes.size()];
                            int iIndex = 0;
                            for (NodeRef node : nodes)
                            {
                                AuthoringWebService.this.lockService.unlock(node, unlockChildren);
                                
                                result[iIndex] = Utils.convertToReference(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, node);
                                iIndex++;
                            }                        
                            
                            return result;
                        }
                    });
        }
        catch (Throwable e)
        {
           if (logger.isDebugEnabled())
           {
              logger.error("Unexpected error occurred", e);
           }
         
           throw new AuthoringFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.authoring.AuthoringServiceSoapPort#getLockStatus(org.alfresco.repo.webservice.types.Predicate)
     */
    public LockStatus[] getLockStatus(final Predicate items) 
            throws RemoteException, AuthoringFault
    {
        try
        {
            return transactionService.getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<LockStatus[]>()
                    {
                        public LockStatus[] execute()
                        {
                            // Get the passed nodes
                            List<NodeRef> nodes = Utils.resolvePredicate(
                                    items,
                                    AuthoringWebService.this.nodeService,
                                    AuthoringWebService.this.searchService,
                                    AuthoringWebService.this.namespaceService);
                            
                            // Gather together the results
                            LockStatus[] result = new LockStatus[nodes.size()];
                            int iIndex = 0;
                            for (NodeRef node : nodes)
                            {
                                // Get the lock owner
                                String lockOwner = (String)AuthoringWebService.this.nodeService.getProperty(node, ContentModel.PROP_LOCK_OWNER);
                                
                                // Get the lock type
                                LockTypeEnum lockTypeEnum = convertFromLockType(AuthoringWebService.this.lockService.getLockType(node));
    
                                LockStatus lockStatus = new LockStatus();
                                lockStatus.setLockOwner(lockOwner);
                                lockStatus.setLockType(lockTypeEnum);
                                lockStatus.setNode(Utils.convertToReference(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, node));
                                
                                result[iIndex] = lockStatus;
                                iIndex++;
                            }                        
                            
                            return result;
                        }
                    });
        }
        catch (Throwable e)
        {
            
           if (logger.isDebugEnabled())
           {
              logger.error("Unexpected error occurred", e);
           }
                    
           e.printStackTrace();
           throw new AuthoringFault(0, e.getMessage());
        }
    }
    
    private LockTypeEnum convertFromLockType(LockType lockType)
    {
        LockTypeEnum result = null; 
        if (lockType != null)
        {
            switch (lockType)
            {
                case WRITE_LOCK:
                    result =  LockTypeEnum.write;
                    break;
                case READ_ONLY_LOCK:
                    result = LockTypeEnum.read;
                    break;                
            }
        }
        return result;
    }

    /**
     * @see org.alfresco.repo.webservice.authoring.AuthoringServiceSoapPort#createVersion(org.alfresco.repo.webservice.types.Predicate,
     *      org.alfresco.repo.webservice.types.NamedValue[], boolean)
     */
    public VersionResult createVersion(final Predicate items, final NamedValue[] comments, final boolean versionChildren) 
                throws RemoteException, AuthoringFault
    {
        try
        {
            return transactionService.getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<VersionResult>()
                    {
                        public VersionResult execute()
                        {
                            // Get the passed nodes
                            List<NodeRef> nodes = Utils.resolvePredicate(
                                    items,
                                    AuthoringWebService.this.nodeService,
                                    AuthoringWebService.this.searchService,
                                    AuthoringWebService.this.namespaceService);
                            
                            // Map the comments into the expected map
                            Map<String, Serializable> mapComments = convertCommnets(comments);
                            
                            List<Reference> versionedReferences = new ArrayList<Reference>(nodes.size());
                            List<org.alfresco.repo.webservice.types.Version> webServiceVersions = new ArrayList<org.alfresco.repo.webservice.types.Version>(nodes.size());
                            
                            // Version each node
                            for (NodeRef node : nodes)
                            {
                                Collection<Version> versions = AuthoringWebService.this.versionService.createVersion(node, mapComments, versionChildren);
                                for (Version version : versions)
                                {
                                    versionedReferences.add(Utils.convertToReference(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, version.getVersionedNodeRef()));
                                    webServiceVersions.add(Utils.convertToVersion(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, version));
                                }
                            }
                            
                            VersionResult result = new VersionResult();                        
                            result.setNodes(versionedReferences.toArray(new Reference[versionedReferences.size()]));
                            result.setVersions(webServiceVersions.toArray(new org.alfresco.repo.webservice.types.Version[webServiceVersions.size()]));
                            return result;
                        }
                    });
        }
        catch (Throwable e)
        {
           if (logger.isDebugEnabled())
           {
              logger.error("Unexpected error occurred", e);
           }
           
           e.printStackTrace();
         
           throw new AuthoringFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.authoring.AuthoringServiceSoapPort#getVersionHistory(org.alfresco.repo.webservice.types.Reference)
     */
    public VersionHistory getVersionHistory(final Reference node)
            throws RemoteException, AuthoringFault
    {
        try
        {
            return transactionService.getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<VersionHistory>()
                    {
                        public VersionHistory execute()
                        {
                            org.alfresco.service.cmr.version.VersionHistory versionHistory = 
                                AuthoringWebService.this.versionService.getVersionHistory(
                                                                            Utils.convertToNodeRef(
                                                                                node, 
                                                                                AuthoringWebService.this.nodeService, 
                                                                                AuthoringWebService.this.searchService,
                                                                                AuthoringWebService.this.namespaceService));
                            
                            VersionHistory webServiceVersionHistory = new VersionHistory();
                            if (versionHistory != null)
                            {
                                Collection<Version> versions = versionHistory.getAllVersions();
                                org.alfresco.repo.webservice.types.Version[] webServiceVersions = new org.alfresco.repo.webservice.types.Version[versions.size()];
                                int iIndex = 0;
                                for (Version version : versions)
                                {
                                    webServiceVersions[iIndex] = Utils.convertToVersion(AuthoringWebService.this.nodeService, AuthoringWebService.this.namespaceService, version);
                                    iIndex ++;
                                }
                                webServiceVersionHistory.setVersions(webServiceVersions);
                            }
                            
                            return webServiceVersionHistory;
                        }
                    });
        }
        catch (Throwable e)
        {
           if (logger.isDebugEnabled())
           {
              logger.error("Unexpected error occurred", e);
           }
         
           throw new AuthoringFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.authoring.AuthoringServiceSoapPort#revertVersion(org.alfresco.repo.webservice.types.Reference,
     *      java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public void revertVersion(final Reference node, final String versionLabel)
            throws RemoteException, AuthoringFault
    {
        try
        {
            transactionService.getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<Object>()
                    {
                        public Object execute()
                        {
                            NodeRef nodeRef = Utils.convertToNodeRef(
                                                        node,
                                                        AuthoringWebService.this.nodeService,
                                                        AuthoringWebService.this.searchService,
                                                        AuthoringWebService.this.namespaceService);
                            
                            org.alfresco.service.cmr.version.VersionHistory versionHistory = AuthoringWebService.this.versionService.getVersionHistory(nodeRef);
                            if (versionHistory != null)
                            {                            
                                Version version = versionHistory.getVersion(versionLabel);
                                if (version != null)
                                {
                                    AuthoringWebService.this.versionService.revert(nodeRef, version);
                                }
                                else
                                {
                                    throw new RuntimeException("The node could not be reverted because the version label is invalid.");
                                }
                            }
                            else
                            {
                                throw new RuntimeException("A unversioned node cannot be reverted.");
                            }
                            
                            return null;
                        }
                    });
        }
        catch (Throwable e)
        {
           if (logger.isDebugEnabled())
           {
              logger.error("Unexpected error occurred", e);
           }
         
           throw new AuthoringFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.authoring.AuthoringServiceSoapPort#deleteAllVersions(org.alfresco.repo.webservice.types.Reference)
     */
    public VersionHistory deleteAllVersions(final Reference node)
            throws RemoteException, AuthoringFault
    {
        try
        {
            return transactionService.getRetryingTransactionHelper().doInTransaction(
                    new RetryingTransactionCallback<VersionHistory>()
                    {
                        public VersionHistory execute()
                        {
                            NodeRef nodeRef = Utils.convertToNodeRef(
                                                        node,
                                                        AuthoringWebService.this.nodeService,
                                                        AuthoringWebService.this.searchService,
                                                        AuthoringWebService.this.namespaceService);
                            
                            AuthoringWebService.this.versionService.deleteVersionHistory(nodeRef);
                            
                            return new VersionHistory();
                        }
                    });
        }
        catch (Throwable e)
        {
           if (logger.isDebugEnabled())
           {
              logger.error("Unexpected error occurred", e);
           }
         
           throw new AuthoringFault(0, e.getMessage());
        }
    }
}
