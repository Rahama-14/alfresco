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
package org.alfresco.service.cmr.search;

import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * This is the common interface for both row (Alfresco node) and column (CMIS style property or function) based results.
 * The meta-data for the results sets contains the detailed info on what columns are available. For row based result
 * sets there is no selector - all the nodes returned do not have to have a specific type or aspect. For example, an FTS
 * search on properties of type d:content has no type constraint implied or otherwise. Searches against properties have
 * an implied type, but as there can be more than one property -> more than one type or aspect implied (eg via OR in FTS
 * or lucene) they are ignored An iterable result set from a searcher query.<b/> Implementations must implement the
 * indexes for row lookup as zero-based.<b/>
 * 
 * @author andyh
 * @param <ROW> 
 * @param <MD> 
 */
public interface ResultSetSPI<ROW extends ResultSetRow, MD extends ResultSetMetaData> extends Iterable<ROW> // Specific iterator over ResultSetRows
{
    /**
     * Get the number of rows in this result set. This will be less than or equal to the maximum number of rows
     * requested or the full length of the results set if no restriction on length are specified. If a skip count is
     * given, the length represents the number of results after the skip count and does not include the items skipped.
     * 
     * @return the number of results. -1 means unknown and can be returned for lazy evaluations of permissions when the
     *         actual size is not known and evaluated upon request.
     */
    public int length();

    /**
     * Get the id of the node at the given index (if there is only one selector or no selector)
     * 
     * @param n
     *            zero-based index
     * @return return the the node ref for the row if there is only one selector
     * @throws AmbiguousSelectorException
     */
    public NodeRef getNodeRef(int n);

    /**
     * Get the score for the node at the given position (if there is only one selector or no selector)
     * 
     * @param n
     *            zero-based index
     * @return return the score for the row if there is only one selector
     * @throws AmbiguousSelectorException
     */
    public float getScore(int n);

    /**
     * Close the result set and release any resources held/ The result set is bound to the transaction and will auto
     * close at the end of the transaction.
     */
    public void close();

    /**
     * Get a row from the result set by row index, starting at 0.
     * 
     * @param i
     *            zero-based index
     * @return return the row
     */
    public ROW getRow(int i);

    /**
     * Get a list of all the node refs in the result set (if there is only one selector or no selector)
     * 
     * @return the node refs if there is only one selector or no selector *
     * @throws AmbiguousSelectorException
     */
    public List<NodeRef> getNodeRefs();

    /**
     * Get a list of all the child associations in the results set. (if there is only one selectoror no selector)
     * 
     * @return the child assoc refs if there is only one selector or no selector *
     * @throws AmbiguousSelectorException
     */
    public List<ChildAssociationRef> getChildAssocRefs();

    /**
     * Get the child assoc ref for a particular row. (if there is only one selectoror no selector)
     * 
     * @param n
     *            zero-based index
     * @return the child assoc ref for the row if there is only one selector or no selector
     */
    public ChildAssociationRef getChildAssocRef(int n);

    /**
     * Get the meta data for the results set.
     * 
     * @return the metadata
     */
    public MD getResultSetMetaData();

    /**
     * Get the start point for this results set in the overall set of rows that match the query - this will be equal to
     * the skip count set when executing the query, and zero if this is not set.
     * 
     * @return the position of the first result in the overall result set
     */
    public int getStart();

    /**
     * Was this result set curtailed - are there more pages to the result set?
     * 
     * @return true if there are more pages in the result set
     */
    public boolean hasMore();
}
