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
package org.alfresco.repo.node.index;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import net.sf.acegisecurity.Authentication;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.Transaction;
import org.alfresco.repo.node.db.NodeDaoService;
import org.alfresco.repo.search.Indexer;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.search.impl.lucene.fts.FullTextSearchIndexer;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.repo.transaction.TransactionServiceImpl;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.VmShutdownListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract helper for reindexing.
 * 
 * @see #reindexImpl()
 * @see #getIndexerWriteLock()
 * @see #isShuttingDown()
 * 
 * @author Derek Hulley
 */
public abstract class AbstractReindexComponent implements IndexRecovery
{
    private static Log logger = LogFactory.getLog(AbstractReindexComponent.class);
    private static Log loggerOnThread = LogFactory.getLog(AbstractReindexComponent.class.getName() + ".threads");
    
    /** kept to notify the thread that it should quit */
    private static VmShutdownListener vmShutdownListener = new VmShutdownListener("IndexRecovery");
    
    private AuthenticationComponent authenticationComponent;
    /** provides transactions to atomically index each missed transaction */
    protected TransactionServiceImpl transactionService;
    /** the component to index the node hierarchy */
    protected Indexer indexer;
    /** the FTS indexer that we will prompt to pick up on any un-indexed text */
    protected FullTextSearchIndexer ftsIndexer;
    /** the component providing searches of the indexed nodes */
    protected SearchService searcher;
    /** the component giving direct access to <b>store</b> instances */
    protected NodeService nodeService;
    /** the component giving direct access to <b>transaction</b> instances */
    protected NodeDaoService nodeDaoService;
    /** the component that holds the reindex worker threads */
    private ThreadPoolExecutor threadPoolExecutor;
    
    private volatile boolean shutdown;
    private final WriteLock indexerWriteLock;
    
    public AbstractReindexComponent()
    {
        shutdown = false;
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        indexerWriteLock = readWriteLock.writeLock();
    }
    
    /**
     * Convenience method to get a common write lock.  This can be used to avoid
     * concurrent access to the work methods.
     */
    protected WriteLock getIndexerWriteLock()
    {
        return indexerWriteLock;
    }
    
    /**
     * Programmatically notify a reindex thread to terminate
     * 
     * @param shutdown true to shutdown, false to reset
     */
    public void setShutdown(boolean shutdown)
    {
        this.shutdown = shutdown;
    }
    
    /**
     * 
     * @return Returns true if the VM shutdown hook has been triggered, or the instance
     *      was programmatically {@link #shutdown shut down}
     */
    protected boolean isShuttingDown()
    {
        return shutdown || vmShutdownListener.isVmShuttingDown();
    }

    /**
     * @param authenticationComponent ensures that reindexing operates as system user
     */
    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }

    /**
     * Set the low-level transaction component to use
     * 
     * @param transactionComponent provide transactions to index each missed transaction
     */
    public void setTransactionService(TransactionServiceImpl transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * @param indexer the indexer that will be index
     */
    public void setIndexer(Indexer indexer)
    {
        this.indexer = indexer;
    }
    
    /**
     * @param ftsIndexer the FTS background indexer
     */
    public void setFtsIndexer(FullTextSearchIndexer ftsIndexer)
    {
        this.ftsIndexer = ftsIndexer;
    }

    /**
     * @param searcher component providing index searches
     */
    public void setSearcher(SearchService searcher)
    {
        this.searcher = searcher;
    }

    /**
     * @param nodeService provides information about nodes for indexing
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param nodeDaoService provides access to transaction-related queries
     */
    public void setNodeDaoService(NodeDaoService nodeDaoService)
    {
        this.nodeDaoService = nodeDaoService;
    }

    /**
     * Set the thread pool to use when doing asynchronous reindexing.  Use <tt>null</tt>
     * to have the calling thread do the indexing.
     * 
     * @param threadPoolExecutor        a pre-configured thread pool for the reindex work
     * 
     * @since 2.1.4
     */
    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor)
    {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    /**
     * Determines if calls to {@link #reindexImpl()} should be wrapped in a transaction or not.
     * The default is <b>true</b>.
     * 
     * @return      Returns <tt>true</tt> if an existing transaction is required for reindexing.
     */
    protected boolean requireTransaction()
    {
        return true;
    }
    
    /**
     * Perform the actual work.  This method will be called as the system user
     * and within an existing transaction.  This thread will only ever be accessed
     * by a single thread per instance.
     *
     */
    protected abstract void reindexImpl();
    
    /**
     * If this object is currently busy, then it just nothing
     */
    public final void reindex()
    {
        PropertyCheck.mandatory(this, "authenticationComponent", this.authenticationComponent);
        PropertyCheck.mandatory(this, "ftsIndexer", this.ftsIndexer);
        PropertyCheck.mandatory(this, "indexer", this.indexer);
        PropertyCheck.mandatory(this, "searcher", this.searcher);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "nodeDaoService", this.nodeDaoService);
        PropertyCheck.mandatory(this, "transactionComponent", this.transactionService);
        
        if (indexerWriteLock.tryLock())
        {
            Authentication auth = null;
            try
            {
                auth = AuthenticationUtil.getCurrentAuthentication();
                // authenticate as the system user
                authenticationComponent.setSystemUserAsCurrentUser();
                RetryingTransactionCallback<Object> reindexWork = new RetryingTransactionCallback<Object>()
                {
                    public Object execute() throws Exception
                    {
                        reindexImpl();
                        return null;
                    }
                };
                if (requireTransaction())
                {
                    transactionService.getRetryingTransactionHelper().doInTransaction(reindexWork, true);
                }
                else
                {
                    reindexWork.execute();
                }
            }
            catch (Throwable e)
            {
                throw new AlfrescoRuntimeException("Reindex failure for " + this.getClass().getName(), e);
            }
            finally
            {
                try { indexerWriteLock.unlock(); } catch (Throwable e) {}
                if (auth != null)
                {
                    authenticationComponent.setCurrentAuthentication(auth);
                }
            }
            // done
            if (logger.isDebugEnabled())
            {
                logger.debug("Reindex work completed: " + this);
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Bypassed reindex work - already busy: " + this);
            }
        }
    }

    protected enum InIndex
    {
        YES, NO, INDETERMINATE;
    }
    
    private static final String KEY_STORE_REFS = "StoreRefCacheMethodInterceptor.StoreRefs";
    @SuppressWarnings("unchecked")
    /**
     * Helper method that caches ADM store references to prevent repeated and unnecessary calls to the
     * NodeService for this list.
     */
    private List<StoreRef> getAdmStoreRefs()
    {
        List<StoreRef> storeRefs = (List<StoreRef>) AlfrescoTransactionSupport.getResource(KEY_STORE_REFS);
        if (storeRefs != null)
        {
            return storeRefs;
        }
        else
        {
            storeRefs = nodeService.getStores();
            Iterator<StoreRef> storeRefsIterator = storeRefs.iterator();
            while (storeRefsIterator.hasNext())
            {
                // Remove AVM stores
                StoreRef storeRef = storeRefsIterator.next();
                if (storeRef.getProtocol().equals(StoreRef.PROTOCOL_AVM))
                {
                    storeRefsIterator.remove();
                }
            }
            // Change the ordering to favour the most common stores
            if (storeRefs.contains(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE))
            {
                storeRefs.remove(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
                storeRefs.add(0, StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
            }
            if (storeRefs.contains(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE))
            {
                storeRefs.remove(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
                storeRefs.add(0, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            }
            // Bind it in
            AlfrescoTransactionSupport.bindResource(KEY_STORE_REFS, storeRefs);
        }
        return storeRefs;
    }
    
    protected InIndex isTxnIdPresentInIndex(long txnId)
    {
        Transaction txn = nodeDaoService.getTxnById(txnId);
        if (txn == null)
        {
            return InIndex.YES;
        }
        return isTxnPresentInIndex(txn);
    }
    
    /**
     * Determines if a given transaction is definitely in the index or not.
     * 
     * @param txn       a specific transaction
     * @return          Returns <tt>true</tt> if the transaction is definitely in the index
     */
    protected InIndex isTxnPresentInIndex(final Transaction txn)
    {
        if (txn == null)
        {
            return InIndex.YES;
        }

        final Long txnId = txn.getId();
        if (logger.isDebugEnabled())
        {
            logger.debug("Checking for transaction in index: " + txnId);
        }
        
        // Check if the txn ID is present in any store's index
        boolean foundInIndex = false;
        List<StoreRef> storeRefs = getAdmStoreRefs();
        for (StoreRef storeRef : storeRefs)
        {
            boolean inStore = isTxnIdPresentInIndex(storeRef, txn);
            if (inStore)
            {
                // found in a particular store
                foundInIndex = true;
                break;
            }
        }
        InIndex result = InIndex.NO;
        if (!foundInIndex)
        {
            // If none of the stores have the transaction, then that might be because it consists of 0 modifications
            int updateCount = nodeDaoService.getTxnUpdateCount(txnId);
            if (updateCount > 0)
            {
                // There were updates, but there is no sign in the indexes
                result = InIndex.NO;
            }
            else
            {
                // We're now in the case where there were no updates
                int deleteCount = nodeDaoService.getTxnDeleteCount(txnId);
                if (deleteCount == 0)
                {
                    // There are no updates or deletes and no entry in the indexes.
                    // There are outdated nodes in the index.
                    result = InIndex.YES;
                }
                else
                {
                    // There were deleted nodes only.  Check that all the deleted nodes were
                    // removed from the index otherwise it is out of date.
                    for (StoreRef storeRef : storeRefs)
                    {
                        if (!haveNodesBeenRemovedFromIndex(storeRef, txn))
                        {
                            result = InIndex.NO;
                            break;
                        }
                    }
                }
            }
        }
        else
        {
            result = InIndex.YES;
        }
        
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Transaction " + txnId + " present in indexes: " + result);
        }
        return result;
    }
    
    /**
     * @return                  Returns true if the given transaction is present in the index
     */
    private boolean isTxnIdPresentInIndex(StoreRef storeRef, Transaction txn)
    {
        long txnId = txn.getId();
        String changeTxnId = txn.getChangeTxnId();
        // do the most update check, which is most common
        ResultSet results = null;
        try
        {
            SearchParameters sp = new SearchParameters();
            sp.addStore(storeRef);
            // search for it in the index, sorting with youngest first, fetching only 1
            sp.setLanguage(SearchService.LANGUAGE_LUCENE);
            sp.setQuery("TX:" + LuceneQueryParser.escape(changeTxnId));
            sp.setLimit(1);
            
            results = searcher.query(sp);
            
            if (results.length() > 0)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Index has results for txn " + txnId + " for store " + storeRef);
                }
                return true;        // there were updates/creates and results for the txn were found
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Transaction " + txnId + " not in index for store " + storeRef + ".  Possibly out of date.");
                }
                return false;
            }
        }
        finally
        {
            if (results != null) { results.close(); }
        }
    }
    
    private boolean haveNodesBeenRemovedFromIndex(final StoreRef storeRef, final Transaction txn)
    {
        final Long txnId = txn.getId();
        // there have been deletes, so we have to ensure that none of the nodes deleted are present in the index
        // get all node refs for the transaction
        List<NodeRef> nodeRefs = nodeDaoService.getTxnChangesForStore(storeRef, txnId);
        boolean foundNodeRef = false;
        for (NodeRef nodeRef : nodeRefs)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Searching for node in index: \n" +
                        "   node: " + nodeRef + "\n" +
                  "   txn: " + txnId);
            }
            // we know that these are all deletions
            ResultSet results = null;
            try
            {
                SearchParameters sp = new SearchParameters();
                sp.addStore(storeRef);
                // search for it in the index, sorting with youngest first, fetching only 1
                sp.setLanguage(SearchService.LANGUAGE_LUCENE);
                sp.setQuery("ID:" + LuceneQueryParser.escape(nodeRef.toString()));
                sp.setLimit(1);

                results = searcher.query(sp);
              
                if (results.length() > 0)
                {
                    foundNodeRef = true;
                    break;
                }
            }
            finally
            {
                if (results != null) { results.close(); }
            }
        }
        if (foundNodeRef)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(" --> Node found (Index out of date)");
            }
        }
        else
        {
            // No nodes found
            if (logger.isDebugEnabled())
            {
                logger.debug(" --> Node not found (OK)");
            }
        }
        return !foundNodeRef;
    }
    
    /**
     * @return          Returns <tt>false</tt> if any one of the transactions aren't in the index.
     */
    protected boolean areTxnsInIndex(List<Transaction> txns)
    {
        for (Transaction txn : txns)
        {
            if (isTxnPresentInIndex(txn) == InIndex.NO)
            {
                // Missing txn
                return false;
            }
        }
        return true;
    }
    
    /**
     * Marker exception to neatly handle VM-driven termination of a reindex
     * 
     * @author Derek Hulley
     * @since 2.1.4
     */
    public static class ReindexTerminatedException extends RuntimeException
    {
        private static final long serialVersionUID = -7928720932368892814L;
    }
    
    /**
     * Callback to notify caller whenever a node has been indexed
     * 
     * @see 
     * @author Derek Hulley
     * @since 2.1.4
     */
    protected interface ReindexNodeCallback
    {
        void reindexedNode(NodeRef nodeRef);
    }
    
    protected void reindexTransaction(Long txnId)
    {
        reindexTransaction(txnId, null);
    }
    
    /**
     * Perform a full reindexing of the given transaction on the current thread.
     * The calling thread must be in the context of a read-only transaction.
     * 
     * @param txnId         the transaction identifier
     * @param callback      the callback to notify of each node indexed
     * 
     * @throws ReindexTerminatedException if the VM is shutdown during the reindex
     */
    protected void reindexTransaction(final long txnId, ReindexNodeCallback callback)
    {
        ParameterCheck.mandatory("txnId", txnId);
        if (logger.isDebugEnabled())
        {
            logger.debug("Reindexing transaction: " + txnId);
        }
        if (AlfrescoTransactionSupport.getTransactionReadState() != TxnReadState.TXN_READ_ONLY)
        {
            throw new AlfrescoRuntimeException("Reindex work must be done in the context of a read-only transaction");
        }
        
        // get the node references pertinent to the transaction
        List<NodeRef> nodeRefs = nodeDaoService.getTxnChanges(txnId);
        // reindex each node
        int nodeCount = 0;
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
            // Make the callback
            if (callback != null)
            {
                callback.reindexedNode(nodeRef);
            }
            // Check for VM shutdown every 100 nodes
            if (++nodeCount % 100 == 0 && isShuttingDown())
            {
                // We can't fail gracefully and run the risk of committing a half-baked transaction
                logger.info("Reindexing of transaction " + txnId + " terminated by VM shutdown.");
                throw new ReindexTerminatedException();
            }
        }
        // done
    }
    
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();
    /**
     * Runnable that does reindex work for a given transaction but waits on a queue before
     * triggering the commit phase.
     * <p>
     * This class uses <code>Object</code>'s default equality and hashcode generation.
     * 
     * @author Derek Hulley
     * @since 2.1.4
     */
    private class ReindexWorkerRunnable extends TransactionListenerAdapter implements Runnable, ReindexNodeCallback
    {
        private final int id;
        private final int uidHashCode;
        private final List<Long> txnIds;
        private long lastIndexedTimestamp;
        private boolean atHeadOfQueue;
        private boolean killed;
        
        private ReindexWorkerRunnable(List<Long> txnIds)
        {
            this.id = ID_GENERATOR.addAndGet(1);
            if (ID_GENERATOR.get() > 1000)
            {
                ID_GENERATOR.set(0);
            }
            this.uidHashCode = id * 13 + 11;
            this.txnIds = txnIds;
            this.atHeadOfQueue = false;
            this.killed = false;
            recordTimestamp();
        }
        
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ReindexWorkerRunnable")
              .append("[id=").append(id)
              .append("[txnIds=").append(txnIds)
              .append("]");
            return sb.toString();
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof ReindexWorkerRunnable))
            {
                return false;
            }
            ReindexWorkerRunnable that = (ReindexWorkerRunnable) obj;
            return this.id == that.id;
        }

        @Override
        public int hashCode()
        {
            return uidHashCode;
        }

        public synchronized void kill()
        {
            this.killed = true;
        }
        
        private synchronized boolean isKilled()
        {
            return killed;
        }

        /**
         * @return      the time that the last node was indexed (nanoseconds)
         */
        public synchronized long getLastIndexedTimestamp()
        {
            return lastIndexedTimestamp;
        }
        
        private synchronized void recordTimestamp()
        {
            this.lastIndexedTimestamp = System.nanoTime();
        }
        
        private synchronized boolean isAtHeadOfQueue()
        {
            return atHeadOfQueue;
        }
        
        private synchronized void waitForHeadOfQueue()
        {
            try { wait(100L); } catch (InterruptedException e) {}
        }

        public synchronized void setAtHeadOfQueue()
        {
            this.notifyAll();
            this.atHeadOfQueue = true;
        }
        
        public void run()
        {
            RetryingTransactionCallback<Object> reindexCallback = new RetryingTransactionCallback<Object>()
            {
                public Object execute() throws Throwable
                {
                    // The first thing is to ensure that beforeCommit will be called
                    AlfrescoTransactionSupport.bindListener(ReindexWorkerRunnable.this);
                    // Now reindex
                    for (Long txnId : txnIds)
                    {
                        if (loggerOnThread.isDebugEnabled())
                        {
                            String msg = String.format(
                                    "   -> Reindexer %5d reindexing %10d",
                                    id, txnId.longValue());
                            loggerOnThread.debug(msg);
                        }
                        reindexTransaction(txnId, ReindexWorkerRunnable.this);
                    }
                    // Done
                    return null;
                }
            };
            // Timestamp for when we start
            recordTimestamp();
            try
            {
                if (loggerOnThread.isDebugEnabled())
                {
                    int txnIdsSize = txnIds.size();
                    String msg = String.format(
                            "Reindexer %5d starting [%10d, %10d] on %s.",
                            id,
                            (txnIdsSize == 0 ? -1 : txnIds.get(0)),
                            (txnIdsSize == 0 ? -1 : txnIds.get(txnIdsSize-1)),
                            Thread.currentThread().getName());
                    loggerOnThread.debug(msg);
                }
                // Do the work
                transactionService.getRetryingTransactionHelper().doInTransaction(reindexCallback, true, true);
            }
            catch (ReindexTerminatedException e)
            {
                // This is OK
                String msg = String.format(
                        "Reindexer %5d terminated: %s.",
                        id,
                        e.getMessage());
                loggerOnThread.warn(msg);
                loggerOnThread.warn(getStackTrace(e));
            }
            catch (Throwable e)
            {
                String msg = String.format(
                        "Reindexer %5d failed with error: %s.",
                        id,
                        e.getMessage());
                loggerOnThread.error(msg);
                loggerOnThread.warn(getStackTrace(e));
            }
            finally
            {
                // Triple check that we get the queue state right
                removeFromQueueAndProdHead();
            }
        }
        
        public String getStackTrace(Throwable t)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw, true);
            t.printStackTrace(pw);
            pw.flush();
            sw.flush();
            return sw.toString();
        }

        
        public synchronized void reindexedNode(NodeRef nodeRef)
        {
            // Check for forced kill
            if (isKilled())
            {
                throw new ReindexTerminatedException();
            }
            recordTimestamp();
        }
        
        /**
         * Removes this instance from the queue and notifies the HEAD
         */
        private void removeFromQueueAndProdHead()
        {
            try
            {
                reindexThreadLock.writeLock().lock();
                // Remove self from head of queue
                reindexThreadQueue.remove(this);
            }
            finally
            {
                reindexThreadLock.writeLock().unlock();
            }
            // Now notify the new head object
            ReindexWorkerRunnable newPeek = peekHeadReindexWorker();
            if (newPeek != null)
            {
                newPeek.setAtHeadOfQueue();
            }
            if (loggerOnThread.isDebugEnabled())
            {
                String msg = String.format(
                        "Reindexer %5d removed from queue.  Current HEAD is %s.",
                        id, newPeek);
                loggerOnThread.debug(msg);
            }
        }
        
        @Override
        public void afterCommit()
        {
            handleQueue();
        }
        @Override
        public void afterRollback()
        {
            handleQueue();
        }
        /**
         * Lucene will do its final commit once this has been allowed to proceed.
         */
        private void handleQueue()
        {
            while (true)
            {
                // Quick check to see if we're at the head of the queue
                ReindexWorkerRunnable peek = peekHeadReindexWorker();
                // Release the current queue head to finish (this might be this instance)
                if (peek != null)
                {
                    peek.setAtHeadOfQueue();
                }
                // Check kill switch
                if (peek == null || isKilled() || isAtHeadOfQueue())
                {
                    // Going to close
                    break;
                }
                else
                {
                    // This thread is not at the head of the queue and has not been flagged
                    // for death, so just wait until someone notifies us to carry on
                    waitForHeadOfQueue();
                    // Loop again
                }
            }
            // Lucene can now get on with the commit.  We didn't have ordering at this level
            // and the IndexInfo locks are handled by Lucene.  So we let the thread go and
            // the other worker threads can get on with it.
            // Record the fact that the thread is on the final straight.  From here on, no
            // more work notifications will be possible so the timestamp needs to spoof it.
            recordTimestamp();
        }
    }
        
    /**
     * FIFO queue to control the ordering of transaction commits.  Synchronization around this object is
     * controlled by the read-write lock.
     */
    private LinkedBlockingQueue<ReindexWorkerRunnable> reindexThreadQueue = new LinkedBlockingQueue<ReindexWorkerRunnable>();
    private ReentrantReadWriteLock reindexThreadLock = new ReentrantReadWriteLock(true);
    
    /**
     * Read-safe method to peek at the head of the queue
     */
    private ReindexWorkerRunnable peekHeadReindexWorker()
    {
        try
        {
            reindexThreadLock.readLock().lock();
            return reindexThreadQueue.peek();
        }
        finally
        {
            reindexThreadLock.readLock().unlock();
        }
    }
    
    /**
     * Performs indexing off the current thread, which may return quickly if there are threads immediately
     * available in the thread pool.
     * <p>
     * Commits are guaranteed to occur in the order in which this reindex jobs are added to the queue.
     *
     * @see #reindexTransaction(long)
     * @see #waitForAsynchronousReindexing()
     * @since 2.1.4
     */
    protected void reindexTransactionAsynchronously(final List<Long> txnIds)
    {
        // Bypass if there is no thread pool
        if (threadPoolExecutor == null || threadPoolExecutor.getMaximumPoolSize() < 2)
        {
            if (loggerOnThread.isDebugEnabled())
            {
                String msg = String.format(
                        "Reindexing inline: %s.",
                        txnIds.toString());
                loggerOnThread.debug(msg);
            }
            RetryingTransactionCallback<Object> reindexCallback = new RetryingTransactionCallback<Object>()
            {
                public Object execute() throws Throwable
                {
                    for (Long txnId : txnIds)
                    {
                        if (loggerOnThread.isDebugEnabled())
                        {
                            String msg = String.format(
                                    "Reindex %10d.",
                                    txnId.longValue());
                            loggerOnThread.debug(msg);
                        }
                        reindexTransaction(txnId, null);
                    }
                    return null;
                }
            };
            transactionService.getRetryingTransactionHelper().doInTransaction(reindexCallback, true, true);
            return;
        }
        
        ReindexWorkerRunnable runnable = new ReindexWorkerRunnable(txnIds);
        try
        {
            reindexThreadLock.writeLock().lock();
            // Add the runnable to the queue to ensure ordering
            reindexThreadQueue.add(runnable);
        }
        finally
        {
            reindexThreadLock.writeLock().unlock();
        }
        // Ship it to a thread.
        // We don't do this in the lock - but the situation should be avoided by having the blocking
        // queue size less than the maximum pool size
        threadPoolExecutor.execute(runnable);
    }
    
    /**
     * Wait for all asynchronous indexing to finish before returning.  This is useful if the calling thread
     * wants to ensure that all reindex work has finished before continuing.
     */
    protected synchronized void waitForAsynchronousReindexing()
    {
        ReindexWorkerRunnable lastRunnable = null;
        long lastTimestamp = Long.MAX_VALUE;
        
        ReindexWorkerRunnable currentRunnable = peekHeadReindexWorker();
        while (currentRunnable != null && !isShuttingDown())
        {
            // Notify the runnable that it is at the head of the queue
            currentRunnable.setAtHeadOfQueue();
            // Give the thread chance to commit
            synchronized(this)
            {
                try { wait(100); } catch (InterruptedException e) {}
            }
            
            long currentTimestamp = currentRunnable.getLastIndexedTimestamp();
            // The head of the queue holds proceedings, so it can't be allowed to continue forever
            // Allow 60s of inactivity.  We don't anticipate more than a few milliseconds between
            // timestamp advances for the reindex threads so this checking is just for emergencies
            // to prevent the queue from getting locked up.
            if (lastRunnable == currentRunnable)
            {
                if (currentTimestamp - lastTimestamp > 60E9)
                {
                    
                    try
                    {
                        reindexThreadLock.writeLock().lock();
                        // Double check
                        ReindexWorkerRunnable checkCurrentRunnable = reindexThreadQueue.peek();
                        if (lastRunnable != checkCurrentRunnable)
                        {
                            // It's moved on - just in time
                        }
                        else
                        {
                            loggerOnThread.info("Terminating reindex thread for inactivity: " + currentRunnable);
                            reindexThreadQueue.remove(currentRunnable);
                            currentRunnable.kill();
                        }
                        // Reset
                        lastRunnable = null;
                        lastTimestamp = Long.MAX_VALUE;
                        // Peek at the queue and check again
                        currentRunnable  = reindexThreadQueue.peek();
                    }
                    finally
                    {
                        reindexThreadLock.writeLock().unlock();
                    }
                    continue;
                }
                // Swap timestamps
                lastRunnable = currentRunnable;
                lastTimestamp = currentTimestamp;
            }
            else
            {
                // Swap timestamps
                lastRunnable = currentRunnable;
                lastTimestamp = currentTimestamp;
            }
            currentRunnable = peekHeadReindexWorker();
        }
    }
}