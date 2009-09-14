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
package org.alfresco.module.org_alfresco_module_dod5015.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementAdminService;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.scripts.Cache;
import org.alfresco.web.scripts.Status;
import org.alfresco.web.scripts.WebScriptRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides the implementation for the customrefs.get webscript.
 * 
 * @author Neil McErlean
 */
public class CustomRefsGet extends AbstractRmWebScript
{
    private static final String REFERENCE_TYPE = "referenceType";
    private static final String REF_ID = "refId";
    private static final String LABEL = "label";
    private static final String SOURCE = "source";
    private static final String TARGET = "target";
    private static final String PARENT_REF = "parentRef";
    private static final String CHILD_REF = "childRef";
    private static final String SOURCE_REF = "sourceRef";
    private static final String TARGET_REF = "targetRef";
    private static final String CUSTOM_REFS_FROM = "customRefsFrom";
    private static final String CUSTOM_REFS_TO = "customRefsTo";
    
    private static Log logger = LogFactory.getLog(CustomRefsGet.class);
    private RecordsManagementAdminService rmAdminService;
    
    public void setRecordsManagementAdminService(RecordsManagementAdminService rmAdminService)
    {
        this.rmAdminService = rmAdminService;
    }

    @Override
    public Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        Map<String, Object> ftlModel = new HashMap<String, Object>();
        
        NodeRef node = parseRequestForNodeRef(req);
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Getting custom reference instances for " + node);
        }

        // All the references that come 'out' from this node.
        List<Map<String, String>> listOfOutwardReferenceData = new ArrayList<Map<String, String>>();
        
        List<AssociationRef> assocsFromThisNode = this.rmAdminService.getCustomReferencesFrom(node);
        addBidirectionalReferenceData(listOfOutwardReferenceData, assocsFromThisNode);
        
        List<ChildAssociationRef> childAssocs = this.rmAdminService.getCustomChildReferences(node);
        addParentChildReferenceData(listOfOutwardReferenceData, childAssocs);
        
        // All the references that come 'in' to this node.
        List<Map<String, String>> listOfInwardReferenceData = new ArrayList<Map<String, String>>();
        
        List<AssociationRef> toAssocs = this.rmAdminService.getCustomReferencesTo(node);
        addBidirectionalReferenceData(listOfInwardReferenceData, toAssocs);
        
        List<ChildAssociationRef> parentAssocs = this.rmAdminService.getCustomParentReferences(node);
        addParentChildReferenceData(listOfInwardReferenceData, parentAssocs);
        
    	if (logger.isDebugEnabled())
    	{
    		logger.debug("Retrieved custom reference instances: " + assocsFromThisNode);
    	}
    	
        ftlModel.put(CUSTOM_REFS_FROM, listOfOutwardReferenceData);
        ftlModel.put(CUSTOM_REFS_TO, listOfInwardReferenceData);

        return ftlModel;
    }

    /**
     * This method goes through the associationRefs specified and constructs a Map<String, String>
     * for each assRef. FTL-relevant data are added to that map. The associationRefs must all be
     * parent/child references.
     * 
     * @param listOfReferenceData
     * @param assocs
     */
    private void addParentChildReferenceData(List<Map<String, String>> listOfReferenceData,
            List<ChildAssociationRef> childAssocs)
    {
        for (ChildAssociationRef childAssRef : childAssocs)
    	{
    		Map<String, String> data = new HashMap<String, String>();

    		QName typeQName = childAssRef.getTypeQName();
    		
    		data.put(CHILD_REF, childAssRef.getChildRef().toString());
    		data.put(PARENT_REF, childAssRef.getParentRef().toString());

            AssociationDefinition assDef = rmAdminService.getCustomReferenceDefinitions().get(typeQName);
            
            if (assDef != null)
            {
                String compoundTitle = assDef.getTitle();
    
                data.put(REF_ID, typeQName.getLocalName());
    
                String[] sourceAndTarget = rmAdminService.splitSourceTargetId(compoundTitle);
                data.put(SOURCE, sourceAndTarget[0]);
                data.put(TARGET, sourceAndTarget[1]);
                data.put(REFERENCE_TYPE, CustomReferenceType.PARENT_CHILD.toString());
                
                listOfReferenceData.add(data);
            }
    	}
    }

    /**
     * This method goes through the associationRefs specified and constructs a Map<String, String>
     * for each assRef. FTL-relevant data are added to that map. The associationRefs must all be
     * bidirectional references.
     * 
     * @param listOfReferenceData
     * @param assocs
     */
    private void addBidirectionalReferenceData(List<Map<String, String>> listOfReferenceData,
            List<AssociationRef> assocs)
    {
        for (AssociationRef assRef : assocs)
    	{
    		Map<String, String> data = new HashMap<String, String>();

    		QName typeQName = assRef.getTypeQName();
            AssociationDefinition assDef = rmAdminService.getCustomReferenceDefinitions().get(typeQName);
            
            if (assDef != null)
            {
                data.put(LABEL, assDef.getTitle());
                data.put(REF_ID, typeQName.getLocalName());
                data.put(REFERENCE_TYPE, CustomReferenceType.BIDIRECTIONAL.toString());
                data.put(SOURCE_REF, assRef.getSourceRef().toString());
                data.put(TARGET_REF, assRef.getTargetRef().toString());
                
                listOfReferenceData.add(data);
            }
        }
    }
}