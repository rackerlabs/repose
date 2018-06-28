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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;


/**
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;html:p xmlns:html="http://www.w3.org/1999/xhtml" xmlns:jaxb="http://java.sun.com/xml/ns/jaxb" xmlns:mod="http://docs.openrepose.org/repose/system-model/v2.0" xmlns:saxon="http://saxon.sf.net/" xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning" xmlns:xerces="http://xerces.apache.org" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;Defines a single node that is a target destination reachable from a cluster&lt;/html:p&gt;
 * </pre>
 *
 *
 * <p>Java class for DestinationEndpoint complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="DestinationEndpoint">
 *   &lt;complexContent>
 *     &lt;extension base="{http://docs.openrepose.org/repose/system-model/v2.0}Destination">
 *       &lt;attribute name="hostname" type="{http://www.w3.org/2001/XMLSchema}string" default="localhost" />
 *       &lt;attribute name="port" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DestinationEndpoint")
public class DestinationEndpoint
    extends Destination
    implements Serializable {

    private final static long serialVersionUID = 1530213507742L;
    @XmlAttribute(name = "hostname")
    protected String hostname;
    @XmlAttribute(name = "port")
    protected Integer port;

    /**
     * Gets the value of the hostname property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getHostname() {
        if (hostname == null) {
            return "localhost";
        } else {
            return hostname;
        }
    }

    /**
     * Sets the value of the hostname property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setHostname(String value) {
        this.hostname = value;
    }

    /**
     * Gets the value of the port property.
     *
     * @return possible object is
     * {@link Integer }
     */
    public int getPort() {
        if (port == null) {
            return 0;
        } else {
            return port;
        }
    }

    /**
     * Sets the value of the port property.
     *
     * @param value allowed object is
     *              {@link Integer }
     */
    public void setPort(Integer value) {
        this.port = value;
    }

}
