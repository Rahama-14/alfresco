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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.repo.admin.patch.impl;

import java.util.Map;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.domain.AccessControlListDAO;
import org.alfresco.repo.domain.hibernate.AclDaoComponentImpl;
import org.alfresco.repo.security.permissions.ACLType;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;

/**
 * Migrate permissions from the OLD format to defining, shared and layered
 */
public class AVMPermissionsPatch extends AbstractPatch
{

    private static final String MSG_SUCCESS = "patch.updateAvmPermissions.result";
    
    private AccessControlListDAO accessControlListDao;

    private AclDaoComponentImpl aclDaoComponent;
    
    @Override
    protected String applyInternal() throws Exception
    {
        Thread progressThread = null;
        if (aclDaoComponent.supportsProgressTracking())
        {
            Long toDo = aclDaoComponent.getAVMHeadNodeCount();
            Long maxId = aclDaoComponent.getMaxAclId();

            progressThread = new Thread(new ProgressWatcher(toDo, maxId), "WCMPactchProgressWatcher");
            progressThread.start();
        }
        
        Map<ACLType, Integer> summary = accessControlListDao.patchAcls();
        
        if (progressThread != null)
        {
            progressThread.interrupt();
            progressThread.join();
        }
        
        // build the result message
        String msg = I18NUtil.getMessage(MSG_SUCCESS, summary.get(ACLType.DEFINING), summary.get(ACLType.LAYERED));
        // done
        return msg;
    }

    public void setAccessControlListDao(AccessControlListDAO accessControlListDao)
    {
        this.accessControlListDao = accessControlListDao;
    }

    public void setAclDaoComponent(AclDaoComponentImpl aclDaoComponent)
    {
        this.aclDaoComponent = aclDaoComponent;
    }
    
    
    private class ProgressWatcher implements Runnable
    {
        private boolean running = true;

        Long toDo;

        Long max;

        ProgressWatcher(Long toDo, Long max)
        {
            this.toDo = toDo;
            this.max = max;
        }

        public void run()
        {
            while (running)
            {
                try
                {
                    Thread.sleep(60000);
                }
                catch (InterruptedException e)
                {
                    running = false;
                }

                if (running)
                {
                    RetryingTransactionHelper txHelper = transactionService.getRetryingTransactionHelper();
                    txHelper.setMaxRetries(1);
                    Long done = txHelper.doInTransaction(new RetryingTransactionCallback<Long>()
                    {

                        public Long execute() throws Throwable
                        {
                            return aclDaoComponent.getAVMNodeCountWithNewACLS(max);
                        }
                    }, true, true);

                    reportProgress(toDo, done);
                }
            }
        }

    }
    
}
