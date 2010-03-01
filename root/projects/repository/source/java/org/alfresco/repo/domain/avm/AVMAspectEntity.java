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
package org.alfresco.repo.domain.avm;


/**
 * Entity bean for <b>avm_aspects</b> table
 * 
 * @author janv
 * @since 3.2
 */

public class AVMAspectEntity
{
    private Long nodeId;
    private Long qnameId;
    
    public AVMAspectEntity()
    {
        // default constructor
    }
    
    public AVMAspectEntity(Long nodeId, Long qnameId)
    {
        this.nodeId = nodeId;
        this.qnameId = qnameId;
    }
    
    public Long getNodeId()
    {
        return nodeId;
    }
    
    public void setNodeId(Long nodeId)
    {
        this.nodeId = nodeId;
    }
    
    public Long getQnameId()
    {
        return qnameId;
    }
    
    public void setQnameId(Long qnameId)
    {
        this.qnameId = qnameId;
    }
}
