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
package org.alfresco.repo.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.PermissionEvaluationMode;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetMetaData;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;

/**
 * An empty result set
 * @author andyh
 *
 */
public class EmptyResultSet implements ResultSet
{

    /**
     * Default constructor
     */
    public EmptyResultSet()
    {
        super();
    }

    public int length()
    {
        return 0;
    }

    public NodeRef getNodeRef(int n)
    {
        throw new UnsupportedOperationException();
    }

    public float getScore(int n)
    {
        throw new UnsupportedOperationException();
    }

    public Iterator<ResultSetRow> iterator()
    {
        ArrayList<ResultSetRow> dummy = new ArrayList<ResultSetRow>(0);
        return dummy.iterator();
    }

    public void close()
    {

    }

    public ResultSetRow getRow(int i)
    {
        throw new UnsupportedOperationException();
    }

    public List<NodeRef> getNodeRefs()
    {
        return Collections.<NodeRef>emptyList();
    }

    public List<ChildAssociationRef> getChildAssocRefs()
    {
        return Collections.<ChildAssociationRef>emptyList();
    }

    public ChildAssociationRef getChildAssocRef(int n)
    {
        throw new UnsupportedOperationException();
    }

    public ResultSetMetaData getResultSetMetaData()
    {
        return new SimpleResultSetMetaData(LimitBy.UNLIMITED, PermissionEvaluationMode.EAGER, new SearchParameters());
    }

    public int getStart()
    {
        return 0;
    }

    public boolean hasMore()
    {
        return false;
    }
}
