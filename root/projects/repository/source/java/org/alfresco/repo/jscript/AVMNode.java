/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.jscript;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.model.WCMModel;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.locking.AVMLock;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.surf.util.Pair;
import org.springframework.extensions.surf.util.ParameterCheck;
import org.mozilla.javascript.Scriptable;

/**
 * Represents a AVM specific node in the Script context. Provides specific implementations
 * of AVM specific methods such as copy, move, rename etc. 
 * 
 * @author Kevin Roast
 */
public class AVMNode extends ScriptNode
{
    private String path;
    private int version;
    private boolean deleted;
    private AVMNodeDescriptor avmRef;
    private QName type;
    
    
    public AVMNode(NodeRef nodeRef, ServiceRegistry services)
    {
        this(nodeRef, services, null);
    }

    public AVMNode(NodeRef nodeRef, ServiceRegistry services, Scriptable scope)
    {
        super(nodeRef, services, scope);
        
        Pair<Integer, String> versionPath = AVMNodeConverter.ToAVMVersionPath(nodeRef);
        init(versionPath.getSecond(), versionPath.getFirst());
    }
    
    public AVMNode(String path, int version, ServiceRegistry services, Scriptable scope)
    {
        super(AVMNodeConverter.ToNodeRef(version, path), services, scope);
        
        init(path, version);
    }
    
    private void init(String path, int version)
    {
        this.path = path;
        this.version = version;
        AVMNodeDescriptor descriptor = this.services.getAVMService().lookup(version, path, true);
        if (descriptor == null)
        {
            throw new IllegalArgumentException("Invalid node specified: " + this.nodeRef.toString());
        }
        this.avmRef = descriptor;
        this.deleted = descriptor.isDeleted();
    }
    
    /**
     * Factory methods
     */
    @Override
    public ScriptNode newInstance(NodeRef nodeRef, ServiceRegistry services, Scriptable scope)
    {
        return new AVMNode(nodeRef, services, scope);
    }
    
    public ScriptNode newInstance(String path, int version, ServiceRegistry services, Scriptable scope)
    {
        return new AVMNode(AVMNodeConverter.ToNodeRef(version, path), services, scope);
    }
    
    // TODO: changing the 'name' property (either directly using .name or with .properties.name)
    //       invalidates the path and the base noderef instance!
    //       AVMService has a specific rename method - use this and block name property changes?
    
    /**
     * @return the full AVM Path to this node
     */
    public String getPath()
    {
        return this.path;
    }
    
    public int getVersion()
    {
        return this.version;
    }
    
    /**
     * @return AVM path to the parent node
     */
    public String getParentPath()
    {
        return AVMNodeConverter.SplitBase(this.path)[0];
    }
    
    /**
     * @return QName type of this node
     */
    public String getType()
    {
        if (this.type == null)
        {
            if (this.deleted == false)
            {
                this.type = this.services.getNodeService().getType(this.nodeRef);
            }
            else
            {
                this.type = avmRef.isDeletedDirectory() ? WCMModel.TYPE_AVM_FOLDER : WCMModel.TYPE_AVM_CONTENT;
            }
        }
        
        return type.toString();
    }
    
    public boolean isDirectory()
    {
        return this.avmRef.isDirectory() || this.avmRef.isDeletedDirectory();
    }
    
    public boolean isFile()
    {
        return this.avmRef.isFile() || this.avmRef.isDeletedFile();
    }
    
    /**
     * @return Helper to return the 'name' property for the node
     */
    @Override
    public String getName()
    {
        return this.avmRef.getName();
    }
    
    /**
     * @return true if the node is currently locked
     */
    public boolean getIsLocked()
    {
        AVMLock lock = this.services.getAVMLockingService().getLock(
                getWebProject(), path.substring(path.indexOf("/")));
        return (lock != null);
    }
    
    /**
     * @return true if this node is locked and the current user is the lock owner
     */
    public boolean isLockOwner()
    {
        boolean lockOwner = false;
        
        AVMLock lock = this.services.getAVMLockingService().getLock(
                getWebProject(), path.substring(path.indexOf("/")));
        if (lock != null)
        {
            List<String> lockUsers = lock.getOwners();
            lockOwner = (lockUsers.contains(this.services.getAuthenticationService().getCurrentUserName()));
        }
        
        return lockOwner;
    }

    /**
     * @return true if this user can perform operations on the node when locked.
     *         This is true if the item is either unlocked, or locked and the current user is the lock owner,
     *         or locked and the current user has Content Manager role in the associated web project.
     */
    public boolean hasLockAccess()
    {
        return this.services.getAVMLockingService().hasAccess(
                getWebProject(), path, this.services.getAuthenticationService().getCurrentUserName());
    }
    
    /**
     * Copy this Node into a new parent destination.
     * 
     * @param destination     Parent node for the copy
     * 
     * @return the copy of this node
     */
    @Override
    public ScriptNode copy(ScriptNode destination)
    {
        ParameterCheck.mandatory("Destination Node", destination);
        
        return getCrossRepositoryCopyHelper().copy(this, destination, getName());
    }
    
    /**
     * Copy this Node into a new parent destination.
     * 
     * @param destination     Parent path for the copy
     * 
     * @return the copy of this node
     */
    public ScriptNode copy(String destination)
    {
        ParameterCheck.mandatoryString("Destination Path", destination);
        
        this.services.getAVMService().copy(this.version, this.path, destination, getName());
        return newInstance(
                AVMNodeConverter.ToNodeRef(-1, AVMNodeConverter.ExtendAVMPath(destination, getName())),
                this.services, this.scope);
    }
    
    /**
     * Move this Node to a new parent destination node.
     * 
     * @param destination   Node
     * 
     * @return true on successful move, false on failure to move.
     */
    @Override
    public boolean move(ScriptNode destination)
    {
        ParameterCheck.mandatory("Destination Node", destination);
        
        boolean success = false;
        
        if (destination instanceof AVMNode)
        {
            success = move(((AVMNode)destination).getPath());
        }
        
        return success;
    }
    
    /**
     * Move this Node to a new parent destination path.
     * 
     * @param destination   Path
     * 
     * @return true on successful move, false on failure to move.
     */
    public boolean move(String destination)
    {
        ParameterCheck.mandatoryString("Destination Path", destination);
        
        boolean success = false;
        
        if (destination != null && destination.length() != 0)
        {
            AVMNode parent = (AVMNode)this.getParent();
            this.services.getAVMService().rename(
                    parent.getPath(), getName(), destination, getName());
            
            reset(AVMNodeConverter.ExtendAVMPath(destination, getName()));
            
            success = true;
        }
        
        return success;
    }
    
    /**
     * Rename this node to the specified name
     * 
     * @param name      New name for the node
     * 
     * @return true on success, false otherwise
     */
    public boolean rename(String name)
    {
        ParameterCheck.mandatoryString("Destination name", name);
        
        boolean success = false;
        
        if (name != null && name.length() != 0)
        {
            String parentPath = ((AVMNode)this.getParent()).getPath();
            this.services.getAVMService().rename(
                    parentPath, getName(), parentPath, name);
            
            reset(AVMNodeConverter.ExtendAVMPath(parentPath, name));
            
            success = true;
        }
        
        return success;
    }
    
    /**
     * @return The list of aspects applied to this node
     */
    @Override
    public Set<QName> getAspectsSet()
    {
        if (this.aspects == null)
        {
            this.aspects = this.services.getAVMService().getAspects(this.version, this.path);
        }
        
        return this.aspects;
    }
    
    /**
     * Reset the Node cached state
     */
    private void reset(String path)
    {
        super.reset();
        this.path = path;
        this.nodeRef = AVMNodeConverter.ToNodeRef(this.version, path);
        this.id = nodeRef.getId();
        AVMNodeDescriptor descriptor = this.services.getAVMService().lookup(this.version, path, true);
        if (descriptor == null)
        {
            throw new IllegalArgumentException("Invalid node specified: " + nodeRef.toString());
        }
        this.avmRef = descriptor;
        this.deleted = descriptor.isDeleted();
    }
    
    /**
     * @return the WebProject identifier for the current path
     */
    private String getWebProject()
    {
        String webProject = this.path.substring(0, this.path.indexOf(':'));
        int index = webProject.indexOf("--");
        if (index != -1)
        {
            webProject = webProject.substring(0, index);
        }
        return webProject;
    }

    @Override
    public String toString()
    {
        if (this.services.getAVMService().lookup(version, this.path) != null)
        {
            return "AVM Path: " + getPath() + 
                   "\nNode Type: " + getType() + 
                   "\nNode Properties: " + this.getProperties().size() + 
                   "\nNode Aspects: " + this.getAspectsSet().toString();
        }
        else
        {
            return "Node no longer exists: " + nodeRef;
        }
    }
}
