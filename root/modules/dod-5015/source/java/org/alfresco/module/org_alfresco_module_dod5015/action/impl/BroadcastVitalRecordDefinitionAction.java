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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.org_alfresco_module_dod5015.action.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.VitalRecordDefinition;
import org.alfresco.module.org_alfresco_module_dod5015.action.RMActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Period;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;

/**
 * Action to implement the consequences of a change to the value of the VitalRecordDefinition properties. When the
 * VitalRecordIndicator or the reviewPeriod properties are changed on a record container, then any descendant folders or
 * records must be updated as a consequence. Descendant folders should have their reviewPeriods and/or
 * vitalRecordIndicators updated to match the new value. Descendant records should have their reviewAsOf date updated.
 * 
 * @author Neil McErlean
 */
public class BroadcastVitalRecordDefinitionAction extends RMActionExecuterAbstractBase
{
    /**
     * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.action.Action,
     *      org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
    {
        this.propagateChangeToChildrenOf(actionedUponNodeRef);
    }

    /**
     * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList)
    {
        // Intentionally empty
    }

    private void propagateChangeToChildrenOf(NodeRef actionedUponNodeRef)
    {
        Map<QName, Serializable> parentProps = nodeService.getProperties(actionedUponNodeRef);
        boolean parentVri = (Boolean) parentProps.get(PROP_VITAL_RECORD_INDICATOR);
        Period parentReviewPeriod = (Period) parentProps.get(PROP_REVIEW_PERIOD);

        List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(actionedUponNodeRef, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
        for (ChildAssociationRef nextAssoc : assocs)
        {
            NodeRef nextChild = nextAssoc.getChildRef();

            // If the child is a record, then the VitalRecord aspect needs to be applied or updated
            if (recordsManagementService.isRecord(nextChild))
            {
                if (parentVri)
                {
                    VitalRecordDefinition vrDefn = recordsManagementService.getVitalRecordDefinition(nextChild);
                    Map<QName, Serializable> aspectProps = new HashMap<QName, Serializable>();
                    aspectProps.put(PROP_REVIEW_AS_OF, vrDefn.getNextReviewDate());

                    nodeService.addAspect(nextChild, RecordsManagementModel.ASPECT_VITAL_RECORD, aspectProps);
                }
                else
                {
                    nodeService.removeAspect(nextChild, RecordsManagementModel.ASPECT_VITAL_RECORD);
                }
            }
            else
            // copy the vitalRecordDefinition properties from the parent to the child
            {
                Map<QName, Serializable> childProps = nodeService.getProperties(nextChild);
                childProps.put(PROP_REVIEW_PERIOD, parentReviewPeriod);
                childProps.put(PROP_VITAL_RECORD_INDICATOR, parentVri);
                nodeService.setProperties(nextChild, childProps);
            }

            // Recurse down the containment hierarchy to all containers
            if (recordsManagementService.isRecord(nextChild) == false)
            {
                this.propagateChangeToChildrenOf(nextChild);
            }
        }
    }

    @Override
    public boolean isExecutableImpl(NodeRef filePlanComponent, Map<String, Serializable> parameters, boolean throwException)
    {
        return true;
    }

    @Override
    public Set<QName> getProtectedProperties()
    {
        HashSet<QName> qnames = new HashSet<QName>();
        qnames.add(PROP_REVIEW_PERIOD);
        qnames.add(PROP_VITAL_RECORD_INDICATOR);
        qnames.add(PROP_REVIEW_AS_OF);
        return qnames;
    }

    @Override
    public Set<QName> getProtectedAspects()
    {
        HashSet<QName> qnames = new HashSet<QName>();
        qnames.add(RecordsManagementModel.ASPECT_VITAL_RECORD);
        return qnames;
    }

}
