/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.cmis.mapping;

import java.io.Serializable;

import org.alfresco.cmis.CMISDictionaryModel;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;

/**
 * Accesser for CMIS is latest version property
 * 
 * @author dward
 */
public class IsLatestVersionProperty extends AbstractVersioningProperty
{
    /**
     * Construct
     * 
     * @param serviceRegistry
     */
    public IsLatestVersionProperty(ServiceRegistry serviceRegistry)
    {
        super(serviceRegistry, CMISDictionaryModel.PROP_IS_LATEST_VERSION);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.property.PropertyAccessor#getValue(org.alfresco.service.cmr.repository.NodeRef)
     */
    public Serializable getValue(NodeRef nodeRef)
    {
        if (isWorkingCopy(nodeRef) || getVersionSeries(nodeRef).equals(nodeRef) && !hasWorkingCopy(nodeRef))
        {
            return true;
        }
        NodeRef versionSeries = getVersionSeries(nodeRef);
        if (hasWorkingCopy(versionSeries))
        {
            return false;
        }
        
        ServiceRegistry serviceRegistry = getServiceRegistry();
        String versionLabel = (String) serviceRegistry.getNodeService().getProperty(nodeRef, ContentModel.PROP_VERSION_LABEL);
        if (versionLabel == null)
        {
            return false;
        }
        VersionHistory versionHistory = serviceRegistry.getVersionService().getVersionHistory(versionSeries);
        if (versionHistory == null)
        {
            return false;
        }
        Version version = versionHistory.getVersion(versionLabel);
        if (version == null)
        {
            return false;
        }
        return versionHistory.getHeadVersion().getFrozenStateNodeRef().equals(version.getFrozenStateNodeRef());
    }
}
