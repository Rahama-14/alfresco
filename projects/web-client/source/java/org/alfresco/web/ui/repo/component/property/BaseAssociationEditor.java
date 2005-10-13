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
package org.alfresco.web.ui.repo.component.property;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.DataDictionary;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * Base class for all association editor components 
 * 
 * @author gavinc
 */
public abstract class BaseAssociationEditor extends UIInput
{
   private final static Log logger = LogFactory.getLog(BaseAssociationEditor.class);
   
   private final static String ACTION_SEPARATOR = ";";
   private final static int ACTION_NONE   = -1;
   private final static int ACTION_REMOVE = 0;
   private final static int ACTION_SELECT = 1;
   private final static int ACTION_ADD = 2;
   private final static int ACTION_CHANGE = 3;
   private final static int ACTION_CANCEL = 4;
   private final static int ACTION_SEARCH = 5;
   private final static int ACTION_SET = 6;
   
   private final static String MSG_ERROR_ASSOC = "error_association";
   private final static String FIELD_CONTAINS = "_contains";
   private final static String FIELD_AVAILABLE = "_available";
   
   /** I18N message strings */
   private final static String MSG_ADD_TO_LIST_BUTTON = "add_to_list_button";
   private final static String MSG_SELECT_BUTTON = "select_button";
   private final static String MSG_SEARCH_SELECT_ITEMS = "search_select_items";
   private final static String MSG_SEARCH_SELECT_ITEM = "search_select_item";
   private final static String MSG_SELECTED_ITEMS = "selected_items";
   private final static String MSG_REMOVE = "remove";
   private final static String MSG_ADD = "add";
   private final static String MSG_OK = "ok";
   private final static String MSG_CANCEL = "cancel";
   private final static String MSG_SEARCH = "search";
   private final static String MSG_NONE = "none";
   private final static String MSG_CHANGE = "change";
   
   protected String associationName;
   protected String availableOptionsSize;
   protected String selectItemMsg;
   protected String selectItemsMsg;
   protected String selectedItemsMsg;
   protected Boolean disabled;
   
   protected boolean showAvailable = false;
   
   /** Map of the original associations keyed by the id of the child */
   protected Map<String, Object> originalAssocs;
   protected Map<String, Object> added;
   protected Map<String, Object> removed;
   
   /** List containing the currently available options */
   protected List<NodeRef> availableOptions;
   
   /** */
   protected String changingAssociation;
   
   // ------------------------------------------------------------------------------
   // Component implementation
   
   /**
    * Default constructor
    */
   public BaseAssociationEditor()
   {
      setRendererType(null);
   }
   
   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.associationName = (String)values[1];
      this.originalAssocs = (Map)values[2];
      this.availableOptions = (List)values[3];
      this.availableOptionsSize = (String)values[4];
      this.selectItemMsg = (String)values[5];
      this.selectItemsMsg = (String)values[6];
      this.selectedItemsMsg = (String)values[7];
      this.changingAssociation = (String)values[8];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[14];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.associationName;
      values[2] = this.originalAssocs;
      values[3] = this.availableOptions;
      values[4] = this.availableOptionsSize;
      values[5] = this.selectItemMsg;
      values[6] = this.selectItemsMsg;
      values[7] = this.selectedItemsMsg;
      values[8] = this.changingAssociation;
      
      // NOTE: we don't save the state of the added and removed maps as these
      //       need to be rebuilt everytime
      
      return (values);
   }

   /**
    * @see javax.faces.component.UIComponent#decode(javax.faces.context.FacesContext)
    */
   public void decode(FacesContext context)
   {
      Map requestMap = context.getExternalContext().getRequestParameterMap();
      Map valuesMap = context.getExternalContext().getRequestParameterValuesMap();
      String fieldId = getHiddenFieldName();
      String value = (String)requestMap.get(fieldId);
      
      int action = ACTION_NONE;
      String removeId = null;
      if (value != null && value.length() != 0)
      {
         // break up the action into it's parts
         int sepIdx = value.indexOf(ACTION_SEPARATOR);
         if (sepIdx != -1)
         {
            action = Integer.parseInt(value.substring(0, sepIdx));
            removeId = value.substring(sepIdx+1);
         }
         else
         {
            action = Integer.parseInt(value);
         }
      }
      
      // gather the current state and queue an event
      String[] addedItems = (String[])valuesMap.get(fieldId + FIELD_AVAILABLE);
      String contains = (String)requestMap.get(fieldId + FIELD_CONTAINS);
      
      AssocEditorEvent event = new AssocEditorEvent(this, action, addedItems, removeId, contains);
      queueEvent(event);
      
      super.decode(context);
   }

   /**
    * @see javax.faces.component.UIComponent#broadcast(javax.faces.event.FacesEvent)
    */
   public void broadcast(FacesEvent event) throws AbortProcessingException
   {
      if (event instanceof AssocEditorEvent)
      {
         AssocEditorEvent assocEvent = (AssocEditorEvent)event;
         Node node = (Node)getValue();
         
         switch (assocEvent.Action)
         {
            case ACTION_SEARCH:
            {
               this.showAvailable = true;
               this.availableOptions = new ArrayList<NodeRef>();
               getAvailableOptions(FacesContext.getCurrentInstance(), assocEvent.Contains);
               break;
            }
            case ACTION_SELECT:
            {
               this.showAvailable = true;
               break;
            }
            case ACTION_ADD:
            {
               addTarget(node, assocEvent.ToAdd);
               break;
            }
            case ACTION_REMOVE:
            {
               removeTarget(node, assocEvent.RemoveId);
               break;
            }
            case ACTION_CHANGE:
            {
               //removeTarget(node, assocEvent.RemoveId);
               this.changingAssociation = assocEvent.RemoveId;
               this.showAvailable = true;
               break;
            }
            case ACTION_CANCEL:
            {
               this.showAvailable = false;
               break;
            }
            case ACTION_SET:
            {
               if (assocEvent.ToAdd != null && assocEvent.ToAdd.length > 0)
               {
                  removeTarget(node, this.changingAssociation);
                  addTarget(node, assocEvent.ToAdd);
               }
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
    * @see javax.faces.component.UIComponent#encodeBegin(javax.faces.context.FacesContext)
    */
   public void encodeBegin(FacesContext context) throws IOException
   {
      if (isRendered() == false)
      {
         return;
      }
      
      ResponseWriter out = context.getResponseWriter();
      String clientId = getClientId(context);

      // get the child associations currently on the node and any that have been added
      NodeService nodeService = Repository.getServiceRegistry(context).getNodeService();
      
      // show the editable association component
      AssociationDefinition assocDef = getAssociationDefinition(context);
      if (assocDef == null)
      {
         logger.warn("Failed to find association definition for association '" + associationName + "'");
         
         // add an error message as the property is not defined in the data dictionary
         String msg = MessageFormat.format(Application.getMessage(context, MSG_ERROR_ASSOC), new Object[] {this.associationName});
         Utils.addErrorMessage(msg);
      }
      else
      {
         String targetType = assocDef.getTargetClass().getName().toString();
         boolean allowMany = assocDef.isTargetMany();
         
         populateAssocationMaps((Node)getValue());
         
         if (isDisabled())
         {
            // show the current list of associations in a read-only form
            renderReadOnlyAssociations(context, out, nodeService);
         }
         else
         {
            // start outer table
            out.write("<table border='0' cellspacing='4' cellpadding='0' class='selector'>");
            
            if (allowMany)
            {
               out.write("<tr><td colspan='2'>1.&nbsp;");
               out.write(getSelectItemsMsg());
               out.write("</td></tr>");
               
               // show the search field
               renderSearchField(context, out);
               
               // show available options for this association
               renderAvailableOptions(context, out, nodeService, targetType, allowMany);
               
               // add the Add to List button
               out.write("<tr><td colspan='2'>2.&nbsp;<input type='submit' value='");
               out.write(Application.getMessage(context, MSG_ADD_TO_LIST_BUTTON));
               out.write("' onclick=\"");
               out.write(generateFormSubmit(context, Integer.toString(ACTION_ADD)));
               out.write("\"/>");
               
               // add some padding
               out.write("<tr><td height='6'></td></tr>");
               
               out.write("<tr><td colspan='2'>");
               out.write(getSelectedItemsMsg());
               out.write(":</td></tr>");
               
               // show all the current associations
               renderExistingAssociations(context, out, nodeService, allowMany);
            }
            else
            {
               if (this.showAvailable)
               {
                  out.write("<tr><td colspan='2'>1.&nbsp;");
                  out.write(getSelectItemMsg());
                  out.write("</td></tr>");
               
                  // show the search field
                  renderSearchField(context, out);
                  
                  // show available options for this association 
                  renderAvailableOptions(context, out, nodeService, targetType, allowMany);
                  
                  // add the ok and cancel buttons
                  out.write("<tr><td colspan='2' align='right'><input type='submit' value='");
                  out.write(Application.getMessage(context, MSG_OK));
                  out.write("' onclick=\"");
                  out.write(generateFormSubmit(context, Integer.toString(ACTION_SET)));
                  out.write("\"/>&nbsp;&nbsp;<input type='submit' value='");
                  out.write(Application.getMessage(context, MSG_CANCEL));
                  out.write("' onclick=\"");
                  out.write(generateFormSubmit(context, Integer.toString(ACTION_CANCEL)));
                  out.write("\"/></td></tr>");
               }
               else
               {
                  // show the select button if required
                  if ((allowMany == false && this.originalAssocs.size() == 0 && this.added.size() == 0) ||
                      (allowMany == false && this.originalAssocs.size() == 1 && this.removed.size() == 1 && this.added.size() == 0) )
                  {
                     out.write("<tr><td><input type='submit' value='");
                     out.write(Application.getMessage(context, MSG_SELECT_BUTTON));
                     out.write("' onclick=\"");
                     out.write(generateFormSubmit(context, Integer.toString(ACTION_SELECT)));
                     out.write("\"/></td></tr>");
                  }
                  else
                  {
                     // show all the current associations
                     renderExistingAssociations(context, out, nodeService, allowMany);
                  }
               }
            }
            
            if (logger.isDebugEnabled())
            {
               logger.debug("number original = " + this.originalAssocs.size());
               logger.debug("number added = " + this.added.size());
               logger.debug("number removed = " + this.removed.size());
            }
            
            // close table
            out.write("</table>");
         }
      }
   }
   
   /**
    * Returns the name of the association this component is editing
    * 
    * @return Association name
    */
   public String getAssociationName()
   {
      ValueBinding vb = getValueBinding("associationName");
      if (vb != null)
      {
         this.associationName = (String)vb.getValue(getFacesContext());
      }
      
      return this.associationName;
   }

   /**
    * Sets the name of the association this component will edit
    * 
    * @param associationName Name of the association to edit
    */
   public void setAssociationName(String associationName)
   {
      this.associationName = associationName;
   }
   
   /**
    * Determines whether the component should be rendered in a disabled state
    * 
    * @return Returns whether the component is disabled
    */
   public boolean isDisabled()
   {
      if (this.disabled == null)
      {
         ValueBinding vb = getValueBinding("disabled");
         if (vb != null)
         {
            this.disabled = (Boolean)vb.getValue(getFacesContext());
         }
      }
      
      if (this.disabled == null)
      {
         this.disabled = Boolean.FALSE;
      }
      
      return this.disabled;
   }

   /**
    * Determines whether the component should be rendered in a disabled state
    * 
    * @param disabled true to disable the component
    */
   public void setDisabled(boolean disabled)
   {
      this.disabled = disabled;
   }
   
   /**
    * Returns the size of the select control when multiple items
    * can be selected 
    * 
    * @return The size of the select control
    */
   public String getAvailableOptionsSize()
   {
      if (this.availableOptionsSize == null)
      {
         this.availableOptionsSize = "4";
      }
      
      return this.availableOptionsSize;
   }
   
   /**
    * Sets the size of the select control used when multiple items can
    * be selected 
    * 
    * @param availableOptionsSize The size
    */
   public void setAvailableOptionsSize(String availableOptionsSize)
   {
      this.availableOptionsSize = availableOptionsSize;
   }
   
   /**
    * Returns the message to display for the selected items, if one hasn't been
    * set it defaults to the message in the bundle under key 'selected_items'.
    * 
    * @return The message
    */
   public String getSelectedItemsMsg()
   {
      ValueBinding vb = getValueBinding("selectedItemsMsg");
      if (vb != null)
      {
         this.selectedItemsMsg = (String)vb.getValue(getFacesContext());
      }
      
      if (this.selectedItemsMsg == null)
      {
         this.selectedItemsMsg = Application.getMessage(getFacesContext(), MSG_SELECTED_ITEMS);
      }
      
      return this.selectedItemsMsg;
   }

   /**
    * Sets the selected items message to display in the UI
    * 
    * @param selectedItemsMsg The message
    */
   public void setSelectedItemsMsg(String selectedItemsMsg)
   {
      this.selectedItemsMsg = selectedItemsMsg;
   }
   
   /**
    * Returns the message to display for select an item, if one hasn't been
    * set it defaults to the message in the bundle under key 'search_select_item'.
    * 
    * @return The message
    */
   public String getSelectItemMsg()
   {
      ValueBinding vb = getValueBinding("selectItemMsg");
      if (vb != null)
      {
         this.selectItemMsg = (String)vb.getValue(getFacesContext());
      }
      
      if (this.selectItemMsg == null)
      {
         this.selectItemMsg = Application.getMessage(getFacesContext(), MSG_SEARCH_SELECT_ITEM);
      }
      
      return this.selectItemMsg;
   }

   /**
    * Sets the select an item message to display in the UI
    * 
    * @param selectedItemMsg The message
    */
   public void setSelectItemMsg(String selectItemMsg)
   {
      this.selectItemMsg = selectItemMsg;
   }
   
   /**
    * Returns the message to display for select items, if one hasn't been
    * set it defaults to the message in the bundle under key 'search_select_items'.
    * 
    * @return The message
    */
   public String getSelectItemsMsg()
   {
      ValueBinding vb = getValueBinding("selectItemsMsg");
      if (vb != null)
      {
         this.selectItemsMsg = (String)vb.getValue(getFacesContext());
      }
      
      if (this.selectItemsMsg == null)
      {
         this.selectItemsMsg = Application.getMessage(getFacesContext(), MSG_SEARCH_SELECT_ITEMS);
      }
      
      return this.selectItemsMsg;
   }

   /**
    * Sets the select items message to display in the UI
    * 
    * @param selectedItemsMsg The message
    */
   public void setSelectItemsMsg(String selectItemsMsg)
   {
      this.selectItemsMsg = selectItemsMsg;
   }
   
   /**
    * Populates all the internal Maps with the appropriate association reference objects
    * 
    * @param node The Node we are dealing with
    */
   protected abstract void populateAssocationMaps(Node node);
   
   /**
    * Renders the existing associations in a read-only form 
    * 
    * @param context FacesContext
    * @param out ResponseWriter
    * @param nodeService The NodeService
    * @throws IOException
    */
   protected abstract void renderReadOnlyAssociations(FacesContext context, ResponseWriter out,
         NodeService nodeService) throws IOException;
   
   /**
    * Renders the existing associations in an editable form
    * 
    * @param context FacesContext
    * @param out ResponseWriter
    * @param nodeService The NodeService
    * @param allowMany Whether multiple associations are allowed 
    * @throws IOException
    */
   protected abstract void renderExistingAssociations(FacesContext context, ResponseWriter out, 
         NodeService nodeService, boolean allowMany) throws IOException;
   
   /**
    * Updates the component and node state to reflect an association being removed 
    * 
    * @param node The node we are dealing with
    * @param targetId The id of the child to remove
    */
   protected abstract void removeTarget(Node node, String targetId);

   /**
    * Updates the component and node state to reflect an association being added 
    * 
    * @param node The node we are dealing with
    * @param childId The id of the child to add
    */
   protected abstract void addTarget(Node node, String[] toAdd);
   
   /**
    * Renders an existing association with the appropriate options
    * 
    * @param context FacesContext
    * @param out Writer to write output to
    * @param nodeService The NodeService
    * @param targetRef The node at the end of the association being rendered
    * @param allowMany Whether the current association allows multiple children
    * @throws IOException
    */
   protected void renderExistingAssociation(FacesContext context, ResponseWriter out, NodeService nodeService,
         NodeRef targetRef, boolean allowMany) throws IOException
   {
      out.write("<tr><td>");

      out.write(Repository.getDisplayPath(nodeService.getPath(targetRef)));
      out.write("/");
      out.write(Repository.getNameForNode(nodeService, targetRef));
      if (allowMany == false)
      {
         out.write("</a>");
      }
      out.write("</td><td><a href='#' title='");
      out.write(Application.getMessage(context, MSG_REMOVE));
      out.write("' onclick=\"");
      out.write(generateFormSubmit(context, ACTION_REMOVE + ACTION_SEPARATOR + targetRef.getId()));
      out.write("\"><img src='");
      out.write(context.getExternalContext().getRequestContextPath());
      out.write("/images/icons/delete.gif' border='0' width='13' height='16'/></a>");
      
      if (allowMany == false)
      {
         out.write("&nbsp;<a href='#' title='");
         out.write(Application.getMessage(context, MSG_CHANGE));
         out.write("' onclick=\"");
         out.write(generateFormSubmit(context, ACTION_CHANGE + ACTION_SEPARATOR + targetRef.getId()));
         out.write("\"><img src='");
         out.write(context.getExternalContext().getRequestContextPath());
         out.write("/images/icons/edit_icon.gif' border='0' width='12' height='16'/></a>");
      }
      
      out.write("</td></tr>");
   }
   
   /**
    * Renders the search fields
    * 
    * @param context Faces Context
    * @param out The Response Writer
    * @throws IOException
    */
   protected void renderSearchField(FacesContext context, ResponseWriter out) throws IOException
   {
      // TODO: externalise the max and size attributes
      out.write("<tr><td colspan='2'><input type='text' maxlength='1024' size='32' name='");
      out.write(getClientId(context) + FIELD_CONTAINS);
      out.write("'/>&nbsp;&nbsp;<input type='submit' value='");
      out.write(Application.getMessage(context, MSG_SEARCH));
      out.write("' onclick=\"");
      out.write(generateFormSubmit(context, Integer.toString(ACTION_SEARCH)));
      out.write("\"/></td></tr>");
   }
   
   /**
    * Renders the <None> message
    * 
    * @param context Faces Context
    * @param out Response Writer
    * @throws IOException
    */
   protected void renderNone(FacesContext context, ResponseWriter out) throws IOException
   {
      out.write("<tr><td>&lt;");
      out.write(Application.getMessage(context, MSG_NONE));
      out.write("&gt;</td></tr>");
   }
   
   /**
    * Renders the list of available options for a new association
    * 
    * @param context FacesContext
    * @param out Writer to write output to
    * @param nodeService The NodeService
    * @param targetType The type of the child at the end of the association
    * @param allowMany Whether the current association allows multiple children
    * @throws IOException
    */
   protected void renderAvailableOptions(FacesContext context, ResponseWriter out, NodeService nodeService, 
         String targetType, boolean allowMany) throws IOException
   {
      boolean itemsPresent = (this.availableOptions != null && this.availableOptions.size() > 0);
      
      out.write("<tr><td colspan='2'><select ");
      if (itemsPresent == false)
      {
         // rather than having a very slim select box set the width if there are no results
         out.write("style='width:240px;' ");
      }
      out.write("name='");
      out.write(getClientId(context) + FIELD_AVAILABLE);
      out.write("' size='");
      if (allowMany)
      {
         out.write(getAvailableOptionsSize());
         out.write("' multiple");
      }
      else
      {
         out.write("1'");
      }
      out.write(">");
      
      if (itemsPresent)
      {
         Node currentNode = (Node)getValue();
         for (NodeRef item : this.availableOptions)
         {
            // NOTE: only show the items that are not already associated to and don't show the current node
            if ((this.originalAssocs.containsKey(item.getId()) == false && this.added.containsKey(item.getId()) == false &&
                item.getId().equals(currentNode.getId()) == false) || 
                this.removed.containsKey(item.getId())) 
            {
               out.write("<option value='");
               out.write(item.getId());
               out.write("'>");
               out.write(Repository.getDisplayPath(nodeService.getPath(item)));
               out.write("/");
               out.write(Repository.getNameForNode(nodeService, item));
               out.write("</option>");
            }
         }
      }
      
      out.write("</select></td></tr>");
   }   
   
   /**
    * Retrieves the AssociationDefinition for the association we are representing
    * 
    * @param context Faces Context
    * @return The AssociationDefinition for the association, null if a definition does not exist
    */
   protected AssociationDefinition getAssociationDefinition(FacesContext context)
   {
      // get some metadata about the association from the data dictionary
      DataDictionary dd = (DataDictionary)FacesContextUtils.getRequiredWebApplicationContext(
               context).getBean(Application.BEAN_DATA_DICTIONARY);
      return dd.getAssociationDefinition((Node)getValue(), this.associationName);
   }
   
   /**
    * Retrieves the available options for the current association
    * 
    * @param context Faces Context
    * @param contains The contains part of the query
    */
   protected void getAvailableOptions(FacesContext context, String contains)
   {
      AssociationDefinition assocDef = getAssociationDefinition(context);
      if (assocDef != null)
      {
         // find and show all the available options for the current association
         StringBuilder query = new StringBuilder("+TYPE:\"");
         query.append(assocDef.getTargetClass().getName().toString());
         query.append("\"");
         
         if (contains != null && contains.length() > 0)
         {
            String safeContains = Utils.remove(contains.trim(), "\"");
            String nameAttr = Repository.escapeQName(QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "name"));
            
            query.append(" AND +@");
            query.append(nameAttr);
            query.append(":*" + safeContains + "*");
         }
         
         if (logger.isDebugEnabled())
            logger.debug("Query: " + query.toString());
          
         ResultSet results = Repository.getServiceRegistry(context).getSearchService().query(
               Repository.getStoreRef(), SearchService.LANGUAGE_LUCENE, query.toString());
         this.availableOptions = results.getNodeRefs();
         
         if (logger.isDebugEnabled())
            logger.debug("Found " + this.availableOptions.size() + " available options");
      }
   }
   
   /**
    * We use a hidden field per picker instance on the page.
    * 
    * @return hidden field name
    */
   private String getHiddenFieldName()
   {
      return getClientId(getFacesContext());
   }
   
   /**
    * Generate FORM submit JavaScript for the specified action
    *  
    * @param context    FacesContext
    * @param action     Action string
    * 
    * @return FORM submit JavaScript
    */
   private String generateFormSubmit(FacesContext context, String action)
   {
      return Utils.generateFormSubmit(context, this, getHiddenFieldName(), action);
   }
   
   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Class representing an action relevant to the AssociationEditor component.
    */
   public static class AssocEditorEvent extends ActionEvent
   {
      public int Action;
      public String[] ToAdd;
      public String RemoveId;
      public String Contains;
      
      public AssocEditorEvent(UIComponent component, int action, String[] toAdd, String removeId, String contains)
      {
         super(component);
         this.Action = action;
         this.ToAdd = toAdd;
         this.RemoveId = removeId;
         this.Contains = contains;
      }
   }
}
