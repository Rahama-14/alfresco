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
package org.alfresco.repo.content.cleanup;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.AbstractContentStore;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.content.EmptyContentReader;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.repo.content.filestore.FileContentWriter;
import org.alfresco.repo.domain.Node;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.domain.hibernate.NodeImpl;
import org.alfresco.repo.node.db.NodeDaoService;
import org.alfresco.repo.node.db.NodeDaoService.NodePropertyHandler;
import org.alfresco.repo.transaction.SingleEntryTransactionResourceInterceptor;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.tools.Repository;
import org.alfresco.tools.ToolException;
import org.alfresco.util.GUID;
import org.alfresco.util.TempFileProvider;
import org.alfresco.util.VmShutdownListener;
import org.apache.commons.lang.mutable.MutableInt;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Loads the repository up with orphaned content and then runs the cleaner.
 * <p>
 * A null content store produces ficticious content URLs.  The DB is loaded with ficticious URLs.
 * The process is kicked off. 
 * 
 * @author Derek Hulley
 * @since 2.1.3
 */
public class ContentStoreCleanerScalabilityRunner extends Repository
{
    private VmShutdownListener vmShutdownListener = new VmShutdownListener("ContentStoreCleanerScalabilityRunner");
    
    private ApplicationContext ctx;
    private SingleEntryTransactionResourceInterceptor txnResourceInterceptor;
    private HibernateHelper hibernateHelper;
    private TransactionService transactionService;
    private NodeDaoService nodeDaoService;
    private DictionaryService dictionaryService;
    private ContentStore contentStore;
    private ContentStoreCleaner cleaner;
    
    /**
     * Do the load and cleanup.
     */
    public static void main(String[] args)
    {
        new ContentStoreCleanerScalabilityRunner().start(args);
    }
    
    @Override
    protected synchronized int execute() throws ToolException
    {
        ctx = super.getApplicationContext();
        
        txnResourceInterceptor = (SingleEntryTransactionResourceInterceptor) ctx.getBean("sessionSizeResourceInterceptor");

        SessionFactory sessionFactory = (SessionFactory) ctx.getBean("sessionFactory");
        hibernateHelper = new HibernateHelper();
        hibernateHelper.setSessionFactory(sessionFactory);
        
        transactionService = (TransactionService) ctx.getBean("TransactionService");
        nodeDaoService = (NodeDaoService) ctx.getBean("nodeDaoService");
        dictionaryService = (DictionaryService) ctx.getBean("dictionaryService");
        
        int orphanCount = 100000;
        
        contentStore = new NullContentStore(orphanCount);
        
        loadData(orphanCount);
    
        long beforeIterate = System.currentTimeMillis();
//        iterateOverProperties();
        long afterIterate = System.currentTimeMillis();
        double aveIterate = (double) (afterIterate - beforeIterate) / (double) orphanCount / 1000D;
        
        System.out.println("Ready to clean store: " + contentStore);
        synchronized(this)
        {
            try { this.wait(10000L); } catch (InterruptedException e) {}
        }
        
        long beforeClean = System.currentTimeMillis();
        clean();
        long afterClean = System.currentTimeMillis();
        double aveClean = (double) (afterClean - beforeClean) / (double) orphanCount / 1000D;
        
        System.out.println();
        System.out.println(String.format("Iterating took %3f per 1000 content URLs in DB", aveIterate));
        System.out.println(String.format("Cleaning took %3f per 1000 content URLs in DB", aveClean));
        
        return 0;
    }
    
    private void loadData(final int maxCount)
    {
        final MutableInt doneCount = new MutableInt(0);
        // Batches of 1000 objects
        RetryingTransactionCallback<Integer> makeNodesCallback = new RetryingTransactionCallback<Integer>()
        {
            public Integer execute() throws Throwable
            {
                for (int i = 0; i < 1000; i++)
                {
                    // We don't need to write anything
                    String contentUrl = FileContentStore.createNewFileStoreUrl();
                    ContentData contentData = new ContentData(contentUrl, MimetypeMap.MIMETYPE_TEXT_PLAIN, 10, "UTF-8");
                    hibernateHelper.makeNode(contentData);
                    
                    int count = doneCount.intValue();
                    count++;
                    doneCount.setValue(count);
                    
                    // Do some reporting
                    if (count % 1000 == 0)
                    {
                        System.out.println(String.format("   " + (new Date()) + "Total created: %6d", count));
                    }
                    
                    // Double check for shutdown
                    if (vmShutdownListener.isVmShuttingDown())
                    {
                        break;
                    }
                }
                return maxCount;
            }
        };
        int repetitions = (int) Math.floor((double)maxCount / 1000.0);
        for (int i = 0; i < repetitions; i++)
        {
            transactionService.getRetryingTransactionHelper().doInTransaction(makeNodesCallback);
        }
    }
    
    private void iterateOverProperties()
    {
        final NodePropertyHandler nodePropertyHandler = new NodePropertyHandler()
        {
            int count = 0;
            public void handle(NodeRef nodeRef, QName nodeTypeQName, QName propertyQName, Serializable value)
            {
                count++;
                if (count % 1000 == 0)
                {
                    System.out.println("   " + (new Date()) + "Iterated over " + count + " content items");
                }
                if (vmShutdownListener.isVmShuttingDown())
                {
                    throw new RuntimeException("VM Shut down");
                }
            }
        };
        final DataTypeDefinition contentDataType = dictionaryService.getDataType(DataTypeDefinition.CONTENT);
        // execute in READ-WRITE txn
        RetryingTransactionCallback<Object> getUrlsCallback = new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                nodeDaoService.getPropertyValuesByActualType(contentDataType, nodePropertyHandler);
                return null;
            };
        };
        transactionService.getRetryingTransactionHelper().doInTransaction(getUrlsCallback);
    }
    
    private void clean()
    {
        ContentStoreCleanerListener listener = new ContentStoreCleanerListener()
        {
            private int count = 0;
            public void beforeDelete(ContentReader reader) throws ContentIOException
            {
                count++;
                if (count % 1000 == 0)
                {
                    System.out.println(String.format("   Total deleted: %6d", count));
                }
            }
        };
        // We use the default cleaner, but fix it up a bit
        cleaner = (ContentStoreCleaner) ctx.getBean("contentStoreCleaner");
        cleaner.setListeners(Collections.singletonList(listener));
        cleaner.setProtectDays(0);
        cleaner.setStores(Collections.singletonList(contentStore));
        
        // The cleaner has its own txns
        cleaner.execute();
    }
    
    private class NullContentStore extends AbstractContentStore
    {
        private ThreadLocal<File> hammeredFile;
        private int count;
        private int deletedCount;
        
        private NullContentStore(int count)
        {
            hammeredFile = new ThreadLocal<File>();
            this.count = count;
        }
        
        public boolean isWriteSupported()
        {
            return true;
        }

        /**
         * Returns a writer to a thread-unique file.  It's always the same file per thread so you must
         * use and close the writer before getting another.
         */
        @Override
        protected ContentWriter getWriterInternal(ContentReader existingContentReader, String newContentUrl)
        {
            File file = hammeredFile.get();
            if (file == null)
            {
                file = TempFileProvider.createTempFile("NullContentStore", ".txt");
                hammeredFile.set(file);
            }
            return new FileContentWriter(file);
        }

        @Override
        public void getUrls(Date createdAfter, Date createdBefore, ContentUrlHandler handler) throws ContentIOException
        {
            // Make up it up
            for (int i = 0; i < count; i++)
            {
                String contentUrl = FileContentStore.createNewFileStoreUrl() + "-imaginary";
                handler.handle(contentUrl);
            }
        }

        public ContentReader getReader(String contentUrl)
        {
            File file = hammeredFile.get();
            if (file == null)
            {
                return new EmptyContentReader(contentUrl);
            }
            else
            {
                return new FileContentReader(file);
            }
        }

        @Override
        public boolean delete(String contentUrl)
        {
            deletedCount++;
            if (deletedCount % 1000 == 0)
            {
                System.out.println(String.format("   Deleted %6d files", deletedCount));
            }
            return true;
        }
    }
    
    private class HibernateHelper extends HibernateDaoSupport
    {
        private Method methodMakeNode;
        private QName dataTypeDefContent;
        private QName contentQName;
        
        public HibernateHelper()
        {
            Class<HibernateHelper> clazz = HibernateHelper.class;
            try
            {
                methodMakeNode = clazz.getMethod("makeNode", new Class[] {ContentData.class});
            }
            catch (NoSuchMethodException e)
            {
                throw new RuntimeException("Failed to get methods");
            }
            dataTypeDefContent = DataTypeDefinition.CONTENT;
            contentQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "realContent");
        }
        /**
         * Creates a node with two properties
         */
        public void makeNode(ContentData contentData)
        {
            throw new UnsupportedOperationException("Fix this method up");
//            StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
//            Long nodeId = nodeDaoService.newNode(storeRef, GUID.generate(), ContentModel.TYPE_CONTENT).getFirst();
//            Node node = (Node) getHibernateTemplate().get(NodeImpl.class, nodeId);
//            
//            PropertyValue propertyValue = new PropertyValue(dataTypeDefContent, contentData);
//            node.getProperties().put(contentQName, propertyValue);
//            // persist the node
//            getHibernateTemplate().save(node);
//            
//            txnResourceInterceptor.performManualCheck(methodMakeNode, 10);
        }
    }
}
