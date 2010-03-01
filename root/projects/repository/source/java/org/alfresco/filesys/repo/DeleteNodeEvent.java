/*
 * Copyright (C) 2006-2010 Alfresco Software Limited.
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
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.filesys.repo;

import org.alfresco.service.cmr.model.FileFolderServiceType;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Delete Node Event Class
 * 
 * @author gkspencer
 */
public class DeleteNodeEvent extends NodeEvent {

	// Deleted node path and confirmation
	
	private String m_path;
	
	private boolean m_deleteConfirm;
	
	/**
	 * Class constructor
	 * 
	 * @param fType FileFolderServiceTtype
	 * @param nodeRef NodeRef
	 * @param path String
	 */
	public DeleteNodeEvent( FileFolderServiceType fType, NodeRef nodeRef, String path) {
		super( fType, nodeRef);
		
		m_path = path;
	}
	
	/**
	 * Return the relative path of the target node
	 * 
	 * @return String
	 */
	public final String getPath() {
		return m_path;
	}
	
	/**
	 * Check if the delete confirm flag is set
	 * 
	 * @return boolean
	 */
	public final boolean hasDeleteConfirm() {
		return m_deleteConfirm;
	}

	/**
	 * Set/clear the delete confirm flag
	 * 
	 * @param delConfirm boolean
	 */
	public final void setDeleteConfirm( boolean delConfirm) {
		m_deleteConfirm = delConfirm;
	}
	
	/**
	 * Return the node event as a string
	 * 
	 * @return String
	 */
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		str.append("[Delete:fType=");
		str.append(getFileType());
		str.append(",nodeRef=");
		str.append(getNodeRef());
		str.append(",path=");
		str.append(getPath());
		str.append(",confirm=");
		str.append(hasDeleteConfirm());
		str.append("]");
		
		return str.toString();
	}
}
