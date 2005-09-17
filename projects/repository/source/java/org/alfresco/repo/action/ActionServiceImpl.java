/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.evaluator.ActionConditionEvaluator;
import org.alfresco.repo.action.executer.ActionExecuter;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionUtil;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.action.ActionConditionDefinition;
import org.alfresco.service.cmr.action.ActionDefinition;
import org.alfresco.service.cmr.action.ActionExecutionDetails;
import org.alfresco.service.cmr.action.ActionExecutionStatus;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ActionServiceException;
import org.alfresco.service.cmr.action.CompositeAction;
import org.alfresco.service.cmr.action.ParameterizedItem;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.DynamicNamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Action service implementation
 * 
 * @author Roy Wetherall
 */
public class ActionServiceImpl implements ActionService, RuntimeActionService, ApplicationContextAware
{ 
	/**
	 * Transaction resource name
	 */
	private static final String POST_TRANSACTION_PENDING_ACTIONS = "postTransactionPendingActions";
	
	/**
	 * Error message
	 */
	private static final String ERR_FAIL = "The action failed to execute due to an error.";
	
	/**
     * The logger
     */
	private static Log logger = LogFactory.getLog(ActionServiceImpl.class); 
	
	/**
	 * The application context
	 */
	private ApplicationContext applicationContext;
	
	/**
	 * The transacton service
	 */
	private TransactionService transactionService;
	
	/**
	 * The node service
	 */
	private NodeService nodeService;
	
	/**
	 * The search service
	 */
	private SearchService searchService;
	
	/**
	 * The asynchronous action execution queue
	 */
	private AsynchronousActionExecutionQueue asynchronousActionExecutionQueue;
	
	/**
	 * Action transaction listener
	 */
	private ActionTransactionListener transactionListener = new ActionTransactionListener(this);
	
	/**
	 * All the condition definitions currently registered
	 */
	private Map<String, ActionConditionDefinition> conditionDefinitions = new HashMap<String, ActionConditionDefinition>();
	
	/**
	 * All the action definitions currently registered
	 */
	private Map<String, ActionDefinition> actionDefinitions = new HashMap<String, ActionDefinition>(); 
	
	/**
	 * Set the application context
	 * 
	 * @param applicationContext	the application context
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		this.applicationContext = applicationContext;
	}
	
	/**
	 * Set the node service
	 * 
	 * @param nodeService  the node service
	 */
	public void setNodeService(NodeService nodeService)
	{
		this.nodeService = nodeService;
	}
	
	/**
	 * Set the search service
	 * 
	 * @param searchService  the search service
	 */
	public void setSearchService(SearchService searchService)
	{
		this.searchService = searchService;
	}
	
	/**
	 * Set the transaction service
	 * 
	 * @param transactionService	the transaction service
	 */
	public void setTransactionService(TransactionService transactionService)
	{
		this.transactionService = transactionService;
	}
	
	/**
	 * Set the asynchronous action execution queue
	 * 
	 * @param asynchronousActionExecutionQueue	the asynchronous action execution queue
	 */
	public void setAsynchronousActionExecutionQueue(
			AsynchronousActionExecutionQueue asynchronousActionExecutionQueue)
	{
		this.asynchronousActionExecutionQueue = asynchronousActionExecutionQueue;
	}
	
	/**
	 * Get the asychronous action execution queue
	 * 
	 * @return	the asynchronous action execution queue
	 */
	public AsynchronousActionExecutionQueue getAsynchronousActionExecutionQueue()
	{
		return asynchronousActionExecutionQueue;
	}
	
	/**
	 * Gets the saved action folder reference
	 * 
	 * @param nodeRef	the node reference
	 * @return			the node reference
	 */
	private NodeRef getSavedActionFolderRef(NodeRef nodeRef)
	{
		List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(nodeRef, ActionableAspect.ASSOC_NAME_SAVEDACTIONFOLDER);
		if (assocs.size() != 1)
		{
			throw new ActionServiceException("Unable to retrieve the saved action folder reference.");
		}
		
		return assocs.get(0).getChildRef();
	}
	
	/**
	 * @see org.alfresco.service.cmr.action.ActionService#getActionDefinition(java.lang.String)
	 */
	public ActionDefinition getActionDefinition(String name)
	{
		return this.actionDefinitions.get(name);
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#getActionDefinitions()
	 */
	public List<ActionDefinition> getActionDefinitions()
	{
		return new ArrayList<ActionDefinition>(this.actionDefinitions.values());
	}	

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#getActionConditionDefinition(java.lang.String)
	 */
	public ActionConditionDefinition getActionConditionDefinition(String name)
	{
		return this.conditionDefinitions.get(name);
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#getActionConditionDefinitions()
	 */
	public List<ActionConditionDefinition> getActionConditionDefinitions()
	{
		return new ArrayList<ActionConditionDefinition>(this.conditionDefinitions.values());
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#createActionCondition(java.lang.String)
	 */
	public ActionCondition createActionCondition(String name)
	{
		return new ActionConditionImpl(GUID.generate(), name);
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#createActionCondition(java.lang.String, java.util.Map)
	 */
	public ActionCondition createActionCondition(String name, Map<String, Serializable> params)
	{
		ActionCondition condition = createActionCondition(name);
		condition.setParameterValues(params);
		return condition;
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#createAction()
	 */
	public Action createAction(String name)
	{
		return new ActionImpl(GUID.generate(),name);
	}
	
	/**
	 * @see org.alfresco.service.cmr.action.ActionService#createAction(java.lang.String, java.util.Map)
	 */
	public Action createAction(String name, Map<String, Serializable> params)
	{
		Action action = createAction(name);
		action.setParameterValues(params);
		return action;
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#createCompositeAction()
	 */
	public CompositeAction createCompositeAction()
	{
		return new CompositeActionImpl(GUID.generate());
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#evaluateAction(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef)
	 */
	public boolean evaluateAction(Action action, NodeRef actionedUponNodeRef)
	{
		boolean result = true;
		
		if (action.hasActionConditions() == true)
		{
			List<ActionCondition> actionConditions = action.getActionConditions();
			for (ActionCondition condition : actionConditions)
			{
				result = result && evaluateActionCondition(condition, actionedUponNodeRef);
			}
		}
		
		return result;
	}
	
	/**
	 * @see org.alfresco.service.cmr.action.ActionService#evaluateActionCondition(org.alfresco.service.cmr.action.ActionCondition, org.alfresco.service.cmr.repository.NodeRef)
	 */
	public boolean evaluateActionCondition(ActionCondition condition, NodeRef actionedUponNodeRef)
	{
		boolean result = false;

    	// Evaluate the condition
    	ActionConditionEvaluator evaluator = (ActionConditionEvaluator)this.applicationContext.getBean(condition.getActionConditionDefinitionName());
    	result = evaluator.evaluate(condition, actionedUponNodeRef);
		
		return result;
	}
	
	/**
	 * @see org.alfresco.service.cmr.action.ActionService#executeAction(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef, boolean)
	 */
	public void executeAction(Action action, NodeRef actionedUponNodeRef, boolean checkConditions)
	{
		executeAction(action, actionedUponNodeRef, checkConditions, action.getExecuteAsychronously());
	}
	
	/**
	 * @see org.alfresco.service.cmr.action.ActionService#executeAction(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef, boolean)
	 */
	public void executeAction(Action action, NodeRef actionedUponNodeRef, boolean checkConditions, boolean executeAsychronously)
	{
		// TODO When do we create this and when don't we create this??
		
		// Create the action execution details node
		NodeRef actionExecutionDetailsNodeRef = createActionExecutionDetails(action, actionedUponNodeRef);
		
		if (executeAsychronously == false)
		{
			executeActionImpl(action, actionedUponNodeRef, checkConditions, actionExecutionDetailsNodeRef, false);
		}
		else
		{
			// Add to the post transaction pending action list
			addPostTransactionPendingAction(action, actionedUponNodeRef, checkConditions, actionExecutionDetailsNodeRef);
		}
	}

	/**
	 * @see org.alfresco.repo.action.RuntimeActionService#executeActionImpl(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef, boolean, org.alfresco.service.cmr.repository.NodeRef)
	 */
	public void executeActionImpl(
			Action action, 
			NodeRef actionedUponNodeRef, 
			boolean checkConditions, 
			NodeRef actionExecutionDetailsNodeRef,
			boolean executedAsynchronously)
	{	
		try
		{
			// Check and execute now
			if (checkConditions == false || evaluateAction(action, actionedUponNodeRef) == true)
			{
				// Update the execution status
				updateExecutionStatus(ActionExecutionStatus.RUNNING, actionExecutionDetailsNodeRef);
				
				// Execute the action
				directActionExecution(action, actionedUponNodeRef);
				
				// Update the execution status
				updateExecutionStatus(ActionExecutionStatus.SUCCEEDED, actionExecutionDetailsNodeRef);
			}
		}
		catch (Throwable exception)
		{
			// Log the exception
			logger.error(
						"An error was encountered whilst executing the action '" + action.getActionDefinitionName() + "'.",
						exception);
			
			// Update the execution status
			updateExecutionFailure(exception, actionExecutionDetailsNodeRef);			
			
			if (executedAsynchronously == true)
			{				
				// If one is specified, queue the compensating action ready for execution
				Action compensatingAction = action.getCompensatingAction();
				if (compensatingAction != null)
				{					
					// Queue the compensating action ready for execution
					NodeRef compensatingActionExecutionDetailsNodeRef = createActionExecutionDetails(
							compensatingAction, 
							actionedUponNodeRef);
					this.asynchronousActionExecutionQueue.executeAction(this, compensatingAction, actionedUponNodeRef, false, compensatingActionExecutionDetailsNodeRef);
					
					// Indicate that a compensating action has been queued
					updateExecutionStatus(ActionExecutionStatus.COMPENSATED, actionExecutionDetailsNodeRef);
				}
			}
				
			// Rethrow the exception
			if (exception instanceof RuntimeException)
			{
				throw (RuntimeException)exception;
			}
			else
			{
				throw new ActionServiceException(ERR_FAIL, exception);
			}
			
		}
	}

	/**
	 * @see org.alfresco.repo.action.RuntimeActionService#directActionExecution(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef)
	 */
	public void directActionExecution(Action action, NodeRef actionedUponNodeRef)
	{
		// Get the action executer and execute
		ActionExecuter executer = (ActionExecuter)this.applicationContext.getBean(action.getActionDefinitionName());
		executer.execute(action, actionedUponNodeRef);
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#executeAction(org.alfresco.service.cmr.action.Action, NodeRef)
	 */
	public void executeAction(Action action, NodeRef actionedUponNodeRef)
	{
		executeAction(action, actionedUponNodeRef, true);
	}

	/**
	 * @see org.alfresco.repo.action.RuntimeActionService#registerActionConditionEvaluator(org.alfresco.repo.action.evaluator.ActionConditionEvaluator)
	 */
	public void registerActionConditionEvaluator(ActionConditionEvaluator actionConditionEvaluator) 
	{
		ActionConditionDefinition cond = actionConditionEvaluator.getActionConditionDefintion();
		this.conditionDefinitions.put(cond.getName(), cond);
	}

	/**
	 * @see org.alfresco.repo.action.RuntimeActionService#registerActionExecuter(org.alfresco.repo.action.executer.ActionExecuter)
	 */
	public void registerActionExecuter(ActionExecuter actionExecuter) 
	{
		ActionDefinition action = actionExecuter.getActionDefinition();
		this.actionDefinitions.put(action.getName(), action);
	}
	
	/**
	 * Gets the action node ref from the action id
	 * 
	 * @param nodeRef	the node reference
	 * @param actionId	the acition id
	 * @return			the action node reference
	 */
	private NodeRef getActionNodeRefFromId(NodeRef nodeRef, String actionId)
	{
		NodeRef result = null;
		
		if (this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_ACTIONABLE) == true)
		{
			DynamicNamespacePrefixResolver namespacePrefixResolver = new DynamicNamespacePrefixResolver();
			namespacePrefixResolver.registerNamespace(NamespaceService.SYSTEM_MODEL_PREFIX, NamespaceService.SYSTEM_MODEL_1_0_URI);
			
			List<NodeRef> nodeRefs = searchService.selectNodes(
					getSavedActionFolderRef(nodeRef),
					"*[@sys:" + ContentModel.PROP_NODE_UUID.getLocalName() + "='" + actionId + "']",
					null,
					namespacePrefixResolver,
					false);
			if (nodeRefs.size() != 0)
			{
				result = nodeRefs.get(0);
			}
		}
		
		return result;
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#saveAction(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.action.Action)
	 */
	public void saveAction(NodeRef nodeRef, Action action)
	{
		NodeRef actionNodeRef = getActionNodeRefFromId(nodeRef, action.getId());
		if (actionNodeRef == null)
		{		
			if (this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_ACTIONABLE) == false)
			{
				// Apply the actionable aspect
				this.nodeService.addAspect(nodeRef, ContentModel.ASPECT_ACTIONABLE, null);
			}
				
			Map<QName, Serializable> props = new HashMap<QName, Serializable>(2);
			props.put(ContentModel.PROP_DEFINITION_NAME, action.getActionDefinitionName());
			props.put(ContentModel.PROP_NODE_UUID, action.getId());
			
			QName actionType = ContentModel.TYPE_ACTION;
			if(action instanceof CompositeAction)
			{
				actionType = ContentModel.TYPE_COMPOSITE_ACTION;
			}
			
			// Create the action node
			actionNodeRef = this.nodeService.createNode(
					getSavedActionFolderRef(nodeRef),
					ContentModel.ASSOC_SAVED_ACTIONS,
					ContentModel.ASSOC_SAVED_ACTIONS,
					actionType,
					props).getChildRef();
			
			// Update the created details
			((ActionImpl)action).setCreator((String)this.nodeService.getProperty(actionNodeRef, ContentModel.PROP_CREATOR));
			((ActionImpl)action).setCreatedDate((Date)this.nodeService.getProperty(actionNodeRef, ContentModel.PROP_CREATED));
		}
		
		saveActionImpl(actionNodeRef, action);
	}
	
	/**
	 * @see org.alfresco.repo.action.RuntimeActionService#saveActionImpl(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.action.Action)
	 */
	public void saveActionImpl(NodeRef actionNodeRef, Action action)
	{
		// Save action properties
		saveActionProperties(actionNodeRef, action);
		
		// Update the parameters of the action
		saveParameters(actionNodeRef, action);
		
		// Update the conditions of the action
		saveConditions(actionNodeRef, action);
		
		if (action instanceof CompositeAction)
		{
			// Update composite action
			saveActions(actionNodeRef, (CompositeAction)action);
		}
		
		// Update the modified details
		((ActionImpl)action).setModifier((String)this.nodeService.getProperty(actionNodeRef, ContentModel.PROP_MODIFIER));
		((ActionImpl)action).setModifiedDate((Date)this.nodeService.getProperty(actionNodeRef, ContentModel.PROP_MODIFIED));
	}

	/**
	 * Save the action property values
	 * 
	 * @param actionNodeRef	the action node reference
	 * @param action		the action
	 */
	private void saveActionProperties(NodeRef actionNodeRef, Action action)
	{
		// Update the action property values
		Map<QName, Serializable> props = this.nodeService.getProperties(actionNodeRef);
		props.put(ContentModel.PROP_ACTION_TITLE, action.getTitle());
		props.put(ContentModel.PROP_ACTION_DESCRIPTION, action.getDescription());
		props.put(ContentModel.PROP_EXECUTE_ASYNCHRONOUSLY, action.getExecuteAsychronously());
		this.nodeService.setProperties(actionNodeRef, props);
		
		// Update the compensating action (model should enforce the singularity of this association)
		Action compensatingAction = action.getCompensatingAction();
		List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(actionNodeRef, ContentModel.ASSOC_COMPENSATING_ACTION);
		if (assocs.size() == 0)
		{
			if (compensatingAction != null)
			{
				Map<QName, Serializable> props2 = new HashMap<QName, Serializable>(2);
				props2.put(ContentModel.PROP_DEFINITION_NAME, compensatingAction.getActionDefinitionName());
				props2.put(ContentModel.PROP_NODE_UUID, compensatingAction.getId());
				
				NodeRef compensatingActionNodeRef = this.nodeService.createNode(
						actionNodeRef,
	                    ContentModel.ASSOC_COMPENSATING_ACTION,
	                    ContentModel.ASSOC_COMPENSATING_ACTION,
	                    ContentModel.TYPE_ACTION,
	                    props2).getChildRef();
				
				saveActionImpl(compensatingActionNodeRef, compensatingAction);
			}
		}
		else
		{
			ChildAssociationRef assoc = assocs.get(0);
			if (compensatingAction == null)
			{
				this.nodeService.removeChild(actionNodeRef, assoc.getChildRef());
			}
			else
			{
				saveActionImpl(assoc.getChildRef(), compensatingAction);
			}
		}
	}

	/**
	 * Save the actions of a composite action
	 * 
	 * @param compositeActionNodeRef	the node reference of the coposite action
	 * @param compositeAction			the composite action
	 */
	private void saveActions(NodeRef compositeActionNodeRef, CompositeAction compositeAction)
	{
		// TODO Need a way of sorting the order of the actions

		Map<String, Action> idToAction = new HashMap<String, Action>();
		for (Action action : compositeAction.getActions())
		{	
			idToAction.put(action.getId(), action);
		}
		
		List<ChildAssociationRef> actionRefs = this.nodeService.getChildAssocs(compositeActionNodeRef, ContentModel.ASSOC_ACTIONS);
		for (ChildAssociationRef actionRef : actionRefs)
		{
			NodeRef actionNodeRef = actionRef.getChildRef();
			if (idToAction.containsKey(actionNodeRef.getId()) == false)
			{
				// Delete the action
				this.nodeService.removeChild(compositeActionNodeRef, actionNodeRef);
			}
			else
			{
				// Update the action
				saveActionImpl(actionNodeRef, idToAction.get(actionNodeRef.getId()));
				idToAction.remove(actionNodeRef.getId());
			}
			
		}
		
		// Create the actions remaining
		for (Map.Entry<String, Action> entry : idToAction.entrySet())
		{
			Map<QName, Serializable> props = new HashMap<QName, Serializable>(2);
			props.put(ContentModel.PROP_DEFINITION_NAME, entry.getValue().getActionDefinitionName());
			props.put(ContentModel.PROP_NODE_UUID, entry.getValue().getId());
			
			NodeRef actionNodeRef = this.nodeService.createNode(
					compositeActionNodeRef,
                    ContentModel.ASSOC_ACTIONS,
                    ContentModel.ASSOC_ACTIONS,
                    ContentModel.TYPE_ACTION,
					props).getChildRef();
			
			saveActionImpl(actionNodeRef, entry.getValue());
		}
	}

	/**
	 * Saves the conditions associated with an action
	 * 
	 * @param actionNodeRef		the action node reference
	 * @param action			the action
	 */
	private void saveConditions(NodeRef actionNodeRef, Action action)
	{
		// TODO Need a way of sorting out the order of the conditions

		Map<String, ActionCondition> idToCondition = new HashMap<String, ActionCondition>();
		for (ActionCondition actionCondition : action.getActionConditions())
		{	
			idToCondition.put(actionCondition.getId(), actionCondition);
		}
		
		List<ChildAssociationRef> conditionRefs = this.nodeService.getChildAssocs(actionNodeRef, ContentModel.ASSOC_CONDITIONS);
		for (ChildAssociationRef conditionRef : conditionRefs)
		{
			NodeRef conditionNodeRef = conditionRef.getChildRef();
			if (idToCondition.containsKey(conditionNodeRef.getId()) == false)
			{
				// Delete the condition
				this.nodeService.removeChild(actionNodeRef, conditionNodeRef);
			}
			else
			{
				// Update the conditions parameters
				saveParameters(conditionNodeRef, idToCondition.get(conditionNodeRef.getId()));
				idToCondition.remove(conditionNodeRef.getId());
			}
			
		}
		
		// Create the conditions remaining
		for (Map.Entry<String, ActionCondition> entry : idToCondition.entrySet())
		{
			Map<QName, Serializable> props = new HashMap<QName, Serializable>(2);
			props.put(ContentModel.PROP_DEFINITION_NAME, entry.getValue().getActionConditionDefinitionName());
			props.put(ContentModel.PROP_NODE_UUID, entry.getValue().getId());
			
			NodeRef conditionNodeRef = this.nodeService.createNode(
					actionNodeRef,
                    ContentModel.ASSOC_CONDITIONS,
                    ContentModel.ASSOC_CONDITIONS,
                    ContentModel.TYPE_ACTION_CONDITION,
					props).getChildRef();
			
			saveParameters(conditionNodeRef, entry.getValue());
		}		
	}

	/**
	 * Saves the parameters associated with an action or condition
	 * 
	 * @param parameterizedNodeRef	the parameterized item node reference
	 * @param item					the parameterized item
	 */
	private void saveParameters(NodeRef parameterizedNodeRef, ParameterizedItem item)
	{
		Map<String, Serializable> parameterMap = new HashMap<String, Serializable>();
		parameterMap.putAll(item.getParameterValues());
		
		List<ChildAssociationRef> parameters = this.nodeService.getChildAssocs(parameterizedNodeRef, ContentModel.ASSOC_PARAMETERS);
		for (ChildAssociationRef ref : parameters)
		{
			NodeRef paramNodeRef = ref.getChildRef();
			Map<QName, Serializable> nodeRefParameterMap = this.nodeService.getProperties(paramNodeRef);
			String paramName = (String)nodeRefParameterMap.get(ContentModel.PROP_PARAMETER_NAME);
			if (parameterMap.containsKey(paramName) == false)
			{
				// Delete parameter from node ref
				this.nodeService.removeChild(parameterizedNodeRef, paramNodeRef);				
			}
			else
			{
				// Update the parameter value
				nodeRefParameterMap.put(ContentModel.PROP_PARAMETER_VALUE, parameterMap.get(paramName));
				this.nodeService.setProperties(paramNodeRef, nodeRefParameterMap);
				parameterMap.remove(paramName);
			}
		}
		
		// Add any remaing parameters
		for (Map.Entry<String, Serializable> entry : parameterMap.entrySet())
		{
			Map<QName, Serializable> nodeRefProperties = new HashMap<QName, Serializable>(2);
			nodeRefProperties.put(ContentModel.PROP_PARAMETER_NAME, entry.getKey());
			nodeRefProperties.put(ContentModel.PROP_PARAMETER_VALUE, entry.getValue());
			
			this.nodeService.createNode(
					parameterizedNodeRef,
                    ContentModel.ASSOC_PARAMETERS,
                    ContentModel.ASSOC_PARAMETERS,
                    ContentModel.TYPE_ACTION_PARAMETER,
					nodeRefProperties);
		}
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#getActions(org.alfresco.service.cmr.repository.NodeRef)
	 */
	public List<Action> getActions(NodeRef nodeRef)
	{
		List<Action> result = new ArrayList<Action>();
		
		if (this.nodeService.exists(nodeRef) == true &&
			this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_ACTIONABLE) == true)
		{
			List<ChildAssociationRef> actions = this.nodeService.getChildAssocs(getSavedActionFolderRef(nodeRef), ContentModel.ASSOC_SAVED_ACTIONS);
			for (ChildAssociationRef action : actions)
			{
				NodeRef actionNodeRef = action.getChildRef();
				result.add(createAction(actionNodeRef));
			}
		}
		
		return result;
	}

	/**
	 * Create an action from the action node reference
	 * 
	 * @param actionNodeRef		the action node reference
	 * @return					the action
	 */
	private Action createAction(NodeRef actionNodeRef)
	{
		Action result = null;
		
		Map<QName, Serializable> properties = this.nodeService.getProperties(actionNodeRef);
		
		QName actionType = this.nodeService.getType(actionNodeRef);
		if (ContentModel.TYPE_COMPOSITE_ACTION.equals(actionType) == true)
		{
			// Create a composite action
			result = new CompositeActionImpl(actionNodeRef.getId());
			populateCompositeAction(actionNodeRef, (CompositeAction)result);
		}
		else
		{
			// Create an action
			result = new ActionImpl(actionNodeRef.getId(), (String)properties.get(ContentModel.PROP_DEFINITION_NAME));
			populateAction(actionNodeRef, result);
		}
		
		return result;
	}

	/**
	 * Populate the details of the action from the node reference
	 * 
	 * @param actionNodeRef		the action node reference
	 * @param action			the action
	 */
	private void populateAction(NodeRef actionNodeRef, Action action)
	{
		// Populate the action properties
		populateActionProperties(actionNodeRef, action);
		
		// Set the parameters
		populateParameters(actionNodeRef, action);
		
		// Set the conditions
		List<ChildAssociationRef> conditions = this.nodeService.getChildAssocs(actionNodeRef, ContentModel.ASSOC_CONDITIONS);
		for (ChildAssociationRef condition : conditions)
		{
			NodeRef conditionNodeRef = condition.getChildRef();
			action.addActionCondition(createActionCondition(conditionNodeRef));
		}
	}

	/**
	 * Populates the action properties from the node reference
	 * 	
	 * @param actionNodeRef	the action node reference
	 * @param action		the action
	 */
	private void populateActionProperties(NodeRef actionNodeRef, Action action)
	{
		Map<QName, Serializable> props = this.nodeService.getProperties(actionNodeRef);
		
		action.setTitle((String)props.get(ContentModel.PROP_ACTION_TITLE));
		action.setDescription((String)props.get(ContentModel.PROP_ACTION_DESCRIPTION));
		action.setExecuteAsynchronously(((Boolean)props.get(ContentModel.PROP_EXECUTE_ASYNCHRONOUSLY)).booleanValue());	
		
		((ActionImpl)action).setCreator((String)props.get(ContentModel.PROP_CREATOR));
		((ActionImpl)action).setCreatedDate((Date)props.get(ContentModel.PROP_CREATED));
		((ActionImpl)action).setModifier((String)props.get(ContentModel.PROP_MODIFIER));
		((ActionImpl)action).setModifiedDate((Date)props.get(ContentModel.PROP_MODIFIED));
		
		// Get the compensating action
		List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(actionNodeRef, ContentModel.ASSOC_COMPENSATING_ACTION);
		if (assocs.size() != 0)
		{
			Action compensatingAction = createAction(assocs.get(0).getChildRef());
			action.setCompensatingAction(compensatingAction);
		}
	}

	/**
	 * Populate the parameteres of a parameterized item from the parameterized item node reference
	 * 
	 * @param parameterizedItemNodeRef	the parameterized item node reference
	 * @param parameterizedItem			the parameterized item
	 */
	private void populateParameters(NodeRef parameterizedItemNodeRef, ParameterizedItem parameterizedItem)
	{
		List<ChildAssociationRef> parameters = this.nodeService.getChildAssocs(parameterizedItemNodeRef, ContentModel.ASSOC_PARAMETERS);
		for (ChildAssociationRef parameter : parameters)
		{
			NodeRef parameterNodeRef = parameter.getChildRef();
			Map<QName, Serializable> properties = this.nodeService.getProperties(parameterNodeRef);
			parameterizedItem.setParameterValue(
					(String)properties.get(ContentModel.PROP_PARAMETER_NAME),
					properties.get(ContentModel.PROP_PARAMETER_VALUE));
		}
	}
	
	/**
	 * Creates an action condition from an action condition node reference
	 * 
	 * @param conditionNodeRef	the condition node reference
	 * @return					the action condition
	 */
	private ActionCondition createActionCondition(NodeRef conditionNodeRef)
	{
		Map<QName, Serializable> properties = this.nodeService.getProperties(conditionNodeRef);
		ActionCondition condition = new ActionConditionImpl(conditionNodeRef.getId(), (String)properties.get(ContentModel.PROP_DEFINITION_NAME));
		populateParameters(conditionNodeRef, condition);
		return condition;
	}

	/**
	 * Populates a composite action from a composite action node reference
	 * 
	 * @param compositeNodeRef	the composite action node reference
	 * @param compositeAction	the composite action
	 */
	public void populateCompositeAction(NodeRef compositeNodeRef, CompositeAction compositeAction)
	{
		populateAction(compositeNodeRef, compositeAction);
		
		List<ChildAssociationRef> actions = this.nodeService.getChildAssocs(compositeNodeRef, ContentModel.ASSOC_ACTIONS);
		for (ChildAssociationRef action : actions)
		{
			NodeRef actionNodeRef = action.getChildRef();
			compositeAction.addAction(createAction(actionNodeRef));
		}		
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#getAction(org.alfresco.service.cmr.repository.NodeRef, java.lang.String)
	 */
	public Action getAction(NodeRef nodeRef, String actionId)
	{
		Action result = null;
		
		if (this.nodeService.exists(nodeRef) == true &&
			this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_ACTIONABLE) == true)
		{
			NodeRef actionNodeRef = getActionNodeRefFromId(nodeRef, actionId);
			if (actionNodeRef != null)
			{
				result = createAction(actionNodeRef);
			}
		}
		
		return result;
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#removeAction(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.action.Action)
	 */
	public void removeAction(NodeRef nodeRef, Action action)
	{
		if (this.nodeService.exists(nodeRef) == true &&
			this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_ACTIONABLE) == true)
		{
			NodeRef actionNodeRef = getActionNodeRefFromId(nodeRef, action.getId());
			if (actionNodeRef != null)
			{
				this.nodeService.removeChild(getSavedActionFolderRef(nodeRef), actionNodeRef);
			}
		}		
	}

	/**
	 * @see org.alfresco.service.cmr.action.ActionService#removeAllActions(org.alfresco.service.cmr.repository.NodeRef)
	 */
	public void removeAllActions(NodeRef nodeRef)
	{
		if (this.nodeService.exists(nodeRef) == true &&
			this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_ACTIONABLE) == true)
		{
			List<ChildAssociationRef> actions = new ArrayList<ChildAssociationRef>(this.nodeService.getChildAssocs(getSavedActionFolderRef(nodeRef), ContentModel.ASSOC_SAVED_ACTIONS));
			for (ChildAssociationRef action : actions)
			{
				this.nodeService.removeChild(getSavedActionFolderRef(nodeRef), action.getChildRef());
			}
		}		
	}
	
	/**
	 * @see org.alfresco.service.cmr.action.ActionService#getActionExecutionHistory(org.alfresco.service.cmr.repository.NodeRef)
	 */
	public List<ActionExecutionDetails> getActionExecutionHistory(NodeRef nodeRef)
	{
		List<ActionExecutionDetails> result = new ArrayList<ActionExecutionDetails>();
		
		if (this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_ACTION_EXECUTION_HISTORY) == true)
		{
			List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(nodeRef, ContentModel.ASSOC_ACTION_EXECUTION_DETAILS);
			for (ChildAssociationRef assoc : assocs)
			{
				NodeRef actionExecutionDetailsNodeRef = assoc.getChildRef();
				result.add(getActionExecutionDetails(actionExecutionDetailsNodeRef));
			}
		}
		
		return result;
	}
	
	/**
	 * Create an action executions details object from the corresponding node reference
	 * 
	 * @param actionExecutionDetailsNodeRef		the action execution details node reference
	 * @return									the action executions details object
	 */
	private ActionExecutionDetails getActionExecutionDetails(NodeRef actionExecutionDetailsNodeRef)
	{
		Map<QName, Serializable> props = this.nodeService.getProperties(actionExecutionDetailsNodeRef);
		
		ActionExecutionDetailsImpl executionDetails = new ActionExecutionDetailsImpl();
		
		// Set the basic properties
		executionDetails.setTitle((String)props.get(ContentModel.PROP_ACTION_EXECUTION_TITLE));
		executionDetails.setExecutionStatus(ActionExecutionStatus.valueOf((String)props.get(ContentModel.PROP_EXECUTION_STATUS)));
		
		// Set the error details if present
		String errorMessage = (String)props.get(ContentModel.PROP_ERROR_MESSAGE);
		if (errorMessage != null)
		{
			executionDetails.setErrorMessage(errorMessage);
		}
		String errorDetails = (String)props.get(ContentModel.PROP_ERROR_DETAILS);
		if (errorDetails != null)
		{
			executionDetails.setErrorDetails(errorDetails);
		}
		
		// Set the action reference
		// TODO need to get the reference to the action node ref and convert into an Action object (if not null)
		
		return executionDetails;
	}

	/**
	 * 
	 * @param action
	 * @param actionedUponNodeRef
	 * @return
	 */
	private NodeRef createActionExecutionDetails(final Action action, final NodeRef actionedUponNodeRef)
	{
		TransactionUtil.TransactionWork<NodeRef> work = new TransactionUtil.TransactionWork<NodeRef>() 
		{
			public NodeRef doWork()
			{
				NodeRef actionExecutionDetailsNodeRef = null;
				
				if (ActionServiceImpl.this.nodeService.exists(actionedUponNodeRef) == true)
				{				
					if (ActionServiceImpl.this.nodeService.hasAspect(actionedUponNodeRef, ContentModel.ASPECT_ACTION_EXECUTION_HISTORY) == false)
					{
						ActionServiceImpl.this.nodeService.addAspect(actionedUponNodeRef, ContentModel.ASPECT_ACTION_EXECUTION_HISTORY, null);
					}
					
					Map<QName, Serializable> props = new HashMap<QName, Serializable>();
					props.put(ContentModel.PROP_ACTION_EXECUTION_TITLE, action.getTitle());
					props.put(ContentModel.PROP_EXECUTION_STATUS, ActionExecutionStatus.PENDING.toString());
							
					actionExecutionDetailsNodeRef = ActionServiceImpl.this.nodeService.createNode(
							actionedUponNodeRef, 
							ContentModel.ASSOC_ACTION_EXECUTION_DETAILS, 
							ContentModel.ASSOC_ACTION_EXECUTION_DETAILS,
							ContentModel.TYPE_ACTION_EXECUTION_DETAILS,
							props).getChildRef();
				}
				
				return actionExecutionDetailsNodeRef;
				
			}					
		};
		
		NodeRef actionExecutionDetailsNodeRef = TransactionUtil.executeInNonPropagatingUserTransaction(
				this.transactionService,
				work);
		
		if (actionExecutionDetailsNodeRef == null)
		{
			// Re-do the work in this transaction (since the node is also in this transaction)
			actionExecutionDetailsNodeRef = work.doWork();
		}
		
		return actionExecutionDetailsNodeRef;
	}
	
	/**
	 * 
	 * @param executionStatus
	 * @param actionExecutionDetailsNodeRef
	 */
	private void updateExecutionStatus(final ActionExecutionStatus executionStatus, final NodeRef actionExecutionDetailsNodeRef)
	{
		if (actionExecutionDetailsNodeRef != null)
		{		
			TransactionUtil.TransactionWork<Boolean> work = new TransactionUtil.TransactionWork<Boolean>() 
			{
				public Boolean doWork()
				{
					boolean result = false;
					if (ActionServiceImpl.this.nodeService.exists(actionExecutionDetailsNodeRef) == true)
					{
						ActionServiceImpl.this.nodeService.setProperty(
								actionExecutionDetailsNodeRef, 
								ContentModel.PROP_EXECUTION_STATUS, 
								executionStatus.toString());
						result = true;
					}
					return Boolean.valueOf(result);
				}					
			};
			
			boolean result = (TransactionUtil.executeInNonPropagatingUserTransaction(
					this.transactionService,
					work)).booleanValue();
			
			if (result == false)
			{
				// Re-do the work in this transaction (since the node is also in this transaction)
				work.doWork();
			}
		}
	}
	
	private void updateExecutionFailure(final Throwable exception, final NodeRef actionExecutionDetailsNodeRef)
	{
		if (actionExecutionDetailsNodeRef != null)
		{		
			TransactionUtil.TransactionWork<Boolean> work = new TransactionUtil.TransactionWork<Boolean>() 
			{
				public Boolean doWork()
				{
					boolean result = false;
					if (ActionServiceImpl.this.nodeService.exists(actionExecutionDetailsNodeRef) == true)
					{
						Map<QName, Serializable> props = ActionServiceImpl.this.nodeService.getProperties(actionExecutionDetailsNodeRef);
						props.put(ContentModel.PROP_EXECUTION_STATUS, ActionExecutionStatus.FAILED.toString());
						props.put(ContentModel.PROP_ERROR_MESSAGE, exception.getMessage());
					
						StringBuilder builder = new StringBuilder(); 
						for (StackTraceElement stackTraceElement : exception.getStackTrace())
						{
							builder.append(stackTraceElement.toString()).append("\n");
						} 
						props.put(ContentModel.PROP_ERROR_DETAILS, builder.toString());
						
						ActionServiceImpl.this.nodeService.setProperties(actionExecutionDetailsNodeRef, props);
						result = true;
					}
					return Boolean.valueOf(result);
				}					
			};
			
			boolean result = (TransactionUtil.executeInNonPropagatingUserTransaction(
					this.transactionService,
					work)).booleanValue();
			
			if (result == false)
			{
				// Re-do the work in this transaction (since the node is also in this transaction)
				work.doWork();
			}
		}
	}
	
	/**
	 * Add a pending action to the list to be queued for execution once the transaction is completed.
	 * 
	 * @param action				the action
	 * @param actionedUponNodeRef	the actioned upon node reference
	 * @param checkConditions		indicates whether to check the conditions before execution
	 */
	@SuppressWarnings("unchecked")
	private void addPostTransactionPendingAction(
			Action action, 
			NodeRef actionedUponNodeRef, 
			boolean checkConditions, 
			NodeRef actionExecutionDetailsNodeRef)
	{
		// Ensure that the transaction listener is bound to the transaction
		AlfrescoTransactionSupport.bindListener(this.transactionListener);
		
		// Add the pending action to the transaction resource
		List<PendingAction> pendingActions = (List<PendingAction>)AlfrescoTransactionSupport.getResource(POST_TRANSACTION_PENDING_ACTIONS);
		if (pendingActions == null)
		{
			pendingActions = new ArrayList<PendingAction>();
			AlfrescoTransactionSupport.bindResource(POST_TRANSACTION_PENDING_ACTIONS, pendingActions);
		}
		
		// Check that action has only been added to the list once
		PendingAction pendingAction = new PendingAction(action, actionedUponNodeRef, checkConditions, actionExecutionDetailsNodeRef);
		if (pendingActions.contains(pendingAction) == false)
		{
			pendingActions.add(pendingAction);
		}		
	}
	
	/**
	 * @see org.alfresco.repo.action.RuntimeActionService#getPostTransactionPendingActions()
	 */
	@SuppressWarnings("unchecked")
	public List<PendingAction> getPostTransactionPendingActions()
	{
		return (List<PendingAction>)AlfrescoTransactionSupport.getResource(POST_TRANSACTION_PENDING_ACTIONS);
	}
	
	/**
	 * Pending action details class
	 */
	public class PendingAction
	{
		/**
		 * The action
		 */
		private Action action;
		
		/**
		 * The actioned upon node reference
		 */
		private NodeRef actionedUponNodeRef;
		
		/**
		 * Indicates whether the conditions should be checked before the action is executed
		 */
		private boolean checkConditions;
		
		/**
		 * Node reference to the action execution detils node
		 */
		private NodeRef actionExecutionDetailsNodeRef;
		
		/**
		 * Constructor 
		 * 
		 * @param action						the action
		 * @param actionedUponNodeRef			the actioned upon node reference
		 * @param checkConditions				indicated whether the conditions need to be checked
		 * @param actionExecutionDetailsNodeRef	the action execution history node reference
		 */
		public PendingAction(Action action, NodeRef actionedUponNodeRef, boolean checkConditions, NodeRef actionExecutionDetailsNodeRef)
		{
			this.action = action;
			this.actionedUponNodeRef = actionedUponNodeRef;
			this.checkConditions = checkConditions;
			this.actionExecutionDetailsNodeRef = actionExecutionDetailsNodeRef;
		}
		
		/**
		 * Get the action
		 * 
		 * @return  the action
		 */
		public Action getAction()
		{
			return action;
		}
		
		/**
		 * Get the actioned upon node reference
		 * 
		 * @return  the actioned upon node reference
		 */
		public NodeRef getActionedUponNodeRef()
		{
			return actionedUponNodeRef;
		}
		
		/**
		 * Get the check conditions value
		 * 
		 * @return	indicates whether the condition should be checked
		 */
		public boolean getCheckConditions()
		{
			return this.checkConditions;
		}
		
		/**
		 * Get the action execution history node reference
		 * 
		 * @return	the action execution history node reference
		 */
		public NodeRef getActionExecutionDetailsNodeRef()
		{
			return actionExecutionDetailsNodeRef;
		}
		
		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			int hashCode = 37 * this.actionedUponNodeRef.hashCode(); 
			hashCode += 37 * this.action.hashCode();
			return hashCode;
		}
		
		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
	        {
	            return true;
	        }
	        if (obj instanceof PendingAction)
	        {
	        	PendingAction that = (PendingAction) obj;
	            return (this.action.equals(that.action) && this.actionedUponNodeRef.equals(that.actionedUponNodeRef));
	        }
	        else
	        {
	            return false;
	        }
		}
	}
}
