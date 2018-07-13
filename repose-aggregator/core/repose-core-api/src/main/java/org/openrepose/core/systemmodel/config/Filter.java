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

import lombok.Data;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.Optional;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Filter", propOrder = {
    "filterCriterion"
})
@Data
public class Filter
    implements Serializable {
    @XmlTransient
    private static final Logger LOG = LoggerFactory.getLogger(Filter.class);
    @XmlTransient
    private static final FilterCriterion DEFAULT_FILTER_CRITERION = new FilterCriterion() {
        @Override
        public boolean evaluate(HttpServletRequestWrapper httpServletRequestWrapper) {
            LOG.trace("The default filter criterion is returning true.");
            return true;
        }
    };
    @XmlElements({
        @XmlElement(name = "methods", type = Methods.class),
        @XmlElement(name = "header", type = Header.class),
        @XmlElement(name = "uri", type = Uri.class),
        @XmlElement(name = "and", type = And.class),
        @XmlElement(name = "not", type = Not.class),
        @XmlElement(name = "or", type = Or.class)
    })
    private FilterCriterion filterCriterion = DEFAULT_FILTER_CRITERION;
    @XmlAttribute(name = "id")
    private String id;
    @XmlAttribute(name = "name", required = true)
    private String name;
    @XmlAttribute(name = "configuration")
    @XmlSchemaType(name = "anyURI")
    private String configuration = "";
    @XmlAttribute(name = "uri-regex")
    private String uriRegex;

    public FilterCriterion getFilterCriterion() {
        // TODO: Remove this when the deprecated uri-regex attribute is deleted.
        if (uriRegex != null) {
            Uri uriFilterCriterion = new Uri();
            uriFilterCriterion.setRegex(uriRegex);
            return uriFilterCriterion;
        }
        return filterCriterion;
    }
}
