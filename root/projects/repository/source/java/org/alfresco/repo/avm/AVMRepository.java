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
 * http://www.alfresco.com/legal/licensing" */

package org.alfresco.repo.avm;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.domain.DbAccessControlList;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.avm.AVMBadArgumentException;
import org.alfresco.service.cmr.avm.AVMCycleException;
import org.alfresco.service.cmr.avm.AVMException;
import org.alfresco.service.cmr.avm.AVMExistsException;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMNotFoundException;
import org.alfresco.service.cmr.avm.AVMStoreDescriptor;
import org.alfresco.service.cmr.avm.AVMWrongTypeException;
import org.alfresco.service.cmr.avm.LayeringDescriptor;
import org.alfresco.service.cmr.avm.VersionDescriptor;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This or AVMStore are
 * the implementors of the operations specified by AVMService.
 * @author britt
 */
public class AVMRepository
{
    @SuppressWarnings("unused")
    private static Log    fgLogger = LogFactory.getLog(AVMRepository.class);

    /**
     * The single instance of AVMRepository.
     */
    private static AVMRepository fgInstance;

    /**
     * The current lookup count.
     */
    private ThreadLocal<Integer> fLookupCount;

    /**
     * The node id issuer.
     */
    private Issuer fNodeIssuer;

    /**
     * The layer id issuer.
     */
    private Issuer fLayerIssuer;

    /**
     * Reference to the ContentStoreImpl
     */
    private ContentStore fContentStore;

    /**
     * The Lookup Cache instance.
     */
    private LookupCache fLookupCache;

    private AVMStoreDAO fAVMStoreDAO;

    private AVMNodeDAO fAVMNodeDAO;

    private VersionRootDAO fVersionRootDAO;

    private VersionLayeredNodeEntryDAO fVersionLayeredNodeEntryDAO;

    private AVMStorePropertyDAO fAVMStorePropertyDAO;

    private ChildEntryDAO fChildEntryDAO;

    private PermissionService fPermissionService;

    private DictionaryService fDictionaryService;

    // A bunch of TransactionListeners that do work for this.

    /**
     * One for create store.
     */
    private CreateStoreTxnListener fCreateStoreTxnListener;

    /**
     * One for purge store.
     */
    private PurgeStoreTxnListener fPurgeStoreTxnListener;

    /**
     * One for create version.
     */
    private CreateVersionTxnListener fCreateVersionTxnListener;

    /**
     * One for purge version.
     */
    private PurgeVersionTxnListener fPurgeVersionTxnListener;

    /**
     * Create a new one.
     */
    public AVMRepository()
    {
        fLookupCount = new ThreadLocal<Integer>();
        fgInstance = this;
    }

    /**
     * Set the node issuer. For Spring.
     * @param nodeIssuer The issuer.
     */
    public void setNodeIssuer(Issuer nodeIssuer)
    {
        fNodeIssuer = nodeIssuer;
    }

    /**
     * Set the layer issuer. For Spring.
     * @param layerIssuer The issuer.
     */
    public void setLayerIssuer(Issuer layerIssuer)
    {
        fLayerIssuer = layerIssuer;
    }

    /**
     * Set the ContentService.
     */
    public void setContentStore(ContentStore store)
    {
        fContentStore = store;
    }

    /**
     * Set the Lookup Cache instance.
     * @param cache The instance to set.
     */
    public void setLookupCache(LookupCache cache)
    {
        fLookupCache = cache;
    }

    public void setCreateStoreTxnListener(CreateStoreTxnListener listener)
    {
        fCreateStoreTxnListener = listener;
    }

    public void setPurgeStoreTxnListener(PurgeStoreTxnListener listener)
    {
        fPurgeStoreTxnListener = listener;
    }

    public void setCreateVersionTxnListener(CreateVersionTxnListener listener)
    {
        fCreateVersionTxnListener = listener;
    }

    public void setPurgeVersionTxnListener(PurgeVersionTxnListener listener)
    {
        fPurgeVersionTxnListener = listener;
    }

    public void setAvmStoreDAO(AVMStoreDAO dao)
    {
        fAVMStoreDAO = dao;
    }

    public void setAvmNodeDAO(AVMNodeDAO dao)
    {
        fAVMNodeDAO = dao;
    }

    public void setVersionRootDAO(VersionRootDAO dao)
    {
        fVersionRootDAO = dao;
    }

    public void setVersionLayeredNodeEntryDAO(VersionLayeredNodeEntryDAO dao)
    {
        fVersionLayeredNodeEntryDAO = dao;
    }

    public void setAvmStorePropertyDAO(AVMStorePropertyDAO dao)
    {
        fAVMStorePropertyDAO = dao;
    }

    public void setChildEntryDAO(ChildEntryDAO dao)
    {
        fChildEntryDAO = dao;
    }

    public void setPermissionService(PermissionService service)
    {
        fPermissionService = service;
    }

    public void setDictionaryService(DictionaryService service)
    {
        fDictionaryService = service;
    }

    /**
     * Create a file.
     * @param path The path to the containing directory.
     * @param name The name for the new file.
     */
    public OutputStream createFile(String path, String name)
    {
        fLookupCount.set(1);
        try
        {
            String[] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null) {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            OutputStream out = store.createFile(pathParts[1], name);
            return out;
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Create a file with the given File as content.
     * @param path The path to the containing directory.
     * @param name The name to give the file.
     * @param data The file contents.
     */
    public void createFile(String path, String name, File data, List<QName> aspects, Map<QName, PropertyValue> properties)
    {
        fLookupCount.set(1);
        try
        {
            String[] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.createFile(pathParts[1], name, data, aspects, properties);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Create a new directory.
     * @param path The path to the containing directory.
     * @param name The name to give the directory.
     */
    public void createDirectory(String path, String name, List<QName> aspects, Map<QName, PropertyValue> properties)
    {
        fLookupCount.set(1);
        try
        {
            String[] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.createDirectory(pathParts[1], name, aspects, properties);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Create a new directory. This assumes that the parent is already
     * copied and therefore should only be used with great care.
     * @param parent The parent node.
     * @param name The name of the new directory.
     * @return A descriptor for the newly created directory.
     */
    public AVMNodeDescriptor createDirectory(AVMNodeDescriptor parent, String name)
    {
        AVMNode node = fAVMNodeDAO.getByID(parent.getId());
        if (node == null)
        {
            throw new AVMNotFoundException(parent.getId() + " not found.");
        }
        if (!(node instanceof DirectoryNode))
        {
            throw new AVMWrongTypeException("Not a directory.");
        }
        if (!can(node, PermissionService.CREATE_CHILDREN))
        {
            throw new AccessDeniedException("Not allowed to write in: " + parent);
        }
        // We need the store to do anything so...
        String [] pathParts = SplitPath(parent.getPath());
        AVMStore store = getAVMStoreByName(pathParts[0]);
        if (store == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        DirectoryNode dir = (DirectoryNode)node;
        DirectoryNode child = null;
        if (dir instanceof LayeredDirectoryNode)
        {
            child = new LayeredDirectoryNodeImpl((String)null, store, null);
            ((LayeredDirectoryNode)child).setPrimaryIndirection(false);
            ((LayeredDirectoryNode)child).setLayerID(parent.getLayerID());
        }
        else
        {
            child = new PlainDirectoryNodeImpl(store);
        }
        dir.putChild(name, child);
        DbAccessControlList acl = dir.getAcl();
        child.setAcl(acl != null ? acl.getCopy() : null);
        fLookupCache.onWrite(pathParts[0]);
        AVMNodeDescriptor desc = child.getDescriptor(parent.getPath(), name, parent.getIndirection(), parent.getIndirectionVersion());
        return desc;
    }

    /**
     * Create a new layered directory.
     * @param srcPath The target indirection for the new layered directory.
     * @param dstPath The path to the containing directory.
     * @param name The name for the new directory.
     */
    public void createLayeredDirectory(String srcPath, String dstPath,
            String name)
    {
        if (dstPath.indexOf(srcPath) == 0)
        {
            throw new AVMCycleException("Cycle would be created.");
        }
        fLookupCount.set(1);
        try
        {
            String[] pathParts = SplitPath(dstPath);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.createLayeredDirectory(srcPath, pathParts[1], name);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Create a new layered file.
     * @param srcPath The target indirection for the new layered file.
     * @param dstPath The path to the containing directory.
     * @param name The name of the new layered file.
     */
    public void createLayeredFile(String srcPath, String dstPath, String name)
    {
        fLookupCount.set(1);
        try
        {
            String[] pathParts = SplitPath(dstPath);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.createLayeredFile(srcPath, pathParts[1], name);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Create a new AVMStore.
     * @param name The name to give the new AVMStore.
     */
    public void createAVMStore(String name)
    {
        AlfrescoTransactionSupport.bindListener(fCreateStoreTxnListener);
        if (getAVMStoreByName(name) != null)
        {
            throw new AVMExistsException("AVMStore exists: " + name);
        }
        // Newing up the object causes it to be written to the db.
        @SuppressWarnings("unused")
        AVMStore rep = new AVMStoreImpl(this, name);
        // Special handling for AVMStore creation.
        rep.getRoot().setStoreNew(null);
        fCreateStoreTxnListener.storeCreated(name);
    }

    /**
     * Create a new branch.
     * @param version The version to branch off.
     * @param srcPath The path to make a branch from.
     * @param dstPath The containing directory.
     * @param name The name of the new branch.
     */
    public void createBranch(int version, String srcPath, String dstPath, String name)
    {
        if (dstPath.indexOf(srcPath) == 0)
        {
            throw new AVMCycleException("Cycle would be created.");
        }
        // Lookup the src node.
        fLookupCount.set(1);
        String [] pathParts;
        Lookup sPath;
        List<VersionLayeredNodeEntry> layeredEntries = null;
        try
        {
            pathParts = SplitPath(srcPath);
            AVMStore srcRepo = getAVMStoreByName(pathParts[0]);
            if (srcRepo == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            if (version < 0)
            {
                fLookupCache.onSnapshot(pathParts[0]);
                version =
                    srcRepo.createSnapshot("Branch Snapshot", null, new HashMap<String, Integer>()).get(pathParts[0]);
            }
            sPath = srcRepo.lookup(version, pathParts[1], false, false);
            if (sPath == null)
            {
                throw new AVMNotFoundException("Path not found.");
            }
            VersionRoot lastVersion = fVersionRootDAO.getByVersionID(srcRepo, version);
            layeredEntries = fVersionLayeredNodeEntryDAO.get(lastVersion);
        }
        finally
        {
            fLookupCount.set(null);
        }
        // Lookup the destination directory.
        fLookupCount.set(1);
        try
        {
            pathParts = SplitPath(dstPath);
            AVMStore dstRepo = getAVMStoreByName(pathParts[0]);
            if (dstRepo == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            Lookup dPath = dstRepo.lookupDirectory(-1, pathParts[1], true);
            if (dPath == null)
            {
                throw new AVMNotFoundException("Path not found.");
            }
            DirectoryNode dirNode = (DirectoryNode)dPath.getCurrentNode();
            if (!can(dirNode, PermissionService.ADD_CHILDREN))
            {
                throw new AccessDeniedException("Not permitted to add children: " + dstPath);
            }
            AVMNode srcNode = sPath.getCurrentNode();
            AVMNode dstNode = null;
            // We do different things depending on what kind of thing we're
            // branching from. I'd be considerably happier if we disallowed
            // certain scenarios, but Jon won't let me :P (bhp).
            if (srcNode.getType() == AVMNodeType.PLAIN_DIRECTORY)
            {
                dstNode = new PlainDirectoryNodeImpl((PlainDirectoryNode)srcNode, dstRepo);
            }
            else if (srcNode.getType() == AVMNodeType.LAYERED_DIRECTORY)
            {
                dstNode =
                    new LayeredDirectoryNodeImpl((LayeredDirectoryNode)srcNode, dstRepo, sPath, false);
                ((LayeredDirectoryNode)dstNode).setLayerID(issueLayerID());
            }
            else if (srcNode.getType() == AVMNodeType.LAYERED_FILE)
            {
                dstNode = new LayeredFileNodeImpl((LayeredFileNode)srcNode, dstRepo);
            }
            else // This is a plain file.
            {
                dstNode = new PlainFileNodeImpl((PlainFileNode)srcNode, dstRepo);
            }
            // dstNode.setVersionID(dstRepo.getNextVersionID());
            dstNode.setAncestor(srcNode);
            dirNode.putChild(name, dstNode);
            dirNode.updateModTime();
            DbAccessControlList acl = srcNode.getAcl();
            dstNode.setAcl(acl != null ? acl.getCopy() : null);
            String beginingPath = AVMNodeConverter.NormalizePath(srcPath);
            String finalPath = AVMNodeConverter.ExtendAVMPath(dstPath, name);
            finalPath = AVMNodeConverter.NormalizePath(finalPath);
            VersionRoot latestVersion = fVersionRootDAO.getMaxVersion(dstRepo);
            for (VersionLayeredNodeEntry entry : layeredEntries)
            {
                String path = entry.getPath();
                if (!path.startsWith(srcPath))
                {
                    continue;
                }
                String newPath = finalPath + path.substring(beginingPath.length());
                VersionLayeredNodeEntry newEntry =
                    new VersionLayeredNodeEntryImpl(latestVersion, newPath);
                fVersionLayeredNodeEntryDAO.save(newEntry);
            }
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get an output stream to a file.
     * @param path The full path to the file.
     * @return An OutputStream.
     */
    public OutputStream getOutputStream(String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            OutputStream out = store.getOutputStream(pathParts[1]);
            return out;
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get a content reader from a file node.
     * @param version The version of the file.
     * @param path The path to the file.
     * @return A ContentReader.
     */
    public ContentReader getContentReader(int version, String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found: " + pathParts[0]);
            }
            return store.getContentReader(version, pathParts[1]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get a ContentWriter to a file node.
     * @param path The path to the file.
     * @return A ContentWriter.
     */
    public ContentWriter createContentWriter(String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found: " + pathParts[0]);
            }
            fLookupCache.onWrite(pathParts[0]);
            ContentWriter writer = store.createContentWriter(pathParts[1]);
            return writer;
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Rename a node.
     * @param srcPath Source containing directory.
     * @param srcName Source name.
     * @param dstPath Destination containing directory.
     * @param dstName Destination name.
     */
    public void rename(String srcPath, String srcName, String dstPath,
                       String dstName)
    {
        // This is about as ugly as it gets.
        if ((dstPath + "/").indexOf(srcPath + srcName + "/") == 0)
        {
            throw new AVMCycleException("Cyclic rename.");
        }
        fLookupCount.set(1);
        String [] pathParts;
        Lookup sPath;
        DirectoryNode srcDir;
        AVMNode srcNode;
        try
        {
            pathParts = SplitPath(srcPath);
            AVMStore srcRepo = getAVMStoreByName(pathParts[0]);
            if (srcRepo == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            sPath = srcRepo.lookupDirectory(-1, pathParts[1], true);
            if (sPath == null)
            {
                throw new AVMNotFoundException("Path not found.");
            }
            srcDir = (DirectoryNode)sPath.getCurrentNode();
            if (!can(srcDir, PermissionService.DELETE_CHILDREN) || !can(srcDir, PermissionService.ADD_CHILDREN))
            {
                throw new AccessDeniedException("Not allowed to read or write: " + srcPath);
            }
            Pair<AVMNode, Boolean> temp = srcDir.lookupChild(sPath, srcName, false);
            srcNode = (temp == null) ? null : temp.getFirst();
            if (srcNode == null)
            {
                throw new AVMNotFoundException("Not found: " + srcName);
            }
            fLookupCache.onDelete(pathParts[0]);
        }
        finally
        {
            fLookupCount.set(null);
        }
        fLookupCount.set(1);
        try
        {
            pathParts = SplitPath(dstPath);
            AVMStore dstRepo = getAVMStoreByName(pathParts[0]);
            if (dstRepo == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            Lookup dPath = dstRepo.lookupDirectory(-1, pathParts[1], true);
            if (dPath == null)
            {
                throw new AVMNotFoundException("Path not found.");
            }
            DirectoryNode dstDir = (DirectoryNode)dPath.getCurrentNode();
            if (!can(dstDir, PermissionService.ADD_CHILDREN))
            {
                throw new AccessDeniedException("Not allowed to write: " + dstPath);
            }
            Pair<AVMNode, Boolean> temp = dstDir.lookupChild(dPath, dstName, true);
            AVMNode child = (temp == null) ? null : temp.getFirst();
            if (child != null && child.getType() != AVMNodeType.DELETED_NODE)
            {
                throw new AVMExistsException("Node exists: " + dstName);
            }
            AVMNode dstNode = null;
            // We've passed the check, so we can go ahead and do the rename.
            if (srcNode.getType() == AVMNodeType.PLAIN_DIRECTORY)
            {
                // If the source is layered then the renamed thing needs to be layered also.
                if (sPath.isLayered())
                {
                    // If this is a rename happening in the same layer we make a new
                    // OverlayedDirectoryNode that is not a primary indirection layer.
                    // Otherwise we do make the new OverlayedDirectoryNode a primary
                    // Indirection layer.  This complexity begs the question of whether
                    // we should allow renames from within one layer to within another
                    // layer.  Allowing it makes the logic absurdly complex.
                    if (dPath.isLayered() && dPath.getTopLayer().equals(sPath.getTopLayer()))
                    {
                        dstNode = new LayeredDirectoryNodeImpl((PlainDirectoryNode)srcNode, dstRepo, sPath, true);
                        ((LayeredDirectoryNode)dstNode).setLayerID(sPath.getTopLayer().getLayerID());
                    }
                    else
                    {
                        dstNode = new LayeredDirectoryNodeImpl((DirectoryNode)srcNode, dstRepo, sPath, srcName);
                        ((LayeredDirectoryNode)dstNode).setLayerID(issueLayerID());
                    }
                }
                else
                {
                    dstNode = new PlainDirectoryNodeImpl((PlainDirectoryNode)srcNode, dstRepo);
                }
            }
            else if (srcNode.getType() == AVMNodeType.LAYERED_DIRECTORY)
            {
                if (!sPath.isLayered() || (sPath.isInThisLayer() &&
                        srcDir.getType() == AVMNodeType.LAYERED_DIRECTORY &&
                        ((LayeredDirectoryNode)srcDir).directlyContains(srcNode)))
                {
                    Lookup srcLookup = lookup(-1, srcPath + "/" + srcName, true);
                    // Use the simple 'copy' constructor.
                    dstNode =
                        new LayeredDirectoryNodeImpl((LayeredDirectoryNode)srcNode, dstRepo, srcLookup, true);
                    ((LayeredDirectoryNode)dstNode).setLayerID(((LayeredDirectoryNode)srcNode).getLayerID());
                }
                else
                {
                    // If the source node is a primary indirection, then the 'copy' constructor
                    // is used.  Otherwise the alternate constructor is called and its
                    // indirection is calculated from it's source context.
                    if (((LayeredDirectoryNode)srcNode).getPrimaryIndirection())
                    {
                        Lookup srcLookup = lookup(-1, srcPath + "/" + srcName, true);
                        dstNode =
                            new LayeredDirectoryNodeImpl((LayeredDirectoryNode)srcNode, dstRepo, srcLookup, true);
                    }
                    else
                    {
                        dstNode =
                            new LayeredDirectoryNodeImpl((DirectoryNode)srcNode, dstRepo, sPath, srcName);
                    }
                    // What needs to be done here is dependent on whether the
                    // rename is to a layered context.  If so then it should get the layer id
                    // of its destination parent.  Otherwise it should get a new layer
                    // id.
                    if (dPath.isLayered())
                    {
                        ((LayeredDirectoryNode)dstNode).setLayerID(dPath.getTopLayer().getLayerID());
                    }
                    else
                    {
                        ((LayeredDirectoryNode)dstNode).setLayerID(issueLayerID());
                    }
                }
            }
            else if (srcNode.getType() == AVMNodeType.LAYERED_FILE)
            {
                dstNode = new LayeredFileNodeImpl((LayeredFileNode)srcNode, dstRepo);
            }
            else // This is a plain file node.
            {
                dstNode = new PlainFileNodeImpl((PlainFileNode)srcNode, dstRepo);
            }
            srcDir.removeChild(sPath, srcName);
            srcDir.updateModTime();
            // dstNode.setVersionID(dstRepo.getNextVersionID());
            if (child != null)
            {
                dstNode.setAncestor(child);
            }
            dstDir.updateModTime();
            dstDir.putChild(dstName, dstNode);
            if (child == null)
            {
                dstNode.setAncestor(srcNode);
            }
            fLookupCache.onWrite(pathParts[0]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Uncover a deleted name in a layered directory.
     * @param dirPath The path to the layered directory.
     * @param name The name to uncover.
     */
    public void uncover(String dirPath, String name)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(dirPath);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.uncover(pathParts[1], name);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Create a snapshot of a single AVMStore.
     * @param store The name of the repository.
     * @param tag The short description.
     * @param description The thick description.
     * @return The version id of the newly snapshotted repository.
     */
    public Map<String, Integer> createSnapshot(String storeName, String tag, String description)
    {
        AlfrescoTransactionSupport.bindListener(fCreateVersionTxnListener);
        AVMStore store = getAVMStoreByName(storeName);
        if (store == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        Map<String, Integer> result = store.createSnapshot(tag, description, new HashMap<String, Integer>());
        for (Map.Entry<String, Integer> entry : result.entrySet())
        {
            fLookupCache.onSnapshot(entry.getKey());
            fCreateVersionTxnListener.versionCreated(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Remove a node and everything underneath it.
     * @param path The path to the containing directory.
     * @param name The name of the node to remove.
     */
    public void remove(String path, String name)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onDelete(pathParts[0]);
            store.removeNode(pathParts[1], name);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get rid of all content that lives only in the given AVMStore.
     * Also removes the AVMStore.
     * @param name The name of the AVMStore to purge.
     */
    @SuppressWarnings("unchecked")
    public void purgeAVMStore(String name)
    {
        AlfrescoTransactionSupport.bindListener(fPurgeStoreTxnListener);
        AVMStore store = getAVMStoreByName(name);
        if (store == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        fLookupCache.onDelete(name);
        AVMNode root = store.getRoot();
        // TODO Probably a special PermissionService.PURGE is needed.
        if (!can(root, PermissionService.DELETE_CHILDREN))
        {
            throw new AccessDeniedException("Not allowed to purge: " + name);
        }
        root.setIsRoot(false);
        List<VersionRoot> vRoots = fVersionRootDAO.getAllInAVMStore(store);
        for (VersionRoot vr : vRoots)
        {
            AVMNode node = vr.getRoot();
            node.setIsRoot(false);
            fVersionLayeredNodeEntryDAO.delete(vr);
            fVersionRootDAO.delete(vr);
        }
        List<AVMNode> newGuys = fAVMNodeDAO.getNewInStore(store);
        for (AVMNode newGuy : newGuys)
        {
            newGuy.setStoreNew(null);
        }
        fAVMStorePropertyDAO.delete(store);
        fAVMStoreDAO.delete(store);
        fAVMStoreDAO.invalidateCache();
        fPurgeStoreTxnListener.storePurged(name);
    }

    /**
     * Remove all content specific to a AVMRepository and version.
     * @param name The name of the AVMStore.
     * @param version The version to purge.
     */
    public void purgeVersion(String name, int version)
    {
        AlfrescoTransactionSupport.bindListener(fPurgeVersionTxnListener);
        AVMStore store = getAVMStoreByName(name);
        if (store == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        fLookupCache.onDelete(name);
        store.purgeVersion(version);
        fPurgeVersionTxnListener.versionPurged(name, version);
    }

    /**
     * Get an input stream from a file.
     * @param version The version to look under.
     * @param path The path to the file.
     * @return An InputStream.
     */
    public InputStream getInputStream(int version, String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            return store.getInputStream(version, pathParts[1]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    public InputStream getInputStream(AVMNodeDescriptor desc)
    {
        AVMNode node = fAVMNodeDAO.getByID(desc.getId());
        if (!(node instanceof FileNode))
        {
            throw new AVMWrongTypeException(desc + " is not a File.");
        }
        if (!can(node, PermissionService.READ_CONTENT))
        {
            throw new AccessDeniedException("Not allowed to read content: " + desc);
        }
        FileNode file = (FileNode)node;
        ContentData data = file.getContentData(null);
        if (data == null)
        {
            throw new AVMException(desc + " has no content.");
        }
        ContentReader reader = fContentStore.getReader(data.getContentUrl());
        return reader.getContentInputStream();
    }

    /**
     * Get a listing of a directory.
     * @param version The version to look under.
     * @param path The path to the directory.
     * @param includeDeleted Whether to see DeletedNodes.
     * @return A List of FolderEntries.
     */
    public SortedMap<String, AVMNodeDescriptor> getListing(int version, String path,
                                                           boolean includeDeleted)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            return store.getListing(version, pathParts[1], includeDeleted);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get the list of nodes directly contained in a directory.
     * @param version The version to look under.
     * @param path The path to the directory to list.
     * @return A Map of names to descriptors.
     */
    public SortedMap<String, AVMNodeDescriptor> getListingDirect(int version, String path,
                                                                 boolean includeDeleted)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            return store.getListingDirect(version, pathParts[1], includeDeleted);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get the list of nodes directly contained in a directory.
     * @param dir The descriptor to the directory node.
     * @param includeDeleted Whether to include deleted children.
     * @return A Map of names to descriptors.
     */
    public SortedMap<String, AVMNodeDescriptor>
        getListingDirect(AVMNodeDescriptor dir, boolean includeDeleted)
    {
        AVMNode node = fAVMNodeDAO.getByID(dir.getId());
        if (node == null)
        {
            throw new AVMBadArgumentException("Invalid Node.");
        }
        if (!can(node, PermissionService.READ_CHILDREN))
        {
            throw new AccessDeniedException("Not allowed to read children: " + dir);
        }
        if (node.getType() == AVMNodeType.PLAIN_DIRECTORY)
        {
            return getListing(dir, includeDeleted);
        }
        if (node.getType() != AVMNodeType.LAYERED_DIRECTORY)
        {
            throw new AVMWrongTypeException("Not a directory.");
        }
        LayeredDirectoryNode dirNode = (LayeredDirectoryNode)node;
        return dirNode.getListingDirect(dir, includeDeleted);
    }

    /**
     * Get a directory listing from a directory node descriptor.
     * @param dir The directory node descriptor.
     * @return A SortedMap listing.
     */
    public SortedMap<String, AVMNodeDescriptor> getListing(AVMNodeDescriptor dir, boolean includeDeleted)
    {
        fLookupCount.set(1);
        try
        {
            AVMNode node = fAVMNodeDAO.getByID(dir.getId());
            if (node == null)
            {
                throw new AVMBadArgumentException("Invalid Node.");
            }
            if (node.getType() != AVMNodeType.LAYERED_DIRECTORY &&
                    node.getType() != AVMNodeType.PLAIN_DIRECTORY)
            {
                throw new AVMWrongTypeException("Not a directory.");
            }
            if (!can(node, PermissionService.READ_CHILDREN))
            {
                throw new AccessDeniedException("Not allowed to read children: " + dir);
            }
            DirectoryNode dirNode = (DirectoryNode)node;
            SortedMap<String, AVMNodeDescriptor> listing = dirNode.getListing(dir, includeDeleted);
            return listing;
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get the names of deleted nodes in a directory.
     * @param version The version to look under.
     * @param path The path to the directory.
     * @return A List of names.
     */
    public List<String> getDeleted(int version, String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            return store.getDeleted(version, pathParts[1]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get descriptors of all AVMStores.
     * @return A list of all descriptors.
     */
    @SuppressWarnings("unchecked")
    public List<AVMStoreDescriptor> getAVMStores()
    {
        List<AVMStore> l = fAVMStoreDAO.getAll();
        List<AVMStoreDescriptor> result = new ArrayList<AVMStoreDescriptor>();
        for (AVMStore store : l)
        {
            result.add(store.getDescriptor());
        }
        return result;
    }

    /**
     * Get a descriptor for an AVMStore.
     * @param name The name to get.
     * @return The descriptor.
     */
    public AVMStoreDescriptor getAVMStore(String name)
    {
        AVMStore store = getAVMStoreByName(name);
        if (store == null)
        {
            return null;
        }
        return store.getDescriptor();
    }

    /**
     * Get all version for a given AVMStore.
     * @param name The name of the AVMStore.
     * @return A Set will all the version ids.
     */
    public List<VersionDescriptor> getAVMStoreVersions(String name)
    {
        AVMStore store = getAVMStoreByName(name);
        if (store == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        return store.getVersions();
    }

    /**
     * Get the set of versions between (inclusive) of the given dates.
     * From or to may be null but not both.
     * @param name The name of the AVMRepository.
     * @param from The earliest date.
     * @param to The latest date.
     * @return The Set of version IDs.
     */
    public List<VersionDescriptor> getAVMStoreVersions(String name, Date from, Date to)
    {
        AVMStore store = getAVMStoreByName(name);
        if (store == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        return store.getVersions(from, to);
    }

    /**
     * Issue a node id.
     * @return The new id.
     */
    public long issueID()
    {
        return fNodeIssuer.issue();
    }

    /**
     * Issue a new layer id.
     * @return The new id.
     */
    public long issueLayerID()
    {
        return fLayerIssuer.issue();
    }

    /**
     * Get the indirection path for a layered node.
     * @param version The version to look under.
     * @param path The path to the node.
     * @return The indirection path.
     */
    public String getIndirectionPath(int version, String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            return store.getIndirectionPath(version, pathParts[1]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get the next version id for the given AVMStore.
     * @param name The name of the AVMStore.
     * @return The next version id.
     */
    public int getLatestVersionID(String name)
    {
        AVMStore store = getAVMStoreByName(name);
        if (store == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        return store.getNextVersionID();
    }

    /**
     * Get the latest extant snapshotted version id.
     * @param name The store name.
     */
    public int getLatestSnapshotID(String name)
    {
        AVMStore store = getAVMStoreByName(name);
        if (store == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        return store.getLastVersionID();
    }

    /**
     * Get an AVMStore by name.
     * @param name The name of the AVMStore.
     * @return The AVMStore.
     */
    private AVMStore getAVMStoreByName(String name)
    {
        AVMStore store = fAVMStoreDAO.getByName(name);
        return store;
    }

    /**
     * Get a descriptor for an AVMStore root.
     * @param version The version to get.
     * @param name The name of the AVMStore.
     * @return The descriptor for the root.
     */
    public AVMNodeDescriptor getAVMStoreRoot(int version, String name)
    {
        AVMStore store = getAVMStoreByName(name);
        if (store == null)
        {
            throw new AVMNotFoundException("Not found: " + name);
        }
        return store.getRoot(version);
    }

    // TODO Fix this awful mess regarding cycle detection.
    /**
     * Lookup a node.
     * @param version The version to look under.
     * @param path The path to lookup.
     * @param includeDeleted Whether to see DeletedNodes.
     * @return A lookup object.
     */
    public Lookup lookup(int version, String path, boolean includeDeleted)
    {
        Integer count = fLookupCount.get();
        try
        {
            if (count == null)
            {
                fLookupCount.set(1);
            }
            else
            {
                fLookupCount.set(count + 1);
            }
            if (fLookupCount.get() > 50)
            {
                throw new AVMCycleException("Cycle in lookup.");
            }
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                return null;
            }
            return store.lookup(version, pathParts[1], false, includeDeleted);
        }
        finally
        {
            if (count == null)
            {
                fLookupCount.set(null);
            }
        }
    }

    /**
     * Lookup a descriptor from a directory descriptor.
     * @param dir The directory descriptor.
     * @param name The name of the child to lookup.
     * @return The child's descriptor.
     */
    public AVMNodeDescriptor lookup(AVMNodeDescriptor dir, String name, boolean includeDeleted)
    {
        fLookupCount.set(1);
        try
        {
            AVMNode node = fAVMNodeDAO.getByID(dir.getId());
            if (node == null)
            {
                throw new AVMNotFoundException("Not found: " + dir.getId());
            }
            if (node.getType() != AVMNodeType.LAYERED_DIRECTORY &&
                    node.getType() != AVMNodeType.PLAIN_DIRECTORY)
            {
                throw new AVMWrongTypeException("Not a directory.");
            }
            DirectoryNode dirNode = (DirectoryNode)node;
            if (!can(dirNode, PermissionService.READ_CHILDREN))
            {
                throw new AccessDeniedException("Not allowed to read children: " + dir);
            }
            return dirNode.lookupChild(dir, name, includeDeleted);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get all the paths to a particular node.
     * @param desc The node descriptor.
     * @return The list of version, paths.
     */
    public List<Pair<Integer, String>> getPaths(AVMNodeDescriptor desc)
    {
        AVMNode node = fAVMNodeDAO.getByID(desc.getId());
        if (node == null)
        {
            throw new AVMNotFoundException("Not found: " + desc);
        }
        List<Pair<Integer, String>> paths = new ArrayList<Pair<Integer, String>>();
        List<String> components = new ArrayList<String>();
        recursiveGetPaths(node, components, paths);
        return paths;
    }

    /**
     * Get a single valid path for a node.
     * @param desc The node descriptor.
     * @return A version, path
     */
    public Pair<Integer, String> getAPath(AVMNodeDescriptor desc)
    {
        AVMNode node = fAVMNodeDAO.getByID(desc.getId());
        if (node == null)
        {
            throw new AVMNotFoundException("Could not find node: " + desc);
        }
        List<String> components = new ArrayList<String>();
        return recursiveGetAPath(node, components);
    }

    /**
     * Get all paths for a node reachable by HEAD.
     * @param desc The node descriptor.
     * @return A List of all the version, path Pairs that match.
     */
    public List<Pair<Integer, String>> getHeadPaths(AVMNodeDescriptor desc)
    {
        AVMNode node = fAVMNodeDAO.getByID(desc.getId());
        if (node == null)
        {
            throw new AVMNotFoundException("Not found: " + desc.getPath());
        }
        List<Pair<Integer, String>> paths = new ArrayList<Pair<Integer, String>>();
        List<String> components = new ArrayList<String>();
        recursiveGetHeadPaths(node, components, paths);
        return paths;
    }

    /**
     * Gets all the pass from to the given node starting from the give version root.
     * @param version The version root.
     * @param node The node to get the paths of.
     * @return A list of all paths in the given version to the node.
     */
    public List<String> getVersionPaths(VersionRoot version, AVMNode node)
    {
        List<String> paths = new ArrayList<String>();
        List<String> components = new ArrayList<String>();
        recursiveGetVersionPaths(node, components, paths, version.getRoot(), version.getAvmStore().getName());
        return paths;
    }

    /**
     * Helper to get all version paths.
     * @param node The current node we are examining.
     * @param components The current path components.
     * @param paths The list to contain found paths.
     * @param root The root node of the version.
     * @param storeName The name of the store.
     */
    private void recursiveGetVersionPaths(AVMNode node, List<String> components, List<String> paths, DirectoryNode root, String storeName)
    {
        if (!can(node, PermissionService.READ_CHILDREN))
        {
            return;
        }
        if (node.equals(root))
        {
            paths.add(this.makePath(components, storeName));
            return;
        }
        List<ChildEntry> entries = fChildEntryDAO.getByChild(node);
        for (ChildEntry entry : entries)
        {
            String name = entry.getKey().getName();
            components.add(name);
            AVMNode parent = entry.getKey().getParent();
            recursiveGetVersionPaths(parent, components, paths, root, storeName);
            components.remove(components.size() - 1);
        }
    }

    /**
     * Get all paths in a particular store in the head version for
     * a particular node.
     * @param desc The node descriptor.
     * @param store The name of the store.
     * @return All matching paths.
     */
    public List<Pair<Integer, String>> getPathsInStoreHead(AVMNodeDescriptor desc, String store)
    {
        AVMStore st = getAVMStoreByName(store);
        if (st == null)
        {
            throw new AVMNotFoundException("Store not found: " + store);
        }
        AVMNode node = fAVMNodeDAO.getByID(desc.getId());
        if (node == null)
        {
            throw new AVMNotFoundException("Not found: " + desc);
        }
        List<Pair<Integer, String>> paths = new ArrayList<Pair<Integer, String>>();
        List<String> components = new ArrayList<String>();
        recursiveGetPathsInStoreHead(node, components, paths, st.getRoot(), store);
        return paths;
    }

    /**
     * Do the actual work.
     * @param node The current node.
     * @param components The currently accumulated path components.
     * @param paths The list to put full paths in.
     */
    private void recursiveGetPaths(AVMNode node, List<String> components,
                                   List<Pair<Integer, String>> paths)
    {
        if (!can(node, PermissionService.READ_CHILDREN))
        {
            return;
        }
        if (node.getIsRoot())
        {
            AVMStore store = fAVMStoreDAO.getByRoot(node);
            if (store != null)
            {
                addPath(components, -1, store.getName(), paths);
            }
            VersionRoot vr = fVersionRootDAO.getByRoot(node);
            if (vr != null)
            {
                addPath(components, vr.getVersionID(), vr.getAvmStore().getName(), paths);
            }
            return;
        }
        List<ChildEntry> entries = fChildEntryDAO.getByChild(node);
        for (ChildEntry entry : entries)
        {
            String name = entry.getKey().getName();
            components.add(name);
            AVMNode parent = entry.getKey().getParent();
            recursiveGetPaths(parent, components, paths);
            components.remove(components.size() - 1);
        }
    }

    /**
     * Do the work of getting one path for a node.
     * @param node The node to get the path of.
     * @param components The storage for path components.
     * @return A path or null.
     */
    private Pair<Integer, String> recursiveGetAPath(AVMNode node, List<String> components)
    {
        if (!can(node, PermissionService.READ_CHILDREN))
        {
            return null;
        }
        if (node.getIsRoot())
        {
            AVMStore store = fAVMStoreDAO.getByRoot(node);
            if (store != null)
            {
                return new Pair<Integer, String>(-1, makePath(components, store.getName()));
            }
            VersionRoot vr = fVersionRootDAO.getByRoot(node);
            if (vr != null)
            {
                return new Pair<Integer, String>(vr.getVersionID(), makePath(components, vr.getAvmStore().getName()));
            }
            return null;
        }
        List<ChildEntry> entries = fChildEntryDAO.getByChild(node);
        for (ChildEntry entry : entries)
        {
            String name = entry.getKey().getName();
            components.add(name);
            Pair<Integer, String> path = recursiveGetAPath(entry.getKey().getParent(), components);
            if (path != null)
            {
                return path;
            }
            components.remove(components.size() - 1);
        }
        return null;
    }

    /**
     * Do the actual work.
     * @param node The current node.
     * @param components The currently accumulated path components.
     * @param paths The list to put full paths in.
     */
    private void recursiveGetHeadPaths(AVMNode node, List<String> components,
                                       List<Pair<Integer, String>> paths)
    {
        if (!can(node, PermissionService.READ_CHILDREN))
        {
            return;
        }
        if (node.getIsRoot())
        {
            AVMStore store = fAVMStoreDAO.getByRoot(node);
            if (store != null)
            {
                addPath(components, -1, store.getName(), paths);
                return;
            }
            return;
        }
        List<ChildEntry> entries = fChildEntryDAO.getByChild(node);
        for (ChildEntry entry : entries)
        {
            String name = entry.getKey().getName();
            components.add(name);
            AVMNode parent = entry.getKey().getParent();
            recursiveGetHeadPaths(parent, components, paths);
            components.remove(components.size() - 1);
        }
    }

    /**
     * Do the actual work.
     * @param node The current node.
     * @param components The currently accumulated path components.
     * @param paths The list to put full paths in.
     */
    private void recursiveGetPathsInStoreHead(AVMNode node, List<String> components,
                                              List<Pair<Integer, String>> paths, DirectoryNode root,
                                              String storeName)
    {
        if (!can(node, PermissionService.READ_CHILDREN))
        {
            return;
        }
        if (node.equals(root))
        {
            addPath(components, -1, storeName, paths);
            return;
        }
        List<ChildEntry> entries = fChildEntryDAO.getByChild(node);
        for (ChildEntry entry : entries)
        {
            String name = entry.getKey().getName();
            components.add(name);
            AVMNode parent = entry.getKey().getParent();
            recursiveGetHeadPaths(parent, components, paths);
            components.remove(components.size() - 1);
        }
    }

    /**
     * Add a path to the list.
     * @param components The path name components.
     * @param version The version id.
     * @param storeName The name of the
     * @param paths The List to add to.
     */
    private void addPath(List<String> components, int version, String storeName,
                         List<Pair<Integer, String>> paths)
    {
        paths.add(new Pair<Integer, String>(version, makePath(components, storeName)));
    }

    /**
     * Alternate version.
     * @param components
     * @param storeName
     * @param paths
     */
    private void addPath(List<String> components, String storeName,
                         List<String> paths)
    {
        paths.add(makePath(components, storeName));
    }

    /**
     * Helper for generating paths.
     * @param components The path components.
     * @param storeName The store that the path is in.
     * @return The path.
     */
    private String makePath(List<String> components, String storeName)
    {
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(storeName);
        pathBuilder.append(":");
        if (components.size() == 0)
        {
            pathBuilder.append("/");
            return pathBuilder.toString();
        }
        for (int i = components.size() - 1; i >= 0; i--)
        {
            pathBuilder.append("/");
            pathBuilder.append(components.get(i));
        }
        return pathBuilder.toString();
    }

    /**
     * Get information about layering of a path.
     * @param version The version to look under.
     * @param path The full avm path.
     * @return A LayeringDescriptor.
     */
    public LayeringDescriptor getLayeringInfo(int version, String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            Lookup lookup = store.lookup(version, pathParts[1], false, true);
            if (lookup == null)
            {
                throw new AVMNotFoundException("Path not found.");
            }
            if (!can(lookup.getCurrentNode(), PermissionService.READ_PROPERTIES))
            {
                throw new AccessDeniedException("Not allowed to read properties: " + path);
            }
            return new LayeringDescriptor(!lookup.getDirectlyContained(),
                    lookup.getAVMStore().getDescriptor(),
                    lookup.getFinalStore().getDescriptor());
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Lookup a directory specifically.
     * @param version The version to look under.
     * @param path The path to lookup.
     * @return A lookup object.
     */
    public Lookup lookupDirectory(int version, String path)
    {
        Integer count = fLookupCount.get();
        try
        {
            if (count == null)
            {
                fLookupCount.set(1);
            }
            fLookupCount.set(fLookupCount.get() + 1);
            if (fLookupCount.get() > 50)
            {
                throw new AVMCycleException("Cycle in lookup.");
            }
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                return null;
            }
            return store.lookupDirectory(version, pathParts[1], false);
        }
        finally
        {
            if (count == null)
            {
                fLookupCount.set(null);
            }
        }
    }

    /**
     * Utility to split a path, foo:bar/baz into its repository and path parts.
     * @param path The fully qualified path.
     * @return The repository name and the repository path.
     */
    private String[] SplitPath(String path)
    {
        String [] pathParts = path.split(":");
        if (pathParts.length != 2)
        {
            throw new AVMException("Invalid path: " + path);
        }
        return pathParts;
    }

    /**
     * Make a directory into a primary indirection.
     * @param path The full path.
     */
    public void makePrimary(String path)
    {
        fLookupCount.set(1);
        try
        {
            String[] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.makePrimary(pathParts[1]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Change what a layered directory points at.
     * @param path The full path to the layered directory.
     * @param target The new target path.
     */
    public void retargetLayeredDirectory(String path, String target)
    {
        fLookupCount.set(1);
        try
        {
            String[] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.retargetLayeredDirectory(pathParts[1], target);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get the history chain for a node.
     * @param desc The node to get history of.
     * @param count The maximum number of ancestors to traverse.  Negative means all.
     * @return A List of ancestors.
     */
    public List<AVMNodeDescriptor> getHistory(AVMNodeDescriptor desc, int count)
    {
        AVMNode node = fAVMNodeDAO.getByID(desc.getId());
        if (node == null)
        {
            throw new AVMNotFoundException("Not found.");
        }
        if (!can(node, PermissionService.READ_PROPERTIES))
        {
            throw new AccessDeniedException("Not allowed to read properties: " + desc);
        }
        if (count < 0)
        {
            count = Integer.MAX_VALUE;
        }
        List<AVMNodeDescriptor> history = new ArrayList<AVMNodeDescriptor>();
        for (int i = 0; i < count; i++)
        {
            node = node.getAncestor();
            if (node == null)
            {
                break;
            }
            if (!can(node, PermissionService.READ_PROPERTIES))
            {
                break;
            }
            history.add(node.getDescriptor("UNKNOWN", "UNKNOWN", "UNKNOWN", -1));
        }
        return history;
    }

    /**
     * Set the opacity of a layered directory. An opaque directory hides
     * the things it points to via indirection.
     * @param path The path to the layered directory.
     * @param opacity True is opaque; false is not.
     */
    public void setOpacity(String path, boolean opacity)
    {
        fLookupCount.set(1);
        try
        {
            String[] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.setOpacity(pathParts[1], opacity);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Set a property on a node.
     * @param path The path to the node.
     * @param name The name of the property.
     * @param value The value of the property.
     */
    public void setNodeProperty(String path, QName name, PropertyValue value)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.setNodeProperty(pathParts[1], name, value);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Set a collection of properties at once.
     * @param path The path to the node.
     * @param properties The Map of QNames to PropertyValues.
     */
    public void setNodeProperties(String path, Map<QName, PropertyValue> properties)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.setNodeProperties(pathParts[1], properties);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get a property by name for a node.
     * @param version The version to look under.
     * @param path The path to the node.
     * @param name The name of the property.
     * @return The PropertyValue or null if it does not exist.
     */
    public PropertyValue getNodeProperty(int version, String path, QName name)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            return store.getNodeProperty(version, pathParts[1], name);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get a Map of all the properties of a node.
     * @param version The version to look under.
     * @param path The path to the node.
     * @return A Map of QNames to PropertyValues.
     */
    public Map<QName, PropertyValue> getNodeProperties(int version, String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            return store.getNodeProperties(version, pathParts[1]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Delete a single property from a node.
     * @param path The path to the node.
     * @param name The name of the property.
     */
    public void deleteNodeProperty(String path, QName name)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.deleteNodeProperty(pathParts[1], name);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Delete all properties on a node.
     * @param path The path to the node.
     */
    public void deleteNodeProperties(String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.deleteNodeProperties(pathParts[1]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Set a property on a store. Overwrites if property exists.
     * @param store The AVMStore.
     * @param name The QName.
     * @param value The PropertyValue to set.
     */
    public void setStoreProperty(String store, QName name, PropertyValue value)
    {
        AVMStore st = getAVMStoreByName(store);
        if (st == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        st.setProperty(name, value);
    }

    /**
     * Set a group of properties on a store. Overwrites any properties that exist.
     * @param store The AVMStore.
     * @param props The properties to set.
     */
    public void setStoreProperties(String store, Map<QName, PropertyValue> props)
    {
        AVMStore st = getAVMStoreByName(store);
        if (st == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        st.setProperties(props);
    }

    /**
     * Get a property from a store.
     * @param store The name of the store.
     * @param name The property
     * @return The property value or null if non-existent.
     */
    public PropertyValue getStoreProperty(String store, QName name)
    {
        if (store == null)
        {
            throw new AVMBadArgumentException("Null store name.");
        }
        AVMStore st = getAVMStoreByName(store);
        if (st == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        return st.getProperty(name);
    }

    /**
     * Queries a given store for properties with keys that match a given pattern.
     * @param store The name of the store.
     * @param keyPattern The sql 'like' pattern, inserted into a QName.
     * @return A Map of the matching key value pairs.
     */
    public Map<QName, PropertyValue> queryStorePropertyKey(String store, QName keyPattern)
    {
        AVMStore st = getAVMStoreByName(store);
        if (st == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        List<AVMStoreProperty> matches =
            fAVMStorePropertyDAO.queryByKeyPattern(st,
                                                   keyPattern);
        Map<QName, PropertyValue> results = new HashMap<QName, PropertyValue>();
        for (AVMStoreProperty prop : matches)
        {
            results.put(prop.getName(), prop.getValue());
        }
        return results;
    }

    /**
     * Queries all AVM stores for properties with keys that match a given pattern.
     * @param keyPattern The sql 'like' pattern, inserted into a QName.
     * @return A List of Pairs of Store name, Map.Entry.
     */
    public Map<String, Map<QName, PropertyValue>>
        queryStoresPropertyKeys(QName keyPattern)
    {
        List<AVMStoreProperty> matches =
            fAVMStorePropertyDAO.queryByKeyPattern(keyPattern);
        Map<String, Map<QName, PropertyValue>> results =
            new HashMap<String, Map<QName, PropertyValue>>();
        for (AVMStoreProperty prop : matches)
        {
            String storeName = prop.getStore().getName();
            Map<QName, PropertyValue> pairs = null;
            if ((pairs = results.get(storeName)) == null)
            {
                pairs = new HashMap<QName, PropertyValue>();
                results.put(storeName, pairs);
            }
            pairs.put(prop.getName(), prop.getValue());
        }
        return results;
    }

    /**
     * Get all the properties for a store.
     * @param store The name of the Store.
     * @return A Map of all the properties.
     */
    public Map<QName, PropertyValue> getStoreProperties(String store)
    {
        if (store == null)
        {
            throw new AVMBadArgumentException("Null store name.");
        }
        AVMStore st = getAVMStoreByName(store);
        if (st == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        return st.getProperties();
    }

    /**
     * Delete a property from a store.
     * @param store The name of the store.
     * @param name The name of the property.
     */
    public void deleteStoreProperty(String store, QName name)
    {
        AVMStore st = getAVMStoreByName(store);
        if (st == null)
        {
            throw new AVMNotFoundException("Store not found.");
        }
        st.deleteProperty(name);
    }

    /**
     * Get the common ancestor of two nodes if one exists. Unfortunately
     * this is a quadratic problem, taking time proportional to the product
     * of the lengths of the left and right history chains.
     * @param left The first node.
     * @param right The second node.
     * @return The common ancestor. There are four possible results. Null means
     * that there is no common ancestor.  Left returned means that left is strictly
     * an ancestor of right.  Right returned means that right is strictly an
     * ancestor of left.  Any other non null return is the common ancestor and
     * indicates that left and right are in conflict.
     */
    public AVMNodeDescriptor getCommonAncestor(AVMNodeDescriptor left,
                                               AVMNodeDescriptor right)
    {
        AVMNode lNode = fAVMNodeDAO.getByID(left.getId());
        AVMNode rNode = fAVMNodeDAO.getByID(right.getId());
        if (lNode == null || rNode == null)
        {
            throw new AVMNotFoundException("Node not found.");
        }
        if (!can(lNode, PermissionService.READ_PROPERTIES))
        {
            throw new AccessDeniedException("Not allowed to read properties: " + left);
        }
        if (!can(rNode, PermissionService.READ_PROPERTIES))
        {
            throw new AccessDeniedException("Not allowed to read properties: " + right);
        }
        // TODO Short changing the permissions checking here. I'm not sure
        // if that's OK.
        List<AVMNode> leftHistory = new ArrayList<AVMNode>();
        List<AVMNode> rightHistory = new ArrayList<AVMNode>();
        while (lNode != null || rNode != null)
        {
            boolean checkRight = false;
            if (lNode != null)
            {
                leftHistory.add(lNode);
                checkRight = true;
                lNode = lNode.getAncestor();
            }
            boolean checkLeft = false;
            if (rNode != null)
            {
                rightHistory.add(rNode);
                checkLeft = true;
                rNode = rNode.getAncestor();
            }
            if (checkRight)
            {
                AVMNode check = leftHistory.get(leftHistory.size() - 1);
                for (AVMNode node : rightHistory)
                {
                    if (node.equals(check))
                    {
                        return node.getDescriptor("", "", "", -1);
                    }
                }
            }
            if (checkLeft)
            {
                AVMNode check = rightHistory.get(rightHistory.size() - 1);
                for (AVMNode node : leftHistory)
                {
                    if (node.equals(check))
                    {
                        return node.getDescriptor("", "", "", -1);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the ContentData for a file.
     * @param version The version to look under.
     * @param path The path to the file.
     * @return The ContentData for the file.
     */
    public ContentData getContentDataForRead(int version, String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            return store.getContentDataForRead(version, pathParts[1]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get the ContentData for a file for writing.
     * @param path The path to the file.
     * @return The ContentData object.
     */
    public ContentData getContentDataForWrite(String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            ContentData result = store.getContentDataForWrite(pathParts[1]);
            return result;
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Set the ContentData on a file.
     * @param path The path to the file.
     * @param data The content data to set.
     */
    public void setContentData(String path, ContentData data)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.setContentData(pathParts[1], data);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get the single instance of AVMRepository.
     * @return The single instance.
     */
    public static AVMRepository GetInstance()
    {
        return fgInstance;
    }

    public void setMetaDataFrom(String path, AVMNodeDescriptor from)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found: " + pathParts[0]);
            }
            AVMNode fromNode = fAVMNodeDAO.getByID(from.getId());
            if (fromNode == null)
            {
                throw new AVMNotFoundException("Node not found: " + from.getPath());
            }
            fLookupCache.onWrite(pathParts[0]);
            store.setMetaDataFrom(pathParts[1], fromNode);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Add an aspect to an AVM Node.
     * @param path The path to the node.
     * @param aspectName The name of the aspect.
     */
    public void addAspect(String path, QName aspectName)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.addAspect(pathParts[1], aspectName);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get all the aspects on an AVM node.
     * @param version The version to look under.
     * @param path The path to the node.
     * @return A List of the QNames of the Aspects.
     */
    public Set<QName> getAspects(int version, String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            return store.getAspects(version, pathParts[1]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Remove an aspect and all associated properties from a node.
     * @param path The path to the node.
     * @param aspectName The name of the aspect.
     */
    public void removeAspect(String path, QName aspectName)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.removeAspect(pathParts[1], aspectName);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Does a node have a particular aspect.
     * @param version The version to look under.
     * @param path The path to the node.
     * @param aspectName The name of the aspect.
     * @return Whether the node has the aspect.
     */
    public boolean hasAspect(int version, String path, QName aspectName)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            return store.hasAspect(version, pathParts[1], aspectName);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Set the ACL on a node.
     * @param path The path to the node.
     * @param acl The ACL to set.
     */
    public void setACL(String path, DbAccessControlList acl)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            store.setACL(pathParts[1], acl);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Get the ACL on a node.
     * @param version The version to look under.
     * @param path The path to the node.
     * @return The ACL.
     */
    public DbAccessControlList getACL(int version, String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            return store.getACL(version, pathParts[1]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Link a node into a directory, directly.
     * @param parentPath The path to the parent.
     * @param name The name to give the node.
     * @param toLink The node to link.
     */
    public void link(String parentPath, String name, AVMNodeDescriptor toLink)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(parentPath);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            store.link(pathParts[1], name, toLink);
            fLookupCache.onWrite(pathParts[0]);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }


    /**
     * This is the danger version of link. It must be called on
     * a copied and unsnapshotted directory.  It blithely inserts
     * a child without checking if a child exists with a conflicting name.
     * @param parent The parent directory.
     * @param name The name to give the child.
     * @param child The child to link in.
     */
    public void link(AVMNodeDescriptor parent, String name, AVMNodeDescriptor child)
    {
        AVMNode node = fAVMNodeDAO.getByID(parent.getId());
        if (!(node instanceof DirectoryNode))
        {
            throw new AVMWrongTypeException("Not a Directory.");
        }
        DirectoryNode dir = (DirectoryNode)node;
        if (!dir.getIsNew())
        {
            throw new AVMException("Directory has not already been copied.");
        }
        if (!can(dir, PermissionService.ADD_CHILDREN))
        {
            throw new AccessDeniedException("Not allowed to write: " + parent);
        }
        dir.link(name, child);
    }

    /**
     * Remove name without leaving behind a deleted node. Dangerous
     * if used unwisely.
     * @param path The path to the layered directory.
     * @param name The name of the child.
     */
    public void flatten(String path, String name)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onDelete(pathParts[0]);
            Lookup lPath = store.lookup(-1, pathParts[1], true, false);
            AVMNode node = lPath.getCurrentNode();
            if (node == null)
            {
                throw new AVMNotFoundException("Path not found.");
            }
            if (!(node instanceof LayeredDirectoryNode))
            {
                throw new AVMWrongTypeException("Not a Layered Directory.");
            }
            if (!can(node, PermissionService.DELETE_CHILDREN))
            {
                throw new AccessDeniedException("Not allowed to write in: " + path);
            }
            LayeredDirectoryNode dir = (LayeredDirectoryNode)node;
            dir.flatten(name);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Force a copy on write.
     * @param path The path to force.
     */
    public AVMNodeDescriptor forceCopy(String path)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found.");
            }
            fLookupCache.onWrite(pathParts[0]);
            // Just force a copy if needed by looking up in write mode.
            Lookup lPath = store.lookup(-1, pathParts[1], true, true);
            if (lPath == null)
            {
                throw new AVMNotFoundException("Path not found.");
            }
            AVMNode node = lPath.getCurrentNode();
            AVMNodeDescriptor desc = node.getDescriptor(lPath);
            return desc;
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Rename a store.
     * @param sourceName The original name.
     * @param destName The new name.
     * @throws AVMNotFoundException
     * @throws AVMExistsException
     */
    public void renameStore(String sourceName, String destName)
    {
        AlfrescoTransactionSupport.bindListener(fPurgeStoreTxnListener);
        AlfrescoTransactionSupport.bindListener(fCreateStoreTxnListener);
        AVMStore store = getAVMStoreByName(sourceName);
        if (store == null)
        {
            throw new AVMNotFoundException("Store Not Found: " + sourceName);
        }
        if (getAVMStoreByName(destName) != null)
        {
            throw new AVMExistsException("Store Already Exists: " + destName);
        }
        if (!FileNameValidator.IsValid(destName))
        {
            throw new AVMBadArgumentException("Bad store name: " + destName);
        }
        store.setName(destName);
        store.createSnapshot("Rename Store", "Rename Store from " + sourceName + " to " + destName, new HashMap<String, Integer>());
        fLookupCache.onDelete(sourceName);
        fAVMStoreDAO.invalidateCache();
        fPurgeStoreTxnListener.storePurged(sourceName);
        fCreateStoreTxnListener.storeCreated(destName);
    }

    /**
     * Revert a head path to a given version. This works by cloning
     * the version to revert to, and then linking that new version into head.
     * The reverted version will have the previous head version as ancestor.
     * @param path The path to the parent directory.
     * @param name The name of the node.
     * @param toRevertTo The descriptor of the version to revert to.
     */
    public void revert(String path, String name, AVMNodeDescriptor toRevertTo)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found: " + pathParts[0]);
            }
            fLookupCache.onWrite(pathParts[0]);
            store.revert(pathParts[1], name, toRevertTo);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Set the GUID on a node.
     * @param path
     * @param guid
     */
    public void setGuid(String path, String guid)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store not found:" + pathParts[0]);
            }
            fLookupCache.onWrite(pathParts[0]);
            store.setGuid(pathParts[1], guid);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Set the encoding on a node.
     * @param path
     * @param encoding
     */
    public void setEncoding(String path, String encoding)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store Not Found: " + pathParts[0]);
            }
            fLookupCache.onWrite(pathParts[0]);
            store.setEncoding(pathParts[1], encoding);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    /**
     * Set the mime type on a node.
     * @param path
     * @param encoding
     */
    public void setMimeType(String path, String mimeType)
    {
        fLookupCount.set(1);
        try
        {
            String [] pathParts = SplitPath(path);
            AVMStore store = getAVMStoreByName(pathParts[0]);
            if (store == null)
            {
                throw new AVMNotFoundException("Store Not Found: " + pathParts[0]);
            }
            fLookupCache.onWrite(pathParts[0]);
            store.setMimeType(pathParts[1], mimeType);
        }
        finally
        {
            fLookupCount.set(null);
        }
    }

    public List<String> getPathsInStoreVersion(AVMNodeDescriptor desc, String store, int version)
    {
        AVMNode node = fAVMNodeDAO.getByID(desc.getId());
        if (node == null)
        {
            throw new AVMNotFoundException("Not found: " + desc);
        }
        List<String> paths = new ArrayList<String>();
        List<String> components = new ArrayList<String>();
        recursiveGetStoreVersionPaths(store, node, version, components, paths);
        return paths;
    }

    /**
     * Do the actual work.
     * @param node The current node.
     * @param components The currently accumulated path components.
     * @param paths The list to put full paths in.
     */
    private void recursiveGetStoreVersionPaths(String storeName, AVMNode node, int version, List<String> components,
                                               List<String> paths)
    {
        if (!can(node, PermissionService.READ))
        {
            return;
        }
        if (node.getIsRoot())
        {
            VersionRoot versionRoot = fVersionRootDAO.getByRoot(node);
            if (versionRoot.getAvmStore().getName().equals(storeName) &&
                versionRoot.getVersionID() == version)
            {
                addPath(components, storeName, paths);
                return;
            }
            return;
        }
        List<ChildEntry> entries = fChildEntryDAO.getByChild(node);
        for (ChildEntry entry : entries)
        {
            String name = entry.getKey().getName();
            components.add(name);
            AVMNode parent = entry.getKey().getParent();
            recursiveGetStoreVersionPaths(storeName, parent, version, components, paths);
            components.remove(components.size() - 1);
        }
    }

    public Map<QName, PropertyValue> getNodeProperties(AVMNodeDescriptor desc)
    {
        AVMNode node = fAVMNodeDAO.getByID(desc.getId());
        if (node == null)
        {
            throw new AVMNotFoundException("Node not found: " + desc);
        }
        if (!can(node, PermissionService.READ_PROPERTIES))
        {
            throw new AccessDeniedException("Not allowed to read properties: " + desc);
        }
        return node.getProperties();
    }

    public ContentData getContentDataForRead(AVMNodeDescriptor desc)
    {
        AVMNode node = fAVMNodeDAO.getByID(desc.getId());
        if (node == null)
        {
            throw new AVMNotFoundException("Node not found: " + desc);
        }
        if (!can(node, PermissionService.READ_CONTENT))
        {
            throw new AccessDeniedException("Not allowed to read: " + desc);
        }
        if (node.getType() == AVMNodeType.PLAIN_FILE)
        {
            PlainFileNode file = (PlainFileNode)node;
            return file.getContentData();
        }
        throw new AVMWrongTypeException("Not a Plain File: " + desc);
    }

    public Set<QName> getAspects(AVMNodeDescriptor desc)
    {
        AVMNode node = fAVMNodeDAO.getByID(desc.getId());
        if (node == null)
        {
            throw new AVMNotFoundException("Node not found: " + desc);
        }
        if (!can(node, PermissionService.READ_PROPERTIES))
        {
            throw new AccessDeniedException("Not allowed to read properties: " + desc);
        }
        Set<QName> aspects = node.getAspects();
        return aspects;
    }

    /**
     * Evaluate permission on a node.
     * I've got a bad feeling about this...
     * @param node
     * @param permission
     * @return
     */
    public boolean can(AVMNode node, String permission)
    {
        DbAccessControlList acl = node.getAcl();
        if (acl == null)
        {
            return true;
        }
        Map<String, Object> context = new HashMap<String, Object>(4);
        context.put(PermissionService.OWNER_AUTHORITY, node.getBasicAttributes().getOwner());
        context.put(PermissionService.ASPECTS, node.getAspects());
        Map<QName, PropertyValue> props = node.getProperties();
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>(5);
        for (Map.Entry<QName, PropertyValue> entry : props.entrySet())
        {
            PropertyDefinition def = fDictionaryService.getProperty(entry.getKey());
            if (def == null)
            {
                continue;
            }
            properties.put(entry.getKey(), entry.getValue().getValue(def.getDataType().getName()));
        }
        context.put(PermissionService.PROPERTIES, properties);
        // TODO put node type in there to.
        return fPermissionService.hasPermission(acl.getId(), context, permission)
            == AccessStatus.ALLOWED;
    }
}
