/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.search.impl.lucene;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.alfresco.repo.search.IndexerException;
import org.alfresco.repo.search.QueryRegisterComponent;
import org.alfresco.repo.search.SearcherException;
import org.alfresco.repo.search.impl.lucene.fts.FullTextSearchIndexer;
import org.alfresco.repo.search.transaction.LuceneIndexLock;
import org.alfresco.repo.search.transaction.SimpleTransaction;
import org.alfresco.repo.search.transaction.SimpleTransactionManager;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.GUID;
import org.apache.lucene.search.BooleanQuery;

/**
 * This class is resource manager LuceneIndexers and LuceneSearchers.
 * 
 * It supports two phase commit inside XA transactions and outside transactions
 * it provides thread local transaction support.
 * 
 * TODO: Provide pluggable support for a transaction manager TODO: Integrate
 * with Spring transactions
 * 
 * @author andyh
 * 
 */

public class LuceneIndexerAndSearcherFactory implements LuceneIndexerAndSearcher, XAResource
{
    private DictionaryService dictionaryService;

    private NamespaceService nameSpaceService;

    private int queryMaxClauses;

    private int indexerBatchSize;

    private int indexerMinMergeDocs;

    private int indexerMergeFactor;

    private int indexerMaxMergeDocs;

    private String lockDirectory;

    /**
     * The factory instance
     */
    private static LuceneIndexerAndSearcherFactory factory = new LuceneIndexerAndSearcherFactory();

    /**
     * A map of active global transactions . It contains all the indexers a
     * transaction has used, with at most one indexer for each store within a
     * transaction
     */

    private static Map<Xid, Map<StoreRef, LuceneIndexer>> activeIndexersInGlobalTx = new HashMap<Xid, Map<StoreRef, LuceneIndexer>>();

    /**
     * Suspended global transactions.
     */
    private static Map<Xid, Map<StoreRef, LuceneIndexer>> suspendedIndexersInGlobalTx = new HashMap<Xid, Map<StoreRef, LuceneIndexer>>();

    /**
     * Thread local indexers - used outside a global transaction
     */

    private static ThreadLocal<Map<StoreRef, LuceneIndexer>> threadLocalIndexers = new ThreadLocal<Map<StoreRef, LuceneIndexer>>();

    /**
     * The dafault timeout for transactions TODO: Respect this
     */

    private int timeout = DEFAULT_TIMEOUT;

    /**
     * Default time out value set to 10 minutes.
     */
    private static final int DEFAULT_TIMEOUT = 600000;

    /**
     * The node service we use to get information about nodes
     */

    private NodeService nodeService;

    private LuceneIndexLock luceneIndexLock;

    private FullTextSearchIndexer luceneFullTextSearchIndexer;

    private String indexRootLocation;

    private ContentService contentService;

    private QueryRegisterComponent queryRegister;

    /**
     * Private constructor for the singleton TODO: FIt in with IOC
     */

    private LuceneIndexerAndSearcherFactory()
    {
        super();
    }

    /**
     * Get the factory instance
     * 
     * @return
     */

    public static LuceneIndexerAndSearcherFactory getInstance()
    {
        return factory;
    }

    /**
     * Setter for getting the node service via IOC Used in the Spring container
     * 
     * @param nodeService
     */

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    public void setNameSpaceService(NamespaceService nameSpaceService)
    {
        this.nameSpaceService = nameSpaceService;
    }

    public void setLuceneIndexLock(LuceneIndexLock luceneIndexLock)
    {
        this.luceneIndexLock = luceneIndexLock;
    }

    public void setLuceneFullTextSearchIndexer(FullTextSearchIndexer luceneFullTextSearchIndexer)
    {
        this.luceneFullTextSearchIndexer = luceneFullTextSearchIndexer;
    }

    public void setIndexRootLocation(String indexRootLocation)
    {
        this.indexRootLocation = indexRootLocation;
    }

    public void setQueryRegister(QueryRegisterComponent queryRegister)
    {
        this.queryRegister = queryRegister;
    }

    /**
     * Check if we are in a global transactoin according to the transaction
     * manager
     * 
     * @return
     */

    private boolean inGlobalTransaction()
    {
        try
        {
            return SimpleTransactionManager.getInstance().getTransaction() != null;
        }
        catch (SystemException e)
        {
            return false;
        }
    }

    /**
     * Get the local transaction - may be null oif we are outside a transaction.
     * 
     * @return
     * @throws IndexerException
     */
    private SimpleTransaction getTransaction() throws IndexerException
    {
        try
        {
            return SimpleTransactionManager.getInstance().getTransaction();
        }
        catch (SystemException e)
        {
            throw new IndexerException("Failed to get transaction", e);
        }
    }

    /**
     * Get an indexer for the store to use in the current transaction for this
     * thread of control.
     * 
     * @param storeRef -
     *            the id of the store
     */
    public LuceneIndexer getIndexer(StoreRef storeRef) throws IndexerException
    {
        if (inGlobalTransaction())
        {
            SimpleTransaction tx = getTransaction();
            // Only find indexers in the active list
            Map<StoreRef, LuceneIndexer> indexers = activeIndexersInGlobalTx.get(tx);
            if (indexers == null)
            {
                if (suspendedIndexersInGlobalTx.containsKey(tx))
                {
                    throw new IndexerException("Trying to obtain an index for a suspended transaction.");
                }
                indexers = new HashMap<StoreRef, LuceneIndexer>();
                activeIndexersInGlobalTx.put(tx, indexers);
                try
                {
                    tx.enlistResource(this);
                }
                // TODO: what to do in each case?
                catch (IllegalStateException e)
                {
                    throw new IndexerException("", e);
                }
                catch (RollbackException e)
                {
                    throw new IndexerException("", e);
                }
                catch (SystemException e)
                {
                    throw new IndexerException("", e);
                }
            }
            LuceneIndexer indexer = indexers.get(storeRef);
            if (indexer == null)
            {
                indexer = createIndexer(storeRef, getTransactionId(tx));
                indexers.put(storeRef, indexer);
            }
            return indexer;
        }
        else
        // A thread local transaction
        {
            Map<StoreRef, LuceneIndexer> indexers = threadLocalIndexers.get();
            if (indexers == null)
            {
                indexers = new HashMap<StoreRef, LuceneIndexer>();
                threadLocalIndexers.set(indexers);
            }
            LuceneIndexer indexer = indexers.get(storeRef);
            if (indexer == null)
            {
                indexer = createIndexer(storeRef, GUID.generate());
                indexers.put(storeRef, indexer);
            }
            return indexer;
        }

    }

    /**
     * Get the transaction identifier uised to store it in the transaction map.
     * 
     * @param tx
     * @return
     */
    private static String getTransactionId(Transaction tx)
    {
        if (tx instanceof SimpleTransaction)
        {
            SimpleTransaction simpleTx = (SimpleTransaction) tx;
            return simpleTx.getGUID();
        }
        else
        {
            return tx.toString();
        }
    }

    /**
     * Encapsulate creating an indexer
     * 
     * @param storeRef
     * @param deltaId
     * @return
     */
    private LuceneIndexerImpl createIndexer(StoreRef storeRef, String deltaId)
    {
        LuceneIndexerImpl indexer = LuceneIndexerImpl.getUpdateIndexer(storeRef, deltaId, this);
        indexer.setNodeService(nodeService);
        indexer.setDictionaryService(dictionaryService);
        indexer.setLuceneIndexLock(luceneIndexLock);
        indexer.setLuceneFullTextSearchIndexer(luceneFullTextSearchIndexer);
        indexer.setContentService(contentService);
        return indexer;
    }

    /**
     * Encapsulate creating a searcher over the main index
     */
    public LuceneSearcher getSearcher(StoreRef storeRef, boolean searchDelta) throws SearcherException
    {
        String deltaId = null;
        if (searchDelta)
        {
            deltaId = getTransactionId(getTransaction());
        }
        LuceneSearcher searcher = getSearcher(storeRef, deltaId);
        return searcher;
    }

    /**
     * Get a searcher over the index and the current delta
     * 
     * @param storeRef
     * @param deltaId
     * @return
     * @throws SearcherException
     */
    private LuceneSearcher getSearcher(StoreRef storeRef, String deltaId) throws SearcherException
    {
        LuceneSearcherImpl searcher = LuceneSearcherImpl.getSearcher(storeRef, deltaId, this);
        searcher.setNamespacePrefixResolver(nameSpaceService);
        searcher.setLuceneIndexLock(luceneIndexLock);
        searcher.setNodeService(nodeService);
        searcher.setDictionaryService(dictionaryService);
        searcher.setQueryRegister(queryRegister);
        return searcher;
    }

    /*
     * XAResource implementation
     */

    public void commit(Xid xid, boolean onePhase) throws XAException
    {
        try
        {
            // TODO: Should be remembering overall state
            // TODO: Keep track of prepare responses
            Map<StoreRef, LuceneIndexer> indexers = activeIndexersInGlobalTx.get(xid);
            if (indexers == null)
            {
                if (suspendedIndexersInGlobalTx.containsKey(xid))
                {
                    throw new XAException("Trying to commit indexes for a suspended transaction.");
                }
                else
                {
                    // nothing to do
                    return;
                }
            }

            if (onePhase)
            {
                if (indexers.size() == 0)
                {
                    return;
                }
                else if (indexers.size() == 1)
                {
                    for (LuceneIndexer indexer : indexers.values())
                    {
                        indexer.commit();
                    }
                    return;
                }
                else
                {
                    throw new XAException("Trying to do one phase commit on more than one index");
                }
            }
            else
            // two phase
            {
                for (LuceneIndexer indexer : indexers.values())
                {
                    indexer.commit();
                }
                return;
            }
        }
        finally
        {
            activeIndexersInGlobalTx.remove(xid);
        }
    }

    public void end(Xid xid, int flag) throws XAException
    {
        Map<StoreRef, LuceneIndexer> indexers = activeIndexersInGlobalTx.get(xid);
        if (indexers == null)
        {
            if (suspendedIndexersInGlobalTx.containsKey(xid))
            {
                throw new XAException("Trying to commit indexes for a suspended transaction.");
            }
            else
            {
                // nothing to do
                return;
            }
        }
        if (flag == XAResource.TMSUSPEND)
        {
            activeIndexersInGlobalTx.remove(xid);
            suspendedIndexersInGlobalTx.put(xid, indexers);
        }
        else if (flag == TMFAIL)
        {
            activeIndexersInGlobalTx.remove(xid);
            suspendedIndexersInGlobalTx.remove(xid);
        }
        else if (flag == TMSUCCESS)
        {
            activeIndexersInGlobalTx.remove(xid);
        }
    }

    public void forget(Xid xid) throws XAException
    {
        activeIndexersInGlobalTx.remove(xid);
        suspendedIndexersInGlobalTx.remove(xid);
    }

    public int getTransactionTimeout() throws XAException
    {
        return timeout;
    }

    public boolean isSameRM(XAResource xar) throws XAException
    {
        return (xar instanceof LuceneIndexerAndSearcherFactory);
    }

    public int prepare(Xid xid) throws XAException
    {
        // TODO: Track state OK, ReadOnly, Exception (=> rolled back?)
        Map<StoreRef, LuceneIndexer> indexers = activeIndexersInGlobalTx.get(xid);
        if (indexers == null)
        {
            if (suspendedIndexersInGlobalTx.containsKey(xid))
            {
                throw new XAException("Trying to commit indexes for a suspended transaction.");
            }
            else
            {
                // nothing to do
                return XAResource.XA_OK;
            }
        }
        boolean isPrepared = true;
        boolean isModified = false;
        for (LuceneIndexer indexer : indexers.values())
        {
            try
            {
                isModified |= indexer.isModified();
                indexer.prepare();
            }
            catch (IndexerException e)
            {
                isPrepared = false;
            }
        }
        if (isPrepared)
        {
            if (isModified)
            {
                return XAResource.XA_OK;
            }
            else
            {
                return XAResource.XA_RDONLY;
            }
        }
        else
        {
            throw new XAException("Failed to prepare: requires rollback");
        }
    }

    public Xid[] recover(int arg0) throws XAException
    {
        // We can not rely on being able to recover at the moment
        // Avoiding for performance benefits at the moment
        // Assume roll back and no recovery - in the worst case we get an unused
        // delta
        // This should be there to avoid recovery of partial commits.
        // It is difficult to see how we can mandate the same conditions.
        return new Xid[0];
    }

    public void rollback(Xid xid) throws XAException
    {
        // TODO: What to do if all do not roll back?
        try
        {
            Map<StoreRef, LuceneIndexer> indexers = activeIndexersInGlobalTx.get(xid);
            if (indexers == null)
            {
                if (suspendedIndexersInGlobalTx.containsKey(xid))
                {
                    throw new XAException("Trying to commit indexes for a suspended transaction.");
                }
                else
                {
                    // nothing to do
                    return;
                }
            }
            for (LuceneIndexer indexer : indexers.values())
            {
                indexer.rollback();
            }
        }
        finally
        {
            activeIndexersInGlobalTx.remove(xid);
        }
    }

    public boolean setTransactionTimeout(int timeout) throws XAException
    {
        this.timeout = timeout;
        return true;
    }

    public void start(Xid xid, int flag) throws XAException
    {
        Map<StoreRef, LuceneIndexer> active = activeIndexersInGlobalTx.get(xid);
        Map<StoreRef, LuceneIndexer> suspended = suspendedIndexersInGlobalTx.get(xid);
        if (flag == XAResource.TMJOIN)
        {
            // must be active
            if ((active != null) && (suspended == null))
            {
                return;
            }
            else
            {
                throw new XAException("Trying to rejoin transaction in an invalid state");
            }

        }
        else if (flag == XAResource.TMRESUME)
        {
            // must be suspended
            if ((active == null) && (suspended != null))
            {
                suspendedIndexersInGlobalTx.remove(xid);
                activeIndexersInGlobalTx.put(xid, suspended);
                return;
            }
            else
            {
                throw new XAException("Trying to rejoin transaction in an invalid state");
            }

        }
        else if (flag == XAResource.TMNOFLAGS)
        {
            if ((active == null) && (suspended == null))
            {
                return;
            }
            else
            {
                throw new XAException("Trying to start an existing or suspended transaction");
            }
        }
        else
        {
            throw new XAException("Unkown flags for start " + flag);
        }

    }

    /*
     * Thread local support for transactions
     */

    /**
     * Commit the transaction
     */

    public void commit() throws IndexerException
    {
        try
        {
            Map<StoreRef, LuceneIndexer> indexers = threadLocalIndexers.get();
            if (indexers != null)
            {
                for (LuceneIndexer indexer : indexers.values())
                {
                    try
                    {
                        indexer.commit();
                    }
                    catch (IndexerException e)
                    {
                        rollback();
                        throw e;
                    }
                }
            }
        }
        finally
        {
            if (threadLocalIndexers.get() != null)
            {
                threadLocalIndexers.get().clear();
            }
        }
    }

    /**
     * Prepare the transaction TODO: Store prepare results
     * 
     * @return
     */
    public int prepare() throws IndexerException
    {
        boolean isPrepared = true;
        boolean isModified = false;
        Map<StoreRef, LuceneIndexer> indexers = threadLocalIndexers.get();
        if (indexers != null)
        {
            for (LuceneIndexer indexer : indexers.values())
            {
                try
                {
                    isModified |= indexer.isModified();
                    indexer.prepare();
                }
                catch (IndexerException e)
                {
                    isPrepared = false;
                    throw new IndexerException("Failed to prepare: requires rollback", e);
                }
            }
        }
        if (isPrepared)
        {
            if (isModified)
            {
                return XAResource.XA_OK;
            }
            else
            {
                return XAResource.XA_RDONLY;
            }
        }
        else
        {
            throw new IndexerException("Failed to prepare: requires rollback");
        }
    }

    /**
     * Roll back the transaction
     */
    public void rollback()
    {
        Map<StoreRef, LuceneIndexer> indexers = threadLocalIndexers.get();

        if (indexers != null)
        {
            for (LuceneIndexer indexer : indexers.values())
            {
                try
                {
                    indexer.rollback();
                }
                catch (IndexerException e)
                {

                }
            }
        }

        if (threadLocalIndexers.get() != null)
        {
            threadLocalIndexers.get().clear();
        }

    }

    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    public String getIndexRootLocation()
    {
        return indexRootLocation;
    }

    public int getIndexerBatchSize()
    {
        return indexerBatchSize;
    }

    public void setIndexerBatchSize(int indexerBatchSize)
    {
        this.indexerBatchSize = indexerBatchSize;
    }

    public int getIndexerMaxMergeDocs()
    {
        return indexerMaxMergeDocs;
    }

    public void setIndexerMaxMergeDocs(int indexerMaxMergeDocs)
    {
        this.indexerMaxMergeDocs = indexerMaxMergeDocs;
    }

    public int getIndexerMergeFactor()
    {
        return indexerMergeFactor;
    }

    public void setIndexerMergeFactor(int indexerMergeFactor)
    {
        this.indexerMergeFactor = indexerMergeFactor;
    }

    public int getIndexerMinMergeDocs()
    {
        return indexerMinMergeDocs;
    }

    public void setIndexerMinMergeDocs(int indexerMinMergeDocs)
    {
        this.indexerMinMergeDocs = indexerMinMergeDocs;
    }

    public String getLockDirectory()
    {
        return lockDirectory;
    }

    public void setLockDirectory(String lockDirectory)
    {
        this.lockDirectory = lockDirectory;
        // Set the lucene lock file via System property
        // org.apache.lucene.lockdir
        System.setProperty("org.apache.lucene.lockdir", lockDirectory);
        // Make sure the lock directory exists
        File lockDir = new File(lockDirectory);
        if (!lockDir.exists())
        {
            lockDir.mkdirs();
        }
        // clean out any existing locks when we start up

        File[] children = lockDir.listFiles();
        if (children != null)
        {
            for (int i = 0; i < children.length; i++)
            {
                File child = children[i];
                if (child.isFile())
                {
                    if (child.exists() && !child.delete() && child.exists())
                    {
                        throw new IllegalStateException("Failed to delete " + child);
                    }
                }
            }
        }
    }

    public int getQueryMaxClauses()
    {
        return queryMaxClauses;
    }

    public void setQueryMaxClauses(int queryMaxClauses)
    {
        this.queryMaxClauses = queryMaxClauses;
        BooleanQuery.setMaxClauseCount(this.queryMaxClauses);
    }

}
