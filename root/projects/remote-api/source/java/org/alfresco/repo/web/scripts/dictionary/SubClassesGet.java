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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Webscript to get the Sub-Classdefinitions using classfilter , namespacePrefix and name
 * 
 * @author Saravanan Sellathurai
 */

public class SubClassesGet extends DictionaryWebServiceBase
{
    private static final String MODEL_PROP_KEY_CLASS_DEFS = "classdefs";
	private static final String MODEL_PROP_KEY_PROPERTY_DETAILS = "propertydefs";
	private static final String MODEL_PROP_KEY_ASSOCIATION_DETAILS = "assocdefs";
	
    private static final String REQ_URL_TEMPL_IMMEDIATE_SUB_TYPE_CHILDREN = "r";
    //private static final String REQ_URL_TEMPL_VAR_CLASS_FILTER = "cf";
    private static final String REQ_URL_TEMPL_VAR_NAMESPACE_PREFIX = "nsp";
    private static final String REQ_URL_TEMPL_VAR_NAME = "n";
    private static final String DICTIONARY_CLASS_NAME = "classname";
    	
    /**
     * @Override  method from DeclarativeWebScript 
     */
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
    	String name = req.getParameter(REQ_URL_TEMPL_VAR_NAME);
    	String namespacePrefix = req.getParameter(REQ_URL_TEMPL_VAR_NAMESPACE_PREFIX);
        String className = req.getServiceMatch().getTemplateVars().get(DICTIONARY_CLASS_NAME);
        String recursiveValue = getValidInput(req.getParameter(REQ_URL_TEMPL_IMMEDIATE_SUB_TYPE_CHILDREN));
        
        boolean recursive = true;
        
        Map<QName, ClassDefinition> classdef = new HashMap<QName, ClassDefinition>();
        Map<QName, Collection<PropertyDefinition>> propdef = new HashMap<QName, Collection<PropertyDefinition>>();
        Map<QName, Collection<AssociationDefinition>> assocdef = new HashMap<QName, Collection<AssociationDefinition>>();
        Map<String, Object> model = new HashMap<String, Object>();
        
        QName classQName = null;
        String namespaceUri = null;
        Collection <QName> qname = null;
        boolean isAspect = false;
        boolean ignoreCheck = false;
        
        // validate recursive parameter => can be either true or false or null
        if(recursiveValue == null)
        {
        	recursive = true;
        }
        else if(recursiveValue.equalsIgnoreCase("true"))
        {
        	recursive = true;
        }
        else if (recursiveValue.equalsIgnoreCase("false"))
        {
        	recursive = false;
        }
        else
        {
        	throw new WebScriptException(Status.STATUS_NOT_FOUND, "Check the value for the parameter recursive=> " + recursiveValue +"  can only be either true or false");
        }
        	
        //validate the className
        if(isValidClassname(className) == true)
        {
        	classQName = QName.createQName(getFullNamespaceURI(className));
        	if(isValidTypeorAspect(className) == true) 
        	{
        		isAspect = true;
        	}
        }
        else
        {
        	throw new WebScriptException(Status.STATUS_NOT_FOUND, "Check the className - " + className + " parameter in the URL");
        }
        
        // collect the subaspects or subtypes of the class
        if(isAspect == true) 
		{
    		qname = this.dictionaryservice.getSubAspects(classQName, recursive);
    	}
    	else
		{
			qname = this.dictionaryservice.getSubTypes(classQName, recursive);
		}
        
        //validate the name parameter
        if(name != null)
        {
        	if(isValidModelName(name) == false)
        	{
        		throw new WebScriptException(Status.STATUS_NOT_FOUND, "Check the name parameter - " + name + " in the URL");
        	}
        }
        
        //validate the name parameter
        if (namespacePrefix == null && name != null)
        {
        	namespaceUri = namespaceService.getNamespaceURI(getPrefixFromModelName(name));
        }
        
        if (namespacePrefix != null && name == null)
        {
        	namespaceUri = namespaceService.getNamespaceURI(namespacePrefix);
        }
        
        if(namespacePrefix == null && name == null)
        {
        	namespaceUri = null;
        	ignoreCheck = true;
        }
        
        if (namespacePrefix != null && name != null)
        {
        	if(isValidClassname(namespacePrefix + "_" + name) == false)
        	{
        		throw new WebScriptException(Status.STATUS_NOT_FOUND, "Check the namespacePrefix - " + namespacePrefix + " and name - "+ name  + " - parameter in the URL");
        	}
        	namespaceUri = namespaceService.getNamespaceURI(namespacePrefix);
        }
        
        for(QName qnameObj: qname)
	    {	
			if((ignoreCheck == true)  || 
			   (qnameObj.getNamespaceURI().equals(namespaceUri)))
			{
	    		classdef.put(qnameObj, this.dictionaryservice.getClass(qnameObj));
	    		propdef.put(qnameObj, this.dictionaryservice.getClass(qnameObj).getProperties().values());
	    		assocdef.put(qnameObj, this.dictionaryservice.getClass(qnameObj).getAssociations().values());
			}
	    }
    	
        List<ClassDefinition> classDefinitions = new ArrayList<ClassDefinition>(classdef.values());
        Collections.sort(classDefinitions, new DictionaryComparators.ClassDefinitionComparator());
    	model.put(MODEL_PROP_KEY_CLASS_DEFS, classDefinitions);
	    model.put(MODEL_PROP_KEY_PROPERTY_DETAILS, propdef.values());
	    model.put(MODEL_PROP_KEY_ASSOCIATION_DETAILS, assocdef.values());
	    return model;        	
		
	 }
   
 }