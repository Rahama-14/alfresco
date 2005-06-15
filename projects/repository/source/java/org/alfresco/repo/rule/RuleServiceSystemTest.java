/**
 * 
 */
package org.alfresco.repo.rule;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.dictionary.impl.DictionaryBootstrap;
import org.alfresco.repo.rule.action.CheckInActionExecutor;
import org.alfresco.repo.rule.action.CheckOutActionExecutor;
import org.alfresco.repo.rule.action.MoveActionExecutor;
import org.alfresco.repo.rule.action.TransformActionExecutor;
import org.alfresco.repo.rule.condition.MatchTextEvaluator;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleActionDefinition;
import org.alfresco.service.cmr.rule.RuleConditionDefinition;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.rule.RuleServiceException;
import org.alfresco.service.cmr.rule.RuleType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.debug.NodeStoreInspector;
import org.alfresco.util.transaction.UserTransactionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StopWatch;

/**
 * @author Roy Wetherall
 */
public class RuleServiceSystemTest extends TestCase
{
	static ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
	
    private RuleService ruleService;
    private NodeService nodeService;
    private StoreRef testStoreRef;
    private NodeRef rootNodeRef;
    private NodeRef nodeRef;
    private NodeRef configFolder;
    private CheckOutCheckInService cociService;
    private LockService lockService;
	private ContentService contentService;
	private ServiceRegistry serviceRegistry;
    
    /**
     * 
     */
    public RuleServiceSystemTest()
    {
        super();
    }
	
	
    
    //protected void onSetUpInTransaction() throws Exception
    @Override
    protected void setUp() throws Exception 
    {
        // Get the required services
		this.serviceRegistry = (ServiceRegistry)applicationContext.getBean("serviceRegistry");
		this.nodeService = (NodeService)applicationContext.getBean("nodeService");
        this.ruleService = (RuleService)applicationContext.getBean("ruleService");
        this.cociService = (CheckOutCheckInService)applicationContext.getBean("versionOperationsService");
        this.lockService = (LockService)applicationContext.getBean("lockService");
		this.contentService = (ContentService)applicationContext.getBean("contentService");
        
        this.testStoreRef = this.nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
        this.rootNodeRef = this.nodeService.getRootNode(this.testStoreRef);
        
        // Create the node used for tests
        this.nodeRef = this.nodeService.createNode(
                this.rootNodeRef,
				QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
    }

    /**
     * Test:
     *          rule type:  inbound
     *          condition:  no-condition()
     *          action:     add-features(
     *                          aspect-name = versionable)
     */
    public void testAddFeaturesAction()
    {
        this.ruleService.makeActionable(this.nodeRef);
        
        this.nodeService.addAspect(this.nodeRef, DictionaryBootstrap.ASPECT_QNAME_LOCKABLE, null);
        
        RuleType ruleType = this.ruleService.getRuleType("inbound");
        RuleConditionDefinition cond = this.ruleService.getConditionDefintion("no-condition");
        RuleActionDefinition action = this.ruleService.getActionDefinition("add-features");
        
        Map<String, Serializable> params = new HashMap<String, Serializable>(1);
        params.put("aspect-name", DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE);        
        
        Rule rule = this.ruleService.createRule(ruleType);
        rule.addRuleCondition(cond, null);
        rule.addRuleAction(action, params);
        
        this.ruleService.addRule(this.nodeRef, rule);
				
		NodeRef newNodeRef = this.nodeService.createNode(
                this.nodeRef,
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();        
        assertTrue(this.nodeService.hasAspect(newNodeRef, DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE));   
		
		//this.transactionManager.commit(this.transactionStatus);
		
        // System.out.println(NodeStoreInspector.dumpNodeStore(this.nodeService, this.testStoreRef));
    }   
    
    /**
     * Test:
     *          rule type:  inbound
     *          condition:  no-condition()
     *          action:     copy()
     */
    public void testCopyAction()
    {
        this.ruleService.makeActionable(this.nodeRef);
        
        RuleType ruleType = this.ruleService.getRuleType("inbound");
        RuleConditionDefinition cond = this.ruleService.getConditionDefintion("no-condition");
        RuleActionDefinition action = this.ruleService.getActionDefinition("copy");
        
        Map<String, Serializable> params = new HashMap<String, Serializable>(1);
        params.put(MoveActionExecutor.PARAM_DESTINATION_FOLDER, this.rootNodeRef);
        params.put(MoveActionExecutor.PARAM_ASSOC_TYPE_QNAME, DictionaryBootstrap.CHILD_ASSOC_QNAME_CHILDREN);
        params.put(MoveActionExecutor.PARAM_ASSOC_QNAME, QName.createQName(NamespaceService.ALFRESCO_URI, "copy"));
        
        Rule rule = this.ruleService.createRule(ruleType);
        rule.addRuleCondition(cond, null);
        rule.addRuleAction(action, params);
        
        this.ruleService.addRule(this.nodeRef, rule);

        Map<QName, Serializable> props =new HashMap<QName, Serializable>(1);
        props.put(DictionaryBootstrap.PROP_QNAME_NAME, "bobbins");
        
        NodeRef newNodeRef = this.nodeService.createNode(
                this.nodeRef,
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "origional"),
                DictionaryBootstrap.TYPE_QNAME_CMOBJECT,
                props).getChildRef(); 
        
        //System.out.println(NodeStoreInspector.dumpNodeStore(this.nodeService, this.testStoreRef));
        
        // Check that the created node is still there
        List<ChildAssociationRef> origRefs = this.nodeService.getChildAssocs(
                this.nodeRef, 
                QName.createQName(NamespaceService.ALFRESCO_URI, "origional"));
        assertNotNull(origRefs);
        assertEquals(1, origRefs.size());
        NodeRef origNodeRef = origRefs.get(0).getChildRef();
        assertEquals(newNodeRef, origNodeRef);

        // Check that the created node has been copied
        List<ChildAssociationRef> copyChildAssocRefs = this.nodeService.getChildAssocs(
                                                    this.rootNodeRef, 
                                                    QName.createQName(NamespaceService.ALFRESCO_URI, "copy"));
        assertNotNull(copyChildAssocRefs);
        assertEquals(1, copyChildAssocRefs.size());
        NodeRef copyNodeRef = copyChildAssocRefs.get(0).getChildRef();
        assertTrue(this.nodeService.hasAspect(copyNodeRef, DictionaryBootstrap.ASPECT_QNAME_COPIEDFROM));
        NodeRef source = (NodeRef)this.nodeService.getProperty(copyNodeRef, DictionaryBootstrap.PROP_QNAME_COPY_REFERENCE);
        assertEquals(newNodeRef, source);
        
        // TODO test deep copy !!
    }
	
	/**
     * Test:
     *          rule type:  inbound
     *          condition:  no-condition()
     *          action:     transform()
     */
    public void testTransformAction()
    {
        this.ruleService.makeActionable(this.nodeRef);
        
        RuleType ruleType = this.ruleService.getRuleType("inbound");
        RuleConditionDefinition cond = this.ruleService.getConditionDefintion("no-condition");
        RuleActionDefinition action = this.ruleService.getActionDefinition(TransformActionExecutor.NAME);
        
        Map<String, Serializable> params = new HashMap<String, Serializable>(1);
		params.put(TransformActionExecutor.PARAM_MIME_TYPE, MimetypeMap.MIMETYPE_HTML);
        params.put(TransformActionExecutor.PARAM_DESTINATION_FOLDER, this.rootNodeRef);
        params.put(TransformActionExecutor.PARAM_ASSOC_TYPE_QNAME, DictionaryBootstrap.CHILD_ASSOC_QNAME_CHILDREN);
        params.put(TransformActionExecutor.PARAM_ASSOC_QNAME, QName.createQName(NamespaceService.ALFRESCO_URI, "transformed"));
        
        Rule rule = this.ruleService.createRule(ruleType);
        rule.addRuleCondition(cond, null);
        rule.addRuleAction(action, params);
        
        this.ruleService.addRule(this.nodeRef, rule);

        Map<QName, Serializable> props =new HashMap<QName, Serializable>(1);
        props.put(DictionaryBootstrap.PROP_QNAME_NAME, "bobbins.txt");
		props.put(DictionaryBootstrap.PROP_QNAME_ENCODING, "UTF-8");
		props.put(DictionaryBootstrap.PROP_QNAME_MIME_TYPE, MimetypeMap.MIMETYPE_TEXT_PLAIN);
        
		// Create the node at the root
        NodeRef newNodeRef = this.nodeService.createNode(
                this.rootNodeRef,
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "origional"),
                DictionaryBootstrap.TYPE_QNAME_CONTENT,
                props).getChildRef(); 
		
		// Set some content on the origional
		ContentWriter contentWriter = this.contentService.getUpdatingWriter(newNodeRef);
		contentWriter.putContent("This is some test content to be transformed from text into HTML.");
		
		// Move the node into the actionable node (this should fire the transformation rule
		this.nodeService.moveNode(
				newNodeRef, 
				this.nodeRef, 
				QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "origional"));
        
        System.out.println(NodeStoreInspector.dumpNodeStore(this.nodeService, this.testStoreRef));
        
        // Check that the created node is still there
        List<ChildAssociationRef> origRefs = this.nodeService.getChildAssocs(
                this.nodeRef, 
                QName.createQName(NamespaceService.ALFRESCO_URI, "origional"));
        assertNotNull(origRefs);
        assertEquals(1, origRefs.size());
        NodeRef origNodeRef = origRefs.get(0).getChildRef();
        assertEquals(newNodeRef, origNodeRef);

        // Check that the created node has been copied
        List<ChildAssociationRef> copyChildAssocRefs = this.nodeService.getChildAssocs(
                                                    this.rootNodeRef, 
                                                    QName.createQName(NamespaceService.ALFRESCO_URI, "transformed"));
        assertNotNull(copyChildAssocRefs);
        assertEquals(1, copyChildAssocRefs.size());
        NodeRef copyNodeRef = copyChildAssocRefs.get(0).getChildRef();
        assertTrue(this.nodeService.hasAspect(copyNodeRef, DictionaryBootstrap.ASPECT_QNAME_COPIEDFROM));
        NodeRef source = (NodeRef)this.nodeService.getProperty(copyNodeRef, DictionaryBootstrap.PROP_QNAME_COPY_REFERENCE);
        assertEquals(newNodeRef, source);
        
        // Check the transoformed content ..
    }
	
    /**
     * Test:
     *          rule type:  inbound
     *          condition:  no-condition()
     *          action:     move()
     */
    public void testMoveAction()
    {
        this.ruleService.makeActionable(this.nodeRef);
        
        RuleType ruleType = this.ruleService.getRuleType("inbound");
        RuleConditionDefinition cond = this.ruleService.getConditionDefintion("no-condition");
        RuleActionDefinition action = this.ruleService.getActionDefinition("move");
        
        Map<String, Serializable> params = new HashMap<String, Serializable>(1);
        params.put(MoveActionExecutor.PARAM_DESTINATION_FOLDER, this.rootNodeRef);
        params.put(MoveActionExecutor.PARAM_ASSOC_TYPE_QNAME, DictionaryBootstrap.CHILD_ASSOC_QNAME_CHILDREN);
        params.put(MoveActionExecutor.PARAM_ASSOC_QNAME, QName.createQName(NamespaceService.ALFRESCO_URI, "copy"));
        
        Rule rule = this.ruleService.createRule(ruleType);
        rule.addRuleCondition(cond, null);
        rule.addRuleAction(action, params);
        
        this.ruleService.addRule(this.nodeRef, rule);
                
        NodeRef newNodeRef = this.nodeService.createNode(
                this.nodeRef,
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "origional"),
                DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef(); 
        
        //System.out.println(NodeStoreInspector.dumpNodeStore(this.nodeService, this.testStoreRef));
        
        // Check that the created node has been moved
        List<ChildAssociationRef> origRefs = this.nodeService.getChildAssocs(
                this.nodeRef, 
                QName.createQName(NamespaceService.ALFRESCO_URI, "origional"));
        assertNotNull(origRefs);
        assertEquals(0, origRefs.size());

        // Check that the created node is in the new location
        List<ChildAssociationRef> copyChildAssocRefs = this.nodeService.getChildAssocs(
                                                    this.rootNodeRef, 
                                                    QName.createQName(NamespaceService.ALFRESCO_URI, "copy"));
        assertNotNull(copyChildAssocRefs);
        assertEquals(1, copyChildAssocRefs.size());
        NodeRef movedNodeRef = copyChildAssocRefs.get(0).getChildRef();
        assertEquals(newNodeRef, movedNodeRef);
    }
    
    /**
     * Test:
     *          rule type:  inbound
     *          condition:  no-condition()
     *          action:     checkout()
     */
    public void testCheckOutAction()
    {
        this.ruleService.makeActionable(this.nodeRef);
        
        RuleType ruleType = this.ruleService.getRuleType("inbound");
        RuleConditionDefinition cond = this.ruleService.getConditionDefintion("no-condition");
        RuleActionDefinition action = this.ruleService.getActionDefinition(CheckOutActionExecutor.NAME);
        
        Rule rule = this.ruleService.createRule(ruleType);
        rule.addRuleCondition(cond, null);
        rule.addRuleAction(action, null);
        
        this.ruleService.addRule(this.nodeRef, rule);
         
        // Create a new node
        NodeRef newNodeRef = this.nodeService.createNode(
                this.nodeRef,
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "checkout"),
                DictionaryBootstrap.TYPE_QNAME_CMOBJECT).getChildRef();
        
        //System.out.println(NodeStoreInspector.dumpNodeStore(this.nodeService, this.testStoreRef));
        
        // Check that the new node has been checked out
        List<ChildAssociationRef> children = this.nodeService.getChildAssocs(this.nodeRef);
        assertNotNull(children);
        assertEquals(2, children.size());
        for (ChildAssociationRef child : children)
        {
            NodeRef childNodeRef = child.getChildRef();
            if (childNodeRef.equals(newNodeRef) == true)
            {
                // check that the node has been locked
                LockStatus lockStatus = this.lockService.getLockStatus(childNodeRef, LockService.LOCK_USER);
                assertEquals(LockStatus.LOCK_OWNER, lockStatus);
            }
            else if (this.nodeService.hasAspect(childNodeRef, DictionaryBootstrap.ASPECT_QNAME_WORKING_COPY) == true)
            {
                // assert that it is the working copy that relates to the origional node
                NodeRef copiedFromNodeRef = (NodeRef)this.nodeService.getProperty(childNodeRef, DictionaryBootstrap.PROP_QNAME_COPY_REFERENCE);
                assertEquals(newNodeRef, copiedFromNodeRef);
            }
        }
    }
    
    /**
     * Test:
     *          rule type:  inbound
     *          condition:  no-condition()
     *          action:     checkin()
     */
    public void testCheckInAction()
    {
        this.ruleService.makeActionable(this.nodeRef);
        
        RuleType ruleType = this.ruleService.getRuleType("inbound");
        RuleConditionDefinition cond = this.ruleService.getConditionDefintion("no-condition");
        RuleActionDefinition action = this.ruleService.getActionDefinition(CheckInActionExecutor.NAME);
        
        Map<String, Serializable> params = new HashMap<String, Serializable>(1);
        params.put(CheckInActionExecutor.PARAM_DESCRIPTION, "The version description.");
        
        Rule rule = this.ruleService.createRule(ruleType);
        rule.addRuleCondition(cond, null);
        rule.addRuleAction(action, params);
        
        this.ruleService.addRule(this.nodeRef, rule);
         
        // Create a new node and check-it out
        NodeRef newNodeRef = this.nodeService.createNode(
                this.rootNodeRef,
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "origional"),
                DictionaryBootstrap.TYPE_QNAME_CMOBJECT).getChildRef();
        NodeRef workingCopy = this.cociService.checkout(newNodeRef);
        
        // Move the working copy into the actionable folder
        this.nodeService.moveNode(
                workingCopy, 
                this.nodeRef, 
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                QName.createQName(NamespaceService.ALFRESCO_URI, "moved"));
		
		// TODO check that the node has been checked out correctly
    }
    
	/**
     * Test:
     *          rule type:  inbound
     *          condition:  match-text(
     *          				text = .doc,
     *          				operation = CONTAINS)
     *          action:     add-features(
     *                          aspect-name = versionable)
     */
	public void testContainsTextCondition()
	{
		this.ruleService.makeActionable(this.nodeRef);
        
        RuleType ruleType = this.ruleService.getRuleType("inbound");
        RuleConditionDefinition cond = this.ruleService.getConditionDefintion(MatchTextEvaluator.NAME);
        RuleActionDefinition action = this.ruleService.getActionDefinition("add-features");
        
        Map<String, Serializable> actionParams = new HashMap<String, Serializable>(1);
		actionParams.put("aspect-name", DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE);        
        
		Map<String, Serializable> condParams = new HashMap<String, Serializable>(1);
		condParams.put(MatchTextEvaluator.PARAM_TEXT, ".doc");
		
        Rule rule = this.ruleService.createRule(ruleType);
        rule.addRuleCondition(cond, condParams);
        rule.addRuleAction(action, actionParams);
        
        this.ruleService.addRule(this.nodeRef, rule);
		
		// Test condition failure
		Map<QName, Serializable> props1 = new HashMap<QName, Serializable>();
		props1.put(DictionaryBootstrap.PROP_QNAME_NAME, "bobbins.txt");
		NodeRef newNodeRef = this.nodeService.createNode(
                this.nodeRef,
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                DictionaryBootstrap.TYPE_QNAME_CMOBJECT,
                props1).getChildRef();   
        
        Map<QName, Serializable> map = this.nodeService.getProperties(newNodeRef);
        String value = (String)this.nodeService.getProperty(newNodeRef, DictionaryBootstrap.PROP_QNAME_NAME);
        
        assertFalse(this.nodeService.hasAspect(newNodeRef, DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE));  
		
		// Test condition success
		Map<QName, Serializable> props2 = new HashMap<QName, Serializable>();
		props2.put(DictionaryBootstrap.PROP_QNAME_NAME, "bobbins.doc");
		NodeRef newNodeRef2 = this.nodeService.createNode(
                this.nodeRef,
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                DictionaryBootstrap.TYPE_QNAME_CMOBJECT,
                props2).getChildRef();        
        assertTrue(this.nodeService.hasAspect(
                newNodeRef2, 
                DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE)); 
		
		try
		{
			// Test name not set
			NodeRef newNodeRef3 = this.nodeService.createNode(
	                this.nodeRef,
	                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
	                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
	                DictionaryBootstrap.TYPE_QNAME_CMOBJECT).getChildRef();        
		}
		catch (RuleServiceException exception)
		{
			// Correct since text-match is a mandatory property
		}
	}
    
    /**
     * Test:
     *          rule type:  outbound
     *          condition:  no-condition()
     *          action:     add-features(
     *                          aspect-name = versionable)
     */
    public void testOutboundRuleType()
    {
        this.ruleService.makeActionable(this.nodeRef);
        
        this.nodeService.addAspect(this.nodeRef, DictionaryBootstrap.ASPECT_QNAME_LOCKABLE, null);
        
        RuleType ruleType = this.ruleService.getRuleType("outbound");
        RuleConditionDefinition cond = this.ruleService.getConditionDefintion("no-condition");
        RuleActionDefinition action = this.ruleService.getActionDefinition("add-features");
        
        Map<String, Serializable> params = new HashMap<String, Serializable>(1);
        params.put("aspect-name", DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE);        
        
        Rule rule = this.ruleService.createRule(ruleType);
        rule.addRuleCondition(cond, null);
        rule.addRuleAction(action, params);
        
        this.ruleService.addRule(this.nodeRef, rule);
        
        // Create a node
        NodeRef newNodeRef = this.nodeService.createNode(
                this.nodeRef,
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
                DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();        
        assertFalse(this.nodeService.hasAspect(newNodeRef, DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE));
        
        // Move the node out of the actionable folder
        this.nodeService.moveNode(
                newNodeRef, 
                this.rootNodeRef, 
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"),                
                QName.createQName(NamespaceService.ALFRESCO_URI, "children"));
        assertTrue(this.nodeService.hasAspect(newNodeRef, DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE));
        
        //System.out.println(NodeStoreInspector.dumpNodeStore(this.nodeService, this.testStoreRef));
    }
    
    /**
     * Performance guideline test
     *
     */
    public void vtestPerformanceOfRuleExecution()
    {
		try
		{
	        StopWatch sw = new StopWatch();
	        
	        // Create actionable nodes
	        sw.start("create nodes with no rule executed");		
			UserTransaction userTransaction1 = this.serviceRegistry.getUserTransaction();
			userTransaction1.begin();
			
			for (int i = 0; i < 100; i++)
	        {
	            this.nodeService.createNode(
	                    this.nodeRef,
	                    DictionaryBootstrap.CHILD_ASSOC_QNAME_CONTAINS,
	                    DictionaryBootstrap.CHILD_ASSOC_QNAME_CONTAINS,
	                    DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef(); 
	            assertFalse(this.nodeService.hasAspect(nodeRef, DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE));
	        }
				
			userTransaction1.commit();
	        sw.stop();
	        
	        this.ruleService.makeActionable(this.nodeRef);
	        
	        RuleType ruleType = this.ruleService.getRuleType("inbound");
	        RuleConditionDefinition cond = this.ruleService.getConditionDefintion("no-condition");
	        RuleActionDefinition action = this.ruleService.getActionDefinition("add-features");
	        
	        Map<String, Serializable> params = new HashMap<String, Serializable>(1);
	        params.put("aspect-name", DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE);        
	        
	        Rule rule = this.ruleService.createRule(ruleType);
	        rule.addRuleCondition(cond, null);
	        rule.addRuleAction(action, params);
	        
	        this.ruleService.addRule(this.nodeRef, rule);
	        
	        sw.start("create nodes with one rule run (apply versionable aspect)");
			UserTransaction userTransaction2 = this.serviceRegistry.getUserTransaction();
			userTransaction2.begin();
			
			NodeRef[] nodeRefs = new NodeRef[100];
	        for (int i = 0; i < 100; i++)
	        {
	            NodeRef nodeRef = this.nodeService.createNode(
	                    this.nodeRef,
						QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
						QName.createQName(NamespaceService.ALFRESCO_URI, "children"),
	                    DictionaryBootstrap.TYPE_QNAME_CONTAINER).getChildRef();
				nodeRefs[i] = nodeRef;
				
				// Check that the versionable aspect has not yet been applied
				assertFalse(this.nodeService.hasAspect(nodeRef, DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE));
	        }
			
			userTransaction2.commit();
	        sw.stop();
			
			// Check that the versionable aspect has been applied to all the created nodes
			for (NodeRef ref : nodeRefs) 
			{
				assertTrue(this.nodeService.hasAspect(ref, DictionaryBootstrap.ASPECT_QNAME_VERSIONABLE));
			}
	        
	        System.out.println(sw.prettyPrint());
		}
		catch (Exception exception)
		{
			throw new RuntimeException(exception);
		}
    }
}
