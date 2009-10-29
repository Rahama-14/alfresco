/**
 * ActionServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.alfresco.repo.webservice.action;

public class ActionServiceLocator extends org.apache.axis.client.Service implements org.alfresco.repo.webservice.action.ActionService {

/**
 * Provides Action and Rule manipulation methods.
 */

    public ActionServiceLocator() {
    }


    public ActionServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public ActionServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for ActionService
    private java.lang.String ActionService_address = "http://localhost:8080/alfresco/api/ActionService";

    public java.lang.String getActionServiceAddress() {
        return ActionService_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String ActionServiceWSDDServiceName = "ActionService";

    public java.lang.String getActionServiceWSDDServiceName() {
        return ActionServiceWSDDServiceName;
    }

    public void setActionServiceWSDDServiceName(java.lang.String name) {
        ActionServiceWSDDServiceName = name;
    }

    public org.alfresco.repo.webservice.action.ActionServiceSoapPort getActionService() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(ActionService_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getActionService(endpoint);
    }

    public org.alfresco.repo.webservice.action.ActionServiceSoapPort getActionService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.alfresco.repo.webservice.action.ActionServiceSoapBindingStub _stub = new org.alfresco.repo.webservice.action.ActionServiceSoapBindingStub(portAddress, this);
            _stub.setPortName(getActionServiceWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setActionServiceEndpointAddress(java.lang.String address) {
        ActionService_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.alfresco.repo.webservice.action.ActionServiceSoapPort.class.isAssignableFrom(serviceEndpointInterface)) {
                org.alfresco.repo.webservice.action.ActionServiceSoapBindingStub _stub = new org.alfresco.repo.webservice.action.ActionServiceSoapBindingStub(new java.net.URL(ActionService_address), this);
                _stub.setPortName(getActionServiceWSDDServiceName());
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
        if ("ActionService".equals(inputPortName)) {
            return getActionService();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/action/1.0", "ActionService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://www.alfresco.org/ws/service/action/1.0", "ActionService"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("ActionService".equals(portName)) {
            setActionServiceEndpointAddress(address);
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
