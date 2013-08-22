package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import static org.junit.Assert.assertNotNull;

@RunWith(Enclosed.class)
public class SchemaTest {

    public static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance( "http://www.w3.org/XML/XMLSchema/v1.1" );

    public static class WhenValidating {

        private JAXBContext jaxbContext;
        private Unmarshaller jaxbUnmarshaller;

        @Before
        public void standUp() throws Exception {
            jaxbContext = JAXBContext.newInstance(
                    com.rackspace.papi.components.clientauth.config.ObjectFactory.class,
                    com.rackspace.papi.components.clientauth.basic.config.ObjectFactory.class,
                    com.rackspace.papi.components.clientauth.openstack.config.ObjectFactory.class);

            jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            jaxbUnmarshaller.setSchema(SCHEMA_FACTORY.newSchema(
                    new StreamSource[]{
                        new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/rackspace-auth-v1.1/rackspace-auth-v1.1.xsd")),
                        new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/openstack-ids-auth/openstack-ids-auth.xsd")),
                        new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/http-basic/http-basic.xsd")),
                        new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/config/client-auth-n-configuration.xsd"))
                    }));
        }

        @Test
        public void shouldValidateAgainstStaticExample() throws Exception {
            final StreamSource sampleSource = new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/examples/openstack/client-auth-n.cfg.xml"));

            assertNotNull("Expected element should not be null", jaxbUnmarshaller.unmarshal(sampleSource, ClientAuthConfig.class).getValue().getOpenstackAuth());
        }
    }
}
