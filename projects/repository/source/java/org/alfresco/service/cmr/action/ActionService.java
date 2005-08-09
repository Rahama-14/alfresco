/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.service.cmr.action;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Action service interface
 * 
 * @author Roy Wetherall
 */
public interface ActionService
{
	/**
	 * Get a named action definition
	 * 
	 * @param name	the name of the action definition
	 * @return		the action definition
	 */
	ActionDefinition getActionDefinition(String name);
	
	/**
	 * Get all the action definitions
	 * 
	 * @return	the list action definitions
	 */
	List<ActionDefinition> getActionDefinitions();
	
	/**
	 * Get a named action condition definition
	 * 
	 * @param name	the name of the action condition definition
	 * @return		the action condition definition
	 */
	ActionConditionDefinition getActionConditionDefinition(String name);
	
	/**
	 * Get all the action condition definitions
	 * 
	 * @return	the list of aciton condition definitions
	 */
	List<ActionConditionDefinition> getActionConditionDefinitions();
	
	/**
	 * Create a new action
	 * 
	 * @param name	the action definition name
	 * @return		the action
	 */
	Action createAction(String name);
	
	/**
	 * Create a composite action 
	 * 
	 * @return	the composite action
	 */
	CompositeAction createCompositeAction();
	
	/**
	 * Create an action condition
	 * 
	 * @param name	the action condition definition name
	 * @return		the action condition
	 */
	ActionCondition createActionCondition(String name);
	
	/**
	 * The actions conditions are always checked.
	 * 
	 * @see ActionService#executeAction(Action, NodeRef, boolean)
	 *  
	 * @param action				the action
	 * @param actionedUponNodeRef	the actioned upon node reference
	 */
	void executeAction(Action action, NodeRef actionedUponNodeRef);
	
	/**
	 * Executes the specified action upon the node reference provided.
	 * <p>
	 * If specified that the conditions should be checked then any conditions
	 * set on the action are evaluated.
	 * <p>
	 * If the conditions fail then the action is not executed.
	 * <p>
	 * If an action has no conditions then the action will always be executed.
	 * <p>
	 * If the conditions are not checked then the action will always be executed.
	 * 
	 * @param action				the action
	 * @param actionedUponNodeRef	the actioned upon node reference
	 * @param checkConditions		indicates whether the conditions should be checked before
	 * 								executing the action
	 */
	void executeAction(Action action, NodeRef actionedUponNodeRef, boolean checkConditions);
	
	/**
	 * Evaluted the conditions set on an action.
	 * <p>
	 * Returns true if the action has no conditions.
	 * <p>
	 * If the action has more than one condition their results are combined using the 'AND' 
	 * logical operator.
	 * 
	 * @param action				the action
	 * @param actionedUponNodeRef	the actioned upon node reference
	 * @return						true if the condition succeeds, false otherwise
	 */
	boolean evaluateAction(Action action, NodeRef actionedUponNodeRef);
	
	/**
	 * Evaluate an action condition.
	 * 
	 * @param condition				the action condition
	 * @param actionedUponNodeRef	the actioned upon node reference
	 * @return						true if the condition succeeds, false otherwise
	 */
	boolean evaluateActionCondition(ActionCondition condition, NodeRef actionedUponNodeRef);
	
	/**
	 * Save an action against a node reference.
	 * <p>
	 * The node will be made configurable if it is not already.
	 * <p>
	 * If the action already exists then its details will be updated.
	 * 
	 * @param nodeRef	the node reference
	 * @param action	the action
	 */
	void saveAction(NodeRef nodeRef, Action action);
	
	/**
	 * Gets all the actions currently saved on the given node reference.
	 * 
	 * @param nodeRef	the ndoe reference
	 * @return			the list of actions
	 */
	List<Action> getActions(NodeRef nodeRef);
	
	/**
	 * Gets an action stored against a given node reference.
	 * <p>
	 * Returns null if the action can not be found.
	 * 
	 * @param nodeRef	the node reference
	 * @param actionId	the action id
	 * @return			the action
	 */
	Action getAction(NodeRef nodeRef, String actionId);
	
	/**
	 * Removes an action associatied with a node reference.
	 * 
	 * @param nodeRef		the node reference
	 * @param action		the action
	 */
	void removeAction(NodeRef nodeRef, Action action);
	
	/**
	 * Removes all actions associated with a node reference
	 * 
	 * @param nodeRef	the node reference
	 */
	void removeAllActions(NodeRef nodeRef);
}
