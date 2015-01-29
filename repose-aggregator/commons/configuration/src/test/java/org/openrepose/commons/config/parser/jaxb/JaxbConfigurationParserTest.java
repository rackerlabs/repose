package org.openrepose.commons.config.parser.jaxb;

import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;

import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class JaxbConfigurationParserTest {
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

    public static class WhenUsingJaxbConfigurationObjectParser {

        @Test
        public void shouldReadConfigurationResource() throws JAXBException, IOException {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
            ConfigurationParser<Element> parser = new JaxbConfigurationParser<Element>(Element.class, jaxbContext, null);

            ConfigurationResource cfgResource = mock(ConfigurationResource.class);
            ByteArrayInputStream cfgStream = new ByteArrayInputStream(CFG_DATA.getBytes());
            when(cfgResource.newInputStream()).thenReturn(cfgStream);

            Element element = parser.read(cfgResource);

            assertNotNull(element);
        }

        @Test(expected=ClassCastException.class)
        public void testRead() throws JAXBException, IOException {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
            ConfigurationParser<String> parser = new JaxbConfigurationParser<String>(String.class, jaxbContext , null);

            ConfigurationResource cfgResource = mock(ConfigurationResource.class);
            ByteArrayInputStream cfgStream = new ByteArrayInputStream(CFG_DATA.getBytes());
            when(cfgResource.newInputStream()).thenReturn(cfgStream);

            parser.read(cfgResource);
        }
  

   }
}
