/*-----------------------------------------------------------------------------
*  Copyright 2007-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.*  
*  
*  Author  Jon Cox  <jcox@alfresco.com>
*  File    AVMSubmitTransactionListener.java
*----------------------------------------------------------------------------*/

package org.alfresco.repo.avm.wf;

import java.util.List;

import org.alfresco.mbeans.VirtServerRegistry;
import org.alfresco.repo.avm.util.RawServices;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.avmsync.AVMDifference;
import org.alfresco.util.VirtServerUtils;
import org.springframework.context.ApplicationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
*  Gets callbacks at critical moments within a transaction
*  (commit, rollback, etc.) to perform JMX update notifications
*  to the virtualization server.
*/
public class AVMSubmitTransactionListener extends TransactionListenerAdapter
{
    private static Log    log = 
        LogFactory.getLog(AVMSubmitTransactionListener.class);

    public AVMSubmitTransactionListener() { }

    /**
    *  Notify virtualization server that webapps in workflow sandbox
    *  are not longer needed, and possibly trigger a notification 
    *  instrucing the virtualization server to reload staging
    *  and every virtual webapp that depends on it.
    */
    @Override
    public void afterCommit()
    {
        List<AVMDifference> stagingDiffs = 
            (List<AVMDifference>)
            AlfrescoTransactionSupport.getResource("staging_diffs");

        if ( stagingDiffs == null) { return; }   // TODO: log this?

        AVMDifference requiresUpdate = null;
         
        for (AVMDifference diff : stagingDiffs)
        {
            // Example values:
            //
            // diff.getSourceVersion() == -1; diff.getSourcePath() ==
            //   mysite--workflow-21edf548-b17e-11db-bd90-35dd2ee4a5c6:/www/avm_webapps/ROOT/x.txt
            //
            // diff.getDestinationVersion() == -1;  diff.getDestinationPath() ==
            //   mysite:/www/avm_webapps/ROOT/x.txt

            if ( requiresUpdate == null )
            {
                if ( VirtServerUtils.requiresUpdateNotification(  diff.getDestinationPath() ) )
                {
                    requiresUpdate = diff;
                }
            }
        }

        ApplicationContext springContext   = RawServices.Instance().getContext();
        VirtServerRegistry vServerRegistry = (VirtServerRegistry) 
                                             springContext.getBean("VirtServerRegistry");



        // TODO: In the future, we might want to allow a single submit to
        //       update multiple staging areas & versions.  If so, 
        //       the logic above will have to look for each unique 
        //       version/webapp tuple, rather than assume everything
        //       is going into the same version and into the same
        //       store/webapp.


        // Only update staging if necessary
        if ( requiresUpdate != null )
        {
            vServerRegistry.updateAllWebapps( requiresUpdate.getDestinationVersion(),
                                              requiresUpdate.getDestinationPath(),
                                              true
                                             );
            if (log.isDebugEnabled())
                log.debug("JMX update to virt server called after commit."     + 
                          "  Version: " + requiresUpdate.getDestinationVersion() + 
                          "  Path: "    + requiresUpdate.getDestinationPath());
        }

        // Remove virtual webapps from workflow sandbox prior to 
        // AVMRemoveWFStoreHandler in the "process-end" clause.
        // This way, even if the workflow is aborted, the JMX message
        // to the virt server is still sent.  Therefore, no longer 
        // doing this here:
        //
        //  if ( ! stagingDiffs.isEmpty() )
        //  {
        //      // All the files are from the same workflow sandbox;
        //      // so to remove all the webapps, you just need to
        //      // look at the 1st difference
        //
        //      AVMDifference d = stagingDiffs.iterator().next();
        //      vServerRegistry.removeAllWebapps( d.getSourceVersion(), 
        //                                        d.getSourcePath(), true );
        //  }

        AlfrescoTransactionSupport.unbindResource("staging_diffs");

        if (log.isDebugEnabled())
            log.debug("staging_diff resource unbound after commit");
    }


    /**
    *  Handle failed transaction.
    */
    @Override
    public void afterRollback()
    {
        AlfrescoTransactionSupport.unbindResource("staging_diffs");

        if (log.isDebugEnabled())
            log.debug("staging_diff resource unbound after rollback");
    }
}
