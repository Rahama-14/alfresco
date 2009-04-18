/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.repo.forms.script;

import java.util.Arrays;
import java.util.List;

import org.alfresco.repo.forms.Form;
import org.alfresco.repo.forms.FormData;
import org.alfresco.repo.forms.FormService;
import org.alfresco.repo.forms.Item;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Script object representing the form service.
 * 
 * @author Neil McErlean
 */
public class ScriptFormService extends BaseScopableProcessorExtension
{
    private static Log logger = LogFactory.getLog(ScriptFormService.class);
    
    /** The site service */
    private FormService formService;

    /**
     * Set the form service
     * 
     * @param formService
     *            the form service
     */
    public void setFormService(FormService formService)
    {
        this.formService = formService;
    }

    /**
     * Returns a form representation of the given item,
     * all known fields for the item are included.
     * 
     * @param itemKind The kind of item to retrieve a form for
     * @param itemId The identifier of the item to retrieve a form for
     * @return The form
     */
    public ScriptForm getForm(String itemKind, String itemId)
    {
        return getForm(itemKind, itemId, null);
    }
    
    /**
     * Returns a form representation of the given item consisting 
     * only of the given fields.
     * 
     * @param itemKind The kind of item to retrieve a form for
     * @param itemId The identifier of the item to retrieve a form for
     * @param fields String array of fields to include, null
     *               indicates all possible fields for the item 
     *               should be included
     * @return The form
     */
    public ScriptForm getForm(String itemKind, String itemId, String[] fields)
    {
        return getForm(itemKind, itemId, fields, null);
    }
    
    /**
     * Returns a form representation of the given item consisting 
     * only of the given fields.
     * 
     * @param itemKind The kind of item to retrieve a form for
     * @param itemId The identifier of the item to retrieve a form for
     * @param fields String array of fields to include, null
     *               indicates all possible fields for the item 
     *               should be included
     * @param forcedFields List of field names from 'fields' list
     *                     that should be forcibly included, it is
     *                     up to the form processor implementation
     *                     to determine how to enforce this
     * @return The form
     */
    public ScriptForm getForm(String itemKind, String itemId, 
                String[] fields, String[] forcedFields)
    {
        // create List<String> representations of field params if necessary
        List<String> fieldsList = null;
        List<String> forcedFieldsList = null;
        
        if (fields != null)
        {
            fieldsList = Arrays.asList(fields);
        }
        
        if (forcedFields != null)
        {
            forcedFieldsList = Arrays.asList(forcedFields);
        }
        
        Form result = formService.getForm(new Item(itemKind, itemId), fieldsList, forcedFieldsList);
        return result == null ? null : new ScriptForm(result);
    }
    
    /**
     * Persists the given data object for the item provided
     * 
     * @param itemKind The kind of item to retrieve a form for
     * @param itemId The identifier of the item to retrieve a form for
     * @param postData The post data, this can be a Map of name value
     *                 pairs, a webscript FormData object or a JSONObject
     */
    public void saveForm(String itemKind, String itemId, Object postData)
    {
        // A note on data conversion as passed in to this method:
        // Each of the 3 submission methods (multipart/formdata, JSON Post and
        // application/x-www-form-urlencoded) pass an instance of FormData into this
        // method.
        FormData dataForFormService = null;
        if (postData instanceof FormData)
        {
            dataForFormService = (FormData)postData;
            // A note on data conversion as passed out of this method:
            // The Repo will handle conversion of String-based data into the types
            // required by the model.
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("ScriptFormService.saveForm: postData not instanceof FormData.");
            }
            return;
        }
       
        formService.saveForm(new Item(itemKind, itemId), dataForFormService);
    }
}
