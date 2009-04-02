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
package org.alfresco.cmis.search;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.alfresco.cmis.CMISDictionaryService;
import org.alfresco.cmis.CMISPropertyDefinition;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext;
import org.alfresco.repo.search.impl.querymodel.PredicateMode;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;

/**
 * @author andyh
 */
public class CmisFunctionEvaluationContext implements FunctionEvaluationContext
{
    private Map<String, NodeRef> nodeRefs;

    private Map<String, Float> scores;

    private NodeService nodeService;

    private CMISDictionaryService cmisDictionaryService;

    private Float score;

    /**
     * @param nodeRefs
     *            the nodeRefs to set
     */
    public void setNodeRefs(Map<String, NodeRef> nodeRefs)
    {
        this.nodeRefs = nodeRefs;
    }

    /**
     * @param scores
     *            the scores to set
     */
    public void setScores(Map<String, Float> scores)
    {
        this.scores = scores;
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
     * @param cmisDictionaryService
     *            the cmisDictionaryService to set
     */
    public void setCmisDictionaryService(CMISDictionaryService cmisDictionaryService)
    {
        this.cmisDictionaryService = cmisDictionaryService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#getNodeRefs()
     */
    public Map<String, NodeRef> getNodeRefs()
    {
        return nodeRefs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#getNodeService()
     */
    public NodeService getNodeService()
    {
        return nodeService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#getProperty(org.alfresco.service.cmr.repository.NodeRef,
     *      org.alfresco.service.namespace.QName)
     */
    public Serializable getProperty(NodeRef nodeRef, QName propertyQName)
    {
        CMISPropertyDefinition propertyDef = cmisDictionaryService.findProperty(propertyQName, null);
        return propertyDef.getPropertyAccessor().getValue(nodeRef);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#getScores()
     */
    public Map<String, Float> getScores()
    {
        return scores;
    }

    /**
     * @return the score
     */
    public Float getScore()
    {
        return score;
    }

    /**
     * @param score
     *            the score to set
     */
    public void setScore(Float score)
    {
        this.score = score;
    }

    public Query buildLuceneEquality(LuceneQueryParser lqp, QName propertyQName, Serializable value, PredicateMode mode) throws ParseException
    {
        CMISPropertyDefinition propertyDef = cmisDictionaryService.findProperty(propertyQName, null);
        return propertyDef.getPropertyLuceneBuilder().buildLuceneEquality(lqp, value, mode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#buildLuceneExists(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      org.alfresco.service.namespace.QName, java.lang.Boolean)
     */
    public Query buildLuceneExists(LuceneQueryParser lqp, QName propertyQName, Boolean not) throws ParseException
    {
        CMISPropertyDefinition propertyDef = cmisDictionaryService.findProperty(propertyQName, null);
        return propertyDef.getPropertyLuceneBuilder().buildLuceneExists(lqp, not);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#buildLuceneGreaterThan(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      org.alfresco.service.namespace.QName, java.io.Serializable,
     *      org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneGreaterThan(LuceneQueryParser lqp, QName propertyQName, Serializable value, PredicateMode mode) throws ParseException
    {
        CMISPropertyDefinition propertyDef = cmisDictionaryService.findProperty(propertyQName, null);
        return propertyDef.getPropertyLuceneBuilder().buildLuceneGreaterThan(lqp, value, mode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#buildLuceneGreaterThanOrEquals(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      org.alfresco.service.namespace.QName, java.io.Serializable,
     *      org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneGreaterThanOrEquals(LuceneQueryParser lqp, QName propertyQName, Serializable value, PredicateMode mode) throws ParseException
    {
        CMISPropertyDefinition propertyDef = cmisDictionaryService.findProperty(propertyQName, null);
        return propertyDef.getPropertyLuceneBuilder().buildLuceneGreaterThanOrEquals(lqp, value, mode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#buildLuceneIn(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      org.alfresco.service.namespace.QName, java.util.Collection, java.lang.Boolean,
     *      org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneIn(LuceneQueryParser lqp, QName propertyQName, Collection<Serializable> values, Boolean not, PredicateMode mode) throws ParseException
    {
        CMISPropertyDefinition propertyDef = cmisDictionaryService.findProperty(propertyQName, null);
        return propertyDef.getPropertyLuceneBuilder().buildLuceneIn(lqp, values, not, mode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#buildLuceneInequality(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      org.alfresco.service.namespace.QName, java.io.Serializable,
     *      org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneInequality(LuceneQueryParser lqp, QName propertyQName, Serializable value, PredicateMode mode) throws ParseException
    {
        CMISPropertyDefinition propertyDef = cmisDictionaryService.findProperty(propertyQName, null);
        return propertyDef.getPropertyLuceneBuilder().buildLuceneInequality(lqp, value, mode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#buildLuceneLessThan(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      org.alfresco.service.namespace.QName, java.io.Serializable,
     *      org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneLessThan(LuceneQueryParser lqp, QName propertyQName, Serializable value, PredicateMode mode) throws ParseException
    {
        CMISPropertyDefinition propertyDef = cmisDictionaryService.findProperty(propertyQName, null);
        return propertyDef.getPropertyLuceneBuilder().buildLuceneLessThan(lqp, value, mode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#buildLuceneLessThanOrEquals(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      org.alfresco.service.namespace.QName, java.io.Serializable,
     *      org.alfresco.repo.search.impl.querymodel.PredicateMode)
     */
    public Query buildLuceneLessThanOrEquals(LuceneQueryParser lqp, QName propertyQName, Serializable value, PredicateMode mode) throws ParseException
    {
        CMISPropertyDefinition propertyDef = cmisDictionaryService.findProperty(propertyQName, null);
        return propertyDef.getPropertyLuceneBuilder().buildLuceneLessThanOrEquals(lqp, value, mode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#buildLuceneLike(org.alfresco.repo.search.impl.lucene.LuceneQueryParser,
     *      org.alfresco.service.namespace.QName, java.io.Serializable, java.lang.Boolean)
     */
    public Query buildLuceneLike(LuceneQueryParser lqp, QName propertyQName, Serializable value, Boolean not) throws ParseException
    {
        CMISPropertyDefinition propertyDef = cmisDictionaryService.findProperty(propertyQName, null);
        return propertyDef.getPropertyLuceneBuilder().buildLuceneLike(lqp, value, not);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext#getLuceneSortField(org.alfresco.service.namespace.QName)
     */
    public String getLuceneSortField(QName propertyQName)
    {
        CMISPropertyDefinition propertyDef = cmisDictionaryService.findProperty(propertyQName, null);
        return propertyDef.getPropertyLuceneBuilder().getLuceneSortField();
    }

}
