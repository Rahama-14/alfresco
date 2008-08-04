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
package org.alfresco.web.bean.forums;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Bean implementation of the "Create Reply Dialog".
 * 
 * @author gavinc
 */
public class CreateReplyDialog extends CreatePostDialog
{
   private static final long serialVersionUID = 8036934269090933533L;

   protected String replyContent = null;
   
   private static final Log logger = LogFactory.getLog(CreateReplyDialog.class);

   // ------------------------------------------------------------------------------
   // Wizard implementation
   
   @Override
   public void init(Map<String, String> parameters)
   {
      super.init(parameters);
      
      this.replyContent = null;
   }
   
   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      // remove link breaks and replace with <br>
      this.content = Utils.replaceLineBreaks(this.content, false);
      
      super.finishImpl(context, outcome);
      
      // setup the referencing aspect with the references association
      // between the new post and the one being replied to
      this.getNodeService().addAspect(this.createdNode, ContentModel.ASPECT_REFERENCING, null);
      this.getNodeService().createAssociation(this.createdNode, this.browseBean.getDocument().getNodeRef(), 
            ContentModel.ASSOC_REFERENCES);
      
      if (logger.isDebugEnabled())
      {
         logger.debug("created new node: " + this.createdNode);
         logger.debug("existing node: " + this.browseBean.getDocument().getNodeRef());
      }
      
      return outcome;
   }
   
   @Override
   public String getFinishButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), "reply");
   }
}
