package org.openrepose.services.datastore.impl

import org.junit.Before
import org.junit.Test
import org.xml.sax.SAXParseException

import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator

import static junit.framework.Assert.assertSame
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat
import static org.junit.matchers.JUnitMatchers.containsString

class SchemaTest {

    private Validator validator;

    @Before
    void setUp() {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);

        Schema schema = factory.newSchema(
                new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/dist-datastore-configuration.xsd")));

        validator = schema.newValidator();

    }

    @Test
    public void shouldValidateExampleConfig() throws Exception {
        final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/dist-datastore.cfg.xml"));
        validator.validate(sampleSource);
    }

    @Test
    public void shouldFailWhenGivenAllowAllIsUsedWithAllowElements() {

        String xml = """<?xml version="1.0" encoding="UTF-8"?>

<distributed-datastore xmlns='http://docs.openrepose.org/repose/distributed-datastore/v1.0'>
    <allowed-hosts allow-all="true">
        <allow host="127.0.0.1" />
    </allowed-hosts>

    <port-config>
        <port port="3888" cluster="38" />
        <port port="3999" cluster="40" node="8" />
        <port port="4000" cluster="40" />
    </port-config>
</distributed-datastore>"""

        assertInvalidConfig(xml, "If allow-all is true then allow elements not allowed.")
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
