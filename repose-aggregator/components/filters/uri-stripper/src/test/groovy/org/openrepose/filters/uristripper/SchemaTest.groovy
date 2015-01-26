package org.openrepose.filters.uristripper

import org.junit.Before
import org.junit.Test
import org.xml.sax.SAXParseException

import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator

import static org.junit.Assert.*
import static org.junit.matchers.JUnitMatchers.containsString

class SchemaTest {

    private Validator validator;

    @Before
    void setUp() {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);

        Schema schema = factory.newSchema(
                new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/uri-stripper.xsd")));

        validator = schema.newValidator();

    }

    @Test
    public void shouldValidateExampleConfig() throws Exception {
        final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/uri-stripper.cfg.xml"));
        validator.validate(sampleSource);
    }

    @Test
    public void shouldFailWhenGivenNegativePosition() {

        String xml =
            """<?xml version="1.0" encoding="UTF-8"?>
<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="true" token-index="-1"/>
"""

        assertInvalidConfig(xml, "is not facet-valid with respect to minInclusive '0' for type 'nonNegativeInt'.")
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