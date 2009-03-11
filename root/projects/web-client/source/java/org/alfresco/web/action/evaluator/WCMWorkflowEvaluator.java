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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.wcm.AVMBrowseBean;
import org.alfresco.web.bean.wcm.AVMNode;
import org.alfresco.web.bean.wcm.AVMUtil;
import org.alfresco.web.bean.wcm.AVMWorkflowUtil;
import org.alfresco.web.bean.wcm.WebProject;

/**
 * UI Action Evaluator - return true if the node is not part of an in-progress WCM workflow.
 * 
 * @author Kevin Roast
 */
public class WCMWorkflowEvaluator extends WCMLockEvaluator
{
   private static final long serialVersionUID = -5847066921917855781L;
   
   private static final String TASK_CACHE = "_alf_sandbox_task_cache";

   /**
    * @see org.alfresco.web.action.ActionEvaluator#evaluate(org.alfresco.web.bean.repository.Node)
    */
   public boolean evaluate(final Node node)
   {
      boolean proceed = false;
      if (super.evaluate(node))
      {
         final FacesContext fc = FacesContext.getCurrentInstance();
         final AVMBrowseBean avmBrowseBean = (AVMBrowseBean)FacesHelper.getManagedBean(fc, AVMBrowseBean.BEAN_NAME);
         WebProject webProject = avmBrowseBean.getWebProject();
         if (webProject == null || webProject.hasWorkflow())
         {
            Map<String, List<WorkflowTask>> cachedSandboxTasks = (Map<String, List<WorkflowTask>>)fc.getExternalContext().getRequestMap().get(TASK_CACHE);
            if (cachedSandboxTasks == null)
            {
                cachedSandboxTasks = new HashMap<String, List<WorkflowTask>>(64, 1.0f);
                fc.getExternalContext().getRequestMap().put(TASK_CACHE, cachedSandboxTasks);
            }
            
            String sandbox = AVMUtil.getStoreName(node.getPath());
            List<WorkflowTask> cachedTasks = cachedSandboxTasks.get(sandbox);
            if (cachedTasks == null)
            {
                cachedTasks = AVMWorkflowUtil.getAssociatedTasksForSandbox(sandbox);
                cachedSandboxTasks.put(sandbox, cachedTasks);
            }
             
            proceed = ((AVMUtil.isWorkflowStore(sandbox) ||
                    !((AVMNode)node).isWorkflowInFlight(cachedTasks)) &&
                    !((AVMNode)node).isDeleted());
         }
         else
         {
            // if the WebProject has no workflow then we can proceed without checking further
            proceed = true;
         }
      }
      return proceed;
   }
}