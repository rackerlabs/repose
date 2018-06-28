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

import javax.xml.bind.annotation.*;
import java.io.Serializable;


/**
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;html:p xmlns:html="http://www.w3.org/1999/xhtml" xmlns:jaxb="http://java.sun.com/xml/ns/jaxb" xmlns:mod="http://docs.openrepose.org/repose/system-model/v2.0" xmlns:saxon="http://saxon.sf.net/" xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning" xmlns:xerces="http://xerces.apache.org" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;
 *                     If present, enables the phone home service which will collect Repose usage data and send it to a
 *                     centralized data collection service.
 *                     If not present, will have no effect.
 *                 &lt;/html:p&gt;
 * </pre>
 *
 *
 * <p>Java class for PhoneHomeServiceConfig complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="PhoneHomeServiceConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="enabled" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="collection-uri" type="{http://www.w3.org/2001/XMLSchema}anyURI" default="http://phone-home.openrepose.org" />
 *       &lt;attribute name="origin-service-id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="contact-email" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PhoneHomeServiceConfig")
public class PhoneHomeServiceConfig
    implements Serializable {

    private final static long serialVersionUID = 1530213507742L;
    @XmlAttribute(name = "enabled", required = true)
    protected boolean enabled;
    @XmlAttribute(name = "collection-uri")
    @XmlSchemaType(name = "anyURI")
    protected String collectionUri;
    @XmlAttribute(name = "origin-service-id")
    protected String originServiceId;
    @XmlAttribute(name = "contact-email")
    protected String contactEmail;

    /**
     * Gets the value of the enabled property.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the value of the enabled property.
     */
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    /**
     * Gets the value of the collectionUri property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCollectionUri() {
        if (collectionUri == null) {
            return "http://phone-home.openrepose.org";
        } else {
            return collectionUri;
        }
    }

    /**
     * Sets the value of the collectionUri property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCollectionUri(String value) {
        this.collectionUri = value;
    }

    /**
     * Gets the value of the originServiceId property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getOriginServiceId() {
        return originServiceId;
    }

    /**
     * Sets the value of the originServiceId property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setOriginServiceId(String value) {
        this.originServiceId = value;
    }

    /**
     * Gets the value of the contactEmail property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getContactEmail() {
        return contactEmail;
    }

    /**
     * Sets the value of the contactEmail property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setContactEmail(String value) {
        this.contactEmail = value;
    }

}
