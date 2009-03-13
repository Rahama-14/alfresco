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
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.version;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.version.common.counter.VersionCounterService;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test simple version store migration
 */
public class VersionMigratorTest extends BaseVersionStoreTest
{
    private static Log logger = LogFactory.getLog(VersionMigratorTest.class);

    protected VersionServiceImpl version1Service = new VersionServiceImpl();
    
    protected Version2ServiceImpl version2Service;
    protected NodeService versionNodeService;
    
    protected VersionMigrator versionMigrator;
    protected PolicyComponent policyComponent;
    protected DictionaryService dictionaryService;
    protected CheckOutCheckInService cociService;
    protected VersionCounterService versionCounterService;
    
    public VersionMigratorTest()
    {
        //super.setDefaultRollback(false); // default is true
    }
    
    protected void onSetUpInTransaction() throws Exception
    {
        super.onSetUpInTransaction();
        
        this.versionMigrator = (VersionMigrator)applicationContext.getBean("versionMigrator");
        this.policyComponent = (PolicyComponent)applicationContext.getBean("policyComponent");
        this.dictionaryService = (DictionaryService)applicationContext.getBean("dictionaryService");
        this.version2Service = (Version2ServiceImpl)applicationContext.getBean("versionService");
        this.versionNodeService = (NodeService)applicationContext.getBean("versionNodeService"); // note: auto-switches between V1 and V2
        
        this.cociService = (CheckOutCheckInService)applicationContext.getBean("CheckoutCheckinService");
        this.versionCounterService = (VersionCounterService)applicationContext.getBean("versionCounterService");
        
        // Version1Service is used to create the version nodes in Version1Store (workspace://lightWeightVersionStore) 
        version1Service.setDbNodeService(dbNodeService);
        version1Service.setNodeService(dbNodeService);
        version1Service.setVersionCounterService(versionCounterDaoService);
        version1Service.setPolicyComponent(policyComponent);
        version1Service.setDictionaryService(dictionaryService);
        version1Service.initialiseWithoutBind(); // TODO - temp - if use intialise, get: "More than one CalculateVersionLabelPolicy behaviour has been registered for the type {http://www.alfresco.org/model/content/1.0}content"
    
        super.setVersionService(version1Service);
    }
    
	/**
     * Test migration of a simple versioned node (one version, no children)
     */
    public void testMigrateOneVersion() throws Exception
    {
        if (version2Service.useDeprecatedV1 == true)
        {
            logger.info("testMigrateOneVersion: skip");
            return;
        }
        
        NodeRef versionableNode = createNewVersionableNode();
        
        logger.info("testMigrateOneVersion: versionedNodeRef = " + versionableNode);
        
        // Get the next version number
        int nextVersion = peekNextVersionNumber(); 
        String nextVersionLabel = peekNextVersionLabel(versionableNode, nextVersion, versionProperties);
        
        // Snap-shot the date-time
        Date beforeVersionDate = new Date();
        long beforeVersionTime = beforeVersionDate.getTime();
        logger.info("beforeVersion Date/Time: " + beforeVersionDate + " [" + beforeVersionTime + "]");
        
        Version oldVersion = createVersion(versionableNode);
        
        // get and store old version details for later comparison - versionNodeService will retrieve these from the old version store
        
        QName oldVersionType = versionNodeService.getType(oldVersion.getFrozenStateNodeRef());
        Set<QName> oldVersionAspects = versionNodeService.getAspects(oldVersion.getFrozenStateNodeRef());
        Map<QName, Serializable> oldVersionProps = versionNodeService.getProperties(oldVersion.getFrozenStateNodeRef());
        
        logger.info("oldVersion props: " + oldVersion);
        logger.info("oldVersion created: " + oldVersion.getCreatedDate() + " [" + oldVersion.getCreatedDate().getTime()+"]");
        
        logger.info("oldVersion props via versionNodeService: " + oldVersionProps);
        
        VersionHistory vh = version1Service.getVersionHistory(versionableNode);
        assertEquals(1, vh.getAllVersions().size());
         
        NodeRef oldVHNodeRef = version1Service.getVersionHistoryNodeRef(versionableNode);
        
        Thread.sleep(70000);

        // Migrate and delete old version history !
        NodeRef versionedNodeRef = versionMigrator.v1GetVersionedNodeRef(oldVHNodeRef);
        NodeRef newVHNodeRef = versionMigrator.migrateVersionHistory(oldVHNodeRef, versionedNodeRef);
        versionMigrator.v1DeleteVersionHistory(oldVHNodeRef);
        
        VersionHistory vh2 = version2Service.getVersionHistory(versionableNode);
        assertEquals(1, vh2.getAllVersions().size());
        
        Version newVersion = vh2.getRootVersion();
        
        logger.info("newVersion props: " + newVersion);
        logger.info("newVersion created: " + newVersion.getCreatedDate() + " [" + newVersion.getCreatedDate().getTime()+"]");
        
        // check new version - switch to new version service to do the check 
        super.setVersionService(version2Service);
        checkNewVersion(beforeVersionTime, nextVersion, nextVersionLabel, newVersion, versionableNode);
        
        // get and compare new version details - - versionNodeService will retrieve these from the new version store
        
        QName newVersionType = versionNodeService.getType(newVersion.getFrozenStateNodeRef());
        Set<QName> newVersionAspects = versionNodeService.getAspects(newVersion.getFrozenStateNodeRef());
        Map<QName, Serializable> newVersionProps = versionNodeService.getProperties(newVersion.getFrozenStateNodeRef());
        
        logger.info("newVersion props via versionNodeService: " + newVersionProps);
        
        assertEquals(oldVersionType, newVersionType);
        
        assertEquals(oldVersionAspects.size(), newVersionAspects.size());
        for (QName key : oldVersionAspects)
        {
            assertTrue(""+key, newVersionAspects.contains(key));
        }
        
        // TODO review against latest merges
        /*
        assertEquals(oldVersionProps.size(), newVersionProps.size());
        for (QName key : oldVersionProps.keySet())
        {
        	assertEquals(""+key, oldVersionProps.get(key), newVersionProps.get(key));
        }
        */
        assertEquals(oldVersionProps.size(), newVersionProps.size()+1);
        for (QName key : oldVersionProps.keySet())
        {
        	if (! key.equals(ContentModel.PROP_ACCESSED))
        	{
        		assertEquals(""+key, oldVersionProps.get(key), newVersionProps.get(key));
        	}
        }

        logger.info("testMigrateOneVersion: Migrated from oldVHNodeRef = " + oldVHNodeRef + " to newVHNodeRef = " + newVHNodeRef);
    }    
    
    /**
     * Test migration of a multiple versioned nodes
     */
    public void testMigrateMultipleVersions() throws Exception
    {
        if (version2Service.useDeprecatedV1 == true)
        {
            logger.info("testMigrateOneVersion: skip");
            return;
        }
        
        NodeRef versionableNode = createNewVersionableNode();
        
        // Get the next version number, next version label and snapshot the date-time
        int nextVersion1 = peekNextVersionNumber(); 
        String nextVersionLabel1 = peekNextVersionLabel(versionableNode, nextVersion1, versionProperties);
        long beforeVersionTime1 = System.currentTimeMillis();
        
        Version version1 = createVersion(versionableNode);
        logger.info(version1);
        
        // Get the next version number, next version label and snapshot the date-time
        int nextVersion2 = peekNextVersionNumber(); 
        String nextVersionLabel2 = peekNextVersionLabel(versionableNode, nextVersion2, versionProperties);
        long beforeVersionTime2 = System.currentTimeMillis();
        
        Version version2 = createVersion(versionableNode);
        logger.info(version2);
        
        // Get the next version number, next version label and snapshot the date-time
        int nextVersion3 = peekNextVersionNumber(); 
        String nextVersionLabel3 = peekNextVersionLabel(versionableNode, nextVersion3, versionProperties);
        long beforeVersionTime3 = System.currentTimeMillis();
        
        Version version3 = createVersion(versionableNode);
        logger.info(version3);
        
        VersionHistory vh1 = version1Service.getVersionHistory(versionableNode);
        assertEquals(3, vh1.getAllVersions().size());
        
        logger.info("testMigrateMultipleVersions: versionedNodeRef = " + versionableNode);
        
        NodeRef oldVHNodeRef = version1Service.getVersionHistoryNodeRef(versionableNode);

        // Migrate and delete old version history !
        NodeRef versionedNodeRef = versionMigrator.v1GetVersionedNodeRef(oldVHNodeRef);
        NodeRef newVHNodeRef = versionMigrator.migrateVersionHistory(oldVHNodeRef, versionedNodeRef);
        versionMigrator.v1DeleteVersionHistory(oldVHNodeRef);
        
        VersionHistory vh2 = version2Service.getVersionHistory(versionableNode);
        assertEquals(3, vh2.getAllVersions().size());
        
        // TODO move check version history into BaseVersionStoreTest
        // check new versions - switch to new version service to do the check
        super.setVersionService(version2Service);
        
        Version[] newVersions = vh2.getAllVersions().toArray(new Version[]{});
        
        checkVersion(beforeVersionTime1, nextVersion1, nextVersionLabel1, newVersions[2], versionableNode);
        checkVersion(beforeVersionTime2, nextVersion2, nextVersionLabel2, newVersions[1], versionableNode);
        checkNewVersion(beforeVersionTime3, nextVersion3, nextVersionLabel3, newVersions[0], versionableNode);
        
        logger.info("testMigrateMultipleVersions: Migrated from oldVHNodeRef = " + oldVHNodeRef + " to newVHNodeRef = " + newVHNodeRef);
    }
    
    public void test_ETHREEOH_1540() throws Exception
    {
        // Create the node used for tests
        NodeRef nodeRef = nodeService.createNode(
                rootNodeRef, 
                ContentModel.ASSOC_CHILDREN, 
                QName.createQName("{test}MyVersionableNode"),
                TEST_TYPE_QNAME,
                this.nodeProperties).getChildRef();
        
        nodeService.addAspect(nodeRef, ContentModel.ASPECT_TITLED, null);
        nodeService.setProperty(nodeRef, ContentModel.PROP_NAME, "name");
        
        // Add the initial content to the node
        ContentWriter contentWriter = this.contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
        contentWriter.setMimetype("text/plain");
        contentWriter.setEncoding("UTF-8");
        contentWriter.putContent("my content");
        
        VersionHistory vh1 = version1Service.getVersionHistory(nodeRef);
        assertNull(vh1);
        
        version2Service.useDeprecatedV1 = true;
        
        // note: for testing only - not recommended !!!
        versionCounterService.setVersionNumber(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, VersionModel.STORE_ID), 100);
        
        // Add the version aspect to the created node
        nodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, null);
        
        vh1 = version1Service.getVersionHistory(nodeRef);
        assertNull(vh1);
        
        NodeRef workingCopyNodeRef = cociService.checkout(nodeRef);
        
        vh1 = version1Service.getVersionHistory(nodeRef);
        assertNull(vh1);

        int v1count = 3;
        
        for (int i = 1; i <= v1count; i++)
        {
            Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
            versionProperties.put(Version.PROP_DESCRIPTION, "This is a test checkin - " + i);
            
            cociService.checkin(workingCopyNodeRef, versionProperties);
            
            vh1 = version1Service.getVersionHistory(nodeRef);
            assertEquals(i, vh1.getAllVersions().size());
            
            workingCopyNodeRef = cociService.checkout(nodeRef);
            
            vh1 = version1Service.getVersionHistory(nodeRef);
            assertEquals(i, vh1.getAllVersions().size());
        }

        NodeRef oldVHNodeRef = version1Service.getVersionHistoryNodeRef(nodeRef);
        
        version2Service.useDeprecatedV1 = false;

        // Migrate and delete old version history !
        NodeRef versionedNodeRef = versionMigrator.v1GetVersionedNodeRef(oldVHNodeRef);
        
        //int nextVersionNumber = versionCounterService.nextVersionNumber(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, VersionModel.STORE_ID));
        //versionCounterService.setVersionNumber(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, Version2Model.STORE_ID), nextVersionNumber);
        
        // to force the error: https://issues.alfresco.com/jira/browse/ETHREEOH-1540
        versionCounterService.setVersionNumber(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, Version2Model.STORE_ID), 0);
        
        NodeRef newVHNodeRef = versionMigrator.migrateVersionHistory(oldVHNodeRef, versionedNodeRef);
        versionMigrator.v1DeleteVersionHistory(oldVHNodeRef);
        
        VersionHistory vh2 = version2Service.getVersionHistory(nodeRef);
        assertEquals(v1count, vh2.getAllVersions().size());
        
        int v2count = 3;
        
        for (int i = 1; i <= v2count; i++)
        {
            versionProperties = new HashMap<String, Serializable>();
            versionProperties.put(Version.PROP_DESCRIPTION, "This is a test checkin - " + (v1count + i));
            
            cociService.checkin(workingCopyNodeRef, versionProperties);
            
            vh2 = version2Service.getVersionHistory(nodeRef);
            assertEquals((v1count + i), vh2.getAllVersions().size());
            
            workingCopyNodeRef = cociService.checkout(nodeRef);
            
            vh2 = version2Service.getVersionHistory(nodeRef);
            assertEquals((v1count + i), vh2.getAllVersions().size());
        }
        
        logger.info("testMigrateOneCheckoutVersion: Migrated from oldVHNodeRef = " + oldVHNodeRef + " to newVHNodeRef = " + newVHNodeRef);
    }    
}
