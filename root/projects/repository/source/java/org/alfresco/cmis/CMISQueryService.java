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
package org.alfresco.cmis;


/**
 * Support to execute CMIS queries
 * 
 * @author andyh
 *
 */
public interface CMISQueryService
{
    /**
     * Execute a CMIS query as defined by options 
     * 
     * @param options
     * @return a result set
     */
    public CMISResultSet query(CMISQueryOptions options);
    
    /**
     * Execute a CMIS query with all the default options;
     * 
     * @param query
     * @return
     */
    public CMISResultSet query(String query);
    

    /**
     * Get the query support level
     */
    public CMISQueryEnum getQuerySupport();
    
    /**
     * Get the join support level in queries.
     */
    public CMISJoinEnum getJoinSupport();
    
    /**
     * Get the full text search support level in queries.
     */
    public CMISFullTextSearchEnum getFullTextSearchSupport();

    /**
     * Can you query Private Working Copies of a document.
     * 
     * @return
     */
    public boolean getPwcSearchable();
    
    /**
     * Can you query non-latest versions of a document. 
     * The current latest version is always searchable according to the type definition.
     * 
     * @return
     */
    public boolean getAllVersionsSearchable();
}
