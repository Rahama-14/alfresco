/*
 * Copyright (C) 2005-2006 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.node.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.domain.Transaction;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Component to check and recover the indexes.
 * 
 * @author Derek Hulley
 */
public class IndexTransactionTracker extends AbstractReindexComponent
{
    private static Log logger = LogFactory.getLog(IndexTransactionTracker.class);
    
    private IndexTransactionTrackerListener listener;
    private NodeIndexer nodeIndexer;
    
    private long maxTxnDurationMs;
    private long reindexLagMs;
    private int maxRecordSetSize;
    private int maxTransactionsPerLuceneCommit;
    private boolean disableInTransactionIndexing;
    
    private boolean started;
    private List<Long> previousTxnIds;
    private Long lastMaxTxnId;
    private long fromTimeInclusive;
    private Map<Long, TxnRecord> voids;
    private boolean forceReindex;
    
    /**
     * Set the defaults.
     * <ul>
     *   <li><b>Maximum transaction duration:</b> 1 hour</li>
     *   <li><b>Reindex lag:</b> 1 second</li>
     *   <li><b>Maximum recordset size:</b> 1000</li>
     *   <li><b>Maximum transactions per Lucene commit:</b> 100</li>
     *   <li><b>Disable in-transaction indexing:</b> false</li>
     * </ul>
     */
    public IndexTransactionTracker()
    {
        maxTxnDurationMs = 3600L * 1000L;
        reindexLagMs = 1000L;
        maxRecordSetSize = 1000;
        maxTransactionsPerLuceneCommit = 100;
        disableInTransactionIndexing = false;
        previousTxnIds = Collections.<Long>emptyList();
        lastMaxTxnId = Long.MAX_VALUE;
        fromTimeInclusive = -1L;
        voids = new TreeMap<Long, TxnRecord>();
        forceReindex = false;
    }

    public synchronized void setListener(IndexTransactionTrackerListener listener)
    {
        this.listener = listener;
    }

    public void setNodeIndexer(NodeIndexer nodeIndexer)
    {
        this.nodeIndexer = nodeIndexer;
    }

    /**
     * Set the expected maximum duration of transaction supported.  This value is used to adjust the
     * look-back used to detect transactions that committed.  Values must be greater than zero.
     * 
     * @param maxTxnDurationMinutes     the maximum length of time a transaction will take in minutes
     * 
     * @since 1.4.5, 2.0.5, 2.1.1
     */
    public void setMaxTxnDurationMinutes(long maxTxnDurationMinutes)
    {
        if (maxTxnDurationMinutes < 1)
        {
            throw new AlfrescoRuntimeException("Maximum transaction duration must be at least one minute.");
        }
        this.maxTxnDurationMs = maxTxnDurationMinutes * 60L * 1000L;
    }
    
    /**
     * Transaction tracking should lag by the average commit time for a transaction.  This will minimize
     * the number of holes in the transaction sequence.  Values must be greater than zero.
     * 
     * @param reindexLagMs              the minimum age of a transaction to be considered by
     *                                  the index transaction tracking
     * 
     * @since 1.4.5, 2.0.5, 2.1.1
     */
    public void setReindexLagMs(long reindexLagMs)
    {
        if (reindexLagMs < 1)
        {
            throw new AlfrescoRuntimeException("Reindex lag must be at least 1 millisecond.");
        }
        this.reindexLagMs = reindexLagMs;
    }

    /**
     * Set the number of transactions to request per query.
     */
    public void setMaxRecordSetSize(int maxRecordSetSize)
    {
        this.maxRecordSetSize = maxRecordSetSize;
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
     * Enable or disabled in-transaction indexing.  Under certain circumstances, the system
     * can run with only index tracking enabled - in-transaction indexing is not always
     * required.  The {@link NodeIndexer} is disabled when this component initialises.
     */
    public void setDisableInTransactionIndexing(boolean disableInTransactionIndexing)
    {
        this.disableInTransactionIndexing = disableInTransactionIndexing;
    }

    /**
     * @return      Returns <tt>false</tt> always.  Transactions are handled internally.
     */
    @Override
    protected boolean requireTransaction()
    {
        return false;
    }

    /** Worker callback for transactional use */
    RetryingTransactionCallback<Long> getStartingCommitTimeWork = new RetryingTransactionCallback<Long>()
    {
        public Long execute() throws Exception
        {
            return getStartingTxnCommitTime();
        }
    };
    /** Worker callback for transactional use */
    RetryingTransactionCallback<Boolean> reindexWork = new RetryingTransactionCallback<Boolean>()
    {
        public Boolean execute() throws Exception
        {
            return reindexInTransaction();
        }
    };

    @Override
    protected void reindexImpl()
    {
        RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
        if (!started)
        {
            // Disable in-transaction indexing
            if (disableInTransactionIndexing && nodeIndexer != null)
            {
                logger.warn("In-transaction indexing is being disabled.");
                nodeIndexer.setEnabled(false);
            }
            // Make sure that we start clean
            voids.clear();
            previousTxnIds = new ArrayList<Long>(maxRecordSetSize);
            lastMaxTxnId = null;                                // So that it is ignored at first
            fromTimeInclusive = retryingTransactionHelper.doInTransaction(getStartingCommitTimeWork, true, true);
            started = true;
        }
        
        while (true)
        {
            Boolean repeat = retryingTransactionHelper.doInTransaction(reindexWork, true, true);
            // Only break out if there isn't any more work to do (for now)
            if (repeat == null || repeat.booleanValue() == false)
            {
                break;
            }
        }
        // Wait for the asynchronous reindexing to complete
        waitForAsynchronousReindexing();
    }
     
    /**
     * @return      Returns <tt>true</tt> if the reindex process can exit otherwise <tt>false</tt> if
     *              a new transaction should be created and the process kicked off again
     */
    private boolean reindexInTransaction()
    {
        long toTimeExclusive = System.currentTimeMillis() - reindexLagMs;
        
        // Check that the voids haven't been filled
        long minLiveVoidTime = checkVoids();
        if (minLiveVoidTime <= fromTimeInclusive)
        {
            // A void was discovered.
            // We need to adjust the search time for transactions, i.e. hop back in time but
            // this also entails a full build from that point on.  So all previous transactions
            // need to be reindexed.
            fromTimeInclusive = minLiveVoidTime;
            previousTxnIds.clear();
        }
        
        // get next transactions to index
        List<Transaction> txns = getNextTransactions(fromTimeInclusive, toTimeExclusive, previousTxnIds);
        
        // If there are no transactions, then all the work is done
        if (txns.size() == 0)
        {
            // We have caught up.
            // There is no need to force reindexing until the next unindex transactions appear.
            forceReindex = false;
            return false;
        }
        
        if (logger.isDebugEnabled())
        {
            String msg = String.format(
                    "Reindexing %d transactions from %s (%s)",
                    txns.size(),
                    (new Date(fromTimeInclusive)).toString(),
                    txns.isEmpty() ? "---" : txns.get(0).getId().toString());
            logger.debug(msg);
        }
        
        // Reindex the transactions.  Voids between the last set of transactions and this
        // set will be detected as well.  Additionally, the last max transaction will be
        // updated by this method.
        long maxProcessedTxnCommitTime = reindexTransactions(txns);
        
        // Call the listener
        synchronized (this)
        {
            if (listener != null)
            {
                listener.indexedTransactions(fromTimeInclusive, maxProcessedTxnCommitTime);
            }
        }
        
        // Move the time on.
        // The next fromTimeInclusive may well pull back transactions that have just been
        // processed.  But we keep track of those and exclude them from the results.
        if (fromTimeInclusive == maxProcessedTxnCommitTime)
        {
            // The time didn't advance.  If no new transaction appear, we could spin on
            // two or more transactions with the same commit time.  So we DON'T clear
            // the list of previous transactions and we allow them to live on.
        }
        else
        {
            // The processing time has moved on
            fromTimeInclusive = maxProcessedTxnCommitTime;
            previousTxnIds.clear();
        }
        for (Transaction txn : txns)
        {
            previousTxnIds.add(txn.getId());
        }
        
        if (isShuttingDown())
        {
            // break out if the VM is shutting down
            return false;
        }
        else
        {
            // There is more work to do and we should be called back right away
            return true;
        }
    }
    
    private static final long ONE_HOUR_MS = 3600*1000;
    /**
     * Find a transaction time to start indexing from (inclusive).  The last recorded transaction by ID
     * is taken and the max transaction duration substracted from its commit time.  A transaction is
     * retrieved for this time and checked for indexing.  If it is present, then that value is chosen.
     * If not, a step back in time is taken again.  This goes on until there are no more transactions
     * or a transaction is found in the index.
     */
    protected long getStartingTxnCommitTime()
    {
        long now = System.currentTimeMillis();
        // Get the last indexed transaction for all transactions
        long lastIndexedAllCommitTimeMs = getLastIndexedCommitTime(now, false);
        // Now check back from this time to make sure there are no remote transactions that weren't indexed
        long lastIndexedRemoteCommitTimeMs = getLastIndexedCommitTime(now, true);
        // The one to start at is the least of the two times
        long startTime = Math.min(lastIndexedAllCommitTimeMs, lastIndexedRemoteCommitTimeMs);
        // Done
        return startTime;
    }
    /**
     * Gets the commit time for the last indexed transaction.  If there are no transactions, then the
     * current time is returned.
     * 
     * @param maxCommitTimeMs   the largest commit time to consider
     * @param remoteOnly        <tt>true</tt> to only look at remotely-committed transactions
     * @return                  Returns the last indexed transaction commit time for all or
     *                          remote-only transactions.
     */
    private long getLastIndexedCommitTime(long maxCommitTimeMs, boolean remoteOnly)
    {
        // Look back in time by the maximum transaction duration
        long maxToTimeExclusive = maxCommitTimeMs - maxTxnDurationMs;
        long toTimeExclusive = maxToTimeExclusive;
        long fromTimeInclusive = 0L;
        double stepFactor = 1.0D;
        boolean firstWasInIndex = true;
found:
        while (true)
        {
            // Get the most recent transaction before the given look-back
            List<Transaction> nextTransactions = nodeDaoService.getTxnsByCommitTimeDescending(
                    0L,
                    toTimeExclusive,
                    1,
                    null,
                    remoteOnly);
            // There are no transactions in that time range
            if (nextTransactions.size() == 0)
            {
                break found;
            }
            // We found a transaction
            Transaction txn = nextTransactions.get(0);
            long txnCommitTime = txn.getCommitTimeMs();
            // Check that it is in the index
            InIndex txnInIndex = isTxnPresentInIndex(txn);
            switch (txnInIndex)
            {
                case YES:
                    fromTimeInclusive = txnCommitTime;
                    break found;
                default:
                    firstWasInIndex = false;
                    // Look further back in time.  Step back by 60 seconds each time, increasing
                    // the step by 10% each iteration.
                    // Don't step back by more than a day
                    long decrement = Math.min(ONE_HOUR_MS, (long) (60000.0D * stepFactor));
                    toTimeExclusive = txnCommitTime - decrement;
                    stepFactor *= 1.1D;
                    continue;
            }
        }
        // If the last transaction (given the max txn duration) was in the index, then we used the
        // maximum commit time i.e. the indexes were up to date up until the most recent time.
        if (firstWasInIndex)
        {
            return maxToTimeExclusive;
        }
        else
        {
            return fromTimeInclusive;
        }
    }
    
    private static final int VOID_BATCH_SIZE = 100;
    /**
     * Voids - otherwise known as 'holes' - in the transaction sequence are timestamped when they are
     * discovered.  This method discards voids that were timestamped before the given date.  It checks
     * all remaining voids, passing back the transaction time for the newly-filled void.  Otherwise
     * the value passed in is passed back.
     * 
     * @return      Returns an adjused start position based on any voids being filled
     *              or <b>Long.MAX_VALUE</b> if no new voids were found
     */
    private long checkVoids()
    {
        long maxHistoricalTime = (fromTimeInclusive - maxTxnDurationMs);
        long fromTimeAdjusted = Long.MAX_VALUE;
        
        List<Long> toExpireTxnIds = new ArrayList<Long>(1);
        Iterator<Long> voidTxnIdIterator = voids.keySet().iterator();
        List<Long> voidTxnIdBatch = new ArrayList<Long>(VOID_BATCH_SIZE);
        
        while (voidTxnIdIterator.hasNext())
        {
            Long voidTxnId = voidTxnIdIterator.next();
            // Add it to the batch
            voidTxnIdBatch.add(voidTxnId);
            // If the batch is full or if there are no more voids, fire the query
            if (voidTxnIdBatch.size() == VOID_BATCH_SIZE || !voidTxnIdIterator.hasNext())
            {
                List<Transaction> filledTxns = nodeDaoService.getTxnsByMinCommitTime(voidTxnIdBatch);
                for (Transaction txn : filledTxns)
                {
                    if (txn.getCommitTimeMs() == null)          // Just coping with Hibernate mysteries
                    {
                        continue;
                    }
                    else if (isTxnPresentInIndex(txn) != InIndex.NO)
                    {
                        // It is in the index so expire it from the voids.
                        // This can happen if void was committed locally.
                        toExpireTxnIds.add(txn.getId());
                    }
                    else
                    {
                        // It's not in the index so we have a timespamp from which to kick off
                        // It is a bone fide first transaction.  A void has been filled.
                        long txnCommitTimeMs = txn.getCommitTimeMs().longValue();
                        // If the value is lower than our current one we keep it
                        if (txnCommitTimeMs < fromTimeAdjusted)
                        {
                            fromTimeAdjusted = txnCommitTimeMs;
                        }
                        // The query selected them in timestamp order so there is no need to process
                        // the remaining transactions in this batch - we have our minimum.
                        break;
                    }
                }
                // Wipe the batch clean
                voidTxnIdBatch.clear();
            }
            // Check if the void must be expired or not
            TxnRecord voidTxnRecord = voids.get(voidTxnId);
            if (voidTxnRecord.txnCommitTime < maxHistoricalTime)
            {
                // It's too late for this void whether or not it has become live
                toExpireTxnIds.add(voidTxnId);
            }
        }
        // Throw away all the expired or removable voids
        int voidCountBefore = voids.size();
        for (Long toRemoveTxnId : toExpireTxnIds)
        {
            voids.remove(toRemoveTxnId);
        }
        int voidCountAfter = voids.size();
        if (logger.isDebugEnabled() && voidCountBefore != voidCountAfter)
        {
            logger.debug("Void count " + voidCountBefore + " -> " + voidCountAfter);
        }
        // Done
        if (logger.isDebugEnabled() && fromTimeAdjusted < Long.MAX_VALUE)
        {
            logger.debug("Returning to void time " + fromTimeAdjusted);
        }
        return fromTimeAdjusted;
    }
    
    private List<Transaction> getNextTransactions(long fromTimeInclusive, long toTimeExclusive, List<Long> previousTxnIds)
    {
        List<Transaction> txns = nodeDaoService.getTxnsByCommitTimeAscending(
                fromTimeInclusive,
                toTimeExclusive,
                maxRecordSetSize,
                previousTxnIds,
                false);
        // done
        return txns;
    }
    
    /**
     * Checks that each of the transactions is present in the index.  As soon as one is found that
     * isn't, all the following transactions will be reindexed.  After the reindexing, the sequence
     * of transaction IDs will be examined for any voids.  These will be recorded.
     * 
     * @param txns      transactions ordered by time ascending
     * @return          returns the commit time of the last transaction in the list
     * @throws          IllegalArgumentException if there are no transactions
     */
    private long reindexTransactions(List<Transaction> txns)
    {
        if (txns.isEmpty())
        {
            throw new IllegalArgumentException("There are no transactions to process");
        }
        
        // Determines the window for void retention
        long now = System.currentTimeMillis();
        long oldestVoidRetentionTime = (now - maxTxnDurationMs);

        // Keep an ordered map of IDs that we process along with their commit times
        Map<Long, TxnRecord> processedTxnRecords = new TreeMap<Long, TxnRecord>();
        
        List<Long> txnIdBuffer = new ArrayList<Long>(maxTransactionsPerLuceneCommit);
        Iterator<Transaction> txnIterator = txns.iterator();
        while (txnIterator.hasNext())
        {
            Transaction txn = txnIterator.next();
            Long txnId = txn.getId();
            Long txnCommitTimeMs = txn.getCommitTimeMs();
            if (txnCommitTimeMs == null)
            {
                // What?  But let's be cautious and treat this as a void
                continue;
            }
            // Keep a record of it
            TxnRecord processedTxnRecord = new TxnRecord();
            processedTxnRecord.txnCommitTime = txnCommitTimeMs;
            processedTxnRecords.put(txnId, processedTxnRecord);
            // Remove this entry from the void list - it is not void
            voids.remove(txnId);
            
            // Reindex the transaction if we are forcing it or if it isn't in the index already
            if (forceReindex || isTxnPresentInIndex(txn) == InIndex.NO)
            {
                // From this point on, until the tracker has caught up, all transactions need to be indexed
                forceReindex = true;
                // Add the transaction to the buffer of transactions that need processing
                txnIdBuffer.add(txnId);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Reindexing transaction: " + txn);
                }
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Reindex skipping transaction: " + txn);
                }
            }
            
            if (isShuttingDown())
            {
                break;
            }
            // Flush the reindex buffer, if it is full or if we are on the last transaction and there are no more
            if (txnIdBuffer.size() >= maxTransactionsPerLuceneCommit || (!txnIterator.hasNext() && txnIdBuffer.size() > 0))
            {
                try
                {
                    // We try the reindex, but for the sake of continuity, have to let it run on
                    reindexTransactionAsynchronously(txnIdBuffer);
                }
                catch (Throwable e)
                {
                    logger.warn("\n" +
                            "Reindex of transactions failed: \n" +
                            "   Transaction IDs: " + txnIdBuffer + "\n" +
                            "   Error: " + e.getMessage(),
                            e);
                }
                // Clear the buffer
                txnIdBuffer = new ArrayList<Long>(maxTransactionsPerLuceneCommit);
            }
        }
        // Use the last ID from the previous iteration as our starting point
        Long lastId = lastMaxTxnId;
        long lastCommitTime = -1L;
        // Walk the processed txn IDs
        for (Map.Entry<Long, TxnRecord> entry : processedTxnRecords.entrySet())
        {
            Long processedTxnId = entry.getKey();
            TxnRecord processedTxnRecord = entry.getValue();
            boolean voidsAreYoungEnough = processedTxnRecord.txnCommitTime >= oldestVoidRetentionTime; 
            if (lastId != null && voidsAreYoungEnough)
            {
                int voidCount = 0;
                // Iterate BETWEEN the last ID and the current one to find voids
                // Only enter the loop if the current upper limit transaction is young enough to
                // consider for voids.
                for (long i = lastId.longValue() + 1; i < processedTxnId; i++)
                {
                    // The voids are optimistically given the same transaction time as transaction with the
                    // largest ID.  We only bother w
                    TxnRecord voidRecord = new TxnRecord();
                    voidRecord.txnCommitTime = processedTxnRecord.txnCommitTime;
                    voids.put(new Long(i), voidRecord);
                    voidCount++;
                }
                if (logger.isDebugEnabled()&& voidCount > 0)
                {
                    logger.debug("Voids detected: " + voidCount + " in range [" + lastId + ", " + processedTxnId + "]");
                }
            }
            lastId = processedTxnId;
            lastCommitTime = processedTxnRecord.txnCommitTime;
        }
        // Having searched for the nodes, we've recorded all the voids.  So move the lastMaxTxnId up.
        lastMaxTxnId = lastId;
        
        // Done
        return lastCommitTime;
    }
    
    private class TxnRecord
    {
        private long txnCommitTime;
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder(128);
            sb.append("TxnRecord")
              .append("[time=").append(txnCommitTime <= 0 ? "---" : new Date(txnCommitTime))
              .append("]");
            return sb.toString();
        }
    }
    
    /**
     * A callback that can be set to provide logging and other record keeping
     * 
     * @author Derek Hulley
     * @since 2.1.4
     */
    public interface IndexTransactionTrackerListener
    {
        void indexedTransactions(long fromTimeInclusive, long toTimeExclusive);
    }
}