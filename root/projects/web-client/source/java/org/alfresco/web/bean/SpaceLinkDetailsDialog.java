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
package org.alfresco.web.bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.NavigationSupport;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.component.UIActionLink;

public class SpaceLinkDetailsDialog extends BaseDetailsBean implements NavigationSupport
{
   private static final long serialVersionUID = 8372741472120796169L;
   
   private static final String MSG_DETAILS_OF = "details_of";
   private static final String MSG_LOCATION = "location";
   private final static String MSG_CLOSE = "close";
   private final static String MSG_LEFT_QUOTE = "left_qoute";
   private final static String MSG_RIGHT_QUOTE = "right_quote";
   
   /**
    * Returns the Space this bean is currently representing
    * 
    * @return The Space Node
    */
   public Node getSpace()
   {
      return getNode();
   }

   @Override
   protected Node getLinkResolvedNode()
   {
      Node space = getSpace();
      if (ApplicationModel.TYPE_FOLDERLINK.equals(space.getType()))
      {
         NodeRef destRef = (NodeRef) space.getProperties().get(ContentModel.PROP_LINK_DESTINATION);
         if (getNodeService().exists(destRef))
         {
            space = new Node(destRef);
         }
      }
      return space;
   }

   @Override
   public Node getNode()
   {
      return this.browseBean.getActionSpace();
   }

   @Override
   protected String getPropertiesPanelId()
   {
      return "space-props";
   }

   @Override
   public Map getTemplateModel()
   {
      HashMap model = new HashMap(1, 1.0f);

      model.put("space", getSpace().getNodeRef());
      model.put(TemplateService.KEY_IMAGE_RESOLVER, imageResolver);

      return model;
   }

   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      return null;
   }

   public String getCurrentItemId()
   {
      return getId();
   }

   public void nextItem(ActionEvent event)
   {
      boolean foundNextItem = false;
      UIActionLink link = (UIActionLink) event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         NodeRef currNodeRef = new NodeRef(Repository.getStoreRef(), id);
         List<Node> nodes = this.browseBean.getParentNodes(currNodeRef);
         if (nodes.size() > 1)
         {
            // perform a linear search - this is slow but stateless
            // otherwise we would have to manage state of last selected node
            // this gets very tricky as this bean is instantiated once and never
            // reset - it does not know when the document has changed etc.
            for (int i = 0; i < nodes.size(); i++)
            {
               if (id.equals(nodes.get(i).getId()) == true)
               {
                  Node next;
                  // found our item - navigate to next
                  if (i != nodes.size() - 1)
                  {
                     next = nodes.get(i + 1);
                  }
                  else
                  {
                     // handle wrapping case
                     next = nodes.get(0);
                  }

                  // prepare for showing details for this node
                  this.browseBean.setupSpaceAction(next.getId(), false);

                  // we found a next item
                  foundNextItem = true;
               }
            }
         }

         // if we did not find a next item make sure the current node is
         // in the dispatch context otherwise the details screen will go back
         // to the default one.
         if (foundNextItem == false)
         {
            Node currNode = new Node(currNodeRef);
            this.navigator.setupDispatchContext(currNode);
         }
      }

   }

   public void previousItem(ActionEvent event)
   {
      boolean foundPreviousItem = false;
      UIActionLink link = (UIActionLink) event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         NodeRef currNodeRef = new NodeRef(Repository.getStoreRef(), id);
         List<Node> nodes = this.browseBean.getParentNodes(currNodeRef);
         if (nodes.size() > 1)
         {
            // see above
            for (int i = 0; i < nodes.size(); i++)
            {
               if (id.equals(nodes.get(i).getId()) == true)
               {
                  Node previous;
                  // found our item - navigate to previous
                  if (i != 0)
                  {
                     previous = nodes.get(i - 1);
                  }
                  else
                  {
                     // handle wrapping case
                     previous = nodes.get(nodes.size() - 1);
                  }

                  // show details for this node
                  this.browseBean.setupSpaceAction(previous.getId(), false);

                  // we found a next item
                  foundPreviousItem = true;
               }
            }
         }

         // if we did not find a previous item make sure the current node is
         // in the dispatch context otherwise the details screen will go back
         // to the default one.
         if (foundPreviousItem == false)
         {
            Node currNode = new Node(currNodeRef);
            this.navigator.setupDispatchContext(currNode);
         }
      }
   }
   
   public String getCancelButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), MSG_CLOSE);
   }

   @Override
   public String getContainerSubTitle()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), MSG_LOCATION) + ": " + 
             getSpace().getNodePath().toDisplayPath(getNodeService(), getPermissionService());
   }

   public String getContainerTitle()
   {
       FacesContext fc = FacesContext.getCurrentInstance();
       return Application.getMessage(fc, MSG_DETAILS_OF) + " " + Application.getMessage(fc, MSG_LEFT_QUOTE) + getName() + Application.getMessage(fc, MSG_RIGHT_QUOTE);
   }
   
   public String getOutcome()
   {
      return "dialog:close:dialog:showSpaceDetails";
   }

   public String cancel()
   {
      this.navigator.resetCurrentNodeProperties();
      return super.cancel();
   }
}
