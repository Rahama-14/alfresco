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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.extensions.config.ConfigElement;
import org.springframework.extensions.config.ConfigException;
import org.springframework.extensions.config.element.ConfigElementAdapter;

/**
 * @author Kevin Roast
 */
public class CommandServletConfigElement extends ConfigElementAdapter
{
   public static final String CONFIG_ELEMENT_ID = "command-servlet";
   
   private Map<String, Class> commandProcessors = new HashMap<String, Class>(4, 1.0f);
   
   /**
    * Default constructor
    */
   public CommandServletConfigElement()
   {
      super("command-servlet");
   }

   /**
    * Constructor
    * 
    * @param name Name of the element this config element represents
    */
   public CommandServletConfigElement(String name)
   {
      super(name);
   }
   
   /**
    * @see org.springframework.extensions.config.element.ConfigElementAdapter#getChildren()
    */
   public List<ConfigElement> getChildren()
   {
      throw new ConfigException("Reading the Command Servlet config via the generic interfaces is not supported");
   }
   
   /**
    * @see org.springframework.extensions.config.element.ConfigElementAdapter#combine(org.alfresco.config.ConfigElement)
    */
   public ConfigElement combine(ConfigElement configElement)
   {
      CommandServletConfigElement newElement = (CommandServletConfigElement)configElement;
      CommandServletConfigElement combinedElement = new CommandServletConfigElement();
      
      for (String name : commandProcessors.keySet())
      {
         combinedElement.addCommandProcessor(name, commandProcessors.get(name));
      }
      for (String name : newElement.commandProcessors.keySet())
      {
         combinedElement.addCommandProcessor(name, newElement.commandProcessors.get(name));
      }
      
      return combinedElement;
   }
   
   /*package*/ void addCommandProcessor(String name, String className)
   {
      try
      {
         Class clazz = Class.forName(className);
         commandProcessors.put(name, clazz);
      }
      catch (Throwable err)
      {
         throw new ConfigException("Unable to load command proccessor class: " +
               className + " due to " + err.getMessage());
      }
   }
   
   private void addCommandProcessor(String name, Class clazz)
   {
      commandProcessors.put(name, clazz);
   }
   
   public Class getCommandProcessor(String name)
   {
      return commandProcessors.get(name);
   }
}
