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
package org.alfresco.web.bean.coci;

import java.io.Serializable;
import java.util.Map;
import java.util.StringTokenizer;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.Utils;

/**
 * This bean class handle done-editing(commit) dialog.
 *
 */
public class DoneEditingDialog extends CheckinCheckoutDialog
{

   private final static String MSG_DONE = "done";
   private static final String MSG_CHECK_IN = "check_in";
   private final static String MSG_MISSING_ORIGINAL_NODE = "missing_original_node";

   private final static String DIALOG_NAME = AlfrescoNavigationHandler.DIALOG_PREFIX + "doneEditingFile";

   /**
    * this flag indicates occurrence when source node isn't versionable, but working copy yet is versionable
    */
   private boolean sourceVersionable;

   /**
    * this field contains reference to source node for working copy
    */
   private NodeRef sourceNodeRef;

   /**
    * @return Returns label for new version with major changes
    */
   public String getMajorNewVersionLabel()
   {
      String label = getCurrentVersionLabel();
      StringTokenizer st = new StringTokenizer(label, ".");
      return (Integer.valueOf(st.nextToken()) + 1) + ".0";
   }

   /**
    * @return Returns label for new version with minor changes
    */
   public String getMinorNewVersionLabel()
   {
      String label = getCurrentVersionLabel();
      StringTokenizer st = new StringTokenizer(label, ".");
      return st.nextToken() + "." + (Integer.valueOf(st.nextToken()) + 1);
   }

   /**
    * @return Returns flag, which indicates occurrence when source node is versionable
    */
   public boolean isSourceVersionable()
   {
      return sourceVersionable;
   }

   /**
    * @return Returns true if source node for selected working copy founded
    */
   public boolean isSourceFounded()
   {
      return (sourceNodeRef != null);
   }

   /**
    * Method for handling done-editing action(e.g. "done_editing_doc")
    * @param event Action Event
    */
   public void handle(ActionEvent event)
   {
      setupContentAction(event);

      FacesContext fc = FacesContext.getCurrentInstance();
      NavigationHandler nh = fc.getApplication().getNavigationHandler();
      // if content is versionable then check-in else move to dialog for filling version info
      if (isVersionable())
      {
         nh.handleNavigation(fc, null, DIALOG_NAME);
      }
      else
      {
         checkinFileOK(fc, null);
         nh.handleNavigation(fc, null, AlfrescoNavigationHandler.DIALOG_PREFIX + "browse");
      }
   }

   @Override
   public void setupContentAction(ActionEvent event)
   {
      super.setupContentAction(event);

      Node node = property.getDocument();
      if (node != null)
      {
         sourceNodeRef = getSourceNodeRef(node.getNodeRef());
         if (sourceNodeRef != null)
            sourceVersionable = getNodeService().hasAspect(sourceNodeRef, ContentModel.ASPECT_VERSIONABLE);
      }
   }

   @Override
   public String getFinishButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), MSG_DONE);
   }

   @Override
   public boolean getFinishButtonDisabled()
   {
      return !isSourceFounded();
   }

   @Override
   public String getContainerTitle()
   {
      if (isSourceFounded())
      {
         return Application.getMessage(FacesContext.getCurrentInstance(), MSG_CHECK_IN) + " '" + getNodeService().getProperty(sourceNodeRef, ContentModel.PROP_NAME) + "'";
      }
      else
      {
         String message = Application.getMessage(FacesContext.getCurrentInstance(), MSG_MISSING_ORIGINAL_NODE);
         Utils.addErrorMessage(message);
         return message;
      }
   }

   @Override
   public void resetState()
   {
      super.resetState();

      sourceVersionable = false;
      sourceNodeRef = null;
   }

   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      return checkinFileOK(context, outcome);
   }

   /**
    * @return Returns version label for source node for working copy. Null indicates error
    */
   private String getCurrentVersionLabel()
   {
      if (isSourceFounded())
      {
         Version curVersion = property.getVersionQueryService().getCurrentVersion(sourceNodeRef);
         return curVersion.getVersionLabel();
      }

      return null;
   }

   /**
    * @param workingCopyNodeRef node reference to working copy
    * @return Returns node reference to node, which is source for working copy node. Null indicates error
    */
   private NodeRef getSourceNodeRef(NodeRef workingCopyNodeRef)
   {
      if (getNodeService().hasAspect(workingCopyNodeRef, ContentModel.ASPECT_COPIEDFROM) == true)
      {
         Map<QName, Serializable> workingCopyProperties = getNodeService().getProperties(workingCopyNodeRef);
         return (NodeRef) workingCopyProperties.get(ContentModel.PROP_COPY_REFERENCE);
      }

      return null;
   }

}
