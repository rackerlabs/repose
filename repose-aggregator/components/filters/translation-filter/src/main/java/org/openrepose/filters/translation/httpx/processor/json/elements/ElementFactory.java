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

import com.fasterxml.jackson.core.JsonToken;
import org.openrepose.filters.translation.httpx.processor.common.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO:Review - Dangerous try-catch statements
public enum ElementFactory {

    // Scalar Elements
    VALUE_TRUE(JsonToken.VALUE_TRUE.name(), "boolean"),
    VALUE_NULL(JsonToken.VALUE_NULL.name(), "null", NullElement.class),
    VALUE_FALSE(JsonToken.VALUE_FALSE.name(), "boolean"),
    VALUE_STRING(JsonToken.VALUE_STRING.name(), "string"),
    VALUE_NUMBER_INT(JsonToken.VALUE_NUMBER_INT.name(), "number"),
    VALUE_NUMBER_FLOAT(JsonToken.VALUE_NUMBER_FLOAT.name(), "number"),

    // Non-scalar Elements
    START_OBJECT(JsonToken.START_OBJECT.name(), "object", StartElement.class),
    START_ARRAY(JsonToken.START_ARRAY.name(), "array", StartElement.class),
    END_OBJECT(JsonToken.END_OBJECT.name(), "array", EndElement.class),
    END_ARRAY(JsonToken.END_ARRAY.name(), "array", EndElement.class);

    private static final Logger LOG = LoggerFactory.getLogger(ElementFactory.class);
    private final String tokenName;
    private final String elementName;
    private final Class elementClass;

    ElementFactory(String tokenName, String elementName, Class elementClass) {
        this.tokenName = tokenName;
        this.elementName = elementName;
        this.elementClass = elementClass;
    }

    ElementFactory(String tokenName, String elementName) {
        this.tokenName = tokenName;
        this.elementName = elementName;
        this.elementClass = null;
    }

    public static <T> Element getElement(String tokenName, String name) {
        Element result = null;
        for (ElementFactory element : values()) {
            if (element.tokenName.equals(tokenName)) {

                if (element.elementClass != null) {
                    try {
                        result = (Element) element.elementClass.getConstructors()[0].newInstance(element.elementName, name);
                    } catch (Exception ex) {
                        result = null;
                        LOG.trace("Caught Unknown Exception", ex);
                    }
                }
                break;
            }
        }
        return result;
    }

    public static <T> Element getScalarElement(String tokenName, String name, T value) {
        Element result = null;
        for (ElementFactory element : values()) {
            if (element.tokenName.equals(tokenName)) {
                if (element.elementClass != null) {
                    try {
                        result = (Element) element.elementClass.getConstructors()[0].newInstance(element.elementName, name, value);
                    } catch (Exception ex) {
                        result = null;
                        LOG.trace("Caught Unknown Exception", ex);
                    }
                } else {
                    result = new ScalarElement<>(element.elementName, name, value);
                }
                break;
            }
        }
        return result;
    }

}
