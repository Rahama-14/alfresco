/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.repo.copy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.evaluator.NoConditionEvaluator;
import org.alfresco.repo.action.executer.AddFeaturesActionExecuter;
import org.alfresco.repo.action.executer.CopyActionExecuter;
import org.alfresco.repo.action.executer.MoveActionExecuter;
import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.dictionary.M2Aspect;
import org.alfresco.repo.dictionary.M2Association;
import org.alfresco.repo.dictionary.M2ChildAssociation;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.dictionary.M2Property;
import org.alfresco.repo.dictionary.M2Type;
import org.alfresco.repo.rule.RuleModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.rule.RuleType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.BaseSpringTest;

/**
 * Node operations service unit tests
 * 
 * @author Roy Wetherall
 */
public class CopyServiceImplTest extends BaseSpringTest 
{
	/**
	 * Services used by the tests
	 */
	private NodeService nodeService;
	private CopyService copyService;
	private DictionaryDAO dictionaryDAO;
	private ContentService contentService;
    private RuleService ruleService;
    private ActionService actionService;
    private AuthenticationComponent authenticationComponent;
	
	/**
	 * Data used by the tests
	 */
	private StoreRef storeRef;
	private NodeRef sourceNodeRef;	
	private NodeRef rootNodeRef;	
	private NodeRef targetNodeRef;
	private NodeRef nonPrimaryChildNodeRef;
	private NodeRef childNodeRef;
	private NodeRef destinationNodeRef;
	
	/**
	 * Types and properties used by the tests
	 */
	private static final String TEST_TYPE_NAMESPACE = "testTypeNamespaceURI";
	private static final QName TEST_TYPE_QNAME = QName.createQName(TEST_TYPE_NAMESPACE, "testType");
	private static final QName PROP1_QNAME_MANDATORY = QName.createQName(TEST_TYPE_NAMESPACE, "prop1Mandatory");
	private static final QName PROP2_QNAME_OPTIONAL = QName.createQName(TEST_TYPE_NAMESPACE, "prop2Optional");
	
	private static final QName TEST_ASPECT_QNAME = QName.createQName(TEST_TYPE_NAMESPACE, "testAspect");
	private static final QName PROP3_QNAME_MANDATORY = QName.createQName(TEST_TYPE_NAMESPACE, "prop3Mandatory");
	private static final QName PROP4_QNAME_OPTIONAL = QName.createQName(TEST_TYPE_NAMESPACE, "prop4Optional");
	
	private static final QName PROP_QNAME_MY_NODE_REF = QName.createQName(TEST_TYPE_NAMESPACE, "myNodeRef");
	private static final QName PROP_QNAME_MY_ANY = QName.createQName(TEST_TYPE_NAMESPACE, "myAny");	
	
    private static final QName TEST_MANDATORY_ASPECT_QNAME = QName.createQName(TEST_TYPE_NAMESPACE, "testMandatoryAspect");
    private static final QName PROP5_QNAME_MANDATORY = QName.createQName(TEST_TYPE_NAMESPACE, "prop5Mandatory");
    
    private static final String TEST_NAME = "testName";
	private static final String TEST_VALUE_1 = "testValue1";
	private static final String TEST_VALUE_2 = "testValue2";
    private static final String TEST_VALUE_3 = "testValue3";
	
    private static final QName TEST_CHILD_ASSOC_TYPE_QNAME = QName.createQName(TEST_TYPE_NAMESPACE, "contains");
    private static final QName TEST_CHILD_ASSOC_QNAME = QName.createQName(TEST_TYPE_NAMESPACE, "testChildAssocName");
	private static final QName TEST_ASSOC_TYPE_QNAME = QName.createQName(TEST_TYPE_NAMESPACE, "testAssocName");
	private static final QName TEST_CHILD_ASSOC_QNAME2 = QName.createQName(TEST_TYPE_NAMESPACE, "testChildAssocName2");
    
    private static final ContentData CONTENT_DATA_TEXT = new ContentData(null, "text/plain", 0L, "UTF-8");
	
	/**
	 * Test content
	 */
	private static final String SOME_CONTENT = "This is some content ...";	
	
	/**
	 * Sets the meta model DAO
	 * 
	 * @param dictionaryDAO  the meta model DAO
	 */
	public void setDictionaryDAO(DictionaryDAO dictionaryDAO)
    {
        this.dictionaryDAO = dictionaryDAO;
    }
	
	/**
	 * On setup in transaction implementation
	 */
	@Override
	protected void onSetUpInTransaction() 
		throws Exception 
	{
		// Set the services
		this.nodeService = (NodeService)this.applicationContext.getBean("dbNodeService");
		this.copyService = (CopyService)this.applicationContext.getBean("copyService");
		this.contentService = (ContentService)this.applicationContext.getBean("contentService");
        this.ruleService = (RuleService)this.applicationContext.getBean("ruleService");
        this.actionService = (ActionService)this.applicationContext.getBean("actionService");
        this.authenticationComponent = (AuthenticationComponent)this.applicationContext.getBean("authenticationComponent");
		
        this.authenticationComponent.setSystemUserAsCurrentUser();
        
		// Create the test model
		createTestModel();
		
		// Create the store and get the root node reference
		this.storeRef = this.nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
		this.rootNodeRef = this.nodeService.getRootNode(storeRef);
		
		// Create the node used for copying
		ChildAssociationRef childAssocRef = this.nodeService.createNode(
				rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
				QName.createQName("{test}test"),
				TEST_TYPE_QNAME,
				createTypePropertyBag());
		this.sourceNodeRef = childAssocRef.getChildRef();
		
		// Create another bag of properties
		Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>();
		aspectProperties.put(PROP3_QNAME_MANDATORY, TEST_VALUE_1);
		aspectProperties.put(PROP4_QNAME_OPTIONAL, TEST_VALUE_2);
		
		// Apply the test aspect
		this.nodeService.addAspect(
				this.sourceNodeRef, 
				TEST_ASPECT_QNAME, 
				aspectProperties);
        
        this.nodeService.addAspect(sourceNodeRef, ContentModel.ASPECT_TITLED, null);
		
		// Add a child
		ChildAssociationRef temp3 =this.nodeService.createNode(
				this.sourceNodeRef, 
                TEST_CHILD_ASSOC_TYPE_QNAME, 
				TEST_CHILD_ASSOC_QNAME, 
				TEST_TYPE_QNAME, 
				createTypePropertyBag());
		this.childNodeRef = temp3.getChildRef();
		
		// Add a child that is primary
		ChildAssociationRef temp2 = this.nodeService.createNode(
				rootNodeRef,
				ContentModel.ASSOC_CHILDREN,
				QName.createQName("{test}testNonPrimaryChild"),
				TEST_TYPE_QNAME,
				createTypePropertyBag());
        
		this.nonPrimaryChildNodeRef = temp2.getChildRef();
		this.nodeService.addChild(
                this.sourceNodeRef,
                this.nonPrimaryChildNodeRef,
                TEST_CHILD_ASSOC_TYPE_QNAME,
                TEST_CHILD_ASSOC_QNAME2);
		
		// Add a target assoc
		ChildAssociationRef temp = this.nodeService.createNode(
				rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
				QName.createQName("{test}testAssoc"),
				TEST_TYPE_QNAME,
				createTypePropertyBag());
		this.targetNodeRef = temp.getChildRef();
		this.nodeService.createAssociation(this.sourceNodeRef, this.targetNodeRef, TEST_ASSOC_TYPE_QNAME);
		
		// Create a node we can use as the destination in a copy
		Map<QName, Serializable> destinationProps = new HashMap<QName, Serializable>();
		destinationProps.put(PROP1_QNAME_MANDATORY, TEST_VALUE_1);			
        destinationProps.put(PROP5_QNAME_MANDATORY, TEST_VALUE_3); 
        destinationProps.put(ContentModel.PROP_CONTENT, CONTENT_DATA_TEXT);
		ChildAssociationRef temp5 = this.nodeService.createNode(
				this.rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
				QName.createQName("{test}testDestinationNode"),
				TEST_TYPE_QNAME,
				destinationProps);
		this.destinationNodeRef = temp5.getChildRef();
	}
	
    @Override
    protected void onTearDownInTransaction() throws Exception
    {
        authenticationComponent.clearCurrentSecurityContext();
        super.onTearDownInTransaction();
    }
    
	/**
	 * Helper method that creates a bag of properties for the test type
	 * 
	 * @return  bag of properties
	 */
	private Map<QName, Serializable> createTypePropertyBag()
	{
		Map<QName, Serializable> result = new HashMap<QName, Serializable>();
		result.put(ContentModel.PROP_NAME, TEST_NAME);
		result.put(PROP1_QNAME_MANDATORY, TEST_VALUE_1);
		result.put(PROP2_QNAME_OPTIONAL, TEST_VALUE_2);
        result.put(PROP5_QNAME_MANDATORY, TEST_VALUE_3);
        result.put(ContentModel.PROP_CONTENT, CONTENT_DATA_TEXT);
		return result;
	}
	
	/**
	 * Creates the test model used by the tests
	 */
	private void createTestModel()
	{
        M2Model model = M2Model.createModel("test:nodeoperations");
        model.createNamespace(TEST_TYPE_NAMESPACE, "test");
        model.createImport(NamespaceService.DICTIONARY_MODEL_1_0_URI, NamespaceService.DICTIONARY_MODEL_PREFIX);
        model.createImport(NamespaceService.SYSTEM_MODEL_1_0_URI, NamespaceService.SYSTEM_MODEL_PREFIX);
        model.createImport(NamespaceService.CONTENT_MODEL_1_0_URI, NamespaceService.CONTENT_MODEL_PREFIX);

        M2Type testType = model.createType("test:" + TEST_TYPE_QNAME.getLocalName());
        testType.setParentName("cm:" + ContentModel.TYPE_CONTENT.getLocalName());
        
        M2Property prop1 = testType.createProperty("test:" + PROP1_QNAME_MANDATORY.getLocalName());
        prop1.setMandatory(true);
        prop1.setType("d:" + DataTypeDefinition.TEXT.getLocalName());
        prop1.setMultiValued(false);
        
		M2Property prop2 = testType.createProperty("test:" + PROP2_QNAME_OPTIONAL.getLocalName());
		prop2.setMandatory(false);
        prop2.setType("d:" + DataTypeDefinition.TEXT.getLocalName());
		prop2.setMandatory(false);
		
		M2Property propNodeRef = testType.createProperty("test:" + PROP_QNAME_MY_NODE_REF.getLocalName());
		propNodeRef.setMandatory(false);
		propNodeRef.setType("d:" + DataTypeDefinition.NODE_REF.getLocalName());
		propNodeRef.setMandatory(false);
		
		M2Property propAnyNodeRef = testType.createProperty("test:" + PROP_QNAME_MY_ANY.getLocalName());
		propAnyNodeRef.setMandatory(false);
		propAnyNodeRef.setType("d:" + DataTypeDefinition.ANY.getLocalName());
		propAnyNodeRef.setMandatory(false);
		
		M2ChildAssociation childAssoc = testType.createChildAssociation("test:" + TEST_CHILD_ASSOC_TYPE_QNAME.getLocalName());
        childAssoc.setTargetClassName("sys:base");
		childAssoc.setTargetMandatory(false);
		
		M2Association assoc = testType.createAssociation("test:" + TEST_ASSOC_TYPE_QNAME.getLocalName());
        assoc.setTargetClassName("sys:base");
		assoc.setTargetMandatory(false);
		
		M2Aspect testAspect = model.createAspect("test:" + TEST_ASPECT_QNAME.getLocalName());
		
		M2Property prop3 = testAspect.createProperty("test:" + PROP3_QNAME_MANDATORY.getLocalName());
		prop3.setMandatory(true);
        prop3.setType("d:" + DataTypeDefinition.TEXT.getLocalName());
		prop3.setMultiValued(false);
		
		M2Property prop4 = testAspect.createProperty("test:" + PROP4_QNAME_OPTIONAL.getLocalName());
		prop4.setMandatory(false);
        prop4.setType("d:" + DataTypeDefinition.TEXT.getLocalName());
		prop4.setMultiValued(false);

        M2Aspect testMandatoryAspect = model.createAspect("test:" + TEST_MANDATORY_ASPECT_QNAME.getLocalName());
        M2Property prop5 = testMandatoryAspect.createProperty("test:" + PROP5_QNAME_MANDATORY.getLocalName());
        prop5.setType("d:" + DataTypeDefinition.TEXT.getLocalName());
        prop5.setMandatory(true);

        testType.addMandatoryAspect("test:" + TEST_MANDATORY_ASPECT_QNAME.getLocalName());
        
        dictionaryDAO.putModel(model);
	}
	
	/**
	 * Test copy new node within store	 
	 */
	public void testCopyToNewNode()
	{
        // Check that the node has no copies
        List<NodeRef> copies = this.copyService.getCopies(this.sourceNodeRef);
        assertNotNull(copies);
        assertTrue(copies.isEmpty());
        
		// Copy to new node without copying children
		NodeRef copy = this.copyService.copy(
				this.sourceNodeRef,
				this.rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
				QName.createQName("{test}copyAssoc"));		
		checkCopiedNode(this.sourceNodeRef, copy, true, true, false);        
        List<NodeRef> copies2 = this.copyService.getCopies(this.sourceNodeRef);
        assertNotNull(copies2);
        assertEquals(1, copies2.size());
		
        // Copy to new node, copying children
		NodeRef copy2 = this.copyService.copy(
				this.sourceNodeRef,
				this.rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
				QName.createQName("{test}copyAssoc2"),
				true);
		checkCopiedNode(this.sourceNodeRef, copy2, true, true, true);
        List<NodeRef> copies3 = this.copyService.getCopies(this.sourceNodeRef);
        assertNotNull(copies3);
        assertEquals(2, copies3.size());
		
		// Check that a copy of a copy works correctly
		NodeRef copyOfCopy = this.copyService.copy(
				copy,
				this.rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
				QName.createQName("{test}copyOfCopy"));
		checkCopiedNode(copy, copyOfCopy, true, true, false);
		
        // TODO check copying from a versioned copy
		// TODO check copying from a lockable copy
		
		// Check copying from a node with content	
		ContentWriter contentWriter = this.contentService.getWriter(this.sourceNodeRef, ContentModel.PROP_CONTENT, true);
		contentWriter.putContent(SOME_CONTENT);		
		NodeRef copyWithContent = this.copyService.copy(
				this.sourceNodeRef,
				this.rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
				QName.createQName("{test}copyWithContent"));
		checkCopiedNode(this.sourceNodeRef, copyWithContent, true, true, false);
		ContentReader contentReader = this.contentService.getReader(copyWithContent, ContentModel.PROP_CONTENT);
		assertNotNull(contentReader);
		assertEquals(SOME_CONTENT, contentReader.getContentString());
		
		// TODO check copying to a different store
        
        //System.out.println(
		//		NodeStoreInspector.dumpNodeStore(this.nodeService, this.storeRef));
	}	
    
    public void testCopyNodeWithRules()
    {
        // Create a new rule and add it to the source noderef
        Rule rule = new Rule();
        rule.setRuleType(RuleType.INBOUND);
        
        Map<String, Serializable> props = new HashMap<String, Serializable>(1);
        props.put(AddFeaturesActionExecuter.PARAM_ASPECT_NAME, ContentModel.ASPECT_VERSIONABLE);
        Action action = this.actionService.createAction(AddFeaturesActionExecuter.NAME, props);
        rule.setAction(action);
        
        ActionCondition actionCondition = this.actionService.createActionCondition(NoConditionEvaluator.NAME);
        action.addActionCondition(actionCondition);
        
        this.ruleService.saveRule(this.sourceNodeRef, rule);
        assertNotNull(rule.getNodeRef());
        assertEquals(this.sourceNodeRef, this.ruleService.getOwningNodeRef(rule));
        
        //System.out.println(
        //        NodeStoreInspector.dumpNodeStore(this.nodeService, this.storeRef));
        //System.out.println(" ------------------------------ ");
        
        // Now copy the node that has rules associated with it
        NodeRef copy = this.copyService.copy(
                this.sourceNodeRef,
                this.rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName("{test}withRulesCopy"),
                true);
        
        //System.out.println(
         //          NodeStoreInspector.dumpNodeStore(this.nodeService, this.storeRef));
        
        checkCopiedNode(this.sourceNodeRef, copy, true, true, true);   
        
        assertTrue(this.nodeService.hasAspect(copy, RuleModel.ASPECT_RULES));
        assertTrue(this.ruleService.hasRules(copy));
        assertTrue(this.ruleService.rulesEnabled(copy));
        
        List<Rule> copiedRules = this.ruleService.getRules(copy);
        assertEquals(1, copiedRules.size());
        Rule copiedRule = copiedRules.get(0);
        
        assertNotNull(copiedRule.getNodeRef());
        assertFalse(copiedRule.getNodeRef().equals(rule.getNodeRef()));
        assertEquals(rule.getTitle(), copiedRule.getTitle());
        assertEquals(rule.getDescription(), copiedRule.getDescription());
        assertEquals(copy, this.ruleService.getOwningNodeRef(copiedRule));
        assertEquals(rule.getAction().getActionDefinitionName(), copiedRule.getAction().getActionDefinitionName());
        
        // Now copy the node without copying the children and check that the rules have been copied
        NodeRef copy2 = this.copyService.copy(
        		this.sourceNodeRef,
        		this.rootNodeRef,
        		ContentModel.ASSOC_CHILDREN,
        		QName.createQName("{test}withRuleCopyNoChildren"),
        		false);
        
//      System.out.println(
        //         NodeStoreInspector.dumpNodeStore(this.nodeService, this.storeRef));        
        
        checkCopiedNode(this.sourceNodeRef, copy2, true, true, false);
        
        //assertTrue(this.configurableService.isConfigurable(copy2));
        //assertNotNull(this.configurableService.getConfigurationFolder(copy2));
        //assertFalse(this.configurableService.getConfigurationFolder(this.sourceNodeRef) == this.configurableService.getConfigurationFolder(copy2));
        
        assertTrue(this.nodeService.hasAspect(copy2, RuleModel.ASPECT_RULES));
        assertTrue(this.ruleService.hasRules(copy2));
        assertTrue(this.ruleService.rulesEnabled(copy2));
        List<Rule> copiedRules2 = this.ruleService.getRules(copy2);
        assertEquals(1, copiedRules.size());
        Rule copiedRule2 = copiedRules2.get(0);
        assertFalse(rule.getNodeRef().equals(copiedRule2.getNodeRef()));
        assertEquals(rule.getTitle(), copiedRule2.getTitle());
        assertEquals(rule.getDescription(), copiedRule2.getDescription());
        assertEquals(this.ruleService.getOwningNodeRef(copiedRule2), copy2);
        assertEquals(rule.getAction().getActionDefinitionName(), copiedRule2.getAction().getActionDefinitionName());                                
    }
	
	public void testCopyToExistingNode()
	{
		// Copy nodes within the same store
		this.copyService.copy(this.sourceNodeRef, this.destinationNodeRef);
		checkCopiedNode(this.sourceNodeRef, this.destinationNodeRef, false, true, false);
		
		// TODO check copying from a copy
		// TODO check copying from a versioned copy
		// TODO check copying from a lockable copy
		// TODO check copying from a node with content
		
		// TODO check copying nodes between stores
		
		//System.out.println(
		//		NodeStoreInspector.dumpNodeStore(this.nodeService, this.storeRef));
	}
	
	/**
	 * Test a potentially recursive copy
	 */
	public void testRecursiveCopy()
	{
		// Need to create a potentially recursive node structure
		NodeRef nodeOne = this.nodeService.createNode(
				this.rootNodeRef,
				ContentModel.ASSOC_CHILDREN,
				ContentModel.ASSOC_CHILDREN,
				ContentModel.TYPE_CONTAINER).getChildRef();
		NodeRef nodeTwo = this.nodeService.createNode(
				nodeOne,
				ContentModel.ASSOC_CHILDREN,
				ContentModel.ASSOC_CHILDREN,
				ContentModel.TYPE_CONTAINER).getChildRef();
		NodeRef nodeThree = this.nodeService.createNode(
				nodeTwo,
				ContentModel.ASSOC_CHILDREN,
				ContentModel.ASSOC_CHILDREN,
				ContentModel.TYPE_CONTAINER).getChildRef();
		
		// Issue a potentialy recursive copy
		this.copyService.copy(nodeOne, nodeThree, ContentModel.ASSOC_CHILDREN, ContentModel.ASSOC_CHILDREN, true);
		
		//System.out.println(
        //         NodeStoreInspector.dumpNodeStore(this.nodeService, this.storeRef));
	}
	
	/**
	 * Test that realtive links between nodes are restored once the copy is completed
	 */
	public void testRelativeLinks()
	{
		QName nodeOneAssocName = QName.createQName("{test}nodeOne");
		QName nodeTwoAssocName = QName.createQName("{test}nodeTwo");
		QName nodeThreeAssocName = QName.createQName("{test}nodeThree");
		QName nodeFourAssocName = QName.createQName("{test}nodeFour");
		
		NodeRef nodeOne = this.nodeService.createNode(
				this.rootNodeRef,
				ContentModel.ASSOC_CHILDREN,
				nodeOneAssocName,
				TEST_TYPE_QNAME).getChildRef();
		NodeRef nodeTwo = this.nodeService.createNode(
				nodeOne,
				TEST_CHILD_ASSOC_TYPE_QNAME,
				nodeTwoAssocName,
				TEST_TYPE_QNAME).getChildRef();
		NodeRef nodeThree = this.nodeService.createNode(
				nodeTwo,
				TEST_CHILD_ASSOC_TYPE_QNAME,
				nodeThreeAssocName,
				TEST_TYPE_QNAME).getChildRef();
		NodeRef nodeFour = this.nodeService.createNode(
				nodeOne,
				TEST_CHILD_ASSOC_TYPE_QNAME,
				nodeFourAssocName,
				TEST_TYPE_QNAME).getChildRef();
		this.nodeService.addChild(nodeFour, nodeThree, TEST_CHILD_ASSOC_TYPE_QNAME, TEST_CHILD_ASSOC_QNAME);
		this.nodeService.createAssociation(nodeTwo, nodeThree, TEST_ASSOC_TYPE_QNAME);
		this.nodeService.setProperty(nodeOne, PROP_QNAME_MY_NODE_REF, nodeThree);
		this.nodeService.setProperty(nodeOne, PROP_QNAME_MY_ANY, nodeThree);
		
		// Make node one actionable with a rule to copy nodes into node two
		Map<String, Serializable> params = new HashMap<String, Serializable>(1);
        params.put(MoveActionExecuter.PARAM_DESTINATION_FOLDER, nodeTwo);
        params.put(MoveActionExecuter.PARAM_ASSOC_TYPE_QNAME, TEST_CHILD_ASSOC_TYPE_QNAME);
        params.put(MoveActionExecuter.PARAM_ASSOC_QNAME, QName.createQName("{test}ruleCopy"));
        Rule rule = new Rule();
        rule.setRuleType(RuleType.INBOUND);        
        Action action = this.actionService.createAction(CopyActionExecuter.NAME, params);
        ActionCondition condition = this.actionService.createActionCondition(NoConditionEvaluator.NAME);
        action.addActionCondition(condition);
        rule.setAction(action);
        this.ruleService.saveRule(nodeOne, rule);
		
		// Do a deep copy
		NodeRef nodeOneCopy = this.copyService.copy(nodeOne, this.rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName("{test}copiedNodeOne"), true);
		NodeRef nodeTwoCopy = null;
		NodeRef nodeThreeCopy = null;
		NodeRef nodeFourCopy = null;
		
		//System.out.println(
		//		NodeStoreInspector.dumpNodeStore(this.nodeService, this.storeRef));
		
		List<ChildAssociationRef> nodeOneCopyChildren = this.nodeService.getChildAssocs(nodeOneCopy);
		assertNotNull(nodeOneCopyChildren);
		assertEquals(3, nodeOneCopyChildren.size());
		for (ChildAssociationRef nodeOneCopyChild : nodeOneCopyChildren)
		{
			if (nodeOneCopyChild.getQName().equals(nodeTwoAssocName) == true)
			{
				nodeTwoCopy = nodeOneCopyChild.getChildRef();
								
				List<ChildAssociationRef>  nodeTwoCopyChildren = this.nodeService.getChildAssocs(nodeTwoCopy);
				assertNotNull(nodeTwoCopyChildren);
				assertEquals(1, nodeTwoCopyChildren.size());
				for (ChildAssociationRef nodeTwoCopyChild : nodeTwoCopyChildren)
				{
					if (nodeTwoCopyChild.getQName().equals(nodeThreeAssocName) == true)
					{
						nodeThreeCopy = nodeTwoCopyChild.getChildRef();
					}
				}
			}
			else if (nodeOneCopyChild.getQName().equals(nodeFourAssocName) == true)
			{
				nodeFourCopy = nodeOneCopyChild.getChildRef();
			}
		}
		assertNotNull(nodeTwoCopy);
		assertNotNull(nodeThreeCopy);
		assertNotNull(nodeFourCopy);
		
		// Check the non primary child assoc
		List<ChildAssociationRef> children = this.nodeService.getChildAssocs(
                nodeFourCopy,
                RegexQNamePattern.MATCH_ALL,
                TEST_CHILD_ASSOC_QNAME);
		assertNotNull(children);
		assertEquals(1, children.size());
		ChildAssociationRef child = children.get(0);
		assertEquals(child.getChildRef(), nodeThreeCopy);
		
		// Check the node ref property
		NodeRef nodeRef = (NodeRef)this.nodeService.getProperty(nodeOneCopy, PROP_QNAME_MY_NODE_REF);
		assertNotNull(nodeRef);
		assertEquals(nodeThreeCopy, nodeRef);
		
		// Check the any property
		NodeRef anyNodeRef = (NodeRef)this.nodeService.getProperty(nodeOneCopy, PROP_QNAME_MY_ANY);
		assertNotNull(anyNodeRef);
		assertEquals(nodeThreeCopy, anyNodeRef);
		
		// Check the target assoc
		List<AssociationRef> assocs = this.nodeService.getTargetAssocs(nodeTwoCopy, TEST_ASSOC_TYPE_QNAME);
		assertNotNull(assocs);
		assertEquals(1, assocs.size());
		AssociationRef assoc = assocs.get(0);
		assertEquals(assoc.getTargetRef(), nodeThreeCopy);		
		
		// Check that the rule parameter values have been made relative
		List<Rule> rules = this.ruleService.getRules(nodeOneCopy);
		assertNotNull(rules);
		assertEquals(1, rules.size());
		Rule copiedRule = rules.get(0);
		assertNotNull(copiedRule);
		Action ruleAction = copiedRule.getAction();
		assertNotNull(ruleAction);
		NodeRef value = (NodeRef)ruleAction.getParameterValue(MoveActionExecuter.PARAM_DESTINATION_FOLDER);
		assertNotNull(value);
		assertEquals(nodeTwoCopy, value);
	}
	 
	public void testCopyAndRename()
	{
		// Check a normal copy with no dup restrictions
		NodeRef copy = this.copyService.copyAndRename(
				this.sourceNodeRef,
				this.rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
				QName.createQName("{test}copyAssoc"),
				false);		
		checkCopiedNode(this.sourceNodeRef, copy, true, true, false); 		
		assertTrue(TEST_NAME.equals(this.nodeService.getProperty(copy, ContentModel.PROP_NAME)));
		
		// Create a folder and content node		
		Map<QName, Serializable> propsFolder = new HashMap<QName, Serializable>(1);
		propsFolder.put(ContentModel.PROP_NAME, "tempFolder");
		NodeRef folderNode = this.nodeService.createNode(this.rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName("{test}tempFolder"), ContentModel.TYPE_FOLDER, propsFolder).getChildRef();		
		Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
		props.put(ContentModel.PROP_NAME, TEST_NAME);
		NodeRef contentNode = this.nodeService.createNode(folderNode, ContentModel.ASSOC_CONTAINS, QName.createQName("{test}renametest"), ContentModel.TYPE_CONTENT, props).getChildRef();
		
		// Now copy the content node with the duplicate name restriction
		NodeRef contentCopy = this.copyService.copy(contentNode, folderNode, ContentModel.ASSOC_CONTAINS, QName.createQName("{test}bobbins"), false);
		assertFalse(TEST_NAME.equals(this.nodeService.getProperty(contentCopy, ContentModel.PROP_NAME)));
	}
	
	/**
	 * https://issues.alfresco.com/jira/browse/ETWOONE-224
	 */
	public void testETWOONE_244()
	{
		// Create a folder and content node		
		Map<QName, Serializable> propsFolder = new HashMap<QName, Serializable>(1);
		propsFolder.put(ContentModel.PROP_NAME, "tempFolder");
		NodeRef folderNode = this.nodeService.createNode(this.rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "tempFolder"), ContentModel.TYPE_FOLDER, propsFolder).getChildRef();		
		Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
		props.put(ContentModel.PROP_NAME, "myDoc.txt");
		NodeRef contentNode = this.nodeService.createNode(folderNode, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,  "myDoc.txt"), ContentModel.TYPE_CONTENT, props).getChildRef();
		
		NodeRef copy = this.copyService.copyAndRename(contentNode, folderNode, ContentModel.ASSOC_CONTAINS, null, false);
		assertEquals("Copy of myDoc.txt", this.nodeService.getProperty(copy, ContentModel.PROP_NAME));
		QName copyQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Copy of myDoc.txt");
		assertEquals(copyQName, this.nodeService.getPrimaryParent(copy).getQName());
		
		copy = this.copyService.copyAndRename(contentNode, folderNode, ContentModel.ASSOC_CONTAINS, null, false);
		assertEquals("Copy of Copy of myDoc.txt", this.nodeService.getProperty(copy, ContentModel.PROP_NAME));
		copyQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Copy of Copy of myDoc.txt");
		assertEquals(copyQName, this.nodeService.getPrimaryParent(copy).getQName());		

		copy = this.copyService.copyAndRename(contentNode, folderNode, ContentModel.ASSOC_CONTAINS, null, false);
		assertEquals("Copy of Copy of Copy of myDoc.txt", this.nodeService.getProperty(copy, ContentModel.PROP_NAME));
		copyQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Copy of Copy of Copy of myDoc.txt");
		assertEquals(copyQName, this.nodeService.getPrimaryParent(copy).getQName());
	}
	
	/**
	 * Check that the copied node contains the state we are expecting
	 * 
	 * @param sourceNodeRef       the source node reference
	 * @param destinationNodeRef  the destination node reference
	 */
	private void checkCopiedNode(NodeRef sourceNodeRef, NodeRef destinationNodeRef, boolean newCopy, boolean sameStore, boolean copyChildren)
	{
		if (newCopy == true)
		{
			if (sameStore == true)
			{
				// Check that the copy aspect has been applied to the copy
				boolean hasCopyAspect = this.nodeService.hasAspect(destinationNodeRef, ContentModel.ASPECT_COPIEDFROM);
				assertTrue(hasCopyAspect);
				NodeRef copyNodeRef = (NodeRef)this.nodeService.getProperty(destinationNodeRef, ContentModel.PROP_COPY_REFERENCE);
				assertNotNull(copyNodeRef);
				assertEquals(sourceNodeRef, copyNodeRef);
			}
			else
			{
				// Check that destiantion has the same id as the source
				assertEquals(sourceNodeRef.getId(), destinationNodeRef.getId());
            }
		}
		
		boolean hasTestAspect = this.nodeService.hasAspect(destinationNodeRef, TEST_ASPECT_QNAME);
		assertTrue(hasTestAspect);
		
		// Check that all the correct properties have been copied
		Map<QName, Serializable> destinationProperties = this.nodeService.getProperties(destinationNodeRef);
		assertNotNull(destinationProperties);
		String value1 = (String)destinationProperties.get(PROP1_QNAME_MANDATORY);
		assertNotNull(value1);
		assertEquals(TEST_VALUE_1, value1);
		String value2 = (String)destinationProperties.get(PROP2_QNAME_OPTIONAL);
		assertNotNull(value2);
		assertEquals(TEST_VALUE_2, value2);
		String value3 = (String)destinationProperties.get(PROP3_QNAME_MANDATORY);
		assertNotNull(value3);
		assertEquals(TEST_VALUE_1, value3);
		String value4 = (String)destinationProperties.get(PROP4_QNAME_OPTIONAL);
		assertNotNull(value4);
		assertEquals(TEST_VALUE_2, value4);
		
		// Check all the target associations have been copied
		List<AssociationRef> destinationTargets = this.nodeService.getTargetAssocs(destinationNodeRef, TEST_ASSOC_TYPE_QNAME);
		assertNotNull(destinationTargets);
		assertEquals(1, destinationTargets.size());
		AssociationRef nodeAssocRef = destinationTargets.get(0);
		assertNotNull(nodeAssocRef);
		assertEquals(this.targetNodeRef, nodeAssocRef.getTargetRef());
		
		// Check all the child associations have been copied
		List<ChildAssociationRef> childAssocRefs = this.nodeService.getChildAssocs(destinationNodeRef);
		assertNotNull(childAssocRefs);
		int expectedSize = 2;
		if (this.nodeService.hasAspect(destinationNodeRef, RuleModel.ASPECT_RULES) == true)
		{
			expectedSize = expectedSize + 1;
		}
		
		assertEquals(expectedSize, childAssocRefs.size());
		for (ChildAssociationRef ref : childAssocRefs) 
		{
			if (ref.getQName().equals(TEST_CHILD_ASSOC_QNAME2) == true)
			{
				// Since this child is non-primary in the source it will always be non-primary in the destination
				assertFalse(ref.isPrimary());
				assertEquals(this.nonPrimaryChildNodeRef, ref.getChildRef());
			}
			else
			{
				if (copyChildren == false)
				{
					if (ref.getTypeQName().equals(RuleModel.ASSOC_RULE_FOLDER) == true)
					{
						assertTrue(ref.isPrimary());
						assertTrue(this.childNodeRef.equals(ref.getChildRef()) == false);
					}
					else
					{
						assertFalse(ref.isPrimary());
						assertEquals(this.childNodeRef, ref.getChildRef());
					}
				}
				else
				{
					assertTrue(ref.isPrimary());
					assertTrue(this.childNodeRef.equals(ref.getChildRef()) == false);
					
					// TODO need to check that the copied child has all the correct details ..
				}
			}	
		}
	}
}
