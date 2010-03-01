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
package org.alfresco.repo.importer;

import java.io.InputStream;

import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;


/**
 * Content Handler that interacts with an Alfresco Importer
 * 
 * @author David Caruana
 */
public interface ImportContentHandler extends ContentHandler, ErrorHandler
{
    /**
     * Sets the Importer
     * 
     * @param importer
     */
    public void setImporter(Importer importer);

    /**
     * Call-back for importing content streams
     * 
     * @param content  content stream identifier
     * @return  the input stream
     */
    public InputStream importStream(String content);
}
