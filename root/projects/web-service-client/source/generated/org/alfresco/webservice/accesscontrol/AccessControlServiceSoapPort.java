/**
 * AccessControlServiceSoapPort.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.alfresco.webservice.accesscontrol;

public interface AccessControlServiceSoapPort extends java.rmi.Remote {

    /**
     * Get the list of the ACL's for a reference.
     */
    public org.alfresco.webservice.accesscontrol.ACL[] getACLs(org.alfresco.webservice.types.Predicate predicate, org.alfresco.webservice.accesscontrol.ACE filter) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Adds a number of ACE's to a collection of references.
     */
    public org.alfresco.webservice.accesscontrol.ACL[] addACEs(org.alfresco.webservice.types.Predicate predicate, org.alfresco.webservice.accesscontrol.ACE[] aces) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Removes ACE's from a collection of references.
     */
    public org.alfresco.webservice.accesscontrol.ACL[] removeACEs(org.alfresco.webservice.types.Predicate predicate, org.alfresco.webservice.accesscontrol.ACE[] aces) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Gets a list of the settable permissions for a set of references.
     */
    public org.alfresco.webservice.accesscontrol.GetPermissionsResult[] getPermissions(org.alfresco.webservice.types.Predicate predicate) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Gets a list of the settable permissions for a list of classes.
     */
    public org.alfresco.webservice.accesscontrol.GetClassPermissionsResult[] getClassPermissions(java.lang.String[] classNames) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Determines whether the current authenticated user has the permissions
     * on each of the specified references.
     */
    public org.alfresco.webservice.accesscontrol.HasPermissionsResult[] hasPermissions(org.alfresco.webservice.types.Predicate predicate, java.lang.String[] permissions) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Set the inherit permission property of a set of references.
     */
    public org.alfresco.webservice.accesscontrol.ACL[] setInheritPermission(org.alfresco.webservice.types.Predicate predicate, boolean inheritPermission) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Get the current node owner.
     */
    public org.alfresco.webservice.accesscontrol.OwnerResult[] getOwners(org.alfresco.webservice.types.Predicate predicate) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Sets the current node owner.
     */
    public org.alfresco.webservice.accesscontrol.OwnerResult[] setOwners(org.alfresco.webservice.types.Predicate predicate, java.lang.String owner) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Getsl aa the authorities avaialble in the repository.
     */
    public java.lang.String[] getAllAuthorities(org.alfresco.webservice.accesscontrol.AuthorityFilter filter) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Ges the authorities for the current user
     */
    public java.lang.String[] getAuthorities() throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Creates new authorities
     */
    public java.lang.String[] createAuthorities(java.lang.String parentAuthority, org.alfresco.webservice.accesscontrol.NewAuthority[] newAuthorites) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Delete authorities
     */
    public void deleteAuthorities(java.lang.String[] authorities) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Adds child authorities to a specified parent authority
     */
    public java.lang.String[] addChildAuthorities(java.lang.String parentAuthority, java.lang.String[] authorities) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Remove child authorities from the specified parent authority
     */
    public void removeChildAuthorities(java.lang.String parentAuthority, java.lang.String[] authorities) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Gets an authorities child authorities
     */
    public java.lang.String[] getChildAuthorities(java.lang.String authority, org.alfresco.webservice.accesscontrol.SiblingAuthorityFilter filter) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;

    /**
     * Gets an authorities parent authorities
     */
    public java.lang.String[] getParentAuthorities(java.lang.String authority, org.alfresco.webservice.accesscontrol.SiblingAuthorityFilter filter) throws java.rmi.RemoteException, org.alfresco.webservice.accesscontrol.AccessControlFault;
}
