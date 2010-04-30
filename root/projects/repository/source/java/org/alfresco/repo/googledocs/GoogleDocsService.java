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
* along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
*/
package org.alfresco.repo.googledocs;

import java.io.InputStream;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Google docs integration service interface
 */
public interface GoogleDocsService 
{
	/**
	 * Initialises the googles doc service, checking the provided credentials are correct.  This need
	 * not be called manually since other service calls will initialise the service on demand, but it can 
	 * be helpful to know the "health" of the service up front.
	 */
	void initialise() throws GoogleDocsServiceInitException;
	
    /**
     * Create a google doc from a given node.  The content of the node will be used 
     * as a basis of the associated google doc.  If the node has no content a new, empty google
     * doc of the correct type will be created.
     * 
     * The permission context provides information about how google sharing permissions should be 
     * set on the created google doc.
     * 
     * @param nodeRef               node reference
     * @param permissionContext     permission context
     */
    void createGoogleDoc(NodeRef nodeRef, GoogleDocsPermissionContext permissionContext);
    
    /**
     * Deletes the google resource associated with the node reference.  This could be a folder or 
     * document.
     *  
     * @param nodeRef   node reference
     */
    void deleteGoogleResource(NodeRef nodeRef);

    /**
     * Gets the content as an input stream of google doc associated with the given node.  The 
     * node must have the google resource aspect and the associated resource should not be a 
     * folder.
     * 
     * @param nodeRef        node reference
     * @return InputStream   the content of the associated google doc
     */
    InputStream getGoogleDocContent(NodeRef nodeRef);

}