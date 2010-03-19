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
package org.alfresco.repo.webservice.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.webservice.AbstractQuery;
import org.alfresco.repo.webservice.Utils;
import org.alfresco.repo.webservice.types.NamedValue;
import org.alfresco.repo.webservice.types.Query;
import org.alfresco.repo.webservice.types.ResultSet;
import org.alfresco.repo.webservice.types.ResultSetRow;
import org.alfresco.repo.webservice.types.ResultSetRowNode;
import org.alfresco.repo.webservice.types.Store;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

/**
 * A query to using full search.
 * 
 * @author Derek Hulley
 * @since 2.1
 */
public class SearchQuery extends AbstractQuery<ResultSet>
{
    private static final long serialVersionUID = 5429510102265380433L;

    private Store store;
    private Query query;

    /**
     * @param node              The node to query against
     * @param association       The association type to query or <tt>null</tt> to query all
     */
    public SearchQuery(Store store, Query query)
    {
        this.store = store;
        this.query = query;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(128);
        sb.append("SearchQuery")
          .append("[ store=").append(this.store.getScheme()).append(":").append(this.store.getAddress())
          .append(" language=").append(this.query.getLanguage())
          .append(" statement=").append(this.query.getStatement())
          .append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public ResultSet execute(ServiceRegistry serviceRegistry)
    {
        SearchService searchService = serviceRegistry.getSearchService();
        NodeService nodeService = serviceRegistry.getNodeService();
        DictionaryService dictionaryService = serviceRegistry.getDictionaryService();
        
        // handle the special search string of * meaning, get everything
        String statement = query.getStatement();
        if (statement.equals("*"))
        {
            statement = "ISNODE:*";
        }
        org.alfresco.service.cmr.search.ResultSet searchResults = null;
        try
        {
            StoreRef storeRef = Utils.convertToStoreRef(store);
            searchResults = searchService.query(storeRef, query.getLanguage(), statement);
            return convert(
                    nodeService,
                    dictionaryService,
                    searchResults);
        }
        finally
        {
            if (searchResults != null)
            {
                try
                {
                    searchResults.close();
                }
                catch (Throwable e)
                {
                }
            }
        }
    }

    private ResultSet convert(
            NodeService nodeService,
            DictionaryService dictionaryService,
            org.alfresco.service.cmr.search.ResultSet searchResults)
    {
        ResultSet results = new ResultSet();
        List<ResultSetRow> rowsList = new ArrayList<org.alfresco.repo.webservice.types.ResultSetRow>();

        int index = 0;
        for (org.alfresco.service.cmr.search.ResultSetRow searchRow : searchResults)
        {
            NodeRef nodeRef = searchRow.getNodeRef();
            // Search can return nodes that no longer exist, so we need to ignore these
            if (!nodeService.exists(nodeRef))
            {
                continue;
            }
            ResultSetRowNode rowNode = createResultSetRowNode(nodeRef, nodeService);
            
            // get the data for the row and build up the columns structure
            Map<String, Serializable> values = searchRow.getValues();
            NamedValue[] columns = new NamedValue[values.size() + 1];
            int col = 1;
            for (String attributeName : values.keySet())
            {
                columns[col] = Utils.createNamedValue(dictionaryService, QName.createQName(attributeName), values.get(attributeName));
                col++;
            }
            
            // add one extra column for the node's path
            columns[0] = Utils.createNamedValue(dictionaryService, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "path"), nodeService.getPath(nodeRef).toString());

            ResultSetRow row = new org.alfresco.repo.webservice.types.ResultSetRow();
            row.setColumns(columns);
            row.setScore(searchRow.getScore());
            row.setRowIndex(index);
            row.setNode(rowNode);

            // add the row to the overall results list
            rowsList.add(row);
            index++;
        }

        // Convert list to array
        int totalRows = rowsList.size();
        ResultSetRow[] rows = rowsList.toArray(new org.alfresco.repo.webservice.types.ResultSetRow[totalRows]);

        // add the rows to the result set and set the total row count
        results.setRows(rows);
        results.setTotalRowCount(totalRows);

        return results;
    }
}