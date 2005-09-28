/*
 * Copyright (C) 2005 Alfresco, Inc.
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
package org.alfresco.repo.content;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.transaction.TransactionUtil;
import org.alfresco.service.cmr.repository.ContentAccessor;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.AfterReturningAdvice;

/**
 * Provides basic support for content accessors.
 * 
 * @author Derek Hulley
 */
public abstract class AbstractContentAccessor implements ContentAccessor
{
    private static Log logger = LogFactory.getLog(AbstractContentAccessor.class);
    
    /** when set, ensures that listeners are executed within a transaction */
    private TransactionService transactionService;
    
    private String contentUrl;
    private String mimetype;
    private String encoding;

    /**
     * @param contentUrl the content URL
     */
    protected AbstractContentAccessor(String contentUrl)
    {
        if (contentUrl == null || contentUrl.length() == 0)
        {
            throw new IllegalArgumentException("contentUrl must be a valid String");
        }
        this.contentUrl = contentUrl;
    }
    
    public ContentData getContentData()
    {
        ContentData property = new ContentData(contentUrl, mimetype, getSize(), encoding);
        return property;
    }

    /**
     * Provides access to transactions for implementing classes
     * 
     * @return Returns a source of user transactions
     */
    protected TransactionService getTransactionService()
    {
        return transactionService;
    }

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder(100);
        sb.append("ContentAccessor[ content=").append(getContentData()).append("]");
        return sb.toString();
    }
    
    public String getContentUrl()
    {
        return contentUrl;
    }
    
    public String getMimetype()
    {
        return mimetype;
    }

    /**
     * @param mimetype the underlying content's mimetype - null if unknown
     */
    public void setMimetype(String mimetype)
    {
        this.mimetype = mimetype;
    }

    /**
     * @return Returns the content encoding - null if unknown
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * @param encoding the underlying content's encoding - null if unknown
     */
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }
    
    /**
     * Advise that listens for the completion of specific methods on the
     * {@link java.nio.channels.ByteChannel} interface.
     * 
     * @author Derek Hulley
     */
    protected class ByteChannelCallbackAdvise implements AfterReturningAdvice
    {
        private List<ContentStreamListener> listeners;

        public ByteChannelCallbackAdvise(List<ContentStreamListener> listeners)
        {
            this.listeners = listeners;
        }
        
        /**
         * Provides transactional callbacks to the listeners 
         */
        public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable
        {
            // check for specific events
            if (method.getName().equals("close"))
            {
                fireChannelClosed();
            }
        }
        
        private void fireChannelClosed()
        {
            if (listeners.size() == 0)
            {
                // nothing to do
                return;
            }
            // ensure that we are in a transaction
            if (transactionService == null)
            {
                throw new AlfrescoRuntimeException("A transaction service is required when there are listeners present");
            }
            TransactionUtil.TransactionWork<Object> work = new TransactionUtil.TransactionWork<Object>()
                    {
                        public Object doWork()
                        {
                            // call the listeners
                            for (ContentStreamListener listener : listeners)
                            {
                                listener.contentStreamClosed();
                            }
                            return null;
                        }
                    };
            TransactionUtil.executeInUserTransaction(transactionService, work);
            // done
            if (logger.isDebugEnabled())
            {
                logger.debug("Content listeners called: close");
            }
        }
    }
    
    /**
     * Wraps a <code>FileChannel</code> to provide callbacks to listeners when the
     * channel is {@link java.nio.channels.Channel#close() closed}.
     * 
     * @author Derek Hulley
     */
    protected class CallbackFileChannel extends FileChannel
    {
        /** the channel to route all calls to */
        private FileChannel delegate;
        /** listeners waiting for the stream close */
        private List<ContentStreamListener> listeners;

        /**
         * @param delegate the channel that will perform the work
         * @param listeners listeners for events coming from this channel
         */
        public CallbackFileChannel(
                FileChannel delegate,
                List<ContentStreamListener> listeners)
        {
            if (delegate == null)
            {
                throw new IllegalArgumentException("FileChannel delegate is required");
            }
            if (delegate instanceof CallbackFileChannel)
            {
                throw new IllegalArgumentException("FileChannel delegate may not be a CallbackFileChannel");
            }
            
            this.delegate = delegate;
            this.listeners = listeners;
        }
        
        /**
         * Closes the channel and makes the callbacks to the listeners
         */
        @Override
        protected void implCloseChannel() throws IOException
        {
            delegate.close();
            fireChannelClosed();
        }

        /**
         * Helper method to notify stream listeners
         */
        private void fireChannelClosed()
        {
            if (listeners.size() == 0)
            {
                // nothing to do
                return;
            }
            // ensure that we are in a transaction
            if (transactionService == null)
            {
                throw new AlfrescoRuntimeException("A transaction service is required when there are listeners present");
            }
            TransactionUtil.TransactionWork<Object> work = new TransactionUtil.TransactionWork<Object>()
                    {
                        public Object doWork()
                        {
                            // call the listeners
                            for (ContentStreamListener listener : listeners)
                            {
                                listener.contentStreamClosed();
                            }
                            return null;
                        }
                    };
            TransactionUtil.executeInUserTransaction(transactionService, work);
            // done
            if (logger.isDebugEnabled())
            {
                logger.debug("Content listeners called: close");
            }
        }

        @Override
        public void force(boolean metaData) throws IOException
        {
            delegate.force(metaData);
        }

        @Override
        public FileLock lock(long position, long size, boolean shared) throws IOException
        {
            return delegate.lock(position, size, shared);
        }

        @Override
        public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException
        {
            return delegate.map(mode, position, size);
        }

        @Override
        public long position() throws IOException
        {
            return delegate.position();
        }

        @Override
        public FileChannel position(long newPosition) throws IOException
        {
            return delegate.position(newPosition);
        }

        @Override
        public int read(ByteBuffer dst) throws IOException
        {
            return delegate.read(dst);
        }

        @Override
        public int read(ByteBuffer dst, long position) throws IOException
        {
            return delegate.read(dst, position);
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException
        {
            return delegate.read(dsts, offset, length);
        }

        @Override
        public long size() throws IOException
        {
            return delegate.size();
        }

        @Override
        public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException
        {
            return delegate.transferFrom(src, position, count);
        }

        @Override
        public long transferTo(long position, long count, WritableByteChannel target) throws IOException
        {
            return delegate.transferTo(position, count, target);
        }

        @Override
        public FileChannel truncate(long size) throws IOException
        {
            return delegate.truncate(size);
        }

        @Override
        public FileLock tryLock(long position, long size, boolean shared) throws IOException
        {
            return delegate.tryLock(position, size, shared);
        }

        @Override
        public int write(ByteBuffer src) throws IOException
        {
            return delegate.write(src);
        }

        @Override
        public int write(ByteBuffer src, long position) throws IOException
        {
            return delegate.write(src, position);
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException
        {
            return delegate.write(srcs, offset, length);
        }
    }
}
