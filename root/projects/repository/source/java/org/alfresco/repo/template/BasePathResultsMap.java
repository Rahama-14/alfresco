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
package org.alfresco.repo.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.QueryParameterDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A special Map that executes an XPath against the parent Node as part of the get()
 * Map interface implementation.
 * 
 * @author Kevin Roast
 */
public abstract class BasePathResultsMap extends BaseTemplateMap
{
    protected static Log logger = LogFactory.getLog(BasePathResultsMap.class);
    
    /**
     * Constructor
     * 
     * @param parent         The parent TemplateNode to execute searches from 
     * @param services       The ServiceRegistry to use
     */
    public BasePathResultsMap(TemplateNode parent, ServiceRegistry services)
    {
        super(parent, services);
    }
    
    /**
     * Return a list or a single Node from executing an xpath against the parent Node.
     * 
     * @param xpath        XPath to execute
     * @param firstOnly    True to return the first result only
     * 
     * @return List<TemplateNode>
     */
    protected List<TemplateNode> getChildrenByXPath(String xpath, QueryParameterDefinition[] params, boolean firstOnly)
    {
        List<TemplateNode> result = null;
        
        if (xpath.length() != 0)
        {
            if (logger.isDebugEnabled())
            {
                String out = "Executing xpath: " + xpath;
                if (params != null)
                {
                   out += " with params:";
                   for (QueryParameterDefinition p : params)
                   {
                      out += " " + p.getDefault();
                   }
                }
                logger.debug(out);
            }
            
            List<NodeRef> nodes = this.services.getSearchService().selectNodes(
                    this.parent.getNodeRef(),
                    xpath,
                    params,
                    this.services.getNamespaceService(),
                    false);
            
            // see if we only want the first result
            if (firstOnly == true)
            {
                if (nodes.size() != 0)
                {
                    result = new ArrayList<TemplateNode>(1);
                    result.add(new TemplateNode(nodes.get(0), this.services, this.parent.getImageResolver()));
                }
            }
            // or all the results
            else
            {
                result = new ArrayList<TemplateNode>(nodes.size()); 
                for (NodeRef ref : nodes)
                {
                    result.add(new TemplateNode(ref, this.services, this.parent.getImageResolver()));
                }
            }
        }
        
        return result != null ? result : (List)Collections.emptyList();
    }
}
