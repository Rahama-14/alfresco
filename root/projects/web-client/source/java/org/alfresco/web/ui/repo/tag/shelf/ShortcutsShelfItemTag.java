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
package org.alfresco.web.ui.repo.tag.shelf;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.el.MethodBinding;

import org.springframework.extensions.webscripts.ui.common.tag.BaseComponentTag;
import org.alfresco.web.ui.repo.component.shelf.UIShortcutsShelfItem;

/**
 * @author Kevin Roast
 */
public class ShortcutsShelfItemTag extends BaseComponentTag
{
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "org.alfresco.faces.ShortcutsShelfItem";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      // self rendering component
      return null;
   }
   
   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      setStringBindingProperty(component, "value", this.value);
      if (isValueReference(this.clickActionListener))
      {
         MethodBinding vb = getFacesContext().getApplication().createMethodBinding(this.clickActionListener, ACTION_CLASS_ARGS);
         ((UIShortcutsShelfItem)component).setClickActionListener(vb);
      }
      else
      {
         throw new FacesException("Click Action listener method binding incorrectly specified: " + this.clickActionListener);
      }
      if (isValueReference(this.removeActionListener))
      {
         MethodBinding vb = getFacesContext().getApplication().createMethodBinding(this.removeActionListener, ACTION_CLASS_ARGS);
         ((UIShortcutsShelfItem)component).setRemoveActionListener(vb);
      }
      else
      {
         throw new FacesException("Remove Action listener method binding incorrectly specified: " + this.clickActionListener);
      }
   }
   
   /**
    * @see javax.servlet.jsp.tagext.Tag#release()
    */
   public void release()
   {
      super.release();
      
      this.value = null;
      this.clickActionListener = null;
      this.removeActionListener = null;
   }
   
   /**
    * Set the value used to bind the shortcuts list to the component
    *
    * @param value     the value
    */
   public void setValue(String value)
   {
      this.value = value;
   }
   
   /**
    * Set the clickActionListener
    *
    * @param clickActionListener     the clickActionListener
    */
   public void setClickActionListener(String clickActionListener)
   {
      this.clickActionListener = clickActionListener;
   }
   
   /**
    * Set the removeActionListener
    *
    * @param removeActionListener     the removeActionListener
    */
   public void setRemoveActionListener(String removeActionListener)
   {
      this.removeActionListener = removeActionListener;
   }


   /** the clickActionListener */
   private String clickActionListener;
   
   /** the removeActionListener */
   private String removeActionListener;
   
   /** the value */
   private String value;
}
