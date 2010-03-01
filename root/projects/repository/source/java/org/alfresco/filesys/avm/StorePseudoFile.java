/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>. */

package org.alfresco.filesys.avm;

import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.FileName;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.server.filesys.pseudo.PseudoFile;
import org.alfresco.jlan.server.filesys.pseudo.PseudoFolderNetworkFile;
import org.alfresco.service.cmr.avm.AVMStoreDescriptor;

/**
 * Store Pseudo File Class
 * 
 * <p>Represents an AVM store as a folder.
 *
 * @author gkspencer
 */
public class StorePseudoFile extends PseudoFile {

	// Store type
	
	private int m_storeType = StoreType.Normal;
	
	// Web project this sandbox links to, or null if this store is not linked
	
	private String m_webProject;
	
	// User name if this is an author sandbox for a web project
	
	private String m_userName;
	
	/**
	 * Class constructor
	 * 
	 * @param storeDesc AVMStoreDescriptor
	 * @param relPath String
	 * @param storeType int
	 */
	public StorePseudoFile( AVMStoreDescriptor storeDesc, String relPath, int storeType)
	{
		super( storeDesc.getName(), FileAttribute.Directory + FileAttribute.ReadOnly);
		
		// Create static file information from the store details
		
		FileInfo fInfo = new FileInfo( storeDesc.getName(), 0L, FileAttribute.Directory + FileAttribute.ReadOnly);

		fInfo.setCreationDateTime( storeDesc.getCreateDate());
		fInfo.setModifyDateTime( storeDesc.getCreateDate());
		fInfo.setAccessDateTime( storeDesc.getCreateDate());
		fInfo.setChangeDateTime( storeDesc.getCreateDate());
		
		fInfo.setPath( relPath);
		fInfo.setFileId( relPath.hashCode());
		
		setFileInfo( fInfo);
		
		setStoreType( storeType);
	}
	
	/**
	 * Class constructor
	 * 
	 * @param storeName String
	 * @param relPath String
	 */
	public StorePseudoFile( String storeName, String relPath)
	{
		super( storeName, FileAttribute.Directory + FileAttribute.ReadOnly);
		
		// Create static file information from the store details
		
		FileInfo fInfo = new FileInfo( storeName, 0L, FileAttribute.Directory + FileAttribute.ReadOnly);

		long timeNow = System.currentTimeMillis();
		fInfo.setCreationDateTime( timeNow);
		fInfo.setModifyDateTime( timeNow);
		fInfo.setAccessDateTime( timeNow);
		fInfo.setChangeDateTime( timeNow);
		
		fInfo.setPath( relPath);
		fInfo.setFileId( relPath.hashCode());
		
		setFileInfo( fInfo);
	}
	
    /**
     * Return a network file for reading/writing the pseudo file
     * 
     * @param netPath String
     * @return NetworkFile
     */
	@Override
	public NetworkFile getFile(String netPath) {
		
		// Split the path to get the name
		
		String[] paths = FileName.splitPath( netPath);
		
		// Create a network file for the folder
		
		return new PseudoFolderNetworkFile( paths[1], netPath);
	}

    /**
     * Return the file information for the pseudo file
     *
     * @return FileInfo
     */
	@Override
	public FileInfo getFileInfo() {
		return getInfo();
	}

	/**
	 * Return the store type
	 * 
	 * @return int
	 */
	public final int isStoreType()
	{
		return m_storeType;
	}
	
	/**
	 * Check if this store is linked to a web project
	 * 
	 * @return boolean
	 */
	public final boolean hasWebProject()
	{
		return m_webProject != null ? true : false;
	}
	
	/**
	 * Get the web project that this store links to, or null if not linked
	 * 
	 * @return String
	 */
	public final String getWebProject()
	{
		return m_webProject;
	}
	
	/**
	 * Set the web project that this store is linked to
	 * 
	 * @param webProject String
	 */
	public final void setWebProject(String webProject)
	{
		m_webProject = webProject;
	}
	
	/**
	 * Check if this store is an author sandbox
	 * 
	 * @return boolean
	 */
	public final boolean hasUserName()
	{
		return m_userName != null ? true : false;
	}
	
	/**
	 * Get the owner of this sandbox
	 * 
	 * @return String
	 */
	public final String getUserName()
	{
		return m_userName;
	}
	
	/**
	 * Set the owner of this sandbox
	 * 
	 * @param userName String
	 */
	public final void setUserName(String userName)
	{
		m_userName = userName;
	}
	
	/**
	 * Set the store type
	 * 
	 * @param storeType int
	 */
	public final void setStoreType(int storeType)
	{
		m_storeType = storeType;
	}
}
