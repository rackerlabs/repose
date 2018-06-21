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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 *
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;html:p xmlns:html="http://www.w3.org/1999/xhtml" xmlns:jaxb="http://java.sun.com/xml/ns/jaxb" xmlns:mod="http://docs.openrepose.org/repose/system-model/v2.0" xmlns:saxon="http://saxon.sf.net/" xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning" xmlns:xerces="http://xerces.apache.org" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;
 *                     Defines a single service cluster in the system model. A service cluster is a collection of nodes
 *                     that provide equivalent functionality. If a service cluster represents a repose cluster, then
 *                     it will contain a filter list and a destination list.
 *                 &lt;/html:p&gt;
 * </pre>
 *
 *
 * <p>Java class for ReposeCluster complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ReposeCluster">
 *   &lt;complexContent>
 *     &lt;extension base="{http://docs.openrepose.org/repose/system-model/v2.0}Cluster">
 *       &lt;sequence>
 *         &lt;element name="filters" type="{http://docs.openrepose.org/repose/system-model/v2.0}FilterList" minOccurs="0"/>
 *         &lt;element name="services" type="{http://docs.openrepose.org/repose/system-model/v2.0}ServicesList" minOccurs="0"/>
 *         &lt;element name="destinations" type="{http://docs.openrepose.org/repose/system-model/v2.0}DestinationList"/>
 *       &lt;/sequence>
 *       &lt;attribute name="rewrite-host-header" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReposeCluster", propOrder = {
    "filters",
    "services",
    "destinations"
})
public class ReposeCluster
    extends Cluster
    implements Serializable
{

    private final static long serialVersionUID = 100L;
    protected FilterList filters;
    protected ServicesList services;
    @XmlElement(required = true)
    protected DestinationList destinations;
    @XmlAttribute(name = "rewrite-host-header")
    protected Boolean rewriteHostHeader;

    /**
     * Gets the value of the filters property.
     *
     * @return
     *     possible object is
     *     {@link FilterList }
     *
     */
    public FilterList getFilters() {
        return filters;
    }

    /**
     * Sets the value of the filters property.
     *
     * @param value
     *     allowed object is
     *     {@link FilterList }
     *
     */
    public void setFilters(FilterList value) {
        this.filters = value;
    }

    /**
     * Gets the value of the services property.
     *
     * @return
     *     possible object is
     *     {@link ServicesList }
     *
     */
    public ServicesList getServices() {
        return services;
    }

    /**
     * Sets the value of the services property.
     *
     * @param value
     *     allowed object is
     *     {@link ServicesList }
     *
     */
    public void setServices(ServicesList value) {
        this.services = value;
    }

    /**
     * Gets the value of the destinations property.
     *
     * @return
     *     possible object is
     *     {@link DestinationList }
     *
     */
    public DestinationList getDestinations() {
        return destinations;
    }

    /**
     * Sets the value of the destinations property.
     *
     * @param value
     *     allowed object is
     *     {@link DestinationList }
     *
     */
    public void setDestinations(DestinationList value) {
        this.destinations = value;
    }

    /**
     * Gets the value of the rewriteHostHeader property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    public boolean isRewriteHostHeader() {
        if (rewriteHostHeader == null) {
            return true;
        } else {
            return rewriteHostHeader;
        }
    }

    /**
     * Sets the value of the rewriteHostHeader property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    public void setRewriteHostHeader(Boolean value) {
        this.rewriteHostHeader = value;
    }

}
