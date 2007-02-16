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
package org.alfresco.repo.rule;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.evaluator.ComparePropertyValueEvaluator;
import org.alfresco.repo.action.executer.ImageTransformActionExecuter;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.AbstractContentTransformerTest;
import org.alfresco.repo.transaction.TransactionUtil;
import org.alfresco.repo.transaction.TransactionUtil.TransactionWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.CyclicChildRelationshipException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleType;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.digester.SetRootRule;


/**
 * Rule service implementation test
 * 
 * @author Roy Wetherall
 */
public class RuleServiceImplTest extends BaseRuleTest
{    
    AuthenticationService authenticationService;
	PermissionService permissionService;  
	
	@Override
	protected void onSetUpInTransaction() throws Exception 
	{
		super.onSetUpInTransaction();		
		this.permissionService = (PermissionService)this.applicationContext.getBean("permissionService");
		this.authenticationService = (AuthenticationService)this.applicationContext.getBean("authenticationService");
	}
	
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
     * Test addRule
     *
     */
    public void testAddRule()
    {
        Rule newRule = createTestRule();        
        this.ruleService.saveRule(this.nodeRef, newRule);
        assertNotNull(newRule.getNodeRef());
        
        // Check the owning node reference
        assertNotNull(this.ruleService.getOwningNodeRef(newRule));
        assertEquals(this.nodeRef, this.ruleService.getOwningNodeRef(newRule));
        
        Rule savedRule = this.ruleService.getRule(newRule.getNodeRef());
        assertNotNull(savedRule);
        assertFalse(savedRule.isAppliedToChildren());
        
        savedRule.applyToChildren(true);
        this.ruleService.saveRule(this.nodeRef, savedRule);
        
        Rule savedRule2 = this.ruleService.getRule(savedRule.getNodeRef());
        assertNotNull(savedRule2);
        assertTrue(savedRule2.isAppliedToChildren());
    }
    
    public void testRemoveAllRules()
    {
        this.ruleService.removeAllRules(this.nodeRef);
        List<Rule> rules1 = this.ruleService.getRules(this.nodeRef);
        assertNotNull(rules1);
        assertEquals(0, rules1.size());
        
        Rule newRule = createTestRule(); //this.ruleService.createRule(ruleType.getName());        
        this.ruleService.saveRule(this.nodeRef, newRule); 
        Rule newRule2 = createTestRule(); //this.ruleService.createRule(ruleType.getName());
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
        assertNotNull(this.nodeService.getProperty(rule.getNodeRef(), ContentModel.PROP_CREATED));
        assertNotNull(this.nodeService.getProperty(rule.getNodeRef(), ContentModel.PROP_CREATOR));
        
        // Check that the condition action have been retireved correctly
        Action action = rule.getAction();
        assertNotNull(action);
        List<ActionCondition> conditions = action.getActionConditions();
        assertNotNull(conditions);
        assertEquals(1, conditions.size());                
    }
    
    /** Ensure the rules are retrieved in the correct order **/
    public void testGetRulesOrder()
    {
        for (int index = 0; index < 10; index++)
        {         
            Rule newRule = createTestRule(true, Integer.toString(index));        
            this.ruleService.saveRule(this.nodeRef, newRule);
        }
        
        // Check that they are all returned in the correct order
        List<Rule> rules = this.ruleService.getRules(this.nodeRef);
        int index = 0;
        for (Rule rule : rules)
        {
            assertEquals(Integer.toString(index), rule.getTitle());
            index++;
        }
        
        // Create a child node
        NodeRef level1 = createNewNode(this.nodeRef);
        for (int index2 = 10; index2 < 20; index2++)
        {         
            Rule newRule = createTestRule(true, Integer.toString(index2));        
            this.ruleService.saveRule(level1, newRule);
        }
        
        // Check that they are all returned in the correct order
        List<Rule> rules2 = this.ruleService.getRules(level1);
        int index2 = 0;
        for (Rule rule : rules2)
        {
            assertEquals(Integer.toString(index2), rule.getTitle());
            index2++;
        }
        
        // Create a child node
        NodeRef level2 = createNewNode(level1);
        for (int index3 = 20; index3 < 30; index3++)
        {         
            Rule newRule = createTestRule(true, Integer.toString(index3));        
            this.ruleService.saveRule(level2, newRule);
        }
        
        // Check that they are all returned in the correct order
        List<Rule> rules3 = this.ruleService.getRules(level2);
        int index3 = 0;
        for (Rule rule : rules3)
        {
            //System.out.println(rule.getTitle());
            assertEquals(Integer.toString(index3), rule.getTitle());
            index3++;
        }
        
        // Update a couple of the rules
        Rule rule1 = rules3.get(2);
        rule1.setDescription("This has been changed");
        this.ruleService.saveRule(this.nodeRef, rule1);
        Rule rule2 = rules3.get(12);
        rule2.setDescription("This has been changed");
        this.ruleService.saveRule(level1, rule2);
        Rule rule3 = rules3.get(22);
        rule3.setDescription("This has been changed");
        this.ruleService.saveRule(level2, rule3);
        
        // Check that they are all returned in the correct order
        List<Rule> rules4 = this.ruleService.getRules(level2);
        int index4 = 0;
        for (Rule rule : rules4)
        {
            assertEquals(Integer.toString(index4), rule.getTitle());
            index4++;
        }
    }
    
    public void testIgnoreInheritedRules()
    {
        // Create the nodes and rules
        this.ruleService.saveRule(this.nodeRef, createTestRule(true, "rule1"));
        this.ruleService.saveRule(this.nodeRef, createTestRule(false, "rule2"));
        NodeRef nodeRef1 = createNewNode(this.nodeRef);
        this.ruleService.saveRule(nodeRef1, createTestRule(true, "rule3"));
        this.ruleService.saveRule(nodeRef1, createTestRule(false, "rule4"));
        NodeRef nodeRef2 = createNewNode(nodeRef1);
        this.ruleService.saveRule(nodeRef2, createTestRule(true, "rule5"));
        this.ruleService.saveRule(nodeRef2, createTestRule(false, "rule6"));
        
        // Apply the ignore aspect    
        this.nodeService.addAspect(nodeRef1, RuleModel.ASPECT_IGNORE_INHERITED_RULES, null);
        
        // Get the rules
        List<Rule> rules1 = this.ruleService.getRules(nodeRef2);
        assertNotNull(rules1);
        assertEquals(3, rules1.size());
        assertEquals("rule3", rules1.get(0).getTitle());
        assertEquals("rule5", rules1.get(1).getTitle());
        assertEquals("rule6", rules1.get(2).getTitle());
        
        // Apply the ignore aspect
        this.nodeService.addAspect(nodeRef2, RuleModel.ASPECT_IGNORE_INHERITED_RULES, null);
        
        // Get the rules
        List<Rule> rules2 = this.ruleService.getRules(nodeRef2);
        assertNotNull(rules2);
        assertEquals(2, rules2.size());
        assertEquals("rule5", rules2.get(0).getTitle());
        assertEquals("rule6", rules2.get(1).getTitle());
        
        // Remove the ignore aspect
        this.nodeService.removeAspect(nodeRef1, RuleModel.ASPECT_IGNORE_INHERITED_RULES);
        this.nodeService.removeAspect(nodeRef2, RuleModel.ASPECT_IGNORE_INHERITED_RULES);
        
        // Get the rules
        List<Rule> rules3 = this.ruleService.getRules(nodeRef2);
        assertNotNull(rules3);
        assertEquals(4, rules3.size());
        assertEquals("rule1", rules3.get(0).getTitle());
        assertEquals("rule3", rules3.get(1).getTitle());
        assertEquals("rule5", rules3.get(2).getTitle());
        assertEquals("rule6", rules3.get(3).getTitle());        
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
    private NodeRef createNewNode(NodeRef parent)
    {
        return this.nodeService.createNode(parent,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName("{test}testnode"),
                ContentModel.TYPE_CONTAINER).getChildRef();
    }
    
    public void testRuleServicePermissionsConsumer()
    {
    	this.authenticationService.createAuthentication("conUser", "password".toCharArray());
    	this.permissionService.setPermission(this.nodeRef, "conUser", PermissionService.CONSUMER, true);
    	this.permissionService.setInheritParentPermissions(this.nodeRef, true);    	
    	
    	this.authenticationService.authenticate("conUser", "password".toCharArray());    	
    	Rule rule = createTestRule();
    	try
    	{
    		this.ruleService.saveRule(this.nodeRef, rule);
    		// Fail
    		fail("Consumers cannot create rules.");
    	}
    	catch (Exception exception)
    	{
    		// Ok
    	}

    }
    
    public void testRuleServicePermissionsEditor()
    {
    	this.authenticationService.createAuthentication("editorUser", "password".toCharArray());
    	this.permissionService.setPermission(this.nodeRef, "editorUser", PermissionService.EDITOR, true);
    	this.permissionService.setInheritParentPermissions(this.nodeRef, true);    	
    	
    	this.authenticationService.authenticate("editorUser", "password".toCharArray());    	
    	Rule rule = createTestRule();
    	try
    	{
    		this.ruleService.saveRule(this.nodeRef, rule);
    		// Fail
    		fail("Editors cannot create rules.");
    	}
    	catch (Exception exception)
    	{
    		// Ok
    	}
    }
    
    public void testRuleServicePermissionsCoordinator()
    {
    	this.authenticationService.createAuthentication("coordUser", "password".toCharArray());
    	this.permissionService.setPermission(this.nodeRef, "coordUser", PermissionService.COORDINATOR, true);
    	this.permissionService.setInheritParentPermissions(this.nodeRef, true);
    	
    	this.authenticationService.authenticate("admin", "admin".toCharArray());    	
		Rule rule2 = createTestRule();
		this.ruleService.saveRule(this.nodeRef, rule2);    	
		this.authenticationService.clearCurrentSecurityContext();  		
    }
    
    /**
     * Tests the rule inheritance within the store, checking that the cache is reset correctly when 
     * rules are added and removed.
     */
    public void testRuleInheritance()
    {
        // Create the nodes and rules
        
        NodeRef rootWithRules = createNewNode(this.rootNodeRef);
        Rule rule1 = createTestRule();
        this.ruleService.saveRule(rootWithRules, rule1);
        Rule rule2 = createTestRule(true);
        this.ruleService.saveRule(rootWithRules, rule2);
        
        NodeRef nonActionableChild = createNewNode(rootWithRules);
        
        NodeRef childWithRules = createNewNode(nonActionableChild);
        Rule rule3 = createTestRule();
        this.ruleService.saveRule(childWithRules, rule3);
        Rule rule4 = createTestRule(true);
        this.ruleService.saveRule(childWithRules, rule4);
        
        NodeRef rootWithRules2 = createNewNode(this.rootNodeRef);
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
        
        List<? extends Rule> allRules = this.ruleService.getRules(childWithRules);
        assertNotNull(allRules);
        assertEquals(4, allRules.size());
        assertTrue(allRules.contains(rule2));
        assertTrue(allRules.contains(rule3));
        assertTrue(allRules.contains(rule4));
        assertTrue(allRules.contains(rule6));
        
        // Check the owning node ref
        int count = 0;
        for (Rule rule : allRules)
        {
            NodeRef owningNodeRef = this.ruleService.getOwningNodeRef(rule);
            assertNotNull(owningNodeRef);
            if (owningNodeRef.equals(childWithRules) == true)
            {
                count++;
            }
        }
        assertEquals(2, count);
        
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
        
        this.ruleService.disableRules(rootWithRules2);
        try
        {
            // Add an association
            this.nodeService.addChild(
                    rootWithRules2, 
                    childWithRules, 
                    ContentModel.ASSOC_CHILDREN,
                    QName.createQName("{test}testnode"));
        }
        finally
        {
            this.ruleService.enableRules(rootWithRules2);
        }
        
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
        NodeRef nodeRef1 = createNewNode(this.rootNodeRef);
        NodeRef nodeRef2 = createNewNode(nodeRef1);
        NodeRef nodeRef3 = createNewNode(nodeRef2);
        try
        {
            this.nodeService.addChild(nodeRef3, nodeRef1, ContentModel.ASSOC_CHILDREN, QName.createQName("{test}loop"));
            fail("Expected detection of cyclic relationship");
        }
        catch (CyclicChildRelationshipException e)
        {
            // expected
            // the node will still have been created in the current transaction, although the txn will be rollback-only
        }
        
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
        NodeRef nodeRef1 = createNewNode(this.rootNodeRef);
        NodeRef nodeRef2 = createNewNode(nodeRef1);
        NodeRef nodeRef3 = createNewNode(nodeRef2);
        NodeRef nodeRef4 = createNewNode(nodeRef1);
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
    
    public void testCyclicRules()
    {
    }
    
    public void testCyclicAsyncRules() throws Exception
    {
        NodeRef nodeRef = createNewNode(this.rootNodeRef);
        
        // Create the first rule
        
        Map<String, Serializable> conditionProps = new HashMap<String, Serializable>();
        conditionProps.put(ComparePropertyValueEvaluator.PARAM_VALUE, "*.jpg");

        Map<String, Serializable> actionProps = new HashMap<String, Serializable>();
        actionProps.put(ImageTransformActionExecuter.PARAM_MIME_TYPE, MimetypeMap.MIMETYPE_IMAGE_GIF);
        actionProps.put(ImageTransformActionExecuter.PARAM_DESTINATION_FOLDER, nodeRef);
        actionProps.put(ImageTransformActionExecuter.PARAM_ASSOC_TYPE_QNAME, ContentModel.ASSOC_CHILDREN);
        actionProps.put(ImageTransformActionExecuter.PARAM_ASSOC_QNAME, ContentModel.ASSOC_CHILDREN);
        
        Rule rule = new Rule();
        rule.setRuleType(this.ruleType.getName());
        rule.setTitle("Convert from *.jpg to *.gif");
        rule.setExecuteAsynchronously(true);
        
        Action action = this.actionService.createAction(ImageTransformActionExecuter.NAME);
        action.setParameterValues(actionProps);
        
        ActionCondition actionCondition = this.actionService.createActionCondition(ComparePropertyValueEvaluator.NAME);
        actionCondition.setParameterValues(conditionProps);
        action.addActionCondition(actionCondition);        
        
        rule.setAction(action);
        
        // Create the next rule
        
        Map<String, Serializable> conditionProps2 = new HashMap<String, Serializable>();
        conditionProps2.put(ComparePropertyValueEvaluator.PARAM_VALUE, "*.gif");

        Map<String, Serializable> actionProps2 = new HashMap<String, Serializable>();
        actionProps2.put(ImageTransformActionExecuter.PARAM_MIME_TYPE, MimetypeMap.MIMETYPE_IMAGE_JPEG);
        actionProps2.put(ImageTransformActionExecuter.PARAM_DESTINATION_FOLDER, nodeRef);
        actionProps2.put(ImageTransformActionExecuter.PARAM_ASSOC_QNAME, ContentModel.ASSOC_CHILDREN);
        
        Rule rule2 = new Rule();
        rule2.setRuleType(this.ruleType.getName());
        rule2.setTitle("Convert from *.gif to *.jpg");
        rule2.setExecuteAsynchronously(true);
        
        Action action2 = this.actionService.createAction(ImageTransformActionExecuter.NAME);
        action2.setParameterValues(actionProps2);
        
        ActionCondition actionCondition2 = this.actionService.createActionCondition(ComparePropertyValueEvaluator.NAME);
        actionCondition2.setParameterValues(conditionProps2);
        action2.addActionCondition(actionCondition2);
        
        rule2.setAction(action2);
        
        // Save the rules
        this.ruleService.saveRule(nodeRef, rule);
        this.ruleService.saveRule(nodeRef, rule);
        
        // Now create new content
        NodeRef contentNode = this.nodeService.createNode(nodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName("{test}testnode"),
                ContentModel.TYPE_CONTENT).getChildRef();
        this.nodeService.setProperty(contentNode, ContentModel.PROP_NAME, "myFile.jpg");
        File file = AbstractContentTransformerTest.loadQuickTestFile("jpg");
        ContentWriter writer = this.contentService.getWriter(contentNode, ContentModel.PROP_CONTENT, true);
        writer.setEncoding("UTF-8");
        writer.setMimetype(MimetypeMap.MIMETYPE_IMAGE_JPEG);
        writer.putContent(file);
        
        setComplete();
        endTransaction();
        
        //final NodeRef finalNodeRef = nodeRef;
        
        // Check to see what has happened
//        ActionServiceImplTest.postAsyncActionTest(
//                this.transactionService,
//                10000, 
//                10, 
//                new AsyncTest()
//                {
//                    public boolean executeTest() 
//                    {
//                        List<ChildAssociationRef> assocs = RuleServiceImplTest.this.nodeService.getChildAssocs(finalNodeRef);
//                        for (ChildAssociationRef ref : assocs)
//                        {
//                            NodeRef child = ref.getChildRef();
//                            System.out.println("Child name: " + RuleServiceImplTest.this.nodeService.getProperty(child, ContentModel.PROP_NAME));
//                        }
//                        
//                        return true;
//                    };
//                });
    }    
}
