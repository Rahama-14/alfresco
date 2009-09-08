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

import org.alfresco.repo.domain.avm.AVMHistoryLinkEntity;
import org.alfresco.repo.domain.avm.AVMMergeLinkEntity;
import org.alfresco.repo.domain.avm.AVMChildEntryEntity;
import org.alfresco.repo.domain.avm.AbstractAVMNodeLinksDAOImpl;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

/**
 * iBatis-specific implementation of the AVMNodeLinks DAO.
 * 
 * @author janv
 * @since 3.2
 */
public class AVMNodeLinksDAOImpl extends AbstractAVMNodeLinksDAOImpl
{
    private static final String SELECT_AVM_NODE_CHILD_ENTRY ="alfresco.avm.select_AVMChildEntry"; // parent + name + child
    private static final String SELECT_AVM_NODE_CHILD_ENTRY_BY_PARENT_AND_NAME ="alfresco.avm.select_AVMChildEntryByParentAndName"; // parent + name
    private static final String SELECT_AVM_NODE_CHILD_ENTRY_BY_PARENT_AND_CHILD ="alfresco.avm.select_AVMChildEntryByParentAndChild"; // parent + child
    private static final String SELECT_AVM_NODE_CHILD_ENTRIES_BY_PARENT ="alfresco.avm.select_AVMNodeChildEntriesByParent"; // parent
    private static final String SELECT_AVM_NODE_CHILD_ENTRIES_BY_PARENT_AND_NAME_PATTERN ="alfresco.avm.select_AVMNodeChildEntriesByParentAndNamePattern"; // parent + name pattern
    private static final String SELECT_AVM_NODE_CHILD_ENTRIES_BY_CHILD ="alfresco.avm.select_AVMNodeChildEntriesByChild"; // child
    
    private static final String INSERT_AVM_NODE_CHILD_ENTRY ="alfresco.avm.insert_AVMChildEntry"; // parent + name + child
    
    private static final String DELETE_AVM_NODE_CHILD_ENTRY_BY_PARENT_AND_NAME ="alfresco.avm.delete_AVMChildEntryByParentAndName"; // parent + name
    private static final String DELETE_AVM_NODE_CHILD_ENTRY_BY_PARENT_AND_CHILD ="alfresco.avm.delete_AVMChildEntryByParentAndChild"; // parent + child
    private static final String DELETE_AVM_NODE_CHILD_ENTRIES_BY_PARENT ="alfresco.avm.delete_AVMNodeChildEntriesByParent"; // parent
    
    private static final String INSERT_AVM_MERGE_LINK ="alfresco.avm.insert_AVMMergeLink";
    private static final String DELETE_AVM_MERGE_LINK ="alfresco.avm.delete_AVMMergeLink";
    
    private static final String SELECT_AVM_MERGE_LINKS_BY_FROM ="alfresco.avm.select_AVMMergeLinksByFrom";
    private static final String SELECT_AVM_MERGE_LINK_BY_TO ="alfresco.avm.select_AVMMergeLinkByTo";
    
    private static final String INSERT_AVM_HISTORY_LINK ="alfresco.avm.insert_AVMHistoryLink";
    private static final String DELETE_AVM_HISTORY_LINK ="alfresco.avm.delete_AVMHistoryLink";
    
    private static final String SELECT_AVM_HISTORY_LINKS_BY_ANCESTOR ="alfresco.avm.select_AVMHistoryLinksByAncestor";
    private static final String SELECT_AVM_HISTORY_LINK_BY_DESCENDENT ="alfresco.avm.select_AVMHistoryLinkByDescendent";
    private static final String SELECT_AVM_HISTORY_LINK ="alfresco.avm.select_AVMHistoryLink";
    
    private SqlMapClientTemplate template;
    
    public void setSqlMapClientTemplate(SqlMapClientTemplate sqlMapClientTemplate)
    {
        this.template = sqlMapClientTemplate;
    }
    
    @Override
    protected AVMChildEntryEntity getChildEntryEntity(AVMChildEntryEntity childEntryEntity)
    {
        return (AVMChildEntryEntity) template.queryForObject(SELECT_AVM_NODE_CHILD_ENTRY, childEntryEntity);
    }
    
    @Override
    protected AVMChildEntryEntity getChildEntryEntity(long parentNodeId, String name)
    {
        AVMChildEntryEntity childEntryEntity = new AVMChildEntryEntity(parentNodeId, name);
        return (AVMChildEntryEntity) template.queryForObject(SELECT_AVM_NODE_CHILD_ENTRY_BY_PARENT_AND_NAME, childEntryEntity);
    }
    
    @Override
    protected AVMChildEntryEntity getChildEntryEntity(long parentNodeId, long childNodeId)
    {
        AVMChildEntryEntity childEntryEntity = new AVMChildEntryEntity(parentNodeId, childNodeId);
        return (AVMChildEntryEntity) template.queryForObject(SELECT_AVM_NODE_CHILD_ENTRY_BY_PARENT_AND_CHILD, childEntryEntity);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMChildEntryEntity> getChildEntryEntitiesByParent(long parentNodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", parentNodeId);
        return (List<AVMChildEntryEntity>) template.queryForList(SELECT_AVM_NODE_CHILD_ENTRIES_BY_PARENT, params);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMChildEntryEntity> getChildEntryEntitiesByParent(long parentNodeId, String childNamePattern)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", parentNodeId);
        params.put("pattern", childNamePattern);
        return (List<AVMChildEntryEntity>) template.queryForList(SELECT_AVM_NODE_CHILD_ENTRIES_BY_PARENT_AND_NAME_PATTERN, params);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMChildEntryEntity> getChildEntryEntitiesByChild(long childNodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", childNodeId);
        return (List<AVMChildEntryEntity>) template.queryForList(SELECT_AVM_NODE_CHILD_ENTRIES_BY_CHILD, params);
    }
    
    @Override
    protected void createChildEntryEntity(AVMChildEntryEntity childEntryEntity)
    {
        template.insert(INSERT_AVM_NODE_CHILD_ENTRY, childEntryEntity);
    }
    
    @Override
    protected int deleteChildEntryEntity(long parentNodeId, String name)
    {
        AVMChildEntryEntity childEntryEntity = new AVMChildEntryEntity(parentNodeId, name);
        return template.delete(DELETE_AVM_NODE_CHILD_ENTRY_BY_PARENT_AND_NAME, childEntryEntity);
    }
    
    @Override
    protected int deleteChildEntryEntity(long parentNodeId, long childNodeId)
    {
        AVMChildEntryEntity childEntryEntity = new AVMChildEntryEntity(parentNodeId, childNodeId);
        return template.delete(DELETE_AVM_NODE_CHILD_ENTRY_BY_PARENT_AND_CHILD, childEntryEntity);
    }
    
    @Override
    protected int deleteChildEntryEntities(long parentNodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", parentNodeId);
        return template.delete(DELETE_AVM_NODE_CHILD_ENTRIES_BY_PARENT, params);
    }
    
    
    @Override
    protected void createMergeLinkEntity(long mergeFromNodeId, long mergeToNodeId)
    {
        AVMMergeLinkEntity mergeLinkEntity = new AVMMergeLinkEntity(mergeFromNodeId, mergeToNodeId);
        template.insert(INSERT_AVM_MERGE_LINK, mergeLinkEntity);
    }
    
    @Override
    protected int deleteMergeLinkEntity(long mergeFromNodeId, long mergeToNodeId)
    {
        AVMMergeLinkEntity mLinkEntity = new AVMMergeLinkEntity(mergeFromNodeId, mergeToNodeId);
        return template.delete(DELETE_AVM_MERGE_LINK, mLinkEntity);
    }
    
    @Override
    protected AVMMergeLinkEntity getMergeLinkEntityByTo(long mergeToNodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", mergeToNodeId);
        return (AVMMergeLinkEntity) template.queryForObject(SELECT_AVM_MERGE_LINK_BY_TO, params);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMMergeLinkEntity> getMergeLinkEntitiesByFrom(long mergeFromNodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", mergeFromNodeId);
        return (List<AVMMergeLinkEntity>) template.queryForList(SELECT_AVM_MERGE_LINKS_BY_FROM, params);
    }
    
    
    @Override
    protected void createHistoryLinkEntity(long ancestorNodeId, long mergeToNodeId)
    {
        AVMHistoryLinkEntity hLinkEntity = new AVMHistoryLinkEntity(ancestorNodeId, mergeToNodeId);
        template.insert(INSERT_AVM_HISTORY_LINK, hLinkEntity);
    }
    
    @Override
    protected int deleteHistoryLinkEntity(long ancestorNodeId, long descendentNodeId)
    {
        AVMHistoryLinkEntity hLinkEntity = new AVMHistoryLinkEntity(ancestorNodeId, descendentNodeId);
        return template.delete(DELETE_AVM_HISTORY_LINK, hLinkEntity);
    }
    
    @Override
    protected AVMHistoryLinkEntity getHistoryLinkEntityByDescendent(long descendentNodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", descendentNodeId);
        return (AVMHistoryLinkEntity) template.queryForObject(SELECT_AVM_HISTORY_LINK_BY_DESCENDENT, params);
    }
    
    @Override
    protected AVMHistoryLinkEntity getHistoryLinkEntity(long ancestorNodeId, long descendentNodeId)
    {
        AVMHistoryLinkEntity hLinkEntity = new AVMHistoryLinkEntity(ancestorNodeId, descendentNodeId);
        return (AVMHistoryLinkEntity) template.queryForObject(SELECT_AVM_HISTORY_LINK, hLinkEntity);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<AVMHistoryLinkEntity> getHistoryLinkEntitiesByAncestor(long ancestorNodeId)
    {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", ancestorNodeId);
        return (List<AVMHistoryLinkEntity>) template.queryForList(SELECT_AVM_HISTORY_LINKS_BY_ANCESTOR, params);
    }
}
