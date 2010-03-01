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
package org.alfresco.repo.importer.view;

import org.alfresco.repo.importer.Importer;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.QName;


/**
 * Maintains state about the currently imported element.
 * 
 * @author David Caruana
 *
 */
public class ElementContext
{
    // Dictionary Service
    private DictionaryService dictionary;
    
    // Element Name
    private QName elementName;
    
    // Importer
    private Importer importer;
    
    
    /**
     * Construct
     * 
     * @param dictionary
     * @param elementName
     * @param progress
     */
    public ElementContext(QName elementName, DictionaryService dictionary, Importer importer)
    {
        this.elementName = elementName;
        this.dictionary = dictionary;
        this.importer = importer;
    }
    
    /**
     * @return  the element name
     */
    public QName getElementName()
    {
        return elementName;
    }
    
    /**
     * @return  the dictionary service
     */
    public DictionaryService getDictionaryService()
    {
        return dictionary;
    }
    
    /**
     * @return  the importer
     */
    public Importer getImporter()
    {
        return importer;
    }
}
