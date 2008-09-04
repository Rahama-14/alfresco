
package org.alfresco.repo.cmis.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;

/**
 * This class was generated by Apache CXF 2.0.6
 * Tue Jul 29 18:22:39 EEST 2008
 * Generated source version: 2.0.6
 * 
 */

@WebService(targetNamespace = "http://www.cmis.org/ns/1.0", name = "RelationshipServicePort")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)

public interface RelationshipServicePort {

    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    @WebResult(name = "getRelationshipsResponse", targetNamespace = "http://www.cmis.org/ns/1.0", partName = "parameters")
    @WebMethod
    public org.alfresco.repo.cmis.ws.GetRelationshipsResponse getRelationships(
        @WebParam(partName = "parameters", name = "getRelationships", targetNamespace = "http://www.cmis.org/ns/1.0")
        GetRelationships parameters
    ) throws RuntimeException, InvalidArgumentException, TypeNotFoundException, ObjectNotFoundException, ConstraintViolationException, FilterNotValidException, OperationNotSupportedException, UpdateConflictException, PermissionDeniedException;
}