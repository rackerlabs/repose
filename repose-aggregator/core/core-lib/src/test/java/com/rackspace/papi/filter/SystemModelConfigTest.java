package com.rackspace.papi.filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;

import static org.junit.Assert.*;

/**
 * This class tests the various asserts within the system model.
 * */

@RunWith(Enclosed.class)
public class SystemModelConfigTest {

    public static class WhenValidatingSystemModel {

        private Validator validator;

        @Before
        public void standUp() throws Exception {
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
            factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);

            Schema schema = factory.newSchema(
                    new StreamSource[]{
                            new StreamSource(SystemModelConfigTest.class.getResourceAsStream("/META-INF/schema/system-model/system-model.xsd"))
                    });

            validator = schema.newValidator();
        }

        @Test
        public void shouldValidateExampleConfig() throws Exception {
            final StreamSource sampleSource = new StreamSource(SystemModelConfigTest.class.getResourceAsStream("/META-INF/schema/examples/system-model.cfg.xml"));
            validator.validate(sampleSource);
        }

        @Test
        public void shouldValidateWhenDDServiceOnlyPresent() throws Exception {
            String xml =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<system-model xmlns=\"http://docs.rackspacecloud.com/repose/system-model/v2.0\">\n" +
                            "    <repose-cluster id=\"repose\">\n" +
                            "        <nodes>\n" +
                            "            <node id=\"node1\" hostname=\"localhost\" http-port=\"8000\"/>\n" +
                            "        </nodes>\n" +
                            "        <services>\n" +
                            "            <service name=\"dist-datastore\"/>\n" +
                            "        </services>\n" +
                            "        <destinations>\n" +
                            "            <endpoint id=\"openrepose\" protocol=\"http\" hostname=\"50.57.189.15\" root-path=\"/\" port=\"8080\"\n" +
                            "                      default=\"true\"/>\n" +
                            "        </destinations>\n" +
                            "    </repose-cluster>\n" +
                            "</system-model>\n";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenOnlyOneDestinationWithDefault() throws Exception {
            String xml =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<system-model xmlns=\"http://docs.rackspacecloud.com/repose/system-model/v2.0\">\n" +
                            "    <repose-cluster id=\"repose\">\n" +
                            "        <nodes>\n" +
                            "            <node id=\"node1\" hostname=\"localhost\" http-port=\"8000\"/>\n" +
                            "        </nodes>\n" +
                            "        <services>\n" +
                            "            <service name=\"dist-datastore\"/>\n" +
                            "        </services>\n" +
                            "        <destinations>\n" +
                            "            <endpoint id=\"openrepose\" protocol=\"http\" hostname=\"50.57.189.15\" root-path=\"/\" port=\"8080\"/>\n" +
                            "        </destinations>\n" +
                            "    </repose-cluster>\n" +
                            "</system-model>\n";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenTwoDestinationsWithOneDefault() throws Exception {
            String xml =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<system-model xmlns=\"http://docs.rackspacecloud.com/repose/system-model/v2.0\">\n" +
                            "    <repose-cluster id=\"repose\">\n" +
                            "        <nodes>\n" +
                            "            <node id=\"node1\" hostname=\"localhost\" http-port=\"8000\"/>\n" +
                            "        </nodes>\n" +
                            "        <services>\n" +
                            "            <service name=\"dist-datastore\"/>\n" +
                            "        </services>\n" +
                            "        <destinations>\n" +
                            "            <endpoint id=\"openrepose1\" protocol=\"http\" hostname=\"50.57.189.15\" root-path=\"/\" port=\"8080\" default=\"false\"/>\n" +
                            "            <endpoint id=\"openrepose2\" protocol=\"http\" hostname=\"50.57.189.15\" root-path=\"/\" port=\"8080\" default=\"true\"/>\n" +
                            "        </destinations>\n" +
                            "    </repose-cluster>\n" +
                            "</system-model>\n";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldNotValidateWhenTwoDestinationsWithTwoDefaults() throws Exception {
            String xml =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<system-model xmlns=\"http://docs.rackspacecloud.com/repose/system-model/v2.0\">\n" +
                            "    <repose-cluster id=\"repose\">\n" +
                            "        <nodes>\n" +
                            "            <node id=\"node1\" hostname=\"localhost\" http-port=\"8000\"/>\n" +
                            "        </nodes>\n" +
                            "        <services>\n" +
                            "            <service name=\"dist-datastore\"/>\n" +
                            "        </services>\n" +
                            "        <destinations>\n" +
                            "            <endpoint id=\"openrepose1\" protocol=\"http\" hostname=\"50.57.189.15\" root-path=\"/\" port=\"8080\"/>\n" +
                            "            <endpoint id=\"openrepose2\" protocol=\"http\" hostname=\"50.57.189.15\" root-path=\"/\" port=\"8080\"/>\n" +
                            "        </destinations>\n" +
                            "    </repose-cluster>\n" +
                            "</system-model>\n";

            assertInvalidConfig(xml, "There should only be one default destination");
        }

        @Test
        public void shouldNotValidateWhenTwoDestinationsWithNoDefault() throws Exception {
            String xml =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<system-model xmlns=\"http://docs.rackspacecloud.com/repose/system-model/v2.0\">\n" +
                            "    <repose-cluster id=\"repose\">\n" +
                            "        <nodes>\n" +
                            "            <node id=\"node1\" hostname=\"localhost\" http-port=\"8000\"/>\n" +
                            "        </nodes>\n" +
                            "        <services>\n" +
                            "            <service name=\"dist-datastore\"/>\n" +
                            "        </services>\n" +
                            "        <destinations>\n" +
                            "            <endpoint id=\"openrepose1\" protocol=\"http\" hostname=\"50.57.189.15\" root-path=\"/\" port=\"8080\" default=\"true\"/>\n" +
                            "            <endpoint id=\"openrepose2\" protocol=\"http\" hostname=\"50.57.189.15\" root-path=\"/\" port=\"8080\" default=\"true\"/>\n" +
                            "        </destinations>\n" +
                            "    </repose-cluster>\n" +
                            "</system-model>\n";

            assertInvalidConfig(xml, "There should only be one default destination");
        }

        private void assertInvalidConfig(String xml, String errorMessage) {
            final StreamSource sampleSource = new StreamSource(new ByteArrayInputStream(xml.getBytes()));
            Exception caught = null;
            try {
                validator.validate(sampleSource);
            } catch (Exception e) {
                caught = e;
            }

            assertNotNull("Expected exception", caught);
            assertSame(SAXParseException.class, caught.getClass());

            assertTrue(caught.getLocalizedMessage().contains(errorMessage));
        }
    }
}
