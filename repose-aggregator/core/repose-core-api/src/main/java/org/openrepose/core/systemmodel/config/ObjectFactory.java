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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the org.openrepose.core.systemmodel.config package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _SystemModel_QNAME = new QName("http://docs.openrepose.org/repose/system-model/v2.0", "system-model");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.openrepose.core.systemmodel.config
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SystemModel }
     */
    public SystemModel createSystemModel() {
        return new SystemModel();
    }

    /**
     * Create an instance of {@link Destination }
     */
    public Destination createDestination() {
        return new Destination();
    }

    /**
     * Create an instance of {@link NodeList }
     */
    public NodeList createNodeList() {
        return new NodeList();
    }

    /**
     * Create an instance of {@link Node }
     */
    public Node createNode() {
        return new Node();
    }

    /**
     * Create an instance of {@link Service }
     */
    public Service createService() {
        return new Service();
    }

    /**
     * Create an instance of {@link FilterList }
     */
    public FilterList createFilterList() {
        return new FilterList();
    }

    /**
     * Create an instance of {@link DestinationList }
     */
    public DestinationList createDestinationList() {
        return new DestinationList();
    }

    /**
     * Create an instance of {@link Filter }
     */
    public Filter createFilter() {
        return new Filter();
    }

    /**
     * Create an instance of {@link ServicesList }
     */
    public ServicesList createServicesList() {
        return new ServicesList();
    }

    /**
     * Create an instance of {@link TracingHeaderConfig }
     */
    public TracingHeaderConfig createTracingHeaderConfig() {
        return new TracingHeaderConfig();
    }

    /**
     * Create an instance of {@link PhoneHomeServiceConfig }
     */
    public PhoneHomeServiceConfig createPhoneHomeServiceConfig() {
        return new PhoneHomeServiceConfig();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SystemModel }{@code >}}
     */
    @XmlElementDecl(namespace = "http://docs.openrepose.org/repose/system-model/v2.0", name = "system-model")
    public JAXBElement<SystemModel> createSystemModel(SystemModel value) {
        return new JAXBElement<>(_SystemModel_QNAME, SystemModel.class, null, value);
    }
}
