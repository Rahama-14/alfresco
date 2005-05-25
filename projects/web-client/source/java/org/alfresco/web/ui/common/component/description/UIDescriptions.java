package org.alfresco.web.ui.common.component.description;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.repo.ref.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.SelfRenderingComponent;

/**
 * Descriptions component that outputs descriptions held in a backing object
 * 
 * @author gavinc
 */
public class UIDescriptions extends SelfRenderingComponent
{
   private Object value;
   
   /**
    * @return Returns the object holding the decriptions
    */
   public Object getValue()
   {
      if (this.value == null)
      {
         ValueBinding vb = getValueBinding("value");
         if (vb != null)
         {
            this.value = vb.getValue(getFacesContext());
         }
      }
      
      return this.value;
   }

   /**
    * @param value Sets the object holding the description
    */
   public void setValue(Object value)
   {
      this.value = value;
   }

   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.Descriptions";
   }

   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.value = values[1];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[2];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.value;
      return (values);
   }
}
