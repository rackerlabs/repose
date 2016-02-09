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
package org.openrepose.filters.translation.xslt.xmlfilterchain;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.filters.translation.TranslationFilter;
import org.openrepose.filters.translation.xslt.StyleSheetInfo;
import org.openrepose.filters.translation.xslt.XsltParameter;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class XsltFilterChainBuilderTest {
    public static class WhenBuildingChains {

        private static SAXTransformerFactory factory;
        private XmlFilterChainBuilder builder;

        @BeforeClass
        public static void before() {
            System.setProperty("javax.xml.transform.TransformerFactory", TranslationFilter.SAXON_HE_FACTORY_NAME);
            factory = (SAXTransformerFactory) TransformerFactory.newInstance();
        }

        @Before
        public void setUp() {
            builder = new XmlFilterChainBuilder(factory, false, true);
        }

        @Test
        public void shouldHandleEmptySetOfStyles() {
            XmlFilterChain chain = builder.build();

            assertNotNull("Should build an empty filter chain", chain);
            assertEquals("Should have 1 filter", 1, chain.getFilters().size());
        }

        @Test
        public void shouldHandleStyleSheetList() {
            XmlFilterChain chain = builder.build(new StyleSheetInfo("", "classpath:///style.xsl"));

            assertNotNull("Should build a filter chain", chain);
            assertEquals("Should have 1 filter", 1, chain.getFilters().size());
        }
    }

    public static class WhenExecutingChains {
        private static SAXTransformerFactory factory;
        private XmlFilterChainBuilder builder;
        private ByteArrayOutputStream output;
        private InputStream body;

        @BeforeClass
        public static void before() {
            System.setProperty("javax.xml.transform.TransformerFactory", TranslationFilter.SAXON_HE_FACTORY_NAME);
            factory = (SAXTransformerFactory) TransformerFactory.newInstance();
        }

        @Before
        public void setUp() {
            builder = new XmlFilterChainBuilder(factory, false, true);
            output = new ByteArrayOutputStream();
            body = getClass().getResourceAsStream("/empty.xml");
        }

        @Test
        public void shouldUseInputOutputStreams() {
            List<XsltParameter> inputs = new ArrayList<>();

            XmlFilterChain chain = builder.build(new StyleSheetInfo("", "classpath:///style.xsl"));
            chain.executeChain(body, output, inputs, null);

            String outResult = output.toString();

            assertTrue("Shoudl have main output", outResult.length() > 0);
        }
    }
}
