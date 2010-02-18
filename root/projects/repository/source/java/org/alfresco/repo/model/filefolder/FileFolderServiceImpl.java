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
package org.alfresco.repo.model.filefolder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.QueryParameterDefImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileFolderServiceType;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.search.QueryParameterDefinition;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.util.SearchLanguageConversion;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of the file/folder-specific service.
 * 
 * @author Derek Hulley
 */
public class FileFolderServiceImpl implements FileFolderService
{
    /** Shallow search for files and folders with a name pattern */
    private static final String XPATH_QUERY_SHALLOW_ALL =
        "./*" +
        "[like(@cm:name, $cm:name, false)" +
        " and not (subtypeOf('" + ContentModel.TYPE_SYSTEM_FOLDER + "'))" +
        " and (subtypeOf('" + ContentModel.TYPE_FOLDER + "') or subtypeOf('" + ContentModel.TYPE_CONTENT + "')" +
        " or subtypeOf('" + ContentModel.TYPE_LINK + "'))]";
    
    /** Deep search for files and folders with a name pattern */
    private static final String XPATH_QUERY_DEEP_ALL =
        ".//*" +
        "[like(@cm:name, $cm:name, false)" +
        " and not (subtypeOf('" + ContentModel.TYPE_SYSTEM_FOLDER + "'))" +
        " and (subtypeOf('" + ContentModel.TYPE_FOLDER + "') or subtypeOf('" + ContentModel.TYPE_CONTENT + "')" +
        " or subtypeOf('" + ContentModel.TYPE_LINK + "'))]";
    
    /** empty parameters */
    private static final QueryParameterDefinition[] PARAMS_ANY_NAME = new QueryParameterDefinition[1];
    
    private static Log logger = LogFactory.getLog(FileFolderServiceImpl.class);

    private NamespaceService namespaceService;
    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private CopyService copyService;
    private SearchService searchService;
    private ContentService contentService;
    private MimetypeService mimetypeService;
    private Set<String> systemNamespaces;
    
    // TODO: Replace this with a more formal means of identifying "system" folders (i.e. aspect or UUID)
    private List<String> systemPaths;
    
    /**
     * Default constructor
     */
    public FileFolderServiceImpl()
    {
        systemNamespaces = new HashSet<String>(5);
    }

    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setCopyService(CopyService copyService)
    {
        this.copyService = copyService;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }
    
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    public void setMimetypeService(MimetypeService mimetypeService)
    {
        this.mimetypeService = mimetypeService;
    }

    /**
     * Set the namespaces that should be treated as 'system' namespaces.
     * <p>
     * When files or folders are renamed, the association path (QName) is normally
     * modified to follow the name of the node.  If, however, the namespace of the
     * patch QName is in this list, the association path is left alone.  This allows
     * parts of the application to use well-known paths even if the end-user is
     * able to modify the objects <b>cm:name</b> value.
     * 
     * @param systemNamespaces          a list of system namespaces
     */
    public void setSystemNamespaces(List<String> systemNamespaces)
    {
        this.systemNamespaces.addAll(systemNamespaces);
    }

    // TODO: Replace this with a more formal means of identifying "system" folders (i.e. aspect or UUID)
    public void setSystemPaths(List<String> systemPaths)
    {
        this.systemPaths = systemPaths;
    }
    
    
    public void init()
    {
    }

    /**
     * Helper method to convert node reference instances to file info
     * 
     * @param nodeRefs the node references
     * @return Return a list of file info
     * @throws InvalidTypeException if the node is not a valid type
     */
    private List<FileInfo> toFileInfo(List<NodeRef> nodeRefs) throws InvalidTypeException
    {
        List<FileInfo> results = new ArrayList<FileInfo>(nodeRefs.size());
        for (NodeRef nodeRef : nodeRefs)
        {
            if (nodeService.exists(nodeRef))
            {
                FileInfo fileInfo = toFileInfo(nodeRef, true);
                results.add(fileInfo);
            }
        }
        return results;
    }
    
    /**
     * Helper method to convert a node reference instance to a file info
     */
    private FileInfo toFileInfo(NodeRef nodeRef, boolean addTranslations) throws InvalidTypeException
    {
        // Get the file attributes
        Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
        // Is it a folder
        QName typeQName = nodeService.getType(nodeRef);
        boolean isFolder = isFolder(typeQName);
        
        // Construct the file info and add to the results
        FileInfo fileInfo = new FileInfoImpl(nodeRef, isFolder, properties);
        // Done
        return fileInfo;
    }

    /**
     * Exception when the type is not a valid File or Folder type
     * 
     * @see ContentModel#TYPE_CONTENT
     * @see ContentModel#TYPE_FOLDER
     * 
     * @author Derek Hulley
     */
    private static class InvalidTypeException extends RuntimeException
    {
        private static final long serialVersionUID = -310101369475434280L;
        
        public InvalidTypeException(String msg)
        {
            super(msg);
        }
    }
    
    /**
     * Checks the type for whether it is a file or folder.  All invalid types
     * lead to runtime exceptions.
     * 
     * @param typeQName the type to check
     * @return Returns true if the type is a valid folder type, false if it is a file.
     * @throws AlfrescoRuntimeException if the type is not handled by this service
     */
    private boolean isFolder(QName typeQName) throws InvalidTypeException
    {
        FileFolderServiceType type = getType(typeQName);
        
        switch (type)
        {
        case FILE:
            return false;
        case FOLDER:
            return true;
        case SYSTEM_FOLDER:
            throw new InvalidTypeException("This service should ignore type " + ContentModel.TYPE_SYSTEM_FOLDER);
        case INVALID:
        default:
            throw new InvalidTypeException("Type is not handled by this service: " + typeQName);
        }
    }

    public boolean exists(NodeRef nodeRef)
    {
        return nodeService.exists(nodeRef);
    }
    
    public FileFolderServiceType getType(QName typeQName)
    {
        if (dictionaryService.isSubClass(typeQName, ContentModel.TYPE_FOLDER))
        {
            if (dictionaryService.isSubClass(typeQName, ContentModel.TYPE_SYSTEM_FOLDER))
            {
                return FileFolderServiceType.SYSTEM_FOLDER;
            }
            return FileFolderServiceType.FOLDER;
        }
        else if (dictionaryService.isSubClass(typeQName, ContentModel.TYPE_CONTENT) ||
                dictionaryService.isSubClass(typeQName, ContentModel.TYPE_LINK))
        {
            // it is a regular file
            return FileFolderServiceType.FILE;
        }
        else
        {
            // unhandled type
            return FileFolderServiceType.INVALID;
        }
    }
    
    public List<FileInfo> list(NodeRef contextNodeRef)
    {
        // execute the query
        List<NodeRef> nodeRefs = listSimple(contextNodeRef, true, true);
        // convert the noderefs
        List<FileInfo> results = toFileInfo(nodeRefs);
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Shallow search for files and folders: \n" +
                    "   context: " + contextNodeRef + "\n" +
                    "   results: " + results);
        }
        return results;
    }

    public List<FileInfo> listFiles(NodeRef contextNodeRef)
    {
        // execute the query
        List<NodeRef> nodeRefs = listSimple(contextNodeRef, false, true);
        // convert the noderefs
        List<FileInfo> results = toFileInfo(nodeRefs);
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Shallow search for files: \n" +
                    "   context: " + contextNodeRef + "\n" +
                    "   results: " + results);
        }
        return results;
    }

    public List<FileInfo> listFolders(NodeRef contextNodeRef)
    {
        // execute the query
        List<NodeRef> nodeRefs = listSimple(contextNodeRef, true, false);
        // convert the noderefs
        List<FileInfo> results = toFileInfo(nodeRefs);
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Shallow search for folders: \n" +
                    "   context: " + contextNodeRef + "\n" +
                    "   results: " + results);
        }
        return results;
    }
    
    public NodeRef searchSimple(NodeRef contextNodeRef, String name)
    {
        NodeRef childNodeRef = nodeService.getChildByName(contextNodeRef, ContentModel.ASSOC_CONTAINS, name);
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Simple name search results: \n" +
                    "   parent: " + contextNodeRef + "\n" +
                    "   name: " + name + "\n" +
                    "   result: " + childNodeRef);
        }
        return childNodeRef;
    }

    /**
     * @see #search(NodeRef, String, boolean, boolean, boolean)
     */
    public List<FileInfo> search(NodeRef contextNodeRef, String namePattern, boolean includeSubFolders)
    {
        return search(contextNodeRef, namePattern, true, true, includeSubFolders);
    }

    private static final String LUCENE_MULTI_CHAR_WILDCARD = "*";
    /**
     * Full search with all options
     */
    public List<FileInfo> search(
            NodeRef contextNodeRef,
            String namePattern,
            boolean fileSearch,
            boolean folderSearch,
            boolean includeSubFolders)
    {
        // get the raw nodeRefs
        List<NodeRef> nodeRefs = searchInternal(contextNodeRef, namePattern, fileSearch, folderSearch, includeSubFolders);
        
        List<FileInfo> results = toFileInfo(nodeRefs);
        // eliminate unwanted files/folders
        Iterator<FileInfo> iterator = results.iterator(); 
        while (iterator.hasNext())
        {
            FileInfo file = iterator.next();
            if (file.isFolder() && !folderSearch)
            {
                iterator.remove();
            }
            else if (!file.isFolder() && !fileSearch)
            {
                iterator.remove();
            }
        }
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Deep search: \n" +
                    "   context: " + contextNodeRef + "\n" +
                    "   pattern: " + namePattern + "\n" +
                    "   files: " + fileSearch + "\n" +
                    "   folders: " + folderSearch + "\n" +
                    "   deep: " + includeSubFolders + "\n" +
                    "   results: " + results);
        }
        return results;
    }
    
    /**
     * Performs a full search, but doesn't translate the node references into
     * file info objects.  This allows {@link #checkExists(NodeRef, String)} to
     * bypass the retrieval of node properties.
     */
    private List<NodeRef> searchInternal(
            NodeRef contextNodeRef,
            String namePattern,
            boolean fileSearch,
            boolean folderSearch,
            boolean includeSubFolders)
    {
        // shortcut if the search is requesting nothing
        if (!fileSearch && !folderSearch)
        {
            return Collections.emptyList();
        }
        
        if (namePattern == null)
        {
            namePattern = LUCENE_MULTI_CHAR_WILDCARD;      // default to wildcard
        }
        // now check if we can use Lucene to handle this query
        boolean anyName = namePattern.equals(LUCENE_MULTI_CHAR_WILDCARD);
        
        List<NodeRef> nodeRefs = null;
        if (!includeSubFolders && anyName)
        {
            nodeRefs = listSimple(contextNodeRef, folderSearch, fileSearch);
        }
        else                // Go with XPath
        {
            // if the name pattern is null, then we use the ANY pattern
            QueryParameterDefinition[] params = null;
            if (namePattern != null)
            {
                // the interface specifies the Lucene syntax, so perform a conversion
                namePattern = SearchLanguageConversion.convert(
                        SearchLanguageConversion.DEF_LUCENE,
                        SearchLanguageConversion.DEF_XPATH_LIKE,
                        namePattern);
                
                params = new QueryParameterDefinition[1];
                params[0] = new QueryParameterDefImpl(
                        ContentModel.PROP_NAME,
                        dictionaryService.getDataType(DataTypeDefinition.TEXT),
                        true,
                        namePattern);
            }
            else
            {
                params = PARAMS_ANY_NAME;
            }
            // determine the correct query to use
            String query = null;
            if (includeSubFolders)
            {
                query = XPATH_QUERY_DEEP_ALL;
            }
            else
            {
                query = XPATH_QUERY_SHALLOW_ALL;
            }
            // execute the query
            nodeRefs = searchService.selectNodes(
                    contextNodeRef,
                    query,
                    params,
                    namespaceService,
                    false);
        }
        // done
        return nodeRefs;
    }
    
    private List<NodeRef> listSimple(NodeRef contextNodeRef, boolean folders, boolean files)
    {
        Set<QName> searchTypeQNames = new HashSet<QName>(10);
        // Build a list of file and folder types
        if (folders)
        {
            Collection<QName> qnames = dictionaryService.getSubTypes(ContentModel.TYPE_FOLDER, true);
            searchTypeQNames.addAll(qnames);
            searchTypeQNames.add(ContentModel.TYPE_FOLDER);
        }
        if (files)
        {
            Collection<QName> qnames = dictionaryService.getSubTypes(ContentModel.TYPE_CONTENT, true);
            searchTypeQNames.addAll(qnames);
            searchTypeQNames.add(ContentModel.TYPE_CONTENT);
            qnames = dictionaryService.getSubTypes(ContentModel.TYPE_LINK, true);
            searchTypeQNames.addAll(qnames);
            searchTypeQNames.add(ContentModel.TYPE_LINK);
        }
        // Remove 'system' folders
        Collection<QName> qnames = dictionaryService.getSubTypes(ContentModel.TYPE_SYSTEM_FOLDER, true);
        searchTypeQNames.removeAll(qnames);
        searchTypeQNames.remove(ContentModel.TYPE_SYSTEM_FOLDER);
        // Shortcut
        if (searchTypeQNames.size() == 0)
        {
            return Collections.emptyList();
        }
        // Do the query
        List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(contextNodeRef, searchTypeQNames);
        List<NodeRef> result = new ArrayList<NodeRef>(childAssocRefs.size());
        for (ChildAssociationRef assocRef : childAssocRefs)
        {
            result.add(assocRef.getChildRef());
        }
        // Done
        return result;
    }
    
    /**
     * @see #move(NodeRef, NodeRef, String)
     */
    public FileInfo rename(NodeRef sourceNodeRef, String newName) throws FileExistsException, FileNotFoundException
    {
    	// NOTE:  
    	//
    	// This information is placed in the transaction to indicate that a rename has taken place.  This information is
    	// used by the rule trigger to ensure inbound rule is not triggered by a file rename
    	//
    	// See http://issues.alfresco.com/browse/AR-1544
    	AlfrescoTransactionSupport.bindResource(sourceNodeRef.toString()+"rename", sourceNodeRef);
    	
        return moveOrCopy(sourceNodeRef, null, newName, true);
    }

    /**
     * @see #moveOrCopy(NodeRef, NodeRef, String, boolean)
     */
    public FileInfo move(NodeRef sourceNodeRef, NodeRef targetParentRef, String newName) throws FileExistsException, FileNotFoundException
    {
        return moveOrCopy(sourceNodeRef, targetParentRef, newName, true);
    }
    
    /**
     * @see #moveOrCopy(NodeRef, NodeRef, String, boolean)
     */
    public FileInfo copy(NodeRef sourceNodeRef, NodeRef targetParentRef, String newName) throws FileExistsException, FileNotFoundException
    {
        return moveOrCopy(sourceNodeRef, targetParentRef, newName, false);
    }

    /**
     * Implements both move and copy behaviour
     * 
     * @param move true to move, otherwise false to copy
     */
    private FileInfo moveOrCopy(NodeRef sourceNodeRef, NodeRef targetParentRef, String newName, boolean move) throws FileExistsException, FileNotFoundException
    {
        // get file/folder in its current state
        FileInfo beforeFileInfo = toFileInfo(sourceNodeRef, true);
        // check the name - null means keep the existing name
        if (newName == null)
        {
            newName = beforeFileInfo.getName();
        }
        
        boolean nameChanged = (newName.equals(beforeFileInfo.getName()) == false);
        
        // we need the current association type
        ChildAssociationRef assocRef = nodeService.getPrimaryParent(sourceNodeRef);
        if (targetParentRef == null)
        {
            targetParentRef = assocRef.getParentRef();
        }
        
        boolean changedParent = !targetParentRef.equals(assocRef.getParentRef());
        // there is nothing to do if both the name and parent folder haven't changed
        if (!nameChanged && !changedParent)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Doing nothing - neither filename or parent has changed: \n" +
                        "   parent: " + targetParentRef + "\n" +
                        "   before: " + beforeFileInfo + "\n" +
                        "   new name: " + newName);
            }
            return beforeFileInfo;
        }
        
        QName existingQName = assocRef.getQName();
        QName qname;
        if (nameChanged && !systemNamespaces.contains(existingQName.getNamespaceURI()))
        {
            // Change the localname to match the new name
            qname = QName.createQName(
                    assocRef.getQName().getNamespaceURI(),
                    QName.createValidLocalName(newName));
        }
        else
        {
            // Keep the localname
            qname = existingQName;
        }
        
        QName targetParentType = nodeService.getType(targetParentRef);
        
        // Fix AWC-1517
        QName assocTypeQname = null;
        if (dictionaryService.isSubClass(targetParentType, ContentModel.TYPE_FOLDER))
        {
        	assocTypeQname = ContentModel.ASSOC_CONTAINS; // cm:folder -> cm:contains
        }
        else if (dictionaryService.isSubClass(targetParentType, ContentModel.TYPE_CONTAINER))
        {
        	assocTypeQname = ContentModel.ASSOC_CHILDREN; // sys:container -> sys:children
        }
        else
        {
        	throw new InvalidTypeException("Unexpected type (" + targetParentType + ") for target parent: " + targetParentRef);
        }
               
        // move or copy
        NodeRef targetNodeRef = null;
        if (move)
        {
            // TODO: Replace this with a more formal means of identifying "system" folders (i.e. aspect or UUID)
            if (!isSystemPath(sourceNodeRef))
            {
                // The cm:name might clash with another node in the target location.
                if (nameChanged)
                {
                    // The name will be changing, so we really need to set the node's name to the new
                    // name.  This can't be done at the same time as the move - to avoid incorrect violations
                    // of the name constraints, the cm:name is set to something random and will be reset
                    // to the correct name later.
                    nodeService.setProperty(sourceNodeRef, ContentModel.PROP_NAME, GUID.generate());
                }
                try
                {
                    // move the node so that the association moves as well
                    ChildAssociationRef newAssocRef = nodeService.moveNode(
                            sourceNodeRef,
                            targetParentRef,
                            assocTypeQname,
                            qname);
                    targetNodeRef = newAssocRef.getChildRef();
                }
                catch (DuplicateChildNodeNameException e)
                {
                    throw new FileExistsException(targetParentRef, newName);
                }
            }
            else
            {
                // system path folders do not need to be moved
                targetNodeRef = sourceNodeRef;
            }
        }
        else
        {
            try
            {
                // Copy the node.  The cm:name will be dropped and reset later.
                targetNodeRef = copyService.copy(
                        sourceNodeRef,
                        targetParentRef,
                        assocTypeQname,
                        qname,
                        true);
            }
            catch (DuplicateChildNodeNameException e)
            {
                throw new FileExistsException(targetParentRef, newName);
            }
        }
       
        // Only update the name if it has changed
        String currentName = (String)nodeService.getProperty(targetNodeRef, ContentModel.PROP_NAME);
        if (currentName.equals(newName) == false)
        {
            try
            {
                // changed the name property
                nodeService.setProperty(targetNodeRef, ContentModel.PROP_NAME, newName);
                
                // May need to update the mimetype, to support apps using .tmp files when saving
                ContentData contentData = (ContentData)nodeService.getProperty(targetNodeRef, ContentModel.PROP_CONTENT);

                // Check the newName and oldName extensions.
                // Keep previous mimetype if
                //      1. new extension is empty
                //      2. new extension is '.tmp'
                //      3. extension was not changed,
                // 
                // It fixes the ETWOTWO-16 issue.
                String oldExt = getExtension(beforeFileInfo.getName());
                String newExt = getExtension(newName);
                if (contentData != null &&
                        newExt.length() != 0 &&
                        !"tmp".equalsIgnoreCase(newExt) &&
                        !newExt.equalsIgnoreCase(oldExt))
                {
                    String targetMimetype = contentData.getMimetype();
                    String newMimetype = mimetypeService.guessMimetype(newName);
                    if (!targetMimetype.equalsIgnoreCase(newMimetype))
                	{
                        contentData = ContentData.setMimetype(contentData, newMimetype);
                        nodeService.setProperty(targetNodeRef, ContentModel.PROP_CONTENT, contentData);
                	}
                }
            }
            catch (DuplicateChildNodeNameException e)
            {
                throw new FileExistsException(targetParentRef, newName);
            }
        }
        
        // get the details after the operation
        FileInfo afterFileInfo = toFileInfo(targetNodeRef, true);
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("" + (move ? "Moved" : "Copied") + " node: \n" +
                    "   parent: " + targetParentRef + "\n" +
                    "   before: " + beforeFileInfo + "\n" +
                    "   after: " + afterFileInfo);
        }
        return afterFileInfo;
    }
    
    /**
     * Determine if the specified node is a special "system" folder path based node
     * 
     * TODO: Replace this with a more formal means of identifying "system" folders (i.e. aspect or UUID)
     * 
     * @param nodeRef  node to check
     * @return  true => system folder path based node
     */
    private boolean isSystemPath(NodeRef nodeRef)
    {
        Path path = nodeService.getPath(nodeRef);
        String prefixedPath = path.toPrefixString(namespaceService);
        return systemPaths.contains(prefixedPath);
    }
    
    public FileInfo create(NodeRef parentNodeRef, String name, QName typeQName) throws FileExistsException
    {
        return createImpl(parentNodeRef, name, typeQName, null);
    }
    
    public FileInfo create(NodeRef parentNodeRef, String name, QName typeQName, QName assocQName) throws FileExistsException
    {
        return createImpl(parentNodeRef, name, typeQName, assocQName);
    }
    
    private FileInfo createImpl(NodeRef parentNodeRef, String name, QName typeQName, QName assocQName) throws FileExistsException
    {
        // file or folder
        boolean isFolder = false;
        try
        {
            isFolder = isFolder(typeQName);
        }
        catch (InvalidTypeException e)
        {
            throw new AlfrescoRuntimeException("The type is not supported by this service: " + typeQName);
        }
        
        // set up initial properties
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>(11);
        properties.put(ContentModel.PROP_NAME, (Serializable) name);
        if (!isFolder)
        {
            // guess a mimetype based on the filename
            String mimetype = mimetypeService.guessMimetype(name);
            ContentData contentData = new ContentData(null, mimetype, 0L, "UTF-8");
            properties.put(ContentModel.PROP_CONTENT, contentData);
        }
        
        // create the node
        if (assocQName == null)
        {
            assocQName = QName.createQName(
                    NamespaceService.CONTENT_MODEL_1_0_URI,
                    QName.createValidLocalName(name));
        }
        ChildAssociationRef assocRef = null;
        try
        {
            assocRef = nodeService.createNode(
                    parentNodeRef,
                    ContentModel.ASSOC_CONTAINS,
                    assocQName,
                    typeQName,
                    properties);
        }
        catch (DuplicateChildNodeNameException e)
        {
            throw new FileExistsException(parentNodeRef, name);
        }
        
        NodeRef nodeRef = assocRef.getChildRef();
        FileInfo fileInfo = toFileInfo(nodeRef, true);
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Created: \n" +
                    "   parent: " + parentNodeRef + "\n" +
                    "   created: " + fileInfo);
        }
        return fileInfo;
    }
    
    public void delete(NodeRef nodeRef)
    {
        nodeService.deleteNode(nodeRef);
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Deleted: \n" +
                    "   node: " + nodeRef);
        }
    }

    public FileInfo makeFolders(NodeRef parentNodeRef, List<String> pathElements, QName folderTypeQName)
    {
        return FileFolderServiceImpl.makeFolders(this, parentNodeRef, pathElements, folderTypeQName);
    }

    public List<FileInfo> getNamePath(NodeRef rootNodeRef, NodeRef nodeRef) throws FileNotFoundException
    {
        // check the root
        if (rootNodeRef == null)
        {
            rootNodeRef = nodeService.getRootNode(nodeRef.getStoreRef());
        }
        try
        {
            List<FileInfo> results = new ArrayList<FileInfo>(10);
            // get the primary path
            Path path = nodeService.getPath(nodeRef);
            // iterate and turn the results into file info objects
            boolean foundRoot = false;
            for (Path.Element element : path)
            {
                // ignore everything down to the root
                Path.ChildAssocElement assocElement = (Path.ChildAssocElement) element;
                NodeRef childNodeRef = assocElement.getRef().getChildRef();
                if (childNodeRef.equals(rootNodeRef))
                {
                    // just found the root - but we don't put in an entry for it
                    foundRoot = true;
                    continue;
                }
                else if (!foundRoot)
                {
                    // keep looking for the root
                    continue;
                }
                // we found the root and expect to be building the path up
                FileInfo pathInfo = toFileInfo(childNodeRef, true);
                results.add(pathInfo);
            }
            // check that we found the root
            if (!foundRoot || results.size() == 0)
            {
                throw new FileNotFoundException(nodeRef);
            }
            // done
            if (logger.isDebugEnabled())
            {
                logger.debug("Built name path for node: \n" +
                        "   root: " + rootNodeRef + "\n" +
                        "   node: " + nodeRef + "\n" +
                        "   path: " + results);
            }
            return results;
        }
        catch (InvalidNodeRefException e)
        {
            throw new FileNotFoundException(nodeRef);
        }
    }

    public FileInfo resolveNamePath(NodeRef rootNodeRef, List<String> pathElements) throws FileNotFoundException
    {
        if (pathElements.size() == 0)
        {
            throw new IllegalArgumentException("Path elements list is empty");
        }
        // walk the folder tree first
        NodeRef parentNodeRef = rootNodeRef;
        StringBuilder currentPath = new StringBuilder(pathElements.size() << 4);
        int folderCount = pathElements.size() - 1;
        for (int i = 0; i < folderCount; i++)
        {
            String pathElement = pathElements.get(i);
            NodeRef folderNodeRef = searchSimple(parentNodeRef, pathElement);
            if (folderNodeRef == null)
            {
                StringBuilder sb = new StringBuilder(128);
                sb.append("Folder not found: " + currentPath);
                throw new FileNotFoundException(sb.toString());
            }
            parentNodeRef = folderNodeRef;
        }
        // we have resolved the folder path - resolve the last component
        String pathElement = pathElements.get(pathElements.size() - 1);
        NodeRef fileNodeRef = searchSimple(parentNodeRef, pathElement);
        if (fileNodeRef == null)
        {
            StringBuilder sb = new StringBuilder(128);
            sb.append("File not found: " + currentPath);
            throw new FileNotFoundException(sb.toString());
        }
        FileInfo result = getFileInfo(fileNodeRef);
        // found it
        if (logger.isDebugEnabled())
        {
            logger.debug("Resoved path element: \n" +
                    "   root: " + rootNodeRef + "\n" +
                    "   path: " + currentPath + "\n" +
                    "   node: " + result);
        }
        return result;
    }

    public FileInfo getFileInfo(NodeRef nodeRef)
    {
        try
        {
            return toFileInfo(nodeRef, true);
        }
        catch (InvalidTypeException e)
        {
            return null;
        }
    }

    public ContentReader getReader(NodeRef nodeRef)
    {
        FileInfo fileInfo = toFileInfo(nodeRef, false);
        if (fileInfo.isFolder())
        {
            throw new InvalidTypeException("Unable to get a content reader for a folder: " + fileInfo);
        }
        return contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
    }

    public ContentWriter getWriter(NodeRef nodeRef)
    {
        FileInfo fileInfo = toFileInfo(nodeRef, false);
        if (fileInfo.isFolder())
        {
            throw new InvalidTypeException("Unable to get a content writer for a folder: " + fileInfo);
        }
        return contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
    }
    
    /**
     * Helper method to create folders.
     * 
     * This uses the provided service for all auditing and permission checks. 
     * 
     * @param service
     * @param parentNodeRef
     * @param pathElements
     * @param folderTypeQName
     * @return FileInfo for the bottom node in pathElements.
     */
    public static FileInfo makeFolders(FileFolderService service, NodeRef parentNodeRef, List<String> pathElements, QName folderTypeQName)
    {
        if (pathElements.size() == 0)
        {
            throw new IllegalArgumentException("Path element list is empty");
        }
        
        // make sure that the folder is correct
        boolean isFolder = service.getType(folderTypeQName) == FileFolderServiceType.FOLDER;
        if (!isFolder)
        {
            throw new IllegalArgumentException("Type is invalid to make folders with: " + folderTypeQName);
        }
        
        NodeRef currentParentRef = parentNodeRef;
        // just loop and create if necessary
        for (String pathElement : pathElements)
        {
            // does it exist?
            // Navigation should not check permissions
            NodeRef nodeRef = AuthenticationUtil.runAs(new SearchAsSystem(service, currentParentRef, pathElement), AuthenticationUtil.getSystemUserName());

            if (nodeRef == null)
            {
                // not present - make it
                // If this uses the public service it will check create permissions
                FileInfo createdFileInfo = service.create(currentParentRef, pathElement, folderTypeQName);
                currentParentRef = createdFileInfo.getNodeRef();
            }
            else
            {
                // it exists
                currentParentRef = nodeRef;
            }
        }
        // done
        // Used to call toFileInfo((currentParentRef, true);
        // If this uses the public service this will check the final read access
        FileInfo fileInfo = service.getFileInfo(currentParentRef);
        
        // Should we check the type?
        return fileInfo;
    }
    
    private static class SearchAsSystem implements RunAsWork<NodeRef>
    {
        FileFolderService service;
        NodeRef node;
        String name;

        SearchAsSystem( FileFolderService service, NodeRef node, String name)
        {
            this.service = service;
            this.node = node;
            this.name = name;
        }
        
        public NodeRef doWork() throws Exception
        {
            return service.searchSimple(node, name);
        }
        
    }
    
    private String getExtension(String name)
    {
        String result = "";
        if (name != null)
        {
            name = name.trim();
            int index = name.lastIndexOf('.');
            if (index > -1 && (index < name.length() - 1))
            {
                result = name.substring(index + 1);
            }
        }
        return result;
    }
}
