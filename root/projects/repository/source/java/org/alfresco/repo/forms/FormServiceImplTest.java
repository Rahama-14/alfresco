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
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.forms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.forms.AssociationFieldDefinition.Direction;
import org.alfresco.repo.forms.PropertyFieldDefinition.FieldConstraint;
import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseAlfrescoSpringTest;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;
import org.springframework.util.StringUtils;

/**
 * Form service implementation unit test.
 * 
 * @author Gavin Cornwell
 */
public class FormServiceImplTest extends BaseAlfrescoSpringTest 
{
    private FormService formService;
    private NamespaceService namespaceService;
    private ScriptService scriptService;
    private PersonService personService;
    private ContentService contentService;

    private NodeRef document;
    private NodeRef associatedDoc;
    private NodeRef childDoc;
    private NodeRef folder;
    private String documentName;
    private String folderName;
    
    private static String VALUE_TITLE = "This is the title for the test document";
    private static String VALUE_DESCRIPTION = "This is the description for the test document";
    private static String VALUE_ORIGINATOR = "fred@customer.com";
    private static String VALUE_ADDRESSEE = "bill@example.com";
    private static String VALUE_ADDRESSEES1 = "harry@example.com";
    private static String VALUE_ADDRESSEES2 = "jane@example.com";
    private static String VALUE_ADDRESSEES3 = "alice@example.com";
    private static String VALUE_SUBJECT = "The subject is...";
    private static String VALUE_MIMETYPE = MimetypeMap.MIMETYPE_TEXT_PLAIN;
    private static String VALUE_ENCODING = "UTF-8";
    private static String VALUE_CONTENT = "This is the content for the test document";
    private static String VALUE_ASSOC_CONTENT = "This is the content for the associated document";
    private static Date VALUE_SENT_DATE = new Date();
    
    private static String LABEL_NAME = "Name";
    private static String LABEL_TITLE = "Title";
    private static String LABEL_DESCRIPTION = "Description";
    private static String LABEL_AUTHOR = "Author";
    private static String LABEL_MODIFIED = "Modified Date";
    private static String LABEL_MODIFIER = "Modifier";
    private static String LABEL_MIMETYPE = "Mimetype";
    private static String LABEL_ENCODING = "Encoding";
    private static String LABEL_SIZE = "Size";
    private static String LABEL_ORIGINATOR = "Originator";
    private static String LABEL_ADDRESSEE = "Addressee";
    private static String LABEL_ADDRESSEES = "Addressees";
    private static String LABEL_SUBJECT = "Subject";
    private static String LABEL_SENT_DATE = "Sent Date";
    private static String LABEL_REFERENCES = "References";
    private static String LABEL_CONTAINS = "Contains";
    
    private static final String USER_ONE = "UserOne_FormServiceImplTest";
    private static final String NODE_FORM_ITEM_KIND = "node";
    
    /**
     * Called during the transaction setup
     */
    @Override
    protected void onSetUpInTransaction() throws Exception
    {
        super.onSetUpInTransaction();
        
        // Get the required services
        this.formService = (FormService)this.applicationContext.getBean("FormService");
        this.namespaceService = (NamespaceService)this.applicationContext.getBean("NamespaceService");
        this.scriptService = (ScriptService)this.applicationContext.getBean("ScriptService");
        this.personService = (PersonService)this.applicationContext.getBean("PersonService");
        this.contentService = (ContentService)this.applicationContext.getBean("ContentService");
        
        AuthenticationComponent authenticationComponent = (AuthenticationComponent) this.applicationContext
                .getBean("authenticationComponent");
        
        // Do the tests as userOne
        createUser(USER_ONE);
        authenticationComponent.setCurrentUser(USER_ONE);
        
        String guid = GUID.generate();
        
        NodeRef rootNode = this.nodeService.getRootNode(
                    new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"));
        
        Map<QName, Serializable> folderProps = new HashMap<QName, Serializable>(1);
        this.folderName = "testFolder" + guid;
        folderProps.put(ContentModel.PROP_NAME, this.folderName);
        this.folder = this.nodeService.createNode(
                rootNode, 
                ContentModel.ASSOC_CHILDREN, 
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "testFolder" + guid),
                ContentModel.TYPE_FOLDER,
                folderProps).getChildRef();
        
        // Create a node
        Map<QName, Serializable> docProps = new HashMap<QName, Serializable>(1);
        this.documentName = "testDocument" + guid + ".txt";
        docProps.put(ContentModel.PROP_NAME, this.documentName);
        this.document = this.nodeService.createNode(
                this.folder, 
                ContentModel.ASSOC_CONTAINS, 
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "testDocument" + guid + ".txt"), 
                ContentModel.TYPE_CONTENT,
                docProps).getChildRef();    
       
        // create a node to use as target of association
        docProps.put(ContentModel.PROP_NAME, "associatedDocument" + guid + ".txt");
        this.associatedDoc = this.nodeService.createNode(
                    this.folder, 
                    ContentModel.ASSOC_CONTAINS, 
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "associatedDocument" + guid + ".txt"), 
                    ContentModel.TYPE_CONTENT,
                    docProps).getChildRef();
        
        // create a node to use as a 2nd child node of the folder
        docProps.put(ContentModel.PROP_NAME, "childDocument" + guid + ".txt");
        this.childDoc = this.nodeService.createNode(
                    this.folder, 
                    ContentModel.ASSOC_CONTAINS, 
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "childDocument" + guid + ".txt"), 
                    ContentModel.TYPE_CONTENT,
                    docProps).getChildRef();
        
        // add some content to the nodes
        ContentWriter writer = this.contentService.getWriter(this.document, ContentModel.PROP_CONTENT, true);
        writer.setMimetype(VALUE_MIMETYPE);
        writer.setEncoding(VALUE_ENCODING);
        writer.putContent(VALUE_CONTENT);
        
        ContentWriter writer2 = this.contentService.getWriter(this.associatedDoc, ContentModel.PROP_CONTENT, true);
        writer2.setMimetype(VALUE_MIMETYPE);
        writer2.setEncoding(VALUE_ENCODING);
        writer2.putContent(VALUE_ASSOC_CONTENT);
        
        // add standard titled aspect
        Map<QName, Serializable> aspectProps = new HashMap<QName, Serializable>(2);
        aspectProps.put(ContentModel.PROP_TITLE, VALUE_TITLE);
        aspectProps.put(ContentModel.PROP_DESCRIPTION, VALUE_DESCRIPTION);
        this.nodeService.addAspect(this.document, ContentModel.ASPECT_TITLED, aspectProps);
        
        // add emailed aspect (has multiple value field)
        aspectProps = new HashMap<QName, Serializable>(5);
        aspectProps.put(ContentModel.PROP_ORIGINATOR, VALUE_ORIGINATOR);
        aspectProps.put(ContentModel.PROP_ADDRESSEE, VALUE_ADDRESSEE);
        List<String> addressees = new ArrayList<String>(2);
        addressees.add(VALUE_ADDRESSEES1);
        addressees.add(VALUE_ADDRESSEES2);
        aspectProps.put(ContentModel.PROP_ADDRESSEES, (Serializable)addressees);
        aspectProps.put(ContentModel.PROP_SUBJECT, VALUE_SUBJECT);
        aspectProps.put(ContentModel.PROP_SENTDATE, VALUE_SENT_DATE);
        this.nodeService.addAspect(this.document, ContentModel.ASPECT_MAILED, aspectProps);
        
        // add referencing aspect (has association)
        aspectProps.clear();
        this.nodeService.addAspect(document, ContentModel.ASPECT_REFERENCING, aspectProps);
        this.nodeService.createAssociation(this.document, this.associatedDoc, ContentModel.ASSOC_REFERENCES);
    }
    
    private void createUser(String userName)
    {
        if (this.authenticationService.authenticationExists(userName) == false)
        {
            this.authenticationService.createAuthentication(userName, "PWD".toCharArray());
            
            PropertyMap ppOne = new PropertyMap(4);
            ppOne.put(ContentModel.PROP_USERNAME, userName);
            ppOne.put(ContentModel.PROP_FIRSTNAME, "firstName");
            ppOne.put(ContentModel.PROP_LASTNAME, "lastName");
            ppOne.put(ContentModel.PROP_EMAIL, "email@email.com");
            ppOne.put(ContentModel.PROP_JOBTITLE, "jobTitle");
            
            this.personService.createPerson(ppOne);
        }        
    }
	
    @SuppressWarnings("unchecked")
	public void testGetAllDocForm() throws Exception
    {
        Form form = this.formService.getForm(new Item(NODE_FORM_ITEM_KIND, this.document.toString()));
        
        // check a form got returned
        assertNotNull("Expecting form to be present", form);
        
        // check item identifier matches
        assertEquals(NODE_FORM_ITEM_KIND, form.getItem().getKind());
        assertEquals(this.document.toString(), form.getItem().getId());
        
        // check the type is correct
        assertEquals(ContentModel.TYPE_CONTENT.toPrefixString(this.namespaceService), 
                    form.getItem().getType());
        
        // check there is no group info
        assertNull("Expecting the form groups to be null!", form.getFieldGroups());
        
        // check the field definitions
        Collection<FieldDefinition> fieldDefs = form.getFieldDefinitions();
        assertNotNull("Expecting to find fields", fieldDefs);
        assertEquals("Expecting to find 22 fields", 22, fieldDefs.size());
        
        // create a Map of the field definitions
        // NOTE: we can safely do this as we know there are no duplicate field names and we're not
        //       concerned with ordering!
        Map<String, FieldDefinition> fieldDefMap = new HashMap<String, FieldDefinition>(fieldDefs.size());
        for (FieldDefinition fieldDef : fieldDefs)
        {
            fieldDefMap.put(fieldDef.getName(), fieldDef);
        }
        
        // find the fields
        PropertyFieldDefinition nameField = (PropertyFieldDefinition)fieldDefMap.get("cm:name");
        PropertyFieldDefinition titleField = (PropertyFieldDefinition)fieldDefMap.get("cm:title");
        PropertyFieldDefinition descField = (PropertyFieldDefinition)fieldDefMap.get("cm:description");
        PropertyFieldDefinition mimetypeField = (PropertyFieldDefinition)fieldDefMap.get("mimetype");
        PropertyFieldDefinition encodingField = (PropertyFieldDefinition)fieldDefMap.get("encoding");
        PropertyFieldDefinition sizeField = (PropertyFieldDefinition)fieldDefMap.get("size");
        PropertyFieldDefinition originatorField = (PropertyFieldDefinition)fieldDefMap.get("cm:originator");
        PropertyFieldDefinition addresseeField = (PropertyFieldDefinition)fieldDefMap.get("cm:addressee");
        PropertyFieldDefinition addresseesField = (PropertyFieldDefinition)fieldDefMap.get("cm:addressees");
        PropertyFieldDefinition subjectField = (PropertyFieldDefinition)fieldDefMap.get("cm:subjectline");
        PropertyFieldDefinition sentDateField = (PropertyFieldDefinition)fieldDefMap.get("cm:sentdate");
        AssociationFieldDefinition referencesField = (AssociationFieldDefinition)fieldDefMap.get("cm:references");
        
        // check fields are present
        assertNotNull("Expecting to find the cm:name field", nameField);
        assertNotNull("Expecting to find the cm:title field", titleField);
        assertNotNull("Expecting to find the cm:description field", descField);
        assertNotNull("Expecting to find the mimetype field", mimetypeField);
        assertNotNull("Expecting to find the encoding field", encodingField);
        assertNotNull("Expecting to find the size field", sizeField);
        assertNotNull("Expecting to find the cm:originator field", originatorField);
        assertNotNull("Expecting to find the cm:addressee field", addresseeField);
        assertNotNull("Expecting to find the cm:addressees field", addresseesField);
        assertNotNull("Expecting to find the cm:subjectline field", subjectField);
        assertNotNull("Expecting to find the cm:sentdate field", sentDateField);
        assertNotNull("Expecting to find the cm:references field", referencesField);
        
        // check the labels of all the fields
        assertEquals("Expecting cm:name label to be " + LABEL_NAME, 
                    LABEL_NAME, nameField.getLabel());
        assertEquals("Expecting cm:title label to be " + LABEL_TITLE, 
                    LABEL_TITLE, titleField.getLabel());
        assertEquals("Expecting cm:description label to be " + LABEL_DESCRIPTION, 
                    LABEL_DESCRIPTION, descField.getLabel());
        assertEquals("Expecting mimetype label to be " + LABEL_MIMETYPE, 
                    LABEL_MIMETYPE, mimetypeField.getLabel());
        assertEquals("Expecting encoding label to be " + LABEL_ENCODING, 
                    LABEL_ENCODING, encodingField.getLabel());
        assertEquals("Expecting size label to be " + LABEL_SIZE, 
                    LABEL_SIZE, sizeField.getLabel());
        assertEquals("Expecting cm:originator label to be " + LABEL_ORIGINATOR, 
                    LABEL_ORIGINATOR, originatorField.getLabel());
        assertEquals("Expecting cm:addressee label to be " + LABEL_ADDRESSEE, 
                    LABEL_ADDRESSEE, addresseeField.getLabel());
        assertEquals("Expecting cm:addressees label to be " + LABEL_ADDRESSEES, 
                    LABEL_ADDRESSEES, addresseesField.getLabel());
        assertEquals("Expecting cm:subjectline label to be " + LABEL_SUBJECT, 
                    LABEL_SUBJECT, subjectField.getLabel());
        assertEquals("Expecting cm:sentdate label to be " + LABEL_SENT_DATE, 
                    LABEL_SENT_DATE, sentDateField.getLabel());
        assertEquals("Expecting cm:references label to be " + LABEL_REFERENCES, 
                    LABEL_REFERENCES, referencesField.getLabel());
        
        // check details of name field
        assertEquals("Expecting cm:name type to be d:text", "d:text", nameField.getDataType());
        assertTrue("Expecting cm:name to be mandatory", nameField.isMandatory());
        assertFalse("Expecting cm:name to be single valued", nameField.isRepeating());
        
        // get the constraint for the name field and check
        List<FieldConstraint> constraints = nameField.getConstraints();
        assertEquals("Expecting 1 constraint for cm:name", 1, constraints.size());
        FieldConstraint constraint = constraints.get(0);
        assertEquals("Expecting name of constraint to be 'REGEX'", "REGEX", constraint.getType());
        Map<String, String> params = constraint.getParams();
        assertNotNull("Expecting constraint parameters", params);
        assertEquals("Expecting 2 constraint parameters", 2, params.size());
        assertNotNull("Expecting an 'expression' constraint parameter", params.get("expression"));
        assertNotNull("Expecting an 'requiresMatch' constraint parameter", params.get("requiresMatch"));
        
        // check details of the addressees field
        assertEquals("Expecting cm:addressees type to be d:text", "d:text", addresseesField.getDataType());
        assertFalse("Expecting cm:addressees to be mandatory", addresseesField.isMandatory());
        assertTrue("Expecting cm:addressees to be multi valued", addresseesField.isRepeating());
        assertNull("Expecting constraints for cm:addressees to be null", addresseesField.getConstraints());
        
        // check the details of the association field
        assertEquals("Expecting cm:references endpoint type to be cm:content", "cm:content", 
                    referencesField.getEndpointType());
        assertEquals("Expecting cm:references endpoint direction to be TARGET", 
                    Direction.TARGET.toString(),
                    referencesField.getEndpointDirection().toString());
        assertFalse("Expecting cm:references endpoint to be optional", 
                    referencesField.isEndpointMandatory());
        assertTrue("Expecting cm:references endpoint to be 1 to many",
                    referencesField.isEndpointMany());
        
        // check the form data
        FormData data = form.getFormData();
        assertNotNull("Expecting form data", data);
        Map<String, FormData.FieldData> fieldData = data.getData();
        assertNotNull("Expecting field data", fieldData);
        assertEquals(VALUE_TITLE, fieldData.get(titleField.getDataKeyName()).getValue());
        assertEquals(VALUE_DESCRIPTION, fieldData.get(descField.getDataKeyName()).getValue());
        assertEquals(VALUE_MIMETYPE, fieldData.get(mimetypeField.getDataKeyName()).getValue());
        assertEquals(VALUE_ENCODING, fieldData.get(encodingField.getDataKeyName()).getValue());
        assertEquals(VALUE_ORIGINATOR, fieldData.get(originatorField.getDataKeyName()).getValue());
        assertEquals(VALUE_ADDRESSEE, fieldData.get(addresseeField.getDataKeyName()).getValue());
        assertEquals(VALUE_SUBJECT, fieldData.get(subjectField.getDataKeyName()).getValue());
        assertTrue("Expecting size to be > 0", ((Long)fieldData.get(sizeField.getDataKeyName()).getValue()).longValue() > 0);
        
        String addressees = (String)fieldData.get(addresseesField.getDataKeyName()).getValue();
        assertNotNull(addressees);
        assertTrue("Expecting the addressees value to have at least 1 comma", addressees.indexOf(",") != -1);
        String[] addresseesArr = StringUtils.delimitedListToStringArray(addressees, ",");
        assertEquals("Expecting 2 addressees", 2, addresseesArr.length);
        assertEquals(VALUE_ADDRESSEES1, addresseesArr[0]);
        assertEquals(VALUE_ADDRESSEES2, addresseesArr[1]);
        
        Calendar calTestValue = Calendar.getInstance();
        calTestValue.setTime(VALUE_SENT_DATE);
        Calendar calServiceValue = Calendar.getInstance();
        calServiceValue.setTime((Date)fieldData.get(sentDateField.getDataKeyName()).getValue());
        assertEquals(calTestValue.getTimeInMillis(), calServiceValue.getTimeInMillis());
        
        List<String> targets = (List<String>)fieldData.get(referencesField.getDataKeyName()).getValue();
        assertEquals("Expecting 1 target", 1, targets.size());
        assertEquals(this.associatedDoc.toString(), targets.get(0));
    }
    
    @SuppressWarnings("unchecked")
    public void testGetSelectedFieldsDocForm() throws Exception
    {
        // define a list of fields to retrieve from the node
        List<String> fields = new ArrayList<String>(8);
        fields.add("cm:name");
        fields.add("cm:title");
        fields.add("mimetype");
        fields.add("cm:modified");
        fields.add("cm:modifier");
        fields.add("cm:subjectline");
        fields.add("cm:sentdate");
        fields.add("cm:references");
        
        Form form = this.formService.getForm(new Item(NODE_FORM_ITEM_KIND, this.document.toString()), fields);
        
        // check a form got returned
        assertNotNull("Expecting form to be present", form);
        
        // check item identifier matches
        assertEquals(NODE_FORM_ITEM_KIND, form.getItem().getKind());
        assertEquals(this.document.toString(), form.getItem().getId());
        
        // check the type is correct
        assertEquals(ContentModel.TYPE_CONTENT.toPrefixString(this.namespaceService), 
                    form.getItem().getType());
        
        // check there is no group info
        assertNull("Expecting the form groups to be null!", form.getFieldGroups());
        
        // check the field definitions
        Collection<FieldDefinition> fieldDefs = form.getFieldDefinitions();
        assertNotNull("Expecting to find fields", fieldDefs);
        assertEquals("Expecting to find " + fields.size() + " fields", fields.size(), fieldDefs.size());
        
        // create a Map of the field definitions
        // NOTE: we can safely do this as we know there are no duplicate field names and we're not
        //       concerned with ordering!
        Map<String, FieldDefinition> fieldDefMap = new HashMap<String, FieldDefinition>(fieldDefs.size());
        for (FieldDefinition fieldDef : fieldDefs)
        {
            fieldDefMap.put(fieldDef.getName(), fieldDef);
        }
        
        // find the fields
        PropertyFieldDefinition nameField = (PropertyFieldDefinition)fieldDefMap.get("cm:name");
        PropertyFieldDefinition titleField = (PropertyFieldDefinition)fieldDefMap.get("cm:title");
        PropertyFieldDefinition mimetypeField = (PropertyFieldDefinition)fieldDefMap.get("mimetype");
        PropertyFieldDefinition modifiedField = (PropertyFieldDefinition)fieldDefMap.get("cm:modified");
        PropertyFieldDefinition modifierField = (PropertyFieldDefinition)fieldDefMap.get("cm:modifier");
        PropertyFieldDefinition subjectField = (PropertyFieldDefinition)fieldDefMap.get("cm:subjectline");
        PropertyFieldDefinition sentDateField = (PropertyFieldDefinition)fieldDefMap.get("cm:sentdate");
        AssociationFieldDefinition referencesField = (AssociationFieldDefinition)fieldDefMap.get("cm:references");
        
        // check fields are present
        assertNotNull("Expecting to find the cm:name field", nameField);
        assertNotNull("Expecting to find the cm:title field", titleField);
        assertNotNull("Expecting to find the mimetype field", mimetypeField);
        assertNotNull("Expecting to find the cm:modified field", modifiedField);
        assertNotNull("Expecting to find the cm:modifier field", modifierField);
        assertNotNull("Expecting to find the cm:subjectline field", subjectField);
        assertNotNull("Expecting to find the cm:sentdate field", sentDateField);
        assertNotNull("Expecting to find the cm:references field", referencesField);
        
        // check the labels of all the fields
        assertEquals("Expecting cm:name label to be " + LABEL_NAME, 
                    LABEL_NAME, nameField.getLabel());
        assertEquals("Expecting cm:title label to be " + LABEL_TITLE, 
                    LABEL_TITLE, titleField.getLabel());
        assertEquals("Expecting mimetype label to be " + LABEL_MIMETYPE, 
                    LABEL_MIMETYPE, mimetypeField.getLabel());
        assertEquals("Expecting cm:modified label to be " + LABEL_MODIFIED, 
                    LABEL_MODIFIED, modifiedField.getLabel());
        assertEquals("Expecting cm:modifier label to be " + LABEL_MODIFIER, 
                    LABEL_MODIFIER, modifierField.getLabel());
        assertEquals("Expecting cm:subjectline label to be " + LABEL_SUBJECT, 
                    LABEL_SUBJECT, subjectField.getLabel());
        assertEquals("Expecting cm:sentdate label to be " + LABEL_SENT_DATE, 
                    LABEL_SENT_DATE, sentDateField.getLabel());
        assertEquals("Expecting cm:references label to be " + LABEL_REFERENCES, 
                    LABEL_REFERENCES, referencesField.getLabel());
        
        // check the details of the modified field
        assertEquals("Expecting cm:modified type to be d:datetime", "d:datetime", modifiedField.getDataType());
        assertTrue("Expecting cm:modified to be mandatory", modifiedField.isMandatory());
        assertFalse("Expecting cm:modified to be single valued", modifiedField.isRepeating());
        
        // check the details of the association field
        assertEquals("Expecting cm:references endpoint type to be cm:content", "cm:content", 
                    referencesField.getEndpointType());
        assertEquals("Expecting cm:references endpoint direction to be TARGET", 
                    Direction.TARGET.toString(),
                    referencesField.getEndpointDirection().toString());
        assertFalse("Expecting cm:references endpoint to be optional", 
                    referencesField.isEndpointMandatory());
        assertTrue("Expecting cm:references endpoint to be 1 to many",
                    referencesField.isEndpointMany());
        
        // check the form data
        FormData data = form.getFormData();
        assertNotNull("Expecting form data", data);
        Map<String, FormData.FieldData> fieldData = data.getData();
        assertNotNull("Expecting field data", fieldData);
        assertEquals(this.documentName, fieldData.get(nameField.getDataKeyName()).getValue());
        assertEquals(VALUE_TITLE, fieldData.get(titleField.getDataKeyName()).getValue());
        assertEquals(VALUE_MIMETYPE, fieldData.get(mimetypeField.getDataKeyName()).getValue());
        assertEquals(VALUE_SUBJECT, fieldData.get(subjectField.getDataKeyName()).getValue());
        assertEquals(USER_ONE, fieldData.get(modifierField.getDataKeyName()).getValue());

        Date modifiedDate = (Date)fieldData.get(modifiedField.getDataKeyName()).getValue();
        assertNotNull("Expecting to find modified date", modifiedDate);
        assertTrue("Expecting modified field to return a Date", (modifiedDate instanceof Date));
        
        Calendar calTestValue = Calendar.getInstance();
        calTestValue.setTime(VALUE_SENT_DATE);
        Calendar calServiceValue = Calendar.getInstance();
        calServiceValue.setTime((Date)fieldData.get(sentDateField.getDataKeyName()).getValue());
        assertEquals(calTestValue.getTimeInMillis(), calServiceValue.getTimeInMillis());
        
        List<String> targets = (List<String>)fieldData.get(referencesField.getDataKeyName()).getValue();
        assertEquals("Expecting 1 target", 1, targets.size());
        assertEquals(this.associatedDoc.toString(), targets.get(0));
    }
    
    public void testMissingFieldsDocForm() throws Exception
    {
        // define a list of fields to retrieve from the node
        List<String> fields = new ArrayList<String>(8);
        fields.add("cm:name");
        fields.add("cm:title");
        
        // add fields that will not be present
        fields.add("cm:author");
        fields.add("wrong-name");
        
        Form form = this.formService.getForm(new Item(NODE_FORM_ITEM_KIND, this.document.toString()), fields);
        
        // check a form got returned
        assertNotNull("Expecting form to be present", form);
        
        // check the field definitions
        Collection<FieldDefinition> fieldDefs = form.getFieldDefinitions();
        assertNotNull("Expecting to find fields", fieldDefs);
        assertEquals("Expecting to find " + (fields.size()-2) + " fields", fields.size()-2, fieldDefs.size());
        
        // create a Map of the field definitions
        // NOTE: we can safely do this as we know there are no duplicate field names and we're not
        //       concerned with ordering!
        Map<String, FieldDefinition> fieldDefMap = new HashMap<String, FieldDefinition>(fieldDefs.size());
        for (FieldDefinition fieldDef : fieldDefs)
        {
            fieldDefMap.put(fieldDef.getName(), fieldDef);
        }
        
        // find the fields
        PropertyFieldDefinition nameField = (PropertyFieldDefinition)fieldDefMap.get("cm:name");
        PropertyFieldDefinition titleField = (PropertyFieldDefinition)fieldDefMap.get("cm:title");
        PropertyFieldDefinition authorField = (PropertyFieldDefinition)fieldDefMap.get("cm:author");
        
        // check fields are present
        assertNotNull("Expecting to find the cm:name field", nameField);
        assertNotNull("Expecting to find the cm:title field", titleField);
        assertNull("Expecting cm:author field to be missing", authorField);
        
        // check the labels of all the fields
        assertEquals("Expecting cm:name label to be " + LABEL_NAME, 
                    LABEL_NAME, nameField.getLabel());
        assertEquals("Expecting cm:title label to be " + LABEL_TITLE, 
                    LABEL_TITLE, titleField.getLabel());
        
        // check the form data
        FormData data = form.getFormData();
        assertNotNull("Expecting form data", data);
        Map<String, FormData.FieldData> fieldData = data.getData();
        assertNotNull("Expecting field data", fieldData);
        assertEquals(this.documentName, fieldData.get(nameField.getDataKeyName()).getValue());
        assertEquals(VALUE_TITLE, fieldData.get(titleField.getDataKeyName()).getValue());
    }
    
    public void testForcedFieldsDocForm() throws Exception
    {
        // define a list of fields to retrieve from the node
        List<String> fields = new ArrayList<String>(4);
        fields.add("cm:name");
        fields.add("cm:title");
        
        // add fields that will not be present
        fields.add("cm:author");
        fields.add("cm:never");
        fields.add("wrong-name");
        
        // try and force the missing fields to appear
        List<String> forcedFields = new ArrayList<String>(2);
        forcedFields.add("cm:author");
        forcedFields.add("cm:never");
        forcedFields.add("wrong-name");
        
        Form form = this.formService.getForm(new Item(NODE_FORM_ITEM_KIND, this.document.toString()), fields, forcedFields);
        
        // check a form got returned
        assertNotNull("Expecting form to be present", form);
        
        // check the field definitions
        Collection<FieldDefinition> fieldDefs = form.getFieldDefinitions();
        assertNotNull("Expecting to find fields", fieldDefs);
        assertEquals("Expecting to find " + (fields.size()-2) + " fields", fields.size()-2, fieldDefs.size());
        
        // create a Map of the field definitions
        // NOTE: we can safely do this as we know there are no duplicate field names and we're not
        //       concerned with ordering!
        Map<String, FieldDefinition> fieldDefMap = new HashMap<String, FieldDefinition>(fieldDefs.size());
        for (FieldDefinition fieldDef : fieldDefs)
        {
            fieldDefMap.put(fieldDef.getName(), fieldDef);
        }
        
        // find the fields
        PropertyFieldDefinition nameField = (PropertyFieldDefinition)fieldDefMap.get("cm:name");
        PropertyFieldDefinition titleField = (PropertyFieldDefinition)fieldDefMap.get("cm:title");
        PropertyFieldDefinition authorField = (PropertyFieldDefinition)fieldDefMap.get("cm:author");
        PropertyFieldDefinition neverField = (PropertyFieldDefinition)fieldDefMap.get("cm:never");
        PropertyFieldDefinition wrongField = (PropertyFieldDefinition)fieldDefMap.get("wrong-name");
        
        // check fields are present
        assertNotNull("Expecting to find the cm:name field", nameField);
        assertNotNull("Expecting to find the cm:title field", titleField);
        assertNotNull("Expecting to find the cm:author field", authorField);
        assertNull("Expecting cm:never field to be missing", neverField);
        assertNull("Expecting wrong-name field to be missing", wrongField);
        
        // check the labels of all the fields
        assertEquals("Expecting cm:name label to be " + LABEL_NAME, 
                    LABEL_NAME, nameField.getLabel());
        assertEquals("Expecting cm:title label to be " + LABEL_TITLE, 
                    LABEL_TITLE, titleField.getLabel());
        assertEquals("Expecting cm:author label to be " + LABEL_AUTHOR, 
                    LABEL_AUTHOR, authorField.getLabel());
        
        // check the form data
        FormData data = form.getFormData();
        assertNotNull("Expecting form data", data);
        Map<String, FormData.FieldData> fieldData = data.getData();
        assertNotNull("Expecting field data", fieldData);
        assertEquals(this.documentName, fieldData.get(nameField.getDataKeyName()).getValue());
        assertEquals(VALUE_TITLE, fieldData.get(titleField.getDataKeyName()).getValue());
        assertNull("Didn't expect to find a value for cm:author", fieldData.get(authorField.getDataKeyName()));
    }
    
    @SuppressWarnings("unchecked")
    public void testGetAllFolderForm() throws Exception
    {
        Form form = this.formService.getForm(new Item(NODE_FORM_ITEM_KIND, this.folder.toString()));
        
        // check a form got returned
        assertNotNull("Expecting form to be present", form);
        
        // check item identifier matches
        assertEquals(NODE_FORM_ITEM_KIND, form.getItem().getKind());
        assertEquals(this.folder.toString(), form.getItem().getId());
        
        // check the type is correct
        assertEquals(ContentModel.TYPE_FOLDER.toPrefixString(this.namespaceService), 
                    form.getItem().getType());
        
        // check there is no group info
        assertNull("Expecting the form groups to be null!", form.getFieldGroups());
        
        // check the field definitions
        Collection<FieldDefinition> fieldDefs = form.getFieldDefinitions();
        assertNotNull("Expecting to find fields", fieldDefs);
        assertEquals("Expecting to find 11 fields", 11, fieldDefs.size());
        
        // create a Map of the field definitions
        // NOTE: we can safely do this as we know there are no duplicate field names and we're not
        //       concerned with ordering!
        Map<String, FieldDefinition> fieldDefMap = new HashMap<String, FieldDefinition>(fieldDefs.size());
        for (FieldDefinition fieldDef : fieldDefs)
        {
            fieldDefMap.put(fieldDef.getName(), fieldDef);
        }
        
        // find the fields
        PropertyFieldDefinition nameField = (PropertyFieldDefinition)fieldDefMap.get("cm:name");
        AssociationFieldDefinition containsField = (AssociationFieldDefinition)fieldDefMap.get("cm:contains");
        
        // check fields are present
        assertNotNull("Expecting to find the cm:name field", nameField);
        assertNotNull("Expecting to find the cm:contains field", containsField);
        
        // check the labels of all the fields
        assertEquals("Expecting cm:name label to be " + LABEL_NAME, 
                    LABEL_NAME, nameField.getLabel());
        assertEquals("Expecting cm:contains label to be " + LABEL_CONTAINS, 
                    LABEL_CONTAINS, containsField.getLabel());
        
        // check details of name field
        assertEquals("Expecting cm:name type to be d:text", "d:text", nameField.getDataType());
        assertTrue("Expecting cm:name to be mandatory", nameField.isMandatory());
        assertFalse("Expecting cm:name to be single valued", nameField.isRepeating());
        
        // check the details of the association field
        assertEquals("Expecting cm:contains endpoint type to be sys:base", "sys:base", 
                    containsField.getEndpointType());
        assertEquals("Expecting cm:contains endpoint direction to be TARGET", 
                    Direction.TARGET.toString(),
                    containsField.getEndpointDirection().toString());
        assertFalse("Expecting cm:contains endpoint to be optional", 
                    containsField.isEndpointMandatory());
        assertTrue("Expecting cm:contains endpoint to be 1 to many",
                    containsField.isEndpointMany());
        
        // check the form data
        FormData data = form.getFormData();
        assertNotNull("Expecting form data", data);
        Map<String, FormData.FieldData> fieldData = data.getData();
        assertNotNull("Expecting field data", fieldData);
        assertEquals(this.folderName, fieldData.get(nameField.getDataKeyName()).getValue());
        
        List<String> children = (List<String>)fieldData.get(containsField.getDataKeyName()).getValue();
        assertEquals("Expecting 3 children", 3, children.size());
        assertEquals(this.document.toString(), children.get(0));
        assertEquals(this.associatedDoc.toString(), children.get(1));
        assertEquals(this.childDoc.toString(), children.get(2));
    }
    
    public void testGetSelectedFieldsFolderForm() throws Exception
    {
        // TODO: attempt to get a form with fields that are not appropriate
        // for a folder type i.e. mimetype and encoding
        
    }
    
    @SuppressWarnings("unchecked")
    public void testSaveForm() throws Exception
    {
        // create FormData object containing the values to update
        FormData data = new FormData();
        
        // update the name
        String newName = "new-" + this.documentName;
        data.addData("prop_cm_name", newName);
        
        // update the title property
        String newTitle = "This is the new title property";
        data.addData("prop_cm_title", newTitle);
        
        // update the mimetype
        String newMimetype = MimetypeMap.MIMETYPE_HTML;
        data.addData("prop_mimetype", newMimetype);
        
        // update the originator
        String newOriginator = "jane@example.com";
        data.addData("prop_cm_originator", newOriginator);
        
        // update the adressees, add another
        String newAddressees = VALUE_ADDRESSEES1 + "," + VALUE_ADDRESSEES2 + "," + VALUE_ADDRESSEES3;
        data.addData("prop_cm_addressees", newAddressees);
        
        // set the date to null (using an empty string)
        data.addData("prop_cm_sentdate", "");
        
        // try and update non-existent properties (make sure there are no exceptions)
        data.addData("prop_cm_wrong", "This should not be persisted");
        data.addData("cm_wrong", "This should not be persisted");
        
        // persist the data
        this.formService.saveForm(new Item(NODE_FORM_ITEM_KIND, this.document.toString()), data);
        
        // retrieve the data directly from the node service to ensure its been changed
        Map<QName, Serializable> updatedProps = this.nodeService.getProperties(this.document);
        String updatedName = (String)updatedProps.get(ContentModel.PROP_NAME);
        String updatedTitle = (String)updatedProps.get(ContentModel.PROP_TITLE);
        String updatedOriginator = (String)updatedProps.get(ContentModel.PROP_ORIGINATOR);
        List updatedAddressees = (List)updatedProps.get(ContentModel.PROP_ADDRESSEES);
        String wrong = (String)updatedProps.get(QName.createQName("cm", "wrong", this.namespaceService));
        Date sentDate = (Date)updatedProps.get(ContentModel.PROP_SENTDATE);
        
        assertEquals(newName, updatedName);
        assertEquals(newTitle, updatedTitle);
        assertEquals(newOriginator, updatedOriginator);
        assertNull("Expecting sentdate to be null", sentDate);
        assertNull("Expecting my:wrong to be null", wrong);
        assertNotNull("Expected there to be addressees", updatedAddressees);
        assertTrue("Expected there to be 3 addressees", updatedAddressees.size() == 3);
        assertEquals(VALUE_ADDRESSEES1, updatedAddressees.get(0));
        assertEquals(VALUE_ADDRESSEES2, updatedAddressees.get(1));
        assertEquals(VALUE_ADDRESSEES3, updatedAddressees.get(2));
        
        // check mimetype was updated
        ContentData contentData = (ContentData)updatedProps.get(ContentModel.PROP_CONTENT);
        if (contentData != null)
        {
            String updatedMimetype = contentData.getMimetype();
            assertEquals(MimetypeMap.MIMETYPE_HTML, updatedMimetype);
        }
    }
    
    public void testNoForm() throws Exception
    {
        // test that a form can not be retrieved for a non-existent item
        try
        {
            this.formService.getForm(new Item("Invalid Kind", "Invalid Id"));
            fail("Expecting getForm for 'Invalid Kind/Item' to fail");
        }
        catch (Exception e)
        {
            // expected
        }
        
        String missingNode = this.document.toString().replace("-", "x");
        
        try
        {
            this.formService.getForm(new Item(NODE_FORM_ITEM_KIND, missingNode));
            fail("Expecting getForm for a missing node to fail");
        }
        catch (FormNotFoundException fnne)
        {
            // expected
        }
        
        // test that a form can not be saved for a non-existent item
        try
        {
            this.formService.saveForm(new Item("Invalid Kind", "Invalid Id"), new FormData());
            fail("Expecting saveForm for 'Invalid Kind/Item' to fail");
        }
        catch (Exception e)
        {
            // expected
        }
        
        try
        {
            this.formService.saveForm(new Item(NODE_FORM_ITEM_KIND, missingNode), new FormData());
            fail("Expecting saveForm for a missing node to fail");
        }
        catch (Exception e)
        {
            // expected
        }
    }
    
    public void testJavascriptAPI() throws Exception
    {
    	Map<String, Object> model = new HashMap<String, Object>();
    	model.put("testDoc", this.document.toString());
    	model.put("testDocName", this.documentName);
    	model.put("testAssociatedDoc", this.associatedDoc.toString());
    	model.put("folder", this.folder.toString());
    	model.put("folderName", this.folderName);
    	
        ScriptLocation location = new ClasspathScriptLocation("org/alfresco/repo/forms/script/test_formService.js");
        this.scriptService.executeScript(location, model);
    }
}
