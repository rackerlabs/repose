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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;html:p xmlns:html="http://www.w3.org/1999/xhtml" xmlns:jaxb="http://java.sun.com/xml/ns/jaxb" xmlns:mod="http://docs.openrepose.org/repose/system-model/v2.0" xmlns:saxon="http://saxon.sf.net/" xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning" xmlns:xerces="http://xerces.apache.org" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;Top level element for defining a power proxy system model&lt;/html:p&gt;
 * </pre>
 *
 *
 * <p>Java class for SystemModel complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SystemModel">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="repose-cluster" type="{http://docs.openrepose.org/repose/system-model/v2.0}ReposeCluster" maxOccurs="unbounded"/>
 *         &lt;element name="service-cluster" type="{http://docs.openrepose.org/repose/system-model/v2.0}Cluster" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="phone-home" type="{http://docs.openrepose.org/repose/system-model/v2.0}PhoneHomeServiceConfig" minOccurs="0"/>
 *         &lt;element name="tracing-header" type="{http://docs.openrepose.org/repose/system-model/v2.0}TracingHeaderConfig" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SystemModel", propOrder = {
    "reposeCluster",
    "serviceCluster",
    "phoneHome",
    "tracingHeader"
})
public class SystemModel
    implements Serializable {

    private final static long serialVersionUID = 1530213507742L;
    @XmlElement(name = "repose-cluster", required = true)
    protected List<ReposeCluster> reposeCluster;
    @XmlElement(name = "service-cluster")
    protected List<Cluster> serviceCluster;
    @XmlElement(name = "phone-home")
    protected PhoneHomeServiceConfig phoneHome;
    @XmlElement(name = "tracing-header")
    protected TracingHeaderConfig tracingHeader;

    /**
     * Gets the value of the reposeCluster property.
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the reposeCluster property.
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReposeCluster().add(newItem);
     * </pre>
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ReposeCluster }
     */
    public List<ReposeCluster> getReposeCluster() {
        if (reposeCluster == null) {
            reposeCluster = new ArrayList<ReposeCluster>();
        }
        return this.reposeCluster;
    }

    /**
     * Gets the value of the serviceCluster property.
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the serviceCluster property.
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getServiceCluster().add(newItem);
     * </pre>
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Cluster }
     */
    public List<Cluster> getServiceCluster() {
        if (serviceCluster == null) {
            serviceCluster = new ArrayList<Cluster>();
        }
        return this.serviceCluster;
    }

    /**
     * Gets the value of the phoneHome property.
     *
     * @return possible object is
     * {@link PhoneHomeServiceConfig }
     */
    public PhoneHomeServiceConfig getPhoneHome() {
        return phoneHome;
    }

    /**
     * Sets the value of the phoneHome property.
     *
     * @param value allowed object is
     *              {@link PhoneHomeServiceConfig }
     */
    public void setPhoneHome(PhoneHomeServiceConfig value) {
        this.phoneHome = value;
    }

    /**
     * Gets the value of the tracingHeader property.
     *
     * @return possible object is
     * {@link TracingHeaderConfig }
     */
    public TracingHeaderConfig getTracingHeader() {
        return tracingHeader;
    }

    /**
     * Sets the value of the tracingHeader property.
     *
     * @param value allowed object is
     *              {@link TracingHeaderConfig }
     */
    public void setTracingHeader(TracingHeaderConfig value) {
        this.tracingHeader = value;
    }

}
