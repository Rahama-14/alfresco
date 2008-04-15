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
package org.alfresco.web.config;

import java.util.Iterator;

import org.alfresco.config.ConfigElement;
import org.alfresco.config.xml.elementreader.ConfigElementReader;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 * @author muzquiano
 */
public class WebSiteElementReader implements ConfigElementReader
{   
   /**
    * @see org.alfresco.config.xml.elementreader.ConfigElementReader#parse(org.dom4j.Element)
    */
   public ConfigElement parse(Element element)
   {
      WebSiteConfigElement configElement = null;
      
      if (element != null)
      {
         configElement = createConfigElement(element);
         
         // process any children there may be
         processChildren(element, configElement);
      }
      
      return configElement;
   }

   /**
    * Recursively processes the children creating the required config element
    * objects as it goes
    * 
    * @param element
    * @param parentConfig
    */
   @SuppressWarnings("unchecked")
   private void processChildren(Element element, WebSiteConfigElement parentConfig)
   {
      // get the list of children for the given element
      Iterator<Element> children = element.elementIterator();
      while (children.hasNext())
      {
         Element child = children.next();
         WebSiteConfigElement childConfigElement = createConfigElement(child);
         parentConfig.addChild(childConfigElement);
         
         // recurse down the children
         processChildren(child, childConfigElement);
      }
   }
   
   /**
    * Creates a ConfigElementImpl object from the given element.
    * 
    * @param element The element to parse
    * @return The GenericConfigElement representation of the given element
    */
   @SuppressWarnings("unchecked")
   private WebSiteConfigElement createConfigElement(Element element)
   {
      // get the name and value of the given element
      String name = element.getName();
      
      // create the config element object and populate with value
      // and attributes
      WebSiteConfigElement configElement = new WebSiteConfigElement(name);
      if ((element.hasContent()) && (element.hasMixedContent() == false))
      {
         String value = element.getTextTrim();
         configElement.setValue(value);
      }
      
      Iterator<Attribute> attrs = element.attributeIterator();
      while (attrs.hasNext())
      {
         Attribute attr = attrs.next();
         String attrName = attr.getName();
         String attrValue = attr.getValue();

         configElement.addAttribute(attrName, attrValue);
      }
      
      return configElement;
   }
   
}
