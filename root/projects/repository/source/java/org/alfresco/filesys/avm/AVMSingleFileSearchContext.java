/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>. */

package org.alfresco.filesys.avm;

import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.SearchContext;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;

/**
 * AVM Filesystem Single File Search Context Class
 * 
 * <p>Contains the details of a non-wildcard file/folder search, where there is only one result to return.
 *
 * @author GKSpencer
 */
public class AVMSingleFileSearchContext extends SearchContext {

	// Details of the single file/folder
	
	private AVMNodeDescriptor m_fileDetails;

	// Flag to indicate file details have been returned
	
	private boolean m_endOfSearch;
	
	// Relative path to the file/folder
	
	private String m_relativePath;
	
	// Mark thel file/folder as read-only
	
	private boolean m_readOnly;
	
	/**
	 * Class constructor
	 * 
	 * @param fileDetails AVMNodeDescriptor
	 * @param relPath String
	 * @param readOnly boolean
	 */
	public AVMSingleFileSearchContext( AVMNodeDescriptor fileDetails, String relPath, boolean readOnly)
	{
		m_fileDetails  = fileDetails;
		m_relativePath = relPath;
		
		m_readOnly = readOnly;
	}
	
    /**
     * Determine if there are more files for the active search.
     * 
     * @return boolean
     */
    public boolean hasMoreFiles()
    {
    	return m_endOfSearch == false ? true : false;
    }

    /**
     * Return file information for the next file in the active search. Returns false if the search
     * is complete.
     * 
     * @param info FileInfo to return the file information.
     * @return true if the file information is valid, else false
     */
    public boolean nextFileInfo(FileInfo info)
    {
    	// Check if the file details have been returned
    	
    	if ( m_endOfSearch == true)
    		return false;

    	// Fill in the file information details
    	
    	info.setFileName( m_fileDetails.getName());
    	
    	if ( m_fileDetails.isFile())
    	{
    		info.setFileSize( m_fileDetails.getLength());
    		info.setAllocationSize((m_fileDetails.getLength() + 512L) & 0xFFFFFFFFFFFFFE00L);
    	}
    	else
    		info.setFileSize( 0L);

    	info.setAccessDateTime( m_fileDetails.getAccessDate());
    	info.setCreationDateTime( m_fileDetails.getCreateDate());
    	info.setModifyDateTime( m_fileDetails.getModDate());

    	// Build the file attributes
    	
    	int attr = 0;
    	
    	if ( m_fileDetails.isDirectory())
    		attr += FileAttribute.Directory;
    	
    	if ( m_fileDetails.getName().startsWith( ".") ||
    			m_fileDetails.getName().equalsIgnoreCase( "Desktop.ini") ||
    			m_fileDetails.getName().equalsIgnoreCase( "Thumbs.db"))
    		attr += FileAttribute.Hidden;

    	if ( m_readOnly == true)
    		attr += FileAttribute.ReadOnly;
    	
    	info.setFileAttributes( attr);
    	info.setFileId( m_relativePath.hashCode());
    	
    	// Set the end of search flag, indicate that the file informatin is valid
    	
    	m_endOfSearch = true;
    	return true;
    }

    /**
     * Return the file name of the next file in the active search. Returns null is the search is
     * complete.
     * 
     * @return String
     */
    public String nextFileName()
    {
    	// Check if the file details have been returned
    	
    	if ( m_endOfSearch == true)
    		return null;

    	// Return the file/folder name, set the end of search flag
    	
    	m_endOfSearch = true;
    	return m_fileDetails.getName();
    }

    /**
     * Return the total number of file entries for this search if known, else return -1
     * 
     * @return int
     */
    public int numberOfEntries()
    {
        return 1;
    }

    /**
     * Return the resume id for the current file/directory in the search.
     * 
     * @return int
     */
    public int getResumeId()
    {
    	return 1;
    }
    
    /**
     * Restart a search at the specified resume point.
     * 
     * @param resumeId Resume point id.
     * @return true if the search can be restarted, else false.
     */
    public boolean restartAt(int resumeId)
    {
    	// Validate the resume id and clear the end of search flag
    	
    	if ( resumeId == 1)
    		m_endOfSearch = false;
    	else
    		return false;
    	return true;
    }

    /**
     * Restart the current search at the specified file.
     * 
     * @param info File to restart the search at.
     * @return true if the search can be restarted, else false.
     */
    public boolean restartAt(FileInfo info)
    {
    	return true;
    }
}
