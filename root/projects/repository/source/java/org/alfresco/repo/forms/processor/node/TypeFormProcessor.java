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

package org.alfresco.repo.forms.processor.node;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.forms.Form;
import org.alfresco.repo.forms.FormData;
import org.alfresco.repo.forms.FormException;
import org.alfresco.repo.forms.FormNotFoundException;
import org.alfresco.repo.forms.Item;
import org.alfresco.repo.forms.FormData.FieldData;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.InvalidQNameException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * FormProcessor implementation that can generate and persist Form objects for
 * types in the Alfresco content model.
 * 
 * @author Gavin Cornwell
 */
public class TypeFormProcessor extends ContentModelFormProcessor<TypeDefinition, NodeRef>
{
    /** Logger */
    private static Log logger = LogFactory.getLog(TypeFormProcessor.class);

    protected static final String NAME_PROP_DATA = PROP + DATA_KEY_SEPARATOR + "cm"
                + DATA_KEY_SEPARATOR + "name";

    public static final String DESTINATION = "alf_destination";

    /*
     * @see
     * org.alfresco.repo.forms.processor.node.ContentModelFormProcessor#getLogger
     * ()
     */
    @Override
    protected Log getLogger()
    {
        return logger;
    }

    /*
     * @see
     * org.alfresco.repo.forms.processor.node.NodeFormProcessor#getTypedItem
     * (org.alfresco.repo.forms.Item)
     */
    @Override
    protected TypeDefinition getTypedItem(Item item)
    {
        TypeDefinition typeDef = null;

        try
        {
            // convert the prefix type into full QName representation
            // the type name may be provided in the prefix form i.e.
            // prefix:type, the : may be replaced with _ if the item id
            // was passed on a URL or the full qname may be provided.
            QName type = null;
            String itemId = item.getId();
            if (itemId.startsWith("{"))
            {
                // item id looks like a full qname
                type = QName.createQName(itemId);
            }
            else if (itemId.indexOf("_") != -1)
            {
                // if item id contains _ change the first occurrence to :
                // as it's more than likely been converted for URL use
                int idx = itemId.indexOf("_");
                String parsedItemId = itemId.substring(0, idx) + ":" + itemId.substring(idx + 1);
                type = QName.createQName(parsedItemId, this.namespaceService);
            }
            else
            {
                // try and create the QName using the item id as is
                type = QName.createQName(itemId, this.namespaceService);
            }

            // retrieve the type from the dictionary
            typeDef = this.dictionaryService.getType(type);

            if (typeDef == null) { throw new FormNotFoundException(item,
                        new IllegalArgumentException("Type does not exist: " + item.getId())); }
        }
        catch (InvalidQNameException iqne)
        {
            throw new FormNotFoundException(item, iqne);
        }

        // return the type definition object for the requested type
        return typeDef;
    }

    /*
     * @see
     * org.alfresco.repo.forms.processor.FilteredFormProcessor#internalGenerate
     * (java.lang.Object, java.util.List, java.util.List,
     * org.alfresco.repo.forms.Form, java.util.Map)
     */
    @Override
    protected void internalGenerate(TypeDefinition item, List<String> fields,
                List<String> forcedFields, Form form, Map<String, Object> context)
    {
        if (logger.isDebugEnabled()) logger.debug("Generating form for item: " + item);

        // generate the form for the node
        generateType(item, fields, forcedFields, form);
        if (logger.isDebugEnabled()) logger.debug("Generating form: " + form);
    }

    /**
     * Sets up the Form object for the given NodeRef
     * 
     * @param nodeRef The NodeRef to generate a Form for
     * @param fields Restricted list of fields to include
     * @param forcedFields List of fields to forcibly include
     * @param form The Form instance to populate
     */
    protected void generateType(TypeDefinition typeDef, List<String> fields,
                List<String> forcedFields, Form form)
    {
        // set the type and URL of the item
        form.getItem().setType(typeDef.getName().toPrefixString(this.namespaceService));
        form.getItem().setUrl(
                    "/api/classes/"
                                + typeDef.getName().toPrefixString(this.namespaceService).replace(
                                            ":", "_"));

        if (fields != null && fields.size() > 0)
        {
            generateSelectedFields(null, typeDef, fields, forcedFields, form);
        }
        else
        {
            // setup field definitions and data
            generateAllPropertyFields(typeDef, form);
            generateAllAssociationFields(typeDef, form);

            // TODO: generate transient properties for content types?
        }
    }

    /**
     * Sets up the field definitions for all the type's properties.
     * 
     * @param typeDef The type being setup
     * @param form The Form instance to populate
     */
    protected void generateAllPropertyFields(TypeDefinition typeDef, Form form)
    {
        // iterate round the property defintions and setup field definition
        Map<QName, PropertyDefinition> propDefs = typeDef.getProperties();
        for (PropertyDefinition propDef : propDefs.values())
        {
            generatePropertyField(propDef, form, this.namespaceService);
        }

        // get all default aspects for the type and iterate round their
        // property definitions too
        List<AspectDefinition> aspects = typeDef.getDefaultAspects(true);
        for (AspectDefinition aspect : aspects)
        {
            propDefs = aspect.getProperties();
            for (PropertyDefinition propDef : propDefs.values())
            {
                generatePropertyField(propDef, form, this.namespaceService);
            }
        }
    }

    /**
     * Sets up the field definitions for all the type's associations.
     * 
     * @param typeDef The type being setup
     * @param form The Form instance to populate
     */
    protected void generateAllAssociationFields(TypeDefinition typeDef, Form form)
    {
        // iterate round the association defintions and setup field definition
        Map<QName, AssociationDefinition> assocDefs = typeDef.getAssociations();
        for (AssociationDefinition assocDef : assocDefs.values())
        {
            generateAssociationField(assocDef, form, this.namespaceService);
        }

        // get all default aspects for the type and iterate round their
        // association definitions too
        List<AspectDefinition> aspects = typeDef.getDefaultAspects(true);
        for (AspectDefinition aspect : aspects)
        {
            assocDefs = aspect.getAssociations();
            for (AssociationDefinition assocDef : assocDefs.values())
            {
                generateAssociationField(assocDef, form, this.namespaceService);
            }
        }
    }

    /*
     * @see
     * org.alfresco.repo.forms.processor.node.NodeFormProcessor#internalPersist
     * (java.lang.Object, org.alfresco.repo.forms.FormData)
     */
    @Override
    protected NodeRef internalPersist(TypeDefinition item, FormData data)
    {
        if (logger.isDebugEnabled()) logger.debug("Persisting form for: " + item);

        // create a new instance of the type
        NodeRef nodeRef = createNode(item, data);

        // persist the form data
        persistNode(nodeRef, data);

        // return the newly created node
        return nodeRef;
    }

    /**
     * Creates a new instance of the given type.
     * <p>
     * If the form data has the name property present it is used as the name of
     * the node.
     * </p>
     * <p>
     * The new node is placed in the location defined by the "destination" data
     * item in the form data (this will usually be a hidden field), this will
     * also be the NodeRef representation of the parent for the new node.
     * </p>
     * 
     * @param typeDef The type defintion of the type to create
     * @param data The form data
     * @return NodeRef representing the newly created node
     */
    protected NodeRef createNode(TypeDefinition typeDef, FormData data)
    {
        NodeRef nodeRef = null;

        if (data != null)
        {
            // firstly, ensure we have a destination to create the node in
            NodeRef parentRef = null;
            FieldData destination = data.getFieldData(DESTINATION);
            if (destination == null) { throw new FormException("Failed to persist form for '"
                        + typeDef.getName().toPrefixString(this.namespaceService) + "' as '"
                        + DESTINATION + "' data was not provided."); }

            // create the parent NodeRef
            parentRef = new NodeRef((String) destination.getValue());

            // remove the destination data to avoid warning during persistence,
            // this can
            // always be retrieved by looking up the created node's parent
            data.removeFieldData(DESTINATION);

            // TODO: determine what association to use when creating the node in
            // the destination,
            // defaults to ContentModel.ASSOC_CONTAINS

            // if a name property is present in the form data use it as the node
            // name,
            // otherwise generate a guid
            String nodeName = null;
            FieldData nameData = data.getFieldData(NAME_PROP_DATA);
            if (nameData != null)
            {
                nodeName = (String) nameData.getValue();

                // remove the name data otherwise 'rename' gets called in
                // persistNode
                data.removeFieldData(NAME_PROP_DATA);
            }
            if (nodeName == null || nodeName.length() == 0)
            {
                nodeName = GUID.generate();
            }

            // create the node
            Map<QName, Serializable> nodeProps = new HashMap<QName, Serializable>(1);
            nodeProps.put(ContentModel.PROP_NAME, nodeName);
            nodeRef = this.nodeService.createNode(
                        parentRef,
                        ContentModel.ASSOC_CONTAINS,
                        QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName
                                    .createValidLocalName(nodeName)), typeDef.getName(), nodeProps)
                        .getChildRef();
        }

        return nodeRef;
    }
}
