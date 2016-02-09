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


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.Assert.assertEquals;

/**
 * @author kush5342
 */
public class BaseElementTest {

    static BaseElement instance;

    @BeforeClass
    public static void setUpClass() {

        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "date", "date", "java.lang.String", "25-dec-05");
        instance = new BaseElement("id:TestID", atts);

    }

    @AfterClass
    public static void tearDownClass() {

        instance = null;
    }

    /**
     * Test of getElement method, of class BaseElement.
     */
    @Test
    public void testGetElement() {
        String expResult = "id:TestID";
        String result = instance.getElement();
        assertEquals(expResult, result);

    }

    /**
     * Test of getLocalName method, of class BaseElement.
     */
    @Test
    public void testGetLocalName_0args() {

        String expResult = "TestID";
        String result = instance.getLocalName();
        assertEquals(expResult, result);

    }

    /**
     * Test of getQname method, of class BaseElement.
     */
    @Test
    public void testGetQname_0args() {

        String expResult = "json:TestID";
        String result = instance.getQname();
        assertEquals(expResult, result);

    }

    /**
     * Test of getAttributes method, of class BaseElement.
     */
    @Test
    public void testGetAttributes() {

        AttributesImpl expResult = new AttributesImpl();
        expResult.addAttribute("", "date", "date", "java.lang.String", "25-dec-05");
        AttributesImpl result = instance.getAttributes();
        assertEquals(expResult.getQName(1), result.getQName(1));

    }

    /**
     * Test of getLocalName method, of class BaseElement.
     */
    @Test
    public void testGetLocalName_String() {

        String name = "id:TestID";
        String expResult = "TestID";
        String result = BaseElement.getLocalName(name);
        assertEquals(expResult, result);

    }

    /**
     * Test of getQname method, of class BaseElement.
     */
    @Test
    public void testGetQname_String() {

        String name = "id:TestID";
        String expResult = "json:TestID";
        String result = BaseElement.getQname(name);
        assertEquals(expResult, result);

    }
}
