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
package org.alfresco.cmis.dictionary;

import java.util.Collection;

import org.alfresco.cmis.dictionary.CMISAbstractDictionaryService.DictionaryRegistry;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.QName;


/**
 * CMIS Policy Type Definition
 * 
 * @author davidc
 */
public class CMISPolicyTypeDefinition extends CMISAbstractTypeDefinition 
{
    private static final long serialVersionUID = 1621538303752395828L;

    /**
     * Construct
     * 
     * @param cmisMapping
     * @param typeId
     * @param cmisClassDef
     */
    public CMISPolicyTypeDefinition(CMISMapping cmisMapping, CMISTypeId typeId, ClassDefinition cmisClassDef)
    {
        isPublic = true;
        
        // Object Type definitions
        this.cmisClassDef = cmisClassDef;
        objectTypeId = typeId;
        displayName = (cmisClassDef.getTitle() != null) ? cmisClassDef.getTitle() : typeId.getId();
        if (typeId == CMISDictionaryModel.POLICY_TYPE_ID)
        {
            objectTypeQueryName = typeId.getId();
            QName parentQName = cmisMapping.getCmisType(cmisClassDef.getParentName());
            if (parentQName != null)
            {
                parentTypeId = cmisMapping.getCmisTypeId(CMISScope.OBJECT, parentQName);
            }
        }
        else
        {
            objectTypeQueryName = cmisMapping.buildPrefixEncodedString(typeId.getQName(), false);
            parentTypeId = CMISDictionaryModel.POLICY_TYPE_ID;
        }
        description = cmisClassDef.getDescription();
        creatable = false;
        queryable = false;
        controllable = false;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.dictionary.CMISObjectTypeDefinition#createSubTypes(org.alfresco.cmis.dictionary.CMISMapping, org.alfresco.service.cmr.dictionary.DictionaryService)
     */
    @Override
    /*package*/ void createSubTypes(CMISMapping cmisMapping, DictionaryService dictionaryService)
    {
        if (objectTypeId.equals(CMISDictionaryModel.POLICY_TYPE_ID))
        {
            // all aspects are sub-type of POLICY_OBJECT_TYPE
            Collection<QName> aspects = dictionaryService.getAllAspects();
            for (QName aspect : aspects)
            {
                if (cmisMapping.isValidCmisPolicy(aspect))
                {
                    subTypeIds.add(cmisMapping.getCmisTypeId(CMISScope.POLICY, aspect));
                }
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.dictionary.CMISObjectTypeDefinition#resolveInheritance(org.alfresco.cmis.dictionary.AbstractCMISDictionaryService.DictionaryRegistry)
     */
    @Override
    /*package*/ void resolveInheritance(DictionaryRegistry registry)
    {
        // NOTE: Force no inheritance of base Policy type
        inheritedProperties.putAll(properties);
        ownedProperties.putAll(properties);
        
        if (logger.isDebugEnabled())
            logger.debug("Type " + objectTypeId + " properties: " + inheritedProperties.size() + ", owned: " + ownedProperties.size());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("CMISPolicyTypeDefinition[");
        builder.append("ObjectTypeId=").append(getTypeId()).append(", ");
        builder.append("ObjectTypeQueryName=").append(getQueryName()).append(", ");
        builder.append("ObjectTypeDisplayName=").append(getDisplayName()).append(", ");
        builder.append("ParentTypeId=").append(getParentType() == null ? "<none>" : getParentType().getTypeId()).append(", ");
        builder.append("Description=").append(getDescription()).append(", ");
        builder.append("Creatable=").append(isCreatable()).append(", ");
        builder.append("Queryable=").append(isQueryable()).append(", ");
        builder.append("Controllable=").append(isControllable()).append(", ");
        builder.append("IncludeInSuperTypeQuery=").append(isIncludeInSuperTypeQuery()).append(", ");
        builder.append("SubTypes=").append(getSubTypes(false).size()).append(", ");
        builder.append("Properties=").append(getPropertyDefinitions().size());
        builder.append("]");
        return builder.toString();
    }

}
