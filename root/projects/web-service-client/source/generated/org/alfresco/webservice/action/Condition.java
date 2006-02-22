/**
 * Condition.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.alfresco.webservice.action;

public class Condition  implements java.io.Serializable {
    private java.lang.String id;

    private java.lang.String conditionName;

    private boolean invertCondition;

    private org.alfresco.webservice.types.NamedValue[] parameters;

    public Condition() {
    }

    public Condition(
           java.lang.String id,
           java.lang.String conditionName,
           boolean invertCondition,
           org.alfresco.webservice.types.NamedValue[] parameters) {
           this.id = id;
           this.conditionName = conditionName;
           this.invertCondition = invertCondition;
           this.parameters = parameters;
    }


    /**
     * Gets the id value for this Condition.
     * 
     * @return id
     */
    public java.lang.String getId() {
        return id;
    }


    /**
     * Sets the id value for this Condition.
     * 
     * @param id
     */
    public void setId(java.lang.String id) {
        this.id = id;
    }


    /**
     * Gets the conditionName value for this Condition.
     * 
     * @return conditionName
     */
    public java.lang.String getConditionName() {
        return conditionName;
    }


    /**
     * Sets the conditionName value for this Condition.
     * 
     * @param conditionName
     */
    public void setConditionName(java.lang.String conditionName) {
        this.conditionName = conditionName;
    }


    /**
     * Gets the invertCondition value for this Condition.
     * 
     * @return invertCondition
     */
    public boolean isInvertCondition() {
        return invertCondition;
    }


    /**
     * Sets the invertCondition value for this Condition.
     * 
     * @param invertCondition
     */
    public void setInvertCondition(boolean invertCondition) {
        this.invertCondition = invertCondition;
    }


    /**
     * Gets the parameters value for this Condition.
     * 
     * @return parameters
     */
    public org.alfresco.webservice.types.NamedValue[] getParameters() {
        return parameters;
    }


    /**
     * Sets the parameters value for this Condition.
     * 
     * @param parameters
     */
    public void setParameters(org.alfresco.webservice.types.NamedValue[] parameters) {
        this.parameters = parameters;
    }

    public org.alfresco.webservice.types.NamedValue getParameters(int i) {
        return this.parameters[i];
    }

    public void setParameters(int i, org.alfresco.webservice.types.NamedValue _value) {
        this.parameters[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Condition)) return false;
        Condition other = (Condition) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.id==null && other.getId()==null) || 
             (this.id!=null &&
              this.id.equals(other.getId()))) &&
            ((this.conditionName==null && other.getConditionName()==null) || 
             (this.conditionName!=null &&
              this.conditionName.equals(other.getConditionName()))) &&
            this.invertCondition == other.isInvertCondition() &&
            ((this.parameters==null && other.getParameters()==null) || 
             (this.parameters!=null &&
              java.util.Arrays.equals(this.parameters, other.getParameters())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getId() != null) {
            _hashCode += getId().hashCode();
        }
        if (getConditionName() != null) {
            _hashCode += getConditionName().hashCode();
        }
        _hashCode += (isInvertCondition() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getParameters() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getParameters());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getParameters(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Condition.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/action/1.0", "Condition"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("id");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/action/1.0", "id"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("conditionName");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/action/1.0", "conditionName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("invertCondition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/action/1.0", "invertCondition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parameters");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/action/1.0", "parameters"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.alfresco.org/ws/model/content/1.0", "NamedValue"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
