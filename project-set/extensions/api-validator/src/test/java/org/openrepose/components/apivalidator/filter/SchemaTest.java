package org.openrepose.components.apivalidator.filter;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.fail;

/*
 * A set of tests to verify that the schema is defined as desired
 * and that configuration files can be validated against it.
 */
public class SchemaTest {
    private Validator validator;

    @Before
    public void setup() throws Exception {
        final StreamSource schemaSource = new StreamSource(
                XSDVersioningTest.class.getResourceAsStream("/META-INF/schema/config/validator-configuration.xsd"));

        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        Schema schema = schemaFactory.newSchema(schemaSource);
        validator = schema.newValidator();
    }

    @Test
    public void shouldNotRequireRaxRolesAttribute() throws IOException {
        String xml =
                "<validators xmlns=\"http://openrepose.org/repose/validator/v1.0\" multi-role-match=\"true\">" +
                        "    <validator" +
                        "        role=\"default\"" +
                        "        default=\"true\"" +
                        "        wadl=\"file://my/wadl/file.wadl\"/>" +
                        "</validators>";

        try {
            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        } catch (SAXException se) {
            fail("Validated bad XML");
        }
    }

    @Test
    public void shouldAllowEnableRaxRolesAttribute() throws IOException {
        String xml =
                "<validators xmlns=\"http://openrepose.org/repose/validator/v1.0\" multi-role-match=\"true\">" +
                "    <validator" +
                "        role=\"default\"" +
                "        default=\"true\"" +
                "        wadl=\"file://my/wadl/file.wadl\"" +
                "        enable-rax-roles=\"true\"/>" +
                "</validators>";

        try {
            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        } catch (SAXException se) {
            fail("Failed to validate XML");
        }
    }

    @Test
    public void shouldNotAllowInvalidRaxRolesAttribute() throws IOException {
        String xml =
                "<validators xmlns=\"http://openrepose.org/repose/validator/v1.0\" multi-role-match=\"true\">" +
                        "    <validator" +
                        "        role=\"default\"" +
                        "        default=\"true\"" +
                        "        wadl=\"file://my/wadl/file.wadl\"" +
                        "        enable-rax-roles=\"foo\"/>" +
                        "</validators>";

        try {
            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
            fail("Validated bad XML");
        } catch (SAXException se) {}
    }
}
