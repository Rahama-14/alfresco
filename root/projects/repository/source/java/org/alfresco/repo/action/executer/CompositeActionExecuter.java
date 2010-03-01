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
package org.alfresco.repo.action.executer;

import java.util.List;

import org.alfresco.repo.action.RuntimeActionService;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.CompositeAction;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Add features action executor implementation.
 * 
 * @author Roy Wetherall
 */
public class CompositeActionExecuter extends ActionExecuterAbstractBase
{
    /**
     * Action constants
     */
	public static final String NAME = "composite-action";
	
	/**
	 * The action service
	 */
	private RuntimeActionService actionService;
	
	/**
	 * Set the action service
	 * 
	 * @param actionService  the action service
	 */
	public void setActionService(RuntimeActionService actionService)
	{
		this.actionService = actionService;
	}

    /**
     * @see org.alfresco.repo.action.executer.ActionExecuter#execute(org.alfresco.service.cmr.repository.NodeRef, NodeRef)
     */
    public void executeImpl(Action action, NodeRef actionedUponNodeRef)
    {
    	if (action instanceof CompositeAction)
		{
			for (Action subAction : ((CompositeAction)action).getActions())
			{
				// We don't check the conditions of sub-actions and they don't have an execution history
				this.actionService.directActionExecution(subAction, actionedUponNodeRef);
			}
		}
    }

    /**
     * Add parameter definitions
     */
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) 
	{
		// No parameters
	}

}
