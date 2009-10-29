
package org.alfresco.repo.cmis.ws;

import java.util.ArrayList;
import java.util.List;
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
 *         &lt;element name="properties" type="{http://docs.oasis-open.org/ns/cmis/core/200901}cmisPropertiesType"/>
 *         &lt;element name="sourceObjectId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="targetObjectId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="applyPolicies" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="addACEs" type="{http://docs.oasis-open.org/ns/cmis/core/200901}cmisAccessControlListType" minOccurs="0"/>
 *         &lt;element name="removeACEs" type="{http://docs.oasis-open.org/ns/cmis/core/200901}cmisAccessControlListType" minOccurs="0"/>
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
    "properties",
    "sourceObjectId",
    "targetObjectId",
    "applyPolicies",
    "addACEs",
    "removeACEs"
})
@XmlRootElement(name = "createRelationship")
public class CreateRelationship {

    @XmlElement(required = true)
    protected String repositoryId;
    @XmlElement(required = true)
    protected CmisPropertiesType properties;
    @XmlElement(required = true)
    protected String sourceObjectId;
    @XmlElement(required = true)
    protected String targetObjectId;
    @XmlElement(nillable = true)
    protected List<String> applyPolicies;
    @XmlElementRef(name = "addACEs", namespace = "http://docs.oasis-open.org/ns/cmis/messaging/200901", type = JAXBElement.class)
    protected JAXBElement<CmisAccessControlListType> addACEs;
    @XmlElementRef(name = "removeACEs", namespace = "http://docs.oasis-open.org/ns/cmis/messaging/200901", type = JAXBElement.class)
    protected JAXBElement<CmisAccessControlListType> removeACEs;

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
     * Gets the value of the properties property.
     * 
     * @return
     *     possible object is
     *     {@link CmisPropertiesType }
     *     
     */
    public CmisPropertiesType getProperties() {
        return properties;
    }

    /**
     * Sets the value of the properties property.
     * 
     * @param value
     *     allowed object is
     *     {@link CmisPropertiesType }
     *     
     */
    public void setProperties(CmisPropertiesType value) {
        this.properties = value;
    }

    /**
     * Gets the value of the sourceObjectId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSourceObjectId() {
        return sourceObjectId;
    }

    /**
     * Sets the value of the sourceObjectId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSourceObjectId(String value) {
        this.sourceObjectId = value;
    }

    /**
     * Gets the value of the targetObjectId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTargetObjectId() {
        return targetObjectId;
    }

    /**
     * Sets the value of the targetObjectId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTargetObjectId(String value) {
        this.targetObjectId = value;
    }

    /**
     * Gets the value of the applyPolicies property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the applyPolicies property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getApplyPolicies().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getApplyPolicies() {
        if (applyPolicies == null) {
            applyPolicies = new ArrayList<String>();
        }
        return this.applyPolicies;
    }

    /**
     * Gets the value of the addACEs property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link CmisAccessControlListType }{@code >}
     *     
     */
    public JAXBElement<CmisAccessControlListType> getAddACEs() {
        return addACEs;
    }

    /**
     * Sets the value of the addACEs property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link CmisAccessControlListType }{@code >}
     *     
     */
    public void setAddACEs(JAXBElement<CmisAccessControlListType> value) {
        this.addACEs = ((JAXBElement<CmisAccessControlListType> ) value);
    }

    /**
     * Gets the value of the removeACEs property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link CmisAccessControlListType }{@code >}
     *     
     */
    public JAXBElement<CmisAccessControlListType> getRemoveACEs() {
        return removeACEs;
    }

    /**
     * Sets the value of the removeACEs property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link CmisAccessControlListType }{@code >}
     *     
     */
    public void setRemoveACEs(JAXBElement<CmisAccessControlListType> value) {
        this.removeACEs = ((JAXBElement<CmisAccessControlListType> ) value);
    }

}
