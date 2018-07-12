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
import lombok.EqualsAndHashCode;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Not", propOrder = {
    "filterCriteria"
})
@Data
@EqualsAndHashCode(callSuper = true)
public class Not
    extends FilterCriterion
    implements Serializable {
    @XmlTransient
    private static final Logger LOG = LoggerFactory.getLogger(Not.class);
    @XmlElements({
        @XmlElement(name = "methods", type = Methods.class),
        @XmlElement(name = "header", type = Header.class),
        @XmlElement(name = "uri", type = Uri.class),
        @XmlElement(name = "and", type = And.class),
        @XmlElement(name = "or", type = Or.class)
    })
    private FilterCriterion filterCriteria;

    @Override
    public boolean evaluate(HttpServletRequestWrapper httpServletRequestWrapper) {
        boolean rtn = !filterCriteria.evaluate(httpServletRequestWrapper);
        LOG.trace("The sub-criterion did{} match the request; returning {}.", rtn ? " not" : "", rtn);
        return rtn;
    }
}
