package com.rackspace.repose.service.httpconnectionpool;

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
import static org.junit.matchers.JUnitMatchers.containsString;

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
                    "<http-connection-pool xmlns='http://docs.rackspacecloud.com/repose/http-connection-pool/v1.0'> " +
                    "  <pools>" +
                    "    <pool id='default' default='true'/> " +
                    "    <pool id='default' default='false'/> " +
                    "  </pools>" +
                    "</http-connection-pool>";
            assertInvalidConfig(xml, "Pools must have unique ids");
        }

        @Test
        public void shouldFailIfMoreThanOneDefaultPool() throws Exception {
            String xml =
                    "<http-connection-pool xmlns='http://docs.rackspacecloud.com/repose/http-connection-pool/v1.0'> " +
                     "  <pools>" +
                     "    <pool id='default' default='true'/> " +
                     "    <pool id='default2' default='true'/> " +
                     "  </pools>" +
                     "</http-connection-pool>";
            assertInvalidConfig(xml, "Only one default pool may be defined");
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

            assertThat(caught.getLocalizedMessage(), containsString(errorMessage));
        }

    }
}
