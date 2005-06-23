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
package org.alfresco.web.ui.common.renderer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIImagePicker;
import org.alfresco.web.ui.common.component.UIListItem;
import org.apache.log4j.Logger;

/**
 * Renderer for the image picker component that outputs the list of images
 * as radio buttons
 * 
 * @author gavinc
 */
public class ImagePickerRadioRenderer extends BaseRenderer
{
   private static Logger logger = Logger.getLogger(ImagePickerRadioRenderer.class);
   
   // ------------------------------------------------------------------------------
   // Renderer implemenation
   
   /**
    * @see javax.faces.render.Renderer#decode(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void decode(FacesContext context, UIComponent component)
   {
      if (Utils.isComponentDisabledOrReadOnly(component))
      {
         return;
      }
      
      String clientId = component.getClientId(context);
      Map paramsMap = context.getExternalContext().getRequestParameterMap();
      
      String submittedValue = (String)paramsMap.get(clientId);
      
      if (logger.isDebugEnabled())
         logger.debug("Submitted value = " + submittedValue);
      
      ((UIInput)component).setSubmittedValue(submittedValue);
   }

   /**
    * @see javax.faces.render.Renderer#encodeBegin(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeBegin(FacesContext context, UIComponent component) throws IOException
   {
      if (component.isRendered() == false)
      {
         return;
      }

      ResponseWriter out = context.getResponseWriter();
      
      UIImagePicker imagePicker = (UIImagePicker)component;

      Map attrs = imagePicker.getAttributes();
      out.write("<table cellpadding='0'");
      outputAttribute(out, attrs.get("spacing"), "cellspacing");
      outputAttribute(out, attrs.get("styleClass"), "class");
      outputAttribute(out, attrs.get("style"), "style");
      out.write(">\n");
   }
   
   /**
    * @see javax.faces.render.Renderer#encodeChildren(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeChildren(FacesContext context, UIComponent component) throws IOException
   {
      if (component.isRendered() == false)
      {
         return;
      }
      
      UIImagePicker imagePicker = (UIImagePicker)component;
      Map attrs = imagePicker.getAttributes();
      
      int columns = 1;
      int position = 0;
      boolean open = false;
      Integer cols = (Integer)attrs.get("columns");
      if (cols != null && cols instanceof Integer)
      {
         columns = cols.intValue();
      }
      
      // retrieve the onclick handler, if there is one
      String onclick = (String)attrs.get("onclick");
         
      ResponseWriter out = context.getResponseWriter();
      
      // get the child components
      for (Iterator i = imagePicker.getChildren().iterator(); i.hasNext(); /**/)
      {
         UIComponent child = (UIComponent)i.next();
         if (child instanceof UIListItem && child.isRendered() == true)
         {
            // found a valid UIListItem child to render
            UIListItem item = (UIListItem)child;
            
            String tooltip = (String)child.getAttributes().get("tooltip");
            
            // if we are at the start of another row output "tr"
            if ((position % columns) == 0)
            {
               // if we are at the end of a row, close it
               if (open)
               {
                  out.write("</tr>\n");
                  open = false;
               }
               
               out.write("<tr>");
               
               // we have started the row
               open = true;
            }
            
            // output the next "cell" i.e. a radio button, the image and optional label 
            out.write("<td>");
           
            out.write("<input type='radio' name='");
            out.write(component.getClientId(context));
            out.write("' value='");
            // TODO: need to take into account values that may need to be converted,
            //       for now presume a string is OK
            out.write(item.getValue().toString());
            out.write("'");
            
            // determine whether this item should be selected
            Object currentValue = imagePicker.getSubmittedValue();
            if (currentValue == null)
            {
               currentValue = imagePicker.getValue();
            }
            
            Object itemValue = item.getValue();
            if (itemValue != null && itemValue.equals(currentValue))
            {
               out.write(" checked='true'");
            }
            
            if (tooltip != null)
            {
               out.write(" title='");
               out.write(Utils.encode(tooltip));
               out.write("'");
            }
            
            if (onclick != null)
            {
               out.write(" onclick='");
               out.write(onclick);
               out.write("'");
            }
            
//            if (item.isDisabled())
//            {
//               out.write(" disabled='true'");
//            }
            
            out.write(">");
            out.write("</td><td align='center'>");
  
            // get the image and make sure there is one!
            String image = (String)child.getAttributes().get("image");
            if (image == null)
            {
               throw new IllegalArgumentException("All child items must specify an image");
            }
            
            out.write(Utils.buildImageTag(context, image, tooltip));
            
            String label = (String)child.getAttributes().get("label");
            if (label != null && label.length() > 0)
            {
               out.write("<br/>");
               out.write(Utils.encode(label));
            }
            
            out.write("</td>");
            
            // we've finished the item so move the position on
            position++;
         }
      }
      
      // if we are in the middle of a row, close it
      if (open)
      {
         out.write("</tr>\n");
      }
   }

   /**
    * @see javax.faces.render.Renderer#encodeEnd(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeEnd(FacesContext context, UIComponent component) throws IOException
   {
      if (component.isRendered() == false)
      {
         return;
      }
      
      ResponseWriter out = context.getResponseWriter();
      out.write("</table>");
   }

   /**
    * @see javax.faces.render.Renderer#getRendersChildren()
    */
   public boolean getRendersChildren()
   {
      return true;
   }
}
