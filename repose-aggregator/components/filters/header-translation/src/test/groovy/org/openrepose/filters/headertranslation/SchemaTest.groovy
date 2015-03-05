package org.openrepose.filters.headertranslation
import org.junit.Before
import org.junit.Test
import org.xml.sax.SAXParseException

import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator

import static org.junit.Assert.*

public class SchemaTest {


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

    @Test
    public void shouldFailIfNonUniqueOriginalName() throws Exception {
        String xml =
            """<header-translation xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xsi:schemaLocation="http://docs.openrepose.org/repose/header-translation/v1.0 ../config/header-translation.xsd"
 xmlns="http://docs.openrepose.org/repose/header-translation/v1.0">

    <header original-name="Content-Type" new-name="rax-content-type"/>
    <header original-name="content-type" new-name="rax-content-length not-rax-content-length something-else"
        remove-original="true"/>

</header-translation>
"""
        assertInvalidConfig(xml, "Original names must be unique. Evaluation is case insensitive.");
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
