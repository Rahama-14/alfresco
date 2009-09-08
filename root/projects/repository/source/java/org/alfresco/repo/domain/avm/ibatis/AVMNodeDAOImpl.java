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
package org.alfresco.repo.domain.avm.ibatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.domain.avm.AVMAspectEntity;
import org.alfresco.repo.domain.avm.AVMNodeEntity;
import org.alfresco.repo.domain.avm.AVMNodePropertyEntity;
import org.alfresco.repo.domain.avm.AbstractAVMNodeDAOImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import com.ibatis.sqlmap.client.event.RowHandler;

/**
 * iBatis-specific implementation of the AVMNode DAO.
 * 
 * @author janv
 * @since 3.2
 */
public class AVMNodeDAOImpl extends AbstractAVMNodeDAOImpl
{
    private static Log logger = LogFactory.getLog(AVMNodeDAOImpl.class);
    
    private static final String INSERT_AVM_NODE = "alfresco.avm.insert_AVMNode";
    private static final String SELECT_AVM_NODE_BY_ID = "alfresco.avm.select_AVMNodeById";
    private static final String UPDATE_AVM_NODE = "alfresco.avm.update_AVMNode";
    private static final String UPDATE_AVM_NODE_MODTIME_AND_GUID = "alfresco.avm.update_AVMNode_modTimeAndGuid";
    private static final String UPDATE_AVM_NODE_MODTIME_AND_CDATA = "alfresco.avm.update_AVMNode_modTimeAndContentData";
    private static final String DELETE_AVM_NODE = "alfresco.avm.delete_AVMNode";
    
    private static final String SELECT_AVM_NODES_NEW_IN_STORE = "alfresco.avm.select_AVMNodes_newInStore";
    private static final String SELECT_AVM_NODES_NEW_LAYERED_IN_STORE = "alfresco.avm.select_AVMNodes_newLayeredInStore";
    private static final String SELECT_AVM_NODE_IDS_NEW_LAYERED_IN_STORE = "alfresco.avm.select_AVMNodes_IDs_newLayeredInStore";
    private static final String UPDATE_AVM_NODES_CLEAR_NEW_IN_STORE = "alfresco.avm.update_AVMNodes_clearNewInStore";
    private static final String SELECT_AVM_NODES_ORPHANS = "alfresco.avm.select_AVMNodes_orphans";
    private static final String SELECT_AVM_NODES_LAYERED_DIRECTORIES = "alfresco.avm.select_AVMNodes_layeredDirectories";
    private static final String SELECT_AVM_NODES_LAYERED_FILES = "alfresco.avm.select_AVMNodes_layeredFiles";
    private static final String SELECT_AVM_NODE_IDS_BY_ACL_ID = "alfresco.avm.select_AVMNodes_IDs_byAcl";
    
    private static final String SELECT_AVM_CONTENT_URLS_FOR_PLAIN_FILES = "alfresco.avm.select_ContentUrlsForPlainFiles";
    
    private static final String SELECT_AVM_NODE_ASPECTS = "alfresco.avm.select_AVMNodeAspects";
    private static final String INSERT_AVM_NODE_ASPECT = "alfresco.avm.insert_AVMNodeAspect";
    private static final String DELETE_AVM_NODE_ASPECT = "alfresco.avm.delete_AVMNodeAspect";
    private static final String DELETE_AVM_NODE_ASPECTS = "alfresco.avm.delete_AVMNodeAspects";
    
    private static final String INSERT_AVM_NODE_PROP = "alfresco.avm.insert_AVMNodeProperty";
    private static final String UPDATE_AVM_NODE_PROP = "alfresco.avm.update_AVMNodeProperty";
    private static final String SELECT_AVM_NODE_PROP = "alfresco.avm.select_AVMNodeProperty";
    private static final String SELECT_AVM_NODE_PROPS = "alfresco.avm.select_AVMNodeProperties";
    private static final String DELETE_AVM_NODE_PROP = "alfresco.avm.delete_AVMNodeProperty";
    private static final String DELETE_AVM_NODE_PROPS = "alfresco.avm.delete_AVMNodeProperties";
    
    private SqlMapClientTemplate template;
    
    public void setSqlMapClientTemplate(SqlMapClientTemplate sqlMapClientTemplate)
    {
        this.template = sqlMapClientTemplate;
    }
    
    @Override
    protected AVMNodeEntity createNodeEntity(AVMNodeEntity nodeEntity)
    {
        Long id = (Long) template.insert(INSERT_AVM_NODE, nodeEntity);
        nodeEntity.setId(id);
        return nodeEntity;
    }
    
    @Override
    protected AVMNodeEntity getNodeEntity(long id)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", id);
        return (AVMNodeEntity) template.queryForObject(SELECT_AVM_NODE_BY_ID, params);
    }
    
    @Override
    protected int updateNodeEntity(AVMNodeEntity updateNodeEntity)
    {
        return template.update(UPDATE_AVM_NODE, updateNodeEntity);
    }
    
    @Override
    protected int updateNodeEntityModTimeAndGuid(AVMNodeEntity updateNodeEntity)
    {
        // partial update
        return template.update(UPDATE_AVM_NODE_MODTIME_AND_GUID, updateNodeEntity);
    }
    
    @Override
    protected int updateNodeEntityModTimeAndContentData(AVMNodeEntity updateNodeEntity)
    {
        // partial update
        return template.update(UPDATE_AVM_NODE_MODTIME_AND_CDATA, updateNodeEntity);
    }
    
    @Override
    protected int deleteNodeEntity(long nodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", nodeId);
        return template.delete(DELETE_AVM_NODE, params);
    }
    
    @Override
    protected void updateNodeEntitiesClearNewInStore(long storeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", storeId);
        template.update(UPDATE_AVM_NODES_CLEAR_NEW_IN_STORE, params);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMNodeEntity> getNodeEntitiesNewInStore(long storeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", storeId);
        return (List<AVMNodeEntity>) template.queryForList(SELECT_AVM_NODES_NEW_IN_STORE, params);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMNodeEntity> getLayeredNodeEntitiesNewInStore(long storeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", storeId);
        return (List<AVMNodeEntity>) template.queryForList(SELECT_AVM_NODES_NEW_LAYERED_IN_STORE, params);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<Long> getLayeredNodeEntityIdsNewInStore(long storeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", storeId);
        return (List<Long>) template.queryForList(SELECT_AVM_NODE_IDS_NEW_LAYERED_IN_STORE, params);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMNodeEntity> getNodeEntityOrphans()
    {
        return (List<AVMNodeEntity>) template.queryForList(SELECT_AVM_NODES_ORPHANS);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMNodeEntity> getAllLayeredDirectoryNodeEntities()
    {
        return (List<AVMNodeEntity>) template.queryForList(SELECT_AVM_NODES_LAYERED_DIRECTORIES);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMNodeEntity> getAllLayeredFileNodeEntities()
    {
        return (List<AVMNodeEntity>) template.queryForList(SELECT_AVM_NODES_LAYERED_FILES);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<Long> getAVMNodeEntityIdsByAclId(long aclId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", aclId);
        return (List<Long>) template.queryForList(SELECT_AVM_NODE_IDS_BY_ACL_ID, params);
    }
    
    @Override
    protected void getPlainFileContentUrls(ContentUrlHandler handler)
    {
        CleanRowHandler rowHandler = new CleanRowHandler(handler);
        
        template.queryWithRowHandler(SELECT_AVM_CONTENT_URLS_FOR_PLAIN_FILES, rowHandler);
        
        if (logger.isDebugEnabled())
        {
            logger.debug("   Listed " + rowHandler.total + " content URLs");
        }
    }
    
    /**
     * Row handler for cleaning content URLs
     */
    private static class CleanRowHandler implements RowHandler
    {
        private final ContentUrlHandler handler;
        
        private int total = 0;
        
        private CleanRowHandler(ContentUrlHandler handler)
        {
            this.handler = handler;
        }
        public void handleRow(Object valueObject)
        {
            handler.handle((String)valueObject);
            total++;
            if (logger.isDebugEnabled() && (total == 0 || (total % 1000 == 0) ))
            {
                logger.debug("   Listed " + total + " content URLs");
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<Long> getAspectEntities(long nodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", nodeId);
        return (List<Long>) template.queryForList(SELECT_AVM_NODE_ASPECTS, params);
    }
    
    @Override
    protected void createAspectEntity(long nodeId, long qnameId)
    {
        AVMAspectEntity aspectEntity = new AVMAspectEntity(nodeId, qnameId);
        template.insert(INSERT_AVM_NODE_ASPECT, aspectEntity);
    }
    
    @Override
    protected int deleteAspectEntity(long nodeId, long qnameId)
    {
        AVMAspectEntity aspectEntity = new AVMAspectEntity(nodeId, qnameId);
        return template.delete(DELETE_AVM_NODE_ASPECT, aspectEntity);
    }
    
    @Override
    protected int deleteAspectEntities(long nodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", nodeId);
        return template.delete(DELETE_AVM_NODE_ASPECTS, params);
    }
    
    @Override
    protected void insertNodePropertyEntity(AVMNodePropertyEntity propEntity)
    {
        template.insert(INSERT_AVM_NODE_PROP, propEntity);
    }
    
    @Override
    protected int updateNodePropertyEntity(AVMNodePropertyEntity updatePropEntity)
    {
        return template.update(UPDATE_AVM_NODE_PROP, updatePropEntity);
    }
    
    @Override
    protected AVMNodePropertyEntity getNodePropertyEntity(long nodeId, long qnameId)
    {
        AVMNodePropertyEntity propEntity = new AVMNodePropertyEntity();
        propEntity.setNodeId(nodeId);
        propEntity.setQnameId(qnameId);
        
        return (AVMNodePropertyEntity) template.queryForObject(SELECT_AVM_NODE_PROP, propEntity);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected List<AVMNodePropertyEntity> getNodePropertyEntities(long nodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", nodeId);
        
        return (List<AVMNodePropertyEntity>) template.queryForList(SELECT_AVM_NODE_PROPS, params);
    }
    
    @Override
    protected int deleteNodePropertyEntity(long nodeId, long qnameId)
    {
        AVMNodePropertyEntity propEntity = new AVMNodePropertyEntity();
        propEntity.setNodeId(nodeId);
        propEntity.setQnameId(qnameId);
        
        return template.delete(DELETE_AVM_NODE_PROP, propEntity);
    }
    
    @Override
    protected int deleteNodePropertyEntities(long nodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", nodeId);
        
        return template.delete(DELETE_AVM_NODE_PROPS, params);
    }
}
