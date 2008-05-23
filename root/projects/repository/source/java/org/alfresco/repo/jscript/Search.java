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
package org.alfresco.repo.jscript;

import java.io.StringReader;
import java.util.LinkedHashSet;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Search component for use by the ScriptService.
 * <p>
 * Provides access to Lucene search facilities including saved search objects. The results
 * from a search are returned as an array (collection) of scriptable Node wrapper objects.
 * <p>
 * The object is added to the root of the model to provide syntax such as:
 * <code>var results = search.luceneSearch(statement);</code>
 * and
 * <code>var results = search.savedSearch(node);</code>
 * 
 * @author Kevin Roast
 */
public final class Search extends BaseScopableProcessorExtension
{
    /** Service registry */
    private ServiceRegistry services;

    /** Default store reference */
    private StoreRef storeRef;
    
    /** Repository helper */
    private Repository repository;

    /**
     * Set the default store reference
     * 
     * @param   storeRef the default store reference
     */
    public void setStoreUrl(String storeRef)
    {
        this.storeRef = new StoreRef(storeRef);
    }

    /**
     * Set the service registry
     * 
     * @param services  the service registry
     */
    public void setServiceRegistry(ServiceRegistry services)
    {
        this.services = services;
    }
    
    /**
     * Set the repository helper
     * 
     * @param repository    the repository helper
     */
    public void setRepositoryHelper(Repository repository)
    {
        this.repository = repository;
    }

    /**
     * Find a single Node by the Node reference
     * 
     * @param ref       The NodeRef of the Node to find
     * 
     * @return the Node if found or null if failed to find
     */
    public ScriptNode findNode(NodeRef ref)
    {
        return findNode(ref.toString());
    }

    /**
     * Find a single Node by the Node reference
     *  
     * @param ref       The fully qualified NodeRef in String format
     *  
     * @return the Node if found or null if failed to find
     */
    public ScriptNode findNode(String ref)
    {
        String query = "ID:" + LuceneQueryParser.escape(ref);
        Object[] result = query(query, SearchService.LANGUAGE_LUCENE);
        if (result.length != 0)
        {
            return (ScriptNode)result[0];
        }
        else
        {
            return null;
        }
    }

    /**
     * Helper to convert a Web Script Request URL to a Node Ref
     * 
     * 1) Node - {store_type}/{store_id}/{node_id} 
     *
     *    Resolve to node via its Node Reference.
     *     
     * 2) Path - {store_type}/{store_id}/{path}
     * 
     *    Resolve to node via its display path.
     *  
     * 3) AVM Path - {store_id}/{path}
     * 
     *    Resolve to AVM node via its display path
     *    
     * 4) QName - {store_type}/{store_id}/{child_qname_path}
     * 
     *    Resolve to node via its child qname path.
     * 
     * @param  referenceType    one of node, path, avmpath or qname
     * @param  reference        array of reference segments (as described above for each reference type)
     * @return ScriptNode       the script node
     */
    public ScriptNode findNode(String referenceType, String[] reference)
    {
        ScriptNode result = null;
        NodeRef nodeRef = this.repository.findNodeRef(referenceType, reference);
        if (nodeRef != null)
        {
            result = new ScriptNode(nodeRef, this.services, getScope());
        }
        return result;
    }
    
    /**
     * Execute a XPath search
     * 
     * @param search        XPath search string to execute
     * 
     * @return JavaScript array of Node results from the search - can be empty but not null
     */
    public Scriptable xpathSearch(String search)
    {
        if (search != null && search.length() != 0)
        {
            Object[] results = query(search, SearchService.LANGUAGE_XPATH);
            return Context.getCurrentContext().newArray(getScope(), results);
        }
        else
        {
            return Context.getCurrentContext().newArray(getScope(), 0);
        }
    }

    /**
     * Execute a Lucene search
     * 
     * @param search        Lucene search string to execute
     * 
     * @return JavaScript array of Node results from the search - can be empty but not null
     */
    public Scriptable luceneSearch(String search)
    {
        if (search != null && search.length() != 0)
        {
            Object[] results = query(search, SearchService.LANGUAGE_LUCENE);
            return Context.getCurrentContext().newArray(getScope(), results);
        }
        else
        {
            return Context.getCurrentContext().newArray(getScope(), 0);
        }
    }

    /**
     * Execute a Lucene search (sorted)
     * 
     * @param search  Lucene search string to execute
     * @param sortKey  property name to sort on
     * @param asc  true => ascending sort
     * 
     * @return JavaScript array of Node results from the search - can be empty but not null
     */
    public Scriptable luceneSearch(String search, String sortColumn, boolean asc)
    {
        if (search == null || search.length() == 0)
        {
            return Context.getCurrentContext().newArray(getScope(), 0);
        }
        if (sortColumn == null || sortColumn.length() == 0)
        {
            return luceneSearch(search);
        }
        
        SortColumn[] sort = new SortColumn[1];
        sort[0] = new SortColumn(sortColumn, asc);
        Object[] results = query(search, sort, SearchService.LANGUAGE_LUCENE);
        return Context.getCurrentContext().newArray(getScope(), results);
    }
    
    /**
     * Execute a saved Lucene search
     * 
     * @param savedSearch   Node that contains the saved search XML content
     * 
     * @return JavaScript array of Node results from the search - can be empty but not null
     */
    public Scriptable savedSearch(ScriptNode savedSearch)
    {
        String search = null;

        // read the Saved Search XML on the specified node - and get the Lucene search from it
        try
        {
            if (savedSearch != null)
            {
                ContentReader content = this.services.getContentService().getReader(
                        savedSearch.getNodeRef(), ContentModel.PROP_CONTENT);
                if (content != null && content.exists())
                {
                    // get the root element
                    SAXReader reader = new SAXReader();
                    Document document = reader.read(new StringReader(content.getContentString()));
                    Element rootElement = document.getRootElement();

                    Element queryElement = rootElement.element("query");
                    if (queryElement != null)
                    {
                        search = queryElement.getText();
                    }
                }
            }
        }
        catch (Throwable err)
        {
            throw new AlfrescoRuntimeException("Failed to find or load saved Search: " + savedSearch.getNodeRef(), err);
        }
        
        if (search != null)
        {
            Object[] results = query(search, SearchService.LANGUAGE_LUCENE);
            return Context.getCurrentContext().newArray(getScope(), results);
        }
        else
        {
            return Context.getCurrentContext().newArray(getScope(), 0);
        }
    }

    /**
     * Execute a saved Lucene search
     * 
     * @param searchRef    NodeRef string that points to the node containing saved search XML content
     * 
     * @return JavaScript array of Node results from the search - can be empty but not null
     */
    public Scriptable savedSearch(String searchRef)
    {
        if (searchRef != null)
        {
            return savedSearch(new ScriptNode(new NodeRef(searchRef), services, null));
        }
        else
        {
            return Context.getCurrentContext().newArray(getScope(), 0);
        }
    }

    /**
     * Execute the query
     * 
     * Removes any duplicates that may be present (ID search can cause duplicates - it is better to remove them here)
     * 
     * @param search    Lucene search to execute
     * @param language  Search language to use e.g. SearchService.LANGUAGE_LUCENE
     * 
     * @return Array of Node objects
     */
    private Object[] query(String search, String language)
    {   
        LinkedHashSet<ScriptNode> set = new LinkedHashSet<ScriptNode>();

        // perform the search against the repo
        ResultSet results = null;
        try
        {
            results = this.services.getSearchService().query(
                    this.storeRef,
                    language,
                    search);

            if (results.length() != 0)
            {
                for (ResultSetRow row: results)
                {
                    NodeRef nodeRef = row.getNodeRef();
                    set.add(new ScriptNode(nodeRef, this.services, getScope()));
                }
            }
        }
        catch (Throwable err)
        {
            throw new AlfrescoRuntimeException("Failed to execute search: " + search, err);
        }
        finally
        {
            if (results != null)
            {
                results.close();
            }
        }

        return set.toArray(new Object[(set.size())]);
    }

    /**
     * Execute the query
     * 
     * Removes any duplicates that may be present (ID search can cause duplicates - it is better to remove them here)
     * 
     * @param search    Lucene search to execute
     * @param sort      Columns to sort by
     * @param language  Search language to use e.g. SearchService.LANGUAGE_LUCENE
     * 
     * @return Array of Node objects
     */
    private Object[] query(String search, SortColumn[] sort, String language)
    {   
        LinkedHashSet<ScriptNode> set = new LinkedHashSet<ScriptNode>();

        // perform the search against the repo
        ResultSet results = null;
        try
        {
            SearchParameters sp = new SearchParameters();
            sp.addStore(this.storeRef);
            sp.setLanguage(language);
            sp.setQuery(search);
            if (sort != null)
            {
                for (SortColumn sd : sort)
                {
                    sp.addSort(sd.column, sd.asc);
                }
            }
            
            results = this.services.getSearchService().query(sp);

            if (results.length() != 0)
            {
                for (ResultSetRow row: results)
                {
                    NodeRef nodeRef = row.getNodeRef();
                    set.add(new ScriptNode(nodeRef, this.services, getScope()));
                }
            }
        }
        catch (Throwable err)
        {
            throw new AlfrescoRuntimeException("Failed to execute search: " + search, err);
        }
        finally
        {
            if (results != null)
            {
                results.close();
            }
        }

        return set.toArray(new Object[(set.size())]);
    }

    /**
     * Search sort column 
     */
    private class SortColumn
    {
        /**
         * Constructor
         * 
         * @param column  column to sort on
         * @param asc  sort direction
         */
        SortColumn(String column, boolean asc)
        {
            this.column = column;
            this.asc = asc;
        }
        
        public String column;
        public boolean asc;
    }
    
}
