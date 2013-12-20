package com.rackspace.papi.components.versioning.util;


import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.io.FilePathReaderImpl;
import com.rackspace.papi.commons.util.io.FileReader;
import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.commons.util.transform.jaxb.StreamToJaxbTransform;
import com.rackspace.papi.components.versioning.schema.VersionChoice;
import com.rackspace.papi.components.versioning.schema.VersionChoiceList;
import org.codehaus.jackson.map.ObjectMapper;
import org.custommonkey.xmlunit.Diff;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.*;

/**
 *
 * @author jhopper
 */
@RunWith(Enclosed.class)
public class ContentTransformerTest {

    private static final Transform<InputStream, JAXBElement<?>> xmlTransformer;

    static {
        try {
            xmlTransformer = new StreamToJaxbTransform(
                    JAXBContext.newInstance(
                    com.rackspace.papi.components.versioning.schema.ObjectFactory.class,
                    com.rackspace.papi.components.versioning.config.ObjectFactory.class));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create JAXBContext for test", ex);
        }
    }

    public static class WhenTransformingVersions {

        private ContentTransformer contentTransformer;
        private FileReader versionJsonFileReader, versionXmlFileReader, choicesJsonFileReader, choicesXmlFileReader;
        private ObjectMapper mapper;

        @Before
        public void standUp() {
            mapper = new ObjectMapper();
            contentTransformer = new ContentTransformer();
            versionJsonFileReader = new FilePathReaderImpl("/META-INF/schema/examples/json/version.json");
            versionXmlFileReader = new FilePathReaderImpl("/META-INF/schema/examples/xml/version.xml");
            choicesJsonFileReader = new FilePathReaderImpl("/META-INF/schema/examples/json/choices.json");
            choicesXmlFileReader = new FilePathReaderImpl("/META-INF/schema/examples/xml/choices.xml");
        }

        @Test
        public void shouldTransformVersionToJson() throws Exception {
            
            String expected;
            expected = versionJsonFileReader.read();
            assertNotNull("no expected string value found!", expected);

            final JAXBElement jaxbElement
                    = xmlTransformer.transform(((FilePathReaderImpl)versionXmlFileReader).getResourceAsStream());
            String actual = contentTransformer.transform(jaxbElement, new MediaType(MimeType.APPLICATION_JSON, -1));
            
            Map<String, Object> expectedMap = mapper.readValue(expected, Map.class);
            Map<String, Object> actualMap = mapper.readValue(actual, Map.class);
            
            assertEquals("Objects should be equivalent", expectedMap, actualMap);
        }

        @Test
        public void shouldTransformVersionToXml() throws Exception {
            String expected;
            expected = versionXmlFileReader.read();
            assertNotNull("no expected string value found!", expected);

            final JAXBElement jaxbElement
                    = xmlTransformer.transform(((FilePathReaderImpl)versionXmlFileReader).getResourceAsStream());
            String actual = contentTransformer.transform(jaxbElement, new MediaType(MimeType.APPLICATION_XML, -1));
            Diff diff = new Diff(expected, actual);
            assertTrue("XML Should be equivalent", diff.similar());
        }

        @Test
        public void shouldTransformVersionsToJson() throws Exception {
            String expected;
            expected = versionJsonFileReader.read();
            assertNotNull("no expected string value found!", expected);

            final JAXBElement jaxbElement
                    = xmlTransformer.transform(((FilePathReaderImpl)versionXmlFileReader).getResourceAsStream());
            
            String actual = contentTransformer.transform(jaxbElement, new MediaType(MimeType.APPLICATION_JSON, -1));
            
            Map<String, Object> expectedMap = mapper.readValue(expected, Map.class);
            Map<String, Object> actualMap = mapper.readValue(actual, Map.class);
            
            assertEquals("Objects should be equivalent", expectedMap, actualMap);
            //assertEquals(expected, contentTransformer.transform(jaxbElement, new MediaType(MimeType.APPLICATION_JSON)));
        }

        @Test
        public void shouldTransformChoicesToJson() throws Exception {
            String expected;
            expected = choicesJsonFileReader.read();
            assertNotNull("no expected string value found!", expected);

            final JAXBElement jaxbElement
                    = xmlTransformer.transform(((FilePathReaderImpl)choicesXmlFileReader).getResourceAsStream());
            
            String actual = contentTransformer.transform(jaxbElement, new MediaType(MimeType.APPLICATION_JSON, -1));
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
