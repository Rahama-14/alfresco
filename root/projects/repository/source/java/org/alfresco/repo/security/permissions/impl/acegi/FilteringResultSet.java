/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.security.permissions.impl.acegi;

import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;

import org.alfresco.repo.search.ResultSetRowIterator;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetMetaData;
import org.alfresco.service.cmr.search.ResultSetRow;

/**
 * Filtering result set to support permission checks
 * 
 * @author andyh
 */
public class FilteringResultSet extends ACLEntryAfterInvocationProvider implements ResultSet
{
    private ResultSet unfiltered;

    private BitSet inclusionMask;

    private ResultSetMetaData resultSetMetaData;

    public FilteringResultSet(ResultSet unfiltered)
    {
        super();
        this.unfiltered = unfiltered;
        inclusionMask = new BitSet(unfiltered.length());
    }
    
    public FilteringResultSet(ResultSet unfiltered, BitSet inclusionMask)
    {
        super();
        this.unfiltered = unfiltered;
        this.inclusionMask = inclusionMask;
    }

    /* package */ResultSet getUnFilteredResultSet()
    {
        return unfiltered;
    }

    /* package */void setIncluded(int i, boolean excluded)
    {
        inclusionMask.set(i, excluded);
    }

    /* package */boolean getIncluded(int i)
    {
        return inclusionMask.get(i);
    }

    public int length()
    {
        return inclusionMask.cardinality();
    }

    private int translateIndex(int n)
    {
        if (n > length())
        {
            throw new IndexOutOfBoundsException();
        }
        int count = -1;
        for (int i = 0, l = unfiltered.length(); i < l; i++)
        {
            if (inclusionMask.get(i))
            {
                count++;
            }
            if (count == n)
            {
                return i;
            }

        }
        throw new IndexOutOfBoundsException();
    }

    public NodeRef getNodeRef(int n)
    {
        return unfiltered.getNodeRef(translateIndex(n));
    }

    public float getScore(int n)
    {
        return unfiltered.getScore(translateIndex(n));
    }

    public void close()
    {
        unfiltered.close();
    }

    public ResultSetRow getRow(int i)
    {
        return unfiltered.getRow(translateIndex(i));
    }

    public List<NodeRef> getNodeRefs()
    {
        List<NodeRef> answer = unfiltered.getNodeRefs();
        for (int i = unfiltered.length() - 1; i >= 0; i--)
        {
            if (!inclusionMask.get(i))
            {
                answer.remove(i);
            }
        }
        return answer;
    }

    public List<ChildAssociationRef> getChildAssocRefs()
    {
        List<ChildAssociationRef> answer = unfiltered.getChildAssocRefs();
        for (int i = unfiltered.length() - 1; i >= 0; i--)
        {
            if (!inclusionMask.get(i))
            {
                answer.remove(i);
            }
        }
        return answer;
    }

    public ChildAssociationRef getChildAssocRef(int n)
    {
        return unfiltered.getChildAssocRef(translateIndex(n));
    }

    public ListIterator<ResultSetRow> iterator()
    {
        return new FilteringIterator();
    }

    class FilteringIterator implements ResultSetRowIterator
    {
        // -1 at the start
        int underlyingPosition = -1;

        public boolean hasNext()
        {
            return inclusionMask.nextSetBit(underlyingPosition + 1) != -1;
        }

        public ResultSetRow next()
        {
            underlyingPosition = inclusionMask.nextSetBit(underlyingPosition + 1);
            if (underlyingPosition == -1)
            {
                throw new IllegalStateException();
            }
            return unfiltered.getRow(underlyingPosition);
        }

        public boolean hasPrevious()
        {
            if (underlyingPosition <= 0)
            {
                return false;
            }
            else
            {
                for (int i = underlyingPosition - 1; i >= 0; i--)
                {
                    if (inclusionMask.get(i))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        public ResultSetRow previous()
        {
            if (underlyingPosition <= 0)
            {
                throw new IllegalStateException();
            }
            for (int i = underlyingPosition - 1; i >= 0; i--)
            {
                if (inclusionMask.get(i))
                {
                    underlyingPosition = i;
                    return unfiltered.getRow(underlyingPosition);
                }
            }
            throw new IllegalStateException();
        }

        public int nextIndex()
        {
            return inclusionMask.nextSetBit(underlyingPosition + 1);
        }

        public int previousIndex()
        {
            if (underlyingPosition <= 0)
            {
                return -1;
            }
            for (int i = underlyingPosition - 1; i >= 0; i--)
            {
                if (inclusionMask.get(i))
                {
                    return i;
                }
            }
            return -1;
        }

        /*
         * Mutation is not supported
         */

        public void remove()
        {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public void set(ResultSetRow o)
        {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public void add(ResultSetRow o)
        {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public boolean allowsReverse()
        {
            return true;
        }

        public ResultSet getResultSet()
        {
            return FilteringResultSet.this;
        }

    }

    public ResultSetMetaData getResultSetMetaData()
    {
        return resultSetMetaData;
    }

    public void setResultSetMetaData(ResultSetMetaData resultSetMetaData)
    {
        this.resultSetMetaData = resultSetMetaData;
    }

    public int getStart()
    {
        throw new UnsupportedOperationException();
    }

    public boolean hasMore()
    {
        throw new UnsupportedOperationException();
    }

}
