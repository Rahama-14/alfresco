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
package org.alfresco.repo.domain.avm;

import junit.framework.TestCase;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.alfresco.util.GUID;
import org.springframework.extensions.surf.util.Pair;
import org.springframework.context.ApplicationContext;

/**
 * @see AVMStoreDAO
 * 
 * @author janv
 * @since 3.2
 */
public class AVMStoreDAOTest extends TestCase
{
    private ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();

    private TransactionService transactionService;
    private RetryingTransactionHelper txnHelper;
    private AVMStoreDAO avmStoreDAO;
    
    @Override
    public void setUp() throws Exception
    {
        ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        transactionService = serviceRegistry.getTransactionService();
        txnHelper = transactionService.getRetryingTransactionHelper();
        
        avmStoreDAO = (AVMStoreDAO) ctx.getBean("newAvmStoreDAO");
    }
    
    private AVMStoreEntity get(final String avmStoreName, final boolean autoCreate, boolean expectSuccess)
    {
        RetryingTransactionCallback<AVMStoreEntity> callback = new RetryingTransactionCallback<AVMStoreEntity>()
        {
            public AVMStoreEntity execute() throws Throwable
            {
                AVMStoreEntity avmStoreEntity = null;
                if (autoCreate)
                {
                    avmStoreEntity = avmStoreDAO.createStore(avmStoreName);
                }
                else
                {
                    avmStoreEntity = avmStoreDAO.getStore(avmStoreName);
                }
                return avmStoreEntity;
            }
        };
        try
        {
            return txnHelper.doInTransaction(callback, !autoCreate, false);
        }
        catch (Throwable e)
        {
            if (expectSuccess)
            {
                // oops
                throw new RuntimeException("Expected to get avmStore '" + avmStoreName + "'.", e);
            }
            else
            {
                return null;
            }
        }
    }
    
    public void testCreateWithCommit() throws Exception
    {
        // Create an avmStore
        String avmStore = GUID.generate();
        AVMStoreEntity avmStoreEntity = get(avmStore, true, true);
        // Check that it can be retrieved
        AVMStoreEntity avmStoreCheck = get(avmStoreEntity.getName(), false, true);
        assertEquals("avmStore ID changed", avmStoreEntity.getId(), avmStoreCheck.getId());
    }
    
    public void testCreateWithRollback() throws Exception
    {
        final String avmStore = GUID.generate();
        // Create an avmStore
        RetryingTransactionCallback<Pair<Long, String>> callback = new RetryingTransactionCallback<Pair<Long, String>>()
        {
            public Pair<Long, String> execute() throws Throwable
            {
                get(avmStore, true, true);
                // Now force a rollback
                throw new RuntimeException("Forced");
            }
        };
        try
        {
            txnHelper.doInTransaction(callback);
            fail("Transaction didn't roll back");
        }
        catch (RuntimeException e)
        {
            // Expected
        }
        // Check that it doesn't exist
        get(avmStore, false, false);
    }
    
    // TODO - review
    public void testCaseInsensitivity() throws Exception
    {
        String avmStore = "AAA-" + GUID.generate();
        AVMStoreEntity lowercase = get(avmStore.toLowerCase(), true, true);
        // Check that the same pair is retrievable using uppercase
        AVMStoreEntity uppercase = get(avmStore.toUpperCase(), false, true);
        assertNotNull(uppercase);
        assertEquals(
                "Upper and lowercase avmStore instance IDs were not the same",
                lowercase.getId(), uppercase.getId());
    }
    
    public void testDelete() throws Exception
    {
        // Create an avmStore
        String avmStore = GUID.generate();
        AVMStoreEntity avmStoreEntity = get(avmStore, true, true);
        
        getAndCheck(avmStoreEntity.getId(), avmStoreEntity);
        
        avmStoreDAO.deleteStore(avmStoreEntity.getId());
        try
        {
            getAndCheck(avmStoreEntity.getId(), avmStoreEntity);
            fail("Entity still exists");
        }
        catch (Throwable e)
        {
            // Expected
        }
    }
    
    public void testUpdate() throws Exception
    {
        // TODO
    }
    
    /**
     * Retrieves and checks the AVMStore for equality
     */
    private void getAndCheck(final Long avmStoreId, AVMStoreEntity checkAVMStoreEntity)
    {
        RetryingTransactionCallback<AVMStoreEntity> callback = new RetryingTransactionCallback<AVMStoreEntity>()
        {
            public AVMStoreEntity execute() throws Throwable
            {
                AVMStoreEntity storeEntity = avmStoreDAO.getStore(avmStoreId);
                return storeEntity;
            }
        };
        AVMStoreEntity result = txnHelper.doInTransaction(callback, true, false);
        assertNotNull("Failed to find result for ID " + avmStoreId, result);
        assertEquals("ContentData retrieved not the same as persisted: ", checkAVMStoreEntity, result);
    }
}
