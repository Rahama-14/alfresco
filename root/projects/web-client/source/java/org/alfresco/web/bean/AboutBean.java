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
package org.alfresco.web.bean;

import javax.faces.context.FacesContext;

import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Repository;

/**
 * Simple backing bean used by the about page to display the version.
 * 
 * @author gavinc
 */
public class AboutBean extends BaseDialogBean
{
   private static final long serialVersionUID = -3777479360531145878L;
   
   private final static String MSG_VERSION = "version";
   private final static String MSG_CLOSE = "close";
   
   transient private DescriptorService descriptorService;
   
   /**
    * Retrieves the version of the repository.
    * 
    * @return The version string
    */
   public String getVersion()
   {
      return this.getDescriptorService().getServerDescriptor().getVersion();
   }
   
   /**
    * Retrieves the edition of the repository.
    * 
    * @return The edition
    */
   public String getEdition()
   {
      return this.getDescriptorService().getServerDescriptor().getEdition();
   }
   
   /**
    * Sets the DescriptorService.
    * 
    * @param descriptorService The DescriptorService
    */
   public void setDescriptorService(DescriptorService descriptorService)
   {
      this.descriptorService = descriptorService;
   }

   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      return null;
   }
   
   @Override
   public String getContainerDescription()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), MSG_VERSION) + " :" + getEdition() + " - v" + getVersion();
   }

   DescriptorService getDescriptorService()
   {
      //check for null in cluster environment  
      if (descriptorService == null)
      {
         descriptorService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getDescriptorService();
      }
      return descriptorService;
   }
   
   @Override
   public String getCancelButtonLabel()
   {
    
      return Application.getMessage(FacesContext.getCurrentInstance(), MSG_CLOSE);
   }
   
   @Override
   protected String getDefaultCancelOutcome() 
   {
	  return "dialog:close:browse";
   }
}
