package com.rackspace.repose.service.ratelimit;

import com.rackspace.papi.commons.validate.xsd.JAXBValidator;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import java.io.StringReader;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class SchemaTest {

    public static class WhenValidating {

        private JAXBContext jaxbContext;
        private Unmarshaller jaxbUnmarshaller;
        private Validator validator;

        @Before
        public void standUp() throws Exception {
            jaxbContext = JAXBContext.newInstance(
                    com.rackspace.repose.service.ratelimit.config.ObjectFactory.class,
                    com.rackspace.repose.service.limits.schema.ObjectFactory.class);

            jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
            factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);

            Schema schema = factory.newSchema(
                    new StreamSource[]{
                            new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/limits/limits.xsd")),
                            new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/rate-limiting-configuration.xsd"))
                    });

            validator = schema.newValidator();

            jaxbUnmarshaller.setSchema(schema);

            jaxbUnmarshaller.setEventHandler(new JAXBValidator());
        }

        @Test
        public void shouldValidateAgainstStaticExample() throws Exception {
            final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/rate-limiting.cfg.xml"));
            assertNotNull("Expected element should not be null", jaxbUnmarshaller.unmarshal(sampleSource, RateLimitingConfiguration.class).getValue().getLimitGroup());
        }

        @Test
        public void shouldFailWhenConfigHasNonUniqueUriAndMethods() throws Exception {
            assertInvalidConfig("/META-INF/schema/examples/invalid/nonunique-uriregex-httpmethod.xml", "Unique http-methods, and uri-regexes");
        }

        @Test
        public void shouldNotFailWhenDuplicateHttpMethodAndDifferentUriRegex() throws Exception {
            final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/valid/unique-uriregex-httpmethod.xml"));
            assertNotNull("Expected element should not be null", jaxbUnmarshaller.unmarshal(sampleSource, RateLimitingConfiguration.class).getValue().getLimitGroup());
        }

        @Test
        public void shouldFailWhenConfigHasNonUniqueLimitGroupIds() throws Exception {
            assertInvalidConfig("/META-INF/schema/examples/invalid/nonunique-limitgroup-ids.xml", "Limit groups must have unique ids");
        }

        @Test
        public void shouldFailIfMoreThanOneDefaultLimitGroup() throws Exception {
            assertInvalidConfig("/META-INF/schema/examples/invalid/morethanone-default-limitgroup.xml", "Only one default limit group may be defined");
        }

        private void assertInvalidConfig(String resource, String errorMessage) {
            final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream(resource));
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
}
