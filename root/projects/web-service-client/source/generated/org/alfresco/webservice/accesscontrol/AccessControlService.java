/**
 * AccessControlService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.2.1 Jun 14, 2005 (09:15:57 EDT) WSDL2Java emitter.
 */

package org.alfresco.webservice.accesscontrol;

public interface AccessControlService extends javax.xml.rpc.Service {

/**
 * Access control service.
 */
    public java.lang.String getAccessControlServiceAddress();

    public org.alfresco.webservice.accesscontrol.AccessControlServiceSoapPort getAccessControlService() throws javax.xml.rpc.ServiceException;

    public org.alfresco.webservice.accesscontrol.AccessControlServiceSoapPort getAccessControlService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
