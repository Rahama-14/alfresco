/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.search.impl.lucene.index;

import java.io.IOException;
import java.util.Map;

import org.springframework.context.ApplicationListener;

/**
 * An interface that exposes information about a Lucene Index and that allows registration of a listener for event
 * notifications.
 * 
 * @author dward
 */
public interface IndexMonitor
{
    /**
     * Gets the relative path of the index directory.
     * 
     * @return the relative path
     */
    public String getRelativePath();

    /**
     * Gets a snapshot of the statuses of the individual entries in this index.
     * 
     * @return a map of entry status names to entry counts
     */
    public Map<String, Integer> getStatusSnapshot();

    /**
     * Gets the actual size of the index in bytes.
     * 
     * @return the actual size in bytes
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public long getActualSize() throws IOException;

    /**
     * Gets the size used on disk by the index directory. A large discrepancy from the value returned by
     * {@link #getActualSize()} may indicate that there are unused data files.
     * 
     * @return the size on disk in bytes
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public long getUsedSize() throws IOException;

    /**
     * Gets the number of documents in the index.
     * 
     * @return the number of documents
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public int getNumberOfDocuments() throws IOException;

    /**
     * Gets the number of fields known to the index.
     * 
     * @return the number of fields
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public int getNumberOfFields() throws IOException;

    /**
     * Gets the number of indexed fields.
     * 
     * @return the number of indexed fields
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public int getNumberOfIndexedFields() throws IOException;

    /**
     * Registers a listener for events on this index.
     * 
     * @param listener
     *            the listener
     */
    public void addApplicationListener(ApplicationListener listener);
}
