/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.search;

import java.util.Collection;
import java.util.Map;

import org.alfresco.service.cmr.search.QueryParameterDefinition;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;

/**
 * The definition of a canned query
 * 
 * @author andyh
 * 
 */
public interface CannedQueryDef
{
    /**
     * Get the unique name for the query
     * 
     * @return
     */
    public QName getQname();

    /**
     * Get the language in which the query is defined.
     * 
     * @return
     */
    public String getLanguage();

    /**
     * Get the definitions for any query parameters.
     * 
     * @return
     */
    public Collection<QueryParameterDefinition> getQueryParameterDefs();

    /**
     * Get the query string.
     * 
     * @return
     */
    public String getQuery();

    /**
     * Return the mechanism that this query definition uses to map namespace
     * prefixes to URIs. A query may use a predefined set of prefixes for known
     * URIs. I would be unwise to rely on the defaults.
     * 
     * @return
     */
    public NamespacePrefixResolver getNamespacePrefixResolver();

    /**
     * Get a map to look up definitions by Qname
     * 
     * @return
     */
    public Map<QName, QueryParameterDefinition> getQueryParameterMap();
}
