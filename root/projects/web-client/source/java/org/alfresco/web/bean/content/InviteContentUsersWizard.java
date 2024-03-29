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
package org.alfresco.web.bean.content;

import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.wizard.BaseInviteUsersWizard;

/**
 * Concrete implementation providing the ability to invite users to content.
 * 
 * @author gavinc
 */
public class InviteContentUsersWizard extends BaseInviteUsersWizard
{
   private static final long serialVersionUID = 9198783146031469545L;
   
   /** Cache of available content permissions */
   Set<String> contentPermissions = null;
   
   @Override
   protected Set<String> getPermissionsForType()
   {
      if (this.contentPermissions == null)
      {
         this.contentPermissions = this.getPermissionService().getSettablePermissions(getNode().getType());
      }
       
      return this.contentPermissions;
   }

   @Override
   protected Node getNode()
   {
      return this.browseBean.getDocument();
   }

    @Override
    protected String getEmailTemplateXPath()
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        String xpath = Application.getRootPath(fc) + "/" + 
              Application.getGlossaryFolderName(fc) + "/" +
              Application.getEmailTemplatesFolderName(fc) + "/" + 
              Application.getNotifyEmailTemplatesFolderName(fc) + "//*";
        return xpath;
    }
}
