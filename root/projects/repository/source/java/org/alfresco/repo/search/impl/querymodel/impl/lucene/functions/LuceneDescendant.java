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
package org.alfresco.repo.search.impl.querymodel.impl.lucene.functions;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.search.impl.lucene.ParseException;
import org.alfresco.repo.search.impl.querymodel.Argument;
import org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext;
import org.alfresco.repo.search.impl.querymodel.QueryModelException;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Descendant;
import org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryBuilderComponent;
import org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryBuilderContext;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.apache.lucene.search.Query;

/**
 * @author andyh
 *
 */
public class LuceneDescendant extends Descendant implements LuceneQueryBuilderComponent
{

    /**
     * 
     */
    public LuceneDescendant()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.impl.lucene.LuceneQueryBuilderComponent#addComponent(org.apache.lucene.search.BooleanQuery,
     *      org.apache.lucene.search.BooleanQuery, org.alfresco.service.cmr.dictionary.DictionaryService,
     *      java.lang.String)
     */
    public Query addComponent(String selector, Map<String, Argument> functionArgs, LuceneQueryBuilderContext luceneContext, FunctionEvaluationContext functionContext)
            throws ParseException
    {
        LuceneQueryParser lqp = luceneContext.getLuceneQueryParser();
        Argument argument = functionArgs.get(ARG_ANCESTOR);
        String id = (String) argument.getValue(functionContext);
        NodeRef nodeRef;
        if(NodeRef.isNodeRef(id))
        {
            nodeRef = new NodeRef(id);
        }
        else
        {
            int lastIndex = id.lastIndexOf('/');
            String versionLabel = id.substring(lastIndex+1);
            String actualId = id.substring(0, lastIndex);
            if(NodeRef.isNodeRef(actualId))
            {
                nodeRef = new NodeRef(actualId);
                Serializable value = functionContext.getNodeService().getProperty(nodeRef, ContentModel.PROP_VERSION_LABEL);
                if (value != null)
                {
                    String actualVersionLabel = DefaultTypeConverter.INSTANCE.convert(String.class, value);
                    if(!actualVersionLabel.equals(versionLabel))
                    {
                        throw new QueryModelException("Object id does not refer to the current version"+id);
                    }
                }
            }
            else
            {
                throw new QueryModelException("Invalid Object Id "+id);
            }
        }
        Path path = functionContext.getNodeService().getPath(nodeRef);
        StringBuilder builder = new StringBuilder(path.toPrefixString(luceneContext.getNamespacePrefixResolver()));
        builder.append("//*");
        Query query = lqp.getFieldQuery("PATH", builder.toString());
        return query;
        
    }
    
}
