/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.web.ui.wcm.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.service.cmr.avm.AVMNotFoundException;
import org.alfresco.service.cmr.avm.locking.AVMLock;
import org.alfresco.service.cmr.avm.locking.AVMLockingService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.wcm.asset.AssetInfo;
import org.alfresco.wcm.util.WCMUtil;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.repo.component.UILockIcon;

/**
 * @author Ariel Backenroth
 */
public class UIAVMLockIcon extends UILockIcon
{
   public static final String ALFRESCO_FACES_AVMLOCKICON = "org.alfresco.faces.AVMLockIcon";
   
   // ------------------------------------------------------------------------------
   // Component implementation
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return ALFRESCO_FACES_AVMLOCKICON;
   }
   
   /**
    * @see javax.faces.component.UIComponentBase#encodeBegin(javax.faces.context.FacesContext)
    */
   public void encodeBegin(FacesContext context) throws IOException
   {
      if (isRendered() == false)
      {
         return;
      }
      
      boolean locked = false;
      boolean lockedOwner = false;
      Object val = getValue();
      List<String> lockUsers = null;
      
      if (val != null) 
      {
         if (val instanceof AssetInfo)
         {
            // via UIUserSandboxes.renderUserFiles()
            
            AssetInfo asset = (AssetInfo)val;
            
            locked = asset.isLocked();
            
            String assetLockOwner = asset.getLockOwner();
            if (assetLockOwner != null)
            {
               lockUsers = new ArrayList<String>(1);
               lockUsers.add(assetLockOwner);
               lockedOwner = assetLockOwner.equals(Application.getCurrentUser(context).getUserName());
            }
         }
         else
         {
            // TODO eventually refactor out
            
            // via browse-sandbox.jsp -> AVMBrowseBean (getFolders/getFiles - directory listing or search)
            
            // get the value and see if the image is locked
            final AVMLockingService avmLockingService = Repository.getServiceRegistry(context).getAVMLockingService();
            
            // NodeRef or String
            final String avmPath = (val instanceof NodeRef 
                                    ? AVMNodeConverter.ToAVMVersionPath((NodeRef)val).getSecond() 
                                    : (val instanceof String
                                       ? (String)val
                                       : null));
            if (avmPath != null)
            {
               String[] pathParts = WCMUtil.splitPath(avmPath);
               AVMLock lock = null;
               try
               {
                  lock = avmLockingService.getLock(WCMUtil.getWebProjectStoreId(pathParts[0]), pathParts[1]);
               }
               catch (AVMNotFoundException nfe)
               {
                  // ignore
               }
               if (lock != null)
               {
                  locked = true;
                  lockUsers = lock.getOwners();
                  lockedOwner = (lockUsers.contains(Application.getCurrentUser(context).getUserName()));
               }
            }
         }
         
         this.encodeBegin(context,
                          locked,
                          lockedOwner,
                          lockUsers == null ? new String[0] : (String[])lockUsers.toArray(new String[lockUsers.size()]));
      }
   }
}
