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
package org.alfresco.service.cmr.tagging;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

/**
 * Taggin Service Interface
 * 
 * @author Roy Wetherall
 */
public interface TaggingService
{
    /**
     * Indicates whether the tag already exists
     * 
     * @param storeRef      store reference
     * @param tag           tag name
     * @return boolean      true if the tag exists, false otherwise
     */
    boolean isTag(StoreRef storeRef, String tag);
    
    /**
     * Get all the tags currently available
     * 
     * @return List<String> list of tags
     */
    List<String> getTags(StoreRef storeRef);
    
    /** 
     * Get all the tags currently available that match the provided filter.
     * 
     * @param storeRef      store reference
     * @param filter        tag filter
     * @return List<String> list of tags
     */
    List<String> getTags(StoreRef storeRef, String filter);
    
    /**
     * Create a new tag
     * 
     * @param storeRef  store reference
     * @param tag       tag name
     */
    NodeRef createTag(StoreRef storeRef, String tag);
    
    /**
     * Delete an existing tag
     * 
     * @param storeRef  store reference
     * @param tag       tag name
     */
    void deleteTag(StoreRef storeRef, String tag);
    
    /**
     * Indicates whether a node has the specified tag or not.
     * 
     * @param nodeRef   node reference
     * @param tag       tag name
     * @return boolean  true if the node has the tag, false otherwise
     */
    boolean hasTag(NodeRef nodeRef, String tag);
    
    /**
     * Add a tag to a node.  Creating the tag if it does not already exist.
     * 
     * @param nodeRef   node reference
     * @param tag       tag name
     */
    void addTag(NodeRef nodeRef, String tag);

    /**
     * Gets the node reference for a given tag.
     * <p>
     * Returns null if tag is not present.
     * 
     * @param storeRef      store reference
     * @param tag           tag
     * @return NodeRef      tag node reference or null not exist
     */
    NodeRef getTagNodeRef(StoreRef storeRef, String tag);

    /**
     * Adds a list of tags to a node.
     * <p>
     * Tags are created if they do not exist.
     * 
     * @param nodeRef   node reference
     * @param tags      list of tags
     */
    void addTags(NodeRef nodeRef, List<String> tags);
    
    /**
     * Remove a tag from a node.
     * 
     * @param nodeRef   node reference
     * @param tag       tag name
     */
    void removeTag(NodeRef nodeRef, String tag);
    
    /**
     * Removes a list of tags from a node.
     * 
     * @param nodeRef   node reference
     * @param tags      list of tags
     */
    void removeTags(NodeRef nodeRef, List<String> tags);
    
    /**
     * Get all the tags on a node
     * 
     * @param nodeRef           node reference
     * @return List<String>     list of tags on the node
     */
    List<String> getTags(NodeRef nodeRef);
    
    /**
     * Sets the list of tags that are applied to a node, replaces any existing
     * tags with those provided.
     * 
     * @param nodeRef   node reference
     * @param tags      list of tags
     */
    void setTags(NodeRef nodeRef, List<String> tags);
    
    /**
     * Clears all tags from an already tagged node.
     * 
     * @param nodeRef   node reference
     */
    void clearTags(NodeRef nodeRef);
    
    /** 
     * Indicates whether the node reference is a tag scope
     * 
     * @param nodeRef   node reference
     * @return boolean  true if node is a tag scope, false otherwise
     */
    boolean isTagScope(NodeRef nodeRef);
    
    /**    
     * Adds a tag scope to the specified node
     * 
     * @param nodeRef   node reference
     */
    void addTagScope(NodeRef nodeRef);
    
    /**
     * Refreshes the tag count of the passed tag scope by recounting all the tags of the children
     * of the scope.
     *
     * @param nodeRef       tag scope node reference
     * @param async         indicates whether the tag scope refresh should happen asynchronously or not
     */
    void refreshTagScope(NodeRef nodeRef, boolean async);
    
    /**
     * Removes a tag scope from a specified node.
     * 
     * Note that any tag count information will be lost when the scope if removed.
     * 
     * @param nodeRef   node reference
     */
    void removeTagScope(NodeRef nodeRef);
    
    /**
     * Finds the 'nearest' tag scope for the specified node.
     * <p>
     * The 'nearest' tag scope is discovered by walking up the primary parent path
     * untill a tag scope is found or the root node is reached.
     * <p>
     * If no tag scope if found then a null value is returned.
     * 
     * @param nodeRef       node reference
     * @return              the 'nearest' tag scope or null if none found
     */
    TagScope findTagScope(NodeRef nodeRef);
    
    /**
     * Finds all the tag scopes for the specified node.
     * <p>
     * The resulting list of tag scopes is ordered with the 'nearest' at the bedining of the list.
     * <p>
     * If no tag scopes are found an empty list is returned.
     * 
     * @param nodeRef           node reference
     * @return List<TagScope>   list of tag scopes
     */
    List<TagScope> findAllTagScopes(NodeRef nodeRef);
    
    /**
     * Find all nodes that have been tagged with the specified tag.
     * 
     * @param  tag              tag name
     * @return List<NodeRef>    list of nodes tagged with specified tag, empty of none found
     */
    List<NodeRef> findTaggedNodes(StoreRef storeRef, String tag);
    
    /**
     * Find all nodes that have been tagged with the specified tag and reside within
     * the context of the node reference provided.
     * 
     * @param tag               tag name
     * @param nodeRef           node providing context for the search
     * @return List<NodeRef>    list of nodes tagged in the context specified, empty if none found
     */
    List<NodeRef> findTaggedNodes(StoreRef storeRef, String tag, NodeRef nodeRef);
}


