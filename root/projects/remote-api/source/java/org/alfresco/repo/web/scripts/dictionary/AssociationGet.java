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
package org.alfresco.repo.web.scripts.dictionary;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/*
 * Webscript to get the Associationdefinition for a given classname and association-name
 * @author Saravanan Sellathurai
 */

public class AssociationGet extends DictionaryWebServiceBase
{
    private static final String MODEL_PROP_KEY_ASSOCIATION_DETAILS = "assocdefs";
	private static final String DICTIONARY_CLASS_NAME = "classname";
	private static final String DICTIONARY_ASSOCIATION_NAME = "assocname";
    
    /**
     * @Override  method from DeclarativeWebScript 
     */
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        String className = req.getServiceMatch().getTemplateVars().get(DICTIONARY_CLASS_NAME);
        String associationName = req.getServiceMatch().getTemplateVars().get(DICTIONARY_ASSOCIATION_NAME);
        Map<String, Object> model = new HashMap<String, Object>(1);
        QName classQname = null;
        QName associationQname = null;
        
        //validate the classname
        if(isValidClassname(className) == false)
        {
        	throw new WebScriptException(Status.STATUS_NOT_FOUND, "Check the classname - " + className + " - parameter in the URL");
        }
       
        classQname = QName.createQName(getFullNamespaceURI(className));
        
        if(associationName == null)
        {
        	throw new WebScriptException(Status.STATUS_NOT_FOUND, "Missing parameter association name in the URL");
        }
        
        associationQname = QName.createQName(getFullNamespaceURI(associationName));
        
		if(this.dictionaryservice.getClass(classQname).getAssociations().get(associationQname) != null)
		{
			model.put(MODEL_PROP_KEY_ASSOCIATION_DETAILS, this.dictionaryservice.getClass(classQname).getAssociations().get(associationQname));
		}
		
		return model;
    }
    
}