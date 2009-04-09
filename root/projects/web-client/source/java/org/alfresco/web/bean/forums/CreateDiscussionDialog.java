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
package org.alfresco.web.bean.forums;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.ReportedException;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Bean implementation for the "Create Discusssion Dialog".
 * 
 * @author gavinc
 */
public class CreateDiscussionDialog extends CreateTopicDialog
{
   private static final long serialVersionUID = 3500493916528264014L;

   protected NodeRef discussingNodeRef;
   
   private static final Log logger = LogFactory.getLog(CreateDiscussionDialog.class);
   
   // ------------------------------------------------------------------------------
   // Wizard implementation
   
   @Override
   public void init(Map<String, String> parameters)
   {
      super.init(parameters);
      
      // get the id of the node we are creating the discussion for
      String id = parameters.get("id");
      if (id == null || id.length() == 0)
      {
         throw new AlfrescoRuntimeException("createDiscussion called without an id");
      }
      
      // create the topic to hold the discussions
      createTopic(id);
   }
   
   @Override
   public String cancel()
   {
      // if the user cancels the creation of a discussion all the setup that was done 
      // when the dialog started needs to be undone i.e. removing the created forum
      // and the discussable aspect
      deleteTopic();
      
      // as we are cancelling the creation of a discussion we know we need to go back
      // to the browse screen, this also makes sure we don't end up in the forum that
      // just got deleted!
      return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME + 
             AlfrescoNavigationHandler.OUTCOME_SEPARATOR + "browse";
   }
   
   // ------------------------------------------------------------------------------
   // Helper methods

   /**
    * Creates a topic for the node with the given id
    * 
    * @param id The id of the node to discuss
    */
   protected void createTopic(final String id)
   {
      RetryingTransactionCallback<NodeRef> createTopicCallback = new RetryingTransactionCallback<NodeRef>()
      {
         public NodeRef execute() throws Throwable
         {
            NodeRef forumNodeRef = null;
            discussingNodeRef = new NodeRef(Repository.getStoreRef(), id);
            
            if (getNodeService().hasAspect(discussingNodeRef, ForumModel.ASPECT_DISCUSSABLE))
            {
               throw new AlfrescoRuntimeException("createDiscussion called for an object that already has a discussion!");
            }
            
            // Add the discussable aspect
            getNodeService().addAspect(discussingNodeRef, ForumModel.ASPECT_DISCUSSABLE, null);
            // The discussion aspect create the necessary child
            List<ChildAssociationRef> destChildren = getNodeService().getChildAssocs(
                  discussingNodeRef,
                  ForumModel.ASSOC_DISCUSSION,
                  RegexQNamePattern.MATCH_ALL);
            // Take the first one
            if (destChildren.size() == 0)
            {
               // Drop the aspect and recreate it.  This should not happen, but just in case ...
               getNodeService().removeAspect(discussingNodeRef, ForumModel.ASPECT_DISCUSSABLE);
               getNodeService().addAspect(discussingNodeRef, ForumModel.ASPECT_DISCUSSABLE, null);
               // The discussion aspect create the necessary child
               destChildren = getNodeService().getChildAssocs(
                     discussingNodeRef,
                     ForumModel.ASSOC_DISCUSSION,
                     RegexQNamePattern.MATCH_ALL);
            }
            if (destChildren.size() == 0)
            {
               throw new AlfrescoRuntimeException("The discussable aspect behaviour is not creating a topic");
            }
            else
            {
               // We just take the first one
               ChildAssociationRef discussionAssoc = destChildren.get(0);
               forumNodeRef = discussionAssoc.getChildRef();
            }
            
            if (logger.isDebugEnabled())
               logger.debug("created forum for content: " + discussingNodeRef.toString());
            
            return forumNodeRef;
         }
      };
      
      FacesContext context = FacesContext.getCurrentInstance();
      NodeRef forumNodeRef = null;
      try
      {
         forumNodeRef = getTransactionService().getRetryingTransactionHelper().doInTransaction(
               createTopicCallback, false);
      }
      catch (Throwable e)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               context, Repository.ERROR_GENERIC), e.getMessage()), e);
         ReportedException.throwIfNecessary(e);
      }
      // finally setup the context for the forum we just created
      if (forumNodeRef != null)
      {
         this.browseBean.clickSpace(forumNodeRef);
      }
//      
//      try
//      {
//         tx = Repository.getUserTransaction(context);
//         tx.begin();
//         
//         this.discussingNodeRef = new NodeRef(Repository.getStoreRef(), id);
//         
//         if (this.getNodeService().hasAspect(this.discussingNodeRef, ForumModel.ASPECT_DISCUSSABLE))
//         {
//            throw new AlfrescoRuntimeException("createDiscussion called for an object that already has a discussion!");
//         }
//         
//         // Add the discussable aspect
//         this.getNodeService().addAspect(this.discussingNodeRef, ForumModel.ASPECT_DISCUSSABLE, null);
//         // The discussion aspect create the necessary child
//         List<ChildAssociationRef> destChildren = this.getNodeService().getChildAssocs(
//                 this.discussingNodeRef,
//                 ForumModel.ASSOC_DISCUSSION,
//                 RegexQNamePattern.MATCH_ALL);
//         // Take the first one
//         if (destChildren.size() == 0)
//         {
//             // Drop the aspect and recreate it.  This should not happen, but just in case ...
//             this.getNodeService().removeAspect(this.discussingNodeRef, ForumModel.ASPECT_DISCUSSABLE);
//         }
//         else
//         {
//             ChildAssociationRef discussionAssoc = destChildren.get(0);
//             forumNodeRef = discussionAssoc.getChildRef();
//         }
//         
////         // create a child forum space using the child association just introduced by
////         // adding the discussable aspect
////         String name = (String)this.getNodeService().getProperty(this.discussingNodeRef, 
////               ContentModel.PROP_NAME);
////         String msg = Application.getMessage(FacesContext.getCurrentInstance(), "discussion_for");
////         String forumName = MessageFormat.format(msg, new Object[] {name});
////         
////         Map<QName, Serializable> forumProps = new HashMap<QName, Serializable>(1);
////         forumProps.put(ContentModel.PROP_NAME, forumName);
////         ChildAssociationRef childRef = this.getNodeService().createNode(this.discussingNodeRef, 
////               ForumModel.ASSOC_DISCUSSION,
////               QName.createQName(NamespaceService.FORUMS_MODEL_1_0_URI, "discussion"), 
////               ForumModel.TYPE_FORUM, forumProps);
////         
////         forumNodeRef = childRef.getChildRef();
////
//         // apply the uifacets aspect
//         Map<QName, Serializable> uiFacetsProps = new HashMap<QName, Serializable>(5);
//         uiFacetsProps.put(ApplicationModel.PROP_ICON, "forum");
//         this.getNodeService().addAspect(forumNodeRef, ApplicationModel.ASPECT_UIFACETS, uiFacetsProps);
//         
//         if (logger.isDebugEnabled())
//            logger.debug("created forum for content: " + this.discussingNodeRef.toString());
//         
//         // commit the transaction
//         tx.commit();
//      }
//      catch (Throwable e)
//      {
//         // rollback the transaction
//         try { if (tx != null) {tx.rollback();} } catch (Exception ex) {}
//         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
//               context, Repository.ERROR_GENERIC), e.getMessage()), e);
//      }
//      
   }
   
   /**
    * Deletes the setup performed during the initialisation of the dialog.
    */
   protected void deleteTopic()
   {
      RetryingTransactionCallback<Object> deleteTopicCallback = new RetryingTransactionCallback<Object>()
      {
         public Object execute() throws Throwable
         {
            // remove this node from the breadcrumb if required
            Node forumNode = navigator.getCurrentNode();
            browseBean.removeSpaceFromBreadcrumb(forumNode);
            
            // remove the discussable aspect from the node we were going to discuss!
            // AWC-1519: removing the aspect that defines the child association now does the 
            //           cascade delete so we no longer have to delete the child explicitly
            getNodeService().removeAspect(discussingNodeRef, ForumModel.ASPECT_DISCUSSABLE);
            // Done
            return null;
         }
      };
      FacesContext context = FacesContext.getCurrentInstance();
      try
      {
         getTransactionService().getRetryingTransactionHelper().doInTransaction(deleteTopicCallback, false);
      }
      catch (Throwable e)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               context, Repository.ERROR_GENERIC), e.getMessage()), e);
         ReportedException.throwIfNecessary(e);
      }
   }
}
