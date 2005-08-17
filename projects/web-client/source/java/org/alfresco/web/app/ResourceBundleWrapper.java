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
package org.alfresco.web.app;

import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * Wrapper around Alfresco Resource Bundle objects. Used to catch and handle missing
 * resource exception to help identify missing I18N strings in client apps.
 * 
 * @author Kevin Roast
 */
public final class ResourceBundleWrapper extends ResourceBundle
{
   private static Logger logger = Logger.getLogger(ResourceBundleWrapper.class);
   private ResourceBundle delegate;
   
   /**
    * Constructor
    * 
    * @param bundle     The ResourceBundle to route calls too
    */
   public ResourceBundleWrapper(ResourceBundle bundle)
   {
      this.delegate = bundle;
   }
   
   /**
    * @see java.util.ResourceBundle#getKeys()
    */
   public Enumeration<String> getKeys()
   {
      return this.delegate.getKeys();
   }
   
   /**
    * @see java.util.ResourceBundle#handleGetObject(java.lang.String)
    */
   protected Object handleGetObject(String key)
   {
      try
      {
         return this.delegate.getObject(key);
      }
      catch (MissingResourceException err)
      {
         if (logger.isEnabledFor(Priority.WARN))
            logger.warn("Failed to find I18N message string key: " + key);
         
         return "$$" + key + "$$";
      }
   }
}
