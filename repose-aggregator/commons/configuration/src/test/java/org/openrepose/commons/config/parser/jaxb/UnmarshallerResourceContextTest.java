package org.openrepose.commons.config.parser.jaxb;

import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.utils.pooling.ResourceContext;
import org.openrepose.commons.utils.pooling.ResourceContextException;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
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
    private static final String CFG_DATA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<element>\n" +
            "    <hello>Hi there.</hello>\n" +
            "    <goodbye>See ya.</goodbye>\n" +
            "</element>\n";

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
        public void shouldPerformUnmarshall() throws IOException, JAXBException, ParserConfigurationException {
            ConfigurationResource cfgResource = mock(ConfigurationResource.class);
            ByteArrayInputStream cfgStream = new ByteArrayInputStream(CFG_DATA.getBytes());
            when(cfgResource.newInputStream()).thenReturn(cfgStream);

            JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
            UnmarshallerValidator unmarshaller = new UnmarshallerValidator( jaxbContext );
            
            ResourceContext<UnmarshallerValidator, Object> resourceContext = new UnmarshallerResourceContext(cfgResource);

            Element element = (Element) resourceContext.perform(unmarshaller);

            assertNotNull(element);
        }

        @Test(expected= ResourceContextException.class)
        public void testPerform()
              throws IOException, JAXBException, NoSuchAlgorithmException, ParserConfigurationException {
            ConfigurationResource cfgResource = mock(ConfigurationResource.class);
            when(cfgResource.newInputStream()).thenReturn(ConfigurationResource.class.getResourceAsStream("/nonexistent_resource"));

            JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
            UnmarshallerValidator unmarshaller = new UnmarshallerValidator( jaxbContext );

            ResourceContext<UnmarshallerValidator, Object> resourceContext = new UnmarshallerResourceContext(cfgResource);

            resourceContext.perform(unmarshaller);
        }
    }


}
