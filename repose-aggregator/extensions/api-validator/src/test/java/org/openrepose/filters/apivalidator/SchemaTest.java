/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.apivalidator;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/*
 * A set of tests to verify that the schema is defined as desired
 * and that configuration files can be validated against it.
 */
public class SchemaTest {
    private Validator validator;

    @Before
    public void setup() throws Exception {
        final StreamSource schemaSource = new StreamSource(
                SchemaTest.class.getResourceAsStream("/META-INF/schema/config/validator-configuration.xsd"));

        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        Schema schema = schemaFactory.newSchema(schemaSource);
        validator = schema.newValidator();
    }

    @Test
    public void shouldNotRequireRaxRolesAttribute() throws IOException, SAXException {
        String xml =
                "<validators xmlns=\"http://docs.openrepose.org/repose/validator/v1.0\" multi-role-match=\"true\">" +
                        "    <validator" +
                        "        role=\"default\"" +
                        "        default=\"true\"" +
                        "        wadl=\"file://my/wadl/file.wadl\"/>" +
                        "</validators>";

        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
    }

    @Test
    public void shouldAllowEnableRaxRolesAttribute() throws IOException, SAXException {
        String xml =
                "<validators xmlns=\"http://docs.openrepose.org/repose/validator/v1.0\" multi-role-match=\"true\">" +
                        "    <validator" +
                        "        role=\"default\"" +
                        "        default=\"true\"" +
                        "        wadl=\"file://my/wadl/file.wadl\"" +
                        "        enable-rax-roles=\"true\"/>" +
                        "</validators>";

        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
    }

    @Test
    public void shouldAllowXsdEngineAttribute() throws IOException, SAXException {
        String xml =
                "<validators xmlns=\"http://docs.openrepose.org/repose/validator/v1.0\">" +
                        "    <validator" +
                        "        role=\"default\"" +
                        "        default=\"true\"" +
                        "        wadl=\"file://my/wadl/file.wadl\"" +
                        "        xsd-engine=\"Xerces\"/>" +
                        "</validators>";

        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
    }

    @Test(expected = SAXException.class)
    public void shouldNotAllowInvalidRaxRolesAttribute() throws IOException, SAXException {
        String xml =
                "<validators xmlns=\"http://docs.openrepose.org/repose/validator/v1.0\" multi-role-match=\"true\">" +
                        "    <validator" +
                        "        role=\"default\"" +
                        "        default=\"true\"" +
                        "        wadl=\"file://my/wadl/file.wadl\"" +
                        "        enable-rax-roles=\"foo\"/>" +
                        "</validators>";

        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
    }

    @Test(expected = SAXException.class)
    public void shouldNotAllowUseSaxonAttribute() throws IOException, SAXException {
        String xml =
                "<validators xmlns=\"http://docs.openrepose.org/repose/validator/v1.0\">" +
                        "    <validator" +
                        "        role=\"default\"" +
                        "        default=\"true\"" +
                        "        wadl=\"file://my/wadl/file.wadl\"" +
                        "        use-saxon=\"true\"/>" +
                        "</validators>";

        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
    }

    @Test(expected = SAXException.class)
    public void shouldNotAllowVersionAttribute() throws IOException, SAXException {
        String xml =
                "<validators xmlns=\"http://docs.openrepose.org/repose/validator/v1.0\" version=\"1\">" +
                        "    <validator" +
                        "        role=\"default\"" +
                        "        default=\"true\"" +
                        "        wadl=\"file://my/wadl/file.wadl\"/>" +
                        "</validators>";

        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
    }
}
