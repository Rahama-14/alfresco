/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Alfresco Network License. You may obtain a
 * copy of the License at
 *
 *   http://www.alfrescosoftware.com/legal/
 *
 * Please view the license relevant to your network subscription.
 *
 * BY CLICKING THE "I UNDERSTAND AND ACCEPT" BOX, OR INSTALLING,  
 * READING OR USING ALFRESCO'S Network SOFTWARE (THE "SOFTWARE"),  
 * YOU ARE AGREEING ON BEHALF OF THE ENTITY LICENSING THE SOFTWARE    
 * ("COMPANY") THAT COMPANY WILL BE BOUND BY AND IS BECOMING A PARTY TO 
 * THIS ALFRESCO NETWORK AGREEMENT ("AGREEMENT") AND THAT YOU HAVE THE   
 * AUTHORITY TO BIND COMPANY. IF COMPANY DOES NOT AGREE TO ALL OF THE   
 * TERMS OF THIS AGREEMENT, DO NOT SELECT THE "I UNDERSTAND AND AGREE"   
 * BOX AND DO NOT INSTALL THE SOFTWARE OR VIEW THE SOURCE CODE. COMPANY   
 * HAS NOT BECOME A LICENSEE OF, AND IS NOT AUTHORIZED TO USE THE    
 * SOFTWARE UNLESS AND UNTIL IT HAS AGREED TO BE BOUND BY THESE LICENSE  
 * TERMS. THE "EFFECTIVE DATE" FOR THIS AGREEMENT SHALL BE THE DAY YOU  
 * CHECK THE "I UNDERSTAND AND ACCEPT" BOX.
 */
package org.alfresco.repo.content.replication;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * </h1><u>Replicating Content Store</u></h1>
 * <p>
 * A content store implementation that is able to replicate content between stores.
 * Content is not persisted by this store, but rather it relies on any number of
 * child {@link org.alfresco.repo.content.ContentStore stores} to provide access to
 * content readers and writers.
 * <p>
 * The order in which the stores appear in the list of stores participating is
 * important.  The first store in the list is known as the <i>primary store</i>.
 * When the replicator goes to fetch content, the stores are searched
 * from first to last.  The stores should therefore be arranged in order of
 * speed.
 * <p>
 * It supports the notion of inbound and/or outbound replication, both of which can be
 * operational at the same time.
 * 
 * </h2><u>Outbound Replication</u></h2>
 * <p>
 * When this is enabled, then the primary store is used for writes.  When the
 * content write completes (i.e. the write channel is closed) then the content
 * is synchronously copied to all other stores.  The write is therefore slowed
 * down, but the content replication will occur <i>in-transaction</i>.
 * <p>
 * The {@link #setOutboundThreadPoolExecutor(boolean) outboundThreadPoolExecutor }
 * property to enable asynchronous replication.<br>
 * With asynchronous replication, there is always a risk that a failure
 * occurs during the replication.  Depending on the configuration of the server,
 * further action may need to be taken to rectify the problem manually.
 *  
 * </h2><u>Inbound Replication</u></h2>
 * <p>
 * This can be used to lazily replicate content onto the primary store.  When
 * content can't be found in the primary store, the other stores are checked
 * in order.  If content is found, then it is copied into the local store
 * before being returned.  Subsequent accesses will use the primary store.<br>
 * This should be used where the secondary stores are much slower, such as in
 * the case of a store against some kind of archival mechanism.
 * 
 * <h2><u>No Replication</u></h2>
 * <p>
 * Content is not written to the primary store only.  The other stores are
 * only used to retrieve content and the primary store is not updated with
 * the content.
 * 
 * @author Derek Hulley
 */
public class ReplicatingContentStore implements ContentStore
{
    /*
     * The replication process uses thread synchronization as it can
     * decide to write content to specific URLs during requests for
     * a reader.
     * While this won't help the underlying stores if there are
     * multiple replications on top of them, it will prevent repeated
     * work from multiple threads entering an instance of this component
     * looking for the same content at the same time.
     */
    
    private static Log logger = LogFactory.getLog(ReplicatingContentStore.class);
    
    private TransactionService transactionService;
    private ContentStore primaryStore;
    private List<ContentStore> secondaryStores;
    private boolean inbound;
    private boolean outbound;
    private ThreadPoolExecutor outboundThreadPoolExecutor;
    
    private Lock readLock;
    private Lock writeLock;

    /**
     * Default constructor set <code>inbound = false</code> and <code>outbound = true</code>;
     */
    public ReplicatingContentStore()
    {
        inbound = false;
        outbound = true;
        
        ReadWriteLock storeLock = new ReentrantReadWriteLock();
        readLock = storeLock.readLock();
        writeLock = storeLock.writeLock();
    }
    
    /**
     * Required to ensure that content listeners are executed in a transaction
     * 
     * @param transactionService
     */
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * Set the primary store that content will be replicated to or from
     * 
     * @param primaryStore the primary content store
     */
    protected void setPrimaryStore(ContentStore primaryStore)
    {
        this.primaryStore = primaryStore;
    }

    /**
     * Set the secondary stores that this component will replicate to or from
     * 
     * @param stores a list of stores to replicate to or from
     */
    public void setSecondaryStores(List<ContentStore> secondaryStores)
    {
        this.secondaryStores = secondaryStores;
    }
    
    /**
     * Set whether or not this component should replicate content to the
     * primary store if not found.
     *  
     * @param inbound true to pull content onto the primary store when found
     *      on one of the other stores
     */
    public void setInbound(boolean inbound)
    {
        this.inbound = inbound;
    }
    
    /**
     * Set whether or not this component should replicate content to all stores
     * as it is written.
     *  
     * @param outbound true to enable synchronous replication to all stores
     */
    public void setOutbound(boolean outbound)
    {
        this.outbound = outbound;
    }

    /**
     * Set the thread pool executer
     * 
     * @param outboundThreadPoolExecutor set this to have the synchronization occur in a separate
     *      thread
     */
    public void setOutboundThreadPoolExecutor(ThreadPoolExecutor outboundThreadPoolExecutor)
    {
        this.outboundThreadPoolExecutor = outboundThreadPoolExecutor;
    }
    
    /**
     * Uses the {@link #getReader(String) reader} to determine this.
     */
    public boolean exists(String contentUrl) throws ContentIOException
    {
        /*
         * Not the most efficient, but prevents duplication of logic 
         */
        ContentReader reader = getReader(contentUrl);
        return reader.exists();
    }

    /**
     * Forwards the call directly to the first store in the list of stores.
     */
    public ContentReader getReader(String contentUrl) throws ContentIOException
    {
        if (primaryStore == null)
        {
            throw new AlfrescoRuntimeException("ReplicatingContentStore not initialised");
        }
        
        // get a read lock so that we are sure that no replication is underway
        ContentReader existingContenReader = null;
        readLock.lock();
        try
        {
            // get a reader from the primary store
            ContentReader primaryReader = primaryStore.getReader(contentUrl);
            
            // give it straight back if the content is there
            if (primaryReader.exists())
            {
                return primaryReader;
            }

            // the content is not in the primary reader so we have to go looking for it
            ContentReader secondaryContentReader = null;
            for (ContentStore store : secondaryStores)
            {
                ContentReader reader = store.getReader(contentUrl);
                if (reader.exists())
                {
                    // found the content in a secondary store
                    secondaryContentReader = reader;
                    break;
                }
            }
            // we already know that the primary has nothing
            // drop out if no content was found
            if (secondaryContentReader == null)
            {
                return primaryReader;
            }
            // secondary content was found
            // return it if we are not doing inbound
            if (!inbound)
            {
                return secondaryContentReader;
            }
            
            // we have to replicate inbound
            existingContenReader = secondaryContentReader;
        }
        finally
        {
            readLock.unlock();
        }
        
        // -- a small gap for concurrent threads to get through --
        
        // do inbound replication
        writeLock.lock();
        try
        {
            // double check the primary
            ContentReader primaryContentReader = primaryStore.getReader(contentUrl);
            if (primaryContentReader.exists())
            {
                // we were beaten to it
                return primaryContentReader;
            }
            // get a writer
            ContentWriter primaryContentWriter = primaryStore.getWriter(existingContenReader, contentUrl);
            // copy it over
            primaryContentWriter.putContent(existingContenReader);
            // get a writer to the new content
            primaryContentReader = primaryContentWriter.getReader();
            // done
            return primaryContentReader;
        }
        finally
        {
            writeLock.unlock();
        }
    }

    /**
     * 
     */
    public ContentWriter getWriter(ContentReader existingContentReader, String newContentUrl) throws ContentIOException
    {
        // get the writer
        ContentWriter writer = primaryStore.getWriter(existingContentReader, newContentUrl);
        
        // attach a replicating listener if outbound replication is on
        if (outbound)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Attaching " + (outboundThreadPoolExecutor == null ? "" : "a") + "synchronous " +
                                "replicating listener to local writer: \n" +
                        "   primary store: " + primaryStore + "\n" +
                        "   writer: " + writer);
            }
            // attach the listener
            ContentStreamListener listener = new ReplicatingWriteListener(secondaryStores, writer, outboundThreadPoolExecutor);
            writer.addListener(listener);
            writer.setTransactionService(transactionService);   // mandatory when listeners are added
        }
        
        // done
        return writer;
    }

    /**
     * Performs a delete on the local store and if outbound replication is on, propogates
     * the delete to the other stores too.
     * 
     * @return Returns the value returned by the delete on the primary store.
     */
    public boolean delete(String contentUrl) throws ContentIOException
    {
        // delete on the primary store
        boolean deleted = primaryStore.delete(contentUrl);
        
        // propogate outbound deletions
        if (outbound)
        {
            for (ContentStore store : secondaryStores)
            {
                store.delete(contentUrl);
            }
            // done
            if (logger.isDebugEnabled())
            {
                logger.debug("Propagated content delete: " + contentUrl);
            }
        }
        return deleted;
    }

    /**
     * @return Returns the results as given by the primary store
     */
    public List<String> listUrls() throws ContentIOException
    {
        // we could choose to get this from any store, but the primary one is by contract
        // the one chosen by the configuration to be the default for reads
        return primaryStore.listUrls();
    }

    /**
     * Replicates the content upon stream closure.  If the thread pool is available,
     * then the process will be asynchronous.
     * <p>
     * No transaction boundaries have been declared as the
     * {@link ContentWriter#addListener(ContentStreamListener)} method indicates that
     * all listeners will be called within a transaction.
     * 
     * @author Derek Hulley
     */
    public static class ReplicatingWriteListener implements ContentStreamListener
    {
        private List<ContentStore> stores;
        private ContentWriter writer;
        private ThreadPoolExecutor threadPoolExecutor;
        
        public ReplicatingWriteListener(
                List<ContentStore> stores,
                ContentWriter writer,
                ThreadPoolExecutor threadPoolExecutor)
        {
            this.stores = stores;
            this.writer = writer;
            this.threadPoolExecutor = threadPoolExecutor;
        }
        
        public void contentStreamClosed() throws ContentIOException
        {
            Runnable runnable = new ReplicateOnCloseRunnable();
            if (threadPoolExecutor == null)
            {
                // execute direct
                runnable.run();
            }
            else
            {
                threadPoolExecutor.execute(runnable);
            }
        }
        
        /**
         * Performs the actual replication work.
         * 
         * @author Derek Hulley
         */
        private class ReplicateOnCloseRunnable implements Runnable
        {
            public void run()
            {
                for (ContentStore store : stores)
                {
                    try
                    {
                        // replicate the content to the store - we know the URL that we want to write to
                        ContentReader reader = writer.getReader();
                        String contentUrl = reader.getContentUrl();
                        // in order to replicate, we have to specify the URL that we are going to write to
                        ContentWriter replicatedWriter = store.getWriter(null, contentUrl);
                        // write it
                        replicatedWriter.putContent(reader);
                        
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Replicated content to store: \n" +
                                    "   url: " + contentUrl + "\n" +
                                    "   to store: " + store);
                        }
                    }
                    catch (Throwable e)
                    {
                        throw new ContentIOException("Content replication failed: \n" +
                                "   url: " + writer.getContentUrl() + "\n" +
                                "   to store: " + store);
                    }
                }
            }
        }
    }
}
