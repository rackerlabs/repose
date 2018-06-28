/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.systemmodel.config;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 *
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;html:p xmlns:html="http://www.w3.org/1999/xhtml" xmlns:jaxb="http://java.sun.com/xml/ns/jaxb" xmlns:mod="http://docs.openrepose.org/repose/system-model/v2.0" xmlns:saxon="http://saxon.sf.net/" xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning" xmlns:xerces="http://xerces.apache.org" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;Defines a filter that can be used to process and route requests&lt;/html:p&gt;
 * </pre>
 *
 *
 * <p>Java class for Filter complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Filter">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice minOccurs="0">
 *         &lt;element name="methods" type="{http://docs.openrepose.org/repose/system-model/v2.0}Methods"/>
 *         &lt;element name="header" type="{http://docs.openrepose.org/repose/system-model/v2.0}Header"/>
 *         &lt;element name="uri" type="{http://docs.openrepose.org/repose/system-model/v2.0}Uri"/>
 *         &lt;element name="and" type="{http://docs.openrepose.org/repose/system-model/v2.0}And"/>
 *         &lt;element name="not" type="{http://docs.openrepose.org/repose/system-model/v2.0}Not"/>
 *         &lt;element name="or" type="{http://docs.openrepose.org/repose/system-model/v2.0}Or"/>
 *       &lt;/choice>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="configuration" type="{http://www.w3.org/2001/XMLSchema}anyURI" default="" />
 *       &lt;attribute name="uri-regex" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Filter", propOrder = {
    "methods",
    "header",
    "uri",
    "and",
    "not",
    "or"
})
public class Filter
    implements Serializable
{

    private final static long serialVersionUID = 1530213507742L;
    protected Methods methods;
    protected Header header;
    protected Uri uri;
    protected And and;
    protected Not not;
    protected Or or;
    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "configuration")
    @XmlSchemaType(name = "anyURI")
    protected String configuration;
    @XmlAttribute(name = "uri-regex")
    protected String uriRegex;

    /**
     * Gets the value of the methods property.
     *
     * @return
     *     possible object is
     *     {@link Methods }
     *
     */
    public Methods getMethods() {
        return methods;
    }

    /**
     * Sets the value of the methods property.
     *
     * @param value
     *     allowed object is
     *     {@link Methods }
     *
     */
    public void setMethods(Methods value) {
        this.methods = value;
    }

    /**
     * Gets the value of the header property.
     *
     * @return
     *     possible object is
     *     {@link Header }
     *
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Sets the value of the header property.
     *
     * @param value
     *     allowed object is
     *     {@link Header }
     *
     */
    public void setHeader(Header value) {
        this.header = value;
    }

    /**
     * Gets the value of the uri property.
     *
     * @return
     *     possible object is
     *     {@link Uri }
     *
     */
    public Uri getUri() {
        return uri;
    }

    /**
     * Sets the value of the uri property.
     *
     * @param value
     *     allowed object is
     *     {@link Uri }
     *
     */
    public void setUri(Uri value) {
        this.uri = value;
    }

    /**
     * Gets the value of the and property.
     *
     * @return
     *     possible object is
     *     {@link And }
     *
     */
    public And getAnd() {
        return and;
    }

    /**
     * Sets the value of the and property.
     *
     * @param value
     *     allowed object is
     *     {@link And }
     *
     */
    public void setAnd(And value) {
        this.and = value;
    }

    /**
     * Gets the value of the not property.
     *
     * @return
     *     possible object is
     *     {@link Not }
     *
     */
    public Not getNot() {
        return not;
    }

    /**
     * Sets the value of the not property.
     *
     * @param value
     *     allowed object is
     *     {@link Not }
     *
     */
    public void setNot(Not value) {
        this.not = value;
    }

    /**
     * Gets the value of the or property.
     *
     * @return
     *     possible object is
     *     {@link Or }
     *
     */
    public Or getOr() {
        return or;
    }

    /**
     * Sets the value of the or property.
     *
     * @param value
     *     allowed object is
     *     {@link Or }
     *
     */
    public void setOr(Or value) {
        this.or = value;
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
     * Gets the value of the configuration property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getConfiguration() {
        if (configuration == null) {
            return "";
        } else {
            return configuration;
        }
    }

    /**
     * Sets the value of the configuration property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setConfiguration(String value) {
        this.configuration = value;
    }

    /**
     * Gets the value of the uriRegex property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUriRegex() {
        return uriRegex;
    }

    /**
     * Sets the value of the uriRegex property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUriRegex(String value) {
        this.uriRegex = value;
    }

}
