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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.search.impl.querymodel.Argument;
import org.alfresco.repo.search.impl.querymodel.Constraint;
import org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext;
import org.alfresco.repo.search.impl.querymodel.impl.BaseConjunction;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * @author andyh
 */
public class LuceneConjunction extends BaseConjunction implements LuceneQueryBuilderComponent
{

    /**
     * @param constraints
     */
    public LuceneConjunction(List<Constraint> constraints)
    {
        super(constraints);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryBuilderComponent#addComponent(java.lang.String,
     *      java.util.Map, org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryBuilderContext,
     *      org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext)
     */
    public Query addComponent(Set<String> selectors, Map<String, Argument> functionArgs, LuceneQueryBuilderContext luceneContext, FunctionEvaluationContext functionContext)
            throws ParseException
    {
        BooleanQuery query = new BooleanQuery();
        for (Constraint constraint : getConstraints())
        {
            if (constraint instanceof LuceneQueryBuilderComponent)
            {
                LuceneQueryBuilderComponent luceneQueryBuilderComponent = (LuceneQueryBuilderComponent) constraint;
                Query constraintQuery = luceneQueryBuilderComponent.addComponent(selectors, functionArgs, luceneContext, functionContext);
                boolean must = false;
                boolean should = false;
                boolean must_not = false;
                if (constraintQuery != null)
                {
                    switch (constraint.getOccur())
                    {
                    case DEFAULT:
                    case MANDATORY:
                        query.add(constraintQuery, BooleanClause.Occur.MUST);
                        must = true;
                        break;
                    case OPTIONAL:
                        query.add(constraintQuery, BooleanClause.Occur.SHOULD);
                        should = true;
                        break;
                    case EXCLUDE:
                        query.add(constraintQuery, BooleanClause.Occur.MUST_NOT);
                        must_not = true;
                        break;
                    }
                    if(!must && must_not)
                    {
                        query.add(new TermQuery(new Term("ISNODE", "T")),  BooleanClause.Occur.MUST);
                    }
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
        return query;

    }

}
