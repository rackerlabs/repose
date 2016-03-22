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
package org.openrepose.filters.versioning.util;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.transform.Transform;
import org.openrepose.commons.utils.transform.jaxb.StreamToJaxbTransform;
import org.openrepose.filters.versioning.schema.VersionChoice;
import org.openrepose.filters.versioning.schema.VersionChoiceList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author jhopper
 */
@RunWith(Enclosed.class)
public class ContentTransformerTest {

    private static final Transform<InputStream, JAXBElement<?>> xmlTransformer;

    static {
        try {
            xmlTransformer = new StreamToJaxbTransform(
                    JAXBContext.newInstance(
                            org.openrepose.filters.versioning.schema.ObjectFactory.class,
                            org.openrepose.filters.versioning.config.ObjectFactory.class));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create JAXBContext for test", ex);
        }
    }

    public static class WhenTransformingVersions {

        private ContentTransformer contentTransformer;
        private String versionJsonFileReader, versionXmlFileReader, choicesJsonFileReader, choicesXmlFileReader;
        private ObjectMapper mapper;

        @Before
        public void standUp() throws Exception {
            mapper = new ObjectMapper();
            contentTransformer = new ContentTransformer();
            versionJsonFileReader = FileUtils.readFileToString(new File(getClass().getResource("/META-INF/schema/examples/json/version.json").toURI()));
            versionXmlFileReader = FileUtils.readFileToString(new File(getClass().getResource("/META-INF/schema/examples/xml/version.xml").toURI()));
            choicesJsonFileReader = FileUtils.readFileToString(new File(getClass().getResource("/META-INF/schema/examples/json/choices.json").toURI()));
            choicesXmlFileReader = FileUtils.readFileToString(new File(getClass().getResource("/META-INF/schema/examples/xml/choices.xml").toURI()));
        }

        @Test
        public void shouldTransformVersionToJson() throws Exception {

            String expected;
            expected = versionJsonFileReader;
            assertNotNull("no expected string value found!", expected);

            final JAXBElement jaxbElement
                    = xmlTransformer.transform(IOUtils.toInputStream(versionXmlFileReader));
            ByteArrayOutputStream transformStream = new ByteArrayOutputStream();
            contentTransformer.transform(jaxbElement, new MediaType(MimeType.APPLICATION_JSON, -1), transformStream);
            String actual = transformStream.toString();

            Map<String, Object> expectedMap = mapper.readValue(expected, Map.class);
            Map<String, Object> actualMap = mapper.readValue(actual, Map.class);

            assertEquals("Objects should be equivalent", expectedMap, actualMap);
        }

        @Test
        public void shouldTransformVersionToXml() throws Exception {
            String expected;
            expected = versionXmlFileReader;
            assertNotNull("no expected string value found!", expected);

            final JAXBElement jaxbElement
                    = xmlTransformer.transform(IOUtils.toInputStream(versionXmlFileReader));
            ByteArrayOutputStream transformStream = new ByteArrayOutputStream();
            contentTransformer.transform(jaxbElement, new MediaType(MimeType.APPLICATION_XML, -1), transformStream);
            String actual = transformStream.toString();
            Diff diff = new Diff(expected, actual);
            assertTrue("XML Should be equivalent", diff.similar());
        }

        @Test
        public void shouldTransformVersionsToJson() throws Exception {
            String expected;
            expected = versionJsonFileReader;
            assertNotNull("no expected string value found!", expected);

            final JAXBElement jaxbElement
                    = xmlTransformer.transform(IOUtils.toInputStream(versionXmlFileReader));

            ByteArrayOutputStream transformStream = new ByteArrayOutputStream();
            contentTransformer.transform(jaxbElement, new MediaType(MimeType.APPLICATION_JSON, -1), transformStream);
            String actual = transformStream.toString();

            Map<String, Object> expectedMap = mapper.readValue(expected, Map.class);
            Map<String, Object> actualMap = mapper.readValue(actual, Map.class);

            assertEquals("Objects should be equivalent", expectedMap, actualMap);
            //assertEquals(expected, contentTransformer.transform(jaxbElement, new MediaType(MimeType.APPLICATION_JSON)));
        }

        @Test
        public void shouldTransformChoicesToJson() throws Exception {
            String expected;
            expected = choicesJsonFileReader;
            assertNotNull("no expected string value found!", expected);

            final JAXBElement jaxbElement
                    = xmlTransformer.transform(IOUtils.toInputStream(choicesXmlFileReader));

            ByteArrayOutputStream transformStream = new ByteArrayOutputStream();
            contentTransformer.transform(jaxbElement, new MediaType(MimeType.APPLICATION_JSON, -1), transformStream);
            String actual = transformStream.toString();

            Map<String, Object> expectedMap = mapper.readValue(expected, Map.class);
            Map<String, Object> actualMap = mapper.readValue(actual, Map.class);

            assertEquals("Objects should be equivalent", expectedMap, actualMap);

            //assertEquals(expected, contentTransformer.transform(jaxbElement, new MediaType(MimeType.APPLICATION_JSON)));
        }
    }

    public static class WhenMarshallingVersions {

        @Test
        public void shouldMarshalVersionXml() {
            final JAXBElement jaxbElement = xmlTransformer.transform(
                    ContentTransformerTest.class.getResourceAsStream("/META-INF/schema/examples/xml/version.xml"));

            assertTrue(jaxbElement.getDeclaredType().isAssignableFrom(VersionChoice.class));
        }

        @Test
        public void shouldMarshalVersionsXml() {
            final JAXBElement jaxbElement = xmlTransformer.transform(
                    ContentTransformerTest.class.getResourceAsStream("/META-INF/schema/examples/xml/versions.xml"));

            assertTrue(jaxbElement.getDeclaredType().isAssignableFrom(VersionChoiceList.class));
        }

        @Test
        public void shouldMarshalChoicesXml() {
            final JAXBElement jaxbElement = xmlTransformer.transform(
                    ContentTransformerTest.class.getResourceAsStream("/META-INF/schema/examples/xml/choices.xml"));

            assertTrue(jaxbElement.getDeclaredType().isAssignableFrom(VersionChoiceList.class));
        }
    }
}
