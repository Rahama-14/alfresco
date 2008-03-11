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
package org.alfresco.repo.domain.hibernate;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.avm.hibernate.SessionCacheChecker;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.util.resource.MethodResourceManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.engine.EntityKey;
import org.hibernate.stat.SessionStatistics;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * A Hibernate-specific resource manager that ensures that the current <code>Session</code>'s
 * entity count doesn't exceed a given threshold.
 * <p/>
 * <b>NOTE: VERY IMPORTANT</b><br/>
 * Do not, under any circumstances, attach an instance of this class to an API that
 * passes stateful objects back and forth.  There must be no <code>Session</code>-linked
 * objects up the stack from where this instance resides.  Failure to observe this will
 * most likely result in data loss of a sporadic nature.
 *
 * @see org.alfresco.repo.domain.hibernate.HibernateNodeTest#testPostCommitClearIssue()
 *
 * @author Derek Hulley
 */
public class SessionSizeResourceManager extends HibernateDaoSupport implements MethodResourceManager
{
    /** key to store the local flag to disable resource control during the current transaction */
    private static final String KEY_DISABLE_IN_TRANSACTION = "SessionSizeResourceManager.DisableInTransaction";

    private static Log logger = LogFactory.getLog(SessionSizeResourceManager.class);

    /** Default 1000 */
    private int threshold;

    /**
     * Disable resource management for the duration of the current transaction.  This is temporary
     * and relies on an active transaction.
     */
    public static void setDisableInTransaction()
    {
        AlfrescoTransactionSupport.bindResource(KEY_DISABLE_IN_TRANSACTION, Boolean.TRUE);
    }
    
    /**
     * Enable resource management for the duration of the current transaction.  This is temporary
     * and relies on an active transaction.
     */
    public static void setEnableInTransaction()
    {
        AlfrescoTransactionSupport.bindResource(KEY_DISABLE_IN_TRANSACTION, Boolean.FALSE);
    }

    /**
     * @return Returns true if the resource management must be ignored in the current transaction.
     *      If <code>false</code>, the global setting will take effect.
     *
     * @see #setDisableInTransaction()
     */
    public static boolean isDisableInTransaction()
    {
        Boolean disableInTransaction = (Boolean) AlfrescoTransactionSupport.getResource(KEY_DISABLE_IN_TRANSACTION);
        if (disableInTransaction == null || disableInTransaction == Boolean.FALSE)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Default public constructor required for bean instantiation.
     */
    public SessionSizeResourceManager()
    {
        this.threshold = 1000;
    }

    /**
     * Set the {@link Session#clear()} threshold.  If the number of entities and collections in the
     * current session exceeds this number, then the session will be cleared.  Have you read the
     * disclaimer?
     *
     * @param threshold the maximum number of entities and associations to keep in memory
     *
     * @see #threshold
     */
    public void setThreshold(int threshold)
    {
        this.threshold = threshold;
    }

    public void manageResources(
            Map<Method, MethodStatistics> methodStatsByMethod,
            long transactionElapsedTimeNs,
            Method currentMethod)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Session Size Manager Invoked.");
            SessionCacheChecker.instance.check();
        }
        if (isDisableInTransaction())
        {
            // Don't do anything
            return;
        }
        // We are go for interfering
        Session session = getSession(false);
        SessionStatistics stats = session.getStatistics();
        int entityCount = stats.getEntityCount();
        int collectionCount = stats.getCollectionCount();
        if ((entityCount + collectionCount) > threshold)
        {
            session.flush();
            selectivelyClear(session, stats);
            // session.clear();
            if (logger.isDebugEnabled())
            {
                String msg = String.format(
                        "Cleared %5d entities and %5d collections from Hibernate Session",
                        entityCount,
                        collectionCount);
                logger.debug(msg);
            }
        }
    }
    
    /**
     * Clear the session now.
     * 
     * @param session
     */
    public static void clear(Session session)
    {
        SessionStatistics stats = session.getStatistics();
        selectivelyClear(session, stats);
    }
    
    
    @SuppressWarnings("unchecked")
    private static void selectivelyClear(Session session, SessionStatistics stats)
    {
        if (logger.isDebugEnabled())
        {
            logger.error(stats);
        }
        Set<EntityKey> keys = new HashSet<EntityKey>((Set<EntityKey>)stats.getEntityKeys());
        for (EntityKey key : keys)
        {
            // This should probably be configurable but frankly the nauseous extrusion of Gavin King's
            // programmatic alimentary tract (hibernate) will go away before this could make a difference.
            if (!key.getEntityName().startsWith("org.alfresco"))
            {
                continue;
            }
            Object val = session.get(key.getEntityName(), key.getIdentifier());
            if (val != null)
            {
                session.evict(val);
            }
        }
    }
}
