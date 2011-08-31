package com.rackspace.papi.components.versioning.util;

import com.rackspace.papi.components.versioning.schema.VersionChoice;
import com.rackspace.papi.components.versioning.schema.VersionChoiceList;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.io.FilePathReaderImpl;
import com.rackspace.papi.commons.util.io.FileReader;
import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.commons.util.transform.jaxb.StreamToJaxbTransform;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

        @Before
        public void standUp() {
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

            assertEquals(expected, contentTransformer.transform(jaxbElement, MediaType.APPLICATION_JSON));
        }

        @Test
        public void shouldTransformVersionToXml() throws Exception {
            String expected;
            expected = versionXmlFileReader.read();
            assertNotNull("no expected string value found!", expected);

            final JAXBElement jaxbElement
                    = xmlTransformer.transform(((FilePathReaderImpl)versionXmlFileReader).getResourceAsStream());

            assertEquals(expected, contentTransformer.transform(jaxbElement, MediaType.APPLICATION_XML));
        }

        @Test
        public void shouldTransformVersionsToJson() throws Exception {
            String expected;
            expected = versionJsonFileReader.read();
            assertNotNull("no expected string value found!", expected);

            final JAXBElement jaxbElement
                    = xmlTransformer.transform(((FilePathReaderImpl)versionXmlFileReader).getResourceAsStream());

            assertEquals(expected, contentTransformer.transform(jaxbElement, MediaType.APPLICATION_JSON));
        }

        @Test
        public void shouldTransformChoicesToJson() throws Exception {
            String expected;
            expected = choicesJsonFileReader.read();
            assertNotNull("no expected string value found!", expected);

            final JAXBElement jaxbElement
                    = xmlTransformer.transform(((FilePathReaderImpl)choicesXmlFileReader).getResourceAsStream());

            assertEquals(expected, contentTransformer.transform(jaxbElement, MediaType.APPLICATION_JSON));
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
