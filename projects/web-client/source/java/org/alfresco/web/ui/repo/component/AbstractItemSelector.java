/*
 * Created on 25-May-2005
 */
package org.alfresco.web.ui.repo.component;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.transaction.UserTransaction;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;

/**
 * Abstract component to allow the selection of a hierarchical item
 * 
 * @author gavinc
 */
public abstract class AbstractItemSelector extends UIInput
{
   protected final static String OPTION = "_option";
   
   protected final static int MODE_BEFORE_SELECTION = 0;
   protected final static int MODE_PERFORM_SELECTION = 1;
   protected final static int MODE_AFTER_SELECTION = 2;
   
   /** label to be displayed before a space is selected */
   protected String label = null;
   
   /** cellspacing between options */
   protected Integer spacing = null;
   
   /** what mode the component is in */
   protected int mode = MODE_BEFORE_SELECTION;
   
   /** currently browsing node id */
   protected String navigationId = null;
   
   // ------------------------------------------------------------------------------
   // Component Impl 
   
   /**
    * Default constructor
    */
   public AbstractItemSelector()
   {
      // set the default renderer
      setRendererType(null);
   }
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public abstract String getFamily();
   
   /**
    * Retrieves the id of the parent node of the current navigation node
    * 
    * @param context The Faces context
    * @return Id of the parent node or null if the parent is the root
    */
   public abstract String getParentNodeId(FacesContext context);
   
   /**
    * Returns a collection of child associations for the current navigation node 
    * 
    * @param context The Faces context
    * @return The children
    */
   public abstract Collection<ChildAssociationRef> getChildrenForNode(FacesContext context);
   
   /**
    * Returns a collection of child associations of the root
    * 
    * @param context The Faces context
    * @return The root options
    */
   public abstract Collection<ChildAssociationRef> getRootChildren(FacesContext context);
   
   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.label = (String)values[1];
      this.spacing = (Integer)values[2];
      this.mode = ((Integer)values[3]).intValue();
      this.navigationId = (String)values[4];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[5];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.label;
      values[2] = this.spacing;
      values[3] = this.mode;
      values[4] = this.navigationId;
      return (values);
   }
   
   /**
    * @see javax.faces.component.UIComponentBase#decode(javax.faces.context.FacesContext)
    */
   public void decode(FacesContext context)
   {
      Map requestMap = context.getExternalContext().getRequestParameterMap();
      String fieldId = getHiddenFieldName();
      String value = (String)requestMap.get(fieldId);
      
      int mode = this.mode;
      if (value != null && value.length() != 0)
      {
         // break up the submitted value into it's parts
         
         // first part is the mode the component is in
         // followed by the id of the selection if we are drilling down
         String id = null;
         int sepIndex = value.indexOf(NamingContainer.SEPARATOR_CHAR);
         if (sepIndex != -1)
         {
            mode = Integer.parseInt(value.substring(0, sepIndex));
            if (value.length() > sepIndex + 1)
            {
               id = value.substring(sepIndex + 1);
            }
         }
         else
         {
            mode = Integer.parseInt(value);
         }
         
         // raise an event so we can pick the changed values up later
         ItemSelectorEvent event = new ItemSelectorEvent(this, mode, id); 
         this.queueEvent(event);
      }
      
      if (mode == MODE_AFTER_SELECTION)
      {
         // only bother to check the selection if the mode is set to END_SELECTION
         // see if a selection has been submitted
         String selection = (String)requestMap.get(getClientId(context) + OPTION);
         if (selection != null && selection.length() != 0)
         {
            ((EditableValueHolder)this).setSubmittedValue(selection);
         }
      }
   }
   
   /**
    * @see javax.faces.component.UIInput#broadcast(javax.faces.event.FacesEvent)
    */
   public void broadcast(FacesEvent event) throws AbortProcessingException
   {
      if (event instanceof ItemSelectorEvent)
      {
         ItemSelectorEvent spaceEvent = (ItemSelectorEvent)event;
         this.mode = spaceEvent.Mode;
         this.navigationId = spaceEvent.Id;
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
      
      String clientId = getClientId(context);
      
      StringBuilder buf = new StringBuilder(512);
      Map attrs = this.getAttributes();
      
      switch (this.mode)
      {
         case MODE_BEFORE_SELECTION:
         case MODE_AFTER_SELECTION:
         {
            String valueId = null;
            String submittedValue = (String)getSubmittedValue();
            if (submittedValue != null)
            {
               valueId = submittedValue;
            }
            else
            {
               valueId = (String)getValue();
            }
            
            // show just the initial or current selection link
            String label;
            if (valueId == null)
            {
               label = getLabel();
            }
            else
            {
               NodeRef nodeRef = new NodeRef(Repository.getStoreRef(), valueId);
               label = Repository.getNameForNode(getNodeService(context), nodeRef);
            }
            
            // output surrounding span for style purposes
            buf.append("<span");
            if (attrs.get("style") != null)
            {
               buf.append(" style=\"")
                  .append(attrs.get("style"))
                  .append('"');
            }
            if (attrs.get("styleClass") != null)
            {
               buf.append(" class=")
                  .append(attrs.get("styleClass"));
            }
            buf.append(">");
            
            // field value is whether we are picking and the current or parent Id value
            String fieldValue = encodeFieldValues(MODE_PERFORM_SELECTION, valueId);
            buf.append("<a href='#' onclick=\"");
            buf.append(Utils.generateFormSubmit(context, this, getHiddenFieldName(), fieldValue));
            buf.append('"');
            if (attrs.get("nodeStyle") != null)
            {
               buf.append(" style=\"")
                  .append(attrs.get("nodeStyle"))
                  .append('"');
            }
            if (attrs.get("nodeStyleClass") != null)
            {
               buf.append(" class=")
                  .append(attrs.get("nodeStyleClass"));
            }
            buf.append(">")
               .append(label)
               .append("</a></span>");
            
            break;
         }
         
         case MODE_PERFORM_SELECTION:
         {
            // show the picker list
            // get the children of the node ref to show
            NodeService service = getNodeService(context);
            UserTransaction tx = null;
            try
            {
               tx = Repository.getUserTransaction(context);
               tx.begin();
               
               buf.append("<table border=0 cellspacing=1 cellpadding=1");
               if (attrs.get("style") != null)
               {
                  buf.append(" style=\"")
                     .append(attrs.get("style"))
                     .append('"');
               }
               if (attrs.get("styleClass") != null)
               {
                  buf.append(" class=")
                     .append(attrs.get("styleClass"));
               }
               buf.append(">");
               
               // render "Go Up" link
               if (this.navigationId != null)
               {                  
                  buf.append("<tr><td></td><td>");
                  
                  // get the id of the parent node of the current navigation node,
                  // null means we are at the root level
                  String id = getParentNodeId(context);
                  
                  // render a link to the parent node
                  renderNodeLink(context, id, "Go Up", buf);
                  buf.append("</td></tr>");
               }
               
               // display the children of the specified navigation node ID
               if (this.navigationId != null)
               {
                  // get a list of children for the current navigation node
                  Collection<ChildAssociationRef> childRefs = getChildrenForNode(context);
                  for (ChildAssociationRef childRef : childRefs)
                  {
                     // render each child found
                     String childId = childRef.getChildRef().getId();
                     buf.append("<tr><td><input type='radio' name='")
                        .append(clientId).append(OPTION).append("' value='")
                        .append(childId).append("'/></td><td>");
                     
                     // get the name for the child (rather than association name)
                     NodeRef childNodeRef = new NodeRef(Repository.getStoreRef(), childId);
                     String name = Repository.getNameForNode(service, childNodeRef);
                     renderNodeLink(context, childId, name, buf);
                     buf.append("</td></tr>");
                  }
               }
               else
               {
                  // no node set - special case so show the root items
                  Collection<ChildAssociationRef> childRefs = getRootChildren(context);
                  for (ChildAssociationRef childRef : childRefs)
                  {
                     // render each root category found
                     String childId = childRef.getChildRef().getId();
                     buf.append("<tr><td><input type='radio' name='")
                        .append(clientId).append(OPTION).append("' value='")
                        .append(childId).append("'/></td><td>");
                     
                     // get the name for the child (rather than association name)
                     NodeRef childNodeRef = new NodeRef(Repository.getStoreRef(), childId);
                     String name = Repository.getNameForNode(service, childNodeRef);
                     renderNodeLink(context, childId, name, buf);
                     buf.append("</td></tr>");
                  }
               }
               
               // render OK button
               String fieldValue = encodeFieldValues(MODE_AFTER_SELECTION, null);
               buf.append("<tr><td></td><td align=center>")
                  .append("<input type='button' onclick=\"")
                  .append(Utils.generateFormSubmit(context, this, getHiddenFieldName(), fieldValue))
                  .append("\" value='OK'></td></tr>");
               
               buf.append("</table>");
               
               tx.commit();
            }
            catch (Throwable err)
            {
               try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
               throw new RuntimeException(err);
            }
            
            break;
         }
      }
      
      context.getResponseWriter().write(buf.toString());
   }
   
   
   // ------------------------------------------------------------------------------
   // Strongly typed component property accessors
   
   /**
    * @return Returns the label.
    */
   public String getLabel()
   {
      ValueBinding vb = getValueBinding("label");
      if (vb != null)
      {
         this.label = (String)vb.getValue(getFacesContext());
      }
      
      return this.label;
   }
   
   /**
    * @param label The label to set.
    */
   public void setLabel(String label)
   {
      this.label = label;
   }

   /**
    * @return Returns the cell spacing value between space options. Default is 2.
    */
   public Integer getSpacing()
   {
      ValueBinding vb = getValueBinding("spacing");
      if (vb != null)
      {
         this.spacing = (Integer)vb.getValue(getFacesContext());
      }
      
      if (this.spacing != null)
      {
         return this.spacing.intValue();
      }
      else
      {
         // return default
         return 2;
      }
   }
   
   /**
    * @param spacing The spacing to set.
    */
   public void setSpacing(Integer spacing)
   {
      this.spacing = spacing;
   }
   
   
   // ------------------------------------------------------------------------------
   // Protected helpers
   
   /**
    * We use a unique hidden field name based on our client Id.
    * This is on the assumption that there won't be many selectors on screen at once!
    * Also means we have less values to decode on submit.
    * 
    * @return hidden field name
    */
   protected String getHiddenFieldName()
   {
      return this.getClientId(getFacesContext());
   }
   
   protected String encodeFieldValues(int mode, String id)
   {
      if (id != null)
      {
         return Integer.toString(mode) + NamingContainer.SEPARATOR_CHAR + id;
      }
      else
      {
         return Integer.toString(mode);
      }
   }
   
   /**
    * Render a node descendant as a clickable link
    * 
    * @param context    FacesContext
    * @param childRef   The ChildAssocRef of the child to render an HTML link for
    *  
    * @return HTML for a descendant link
    */
   protected String renderNodeLink(FacesContext context, String id, String name, StringBuilder buf)
   {
      buf.append("<a href='#' onclick=\"");
      String fieldValue = encodeFieldValues(MODE_PERFORM_SELECTION, id);
      buf.append(Utils.generateFormSubmit(context, this, getHiddenFieldName(), fieldValue));
      buf.append('"');
      Map attrs = this.getAttributes();
      if (attrs.get("nodeStyle") != null)
      {
         buf.append(" style=\"")
            .append(attrs.get("nodeStyle"))
            .append('"');
      }
      if (attrs.get("nodeStyleClass") != null)
      {
         buf.append(" class=")
            .append(attrs.get("nodeStyleClass"));
      }
      buf.append('>');

      buf.append(Utils.encode(name));
      
      buf.append("</a>");
      
      return buf.toString();
   }
   
   /**
    * Use Spring JSF integration to return the node service bean instance
    * 
    * @param context    FacesContext
    * 
    * @return node service bean instance or throws runtime exception if not found
    */
   protected static NodeService getNodeService(FacesContext context)
   {
      NodeService service = Repository.getServiceRegistry(context).getNodeService();
      if (service == null)
      {
         throw new IllegalStateException("Unable to obtain NodeService bean reference.");
      }
      
      return service;
   }
   
   /**
    * Class representing the clicking of a breadcrumb element.
    */
   public static class ItemSelectorEvent extends ActionEvent
   {
      public ItemSelectorEvent(UIComponent component, int mode, String id)
      {
         super(component);
         Mode = mode;
         Id = id;
      }
      
      public int Mode;
      private String Id;
   }
}
