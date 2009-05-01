
package org.alfresco.repo.cmis.ws;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for enumServiceException.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="enumServiceException">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="constraint"/>
 *     &lt;enumeration value="contentAlreadyExists"/>
 *     &lt;enumeration value="filterNotValid"/>
 *     &lt;enumeration value="folderNotValid"/>
 *     &lt;enumeration value="invalidArgument"/>
 *     &lt;enumeration value="notInFolder"/>
 *     &lt;enumeration value="objectNotFound"/>
 *     &lt;enumeration value="offset"/>
 *     &lt;enumeration value="notSupported"/>
 *     &lt;enumeration value="permissionDenied"/>
 *     &lt;enumeration value="runtime"/>
 *     &lt;enumeration value="storage"/>
 *     &lt;enumeration value="streamNotSupported"/>
 *     &lt;enumeration value="type"/>
 *     &lt;enumeration value="updateConflict"/>
 *     &lt;enumeration value="versioning"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "enumServiceException")
@XmlEnum
public enum EnumServiceException {

    @XmlEnumValue("constraint")
    CONSTRAINT("constraint"),
    @XmlEnumValue("contentAlreadyExists")
    CONTENT_ALREADY_EXISTS("contentAlreadyExists"),
    @XmlEnumValue("filterNotValid")
    FILTER_NOT_VALID("filterNotValid"),
    @XmlEnumValue("folderNotValid")
    FOLDER_NOT_VALID("folderNotValid"),
    @XmlEnumValue("invalidArgument")
    INVALID_ARGUMENT("invalidArgument"),
    @XmlEnumValue("notInFolder")
    NOT_IN_FOLDER("notInFolder"),
    @XmlEnumValue("objectNotFound")
    OBJECT_NOT_FOUND("objectNotFound"),
    @XmlEnumValue("offset")
    OFFSET("offset"),
    @XmlEnumValue("notSupported")
    NOT_SUPPORTED("notSupported"),
    @XmlEnumValue("permissionDenied")
    PERMISSION_DENIED("permissionDenied"),
    @XmlEnumValue("runtime")
    RUNTIME("runtime"),
    @XmlEnumValue("storage")
    STORAGE("storage"),
    @XmlEnumValue("streamNotSupported")
    STREAM_NOT_SUPPORTED("streamNotSupported"),
    @XmlEnumValue("type")
    TYPE("type"),
    @XmlEnumValue("updateConflict")
    UPDATE_CONFLICT("updateConflict"),
    @XmlEnumValue("versioning")
    VERSIONING("versioning");
    private final String value;

    EnumServiceException(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static EnumServiceException fromValue(String v) {
        for (EnumServiceException c: EnumServiceException.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
