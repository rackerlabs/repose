package org.openrepose.services.httpclient.impl;

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

@RunWith(Enclosed.class)
public class SchemaTest {

    public static class WhenValidatingConfiguration {

        private Validator validator;

        @Before
        public void standUp() throws Exception {
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
            factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);

            Schema schema = factory.newSchema(
                    new StreamSource[]{
                            new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/http-connection-pool.xsd"))
                    });

            validator = schema.newValidator();
        }

        @Test
        public void shouldValidateExampleConfig() throws Exception {
            final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/http-connection-pool.cfg.xml"));
            validator.validate(sampleSource);
        }


        @Test
        public void shouldFailWhenConfigHasNonUniquePoolIds() throws Exception {
            String xml =
                    "<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'> " +
                            "    <pool id='default' default='true'/> " +
                            "    <pool id='default' default='false'/> " +
                            "</http-connection-pools>";
            assertInvalidConfig(xml, "Pools must have unique ids");
        }

        @Test
        public void shouldFailIfMoreThanOneDefaultPool() throws Exception {
            String xml =
                    "<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'> " +
                            "    <pool id='default' default='true'/> " +
                            "    <pool id='default2' default='true'/> " +
                            "</http-connection-pools>";
            assertInvalidConfig(xml, "One and only one default pool must be defined");
        }


        @Test
        public void shouldFailINoDefaultPool() throws Exception {
            String xml =
                    "<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'> " +
                            "    <pool id='default' default='false'/> " +
                            "    <pool id='default2' default='false'/> " +
                            "</http-connection-pools>";
            assertInvalidConfig(xml, "One and only one default pool must be defined");
        }


        @Test
        public void shouldFailIfMaxPerRouteGreaterThanMaxTotal() throws Exception {
            String xml =
                    "<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'> " +
                    "<pool id='default' default='true' http.conn-manager.max-per-route='200' http.conn-manager.max-total='199' /> " +
                    "</http-connection-pools>";
            assertInvalidConfig(xml, "Max connections per route must be less than or equal to total max connections");
        }

        @Test
        public void shouldPassIfMaxPerRouteEqualsMaxTotal() throws Exception {
            String xml =
                    "<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'> " +
                            "<pool id='default' default='true' http.conn-manager.max-per-route='200' http.conn-manager.max-total='200' /> " +
                            "</http-connection-pools>";
            final StreamSource sampleSource = new StreamSource(new ByteArrayInputStream(xml.getBytes()));
            validator.validate(sampleSource);
        }

        @Test
        public void shouldFailIfChunkedEncodingIsConfiguredWrong() throws Exception {
            String xml =
                    "<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'> " +
                            "<pool id='default' default='true' chunked-encoding='blah' http.conn-manager.max-per-route='200' http.conn-manager.max-total='199' /> " +
                            "</http-connection-pools>";
            assertInvalidConfig(xml, "'blah' is not a valid value for 'boolean'");
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
