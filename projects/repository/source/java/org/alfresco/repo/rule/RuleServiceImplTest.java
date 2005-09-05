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
package org.alfresco.repo.rule;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleType;
import org.alfresco.service.namespace.QName;


/**
 * Rule service implementation test
 * 
 * @author Roy Wetherall
 */
public class RuleServiceImplTest extends BaseRuleTest
{    
   
    /**
     * Test get rule type
     */
    public void testGetRuleType()
    {
        List<RuleType> ruleTypes = this.ruleService.getRuleTypes();
        assertNotNull(ruleTypes);  
        
        // Visual check to make sure that the display labels are being returned correctly
        for (RuleType type : ruleTypes)
		{
			System.out.println(type.getDisplayLabel());
		}
    }
    
    /**
     * Test createRule
     */
    public void testCreateRule()
    {
        Rule newRule = this.ruleService.createRule("ruleType1");
        assertNotNull(newRule);
        assertNotNull(newRule.getId());
        assertEquals("ruleType1", newRule.getRuleTypeName());
    }
    
    /**
     * Test addRule
     *
     */
    public void testAddRule()
    {
        Rule newRule = createTestRule();        
        this.ruleService.saveRule(this.nodeRef, newRule);        
    }
    
    public void testRemoveAllRules()
    {
        this.ruleService.removeAllRules(this.nodeRef);
        List<Rule> rules1 = this.ruleService.getRules(this.nodeRef);
        assertNotNull(rules1);
        assertEquals(0, rules1.size());
        
        Rule newRule = this.ruleService.createRule(ruleType.getName());        
        this.ruleService.saveRule(this.nodeRef, newRule); 
        Rule newRule2 = this.ruleService.createRule(ruleType.getName());
        this.ruleService.saveRule(this.nodeRef, newRule2); 
        
        List<Rule> rules2 = this.ruleService.getRules(this.nodeRef);
        assertNotNull(rules2);
        assertEquals(2, rules2.size());
        
        this.ruleService.removeAllRules(this.nodeRef);
        
        List<Rule> rules3 = this.ruleService.getRules(this.nodeRef);
        assertNotNull(rules3);
        assertEquals(0, rules3.size());
        
    }
    
    /**
     * Test get rules
     */
    public void testGetRules()
    {
        // Check that there are no rules associationed with the node
        List<Rule> noRules = this.ruleService.getRules(this.nodeRef);
        assertNotNull(noRules);
        assertEquals(0, noRules.size());
        
        // Check that we still get nothing back after the details of the node
        // have been cached in the rule store
        List<Rule> noRulesAfterCache = this.ruleService.getRules(this.nodeRef);
        assertNotNull(noRulesAfterCache);
        assertEquals(0, noRulesAfterCache.size());
        
        // Add a rule to the node
        testAddRule();
        
        // Get the rule from the rule service
        List<Rule> rules = this.ruleService.getRules(this.nodeRef);
        assertNotNull(rules);
        assertEquals(1, rules.size());
        
        // Check the details of the rule
        Rule rule = rules.get(0);
        assertEquals("title", rule.getTitle());
        assertEquals("description", rule.getDescription());
        assertNotNull(rule.getCreatedDate());
        assertNotNull(rule.getModifiedDate());
        
        // Check that the condition action have been retireved correctly
        List<ActionCondition> conditions = rule.getActionConditions();
        assertNotNull(conditions);
        assertEquals(1, conditions.size());        
        List<Action> actions = rule.getActions();
        assertNotNull(actions);
        assertEquals(1, actions.size());
    }
    
    /**
     * Test disabling the rules
     */
    public void testRulesDisabled()
    {
        testAddRule();
        assertTrue(this.ruleService.rulesEnabled(this.nodeRef));
        this.ruleService.disableRules(this.nodeRef);
        assertFalse(this.ruleService.rulesEnabled(this.nodeRef));
        this.ruleService.enableRules(this.nodeRef);
        assertTrue(this.ruleService.rulesEnabled(this.nodeRef));
    }
    
    /**
     * Helper method to easily create a new node which can be actionable (or not)
     * 
     * @param parent        the parent node
     * @param isActionable  indicates whether the node is actionable or not
     */
    private NodeRef createNewNode(NodeRef parent, boolean isActionable)
    {
        NodeRef newNodeRef = this.nodeService.createNode(parent,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName("{test}testnode"),
                ContentModel.TYPE_CONTAINER).getChildRef();                
        return newNodeRef;
    }
    
    /**
     * Tests the rule inheritance within the store, checking that the cache is reset correctly when 
     * rules are added and removed.
     */
    public void testRuleInheritance()
    {
        // Create the nodes and rules
        
        NodeRef rootWithRules = createNewNode(this.rootNodeRef, true);
        Rule rule1 = createTestRule();
        this.ruleService.saveRule(rootWithRules, rule1);
        Rule rule2 = createTestRule(true);
        this.ruleService.saveRule(rootWithRules, rule2);
        
        NodeRef nonActionableChild = createNewNode(rootWithRules, false);
        
        NodeRef childWithRules = createNewNode(nonActionableChild, true);
        Rule rule3 = createTestRule();
        this.ruleService.saveRule(childWithRules, rule3);
        Rule rule4 = createTestRule(true);
        this.ruleService.saveRule(childWithRules, rule4);
        
        NodeRef rootWithRules2 = createNewNode(this.rootNodeRef, true);
        this.nodeService.addChild(
                rootWithRules2, 
                childWithRules, 
                ContentModel.ASSOC_CHILDREN,
                QName.createQName("{test}testnode"));
        Rule rule5 = createTestRule();
        this.ruleService.saveRule(rootWithRules2, rule5);
        Rule rule6 = createTestRule(true);
        this.ruleService.saveRule(rootWithRules2, rule6);
                        
        // Check that the rules are inherited in the correct way
        
        List<? extends Rule> allRules = this.ruleService.getRules(childWithRules, true);
        assertNotNull(allRules);
        assertEquals(4, allRules.size());
        assertTrue(allRules.contains(rule2));
        assertTrue(allRules.contains(rule3));
        assertTrue(allRules.contains(rule4));
        assertTrue(allRules.contains(rule6));
        
        List<? extends Rule> myRules = this.ruleService.getRules(childWithRules, false);
        assertNotNull(myRules);
        assertEquals(2, myRules.size());
        assertTrue(myRules.contains(rule3));
        assertTrue(myRules.contains(rule4));
        
        List<? extends Rule> allRules2 = this.ruleService.getRules(nonActionableChild, true);
        assertNotNull(allRules2);
        assertEquals(1, allRules2.size());
        assertTrue(allRules2.contains(rule2));
        
        List<? extends Rule> myRules2 = this.ruleService.getRules(nonActionableChild, false);
        assertNotNull(myRules2);
        assertEquals(0, myRules2.size());
        
        List<? extends Rule> allRules3 = this.ruleService.getRules(rootWithRules, true);
        assertNotNull(allRules3);
        assertEquals(2, allRules3.size());
        assertTrue(allRules3.contains(rule1));
        assertTrue(allRules3.contains(rule2));
        
        List<? extends Rule> myRules3 = this.ruleService.getRules(rootWithRules, false);
        assertNotNull(myRules3);
        assertEquals(2, myRules3.size());
        assertTrue(myRules3.contains(rule1));
        assertTrue(myRules3.contains(rule2));
        
        List<? extends Rule> allRules4 = this.ruleService.getRules(rootWithRules2, true);
        assertNotNull(allRules4);
        assertEquals(2, allRules4.size());
        assertTrue(allRules4.contains(rule5));
        assertTrue(allRules4.contains(rule6));
        
        List<? extends Rule> myRules4 = this.ruleService.getRules(rootWithRules2, false);
        assertNotNull(myRules4);
        assertEquals(2, myRules4.size());
        assertTrue(myRules4.contains(rule5));
        assertTrue(myRules4.contains(rule6));
        
        // Take the root node and add another rule
        
        Rule rule7 = createTestRule(true);
        this.ruleService.saveRule(rootWithRules, rule7);
        
        List<? extends Rule> allRules5 = this.ruleService.getRules(childWithRules, true);
        assertNotNull(allRules5);
        assertEquals(5, allRules5.size());
        assertTrue(allRules5.contains(rule2));
        assertTrue(allRules5.contains(rule3));
        assertTrue(allRules5.contains(rule4));
        assertTrue(allRules5.contains(rule6));
        assertTrue(allRules5.contains(rule7));
        
        List<? extends Rule> allRules6 = this.ruleService.getRules(nonActionableChild, true);
        assertNotNull(allRules6);
        assertEquals(2, allRules6.size());
        assertTrue(allRules6.contains(rule2));
        assertTrue(allRules6.contains(rule7));
        
        List<? extends Rule> allRules7 = this.ruleService.getRules(rootWithRules, true);
        assertNotNull(allRules7);
        assertEquals(3, allRules7.size());
        assertTrue(allRules7.contains(rule1));
        assertTrue(allRules7.contains(rule2));
        assertTrue(allRules7.contains(rule7));
        
        List<? extends Rule> allRules8 = this.ruleService.getRules(rootWithRules2, true);
        assertNotNull(allRules8);
        assertEquals(2, allRules8.size());
        assertTrue(allRules8.contains(rule5));
        assertTrue(allRules8.contains(rule6));
         
        // Take the root node and and remove a rule
        
        this.ruleService.removeRule(rootWithRules, rule7);
        
        List<? extends Rule> allRules9 = this.ruleService.getRules(childWithRules, true);
        assertNotNull(allRules9);
        assertEquals(4, allRules9.size());
        assertTrue(allRules9.contains(rule2));
        assertTrue(allRules9.contains(rule3));
        assertTrue(allRules9.contains(rule4));
        assertTrue(allRules9.contains(rule6));
        
        List<? extends Rule> allRules10 = this.ruleService.getRules(nonActionableChild, true);
        assertNotNull(allRules10);
        assertEquals(1, allRules10.size());
        assertTrue(allRules10.contains(rule2));
        
        List<? extends Rule> allRules11 = this.ruleService.getRules(rootWithRules, true);
        assertNotNull(allRules11);
        assertEquals(2, allRules11.size());
        assertTrue(allRules11.contains(rule1));
        assertTrue(allRules11.contains(rule2));
        
        List<? extends Rule> allRules12 = this.ruleService.getRules(rootWithRules2, true);
        assertNotNull(allRules12);
        assertEquals(2, allRules12.size());
        assertTrue(allRules12.contains(rule5));
        assertTrue(allRules12.contains(rule6));
        
        // Delete an association
        
        this.nodeService.removeChild(rootWithRules2, childWithRules);
        
        List<? extends Rule> allRules13 = this.ruleService.getRules(childWithRules, true);
        assertNotNull(allRules13);
        assertEquals(3, allRules13.size());
        assertTrue(allRules13.contains(rule2));
        assertTrue(allRules13.contains(rule3));
        assertTrue(allRules13.contains(rule4));
        
        List<? extends Rule> allRules14 = this.ruleService.getRules(nonActionableChild, true);
        assertNotNull(allRules14);
        assertEquals(1, allRules14.size());
        assertTrue(allRules14.contains(rule2));
        
        List<? extends Rule> allRules15 = this.ruleService.getRules(rootWithRules, true);
        assertNotNull(allRules15);
        assertEquals(2, allRules15.size());
        assertTrue(allRules15.contains(rule1));
        assertTrue(allRules15.contains(rule2));
       
        List<? extends Rule> allRules16 = this.ruleService.getRules(rootWithRules2, true);
        assertNotNull(allRules16);
        assertEquals(2, allRules16.size());
        assertTrue(allRules16.contains(rule5));
        assertTrue(allRules16.contains(rule6));
        
        // Add an association
        this.nodeService.addChild(
                rootWithRules2, 
                childWithRules, 
                ContentModel.ASSOC_CHILDREN,
                QName.createQName("{test}testnode"));
        
        List<? extends Rule> allRules17 = this.ruleService.getRules(childWithRules, true);
        assertNotNull(allRules17);
        assertEquals(4, allRules17.size());
        assertTrue(allRules17.contains(rule2));
        assertTrue(allRules17.contains(rule3));
        assertTrue(allRules17.contains(rule4));
        assertTrue(allRules17.contains(rule6));
        
        List<? extends Rule> allRules18 = this.ruleService.getRules(nonActionableChild, true);
        assertNotNull(allRules18);
        assertEquals(1, allRules18.size());
        assertTrue(allRules18.contains(rule2));
        
        List<? extends Rule> allRules19 = this.ruleService.getRules(rootWithRules, true);
        assertNotNull(allRules19);
        assertEquals(2, allRules19.size());
        assertTrue(allRules19.contains(rule1));
        assertTrue(allRules19.contains(rule2));
        
        List<? extends Rule> allRules20 = this.ruleService.getRules(rootWithRules2, true);
        assertNotNull(allRules20);
        assertEquals(2, allRules20.size());
        assertTrue(allRules20.contains(rule5));
        assertTrue(allRules20.contains(rule6));
        
        // Delete node
        
        this.nodeService.deleteNode(rootWithRules2);
        
        List<? extends Rule> allRules21 = this.ruleService.getRules(childWithRules, true);
        assertNotNull(allRules21);
        assertEquals(3, allRules21.size());
        assertTrue(allRules21.contains(rule2));
        assertTrue(allRules21.contains(rule3));
        assertTrue(allRules21.contains(rule4));
        
        List<? extends Rule> allRules22 = this.ruleService.getRules(nonActionableChild, true);
        assertNotNull(allRules22);
        assertEquals(1, allRules22.size());
        assertTrue(allRules22.contains(rule2));
        
        List<? extends Rule> allRules23 = this.ruleService.getRules(rootWithRules, true);
        assertNotNull(allRules23);
        assertEquals(2, allRules23.size());
        assertTrue(allRules23.contains(rule1));
        assertTrue(allRules23.contains(rule2));              
    }
    
    /**
     * Ensure that the rule store can cope with a cyclic node graph
     * 
     * @throws Exception
     */
    public void testCyclicGraphWithInheritedRules()
        throws Exception
    {
        NodeRef nodeRef1 = createNewNode(this.rootNodeRef, true);
        NodeRef nodeRef2 = createNewNode(nodeRef1, true);
        NodeRef nodeRef3 = createNewNode(nodeRef2, true);
        this.nodeService.addChild(nodeRef3, nodeRef1, ContentModel.ASSOC_CHILDREN, QName.createQName("{test}loop"));
        
        Rule rule1 = createTestRule(true);
        this.ruleService.saveRule(nodeRef1, rule1);
        Rule rule2 = createTestRule(true);
        this.ruleService.saveRule(nodeRef2, rule2);
        Rule rule3 = createTestRule(true);
        this.ruleService.saveRule(nodeRef3, rule3);                
        
        List<? extends Rule> allRules1 = this.ruleService.getRules(nodeRef1, true);
        assertNotNull(allRules1);
        assertEquals(3, allRules1.size());
        assertTrue(allRules1.contains(rule1));
        assertTrue(allRules1.contains(rule2));
        assertTrue(allRules1.contains(rule3));
        
        List<? extends Rule> allRules2 = this.ruleService.getRules(nodeRef2, true);
        assertNotNull(allRules2);
        assertEquals(3, allRules2.size());
        assertTrue(allRules2.contains(rule1));
        assertTrue(allRules2.contains(rule2));
        assertTrue(allRules2.contains(rule3));
        
        List<? extends Rule> allRules3 = this.ruleService.getRules(nodeRef3, true);
        assertNotNull(allRules3);
        assertEquals(3, allRules3.size());
        assertTrue(allRules3.contains(rule1));
        assertTrue(allRules3.contains(rule2));
        assertTrue(allRules3.contains(rule3));            
    }
    
    /**
     * Ensures that rules are not duplicated when inherited    
     */
    public void testRuleDuplication()
    {
        NodeRef nodeRef1 = createNewNode(this.rootNodeRef, true);
        NodeRef nodeRef2 = createNewNode(nodeRef1, true);
        NodeRef nodeRef3 = createNewNode(nodeRef2, true);
        NodeRef nodeRef4 = createNewNode(nodeRef1, true);
        this.nodeService.addChild(nodeRef4, nodeRef3, ContentModel.ASSOC_CHILDREN, QName.createQName("{test}test"));
        
        Rule rule1 = createTestRule(true);
        this.ruleService.saveRule(nodeRef1, rule1);
        Rule rule2 = createTestRule(true);
        this.ruleService.saveRule(nodeRef2, rule2);
        Rule rule3 = createTestRule(true);
        this.ruleService.saveRule(nodeRef3, rule3);
        Rule rule4 = createTestRule(true);
        this.ruleService.saveRule(nodeRef4, rule4);
        
        List<? extends Rule> allRules1 = this.ruleService.getRules(nodeRef1, true);
        assertNotNull(allRules1);
        assertEquals(1, allRules1.size());
        assertTrue(allRules1.contains(rule1));
        
        List<? extends Rule> allRules2 = this.ruleService.getRules(nodeRef2, true);
        assertNotNull(allRules2);
        assertEquals(2, allRules2.size());
        assertTrue(allRules2.contains(rule1));
        assertTrue(allRules2.contains(rule2));
        
        List<? extends Rule> allRules3 = this.ruleService.getRules(nodeRef3, true);
        assertNotNull(allRules3);
        assertEquals(4, allRules3.size());
        assertTrue(allRules3.contains(rule1));
        assertTrue(allRules3.contains(rule2));
        assertTrue(allRules3.contains(rule3));
        assertTrue(allRules3.contains(rule4));
        
        List<? extends Rule> allRules4 = this.ruleService.getRules(nodeRef4, true);
        assertNotNull(allRules4);
        assertEquals(2, allRules4.size());
        assertTrue(allRules4.contains(rule1));
        assertTrue(allRules4.contains(rule4));
        
    }
}
