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
package org.alfresco.repo.jscript;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.config.JNDIConstants;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMStoreDescriptor;
import org.alfresco.util.ParameterCheck;
import org.alfresco.wcm.sandbox.SandboxService;
import org.alfresco.wcm.util.WCMUtil;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Helper to access AVM nodes from a script context.
 *  
 * @author Kevin Roast
 */
public final class AVM extends BaseScopableProcessorExtension
{
    /** Repository Service Registry */
    private ServiceRegistry services;

    /**
     * Set the service registry
     * 
     * @param serviceRegistry the service registry
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        this.services = serviceRegistry;
    }

    /**
     * @return a array of all AVM stores in the system
     */
    public Scriptable getStores()
    {
        List<AVMStoreDescriptor> stores = this.services.getAVMService().getStores();
        Object[] results = new Object[stores.size()];
        int i=0;
        for (AVMStoreDescriptor store : stores)
        {
            results[i++] = new AVMScriptStore(this.services, store, getScope());
        }
        return Context.getCurrentContext().newArray(getScope(), results);
    }
    
    /**
     * Return an AVM store object for the specified store name
     * 
     * @param store         Store name to lookup
     * 
     * @return the AVM store object for the specified store or null if not found
     */
    public AVMScriptStore lookupStore(String store)
    {
        ParameterCheck.mandatoryString("Store", store);
        AVMScriptStore avmStore = null;
        AVMStoreDescriptor descriptor = this.services.getAVMService().getStore(store);
        if (descriptor != null)
        {
            avmStore = new AVMScriptStore(this.services, descriptor, getScope());
        }
        return avmStore;
    }
    
    /**
     * Return an AVM Node representing the public store root folder.
     * 
     * @param store     Store name to lookup root folder for
     * 
     * @return AVM Node representing the public store root folder, or null if not found.
     */
    public AVMNode lookupStoreRoot(String store)
    {
        AVMNode rootNode = null;
        if (store != null && store.length() != 0)
        {
            String rootPath = store + ':' + getWebappsFolderPath();
            AVMNodeDescriptor nodeDesc = this.services.getAVMService().lookup(-1, rootPath);
            if (nodeDesc != null)
            {
                rootNode = new AVMNode(AVMNodeConverter.ToNodeRef(-1, rootPath), this.services, getScope());
            }
        }
        return rootNode;
    }

    /**
     * Return an AVM Node for the fully qualified path.
     * 
     * @param path   Fully qualified path to node to lookup
     * 
     * @return AVM Node for the fully qualified path, or null if not found.
     */
    public AVMNode lookupNode(String path)
    {
        AVMNode node = null;
        if (path != null && path.length() != 0)
        {
            AVMNodeDescriptor nodeDesc = this.services.getAVMService().lookup(-1, path);
            if (nodeDesc != null)
            {
                node = new AVMNode(AVMNodeConverter.ToNodeRef(-1, path), this.services, getScope());
            }
        }
        return node;
    }
    
    /**
     * Return the list of modified items for the specified user sandbox against staging store id
     * for a specific webapp.
     * 
     * @param storeId      Root Store ID
     * @param username     Username to get modified items for
     * @param webapp       Webapp name to filter by
     * 
     * @return List of AVMNode objects representing the modified items
     */
    public List<AVMNode> getModifiedItems(String storeId, String username, String webapp)
    {
        ParameterCheck.mandatoryString("Store ID", storeId);
        ParameterCheck.mandatoryString("Username", username);
        ParameterCheck.mandatoryString("Webapp", webapp);

        SandboxService sbService = this.services.getSandboxService();
        
        // get modified items - not including deleted
        List<AVMNodeDescriptor> nodes = sbService.listChangedWebApp(storeId, webapp, false);
        
        List<AVMNode> items = new ArrayList<AVMNode>(nodes.size());
        
        for (AVMNodeDescriptor node : nodes)
        {
            // convert each diff/node record into an AVM Node wrapper
            items.add(new AVMNode(node.getPath(), -1, this.services, getScope()));
        }

        return items;
    }

    /**
     * @param storeId   Store ID to build staging store name for
     * 
     * @return the Staging Store name for the given store ID
     */
    public static String stagingStore(String storeId)
    {
        return WCMUtil.buildStagingStoreName(storeId);
    }

    /**
     * @param storeId   Store ID to build sandbox store name for
     * @param username  Username of the sandbox user
     * 
     * @return the Sandbox Store name for the given store ID and username
     */
    public static String userSandboxStore(String storeId, String username)
    {
        return WCMUtil.buildUserMainStoreName(storeId, username);
    }

    /**
     * @param storeId   Store ID to build preview URL for
     * 
     * @return the preview URL to the staging store for the specified store ID
     */
    public String websiteStagingUrl(String storeId)
    {
        return WCMUtil.buildStoreUrl(this.services.getAVMService(), storeId, getVServerDomain(), getVServerPort());
    }

    /**
     * @param storeId   Store ID to build preview URL for
     * @param username  Username to build sandbox preview URL for
     * 
     * @return the preview URL to the user sandbox for the specified store ID and username
     */
    public String websiteUserSandboxUrl(String storeId, String username)
    {
        ParameterCheck.mandatoryString("Store ID", storeId);
        ParameterCheck.mandatoryString("Username", username);
        return websiteStagingUrl(userSandboxStore(storeId, username));
    }

    /**
     * @param store     Store ID of the asset
     * @param assetPath Store relative path to the asset
     * 
     * @return the preview URL to the specified store asset
     */
    public String assetUrl(String store, String assetPath)
    {
        return WCMUtil.buildAssetUrl(assetPath, getVServerDomain(), getVServerPort(), WCMUtil.lookupStoreDNS(this.services.getAVMService(), store));
    }

    /**
     * @param avmPath   Fully qualified AVM path of the asset
     * 
     * @return the preview URL to the specified asset
     */
    public String assetUrl(String avmPath)
    {
        ParameterCheck.mandatoryString("AVM Path", avmPath);
        String[] s = avmPath.split(":");
        if (s.length != 2)
        {
            throw new IllegalArgumentException("Expected exactly one ':' in " + avmPath);
        }
        return assetUrl(s[0], s[1]);
    }

    /**
     * @return VServer Port
     */
    private String getVServerPort()
    {
        Integer port = this.services.getVirtServerRegistry().getVirtServerHttpPort();
        if (port == null)
        {
            port = JNDIConstants.DEFAULT_VSERVER_PORT;
        }
        return port.toString();
    }

    /**
     * @return VServer Domain
     */
    private String getVServerDomain()
    {
        String domain = this.services.getVirtServerRegistry().getVirtServerFQDN();
        if (domain == null)
        {
            domain = JNDIConstants.DEFAULT_VSERVER_IP;
        }
        return domain;
    }

    /**
     * @return the path to the webapps folder in a standard web store.
     */
    public static String getWebappsFolderPath()
    {
        return JNDIConstants.DIR_DEFAULT_WWW_APPBASE;
    }
}
