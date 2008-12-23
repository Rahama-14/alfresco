/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General protected License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General protected License for more details.

 * You should have received a copy of the GNU General protected License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.wcm.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.config.JNDIConstants;
import org.alfresco.mbeans.VirtServerRegistry;
import org.alfresco.model.WCMAppModel;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.service.cmr.avm.AVMBadArgumentException;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.VirtServerUtils;
import org.alfresco.wcm.sandbox.SandboxConstants;


/**
 * Helper methods and constants related to WCM directories, paths and store name manipulation.
 * 
 * TODO refactor ...
 * 
 * @author Ariel Backenroth
 * @author Kevin Roast
 */
public class WCMUtil
{
   /**
    * Extracts the sandbox store id from the avm path
    *
    * @param avmPath an absolute avm path
    * 
    * @return the sandbox store id
    */
   public static String getSandboxStoreId(final String avmPath)
   {
      final int i = avmPath.indexOf(AVM_STORE_SEPARATOR);
      if (i == -1)
      {
         throw new IllegalArgumentException("path " + avmPath + " does not contain a store");
      }
      return avmPath.substring(0, i);
   }
   
   /**
    * Extracts the web project store id from the (sandbox) store name
    * <p>
    * For example, if the (sandbox) store name is: teststore--admin then the web project store id is: teststore
    * <p>
    * Note: Although the staging sandbox store name is currently equivalent to the web project store id, it should
    * be derived using 'buildStagingStoreName'.
    * 
    * @param storeName the sandbox store id
    * 
    * @return the web project store id
    */
   public static String getWebProjectStoreId(final String storeName)
   {
      final int index = storeName.indexOf(WCMUtil.STORE_SEPARATOR);
      return (index == -1
              ? storeName
              : storeName.substring(0, index));
   }
   
   /**
    * Extracts the web project store id from the avm path
    *
    * For example, if the avm path is: teststore--admin:/www/ROOT then the web project id is: teststore
    *
    * @param avmPath an absolute avm path
    * 
    * @return the web project store id.
    */
   public static String getWebProjectStoreIdFromPath(final String avmPath)
   {
       return getWebProjectStoreId(getSandboxStoreId(avmPath));
   }

   /**
    * Indicates whether the store name describes a preview store.
    *
    * @param storeName the store name
    * 
    * @return <tt>true</tt> if the store is a preview store, <tt>false</tt> otherwise.
    */
   protected static boolean isPreviewStore(final String storeName)
   {
      return ((storeName != null) && (storeName.endsWith(WCMUtil.STORE_SEPARATOR + WCMUtil.STORE_PREVIEW)));
   }

   /**
    * Indicates whether the store name describes a workflow store.
    *
    * @param storeName the store name
    * 
    * @return <tt>true</tt> if the store is a workflow store, <tt>false</tt> otherwise.
    */
   protected static boolean isWorkflowStore(String storeName)
   {
      if (WCMUtil.isPreviewStore(storeName))
      {
         storeName = WCMUtil.getCorrespondingMainStoreName(storeName);
      }
      
      return ((storeName != null) && (storeName.indexOf(STORE_SEPARATOR + STORE_WORKFLOW) != -1));
   }
   
   /**
    * Indicates whether the store name describes a user store.
    *
    * @param storeName the store name
    * 
    * @return <tt>true</tt> if the store is a user store, <tt>false</tt> otherwise.
    */
   public static boolean isUserStore(String storeName)
   {
      if (WCMUtil.isPreviewStore(storeName))
      {
         storeName = WCMUtil.getCorrespondingMainStoreName(storeName);
      }
      return ((storeName != null) && (storeName.indexOf(WCMUtil.STORE_SEPARATOR) != -1));
   }
   
   /**
    * Indicates whether the store name describes a staging store.
    * 
    * @param storeName the store name
    * 
    * @return <tt>true</tt> if the store is a main store, <tt>false</tt> otherwise.
    */
   public static boolean isStagingStore(String storeName)
   {
      return ((storeName != null) && (storeName.indexOf(WCMUtil.STORE_SEPARATOR) == -1));
   }

   /**
    * Extracts the username from the store name.
    *
    * @param storeName the store name
    * 
    * @return the username associated or <tt>null</tt> if this is a staging store.
    */
   public static String getUserName(String storeName)
   {
      if (WCMUtil.isPreviewStore(storeName))
      {
         storeName = WCMUtil.getCorrespondingMainStoreName(storeName);
      }
      final int index = storeName.indexOf(WCMUtil.STORE_SEPARATOR);
      return (index == -1
              ? null
              : storeName.substring(index + WCMUtil.STORE_SEPARATOR.length()));
   }
   
   /**
    * Extracts the workflow id
    * 
    * @param storeName
    * @return
    */
   public static String getWorkflowId(String storeName)
   {
      if (WCMUtil.isPreviewStore(storeName))
      {
         storeName = WCMUtil.getCorrespondingMainStoreName(storeName);
      }
      final int index = storeName.indexOf(STORE_SEPARATOR + STORE_WORKFLOW);
      return (index == -1
              ? null
              : storeName.substring(index + WCMUtil.STORE_SEPARATOR.length()));
   }

   /**
    * Returns the corresponding main store name if this is a preview store name.
    *
    * @param storeName the preview store name.
    * 
    * @return the corresponding main store name.
    * 
    * @exception IllegalArgumentException if this is not a preview store name.
    */
   protected static String getCorrespondingMainStoreName(final String storeName)
   {
      if (!WCMUtil.isPreviewStore(storeName))
      {
         throw new IllegalArgumentException("store " + storeName + " is not a preview store");
      }
      return storeName.substring(0, 
                                 (storeName.length() - 
                                  (WCMUtil.STORE_SEPARATOR + WCMUtil.STORE_PREVIEW).length()));
   }

   /**
    * Returns the corresponding preview store name if this is a main store name.
    *
    * @param storeName the main store name.
    * 
    * @return the corresponding preview store name.
    * 
    * @exception IllegalArgumentException if this is not a main store name.
    */
   protected static String getCorrespondingPreviewStoreName(final String storeName)
   {
      if (WCMUtil.isPreviewStore(storeName))
      {
         throw new IllegalArgumentException("store " + storeName + " is already a preview store");
      }
      return storeName + WCMUtil.STORE_SEPARATOR + WCMUtil.STORE_PREVIEW;
   }

   /**
    * Returns the corresponding path in the main store name if this is a path in 
    * a preview store.
    *
    * @param avmPath an avm path within the main store.
    * 
    * @return the corresponding path within the preview store.
    * 
    * @exception IllegalArgumentException if this is not a path within the preview store.
    */
   protected static String getCorrespondingPathInMainStore(final String avmPath)
   {
      String storeName = WCMUtil.getSandboxStoreId(avmPath);
      storeName = WCMUtil.getCorrespondingMainStoreName(storeName);
      return WCMUtil.getCorrespondingPath(avmPath, storeName);
   }

   /**
    * Returns the corresponding path in the preview store name if this is a path in 
    * a main store.
    *
    * @param avmPath an avm path within the main store.
    * 
    * @return the corresponding path within the preview store.
    * 
    * @exception IllegalArgumentException if this is not a path within the preview store.
    */
   protected static String getCorrespondingPathInPreviewStore(final String avmPath)
   {
      String storeName = WCMUtil.getSandboxStoreId(avmPath);
      storeName = WCMUtil.getCorrespondingPreviewStoreName(storeName);
      return WCMUtil.getCorrespondingPath(avmPath, storeName);
   }

   /**
    * Returns the corresponding path in the store provided.
    * 
    * @param avmPath an avm path
    * @param otherStore the other store to return the corresponding path for
    * 
    * @return the corresponding path within the supplied store
    */
   protected static String getCorrespondingPath(final String avmPath, final String otherStore)
   {
      return (otherStore + AVM_STORE_SEPARATOR + WCMUtil.getStoreRelativePath(avmPath));
   }
   
   /**
    * Returns the main staging store name for the specified web project
    * 
    * @param wpStoreId   web project store id to build staging store name for
    * @return String     main staging store name for the specified web project store id
    */
   public static String buildStagingStoreName(final String wpStoreId)
   {
       ParameterCheck.mandatoryString("wpStoreId", wpStoreId);
       return wpStoreId;
   }
   
   /**
    * Returns the preview store name for the specified store id.
    * 
    * @param storeId store id to build preview store name for
    * 
    * @return preview store name for the specified store id
    */
   protected static String buildStagingPreviewStoreName(final String storeId)
   {
      return (WCMUtil.buildStagingStoreName(storeId) + WCMUtil.STORE_SEPARATOR + 
              WCMUtil.STORE_PREVIEW);
   }
   
   /**
    * Returns the user's main store name for a specific username
    * 
    * @param storeId store id to build user store name for
    * @param username of the user to build store name for
    * 
    * @return the main store for the specified user and store id
    */
   public static String buildUserMainStoreName(final String storeId, 
                                               final String userName)
   {
       ParameterCheck.mandatoryString("userName", userName);
       return (WCMUtil.buildStagingStoreName(storeId) + WCMUtil.STORE_SEPARATOR + 
               userName);
   }
   
   /**
    * Returns the preview store name for a specific username.
    * 
    * @param storeId store id to build user preview store name for
    * @param username of the user to build preview store name for
    * 
    * @return the preview store for the specified user and store id
    */
   protected static String buildUserPreviewStoreName(final String storeId, 
                                                  final String username)
   {
      return (WCMUtil.buildUserMainStoreName(storeId, username) + WCMUtil.STORE_SEPARATOR + 
              WCMUtil.STORE_PREVIEW);
   }

   /**
    * Returns the store name for a specific workflow Id.
    * 
    * @param storeId store id to build workflow store name for
    * @param workflowId of the user to build workflow store name for
    * 
    * @return the store for the specified workflow and store ids
    */
   protected static String buildWorkflowMainStoreName(final String storeId, 
                                                   final String workflowId)
   {
       ParameterCheck.mandatoryString("workflowId", workflowId);
       return (WCMUtil.buildStagingStoreName(storeId) + WCMUtil.STORE_SEPARATOR +
               workflowId);
   }

   /**
    * Returns the preview store name for a specific workflow Id.
    * 
    * @param storeId store id to build preview workflow store name for
    * @param workflowId of the user to build preview workflow store name for
    * 
    * @return the store for the specified preview workflow and store ids
    */
   protected static String buildWorkflowPreviewStoreName(final String storeId, 
                                                      final String workflowId)
   {
      return (WCMUtil.buildWorkflowMainStoreName(storeId, workflowId) +
              WCMUtil.STORE_SEPARATOR + WCMUtil.STORE_PREVIEW);
   }

   /**
    * Returns the root path for the specified store name
    * 
    * eg. mystore -> mystore:/www
    * 
    * @param storeName store to build root path for
    * 
    * @return root path for the specified store name
    */
   protected static String buildStoreRootPath(final String storeName)
   {
       ParameterCheck.mandatoryString("storeName", storeName);
       return storeName + AVM_STORE_SEPARATOR + "/" + JNDIConstants.DIR_DEFAULT_WWW;
   }

   /**
    * Returns the root path for the specified sandbox name
    * 
    * * eg. mystore -> mystore:/www/avm_webapps
    * 
    * @param storeName store to build root sandbox path for
    * 
    * @return root sandbox path for the specified store name
    */
   public static String buildSandboxRootPath(final String storeName)
   {
       ParameterCheck.mandatoryString("storeName", storeName);
       return storeName + AVM_STORE_SEPARATOR + JNDIConstants.DIR_DEFAULT_WWW_APPBASE;
   }
   
   /**
    * Returns the root webapp path for the specified store and webapp name
    * 
    * @param storeName store to build root webapp path for
    * @param webapp webapp folder name
    * 
    * @return the root webapp path for the specified store and webapp name
    */
   public static String buildStoreWebappPath(final String storeName, String webApp)
   {
       ParameterCheck.mandatoryString("webApp", webApp);
       return WCMUtil.buildSandboxRootPath(storeName) + '/' + webApp;
   }
   
   public static String buildStoreUrl(AVMService avmService, String storeName, String domain, String port)
   {
       ParameterCheck.mandatoryString("storeName", storeName);

       if (domain == null || port == null)
       {
           throw new IllegalArgumentException("Domain and port are mandatory.");
       }
       
       return MessageFormat.format(JNDIConstants.PREVIEW_SANDBOX_URL, 
               lookupStoreDNS(avmService, storeName), 
               domain, 
               port);
   }

   protected static String buildWebappUrl(AVMService avmService, final String storeName, final String webApp, String domain, String port)
   {
       ParameterCheck.mandatoryString("webApp", webApp);
       return (webApp.equals(DIR_ROOT)
               ? buildStoreUrl(avmService, storeName, domain, port)
               : buildStoreUrl(avmService, storeName, domain, port) + '/' + webApp);
   }
   
   public static String buildAssetUrl(String assetPath, String domain, String port, String dns)
   {
       ParameterCheck.mandatoryString("assetPath", assetPath);
       
      if (domain == null || port == null || dns == null)
      {
         throw new IllegalArgumentException("Domain, port and dns name are mandatory.");
      }
      
      if (assetPath.startsWith(JNDIConstants.DIR_DEFAULT_WWW_APPBASE))
      {
         assetPath = assetPath.substring((JNDIConstants.DIR_DEFAULT_WWW_APPBASE).length());
      }
      if (assetPath.startsWith('/' + DIR_ROOT))
      {
         assetPath = assetPath.substring(('/' + DIR_ROOT).length());
      }
      if (assetPath.length() == 0 || assetPath.charAt(0) != '/')
      {
         assetPath = '/' + assetPath;
      }
      
      return MessageFormat.format(JNDIConstants.PREVIEW_ASSET_URL, dns, domain, port, assetPath);
   }
   
   public static String lookupStoreDNS(AVMService avmService, String store)
   {
       ParameterCheck.mandatoryString("store", store);
      
       final Map<QName, PropertyValue> props = 
         avmService.queryStorePropertyKey(store, QName.createQName(null, SandboxConstants.PROP_DNS + '%'));
       
       return (props.size() == 1
              ? props.keySet().iterator().next().getLocalName().substring(SandboxConstants.PROP_DNS.length())
              : null);
   }
   
   /**
    * Converts the provided path to an absolute path within the avm.
    *
    * @param parentAVMPath used as the parent path if the provided path
    * is relative, otherwise used to extract the parent path portion up until
    * the webapp directory.
    * @param path a path relative to the parentAVMPath path, or if it is
    * absolute, it is relative to the sandbox used in the parentAVMPath.
    *
    * @return an absolute path within the avm using the paths provided.
    */
   /*
   protected static String buildPath(final String parentAVMPath,
                                  final String path,
                                  final PathRelation relation)
   {
      String parent = parentAVMPath;
      if (path == null || path.length() == 0 || ".".equals(path) || "./".equals(path))
      {
         return parent;
      }
      
      if (path.charAt(0) == '/')
      {
         final Matcher m = relation.pattern().matcher(parent);
         if (m.matches())
         {
            parent = m.group(1);
         }
      } 
      else if (parent.charAt(parent.length() - 1) != '/')
      {
         parent = parent + '/';
      }

      return parent + path;
   }
   */

   /**
    * Returns a path relative to the store portion of the avm path.
    *
    * @param absoluteAVMPath an absolute path within the avm
    * @return the path without the store prefix.
    */
   public static String getStoreRelativePath(final String absoluteAVMPath)
   {
       ParameterCheck.mandatoryString("absoluteAVMPath", absoluteAVMPath);
       return absoluteAVMPath.substring(absoluteAVMPath.indexOf(AVM_STORE_SEPARATOR) + 1);
   }

   /**
    * Returns a path relative to the webapp portion of the avm path.
    *
    * @param absoluteAVMPath an absolute path within the avm
    * @return a relative path within the webapp.
    */
   protected static String getWebappRelativePath(final String absoluteAVMPath)
   {
      final Matcher m = WEBAPP_RELATIVE_PATH_PATTERN.matcher(absoluteAVMPath);
      return m.matches() && m.group(3).length() != 0 ? m.group(3) : "/";
   }
   
   /**
    * Returns the webapp within the path
    *
    * @param absoluteAVMPath the path from which to extract the webapp name
    *
    * @return an the webapp name contained within the path or <tt>null</tt>.
    */
   protected static String getWebapp(final String absoluteAVMPath)
   {
      final Matcher m = WEBAPP_RELATIVE_PATH_PATTERN.matcher(absoluteAVMPath);
      return m.matches() && m.group(2).length() != 0 ? m.group(2) : null;
   }

   /**
    * Returns the path portion up the webapp
    *
    * @param absoluteAVMPath the path from which to extract the webapp path
    *
    * @return an absolute avm path to the webapp contained within
    * the path or <tt>null</tt>.
    */
   protected static String getWebappPath(final String absoluteAVMPath)
   {
      final Matcher m = WEBAPP_RELATIVE_PATH_PATTERN.matcher(absoluteAVMPath);
      return m.matches() && m.group(1).length() != 0 ? m.group(1) : null;
   }

   /**
    * Returns a path relative to the sandbox porition of the avm path.
    *
    * @param absoluteAVMPath an absolute path within the avm
    * @return a relative path within the sandbox.
    */
   protected static String getSandboxRelativePath(final String absoluteAVMPath)
   {
      final Matcher m = SANDBOX_RELATIVE_PATH_PATTERN.matcher(absoluteAVMPath);
      return m.matches() && m.group(2).length() != 0 ? m.group(2) : "/";
   }

   /**
    * Returns the path portion up the sandbox
    *
    * @param absoluteAVMPath the path from which to extract the sandbox path
    *
    * @return an absolute avm path to the sandbox contained within
    * the path or <tt>null</tt>.
    */
   protected static String getSandboxPath(final String absoluteAVMPath)
   {
      final Matcher m = SANDBOX_RELATIVE_PATH_PATTERN.matcher(absoluteAVMPath);
      return m.matches() && m.group(1).length() != 0 ? m.group(1) : null;
   }

   protected static Map<String, String> listWebUsers(NodeService nodeService, NodeRef wpNodeRef)
   {
       List<ChildAssociationRef> userInfoRefs = nodeService.getChildAssocs(wpNodeRef, WCMAppModel.ASSOC_WEBUSER, RegexQNamePattern.MATCH_ALL);
       
       Map<String, String> webUsers = new HashMap<String, String>(23);
       
       for (ChildAssociationRef ref : userInfoRefs)
       {
           NodeRef userInfoRef = ref.getChildRef();
           String userName = (String)nodeService.getProperty(userInfoRef, WCMAppModel.PROP_WEBUSERNAME);
           String userRole = (String)nodeService.getProperty(userInfoRef, WCMAppModel.PROP_WEBUSERROLE);
           
           webUsers.put(userName, userRole);
        }

       return webUsers;
   }
   
   /**
    * Creates all directories for a path if they do not already exist.
    */
   /*
   protected static void makeAllDirectories(AVMService avmService, final String avmDirectoryPath)
   {
      // LOGGER.debug("mkdir -p " + avmDirectoryPath);
      String s = avmDirectoryPath;
      final Stack<String[]> dirNames = new Stack<String[]>();
      while (s != null)
      {
         try
         {
            if (avmService.lookup(-1, s) != null)
            {
               // LOGGER.debug("path " + s + " exists");
               break;
            }
         }
         catch (AVMNotFoundException avmfe)
         {
         }
         final String[] sb = AVMNodeConverter.SplitBase(s);
         s = sb[0];
         // LOGGER.debug("pushing " + sb[1]);
         dirNames.push(sb);
      }

      while (!dirNames.isEmpty())
      {
         final String[] sb = dirNames.pop();
         // LOGGER.debug("creating " + sb[1] + " in " + sb[0]);
         avmService.createDirectory(sb[0], sb[1]);
      }
   }
   */

    /**
     * Update notification on the virtualisation server webapp as required for the specified path
     * 
     * @param path    Path to match against
     * @param force   True to force update of server even if path does not match
     */
    public static void updateVServerWebapp(VirtServerRegistry vServerRegistry, String path, boolean force)
    {
        if (force || VirtServerUtils.requiresUpdateNotification(path))
        {
            final int webappIndex = path.indexOf('/', 
                                                 path.indexOf(JNDIConstants.DIR_DEFAULT_APPBASE) + 
                                                 JNDIConstants.DIR_DEFAULT_APPBASE.length() + 1);

            if (webappIndex != -1)
            {
                path = path.substring(0, webappIndex);
            }
            vServerRegistry.updateWebapp(-1, path, true);
        }
    }
   
    /**
     * Removal notification on all the virtualisation server webapp as required by the specified path
     * 
     * @param path    Path to match against
     * @param force   True to force update of server even if path does not match
     */
    protected static void removeAllVServerWebapps(VirtServerRegistry vServerRegistry, String path, boolean force)
    {
        if (force || VirtServerUtils.requiresUpdateNotification(path))
        {
            final int webappIndex = path.indexOf('/', 
                                                 path.indexOf(JNDIConstants.DIR_DEFAULT_APPBASE) + 
                                                 JNDIConstants.DIR_DEFAULT_APPBASE.length() + 1);

            if (webappIndex != -1)
            {
                path = path.substring(0, webappIndex);
            }
            vServerRegistry.removeAllWebapps(-1, path, true);
        }
    }

   /**
    * Removal notification on the virtualisation server webapp as required for the specified path
    * 
    * @param path    Path to match against
    * @param force   True to force update of server even if path does not match
    */
   protected static void removeVServerWebapp(VirtServerRegistry vServerRegistry, String path, boolean force)
   {
      if (force || VirtServerUtils.requiresUpdateNotification(path))
      {
         final int webappIndex = path.indexOf('/', 
                                              path.indexOf(JNDIConstants.DIR_DEFAULT_APPBASE) + 
                                              JNDIConstants.DIR_DEFAULT_APPBASE.length() + 1);

         if (webappIndex != -1)
         {
            path = path.substring(0, webappIndex);
         }
         vServerRegistry.removeWebapp(-1, path, true);
      }
   }
   
   public static String[] splitPath(String path)
   {
       String[] storePath = path.split(AVM_STORE_SEPARATOR);
       if (storePath.length != 2)
       {
           throw new AVMBadArgumentException("Invalid Path: " + path);
       }
       return storePath;
   }
   
   // Component Separator.
   protected static final String STORE_SEPARATOR = "--";
   
   public static final String AVM_STORE_SEPARATOR = ":";
   
   // names of the stores representing the layers for an AVM website
   //XXXarielb this should be private
   protected final static String STORE_WORKFLOW = "workflow";
   protected final static String STORE_PREVIEW = "preview";
   
   // servlet default webapp
   //    Note: this webapp is mapped to the URL path ""
   public final static String DIR_ROOT = "ROOT";
   
   protected final static String SPACE_ICON_WEBSITE       = "space-icon-website";
   
   // web user role permissions
   public static final String ROLE_CONTENT_MANAGER     = "ContentManager";
   public static final String ROLE_CONTENT_PUBLISHER   = "ContentPublisher";
   public static final String ROLE_CONTENT_REVIEWER    = "ContentReviewer";
   public static final String ROLE_CONTENT_CONTRIBUTOR = "ContentContributor";
   
   private final static Pattern WEBAPP_RELATIVE_PATH_PATTERN = 
      Pattern.compile("([^:]+:/" + JNDIConstants.DIR_DEFAULT_WWW +
                      "/" + JNDIConstants.DIR_DEFAULT_APPBASE + "/([^/]+))(.*)");
   
   private final static Pattern SANDBOX_RELATIVE_PATH_PATTERN = 
      Pattern.compile("([^:]+:/" + JNDIConstants.DIR_DEFAULT_WWW +
                      "/" + JNDIConstants.DIR_DEFAULT_APPBASE + ")(.*)");
}
