/*
 * Created on 04-Apr-2005
 */
package org.alfresco.web.ui.common.tag;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;

/**
 * @author kevinr
 */
public class ActionLinkTag extends HtmlComponentTag
{
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.ActionLink";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      return "org.alfresco.faces.ActionLinkRenderer";
   }
   
   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      
      setActionProperty((UICommand)component, this.action);
      setActionListenerProperty((UICommand)component, this.actionListener);
      setStringProperty(component, "image", this.image);
      setBooleanProperty(component, "showLink", this.showLink);
      setStringProperty(component, "verticalAlign", this.verticalAlign);
      setIntProperty(component, "padding", this.padding);
      setStringProperty(component, "href", this.href);
      setStringProperty(component, "value", this.value);
      
      // TODO: Add image width/height properties
   }

   /**
    * @see org.alfresco.web.ui.common.tag.HtmlComponentTag#release()
    */
   public void release()
   {
      super.release();
      this.value = null;
      this.action = null;
      this.actionListener = null;
      this.image = null;
      this.showLink = null;
      this.verticalAlign = null;
      this.padding = null;
      this.href = null;
   }
   
   /**
    * Set the value
    *
    * @param value     the value
    */
   public void setValue(String value)
   {
      this.value = value;
   }

   /**
    * Set the action
    *
    * @param action     the action
    */
   public void setAction(String action)
   {
      this.action = action;
   }

   /**
    * Set the actionListener
    *
    * @param actionListener     the actionListener
    */
   public void setActionListener(String actionListener)
   {
      this.actionListener = actionListener;
   }

   /**
    * Set the image
    *
    * @param image     the image
    */
   public void setImage(String image)
   {
      this.image = image;
   }

   /**
    * Set the showLink
    *
    * @param showLink     the showLink
    */
   public void setShowLink(String showLink)
   {
      this.showLink = showLink;
   }
   
   /**
    * Set the vertical alignment value
    *
    * @param verticalAlign     the vertical alignment value
    */
   public void setVerticalAlign(String verticalAlign)
   {
      this.verticalAlign = verticalAlign;
   }
   
   /**
    * Set the padding in pixels
    *
    * @param padding     the padding in pixels
    */
   public void setPadding(String padding)
   {
      this.padding = padding;
   }
   
   /**
    * Set the href to use instead of a JSF action
    *
    * @param href     the href
    */
   public void setHref(String href)
   {
      this.href = href;
   }
   

   /** the padding in pixels */
   private String padding;

   /** the vertical alignment value */
   private String verticalAlign;
   
   /** the value (text to display) */
   private String value;

   /** the action */
   private String action;

   /** the actionListener */
   private String actionListener;

   /** the image */
   private String image;

   /** the showLink boolean */
   private String showLink;
   
   /** the href link */
   private String href;
}
