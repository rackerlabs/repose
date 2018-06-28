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

import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;html:p xmlns:html="http://www.w3.org/1999/xhtml" xmlns:jaxb="http://java.sun.com/xml/ns/jaxb" xmlns:mod="http://docs.openrepose.org/repose/system-model/v2.0" xmlns:saxon="http://saxon.sf.net/" xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning" xmlns:xerces="http://xerces.apache.org" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;Defines the logical OR conditional processing element.&lt;/html:p&gt;
 * </pre>
 *
 *
 * <p>Java class for Or complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Or">
 *   &lt;complexContent>
 *     &lt;extension base="{http://docs.openrepose.org/repose/system-model/v2.0}FilterCriterion">
 *       &lt;choice maxOccurs="unbounded" minOccurs="2">
 *         &lt;element name="methods" type="{http://docs.openrepose.org/repose/system-model/v2.0}Methods"/>
 *         &lt;element name="header" type="{http://docs.openrepose.org/repose/system-model/v2.0}Header"/>
 *         &lt;element name="uri" type="{http://docs.openrepose.org/repose/system-model/v2.0}Uri"/>
 *         &lt;element name="not" type="{http://docs.openrepose.org/repose/system-model/v2.0}Not"/>
 *         &lt;element name="and" type="{http://docs.openrepose.org/repose/system-model/v2.0}And"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Or", propOrder = {
    "filterCriteria"
})
public class Or
    extends FilterCriterion
    implements Serializable {

    private final static long serialVersionUID = 1530213507742L;
    @XmlElements({
        @XmlElement(name = "methods", type = Methods.class),
        @XmlElement(name = "header", type = Header.class),
        @XmlElement(name = "uri", type = Uri.class),
        @XmlElement(name = "not", type = Not.class),
        @XmlElement(name = "and", type = And.class)
    })
    protected List<FilterCriterion> filterCriteria;

    /**
     * Gets the value of the methodsOrHeaderOrUri property.
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the methodsOrHeaderOrUri property.
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMethodsOrHeaderOrUri().add(newItem);
     * </pre>
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Methods }
     * {@link Header }
     * {@link Uri }
     * {@link Not }
     * {@link And }
     */
    public List<FilterCriterion> getFilterCriteria() {
        if (filterCriteria == null) {
            filterCriteria = new ArrayList<>();
        }
        return this.filterCriteria;
    }

    @Override
    boolean evaluate(HttpServletRequestWrapper httpServletRequestWraper) {
        return getFilterCriteria().stream()
            .anyMatch(criterion -> criterion.evaluate(httpServletRequestWraper));
    }
}
