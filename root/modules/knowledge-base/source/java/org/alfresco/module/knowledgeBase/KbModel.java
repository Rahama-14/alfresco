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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.module.knowledgeBase;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;

/**
 * Knowledge base model contants
 * 
 * @author Roy Wetherall
 */
public interface KbModel
{
    public static final StoreRef SPACES_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    
    public static final String KB_URI = "http://www.alfresco.org/model/knowledgebase/1.0";
    public static final String KB_PREFIX = "kb";
    
    public static final QName ASPECT_ARTICLE = QName.createQName(KB_URI, "article");
    public static final QName TYPE_KNOWLEDGE_BASE = QName.createQName(KB_URI, "knowledgeBase");
    
    public static final QName PROP_KB_ID = QName.createQName(KB_URI, "kbId");
    public static final QName PROP_STATUS = QName.createQName(KB_URI, "status");
    public static final QName PROP_VISIBILITY = QName.createQName(KB_URI, "visibility");
    
    public static final QName ASSOC_KNOWLEDGE_BASE = QName.createQName(KB_URI, "knowledgeBase");
    public static final QName ASSOC_PUBLISHED = QName.createQName(KB_URI, "published");
    
    // Group names
    public static final String GROUP_INTERNAL = "GROUP_KnowledgeBase_Internal";
    public static final String GROUP_TIER_1 = "GROUP_KnowledgeBase_Tier_1";
    public static final String GROUP_TIER_2 = "GROUP_KnowledgeBase_Tier_2";
    
    // Categories
    public static final NodeRef STATUS_DRAFT = new NodeRef(SPACES_STORE, "kb:status-draft");
    public static final NodeRef STATUS_PENDING = new NodeRef(SPACES_STORE, "kb:status-pending");
    public static final NodeRef STATUS_PUBLISHED = new NodeRef(SPACES_STORE, "kb:status-published");
    public static final NodeRef STATUS_ARCHIVED = new NodeRef(SPACES_STORE, "kb:status-archived");
    
    // Visibility
    public static final NodeRef VISIBILITY_INTERNAL = new NodeRef(SPACES_STORE, "kb:visibility-internal");
    public static final NodeRef VISIBILITY_TIER_1 = new NodeRef(SPACES_STORE, "kb:visibility-tier-one");
    public static final NodeRef VISIBILITY_TIER_2 = new NodeRef(SPACES_STORE, "kb:visibility-tier-two");
    public static final NodeRef VISIBILITY_TIER_3 = new NodeRef(SPACES_STORE, "kb:visibility-tier-three");
}
