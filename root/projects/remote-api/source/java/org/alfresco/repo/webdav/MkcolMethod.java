/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.repo.webdav;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Implements the WebDAV MKCOL method
 * 
 * @author gavinc
 */
public class MkcolMethod extends WebDAVMethod
{
    /**
     * Default constructor
     */
    public MkcolMethod()
    {
    }

    /**
     * Parse the request headers
     * 
     * @Exception WebDAVServerException
     */
    protected void parseRequestHeaders() throws WebDAVServerException
    {
        // Nothing to do in this method
    }

    /**
     * Parse the request body
     * 
     * @exception WebDAVServerException
     */
    protected void parseRequestBody() throws WebDAVServerException
    {
        // There should not be a body with the MKCOL request

        if (m_request.getContentLength() > 0)
        {
            throw new WebDAVServerException(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    /**
     * Execute the request
     * 
     * @exception WebDAVServerException
     */
    protected void executeImpl() throws WebDAVServerException, Exception
    {
        FileFolderService fileFolderService = getFileFolderService();

        // see if it exists
        try
        {
            getDAVHelper().getNodeForPath(getRootNodeRef(), getPath(), getServletPath());
            // already exists
            throw new WebDAVServerException(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        catch (FileNotFoundException e)
        {
            // it doesn't exist
        }
        
        // Trim the last path component and check if the parent path exists
        String parentPath = getPath();
        int lastPos = parentPath.lastIndexOf(WebDAVHelper.PathSeperator);
        
        NodeRef parentNodeRef = null;

        if ( lastPos == 0)
        {
            // Create new folder at root

            parentPath = WebDAVHelper.PathSeperator;
            parentNodeRef = getRootNodeRef();
        }
        else if (lastPos != -1)
        {
            // Trim the last path component
            parentPath = parentPath.substring(0, lastPos + 1);
            try
            {
                FileInfo parentFileInfo = getDAVHelper().getNodeForPath(getRootNodeRef(), parentPath, m_request.getServletPath());
                parentNodeRef = parentFileInfo.getNodeRef();
            }
            catch (FileNotFoundException e)
            {
                // parent path is missing
                throw new WebDAVServerException(HttpServletResponse.SC_CONFLICT);
            }
        }
        else
        {
            // Looks like a bad path
            throw new WebDAVServerException(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }

        // Get the new folder name
        String folderName = getPath().substring(lastPos + 1);

        // Create the new folder node
        fileFolderService.create(parentNodeRef, folderName, ContentModel.TYPE_FOLDER);

        // Return a success status
        m_response.setStatus(HttpServletResponse.SC_CREATED);
    }
}
