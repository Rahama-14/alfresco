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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.config.Config;
import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigException;
import org.alfresco.config.xml.XMLConfigService;
import org.alfresco.util.BaseTest;

/**
 * JUnit tests to exercise the forms-related capabilities in to the web client
 * config service. These tests only include those that require a single config
 * xml file. Override-related tests, which use multiple config xml files, are
 * located in peer classes in this package.
 * 
 * @author Neil McErlean
 */
public class FormConfigTest extends BaseTest
{
    protected XMLConfigService configService;
    protected Config globalConfig;
    protected ConfigElement globalConstraintHandlers;
    protected FormsConfigElement formsConfigElement;
    protected FormConfigElement defaultFormInContent;
    protected DefaultControlsConfigElement defltCtrlsConfElement;

    protected String getConfigXmlFile()
    {
        return "test-config-forms.xml";
    }
    
    public void testFormSubmissionUrl()
    {
        assertEquals("Submission URL was incorrect.", "submission/url",
                defaultFormInContent.getSubmissionURL());
    }
    
    public void testGetFormTemplates()
    {
        assertEquals("Incorrect template.","/create/template",
        		defaultFormInContent.getFormTemplate(Mode.CREATE));
    }

    public void testGetFormFieldVisibilitiesForModes()
    {
        assertTrue("Field 'name' should be visible.", defaultFormInContent
                .isFieldVisible("name", Mode.CREATE));
        assertTrue("Field 'title' should be visible.", defaultFormInContent
                .isFieldVisible("title", Mode.CREATE));
        assertFalse("Field 'rubbish' should be invisible.", defaultFormInContent
                .isFieldVisible("rubbish", Mode.CREATE));

        assertTrue("Field 'name' should be visible.", defaultFormInContent
                .isFieldVisible("name", Mode.EDIT));
        assertFalse("Field 'title' should be invisible.", defaultFormInContent
                .isFieldVisible("title", Mode.EDIT));
        assertFalse("Field 'rubbish' should be invisible.", defaultFormInContent
                .isFieldVisible("rubbish", Mode.EDIT));

        assertTrue("Field 'name' should be visible.", defaultFormInContent
                .isFieldVisible("name", Mode.VIEW));
        assertTrue("Field 'title' should be visible.", defaultFormInContent
                .isFieldVisible("title", Mode.VIEW));
        assertFalse("Field 'rubbish' should be invisible.", defaultFormInContent
                .isFieldVisible("rubbish", Mode.VIEW));
    }
    
    public void testVisibleFieldsMustBeCorrectlyOrdered()
    {
        FormConfigElement setForm = readFormFromConfig("set_membership");

        List<String> fieldNames = setForm.getVisibleViewFieldNames();
    	
    	List<String> expectedFieldNames = new ArrayList<String>();
        expectedFieldNames.add("all");
        expectedFieldNames.add("v");
        expectedFieldNames.add("cev");
        expectedFieldNames.add("all_set");
        expectedFieldNames.add("v_set");
        expectedFieldNames.add("cev_set");
    	assertEquals("Visible fields wrong.", expectedFieldNames, fieldNames);
    }

    public void testGetSetsFromForm()
    {
        Set<String> expectedSetIds = new HashSet<String>();
        expectedSetIds.add(FormConfigElement.DEFAULT_SET_ID);
        expectedSetIds.add("details");
        expectedSetIds.add("user");
        assertEquals("Set IDs were wrong.", expectedSetIds, defaultFormInContent.getSets().keySet());

        Map<String, FormSet> sets = defaultFormInContent.getSets();
        assertEquals("Set parent was wrong.", "details", sets.get("user")
                .getParentId());
        assertEquals("Set parent was wrong.", null, sets.get("details")
                .getParentId());

        assertEquals("Set parent was wrong.", "fieldset", sets.get("details")
                .getAppearance());
        assertEquals("Set parent was wrong.", "panel", sets.get("user")
                .getAppearance());
    }

    /**
     * This test method retrieves those fields which are visible or hidden for a
     * particular mode and for the default set.
     */
    public void testGetFieldsInTheDefaultSet_ViewEditCreate()
    {
        FormConfigElement setForm = readFormFromConfig("set_membership");

        final String testSetId = FormConfigElement.DEFAULT_SET_ID;

        String[] visibleViewFields_Create = setForm.getVisibleCreateFieldNamesForSet(testSetId);
        String[] visibleViewFields_Edit = setForm.getVisibleEditFieldNamesForSet(testSetId);
        String[] visibleViewFields_View = setForm.getVisibleViewFieldNamesForSet(testSetId);
        
        assertNotNull(visibleViewFields_Create);
        assertNotNull(visibleViewFields_Edit);
        assertNotNull(visibleViewFields_View);
        
        assertEquals(Arrays.asList(new String[]{"all", "c", "ce", "cev"}),
                     Arrays.asList(visibleViewFields_Create));
        assertEquals(Arrays.asList(new String[]{"all", "e", "ce", "cev"}),
                     Arrays.asList(visibleViewFields_Edit));
        assertEquals(Arrays.asList(new String[]{"all", "v", "cev"}),
                     Arrays.asList(visibleViewFields_View));
    }
    
    /**
     * This test method retrieves those fields which are visible or hidden for a
     * particular mode and for a non-default set.
     */
    public void testGetFieldsInASet_ViewEditCreate()
    {
        FormConfigElement setForm = readFormFromConfig("set_membership");

        final String testSetId = "user";

        String[] visibleViewFields_Create = setForm.getVisibleCreateFieldNamesForSet(testSetId);
        String[] visibleViewFields_Edit = setForm.getVisibleEditFieldNamesForSet(testSetId);
        String[] visibleViewFields_View = setForm.getVisibleViewFieldNamesForSet(testSetId);

        assertEquals(Arrays.asList(visibleViewFields_Create),
                Arrays.asList(new String[]{"all_set", "c_set", "ce_set", "cev_set"}));
        assertEquals(Arrays.asList(visibleViewFields_Edit),
                Arrays.asList(new String[]{"all_set", "e_set", "ce_set", "cev_set"}));
        assertEquals(Arrays.asList(visibleViewFields_View),
                Arrays.asList(new String[]{"all_set", "v_set", "cev_set"}));
    }
    
    public void testGetRootSetsShouldReturnTheDefaultSetWhenNoSetsDeclared() throws Exception
    {
        FormConfigElement formWithoutSets = this.readFormFromConfig("form_without_sets");
        Map<String, FormSet> rootSets = formWithoutSets.getRootSets();
        
        assertEquals("A form without any explicit sets should return one root set.", 1, rootSets.size());
        assertEquals("The root set was incorrect.",
                FormConfigElement.DEFAULT_SET_ID, rootSets.keySet().iterator().next());
        assertNull(rootSets.get(FormConfigElement.DEFAULT_SET_ID).getParentId());
    }
    
    /**
     * Even when no sets are explicitly declared in the config xml, there is always
     * the default set. In this scenario all fields are implicit members of that set and
     * should be visible as normal.
     */
    public void testGetVisibleFieldNamesForDefaultSet() throws Exception
    {
        FormConfigElement formWithoutSets = this.readFormFromConfig("form_without_sets");
        final String testSetId = FormConfigElement.DEFAULT_SET_ID;
        String[] visibleCreateFields = formWithoutSets.getVisibleCreateFieldNamesForSet(testSetId);
        String[] visibleEditFields = formWithoutSets.getVisibleEditFieldNamesForSet(testSetId);
        String[] visibleViewFields = formWithoutSets.getVisibleViewFieldNamesForSet(testSetId);
        
        List<String> expectedCreateFields = Arrays.asList(new String[]{"all", "c", "ce", "cev"});
        List<String> expectedEditFields = Arrays.asList(new String[]{"all", "e", "ce", "cev"});
        List<String> expectedViewFields = Arrays.asList(new String[]{"all", "v", "cev"});
        
        assertEquals(expectedCreateFields, Arrays.asList(visibleCreateFields));
        assertEquals(expectedEditFields, Arrays.asList(visibleEditFields));
        assertEquals(expectedViewFields, Arrays.asList(visibleViewFields));
    }
    
    public void testAccessAllFieldRelatedData()
    {
        // Field checks
        Map<String, FormField> fields = defaultFormInContent.getFields();
        assertEquals("Wrong number of Fields.", 5, fields.size());

        FormField usernameField = fields.get("username");
        assertNotNull("usernameField was null.", usernameField);
        assertTrue("Missing attribute.", usernameField.getAttributes()
                .containsKey("set"));
        assertEquals("Incorrect attribute.", "user", usernameField
                .getAttributes().get("set"));
        assertNull("username field's template should be null.", usernameField.getControl()
                .getTemplate());

        FormField nameField = fields.get("name");
        String nameTemplate = nameField.getControl().getTemplate();
        assertNotNull("name field had null template", nameTemplate);
        assertEquals("name field had incorrect template.",
                "alfresco/extension/formcontrols/my-name.ftl", nameTemplate);

        List<ControlParam> controlParams = nameField.getControl().getParams();
        assertNotNull("name field should have control params.", controlParams);
        assertEquals("name field has incorrect number of control params.", 1,
                controlParams.size());

        ControlParam firstCP = controlParams.iterator().next();
        assertEquals("Control param has wrong name.", "foo", firstCP.getName());
        assertEquals("Control param has wrong value.", "bar", firstCP.getValue());

        ConstraintHandlerDefinition regExConstraint
            = nameField.getConstraintDefinitionMap().values().iterator().next();
        assertEquals("name field had incorrect type.", "REGEX",
        		regExConstraint.getType());
        assertEquals("name field had incorrect message.",
                "The name can not contain the character '{0}'",
                regExConstraint.getMessage());
        assertEquals("name field had incorrect message-id.",
                "field_error_name", regExConstraint.getMessageId());
    }

    public void testControlParamSequenceThatIncludesValuelessParamsParsesCorrectly()
    {
        // Field checks
        Map<String, FormField> fields = defaultFormInContent.getFields();

        FormField testField = fields.get("fieldWithMixedCtrlParams");

        List<ControlParam> controlParams = testField.getControl().getParams();
        assertNotNull("field should have control params.", controlParams);
        assertEquals("field has incorrect number of control params.", 4,
                controlParams.size());

        List<ControlParam> expectedCPs = new ArrayList<ControlParam>();
        expectedCPs.add(new ControlParam("one", "un"));
        expectedCPs.add(new ControlParam("two", "deux"));
        expectedCPs.add(new ControlParam("three", ""));
        expectedCPs.add(new ControlParam("four", "quatre"));
        
        assertEquals(expectedCPs, controlParams);
    }
    
    public void testFormsShouldSupportMultipleConstraintMessageTags()
    {
    	FormField nameField = defaultFormInContent.getFields().get("name");
    	Map<String, ConstraintHandlerDefinition> constraintDefinitions = nameField.getConstraintDefinitionMap();
		assertEquals(2, constraintDefinitions.size());
    	
		Iterator<ConstraintHandlerDefinition> valuesIterator = constraintDefinitions.values().iterator();
    	ConstraintHandlerDefinition regexField = valuesIterator.next();
    	ConstraintHandlerDefinition applesField = valuesIterator.next();

    	assertEquals("REGEX", regexField.getType());
    	assertEquals("LIMIT", applesField.getType());
    	
    	assertEquals("limit exceeded", applesField.getMessage());
    	assertEquals("", applesField.getMessageId());
    }
    
    public void testFormConfigElementShouldHaveNoChildren()
    {
        try
        {
            defaultFormInContent.getChildren();
            fail("getChildren() did not throw an exception.");
        } catch (ConfigException expectedException)
        {
            // intentionally empty
        }
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        configService = initXMLConfigService(getConfigXmlFile());
        assertNotNull("configService was null.", configService);
    
        Config contentConfig = configService.getConfig("content");
        assertNotNull("contentConfig was null.", contentConfig);
    
        ConfigElement confElement = contentConfig.getConfigElement("forms");
        assertNotNull("confElement was null.", confElement);
        assertTrue("confElement should be instanceof FormsConfigElement.",
                confElement instanceof FormsConfigElement);
        formsConfigElement = (FormsConfigElement) confElement;
        defaultFormInContent = formsConfigElement.getDefaultForm();
    
        globalConfig = configService.getGlobalConfig();
    
        FormsConfigElement globalForms = (FormsConfigElement)globalConfig.getConfigElement("forms");
        defltCtrlsConfElement = globalForms.getDefaultControls();
        assertNotNull("global default-controls element should not be null",
                defltCtrlsConfElement);
    
        globalConstraintHandlers = globalForms.getConstraintHandlers();
        assertNotNull("global constraint-handlers element should not be null",
                globalConstraintHandlers);
    }

    private FormConfigElement readFormFromConfig(String condition)
    {
        Config setConfig = configService.getConfig(condition);
        assertNotNull("setConfig was null.", setConfig);
    
        ConfigElement confElement = setConfig.getConfigElement("forms");
        assertNotNull("confElement was null.", confElement);
        assertTrue("confElement should be instanceof FormsConfigElement.",
                confElement instanceof FormsConfigElement);
        FormsConfigElement setFormsConfigElement = (FormsConfigElement) confElement;
        FormConfigElement setForm = setFormsConfigElement.getDefaultForm();
        return setForm;
    }
}
