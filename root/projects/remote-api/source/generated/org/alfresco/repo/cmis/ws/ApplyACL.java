
package org.alfresco.repo.cmis.ws;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="repositoryId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="objectId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="addACEs" type="{http://docs.oasis-open.org/ns/cmis/core/200901}cmisAccessControlListType"/>
 *         &lt;element name="removeACEs" type="{http://docs.oasis-open.org/ns/cmis/core/200901}cmisAccessControlListType"/>
 *         &lt;element name="propogationType" type="{http://docs.oasis-open.org/ns/cmis/core/200901}enumACLPropagation" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "repositoryId",
    "objectId",
    "addACEs",
    "removeACEs",
    "propogationType"
})
@XmlRootElement(name = "applyACL")
public class ApplyACL {

    @XmlElement(required = true)
    protected String repositoryId;
    @XmlElement(required = true)
    protected String objectId;
    @XmlElement(required = true)
    protected CmisAccessControlListType addACEs;
    @XmlElement(required = true)
    protected CmisAccessControlListType removeACEs;
    @XmlElementRef(name = "propogationType", namespace = "http://docs.oasis-open.org/ns/cmis/messaging/200901", type = JAXBElement.class)
    protected JAXBElement<EnumACLPropagation> propogationType;

    /**
     * Gets the value of the repositoryId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRepositoryId() {
        return repositoryId;
    }

    /**
     * Sets the value of the repositoryId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRepositoryId(String value) {
        this.repositoryId = value;
    }

    /**
     * Gets the value of the objectId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * Sets the value of the objectId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setObjectId(String value) {
        this.objectId = value;
    }

    /**
     * Gets the value of the addACEs property.
     * 
     * @return
     *     possible object is
     *     {@link CmisAccessControlListType }
     *     
     */
    public CmisAccessControlListType getAddACEs() {
        return addACEs;
    }

    /**
     * Sets the value of the addACEs property.
     * 
     * @param value
     *     allowed object is
     *     {@link CmisAccessControlListType }
     *     
     */
    public void setAddACEs(CmisAccessControlListType value) {
        this.addACEs = value;
    }

    /**
     * Gets the value of the removeACEs property.
     * 
     * @return
     *     possible object is
     *     {@link CmisAccessControlListType }
     *     
     */
    public CmisAccessControlListType getRemoveACEs() {
        return removeACEs;
    }

    /**
     * Sets the value of the removeACEs property.
     * 
     * @param value
     *     allowed object is
     *     {@link CmisAccessControlListType }
     *     
     */
    public void setRemoveACEs(CmisAccessControlListType value) {
        this.removeACEs = value;
    }

    /**
     * Gets the value of the propogationType property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link EnumACLPropagation }{@code >}
     *     
     */
    public JAXBElement<EnumACLPropagation> getPropogationType() {
        return propogationType;
    }

    /**
     * Sets the value of the propogationType property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link EnumACLPropagation }{@code >}
     *     
     */
    public void setPropogationType(JAXBElement<EnumACLPropagation> value) {
        this.propogationType = ((JAXBElement<EnumACLPropagation> ) value);
    }

}
