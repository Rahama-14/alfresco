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
package org.alfresco.repo.rule.ruletrigger;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A rule trigger for the creation of <b>secondary child associations</b>.
 * <p>
 * Policy names supported are:
 * <ul>
 *   <li>{@linkplain NodeServicePolicies.OnCreateChildAssociationPolicy}</li>
 * </ul>
 * 
 * @author Roy Wetherall
 */
public class OnCreateChildAssociationRuleTrigger
        extends RuleTriggerAbstractBase
        implements NodeServicePolicies.OnCreateChildAssociationPolicy
{
    /**
     * The logger
     */
    private static Log logger = LogFactory.getLog(OnCreateChildAssociationRuleTrigger.class);
	
	private static final String POLICY_NAME = "onCreateChildAssociation";
    
    private boolean isClassBehaviour = false;
		
	public void setIsClassBehaviour(boolean isClassBehaviour)
	{
		this.isClassBehaviour = isClassBehaviour;
	}
	
	/**
	 * @see org.alfresco.repo.rule.ruletrigger.RuleTrigger#registerRuleTrigger()
	 */
	public void registerRuleTrigger()
	{
		if (isClassBehaviour == true)
		{
			this.policyComponent.bindClassBehaviour(
					QName.createQName(NamespaceService.ALFRESCO_URI, POLICY_NAME), 
					this, 
					new JavaBehaviour(this, POLICY_NAME));
		}
		else
		{		
			this.policyComponent.bindAssociationBehaviour(
					QName.createQName(NamespaceService.ALFRESCO_URI, POLICY_NAME), 
					this, 
					new JavaBehaviour(this, POLICY_NAME));
		}
	}

    public void onCreateChildAssociation(ChildAssociationRef childAssocRef, boolean isNewNode)
    {
        if (isNewNode)
        {
            return;
        }
        if (logger.isDebugEnabled() == true)
        {
            logger.debug("Single child assoc trigger (policy = " + POLICY_NAME + ") fired for parent node " + childAssocRef.getParentRef() + " and child node " + childAssocRef.getChildRef());
        }
        
        // NOTE:
        //
        // We check for the presence of this resource in the transaction to determine whether a rename has been issued.  If that is the case 
        // then we don't want to trigger any associated rules.
        //
        // See http://issues.alfresco.com/browse/AR-1544
        if (AlfrescoTransactionSupport.getResource(childAssocRef.getChildRef().toString()+"rename") == null)
        {
        	triggerRules(childAssocRef.getParentRef(), childAssocRef.getChildRef());
        }
        else
        {
        	// Remove the marker
        	AlfrescoTransactionSupport.unbindResource(childAssocRef.getChildRef().toString()+"rename");
        }
    }
}
