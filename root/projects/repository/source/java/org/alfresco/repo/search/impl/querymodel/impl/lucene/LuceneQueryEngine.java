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
package org.alfresco.repo.search.impl.querymodel.impl.lucene;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.search.SearcherException;
import org.alfresco.repo.search.impl.lucene.ClosingIndexSearcher;
import org.alfresco.repo.search.impl.lucene.LuceneIndexerAndSearcher;
import org.alfresco.repo.search.impl.lucene.LuceneResultSet;
import org.alfresco.repo.search.impl.lucene.LuceneSearcher;
import org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext;
import org.alfresco.repo.search.impl.querymodel.Query;
import org.alfresco.repo.search.impl.querymodel.QueryEngine;
import org.alfresco.repo.search.impl.querymodel.QueryEngineResults;
import org.alfresco.repo.search.impl.querymodel.QueryModelFactory;
import org.alfresco.repo.search.impl.querymodel.QueryOptions;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.Pair;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Sort;

/**
 * @author andyh
 */
public class LuceneQueryEngine implements QueryEngine
{
    private DictionaryService dictionaryService;

    private LuceneIndexerAndSearcher indexAndSearcher;

    private NodeService nodeService;

    private TenantService tenantService;

    private NamespaceService namespaceService;

    /**
     * @param dictionaryService
     *            the dictionaryService to set
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     * @param indexAndSearcher
     *            the indexAndSearcher to set
     */
    public void setIndexAndSearcher(LuceneIndexerAndSearcher indexAndSearcher)
    {
        this.indexAndSearcher = indexAndSearcher;
    }

    /**
     * @param nodeService
     *            the nodeService to set
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param tenantService
     *            the tenantService to set
     */
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }

    /**
     * @param namespaceService
     *            the namespaceService to set
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    public QueryModelFactory getQueryModelFactory()
    {
        return new LuceneQueryModelFactory();
    }

    public QueryEngineResults executeQuery(Query query, QueryOptions options, FunctionEvaluationContext functionContext)
    {
        Set<String> selectorGroup = null;
        if (query.getSource() != null)
        {
            List<Set<String>> selectorGroups = query.getSource().getSelectorGroups(functionContext);

            if (selectorGroups.size() == 0)
            {
                throw new UnsupportedOperationException("No selectors");
            }

            if (selectorGroups.size() > 1)
            {
                throw new UnsupportedOperationException("Advanced join is not supported");
            }

            selectorGroup = selectorGroups.get(0);
        }

        SearchParameters searchParameters = new SearchParameters();
        searchParameters.setBulkFetch(options.getFetchSize() > 0);
        searchParameters.setBulkFetchSize(options.getFetchSize());
        if (options.getMaxItems() > 0)
        {
            searchParameters.setLimitBy(LimitBy.FINAL_SIZE);
            searchParameters.setLimit(options.getMaxItems() + options.getSkipCount());
        }
        else
        {
            searchParameters.setLimitBy(LimitBy.UNLIMITED);
        }

        try
        {
            StoreRef storeRef = options.getStores().get(0);
            if (query instanceof LuceneQueryBuilder)
            {
                SearchService searchService = indexAndSearcher.getSearcher(storeRef, options.isIncludeInTransactionData());
                if (searchService instanceof LuceneSearcher)
                {
                    LuceneSearcher luceneSearcher = (LuceneSearcher) searchService;
                    ClosingIndexSearcher searcher = luceneSearcher.getClosingIndexSearcher();
                    LuceneQueryBuilderContext luceneContext = new LuceneQueryBuilderContext(dictionaryService, namespaceService, tenantService, searchParameters, indexAndSearcher,
                            searcher.getIndexReader());

                    LuceneQueryBuilder builder = (LuceneQueryBuilder) query;
                    org.apache.lucene.search.Query luceneQuery = builder.buildQuery(selectorGroup, luceneContext, functionContext);
                    // System.out.println(luceneQuery);

                    Sort sort = builder.buildSort(selectorGroup, luceneContext, functionContext);

                    Hits hits;

                    if (sort == null)
                    {
                        hits = searcher.search(luceneQuery);
                    }
                    else
                    {
                        hits = searcher.search(luceneQuery, sort);
                    }

                    LuceneResultSet result = new LuceneResultSet(hits, searcher, nodeService, tenantService, null, searchParameters, indexAndSearcher);
                    Map<Set<String>, ResultSet> map = new HashMap<Set<String>, ResultSet>(1);
                    map.put(selectorGroup, result);
                    return new QueryEngineResults(map);
                }
                else
                {
                    throw new UnsupportedOperationException();
                }
            }
            else
            {
                throw new UnsupportedOperationException();
            }
        }
        catch (ParseException e)
        {
            throw new SearcherException("Failed to parse query: " + e);
        }
        catch (IOException e)
        {
            throw new SearcherException("IO exception during search", e);
        }
    }

}
