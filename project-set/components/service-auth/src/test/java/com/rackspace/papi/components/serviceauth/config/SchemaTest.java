package com.rackspace.papi.components.serviceauth.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import static org.junit.Assert.assertNotNull;

/**
 *
 * @author jhopper
 */
@RunWith(Enclosed.class)
public class SchemaTest {

    public static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    public static class WhenValidating {

        private JAXBContext jaxbContext;
        private Unmarshaller jaxbUnmarshaller;

        @Before
        public void standUp() throws Exception {
            jaxbContext = JAXBContext.newInstance(
                    com.rackspace.papi.components.serviceauth.config.ObjectFactory.class);

            jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            jaxbUnmarshaller.setSchema(SCHEMA_FACTORY.newSchema(
                    new StreamSource[]{
                        new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/service-auth/service-auth-configuration.xsd"))
                    }));
        }

        @Test
        public void shouldValidateAgainstStaticExample() throws Exception {
            final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/xsd/service-auth.cfg.xml"));

            assertNotNull("Expected element should not be null", jaxbUnmarshaller.unmarshal(sampleSource, ServiceAuthConfig.class).getValue().getHttpBasic().getCredentials());
        }
    }
}
