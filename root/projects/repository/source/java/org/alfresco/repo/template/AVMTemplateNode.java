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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.repo.template;

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.WCMModel;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.audit.AuditInfo;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.locking.AVMLock;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.TemplateImageResolver;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespacePrefixResolverProvider;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNameMap;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

import freemarker.ext.dom.NodeModel;

/**
 * AVM node class for use by a Template model.
 * <p>
 * The class exposes Node properties, children as dynamically populated maps and lists.
 * <p>
 * Various helper methods are provided to access common and useful node variables such
 * as the content url and type information. 
 * <p>
 * See {@link http://wiki.alfresco.com/wiki/Template_Guide}
 * 
 * @author Kevin Roast
 */
public class AVMTemplateNode extends BasePermissionsNode implements NamespacePrefixResolverProvider
{
    private static Log logger = LogFactory.getLog(AVMTemplateNode.class);
    
    private final static String NAMESPACE_BEGIN = "" + QName.NAMESPACE_BEGIN;
    
    /** Cached values */
    private NodeRef nodeRef;
    private String name;
    private QName type;
    private String path;
    private int version;
    private boolean deleted;
    private QNameMap<String, Serializable> properties;
    private boolean propsRetrieved = false;
    private AVMTemplateNode parent = null;
    private AVMNodeDescriptor avmRef;
    
    
    // ------------------------------------------------------------------------------
    // Construction 
    
    /**
     * Constructor
     * 
     * @param nodeRef       The NodeRef for the AVM node this wrapper represents
     * @param services      The ServiceRegistry the Node can use to access services
     * @param resolver      Image resolver to use to retrieve icons
     */
    public AVMTemplateNode(NodeRef nodeRef, ServiceRegistry services, TemplateImageResolver resolver)
    {
        if (nodeRef == null)
        {
            throw new IllegalArgumentException("NodeRef must be supplied.");
        }
      
        if (services == null)
        {
            throw new IllegalArgumentException("The ServiceRegistry must be supplied.");
        }
        
        this.nodeRef = nodeRef;
        Pair<Integer, String> pair = AVMNodeConverter.ToAVMVersionPath(nodeRef);
        this.services = services;
        this.imageResolver = resolver;
        init(pair.getFirst(), pair.getSecond(), null);
    }
    
    /**
     * Constructor
     * 
     * @param path          AVM path to the node
     * @param version       Version number for avm path
     * @param services      The ServiceRegistry the Node can use to access services
     * @param resolver      Image resolver to use to retrieve icons
     */
    public AVMTemplateNode(String path, int version, ServiceRegistry services, TemplateImageResolver resolver)
    {
        if (path == null)
        {
            throw new IllegalArgumentException("Path must be supplied.");
        }
        
        if (services == null)
        {
            throw new IllegalArgumentException("The ServiceRegistry must be supplied.");
        }
        
        this.nodeRef = AVMNodeConverter.ToNodeRef(version, path);
        this.services = services;
        this.imageResolver = resolver;
        init(version, path, null);
    }
    
    /**
     * Constructor
     * 
     * @param descriptor    AVMNodeDescriptior
     * @param services      
     * @param resolver
     */
    public AVMTemplateNode(AVMNodeDescriptor descriptor, ServiceRegistry services, TemplateImageResolver resolver)
    {
        if (descriptor == null)
        {
            throw new IllegalArgumentException("AVMNodeDescriptor must be supplied.");
        }
        
        if (services == null)
        {
            throw new IllegalArgumentException("The ServiceRegistry must be supplied.");
        }
        
        this.version = -1;
        this.path = descriptor.getPath();
        this.nodeRef = AVMNodeConverter.ToNodeRef(this.version, this.path);
        this.services = services;
        this.imageResolver = resolver;
        init(this.version, this.path, descriptor);
    }
    
    private void init(int version, String path, AVMNodeDescriptor descriptor)
    {
        this.version = version;
        this.path = path;
        this.properties = new QNameMap<String, Serializable>(this);
        if (descriptor == null)
        {
            descriptor = this.services.getAVMService().lookup(version, path, true);
            if (descriptor == null)
            {
                throw new IllegalArgumentException("Invalid node specified: " + nodeRef.toString());
            }
        }
        this.avmRef = descriptor;
        this.deleted = descriptor.isDeleted();
    }
    
    
    // ------------------------------------------------------------------------------
    // AVM Node API
    
    /**
     * @return ID for the AVM path - the path.
     */
    public String getId()
    {
        return this.path;
    }
    
    /**
     * @return the path for this AVM node.
     */
    public String getPath()
    {
        return this.path;
    }
    
    /**
     * @return the version part of the AVM path.
     */
    public int getVersion()
    {
        return this.version;
    }

    /**
     * @return file/folder name of the AVM path.
     */
    public String getName()
    {
        if (this.name == null)
        {
            this.name = AVMNodeConverter.SplitBase(this.path)[1];
        }
        return this.name;
    }
    
    /**
     * @return AVM path to the parent node
     */
    public String getParentPath()
    {
        return AVMNodeConverter.SplitBase(this.path)[0];
    }

    /**
     * @see org.alfresco.repo.template.TemplateNodeRef#getNodeRef()
     */
    public NodeRef getNodeRef()
    {
        return this.nodeRef;
    }
    
    /**
     * @see org.alfresco.repo.template.TemplateNodeRef#getType()
     */
    public QName getType()
    {
        if (this.type == null)
        {
            if (this.deleted == false)
            {
                this.type = this.services.getNodeService().getType(this.nodeRef);
            }
            else
            {
                this.type = this.avmRef.isDeletedDirectory() ? WCMModel.TYPE_AVM_FOLDER : WCMModel.TYPE_AVM_CONTENT;
            }
        }

        return type;
    }
    
    /**
     * @return true if the item is a deleted node, false otherwise
     */
    public boolean getIsDeleted()
    {
        return this.avmRef.isDeleted();
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
    public boolean getIsLockOwner()
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
    public boolean getHasLockAccess()
    {
        return this.services.getAVMLockingService().hasAccess(
                getWebProject(), path, this.services.getAuthenticationService().getCurrentUserName());
    }

    
    // ------------------------------------------------------------------------------
    // TemplateProperties API
    
    /**
     * @return the immediate parent in the node path
     */
    public TemplateProperties getParent()
    {
        if (this.parent == null)
        {
            this.parent = new AVMTemplateNode(this.getParentPath(), this.version, this.services, this.imageResolver);
        }
        return this.parent;
    }
    
    /**
     * @return true if this Node is a container (i.e. a folder)
     */
    @Override
    public boolean getIsContainer()
    {
        return this.avmRef.isDirectory() || this.avmRef.isDeletedDirectory();
    }
    
    /**
     * @return true if this Node is a document (i.e. a file)
     */
    @Override
    public boolean getIsDocument()
    {
        return this.avmRef.isFile() || this.avmRef.isDeletedFile();
    }
    
    /**
     * @see org.alfresco.repo.template.TemplateProperties#getChildren()
     */
    public List<TemplateProperties> getChildren()
    {
        if (this.children == null)
        {
            // use the NodeService so appropriate permission checks are performed
            List<ChildAssociationRef> childRefs = this.services.getNodeService().getChildAssocs(this.nodeRef);
            this.children = new ArrayList<TemplateProperties>(childRefs.size());
            for (ChildAssociationRef ref : childRefs)
            {
                // create our Node representation from the NodeRef
                AVMTemplateNode child = new AVMTemplateNode(ref.getChildRef(), this.services, this.imageResolver);
                this.children.add(child);
            }
        }
        
        return this.children;
    }

    /**
     * @see org.alfresco.repo.template.TemplateProperties#getProperties()
     */
    public Map<String, Serializable> getProperties()
    {
        if (!this.propsRetrieved)
        {
            if (!this.deleted)
            {
                Map<QName, PropertyValue> props = this.services.getAVMService().getNodeProperties(this.version, this.path);
                for (QName qname: props.keySet())
                {
                    Serializable propValue = props.get(qname).getValue(DataTypeDefinition.ANY);
                    if (propValue instanceof NodeRef)
                    {
                        // NodeRef object properties are converted to new TemplateNode objects
                        // so they can be used as objects within a template
                        NodeRef nodeRef = (NodeRef)propValue;
                        if (StoreRef.PROTOCOL_AVM.equals(nodeRef.getStoreRef().getProtocol()))
                        {
                            propValue = new AVMTemplateNode(nodeRef, this.services, this.imageResolver);
                        }
                        else
                        {
                            propValue = new TemplateNode(nodeRef, this.services, this.imageResolver);
                        }
                    }
                    else if (propValue instanceof ContentData)
                    {
                        // ContentData object properties are converted to TemplateContentData objects
                        // so the content and other properties of those objects can be accessed
                        propValue = new TemplateContentData((ContentData)propValue, qname);
                    }
                    this.properties.put(qname.toString(), propValue);
                }
            }
            
            // AVM node properties not available in usual getProperties() call
            this.properties.put("name", this.avmRef.getName());
            this.properties.put("created", new Date(this.avmRef.getCreateDate()));
            this.properties.put("modified", new Date(this.avmRef.getModDate()));
            this.properties.put("creator", this.avmRef.getCreator());
            this.properties.put("modifier", this.avmRef.getLastModifier());
            
            this.propsRetrieved = true;
        }
        
        return this.properties;
    }
    
    /**
     * @return The list of aspects applied to this node
     */
    @Override
    public Set<QName> getAspects()
    {
        if (this.aspects == null)
        {
            this.aspects = this.services.getAVMService().getAspects(this.version, this.path);
        }
        
        return this.aspects;
    }
    
    
    // ------------------------------------------------------------------------------
    // Audit API
    
    /**
     * @return a list of AuditInfo objects describing the Audit Trail for this node instance
     */
    public List<AuditInfo> getAuditTrail()
    {
        return this.services.getAuditService().getAuditTrail(this.nodeRef);
    }
    
    
    // ------------------------------------------------------------------------------
    // Node Helper API 
    
    /**
     * @return FreeMarker NodeModel for the XML content of this node, or null if no parsable XML found
     */
    public NodeModel getXmlNodeModel()
    {
        try
        {
            return NodeModel.parse(new InputSource(new StringReader(getContent())));
        }
        catch (Throwable err)
        {
            if (logger.isDebugEnabled())
                logger.debug(err.getMessage(), err);
            
            return null;
        }
    }
    
    /**
     * @return Display path to this node - the path built of 'cm:name' attribute values.
     */
    @Override
    public String getDisplayPath()
    {
        return this.path;
    }
    
    
    public NamespacePrefixResolver getNamespacePrefixResolver()
    {
        return this.services.getNamespaceService();
    }
    
    
    // ------------------------------------------------------------------------------
    // Private helpers
    
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
}
