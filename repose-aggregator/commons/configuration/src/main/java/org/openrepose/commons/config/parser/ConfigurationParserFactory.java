package org.openrepose.commons.config.parser;

import org.openrepose.commons.config.ConfigurationResourceException;
import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.parser.inputstream.InputStreamConfigurationParser;
import org.openrepose.commons.config.parser.jaxb.JaxbConfigurationParser;
import org.openrepose.commons.config.parser.properties.PropertiesFileConfigurationParser;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public final class ConfigurationParserFactory {

    private ConfigurationParserFactory() {
    }

    public static <T> ConfigurationParser<T> newConfigurationParser(ConfigurationParserType type, Class<T> configurationClass) {
        switch (type) {
            case XML:
                return getXmlConfigurationParser(configurationClass, null);
            case PROPERTIES:
                return (ConfigurationParser<T>) newPropertiesFileConfigurationParser();
            case RAW:
                return (ConfigurationParser<T>) newInputStreamConfigurationParser();
        }

        throw new IllegalArgumentException("Unknown configuration parser type: " + type);
    }

    public static ConfigurationParser<InputStream> newInputStreamConfigurationParser() {
        return new InputStreamConfigurationParser();
    }

    public static ConfigurationParser<Properties> newPropertiesFileConfigurationParser() {
        return new PropertiesFileConfigurationParser();
    }

    public static <T> JaxbConfigurationParser<T> getXmlConfigurationParser(Class<T> configurationClass,URL xsdStreamSource) {
        try {
            final JAXBContext jaxbCtx = JAXBContext.newInstance(configurationClass.getPackage().getName());
            return new JaxbConfigurationParser<T>(configurationClass, jaxbCtx ,xsdStreamSource);
        } catch (JAXBException jaxbe) {
            throw new ConfigurationResourceException("Failed to create a JAXB context for a configuration parser. Reason: " + jaxbe.getMessage(), jaxbe);
        }
    }
    
}
