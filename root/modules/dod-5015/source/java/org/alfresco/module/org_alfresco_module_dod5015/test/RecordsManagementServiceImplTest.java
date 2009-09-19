/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.org_alfresco_module_dod5015.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.DOD5015Model;
import org.alfresco.module.org_alfresco_module_dod5015.DispositionAction;
import org.alfresco.module.org_alfresco_module_dod5015.DispositionActionDefinition;
import org.alfresco.module.org_alfresco_module_dod5015.DispositionSchedule;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.module.org_alfresco_module_dod5015.action.RecordsManagementActionService;
import org.alfresco.module.org_alfresco_module_dod5015.action.impl.FileAction;
import org.alfresco.module.org_alfresco_module_dod5015.event.RecordsManagementEvent;
import org.alfresco.module.org_alfresco_module_dod5015.event.RecordsManagementEventService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Period;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.BaseSpringTest;

/**
 * 
 * 
 * @author Roy Wetherall
 */
public class RecordsManagementServiceImplTest extends BaseSpringTest implements RecordsManagementModel
{    
	protected static StoreRef SPACES_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	
	private NodeRef filePlan;
	
	private ImporterService importService;
	private NodeService nodeService;
	private NodeService unprotectedNodeService;
	private PermissionService permissionService;
    private RecordsManagementActionService rmActionService;
    private RecordsManagementEventService rmEventService;
    private RecordsManagementService rmService;
	private SearchService searchService;
	private TransactionService transactionService;
	private RetryingTransactionHelper transactionHelper;
	
	@Override
	protected void onSetUpInTransaction() throws Exception 
	{
		super.onSetUpInTransaction();

		// Get the service required in the tests
		this.nodeService = (NodeService)this.applicationContext.getBean("NodeService"); 
		this.unprotectedNodeService = (NodeService)this.applicationContext.getBean("nodeService"); 
		this.importService = (ImporterService)this.applicationContext.getBean("importerComponent");
		this.transactionService = (TransactionService)this.applicationContext.getBean("TransactionService");
		this.searchService = (SearchService)this.applicationContext.getBean("searchService");
        this.rmActionService = (RecordsManagementActionService)this.applicationContext.getBean("recordsManagementActionService");
        this.rmEventService = (RecordsManagementEventService)this.applicationContext.getBean("recordsManagementEventService");
        this.rmService = (RecordsManagementService)this.applicationContext.getBean("recordsManagementService");
		this.permissionService = (PermissionService)this.applicationContext.getBean("PermissionService");
		this.transactionHelper = (RetryingTransactionHelper)this.applicationContext.getBean("retryingTransactionHelper");

		// Set the current security context as admin
		AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
		
		// Get the test data
		setUpTestData();    
	}
	
	private void setUpTestData()
	{
        filePlan = TestUtilities.loadFilePlanData(applicationContext);
	}

    @Override
    protected void onTearDownInTransaction() throws Exception
    {
        try
        {
            UserTransaction txn = transactionService.getUserTransaction(false);
            txn.begin();
            this.nodeService.deleteNode(filePlan);
            txn.commit();
        }
        catch (Exception e)
        {
            // Nothing
            //System.out.println("DID NOT DELETE FILE PLAN!");
        }
    }
    
    public void testDispositionPresence() throws Exception
    {
        // create a record category node in 
        NodeRef rootNode = this.nodeService.getRootNode(SPACES_STORE);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
        String recordCategoryName = "Test Record Category";
        props.put(ContentModel.PROP_NAME, recordCategoryName);
        NodeRef nodeRef = this.nodeService.createNode(rootNode, ContentModel.ASSOC_CHILDREN, 
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(recordCategoryName)), 
                    DOD5015Model.TYPE_RECORD_CATEGORY, props).getChildRef();
        
        setComplete();
        endTransaction();        
        UserTransaction txn = transactionService.getUserTransaction(false);
        txn.begin();
        
        // ensure the record category node has the scheduled aspect and the disposition schedule association
        assertTrue(this.nodeService.hasAspect(nodeRef, RecordsManagementModel.ASPECT_SCHEDULED));
        List<ChildAssociationRef> scheduleAssocs = this.nodeService.getChildAssocs(nodeRef, ASSOC_DISPOSITION_SCHEDULE, RegexQNamePattern.MATCH_ALL);
        
        
        assertNotNull(scheduleAssocs);
        assertEquals(1, scheduleAssocs.size());
        
        // test retrieval of the disposition schedule via RM service
        DispositionSchedule schedule = this.rmService.getDispositionSchedule(nodeRef);
        assertNotNull(schedule);
        
        txn.commit();
    }
    
    /**
     * This test method contains a subset of the tests in TC 7-2 of the DoD doc.
     * @throws Exception
     */
    public void testRescheduleRecord_IsNotCutOff() throws Exception
    {
         final NodeRef recCat = TestUtilities.getRecordCategory(searchService, "Reports", "AIS Audit Records");
        // This RC has disposition instructions "Cut off monthly, hold 1 month, then destroy."
        
        setComplete();
        endTransaction();

        // Create a suitable folder for this test.
        final NodeRef testFolder = transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>()
                {
                    public NodeRef execute() throws Throwable
                    {
                        Map<QName, Serializable> folderProps = new HashMap<QName, Serializable>(1);
                        String folderName = "testFolder" + System.currentTimeMillis();
                        folderProps.put(ContentModel.PROP_NAME, folderName);
                        NodeRef recordFolder = nodeService.createNode(recCat,
                                                                      ContentModel.ASSOC_CONTAINS, 
                                                                      QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, folderName), 
                                                                      TYPE_RECORD_FOLDER).getChildRef();
                        return recordFolder;
                    }          
                });
        
        // Create a record in the test folder. File it and declare it.
        final NodeRef testRecord = transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>()
                {
                    public NodeRef execute() throws Throwable
                    {
                        final NodeRef result = nodeService.createNode(testFolder, ContentModel.ASSOC_CONTAINS,
                                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
                                        "Record" + System.currentTimeMillis() + ".txt"),
                                ContentModel.TYPE_CONTENT).getChildRef();
                        
                        rmActionService.executeRecordsManagementAction(result, "file");
                        TestUtilities.declareRecord(result, unprotectedNodeService, rmActionService);
                        return result;
                    }          
                });        

        assertTrue("recCat missing scheduled aspect", nodeService.hasAspect(recCat, RecordsManagementModel.ASPECT_SCHEDULED));
        assertFalse("folder should not have scheduled aspect", nodeService.hasAspect(testFolder, RecordsManagementModel.ASPECT_SCHEDULED));
        assertFalse("record should not have scheduled aspect", nodeService.hasAspect(testRecord, RecordsManagementModel.ASPECT_SCHEDULED));

        assertFalse("recCat should not have dispositionLifecycle aspect", nodeService.hasAspect(recCat, RecordsManagementModel.ASPECT_DISPOSITION_LIFECYCLE));
        assertTrue("testFolder missing dispositionLifecycle aspect", nodeService.hasAspect(testFolder, RecordsManagementModel.ASPECT_DISPOSITION_LIFECYCLE));
        assertFalse("testRecord should not have dispositionLifecycle aspect", nodeService.hasAspect(testRecord, RecordsManagementModel.ASPECT_DISPOSITION_LIFECYCLE));

        // Change the cutoff conditions for the associated record category
        final Date dateBeforeChange = transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Date>()
                {
                    public Date execute() throws Throwable
                    {
                        Date asOfDate = rmService.getNextDispositionAction(testFolder).getAsOfDate();
                        System.out.println("Going to change the disposition asOf Date.");
                        System.out.println(" - Original value: " + asOfDate);

                        // Now change "Cut off monthly, hold 1 month, then destroy."
                        //    to "Cut off yearly, hold 1 month, then destroy."
                        List<DispositionActionDefinition> dads = rmService.getDispositionSchedule(testFolder).getDispositionActionDefinitions();
                        DispositionActionDefinition firstDAD = dads.get(0);
                        assertEquals("cutoff", firstDAD.getName());
                        NodeRef dadNode = firstDAD.getNodeRef();
                        
                        nodeService.setProperty(dadNode, PROP_DISPOSITION_PERIOD, new Period("year|1"));

                        return asOfDate;
                    }          
                });

        // view the record metadata to verify that the record has been rescheduled.
        transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                        DispositionAction nextDispositionAction = rmService.getNextDispositionAction(testFolder);
                        
                        assertEquals("cutoff", nextDispositionAction.getName());
                        Date asOfDateAfterChange = nextDispositionAction.getAsOfDate();
                        System.out.println(" - Updated  value: " + asOfDateAfterChange);
                        
                        assertFalse("Expected disposition asOf date to change.", asOfDateAfterChange.equals(dateBeforeChange));
                        return null;
                    }          
                });

        // Change the disposition type (e.g. time-based to event-based)
        transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                        List<RecordsManagementEvent> rmes = rmService.getNextDispositionAction(testFolder).getDispositionActionDefinition().getEvents();
                        System.out.println("Going to change the RMEs.");
                        System.out.println(" - Original value: " + rmes);

                        List<DispositionActionDefinition> dads = rmService.getDispositionSchedule(testFolder).getDispositionActionDefinitions();
                        DispositionActionDefinition firstDAD = dads.get(0);
                        assertEquals("cutoff", firstDAD.getName());
                        NodeRef dadNode = firstDAD.getNodeRef();
                        
//                        nodeService.setProperty(dadNode, PROP_DISPOSITION_PERIOD, null);
                        List<String> eventNames= new ArrayList<String>();
                        eventNames.add("study_complete");
                        nodeService.setProperty(dadNode, PROP_DISPOSITION_EVENT, (Serializable)eventNames);

                        return null;
                    }          
                });
        // Now add a second event to the same 
        transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                        DispositionAction nextDispositionAction = rmService.getNextDispositionAction(testFolder);
                        StringBuilder buf = new StringBuilder();
                        for (RecordsManagementEvent e : nextDispositionAction.getDispositionActionDefinition().getEvents()) {
                            buf.append(e.getName()).append(',');
                        }

                        System.out.println("Going to change the RMEs again.");
                        System.out.println(" - Original value: " + buf.toString());

                        List<DispositionActionDefinition> dads = rmService.getDispositionSchedule(testFolder).getDispositionActionDefinitions();
                        DispositionActionDefinition firstDAD = dads.get(0);
                        assertEquals("cutoff", firstDAD.getName());
                        NodeRef dadNode = firstDAD.getNodeRef();
                        
                        List<String> eventNames= new ArrayList<String>();
                        eventNames.add("study_complete");
                        eventNames.add("case_complete");
                        nodeService.setProperty(dadNode, PROP_DISPOSITION_EVENT, (Serializable)eventNames);

                        return null;
                    }          
                });

        // View the record metadata to verify that the record has been rescheduled.
        transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                        DispositionAction nextDispositionAction = rmService.getNextDispositionAction(testFolder);
                        
                        assertEquals("cutoff", nextDispositionAction.getName());
                        StringBuilder buf = new StringBuilder();
                        for (RecordsManagementEvent e : nextDispositionAction.getDispositionActionDefinition().getEvents()) {
                            buf.append(e.getName()).append(',');
                        }
                        System.out.println(" - Updated  value: " + buf.toString());
                        
                        assertFalse("Disposition should not be eligible.", nextDispositionAction.isEventsEligible());
                        return null;
                    }          
                });

        // Tidy up test nodes.
        transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                        nodeService.deleteNode(testRecord);

                        // Change the disposition Period back to what it was.
                        List<DispositionActionDefinition> dads = rmService.getDispositionSchedule(testFolder).getDispositionActionDefinitions();
                        DispositionActionDefinition firstDAD = dads.get(0);
                        assertEquals("cutoff", firstDAD.getName());
                        NodeRef dadNode = firstDAD.getNodeRef();
                        nodeService.setProperty(dadNode, PROP_DISPOSITION_PERIOD, new Period("month|1"));

                        nodeService.deleteNode(testFolder);

                        return null;
                    }          
                });
    }
    
	public void testGetDispositionInstructions() throws Exception
	{	
	    // Get a record
	    // TODO
	    
	    // Get a record folder
	    NodeRef folderRecord = TestUtilities.getRecordFolder(searchService, "Reports", "AIS Audit Records", "January AIS Audit Records");
	    assertNotNull(folderRecord);
	    assertEquals("January AIS Audit Records", this.nodeService.getProperty(folderRecord, ContentModel.PROP_NAME));
	    
	    assertFalse(rmService.isRecord(folderRecord));
	    assertTrue(rmService.isRecordFolder(folderRecord));
	    assertFalse(rmService.isRecordsManagementContainer(folderRecord));	 
	    
	    DispositionSchedule di = this.rmService.getDispositionSchedule(folderRecord);
	    assertNotNull(di);
	    assertEquals("N1-218-00-4 item 023", di.getDispositionAuthority());
	    assertEquals("Cut off monthly, hold 1 month, then destroy.", di.getDispositionInstructions());
	    assertFalse(di.isRecordLevelDisposition());
	    
	    // Get a record category
	    NodeRef recordCategory = TestUtilities.getRecordCategory(this.searchService, "Reports", "AIS Audit Records");    
	    assertNotNull(recordCategory);
	    assertEquals("AIS Audit Records", this.nodeService.getProperty(recordCategory, ContentModel.PROP_NAME));
        	    
	    assertFalse(rmService.isRecord(recordCategory));
        assertFalse(rmService.isRecordFolder(recordCategory));
        assertTrue(rmService.isRecordsManagementContainer(recordCategory));   
        
        di = this.rmService.getDispositionSchedule(recordCategory);
        assertNotNull(di);
        assertEquals("N1-218-00-4 item 023", di.getDispositionAuthority());
        assertEquals("Cut off monthly, hold 1 month, then destroy.", di.getDispositionInstructions());
        assertFalse(di.isRecordLevelDisposition());
        
        List<DispositionActionDefinition> das = di.getDispositionActionDefinitions();
        assertNotNull(das);
        assertEquals(2, das.size());
        assertEquals("cutoff", das.get(0).getName());
        assertEquals("destroy", das.get(1).getName());
    }
    
	public void xxxtestUpdateNextDispositionAction()
	{
	    FileAction fileAction = (FileAction)applicationContext.getBean("file");
	    
	    // Get a record folder
        NodeRef folderRecord = TestUtilities.getRecordFolder(searchService, "Reports", "AIS Audit Records", "January AIS Audit Records");
        assertNotNull(folderRecord);
        assertEquals("January AIS Audit Records", this.nodeService.getProperty(folderRecord, ContentModel.PROP_NAME));
        
        DispositionSchedule di = this.rmService.getDispositionSchedule(folderRecord);
        assertNotNull(di);
        assertEquals("N1-218-00-4 item 023", di.getDispositionAuthority());
        assertEquals("Cut off monthly, hold 1 month, then destroy.", di.getDispositionInstructions());
        assertFalse(di.isRecordLevelDisposition());
        
        assertFalse(this.nodeService.hasAspect(folderRecord, ASPECT_DISPOSITION_LIFECYCLE));
        
        fileAction.updateNextDispositionAction(folderRecord);
        
        
        // Check the next disposition action
        assertTrue(this.nodeService.hasAspect(folderRecord, ASPECT_DISPOSITION_LIFECYCLE));
        NodeRef ndNodeRef = this.nodeService.getChildAssocs(folderRecord, ASSOC_NEXT_DISPOSITION_ACTION, RegexQNamePattern.MATCH_ALL).get(0).getChildRef();
        assertNotNull(ndNodeRef);
        assertEquals("cutoff", this.nodeService.getProperty(ndNodeRef, PROP_DISPOSITION_ACTION));
        assertEquals(di.getDispositionActionDefinitions().get(0).getId(), this.nodeService.getProperty(ndNodeRef, PROP_DISPOSITION_ACTION_ID));
        assertNotNull(this.nodeService.getProperty(ndNodeRef, PROP_DISPOSITION_AS_OF));
        
        // Check the history is empty
        // TODO        
        
        Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
        props.put(PROP_CUT_OFF_DATE, new Date());
        this.unprotectedNodeService.addAspect(folderRecord, ASPECT_CUT_OFF, props);        
        fileAction.updateNextDispositionAction(folderRecord);
        
        assertTrue(this.nodeService.hasAspect(folderRecord, ASPECT_DISPOSITION_LIFECYCLE));
        ndNodeRef = this.nodeService.getChildAssocs(folderRecord, ASSOC_NEXT_DISPOSITION_ACTION, RegexQNamePattern.MATCH_ALL).get(0).getChildRef();
        assertNotNull(ndNodeRef);
        assertEquals("destroy", this.nodeService.getProperty(ndNodeRef, PROP_DISPOSITION_ACTION));
        assertEquals(di.getDispositionActionDefinitions().get(1).getId(), this.nodeService.getProperty(ndNodeRef, PROP_DISPOSITION_ACTION_ID));
        assertNotNull(this.nodeService.getProperty(ndNodeRef, PROP_DISPOSITION_AS_OF));
        
        // Check the history has an action
        // TODO
        
        fileAction.updateNextDispositionAction(folderRecord);
        
        assertTrue(this.nodeService.hasAspect(folderRecord, ASPECT_DISPOSITION_LIFECYCLE));
        assertTrue(this.nodeService.getChildAssocs(folderRecord, ASSOC_NEXT_DISPOSITION_ACTION, RegexQNamePattern.MATCH_ALL).isEmpty());
        
        // Check the history has both actions
        // TODO
	}
	
}
