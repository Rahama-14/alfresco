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
package org.alfresco.repo.transaction;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.alfresco.repo.cache.EhCacheAdapter;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @see org.alfresco.repo.transaction.TransactionServiceImpl
 * 
 * @author Derek Hulley
 */
public class TransactionServiceImplTest extends TestCase
{
    private static ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
    
    private PlatformTransactionManager transactionManager;
    private TransactionServiceImpl transactionService;
    private NodeService nodeService;
    
    public void setUp() throws Exception
    {
        transactionManager = (PlatformTransactionManager) ctx.getBean("transactionManager");
        transactionService = new TransactionServiceImpl();
        transactionService.setTransactionManager(transactionManager);
        
        CacheManager cacheManager = new CacheManager();
        Cache sysAdminEhCache = new Cache("sysAdminCache", 10, false, true, 0L, 0L);
        cacheManager.addCache(sysAdminEhCache);      
        EhCacheAdapter<String, Object> sysAdminCache = new EhCacheAdapter<String, Object>();
        sysAdminCache.setCache(sysAdminEhCache);

        transactionService.setSysAdminCache(sysAdminCache);
        
        transactionService.setAllowWrite(true);

        nodeService = (NodeService) ctx.getBean("dbNodeService");
    }
    
    public void testPropagatingTxn() throws Exception
    {
        // start a transaction
        UserTransaction txnOuter = transactionService.getUserTransaction();
        txnOuter.begin();
        String txnIdOuter = AlfrescoTransactionSupport.getTransactionId();
        
        // start a propagating txn
        UserTransaction txnInner = transactionService.getUserTransaction();
        txnInner.begin();
        String txnIdInner = AlfrescoTransactionSupport.getTransactionId();
        
        // the txn IDs should be the same
        assertEquals("Txn ID not propagated", txnIdOuter, txnIdInner);
        
        // rollback the inner
        txnInner.rollback();
        
        // check both transactions' status
        assertEquals("Inner txn not marked rolled back", Status.STATUS_ROLLEDBACK, txnInner.getStatus());
        assertEquals("Outer txn not marked for rolled back", Status.STATUS_MARKED_ROLLBACK, txnOuter.getStatus());
        
        try
        {
            txnOuter.commit();
            fail("Outer txn not marked for rollback");
        }
        catch (RollbackException e)
        {
            // expected
            txnOuter.rollback();
        }
    }
    
    public void testNonPropagatingTxn() throws Exception
    {
        // start a transaction
        UserTransaction txnOuter = transactionService.getUserTransaction();
        txnOuter.begin();
        String txnIdOuter = AlfrescoTransactionSupport.getTransactionId();
        
        // start a propagating txn
        UserTransaction txnInner = transactionService.getNonPropagatingUserTransaction();
        txnInner.begin();
        String txnIdInner = AlfrescoTransactionSupport.getTransactionId();
        
        // the txn IDs should be different
        assertNotSame("Txn ID not propagated", txnIdOuter, txnIdInner);
        
        // rollback the inner
        txnInner.rollback();

        // outer should commit without problems
        txnOuter.commit();
    }
    
    public void testReadOnlyTxn() throws Exception
    {
        // start a read-only transaction
        transactionService.setAllowWrite(false);
        
        UserTransaction txn = transactionService.getUserTransaction();
        txn.begin();
        
        // do some writing
        try
        {
            nodeService.createStore(
                    StoreRef.PROTOCOL_WORKSPACE,
                    getName() + "_" + System.currentTimeMillis());
            txn.commit();
            fail("Read-only transaction wasn't detected");
        }
        catch (InvalidDataAccessApiUsageException e)
        {
            @SuppressWarnings("unused")
            int i = 0;
            // expected
        }
        finally
        {
            try
            {
                txn.rollback();
            }
            catch (Throwable e) {}
        }
    }
    
    public void testGetRetryingTransactionHelper()
    {
        RetryingTransactionCallback<Object> callback = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Throwable
            {
                return null;
            }
        };
        
        assertFalse("Retriers must be new instances",
                transactionService.getRetryingTransactionHelper() == transactionService.getRetryingTransactionHelper());
        
        transactionService.setAllowWrite(true);
        transactionService.getRetryingTransactionHelper().doInTransaction(callback, true);
        transactionService.getRetryingTransactionHelper().doInTransaction(callback, false);

        transactionService.setAllowWrite(false);
        transactionService.getRetryingTransactionHelper().doInTransaction(callback, true);
        try
        {
            transactionService.getRetryingTransactionHelper().doInTransaction(callback, false);
            fail("Expected AccessDeniedException when starting to write to a read-only transaction service.");
        }
        catch (AccessDeniedException e)
        {
            // Expected
        }
    }
}
