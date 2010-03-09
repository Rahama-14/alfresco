/**
 * EnumBaseObjectTypeIds.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.alfresco.repo.cmis.ws;

public class EnumBaseObjectTypeIds implements java.io.Serializable {
    private java.lang.String _value_;
    private static java.util.HashMap _table_ = new java.util.HashMap();

    // Constructor
    protected EnumBaseObjectTypeIds(java.lang.String value) {
        _value_ = value;
        _table_.put(_value_,this);
    }

    public static final java.lang.String _value1 = "cmis:document";
    public static final java.lang.String _value2 = "cmis:folder";
    public static final java.lang.String _value3 = "cmis:relationship";
    public static final java.lang.String _value4 = "cmis:policy";
    public static final EnumBaseObjectTypeIds value1 = new EnumBaseObjectTypeIds(_value1);
    public static final EnumBaseObjectTypeIds value2 = new EnumBaseObjectTypeIds(_value2);
    public static final EnumBaseObjectTypeIds value3 = new EnumBaseObjectTypeIds(_value3);
    public static final EnumBaseObjectTypeIds value4 = new EnumBaseObjectTypeIds(_value4);
    public java.lang.String getValue() { return _value_;}
    public static EnumBaseObjectTypeIds fromValue(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        EnumBaseObjectTypeIds enumeration = (EnumBaseObjectTypeIds)
            _table_.get(value);
        if (enumeration==null) throw new java.lang.IllegalArgumentException();
        return enumeration;
    }
    public static EnumBaseObjectTypeIds fromString(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        return fromValue(value);
    }
    public boolean equals(java.lang.Object obj) {return (obj == this);}
    public int hashCode() { return toString().hashCode();}
    public java.lang.String toString() { return _value_;}
    public java.lang.Object readResolve() throws java.io.ObjectStreamException { return fromValue(_value_);}
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new org.apache.axis.encoding.ser.EnumSerializer(
            _javaType, _xmlType);
    }
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new org.apache.axis.encoding.ser.EnumDeserializer(
            _javaType, _xmlType);
    }
    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(EnumBaseObjectTypeIds.class);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://docs.oasis-open.org/ns/cmis/core/200908/", "enumBaseObjectTypeIds"));
    }
    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

}
