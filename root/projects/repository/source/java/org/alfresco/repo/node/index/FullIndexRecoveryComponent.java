/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.node.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.springframework.extensions.surf.util.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.Transaction;
import org.alfresco.repo.node.index.AbstractReindexComponent.InIndex;
import org.alfresco.repo.node.index.IndexTransactionTracker.IndexTransactionTrackerListener;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Component to check and recover the indexes.  By default, the server is
 * put into read-only mode during the reindex process in order to prevent metadata changes.
 * This is not critical and can be {@link #setLockServer(boolean) switched off} if the
 * server is required immediately.
 * <p>
 * 
 * @see RecoveryMode
 * 
 * @author Derek Hulley
 */
public class FullIndexRecoveryComponent extends AbstractReindexComponent
{
    private static final String ERR_INDEX_OUT_OF_DATE = "index.recovery.out_of_date";
    private static final String MSG_TRACKING_STARTING = "index.tracking.starting";
    private static final String MSG_TRACKING_COMPLETE = "index.tracking.complete";
    private static final String MSG_TRACKING_PROGRESS = "index.tracking.progress";
    private static final String MSG_RECOVERY_STARTING = "index.recovery.starting";
    private static final String MSG_RECOVERY_COMPLETE = "index.recovery.complete";
    private static final String MSG_RECOVERY_PROGRESS = "index.recovery.progress";
    private static final String MSG_RECOVERY_TERMINATED = "index.recovery.terminated";
    private static final String MSG_RECOVERY_ERROR = "index.recovery.error";
    
    private static Log logger = LogFactory.getLog(FullIndexRecoveryComponent.class);
    
    public static enum RecoveryMode
    {
        /** Do nothing - not even a check. */
        NONE,
        /**
         * Perform a quick check on the state of the indexes only.  This only checks that the
         * first N transactions are present in the index and doesn't guarantee that the indexes
         * are wholely consistent.  Normally, the indexes are consistent up to a certain time.
         * The system does a precautionary index top-up by default, so the last transactions are
         * not validated.
         */
        VALIDATE,
        /**
         * Performs a validation and starts a recovery if necessary.  In this mode, if start
         * transactions are missing then FULL mode is enabled.  If end transactions are missing
         * then the indexes will be "topped up" to bring them up to date.
         */
        AUTO,
        /**
         * Performs a full pass-through of all recorded transactions to ensure that the indexes
         * are up to date.
         */
        FULL;
    }
    
    private RecoveryMode recoveryMode;
    private boolean lockServer;
    private IndexTransactionTracker indexTracker;
    private boolean stopOnError;
    private int maxTransactionsPerLuceneCommit;
    
    /**
     * <ul>
     *   <li><b>recoveryMode: </b>VALIDATE</li>
     *   <li><b>stopOnError:</b> true</li>
     * </ul>
     *
     */
    public FullIndexRecoveryComponent()
    {
        recoveryMode = RecoveryMode.VALIDATE;
        maxTransactionsPerLuceneCommit = 100;
    }

    /**
     * Set the type of recovery to perform.  Default is {@link RecoveryMode#VALIDATE to validate}
     * the indexes only.
     * 
     * @param recoveryMode one of the {@link RecoveryMode } values
     */
    public void setRecoveryMode(String recoveryMode)
    {
        this.recoveryMode = RecoveryMode.valueOf(recoveryMode);
    }

    /**
     * Set the number of transactions to process per Lucene write.
     * Larger values generate less contention on the Lucene IndexInfo files.
     */
    public void setMaxTransactionsPerLuceneCommit(int maxTransactionsPerLuceneCommit)
    {
        this.maxTransactionsPerLuceneCommit = maxTransactionsPerLuceneCommit;
    }

    /**
     * Set this on to put the server into READ-ONLY mode for the duration of the index recovery.
     * The default is <tt>true</tt>, i.e. the server will be locked against further updates.
     * 
     * @param lockServer true to force the server to be read-only
     */
    public void setLockServer(boolean lockServer)
    {
        this.lockServer = lockServer;
    }

    /**
     * Set the tracker that will be used for AUTO mode.
     * 
     * @param indexTracker      an index tracker component
     */
    public void setIndexTracker(IndexTransactionTracker indexTracker)
    {
        this.indexTracker = indexTracker;
    }

    /**
     * Set whether a full rebuild should stop in the event of encoutering an error.  The default is
     * to stop reindexing, and this will lead to the server startup failing when index recovery mode
     * is <b>FULL</b>.  Sometimes, it is necessary to start the server up regardless of any errors
     * with particular nodes.
     * 
     * @param stopOnError       <tt>true</tt> to stop reindexing when an error is encountered.
     */
    public void setStopOnError(boolean stopOnError)
    {
        this.stopOnError = stopOnError;
    }

    @Override
    protected void reindexImpl()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Performing index recovery for type: " + recoveryMode);
        }
        
        // Ignore when NONE
        if (recoveryMode == RecoveryMode.NONE)
        {
            return;
        }
        
        // put the server into read-only mode for the duration
        boolean allowWrite = !transactionService.isReadOnly();
        try
        {
            if (lockServer)
            {
                // set the server into read-only mode
                transactionService.setAllowWrite(false);
            }
            
            int startSample = 10;
            InIndex startAllPresent;
            do
            {
                // Check that the first and last meaningful transactions are indexed 
                List<Transaction> startTxns = nodeDaoService.getTxnsByCommitTimeAscending(
                        Long.MIN_VALUE, Long.MAX_VALUE, startSample, null, false);
                startAllPresent = areTxnsInStartSample(startTxns);
                startSample += 10;
                if(startSample > 1000)
                {
                    startAllPresent = InIndex.NO;
                    break;
                }
            }
            while(startAllPresent == InIndex.INDETERMINATE);
            
            int endSample = 10;
            InIndex endAllPresent;
            do
            {
                List<Transaction> endTxns = nodeDaoService.getTxnsByCommitTimeDescending(
                        Long.MIN_VALUE, Long.MAX_VALUE, endSample, null, false);
                endAllPresent = areAllTxnsInEndSample(endTxns);
                endSample += 10;
                if(endSample > 1000)
                {
                    endAllPresent = InIndex.NO;
                    break;
                }
            }
            while(endAllPresent == InIndex.INDETERMINATE);
            
            // check the level of cover required
            switch (recoveryMode)
            {
            case AUTO:
                if (startAllPresent == InIndex.NO)
                {
                    // Initial transactions are missing - rebuild
                    performFullRecovery();
                }
                else if (endAllPresent == InIndex.NO)
                {
                    performPartialRecovery();
                }
                break;
            case VALIDATE:
                // Check
                if ((startAllPresent == InIndex.NO) || (endAllPresent == InIndex.NO))
                {
                    // Index is out of date
                    logger.warn(I18NUtil.getMessage(ERR_INDEX_OUT_OF_DATE));
                }
                break;
            case FULL:
                performFullRecovery();
                break;
            }
        }
        finally
        {
            // restore read-only state
            transactionService.setAllowWrite(allowWrite);
        }
        
    }
    
    /**
     * @return          Returns <tt>false</tt> if any one of the transactions aren't in the index.
     */
    protected InIndex areAllTxnsInEndSample(List<Transaction> txns)
    {
        int yesCount = 0;
        for (Transaction txn : txns)
        {
            if (isTxnPresentInIndex(txn) == InIndex.NO)
            {
                // Missing txn
                return InIndex.NO;
            }
            if (isTxnPresentInIndex(txn) == InIndex.YES)
            {
                yesCount++;
            }
        }
        // Work around for TX that is written to the repo at start up .. must be more than one real add /update
        if(yesCount > 1)
        {
            return InIndex.YES;
        }
        else
        {
            return InIndex.INDETERMINATE;
        }
    }
    
    protected InIndex areTxnsInStartSample(List<Transaction> txns)
    {
        InIndex current = InIndex.INDETERMINATE;
        for (Transaction txn : txns)
        {
            current = isTxnPresentInIndex(txn);
            if (current == InIndex.NO)
            {
                // Missing txn
                return InIndex.NO;
            } 
        }
        return current;
    }
    
    private void performPartialRecovery()
    {
        // Log the AUTO recovery
        IndexTransactionTrackerListener trackerListener = new IndexTransactionTrackerListener()
        {
            long lastLogged = 0L;
            public void indexedTransactions(long fromTimeInclusive, long toTimeExclusive)
            {
                long now = System.currentTimeMillis();
                if (now - lastLogged < 10000L)
                {
                    // Don't log more than once a minute
                    return;
                }
                lastLogged = now;
                // Log it
                Date toTimeDate = new Date(toTimeExclusive);
                String msgAutoProgress = I18NUtil.getMessage(MSG_TRACKING_PROGRESS, toTimeDate.toString());
                logger.info(msgAutoProgress);
            }
        };
        try
        {
            // Register the listener
            indexTracker.setListener(trackerListener);
            // Trigger the tracker, which will top up the indexes
            logger.info(I18NUtil.getMessage(MSG_TRACKING_STARTING));
            indexTracker.reindex();
            logger.info(I18NUtil.getMessage(MSG_TRACKING_COMPLETE));
        }
        finally
        {
            // Remove the listener
            indexTracker.setListener(null);
        }
    }
    
    private static final int MAX_TRANSACTIONS_PER_ITERATION = 1000;
    private void performFullRecovery()
    {
        int txnCount = nodeDaoService.getTransactionCount();
        // starting
        String msgStart = I18NUtil.getMessage(MSG_RECOVERY_STARTING, txnCount);
        logger.info(msgStart);
        
        // count the transactions
        int processedCount = 0;
        long fromTimeInclusive = Long.MIN_VALUE;
        long toTimeExclusive = Long.MAX_VALUE;
        List<Long> lastTxnIds = Collections.<Long>emptyList();
        while(true)
        {
            List<Transaction> nextTxns = nodeDaoService.getTxnsByCommitTimeAscending(
                    fromTimeInclusive,
                    toTimeExclusive,
                    MAX_TRANSACTIONS_PER_ITERATION,
                    lastTxnIds,
                    false);

            lastTxnIds = new ArrayList<Long>(nextTxns.size());
            // reindex each transaction
            List<Long> txnIdBuffer = new ArrayList<Long>(maxTransactionsPerLuceneCommit);
            Iterator<Transaction> txnIterator = nextTxns.iterator();
            while (txnIterator.hasNext())
            {
                Transaction txn = txnIterator.next();
                Long txnId = txn.getId();
                // Keep it to ensure we exclude it from the next iteration
                lastTxnIds.add(txnId);
                // check if we have to terminate
                if (isShuttingDown())
                {
                    String msgTerminated = I18NUtil.getMessage(MSG_RECOVERY_TERMINATED);
                    logger.warn(msgTerminated);
                    return;
                }
                // Allow exception to bubble out or not
                if (stopOnError)
                {
                    reindexTransaction(txnId);
                }
                else
                {
                    // Add the transaction ID to the buffer
                    txnIdBuffer.add(txnId);
                    // Reindex if the buffer is full or if there are no more transactions
                    if (!txnIterator.hasNext() || txnIdBuffer.size() >= maxTransactionsPerLuceneCommit)
                    {
                        try
                        {
                            reindexTransactionAsynchronously(txnIdBuffer);
                        }
                        catch (Throwable e)
                        {
                            String msgError = I18NUtil.getMessage(MSG_RECOVERY_ERROR, txnId, e.getMessage());
                            logger.info(msgError, e);
                        }
                        // Clear the buffer
                        txnIdBuffer = new ArrayList<Long>(maxTransactionsPerLuceneCommit);
                    }
                }
                // Although we use the same time as this transaction for the next iteration, we also
                // make use of the exclusion list to ensure that it doesn't get pulled back again.
                fromTimeInclusive = txn.getCommitTimeMs();
                
                // dump a progress report every 10% of the way
                double before = (double) processedCount / (double) txnCount * 10.0;     // 0 - 10 
                processedCount++;
                double after = (double) processedCount / (double) txnCount * 10.0;      // 0 - 10
                if (Math.floor(before) < Math.floor(after))                             // crossed a 0 - 10 integer boundary
                {
                    int complete = ((int)Math.floor(after))*10;
                    String msgProgress = I18NUtil.getMessage(MSG_RECOVERY_PROGRESS, complete);
                    logger.info(msgProgress);
                }
            }
            
            // Wait for the asynchronous process to catch up
            waitForAsynchronousReindexing();
            
            // have we finished?
            if (nextTxns.size() == 0)
            {
                // there are no more
                break;
            }
        }
        // done
        String msgDone = I18NUtil.getMessage(MSG_RECOVERY_COMPLETE);
        logger.info(msgDone);
    }
    
    /**
     * Perform full reindexing of the given transaction.  A read-only transaction is created
     * <b>if one doesn't already exist</b>.
     * 
     * @param txnId the transaction identifier
     */
    public void reindexTransaction(final long txnId)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Reindexing transaction: " + txnId);
        }
        
        RetryingTransactionCallback<Object> reindexWork = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                // get the node references pertinent to the transaction
                List<NodeRef> nodeRefs = nodeDaoService.getTxnChanges(txnId);
                // reindex each node
                for (NodeRef nodeRef : nodeRefs)
                {
                    Status nodeStatus = nodeService.getNodeStatus(nodeRef);
                    if (nodeStatus == null)
                    {
                        // it's not there any more
                        continue;
                    }
                    if (nodeStatus.isDeleted())                                 // node deleted
                    {
                        // only the child node ref is relevant
                        ChildAssociationRef assocRef = new ChildAssociationRef(
                                ContentModel.ASSOC_CHILDREN,
                                null,
                                null,
                                nodeRef);
                      indexer.deleteNode(assocRef);
                    }
                    else                                                        // node created
                    {
                        // reindex
                        indexer.updateNode(nodeRef);
                    }
                }
                // done
                return null;
            }
        };
        transactionService.getRetryingTransactionHelper().doInTransaction(reindexWork, true, false);
        // done
    }
}