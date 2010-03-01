/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
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
package org.alfresco.repo.transfer;

/**
 * A bucket for little odds and ends for the transfer service.   
 * 
 * If this becomes a big class then refactor it away.
 *  
 * @author Mark Rogers
 */
public class TransferCommons
{
    /**
     * The Mime Part Name of the manifest file
     */
    public final static String PART_NAME_MANIFEST = "manifest";
    
    /**
     * Mapping between contentUrl and part name.
     * 
     * @param URL
     * @return the part name
     */
    public final static String URLToPartName(String contentUrl)
    {
        return contentUrl.substring(contentUrl.lastIndexOf('/')+1);
    }
}
