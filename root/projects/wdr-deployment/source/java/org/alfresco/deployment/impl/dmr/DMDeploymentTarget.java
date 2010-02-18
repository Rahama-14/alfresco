/*
 * Copyright (C) 2009-2009 Alfresco Software Limited.
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
 * http://www.alfresco.com/legal/licensing
 */

package org.alfresco.deployment.impl.dmr;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.alfresco.deployment.FSDeploymentRunnable;
import org.alfresco.deployment.FileDescriptor;
import org.alfresco.deployment.FileType;
import org.alfresco.deployment.impl.DeploymentException;
import org.alfresco.deployment.impl.server.DeployedFile;
import org.alfresco.deployment.impl.server.Deployment;
import org.alfresco.deployment.impl.server.DeploymentReceiverAuthenticator;
import org.alfresco.deployment.impl.server.DeploymentState;
import org.alfresco.deployment.DeploymentTarget;
import org.alfresco.model.ContentModel;
import org.alfresco.model.WCMAppModel;
import org.alfresco.repo.model.filefolder.FileFolderServiceImpl;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This implements a deployment target for deployment to a DM Store  
 */
public class DMDeploymentTarget implements Serializable, DeploymentTarget
{   
	/**
	 * version id
	 */
	private static final long serialVersionUID = 1257869549338878302L;
   
    /**
     * Deployments in progress
     */
    private Map<String, DMDeployment> fDeployments = Collections.synchronizedMap(new HashMap<String, DMDeployment>());
    
    /**
     * The logger for this target
     */
    private static Log logger = LogFactory.getLog(DMDeploymentTarget.class);

    /**
     * The name of this target.
     */
    private String fTargetName;
    
    /**
     * Name mapper
     */
    private StoreNameMapper nameMapper;
    
    /**
     * Root Locator
     */
    private RootLocator rootLocator;
    
    /**
     * Which store to use for our deployed content
     */
    private String storeRef = "workspace://SpacesStore";
            
    /**
     * The authenticator for this target
     */
    private DeploymentReceiverAuthenticator authenticator;
       
    /**
     * Runnables that will be invoked after commit.
     */
    private List<FSDeploymentRunnable> postCommit;
    
    /**
     * Runnables that will be invoked during prepare phase.
     */
    private List<FSDeploymentRunnable> prepare;
    	   
    private TransactionService trxService;
    
    private FileFolderService fileFolderService;
    
    private NodeService nodeService;
    
    private SearchService searchService;
    
    private DictionaryService dictionaryService;
    
    /**
     * initialise this target
     */
    public void init() 
    {
        PropertyCheck.mandatory(this, "authenticator", authenticator);
        PropertyCheck.mandatory(this, "trxService", trxService); 
        PropertyCheck.mandatory(this, "fileFolderService", fileFolderService); 
        PropertyCheck.mandatory(this, "nodeService", nodeService); 
        PropertyCheck.mandatory(this, "dictionaryService", getDictionaryService()); 
        PropertyCheck.mandatory(this, "nameMapper", nameMapper); 
        PropertyCheck.mandatory(this, "rootLocator", rootLocator); 
    }
    
    /**
     * Get the target name.
     * @return
     */
    public String getName()
    {
        return fTargetName;
    }
    
    /**
     * Implementation of begin for DMR
     */
	public synchronized String begin(String targetName, final String storeName, int version, String user, char[] password) 
	{ 
		// Authenticate with the user and password
		if(!authenticator.logon(user, password))
		{
			logger.warn("Invalid user name or password");
			throw new DeploymentException("Invalid user name or password.");
		}
    
        String ticket = GUID.generate();
        logger.debug("begin deploy, target:" + targetName + ", ticket:" + ticket);
        
        final String localStoreName =  nameMapper.mapProjectName(storeName);
        
        RetryingTransactionHelper tran = trxService.getRetryingTransactionHelper();
       
        RetryingTransactionCallback<FileInfo> createCB = new RetryingTransactionCallback<FileInfo>()
        {
            public FileInfo execute() throws Throwable
            {
                /**
                 * Get the DM deployment root and make sure it exists.
                 */ 
                String rootQuery = rootLocator.getRootQuery(localStoreName);
                ResultSet result = searchService.query(new StoreRef(getStoreRef()), SearchService.LANGUAGE_XPATH, rootQuery);
                
                if(result.length() != 1)
                {
                    throw new DeploymentException("root path not found or not unique:" + rootQuery);
                }
                NodeRef rootNodeRef = result.getNodeRef(0);
                                
                /**
                 * If the project root does not exist then create it
                 */
                NodeRef childNode = nodeService.getChildByName(rootNodeRef, ContentModel.ASSOC_CONTAINS, localStoreName);
                if(childNode != null)
                {
                    logger.debug("project root already exists" + localStoreName);
                    if(!nodeService.hasAspect(childNode, WCMAppModel.ASPECT_DEPLOYED))
                    {
                        logger.debug("adding deployed aspect - did not exist");
                        setWCMGuid(childNode, "0");
                    }
                }
                else
                {    
                    logger.debug("project root not found - create new one" + localStoreName);
                    Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>(3, 1.0f);
                    contentProps.put(ContentModel.PROP_NAME, localStoreName);
                    contentProps.put(ContentModel.PROP_TITLE, "Web Project :" + storeName);
                    contentProps.put(ContentModel.PROP_DESCRIPTION, "WCM Deployed root web-project: " + storeName);
                    QName localQName = QName.createQName(
                            NamespaceService.CONTENT_MODEL_1_0_URI,
                            QName.createValidLocalName(localStoreName));
                    ChildAssociationRef childRef = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CONTAINS, localQName, ContentModel.TYPE_FOLDER, contentProps);
                    childNode = childRef.getChildRef();
                    setWCMGuid(childNode, "0");
                    logger.debug("project root node created");
                }
                
                return fileFolderService.getFileInfo(childNode);          
            }
        };
        
        FileInfo rootNode = tran.doInTransaction(createCB);

        try
        {
            DMDeployment deployment = new DMDeployment(ticket, targetName, storeName, version, rootNode);
            fDeployments.put(ticket, deployment);
        }
        catch (IOException e)
        {
        	logger.error("Could not create logfile", e);
            throw new DeploymentException("Could not create logfile; Deployment cannot continue", e);
        }
        if(logger.isDebugEnabled())
        {
            logger.debug("success - begin deployment:" + ticket);
        }
        return ticket;
	}
	
	
	public void prepare(String ticket) 
	{
	  	logger.info("Prepare ticket: " + ticket);
        Deployment deployment = fDeployments.get(ticket);
        if (deployment == null)
        {
        	logger.debug("Could not prepare: invalid token ticket:" + ticket);
        	// We are most likely to get here because we are aborting an already aborted ticket
        	return;
        }
        if (deployment.getState() != DeploymentState.WORKING)
        {
            throw new DeploymentException("Deployment cannot be prepared: already aborting, or committing.");
        }
        try
        {
            /**
             * Check that the temporary files are still there
             */
            for(DeployedFile file : deployment)
            {
            	if(file.getType() == FileType.FILE)
            	{
            		File content = new File(file.getPreLocation());            		
            		if(!content.exists())
            		{
            			throw new DeploymentException("Unable to prepare, missing temporary file." + content.getAbsolutePath());
            		}
            	}
            }
            
            /**
             *  Run any end user callbacks
             */
            if (prepare != null && prepare.size() > 0)
            {
                for (FSDeploymentRunnable runnable : prepare)
                {
                    try
                    {
                        runnable.init(deployment);
                        runnable.run();
                    }
                    catch (Throwable t)
                    {
                        String msg = "Error thrown in prepare; rolled back";
                        
                        if(t.getCause() != null)
                        {
                           msg = msg + " :" + t.getCause().getMessage(); 
                        }
                        logger.error(msg, t);
               
                        throw new DeploymentException(msg, t);
                    }
                }
            }
        	
            // Mark the deployment as prepared
            deployment.prepare();
            
            logger.debug("prepared successfully ticket:" + ticket);
        }
        catch (IOException e)
        {
        	logger.error("Error while preparing ticket:" + ticket, e);
            throw new DeploymentException("Could not prepare.", e);
        }
	}


	public void abort(String ticket) 
	{
	  	logger.info("Abort ticket: " + ticket);
        Deployment deployment = fDeployments.get(ticket);
        if (deployment == null)
        {
        	logger.debug("Could not abort: invalid token ticket:" + ticket);
        	// We are most likely to get here because we are aborting an already aborted ticket
        	return;
        }
        if (deployment.getState() != DeploymentState.WORKING && deployment.getState() != DeploymentState.PREPARED)
        {
            throw new DeploymentException("Deployment cannot be aborted: already aborting, or committing.");
        }
        try
        {
        	// Mark the deployment
            deployment.abort();
                        	    		
            /**
             * Delete any temporary files that may have been transferred over.
             */
            for(DeployedFile file : deployment)
            {
            	if(file.getType() == FileType.FILE)
            	{
            		File content = new File(file.getPreLocation());            		
            		content.delete();
            	}
            }
        }
        catch (IOException e)
        {
        	logger.error("Error while aborting ticket:" + ticket, e);
            throw new DeploymentException("Could not abort.", e);
        }
        finally 
        {
            fDeployments.remove(ticket);           
        }		
	}

	public void commit(String ticket) 
	{	
	        final DMDeployment deployment = fDeployments.get(ticket);
	        if (deployment == null)
	        {
	        	String msg = "Could not commit because invalid ticket:" + ticket;
	        	logger.error(msg);
	            throw new DeploymentException(msg);
	        }
	        logger.debug("commit ticket:" + ticket);
	        
	        RetryingTransactionHelper tran = trxService.getRetryingTransactionHelper();
	        
	        RetryingTransactionCallback<Boolean> commitCB = new RetryingTransactionCallback<Boolean>()
	        {
	            public Boolean execute() throws Throwable
	            {
	                /**
	                 * Need to write content into nodes.
	                 */
	                for(DeployedFile xxx : deployment)
	                {
	                   
	                    if(xxx.getType() == FileType.FILE)
	                    {
	                        DMDeployedFile file = (DMDeployedFile)xxx;
	                        ContentWriter writer = fileFolderService.getWriter(file.getDestNodeRef());
                            writer.setMimetype(file.getMimeType());
                            writer.setEncoding(file.getEncoding());
                            writer.putContent(new File(file.getPreLocation()));
                            setWCMGuid(file.getDestNodeRef(), file.getGuid());
	                    }
	                    if(xxx.getType() == FileType.DELETED)
	                    {
	                        DMDeployedFile file = (DMDeployedFile)xxx;
	                        nodeService.deleteNode(file.getDestNodeRef());
	                    }     
	                }
	                return Boolean.TRUE;
	            }
        };
 
	        
	        try
	        {
	            tran.doInTransaction(commitCB);
	          		
	            // Mark the deployment as committed
	            deployment.commit();
	        	
	            /**
	             * Now run the post commit runnables.
	             */
	            if (postCommit != null && postCommit.size() > 0)
	            {
	                for (FSDeploymentRunnable runnable : postCommit)
	                {
	                    try
	                    {
	                        runnable.init(deployment);
	                        runnable.run();
	                    }
	                    catch (Throwable t)
	                    {
	                    	logger.error("Error from postCommit event t:" + t.toString(), t);
	                    }
	                }
	            }
	            
	            // Remove tempoarary files which are no longer needed
	            for(DeployedFile file : deployment)
	            {
	                if(file.getType() == FileType.FILE)
	                {
	                    File content = new File(file.getPreLocation());                 
	                    content.delete();
	                }
	            }

	            logger.debug("commited successfully ticket:" + ticket);
	        }
	        catch (Exception e)
	        {
	        	throw new DeploymentException("Could not commit", e);
	        }
	        finally
	        {
	            fDeployments.remove(ticket);
	        }
	}

	/**
	 * DMR Implementation of delete a file or directory
	 */
	public void delete(String ticket, String path) 
	{
		final DMDeployment deployment = fDeployments.get(ticket);
		if (deployment == null)
		{
			String msg = "Could not delete because invalid ticket:" + ticket;
			throw new DeploymentException(msg);
		}
		
		final String destPath = mapPath(deployment.getAuthoringStoreName(), path);
		
	    RetryingTransactionHelper tran = trxService.getRetryingTransactionHelper();
	    
		try
		{
			RetryingTransactionCallback<NodeRef> deleteCB = new RetryingTransactionCallback<NodeRef>()
			{
				public NodeRef execute() throws Throwable
				{
			        List<String> pathElements = toFolderPath(destPath);
					
			        /**
					 * Remove the file or directory
					 */
				    FileInfo context = fileFolderService.resolveNamePath(deployment.getRootNode().getNodeRef(), pathElements);
					logger.debug("Delete file" + destPath);
					return context.getNodeRef();
				}
			};

			NodeRef toDelete = tran.doInTransaction(deleteCB);

			/**
			 * Update the deployment record
			 */
			DMDeployedFile file = new DMDeployedFile(FileType.DELETED, 
					null,
					destPath,
					null,
					false);
			file.setDestNodeRef(toDelete);
			deployment.add(file);
		}
		catch (IOException e)
		{
			throw new DeploymentException("Could not update log.", e);
		}		
	}

	/**
	 * DMR implementation of getListing
	 */
	public List<FileDescriptor> getListing(String ticket, String path) 
	{
		final DMDeployment deployment = fDeployments.get(ticket);
	    if (deployment == null)
	    {
	        throw new DeploymentException("getListing invalid ticket. ticket:" + ticket);
	    }
	    
	    final String destPath = mapPath(deployment.getAuthoringStoreName(), path);
	    
        RetryingTransactionHelper tran = trxService.getRetryingTransactionHelper();
	    
	    final List<String> pathElements = toFolderPath(destPath);
	    
	    RetryingTransactionCallback<List<FileDescriptor>> listCB = new RetryingTransactionCallback<List<FileDescriptor>>()
	    {
	        public List<FileDescriptor> execute() throws Throwable
	        {
	            try
	            {
	                FileInfo context = null; 
	                if(pathElements.size() > 0)
	                {
	                    context = fileFolderService.resolveNamePath(deployment.getRootNode().getNodeRef(), pathElements);
	                }
	                else
	                {
	                    context = deployment.getRootNode();
	                }
	                
	                final FileInfo finalContext = context;
	                List<FileInfo> files = fileFolderService.list(finalContext.getNodeRef());
	        
	                /**
	                 * Map the listing into the over the wire format
	                 */
	                List<FileDescriptor> returnVal = new LinkedList<FileDescriptor>();
	                for(FileInfo file : files)
	                {
	                    if(logger.isDebugEnabled())
	                    {
	                        logger.debug("listing "+ file.getName());
	                    }
	                    returnVal.add(new FileDescriptor(file.getName(), mapFileTypeFromDM(file.isFolder()), getWCMGuid(file)));
	                }
	                
	                return returnVal;
	            }
	            catch (FileNotFoundException fe)
	            {
	                    throw new DeploymentException("Unable to list folder", fe);
	            }
	        }
	    };  // end of callback definition
	        
	    List<FileDescriptor> files = tran.doInTransaction(listCB);
	            
	    return files;
	}
	
	private FileType mapFileTypeFromDM(boolean isFolder)
	{
		return isFolder ? FileType.DIR : FileType.FILE;
	}

	/**
	 * DMR Implementation of create new directory
	 */
	public void createDirectory(String ticket, String path, String guid, Set<String>aspects, Map<String, Serializable> props) 
	{
		final DMDeployment deployment = fDeployments.get(ticket);
		if (deployment == null)
		{
			throw new DeploymentException("mkdir invalid ticket. ticket:" + ticket);
		}

		try
		{
			final Set<String> faspects = aspects;
			final Map<String, Serializable> fprops = props;
			final String fguid = guid;
        	RetryingTransactionHelper trn = trxService.getRetryingTransactionHelper();
        	
            final String destPath = mapPath(deployment.getAuthoringStoreName(), path);
			
        	RetryingTransactionCallback<NodeRef> createDirectoryCB = new RetryingTransactionCallback<NodeRef>() {
        	    
				public NodeRef execute() throws Throwable {
				    
			        final List<String> pathElements = toFolderPath(getParentPath(destPath));
			        final String folderName = getFileName(destPath);
			        
			        NodeRef contextNodeRef;
			        if(pathElements.size() > 0)
			        {
			            FileInfo parentInfo = fileFolderService.resolveNamePath(deployment.getRootNode().getNodeRef(), pathElements);
			            contextNodeRef = parentInfo.getNodeRef();
			        }
			        else
			        {
			             contextNodeRef = deployment.getRootNode().getNodeRef();
			        }
			      
                    /**
                     * Check whether we are replacing an existing file with a folder 
                     */
			        NodeRef exists = fileFolderService.searchSimple(contextNodeRef, folderName);
			        if(exists != null)
			        {
			            FileInfo info = fileFolderService.getFileInfo(exists);
					    if (info != null && !info.isFolder()) 
					    {
						    // The new directory replaces the file
						    nodeService.deleteNode(exists);
					    }
			        }

					Map<QName, Serializable> propertyMap = new HashMap<QName, Serializable>();
					for (String key : fprops.keySet()) 
					{
						propertyMap.put(QName.createQName(key), fprops.get(key));
					}
					
					// Prop name must be specified otherwise the deployment fails
                    propertyMap.put(ContentModel.PROP_NAME, folderName);
                    
                    QName localQName = QName.createQName(
                            NamespaceService.CONTENT_MODEL_1_0_URI,
                            QName.createValidLocalName(folderName));

					ChildAssociationRef child = nodeService.createNode(contextNodeRef, ContentModel.ASSOC_CONTAINS, localQName , ContentModel.TYPE_FOLDER, propertyMap);
					
				    updateAspects(child.getChildRef(), faspects, propertyMap);
				    						
					setWCMGuid(child.getChildRef(), fguid);

					return child.getChildRef();
				}
			};
    		
    		/**
    		 * Now do the create directory transaction
    		 */
    		trn.doInTransaction(createDirectoryCB);

			DeployedFile file = new DeployedFile(FileType.DIR,
					null,
					destPath,
					guid,
					true);

			deployment.add(file);
		}
		catch (IOException e)
		{
			throw new DeploymentException("Could not log mkdir of " + path + " error: " + e.toString(), e);
		}
	}
	
	private class SendRetVal
	{
	    NodeRef nodeRef;
	    boolean isNew;
	};

	/**
	 * DMR Implementation of Send 
	 */
	public OutputStream send(String ticket, String path, String guid, String encoding, String mimeType, Set<String>aspects, Map<String, Serializable> props)
	{
        final DMDeployment deployment = fDeployments.get(ticket);
        if (deployment == null)
        {
            throw new DeploymentException("Deployment timed out or invalid ticket.");
        }
        
        final String destPath = mapPath(deployment.getAuthoringStoreName(), path);
        
        try
        {     	        	
        	RetryingTransactionHelper trn = trxService.getRetryingTransactionHelper();
			final Set<String>faspects = aspects;
			final Map<String, Serializable> fprops = props;
				
        	RetryingTransactionCallback<SendRetVal> sendCB = new RetryingTransactionCallback<SendRetVal>()
    		{
    			public SendRetVal execute() throws Throwable
    			{
  				    boolean newFile = false;  				
    				logger.debug("send " + destPath);
    	
                    final List<String> pathElements = toFolderPath(getParentPath(destPath));
                    final String fileName = getFileName(destPath);
                    
                    NodeRef parentNodeRef;
                    if(pathElements.size() > 0)
                    {
                        FileInfo parentInfo = fileFolderService.resolveNamePath(deployment.getRootNode().getNodeRef(), pathElements);
                        parentNodeRef = parentInfo.getNodeRef();
                    }
                    else
                    {
                         parentNodeRef = deployment.getRootNode().getNodeRef();
                    }
                  
                    /**
                     * Check whether we are replacing an existing file with a folder 
                     */
                    NodeRef dest = fileFolderService.searchSimple(parentNodeRef, fileName);
                    if(dest != null)
                    {
                        FileInfo info = fileFolderService.getFileInfo(dest);
                        if (info != null && info.isFolder()) 
                        {
                            // The new file replaces the folder
                            nodeService.deleteNode(dest);
                            dest = null;
                        }
                    }
                    
                    Map<QName, Serializable>propertyMap = new HashMap<QName, Serializable>();
                    //System.out.println("Path :" + destPath);
                    
                    for (Map.Entry<String, Serializable> entry :  fprops.entrySet())
                    {
                        QName qname = QName.createQName(entry.getKey());
                        propertyMap.put(qname, entry.getValue());
                        System.out.println(qname + " value:" + entry.getValue());
                    }
                    
                    // Prop name must be specified otherwise this target cannot work
                    propertyMap.put(ContentModel.PROP_NAME, fileName);
                    propertyMap.put(WCMAppModel.PROP_GUID, "0");

    				if(dest == null)
    				{
    				    // This is a new file
                        QName localQName = QName.createQName(
                                NamespaceService.CONTENT_MODEL_1_0_URI,
                                QName.createValidLocalName(fileName));
                        ChildAssociationRef child = nodeService.createNode(parentNodeRef, ContentModel.ASSOC_CONTAINS, localQName, ContentModel.TYPE_CONTENT, propertyMap);
    					//logger.debug("Create file" + destPath);
    					newFile = true;
    					dest = child.getChildRef();
    				}
    				else
    				{	
    					// The file already exists
    					logger.debug("file exists - update" + destPath);
    					newFile = false;
    					nodeService.setProperties(dest, propertyMap);
    				}  
    				
    			    updateAspects(dest, faspects, propertyMap);

    			    SendRetVal ret = new SendRetVal();
    			    ret.nodeRef = dest;
    			    ret.isNew = newFile;
   				    return ret;
    			}
    		};    		
	
    		/**
    		 * Now do the send transaction
    		 */
    		SendRetVal ret = trn.doInTransaction(sendCB);
    		NodeRef destNode = ret.nodeRef;
    		
			/**
			 * Open a temporary file to receive the contents.
			 */
			File tempFile = TempFileProvider.createTempFile(ticket, "bin");
			OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile));

	   	   /**
	   	  	* Update Deployment record
	   	  	*/
       		DeployedFile file = new DMDeployedFile(FileType.FILE,
                                             tempFile.getAbsolutePath(),
                                             destPath,
                                             guid,
                                             ret.isNew,
                                             destNode,
                                             encoding,
                                             mimeType);
       		deployment.add(file);
        	
        	return out;
        } 
        catch (IOException e)	    
        {
        		throw new DeploymentException("Could not send for path:" + path, e);
	    }
	}

	/**
	 * DMR implementation of update directory
	 */
	public void updateDirectory(String ticket, String path, String guid, Set<String>aspects, Map<String, Serializable> props) 
	{
	    final DMDeployment deployment = fDeployments.get(ticket);
	    if (deployment == null)
	    {
	        throw new DeploymentException("Deployment invalid ticket.");
	    }
	    
	    final String destPath = mapPath(deployment.getAuthoringStoreName(), path);

	    try
	    {
	        final Set<String> faspects = aspects;
	        final Map<String, Serializable> fprops = props;
	        final String fguid = guid;
	        RetryingTransactionHelper trn = trxService.getRetryingTransactionHelper();

	        RetryingTransactionCallback<NodeRef> updateDirectoryCB = new RetryingTransactionCallback<NodeRef>() {

	            public NodeRef execute() throws Throwable 
	            {    
	                final List<String> pathElements = toFolderPath(destPath);
	                final String fileName = getFileName(destPath);

	                /**
	                 * File should exist, so we don't need to check it exists prior to accessing it
	                 */
	                FileInfo info = fileFolderService.resolveNamePath(deployment.getRootNode().getNodeRef(), pathElements);
	                NodeRef nodeRef = info.getNodeRef();

	                Map<QName, Serializable> propertyMap = new HashMap<QName, Serializable>();
	                for (String key : fprops.keySet()) 
	                {
	                    propertyMap.put(QName.createQName(key), fprops.get(key));
	                }
	                
	                propertyMap.put(ContentModel.PROP_NAME, fileName);

	                nodeService.setProperties(nodeRef, propertyMap);
	                
	                updateAspects(nodeRef, faspects, propertyMap);
	                    
	                setWCMGuid(nodeRef, fguid);

	                return nodeRef;
	            }
	        };

	        /**
	         * Now do the update directory transaction
	         */
	        trn.doInTransaction(updateDirectoryCB);

	        /**
	         * Update Deployment
	         */
	        DeployedFile file = new DeployedFile(FileType.SETGUID,
	                null,
	                destPath,
	                guid,
	                false);
	        deployment.add(file);

	        logger.debug("end update directory");
	    }

	    catch (Exception e)
	    {
	        throw new DeploymentException("Could not set guid on " + path, e);
	    }		
	}
		
	private String getParentPath(String path) 
	{
		int pos  = path.lastIndexOf('/');
		return path.substring(0, pos + 1);
	}
	
	private String getFileName(String path)
	{
		int pos  = path.lastIndexOf('/');
		return path.substring(pos + 1);
	}
	
	private String getWCMGuid(FileInfo file)
	{
	    NodeRef nodeRef = file.getNodeRef();
	    if (nodeService.hasAspect(nodeRef, WCMAppModel.ASPECT_DEPLOYED))
	    {
	        Serializable s = nodeService.getProperty(nodeRef, WCMAppModel.PROP_GUID);
	        return (String)s;
	    }

	    return "0";
	}
	
	private String mapPath(String storeName, String path)
	{
	    return path;
	}
	
	/**
	 * 
	 * @param nodeRef
	 * @param value
	 */
	private void setWCMGuid(NodeRef nodeRef, String value)
	{
	    if(nodeService.hasAspect(nodeRef, WCMAppModel.ASPECT_DEPLOYED))
	    {
	        nodeService.setProperty(nodeRef, WCMAppModel.PROP_GUID, value);
	    }
	    else 
	    {
	        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
	        props.put(WCMAppModel.PROP_GUID, value);
	        nodeService.addAspect(nodeRef, WCMAppModel.ASPECT_DEPLOYED, props);
	    }
	}
	
	public int getCurrentVersion(String target, String storeName) {
	        // Not implemented yet
	        return -1;
	}
	
	private void updateAspects(NodeRef dest, Set<String>faspects,  Map<QName, Serializable>propertyMap)
	{
	    /**
	     * Work out the aspects implied by the property definitions.
	     * for example cm:titled is implied by a cm:title property.
	     */
	     Set<QName> impliedAspects = new TreeSet<QName>();
	     for (QName newPropertyQName : propertyMap.keySet())
	     {
	            PropertyDefinition propDef = getDictionaryService().getProperty(newPropertyQName);
	            if (propDef == null)
	            {
	                continue;               // Ignore undefined properties
	            }
	            if (!propDef.getContainerClass().isAspect())
	            {
	                continue;
	            }
	            QName containerClassQName = propDef.getContainerClass().getName();
	            impliedAspects.add(containerClassQName);
	    }
	    
	    List<QName>aspectList = new ArrayList<QName>(faspects.size());
	    for(String aspect : faspects)
	    {
	        aspectList.add(QName.createQName(aspect));
	    }
    
	    // Add Missing Aspects
	    for(QName aspect : aspectList)
	    {
	        if(nodeService.hasAspect(dest, aspect))
	        {
	            nodeService.addAspect(dest, aspect, null);
	        }
	    }
    
	    // Remove Obsolete Aspects                  
	    for(QName aspect : nodeService.getAspects(dest))
	    {
	        if(!aspectList.contains(aspect) && !impliedAspects.contains(aspect))
	        {
	            nodeService.removeAspect(dest, aspect);
	        }
	    }
	}
	
    static private List<String> toFolderPath(String folderPath)
    {
        if (!PropertyCheck.isValidPropertyString(folderPath))
        {
            folderPath = "";
        }
        
        List<String> ret = new ArrayList<String>(5);
        StringTokenizer tokenizer = new StringTokenizer(folderPath, "/");
        while (tokenizer.hasMoreTokens())
        {
            String folderName = tokenizer.nextToken();
            if (folderName.length() == 0)
            {
                throw new IllegalArgumentException("Invalid folder name path for property 'folderPath': " + folderPath);
            }
            ret.add(folderName);
        }
        return ret;
    }
	
	public void setPostCommit(List<FSDeploymentRunnable> postCommit) 
	{
		this.postCommit = postCommit;
	}

	public List<FSDeploymentRunnable> getPostCommit() 
	{
		return postCommit;
	}

	public void setPrepare(List<FSDeploymentRunnable> prepare) 
	{
		this.prepare = prepare;
	}

	public List<FSDeploymentRunnable> getPrepare() 
	{
		return prepare;
	}

	public void setTransactionService(TransactionService trxService) 
	{
		this.trxService = trxService;
	}

	public TransactionService getTransactionService() 
	{
		return trxService;
	}

    public void setFileFolderService(FileFolderService fileFolderService)
    {
        this.fileFolderService = fileFolderService;
    }

    public FileFolderService getFileFolderService()
    {
        return fileFolderService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public NodeService getNodeService()
    {
        return nodeService;
    }
    
    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    public SearchService getSearchService()
    {
        return searchService;
    }
    
    public void setAuthenticator(DeploymentReceiverAuthenticator authenticator) 
    {
        this.authenticator = authenticator;
    }

    public DeploymentReceiverAuthenticator getAuthenticator() 
    {
        return authenticator;
    }

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    public DictionaryService getDictionaryService()
    {
        return dictionaryService;
    }

    public void setStoreNameMapper(StoreNameMapper nameMapper)
    {
        this.nameMapper = nameMapper;
    }

    public StoreNameMapper getStoreNameMapper()
    {
        return nameMapper;
    }
    
    public void setRootLocator(RootLocator rootLocator)
    {
        this.rootLocator = rootLocator;
    }

    public RootLocator getRootLocator()
    {
        return rootLocator;
    }

    public void setStoreRef(String storeRef)
    {
        this.storeRef = storeRef;
    }

    public String getStoreRef()
    {
        return storeRef;
    }
}