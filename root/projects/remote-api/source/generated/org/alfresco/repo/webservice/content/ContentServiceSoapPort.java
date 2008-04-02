/**
 * ContentServiceSoapPort.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.alfresco.repo.webservice.content;

public interface ContentServiceSoapPort extends java.rmi.Remote {

    /**
     * Retrieves content from the repository.
     */
    public org.alfresco.repo.webservice.content.Content[] read(org.alfresco.repo.webservice.types.Predicate items, java.lang.String property) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;

    /**
     * Writes content to the repository.
     */
    public org.alfresco.repo.webservice.content.Content write(org.alfresco.repo.webservice.types.Reference node, java.lang.String property, byte[] content, org.alfresco.repo.webservice.types.ContentFormat format) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;

    /**
     * Clears content from the repository.
     */
    public org.alfresco.repo.webservice.content.Content[] clear(org.alfresco.repo.webservice.types.Predicate items, java.lang.String property) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;

    /**
     * Transforms content from one mimetype to another.
     */
    public org.alfresco.repo.webservice.content.Content transform(org.alfresco.repo.webservice.types.Reference source, java.lang.String property, org.alfresco.repo.webservice.types.Reference destinationReference, java.lang.String destinationProperty, org.alfresco.repo.webservice.types.ContentFormat destinationFormat) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;
}
