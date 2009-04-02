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
package org.alfresco.cmis.property;

import java.io.Serializable;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.search.impl.querymodel.PredicateMode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.SearchLanguageConversion;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Property accessor for fixed value mapping (eg to null, true, etc)
 * 
 * @author andyh
 */
public class FixedValuePropertyAccessor extends AbstractPropertyAccessor
{
    private Serializable value;
    
    /**
     * Construct
     * 
     * @param serviceRegistry
     * @param propertyName
     * @param value
     */
    public FixedValuePropertyAccessor(ServiceRegistry serviceRegistry, String propertyName, Serializable value)
    {
        super(serviceRegistry, propertyName);
        this.value = value;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyAccessor#getValue(org.alfresco.service.cmr.repository.NodeRef)
     */
    public Serializable getValue(NodeRef nodeRef)
    {
        return value;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyAccessor#setValue(org.alfresco.service.cmr.repository.NodeRef, java.io.Serializable)
     */
    public void setValue(NodeRef nodeRef, Serializable value)
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneEquality(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneEquality(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        if (EqualsHelper.nullSafeEquals(value, value))
        {
            return new MatchAllDocsQuery();
        }
        else
        {
            return new TermQuery(new Term("NO_TOKENS", "__"));
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneExists(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.lang.Boolean)
     */
    public Query buildLuceneExists(LuceneQueryParser lqp, Boolean not) throws ParseException
    {
        if (not)
        {
            if (value == null)
            {
                return new MatchAllDocsQuery();
            }
            else
            {
                return new TermQuery(new Term("NO_TOKENS", "__"));
            }
        }
        else
        {
            if (value == null)
            {
                return new TermQuery(new Term("NO_TOKENS", "__"));
            }
            else
            {
                return new MatchAllDocsQuery();
            }
        }

    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneGreaterThan(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    @SuppressWarnings("unchecked")
    public Query buildLuceneGreaterThan(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        if (value instanceof Comparable)
        {
            Comparable comparable = (Comparable) value;
            if (comparable.compareTo(value) > 0)
            {
                return new MatchAllDocsQuery();
            }
            else
            {
                return new TermQuery(new Term("NO_TOKENS", "__"));
            }
        }
        else
        {
            return new TermQuery(new Term("NO_TOKENS", "__"));
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneGreaterThanOrEquals(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    @SuppressWarnings("unchecked")
    public Query buildLuceneGreaterThanOrEquals(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        if (value instanceof Comparable)
        {
            Comparable comparable = (Comparable) value;
            if (comparable.compareTo(value) >= 0)
            {
                return new MatchAllDocsQuery();
            }
            else
            {
                return new TermQuery(new Term("NO_TOKENS", "__"));
            }
        }
        else
        {
            return new TermQuery(new Term("NO_TOKENS", "__"));
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneIn(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.util.Collection, java.lang.Boolean, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneIn(LuceneQueryParser lqp, Collection<Serializable> values, Boolean not, PredicateMode mode) throws ParseException
    {
        boolean in = false;
        for (Serializable value : values)
        {
            if (EqualsHelper.nullSafeEquals(value, value))
            {
                in = true;
                break;
            }
        }

        if (in == !not)
        {
            return new MatchAllDocsQuery();
        }
        else
        {
            return new TermQuery(new Term("NO_TOKENS", "__"));
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneInequality(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneInequality(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        if (!EqualsHelper.nullSafeEquals(value, value))
        {
            return new MatchAllDocsQuery();
        }
        else
        {
            return new TermQuery(new Term("NO_TOKENS", "__"));
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneLessThan(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    @SuppressWarnings("unchecked")
    public Query buildLuceneLessThan(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        if (value instanceof Comparable)
        {
            Comparable comparable = (Comparable) value;
            if (comparable.compareTo(value) < 0)
            {
                return new MatchAllDocsQuery();
            }
            else
            {
                return new TermQuery(new Term("NO_TOKENS", "__"));
            }
        }
        else
        {
            return new TermQuery(new Term("NO_TOKENS", "__"));
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneLessThanOrEquals(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    @SuppressWarnings("unchecked")
    public Query buildLuceneLessThanOrEquals(LuceneQueryParser lqp, Serializable value, PredicateMode mode) throws ParseException
    {
        if (value instanceof Comparable)
        {
            Comparable comparable = (Comparable) value;
            if (comparable.compareTo(value) <= 0)
            {
                return new MatchAllDocsQuery();
            }
            else
            {
                return new TermQuery(new Term("NO_TOKENS", "__"));
            }
        }
        else
        {
            return new TermQuery(new Term("NO_TOKENS", "__"));
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#buildLuceneLike(org.alfresco.repo.search.impl.lucene.LuceneQueryParser, java.io.Serializable, java.lang.Boolean)
     */
    public Query buildLuceneLike(LuceneQueryParser lqp, Serializable value, Boolean not) throws ParseException
    {
        if (value != null)
        {
            boolean matches = false;

            Object converted = DefaultTypeConverter.INSTANCE.convert(value.getClass(), value);
            String asString = DefaultTypeConverter.INSTANCE.convert(String.class, converted);
            String regExpression = SearchLanguageConversion.convertSQLLikeToRegex(asString);
            Pattern pattern = Pattern.compile(regExpression);
            String target = DefaultTypeConverter.INSTANCE.convert(String.class, value);
            Matcher matcher = pattern.matcher(target);
            if (matcher.matches())
            {
                matches = true;
            }

            if (matches == !not)
            {
                return new MatchAllDocsQuery();
            }
            else
            {
                return new TermQuery(new Term("NO_TOKENS", "__"));
            }
        }
        else
        {
            return new TermQuery(new Term("NO_TOKENS", "__"));
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyLuceneBuilder#getLuceneSortField()
     */
    public String getLuceneSortField()
    {
        throw new UnsupportedOperationException();
    }

}
