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
package org.alfresco.web.action.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avm.locking.AVMLock;
import org.alfresco.service.cmr.avm.locking.AVMLockingService;
import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.wcm.AVMBrowseBean;
import org.alfresco.web.bean.wcm.AVMUtil;
import org.alfresco.web.bean.wcm.WebProject;

/**
 * UI Action Evaluator - return true if the node is not part of an in-progress 
 * WCM workflow and is locked
 * 
 * @author Gavin Cornwell
 */
public class WCMUnlockEvaluator extends BaseActionEvaluator
{
   /**
    * @see org.alfresco.web.action.ActionEvaluator#evaluate(org.alfresco.web.bean.repository.Node)
    */
   public boolean evaluate(final Node node)
   {
      boolean proceed = false;
      
      FacesContext context = FacesContext.getCurrentInstance();
      AVMService avmService = Repository.getServiceRegistry(context).getAVMService();
      AVMLockingService avmLockingService = Repository.getServiceRegistry(context).getAVMLockingService();
      AVMBrowseBean avmBrowseBean = (AVMBrowseBean)FacesHelper.getManagedBean(context, AVMBrowseBean.BEAN_NAME);
 
      Pair<Integer, String> p = AVMNodeConverter.ToAVMVersionPath(node.getNodeRef());
      String path = p.getSecond();
      
      if (avmService.lookup(p.getFirst(), path) != null)
      {
          WebProject webProject = avmBrowseBean.getWebProject();
          if (webProject == null)
          {
             // when in a workflow context, the WebProject may not be accurate on the browsebean
             // so get the web project associated with the path
             webProject = new WebProject(path);
          }
          
          // determine if the item is locked
          AVMLock lock = avmLockingService.getLock(webProject.getStoreId(), AVMUtil.getStoreRelativePath(path));
          proceed = (lock != null);
      }
      
      return proceed;
   }
}
