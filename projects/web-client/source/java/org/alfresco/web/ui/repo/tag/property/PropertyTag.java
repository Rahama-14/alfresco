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
   private String converter;
   
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
    * @param converter Sets the id of the converter
    */
   public void setConverter(String converter)
   {
      this.converter = converter;
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
      setStringProperty(component, "converter", this.converter);
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
      this.converter = null;
      this.readOnly = null;
      
      super.release();
   }
}
