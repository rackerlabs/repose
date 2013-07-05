package com.rackspace.papi.components.ratelimit;


import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.commons.util.transform.jaxb.StreamToJaxbTransform;
import com.rackspace.repose.service.limits.schema.Limits;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.papi.components.ratelimit.util.LimitsEntityTransformer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TODO: Write JSON validation tests, you dummy
 * 
 * @author jhopper
 */
@RunWith(Enclosed.class)
public class SchemaTest {

    public static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance( "http://www.w3.org/XML/XMLSchema/v1.1" );

    public static class WhenValidating {

        private JAXBContext jaxbContext;
        private Unmarshaller jaxbUnmarshaller;

        @Before
        public void standUp() throws Exception {
            jaxbContext = JAXBContext.newInstance(
                    com.rackspace.repose.service.limits.schema.ObjectFactory.class,
                    com.rackspace.repose.service.ratelimit.config.ObjectFactory.class);

            jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            jaxbUnmarshaller.setSchema(SCHEMA_FACTORY.newSchema(
                    new StreamSource[]{
                        new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/limits/limits.xsd")),
                        new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/rate-limiting-configuration.xsd"))
                    }));
        }

        @Test
        public void shouldValidateAgainstConfigurationExample() throws Exception {
            final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/rate-limiting.cfg.xml"));

            assertNotNull("Expected element should not be null",
                    jaxbUnmarshaller.unmarshal(sampleSource, RateLimitingConfiguration.class).getValue().getRequestEndpoint());
        }

        @Test
        public void shouldValidateAgainstLiveLimitsExample() throws Exception {
            final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/limits.xml"));

            assertTrue("Expected element should have child elements",
                    jaxbUnmarshaller.unmarshal(sampleSource, Limits.class).getValue().getRates().getRate().size() > 0);
        }

        @Test @Ignore
        public void output() throws Exception {
            final LimitsEntityTransformer transformer = new LimitsEntityTransformer(jaxbContext);

            final Transform<InputStream, JAXBElement<Limits>> limitsTransformer = new StreamToJaxbTransform(jaxbContext);
            
            final JAXBElement<Limits> limitsElement = 
                    limitsTransformer.transform(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/limits.xml"));

            System.out.println(transformer.entityAsXml(limitsElement.getValue()));
            System.out.println(transformer.entityAsJson(limitsElement.getValue()));
        }
    }
}
