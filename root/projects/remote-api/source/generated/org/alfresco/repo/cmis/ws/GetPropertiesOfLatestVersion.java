
package org.alfresco.repo.cmis.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getPropertiesOfLatestVersion element declaration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;element name="getPropertiesOfLatestVersion">
 *   &lt;complexType>
 *     &lt;complexContent>
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *         &lt;sequence>
 *           &lt;element name="repositoryId" type="{http://www.cmis.org/ns/1.0}ID"/>
 *           &lt;element name="versionSeriesId" type="{http://www.cmis.org/ns/1.0}ID"/>
 *           &lt;element name="majorVersion" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *           &lt;element ref="{http://www.cmis.org/ns/1.0}filter" minOccurs="0"/>
 *         &lt;/sequence>
 *       &lt;/restriction>
 *     &lt;/complexContent>
 *   &lt;/complexType>
 * &lt;/element>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "repositoryId",
    "versionSeriesId",
    "majorVersion",
    "filter"
})
@XmlRootElement(name = "getPropertiesOfLatestVersion")
public class GetPropertiesOfLatestVersion {

    @XmlElement(namespace = "http://www.cmis.org/ns/1.0", required = true)
    protected String repositoryId;
    @XmlElement(namespace = "http://www.cmis.org/ns/1.0", required = true)
    protected String versionSeriesId;
    @XmlElement(namespace = "http://www.cmis.org/ns/1.0")
    protected Boolean majorVersion;
    @XmlElement(namespace = "http://www.cmis.org/ns/1.0")
    protected String filter;

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
     * Gets the value of the versionSeriesId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersionSeriesId() {
        return versionSeriesId;
    }

    /**
     * Sets the value of the versionSeriesId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersionSeriesId(String value) {
        this.versionSeriesId = value;
    }

    /**
     * Gets the value of the majorVersion property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isMajorVersion() {
        return majorVersion;
    }

    /**
     * Sets the value of the majorVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setMajorVersion(Boolean value) {
        this.majorVersion = value;
    }

    /**
     * Gets the value of the filter property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFilter() {
        return filter;
    }

    /**
     * Sets the value of the filter property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFilter(String value) {
        this.filter = value;
    }

}