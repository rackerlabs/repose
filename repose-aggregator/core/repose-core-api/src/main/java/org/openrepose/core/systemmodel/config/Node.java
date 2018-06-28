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
import javax.xml.bind.annotation.XmlType;


/**
 *
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;html:p xmlns:html="http://www.w3.org/1999/xhtml" xmlns:jaxb="http://java.sun.com/xml/ns/jaxb" xmlns:mod="http://docs.openrepose.org/repose/system-model/v2.0" xmlns:saxon="http://saxon.sf.net/" xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning" xmlns:xerces="http://xerces.apache.org" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;Defines a single host in the system model&lt;/html:p&gt;
 * </pre>
 *
 *
 * <p>Java class for Node complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Node">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="hostname" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="http-port" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 *       &lt;attribute name="https-port" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Node")
public class Node
    implements Serializable
{

    private final static long serialVersionUID = 1530213507742L;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "hostname", required = true)
    protected String hostname;
    @XmlAttribute(name = "http-port")
    protected Integer httpPort;
    @XmlAttribute(name = "https-port")
    protected Integer httpsPort;

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
     * Gets the value of the hostname property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets the value of the hostname property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setHostname(String value) {
        this.hostname = value;
    }

    /**
     * Gets the value of the httpPort property.
     *
     * @return
     *     possible object is
     *     {@link Integer }
     *
     */
    public int getHttpPort() {
        if (httpPort == null) {
            return  0;
        } else {
            return httpPort;
        }
    }

    /**
     * Sets the value of the httpPort property.
     *
     * @param value
     *     allowed object is
     *     {@link Integer }
     *
     */
    public void setHttpPort(Integer value) {
        this.httpPort = value;
    }

    /**
     * Gets the value of the httpsPort property.
     *
     * @return
     *     possible object is
     *     {@link Integer }
     *
     */
    public int getHttpsPort() {
        if (httpsPort == null) {
            return  0;
        } else {
            return httpsPort;
        }
    }

    /**
     * Sets the value of the httpsPort property.
     *
     * @param value
     *     allowed object is
     *     {@link Integer }
     *
     */
    public void setHttpsPort(Integer value) {
        this.httpsPort = value;
    }

}
