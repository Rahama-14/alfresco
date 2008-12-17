/**
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

package org.alfresco.repo.action.executer;

import java.util.List;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.blog.BlogDetails;
import org.alfresco.repo.blog.BlogIntegrationRuntimeException;
import org.alfresco.repo.blog.BlogIntegrationService;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import org.alfresco.model.BlogIntegrationModel;
import org.alfresco.model.ContentModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Post blog repository action
 *
 * @author mikeh
 */
public class BlogAction extends ActionExecuterAbstractBase implements BlogIntegrationModel 
{
   public static final String NAME = "blog-post";
   public static final String PARAM_BLOG_ACTION = "action";

   private static Log logger = LogFactory.getLog(BlogAction.class);
   
   private DictionaryService dictionaryService;
   private NodeService nodeService;
   private BlogIntegrationService blogIntegrationService;

   /**
    * Set the node service
    *
    * @param nodeService  the node service
    */
   public void setNodeService(NodeService nodeService)
   {
      this.nodeService = nodeService;
   }

   /**
    * Set the dictionary service
    *
    * @param dictionaryService  the dictionary service
    */
   public void setDictionaryService(DictionaryService dictionaryService)
   {
      this.dictionaryService = dictionaryService;
   }

   /**
    * Set the blog integration service
    * 
    * @param blogIntegrationService    the blog integration service
    */
   public void setBlogIntegrationService(BlogIntegrationService blogIntegrationService)
   {
       this.blogIntegrationService = blogIntegrationService;
   }
   
   /**
    * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef)
    */
   @Override
   protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
   {
      String blogAction = (String)action.getParameterValue(PARAM_BLOG_ACTION);
      try
      {
         if ("post".equals(blogAction) == true)
         {
             QName type = this.nodeService.getType(actionedUponNodeRef);
             if (this.dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT) == true)
             {
                 List<BlogDetails> list = this.blogIntegrationService.getBlogDetails(actionedUponNodeRef);
                 if (list.size() != 0)
                 {
                     // Take the 'nearest' blog details
                     BlogDetails blogDetails = list.get(0);
                     this.blogIntegrationService.newPost(blogDetails, actionedUponNodeRef, ContentModel.PROP_CONTENT, true);
                 }
             }
         }
         else if ("update".equals(blogAction) == true)
         {
             QName type = this.nodeService.getType(actionedUponNodeRef);
             if (this.nodeService.hasAspect(actionedUponNodeRef, ASPECT_BLOG_POST) == true &&
                 this.dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT) == true)
             {
                 this.blogIntegrationService.updatePost(actionedUponNodeRef, ContentModel.PROP_CONTENT, true);
             }
         }
         else if ("remove".equals(blogAction) == true)
         {
             QName type = this.nodeService.getType(actionedUponNodeRef);
             if (this.nodeService.hasAspect(actionedUponNodeRef, ASPECT_BLOG_POST) == true &&
                 this.dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT) == true)
             {
                 this.blogIntegrationService.deletePost(actionedUponNodeRef);
             }
         }
         else
         {
             throw new BlogIntegrationRuntimeException("Invalid action has been specified '" + blogAction + "'");
         }
         
         action.setParameterValue(PARAM_RESULT, "");
      }
      catch (BlogIntegrationRuntimeException ex)
      {
         action.setParameterValue(PARAM_RESULT, ex.getMessage());
      }
      catch (Exception ex)
      {
         action.setParameterValue(PARAM_RESULT, "Action failed. Please check blog configuration parameters.");
      }
   }

   /**
    * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
    */
   @Override
   protected void addParameterDefinitions(List<ParameterDefinition> paramList)
   {
      // Add definitions for action parameters
      paramList.add(
            new ParameterDefinitionImpl(PARAM_BLOG_ACTION,
            DataTypeDefinition.TEXT, 
            true,
            getParamDisplayLabel(PARAM_BLOG_ACTION)));

      paramList.add(
            new ParameterDefinitionImpl(PARAM_RESULT,
            DataTypeDefinition.TEXT, 
            false,
            getParamDisplayLabel(PARAM_RESULT)));

   }
}
