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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.cmis.CMISDictionaryService;
import org.alfresco.cmis.CMISJoinEnum;
import org.alfresco.cmis.CMISQueryEnum;
import org.alfresco.cmis.CMISQueryOptions;
import org.alfresco.cmis.CMISQueryService;
import org.alfresco.cmis.CMISResultSet;
import org.alfresco.cmis.CMISScope;
import org.alfresco.cmis.CMISServices;
import org.alfresco.cmis.CMISQueryOptions.CMISQueryMode;
import org.alfresco.repo.search.impl.querymodel.Query;
import org.alfresco.repo.search.impl.querymodel.QueryEngine;
import org.alfresco.repo.search.impl.querymodel.QueryEngineResults;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;

/**
 * @author andyh
 */
public class CMISQueryServiceImpl implements CMISQueryService
{
    private CMISServices cmisService;

    private CMISDictionaryService cmisDictionaryService;

    private QueryEngine queryEngine;

    private NodeService nodeService;
    
    private DictionaryService alfrescoDictionaryService;

    /**
     * @param service
     *            the service to set
     */
    public void setCMISService(CMISServices cmisService)
    {
        this.cmisService = cmisService;
    }

    /**
     * @param cmisDictionaryService
     *            the cmisDictionaryService to set
     */
    public void setCMISDictionaryService(CMISDictionaryService cmisDictionaryService)
    {
        this.cmisDictionaryService = cmisDictionaryService;
    }

    /**
     * @param queryEngine
     *            the queryEngine to set
     */
    public void setQueryEngine(QueryEngine queryEngine)
    {
        this.queryEngine = queryEngine;
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
     * @param alfrescoDictionaryService
     *            the Alfresco Dictionary Service  to set
     */
    public void setAlfrescoDictionaryService(DictionaryService alfrescoDictionaryService)
    {
        this.alfrescoDictionaryService = alfrescoDictionaryService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISQueryService#query(org.alfresco.cmis.search.CMISQueryOptions)
     */
    public CMISResultSet query(CMISQueryOptions options)
    {
        CMISJoinEnum joinSupport = getJoinSupport();
        if(options.getQueryMode() == CMISQueryOptions.CMISQueryMode.CMS_WITH_ALFRESCO_EXTENSIONS)
        {
            joinSupport = CMISJoinEnum.INNER_JOIN_SUPPORT;
        }
        
        // TODO: Refactor to avoid duplication of valid scopes here and in CMISQueryParser
        
        CMISScope[] validScopes = (options.getQueryMode() == CMISQueryMode.CMS_STRICT) ? CmisFunctionEvaluationContext.STRICT_SCOPES : CmisFunctionEvaluationContext.ALFRESCO_SCOPES;
        CmisFunctionEvaluationContext functionContext = new CmisFunctionEvaluationContext();
        functionContext.setCmisDictionaryService(cmisDictionaryService);
        functionContext.setNodeService(nodeService);
        functionContext.setValidScopes(validScopes);
        
        CMISQueryParser parser = new CMISQueryParser(options, cmisDictionaryService, joinSupport);
        Query query = parser.parse(queryEngine.getQueryModelFactory(), functionContext);

        QueryEngineResults results = queryEngine.executeQuery(query, options, functionContext);
        Map<String, ResultSet> wrapped = new HashMap<String, ResultSet>();
        Map<Set<String>, ResultSet> map = results.getResults();
        for (Set<String> group : map.keySet())
        {
            ResultSet current = map.get(group);
            for (String selector : group)
            {
                wrapped.put(selector, current);
            }
        }
        CMISResultSet cmis = new CMISResultSetImpl(wrapped, options, nodeService, query, cmisDictionaryService, alfrescoDictionaryService);
        return cmis;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISQueryService#query(java.lang.String)
     */
    public CMISResultSet query(String query)
    {
        CMISQueryOptions options = new CMISQueryOptions(query, cmisService.getDefaultRootStoreRef());
        return query(options);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISQueryService#getPwcSearchable()
     */
    public boolean getPwcSearchable()
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISQueryService#getAllVersionsSearchable()
     */
    public boolean getAllVersionsSearchable()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISQueryService#getQuerySupport()
     */
    public CMISQueryEnum getQuerySupport()
    {
        return CMISQueryEnum.BOTH_COMBINED;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.cmis.search.CMISQueryService#getJoinSupport()
     */
    public CMISJoinEnum getJoinSupport()
    {
        return CMISJoinEnum.NO_JOIN_SUPPORT;
    }
}
