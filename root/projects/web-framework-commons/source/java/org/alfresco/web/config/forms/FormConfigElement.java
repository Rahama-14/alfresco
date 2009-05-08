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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.config.forms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigException;
import org.alfresco.config.element.ConfigElementAdapter;
import org.alfresco.connector.Connector;
import org.alfresco.connector.ConnectorService;
import org.alfresco.connector.Response;
import org.alfresco.connector.exception.ConnectorServiceException;
import org.alfresco.web.site.FrameworkHelper;
import org.alfresco.web.site.RequestContext;
import org.alfresco.web.site.ThreadLocalRequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This config element represents form values for the client. Note the distinction
 * between 'form' and 'forms' elements in the config xml. Form elements are contained
 * by forms elements.
 * 
 * @author Neil McErlean.
 */
public class FormConfigElement extends ConfigElementAdapter
{
    private static final long serialVersionUID = -7008510360503886308L;
    private static Log logger = LogFactory.getLog(FormConfigElement.class);
    
    public static final String FORM_NAME_ID = "form";
    public static final String DEFAULT_SET_ID = "";
    
    private String formId;
    private String submissionURL;
    
    private String createTemplate;
    private String editTemplate;
    private String viewTemplate;
    
    FieldVisibilityManager fieldVisibilityManager = new FieldVisibilityManager();
    private final Map<String, FormSet> sets = new LinkedHashMap<String, FormSet>(4);
    private Map<String, FormField> fields = new LinkedHashMap<String, FormField>(8);
    private List<String> forcedFields = new ArrayList<String>(4);
    
    public FormConfigElement()
    {
        this(FORM_NAME_ID);
    }

    public FormConfigElement(String name)
    {
        super(name);
        // There should always be a 'default set' for those fields which do not declare
        // explicit membership of any set.
        FormSet defaultSet = new FormSet(DEFAULT_SET_ID);
        this.sets.put(DEFAULT_SET_ID, defaultSet);
    }

    /**
     * @see org.alfresco.config.ConfigElement#getChildren()
     */
    @Override
    public List<ConfigElement> getChildren()
    {
        throw new ConfigException(
                "Reading the form config via the generic interfaces is not supported");
    }

    /**
     * @see org.alfresco.config.ConfigElement#combine(org.alfresco.config.ConfigElement)
     */
    @Override
    public ConfigElement combine(ConfigElement otherConfigElement)
    {
        if (otherConfigElement == null)
        {
            return this;
        }
        
        FormConfigElement otherFormElem = (FormConfigElement)otherConfigElement;
        FormConfigElement result = new FormConfigElement();
        
        combineSubmissionURL(otherFormElem, result);
        
        combineTemplates(otherFormElem, result);
        
        combineFieldVisibilities(otherFormElem, result);
        
        combineSets(otherFormElem, result);
        
        combineFields(otherFormElem, result);
        
        return result;
    }

    private void combineFields(FormConfigElement otherFormElem,
            FormConfigElement result)
    {
        Map<String, FormField> newFields = new LinkedHashMap<String, FormField>();
        for (String nextFieldId : this.fields.keySet())
        {
            FormField nextFieldFromThis = this.fields.get(nextFieldId);
            if (otherFormElem.getFields().containsKey(nextFieldId))
            {
                FormField combinedField = nextFieldFromThis
                    .combine(otherFormElem.getFields().get(nextFieldId));
                newFields.put(nextFieldId, combinedField);
            }
            else
            {
                newFields.put(nextFieldId, nextFieldFromThis);
            }
        }

        for (String nextFieldId : otherFormElem.fields.keySet())
        {
            if (!this.fields.containsKey(nextFieldId))
            {
                newFields.put(nextFieldId, otherFormElem.fields.get(nextFieldId));
            }
            else
            {
                // handled by above loop.
            }
        }
        result.setFields(newFields);
        
        // Combine the lists of 'forced' fields.
        result.forcedFields.addAll(this.forcedFields);
        for (String fieldName : otherFormElem.forcedFields)
        {
            if (result.forcedFields.contains(fieldName) == false)
            {
                result.forcedFields.add(fieldName);
            }
        }
    }

    private void combineSets(FormConfigElement otherFormElem,
            FormConfigElement result)
    {
        for (String nextOldSet : sets.keySet())
        {
            FormSet nextOldSetData = sets.get(nextOldSet);
            String setId = nextOldSetData.getSetId();
            String parentId = nextOldSetData.getParentId();
            String appearance = nextOldSetData.getAppearance();
            String label = nextOldSetData.getLabel();
            String labelId = nextOldSetData.getLabelId();
            result.addSet(setId, parentId, appearance, label, labelId);
        }
        for (String nextNewSet : otherFormElem.sets.keySet())
        {
            FormSet nextNewSetData = otherFormElem.sets.get(nextNewSet);
            String setId = nextNewSetData.getSetId();
            String parentId = nextNewSetData.getParentId();
            String appearance = nextNewSetData.getAppearance();
            String label = nextNewSetData.getLabel();
            String labelId = nextNewSetData.getLabelId();
            result.addSet(setId, parentId, appearance, label, labelId);
        }
    }

    private void combineFieldVisibilities(FormConfigElement otherFormElem,
            FormConfigElement result)
    {
        FieldVisibilityManager combinedManager
                = this.fieldVisibilityManager.combine(otherFormElem.fieldVisibilityManager);
        result.fieldVisibilityManager = combinedManager;
    }

    private void combineTemplates(FormConfigElement otherFormElem,
            FormConfigElement result)
    {
        result.setFormTemplate("create-form", createTemplate);
        result.setFormTemplate("create-form", otherFormElem.createTemplate);
        
        result.setFormTemplate("edit-form", editTemplate);
        result.setFormTemplate("edit-form", otherFormElem.editTemplate);
        
        result.setFormTemplate("view-form", viewTemplate);
        result.setFormTemplate("view-form", otherFormElem.viewTemplate);
    }

    private void combineSubmissionURL(FormConfigElement otherFormElem,
            FormConfigElement result)
    {
        String otherSubmissionURL = otherFormElem.getSubmissionURL();
        result.setSubmissionURL(otherSubmissionURL == null ? this.submissionURL : otherSubmissionURL);
    }
    
    public String getId()
    {
    	return this.formId;
    }

    public String getSubmissionURL()
    {
        return this.submissionURL;
    }
    
    public Map<String, FormSet> getSets()
    {
        return Collections.unmodifiableMap(this.sets);
    }

    public String[] getSetIDs()
    {
        return this.getSetIDsAsList().toArray(new String[0]);
    }
    
    public List<String> getSetIDsAsList()
    {
        Set<String> keySet = sets.keySet();
        
        List<String> result = new ArrayList<String>(keySet.size());
        result.addAll(keySet);
        
        return Collections.unmodifiableList(result);
    }

    public FormSet[] getRootSets()
    {
        return this.getRootSetsAsList().toArray(new FormSet[0]);
    }

    /**
     * This method returns a Map of those &lt;set&gt;s which have no declared parentID
     * i&#46;e&#46; those that are 'roots' in the tree of sets. Note that this map will
     * always have at least one member; the default set.
     * 
     * @return
     * @see #DEFAULT_SET_ID
     */
    public List<FormSet> getRootSetsAsList()
    {
        List<FormSet> result = new ArrayList<FormSet>(sets.size());
        for (Iterator<String> iter = sets.keySet().iterator(); iter.hasNext(); )
        {
            String nextKey = iter.next();
            FormSet nextSet = sets.get(nextKey);
            String nextParentID = nextSet.getParentId();
            if (nextParentID == null)
            {
                result.add(nextSet);
            }
        }
        return result;
    }
    
    //TODO In the case where we have <field-visibility> but no <appearance> i.e. no <fields>
    //     this method should include FormField objects for each 'show'n field.
    //     These objects will have no associated metadata.
    public Map<String, FormField> getFields()
    {
        return Collections.unmodifiableMap(this.fields);
    }
    
    public String[] getHiddenCreateFieldNames()
    {
        return this.getHiddenCreateFieldNamesAsList().toArray(new String[0]);
    }

    public String[] getHiddenEditFieldNames()
    {
        return this.getHiddenEditFieldNamesAsList().toArray(new String[0]);
    }
    
    public String[] getHiddenViewFieldNames()
    {
        return this.getHiddenViewFieldNamesAsList().toArray(new String[0]);
    }
    
    public String[] getVisibleCreateFieldNames()
    {
        return this.getVisibleCreateFieldNamesAsList().toArray(new String[0]);
    }

    public String[] getVisibleEditFieldNames()
    {
        return this.getVisibleEditFieldNamesAsList().toArray(new String[0]);
    }
    
    public String[] getVisibleViewFieldNames()
    {
        return this.getVisibleViewFieldNamesAsList().toArray(new String[0]);
    }
    
    /**
     * @see FieldVisibilityManager#getFieldNamesHiddenInMode(Mode)
     */
    public List<String> getHiddenCreateFieldNamesAsList()
    {
        return getFieldNamesHiddenInMode(Mode.CREATE);
    }
    
    /**
     * @see FieldVisibilityManager#getFieldNamesHiddenInMode(Mode)
     */
    public List<String> getHiddenEditFieldNamesAsList()
    {
        return getFieldNamesHiddenInMode(Mode.EDIT);
    }
    
    /**
     * @see FieldVisibilityManager#getFieldNamesHiddenInMode(Mode)
     */
    public List<String> getHiddenViewFieldNamesAsList()
    {
        return getFieldNamesHiddenInMode(Mode.VIEW);
    }

    /**
     * @see FieldVisibilityManager#getFieldNamesVisibleInMode(Mode)
     */
    public List<String> getVisibleCreateFieldNamesAsList()
    {
        return getFieldNamesVisibleInMode(Mode.CREATE);
    }
    
    /**
     * @see FieldVisibilityManager#getFieldNamesVisibleInMode(Mode)
     */
    public List<String> getVisibleEditFieldNamesAsList()
    {
        return getFieldNamesVisibleInMode(Mode.EDIT);
    }
    
    /**
     * @see FieldVisibilityManager#getFieldNamesVisibleInMode(Mode)
     */
    public List<String> getVisibleViewFieldNamesAsList()
    {
        return getFieldNamesVisibleInMode(Mode.VIEW);
    }

    /**
     * This method returns an array of fieldNames for those fields which are
     * visible in Create mode and which are also members of the specified set.
     * 
     * @param setId
     * @return the array of fieldNames (if there are any).
     *         <code>null</code> will be returned if the specified setId is not
     *         recognised or if the visible fields are not known.
     */
    public String[] getVisibleCreateFieldNamesForSet(String setId)
    {
        List <String> result = getVisibleFieldNamesFor(setId, Mode.CREATE);
        if (result == null)
        {
            return null;
        }
        return result.toArray(new String[0]);
    }

    public String[] getVisibleEditFieldNamesForSet(String setId)
    {
        List <String> result = getVisibleFieldNamesFor(setId, Mode.EDIT);
        if (result == null)
        {
            return null;
        }
        return result.toArray(new String[0]);
    }

    public String[] getVisibleViewFieldNamesForSet(String setId)
    {
        List <String> result = getVisibleFieldNamesFor(setId, Mode.VIEW);
        if (result == null)
        {
            return null;
        }
        return result.toArray(new String[0]);
    }

    private List<String> getVisibleFieldNamesFor(String setId, Mode mode)
    {
        List <String> result = new ArrayList<String>();

        // Does the requested setId exist?
        FormSet specifiedSet = getSets().get(setId);
        if (specifiedSet == null)
        {
            return null;
        }

        // Get all fields visible in the specified mode - irrespective of set membership.
        final List<String> visibleFields = getFieldNamesVisibleInMode(mode);
        if (visibleFields == null)
        {
            return null;
        }
        
        // Now go through the fields and filter out those that are not members of
        // the specified set
        for (String fieldName : visibleFields)
        {
            final FormField formField = getFields().get(fieldName);
            String set = null;
            if (formField != null)
            {
                set = formField.getSet();
            }
            
            if (set == null)
            {
                // All fields without an explicit set are in the default set.
                set = FormConfigElement.DEFAULT_SET_ID;
            }
            
            // if set of field matches requested one add to results
            if (set.equals(setId))
            {
                result.add(fieldName);
            }
        }
        return result;
    }

    public String getCreateTemplate()
    {
        return this.createTemplate;
    }
    
    public String getEditTemplate()
    {
        return this.editTemplate;
    }
    
    public String getViewTemplate()
    {
        return this.viewTemplate;
    }
    
    void setFormId(String formId)
    {
    	this.formId = formId;
    }
    
    /**
     * 
     * @param m
     * @return <code>null</code> if there is no template available for the specified mode.
     */
    public String getFormTemplate(Mode m)
    {
        switch (m)
        {
        case CREATE: return getCreateTemplate();
        case EDIT: return getEditTemplate();
        case VIEW: return getViewTemplate();
        default: return null;
        }
    }
    
    /**
     * This method checks whether the specified field is visible in the specified mode.
     * 
     * @param fieldId the id of the field
     * @param m a mode.
     * @return
     */
    public boolean isFieldVisible(String fieldId, Mode m)
    {
        return fieldVisibilityManager.isFieldVisible(fieldId, m);
    }

    /**
     * This method checks whether the specified field is visible in the specified mode.
     * This is added as a convenience for JavaScript clients.
     * 
     * @param fieldId
     * @param modeString
     * @return
     * @see #isFieldVisible(String, Mode)
     */
    public boolean isFieldVisibleInMode(String fieldId, String modeString)
    {
        // Note. We cannot have the same method name as isFieldVisible(String, Mode)
        // as this is intended for use by JavaScript clients where method overloading
        // is not supported.
        Mode m = Mode.modeFromString(modeString);
        return this.isFieldVisible(fieldId, m);
    }
    
    /**
     * Determines whether the given fieldId has been configured as 'force'd
     * 
     * @param fieldId The field id to check
     * @return true if the field is being forced to be visible
     */
    public boolean isFieldForced(String fieldId)
    {
        return this.forcedFields.contains(fieldId);
    }
    
    public String[] getForcedFields()
    {
        return this.getForcedFieldsAsList().toArray(new String[0]);
    }
    
    /**
     * Returns the list of fields that have been forced to be visible
     * 
     * @return List of field ids
     */
    public List<String> getForcedFieldsAsList()
    {
        return this.forcedFields;
    }

    private List<String> getFieldNamesHiddenInMode(Mode mode)
    {
        List<String> result = fieldVisibilityManager.getFieldNamesHiddenInMode(mode);
        return result;
    }

    private List<String> getFieldNamesVisibleInMode(Mode mode)
    {
        List<String> result = fieldVisibilityManager.getFieldNamesVisibleInMode(mode);
        return result;
    }

    /* package */void setSubmissionURL(String newURL)
    {
        this.submissionURL = newURL;
    }
    
    /* package */void setFormTemplate(String nodeName, String newTemplate)
    {
        if (nodeName.equals("create-form"))
        {
            createTemplate = newTemplate;
        }
        else if (nodeName.equals("edit-form"))
        {
            editTemplate = newTemplate;
        }
        else if (nodeName.equals("view-form"))
        {
            viewTemplate = newTemplate;
        }
        else
        {
            if (logger.isWarnEnabled())
            {
                logger.warn("Unrecognised mode: " + nodeName);
            }
            return;
        }
    }
    
    /* package */void addFieldVisibility(String showOrHide, String fieldId,
            String mode, String forceString)
    {
        fieldVisibilityManager.addInstruction(showOrHide, fieldId, mode);
        
        boolean isForced = new Boolean(forceString);
        if (isForced && (this.forcedFields.contains(fieldId) == false))
        {
            this.forcedFields.add(fieldId);
        }
    }
    
    /* package */void addSet(String setId, String parentSetId, String appearance,
                             String label, String labelId)
    {
        FormSet newFormSetObject = new FormSet(setId, parentSetId, appearance, 
                    label, labelId);
        
        // We disallow the declaration of sets whose parents do not already exist.
        // The reason for this is to ensure that cycles within the parent structure
        // are not possible.
        if (parentSetId != null &&
                !sets.containsKey(parentSetId))
        {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Set [").append(setId).append("] has undefined parent [")
            .append(parentSetId).append("].");
            throw new ConfigException(errorMsg.toString());
        }
        
        // We disallow the creation of sets whose id is that of the default set and
        // who have parents. In other words, the default set must be a root set.
        if (setId.equals(DEFAULT_SET_ID) && parentSetId != null)
        {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Default set cannot have any parent set. Parent specified was: [")
                .append(parentSetId).append("].");
            throw new ConfigException(errorMsg.toString());
        }
        
        sets.put(setId, newFormSetObject);
        
        // Set parent/child references
        if (parentSetId != null)
        {
            FormSet parentObject = sets.get(parentSetId);
            
            newFormSetObject.setParent(parentObject);
            parentObject.addChild(newFormSetObject);
        }
    }
    
    /* package */void addField(String fieldId, List<String> attributeNames,
            List<String> attributeValues)
    {
        if (attributeNames == null)
        {
            attributeNames = Collections.emptyList();
        }
        if (attributeValues == null)
        {
            attributeValues = Collections.emptyList();
        }
        if (attributeNames.size() < attributeValues.size()
                && logger.isWarnEnabled())
        {
            StringBuilder msg = new StringBuilder();
            msg.append("field ")
            .append(fieldId)
            .append(" has ")
            .append(attributeNames.size())
            .append(" xml attribute names and ")
            .append(attributeValues.size())
            .append(" xml attribute values. The trailing extra data will be ignored.");
            logger.warn(msg.toString());
        }
        
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        for (int i = 0; i < attributeNames.size(); i++)
        {
            attrs.put(attributeNames.get(i), attributeValues.get(i));
        }
        fields.put(fieldId, new FormField(fieldId, attrs));
    }
    
    /* package */ void setFields(Map<String, FormField> newFieldsMap)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Setting new fields map " + newFieldsMap);
        }
        this.fields = newFieldsMap;
    }
    
    /* package */ void addControlForField(String fieldId, String template,
            List<String> controlParamNames, List<String> controlParamValues)
    {
        if (controlParamNames == null)
        {
            controlParamNames = Collections.emptyList();
        }
        if (controlParamValues == null)
        {
            controlParamValues = Collections.emptyList();
        }
        if (controlParamNames.size() < controlParamValues.size()
                && logger.isWarnEnabled())
        {
            StringBuilder msg = new StringBuilder();
            msg.append("field ")
            .append(fieldId)
            .append(" has ")
            .append(controlParamNames.size())
            .append(" control-param names and ")
            .append(controlParamValues.size())
            .append(" control-param values. The trailing extra data will be ignored.");
            logger.warn(msg.toString());
        }
        
        FormField field = fields.get(fieldId);
        field.getControl().setTemplate(template);
        for (int i = 0; i < controlParamNames.size(); i++)
        {
            ControlParam cp = new ControlParam(controlParamNames.get(i), controlParamValues.get(i));
            field.getControl().addControlParam(cp);
        }
    }
    
    /* package */void addConstraintForField(String fieldId, String type,
            String message, String messageId, String validationHandler, String event)
    {
        FormField field = fields.get(fieldId);
        field.addConstraintDefinition(type, message, messageId, validationHandler, event);
    }
}