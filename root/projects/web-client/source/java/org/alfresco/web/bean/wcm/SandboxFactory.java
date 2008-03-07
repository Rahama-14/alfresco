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
package org.alfresco.web.bean.wcm;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.config.JNDIConstants;
import org.alfresco.model.WCMAppModel;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.sandbox.SandboxConstants;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.DNSNameMangler;
import org.alfresco.util.GUID;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper factory to create AVM sandbox structures.
 * 
 * @author Kevin Roast
 */
public final class SandboxFactory
{
   private static Log logger = LogFactory.getLog(SandboxFactory.class);
   
   /**
    * Private constructor
    */
   private SandboxFactory()
   {
   }
   
   /**
    * Create the staging sandbox for the named store.
    * 
    * A staging sandbox is comprised of two stores, the first named 'storename-staging' with a
    * preview store named 'storename-preview' layered over the staging store.
    * 
    * Various store meta-data properties are set including:
    * Identifier for store-types: .sandbox.staging.main and .sandbox.staging.preview
    * Store-id: .sandbox-id.<guid> (unique across all stores in the sandbox)
    * DNS: .dns.<store> = <path-to-webapps-root>
    * Website Name: .website.name = website name
    * 
    * @param storeId             The store name to create the sandbox for.
    * @param webProjectNodeRef   The noderef for the webproject.
    * @param branchStoreId       The ID of the store to branch this staging store from.
    */
   public static SandboxInfo createStagingSandbox(String storeId, 
                                                  NodeRef webProjectNodeRef,
                                                  String branchStoreId)
   {
      ServiceRegistry services = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
      AVMService avmService = services.getAVMService();
      PermissionService permissionService = services.getPermissionService();
      
      // create the 'staging' store for the website
      String stagingStoreName = AVMUtil.buildStagingStoreName(storeId);
      avmService.createStore(stagingStoreName);
      if (logger.isDebugEnabled())
         logger.debug("Created staging sandbox store: " + stagingStoreName);
      
      // we can either branch from an existing staging store or create a new structure
      if (branchStoreId != null)
      {
         String branchStorePath = AVMUtil.buildStagingStoreName(branchStoreId) + ":/" +
                                  JNDIConstants.DIR_DEFAULT_WWW;
         avmService.createBranch(-1, branchStorePath,
                                 stagingStoreName + ":/", JNDIConstants.DIR_DEFAULT_WWW);
      }
      else
      {
         // create the system directories 'www' and 'avm_webapps'
         avmService.createDirectory(stagingStoreName + ":/", JNDIConstants.DIR_DEFAULT_WWW);
         avmService.createDirectory(AVMUtil.buildStoreRootPath(stagingStoreName), 
                                    JNDIConstants.DIR_DEFAULT_APPBASE);
      }
      
      
      
      // set staging area permissions
      SandboxFactory.setStagingPermissions(storeId, webProjectNodeRef);
      
      // Add permissions for layers
      
      // tag the store with the store type
      avmService.setStoreProperty(stagingStoreName,
                                  SandboxConstants.PROP_SANDBOX_STAGING_MAIN,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
      avmService.setStoreProperty(stagingStoreName,
                                  SandboxConstants.PROP_WEB_PROJECT_NODE_REF,
                                  new PropertyValue(DataTypeDefinition.NODE_REF, webProjectNodeRef));
      
      // tag the store with the DNS name property
      tagStoreDNSPath(avmService, stagingStoreName, storeId);
      
      // snapshot the store
      avmService.createSnapshot(stagingStoreName, null, null);
      
      
      
      
      
      // create the 'preview' store for the website
      String previewStoreName = AVMUtil.buildStagingPreviewStoreName(storeId);
      avmService.createStore(previewStoreName);
      if (logger.isDebugEnabled())
         logger.debug("Created staging preview sandbox store: " + previewStoreName +
                      " above " + stagingStoreName);
      
      // create a layered directory pointing to 'www' in the staging area
      avmService.createLayeredDirectory(AVMUtil.buildStoreRootPath(stagingStoreName), 
                                        previewStoreName + ":/", 
                                        JNDIConstants.DIR_DEFAULT_WWW);
      
    
      // apply READ permissions for all users
      //dirRef = AVMNodeConverter.ToNodeRef(-1, AVMUtil.buildStoreRootPath(previewStoreName));
      //permissionService.setPermission(dirRef, PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);
      
      // tag the store with the store type
      avmService.setStoreProperty(previewStoreName,
                                  SandboxConstants.PROP_SANDBOX_STAGING_PREVIEW,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
      
      // tag the store with the DNS name property
      tagStoreDNSPath(avmService, previewStoreName, storeId, "preview");

      // The preview store depends on the main staging store (dist=1)
      tagStoreBackgroundLayer(avmService,previewStoreName,stagingStoreName,1);
      
      // snapshot the store
      avmService.createSnapshot(previewStoreName, null, null);
      
      
      // tag all related stores to indicate that they are part of a single sandbox
      final QName sandboxIdProp = QName.createQName(SandboxConstants.PROP_SANDBOXID + GUID.generate());
      avmService.setStoreProperty(stagingStoreName,
                                  sandboxIdProp,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
      avmService.setStoreProperty(previewStoreName,
                                  sandboxIdProp,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
      
      if (logger.isDebugEnabled())
      {
         dumpStoreProperties(avmService, stagingStoreName);
         dumpStoreProperties(avmService, previewStoreName);
      }

      return new SandboxInfo( new String[] { stagingStoreName, previewStoreName } );
   }

   public static void setStagingPermissions(String storeId, 
           NodeRef webProjectNodeRef)
   {
      ServiceRegistry services = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
      PermissionService permissionService = services.getPermissionService();
      NodeService nodeService = services.getNodeService();
      
      String storeName = AVMUtil.buildStagingStoreName(storeId);
      NodeRef dirRef = AVMNodeConverter.ToNodeRef(-1, AVMUtil.buildStoreRootPath(storeName));
       
      // Apply sepcific user permissions as set on the web project
      // All these will be masked out
      List<ChildAssociationRef> userInfoRefs = nodeService.getChildAssocs(
               webProjectNodeRef, WCMAppModel.ASSOC_WEBUSER, RegexQNamePattern.MATCH_ALL);
      for (ChildAssociationRef ref : userInfoRefs)
      {
         NodeRef userInfoRef = ref.getChildRef();
         String username = (String)nodeService.getProperty(userInfoRef, WCMAppModel.PROP_WEBUSERNAME);
         String userrole = (String)nodeService.getProperty(userInfoRef, WCMAppModel.PROP_WEBUSERROLE);
      
         permissionService.setPermission(dirRef, username, userrole, true);
      }
   }
   
   public static void setStagingPermissionMasks(String storeId)
   {
      FacesContext context = FacesContext.getCurrentInstance();
      ServiceRegistry services = Repository.getServiceRegistry(context);
      PermissionService permissionService = services.getPermissionService();
      
      String storeName = AVMUtil.buildStagingStoreName(storeId);
      NodeRef dirRef = AVMNodeConverter.ToNodeRef(-1, AVMUtil.buildStoreRootPath(storeName));
      
      // apply READ permissions for all users
      permissionService.setPermission(dirRef, PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);

      // Set store permission masks
      String currentUser = Application.getCurrentUser(context).getUserName();
      permissionService.setPermission(dirRef.getStoreRef(), currentUser, PermissionService.CHANGE_PERMISSIONS, true);
      permissionService.setPermission(dirRef.getStoreRef(), PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);
   }
   
   public static void updateStagingAreaManagers(String storeId, 
           NodeRef webProjectNodeRef, final List<String> managers)
   {
       // The stores have the mask set in updateSandboxManagers
       String storeName = AVMUtil.buildStagingStoreName(storeId);
       ServiceRegistry services = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
       PermissionService permissionService = services.getPermissionService();
    
       NodeRef dirRef = AVMNodeConverter.ToNodeRef(-1, AVMUtil.buildStoreRootPath(storeName));
       for (String manager : managers)
       {
           permissionService.setPermission(dirRef, manager, AVMUtil.ROLE_CONTENT_MANAGER, true);
           
           // give the manager change permissions permission in the staging area store
           permissionService.setPermission(dirRef.getStoreRef(), manager, 
                    PermissionService.CHANGE_PERMISSIONS, true);
       }
   }
   
   public static void addStagingAreaUser(String storeId, String authority, String role)
   {
       // The stores have the mask set in updateSandboxManagers
       String storeName = AVMUtil.buildStagingStoreName(storeId);
       ServiceRegistry services = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
       PermissionService permissionService = services.getPermissionService();
    
       NodeRef dirRef = AVMNodeConverter.ToNodeRef(-1, AVMUtil.buildStoreRootPath(storeName));
       permissionService.setPermission(dirRef, authority, role, true);
   }
   
   /**
    * Create a user sandbox for the named store.
    * 
    * A user sandbox is comprised of two stores, the first 
    * named 'storename--username' layered over the staging store with a preview store 
    * named 'storename--username--preview' layered over the main store.
    * 
    * Various store meta-data properties are set including:
    * Identifier for store-types: .sandbox.author.main and .sandbox.author.preview
    * Store-id: .sandbox-id.<guid> (unique across all stores in the sandbox)
    * DNS: .dns.<store> = <path-to-webapps-root>
    * Website Name: .website.name = website name
    * 
    * @param storeId    The store id to create the sandbox for
    * @param managers   The list of authorities who have ContentManager role in the website
    * @param username   Username of the user to create the sandbox for
    * @param role       Role permission for the user
    * @return           Summary information regarding the sandbox
    */
   public static SandboxInfo createUserSandbox(String storeId, 
                                               List<String> managers,
                                               String username, 
                                               String role)
   {
      ServiceRegistry services = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
      AVMService avmService = services.getAVMService();
      PermissionService permissionService = services.getPermissionService();
      
      // create the user 'main' store
      String userStoreName    = AVMUtil.buildUserMainStoreName(storeId, username);
      String previewStoreName = AVMUtil.buildUserPreviewStoreName(storeId, username);
      
      if (avmService.getStore(userStoreName) != null)
      {
         if (logger.isDebugEnabled())
         {
            logger.debug("Not creating as store already exists: " + userStoreName);
         }
         return new SandboxInfo( new String[] { userStoreName, previewStoreName } );
      }
      
      avmService.createStore(userStoreName);
      String stagingStoreName = AVMUtil.buildStagingStoreName(storeId);
      if (logger.isDebugEnabled())
         logger.debug("Created user sandbox store: " + userStoreName +
                      " above staging store " + stagingStoreName);
      
      // create a layered directory pointing to 'www' in the staging area
      avmService.createLayeredDirectory(AVMUtil.buildStoreRootPath(stagingStoreName), 
                                        userStoreName + ":/", 
                                        JNDIConstants.DIR_DEFAULT_WWW);
      NodeRef dirRef = AVMNodeConverter.ToNodeRef(-1, AVMUtil.buildStoreRootPath(userStoreName));
      
      // Apply access mask to the store (ACls are applie to the staging area)
      
      // apply the user role permissions to the sandbox
      permissionService.setPermission(dirRef.getStoreRef(), username, PermissionService.ALL_PERMISSIONS, true);
      permissionService.setPermission(dirRef.getStoreRef(), PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);
      // apply the manager role permission for each manager in the web project
      for (String manager : managers)
      {
         permissionService.setPermission(dirRef.getStoreRef(), manager, AVMUtil.ROLE_CONTENT_MANAGER, true);
      }
      
      // tag the store with the store type
      avmService.setStoreProperty(userStoreName,
                                  SandboxConstants.PROP_SANDBOX_AUTHOR_MAIN,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
      
      // tag the store with the base name of the website so that corresponding
      // staging areas can be found.
      avmService.setStoreProperty(userStoreName,
                                  SandboxConstants.PROP_WEBSITE_NAME,
                                  new PropertyValue(DataTypeDefinition.TEXT, storeId));
      
      // tag the store, oddly enough, with its own store name for querying.
      avmService.setStoreProperty(userStoreName,
                                  QName.createQName(null, SandboxConstants.PROP_SANDBOX_STORE_PREFIX + userStoreName),
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
         
      // tag the store with the DNS name property
      tagStoreDNSPath(avmService, userStoreName, storeId, username);
      
      // The user store depends on the main staging store (dist=1)
      tagStoreBackgroundLayer(avmService,userStoreName,stagingStoreName,1);

      // snapshot the store
      avmService.createSnapshot(userStoreName, null, null);
      
      // create the user 'preview' store
      avmService.createStore(previewStoreName);
      if (logger.isDebugEnabled())
         logger.debug("Created user preview sandbox store: " + previewStoreName +
                      " above " + userStoreName);
      
      // create a layered directory pointing to 'www' in the user 'main' store
      avmService.createLayeredDirectory(AVMUtil.buildStoreRootPath(userStoreName), 
                                        previewStoreName + ":/", 
                                        JNDIConstants.DIR_DEFAULT_WWW);
      dirRef = AVMNodeConverter.ToNodeRef(-1, AVMUtil.buildStoreRootPath(previewStoreName));
      
      // Apply access mask to the store (ACls are applied to the staging area)
      
      // apply the user role permissions to the sandbox
      permissionService.setPermission(dirRef.getStoreRef(), username, PermissionService.ALL_PERMISSIONS, true);
      permissionService.setPermission(dirRef.getStoreRef(), PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);
      // apply the manager role permission for each manager in the web project
      for (String manager : managers)
      {
         permissionService.setPermission(dirRef.getStoreRef(), manager, AVMUtil.ROLE_CONTENT_MANAGER, true);
      }
      
      // tag the store with the store type
      avmService.setStoreProperty(previewStoreName,
                                  SandboxConstants.PROP_SANDBOX_AUTHOR_PREVIEW,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
      
      // tag the store with its own store name for querying.
      avmService.setStoreProperty(previewStoreName,
                                  QName.createQName(null, SandboxConstants.PROP_SANDBOX_STORE_PREFIX + previewStoreName),
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
         
      // tag the store with the DNS name property
      tagStoreDNSPath(avmService, previewStoreName, storeId, username, "preview");
      
      // The preview user store depends on the main user store (dist=1)
      tagStoreBackgroundLayer(avmService,previewStoreName, userStoreName,1);

      // The preview user store depends on the main staging store (dist=2)
      tagStoreBackgroundLayer(avmService,previewStoreName, stagingStoreName,2);

         
      // snapshot the store
      avmService.createSnapshot(previewStoreName, null, null);
      
      
      // tag all related stores to indicate that they are part of a single sandbox
      QName sandboxIdProp = QName.createQName(null, SandboxConstants.PROP_SANDBOXID + GUID.generate());
      avmService.setStoreProperty(userStoreName, 
                                  sandboxIdProp,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
      avmService.setStoreProperty(previewStoreName,
                                  sandboxIdProp,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
      
      if (logger.isDebugEnabled())
      {
         dumpStoreProperties(avmService, userStoreName);
         dumpStoreProperties(avmService, previewStoreName);
      }
      return new SandboxInfo( new String[] { userStoreName, previewStoreName } );
   }
   
   /**
    * Create a workflow sandbox for the named store.
    * 
    * Various store meta-data properties are set including:
    * Identifier for store-types: .sandbox.workflow.main and .sandbox.workflow.preview
    * Store-id: .sandbox-id.<guid> (unique across all stores in the sandbox)
    * DNS: .dns.<store> = <path-to-webapps-root>
    * Website Name: .website.name = website name
    * 
    * @param storeId The id of the store to create a sandbox for
    * @return Information about the sandbox
    */
   public static SandboxInfo createWorkflowSandbox(final String storeId)
   {
      final ServiceRegistry services = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
      final AVMService avmService = services.getAVMService();
      
      final String stagingStoreName = AVMUtil.buildStagingStoreName(storeId);

      // create the workflow 'main' store
      final String packageName = AVMUtil.STORE_WORKFLOW + "-" + GUID.generate();
      final String mainStoreName = 
         AVMUtil.buildWorkflowMainStoreName(storeId, packageName);
      
      avmService.createStore(mainStoreName);
      if (logger.isDebugEnabled())
         logger.debug("Created workflow sandbox store: " + mainStoreName);
         
      // create a layered directory pointing to 'www' in the staging area
      avmService.createLayeredDirectory(AVMUtil.buildStoreRootPath(stagingStoreName), 
                                        mainStoreName + ":/", 
                                        JNDIConstants.DIR_DEFAULT_WWW);
         
      // tag the store with the store type
      avmService.setStoreProperty(mainStoreName,
                                  SandboxConstants.PROP_SANDBOX_WORKFLOW_MAIN,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
         
      // tag the store with the base name of the website so that corresponding
      // staging areas can be found.
      avmService.setStoreProperty(mainStoreName,
                                  SandboxConstants.PROP_WEBSITE_NAME,
                                  new PropertyValue(DataTypeDefinition.TEXT, storeId));
         
      // tag the store, oddly enough, with its own store name for querying.
      avmService.setStoreProperty(mainStoreName,
                                  QName.createQName(null, SandboxConstants.PROP_SANDBOX_STORE_PREFIX + mainStoreName),
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
         
      // tag the store with the DNS name property
      tagStoreDNSPath(avmService, mainStoreName, storeId, packageName);
         

      // The main workflow store depends on the main staging store (dist=1)
      tagStoreBackgroundLayer(avmService,mainStoreName, stagingStoreName ,1);

      // snapshot the store
      avmService.createSnapshot(mainStoreName, null, null);
         
      // create the workflow 'preview' store
      final String previewStoreName = 
         AVMUtil.buildWorkflowPreviewStoreName(storeId, packageName);
      avmService.createStore(previewStoreName);
      if (logger.isDebugEnabled())
         logger.debug("Created workflow sandbox preview store: " + previewStoreName);
         
      // create a layered directory pointing to 'www' in the workflow 'main' store
      avmService.createLayeredDirectory(AVMUtil.buildStoreRootPath(mainStoreName), 
                                        previewStoreName + ":/", 
                                        JNDIConstants.DIR_DEFAULT_WWW);
         
      // tag the store with the store type
      avmService.setStoreProperty(previewStoreName,
                                  SandboxConstants.PROP_SANDBOX_WORKFLOW_PREVIEW,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
      
      // tag the store with its own store name for querying.
      avmService.setStoreProperty(previewStoreName,
                                  QName.createQName(null, 
                                                    SandboxConstants.PROP_SANDBOX_STORE_PREFIX + previewStoreName),
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
         
      // tag the store with the DNS name property
      tagStoreDNSPath(avmService, previewStoreName, storeId, packageName, "preview");


      // The preview worfkflow store depends on the main workflow store (dist=1)
      tagStoreBackgroundLayer(avmService,previewStoreName, mainStoreName,1);

      // The preview workflow store depends on the main staging store (dist=2)
      tagStoreBackgroundLayer(avmService,previewStoreName, stagingStoreName,2);

      
      // snapshot the store
      avmService.createSnapshot(previewStoreName, null, null);
         
         
      // tag all related stores to indicate that they are part of a single sandbox
      final QName sandboxIdProp = QName.createQName(SandboxConstants.PROP_SANDBOXID + GUID.generate());
      avmService.setStoreProperty(mainStoreName, 
                                  sandboxIdProp,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
      avmService.setStoreProperty(previewStoreName, 
                                  sandboxIdProp,
                                  new PropertyValue(DataTypeDefinition.TEXT, null));
      
      if (logger.isDebugEnabled())
      {
         dumpStoreProperties(avmService, mainStoreName);
         dumpStoreProperties(avmService, previewStoreName);
      }
      return new SandboxInfo( new String[] { mainStoreName, previewStoreName } );
   }
   
   /**
    * Update the permissions for the list of sandbox managers applied to a user sandbox.
    * <p>
    * Ensures that all managers in the list have full WRITE access to the specified user stores.
    * 
    * @param storeId    The store id of the sandbox to update
    * @param managers   The list of authorities who have ContentManager role in the web project
    * @param username   Username of the user sandbox to update
    */
   public static void updateSandboxManagers(
         final String storeId, final List<String> managers, final String username)
   {
      final ServiceRegistry services = Repository.getServiceRegistry(FacesContext.getCurrentInstance());
      final PermissionService permissionService = services.getPermissionService();
      
      final String userStoreName    = AVMUtil.buildUserMainStoreName(storeId, username);
      final String previewStoreName = AVMUtil.buildUserPreviewStoreName(storeId, username);
      
      // Apply masks to the stores
      
      // apply the manager role permission to the user main sandbox for each manager
      NodeRef dirRef = AVMNodeConverter.ToNodeRef(-1, AVMUtil.buildStoreRootPath(userStoreName));
      for (String manager : managers)
      {
         permissionService.setPermission(dirRef.getStoreRef(), manager, AVMUtil.ROLE_CONTENT_MANAGER, true);
      }
      
      // apply the manager role permission to the user preview sandbox for each manager
      dirRef = AVMNodeConverter.ToNodeRef(-1, AVMUtil.buildStoreRootPath(previewStoreName));
      for (String manager : managers)
      {
         permissionService.setPermission(dirRef.getStoreRef(), manager, AVMUtil.ROLE_CONTENT_MANAGER, true);
      }
   }
   
   /**
    * Tag a named store with a DNS path meta-data attribute.
    * The DNS meta-data attribute is set to the system path 'store:/www/avm_webapps'
    * 
    * @param store  Name of the store to tag
    */
   private static void tagStoreDNSPath(AVMService avmService, String store, String... components)
   {
      String path = AVMUtil.buildSandboxRootPath(store);
      // DNS name mangle the property name - can only contain value DNS characters!
      String dnsProp = SandboxConstants.PROP_DNS + DNSNameMangler.MakeDNSName(components);
      avmService.setStoreProperty(store, QName.createQName(null, dnsProp),
            new PropertyValue(DataTypeDefinition.TEXT, path));
   }

   /**
    *   Tags a store with a property that indicates one of its 
    *   backgroundStore layers, and the distance of that layer. 
    *   This function must be called separately for each background 
    *   store;  for example the "mysite--alice--preview" store had 
    *   as its immediate background "mysite--alice", which itself had
    *   as its background store "mysite", you'd make a sequence of
    *   calls like this:
    *
    *   <pre>
    *    tagStoreBackgroundLayer("mysite--alice",          "mysite",        1);
    *    tagStoreBackgroundLayer("mysite--alice--preview", "mysite--alice", 1);
    *    tagStoreBackgroundLayer("mysite--alice--preview", "mysite",        2);
    *   </pre>
    *
    *   This make it easy for other parts of the system to determine
    *   which stores depend on others directly or indirectly (which is
    *   useful for reloading virtualized webapps).
    *
    * @param store            Name of the store to tag
    * @param backgroundStore  Name of store's background store
    * @param distance         Distance from store.
    *                         The backgroundStore 'mysite' is 1 away from the store 'mysite--alice'
    *                         but 2 away from the store 'mysite--alice--preview'.
    */
   private static void tagStoreBackgroundLayer(AVMService  avmService, 
                                               String      store, 
                                               String      backgroundStore, 
                                               int         distance)
   {
      String prop_key = SandboxConstants.PROP_BACKGROUND_LAYER + backgroundStore;
      avmService.setStoreProperty(store, QName.createQName(null, prop_key),
            new PropertyValue(DataTypeDefinition.INT, distance));
   }

   /**
    * Debug helper method to dump the properties of a store
    *  
    * @param store   Store name to dump properties for
    */
   private static void dumpStoreProperties(AVMService avmService, String store)
   {
      logger.debug("Store " + store);
      Map<QName, PropertyValue> props = avmService.getStoreProperties(store);
      for (QName name : props.keySet())
      {
         logger.debug("   " + name + ": " + props.get(name));
      }
   }
}
