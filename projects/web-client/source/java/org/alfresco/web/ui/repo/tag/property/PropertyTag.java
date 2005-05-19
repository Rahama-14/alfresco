package org.alfresco.web.ui.repo.tag.property;

import javax.faces.component.UIComponent;

import org.alfresco.web.ui.common.tag.BaseComponentTag;

/**
 * @author gavinc
 */
public class PropertyTag extends BaseComponentTag
{
   private String name;
   private String displayLabel;
   private String readOnly;
   private String mode;
   
   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      return "org.alfresco.faces.PropertyRenderer";
   }
   
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.Property";
   }

   /**
    * @param displayLabel Sets the display label
    */
   public void setDisplayLabel(String displayLabel)
   {
      this.displayLabel = displayLabel;
   }

   /**
    * @param name Sets the name
    */
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * @param readOnly Sets whether the property is read only
    */
   public void setReadOnly(String readOnly)
   {
      this.readOnly = readOnly;
   }
   
   /**
    * @param mode The mode, either "edit" or "view"
    */
   public void setMode(String mode)
   {
      this.mode = mode;
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      
      setStringProperty(component, "name", this.name);
      setStringProperty(component, "displayLabel", this.displayLabel);
      setStringProperty(component, "mode", this.mode);
      setBooleanProperty(component, "readOnly", this.readOnly);
   }
   
   /**
    * @see javax.faces.webapp.UIComponentTag#release()
    */
   public void release()
   {
      this.name = null;
      this.displayLabel = null;
      this.mode = null;
      this.readOnly = null;
      
      super.release();
   }
}
