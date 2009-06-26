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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionServiceException;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ApplicationContextHelper;
import org.alfresco.util.GUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

/**
 * versionService test class.
 * 
 * @author Roy Wetherall, janv
 */
public class VersionServiceImplTest extends BaseVersionStoreTest
{
    private static Log logger = LogFactory.getLog(VersionServiceImplTest.class);

    private static final String UPDATED_VALUE_1 = "updatedValue1";
	private static final String UPDATED_VALUE_2 = "updatedValue2";
	private static final String UPDATED_VALUE_3 = "updatedValue3";
	private static final String UPDATED_CONTENT_1 = "updatedContent1";
	private static final String UPDATED_CONTENT_2 = "updatedContent2";

	public void testSetup()
    {
	    // NOOP
    }
	
	/**
     * Tests the creation of the initial version of a versionable node
     */
    public void testCreateIntialVersion()
    {
        NodeRef versionableNode = createNewVersionableNode();
        createVersion(versionableNode);
    }
    
    /**
     * Test creating a version history with many versions from the same workspace
     */
    public void testCreateManyVersionsSameWorkspace()
    {
        NodeRef versionableNode = createNewVersionableNode();
        createVersion(versionableNode);
        // TODO mess with some of the properties and stuff as you version
        createVersion(versionableNode);
        // TODO mess with some of the properties and stuff as you version
        createVersion(versionableNode);
        
        VersionHistory vh = this.versionService.getVersionHistory(versionableNode);
        assertNotNull(vh);
        assertEquals(3, vh.getAllVersions().size());
        
        // TODO check list of versions ... !
    }
    
    /**
     * Tests the creation of multiple versions of a versionable node with null version properties
     */
    public void testCreateManyVersionsWithNullVersionProperties()
    {
        this.versionProperties = null;
        
        NodeRef versionableNode = createNewVersionableNode();
        createVersion(versionableNode);
        createVersion(versionableNode);
        createVersion(versionableNode);
        
        VersionHistory vh = this.versionService.getVersionHistory(versionableNode);
        assertNotNull(vh);
        assertEquals(3, vh.getAllVersions().size());
    }
    
    /**
     * Test versioning a non versionable node ie: no version apsect
     */
    public void testCreateInitialVersionWhenNotVersionable()
    {
        NodeRef node = createNewNode(); // not marked as versionable
        createVersion(node);
    }
    
    // TODO test versioning numberious times with branchs implies by different workspaces
    
    /**
     * Test versioning the children of a verionable node
     */
    public void testVersioningChildren()
    {
        NodeRef versionableNode = createNewVersionableNode();
        
        // Snap shot data
        int expectedVersionNumber = peekNextVersionNumber();
		String expectedVersionLabel = peekNextVersionLabel(versionableNode, expectedVersionNumber, versionProperties);
		
		// Snap-shot the node created date-time
        long beforeVersionTime = ((Date)nodeService.getProperty(versionableNode, ContentModel.PROP_CREATED)).getTime();
        
        // Version the node and its children
        Collection<Version> versions = this.versionService.createVersion(
                versionableNode, 
                this.versionProperties,
                true);
        
        // Check the returned versions are correct
        CheckVersionCollection(expectedVersionNumber, expectedVersionLabel, beforeVersionTime, versions);
        
        // TODO check the version history is correct
    }	
    
    /**
     * Test versioning many nodes in one go
     */
    public void testVersioningManyNodes()
    {
        NodeRef versionableNode = createNewVersionableNode();
        
        // Snap shot data
        int expectedVersionNumber = peekNextVersionNumber(); 
		String expectedVersionLabel = peekNextVersionLabel(versionableNode, expectedVersionNumber, versionProperties);  
		
		// Snap-shot the node created date-time
        long beforeVersionTime = ((Date)nodeService.getProperty(versionableNode, ContentModel.PROP_CREATED)).getTime();
        
        // Version the list of nodes created
        Collection<Version> versions = this.versionService.createVersion(
                this.versionableNodes.values(),
                this.versionProperties);
        
        // Check the returned versions are correct
        CheckVersionCollection(expectedVersionNumber, expectedVersionLabel, beforeVersionTime, versions);     

        // TODO check the version histories
    }
    
    /**
     * Helper method to check the validity of the list of newly created versions.
     * 
     * @param expectedVersionNumber  the expected version number that all the versions should have
     * @param beforeVersionTime      the time before the versions where created
     * @param versions               the collection of version objects
     */
    private void CheckVersionCollection(int expectedVersionNumber, String expectedVersionLabel, long beforeVersionTime, Collection<Version> versions)
    {
        for (Version version : versions)
        {
            // Get the frozen id from the version
            String frozenNodeId = null;
            
            // Switch VersionStore depending on configured impl
            if (versionService.getVersionStoreReference().getIdentifier().equals(Version2Model.STORE_ID))
            {
                // V2 version store (eg. workspace://version2Store)
                frozenNodeId = ((NodeRef)version.getVersionProperty(Version2Model.PROP_FROZEN_NODE_REF)).getId();
            } 
            else if (versionService.getVersionStoreReference().getIdentifier().equals(VersionModel.STORE_ID))
            {
                // Deprecated V1 version store (eg. workspace://lightWeightVersionStore)
                frozenNodeId = (String)version.getVersionProperty(VersionModel.PROP_FROZEN_NODE_ID);
            }
            
            assertNotNull("Unable to retrieve the frozen node id from the created version.", frozenNodeId);
            
            // Get the origional node ref (based on the forzen node)
            NodeRef originalNodeRef = this.versionableNodes.get(frozenNodeId);
            assertNotNull("The versionable node ref that relates to the frozen node id can not be found.", originalNodeRef);
            
            // Check the new version
            checkNewVersion(beforeVersionTime, expectedVersionNumber, expectedVersionLabel, version, originalNodeRef);
        }
    }
    
    private void CheckVersionHistory(VersionHistory vh, List<Version> expectedVersions)
    {
        if (vh == null)
        {
            assertNull(expectedVersions);
        }
        else
        {
            Iterator<Version> itr = expectedVersions.iterator();
            
            for (Version version : vh.getAllVersions())
            {
                Version expectedVersion = itr.next();
                
                assertEquals(version.getVersionLabel(), expectedVersion.getVersionLabel());
                assertEquals(version.getFrozenStateNodeRef(), expectedVersion.getFrozenStateNodeRef());
            }
            
            assertFalse(itr.hasNext());
        }
    }
    
    /**
     * Tests the version history
     */
    public void testNoVersionHistory()
    {
        NodeRef nodeRef = createNewVersionableNode();
        
        VersionHistory vh = this.versionService.getVersionHistory(nodeRef);
        assertNull(vh);
    }
    
    /**
     * Tests getVersionHistory when all the entries in the version history
     * are from the same workspace.
     */
    public void testGetVersionHistorySameWorkspace()
    {
        NodeRef versionableNode = createNewVersionableNode();
        
        Version version1 = addToVersionHistory(versionableNode, null);
        Version version2 = addToVersionHistory(versionableNode, version1);
        Version version3 = addToVersionHistory(versionableNode, version2);
        Version version4 = addToVersionHistory(versionableNode, version3);
        addToVersionHistory(versionableNode, version4);    
    }
    
    /**
     * Adds another version to the version history then checks that getVersionHistory is returning
     * the correct data.
     * 
     * @param versionableNode  the versionable node reference
     * @param parentVersion    the parent version
     */
    private Version addToVersionHistory(NodeRef versionableNode, Version parentVersion)
    {
        Version createdVersion = createVersion(versionableNode);
        
        VersionHistory vh = this.versionService.getVersionHistory(versionableNode);
        assertNotNull("The version history should not be null since we know we have versioned this node.", vh);
        
        if (parentVersion == null)
        {
            // Check the root is the newly created version
            Version root = vh.getRootVersion();
            assertNotNull(
                    "The root version should never be null, since every version history ust have a root version.", 
                    root);
            assertEquals(createdVersion.getVersionLabel(), root.getVersionLabel());
        }
        
        // Get the version from the version history
        Version version = vh.getVersion(createdVersion.getVersionLabel());
        assertNotNull(version);
        assertEquals(createdVersion.getVersionLabel(), version.getVersionLabel());
        
        // Check that the version is a leaf node of the version history (since it is newly created)
        Collection<Version> suc = vh.getSuccessors(version);
        assertNotNull(suc);
        assertEquals(0, suc.size());
        
        // Check that the predessor is the passed parent version (if root version should be null)
        Version pre = vh.getPredecessor(version);
        if (parentVersion == null)
        {
            assertNull(pre);
        }
        else
        {
            assertNotNull(pre);
            assertEquals(parentVersion.getVersionLabel(), pre.getVersionLabel());
        }
        
        if (parentVersion != null)
        {
            // Check that the successors of the parent are the created version
            Collection<Version> parentSuc = vh.getSuccessors(parentVersion);
            assertNotNull(parentSuc);
            assertEquals(1, parentSuc.size());
            Version tempVersion = (Version)parentSuc.toArray()[0];
            assertEquals(version.getVersionLabel(), tempVersion.getVersionLabel());
        }
        
        return createdVersion;
    }
    
    /**
     * Test revert
     */
    @SuppressWarnings("unused")
    public void testRevert()
    {
    	// Create a versionable node
    	NodeRef versionableNode = createNewVersionableNode();
    	
    	// Store the node details for later
    	Set<QName> origAspects = this.dbNodeService.getAspects(versionableNode);
    	
    	// Create the initial version
    	Version version1 = createVersion(versionableNode);
    	
    	// Change the property and content values
    	this.dbNodeService.setProperty(versionableNode, PROP_1, UPDATED_VALUE_1);
    	this.dbNodeService.setProperty(versionableNode, PROP_2, null);
    	ContentWriter contentWriter = this.contentService.getWriter(versionableNode, ContentModel.PROP_CONTENT, true);
    	assertNotNull(contentWriter);
    	contentWriter.putContent(UPDATED_CONTENT_1);
    	
    	// Change the aspects on the node
    	this.dbNodeService.addAspect(versionableNode, ApplicationModel.ASPECT_SIMPLE_WORKFLOW, null);
    	
    	// Store the node details for later
    	Set<QName> origAspects2 = this.dbNodeService.getAspects(versionableNode);
    	
    	// Create a new version
    	Version version2 = createVersion(versionableNode);
    	
    	// Change the property and content values
    	this.dbNodeService.setProperty(versionableNode, PROP_1, UPDATED_VALUE_2);
    	this.dbNodeService.setProperty(versionableNode, PROP_2, UPDATED_VALUE_3);
    	this.dbNodeService.setProperty(versionableNode, PROP_3, null);
    	ContentWriter contentWriter2 = this.contentService.getWriter(versionableNode, ContentModel.PROP_CONTENT, true);
    	assertNotNull(contentWriter2);
    	contentWriter2.putContent(UPDATED_CONTENT_2);
        
        String versionLabel = (String)this.dbNodeService.getProperty(versionableNode, ContentModel.PROP_VERSION_LABEL);
    	
    	// Revert to the previous version
    	this.versionService.revert(versionableNode);
    	
    	// Check that the version label is unchanged
        assertEquals(versionLabel, this.dbNodeService.getProperty(versionableNode, ContentModel.PROP_VERSION_LABEL));
    	
    	// Check that the properties have been reverted
    	assertEquals(UPDATED_VALUE_1, this.dbNodeService.getProperty(versionableNode, PROP_1));
    	assertNull(this.dbNodeService.getProperty(versionableNode, PROP_2));
    	assertEquals(VALUE_3, this.dbNodeService.getProperty(versionableNode, PROP_3));
    	
    	// Check that the content has been reverted
    	ContentReader contentReader1 = this.contentService.getReader(versionableNode, ContentModel.PROP_CONTENT);
    	assertNotNull(contentReader1);
    	assertEquals(UPDATED_CONTENT_1, contentReader1.getContentString());
    	
    	// Check that the aspects have been reverted correctly
    	Set<QName> aspects1 = this.dbNodeService.getAspects(versionableNode);
    	assertEquals(aspects1.size(), origAspects2.size());
    	
    	// Revert to the first version
    	this.versionService.revert(versionableNode, version1);
    	
    	// Check that the version label is correct
        assertEquals(versionLabel, this.dbNodeService.getProperty(versionableNode, ContentModel.PROP_VERSION_LABEL));
    	
    	// Check that the properties are correct
    	assertEquals(VALUE_1, this.dbNodeService.getProperty(versionableNode, PROP_1));
    	assertEquals(VALUE_2, this.dbNodeService.getProperty(versionableNode, PROP_2));
    	assertEquals(VALUE_3, this.dbNodeService.getProperty(versionableNode, PROP_3));
    	
    	// Check that the content is correct
    	ContentReader contentReader2 = this.contentService.getReader(versionableNode, ContentModel.PROP_CONTENT);
    	assertNotNull(contentReader2);
    	assertEquals(TEST_CONTENT, contentReader2.getContentString());
    	
    	// Check that the aspects have been reverted correctly
    	Set<QName> aspects2 = this.dbNodeService.getAspects(versionableNode);
    	assertEquals(aspects2.size(), origAspects.size());
    	
    	// Check that the version label is still the same
        assertEquals(versionLabel, this.dbNodeService.getProperty(versionableNode, ContentModel.PROP_VERSION_LABEL));        
    }
    
    /**
     * Test restore
     */
    public void testRestore()
    {
        // Try and restore a node without any version history
        try
        {
            this.versionService.restore(
                    new NodeRef(this.testStoreRef, "123"),
                    rootNodeRef, 
                    ContentModel.ASSOC_CHILDREN, 
                    QName.createQName("{test}MyVersionableNode"));
            fail("An exception should have been raised since this node has no version history.");
        }
        catch (VersionServiceException exception)
        {
            // We where expecting this exception
        }
        
        // Create a versionable node
        NodeRef versionableNode = createNewVersionableNode();
        
        // Store the node details for later
        Set<QName> origAspects = this.dbNodeService.getAspects(versionableNode);
        
        // Try and restore the node (fail since exist!!)
        try
        {
            this.versionService.restore(
                    versionableNode,
                    rootNodeRef, 
                    ContentModel.ASSOC_CHILDREN, 
                    QName.createQName("{test}MyVersionableNode"));
            fail("An exception should have been raised since this node exists and you can't restore a node that exists.");
        }
        catch (VersionServiceException exception)
        {
            // We where expecting this exception
        }
        
        // Version it
        this.versionService.createVersion(versionableNode, null);
        
        // Delete it
        this.dbNodeService.deleteNode(versionableNode);
        assertFalse(this.dbNodeService.exists(versionableNode));
        
        // Try and resotre it
        NodeRef restoredNode = this.versionService.restore(
                versionableNode, 
                this.rootNodeRef, 
                ContentModel.ASSOC_CHILDREN, 
                QName.createQName("{test}MyVersionableNode"));
        
        assertNotNull(restoredNode);
        assertTrue(this.dbNodeService.exists(restoredNode));
        
        // Check that the properties are correct
        assertEquals(VALUE_1, this.dbNodeService.getProperty(restoredNode, PROP_1));
        assertEquals(VALUE_2, this.dbNodeService.getProperty(restoredNode, PROP_2));
        assertEquals(VALUE_3, this.dbNodeService.getProperty(restoredNode, PROP_3));
        
        // Check that the content is correct
        ContentReader contentReader2 = this.contentService.getReader(restoredNode, ContentModel.PROP_CONTENT);
        assertNotNull(contentReader2);
        assertEquals(TEST_CONTENT, contentReader2.getContentString());
        
        // Check that the aspects have been reverted correctly
        Set<QName> aspects2 = this.dbNodeService.getAspects(restoredNode);
        assertEquals(aspects2.size(), origAspects.size());
    }
    
    /**
     * Test deleteVersionHistory
     */
    public void testDeleteVersionHistory()
    {
    	// Create a versionable node
    	NodeRef versionableNode = createNewVersionableNode();
    	
    	// Check that there is no version history
    	VersionHistory versionHistory1 = this.versionService.getVersionHistory(versionableNode);
    	assertNull(versionHistory1);
    	
    	// Create a couple of versions
    	createVersion(versionableNode);
    	Version version1 = createVersion(versionableNode);
    	
    	// Check that the version label is correct on the versionable node
    	String versionLabel1 = (String)this.dbNodeService.getProperty(versionableNode, ContentModel.PROP_VERSION_LABEL);
    	assertNotNull(versionLabel1);
    	assertEquals(version1.getVersionLabel(), versionLabel1);
    	
    	// Check that the version history has been created correctly
    	VersionHistory versionHistory2 = this.versionService.getVersionHistory(versionableNode);
    	assertNotNull(versionHistory2);
    	assertEquals(2, versionHistory2.getAllVersions().size());
    	
    	// Delete the version history
    	this.versionService.deleteVersionHistory(versionableNode);
    	
    	// Check that there is no version history available for the node
    	VersionHistory versionHistory3 = this.versionService.getVersionHistory(versionableNode);
    	assertNull(versionHistory3);
    	
    	// Check that the current version property on the versionable node is no longer set
    	String versionLabel2 = (String)this.dbNodeService.getProperty(versionableNode, ContentModel.PROP_VERSION_LABEL);
    	assertNull(versionLabel2);
    	
    	// Create a couple of versions
    	createVersion(versionableNode);
    	Version version2 = createVersion(versionableNode);
    	
    	// Check that the version history is correct
    	VersionHistory versionHistory4 = this.versionService.getVersionHistory(versionableNode);
    	assertNotNull(versionHistory4);
    	assertEquals(2, versionHistory4.getAllVersions().size());
    	
    	// Check that the version label is correct on the versionable node    
    	String versionLabel3 = (String)this.dbNodeService.getProperty(versionableNode, ContentModel.PROP_VERSION_LABEL);
    	assertNotNull(versionLabel3);
    	assertEquals(version2.getVersionLabel(), versionLabel3);
    	
    }
    
    /**
     * Test deleteVersion
     */
    public void testDeleteVersion()
    {
        // Create a versionable node
        NodeRef versionableNode = createNewVersionableNode();
        
        // Check that there is no version history
        VersionHistory versionHistory = this.versionService.getVersionHistory(versionableNode);
        CheckVersionHistory(versionHistory, null);
        
        // Check that the current version property on the versionable node is not set
        String versionLabel = (String)this.dbNodeService.getProperty(versionableNode, ContentModel.PROP_VERSION_LABEL);
        assertNull(versionLabel);
        
        // Check that there is no current version
        Version version = this.versionService.getCurrentVersion(versionableNode);
        assertNull(version);
        
        // Create a couple of versions
        Version version0 = createVersion(versionableNode);
        Version version1 = createVersion(versionableNode);
        
        // Check that the version label is correct on the versionable node
        String versionLabel1 = (String)this.dbNodeService.getProperty(versionableNode, ContentModel.PROP_VERSION_LABEL);
        assertEquals("1.1", versionLabel1);
        assertEquals(version1.getVersionLabel(), versionLabel1);
        
        // Check the version history
        List<Version> expectedVersions = new ArrayList<Version>(2);
        expectedVersions.add(version1);
        expectedVersions.add(version0);
        versionHistory = this.versionService.getVersionHistory(versionableNode);
        assertEquals(2, versionHistory.getAllVersions().size());
        CheckVersionHistory(versionHistory, expectedVersions);
        
        // Check current version
        Version currentVersion = this.versionService.getCurrentVersion(versionableNode);
        assertEquals(currentVersion.getVersionLabel(), version1.getVersionLabel());
        assertEquals(currentVersion.getFrozenStateNodeRef(), version1.getFrozenStateNodeRef());
        
        // Create a couple more versions
        Version version2 = createVersion(versionableNode);
        Version version3 = createVersion(versionableNode);
        
        // Check that the version label is correct on the versionable node
        String versionLabel3 = (String)this.dbNodeService.getProperty(versionableNode, ContentModel.PROP_VERSION_LABEL);
        assertEquals("1.3", versionLabel3);
        assertEquals(version3.getVersionLabel(), versionLabel3);
        
        // Check the version history
        expectedVersions = new ArrayList<Version>(4);
        expectedVersions.add(version3);
        expectedVersions.add(version2);
        expectedVersions.add(version1);
        expectedVersions.add(version0);
        versionHistory = this.versionService.getVersionHistory(versionableNode);
        assertEquals(4, versionHistory.getAllVersions().size());
        CheckVersionHistory(versionHistory, expectedVersions);
        
        // Check current version
        currentVersion = this.versionService.getCurrentVersion(versionableNode);
        assertEquals(currentVersion.getVersionLabel(), version3.getVersionLabel());
        assertEquals(currentVersion.getFrozenStateNodeRef(), version3.getFrozenStateNodeRef());

        // Delete version 2
        this.versionService.deleteVersion(versionableNode, version2);
        
        // Delete version 0
        this.versionService.deleteVersion(versionableNode, version0);
        
        // Check the version history
        expectedVersions = new ArrayList<Version>(2);
        expectedVersions.add(version3);
        expectedVersions.add(version1);
        versionHistory = this.versionService.getVersionHistory(versionableNode);
        assertEquals(2, versionHistory.getAllVersions().size());
        CheckVersionHistory(versionHistory, expectedVersions);
        
        // Check current version is unchanged
        currentVersion = this.versionService.getCurrentVersion(versionableNode);
        assertEquals(currentVersion.getVersionLabel(), version3.getVersionLabel());
        assertEquals(currentVersion.getFrozenStateNodeRef(), version3.getFrozenStateNodeRef());
        
        // Delete version 3
        this.versionService.deleteVersion(versionableNode, version3);
        
        // Check the version history size
        expectedVersions = new ArrayList<Version>(1);
        expectedVersions.add(version1);
        versionHistory = this.versionService.getVersionHistory(versionableNode);
        assertEquals(1, versionHistory.getAllVersions().size());
        CheckVersionHistory(versionHistory, expectedVersions);
        
        // Check current version has changed to version 1
        currentVersion = this.versionService.getCurrentVersion(versionableNode);
        assertEquals(currentVersion.getVersionLabel(), version1.getVersionLabel());
        assertEquals(currentVersion.getFrozenStateNodeRef(), version1.getFrozenStateNodeRef());
        
        // Create version 4
        Version version4 = createVersion(versionableNode);
        
        // Check the version history size
        expectedVersions = new ArrayList<Version>(2);
        expectedVersions.add(version4);
        expectedVersions.add(version1);
        versionHistory = this.versionService.getVersionHistory(versionableNode);
        assertEquals(2, versionHistory.getAllVersions().size());
        CheckVersionHistory(versionHistory, expectedVersions);
        
        // Check current version has changed to version 4
        currentVersion = this.versionService.getCurrentVersion(versionableNode);
        assertEquals(currentVersion.getVersionLabel(), version4.getVersionLabel());
        assertEquals(currentVersion.getFrozenStateNodeRef(), version4.getFrozenStateNodeRef());
        
        // Delete version 1
        this.versionService.deleteVersion(versionableNode, version1);
        
        // Delete version 4
        this.versionService.deleteVersion(versionableNode, version4);
        
        // Check the version history is empty
        versionHistory = this.versionService.getVersionHistory(versionableNode);
        CheckVersionHistory(versionHistory, null);
        
        // Check that the current version property on the versionable node is no longer set
        versionLabel = (String)this.dbNodeService.getProperty(versionableNode, ContentModel.PROP_VERSION_LABEL);
        assertNull(versionLabel);
        
        // Check that there is no current version
        version = this.versionService.getCurrentVersion(versionableNode);
        assertNull(version);
    }
    
    public void testAutoVersionOnInitialVersionOn()
    {
        // Create a versionable node
        final NodeRef versionableNode = createNewVersionableNode();
        
        setComplete();
        endTransaction();

        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                // Check that the initial version has not been created
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);
                assertNotNull(versionHistory);
                assertEquals(1, versionHistory.getAllVersions().size());                
                
                // Add some content 
                ContentWriter contentWriter = contentService.getWriter(versionableNode, ContentModel.PROP_CONTENT, true);
                assertNotNull(contentWriter);
                contentWriter.putContent(UPDATED_CONTENT_1);
                
                return null;
            }
        });
        
        // Now lets have a look and make sure we have the correct number of entries in the version history
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);
                assertNotNull(versionHistory);
                assertEquals(2, versionHistory.getAllVersions().size());
                
                return null;
            }
        
        });
    }
    
    public void testAutoVersionOff()
    {
        // Create a versionable node
        final NodeRef versionableNode = createNewVersionableNode();
        
        this.dbNodeService.setProperty(versionableNode, ContentModel.PROP_AUTO_VERSION, false);
        
        setComplete();
        endTransaction();
        
        // The initial version should have been created now
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                // Add some content 
                ContentWriter contentWriter = contentService.getWriter(versionableNode, ContentModel.PROP_CONTENT, true);
                assertNotNull(contentWriter);
                contentWriter.putContent(UPDATED_CONTENT_1);
                
                return null;
            }
        });
        
        // Now lets have a look and make sure we have the correct number of entries in the version history
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);
                assertNotNull(versionHistory);
                assertEquals(1, versionHistory.getAllVersions().size());
                
                return null;
            }
        
        });
    }
    
    public void testInitialVersionOff()
    {
        // Create node (this node has some content)
        HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(ContentModel.PROP_INITIAL_VERSION, false);
        HashMap<QName, Serializable> props2 = new HashMap<QName, Serializable>();
        props2.put(ContentModel.PROP_NAME, "test.txt");
        final NodeRef nodeRef = this.dbNodeService.createNode(
                rootNodeRef, 
                ContentModel.ASSOC_CHILDREN, 
                QName.createQName("{test}MyVersionableNode2"),
                TEST_TYPE_QNAME,
                props2).getChildRef();
        this.dbNodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, props);
        
        setComplete();
        endTransaction();

        // The initial version should NOT have been created
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(nodeRef);
                assertNull(versionHistory);
                
                return null;
            }
        });
       
    }
    
    public void testAddRemoveVersionableAspect()
    {
    	HashMap<QName, Serializable> props2 = new HashMap<QName, Serializable>();
        props2.put(ContentModel.PROP_NAME, "test.txt");
        final NodeRef nodeRef = this.dbNodeService.createNode(
                rootNodeRef, 
                ContentModel.ASSOC_CHILDREN, 
                QName.createQName("{test}MyVersionableNode2"),
                TEST_TYPE_QNAME,
                props2).getChildRef();
        this.dbNodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, null);
        
        setComplete();
        endTransaction();
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
            	// Check that the version history has been created
                VersionHistory versionHistory = versionService.getVersionHistory(nodeRef);
                assertNotNull(versionHistory);
                assertEquals(1, versionHistory.getAllVersions().size());
                
                // Remove the versionable aspect 
                dbNodeService.removeAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE);
                
                return null;
            }
        });
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
            	// Check that the version history has been removed
                VersionHistory versionHistory = versionService.getVersionHistory(nodeRef);
                assertNull(versionHistory);
                
                // Re-add the versionable aspect
                dbNodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, null);
                
                return null;
            }
        });
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
            	// Check that the version history has been created 
                VersionHistory versionHistory = versionService.getVersionHistory(nodeRef);
                assertNotNull(versionHistory);
                assertEquals(1, versionHistory.getAllVersions().size());                
                
                return null;
            }
        });
    }
    
    public void testAutoRemovalOfVersionHistory()
    {
    	StoreRef spacesStoreRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    	NodeRef root = this.dbNodeService.getRootNode(spacesStoreRef);
    	
    	HashMap<QName, Serializable> props2 = new HashMap<QName, Serializable>();
        props2.put(ContentModel.PROP_NAME, "test-" + GUID.generate() + ".txt");
        final NodeRef nodeRef = this.dbNodeService.createNode(
                root, 
                ContentModel.ASSOC_CHILDREN, 
                QName.createQName("{test}MyVersionableNode2"),
                ContentModel.TYPE_CONTENT,
                props2).getChildRef();
        this.dbNodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, null);
        
        setComplete();
        endTransaction();
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
            	VersionHistory versionHistory = versionService.getVersionHistory(nodeRef);
                assertNotNull(versionHistory);
                assertEquals(1, versionHistory.getAllVersions().size());
                
            	// Delete the node
                dbNodeService.deleteNode(nodeRef);
                
                return null;
            }
        });
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
            	// Get the archived noderef
            	NodeRef archivedNodeRef = nodeArchiveService.getArchivedNode(nodeRef);
            	
            	// The archived noderef should still have a link to the version history
            	VersionHistory versionHistory = versionService.getVersionHistory(archivedNodeRef);
                assertNotNull(versionHistory);
                assertEquals(1, versionHistory.getAllVersions().size()); 
                
                // Delete the node for good
                dbNodeService.deleteNode(archivedNodeRef);
                
                return null;
            }
        });
        
        txnHelper.doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
            	// Get the archived noderef
            	NodeRef archivedNodeRef = nodeArchiveService.getArchivedNode(nodeRef);
            	
            	// Check that the version histories have been deleted
            	VersionHistory versionHistory12 = versionService.getVersionHistory(nodeRef);
                assertNull(versionHistory12);
            	VersionHistory versionHistory23 = versionService.getVersionHistory(archivedNodeRef);
                assertNull(versionHistory23);
                
                return null;
            }
        });
    }
    
    public void testAutoVersionOnUpdatePropsOnly()
    {
        // test auto-version props on
        final NodeRef versionableNode = createNewVersionableNode();
        this.dbNodeService.setProperty(versionableNode, ContentModel.PROP_AUTO_VERSION_PROPS, true);
        
        setComplete();
        endTransaction();
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);
                assertNotNull(versionHistory);
                assertEquals(1, versionHistory.getAllVersions().size());
                
                nodeService.setProperty(versionableNode, ContentModel.PROP_AUTHOR, "ano author 1");
                
                return null;
            }
        });
        
        // Now lets have a look and make sure we have the correct number of entries in the version history
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);
                assertNotNull(versionHistory);
                assertEquals(2, versionHistory.getAllVersions().size());
                
                return null;
            }
        
        });
        
        // test auto-version props off
        
        startNewTransaction();
        
        final NodeRef versionableNode2 = createNewVersionableNode();
        this.dbNodeService.setProperty(versionableNode2, ContentModel.PROP_AUTO_VERSION_PROPS, false);
        
        setComplete();
        endTransaction();
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode2);
                assertNotNull(versionHistory);
                assertEquals(1, versionHistory.getAllVersions().size());
                
                nodeService.setProperty(versionableNode2, ContentModel.PROP_AUTHOR, "ano author 2");
                
                return null;
            }
        });
        
        // Now lets have a look and make sure we have the correct number of entries in the version history
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode2);
                assertNotNull(versionHistory);
                assertEquals(1, versionHistory.getAllVersions().size());
                
                return null;
            }
        
        });
    }
    
    public void testAutoVersionOnUpdatePropsOnlyWithExcludes()
    {
        // test auto-version props on - without any excludes
        final NodeRef versionableNode = createNewVersionableNode();
        this.dbNodeService.setProperty(versionableNode, ContentModel.PROP_AUTO_VERSION_PROPS, true);
        
        setComplete();
        endTransaction();
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);
                assertNotNull(versionHistory);
                assertEquals(1, versionHistory.getAllVersions().size());
                
                nodeService.setProperty(versionableNode, ContentModel.PROP_AUTHOR, "ano author 1");
                nodeService.setProperty(versionableNode, ContentModel.PROP_DESCRIPTION, "description 1");
                
                return null;
            }
        });
        
        // Now lets have a look and make sure we have the correct number of entries in the version history
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);
                assertNotNull(versionHistory);
                assertEquals(2, versionHistory.getAllVersions().size());
                
                return null;
            }
        
        });
        
        VersionableAspect versionableAspect = (VersionableAspect)applicationContext.getBean("versionableAspect");
        
        List<String> excludedOnUpdateProps = new ArrayList<String>(1);
        excludedOnUpdateProps.add(ContentModel.PROP_AUTHOR.toPrefixString());
        versionableAspect.setExcludedOnUpdateProps(excludedOnUpdateProps);
        
        // test auto-version props on - with an excluded prop change
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);
                assertNotNull(versionHistory);
                assertEquals(2, versionHistory.getAllVersions().size());
                
                nodeService.setProperty(versionableNode, ContentModel.PROP_AUTHOR, "ano author 2");
                nodeService.setProperty(versionableNode, ContentModel.PROP_DESCRIPTION, "description 2");
                
                return null;
            }
        });
        
        // Now lets have a look and make sure we have the correct number of entries in the version history
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);
                assertNotNull(versionHistory);
                assertEquals(2, versionHistory.getAllVersions().size());
                
                return null;
            }
        
        });
        
        // test auto-version props on - with a non-excluded prop change
        
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);
                assertNotNull(versionHistory);
                assertEquals(2, versionHistory.getAllVersions().size());
                
                nodeService.setProperty(versionableNode, ContentModel.PROP_DESCRIPTION, "description 3");
                
                return null;
            }
        });
        
        // Now lets have a look and make sure we have the correct number of entries in the version history
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                VersionHistory versionHistory = versionService.getVersionHistory(versionableNode);
                assertNotNull(versionHistory);
                assertEquals(3, versionHistory.getAllVersions().size());
                
                return null;
            }
        
        });
    }
    
    public void testAR807() 
    {
    	QName prop = QName.createQName("http://www.alfresco.org/test/versionstorebasetest/1.0", "intProp");
    	
        ChildAssociationRef childAssociation = 
        	nodeService.createNode(this.rootNodeRef, 
                    				 ContentModel.ASSOC_CHILDREN, 
                    				 QName.createQName("http://www.alfresco.org/test/versionstorebasetest/1.0", "integerTest"), 
                    				 TEST_TYPE_QNAME);
        NodeRef newNode = childAssociation.getChildRef();
        nodeService.setProperty(newNode, prop, 1);

        Object editionCode = nodeService.getProperty(newNode, prop);
        assertEquals(editionCode.getClass(), Integer.class);

        Map<String, Serializable> versionProps = new HashMap<String, Serializable>(1);
        versionProps.put(VersionModel.PROP_VERSION_TYPE, VersionType.MAJOR);
        Version version = versionService.createVersion(newNode, versionProps);

        NodeRef versionNodeRef = version.getFrozenStateNodeRef();
        assertNotNull(versionNodeRef);
        
        Object editionCodeArchive = nodeService.getProperty(versionNodeRef, prop);
        assertEquals(editionCodeArchive.getClass(), Integer.class);
    }    
    public static void main(String ... args)
    {
        try
        {
            doMain(args);
            System.exit(1);
        }
        catch (Throwable e)
        {
            logger.error(e);
            System.exit(1);
        }
    }
    private static void doMain(String ... args)
    {
        if (args.length != 1)
        {
            System.out.println("Usage: VersionServiceImplTest fileCount");
            System.exit(1);
        }
        int fileCount = Integer.parseInt(args[0]);
        
        ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
        final ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        final FileFolderService fileFolderService = serviceRegistry.getFileFolderService();
        final NodeService nodeService = serviceRegistry.getNodeService();
        final VersionService versionService = serviceRegistry.getVersionService();
        final AuthenticationComponent authenticationComponent = (AuthenticationComponent) ctx.getBean("authenticationComponent");
        
        authenticationComponent.setSystemUserAsCurrentUser();
        
        // TEMP - for migration testing - force V1 store (override repository property)  
        final Version2ServiceImpl version2ServiceImpl = (Version2ServiceImpl)ctx.getBean("versionService");
        version2ServiceImpl.setOnlyUseDeprecatedV1(true);
        
        System.out.println("Using: " + versionService.getVersionStoreReference());
        
        // Create a new store
        StoreRef storeRef = new StoreRef("test", "VersionServiceImplTest-main-"+System.currentTimeMillis());
        if (!nodeService.exists(storeRef))
        {
            nodeService.createStore(storeRef.getProtocol(), storeRef.getIdentifier());
        }
        NodeRef rootNodeRef = nodeService.getRootNode(storeRef);
        // Create a folder
        NodeRef folderNodeRef = nodeService.createNode(
                rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName("test", "versionMain"),
                ContentModel.TYPE_FOLDER).getChildRef();
        // Now load the folder with the prescribed number of documents
        int count = 0;
        long start = System.currentTimeMillis();
        long lastReport = start;
        for (int i = 0; i < fileCount; i++)
        {
            fileFolderService.create(folderNodeRef, "file-" + i, ContentModel.TYPE_CONTENT);
            count++;
            // Report every 10s
            long now = System.currentTimeMillis();
            if (now - lastReport > 10000L)
            {
                long delta = (now - start);
                double average = (double) delta / (double) count;
                System.out.println(
                        "File Creation: \n" +
                        "   Count:        " + count + " of " + fileCount + "\n" +
                        "   Average (ms): " + average);
                lastReport = now;
            }
        }
        // Get all the children again
        List<FileInfo> files = fileFolderService.listFiles(folderNodeRef);
        // Version each one
        count = 0;
        start = System.currentTimeMillis();
        lastReport = start;
        for (FileInfo fileInfo : files)
        {
            NodeRef nodeRef = fileInfo.getNodeRef();
            versionService.createVersion(nodeRef, null);
            count++;
            // Report every 10s
            long now = System.currentTimeMillis();
            if (now - lastReport > 10000L)
            {
                long delta = (now - start);
                double average = (double) delta / (double) count;
                System.out.println(
                        "Version: \n" +
                        "   Count:        " + count + " of " + fileCount + "\n" +
                        "   Average (ms): " + average);
                lastReport = now;
            }
        }
        
        System.out.println("Finished: " + fileCount);
    }
}
