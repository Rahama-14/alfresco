/*
 * Created on 01-Apr-2005
 */
package org.alfresco.web.ui.common.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

/**
 * @author kevinr
 */
public class UIBreadcrumb extends UICommand
{
   // ------------------------------------------------------------------------------
   // Construction 
   
   /**
    * Default Constructor
    */
   public UIBreadcrumb()
   {
      setRendererType("awc.faces.BreadcrumbRenderer");
   }


   // ------------------------------------------------------------------------------
   // Component implementation 
   
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "awc.faces.Controls";
   }
   
   /**
    * @see javax.faces.component.UICommand#broadcast(javax.faces.event.FacesEvent)
    */
   public void broadcast(FacesEvent event) throws AbortProcessingException
   {
      if (event instanceof BreadcrumbEvent)
      {
         setSelectedPathIndex( ((BreadcrumbEvent)event).SelectedIndex );
      }
      
      // default ActionEvent processing for a UICommand
      super.broadcast(event);
   }

   /**
    * Set the selected path index. This modifies the current path value.
    * 
    * @return last selected path index
    */
   public void setSelectedPathIndex(int index)
   {
      // getValue() will return a List of IBreadcrumbHandler (see impl below)
      List<IBreadcrumbHandler> elements = (List)getValue();
      
      if (elements.size() >= index)
      {
         // copy path elements up to the selected index to a new List
         List<IBreadcrumbHandler> path = new ArrayList<IBreadcrumbHandler>(index + 1);
         path.addAll(elements.subList(0, index + 1));
         
         // set the new List as our new path value
         setValue(path);
         
         // call the app logic for the element handler and perform any required navigation
         String outcome = path.get(index).navigationOutcome(this);
         if (outcome != null)
         {
            String viewId = getFacesContext().getViewRoot().getViewId();
            getFacesContext().getApplication().getNavigationHandler().handleNavigation(
                  getFacesContext(), viewId, outcome);
         }
      }
   }
   
   /**
    * Override getValue() to deal with converting a String path into a valid List of IBreadcrumbHandler
    */
   public Object getValue()
   {
      List<IBreadcrumbHandler> elements = null;
      
      Object value = super.getValue();
      if (value instanceof String)
      {
         elements = new ArrayList(8);
         // found a String based path - convert to List of IBreadcrumbHandler instances
         StringTokenizer t = new StringTokenizer((String)value, SEPARATOR);
         while (t.hasMoreTokens() == true)
         {
            IBreadcrumbHandler handler = new DefaultPathHandler(t.nextToken());
            elements.add(handler);
         }
         
         // save result so we don't need to repeat the conversion
         setValue(elements);
      }
      else if (value instanceof List)
      {
         elements = (List)value;
      }
      else if (value != null)
      {
         throw new IllegalArgumentException("UIBreadcrumb value must be a String path or List of IBreadcrumbHandler!");
      }
      else
      {
         elements = new ArrayList(8);
      }
      
      return elements;
   }
   
   /**
    * Append a handler object to the current breadcrumb structure
    * 
    * @param handler    The IBreadcrumbHandler to append
    */
   public void appendHandler(IBreadcrumbHandler handler)
   {
      if (handler == null)
      {
         throw new NullPointerException("IBreadcrumbHandler instance cannot be null!");
      }
      
      List elements = (List)getValue();
      elements.add(handler);
   }
   
   
   // ------------------------------------------------------------------------------
   // Strongly typed component property accessors 
   
   /**
    * Get the visible separator value for outputing the breadcrumb
    * 
    * @return separator string
    */
   public String getSeparator()
   {
      ValueBinding vb = getValueBinding("separator");
      if (vb != null)
      {
         this.separator = (String)vb.getValue(getFacesContext());
      }
      
      return this.separator;
   }
   
   /**
    * Set separator
    * 
    * @param separator     visible separator value for outputing the breadcrumb
    */
   public void setSeparator(String separator)
   {
      this.separator = separator;
   }
   
   /**
    * Get whether to show the root of the path
    * 
    * @return true to show the root of the path, false to hide it
    */
   public boolean getShowRoot()
   {
      ValueBinding vb = getValueBinding("showRoot");
      if (vb != null)
      {
         this.showRoot = (Boolean)vb.getValue(getFacesContext());
      }
      
      if (this.showRoot != null)
      {
         return this.showRoot.booleanValue();
      }
      else
      {
         // return default
         return true;
      }
   }
   
   /**
    * Set whether to show the root of the path
    * 
    * @param showRoot      Whether to show the root of the path
    */
   public void setShowRoot(boolean showRoot)
   {
      this.showRoot = Boolean.valueOf(showRoot);
   }
   
   
   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Class representing the clicking of a breadcrumb element.
    */
   public static class BreadcrumbEvent extends ActionEvent
   {
      public BreadcrumbEvent(UIComponent component, int selectedIndex)
      {
         super(component);
         SelectedIndex = selectedIndex;
      }
      
      public int SelectedIndex = 0;
   }
   
   /**
    * Class representing a handler for the default String path based breadcrumb
    */
   private static class DefaultPathHandler implements IBreadcrumbHandler
   {
      /**
       * Constructor
       * 
       * @param label      The element display label
       */
      public DefaultPathHandler(String label)
      {
         this.label = label;
      }
      
      /**
       * Return the element display label
       */
      public String toString()
      {
         return this.label;
      }
      
      /**
       * @see org.alfresco.web.ui.common.component.IBreadcrumbHandler#navigationOutcome(org.alfresco.web.ui.common.component.UIBreadcrumb)
       */
      public String navigationOutcome(UIBreadcrumb breadcrumb)
      {
         // no outcome for the default handler - return to current page
         return null;
      }
      
      private String label;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data
   
   /** visible separator value */
   private String separator = null;
   
   /** true to show the root of the breadcrumb path, false otherwise */
   private Boolean showRoot = null;
   
   /** the separator for a breadcrumb path value */
   public final static String SEPARATOR = "/";
}
