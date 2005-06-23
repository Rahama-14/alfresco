package org.alfresco.web.ui.common.component.description;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.web.ui.common.component.SelfRenderingComponent;

/**
 * Description component that outputs a dynamic description
 * 
 * @author gavinc
 */
public class UIDescription extends SelfRenderingComponent
{
   private String controlValue;
   private String text;

   /**
    * @return The control value the description is for
    */
   public String getControlValue()
   {
      if (this.controlValue == null)
      {
         ValueBinding vb = getValueBinding("controlValue");
         if (vb != null)
         {
            this.controlValue = (String)vb.getValue(getFacesContext());
         }
      }
      
      return this.controlValue;
   }

   /**
    * @param controlValue Sets the control value this description is for
    */
   public void setControlValue(String controlValue)
   {
      this.controlValue = controlValue;
   }

   /**
    * @return Returns the description text
    */
   public String getText()
   {
      if (this.text == null)
      {
         ValueBinding vb = getValueBinding("text");
         if (vb != null)
         {
            this.text = (String)vb.getValue(getFacesContext());
         }
      }
      
      return this.text;
   }

   /**
    * @param text Sets the description text 
    */
   public void setText(String text)
   {
      this.text = text;
   }

   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.Description";
   }

   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.controlValue = (String)values[1];
      this.text = (String)values[2];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[3];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.controlValue;
      values[2] = this.text;
      return (values);
   }

   /**
    * @see javax.faces.component.UIComponent#getRendersChildren()
    */
   public boolean getRendersChildren()
   {
      return false;
   }
}
