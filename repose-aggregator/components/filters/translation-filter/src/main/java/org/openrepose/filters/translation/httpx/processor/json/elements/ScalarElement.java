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

import org.openrepose.filters.translation.httpx.processor.common.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class ScalarElement<T> extends BaseElement implements Element {

    private final T value;

    public ScalarElement(String element, String name, T value) {
        super(element);

        this.value = value;
        if (name != null) {
            getAttributes().addAttribute("", "name", "name", "xsd:string", name);
        }
    }

    @Override
    public void outputElement(ContentHandler handler) throws SAXException {
        handler.startElement(JSONX_URI, getLocalName(), getQname(), getAttributes());
        if (value != null) {
            char[] charValue = value.toString().toCharArray();
            handler.characters(charValue, 0, charValue.length);
        }
        handler.endElement(JSONX_URI, getLocalName(), getQname());
    }
}
