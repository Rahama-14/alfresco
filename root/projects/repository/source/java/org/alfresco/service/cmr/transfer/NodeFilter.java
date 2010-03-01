/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
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

package org.alfresco.service.cmr.transfer;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author brian
 *
 * Examines the supplied node and indicates whether it has been accepted by the filter.
 * <p>
 * The NodeCrawler will first initialise this filter by calling the 
 * setServiceRegistry and init methods. Then the accept method will be called to either accept or 
 * reject the node.
 */
public interface NodeFilter
{

    /**
     * Examines the supplied node and indicates whether it has been accepted by the filter.
     * @param thisNode
     * @param serviceRegistry 
     * @return true if the supplied node matches the criteria specified on this filter, and false
     * otherwise.
     */
    boolean accept(NodeRef thisNode);
    
    /**
     * 
     */
    void init();
    
    /**
     * 
     * @param serviceRegistry
     */
    void setServiceRegistry(ServiceRegistry serviceRegistry);
}
