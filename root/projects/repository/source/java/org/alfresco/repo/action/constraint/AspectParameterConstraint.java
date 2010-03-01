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

package org.alfresco.repo.action.constraint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.QName;

/**
 * Type parameter constraint
 * 
 * @author Roy Wetherall
 */
public class AspectParameterConstraint extends BaseParameterConstraint
{
    /** Name constant */
    public static final String NAME = "ac-aspects";
    
    private DictionaryService dictionaryService;
    
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }
          
    /**
     * @see org.alfresco.service.cmr.action.ParameterConstraint#getAllowableValues()
     */
    protected Map<String, String> getAllowableValuesImpl()
    {   
        Collection<QName> aspects = dictionaryService.getAllAspects();
        Map<String, String> result = new HashMap<String, String>(aspects.size());
        for (QName aspect : aspects)
        {
            AspectDefinition aspectDef = dictionaryService.getAspect(aspect);
            if (aspectDef != null && aspectDef.getTitle() != null)
            {
                result.put(aspect.toPrefixString(), aspectDef.getTitle());
            }
        }        
        return result;
    }    
    
    
}
