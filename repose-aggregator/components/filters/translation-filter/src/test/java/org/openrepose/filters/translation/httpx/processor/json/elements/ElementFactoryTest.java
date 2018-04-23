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

import org.junit.Test;
import org.openrepose.filters.translation.httpx.processor.common.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;

/**
 * @author kush5342
 */
public class ElementFactoryTest {


    private static final Logger LOG = LoggerFactory.getLogger(ElementFactory.class);

    /**
     * Test of getElement method, of class ElementFactory.
     */
    @Test
    public void testGetElement() {

        String tokenName = "START_OBJECT";
        String name = "fid";
        Element result = ElementFactory.getElement(tokenName, name);
        assertNotNull(result);


    }

    /**
     * Test of getScalarElement method, of class ElementFactory.
     */
    @Test
    public void testGetScalarElement() {


        String tokenName = "VALUE_STRING";
        String name = "fid";
        Object value = "value";
        Element result = ElementFactory.getScalarElement(tokenName, name, value);
        assertNotNull(result);

    }
}
