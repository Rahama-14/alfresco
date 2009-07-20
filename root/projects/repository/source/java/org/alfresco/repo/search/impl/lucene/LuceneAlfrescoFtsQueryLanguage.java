package org.alfresco.repo.search.impl.lucene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.search.impl.parsers.AlfrescoFunctionEvaluationContext;
import org.alfresco.repo.search.impl.parsers.CMISParser;
import org.alfresco.repo.search.impl.parsers.FTSQueryParser;
import org.alfresco.repo.search.impl.querymodel.Argument;
import org.alfresco.repo.search.impl.querymodel.Column;
import org.alfresco.repo.search.impl.querymodel.Constraint;
import org.alfresco.repo.search.impl.querymodel.Function;
import org.alfresco.repo.search.impl.querymodel.Order;
import org.alfresco.repo.search.impl.querymodel.Ordering;
import org.alfresco.repo.search.impl.querymodel.QueryEngine;
import org.alfresco.repo.search.impl.querymodel.QueryEngineResults;
import org.alfresco.repo.search.impl.querymodel.QueryModelFactory;
import org.alfresco.repo.search.impl.querymodel.QueryOptions;
import org.alfresco.repo.search.impl.querymodel.QueryOptions.Connective;
import org.alfresco.repo.search.impl.querymodel.impl.functions.PropertyAccessor;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Score;
import org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneOrdering;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.search.SearchParameters.SortDefinition;
import org.alfresco.service.cmr.search.SearchParameters.SortDefinition.SortType;

/**
 * Alfresco FTS Query language support
 * 
 * @author andyh
 */
public class LuceneAlfrescoFtsQueryLanguage implements LuceneQueryLanguageSPI
{
    QueryEngine queryEngine;

    /**
     * Set the query engine
     * 
     * @param queryEngine
     */
    public void setQueryEngine(QueryEngine queryEngine)
    {
        this.queryEngine = queryEngine;
    }

    public ResultSet executQuery(SearchParameters searchParameters, ADMLuceneSearcherImpl admLuceneSearcher)
    {
        String ftsExpression = searchParameters.getQuery();
        QueryModelFactory factory = queryEngine.getQueryModelFactory();
        AlfrescoFunctionEvaluationContext context = new AlfrescoFunctionEvaluationContext(admLuceneSearcher.getNamespacePrefixResolver(), admLuceneSearcher.getDictionaryService(),
                searchParameters.getNamespace());

        QueryOptions options = new QueryOptions(searchParameters.getQuery(), null);
        options.setFetchSize(searchParameters.getBulkFecthSize());
        options.setIncludeInTransactionData(!searchParameters.excludeDataInTheCurrentTransaction());
        options.setDefaultFTSConnective(searchParameters.getDefaultOperator() == SearchParameters.Operator.OR ? Connective.OR : Connective.AND);
        options.setDefaultFTSFieldConnective(searchParameters.getDefaultOperator() == SearchParameters.Operator.OR ? Connective.OR : Connective.AND);
        options.setSkipCount(searchParameters.getSkipCount());
        options.setMaxPermissionChecks(searchParameters.getMaxPermissionChecks());
        options.setMaxPermissionCheckTimeMillis(searchParameters.getMaxPermissionCheckTimeMillis());
        if (searchParameters.getLimitBy() == LimitBy.FINAL_SIZE)
        {
            options.setMaxItems(searchParameters.getLimit());
        }
        else
        {
            options.setMaxItems(searchParameters.getMaxItems());
        }
        options.setMlAnalaysisMode(searchParameters.getMlAnalaysisMode());
        options.setLocales(searchParameters.getLocales());
        options.setStores(searchParameters.getStores());

        Constraint constraint = FTSQueryParser.buildFTS(ftsExpression, factory, context, null, null, options.getDefaultFTSConnective(), options.getDefaultFTSFieldConnective(),
                searchParameters.getQueryTemplates());
        org.alfresco.repo.search.impl.querymodel.Query query = factory.createQuery(null, null, constraint, buildOrderings(factory, searchParameters));

        QueryEngineResults results = queryEngine.executeQuery(query, options, context);
        return results.getResults().values().iterator().next();
    }

    public String getName()
    {
        return SearchService.LANGUAGE_FTS_ALFRESCO;
    }

    public void setFactories(List<AbstractLuceneIndexerAndSearcherFactory> factories)
    {
        for (AbstractLuceneIndexerAndSearcherFactory factory : factories)
        {
            factory.registerQueryLanguage(this);
        }
    }

    public List<Ordering> buildOrderings(QueryModelFactory factory, SearchParameters searchParameters)
    {
        List<Ordering> orderings = new ArrayList<Ordering>(searchParameters.getSortDefinitions().size());
        for (SortDefinition sd : searchParameters.getSortDefinitions())
        {
            if (sd.getSortType() == SortType.FIELD)
            {
                Function function = factory.getFunction(PropertyAccessor.NAME);
                Argument arg = factory.createPropertyArgument(PropertyAccessor.ARG_PROPERTY, true, true, "", sd.getField());
                Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                functionArguments.put(arg.getName(), arg);
                Column column = factory.createColumn(function, functionArguments, sd.getField());

                Order order = sd.isAscending() ? Order.ASCENDING : Order.DESCENDING;

                Ordering ordering = factory.createOrdering(column, order);
                
                orderings.add(ordering);
            }
            else  if (sd.getSortType() == SortType.SCORE)
            {
                Function function = factory.getFunction(Score.NAME);
                Map<String, Argument> functionArguments = new LinkedHashMap<String, Argument>();
                Column column = factory.createColumn(function, functionArguments, Score.NAME);
                Order order = sd.isAscending() ? Order.ASCENDING : Order.DESCENDING;

                Ordering ordering = factory.createOrdering(column, order);
                
                orderings.add(ordering);
            }
            else
            {
                throw new UnsupportedOperationException("Unsupported Ordering "+sd.getSortType());
            }
        }
        return orderings;
    }
}
