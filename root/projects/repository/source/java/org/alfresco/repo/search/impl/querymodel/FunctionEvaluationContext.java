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
package org.alfresco.repo.search.impl.querymodel;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alfresco.repo.search.MLAnalysisMode;
import org.alfresco.repo.search.impl.lucene.LuceneFunction;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryBuilderContext;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;

/**
 * The function evaluation context for lucene query implementations.
 * 
 * This context is used at query time and also when navigating the results to get column values.
 * 
 * @author andyh
 */
public interface FunctionEvaluationContext
{
    /**
     * @return the matching nodes by selector (at navigation time)
     */
    public Map<String, NodeRef> getNodeRefs();

    /**
     * @return the scores by selector (at navigation time)
     */
    public Map<String, Float> getScores();

    /**
     * Get a property
     * @param nodeRef
     * @param propertyName
     * @return the property (at navigation time)
     */
    public Serializable getProperty(NodeRef nodeRef, String propertyName);

    /**
     * @return the node service
     */
    public NodeService getNodeService();

    /**
     * @return the score (at navigation time)
     */
    public Float getScore();

    /**
     * @param lqp
     * @param propertyName
     * @param value
     * @param mode
     * @param luceneFunction
     * @return the query
     * @throws ParseException
     */
    public Query buildLuceneEquality(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode, LuceneFunction luceneFunction) throws ParseException;

    /**
     * Note: null and not null are not required to support functions from the spec
     * @param lqp
     * @param propertyName
     * @param not
     * @return the query
     * @throws ParseException
     */
    public Query buildLuceneExists(LuceneQueryParser lqp, String propertyName, Boolean not) throws ParseException;

    /**
     * @param lqp
     * @param propertyName
     * @param value
     * @param mode
     * @param luceneFunction
     * @return the query
     * @throws ParseException
     */
    public Query buildLuceneGreaterThan(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode, LuceneFunction luceneFunction) throws ParseException;

    /**
     * @param lqp
     * @param propertyName
     * @param value
     * @param mode
     * @param luceneFunction
     * @return the query
     * @throws ParseException
     */
    public Query buildLuceneGreaterThanOrEquals(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode, LuceneFunction luceneFunction) throws ParseException;

    /**
     * @param lqp
     * @param propertyName
     * @param value
     * @param mode
     * @param luceneFunction
     * @return the query
     * @throws ParseException
     */
    public Query buildLuceneLessThan(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode, LuceneFunction luceneFunction) throws ParseException;

    /**
     * @param lqp
     * @param propertyName
     * @param value
     * @param mode
     * @param luceneFunction
     * @return the query
     * @throws ParseException
     */
    public Query buildLuceneLessThanOrEquals(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode, LuceneFunction luceneFunction) throws ParseException;

    /**
     * Note: Like is not required to support functions from the spec
     * @param lqp
     * @param propertyName
     * @param value
     * @param not
     * @return the query
     * @throws ParseException
     */
    public Query buildLuceneLike(LuceneQueryParser lqp, String propertyName, Serializable value, Boolean not) throws ParseException;

    /**
     * @param lqp
     * @param propertyName
     * @param value
     * @param mode
     * @param luceneFunction
     * @return the query
     * @throws ParseException
     */
    public Query buildLuceneInequality(LuceneQueryParser lqp, String propertyName, Serializable value, PredicateMode mode, LuceneFunction luceneFunction) throws ParseException;

    /**
     * Note: In is not required to support functions from the spec
     * @param lqp
     * @param propertyName
     * @param values
     * @param not
     * @param mode
     * @return the query
     * @throws ParseException
     */
    public Query buildLuceneIn(LuceneQueryParser lqp, String propertyName, Collection<Serializable> values, Boolean not, PredicateMode mode) throws ParseException;

    /**
     * @param propertyName
     * @param luceneContext 
     * @param locales 
     * @param analysisMode 
     * @param reader 
     * @return the field used for sorting the given property
     */
    public String getLuceneSortField(LuceneQueryParser lqp, String propertyName);
    
    /**
     * @param propertyName
     * @return - is this an object id
     */
    public boolean isObjectId(String propertyName);

    /**
     * @param propertyName
     * @return is this property queryable
     */
    public boolean isQueryable(String propertyName);

    /**
     * @param propertyName
     * @return Is this property orderable
     */
    public boolean isOrderable(String propertyName);
    
    /**
     * @param propertyName
     * @return the lucene field name for the property
     */
    public String getLuceneFieldName(String propertyName);
    
    /**
     * @param functionArgument
     * @return the lucene function appropriate to a function argument
     */
    public LuceneFunction getLuceneFunction(FunctionArgument functionArgument);
    

}
