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
package org.openrepose.filters.translation.httpx.processor.json.elements;

import org.xml.sax.helpers.AttributesImpl;

public class BaseElement {

    public static final String QNAME_PREFIX = "json";
    public static final String JSONX_URI = "http://www.ibm.com/xmlns/prod/2009/jsonx";
    private final AttributesImpl attrs;
    private final String element;

    public BaseElement(String element) {
        this.element = element;
        this.attrs = new AttributesImpl();
    }

    public BaseElement(String element, AttributesImpl attrs) {
        this.element = element;
        this.attrs = attrs;
    }

    protected static String getLocalName(String name) {
        String[] parts = name.split(":");
        return parts[parts.length - 1];
    }

    protected static String getQname(String name) {
        return QNAME_PREFIX + ":" + getLocalName(name);
    }

    public String getElement() {
        return element;
    }

    public String getLocalName() {
        return getLocalName(element);
    }

    public String getQname() {
        return getQname(element);
    }

    public AttributesImpl getAttributes() {
        return attrs;
    }
}
