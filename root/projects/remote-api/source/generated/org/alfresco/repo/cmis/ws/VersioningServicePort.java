
package org.alfresco.repo.cmis.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * This class was generated by Apache CXF 2.0.6
 * Wed Jul 23 17:17:31 EEST 2008
 * Generated source version: 2.0.6
 * 
 */

@WebService(targetNamespace = "http://www.cmis.org/ns/1.0", name = "VersioningServicePort")

public interface VersioningServicePort {

    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    @WebResult(name = "checkInResponse", targetNamespace = "http://www.cmis.org/ns/1.0", partName = "parameters")
    @WebMethod
    public org.alfresco.repo.cmis.ws.CheckInResponse checkIn(
        @WebParam(partName = "parameters", name = "checkIn", targetNamespace = "http://www.cmis.org/ns/1.0")
        CheckIn parameters
    ) throws RuntimeException, InvalidArgumentException, ObjectNotFoundException, StorageException, ConstraintViolationException, OperationNotSupportedException, UpdateConflictException, StreamNotSupportedException, PermissionDeniedException;

    @ResponseWrapper(localName = "cancelCheckOutResponse", targetNamespace = "http://www.cmis.org/ns/1.0", className = "org.alfresco.repo.cmis.ws.CancelCheckOutResponse")
    @RequestWrapper(localName = "cancelCheckOut", targetNamespace = "http://www.cmis.org/ns/1.0", className = "org.alfresco.repo.cmis.ws.CancelCheckOut")
    @WebMethod
    public void cancelCheckOut(
        @WebParam(name = "repositoryId", targetNamespace = "http://www.cmis.org/ns/1.0")
        java.lang.String repositoryId,
        @WebParam(name = "documentId", targetNamespace = "http://www.cmis.org/ns/1.0")
        java.lang.String documentId
    ) throws RuntimeException, InvalidArgumentException, ObjectNotFoundException, OperationNotSupportedException, UpdateConflictException, PermissionDeniedException;

    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    @WebResult(name = "getAllVersionsResponse", targetNamespace = "http://www.cmis.org/ns/1.0", partName = "parameters")
    @WebMethod
    public org.alfresco.repo.cmis.ws.GetAllVersionsResponse getAllVersions(
        @WebParam(partName = "parameters", name = "getAllVersions", targetNamespace = "http://www.cmis.org/ns/1.0")
        GetAllVersions parameters
    ) throws RuntimeException, InvalidArgumentException, ObjectNotFoundException, ConstraintViolationException, FilterNotValidException, OperationNotSupportedException, UpdateConflictException, PermissionDeniedException;

    @ResponseWrapper(localName = "deleteAllVersionsResponse", targetNamespace = "http://www.cmis.org/ns/1.0", className = "org.alfresco.repo.cmis.ws.DeleteAllVersionsResponse")
    @RequestWrapper(localName = "deleteAllVersions", targetNamespace = "http://www.cmis.org/ns/1.0", className = "org.alfresco.repo.cmis.ws.DeleteAllVersions")
    @WebMethod
    public void deleteAllVersions(
        @WebParam(name = "repositoryId", targetNamespace = "http://www.cmis.org/ns/1.0")
        java.lang.String repositoryId,
        @WebParam(name = "versionSeriesId", targetNamespace = "http://www.cmis.org/ns/1.0")
        java.lang.String versionSeriesId
    ) throws RuntimeException, InvalidArgumentException, ObjectNotFoundException, ConstraintViolationException, OperationNotSupportedException, UpdateConflictException, PermissionDeniedException;

    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    @WebResult(name = "getPropertiesOfLatestVersionResponse", targetNamespace = "http://www.cmis.org/ns/1.0", partName = "parameters")
    @WebMethod
    public org.alfresco.repo.cmis.ws.GetPropertiesOfLatestVersionResponse getPropertiesOfLatestVersion(
        @WebParam(partName = "parameters", name = "getPropertiesOfLatestVersion", targetNamespace = "http://www.cmis.org/ns/1.0")
        GetPropertiesOfLatestVersion parameters
    ) throws RuntimeException, InvalidArgumentException, ObjectNotFoundException, FilterNotValidException, OperationNotSupportedException, UpdateConflictException, PermissionDeniedException;

    @ResponseWrapper(localName = "checkOutResponse", targetNamespace = "http://www.cmis.org/ns/1.0", className = "org.alfresco.repo.cmis.ws.CheckOutResponse")
    @RequestWrapper(localName = "checkOut", targetNamespace = "http://www.cmis.org/ns/1.0", className = "org.alfresco.repo.cmis.ws.CheckOut")
    @WebMethod
    public void checkOut(
        @WebParam(name = "repositoryId", targetNamespace = "http://www.cmis.org/ns/1.0")
        java.lang.String repositoryId,
        @WebParam(mode = WebParam.Mode.INOUT, name = "documentId", targetNamespace = "http://www.cmis.org/ns/1.0")
        javax.xml.ws.Holder<java.lang.String> documentId,
        @WebParam(mode = WebParam.Mode.OUT, name = "contentCopied", targetNamespace = "http://www.cmis.org/ns/1.0")
        javax.xml.ws.Holder<java.lang.Boolean> contentCopied
    ) throws RuntimeException, InvalidArgumentException, ObjectNotFoundException, ConstraintViolationException, OperationNotSupportedException, UpdateConflictException, PermissionDeniedException;
}