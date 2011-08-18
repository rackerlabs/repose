package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class UnmarshallerResourceContextTest {

    public static class WhenUsingUnmarshallerResourceContextTest {

        @Test
        public void shouldPerformUnmarshall() throws IOException, JAXBException {
            ConfigurationResource cfgResource = mock(ConfigurationResource.class);
            when(cfgResource.newInputStream()).thenReturn(ConfigurationResource.class.getResourceAsStream("/META-INF/test/element.xml"));

            JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            
            ResourceContext<Unmarshaller, Object> resourceContext = new UnmarshallerResourceContext(cfgResource);

            Element element = (Element) resourceContext.perform(unmarshaller);

            assertNotNull(element);
        }

        @Test(expected= ResourceContextException.class)
        public void shouldThrowResourceContextExceptionForNonexistentResource() throws IOException, JAXBException, NoSuchAlgorithmException {
            ConfigurationResource cfgResource = mock(ConfigurationResource.class);
            when(cfgResource.newInputStream()).thenReturn(ConfigurationResource.class.getResourceAsStream("/nonexistent_resource"));

            JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            ResourceContext<Unmarshaller, Object> resourceContext = new UnmarshallerResourceContext(cfgResource);

            resourceContext.perform(unmarshaller);
        }
    }
}
