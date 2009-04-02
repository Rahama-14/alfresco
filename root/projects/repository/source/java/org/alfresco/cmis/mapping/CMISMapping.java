/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.cmis.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.cmis.CMISContentStreamAllowedEnum;
import org.alfresco.cmis.CMISDataTypeEnum;
import org.alfresco.cmis.dictionary.CMISDictionaryModel;
import org.alfresco.cmis.dictionary.CMISPropertyId;
import org.alfresco.cmis.dictionary.CMISScope;
import org.alfresco.cmis.dictionary.CMISTypeId;
import org.alfresco.cmis.property.AbstractPropertyAccessor;
import org.alfresco.cmis.property.CheckinCommentPropertyAccessor;
import org.alfresco.cmis.property.ContentStreamLengthPropertyAccessor;
import org.alfresco.cmis.property.ContentStreamMimetypePropertyAccessor;
import org.alfresco.cmis.property.ContentStreamUriPropertyAccessor;
import org.alfresco.cmis.property.DirectPropertyAccessor;
import org.alfresco.cmis.property.FixedValuePropertyAccessor;
import org.alfresco.cmis.property.IsImmutablePropertyAccessor;
import org.alfresco.cmis.property.IsLatestMajorVersionPropertyAccessor;
import org.alfresco.cmis.property.IsLatestVersionPropertyAccessor;
import org.alfresco.cmis.property.IsMajorVersionPropertyAccessor;
import org.alfresco.cmis.property.IsVersionSeriesCheckedOutPropertyAccessor;
import org.alfresco.cmis.property.ObjectIdPropertyAccessor;
import org.alfresco.cmis.property.ObjectTypeIdPropertyAccessor;
import org.alfresco.cmis.property.ParentPropertyAccessor;
import org.alfresco.cmis.property.VersionSeriesCheckedOutByPropertyAccessor;
import org.alfresco.cmis.property.VersionSeriesCheckedOutIdPropertyAccessor;
import org.alfresco.cmis.property.VersionSeriesIdPropertyAccessor;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;


/**
 * CMIS <-> Alfresco mappings
 * 
 * @author andyh
 */
public class CMISMapping implements InitializingBean
{
    /**
     * The Alfresco CMIS model URI.
     */
    public static String CMIS_MODEL_URI = "http://www.alfresco.org/model/cmis/0.5";

    /**
     * The Alfresco CMIS Model name.
     */
    public static String CMIS_MODEL_NAME = "cmismodel";

    /**
     * The QName for the Alfresco CMIS Model.
     */
    public static QName CMIS_MODEL_QNAME = QName.createQName(CMIS_MODEL_URI, CMIS_MODEL_NAME);

    // CMIS Internal Types
    public static String OBJECT_OBJECT_TYPE = "Object";
    public static String FILESYSTEM_OBJECT_TYPE ="FileSystemObject";
    
    // CMIS Data Types
    public static QName CMIS_DATATYPE_ID = QName.createQName(CMIS_MODEL_URI, "id");
    public static QName CMIS_DATATYPE_URI = QName.createQName(CMIS_MODEL_URI, "uri");
    public static QName CMIS_DATATYPE_XML = QName.createQName(CMIS_MODEL_URI, "xml");
    public static QName CMIS_DATATYPE_HTML = QName.createQName(CMIS_MODEL_URI, "html");

    // CMIS Types
    public static QName OBJECT_QNAME = QName.createQName(CMIS_MODEL_URI, OBJECT_OBJECT_TYPE);
    public static QName FILESYSTEM_OBJECT_QNAME = QName.createQName(CMIS_MODEL_URI, FILESYSTEM_OBJECT_TYPE);
    public static QName DOCUMENT_QNAME = QName.createQName(CMIS_MODEL_URI, CMISDictionaryModel.DOCUMENT_OBJECT_TYPE);
    public static QName FOLDER_QNAME = QName.createQName(CMIS_MODEL_URI, CMISDictionaryModel.FOLDER_OBJECT_TYPE);
    public static QName RELATIONSHIP_QNAME = QName.createQName(CMIS_MODEL_URI, CMISDictionaryModel.RELATIONSHIP_OBJECT_TYPE);
    public static QName POLICY_QNAME = QName.createQName(CMIS_MODEL_URI, CMISDictionaryModel.POLICY_OBJECT_TYPE);

    // CMIS Internal Type Ids
    public static CMISTypeId OBJECT_TYPE_ID = new CMISTypeId(CMISScope.OBJECT, OBJECT_OBJECT_TYPE.toLowerCase(), OBJECT_QNAME);
    public static CMISTypeId FILESYSTEM_OBJECT_TYPE_ID = new CMISTypeId(CMISScope.OBJECT, FILESYSTEM_OBJECT_TYPE.toLowerCase(), FILESYSTEM_OBJECT_QNAME);

    // Properties
    public static QName PROP_OBJECT_ID_QNAME = QName.createQName(CMIS_MODEL_URI, CMISDictionaryModel.PROP_OBJECT_ID);

    // Service Dependencies
    private ServiceRegistry serviceRegistry;

    // Mappings
    private Map<QName, CMISTypeId> mapAlfrescoQNameToTypeId = new HashMap<QName, CMISTypeId>();
    private Map<QName, QName> mapCmisQNameToAlfrescoQName = new HashMap<QName, QName>();
    private Map<QName, QName> mapAlfrescoQNameToCmisQName = new HashMap<QName, QName>();
    private Map<QName, CMISDataTypeEnum> mapAlfrescoToCmisDataType = new HashMap<QName, CMISDataTypeEnum>();
    private Map<String, AbstractPropertyAccessor> propertyAccessors = new HashMap<String, AbstractPropertyAccessor>();
    
    
    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception
    {
        mapAlfrescoQNameToTypeId.put(OBJECT_QNAME, OBJECT_TYPE_ID);
        mapAlfrescoQNameToTypeId.put(FILESYSTEM_OBJECT_QNAME, FILESYSTEM_OBJECT_TYPE_ID);
        mapAlfrescoQNameToTypeId.put(DOCUMENT_QNAME, CMISDictionaryModel.DOCUMENT_TYPE_ID);
        mapAlfrescoQNameToTypeId.put(FOLDER_QNAME, CMISDictionaryModel.FOLDER_TYPE_ID);
        mapAlfrescoQNameToTypeId.put(RELATIONSHIP_QNAME, CMISDictionaryModel.RELATIONSHIP_TYPE_ID);
        mapAlfrescoQNameToTypeId.put(POLICY_QNAME, CMISDictionaryModel.POLICY_TYPE_ID);

        mapAlfrescoQNameToCmisQName.put(ContentModel.TYPE_CONTENT, DOCUMENT_QNAME);
        mapAlfrescoQNameToCmisQName.put(ContentModel.TYPE_FOLDER, FOLDER_QNAME);

        mapCmisQNameToAlfrescoQName.put(DOCUMENT_QNAME, ContentModel.TYPE_CONTENT);
        mapCmisQNameToAlfrescoQName.put(FOLDER_QNAME, ContentModel.TYPE_FOLDER);
        mapCmisQNameToAlfrescoQName.put(RELATIONSHIP_QNAME, null);
        mapCmisQNameToAlfrescoQName.put(POLICY_QNAME, null);

        mapAlfrescoToCmisDataType.put(DataTypeDefinition.ANY, null);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.ASSOC_REF, null);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.BOOLEAN, CMISDataTypeEnum.BOOLEAN);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.CATEGORY, CMISDataTypeEnum.ID);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.CHILD_ASSOC_REF, null);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.CONTENT, null);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.DATE, CMISDataTypeEnum.DATETIME);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.DATETIME, CMISDataTypeEnum.DATETIME);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.DOUBLE, CMISDataTypeEnum.DECIMAL);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.FLOAT, CMISDataTypeEnum.DECIMAL);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.INT, CMISDataTypeEnum.INTEGER);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.LOCALE, null);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.LONG, CMISDataTypeEnum.INTEGER);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.MLTEXT, CMISDataTypeEnum.STRING);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.NODE_REF, CMISDataTypeEnum.ID);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.PATH, null);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.QNAME, null);
        mapAlfrescoToCmisDataType.put(DataTypeDefinition.TEXT, CMISDataTypeEnum.STRING);
        mapAlfrescoToCmisDataType.put(CMIS_DATATYPE_ID, CMISDataTypeEnum.ID);
        mapAlfrescoToCmisDataType.put(CMIS_DATATYPE_URI, CMISDataTypeEnum.URI);
        mapAlfrescoToCmisDataType.put(CMIS_DATATYPE_XML, CMISDataTypeEnum.XML);
        mapAlfrescoToCmisDataType.put(CMIS_DATATYPE_HTML, CMISDataTypeEnum.HTML);

        registerPropertyAccessor(new ObjectIdPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new FixedValuePropertyAccessor(serviceRegistry, CMISDictionaryModel.PROP_URI, null));
        registerPropertyAccessor(new ObjectTypeIdPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new DirectPropertyAccessor(serviceRegistry, CMISDictionaryModel.PROP_CREATED_BY, ContentModel.PROP_CREATOR));
        registerPropertyAccessor(new DirectPropertyAccessor(serviceRegistry, CMISDictionaryModel.PROP_CREATION_DATE, ContentModel.PROP_CREATED));
        registerPropertyAccessor(new DirectPropertyAccessor(serviceRegistry, CMISDictionaryModel.PROP_LAST_MODIFIED_BY, ContentModel.PROP_MODIFIER));
        registerPropertyAccessor(new DirectPropertyAccessor(serviceRegistry, CMISDictionaryModel.PROP_LAST_MODIFICATION_DATE, ContentModel.PROP_MODIFIED));
        registerPropertyAccessor(new FixedValuePropertyAccessor(serviceRegistry, CMISDictionaryModel.PROP_CHANGE_TOKEN, null));
        registerPropertyAccessor(new DirectPropertyAccessor(serviceRegistry, CMISDictionaryModel.PROP_NAME, ContentModel.PROP_NAME));
        registerPropertyAccessor(new IsImmutablePropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new IsLatestVersionPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new IsMajorVersionPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new IsLatestMajorVersionPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new DirectPropertyAccessor(serviceRegistry, CMISDictionaryModel.PROP_VERSION_LABEL, ContentModel.PROP_VERSION_LABEL));
        registerPropertyAccessor(new VersionSeriesIdPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new IsVersionSeriesCheckedOutPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new VersionSeriesCheckedOutByPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new VersionSeriesCheckedOutIdPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new CheckinCommentPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new FixedValuePropertyAccessor(serviceRegistry, CMISDictionaryModel.PROP_CONTENT_STREAM_ALLOWED, CMISContentStreamAllowedEnum.ALLOWED.toString()));
        registerPropertyAccessor(new ContentStreamLengthPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new ContentStreamMimetypePropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new DirectPropertyAccessor(serviceRegistry, CMISDictionaryModel.PROP_CONTENT_STREAM_FILENAME, ContentModel.PROP_NAME));
        registerPropertyAccessor(new ContentStreamUriPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new ParentPropertyAccessor(serviceRegistry));
        registerPropertyAccessor(new FixedValuePropertyAccessor(serviceRegistry, CMISDictionaryModel.PROP_ALLOWED_CHILD_OBJECT_TYPE_IDS, null));
    }


    /**
     * @param serviceRegistry
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * @return namespaceService
     */
    /*package*/ NamespaceService getNamespaceService()
    {
        return serviceRegistry.getNamespaceService();
    }

    /**
     * Gets the CMIS Type Id given the serialized type Id
     * 
     * @param typeId  type id in the form of <ROOT_TYPE_ID>/<PREFIX>_<LOCALNAME>
     * @return
     */
    public CMISTypeId getCmisTypeId(String typeId)
    {
        // Is it a CMIS root object type id?
        if (typeId.equalsIgnoreCase(CMISDictionaryModel.DOCUMENT_TYPE_ID.getId()))
        {
            return CMISDictionaryModel.DOCUMENT_TYPE_ID;
        }
        else if (typeId.equalsIgnoreCase(CMISDictionaryModel.FOLDER_TYPE_ID.getId()))
        {
            return CMISDictionaryModel.FOLDER_TYPE_ID;
        }
        else if (typeId.equalsIgnoreCase(CMISDictionaryModel.RELATIONSHIP_TYPE_ID.getId()))
        {
            return CMISDictionaryModel.RELATIONSHIP_TYPE_ID;
        }
        else if (typeId.equalsIgnoreCase(CMISDictionaryModel.POLICY_TYPE_ID.getId()))
        {
            return CMISDictionaryModel.POLICY_TYPE_ID;
        }
        else if (typeId.equalsIgnoreCase(OBJECT_TYPE_ID.getId()))
        {
            return OBJECT_TYPE_ID;
        }
        else if (typeId.equalsIgnoreCase(FILESYSTEM_OBJECT_TYPE_ID.getId()))
        {
            return FILESYSTEM_OBJECT_TYPE_ID;
        }

        // Is it an Alfresco type id?
        if (typeId.length() < 4 || typeId.charAt(1) != '/')
        {
            throw new AlfrescoRuntimeException("Malformed type id '" + typeId + "'");
        }

        // Alfresco type id
        CMISScope scope = CMISScope.toScope(typeId.charAt(0));
        if (scope == null)
        {
            throw new AlfrescoRuntimeException("Malformed type id '" + typeId + "'; discriminator " + typeId.charAt(0) + " unknown");
        }
        QName typeQName = QName.resolveToQName(serviceRegistry.getNamespaceService(), typeId.substring(2).replace('_', ':'));

        // Construct CMIS Type Id
        return new CMISTypeId(scope, typeId, typeQName);
    }

    /**
     * Gets the CMIS Type Id given the Alfresco QName for the type in any Alfresco model
     * 
     * @param typeQName
     * @return
     */
    public CMISTypeId getCmisTypeId(CMISScope scope, QName typeQName)
    {
        CMISTypeId typeId = mapAlfrescoQNameToTypeId.get(typeQName);
        if (typeId == null)
        {
            StringBuilder builder = new StringBuilder(128);
            builder.append(scope.discriminator());
            builder.append("/");
            builder.append(buildPrefixEncodedString(typeQName, false));
            return new CMISTypeId(scope, builder.toString(), typeQName);
        }
        else
        {
            return typeId;
        }
    }

    public CMISTypeId getCmisTypeId(QName classQName)
    {
        if (classQName.equals(ContentModel.TYPE_CONTENT))
        {
            return getCmisTypeId(CMISScope.DOCUMENT, classQName);
        }
        if (classQName.equals(ContentModel.TYPE_FOLDER))
        {
            return getCmisTypeId(CMISScope.FOLDER, classQName);
        }
        if (classQName.equals(CMISMapping.RELATIONSHIP_QNAME))
        {
            return getCmisTypeId(CMISScope.RELATIONSHIP, classQName);
        }
        if (classQName.equals(CMISMapping.POLICY_QNAME))
        {
            return getCmisTypeId(CMISScope.POLICY, classQName);
        }
        if (classQName.equals(CMISMapping.OBJECT_QNAME))
        {
            return getCmisTypeId(CMISScope.OBJECT, classQName);
        }
        if (classQName.equals(CMISMapping.FILESYSTEM_OBJECT_QNAME))
        {
            return getCmisTypeId(CMISScope.OBJECT, classQName);
        }
        if (isValidCmisDocument(classQName))
        {
            return getCmisTypeId(CMISScope.DOCUMENT, classQName);
        }
        if (isValidCmisFolder(classQName))
        {
            return getCmisTypeId(CMISScope.FOLDER, classQName);
        }
        if (isValidCmisRelationship(classQName))
        {
            return getCmisTypeId(CMISScope.RELATIONSHIP, classQName);
        }
        if (isValidCmisPolicy(classQName))
        {
            return getCmisTypeId(CMISScope.POLICY, classQName);
        }

        return null;
    }

    public String buildPrefixEncodedString(QName qname, boolean upperCase)
    {
        StringBuilder builder = new StringBuilder(128);

        if (!qname.getNamespaceURI().equals(CMIS_MODEL_URI))
        {
            Collection<String> prefixes = serviceRegistry.getNamespaceService().getPrefixes(qname.getNamespaceURI());
            if (prefixes.size() == 0)
            {
                throw new NamespaceException("A namespace prefix is not registered for uri " + qname.getNamespaceURI());
            }
            String resolvedPrefix = prefixes.iterator().next();

            builder.append(upperCase ? resolvedPrefix.toUpperCase() : resolvedPrefix);
            builder.append("_");
        }

        builder.append(upperCase ? qname.getLocalName().toUpperCase() : qname.getLocalName());
        return builder.toString();
    }

    /**
     * Is this a valid cmis document or folder type (not a relationship)
     * 
     * @param dictionaryService
     * @param typeQName
     * @return
     */
    public boolean isValidCmisDocumentOrFolder(QName typeQName)
    {
        return isValidCmisFolder(typeQName) || isValidCmisDocument(typeQName);
    }

    /**
     * Is this a valid CMIS folder type?
     * 
     * @param dictionaryService
     * @param typeQName
     * @return
     */
    public boolean isValidCmisFolder(QName typeQName)
    {
        if (typeQName == null)
        {
            return false;
        }
        if (typeQName.equals(FOLDER_QNAME))
        {
            return true;
        }

        if (serviceRegistry.getDictionaryService().isSubClass(typeQName, ContentModel.TYPE_FOLDER))
        {
            if (typeQName.equals(ContentModel.TYPE_FOLDER))
            {
                return false;
            }
            else
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Is this a valid CMIS document type?
     * 
     * @param dictionaryService
     * @param typeQName
     * @return
     */
    public boolean isValidCmisDocument(QName typeQName)
    {
        if (typeQName == null)
        {
            return false;
        }
        if (typeQName.equals(DOCUMENT_QNAME))
        {
            return true;
        }

        if (serviceRegistry.getDictionaryService().isSubClass(typeQName, ContentModel.TYPE_CONTENT))
        {
            if (typeQName.equals(ContentModel.TYPE_CONTENT))
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Is this a valid CMIS policy type?
     * 
     * @param dictionaryService
     * @param typeQName
     * @return
     */
    public boolean isValidCmisPolicy(QName typeQName)
    {
        if (typeQName == null)
        {
            return false;
        }
        if (typeQName.equals(POLICY_QNAME))
        {
            return true;
        }

        AspectDefinition aspectDef = serviceRegistry.getDictionaryService().getAspect(typeQName);
        if (aspectDef == null)
        {
            return false;
        }
        
        if (aspectDef.getName().equals(ContentModel.ASPECT_VERSIONABLE) ||
            aspectDef.getName().equals(ContentModel.ASPECT_AUDITABLE) ||
            aspectDef.getName().equals(ContentModel.ASPECT_REFERENCEABLE))
        {
            return false;
        }
        return true;
    }
    
    /**
     * Is an association valid in CMIS? It must be a non-child relationship and the source and target must both be valid
     * CMIS types.
     * 
     * @param dictionaryService
     * @param associationQName
     * @return
     */
    public boolean isValidCmisRelationship(QName associationQName)
    {
        if (associationQName == null)
        {
            return false;
        }
        if (associationQName.equals(RELATIONSHIP_QNAME))
        {
            return true;
        }
        AssociationDefinition associationDefinition = serviceRegistry.getDictionaryService().getAssociation(associationQName);
        if (associationDefinition == null)
        {
            return false;
        }
        if (associationDefinition.isChild())
        {
            return false;
        }
        if (!isValidCmisDocumentOrFolder(getCmisType(associationDefinition.getSourceClass().getName())))
        {
            return false;
        }
        if (!isValidCmisDocumentOrFolder(getCmisType(associationDefinition.getTargetClass().getName())))
        {
            return false;
        }
        return true;
    }

    /**
     * Given an Alfresco model type map it to the appropriate type. Maps cm:folder and cm:content to the CMIS
     * definitions
     * 
     * @param typeQName
     * @return
     */
    public QName getCmisType(QName typeQName)
    {
        QName mapped = mapAlfrescoQNameToCmisQName.get(typeQName);
        if (mapped != null)
        {
            return mapped;
        }
        return typeQName;
    }

    /**
     * Is Alfresco Type mapped to an alternative CMIS Type?
     * 
     * @param typeQName
     * @return
     */
    public boolean isRemappedType(QName typeQName)
    {
        return mapAlfrescoQNameToCmisQName.containsKey(typeQName);
    }
    
    /**
     * Given a CMIS model type map it to the appropriate Alfresco type.
     * 
     * @param cmisTypeQName
     * @return
     */
    public QName getAlfrescoType(QName cmisTypeQName)
    {
        QName mapped = mapCmisQNameToAlfrescoQName.get(cmisTypeQName);
        if (mapped != null)
        {
            return mapped;
        }
        return cmisTypeQName;
    }

    /**
     * Get the CMIS property name from the property QName.
     * 
     * @param namespaceService
     * @param propertyQName
     * @return
     */
    public String getCmisPropertyName(QName propertyQName)
    {
        return buildPrefixEncodedString(propertyQName, false);
    }

    /**
     * Get the CMIS property type for a property
     * 
     * @param dictionaryService
     * @param propertyQName
     * @return
     */
    public CMISDataTypeEnum getDataType(DataTypeDefinition datatype)
    {
        return getDataType(datatype.getName());
    }
    
    public CMISDataTypeEnum getDataType(QName dataType)
    {
        return mapAlfrescoToCmisDataType.get(dataType);
    }

    /**
     * Lookup a CMIS property name and get the Alfresco property QName
     * 
     * @param dictionaryService
     * @param namespaceService
     * @param cmisPropertyName
     * @return
     */
    public QName getPropertyQName(String cmisPropertyName)
    {
        // Try the cmis model first - it it matches we are done
        QName cmisPropertyQName = QName.createQName(CMIS_MODEL_URI, cmisPropertyName);
        if (serviceRegistry.getDictionaryService().getProperty(cmisPropertyQName) != null)
        {
            return cmisPropertyQName;
        }

        // Find prefix and property name - in upper case

        int split = cmisPropertyName.indexOf('_');

        // CMIS case insensitive hunt - no prefix
        if (split == -1)
        {
            for (QName qname : serviceRegistry.getDictionaryService().getAllProperties(null))
            {
                if (qname.getNamespaceURI().equals(CMIS_MODEL_URI))
                {
                    if (qname.getLocalName().equalsIgnoreCase(cmisPropertyName))
                    {
                        return qname;
                    }
                }
            }
            return null;
        }

        String prefix = cmisPropertyName.substring(0, split);
        String localName = cmisPropertyName.substring(split + 1);

        // Try lower case version first.

        QName propertyQName = QName.createQName(prefix.toLowerCase(), localName.toLowerCase(), serviceRegistry.getNamespaceService());
        if (serviceRegistry.getDictionaryService().getProperty(propertyQName) != null)
        {
            return propertyQName;
        }

        // Full case insensitive hunt

        for (String test : serviceRegistry.getNamespaceService().getPrefixes())
        {
            if (test.equalsIgnoreCase(prefix))
            {
                prefix = test;
                break;
            }
        }
        String uri = serviceRegistry.getNamespaceService().getNamespaceURI(prefix);

        for (QName qname : serviceRegistry.getDictionaryService().getAllProperties(null))
        {
            if (qname.getNamespaceURI().equals(uri))
            {
                if (qname.getLocalName().equalsIgnoreCase(localName))
                {
                    return qname;
                }
            }
        }

        return null;
    }

    /**
     * @param namespaceService
     * @param propertyQName
     * @return
     */
    public String getCmisPropertyId(QName propertyQName)
    {
        if (propertyQName.getNamespaceURI().equals(CMIS_MODEL_URI))
        {
            return propertyQName.getLocalName();
        }
        else
        {
            return propertyQName.toString();
        }
    }

    /**
     * Get a Property Accessor
     * 
     * @param propertyId
     * @return
     */
    public AbstractPropertyAccessor getPropertyAccessor(CMISPropertyId propertyId)
    {
        AbstractPropertyAccessor propertyAccessor = propertyAccessors.get(propertyId.getName());
        if (propertyAccessor == null)
        {
            QName propertyQName = propertyId.getQName();
            if (propertyQName == null)
            {
                throw new AlfrescoRuntimeException("Can't get property accessor for property id " + propertyId.getName() + " due to unknown property QName");
            }
            propertyAccessor = new DirectPropertyAccessor(serviceRegistry, propertyId.getName(), propertyQName);
        }
        return propertyAccessor;
    }
 
    /**
     * Register pre-defined Property Accessor
     * 
     * @param propertyAccessor
     */
    private void registerPropertyAccessor(AbstractPropertyAccessor propertyAccessor)
    {
        propertyAccessors.put(propertyAccessor.getName(), propertyAccessor);
    }
    
}
