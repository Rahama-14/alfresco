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
package org.alfresco.repo.version;

import java.util.Collection;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.QName;

/**
 * versionService test class.
 * 
 * @author Roy Wetherall
 */
public class VersionServiceImplTest extends BaseVersionStoreTest
{
	private static final String UPDATED_VALUE_1 = "updatedValue1";
	private static final String UPDATED_VALUE_2 = "updatedValue2";
	private static final String UPDATED_VALUE_3 = "updatedValue3";
	private static final String UPDATED_CONTENT_1 = "updatedContent1";
	private static final String UPDATED_CONTENT_2 = "updatedContent2";

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
    }
    
    // TODO test versioning a non versionable node ie: no version apsect
    
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
        long beforeVersionTime = System.currentTimeMillis();
        
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
        long beforeVersionTime = System.currentTimeMillis();
        
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
            String frozenNodeId = (String)version.getVersionProperty(VersionModel.PROP_FROZEN_NODE_ID);
            assertNotNull("Unable to retrieve the frozen node id from the created version.", frozenNodeId);
            
            // Get the origional node ref (based on the forzen node)
            NodeRef origionaNodeRef = this.versionableNodes.get(frozenNodeId);
            assertNotNull("The versionable node ref that relates to the frozen node id can not be found.", origionaNodeRef);
            
            // Check the new version
            checkNewVersion(beforeVersionTime, expectedVersionNumber, expectedVersionLabel, version, origionaNodeRef);
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
    	ContentWriter contentWriter = this.contentService.getUpdatingWriter(versionableNode);
    	assertNotNull(contentWriter);
    	contentWriter.putContent(UPDATED_CONTENT_1);
    	
    	// Change the aspects on the node
    	this.dbNodeService.addAspect(versionableNode, ContentModel.ASPECT_SIMPLE_WORKFLOW, null);
    	
    	// Store the node details for later
    	Set<QName> origAspects2 = this.dbNodeService.getAspects(versionableNode);
    	
    	// Create a new version
    	Version version2 = createVersion(versionableNode);
    	
    	// Change the property and content values
    	this.dbNodeService.setProperty(versionableNode, PROP_1, UPDATED_VALUE_2);
    	this.dbNodeService.setProperty(versionableNode, PROP_2, UPDATED_VALUE_3);
    	this.dbNodeService.setProperty(versionableNode, PROP_3, null);
    	ContentWriter contentWriter2 = this.contentService.getUpdatingWriter(versionableNode);
    	assertNotNull(contentWriter2);
    	contentWriter2.putContent(UPDATED_CONTENT_2);
    	
    	// Revert to the previous version
    	this.versionService.revert(versionableNode);
    	
    	// TODO check the version label is correct
    	
    	// Check that the properties have been reverted
    	assertEquals(UPDATED_VALUE_1, this.dbNodeService.getProperty(versionableNode, PROP_1));
    	assertNull(this.dbNodeService.getProperty(versionableNode, PROP_2));
    	assertEquals(VALUE_3, this.dbNodeService.getProperty(versionableNode, PROP_3));
    	
    	// Check that the content has been reverted
    	ContentReader contentReader1 = this.contentService.getReader(versionableNode);
    	assertNotNull(contentReader1);
    	assertEquals(UPDATED_CONTENT_1, contentReader1.getContentString());
    	
    	// Check that the aspects have been reverted correctly
    	Set<QName> aspects1 = this.dbNodeService.getAspects(versionableNode);
    	assertEquals(aspects1.size(), origAspects2.size());
    	
    	// Revert to the first version
    	this.versionService.revert(versionableNode, version1);
    	
    	// TODO check that the version label is correct
    	
    	// Check that the properties are correct
    	assertEquals(VALUE_1, this.dbNodeService.getProperty(versionableNode, PROP_1));
    	assertEquals(VALUE_2, this.dbNodeService.getProperty(versionableNode, PROP_2));
    	assertEquals(VALUE_3, this.dbNodeService.getProperty(versionableNode, PROP_3));
    	
    	// Check that the content is correct
    	ContentReader contentReader2 = this.contentService.getReader(versionableNode);
    	assertNotNull(contentReader2);
    	assertEquals(TEST_CONTENT, contentReader2.getContentString());
    	
    	// Check that the aspects have been reverted correctly
    	Set<QName> aspects2 = this.dbNodeService.getAspects(versionableNode);
    	assertEquals(aspects2.size(), origAspects.size());
    	
    	// TODO version the reverted node and ensure that the next version is created correctly
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
}
