/*
 * Copyright (C) 2005 Alfresco, Inc.
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
/**
 * 
 */
package org.alfresco.repo.action.executer;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.rule.RuleServiceException;
import org.alfresco.service.namespace.QName;

/**
 * Copy action executor.
 * <p>
 * Copies the actioned upon node to a specified location.
 * 
 * @author Roy Wetherall
 */
public class CopyActionExecuter extends ActionExecuterAbstractBase
{
    public static final String ERR_OVERWRITE = "Unable to overwrite copy because more than one have been found.";
    
    public static final String NAME = "copy";
    public static final String PARAM_DESTINATION_FOLDER = "destination-folder";
    public static final String PARAM_ASSOC_TYPE_QNAME = "assoc-type";
    public static final String PARAM_ASSOC_QNAME = "assoc-name";
    public static final String PARAM_DEEP_COPY = "deep-copy";
    public static final String PARAM_OVERWRITE_COPY = "overwrite-copy";
    
    /**
     * Node operations service
     */
    private CopyService copyService;
	
	/**
	 * The node service
	 */
	private NodeService nodeService;
	
    /**
     * Sets the node service
     * 
     * @param nodeService   the node service
     */
	public void setNodeService(NodeService nodeService) 
	{
		this.nodeService = nodeService;
	}
	
    /**
     * Sets the copy service
     * 
     * @param copyService   the copy service
     */
	public void setCopyService(CopyService copyService) 
	{
		this.copyService = copyService;
	}
    

    /**
     * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
     */
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) 
	{
		paramList.add(new ParameterDefinitionImpl(PARAM_DESTINATION_FOLDER, DataTypeDefinition.NODE_REF, true, getParamDisplayLabel(PARAM_DESTINATION_FOLDER)));
		paramList.add(new ParameterDefinitionImpl(PARAM_ASSOC_TYPE_QNAME, DataTypeDefinition.QNAME, true, getParamDisplayLabel(PARAM_ASSOC_TYPE_QNAME)));
		paramList.add(new ParameterDefinitionImpl(PARAM_ASSOC_QNAME, DataTypeDefinition.QNAME, true, getParamDisplayLabel(PARAM_ASSOC_QNAME)));
		paramList.add(new ParameterDefinitionImpl(PARAM_DEEP_COPY, DataTypeDefinition.BOOLEAN, false, getParamDisplayLabel(PARAM_DEEP_COPY)));		
        paramList.add(new ParameterDefinitionImpl(PARAM_OVERWRITE_COPY, DataTypeDefinition.BOOLEAN, false, getParamDisplayLabel(PARAM_OVERWRITE_COPY)));
	}

    /**
     * @see org.alfresco.repo.action.executer.ActionExecuter#execute(org.alfresco.repo.ref.NodeRef, org.alfresco.repo.ref.NodeRef)
     */
    public void executeImpl(Action ruleAction, NodeRef actionedUponNodeRef)
    {
		if (this.nodeService.exists(actionedUponNodeRef) == true)
		{
	        NodeRef destinationParent = (NodeRef)ruleAction.getParameterValue(PARAM_DESTINATION_FOLDER);
	        QName destinationAssocTypeQName = (QName)ruleAction.getParameterValue(PARAM_ASSOC_TYPE_QNAME);
	        QName destinationAssocQName = (QName)ruleAction.getParameterValue(PARAM_ASSOC_QNAME);
	        
	        // Get the deep copy value
	        boolean deepCopy = false;
            Boolean deepCopyValue = (Boolean)ruleAction.getParameterValue(PARAM_DEEP_COPY);
            if (deepCopyValue != null)
            {
                deepCopy = deepCopyValue.booleanValue();
            }
	        
            // Get the overwirte value
            boolean overwrite = true;
            Boolean overwriteValue = (Boolean)ruleAction.getParameterValue(PARAM_OVERWRITE_COPY);
            if (overwriteValue != null)
            {
                overwrite = overwriteValue.booleanValue();
            }
            
            // Since we are overwriting we need to figure out whether the destination node exists
            NodeRef destinationNodeRef = null;
            if (overwrite == true)
            {
                // Try and find copies of the actioned upon node reference
                List<NodeRef> copies = this.copyService.getCopies(actionedUponNodeRef);
                if (copies != null && copies.isEmpty() == false)
                {
                    for (NodeRef copy : copies)
                    {
                        // Ignore if the copy is a working copy
                        if (this.nodeService.hasAspect(copy, ContentModel.ASPECT_WORKING_COPY) == false)
                        {
                            // We can assume that we are looking for a node created by this action so the primary parent will
                            // match the destination folder
                            NodeRef parent = this.nodeService.getPrimaryParent(copy).getParentRef();
                            if (parent.equals(destinationParent) == true)
                            {
                                if (destinationNodeRef == null)
                                {
                                    destinationNodeRef = copy;
                                }
                                else
                                {
                                    throw new RuleServiceException(ERR_OVERWRITE);
                                }
                            }
                            
                        }
                    }
                }
            }
            
            if (destinationNodeRef != null)
            {
                // Overwrite the state of the destination node ref with the actioned upon node state
                this.copyService.copy(actionedUponNodeRef, destinationNodeRef);
            }
            else
            {
                // Create a new copy of the node
                this.copyService.copyAndRename(
	                actionedUponNodeRef, 
	                destinationParent,
	                destinationAssocTypeQName,
	                destinationAssocQName,
	                deepCopy);
            }
		}
    }
}
