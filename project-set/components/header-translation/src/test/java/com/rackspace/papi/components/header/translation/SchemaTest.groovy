package com.rackspace.papi.components.header.translation

import org.junit.Before
import org.junit.Test
import org.xml.sax.SAXParseException

import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertSame
import static org.junit.Assert.assertThat
import static org.junit.matchers.JUnitMatchers.containsString

public class SchemaTest {

    public static class WhenValidatingConfiguration {

        private Validator validator;

        @Before
        public void standUp() throws Exception {
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
            factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);

            Schema schema = factory.newSchema(
                        new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/header-translation.xsd")));

            validator = schema.newValidator();
        }

        @Test
        public void shouldValidateExampleConfig() throws Exception {
            final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/header-translation.cfg.xml"));
            validator.validate(sampleSource);
        }

    }
}
