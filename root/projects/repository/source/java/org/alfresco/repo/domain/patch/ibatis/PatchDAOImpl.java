/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.repo.domain.patch.ibatis;

import java.util.List;

import org.alfresco.repo.domain.avm.AVMNodeEntity;
import org.alfresco.repo.domain.patch.AbstractPatchDAOImpl;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

/**
 * iBatis-specific implementation of the AVMPatch DAO.
 * 
 * @author janv
 * @since 3.2
 */
public class PatchDAOImpl extends AbstractPatchDAOImpl
{
    private static final String SELECT_AVM_NODE_ENTITIES_COUNT_WHERE_NEW_IN_STORE = "select.AVMNodeEntitiesCountWhereNewInStore";
    private static final String SELECT_AVM_NODE_ENTITIES_WITH_EMPTY_GUID = "select.AVMNodesWithEmptyGUID";
    private static final String SELECT_AVM_LD_NODE_ENTITIES_NULL_VERSION = "select.AVMNodes.nullVersionLayeredDirectories";
    private static final String SELECT_AVM_LF_NODE_ENTITIES_NULL_VERSION = "select.AVMNodes.nullVersionLayeredFiles";
    
    private SqlMapClientTemplate avmTemplate;
    
    public void setAvmSqlMapClientTemplate(SqlMapClientTemplate avmSqlMapClientTemplate)
    {
        this.avmTemplate = avmSqlMapClientTemplate;
    }
    
    @Override
    protected Long getAVMNodeEntitiesCountWhereNewInStore()
    {
        return (Long) avmTemplate.queryForObject(SELECT_AVM_NODE_ENTITIES_COUNT_WHERE_NEW_IN_STORE);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMNodeEntity> getAVMNodeEntitiesWithEmptyGUID()
    {
        return (List<AVMNodeEntity>) avmTemplate.queryForList(SELECT_AVM_NODE_ENTITIES_WITH_EMPTY_GUID);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMNodeEntity> getNullVersionLayeredDirectoryNodeEntities()
    {
        return (List<AVMNodeEntity>) avmTemplate.queryForList(SELECT_AVM_LD_NODE_ENTITIES_NULL_VERSION);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMNodeEntity> getNullVersionLayeredFileNodeEntities()
    {
        return (List<AVMNodeEntity>) avmTemplate.queryForList(SELECT_AVM_LF_NODE_ENTITIES_NULL_VERSION);
    }
}
