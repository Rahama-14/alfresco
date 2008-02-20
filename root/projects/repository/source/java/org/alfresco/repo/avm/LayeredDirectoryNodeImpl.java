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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.alfresco.service.cmr.avm.AVMBadArgumentException;
import org.alfresco.service.cmr.avm.AVMException;
import org.alfresco.service.cmr.avm.AVMExistsException;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMNotFoundException;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.Pair;

/**
 * A layered directory node.  A layered directory node points at
 * an underlying directory, which may or may not exist.  The visible
 * contents of a layered directory node is the contents of the underlying node
 * pointed at plus those nodes added to or modified in the layered directory node minus
 * those nodes which have been deleted in the layered directory node.
 * @author britt
 */
class LayeredDirectoryNodeImpl extends DirectoryNodeImpl implements LayeredDirectoryNode
{
    static final long serialVersionUID = 4623043057918181724L;

    /**
     * The layer id.
     */
    private long fLayerID;

    /**
     * The pointer to the underlying directory.
     */
    private String fIndirection;

    /**
     * Whether this is a primary indirection node.
     */
    private boolean fPrimaryIndirection;

    /**
     * Whether this is opaque.
     */
    private boolean fOpacity;

    /**
     * The indirection version.
     */
    private int fIndirectionVersion;

    /**
     * Default constructor. Called by Hibernate.
     */
    protected LayeredDirectoryNodeImpl()
    {
    }

    /**
     * Make a new one from a specified indirection path.
     * @param indirection The indirection path to set.
     * @param store The store that owns this node.
     */
    public LayeredDirectoryNodeImpl(String indirection, AVMStore store, AVMNode toCopy)
    {
        super(store.getAVMRepository().issueID(), store);
        fLayerID = -1;
        fIndirection = indirection;
        fIndirectionVersion = -1;
        fPrimaryIndirection = true;
        fOpacity = false;
        if (toCopy != null)
        {
            setVersionID(toCopy.getVersionID() + 1);
        }
        else
        {
            setVersionID(1);
        }
        AVMDAOs.Instance().fAVMNodeDAO.save(this);
        AVMDAOs.Instance().fAVMNodeDAO.flush();
        if (toCopy != null)
        {
            copyProperties(toCopy);
            copyACLs(toCopy);
            copyAspects(toCopy);
        }
    }

    /**
     * Kind of copy constructor, sort of.
     * @param other The LayeredDirectoryNode we are copied from.
     * @param repos The AVMStore object we use.
     */
    @SuppressWarnings("unchecked")
    public LayeredDirectoryNodeImpl(LayeredDirectoryNode other,
                                    AVMStore repos,
                                    Lookup lookup, boolean copyAll)
    {
        super(repos.getAVMRepository().issueID(), repos);
        fIndirection = other.getIndirection();
        fPrimaryIndirection = other.getPrimaryIndirection();
        fIndirectionVersion = -1;
        fLayerID = -1;
        fOpacity = false;
        AVMDAOs.Instance().fAVMNodeDAO.save(this);
        Map<String, AVMNode> children = null;
        if (copyAll)
        {
            children = other.getListing(lookup, true);
        }
        else
        {
            children = other.getListingDirect(lookup, true);
        }
        for (Map.Entry<String, AVMNode> child : children.entrySet())
        {
            ChildKey key = new ChildKey(this, child.getKey());
            ChildEntry entry = new ChildEntryImpl(key, child.getValue());
            AVMDAOs.Instance().fChildEntryDAO.save(entry);
        }
        setVersionID(other.getVersionID() + 1);
        AVMDAOs.Instance().fAVMNodeDAO.flush();
        copyProperties(other);
        copyAspects(other);
        copyACLs(other);
    }

    /**
     * Construct one from a PlainDirectoryNode.  Called when a COW is performed in a layered
     * context.
     * @param other The PlainDirectoryNode.
     * @param store The AVMStore we should belong to.
     * @param lPath The Lookup object.
     */
    @SuppressWarnings("unchecked")
    public LayeredDirectoryNodeImpl(PlainDirectoryNode other,
                                    AVMStore store,
                                    Lookup lPath,
                                    boolean copyContents)
    {
        super(store.getAVMRepository().issueID(), store);
        fIndirection = null;
        fPrimaryIndirection = false;
        fIndirectionVersion = -1;
        fLayerID = -1;
        fOpacity = false;
        AVMDAOs.Instance().fAVMNodeDAO.save(this);
        if (copyContents)
        {
            for (ChildEntry child : AVMDAOs.Instance().fChildEntryDAO.getByParent(other))
            {
                ChildKey key = new ChildKey(this, child.getKey().getName());
                ChildEntryImpl newChild = new ChildEntryImpl(key,
                                                             child.getChild());
                AVMDAOs.Instance().fChildEntryDAO.save(newChild);
            }
        }
        setVersionID(other.getVersionID() + 1);
        AVMDAOs.Instance().fAVMNodeDAO.flush();
        copyProperties(other);
        copyAspects(other);
        copyACLs(other);
    }

    /**
     * Create a new layered directory based on a directory we are being named from
     * that is in not in the layer of the source lookup.
     * @param dir The directory
     * @param store The store
     * @param srcLookup The source lookup.
     * @param name The name of the target.
     */
    public LayeredDirectoryNodeImpl(DirectoryNode dir,
                                    AVMStore store,
                                    Lookup srcLookup,
                                    String name)
    {
        super(store.getAVMRepository().issueID(), store);
        fIndirection = srcLookup.getIndirectionPath() + "/" + name;
        fPrimaryIndirection = true;
        fIndirectionVersion = -1;
        fLayerID = -1;
        fOpacity = false;
        setVersionID(dir.getVersionID() + 1);
        AVMDAOs.Instance().fAVMNodeDAO.save(this);
        Map<String, AVMNode> children = dir.getListing(srcLookup, true);
        for (Map.Entry<String, AVMNode> child : children.entrySet())
        {
            ChildKey key = new ChildKey(this, child.getKey());
            ChildEntry entry = new ChildEntryImpl(key, child.getValue());
            AVMDAOs.Instance().fChildEntryDAO.save(entry);
        }
        AVMDAOs.Instance().fAVMNodeDAO.flush();
        copyProperties(dir);
        copyAspects(dir);
        copyACLs(dir);
    }

    /**
     * Is this a primary indirection node.
     * @return Whether this is a primary indirection.
     */
    public boolean getPrimaryIndirection()
    {
        return fPrimaryIndirection;
    }

    /**
     * Set the primary indirection state of this.
     * @param has Whether this is a primary indirection node.
     */
    public void setPrimaryIndirection(boolean has)
    {
        fPrimaryIndirection = has;
    }

    /**
     * Get the indirection path.
     * @return The indirection path.
     */
    public String getIndirection()
    {
        return fIndirection;
    }

    /**
     * Get the underlying path in the Lookup's context.
     * @param lPath The Lookup.
     * @return The underlying path.
     */
    public String getUnderlying(Lookup lPath)
    {
        if (fPrimaryIndirection)
        {
            return fIndirection;
        }
        return lPath.getCurrentIndirection();
    }

    /**
     * Get the underlying version in the lookup path context.
     * @param lPath The Lookup.
     * @return The effective underlying version.
     */
    public int getUnderlyingVersion(Lookup lPath)
    {
        if (lPath.getVersion() == -1)
        {
            return -1;
        }
        if (fPrimaryIndirection)
        {
            return fIndirectionVersion;
        }
        return lPath.getCurrentIndirectionVersion();
    }

    /**
     * Get the layer id.
     * @return The layer id.
     */
    public long getLayerID()
    {
        return fLayerID;
    }

    /**
     * Set the layer id.
     * @param id The id to set.
     */
    public void setLayerID(long id)
    {
        fLayerID = id;
    }

    /**
     * Copy on write logic.
     * @param lPath
     * @return The copy or null.
     */
    public AVMNode copy(Lookup lPath)
    {
        // Capture the store.
        AVMStore store = lPath.getAVMStore();
        LayeredDirectoryNodeImpl newMe = null;
        if (!lPath.isInThisLayer())
        {
            // This means that this is being seen indirectly through the topmost
            // layer.  The following creates a node that will inherit its
            // indirection from its parent.
            newMe = new LayeredDirectoryNodeImpl((String)null,
                                                 store, this);
            newMe.setPrimaryIndirection(false);
            newMe.setLayerID(lPath.getTopLayer().getLayerID());
        }
        else
        {
            // A simple copy is made.
            newMe = new LayeredDirectoryNodeImpl(this,
                                                 store,
                                                 lPath,
                                                 false);
            newMe.setLayerID(getLayerID());
        }
        newMe.setAncestor(this);
        return newMe;
    }

    /**
     * Insert a child node without COW.
     * @param name The name to give the child.
     */
    public void putChild(String name, AVMNode node)
    {
        if (DEBUG)
        {
            checkReadOnly();
        }
        ChildKey key = new ChildKey(this, name);
        ChildEntry existing = AVMDAOs.Instance().fChildEntryDAO.get(key);
        if (existing != null)
        {
            AVMDAOs.Instance().fChildEntryDAO.delete(existing);
        }
        ChildEntry entry = new ChildEntryImpl(key, node);
        AVMDAOs.Instance().fAVMNodeDAO.flush();
        AVMDAOs.Instance().fChildEntryDAO.save(entry);
    }


    /**
     * Does this node directly contain the indicated node.
     * @param node The node we are checking.
     * @return Whether node is directly contained.
     */
    public boolean directlyContains(AVMNode node)
    {
        return AVMDAOs.Instance().fChildEntryDAO.getByParentChild(this, node) != null;
    }

    /**
     * Get a listing of the virtual contents of this directory.
     * @param lPath The Lookup.
     * @return A Map from names to nodes. This is a sorted Map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, AVMNode> getListing(Lookup lPath, boolean includeDeleted)
    {
        // Get the base listing from the thing we indirect to.
        Map<String, AVMNode> listing = new HashMap<String, AVMNode>();
        if (!fOpacity)
        {
            Lookup lookup = AVMRepository.GetInstance().lookupDirectory(getUnderlyingVersion(lPath), getUnderlying(lPath));
            if (lookup != null)
            {
                DirectoryNode dir = (DirectoryNode)lookup.getCurrentNode();
                Map<String, AVMNode> underListing = dir.getListing(lookup, includeDeleted);
                for (Map.Entry<String, AVMNode> entry : underListing.entrySet())
                {
                    if (entry.getValue().getType() == AVMNodeType.LAYERED_DIRECTORY ||
                        entry.getValue().getType() == AVMNodeType.PLAIN_DIRECTORY)
                    {
                        if (!AVMRepository.GetInstance().can(entry.getValue(), PermissionService.READ_CHILDREN))
                        {
                            continue;
                        }
                    }
                    listing.put(entry.getKey(), entry.getValue());
                }
            }
        }
        for (ChildEntry entry : AVMDAOs.Instance().fChildEntryDAO.getByParent(this))
        {
            if (entry.getChild().getType() == AVMNodeType.LAYERED_DIRECTORY ||
                entry.getChild().getType() == AVMNodeType.PLAIN_DIRECTORY)
            {
                if (!AVMRepository.GetInstance().can(entry.getChild(), PermissionService.READ_CHILDREN))
                {
                    continue;
                }
            }
            if (!includeDeleted && entry.getChild().getType() == AVMNodeType.DELETED_NODE)
            {
                listing.remove(entry.getKey().getName());
            }
            else
            {
                listing.put(entry.getKey().getName(), entry.getChild());
            }
        }
        return listing;
    }

    /**
     * Get a listing of the nodes directly contained by a directory.
     * @param lPath The Lookup to this directory.
     * @return A Map of names to nodes.
     */
    public Map<String, AVMNode> getListingDirect(Lookup lPath, boolean includeDeleted)
    {
        Map<String, AVMNode> listing = new HashMap<String, AVMNode>();
        for (ChildEntry entry : AVMDAOs.Instance().fChildEntryDAO.getByParent(this))
        {
            if (entry.getChild().getType() == AVMNodeType.LAYERED_DIRECTORY ||
                entry.getChild().getType() == AVMNodeType.PLAIN_DIRECTORY)
            {
                if (!AVMRepository.GetInstance().can(entry.getChild(), PermissionService.READ_CHILDREN))
                {
                    continue;
                }
            }
            if (includeDeleted || entry.getChild().getType() != AVMNodeType.DELETED_NODE)
            {
                listing.put(entry.getKey().getName(), entry.getChild());
            }
        }
        return listing;
    }

    /**
     * Get the direct contents of this directory.
     * @param dir The descriptor that describes us.
     * @param includeDeleted Whether to inlude deleted nodes.
     * @return A Map of Strings to descriptors.
     */
    public SortedMap<String, AVMNodeDescriptor> getListingDirect(AVMNodeDescriptor dir,
                                                                 boolean includeDeleted)
    {
        List<ChildEntry> children = AVMDAOs.Instance().fChildEntryDAO.getByParent(this);
        SortedMap<String, AVMNodeDescriptor> listing = new TreeMap<String, AVMNodeDescriptor>();
        for (ChildEntry child : children)
        {
            AVMNode childNode = child.getChild();
            if (childNode.getType() == AVMNodeType.LAYERED_DIRECTORY ||
                childNode.getType() == AVMNodeType.PLAIN_DIRECTORY)
            {
                if (!AVMRepository.GetInstance().can(childNode, PermissionService.READ_CHILDREN))
                {
                    continue;
                }
            }
            if (!includeDeleted && childNode.getType() == AVMNodeType.DELETED_NODE)
            {
                continue;
            }
            AVMNodeDescriptor childDesc =
                childNode.getDescriptor(dir.getPath(), child.getKey().getName(), dir.getIndirection(), dir.getIndirectionVersion());
            listing.put(child.getKey().getName(), childDesc);
        }
        return listing;
    }

    /**
     * Get a listing from a directory node descriptor.
     * @param dir The directory node descriptor.
     * @param includeDeleted Should DeletedNodes be shown.
     * @return A Map of names to node descriptors.
     */
    public SortedMap<String, AVMNodeDescriptor> getListing(AVMNodeDescriptor dir,
                                                           boolean includeDeleted)
    {
        if (dir.getPath() == null || dir.getIndirection() == null)
        {
            throw new AVMBadArgumentException("Illegal null argument.");
        }
        SortedMap<String, AVMNodeDescriptor> baseListing = new TreeMap<String, AVMNodeDescriptor>();
        // If we are not opaque, get the underlying base listing.
        if (!fOpacity)
        {
            Lookup lookup = AVMRepository.GetInstance().lookupDirectory(-1, dir.getIndirection());
            if (lookup != null)
            {
                DirectoryNode dirNode = (DirectoryNode)lookup.getCurrentNode();
                Map<String, AVMNode> listing = dirNode.getListing(lookup, includeDeleted);
                for (Map.Entry<String, AVMNode> entry : listing.entrySet())
                {
                    if (entry.getValue().getType() == AVMNodeType.LAYERED_DIRECTORY ||
                        entry.getValue().getType() == AVMNodeType.PLAIN_DIRECTORY)
                    {
                        if (!AVMRepository.GetInstance().can(entry.getValue(), PermissionService.READ_CHILDREN))
                        {
                            continue;
                        }
                    }
                    baseListing.put(entry.getKey(),
                                    entry.getValue().getDescriptor(dir.getPath(), entry.getKey(),
                                                                   lookup.getCurrentIndirection(),
                                                                   lookup.getCurrentIndirectionVersion()));
                }
            }
        }
        List<ChildEntry> children = AVMDAOs.Instance().fChildEntryDAO.getByParent(this);
        for (ChildEntry child : children)
        {
            if (child.getChild().getType() == AVMNodeType.LAYERED_DIRECTORY ||
                child.getChild().getType() == AVMNodeType.PLAIN_DIRECTORY)
            {
                if (!AVMRepository.GetInstance().can(child.getChild(), PermissionService.READ_CHILDREN))
                {
                    continue;
                }
            }
            if (!includeDeleted && child.getChild().getType() == AVMNodeType.DELETED_NODE)
            {
                baseListing.remove(child.getKey().getName());
            }
            else
            {
                baseListing.put(child.getKey().getName(),
                        child.getChild().getDescriptor(dir.getPath(),
                                child.getKey().getName(),
                                dir.getIndirection(),
                                dir.getIndirectionVersion()));
            }
        }
        return baseListing;
    }

    /**
     * Get the names of nodes deleted in this directory.
     * @return A List of names.
     */
    public List<String> getDeletedNames()
    {
        List<ChildEntry> children = AVMDAOs.Instance().fChildEntryDAO.getByParent(this);
        List<String> listing = new ArrayList<String>();
        for (ChildEntry entry : children)
        {
            if (entry.getChild().getType() == AVMNodeType.DELETED_NODE)
            {
                listing.add(entry.getKey().getName());
            }
        }
        return listing;
    }

    /**
     * Lookup a child by name.
     * @param lPath The Lookup.
     * @param name The name we are looking.
     * @param version The version in which we are looking.
     * @param write Whether this lookup is occurring in a write context.
     * @return The child or null if not found.
     */
    @SuppressWarnings("unchecked")
    public Pair<AVMNode, Boolean> lookupChild(Lookup lPath, String name, boolean includeDeleted)
    {
        ChildKey key = new ChildKey(this, name);
        ChildEntry entry = AVMDAOs.Instance().fChildEntryDAO.get(key);
        if (entry != null)
        {
            if (!includeDeleted && entry.getChild().getType() == AVMNodeType.DELETED_NODE)
            {
                return null;
            }
            Pair<AVMNode, Boolean> result = new Pair<AVMNode, Boolean>(AVMNodeUnwrapper.Unwrap(entry.getChild()), true);
            return result;
        }
        // Don't check our underlying directory if we are opaque.
        if (fOpacity)
        {
            return null;
        }
        // Not here so check our indirection.
        Lookup lookup = AVMRepository.GetInstance().lookupDirectory(getUnderlyingVersion(lPath), getUnderlying(lPath));
        if (lookup != null)
        {
            DirectoryNode dir = (DirectoryNode)lookup.getCurrentNode();
            Pair<AVMNode, Boolean> retVal = dir.lookupChild(lookup, name, includeDeleted);
            if (retVal != null)
            {
                retVal.setSecond(false);
            }
            lPath.setFinalStore(lookup.getFinalStore());
            return retVal;
        }
        else
        {
            return null;
        }
    }

    /**
     * Lookup a child using a node descriptor as context.
     * @param mine The node descriptor for this,
     * @param name The name to lookup,
     * @return The node descriptor.
     */
    public AVMNodeDescriptor lookupChild(AVMNodeDescriptor mine, String name, boolean includeDeleted)
    {
        if (mine.getPath() == null || mine.getIndirection() == null)
        {
            throw new AVMBadArgumentException("Illegal null argument.");
        }
        ChildKey key = new ChildKey(this, name);
        ChildEntry entry = AVMDAOs.Instance().fChildEntryDAO.get(key);
        if (entry != null)
        {
            if (!includeDeleted && entry.getChild().getType() == AVMNodeType.DELETED_NODE)
            {
                return null;
            }
            AVMNodeDescriptor desc = entry.getChild().getDescriptor(mine.getPath(),
                                                                    name,
                                                                    mine.getIndirection(),
                                                                    mine.getIndirectionVersion());
            return desc;
        }
        // If we are opaque don't check underneath.
        if (fOpacity)
        {
            return null;
        }
        Lookup lookup = AVMRepository.GetInstance().lookupDirectory(mine.getIndirectionVersion(), mine.getIndirection());
        if (lookup != null)
        {
            DirectoryNode dir = (DirectoryNode)lookup.getCurrentNode();
            Pair<AVMNode, Boolean> child = dir.lookupChild(lookup, name, includeDeleted);
            if (child == null)
            {
                return null;
            }
            AVMNodeDescriptor desc = child.getFirst().getDescriptor(lookup);
            return desc;
        }
        else
        {
            return null;
        }
    }

    /**
     * Directly remove a child. Do not COW. Do not pass go etc.
     * @param lPath The lookup that arrived at this.
     * @param name The name of the child to remove.
     */
    @SuppressWarnings("unchecked")
    public void removeChild(Lookup lPath, String name)
    {
        if (DEBUG)
        {
            checkReadOnly();
        }
        ChildKey key = new ChildKey(this, name);
        ChildEntry entry = AVMDAOs.Instance().fChildEntryDAO.get(key);
        AVMNode child = null;
        boolean indirect = false;
        if (entry != null)
        {
            child = entry.getChild();
            if (child.getType() == AVMNodeType.DELETED_NODE)
            {
                return;
            }
            AVMDAOs.Instance().fChildEntryDAO.delete(entry);
        }
        else
        {
            Pair<AVMNode, Boolean> temp = lookupChild(lPath, name, false);
            if (temp == null)
            {
                child = null;
            }
            else
            {
                child = temp.getFirst();
            }
            indirect = true;
        }
        if (child != null && (indirect || child.getStoreNew() == null || child.getAncestor() != null))
        {
            DeletedNodeImpl ghost = new DeletedNodeImpl(lPath.getAVMStore().getAVMRepository().issueID(),
                    lPath.getAVMStore());
            AVMDAOs.Instance().fAVMNodeDAO.save(ghost);
            AVMDAOs.Instance().fAVMNodeDAO.flush();
            ghost.setAncestor(child);
            ghost.setDeletedType(child.getType());
            this.putChild(name, ghost);
        }
        else
        {
            AVMDAOs.Instance().fAVMNodeDAO.flush();
        }
    }

    /**
     * Get the type of this node.
     * @return The type of this node.
     */
    public int getType()
    {
        return AVMNodeType.LAYERED_DIRECTORY;
    }

    /**
     * For diagnostics. Get a String representation.
     * @param lPath The Lookup.
     * @return A String representation.
     */
    public String toString(Lookup lPath)
    {
        return "[LD:" + getId() + ":" + getUnderlying(lPath) + "]";
    }

    /**
     * Set the primary indirection. No COW.
     * @param path The indirection path.
     */
    public void rawSetPrimary(String path)
    {
        if (DEBUG)
        {
            checkReadOnly();
        }
        fIndirection = path;
        fPrimaryIndirection = true;
    }

    /**
     * Make this node become a primary indirection.  COW.
     * @param lPath The Lookup.
     */
    public void turnPrimary(Lookup lPath)
    {
        if (DEBUG)
        {
            checkReadOnly();
        }
        String path = lPath.getCurrentIndirection();
        rawSetPrimary(path);
    }

    /**
     * Make this point at a new target.
     * @param lPath The Lookup.
     */
    public void retarget(Lookup lPath, String target)
    {
        if (DEBUG)
        {
            checkReadOnly();
        }
        rawSetPrimary(target);
    }

    /**
     * Let anything behind name in this become visible.
     * @param lPath The Lookup.
     * @param name The name to uncover.
     */
    public void uncover(Lookup lPath, String name)
    {
        if (DEBUG)
        {
            checkReadOnly();
        }
        ChildKey key = new ChildKey(this, name);
        ChildEntry entry = AVMDAOs.Instance().fChildEntryDAO.get(key);
        if (entry.getChild().getType() != AVMNodeType.DELETED_NODE)
        {
            throw new AVMException("One can only uncover deleted nodes.");
        }
        if (entry != null)
        {
            AVMDAOs.Instance().fChildEntryDAO.delete(entry);
        }
    }

    /**
     * Get the descriptor for this node.
     * @param lPath The Lookup.
     * @return A descriptor.
     */
    public AVMNodeDescriptor getDescriptor(Lookup lPath, String name)
    {
        BasicAttributes attrs = getBasicAttributes();
        String path = lPath.getRepresentedPath();
        path = AVMNodeConverter.ExtendAVMPath(path, name);
        String indirect = null;
        int indirectionVersion = -1;
        if (fPrimaryIndirection)
        {
            indirect = fIndirection;
            indirectionVersion = fIndirectionVersion;
        }
        else
        {
            indirect = AVMNodeConverter.ExtendAVMPath(lPath.getCurrentIndirection(), name);
            indirectionVersion = lPath.getCurrentIndirectionVersion();
        }
        return new AVMNodeDescriptor(path,
                                     name,
                                     AVMNodeType.LAYERED_DIRECTORY,
                                     attrs.getCreator(),
                                     attrs.getOwner(),
                                     attrs.getLastModifier(),
                                     attrs.getCreateDate(),
                                     attrs.getModDate(),
                                     attrs.getAccessDate(),
                                     getId(),
                                     getGuid(),
                                     getVersionID(),
                                     indirect,
                                     indirectionVersion,
                                     fPrimaryIndirection,
                                     fLayerID,
                                     fOpacity,
                                     -1,
                                     -1);
    }

    /**
     * Get the descriptor for this node.
     * @param lPath The Lookup.
     * @return A descriptor.
     */
    public AVMNodeDescriptor getDescriptor(Lookup lPath)
    {
        BasicAttributes attrs = getBasicAttributes();
        String path = lPath.getRepresentedPath();
        String name = path.substring(path.lastIndexOf("/") + 1);
        return new AVMNodeDescriptor(path,
                                     name,
                                     AVMNodeType.LAYERED_DIRECTORY,
                                     attrs.getCreator(),
                                     attrs.getOwner(),
                                     attrs.getLastModifier(),
                                     attrs.getCreateDate(),
                                     attrs.getModDate(),
                                     attrs.getAccessDate(),
                                     getId(),
                                     getGuid(),
                                     getVersionID(),
                                     getUnderlying(lPath),
                                     getUnderlyingVersion(lPath),
                                     fPrimaryIndirection,
                                     fLayerID,
                                     fOpacity,
                                     -1,
                                     -1);
    }

    /**
     * Get a descriptor for this.
     * @param parentPath The parent path.
     * @param name The name this was looked up with.
     * @param parentIndirection The indirection of the parent.
     * @return The descriptor.
     */
    public AVMNodeDescriptor getDescriptor(String parentPath, String name, String parentIndirection, int parentIndirectionVersion)
    {
        BasicAttributes attrs = getBasicAttributes();
        String path = parentPath.endsWith("/") ? parentPath + name : parentPath + "/" + name;
        String indirection = null;
        int indirectionVersion = -1;
        if (fPrimaryIndirection)
        {
            indirection = fIndirection;
            indirectionVersion = fIndirectionVersion;
        }
        else
        {
            indirection = parentIndirection.endsWith("/") ? parentIndirection + name :
                parentIndirection + "/" + name;
            indirectionVersion = parentIndirectionVersion;
        }
        return new AVMNodeDescriptor(path,
                                     name,
                                     AVMNodeType.LAYERED_DIRECTORY,
                                     attrs.getCreator(),
                                     attrs.getOwner(),
                                     attrs.getLastModifier(),
                                     attrs.getCreateDate(),
                                     attrs.getModDate(),
                                     attrs.getAccessDate(),
                                     getId(),
                                     getGuid(),
                                     getVersionID(),
                                     indirection,
                                     indirectionVersion,
                                     fPrimaryIndirection,
                                     fLayerID,
                                     fOpacity,
                                     -1,
                                     -1);
    }

    /**
     * Set the indirection.
     * @param indirection
     */
    public void setIndirection(String indirection)
    {
        fIndirection = indirection;
    }

    /**
     * Does nothing because LayeredDirectoryNodes can't be roots.
     * @param isRoot
     */
    public void setIsRoot(boolean isRoot)
    {
    }

    /**
     * Get the opacity of this.
     * @return The opacity.
     */
    public boolean getOpacity()
    {
        return fOpacity;
    }

    /**
     * Set the opacity of this, ie, whether it blocks things normally
     * seen through its indirection.
     * @param opacity
     */
    public void setOpacity(boolean opacity)
    {
        fOpacity = opacity;
    }

    /**
     * Link a node with the given id into this directory.
     * @param lPath The Lookup for this.
     * @param name The name to give the node.
     * @param toLink The node to link in.
     */
    public void link(Lookup lPath, String name, AVMNodeDescriptor toLink)
    {
        if (DEBUG)
        {
            checkReadOnly();
        }
        AVMNode node = AVMDAOs.Instance().fAVMNodeDAO.getByID(toLink.getId());
        if (node == null)
        {
            throw new AVMNotFoundException("Not Found: " + toLink.getId());
        }
        if (node.getType() == AVMNodeType.LAYERED_DIRECTORY &&
            !((LayeredDirectoryNode)node).getPrimaryIndirection())
        {
            throw new AVMBadArgumentException("Non primary layered directories cannot be linked.");
        }
        // Look for an existing child of that name.
        Pair<AVMNode, Boolean> temp = lookupChild(lPath, name, true);
        AVMNode existing = (temp == null) ? null : temp.getFirst();
        ChildKey key = new ChildKey(this, name);
        if (existing != null)
        {
            if (existing.getType() != AVMNodeType.DELETED_NODE)
            {
                // If the existing child is not a DELETED_NODE it's an error.
                throw new AVMExistsException(name + " exists.");
            }
            // Only if the existing DELETED_NODE child exists directly in this
            // directory do we delete it.
            if (directlyContains(existing))
            {
                ChildEntry entry = AVMDAOs.Instance().fChildEntryDAO.get(key);
                AVMDAOs.Instance().fChildEntryDAO.delete(entry);
            }
        }
        // Make the new ChildEntry and save.
        ChildEntry newChild = new ChildEntryImpl(key, node);
        AVMDAOs.Instance().fChildEntryDAO.save(newChild);
    }

    /**
     * Remove name without leaving behind a deleted node.
     * @param name The name of the child to flatten.
     */
    public void flatten(String name)
    {
        if (DEBUG)
        {
            checkReadOnly();
        }
        ChildKey key = new ChildKey(this, name);
        ChildEntry entry = AVMDAOs.Instance().fChildEntryDAO.get(key);
        if (entry != null)
        {
            AVMDAOs.Instance().fChildEntryDAO.delete(entry);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.LayeredDirectoryNode#setIndirectionVersion(int)
     */
    public void setIndirectionVersion(Integer version)
    {
        if (version == null)
        {
            fIndirectionVersion = -1;
        }
        else
        {
            fIndirectionVersion = version;
        }
    }

    /**
     * Get the indirection version.
     * @return The indirection version.
     */
    public Integer getIndirectionVersion()
    {
        return fIndirectionVersion;
    }
}
