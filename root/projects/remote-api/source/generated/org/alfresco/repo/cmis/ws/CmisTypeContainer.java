
package org.alfresco.repo.cmis.ws;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for cmisTypeContainer complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="cmisTypeContainer">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="type" type="{http://docs.oasis-open.org/ns/cmis/core/200901}cmisTypeDefinitionType"/>
 *         &lt;element name="children" type="{http://docs.oasis-open.org/ns/cmis/messaging/200901}cmisTypeContainer" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cmisTypeContainer", propOrder = {
    "type",
    "children"
})
public class CmisTypeContainer {

    @XmlElement(required = true)
    protected CmisTypeDefinitionType type;
    protected List<CmisTypeContainer> children;

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link CmisTypeDefinitionType }
     *     
     */
    public CmisTypeDefinitionType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link CmisTypeDefinitionType }
     *     
     */
    public void setType(CmisTypeDefinitionType value) {
        this.type = value;
    }

    /**
     * Gets the value of the children property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the children property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getChildren().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CmisTypeContainer }
     * 
     * 
     */
    public List<CmisTypeContainer> getChildren() {
        if (children == null) {
            children = new ArrayList<CmisTypeContainer>();
        }
        return this.children;
    }

}
