/**
 * AuthenticationServiceSoapPort.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.2.1 Jun 14, 2005 (09:15:57 EDT) WSDL2Java emitter.
 */

package org.alfresco.example.webservice.authentication;

public interface AuthenticationServiceSoapPort extends java.rmi.Remote {
    public org.alfresco.example.webservice.authentication.AuthenticationResult authenticate(java.lang.String username, java.lang.String password) throws java.rmi.RemoteException, org.alfresco.example.webservice.authentication.AuthenticationFault;
}
