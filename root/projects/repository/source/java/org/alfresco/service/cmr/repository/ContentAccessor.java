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
package org.alfresco.service.cmr.repository;

import java.util.Locale;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.transaction.TransactionService;

/**
 * Interface for instances that provide read and write access to content.
 * 
 * @author Derek Hulley
 */
public interface ContentAccessor
{    
    /**
     * Gets the open/close state of the underlying IO Channel.
     * 
     * @return Returns true if the underlying IO Channel is open
     */
    public boolean isChannelOpen();
    
    /**
     * Use this method to register any interest in events against underlying
     * content streams. 
     * {@link #getContentOutputStream() output stream}.
     * <p>
     * This method can only be used before the content stream has been retrieved.
     * <p>
     * When the stream has been closed, all listeners will be called
     * within a {@link #setTransactionService(TransactionService) transaction} -
     * to this end, a {@link TransactionService} must have been set as well.
     * 
     * @param listener a listener that will be called for output stream
     *      event notification
     *      
     * @see #setRetryingTransactionHelper(RetryingTransactionHelper)
     */
    public void addListener(ContentStreamListener listener);
    
    /**
     * Set the transaction helper for callbacks.
     */
    public void setRetryingTransactionHelper(RetryingTransactionHelper helper);
    
    /**
     * Gets the size of the content that this reader references.
     * 
     * @return Returns the document byte length, or <code>OL</code> if the
     *      content doesn't {@link #exists() exist}.
     */
    public long getSize();
    
    /**
     * Get the data representation of the content being accessed.
     * <p>
     * The content {@link #setMimetype(String) mimetype } must be set before this
     * method is called as the content data requires a mimetype whenever the
     * content URL is specified.
     * 
     * @return Returns the content data
     * 
     * @see ContentData#ContentData(String, String, long, String)
     */
    public ContentData getContentData();
    
    /**
     * Retrieve the URL that this accessor references
     * 
     * @return the content URL
     */
    public String getContentUrl();
    
    /**
     * Get the content mimetype
     * 
     * @return Returns a content mimetype
     */
    public String getMimetype();
    
    /**
     * Set the mimetype that must be used for accessing the content
     * 
     * @param mimetype the content mimetype
     */
    public void setMimetype(String mimetype);
    
    /**
     * Get the encoding of the content being accessed
     * 
     * @return Returns a valid java String encoding
     */
    public String getEncoding();
    
    /**
     * Set the <code>String</code> encoding for this accessor
     * 
     * @param encoding a java-recognised encoding format
     */
    public void setEncoding(String encoding);

    /**
     * Get the locale of the content being accessed
     *
     * @return  Returns a valid java Locale
     */
    public Locale getLocale();
    
    /**
     * Set the <code>Locale</code> for this accessor
     * 
     * @param locale    a java-recognised locale
     */
    public void setLocale(Locale locale);
}
