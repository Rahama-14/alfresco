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
package org.alfresco.repo.webdav;

import javax.servlet.http.HttpServletResponse;

/**
 * Abstract base class for the hierarchical methods COPY and MOVE
 * 
 * @author gavinc
 */
public abstract class HierarchicalMethod extends WebDAVMethod
{
    // Request parameters
    
    protected String m_strDestinationPath;
    protected boolean m_overwrite = false;

    /**
     * Default constructor
     */
    public HierarchicalMethod()
    {
    }

    /**
     * Return the destination path
     * 
     * @return String
     */
    public final String getDestinationPath()
    {
        return m_strDestinationPath;
    }
    
    /**
     * Return the overwrite setting
     * 
     * @return boolean
     */
    public final boolean hasOverWrite()
    {
        return m_overwrite;
    }
    
    /**
     * Parse the request headers
     * 
     * @exception WebDAVServerException
     */
    protected void parseRequestHeaders() throws WebDAVServerException
    {
        // Get the destination path for the copy

        String strDestination = m_request.getHeader(WebDAV.HEADER_DESTINATION);

        if (logger.isDebugEnabled())
            logger.debug("Parsing Destination header: " + strDestination);

        if (strDestination != null && strDestination.length() > 0)
        {
            int offset = -1;
            
            if ( strDestination.startsWith("http://"))
                offset = 7;
            else if ( strDestination.startsWith("https://"))
                offset = 8;
            
            if (offset != -1)
            {
                offset = strDestination.indexOf(WebDAV.PathSeperator, offset);
                if ( offset != -1)
                {
                    String strPath = strDestination.substring(offset);
                    String servletPath = m_request.getServletPath();
                    
                    offset = strPath.indexOf(servletPath);
                    if ( offset != -1)
                        strPath = strPath.substring(offset + servletPath.length());
                    
                    m_strDestinationPath = WebDAV.decodeURL(strPath);
                }
            }
        }

        // Failed to fix the destination path, return an error

        if (m_strDestinationPath == null)
        {
            logger.warn("Failed to parse the Destination header: " + strDestination);
            throw new WebDAVServerException(HttpServletResponse.SC_BAD_REQUEST);
        }

        // Check if the copy should overwrite an existing file

        String strOverwrite = m_request.getHeader(WebDAV.HEADER_OVERWRITE);
        if (strOverwrite != null && strOverwrite.equals(WebDAV.T))
        {
            m_overwrite = true;
        }
    }

    /**
     * Parse the request body
     * 
     * @exception WebDAVServerException
     */
    protected void parseRequestBody() throws WebDAVServerException
    {
        // NOTE: Hierarchical methods do have a body to define what should happen
        // to the properties when they are moved or copied, however, this
        // feature is not implemented by many servers, including ours!!
    }
}
