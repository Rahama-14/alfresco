/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Immutable property value storage class.
 * <p>
 * The 
 * 
 * @author Derek Hulley
 */
public class PropertyValue implements Cloneable, Serializable
{
    private static final long serialVersionUID = -497902497351493075L;

    private static Log logger = LogFactory.getLog(PropertyValue.class);

    /** potential value types */
    private static enum ValueType
    {
        NULL
        {
            @Override
            Serializable convert(Serializable value)
            {
                return null;
            }
        },
        BOOLEAN
        {
            @Override
            Serializable convert(Serializable value)
            {
                return DefaultTypeConverter.INSTANCE.convert(Boolean.class, value);
            }
        },
        INTEGER
        {
            @Override
            protected ValueType getPersistedType()
            {
                return ValueType.LONG;
            }

            @Override
            Serializable convert(Serializable value)
            {
                return DefaultTypeConverter.INSTANCE.convert(Integer.class, value);
            }
        },
        LONG
        {
            @Override
            Serializable convert(Serializable value)
            {
                return DefaultTypeConverter.INSTANCE.convert(Long.class, value);
            }
        },
        FLOAT
        {
            @Override
            Serializable convert(Serializable value)
            {
                return DefaultTypeConverter.INSTANCE.convert(Float.class, value);
            }
        },
        DOUBLE
        {
            @Override
            Serializable convert(Serializable value)
            {
                return DefaultTypeConverter.INSTANCE.convert(Double.class, value);
            }
        },
        STRING
        {
            @Override
            Serializable convert(Serializable value)
            {
                return DefaultTypeConverter.INSTANCE.convert(String.class, value);
            }
        },
        DATE
        {
            @Override
            protected ValueType getPersistedType()
            {
                return ValueType.STRING;
            }

            @Override
            Serializable convert(Serializable value)
            {
                return DefaultTypeConverter.INSTANCE.convert(Date.class, value);
            }
        },
        SERIALIZABLE
        {
            @Override
            Serializable convert(Serializable value)
            {
                return value;
            }
        },
        CONTENT
        {
            @Override
            protected ValueType getPersistedType()
            {
                return ValueType.STRING;
            }

            @Override
            Serializable convert(Serializable value)
            {
                return DefaultTypeConverter.INSTANCE.convert(ContentData.class, value);
            }
        },
        NODEREF
        {
            @Override
            protected ValueType getPersistedType()
            {
                return ValueType.STRING;
            }

            @Override
            Serializable convert(Serializable value)
            {
                return DefaultTypeConverter.INSTANCE.convert(NodeRef.class, value);
            }
        },
        QNAME
        {
            @Override
            protected ValueType getPersistedType()
            {
                return ValueType.STRING;
            }

            @Override
            Serializable convert(Serializable value)
            {
                return DefaultTypeConverter.INSTANCE.convert(QName.class, value);
            }
        },
        PATH
        {
            @Override
            protected ValueType getPersistedType()
            {
                return ValueType.SERIALIZABLE;
            }

            @Override
            Serializable convert(Serializable value)
            {
                return DefaultTypeConverter.INSTANCE.convert(Path.class, value);
            }
        };
        
        /** override if the type gets persisted in a different format */
        protected ValueType getPersistedType()
        {
            return this;
        }
        
        /**
         * @see DefaultTypeConverter.INSTANCE#convert(Class, Object)
         */
        abstract Serializable convert(Serializable value);
        
        protected ArrayList<Serializable> convert(Collection collection)
        {
            ArrayList<Serializable> arrayList = new ArrayList<Serializable>(collection.size());
            for (Object object : collection)
            {
                Serializable newValue = null;
                if (object != null)
                {
                    if (!(object instanceof Serializable))
                    {
                        throw new AlfrescoRuntimeException("Collection values must contain Serializable instances: \n" +
                                "   value type: " + this + "\n" +
                                "   collection: " + collection + "\n" +
                                "   value: " + object);
                    }
                    Serializable value = (Serializable) object;
                    newValue = convert(value);
                }
                arrayList.add(newValue);
            }
            // done
            return arrayList;
        }
    }
    
    /** a mapping from a property type <code>QName</code> to the corresponding value type */
    private static Map<QName, ValueType> valueTypesByPropertyType;
    static
    {
        valueTypesByPropertyType = new HashMap<QName, ValueType>(17);
        valueTypesByPropertyType.put(DataTypeDefinition.ANY, ValueType.SERIALIZABLE);
        valueTypesByPropertyType.put(DataTypeDefinition.BOOLEAN, ValueType.BOOLEAN);
        valueTypesByPropertyType.put(DataTypeDefinition.INT, ValueType.INTEGER);
        valueTypesByPropertyType.put(DataTypeDefinition.LONG, ValueType.LONG);
        valueTypesByPropertyType.put(DataTypeDefinition.DOUBLE, ValueType.DOUBLE);
        valueTypesByPropertyType.put(DataTypeDefinition.FLOAT, ValueType.FLOAT);
        valueTypesByPropertyType.put(DataTypeDefinition.DATE, ValueType.DATE);
        valueTypesByPropertyType.put(DataTypeDefinition.DATETIME, ValueType.DATE);
        valueTypesByPropertyType.put(DataTypeDefinition.CATEGORY, ValueType.NODEREF);
        valueTypesByPropertyType.put(DataTypeDefinition.CONTENT, ValueType.CONTENT);
        valueTypesByPropertyType.put(DataTypeDefinition.TEXT, ValueType.STRING);
        valueTypesByPropertyType.put(DataTypeDefinition.GUID, ValueType.STRING);
        valueTypesByPropertyType.put(DataTypeDefinition.NODE_REF, ValueType.NODEREF);
        valueTypesByPropertyType.put(DataTypeDefinition.PATH, ValueType.PATH);
        valueTypesByPropertyType.put(DataTypeDefinition.QNAME, ValueType.QNAME);
    }

    /** the type of the property, prior to serialization persistence */
    private ValueType actualType;
    /** true if the property values are contained in a collection */
    private boolean isMultiValued;
    /** the type of persistence used */
    private ValueType persistedType;
    
    private Boolean booleanValue;
    private Long longValue;
    private Float floatValue;
    private Double doubleValue;
    private String stringValue;
    private Serializable serializableValue;
    
    /**
     * default constructor
     */
    public PropertyValue()
    {
    }
    
    /**
     * Construct a new property value.
     * 
     * @param typeQName the dictionary-defined property type to store the property as
     * @param value the value to store.  This will be converted into a format compatible
     *      with the type given
     * 
     * @throws java.lang.UnsupportedOperationException if the value cannot be converted to the
     *      type given
     */
    public PropertyValue(QName typeQName, Serializable value)
    {
        this.actualType = makeValueType(typeQName);
        if (value == null)
        {
            setPersistedValue(ValueType.NULL, null);
            setMultiValued(false);
        }
        else if (value instanceof Collection)
        {
            Collection collection = (Collection) value;
            ValueType collectionValueType = makeValueType(typeQName);
            // convert the collection values - we need to do this to ensure that the
            // values provided conform to the given type
            ArrayList<Serializable> convertedCollection = collectionValueType.convert(collection);
            // the persisted type is, nonetheless, a serializable
            setPersistedValue(ValueType.SERIALIZABLE, convertedCollection);
            setMultiValued(true);
        }
        else
        {
            // get the persisted type
            ValueType valueType = makeValueType(typeQName);
            ValueType persistedValueType = valueType.getPersistedType();
            // convert to the persistent type
            value = persistedValueType.convert(value);
            setPersistedValue(persistedValueType, value);
            setMultiValued(false);
        }
    }
    
    /**
     * Helper method to convert the type <code>QName</code> into a <code>ValueType</code>
     * 
     * @return Returns the <code>ValueType</code>  - never null
     */
    private ValueType makeValueType(QName typeQName)
    {
        ValueType valueType = valueTypesByPropertyType.get(typeQName);
        if (valueType == null)
        {
            throw new AlfrescoRuntimeException(
                    "Property type not recognised: \n" +
                    "   type: " + typeQName + "\n" +
                    "   property: " + this);
        }
        return valueType;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (obj instanceof PropertyValue)
        {
            PropertyValue that = (PropertyValue) obj;
            return (this.actualType.equals(that.actualType) &&
                    EqualsHelper.nullSafeEquals(this.booleanValue, that.booleanValue) &&
                    EqualsHelper.nullSafeEquals(this.longValue, that.longValue) &&
                    EqualsHelper.nullSafeEquals(this.floatValue, that.floatValue) &&
                    EqualsHelper.nullSafeEquals(this.doubleValue, that.doubleValue) &&
                    EqualsHelper.nullSafeEquals(this.stringValue, that.stringValue) &&
                    EqualsHelper.nullSafeEquals(this.serializableValue, that.serializableValue)
                    );
            
        }
        else
        {
            return false;
        }
    }
    
    @Override
    public int hashCode()
    {
        int h = 0;
        if (actualType != null)
            h = actualType.hashCode();
        Serializable persistedValue = getPersistedValue();
        if (persistedValue != null)
            h += 17 * persistedValue.hashCode();
        return h;
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(128);
        sb.append("PropertyValue")
          .append("[actual-type=").append(actualType)
          .append(", multi-valued=").append(isMultiValued)
          .append(", value-type=").append(persistedType)
          .append(", value=").append(getPersistedValue())
          .append("]");
        return sb.toString();
    }

    public String getActualType()
    {
        return actualType.toString();
    }

    public void setActualType(String actualType)
    {
        this.actualType = ValueType.valueOf(actualType);
    }

    public boolean isMultiValued()
    {
        return isMultiValued;
    }

    public void setMultiValued(boolean isMultiValued)
    {
        this.isMultiValued = isMultiValued;
    }

    public String getPersistedType()
    {
        return persistedType.toString();
    }
    public void setPersistedType(String persistedType)
    {
        this.persistedType = ValueType.valueOf(persistedType);
    }
    
    /**
     * Stores the value in the correct slot based on the type of persistence requested.
     * No conversion is done.
     * 
     * @param persistedType the value type
     * @param value the value - it may only be null if the persisted type is {@link ValueType#NULL}
     */
    public void setPersistedValue(ValueType persistedType, Serializable value)
    {
        switch (persistedType)
        {
            case NULL:
                if (value != null)
                {
                    throw new AlfrescoRuntimeException("Value must be null for persisted type: " + persistedType);
                }
                break;
            case BOOLEAN:
                this.booleanValue = (Boolean) value;
                break;
            case LONG:
                this.longValue = (Long) value;
                break;
            case FLOAT:
                this.floatValue = (Float) value;
                break;
            case DOUBLE:
                this.doubleValue = (Double) value;
                break;
            case STRING:
                this.stringValue = (String) value;
                break;
            case SERIALIZABLE:
                this.serializableValue = (Serializable) value;
                break;
            default:
                throw new AlfrescoRuntimeException("Unrecognised value type: " + persistedType);
        }
        // we store the type that we persisted as
        this.persistedType = persistedType;
    }

    /**
     * @return Returns the persisted value, keying off the persisted value type
     */
    private Serializable getPersistedValue()
    {
        switch (persistedType)
        {
            case NULL:
                return null;
            case BOOLEAN:
                return this.booleanValue;
            case LONG:
                return this.longValue;
            case FLOAT:
                return this.floatValue;
            case DOUBLE:
                return this.doubleValue;
            case STRING:
                return this.stringValue;
            case SERIALIZABLE:
                return this.serializableValue;
            default:
                throw new AlfrescoRuntimeException("Unrecognised value type: " + persistedType);
        }
    }

    /**
     * Fetches the value as a desired type.  Collections (i.e. multi-valued properties)
     * will be converted as a whole to ensure that all the values returned within the
     * collection match the given type.
     * 
     * @param typeQName the type required for the return value
     * @return Returns the value of this property as the desired type, or a <code>Collection</code>
     *      of values of the required type
     * 
     * @throws java.lang.UnsupportedOperationException if the value cannot be converted to the
     *      type given
     * 
     * @see DataTypeDefinition#ANY The static qualified names for the types
     */
    public Serializable getValue(QName typeQName)
    {
        // first check for null
        
        ValueType requiredType = makeValueType(typeQName);
        
        // we need to convert
        Serializable ret = null;
        if (persistedType == ValueType.NULL)
        {
            ret = null;
        }
        else if (this.isMultiValued)
        {
            // collections are always stored
            Collection collection = (Collection) this.serializableValue;
            // convert the collection values - we need to do this to ensure that the
            // values provided conform to the given type
            ArrayList<Serializable> convertedCollection = requiredType.convert(collection);
            ret = convertedCollection;
        }
        else
        {
            Serializable persistedValue = getPersistedValue();
            // convert the type
            ret = requiredType.convert(persistedValue);
        }
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Fetched value: \n" +
                    "   property value: " + this + "\n" +
                    "   requested type: " + requiredType + "\n" +
                    "   result: " + ret);
        }
        return ret;
    }
    
    public boolean getBooleanValue()
    {
        if (booleanValue == null)
            return false;
        else
            return booleanValue.booleanValue();
    }
    public void setBooleanValue(boolean value)
    {
        this.booleanValue = Boolean.valueOf(value);
    }
    
    public long getLongValue()
    {
        if (longValue == null)
            return 0;
        else
            return longValue.longValue();
    }
    public void setLongValue(long value)
    {
        this.longValue = Long.valueOf(value);
    }
    
    public float getFloatValue()
    {
        if (floatValue == null)
            return 0.0F;
        else
            return floatValue.floatValue();
    }
    public void setFloatValue(float value)
    {
        this.floatValue = Float.valueOf(value);
    }
    
    public double getDoubleValue()
    {
        if (doubleValue == null)
            return 0.0;
        else
            return doubleValue.doubleValue();
    }
    public void setDoubleValue(double value)
    {
        this.doubleValue = Double.valueOf(value);
    }
    
    public String getStringValue()
    {
        return stringValue;
    }
    public void setStringValue(String value)
    {
        this.stringValue = value;
    }
    
    public Serializable getSerializableValue()
    {
        return serializableValue;
    }
    public void setSerializableValue(Serializable value)
    {
        this.serializableValue = value;
    }
}
