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
import org.junit.Test;
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
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

// suppress unchecked assignment warnings in the static initializer
@SuppressWarnings("unchecked")
public class ContentTransformerTest {

    private static final String EXAMPLES_DIR = "/META-INF/schema/examples/";
    private static final String XML_VERSION = EXAMPLES_DIR + "xml/version.xml";
    private static final String XML_VERSIONS = EXAMPLES_DIR + "xml/versions.xml";
    private static final String JSON_VERSION = EXAMPLES_DIR + "json/version.json";
    private static final String JSON_VERSIONS = EXAMPLES_DIR + "json/versions.json";
    private static final String JSON_VERSION_IDENTITY = EXAMPLES_DIR + "json/version-identity.json";
    private static final String JSON_VERSIONS_IDENTITY = EXAMPLES_DIR + "json/versions-identity.json";
    private static final String XML_CHOICES = EXAMPLES_DIR + "xml/choices.xml";
    private static final String JSON_CHOICES = EXAMPLES_DIR + "json/choices.json";

    private static final Transform<InputStream, JAXBElement<?>> xmlTransformer;

    private ObjectMapper mapper = new ObjectMapper();
    private String versionXmlFileReader = getFileAsString(XML_VERSION);
    private String versionsXmlFileReader = getFileAsString(XML_VERSIONS);

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

    @Test
    public void shouldTransformVersionToJson() throws Exception {
        String expected = getFileAsString(JSON_VERSION);
        String actual = transformXmlToFormat(
                versionXmlFileReader,
                new MediaType(MimeType.APPLICATION_JSON, -1),
                new ContentTransformer(JsonFormat.COMPUTE));

        assertThat("No expected string value found!", expected, not(nullValue()));
        assertThat(mapper.readValue(actual, Map.class), equalTo(mapper.readValue(expected, Map.class)));
    }

    @Test
    public void shouldTransformVersionsToJson() throws Exception {
        String expected = getFileAsString(JSON_VERSIONS);
        String actual = transformXmlToFormat(
                versionsXmlFileReader,
                new MediaType(MimeType.APPLICATION_JSON, -1),
                new ContentTransformer(JsonFormat.COMPUTE));

        assertThat("No expected string value found!", expected, not(nullValue()));
        assertThat(mapper.readValue(actual, Map.class), equalTo(mapper.readValue(expected, Map.class)));
    }

    @Test
    public void shouldTransformVersionIdentityToJson() throws Exception {
        String expected = getFileAsString(JSON_VERSION_IDENTITY);
        String actual = transformXmlToFormat(
                versionXmlFileReader,
                new MediaType(MimeType.APPLICATION_JSON, -1),
                new ContentTransformer(JsonFormat.IDENTITY));

        assertThat("No expected string value found!", expected, not(nullValue()));
        assertThat(mapper.readValue(actual, Map.class), equalTo(mapper.readValue(expected, Map.class)));
    }

    @Test
    public void shouldTransformVersionsIdentityToJson() throws Exception {
        String expected = getFileAsString(JSON_VERSIONS_IDENTITY);
        String actual = transformXmlToFormat(
                versionsXmlFileReader,
                new MediaType(MimeType.APPLICATION_JSON, -1),
                new ContentTransformer(JsonFormat.IDENTITY));

        assertThat("No expected string value found!", expected, not(nullValue()));
        assertThat(mapper.readValue(actual, Map.class), equalTo(mapper.readValue(expected, Map.class)));
    }

    @Test
    public void shouldTransformVersionToXml() throws Exception {
        String expected = versionXmlFileReader;
        String actual = transformXmlToFormat(
                versionXmlFileReader,
                new MediaType(MimeType.APPLICATION_XML, -1),
                new ContentTransformer(JsonFormat.COMPUTE));

        assertThat("No expected string value found!", expected, not(nullValue()));
        Diff diff = new Diff(expected, actual);
        assertTrue("XML Should be equivalent: " + diff, diff.similar());
    }

    @Test
    public void shouldTransformVersionsToXml() throws Exception {
        String expected = versionsXmlFileReader;
        String actual = transformXmlToFormat(
                versionsXmlFileReader,
                new MediaType(MimeType.APPLICATION_XML, -1),
                new ContentTransformer(JsonFormat.COMPUTE));

        assertThat("No expected string value found!", expected, not(nullValue()));
        Diff diff = new Diff(expected, actual);
        assertTrue("XML Should be equivalent: " + diff, diff.similar());
    }

    @Test
    public void shouldTransformChoicesToJson() throws Exception {
        String expected = getFileAsString(JSON_CHOICES);
        String actual = transformXmlToFormat(
                getFileAsString("/META-INF/schema/examples/xml/choices.xml"),
                new MediaType(MimeType.APPLICATION_JSON, -1),
                new ContentTransformer(JsonFormat.COMPUTE));

        assertThat("No expected string value found!", expected, not(nullValue()));
        assertThat(mapper.readValue(actual, Map.class), equalTo(mapper.readValue(expected, Map.class)));
    }

    @Test
    public void shouldMarshalVersionXml() {
        final JAXBElement jaxbElement = xmlTransformer.transform(
                ContentTransformerTest.class.getResourceAsStream(XML_VERSION));

        assertTrue(jaxbElement.getDeclaredType().isAssignableFrom(VersionChoice.class));
    }

    @Test
    public void shouldMarshalVersionsXml() {
        final JAXBElement jaxbElement = xmlTransformer.transform(
                ContentTransformerTest.class.getResourceAsStream(XML_VERSIONS));

        assertTrue(jaxbElement.getDeclaredType().isAssignableFrom(VersionChoiceList.class));
    }

    @Test
    public void shouldMarshalChoicesXml() {
        final JAXBElement jaxbElement = xmlTransformer.transform(
                ContentTransformerTest.class.getResourceAsStream(XML_CHOICES));

        assertTrue(jaxbElement.getDeclaredType().isAssignableFrom(VersionChoiceList.class));
    }

    private String transformXmlToFormat(
            String source,
            MediaType mediaType,
            ContentTransformer contentTransformer) {
        JAXBElement jaxbElement = xmlTransformer.transform(IOUtils.toInputStream(source));
        ByteArrayOutputStream transformStream = new ByteArrayOutputStream();
        contentTransformer.transform(jaxbElement, mediaType, transformStream);
        return transformStream.toString();
    }

    private String getFileAsString(String fileName) {
        try {
            return FileUtils.readFileToString(new File(getClass().getResource(fileName).toURI()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
