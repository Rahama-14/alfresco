/**
 * ContentService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.alfresco.webservice.content;

public interface ContentService extends javax.xml.rpc.Service {

/**
 * Provides read and write access to content streams.
 */
    public java.lang.String getContentServiceAddress();

    public org.alfresco.webservice.content.ContentServiceSoapPort getContentService() throws javax.xml.rpc.ServiceException;

    public org.alfresco.webservice.content.ContentServiceSoapPort getContentService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
