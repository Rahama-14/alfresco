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
package org.alfresco.repo.admin.patch.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.springframework.extensions.surf.util.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

/**
 * Apply Version Edition to Repository Descriptor
 *
 * @author David Caruana
 */
public class DescriptorUpdatePatch extends AbstractPatch
{
    private static final String MSG_SUCCESS = "patch.descriptorUpdate.result";

    private ImporterBootstrap systemBootstrap;
    
    public void setSystemBootstrap(ImporterBootstrap systemBootstrap)
    {
        this.systemBootstrap = systemBootstrap;
    }

    @Override
    protected String applyInternal() throws Exception
    {
        checkPropertyNotNull(systemBootstrap, "systemBootstrap");
        
        // retrieve system descriptor location
        StoreRef storeRef = systemBootstrap.getStoreRef();
        Properties systemProperties = systemBootstrap.getConfiguration();

        // check for the store
        if (nodeService.exists(storeRef))
        {
            // get the current descriptor
            String path = systemProperties.getProperty("system.descriptor.current.childname");
            String searchPath = "/" + path;
            NodeRef rootNodeRef = nodeService.getRootNode(storeRef);
            List<NodeRef> nodeRefs = searchService.selectNodes(rootNodeRef, searchPath, null, namespaceService, false);
            if (nodeRefs.size() > 0)
            {
                NodeRef descriptorNodeRef = nodeRefs.get(0);

                // set version edition
                Serializable value = nodeService.getProperty(descriptorNodeRef, ContentModel.PROP_SYS_VERSION_EDITION);
                if (value == null)
                {
                    String edition = systemProperties.getProperty("version.edition");
                    Collection<String> editions = new ArrayList<String>();
                    editions.add(edition);
                    nodeService.setProperty(descriptorNodeRef, ContentModel.PROP_SYS_VERSION_EDITION, (Serializable)editions);
                }
            }
        }
        
        // done
        String msg = I18NUtil.getMessage(MSG_SUCCESS);
        return msg;
    }
    
}
