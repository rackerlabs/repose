package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.config.parser.ConfigurationObjectParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class JaxbConfigurationObjectParserTest {

    public static class WhenUsingJaxbConfigurationObjectParser {

        @Test
        public void shouldReadConfigurationResource() throws JAXBException, IOException {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
            ConfigurationObjectParser<Element> parser = new JaxbConfigurationObjectParser<Element>(Element.class, jaxbContext);

            ConfigurationResource cfgResource = mock(ConfigurationResource.class);
            when(cfgResource.newInputStream()).thenReturn(ConfigurationResource.class.getResourceAsStream("/META-INF/test/element.xml"));

            Element element = parser.read(cfgResource);

            assertNotNull(element);
        }

        @Test(expected=ClassCastException.class)
        public void shouldThrowClassCastException() throws JAXBException, IOException {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
            ConfigurationObjectParser<String> parser = new JaxbConfigurationObjectParser<String>(String.class, jaxbContext);

            ConfigurationResource cfgResource = mock(ConfigurationResource.class);
            when(cfgResource.newInputStream()).thenReturn(ConfigurationResource.class.getResourceAsStream("/META-INF/test/element.xml"));

            parser.read(cfgResource);
        }
    }
}
