package org.alfresco.repo.version.common;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.alfresco.repo.ref.NodeRef;
import org.alfresco.repo.ref.StoreRef;
import org.alfresco.repo.version.Version;
import org.alfresco.repo.version.VersionDoesNotExistException;
import org.alfresco.repo.version.VersionServiceException;

import junit.framework.TestCase;

/**
 * VersionHistoryImpl Unit Test Class
 * 
 * @author Roy Wetherall
 */
public class VersionHistoryImplTest extends TestCase
{
    /**
     * Data used in the tests
     */
    private Version rootVersion = null;    
    private Version childVersion1 = null;
    private Version childVersion2 = null;
    
    /**
     * Set up
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        // Create dummy node ref
        NodeRef nodeRef = new NodeRef(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "test"), "test");
        
        HashMap<String, Serializable> versionProperties1 = new HashMap<String, Serializable>();
        versionProperties1.put(Version.PROP_VERSION_LABEL, "1");
        versionProperties1.put(Version.PROP_CREATED_DATE, new Date());
        versionProperties1.put("testProperty", "testValue");
        this.rootVersion = new VersionImpl(versionProperties1, nodeRef);
        
        HashMap<String, Serializable> versionProperties2 = new HashMap<String, Serializable>();
        versionProperties2.put(Version.PROP_VERSION_LABEL, "2");
        versionProperties2.put(Version.PROP_CREATED_DATE, new Date());
        versionProperties2.put("testProperty", "testValue");
        this.childVersion1 = new VersionImpl(versionProperties2, nodeRef);
        
        HashMap<String, Serializable> versionProperties3 = new HashMap<String, Serializable>();
        versionProperties3.put(Version.PROP_VERSION_LABEL, "3");
        versionProperties3.put(Version.PROP_CREATED_DATE, new Date());
        versionProperties3.put("testProperty", "testValue");
        this.childVersion2 = new VersionImpl(versionProperties3, nodeRef);                
    }

    /**
     * Test constructor
     */
    public void testConstructor()
    {
        testContructorImpl();
    }
    
    /**
     * Test construtor helper
     * 
     * @return new version history
     */
    private VersionHistoryImpl testContructorImpl()
    {
        VersionHistoryImpl vh = new VersionHistoryImpl(this.rootVersion);
        assertNotNull(vh);
        
        return vh;
    }
    
    /**
     * Exception case - a root version must be specified when creating a 
     *                  version history object
     */
    public void testRootVersionSpecified()
    {
        try
        {
            VersionHistoryImpl vh = new VersionHistoryImpl(null);
            fail();
        }
        catch(VersionServiceException exception)
        {
        }
    }

    /**
     * Test getRootVersion
     *
     *@return root version
     */
    public void testGetRootVersion()
    {
        VersionHistoryImpl vh = testContructorImpl();
        
        Version rootVersion = vh.getRootVersion();
        assertNotNull(rootVersion);
        assertEquals(rootVersion, this.rootVersion);        
    }
    
    /**
     * Test getAllVersions
     */
    public void testGetAllVersions()
    {
        VersionHistoryImpl vh = testAddVersionImpl();
        
        Collection<Version> allVersions = vh.getAllVersions();
        assertNotNull(allVersions);
        assertEquals(3, allVersions.size());
    }
    
    /**
     * Test addVersion
     * 
     * @return version history
     */
    public void testAddVersion()
    {
        testAddVersionImpl();
    }
    
    /**
     * Test addVersion helper
     * 
     * @return version history with version tree built
     */
    private VersionHistoryImpl testAddVersionImpl()
    {
        VersionHistoryImpl vh = testContructorImpl();
        Version rootVersion = vh.getRootVersion();
        
        vh.addVersion(this.childVersion1, rootVersion);
        vh.addVersion(this.childVersion2, rootVersion);
        
        return vh;
    }
    
    /**
     * TODO Exception case - add version that has already been added
     */
    
    /**
     * TODO Exception case - add a version with a duplicate version label
     */
    
    /**
     * Test getPredecessor
     */
    public void testGetPredecessor()
    {
        VersionHistoryImpl vh = testAddVersionImpl();
        
        Version version1 = vh.getPredecessor(this.childVersion1);
        assertEquals(version1.getVersionLabel(), this.rootVersion.getVersionLabel());
        
        Version version2 = vh.getPredecessor(this.childVersion2);
        assertEquals(version2.getVersionLabel(), this.rootVersion.getVersionLabel());
        
        Version version3 = vh.getPredecessor(this.rootVersion);
        assertNull(version3);
        
        try
        {
            Version version4 = vh.getPredecessor(null);
            assertNull(version4);
        }
        catch (Exception exception)
        {
            fail("Should continue by returning null.");
        }
    }
    
    /**
     * Test getSuccessors
     */
    public void testGetSuccessors()
    {
        VersionHistoryImpl vh = testAddVersionImpl();
        
        Collection<Version> versions1 = vh.getSuccessors(this.rootVersion);
        assertNotNull(versions1);
        assertEquals(versions1.size(), 2);
        
        boolean isFirst = true;
        for (Version version : versions1)
        {
            String versionLabel = version.getVersionLabel();
            if (!(versionLabel == "2" || versionLabel == "3"))
            {
                fail("There is a version in this collection that should not be here.");
            }
        }
        
        Collection versions2 = vh.getSuccessors(this.childVersion1);
        assertNotNull(versions2);
        assertTrue(versions2.isEmpty());
        
        Collection versions3 = vh.getSuccessors(this.childVersion2);
        assertNotNull(versions3);
        assertTrue(versions3.isEmpty());
    }
    
    /**
     * Test getVersion
     */
    public void testGetVersion()
    {
        VersionHistoryImpl vh = testAddVersionImpl();
       
        Version version1 = vh.getVersion("1");
        assertEquals(version1.getVersionLabel(), this.rootVersion.getVersionLabel());
        
        Version version2 = vh.getVersion("2");
        assertEquals(version2.getVersionLabel(), this.childVersion1.getVersionLabel());
        
        Version version3 = vh.getVersion("3");
        assertEquals(version3.getVersionLabel(), this.childVersion2.getVersionLabel());
        
        try
        {
            Version version = vh.getVersion("invalidLabel");
            fail("An exception should have been thrown if the version can not be retrieved.");
        }
        catch (VersionDoesNotExistException exception)
        {
            System.out.println("Error message: " + exception.getMessage());
        }
    }    
}
