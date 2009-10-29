/**
 * DictionaryServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.alfresco.repo.webservice.dictionary;

public class DictionaryServiceLocator extends org.apache.axis.client.Service implements org.alfresco.repo.webservice.dictionary.DictionaryService {

/**
 * Provides read access to the Repository Dictionary.
 */

    public DictionaryServiceLocator() {
    }


    public DictionaryServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public DictionaryServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for DictionaryService
    private java.lang.String DictionaryService_address = "http://localhost:8080/alfresco/api/DictionaryService";

    public java.lang.String getDictionaryServiceAddress() {
        return DictionaryService_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String DictionaryServiceWSDDServiceName = "DictionaryService";

    public java.lang.String getDictionaryServiceWSDDServiceName() {
        return DictionaryServiceWSDDServiceName;
    }

    public void setDictionaryServiceWSDDServiceName(java.lang.String name) {
        DictionaryServiceWSDDServiceName = name;
    }

    public org.alfresco.repo.webservice.dictionary.DictionaryServiceSoapPort getDictionaryService() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(DictionaryService_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getDictionaryService(endpoint);
    }

    public org.alfresco.repo.webservice.dictionary.DictionaryServiceSoapPort getDictionaryService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.alfresco.repo.webservice.dictionary.DictionaryServiceSoapBindingStub _stub = new org.alfresco.repo.webservice.dictionary.DictionaryServiceSoapBindingStub(portAddress, this);
            _stub.setPortName(getDictionaryServiceWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setDictionaryServiceEndpointAddress(java.lang.String address) {
        DictionaryService_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.alfresco.repo.webservice.dictionary.DictionaryServiceSoapPort.class.isAssignableFrom(serviceEndpointInterface)) {
                org.alfresco.repo.webservice.dictionary.DictionaryServiceSoapBindingStub _stub = new org.alfresco.repo.webservice.dictionary.DictionaryServiceSoapBindingStub(new java.net.URL(DictionaryService_address), this);
                _stub.setPortName(getDictionaryServiceWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("DictionaryService".equals(inputPortName)) {
            return getDictionaryService();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/dictionary/1.0", "DictionaryService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/dictionary/1.0", "DictionaryService"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("DictionaryService".equals(portName)) {
            setDictionaryServiceEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
