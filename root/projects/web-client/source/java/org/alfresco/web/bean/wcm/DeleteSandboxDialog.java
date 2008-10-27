/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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

import java.text.MessageFormat;

import javax.faces.context.FacesContext;

import org.alfresco.model.WCMAppModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.wcm.webproject.WebProjectService;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Bean implementation for the AVM "Delete Sandbox" dialog
 * 
 * @author kevinr
 */
public class DeleteSandboxDialog extends BaseDialogBean
{
   private static final long serialVersionUID = 6139801947722234685L;

   private static final Log logger = LogFactory.getLog(DeleteSandboxDialog.class);
   
   protected AVMBrowseBean avmBrowseBean;
   transient private WebProjectService wpService;
   
   // ------------------------------------------------------------------------------
   // Bean property getters and setters 
   
   /**
    * @param avmBrowseBean The avmBrowseBean to set.
    */
   public void setAvmBrowseBean(AVMBrowseBean avmBrowseBean)
   {
      this.avmBrowseBean = avmBrowseBean;
   }
   
   public void setWebProjectService(WebProjectService wpService)
   {
      this.wpService = wpService;
   }

   protected WebProjectService getWebProjectService()
   {
      if (wpService == null)
      {
         wpService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getWebProjectService();
      }
      return wpService;
   }
   
   // ------------------------------------------------------------------------------
   // Dialog implementation
   
   @Override
   protected String finishImpl(FacesContext context, String outcome)
         throws Exception
   {
      // the username for the sandbox to delete (also uninvites from the web project)
      String username = this.avmBrowseBean.getUsername();
      if (username != null)
      {
         Node website = this.avmBrowseBean.getWebsite();
         getWebProjectService().uninviteWebUser(website.getNodeRef(), username);
          
         String wpStoreId = getWebProjectService().getWebProject(website.getNodeRef()).getStoreId();
         String mainStore = AVMUtil.buildUserMainStoreName(wpStoreId, username);
          
         // if the sandbox is allocated to a test server release it
         NodeRef testServer = DeploymentUtil.findAllocatedTestServer(mainStore);
         if (testServer != null)
         {
            getNodeService().setProperty(testServer, WCMAppModel.PROP_DEPLOYSERVERALLOCATEDTO, null);
             
            if (logger.isDebugEnabled())
            {
               logger.debug("Released test server from user sandbox: " + mainStore);
            }
         }
      }
      
      return outcome;
   }
      
   @Override
   protected String doPostCommitProcessing(FacesContext context, String outcome)
   {
      return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
   }
   
   @Override
   protected String getErrorMessageId()
   {
      return "error_delete_sandbox";
   }

   @Override
   public boolean getFinishButtonDisabled()
   {
      return false;
   }
   
   
   // ------------------------------------------------------------------------------
   // Bean Getters and Setters

   /**
    * Returns the confirmation to display to the user before deleting the user sandbox.
    * 
    * @return The formatted message to display
    */
   public String getConfirmMessage()
   {
      String fileConfirmMsg = Application.getMessage(FacesContext.getCurrentInstance(), 
               "delete_sandbox_confirm");
      
      return MessageFormat.format(fileConfirmMsg, 
            new Object[] {this.avmBrowseBean.getUsername()});
   }
}
