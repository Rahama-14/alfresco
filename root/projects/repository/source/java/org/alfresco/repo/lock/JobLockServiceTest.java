/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.repo.lock;

import junit.framework.TestCase;

import org.alfresco.repo.domain.locks.LockDAO;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

/**
 * Tests the high-level capabilities provided by the service implementation.  The DAO tests
 * stress the underlying database work, so we only need to deal with deadlock resolution, etc.
 * 
 * @see JobLockService      the service being tested
 * @see LockDAO             the DAO being indirectly tested
 * 
 * @author Derek Hulley
 * @since 3.2
 */
public class JobLockServiceTest extends TestCase
{
    public static final String NAMESPACE = "http://www.alfresco.org/test/JobLockServiceTest";
    
    private ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();

    private TransactionService transactionService;
    private RetryingTransactionHelper txnHelper;
    private JobLockService jobLockService;
    // Lock names for the tests
    private QName lockA;
    private QName lockAA;
    private QName lockAAA;
    private QName lockAAB;
    private QName lockAAC;
    private QName lockAB;
    private QName lockABA;
    private QName lockABB;
    private QName lockABC;
    
    @Override
    public void setUp() throws Exception
    {
        ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        transactionService = serviceRegistry.getTransactionService();
        txnHelper = transactionService.getRetryingTransactionHelper();
        
        jobLockService = (JobLockService) ctx.getBean("jobLockService");
        
        // Get the test name
        String testName = getName();
        // Build lock names for the test
        lockA = QName.createQName(NAMESPACE, "a-" + testName);
        lockAA = QName.createQName(NAMESPACE, "a-" + testName + ".a-" + testName);
        lockAAA = QName.createQName(NAMESPACE, "a-" + testName + ".a-" + testName + ".a-" + testName);
        lockAAB = QName.createQName(NAMESPACE, "a-" + testName + ".a-" + testName + ".b-" + testName);
        lockAAC = QName.createQName(NAMESPACE, "a-" + testName + ".a-" + testName + ".c-" + testName);
        lockAB = QName.createQName(NAMESPACE, "a-" + testName + ".b-" + testName);
        lockABA = QName.createQName(NAMESPACE, "a-" + testName + ".b-" + testName + ".a-" + testName);
        lockABB = QName.createQName(NAMESPACE, "a-" + testName + ".b-" + testName + ".b-" + testName);
        lockABC = QName.createQName(NAMESPACE, "a-" + testName + ".b-" + testName + ".c-" + testName);
    }
    
    public void testSetUp()
    {
        assertNotNull(jobLockService);
    }
    
    public void testEnforceTxn()
    {
        try
        {
            jobLockService.getTransacionalLock(lockAAA, 50L);
            fail("Service did not enforce the presence of a transaction");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }
    }
    
    /**
     * Checks that the lock can be aquired by a read-only transaction i.e. that locking is
     * independent of the outer transaction.
     */
    public void testLockInReadOnly() throws Exception
    {
        RetryingTransactionCallback<Object> lockCallback = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Throwable
            {
                jobLockService.getTransacionalLock(lockAAA, 500);
                return null;
            }
        };
        txnHelper.doInTransaction(lockCallback, true, true);
    }
    
    /**
     * Checks that locks are released on commit
     */
    public void testLockReleaseOnCommit() throws Exception
    {
        RetryingTransactionCallback<Object> lockCallback = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Throwable
            {
                jobLockService.getTransacionalLock(lockAAA, 5000);
                return null;
            }
        };
        txnHelper.doInTransaction(lockCallback, true, true);
        // The lock should be free now, even though the TTL was high
        RetryingTransactionCallback<Object> lockCheckCallback = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Throwable
            {
                jobLockService.getTransacionalLock(lockAAA, 50);
                return null;
            }
        };
        txnHelper.doInTransaction(lockCheckCallback, true, true);
    }
    
    /**
     * Checks that locks are released on rollback
     */
    public void testLockReleaseOnRollback() throws Exception
    {
        RetryingTransactionCallback<Object> lockCallback = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Throwable
            {
                jobLockService.getTransacionalLock(lockAAA, 5000);
                throw new UnsupportedOperationException("ALERT!");
            }
        };
        try
        {
            txnHelper.doInTransaction(lockCallback, true, true);
            fail("Expected transaction failure");
        }
        catch (UnsupportedOperationException e)
        {
            // Expected
        }
        // The lock should be free now, even though the TTL was high
        RetryingTransactionCallback<Object> lockCheckCallback = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Throwable
            {
                jobLockService.getTransacionalLock(lockAAA, 50);
                return null;
            }
        };
        txnHelper.doInTransaction(lockCheckCallback, true, true);
    }
    
    /**
     * Sets up two threads in a deadlock scenario.  Each of the threads has a long wait timeout
     * for the required locks.  If there were a deadlock, the shorter of the the wait times would
     * be how long it would take before one of them is thrown out.  Firstly, we check that one
     * of the threads <i>is</i> thrown out.  Then we check that the thread is thrown out quickly.
     */
    public synchronized void testDeadlockPrevention() throws Throwable
    {
        DeadlockingThread t1 = new DeadlockingThread(lockAAA, lockAAB);
        DeadlockingThread t2 = new DeadlockingThread(lockAAB, lockAAA);
        // Start them
        t1.start();
        t2.start();
        // They can take their first locks (there should be no contention)
        t1.incrementNextLock();
        t2.incrementNextLock();
        // Wait for them to do this
        try { this.wait(2000L); } catch (InterruptedException e) {}
        // Advance again
        t1.incrementNextLock();
        t2.incrementNextLock();
        // Wait for them to do this
        try { this.wait(2000L); } catch (InterruptedException e) {}
        // Advance again, to end threads
        t1.incrementNextLock();
        t2.incrementNextLock();
        // Wait for them to end (commit/rollback)
        try { this.wait(2000L); } catch (InterruptedException e) {}
        
        if (t1.otherFailure != null)
        {
            throw t1.otherFailure;
        }
        if (t2.otherFailure != null)
        {
            throw t2.otherFailure;
        }
        assertNull("T1 should have succeeded as the ordered locker: " + t1.lockFailure, t1.lockFailure);
        assertNotNull("T2 should have failed as the unordered locker.", t2.lockFailure);
    }
    
    private class DeadlockingThread extends Thread
    {
        private final QName[] lockQNames;
        private volatile int nextLock = -1;
        private LockAcquisitionException lockFailure;
        private Throwable otherFailure;
        
        private DeadlockingThread(QName ... lockQNames)
        {
            super("DeadlockingThread");
            this.lockQNames = lockQNames;
            setDaemon(true);
        }
        
        private void incrementNextLock()
        {
            nextLock++;
        }
        
        @Override
        public void run()
        {
            RetryingTransactionCallback<Object> runCallback = new RetryingTransactionCallback<Object>()
            {
                public synchronized Object execute() throws Throwable
                {
                    int currentLock = -1;
                    // Take the locks in turn, quitting when told to take a lock that's not there
                    while (currentLock < lockQNames.length - 1)
                    {
                        // Check if we have been instructed to take a lock
                        if (nextLock > currentLock)
                        {
                            // Advance and grab the lock
                            currentLock++;
                            jobLockService.getTransacionalLock(lockQNames[currentLock], 5000L);
                        }
                        else
                        {
                            // No advance, so wait a bit more
                            try { this.wait(20L); } catch (InterruptedException e) {}
                        }
                    }
                    return null;
                }
            };
            try
            {
                txnHelper.doInTransaction(runCallback, true);
            }
            catch (LockAcquisitionException e)
            {
                lockFailure = e;
            }
            catch (Throwable e)
            {
                otherFailure = e;
            }
        }
    }
}
