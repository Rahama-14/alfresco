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
package org.alfresco.cmis;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Service to retrieving renditions of document
 * 
 * @author Stas Sokolovsky
 */
public interface CMISRenditionService
{
    /**
     * Get renditions for a document.
     * 
     * @param node node reference of document
     * @param renditionFilter renditions filter
     * @return list of renditions
     * @throws CMISFilterNotValidException 
     */
    public List<CMISRendition> getRenditions(NodeRef node, String renditionFilter) throws CMISFilterNotValidException;

}
