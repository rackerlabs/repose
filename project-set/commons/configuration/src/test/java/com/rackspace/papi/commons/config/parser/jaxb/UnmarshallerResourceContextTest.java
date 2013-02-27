package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.junit.After;
import org.junit.AfterClass;

import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class UnmarshallerResourceContextTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

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
        public void testPerform() throws IOException, JAXBException, NoSuchAlgorithmException {
            ConfigurationResource cfgResource = mock(ConfigurationResource.class);
            when(cfgResource.newInputStream()).thenReturn(ConfigurationResource.class.getResourceAsStream("/nonexistent_resource"));

            JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            ResourceContext<Unmarshaller, Object> resourceContext = new UnmarshallerResourceContext(cfgResource);

            resourceContext.perform(unmarshaller);
        }
    }


}
