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
package org.alfresco.web.bean.wizard;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.transaction.UserTransaction;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.evaluator.InCategoryEvaluator;
import org.alfresco.repo.action.evaluator.MatchTextEvaluator;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.action.ActionConditionDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.rule.RuleType;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.RulesBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.data.QuickSort;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handler class used by the New Space Wizard 
 * 
 * @author gavinc
 */
public class NewRuleWizard extends BaseActionWizard
{
   // parameter names for conditions
   public static final String PROP_CONTAINS_TEXT = "containstext";
   
   private static Log logger = LogFactory.getLog(NewRuleWizard.class);
   
   private static final String ERROR = "error_rule";
   
   // TODO: retrieve these from the config service
   private static final String WIZARD_TITLE = "New Rule Wizard";
   private static final String WIZARD_TITLE_EDIT = "Edit Rule Wizard";
   private static final String WIZARD_DESC = "This wizard helps you create a new rule.";
   private static final String WIZARD_DESC_EDIT = "This wizard helps you modify a rule.";
   private static final String STEP1_TITLE = "Step One - Enter Details";
   private static final String STEP2_TITLE = "Step Two - Select ActionCondition";
   private static final String STEP3_TITLE = "Step Three - ActionCondition Settings";
   private static final String STEP4_TITLE = "Step Four - Select Action";
   private static final String STEP5_TITLE = "Step Five - Action Settings";
   private static final String FINISH_INSTRUCTION = "To create the rule click Finish.";
   private static final String FINISH_INSTRUCTION_EDIT = "To update the rule click Finish.";
   
   // new rule wizard specific properties
   private String title;
   private String description;
   private String type;
   private String condition;
   private RuleService ruleService;
   private RulesBean rulesBean;
   private List<SelectItem> types;
   private List<SelectItem> conditions;
   private Map<String, String> conditionDescriptions;
   private Map<String, String> conditionProperties;
   
   /**
    * Deals with the finish button being pressed
    * 
    * @return outcome
    */
   public String finish()
   {
      String outcome = FINISH_OUTCOME;
      
      UserTransaction tx = null;
   
      try
      {
         tx = Repository.getUserTransaction(FacesContext.getCurrentInstance());
         tx.begin();
         
         // set up parameters maps for the condition
         Map<String, Serializable> conditionParams = new HashMap<String, Serializable>();
         if (this.condition.equals(MatchTextEvaluator.NAME))
         {
            conditionParams.put(MatchTextEvaluator.PARAM_TEXT, 
                  this.conditionProperties.get(PROP_CONTAINS_TEXT));
         }
         else if (this.condition.equals(InCategoryEvaluator.NAME))
         {
            // put the selected category in the condition params
            NodeRef catNodeRef = new NodeRef(Repository.getStoreRef(), 
                  this.conditionProperties.get(PROP_CATEGORY));
            conditionParams.put(InCategoryEvaluator.PARAM_CATEGORY_VALUE, catNodeRef);
            
            // add the classifiable aspect
            conditionParams.put(InCategoryEvaluator.PARAM_CATEGORY_ASPECT, ContentModel.ASPECT_GEN_CLASSIFIABLE);
         }
         
         // build the action params map based on the selected action instance
         Map<String, Serializable> actionParams = buildActionParams();
         
         // get hold of the space the rule will apply to and make sure
         // it is actionable
         Node currentSpace = browseBean.getActionSpace();
         
         Rule rule = null;
         
         if (this.editMode)
         {
            // update the existing rule in the repository
            rule = this.rulesBean.getCurrentRule();
            
            // we know there is only one condition and action
            // so remove the first one
            rule.removeActionCondition(rule.getActionConditions().get(0));
            rule.removeAction(rule.getActions().get(0));
         }
         else
         {
            rule = this.ruleService.createRule(this.getType());
         }

         // setup the rule and add it to the space
         rule.setTitle(this.title);
         rule.setDescription(this.description);
         
         // Add the action to the rule
         Action action = this.actionService.createAction(this.getAction());
         action.setParameterValues(actionParams);
         rule.addAction(action);
         
         // Add the condition to the rule
         ActionCondition condition = this.actionService.createActionCondition(this.getCondition());
         condition.setParameterValues(conditionParams);
         rule.addActionCondition(condition);
         
         // Save the rule
         this.ruleService.saveRule(currentSpace.getNodeRef(), rule);
         
         if (logger.isDebugEnabled())
         {
            logger.debug(this.editMode ? "Updated" : "Added" 
                         + " rule '" + this.title + "' with condition '" + 
                         this.condition + "', action '" + this.action + 
                         "', condition params of " +
                         this.conditionProperties + " and action params of " + 
                         this.actionProperties);
         }
         
         // commit the transaction
         tx.commit();
      }
      catch (Exception e)
      {
         // rollback the transaction
         try { if (tx != null) {tx.rollback();} } catch (Exception ex) {}
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), ERROR), e.getMessage()), e);
         outcome = null;
      }
      
      return outcome;
   }

   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#next()
    */
   public String next()
   {
      String outcome = super.next();
      
      // if the outcome is "no-condition" we must move the step counter
      // on as there are no settings for "no-condition"
      if (outcome.equals("no-condition"))
      {
         this.currentStep++;
         
         if (logger.isDebugEnabled())
            logger.debug("current step is now " + this.currentStep + 
                         " as there are no settings associated with the selected condition");
      }
      
      return outcome;
   }
   
   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#back()
    */
   public String back()
   {
      String outcome = super.back();
      
      // if the outcome is "no-condition" we must move the step counter
      // back as there are no settings for "no-condition"
      if (outcome.equals("no-condition"))
      {
         this.currentStep--;
         
         if (logger.isDebugEnabled())
            logger.debug("current step is now " + this.currentStep + 
                         " as there are no settings associated with the selected condition");
      }
      
      return outcome;
   }

   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getWizardDescription()
    */
   public String getWizardDescription()
   {
      if (this.editMode)
      {
         return WIZARD_DESC_EDIT;
      }
      else
      {
         return WIZARD_DESC;
      }
   }

   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getWizardTitle()
    */
   public String getWizardTitle()
   {
      if (this.editMode)
      {
         return WIZARD_TITLE_EDIT;
      }
      else
      {
         return WIZARD_TITLE;
      }
   }
   
   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getStepDescription()
    */
   public String getStepDescription()
   {
      String stepDesc = null;
      
      switch (this.currentStep)
      {
         case 6:
         {
            stepDesc = SUMMARY_DESCRIPTION;
            break;
         }
         default:
         {
            stepDesc = "";
         }
      }
      
      return stepDesc;
   }

   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getStepTitle()
    */
   public String getStepTitle()
   {
      String stepTitle = null;
      
      switch (this.currentStep)
      {
         case 1:
         {
            stepTitle = STEP1_TITLE;
            break;
         }
         case 2:
         {
            stepTitle = STEP2_TITLE;
            break;
         }
         case 3:
         {
            stepTitle = STEP3_TITLE;
            break;
         }
         case 4:
         {
            stepTitle = STEP4_TITLE;
            break;
         }
         case 5:
         {
            stepTitle = STEP5_TITLE;
            break;
         }
         case 6:
         {
            stepTitle = SUMMARY_TITLE;
            break;
         }
         default:
         {
            stepTitle = "";
         }
      }
      
      return stepTitle;
   }
   
   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#getStepInstructions()
    */
   public String getStepInstructions()
   {
      String stepInstruction = null;
      
      switch (this.currentStep)
      {
         case 5:
         {
            if (this.editMode)
            {
               stepInstruction = FINISH_INSTRUCTION_EDIT;
            }
            else
            {
               stepInstruction = FINISH_INSTRUCTION;
            }
            break;
         }
         case 6:
         {
            if (this.editMode)
            {
               stepInstruction = FINISH_INSTRUCTION_EDIT;
            }
            else
            {
               stepInstruction = FINISH_INSTRUCTION;
            }
            break;
         }
         default:
         {
            stepInstruction = DEFAULT_INSTRUCTION;
         }
      }
      
      return stepInstruction;
   }
   
   /**
    * Initialises the wizard
    */
   public void init()
   {
      super.init();
      
      this.title = null;
      this.description = null;
      this.type = "inbound";
      this.condition = "no-condition";
      
      if (this.conditions != null)
      {
         this.conditions.clear();
         this.conditions = null;
      }
      
      if (this.conditionDescriptions != null)
      {
         this.conditionDescriptions.clear();
         this.conditionDescriptions = null;
      }
      
      this.conditionProperties = new HashMap<String, String>(1);
   }
   
   /**
    * Sets the context of the rule up before performing the 
    * standard wizard editing steps
    *  
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#startWizardForEdit(javax.faces.event.ActionEvent)
    */
   public void startWizardForEdit(ActionEvent event)
   {
      // setup context for rule to be edited
      this.rulesBean.setupRuleAction(event);
      
      // perform the usual edit processing
      super.startWizardForEdit(event);
   }

   /**
    * Populates the values of the backing bean ready for editing the rule
    * 
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#populate()
    */
   public void populate()
   {
      // get hold of the current rule details
      Rule rule = this.rulesBean.getCurrentRule();
      
      if (rule == null)
      {
         throw new AlfrescoRuntimeException("Failed to locate the current rule");
      }
      
      // populate the bean with current values 
      this.type = rule.getRuleTypeName();
      this.title = rule.getTitle();
      this.description = rule.getDescription();
      // we know there is only 1 condition and action
      this.condition = rule.getActionConditions().get(0).getActionConditionDefinitionName();
      this.action = rule.getActions().get(0).getActionDefinitionName();
      
      // populate the condition property bag with the relevant values
      Map<String, Serializable> condProps = rule.getActionConditions().get(0).getParameterValues();
      if (this.condition.equals(MatchTextEvaluator.NAME))
      {
         this.conditionProperties.put(PROP_CONTAINS_TEXT, 
               (String)condProps.get(MatchTextEvaluator.PARAM_TEXT));
      }
      else if (this.condition.equals(InCategoryEvaluator.NAME))
      {
         NodeRef catNodeRef = (NodeRef)condProps.get(InCategoryEvaluator.PARAM_CATEGORY_VALUE);
         this.conditionProperties.put(PROP_CATEGORY, catNodeRef.getId());
      }
      
      // populate the action property bag with the relevant values
      populateActionFromProperties(rule.getActions().get(0).getParameterValues());
   }

   /**
    * @return Returns the summary data for the wizard.
    */
   public String getSummary()
   {
      String summaryCondition = this.actionService.getActionConditionDefinition(
            this.condition).getTitle();
      
      String summaryAction = this.actionService.getActionDefinition(
            this.action).getTitle();
      
      return buildSummary(
            new String[] {"Name", "Description", "ActionCondition", "Action"},
            new String[] {this.title, this.description, summaryCondition, summaryAction});
   }
   
   /**
    * @return Returns the description.
    */
   public String getDescription()
   {
      return description;
   }
   
   /**
    * @param description The description to set.
    */
   public void setDescription(String description)
   {
      this.description = description;
   } 

   /**
    * @return Returns the title.
    */
   public String getTitle()
   {
      return title;
   }
   
   /**
    * @param title The title to set.
    */
   public void setTitle(String title)
   {
      this.title = title;
   }

   /**
    * @return Returns the type.
    */
   public String getType()
   {
      return type;
   }

   /**
    * @param type The type to set
    */
   public void setType(String type)
   {
      this.type = type;
   }

   /**
    * @return Returns the selected condition
    */
   public String getCondition()
   {
      return this.condition;
   }

   /**
    * @param condition Sets the selected condition
    */
   public void setCondition(String condition)
   {
      this.condition = condition;
   }

   /**
    * @param ruleService Sets the rule service to use
    */
   public void setRuleService(RuleService ruleService)
   {
      this.ruleService = ruleService;
   }
   
   /**
    * Sets the RulesBean instance to be used by the wizard in edit mode
    * 
    * @param rulesBean The RulesBean
    */
   public void setRulesBean(RulesBean rulesBean)
   {
      this.rulesBean = rulesBean;
   }

   /**
    * @return Returns the list of selectable conditions
    */
   public List<SelectItem> getConditions()
   {
      if (this.conditions == null)
      {
         List<ActionConditionDefinition> ruleConditions = this.actionService.getActionConditionDefinitions();
         this.conditions = new ArrayList<SelectItem>();
         for (ActionConditionDefinition ruleConditionDef : ruleConditions)
         {
            this.conditions.add(new SelectItem(ruleConditionDef.getName(), 
                  ruleConditionDef.getTitle()));
         }
         
         // make sure the list is sorted by the label
         QuickSort sorter = new QuickSort(this.conditions, "label", true, IDataContainer.SORT_CASEINSENSITIVE);
         sorter.sort();
      }
      
      return this.conditions;
   }
   
   /**
    * @return Returns a map of all the condition descriptions 
    */
   public Map<String, String> getConditionDescriptions()
   {
      if (this.conditionDescriptions == null)
      {
         List<ActionConditionDefinition> ruleConditions = this.actionService.getActionConditionDefinitions();
         this.conditionDescriptions = new HashMap<String, String>();
         for (ActionConditionDefinition ruleConditionDef : ruleConditions)
         {
            this.conditionDescriptions.put(ruleConditionDef.getName(), 
                  ruleConditionDef.getDescription());
         }
      }
      
      return this.conditionDescriptions;
   }

   /**
    * @return Returns the types of rules that can be defined
    */
   public List<SelectItem> getTypes()
   {
      if (this.types == null)
      {
         List<RuleType> ruleTypes = this.ruleService.getRuleTypes();
         this.types = new ArrayList<SelectItem>();
         for (RuleType ruleType : ruleTypes)
         {
            this.types.add(new SelectItem(ruleType.getName(), ruleType.getDisplayLabel()));
         }
      }
      
      return this.types;
   }

   /**
    * @return Gets the condition settings 
    */
   public Map<String, String> getConditionProperties()
   {
      return this.conditionProperties;
   }
   
   /**
    * @see org.alfresco.web.bean.wizard.AbstractWizardBean#determineOutcomeForStep(int)
    */
   protected String determineOutcomeForStep(int step)
   {
      String outcome = null;
      
      switch(step)
      {
         case 1:
         {
            outcome = "details";
            break;
         }
         case 2:
         {
            outcome = "condition";
            break;
         }
         case 3:
         {
            outcome = this.condition;
            break;
         }
         case 4:
         {
            outcome = "action";
            break;
         }
         case 5:
         {
            outcome = this.action;
            break;
         }
         case 6:
         {
            outcome = "summary";
            break;
         }
         default:
         {
            outcome = CANCEL_OUTCOME;
         }
      }
      
      return outcome;
   }   
}
