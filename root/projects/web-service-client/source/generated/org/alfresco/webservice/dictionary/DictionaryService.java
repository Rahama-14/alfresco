/**
 * DictionaryService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.alfresco.webservice.dictionary;

public interface DictionaryService extends javax.xml.rpc.Service {

/**
 * Provides read access to the Repository Dictionary.
 */
    public java.lang.String getDictionaryServiceAddress();

    public org.alfresco.webservice.dictionary.DictionaryServiceSoapPort getDictionaryService() throws javax.xml.rpc.ServiceException;

    public org.alfresco.webservice.dictionary.DictionaryServiceSoapPort getDictionaryService(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
