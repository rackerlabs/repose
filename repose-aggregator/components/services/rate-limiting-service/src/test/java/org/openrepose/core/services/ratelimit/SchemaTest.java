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
package org.openrepose.core.services.ratelimit;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(Enclosed.class)
public class SchemaTest {

    @RunWith(Parameterized.class)
    public static class WhenValidatingConfiguredMethod {

        private String method;
        private Validator validator;

        public WhenValidatingConfiguredMethod(String method) {
            this.method = method;
        }

        // TODO Upgrade jUnit to 4.11 to name parameterized tests
        @Parameterized.Parameters // (name = "{0} method")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"GET"}, {"DELETE"}, {"POST"}, {"PUT"},
                    {"PATCH"}, {"HEAD"}, {"OPTIONS"},
                    {"CONNECT"}, {"TRACE"}, {"ALL"}
            });
        }

        @Before
        public void standUp() throws Exception {
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
            factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);

            Schema schema = factory.newSchema(
                    new StreamSource[]{
                            new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/limits/limits.xsd")),
                            new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/rate-limiting-configuration.xsd"))
                    });

            validator = schema.newValidator();
        }

        @Test
        public void shouldValidateWhenValidMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit id=\"one\" uri='foo' uri-regex='foo' http-methods='" + method + "' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }
    }

    public static class WhenValidatingRateLimitConfiguration {

        private Validator validator;

        @Before
        public void standUp() throws Exception {
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
            factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);

            Schema schema = factory.newSchema(
                    new StreamSource[]{
                            new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/limits/limits.xsd")),
                            new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/rate-limiting-configuration.xsd"))
                    });

            validator = schema.newValidator();
        }

        @Test
        public void shouldValidateExampleConfig() throws Exception {
            final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/rate-limiting.cfg.xml"));
            validator.validate(sampleSource);
        }

        @Test
        public void shouldValidateWhenDuplicateHttpMethodAndDifferentUriRegex() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit id=\"one\" uri='foo' uri-regex='foo' http-methods='GET PUT' value='1' unit='HOUR'/>" +
                            "       <limit id=\"two\" uri='foo' uri-regex='bar' http-methods='GET PUT' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenDuplicateUriRegexsAndDifferentMethods() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit id=\"one\" uri='foo' uri-regex='foo' http-methods='GET PUT' value='1' unit='HOUR'/>" +
                            "       <limit id=\"two\" uri='foo' uri-regex='foo' http-methods='POST DELETE' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateIfLimitIdsSameAcrossGroups() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='customer-limits' groups='customer foo'> " +
                            "        <limit id=\"one\" uri='foo' uri-regex='foo' http-methods='ALL' value='1' unit='HOUR'/>" +
                            "        <limit id=\"two\" uri='foo2' uri-regex='foo2' http-methods='ALL' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits2' groups='customer2'> " +
                            "        <limit id=\"three\" uri='foo' uri-regex='foo' http-methods='ALL' value='1' unit='HOUR'/>" +
                            "        <limit id=\"four\" uri='foo2' uri-regex='foo2' http-methods='ALL' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "</rate-limiting>";
            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldFailWhenInvalidMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit id=\"one\" uri='foo' uri-regex='foo' http-methods='FOO' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            assertInvalidConfig(xml, "It must be a value from the enumeration.");
        }

        @Test
        public void shouldFailWhenConfigHasNonUniqueLimitGroupIds() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'/> " +
                            "    <limit-group id='test-limits' groups='user'/> " +
                            "</rate-limiting>";
            assertInvalidConfig(xml, "Limit groups must have unique ids");
        }

        @Test
        public void shouldFailIfMoreThanOneDefaultLimitGroup() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='customer-limits' groups='customer foo' default='true'/> " +
                            "    <limit-group id='test-limits' groups='user' default='true'/> " +
                            "</rate-limiting>";
            assertInvalidConfig(xml, "Only one default limit group may be defined");
        }

        @Test
        public void shouldFailIfNonUniqueLimitIdsUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='customer-limits' groups='customer'> " +
                            "        <limit id=\"one\" uri='foo' uri-regex='foo' http-methods='ALL' value='1' unit='HOUR'/>" +
                            "        <limit id=\"one\" uri='foo2' uri-regex='foo2' http-methods='ALL' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "</rate-limiting>";
            assertInvalidConfig(xml, "Limits must have unique ids");
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

            assert (caught.getLocalizedMessage().contains(errorMessage));
        }

    }
}
