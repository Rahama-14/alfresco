/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.web.ui.repo.component.shelf;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.clipboard.ClipboardItem;
import org.alfresco.web.bean.clipboard.ClipboardStatus;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.WebResources;


/**
 * @author Kevin Roast
 */
public class UIClipboardShelfItem extends UIShelfItem
{
   // ------------------------------------------------------------------------------
   // Component Impl

   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.collections = (List)values[1];
      this.pasteActionListener = (MethodBinding)values[2];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[3];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.collections;
      values[2] = this.pasteActionListener;
      return values;
   }
   
   /**
    * @see javax.faces.component.UIComponentBase#decode(javax.faces.context.FacesContext)
    */
   public void decode(FacesContext context)
   {
      Map requestMap = context.getExternalContext().getRequestParameterMap();
      String fieldId = getHiddenFieldName();
      String value = (String)requestMap.get(fieldId);
      
      if (value != null && value.length() != 0)
      {
         // decode the values - we are expecting an action identifier and an index
         int sepIndex = value.indexOf(NamingContainer.SEPARATOR_CHAR);
         int action = Integer.parseInt(value.substring(0, sepIndex));
         int index = Integer.parseInt(value.substring(sepIndex + 1));
         
         // raise an event to process the action later in the lifecycle
         ClipboardEvent event = new ClipboardEvent(this, action, index);
         this.queueEvent(event);
      }
   }
   
   /**
    * @see javax.faces.component.UIComponentBase#broadcast(javax.faces.event.FacesEvent)
    */
   public void broadcast(FacesEvent event) throws AbortProcessingException
   {
      if (event instanceof ClipboardEvent)
      {
         // found an event we should handle
         ClipboardEvent clipEvent = (ClipboardEvent)event;
         
         List<ClipboardItem> items = getCollections();
         if (items.size() > clipEvent.Index)
         {
            // process the action
            switch (clipEvent.Action)
            {
               case ACTION_REMOVE_ALL:
                  items.clear();
                  break;
               
               case ACTION_REMOVE_ITEM:
                  items.remove(clipEvent.Index);
                  break;
               
               case ACTION_PASTE_ALL:
               case ACTION_PASTE_ITEM:
                  Utils.processActionMethod(getFacesContext(), getPasteActionListener(), clipEvent);
                  break;
            }
         }
      }
      else
      {
         super.broadcast(event);
      }
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
      
      ResponseWriter out = context.getResponseWriter();
      
      List<ClipboardItem> items = getCollections();
      out.write(SHELF_START);
      if (items.size() != 0)
      {
         DictionaryService dd = Repository.getServiceRegistry(context).getDictionaryService();
         
         ResourceBundle bundle = Application.getBundle(context);
         
         for (int i=0; i<items.size(); i++)
         {
            ClipboardItem item = items.get(i);
            
            // start row with cut/copy state icon
            out.write("<tr><td>");
            if (item.Mode == ClipboardStatus.COPY)
            {
               out.write(Utils.buildImageTag(context, WebResources.IMAGE_COPY, 14, 16, bundle.getString(MSG_COPY), null, "absmiddle"));
            }
            else
            {
               out.write(Utils.buildImageTag(context, WebResources.IMAGE_CUT, 13, 16, bundle.getString(MSG_CUT), null, "absmiddle"));
            }
            out.write("</td><td>");
            
            if (dd.isSubClass(item.Node.getType(), ContentModel.TYPE_FOLDER))
            {
               // start row with Space icon
               out.write(Utils.buildImageTag(context, WebResources.IMAGE_SPACE, 16, 16, null, null, "absmiddle"));
            }
            else if (dd.isSubClass(item.Node.getType(), ContentModel.TYPE_CONTENT))
            {
               String image = Repository.getFileTypeImage(item.Node, true);
               out.write(Utils.buildImageTag(context, image, 16, 16, null, null, "absmiddle"));
            }
            
            // output cropped item label - we also output with no breaks, this is ok
            // as the copped label will ensure a sensible maximum width
            out.write("</td><td width=100%><nobr>&nbsp;");
            out.write(Utils.cropEncode(item.Node.getName()));
            
            // output actions
            out.write("</nobr></td><td align=right><nobr>");
            out.write(buildActionLink(ACTION_REMOVE_ITEM, i, bundle.getString(MSG_REMOVE_ITEM), WebResources.IMAGE_REMOVE));
            out.write("&nbsp;");
            out.write(buildActionLink(ACTION_PASTE_ITEM, i, bundle.getString(MSG_PASTE_ITEM), WebResources.IMAGE_PASTE));
            
            // end actions cell and end row
            out.write("</nobr></td></tr>");
         }
         
         // output general actions if any clipboard items are present
         out.write("<tr><td colspan=3><nobr>");
         out.write(buildActionLink(ACTION_PASTE_ALL, -1, bundle.getString(MSG_PASTE_ALL), null));
         out.write("&nbsp;");
         out.write(buildActionLink(ACTION_REMOVE_ALL, -1, bundle.getString(MSG_REMOVE_ALL), null));
         out.write("</nobr></td><td></td></tr>");
      }
      
      out.write(SHELF_END);
   }
   
   
   // ------------------------------------------------------------------------------
   // Strongly typed component property accessors 
   
   /**
    * @param collections   Set the clipboard item collections to use
    */
   public void setCollections(List<ClipboardItem> collections)
   {
      this.collections = collections;
   }
   
   /**
    * @return The clipboard item collections to use
    */
   public List<ClipboardItem> getCollections()
   {
      ValueBinding vb = getValueBinding("collections");
      if (vb != null)
      {
         this.collections = (List<ClipboardItem>)vb.getValue(getFacesContext());
      }
      
      return this.collections;
   }
   
   /** 
    * @param binding    The MethodBinding to call when Paste is selected by the user
    */
   public void setPasteActionListener(MethodBinding binding)
   {
      this.pasteActionListener = binding;
   }
   
   /** 
    * @return The MethodBinding to call when Paste is selected by the user
    */
   public MethodBinding getPasteActionListener()
   {
      return this.pasteActionListener;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private helpers
   
   /**
    * We use a hidden field name on the assumption that only one clipboard shelf item instance
    * is present on a single page.
    * 
    * @return hidden field name
    */
   private String getHiddenFieldName()
   {
      return getClientId(getFacesContext());
   }
   
   /**
    * Encode the specified values for output to a hidden field
    * 
    * @param action     Action identifer
    * @param index      Index of the clipboard item the action is for
    * 
    * @return encoded values
    */
   private static String encodeValues(int action, int index)
   {
      return Integer.toString(action) + NamingContainer.SEPARATOR_CHAR + Integer.toString(index);
   }
   
   /**
    * Build HTML for an link representing a clipboard action
    * 
    * @param action     action indentifier to represent
    * @param index      index of the clipboard item this action relates too
    * @param text       of the action to display
    * @param image      image icon to display
    * 
    * @return HTML for action link
    */
   private String buildActionLink(int action, int index, String text, String image)
   {
      FacesContext context = getFacesContext(); 
      
      StringBuilder buf = new StringBuilder(256);
      
      buf.append("<a href='#' onclick=\"");
      // generate JavaScript to set a hidden form field and submit
      // a form which request attributes that we can decode
      buf.append(Utils.generateFormSubmit(context, this, getHiddenFieldName(), encodeValues(action, index)));
      buf.append("\">");
      
      if (image != null)
      {
         buf.append(Utils.buildImageTag(context, image, text));
      }
      else
      {
         buf.append(Utils.encode(text));
      }
      
      buf.append("</a>");
      
      return buf.toString();
   }
   
   
   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Class representing the an action relevant to the Clipboard element.
    */
   public static class ClipboardEvent extends ActionEvent
   {
      public ClipboardEvent(UIComponent component, int action, int index)
      {
         super(component);
         Action = action;
         Index = index;
      }
      
      public int Action;
      public int Index;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data
   
   /** I18N messages */
   private static final String MSG_REMOVE_ALL = "remove_all";
   private static final String MSG_PASTE_ALL = "paste_all";
   private static final String MSG_PASTE_ITEM = "paste_item";
   private static final String MSG_REMOVE_ITEM = "remove_item";
   private static final String MSG_CUT = "cut";
   private static final String MSG_COPY = "copy";
   
   private final static int ACTION_REMOVE_ITEM = 0;
   private final static int ACTION_REMOVE_ALL = 1;
   private final static int ACTION_PASTE_ITEM = 2;
   private final static int ACTION_PASTE_ALL = 3;
   
   /** the current list of clipboard items */
   private List<ClipboardItem> collections;
   
   /** action listener called when a paste action occurs */
   private MethodBinding pasteActionListener;
}
