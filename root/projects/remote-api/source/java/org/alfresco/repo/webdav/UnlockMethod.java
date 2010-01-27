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

import java.util.LinkedList;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.WebDAVModel;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeService;

/**
 * Implements the WebDAV UNLOCK method
 * 
 * @author gavinc
 */
public class UnlockMethod extends WebDAVMethod
{
    private String m_strLockToken = null;

    /**
     * Default constructor
     */
    public UnlockMethod()
    {
    }

    /**
     * Return the lock token of an existing lock
     * 
     * @return String
     */
    protected final String getLockToken()
    {
        return m_strLockToken;
    }

    /**
     * Parse the request headers
     * 
     * @exception WebDAVServerException
     */
    protected void parseRequestHeaders() throws WebDAVServerException
    {
        // Get the lock token, if any
        String strLockTokenHeader = m_request.getHeader(WebDAV.HEADER_LOCK_TOKEN);

        // DEBUG
        if (logger.isDebugEnabled())
            logger.debug("Parsing Lock-Token header: " + strLockTokenHeader);

        // Validate the lock token
        if (strLockTokenHeader != null && strLockTokenHeader.startsWith("<") && strLockTokenHeader.endsWith(">"))
        {
            try
            {
                m_strLockToken = strLockTokenHeader.substring(
                        WebDAV.OPAQUE_LOCK_TOKEN.length() + 1,
                        strLockTokenHeader.length() - 1);
            }
            catch (IndexOutOfBoundsException e)
            {
                logger.warn("Failed to parse If header: " + strLockTokenHeader);
            }
        }

        // If there is no token this is a bad request so send an error back
        if (m_strLockToken == null)
        {
            throw new WebDAVServerException(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Parse the request body
     * 
     * @exception WebDAVServerException
     */
    protected void parseRequestBody() throws WebDAVServerException
    {
        // Nothing to do in this method
    }

    /**
     * Execute the request
     * 
     * @exception WebDAVServerException
     */
    protected void executeImpl() throws WebDAVServerException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Lock node; path=" + getPath() + ", token=" + getLockToken());
        }

        FileInfo lockNodeInfo = null;
        try
        {
            lockNodeInfo = getDAVHelper().getNodeForPath(getRootNodeRef(), getPath(), getServletPath());
        }
        catch (FileNotFoundException e)
        {
            throw new WebDAVServerException(HttpServletResponse.SC_NOT_FOUND);
        }

        // Parse the lock token
        String[] lockInfo = WebDAV.parseLockToken(getLockToken());
        if (lockInfo == null)
        {
            // Bad lock token
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        // Get the lock status for the node
        LockService lockService = getDAVHelper().getLockService();
        NodeService nodeService = getNodeService();
        // String nodeId = lockInfo[0];
        // String userName = lockInfo[1];

        LockStatus lockSts = lockService.getLockStatus(lockNodeInfo.getNodeRef());
        if (lockSts == LockStatus.LOCK_OWNER)
        {
            // Unlock the node
            lockService.unlock(lockNodeInfo.getNodeRef());
            nodeService.removeProperty(lockNodeInfo.getNodeRef(), WebDAVModel.PROP_OPAQUE_LOCK_TOKEN);
            nodeService.removeProperty(lockNodeInfo.getNodeRef(), WebDAVModel.PROP_LOCK_DEPTH);
            nodeService.removeProperty(lockNodeInfo.getNodeRef(), WebDAVModel.PROP_LOCK_SCOPE);

            // Indicate that the unlock was successful
            m_response.setStatus(HttpServletResponse.SC_NO_CONTENT);

            // DEBUG
            if (logger.isDebugEnabled())
            {
                logger.debug("Unlock token=" + getLockToken() + " Successful");
            }
        }
        else if (lockSts == LockStatus.NO_LOCK)
        {
            String sharedLocks = (String) nodeService.getProperty(lockNodeInfo.getNodeRef(), WebDAVModel.PROP_SHARED_LOCK_TOKENS);
            if (sharedLocks != null)
            {
                LinkedList<String> locks = LockInfo.parseSharedLockTokens(sharedLocks);
                
                if (locks != null && locks.contains(m_strLockToken))
                {
                    locks.remove(m_strLockToken);
                    nodeService.setProperty(lockNodeInfo.getNodeRef(), WebDAVModel.PROP_SHARED_LOCK_TOKENS, LockInfo.makeSharedLockTokensString(locks));

                    // Indicate that the unlock was successful
                    m_response.setStatus(HttpServletResponse.SC_NO_CONTENT);

                    // DEBUG
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Unlock token=" + getLockToken() + " Successful");
                    }
                }
            }
            else
            {
            // DEBUG
            if (logger.isDebugEnabled())
                logger.debug("Unlock token=" + getLockToken() + " Not locked");

            // Node is not locked
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }
            
            
        }
        else if (lockSts == LockStatus.LOCKED)
        {
            // DEBUG
            if (logger.isDebugEnabled())
                logger.debug("Unlock token=" + getLockToken() + " Not lock owner");

            // Node is locked but not by this user
            throw new WebDAVServerException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }
        else if (lockSts == LockStatus.LOCK_EXPIRED)
        {
            // DEBUG
            if (logger.isDebugEnabled())
                logger.debug("Unlock token=" + getLockToken() + " Lock expired");

            // Return a success status
            m_response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }
}
