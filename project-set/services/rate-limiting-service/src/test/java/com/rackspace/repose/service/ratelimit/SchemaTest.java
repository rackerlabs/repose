package com.rackspace.repose.service.ratelimit;

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
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                    "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                    "       <limit uri='foo' uri-regex='foo' http-methods='GET PUT' value='1' unit='HOUR'/>" +
                    "       <limit uri='foo' uri-regex='bar' http-methods='GET PUT' value='1' unit='HOUR'/>" +
                    "    </limit-group>" +
                    "    <limit-group id='customer-limits' groups='user'/> " +
                    "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenDuplicateUriRegexsAndDifferentMethods() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                    "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                    "       <limit uri='foo' uri-regex='foo' http-methods='GET PUT' value='1' unit='HOUR'/>" +
                    "       <limit uri='foo' uri-regex='foo' http-methods='POST DELETE' value='1' unit='HOUR'/>" +
                    "    </limit-group>" +
                    "    <limit-group id='customer-limits' groups='user'/> " +
                    "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        // TODO Break method validation tests into a parameterized test suite
        @Test
        public void shouldValidateWhenGetMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='GET' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenDeleteMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='DELETE' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenPostMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='POST' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenPutMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='PUT' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenPatchMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='PATCH' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenHeadMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='HEAD' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenOptionsMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='OPTIONS' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenConnectMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='CONNECT' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenTraceMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='TRACE' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldValidateWhenAllMethodUsed() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='ALL' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        }

        @Test
        public void shouldFailWhenConfigHasNonUniqueUriAndMethods() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='PUT' value='1' unit='HOUR'/>" +
                            "       <limit uri='foo' uri-regex='foo' http-methods='PUT' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            assertInvalidConfig(xml, "Unique http-methods, and uri-regexes");
        }

        @Test
        public void shouldFailWhenConfigHasNonUniqueUriAndListOfMethods() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='PUT GET' value='1' unit='HOUR'/>" +
                            "       <limit uri='foo' uri-regex='foo' http-methods='GET PUT' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            assertInvalidConfig(xml, "Unique http-methods, and uri-regexes");
        }

        @Test
        public void shouldFailWhenConfigHasNonUniqueUriAndMatchingMethods() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='PUT GET' value='1' unit='HOUR'/>" +
                            "       <limit uri='foo' uri-regex='foo' http-methods='GET PUT POST' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            assertInvalidConfig(xml, "Unique http-methods, and uri-regexes");
        }

        @Test
        public void shouldFailWhenConfigHasNonUniqueUriAndSomeMatchingMethods() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='DELETE PUT GET' value='1' unit='HOUR'/>" +
                            "       <limit uri='foo' uri-regex='foo' http-methods='GET PUT POST' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            assertInvalidConfig(xml, "Unique http-methods, and uri-regexes");
        }

        @Test
        public void shouldFailWhenConfigHasNonUniqueUriAndSingleMatchingMethod() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                            "    <limit-group id='test-limits' groups='customer foo' default='true'> " +
                            "       <limit uri='foo' uri-regex='foo' http-methods='DELETE PUT GET' value='1' unit='HOUR'/>" +
                            "       <limit uri='foo' uri-regex='foo' http-methods='GET' value='1' unit='HOUR'/>" +
                            "    </limit-group>" +
                            "    <limit-group id='customer-limits' groups='user'/> " +
                            "</rate-limiting>";

            assertInvalidConfig(xml, "Unique http-methods, and uri-regexes");
        }

        @Test
        public void shouldFailWhenConfigHasNonUniqueLimitGroupIds() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                    "    <limit-group id='test-limits' groups='customer foo' default='true'/> " +
                    "    <limit-group id='test-limits' groups='user'/> " +
                    "</rate-limiting>";
            assertInvalidConfig(xml, "Limit groups must have unique ids");
        }

        @Test
        public void shouldFailIfMoreThanOneDefaultLimitGroup() throws Exception {
            String xml =
                    "<rate-limiting xmlns='http://docs.rackspacecloud.com/repose/rate-limiting/v1.0'> " +
                    "    <limit-group id='customer-limits' groups='customer foo' default='true'/> " +
                    "    <limit-group id='test-limits' groups='user' default='true'/> " +
                    "</rate-limiting>";
            assertInvalidConfig(xml, "Only one default limit group may be defined");
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
