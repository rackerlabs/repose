package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.config.parser.common.ConfigurationParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class JaxbConfigurationParserTest {

    public static class WhenUsingJaxbConfigurationObjectParser {

        @Test
        public void shouldReadConfigurationResource() throws JAXBException, IOException {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
            ConfigurationParser<Element> parser = new JaxbConfigurationParser<Element>(Element.class, jaxbContext, null);

            ConfigurationResource cfgResource = mock(ConfigurationResource.class);
            when(cfgResource.newInputStream()).thenReturn(ConfigurationResource.class.getResourceAsStream("/META-INF/test/element.xml"));

            Element element = parser.read(cfgResource);

            assertNotNull(element);
        }

        @Test(expected=ClassCastException.class)
        public void shouldThrowClassCastException() throws JAXBException, IOException {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
            ConfigurationParser<String> parser = new JaxbConfigurationParser<String>(String.class, jaxbContext , null);

            ConfigurationResource cfgResource = mock(ConfigurationResource.class);
            when(cfgResource.newInputStream()).thenReturn(ConfigurationResource.class.getResourceAsStream("/META-INF/test/element.xml"));

            parser.read(cfgResource);
        }
    }
}
