/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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

import org.springframework.extensions.config.ConfigElement;
import org.springframework.extensions.config.xml.elementreader.ConfigElementReader;
import org.dom4j.Element;

/**
 * Responsible for loading Web Framework configuration settings from
 * the web-framework-config*.xml files that are loaded via the configuration
 * service.
 * 
 * @author muzquiano
 */
public class WebFrameworkConfigElementReader implements ConfigElementReader
{   
   /**
    * Called from the configuration service to handle the loading of the
    * Web Framework configuration XML.
    * 
    * @param element the element
    * 
    * @return the config element
    * 
    * @see org.springframework.extensions.config.xml.elementreader.ConfigElementReader#parse(org.dom4j.Element)
    */
   public ConfigElement parse(Element elem)
   {
       ConfigElement configElement = null;
       if (elem != null)
       {
           configElement = WebFrameworkConfigElement.newInstance(elem);
       }
       return configElement;
   }
}
