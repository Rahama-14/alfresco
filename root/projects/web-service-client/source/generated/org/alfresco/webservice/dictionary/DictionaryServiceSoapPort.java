/**
 * DictionaryServiceSoapPort.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.alfresco.webservice.dictionary;

public interface DictionaryServiceSoapPort extends java.rmi.Remote {

    /**
     * Retrieves the class definitions of types and aspects.
     */
    public org.alfresco.webservice.types.ClassDefinition[] getClasses(org.alfresco.webservice.dictionary.ClassPredicate types, org.alfresco.webservice.dictionary.ClassPredicate aspects) throws java.rmi.RemoteException, org.alfresco.webservice.dictionary.DictionaryFault;

    /**
     * Retrieves property definitions.
     */
    public org.alfresco.webservice.types.PropertyDefinition[] getProperties(java.lang.String[] propertyNames) throws java.rmi.RemoteException, org.alfresco.webservice.dictionary.DictionaryFault;

    /**
     * Retrieves association definitions.
     */
    public org.alfresco.webservice.types.AssociationDefinition[] getAssociations(java.lang.String[] associationNames) throws java.rmi.RemoteException, org.alfresco.webservice.dictionary.DictionaryFault;

    /**
     * Determines whether a type (or aspect) is a sub class of another
     * type (or aspect).
     */
    public boolean isSubClass(java.lang.String className, java.lang.String isSubClassOfName) throws java.rmi.RemoteException, org.alfresco.webservice.dictionary.DictionaryFault;
}
