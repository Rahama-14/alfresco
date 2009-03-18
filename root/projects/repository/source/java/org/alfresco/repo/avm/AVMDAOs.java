/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * http://www.alfresco.com/legal/licensing" */

package org.alfresco.repo.avm;

import org.alfresco.repo.attributes.AttributeDAO;
import org.alfresco.repo.attributes.GlobalAttributeEntryDAO;
import org.alfresco.repo.attributes.ListEntryDAO;
import org.alfresco.repo.attributes.MapEntryDAO;
import org.alfresco.repo.domain.QNameDAO;

/**
 * This is the (shudder) global context for AVM.  It a rendezvous
 * point for access to needed global instances.
 * @author britt
 */
public class AVMDAOs
{
    /**
     * The single instance of an AVMContext.
     */
    private static final AVMDAOs fgInstance = new AVMDAOs();
    
    private AVMDAOs()
    {
    }

    /**
     * Get the instance of this.
     * @return
     */
    public static AVMDAOs Instance()
    {
        return fgInstance;
    }
    
    /**
     * The AVMNodeDAO.
     */
    public AVMNodeDAO fAVMNodeDAO;
    
    /**
     * The QName DAO
     */
    public QNameDAO fQNameDAO;
    
    /**
     *  The AVMStore DAO.
     */
    public AVMStoreDAO fAVMStoreDAO;
    
    /**
     * The VersionRootDAO.
     */
    public VersionRootDAO fVersionRootDAO;
    
    /**
     * The ChildEntryDAO.
     */
    public ChildEntryDAO fChildEntryDAO;
    
    /**
     * The HistoryLinkDAO.
     */
    public HistoryLinkDAO fHistoryLinkDAO;
    
    /**
     * The MergeLinkDAO.
     */
    public MergeLinkDAO fMergeLinkDAO;
    
    /**
     * The AVMStorePropertyDAO
     */
    public AVMStorePropertyDAO fAVMStorePropertyDAO;
    
    public AttributeDAO fAttributeDAO;
    
    public MapEntryDAO fMapEntryDAO;
    
    public GlobalAttributeEntryDAO fGlobalAttributeEntryDAO;
    
    public ListEntryDAO fListEntryDAO;
    
    public VersionLayeredNodeEntryDAO fVersionLayeredNodeEntryDAO;
    
    /**
     * @param nodeDAO the fAVMNodeDAO to set
     */
    public void setNodeDAO(AVMNodeDAO nodeDAO)
    {
        fAVMNodeDAO = nodeDAO;
    }

    public void setQnameDAO(QNameDAO qnameDAO)
    {
        this.fQNameDAO = qnameDAO;
    }

    /**
     * @param childEntryDAO the fChildEntryDAO to set
     */
    public void setChildEntryDAO(ChildEntryDAO childEntryDAO)
    {
        fChildEntryDAO = childEntryDAO;
    }

    /**
     * @param historyLinkDAO the fHistoryLinkDAO to set
     */
    public void setHistoryLinkDAO(HistoryLinkDAO historyLinkDAO)
    {
        fHistoryLinkDAO = historyLinkDAO;
    }

    /**
     * @param mergeLinkDAO the fMergeLinkDAO to set
     */
    public void setMergeLinkDAO(MergeLinkDAO mergeLinkDAO)
    {
        fMergeLinkDAO = mergeLinkDAO;
    }

    /**
     * @param aVMStoreDAO The fAVMStoreDAO to set
     */
    public void setAvmStoreDAO(AVMStoreDAO aVMStoreDAO)
    {
        fAVMStoreDAO = aVMStoreDAO;
    }

    /**
     * @param versionRootDAO the fVersionRootDAO to set
     */
    public void setVersionRootDAO(VersionRootDAO versionRootDAO)
    {
        fVersionRootDAO = versionRootDAO;
    }
    
    public void setAvmStorePropertyDAO(AVMStorePropertyDAO avmStorePropertyDAO)
    {
        fAVMStorePropertyDAO = avmStorePropertyDAO;
    }
    
    public void setAttributeDAO(AttributeDAO dao)
    {
        fAttributeDAO = dao;
    }
    
    public void setMapEntryDAO(MapEntryDAO dao)
    {
        fMapEntryDAO = dao;
    }
    
    public void setGlobalAttributeEntryDAO(GlobalAttributeEntryDAO dao)
    {
        fGlobalAttributeEntryDAO = dao;
    }
    
    public void setListEntryDAO(ListEntryDAO dao)
    {
        fListEntryDAO = dao;
    }
    
    public void setVersionLayeredNodeEntryDAO(VersionLayeredNodeEntryDAO dao)
    {
        fVersionLayeredNodeEntryDAO = dao;
    }
}
