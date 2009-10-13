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
package org.alfresco.web.bean.trashcan;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;

public class TrashcanItemDetailsDialog extends TrashcanDialog
{
   private static final long serialVersionUID = 1767515883530860417L;
   
   private static final String MSG_DETAILS_OF = "details_of";
   private static final String MSG_CLOSE = "close";
   private final static String MSG_LEFT_QUOTE = "left_qoute";
   private final static String MSG_RIGHT_QUOTE = "right_quote";
   
   transient private PermissionService permissionService;

   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      return null;
   }

   @Override
   public String getContainerTitle()
   {
       FacesContext fc = FacesContext.getCurrentInstance();
       return Application.getMessage(fc, MSG_DETAILS_OF) + " " + Application.getMessage(fc, MSG_LEFT_QUOTE) + 
             property.getItem().getName() + Application.getMessage(fc, MSG_RIGHT_QUOTE);
   }

   @Override
   public String getCancelButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), MSG_CLOSE);
   }
   
   @Override
   public Object getActionsContext()
   {
      return getItem();
   }

   public Node getItem()
   {
      return property.getItem();
   }
   
   public void setPermissionService(PermissionService permissionService)
   {
      this.permissionService = permissionService;
   }
   
   /**
    *@return permissionService
    */
   protected PermissionService getPermissionService()
   {
    //check for null for cluster environment
      if (permissionService == null)
      {
         permissionService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getPermissionService();
      }
      return permissionService;
   }
}
