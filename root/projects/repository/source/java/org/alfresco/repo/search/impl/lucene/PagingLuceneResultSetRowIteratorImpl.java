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
package org.alfresco.repo.search.impl.lucene;

import java.util.ListIterator;

import org.alfresco.service.cmr.search.ResultSetRow;

/**
 * @author andyh
 */
public class PagingLuceneResultSetRowIteratorImpl implements ListIterator<ResultSetRow>
{
    /**
     * The result set
     */
    private PagingLuceneResultSet resultSet;

    /**
     * The current position
     */
    private int position = -1;

    /**
     * The maximum position
     */
    private int max;

    /**
     * Create an iterator over the result set. Follows stadard ListIterator conventions
     * 
     * @param resultSet
     */
    public PagingLuceneResultSetRowIteratorImpl(PagingLuceneResultSet resultSet)
    {
        this.resultSet = resultSet;
        this.max = resultSet.getLength();
    }

    public PagingLuceneResultSet getResultSet()
    {
        return resultSet;
    }

    /*
     * ListIterator implementation
     */
    public boolean hasNext()
    {
        return position < (max - 1);
    }

    public boolean allowsReverse()
    {
        return true;
    }

    public boolean hasPrevious()
    {
        return position > 0;
    }

    public ResultSetRow next()
    {
        return resultSet.getRow(moveToNextPosition());
    }

    protected int moveToNextPosition()
    {
        return ++position;
    }

    public ResultSetRow previous()
    {
        return resultSet.getRow(moveToPreviousPosition());
    }

    protected int moveToPreviousPosition()
    {
        return --position;
    }

    public int nextIndex()
    {
        return position + 1;
    }

    public int previousIndex()
    {
        return position - 1;
    }

    /*
     * Mutation is not supported
     */

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    public void set(ResultSetRow o)
    {
        throw new UnsupportedOperationException();
    }

    public void add(ResultSetRow o)
    {
        throw new UnsupportedOperationException();
    }

}
