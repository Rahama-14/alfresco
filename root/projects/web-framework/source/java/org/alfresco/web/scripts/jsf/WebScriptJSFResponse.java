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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.web.scripts.jsf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;

import javax.faces.component.UIForm;
import javax.faces.context.FacesContext;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.scripts.Cache;
import org.alfresco.web.scripts.Runtime;
import org.alfresco.web.scripts.WebScriptResponse;
import org.alfresco.web.scripts.WebScriptResponseImpl;
import org.alfresco.web.ui.common.JSFUtils;
import org.alfresco.web.ui.common.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlFormRendererBase;

/**
 * Implementation of a WebScript Response for the JSF environment.
 * 
 * @author Kevin Roast
 */
public class WebScriptJSFResponse extends WebScriptResponseImpl implements WebScriptResponse
{
   private FacesContext fc;
   private UIWebScript component;
   
   WebScriptJSFResponse(Runtime container, FacesContext fc, UIWebScript component)
   {
      super(container);
      this.fc = fc;
      this.component = component;
   }
   
   /**
    * @see org.alfresco.web.scripts.WebScriptResponse#encodeScriptUrl(java.lang.String)
    */
   public String encodeScriptUrl(String url)
   {
      UIForm form = JSFUtils.getParentForm(fc, component);
      if (form == null)
      {
         throw new IllegalStateException("Must nest components inside UIForm to generate form submit!");
      }
      
      String fieldId = component.getClientId(fc);
      String formClientId = form.getClientId(fc);
      
      StringBuilder buf = new StringBuilder(256);
      // dirty - but can't see any other way to convert to a JSF action click... 
      buf.append("#\" onclick=\"");
      buf.append("document.forms[");
      buf.append("'");
      buf.append(formClientId);
      buf.append("'");
      buf.append("]['");
      buf.append(fieldId);
      buf.append("'].value=");
      buf.append("'");
      // encode the URL to the webscript
      try
      {
         buf.append(URLEncoder.encode(url, "UTF-8"));
      }
      catch (UnsupportedEncodingException e)
      {
         throw new AlfrescoRuntimeException("Unable to utf-8 encode script url.");
      }
      buf.append("'");
      buf.append(";");
      
      buf.append("document.forms[");
      buf.append("'");
      buf.append(formClientId);
      buf.append("'");
      buf.append("].submit();");
      
      buf.append("return false;");
      
      // weak, but this seems to be the way Sun RI/MyFaces do it...
      HtmlFormRendererBase.addHiddenCommandParameter(fc, form, fieldId);
      
      return buf.toString();
   }

   /**
    * @see org.alfresco.web.scripts.WebScriptResponse#reset()
    */
   public void reset()
   {
       // nothing to do
   }

   /**
    * @see org.alfresco.web.scripts.WebScriptResponse#getOutputStream()
    */
   public OutputStream getOutputStream() throws IOException
   {
      return fc.getResponseStream();
   }

   /**
    * @see org.alfresco.web.scripts.WebScriptResponse#getWriter()
    */
   public Writer getWriter() throws IOException
   {
      return fc.getResponseWriter();
   }

   /**
    * @see org.alfresco.web.scripts.WebScriptResponse#setStatus(int)
    */
   public void setStatus(int status)
   {
      // makes no sense in the JSF env
   }
    
   /**
    * @see org.alfresco.web.scripts.WebScriptResponse#setCache()
    */
   public void setCache(Cache cache)
   {
      // NOTE: not applicable
   }
   
   /**
    * @see org.alfresco.web.scripts.WebScriptResponse#setContentType(java.lang.String)
    */
   public void setContentType(String contentType)
   {
      // Alfresco JSF framework only supports the default of text-html
   }
   
   /* (non-Javadoc)
    * @see org.alfresco.web.scripts.WebScriptResponse#getEncodeScriptUrlFunction(java.lang.String)
    */
   public String getEncodeScriptUrlFunction(String name)
   {
      UIForm form = JSFUtils.getParentForm(fc, component);
      if (form == null)
      {
         throw new IllegalStateException("Must nest components inside UIForm to generate form submit!");
      }
      String fieldId = component.getClientId(fc);
      String formClientId = form.getClientId(fc);
      HtmlFormRendererBase.addHiddenCommandParameter(fc, form, fieldId);
      
      String func = ENCODE_FUNCTION.replace("$name$", name);
      func = func.replace("$formClientId$", formClientId);
      func = func.replace("$fieldId$", fieldId);
      return StringUtils.encodeJavascript(func);
   }
   
   private static final String ENCODE_FUNCTION = 
            "{ $name$: function(url) {" + 
            " var out = '';" + 
            " out += \"#\\\" onclick=\\\"document.forms['$formClientId$']['$fieldId$'].value='\";" + 
            " out += escape(url);" + 
            " out += \"';document.forms['$formClientId$'].submit();return false;\";" + 
            " return out; } }";

}
