//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.12.08 at 12:37:26 PM GMT+05:30 
//


package org.apache.airavat.entity.v0;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for inputType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="inputType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="feed" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="start-instance" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="end-instance" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="input" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "inputType", propOrder = {
    "value"
})
public class InputType {

    @XmlValue
    protected String value;
    @XmlAttribute
    protected String id;
    @XmlAttribute
    protected String feed;
    @XmlAttribute(name = "start-instance")
    protected String startInstance;
    @XmlAttribute(name = "end-instance")
    protected String endInstance;
    @XmlAttribute
    protected String name;
    @XmlAttribute
    protected String input;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the feed property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFeed() {
        return feed;
    }

    /**
     * Sets the value of the feed property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFeed(String value) {
        this.feed = value;
    }

    /**
     * Gets the value of the startInstance property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStartInstance() {
        return startInstance;
    }

    /**
     * Sets the value of the startInstance property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStartInstance(String value) {
        this.startInstance = value;
    }

    /**
     * Gets the value of the endInstance property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEndInstance() {
        return endInstance;
    }

    /**
     * Sets the value of the endInstance property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEndInstance(String value) {
        this.endInstance = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the input property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInput() {
        return input;
    }

    /**
     * Sets the value of the input property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInput(String value) {
        this.input = value;
    }

}
