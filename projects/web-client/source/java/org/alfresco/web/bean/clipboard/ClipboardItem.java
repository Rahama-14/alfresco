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
package org.alfresco.web.bean.clipboard;

import org.alfresco.web.bean.repository.Node;

/**
 * @author Kevin Roast
 */
public class ClipboardItem
{
   /**
    * Constructor
    * 
    * @param node       The node on the clipboard
    * @param mode       The ClipboardStatus enum value
    */
   public ClipboardItem(Node node, ClipboardStatus mode)
   {
      this.Node = node;
      this.Mode = mode;
   }
   
   /**
    * Override equals() to compare NodeRefs
    */
   public boolean equals(Object obj)
   {
      if (obj == this)
      {
         return true;
      }
      if (obj instanceof ClipboardItem)
      {
         return ((ClipboardItem)obj).Node.getNodeRef().equals(Node.getNodeRef());
      }
      else
      {
         return false;
      }
   }
   
   /**
    * Override hashCode() to use the internal NodeRef hashcode instead
    */
   public int hashCode()
   {
      return Node.getNodeRef().hashCode();
   }
   
   
   public Node Node;
   public ClipboardStatus Mode;
}
