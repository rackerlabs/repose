package org.openrepose.components.apivalidator.filter;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class XSDVersioningTest {

    public static class When {
        private Validator validator;

        @Before
        public void setup() throws Exception {
            final StreamSource schemaSource = new StreamSource(XSDVersioningTest.class.getResourceAsStream("/META-INF/schema/config/validator-configuration.xsd"));

            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
            Schema schema = schemaFactory.newSchema(schemaSource);
            validator = schema.newValidator();
        }

        @Test
        public void shouldDefaultToVersion1() {
            boolean validated = false;
            String xml =
                    "<validators xmlns=\"http://openrepose.org/repose/validator/v1.0\" multi-role-match=\"true\">" +
                    "    <validator" +
                    "        role=\"default\"" +
                    "        default=\"true\"" +
                    "        wadl=\"file://my/wadl/file.wadl\"" +
                    "        dot-output=\"/tmp/default.dot\"" +
                    "        check-well-formed=\"false\"" +
                    "        check-xsd-grammar=\"true\"" +
                    "        check-elements=\"true\"" +
                    "        check-plain-params=\"true\"" +
                    "        do-xsd-grammar-transform=\"true\"" +
                    "        enable-pre-process-extension=\"true\"" +
                    "        remove-dups=\"true\"" +
                    "        xpath-version=\"2\"" +
                    "        xsl-engine=\"XalanC\"" +
                    "        use-saxon=\"false\"" +
                    "        enable-ignore-xsd-extension=\"false\"" +
                    "        join-xpath-checks=\"false\"" +
                    "        validator-name=\"testName\"" +
                    "        check-headers=\"true\"/>" +
                    "</validators>";

            try {
                validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
                validated = true;
            } catch (Exception e) {}

            assertTrue(validated);
        }

        @Test
        public void shouldValidateVersion1() {
            boolean validated = false;
            String xml =
                    "<validators xmlns=\"http://openrepose.org/repose/validator/v1.0\" multi-role-match=\"true\" version=\"1\">" +
                            "    <validator" +
                            "        role=\"default\"" +
                            "        default=\"true\"" +
                            "        wadl=\"file://my/wadl/file.wadl\"" +
                            "        dot-output=\"/tmp/default.dot\"" +
                            "        check-well-formed=\"false\"" +
                            "        check-xsd-grammar=\"true\"" +
                            "        check-elements=\"true\"" +
                            "        check-plain-params=\"true\"" +
                            "        do-xsd-grammar-transform=\"true\"" +
                            "        enable-pre-process-extension=\"true\"" +
                            "        remove-dups=\"true\"" +
                            "        xpath-version=\"2\"" +
                            "        xsl-engine=\"XalanC\"" +
                            "        use-saxon=\"false\"" +
                            "        enable-ignore-xsd-extension=\"false\"" +
                            "        join-xpath-checks=\"false\"" +
                            "        validator-name=\"testName\"" +
                            "        check-headers=\"true\"/>" +
                            "</validators>";

            try {
                validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
                validated = true;
            } catch (Exception e) {}

            assertTrue(validated);
        }

        @Test
        public void shouldValidateVersion2() {
            boolean validated = false;
            String xml =
                    "<validators xmlns=\"http://openrepose.org/repose/validator/v1.0\" multi-role-match=\"true\" version=\"2\">" +
                            "    <validator" +
                            "        role=\"default\"" +
                            "        default=\"true\"" +
                            "        wadl=\"file://my/wadl/file.wadl\"" +
                            "        dot-output=\"/tmp/default.dot\"" +
                            "        check-well-formed=\"false\"" +
                            "        check-xsd-grammar=\"true\"" +
                            "        check-elements=\"true\"" +
                            "        check-plain-params=\"true\"" +
                            "        do-xsd-grammar-transform=\"true\"" +
                            "        enable-pre-process-extension=\"true\"" +
                            "        remove-dups=\"true\"" +
                            "        xpath-version=\"2\"" +
                            "        xsl-engine=\"XalanC\"" +
                            "        xsd-engine=\"Xerces\"" +
                            "        enable-ignore-xsd-extension=\"false\"" +
                            "        join-xpath-checks=\"false\"" +
                            "        validator-name=\"testName\"" +
                            "        check-headers=\"true\"/>" +
                            "</validators>";

            try {
                validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
                validated = true;
            } catch (Exception e) {}

            assertTrue(validated);
        }
    }
}
