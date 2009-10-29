/**
 * CmisTypeDefinitionType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */
package org.alfresco.repo.cmis.ws;

public class CmisTypeDefinitionType implements java.io.Serializable, org.apache.axis.encoding.AnyContentType
{
    private static final long serialVersionUID = -6099012778586600211L;

    private java.lang.String id;
    private java.lang.String localName;
    private org.apache.axis.types.URI localNamespace;
    private java.lang.String displayName;
    private java.lang.String queryName;
    private java.lang.String description;
    private org.alfresco.repo.cmis.ws.EnumBaseObjectTypeIds baseTypeId;
    private java.lang.String parentId;
    private boolean creatable;
    private boolean fileable;
    private boolean queryable;
    private boolean fulltextindexed;
    private boolean includedInSupertypeQuery;
    private boolean controllablePolicy;
    private boolean controllableACL;
    private org.alfresco.repo.cmis.ws.CmisPropertyBooleanDefinitionType[] propertyBooleanDefinition;
    private org.alfresco.repo.cmis.ws.CmisPropertyDateTimeDefinitionType[] propertyDateTimeDefinition;
    private org.alfresco.repo.cmis.ws.CmisPropertyDecimalDefinitionType[] propertyDecimalDefinition;
    private org.alfresco.repo.cmis.ws.CmisPropertyIdDefinitionType[] propertyIdDefinition;
    private org.alfresco.repo.cmis.ws.CmisPropertyIntegerDefinitionType[] propertyIntegerDefinition;
    private org.alfresco.repo.cmis.ws.CmisPropertyHtmlDefinitionType[] propertyHtmlDefinition;
    private org.alfresco.repo.cmis.ws.CmisPropertyXhtmlDefinitionType[] propertyXhtmlDefinition;
    private org.alfresco.repo.cmis.ws.CmisPropertyStringDefinitionType[] propertyStringDefinition;
    private org.alfresco.repo.cmis.ws.CmisPropertyXmlDefinitionType[] propertyXmlDefinition;
    private org.alfresco.repo.cmis.ws.CmisPropertyUriDefinitionType[] propertyUriDefinition;
    private org.apache.axis.message.MessageElement[] _any;

    public CmisTypeDefinitionType()
    {
    }

    public CmisTypeDefinitionType(java.lang.String id, java.lang.String localName, org.apache.axis.types.URI localNamespace, java.lang.String displayName,
            java.lang.String queryName, java.lang.String description, org.alfresco.repo.cmis.ws.EnumBaseObjectTypeIds baseTypeId, java.lang.String parentId, boolean creatable,
            boolean fileable, boolean queryable, boolean fulltextindexed, boolean includedInSupertypeQuery, boolean controllablePolicy, boolean controllableACL,
            org.alfresco.repo.cmis.ws.CmisPropertyBooleanDefinitionType[] propertyBooleanDefinition,
            org.alfresco.repo.cmis.ws.CmisPropertyDateTimeDefinitionType[] propertyDateTimeDefinition,
            org.alfresco.repo.cmis.ws.CmisPropertyDecimalDefinitionType[] propertyDecimalDefinition, org.alfresco.repo.cmis.ws.CmisPropertyIdDefinitionType[] propertyIdDefinition,
            org.alfresco.repo.cmis.ws.CmisPropertyIntegerDefinitionType[] propertyIntegerDefinition,
            org.alfresco.repo.cmis.ws.CmisPropertyHtmlDefinitionType[] propertyHtmlDefinition, org.alfresco.repo.cmis.ws.CmisPropertyXhtmlDefinitionType[] propertyXhtmlDefinition,
            org.alfresco.repo.cmis.ws.CmisPropertyStringDefinitionType[] propertyStringDefinition, org.alfresco.repo.cmis.ws.CmisPropertyXmlDefinitionType[] propertyXmlDefinition,
            org.alfresco.repo.cmis.ws.CmisPropertyUriDefinitionType[] propertyUriDefinition, org.apache.axis.message.MessageElement[] _any)
    {
        this.id = id;
        this.localName = localName;
        this.localNamespace = localNamespace;
        this.displayName = displayName;
        this.queryName = queryName;
        this.description = description;
        this.baseTypeId = baseTypeId;
        this.parentId = parentId;
        this.creatable = creatable;
        this.fileable = fileable;
        this.queryable = queryable;
        this.fulltextindexed = fulltextindexed;
        this.includedInSupertypeQuery = includedInSupertypeQuery;
        this.controllablePolicy = controllablePolicy;
        this.controllableACL = controllableACL;
        this.propertyBooleanDefinition = propertyBooleanDefinition;
        this.propertyDateTimeDefinition = propertyDateTimeDefinition;
        this.propertyDecimalDefinition = propertyDecimalDefinition;
        this.propertyIdDefinition = propertyIdDefinition;
        this.propertyIntegerDefinition = propertyIntegerDefinition;
        this.propertyHtmlDefinition = propertyHtmlDefinition;
        this.propertyXhtmlDefinition = propertyXhtmlDefinition;
        this.propertyStringDefinition = propertyStringDefinition;
        this.propertyXmlDefinition = propertyXmlDefinition;
        this.propertyUriDefinition = propertyUriDefinition;
        this._any = _any;
    }

    /**
     * Gets the id value for this CmisTypeDefinitionType.
     * 
     * @return id
     */
    public java.lang.String getId()
    {
        return id;
    }

    /**
     * Sets the id value for this CmisTypeDefinitionType.
     * 
     * @param id
     */
    public void setId(java.lang.String id)
    {
        this.id = id;
    }

    /**
     * Gets the localName value for this CmisTypeDefinitionType.
     * 
     * @return localName
     */
    public java.lang.String getLocalName()
    {
        return localName;
    }

    /**
     * Sets the localName value for this CmisTypeDefinitionType.
     * 
     * @param localName
     */
    public void setLocalName(java.lang.String localName)
    {
        this.localName = localName;
    }

    /**
     * Gets the localNamespace value for this CmisTypeDefinitionType.
     * 
     * @return localNamespace
     */
    public org.apache.axis.types.URI getLocalNamespace()
    {
        return localNamespace;
    }

    /**
     * Sets the localNamespace value for this CmisTypeDefinitionType.
     * 
     * @param localNamespace
     */
    public void setLocalNamespace(org.apache.axis.types.URI localNamespace)
    {
        this.localNamespace = localNamespace;
    }

    /**
     * Gets the displayName value for this CmisTypeDefinitionType.
     * 
     * @return displayName
     */
    public java.lang.String getDisplayName()
    {
        return displayName;
    }

    /**
     * Sets the displayName value for this CmisTypeDefinitionType.
     * 
     * @param displayName
     */
    public void setDisplayName(java.lang.String displayName)
    {
        this.displayName = displayName;
    }

    /**
     * Gets the queryName value for this CmisTypeDefinitionType.
     * 
     * @return queryName
     */
    public java.lang.String getQueryName()
    {
        return queryName;
    }

    /**
     * Sets the queryName value for this CmisTypeDefinitionType.
     * 
     * @param queryName
     */
    public void setQueryName(java.lang.String queryName)
    {
        this.queryName = queryName;
    }

    /**
     * Gets the description value for this CmisTypeDefinitionType.
     * 
     * @return description
     */
    public java.lang.String getDescription()
    {
        return description;
    }

    /**
     * Sets the description value for this CmisTypeDefinitionType.
     * 
     * @param description
     */
    public void setDescription(java.lang.String description)
    {
        this.description = description;
    }

    /**
     * Gets the baseTypeId value for this CmisTypeDefinitionType.
     * 
     * @return baseTypeId
     */
    public org.alfresco.repo.cmis.ws.EnumBaseObjectTypeIds getBaseTypeId()
    {
        return baseTypeId;
    }

    /**
     * Sets the baseTypeId value for this CmisTypeDefinitionType.
     * 
     * @param baseTypeId
     */
    public void setBaseTypeId(org.alfresco.repo.cmis.ws.EnumBaseObjectTypeIds baseTypeId)
    {
        this.baseTypeId = baseTypeId;
    }

    /**
     * Gets the parentId value for this CmisTypeDefinitionType.
     * 
     * @return parentId
     */
    public java.lang.String getParentId()
    {
        return parentId;
    }

    /**
     * Sets the parentId value for this CmisTypeDefinitionType.
     * 
     * @param parentId
     */
    public void setParentId(java.lang.String parentId)
    {
        this.parentId = parentId;
    }

    /**
     * Gets the creatable value for this CmisTypeDefinitionType.
     * 
     * @return creatable
     */
    public boolean isCreatable()
    {
        return creatable;
    }

    /**
     * Sets the creatable value for this CmisTypeDefinitionType.
     * 
     * @param creatable
     */
    public void setCreatable(boolean creatable)
    {
        this.creatable = creatable;
    }

    /**
     * Gets the fileable value for this CmisTypeDefinitionType.
     * 
     * @return fileable
     */
    public boolean isFileable()
    {
        return fileable;
    }

    /**
     * Sets the fileable value for this CmisTypeDefinitionType.
     * 
     * @param fileable
     */
    public void setFileable(boolean fileable)
    {
        this.fileable = fileable;
    }

    /**
     * Gets the queryable value for this CmisTypeDefinitionType.
     * 
     * @return queryable
     */
    public boolean isQueryable()
    {
        return queryable;
    }

    /**
     * Sets the queryable value for this CmisTypeDefinitionType.
     * 
     * @param queryable
     */
    public void setQueryable(boolean queryable)
    {
        this.queryable = queryable;
    }

    /**
     * Gets the fulltextindexed value for this CmisTypeDefinitionType.
     * 
     * @return fulltextindexed
     */
    public boolean isFulltextindexed()
    {
        return fulltextindexed;
    }

    /**
     * Sets the fulltextindexed value for this CmisTypeDefinitionType.
     * 
     * @param fulltextindexed
     */
    public void setFulltextindexed(boolean fulltextindexed)
    {
        this.fulltextindexed = fulltextindexed;
    }

    /**
     * Gets the includedInSupertypeQuery value for this CmisTypeDefinitionType.
     * 
     * @return includedInSupertypeQuery
     */
    public boolean isIncludedInSupertypeQuery()
    {
        return includedInSupertypeQuery;
    }

    /**
     * Sets the includedInSupertypeQuery value for this CmisTypeDefinitionType.
     * 
     * @param includedInSupertypeQuery
     */
    public void setIncludedInSupertypeQuery(boolean includedInSupertypeQuery)
    {
        this.includedInSupertypeQuery = includedInSupertypeQuery;
    }

    public boolean isControllablePolicy()
    {
        return controllablePolicy;
    }

    public void setControllablePolicy(boolean controllablePolicy)
    {
        this.controllablePolicy = controllablePolicy;
    }

    public boolean isControllableACL()
    {
        return controllableACL;
    }

    public void setControllableACL(boolean controllableACL)
    {
        this.controllableACL = controllableACL;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyBooleanDefinitionType[] getPropertyBooleanDefinition()
    {
        return propertyBooleanDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyBooleanDefinitionType getPropertyBooleanDefinition(int index)
    {
        return propertyBooleanDefinition[index];
    }

    public void setPropertyBooleanDefinition(org.alfresco.repo.cmis.ws.CmisPropertyBooleanDefinitionType[] propertyBooleanDefinition)
    {
        this.propertyBooleanDefinition = propertyBooleanDefinition;
    }

    public void setPropertyBooleanDefinition(int index, org.alfresco.repo.cmis.ws.CmisPropertyBooleanDefinitionType propertyBooleanDefinition)
    {
        this.propertyBooleanDefinition[index] = propertyBooleanDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyDateTimeDefinitionType[] getPropertyDateTimeDefinition()
    {
        return propertyDateTimeDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyDateTimeDefinitionType getPropertyDateTimeDefinition(int index)
    {
        return propertyDateTimeDefinition[index];
    }

    public void setPropertyDateTimeDefinition(org.alfresco.repo.cmis.ws.CmisPropertyDateTimeDefinitionType[] propertyDateTimeDefinition)
    {
        this.propertyDateTimeDefinition = propertyDateTimeDefinition;
    }

    public void setPropertyDateTimeDefinition(int index, org.alfresco.repo.cmis.ws.CmisPropertyDateTimeDefinitionType propertyDateTimeDefinition)
    {
        this.propertyDateTimeDefinition[index] = propertyDateTimeDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyDecimalDefinitionType[] getPropertyDecimalDefinition()
    {
        return propertyDecimalDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyDecimalDefinitionType getPropertyDecimalDefinition(int index)
    {
        return propertyDecimalDefinition[index];
    }

    public void setPropertyDecimalDefinition(org.alfresco.repo.cmis.ws.CmisPropertyDecimalDefinitionType[] propertyDecimalDefinition)
    {
        this.propertyDecimalDefinition = propertyDecimalDefinition;
    }

    public void setPropertyDecimalDefinition(int index, org.alfresco.repo.cmis.ws.CmisPropertyDecimalDefinitionType propertyDecimalDefinition)
    {
        this.propertyDecimalDefinition[index] = propertyDecimalDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyIdDefinitionType[] getPropertyIdDefinition()
    {
        return propertyIdDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyIdDefinitionType getPropertyIdDefinition(int index)
    {
        return propertyIdDefinition[index];
    }

    public void setPropertyIdDefinition(org.alfresco.repo.cmis.ws.CmisPropertyIdDefinitionType[] propertyIdDefinition)
    {
        this.propertyIdDefinition = propertyIdDefinition;
    }

    public void setPropertyIdDefinition(int index, org.alfresco.repo.cmis.ws.CmisPropertyIdDefinitionType propertyIdDefinition)
    {
        this.propertyIdDefinition[index] = propertyIdDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyIntegerDefinitionType[] getPropertyIntegerDefinition()
    {
        return propertyIntegerDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyIntegerDefinitionType getPropertyIntegerDefinition(int index)
    {
        return propertyIntegerDefinition[index];
    }

    public void setPropertyIntegerDefinition(org.alfresco.repo.cmis.ws.CmisPropertyIntegerDefinitionType[] propertyIntegerDefinition)
    {
        this.propertyIntegerDefinition = propertyIntegerDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyHtmlDefinitionType[] getPropertyHtmlDefinition()
    {
        return propertyHtmlDefinition;
    }

    public void setPropertyHtmlDefinition(org.alfresco.repo.cmis.ws.CmisPropertyHtmlDefinitionType[] propertyHtmlDefinition)
    {
        this.propertyHtmlDefinition = propertyHtmlDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyXhtmlDefinitionType[] getPropertyXhtmlDefinition()
    {
        return propertyXhtmlDefinition;
    }

    public void setPropertyXhtmlDefinition(org.alfresco.repo.cmis.ws.CmisPropertyXhtmlDefinitionType[] propertyXhtmlDefinition)
    {
        this.propertyXhtmlDefinition = propertyXhtmlDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyStringDefinitionType[] getPropertyStringDefinition()
    {
        return propertyStringDefinition;
    }

    public void setPropertyStringDefinition(org.alfresco.repo.cmis.ws.CmisPropertyStringDefinitionType[] propertyStringDefinition)
    {
        this.propertyStringDefinition = propertyStringDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyXmlDefinitionType[] getPropertyXmlDefinition()
    {
        return propertyXmlDefinition;
    }

    public void setPropertyXmlDefinition(org.alfresco.repo.cmis.ws.CmisPropertyXmlDefinitionType[] propertyXmlDefinition)
    {
        this.propertyXmlDefinition = propertyXmlDefinition;
    }

    public org.alfresco.repo.cmis.ws.CmisPropertyUriDefinitionType[] getPropertyUriDefinition()
    {
        return propertyUriDefinition;
    }

    public void setPropertyUriDefinition(org.alfresco.repo.cmis.ws.CmisPropertyUriDefinitionType[] propertyUriDefinition)
    {
        this.propertyUriDefinition = propertyUriDefinition;
    }

    /**
     * Gets the _any value for this CmisTypeDefinitionType.
     * 
     * @return _any
     */
    public org.apache.axis.message.MessageElement[] get_any()
    {
        return _any;
    }

    /**
     * Sets the _any value for this CmisTypeDefinitionType.
     * 
     * @param _any
     */
    public void set_any(org.apache.axis.message.MessageElement[] _any)
    {
        this._any = _any;
    }

    private java.lang.Object __equalsCalc = null;

    public synchronized boolean equals(java.lang.Object obj)
    {
        if (!(obj instanceof CmisTypeDefinitionType))
            return false;
        CmisTypeDefinitionType other = (CmisTypeDefinitionType) obj;
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (__equalsCalc != null)
        {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true
                && ((this.id == null && other.getId() == null) || (this.id != null && this.id.equals(other.getId())))
                && ((this.localName == null && other.getLocalName() == null) || (this.localName != null && this.localName.equals(other.getLocalName())))
                && ((this.localNamespace == null && other.getLocalNamespace() == null) || (this.localNamespace != null && this.localNamespace.equals(other.getLocalNamespace())))
                && ((this.displayName == null && other.getDisplayName() == null) || (this.displayName != null && this.displayName.equals(other.getDisplayName())))
                && ((this.queryName == null && other.getQueryName() == null) || (this.queryName != null && this.queryName.equals(other.getQueryName())))
                && ((this.description == null && other.getDescription() == null) || (this.description != null && this.description.equals(other.getDescription())))
                && ((this.baseTypeId == null && other.getBaseTypeId() == null) || (this.baseTypeId != null && this.baseTypeId.equals(other.getBaseTypeId())))
                && ((this.parentId == null && other.getParentId() == null) || (this.parentId != null && this.parentId.equals(other.getParentId())))
                && this.creatable == other.isCreatable()
                && this.fileable == other.isFileable()
                && this.queryable == other.isQueryable()
                && this.fulltextindexed == other.isFulltextindexed()
                && this.includedInSupertypeQuery == other.isIncludedInSupertypeQuery()
                && this.controllablePolicy == other.isControllablePolicy()
                && this.controllableACL == other.isControllableACL()
                && ((this.propertyBooleanDefinition == null && other.getPropertyBooleanDefinition() == null) || (this.propertyBooleanDefinition != null && this.propertyBooleanDefinition
                        .equals(other.getPropertyBooleanDefinition())))
                && ((this.propertyDateTimeDefinition == null && other.getPropertyDateTimeDefinition() == null) || (this.propertyDateTimeDefinition != null && this.propertyDateTimeDefinition
                        .equals(other.getPropertyDateTimeDefinition())))
                && ((this.propertyDecimalDefinition == null && other.getPropertyDecimalDefinition() == null) || (this.propertyDecimalDefinition != null && this.propertyDecimalDefinition
                        .equals(other.getPropertyDecimalDefinition())))
                && ((this.propertyIdDefinition == null && other.getPropertyIdDefinition() == null) || (this.propertyIdDefinition != null && this.propertyIdDefinition.equals(other
                        .getPropertyIdDefinition())))
                && ((this.propertyIntegerDefinition == null && other.getPropertyIntegerDefinition() == null) || (this.propertyIntegerDefinition != null && this.propertyIntegerDefinition
                        .equals(other.getPropertyIntegerDefinition())))
                && ((this.propertyHtmlDefinition == null && other.getPropertyHtmlDefinition() == null) || (this.propertyHtmlDefinition != null && this.propertyHtmlDefinition
                        .equals(other.getPropertyHtmlDefinition())))
                && ((this.propertyXhtmlDefinition == null && other.getPropertyXhtmlDefinition() == null) || (this.propertyXhtmlDefinition != null && this.propertyXhtmlDefinition
                        .equals(other.getPropertyXhtmlDefinition())))
                && ((this.propertyStringDefinition == null && other.getPropertyStringDefinition() == null) || (this.propertyStringDefinition != null && this.propertyStringDefinition
                        .equals(other.getPropertyStringDefinition())))
                && ((this.propertyXmlDefinition == null && other.getPropertyXmlDefinition() == null) || (this.propertyXmlDefinition != null && this.propertyXmlDefinition
                        .equals(other.getPropertyXmlDefinition())))
                && ((this.propertyUriDefinition == null && other.getPropertyUriDefinition() == null) || (this.propertyUriDefinition != null && this.propertyUriDefinition
                        .equals(other.getPropertyUriDefinition())))
                && ((this._any == null && other.get_any() == null) || (this._any != null && java.util.Arrays.equals(this._any, other.get_any())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;

    public synchronized int hashCode()
    {
        if (__hashCodeCalc)
        {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getId() != null)
        {
            _hashCode += getId().hashCode();
        }
        if (getLocalName() != null)
        {
            _hashCode += getLocalName().hashCode();
        }
        if (getLocalNamespace() != null)
        {
            _hashCode += getLocalNamespace().hashCode();
        }
        if (getDisplayName() != null)
        {
            _hashCode += getDisplayName().hashCode();
        }
        if (getQueryName() != null)
        {
            _hashCode += getQueryName().hashCode();
        }
        if (getDescription() != null)
        {
            _hashCode += getDescription().hashCode();
        }
        if (getBaseTypeId() != null)
        {
            _hashCode += getBaseTypeId().hashCode();
        }
        if (getParentId() != null)
        {
            _hashCode += getParentId().hashCode();
        }
        _hashCode += (isCreatable() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isFileable() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isQueryable() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isFulltextindexed() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isIncludedInSupertypeQuery() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isControllablePolicy() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isControllableACL() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getPropertyBooleanDefinition() != null)
        {
            _hashCode += getPropertyBooleanDefinition().hashCode();
        }
        if (getPropertyDateTimeDefinition() != null)
        {
            _hashCode += getPropertyDateTimeDefinition().hashCode();
        }
        if (getPropertyDecimalDefinition() != null)
        {
            _hashCode += getPropertyDecimalDefinition().hashCode();
        }
        if (getPropertyIdDefinition() != null)
        {
            _hashCode += getPropertyIdDefinition().hashCode();
        }
        if (getPropertyIntegerDefinition() != null)
        {
            _hashCode += getPropertyIntegerDefinition().hashCode();
        }
        if (getPropertyHtmlDefinition() != null)
        {
            _hashCode += getPropertyHtmlDefinition().hashCode();
        }
        if (getPropertyXhtmlDefinition() != null)
        {
            _hashCode += getPropertyXhtmlDefinition().hashCode();
        }
        if (getPropertyStringDefinition() != null)
        {
            _hashCode += getPropertyStringDefinition().hashCode();
        }
        if (getPropertyXmlDefinition() != null)
        {
            _hashCode += getPropertyXmlDefinition().hashCode();
        }
        if (getPropertyUriDefinition() != null)
        {
            _hashCode += getPropertyUriDefinition().hashCode();
        }
        if (get_any() != null)
        {
            for (int i = 0; i < java.lang.reflect.Array.getLength(get_any()); i++)
            {
                java.lang.Object obj = java.lang.reflect.Array.get(get_any(), i);
                if (obj != null && !obj.getClass().isArray())
                {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc = new org.apache.axis.description.TypeDesc(CmisTypeDefinitionType.class, true);

    static
    {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "cmisTypeDefinitionType"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("id");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "id"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("localName");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "localName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("localNamespace");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "localNamespace"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyURI"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("displayName");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "displayName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("queryName");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "queryName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("description");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "description"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("baseTypeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "baseTypeId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "enumBaseObjectTypeIds"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parentId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "parentId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("creatable");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "creatable"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("fileable");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "fileable"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("queryable");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "queryable"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("fulltextindexed");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "fulltextindexed"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("includedInSupertypeQuery");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "includedInSupertypeQuery"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("controllablePolicy");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "controllablePolicy"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("controllableACL");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "controllableACL"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertyBooleanDefinition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "propertyBooleanDefinition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "cmisPropertyBooleanDefinitionType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertyDateTimeDefinition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "propertyDateTimeDefinition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "cmisPropertyDateTimeDefinitionType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertyDecimalDefinition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "propertyDecimalDefinition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "cmisPropertyDecimalDefinitionType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertyIdDefinition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "propertyIdDefinition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "cmisPropertyIdDefinitionType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertyIntegerDefinition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "propertyIntegerDefinition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "cmisPropertyIntegerDefinitionType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertyHtmlDefinition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "propertyHtmlDefinition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "cmisPropertyHtmlDefinitionType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertyXhtmlDefinition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "propertyXhtmlDefinition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "cmisPropertyXhtmlDefinitionType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertyStringDefinition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "propertyStringDefinition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "cmisPropertyStringDefinitionType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertyXmlDefinition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "propertyXmlDefinition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "cmisPropertyXmlDefinitionType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("propertyUriDefinition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "propertyUriDefinition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200901", "cmisPropertyUriDefinitionType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc()
    {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(java.lang.String mechType, java.lang.Class<?> _javaType, javax.xml.namespace.QName _xmlType)
    {
        return new org.apache.axis.encoding.ser.BeanSerializer(_javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(java.lang.String mechType, java.lang.Class<?> _javaType, javax.xml.namespace.QName _xmlType)
    {
        return new org.apache.axis.encoding.ser.BeanDeserializer(_javaType, _xmlType, typeDesc);
    }
}
