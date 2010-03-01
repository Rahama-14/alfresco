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
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.remote;

import java.util.List;

import org.alfresco.service.cmr.avmsync.AVMDifference;
import org.alfresco.service.cmr.avmsync.AVMSyncService;
import org.alfresco.service.cmr.remote.AVMSyncServiceTransport;
import org.alfresco.util.NameMatcher;

/**
 * Client side wrapper around the RMI based AVMSyncServiceTransport.
 * @author britt
 */
public class AVMSyncServiceRemote implements AVMSyncService 
{
    /**
     * The instance of AVMSyncServiceTransport.
     */
    private AVMSyncServiceTransport fTransport;
    
    /**
     * The ticket holder.
     */
    private ClientTicketHolder fTicketHolder;
    
    /**
     * Default constructor.
     */
    public AVMSyncServiceRemote()
    {
    }

    /**
     * Set the transport for the service.
     */
    public void setAvmSyncServiceTransport(AVMSyncServiceTransport transport)
    {
        fTransport = transport;
    }
    
    /**
     * Setter.
     * @param ticketHolder To set.
     */
    public void setClientTicketHolder(ClientTicketHolder ticketHolder)
    {
        fTicketHolder = ticketHolder;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.avmsync.AVMSyncService#compare(int, java.lang.String, int, java.lang.String)
     */
    public List<AVMDifference> compare(int srcVersion, String srcPath,
            int dstVersion, String dstPath, NameMatcher excluder) 
    {
        return fTransport.compare(fTicketHolder.getTicket(), srcVersion, srcPath, dstVersion, dstPath, excluder);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.avmsync.AVMSyncService#flatten(java.lang.String, java.lang.String)
     */
    public void flatten(String layerPath, String underlyingPath) 
    {
        fTransport.flatten(fTicketHolder.getTicket(), layerPath, underlyingPath);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.avmsync.AVMSyncService#resetLayer(java.lang.String)
     */
    public void resetLayer(String layerPath) 
    {
        fTransport.resetLayer(fTicketHolder.getTicket(), layerPath);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.avmsync.AVMSyncService#update(java.util.List, boolean, boolean, boolean, boolean, java.lang.String, java.lang.String)
     */
    public void update(List<AVMDifference> diffList, 
                       NameMatcher excluder, boolean ignoreConflicts,
                       boolean ignoreOlder, boolean overrideConflicts,
                       boolean overrideOlder, String tag, String description) 
    {
        fTransport.update(fTicketHolder.getTicket(), diffList, excluder, ignoreConflicts, ignoreOlder, overrideConflicts, overrideOlder, tag, description);
    }
}
