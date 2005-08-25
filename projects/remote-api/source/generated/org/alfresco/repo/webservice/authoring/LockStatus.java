/**
 * LockStatus.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.2.1 Jun 14, 2005 (09:15:57 EDT) WSDL2Java emitter.
 */

package org.alfresco.repo.webservice.authoring;

public class LockStatus  implements java.io.Serializable {
    private org.alfresco.repo.webservice.types.Reference node;
    private org.alfresco.repo.webservice.authoring.LockTypeEnum lockType;
    private org.alfresco.repo.webservice.types.Reference lockOwner;

    public LockStatus() {
    }

    public LockStatus(
           org.alfresco.repo.webservice.types.Reference node,
           org.alfresco.repo.webservice.authoring.LockTypeEnum lockType,
           org.alfresco.repo.webservice.types.Reference lockOwner) {
           this.node = node;
           this.lockType = lockType;
           this.lockOwner = lockOwner;
    }


    /**
     * Gets the node value for this LockStatus.
     * 
     * @return node
     */
    public org.alfresco.repo.webservice.types.Reference getNode() {
        return node;
    }


    /**
     * Sets the node value for this LockStatus.
     * 
     * @param node
     */
    public void setNode(org.alfresco.repo.webservice.types.Reference node) {
        this.node = node;
    }


    /**
     * Gets the lockType value for this LockStatus.
     * 
     * @return lockType
     */
    public org.alfresco.repo.webservice.authoring.LockTypeEnum getLockType() {
        return lockType;
    }


    /**
     * Sets the lockType value for this LockStatus.
     * 
     * @param lockType
     */
    public void setLockType(org.alfresco.repo.webservice.authoring.LockTypeEnum lockType) {
        this.lockType = lockType;
    }


    /**
     * Gets the lockOwner value for this LockStatus.
     * 
     * @return lockOwner
     */
    public org.alfresco.repo.webservice.types.Reference getLockOwner() {
        return lockOwner;
    }


    /**
     * Sets the lockOwner value for this LockStatus.
     * 
     * @param lockOwner
     */
    public void setLockOwner(org.alfresco.repo.webservice.types.Reference lockOwner) {
        this.lockOwner = lockOwner;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof LockStatus)) return false;
        LockStatus other = (LockStatus) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.node==null && other.getNode()==null) || 
             (this.node!=null &&
              this.node.equals(other.getNode()))) &&
            ((this.lockType==null && other.getLockType()==null) || 
             (this.lockType!=null &&
              this.lockType.equals(other.getLockType()))) &&
            ((this.lockOwner==null && other.getLockOwner()==null) || 
             (this.lockOwner!=null &&
              this.lockOwner.equals(other.getLockOwner())));
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
        if (getNode() != null) {
            _hashCode += getNode().hashCode();
        }
        if (getLockType() != null) {
            _hashCode += getLockType().hashCode();
        }
        if (getLockOwner() != null) {
            _hashCode += getLockOwner().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(LockStatus.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/authoring/1.0", "LockStatus"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("node");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/authoring/1.0", "node"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.alfresco.org/ws/model/content/1.0", "Reference"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lockType");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/authoring/1.0", "lockType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/authoring/1.0", "LockTypeEnum"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lockOwner");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/authoring/1.0", "lockOwner"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.alfresco.org/ws/model/content/1.0", "Reference"));
        elemField.setNillable(false);
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
