/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.web.ui.repo.tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.alfresco.web.app.Application;

/**
 * A non-JSF tag library to support dynamic web client message properties. Loads custom bundle by searching repository 
 * before searching classpath.
 * 
 * @author janv
 */
public class LoadBundleTag extends TagSupport
{
   private static final long serialVersionUID = -7336055169875448199L;
   
   private String _var;

   public void setVar(String var)
   {
       _var = var;
   }

   public int doStartTag() throws JspException
   {
       FacesContext facesContext = FacesContext.getCurrentInstance();
       if (facesContext == null)
       {
           throw new JspException("No faces context?!");
       }

       try
       {
           ResourceBundle bundle = Application.getBundle(facesContext);
           facesContext.getExternalContext().getRequestMap().put(_var, new BundleMap(bundle));
       }
       catch(IllegalStateException ex)
       {
           throw new JspException(ex);
       }

       return Tag.SKIP_BODY;
   }
   
   // based on org.apache.myfaces.taglib.core.LoadBundleTag.BundleMap
   private static class BundleMap implements Map
   {
       private ResourceBundle _bundle;
       private List _values;

       public BundleMap(ResourceBundle bundle)
       {
           _bundle = bundle;
       }

       public Object get(Object key)
       {
           try {
               return _bundle.getObject(key.toString());
           } catch (Exception e) {
               return "$$" + key + "$$";
           }
       }

       public boolean isEmpty()
       {
           return !_bundle.getKeys().hasMoreElements();
       }

       public boolean containsKey(Object key)
       {
           try {
               return _bundle.getObject(key.toString()) != null;
           } catch (MissingResourceException e) {
               return false;
           }
       }
       
       public Collection values()
       {
           if (_values == null)
           {
               _values = new ArrayList();
               for (Enumeration enumer = _bundle.getKeys(); enumer.hasMoreElements(); )
               {
                   String v = _bundle.getString((String)enumer.nextElement());
                   _values.add(v);
               }
           }
           return _values;
       }

       public int size()
       {
           return values().size();
       }

       public boolean containsValue(Object value)
       {
           return values().contains(value);
       }

       public Set entrySet()
       {
           Set set = new HashSet();
           for (Enumeration enumer = _bundle.getKeys(); enumer.hasMoreElements(); )
           {
               final String k = (String)enumer.nextElement();
               set.add(new Map.Entry() {
                   public Object getKey()
                   {
                       return k;
                   }

                   public Object getValue()
                   {
                       return _bundle.getObject(k);
                   }

                   public Object setValue(Object value)
                   {
                       throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
                   }
               });
           }
           return set;
       }

       public Set keySet()
       {
           Set set = new HashSet();
           for (Enumeration enumer = _bundle.getKeys(); enumer.hasMoreElements(); )
           {
               set.add(enumer.nextElement());
           }
           return set;
       }
       
       public Object remove(Object key)
       {
           throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
       }

       public void putAll(Map t)
       {
           throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
       }

       public Object put(Object key, Object value)
       {
           throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
       }

       public void clear()
       {
           throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
       }
   }
}