/**
 * RepositoryServiceSoapPort.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.2.1 Jun 14, 2005 (09:15:57 EDT) WSDL2Java emitter.
 */

package org.alfresco.repo.webservice.repository;

public interface RepositoryServiceSoapPort extends java.rmi.Remote {

    /**
     * Retrieves a list of stores where content resources are held.
     */
    public org.alfresco.repo.webservice.types.Store[] getStores() throws java.rmi.RemoteException, org.alfresco.repo.webservice.repository.RepositoryFault;

    /**
     * Executes a query against a store.
     */
    public org.alfresco.repo.webservice.repository.QueryResult query(org.alfresco.repo.webservice.types.Store store, org.alfresco.repo.webservice.types.Query query, boolean includeMetaData) throws java.rmi.RemoteException, org.alfresco.repo.webservice.repository.RepositoryFault;

    /**
     * Executes a query to retrieve the children of the specified
     * resource.
     */
    public org.alfresco.repo.webservice.repository.QueryResult queryChildren(org.alfresco.repo.webservice.types.Reference node) throws java.rmi.RemoteException, org.alfresco.repo.webservice.repository.RepositoryFault;

    /**
     * Executes a query to retrieve the parents of the specified resource.
     */
    public org.alfresco.repo.webservice.repository.QueryResult queryParents(org.alfresco.repo.webservice.types.Reference node) throws java.rmi.RemoteException, org.alfresco.repo.webservice.repository.RepositoryFault;

    /**
     * Executes a query to retrieve associated resources of the specified
     * resource.
     */
    public org.alfresco.repo.webservice.repository.QueryResult queryAssociated(org.alfresco.repo.webservice.types.Reference node, org.alfresco.repo.webservice.repository.Association[] association) throws java.rmi.RemoteException, org.alfresco.repo.webservice.repository.RepositoryFault;

    /**
     * Fetches the next batch of query results.
     */
    public org.alfresco.repo.webservice.repository.QueryResult fetchMore(java.lang.String querySession) throws java.rmi.RemoteException, org.alfresco.repo.webservice.repository.RepositoryFault;

    /**
     * Executes a CML script to manipulate the contents of a Repository
     * store.
     */
    public org.alfresco.repo.webservice.repository.UpdateResult update(org.alfresco.repo.webservice.types.CML statements) throws java.rmi.RemoteException, org.alfresco.repo.webservice.repository.RepositoryFault;

    /**
     * Describes a content resource.
     */
    public org.alfresco.repo.webservice.types.NodeDefinition[] describe(org.alfresco.repo.webservice.types.Predicate items) throws java.rmi.RemoteException, org.alfresco.repo.webservice.repository.RepositoryFault;
}
