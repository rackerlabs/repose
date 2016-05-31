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
import org.openrepose.filters.versioning.config.JsonFormat;
import org.openrepose.filters.versioning.schema.VersionChoice;
import org.openrepose.filters.versioning.schema.VersionChoiceList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author jhopper
 */
@RunWith(Enclosed.class)
@SuppressWarnings("unchecked")
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
        private String versionJsonFileReader, versionsJsonFileReader, versionJsonIdentityFileReader,
                versionXmlFileReader, versionsXmlFileReader, choicesJsonFileReader, choicesXmlFileReader;
        private ObjectMapper mapper;

        @Before
        public void standUp() throws Exception {
            mapper = new ObjectMapper();

            versionJsonFileReader = FileUtils.readFileToString(new File(getClass().getResource("/META-INF/schema/examples/json/version.json").toURI()));
            versionsJsonFileReader = FileUtils.readFileToString(new File(getClass().getResource("/META-INF/schema/examples/json/versions.json").toURI()));
            versionJsonIdentityFileReader = FileUtils.readFileToString(new File(getClass().getResource("/META-INF/schema/examples/json/versions-identity.json").toURI()));

            versionXmlFileReader = FileUtils.readFileToString(new File(getClass().getResource("/META-INF/schema/examples/xml/version.xml").toURI()));
            versionsXmlFileReader = FileUtils.readFileToString(new File(getClass().getResource("/META-INF/schema/examples/xml/versions.xml").toURI()));

            choicesJsonFileReader = FileUtils.readFileToString(new File(getClass().getResource("/META-INF/schema/examples/json/choices.json").toURI()));
            choicesXmlFileReader = FileUtils.readFileToString(new File(getClass().getResource("/META-INF/schema/examples/xml/choices.xml").toURI()));
        }

        @Test
        public void shouldTransformVersionToJson() throws Exception {
            String expected = versionJsonFileReader;
            String actual = transformXmlToFormat(
                    versionXmlFileReader,
                    new MediaType(MimeType.APPLICATION_JSON, -1),
                    new ContentTransformer(JsonFormat.COMPUTE));

            assertNotNull("No expected string value found!", expected);
            assertEquals(
                    "Objects should be equivalent",
                    mapper.readValue(expected, Map.class),
                    mapper.readValue(actual, Map.class));
        }

        @Test
        public void shouldTransformVersionsToJson() throws Exception {
            String expected = versionsJsonFileReader;
            String actual = transformXmlToFormat(
                    versionsXmlFileReader,
                    new MediaType(MimeType.APPLICATION_JSON, -1),
                    new ContentTransformer(JsonFormat.COMPUTE));

            assertNotNull("No expected string value found!", expected);
            assertEquals(
                    "Objects should be equivalent",
                    mapper.readValue(expected, Map.class),
                    mapper.readValue(actual, Map.class));
        }

        @Test
        public void shouldTransformVersionToXml() throws Exception {
            String expected = versionXmlFileReader;
            String actual = transformXmlToFormat(
                    versionXmlFileReader,
                    new MediaType(MimeType.APPLICATION_XML, -1),
                    new ContentTransformer(JsonFormat.COMPUTE));

            assertNotNull("No expected string value found!", expected);
            assertTrue(
                    "XML Should be equivalent, Expected:\n" + expected + "\nActual:\n" + actual,
                    new Diff(expected, actual).similar());
        }

        @Test
        public void shouldTransformVersionsToXml() throws Exception {
            String expected = versionsXmlFileReader;
            String actual = transformXmlToFormat(
                    versionsXmlFileReader,
                    new MediaType(MimeType.APPLICATION_XML, -1),
                    new ContentTransformer(JsonFormat.COMPUTE));

            assertNotNull("No expected string value found!", expected);
            assertTrue(
                    "XML Should be equivalent, Expected:\n" + expected + "\nActual:\n" + actual,
                    new Diff(expected, actual).similar());
        }

        @Test
        public void shouldTransformChoicesToJson() throws Exception {
            String expected = choicesJsonFileReader;
            String actual = transformXmlToFormat(
                    choicesXmlFileReader,
                    new MediaType(MimeType.APPLICATION_JSON, -1),
                    new ContentTransformer(JsonFormat.COMPUTE));

            assertNotNull("No expected string value found!", expected);
            assertEquals(
                    "Objects should be equivalent",
                    mapper.readValue(expected, Map.class),
                    mapper.readValue(actual, Map.class));
        }

        private String transformXmlToFormat(
                String source,
                MediaType mediaType,
                ContentTransformer contentTransformer) throws IOException {
            JAXBElement jaxbElement = xmlTransformer.transform(IOUtils.toInputStream(source));
            ByteArrayOutputStream transformStream = new ByteArrayOutputStream();
            contentTransformer.transform(jaxbElement, mediaType, transformStream);
            return transformStream.toString();
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
