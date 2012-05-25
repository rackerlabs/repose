package com.rackspace.papi.commons.config.parser;

import com.rackspace.papi.commons.config.ConfigurationResourceException;
import com.rackspace.papi.commons.config.parser.common.ConfigurationParser;
import com.rackspace.papi.commons.config.parser.inputstream.InputStreamConfigurationParser;
import com.rackspace.papi.commons.config.parser.jaxb.JaxbConfigurationParser;
import com.rackspace.papi.commons.config.parser.properties.PropertiesFileConfigurationParser;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigurationParserFactory {

    private ConfigurationParserFactory() {
    }

    public static <T> ConfigurationParser<T> newConfigurationParser(ConfigurationParserType type, Class<T> configurationClass) {
        switch (type) {
            case XML:
                return getXmlConfigurationParser(configurationClass);
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

    public static <T> JaxbConfigurationParser<T> getXmlConfigurationParser(Class<T> configurationClass) {
        try {
            final JAXBContext jaxbCtx = JAXBContext.newInstance(configurationClass.getPackage().getName());
            return new JaxbConfigurationParser<T>(configurationClass, jaxbCtx);
        } catch (JAXBException jaxbe) {
            throw new ConfigurationResourceException("Failed to create a JAXB context for a configuration parser. Reason: " + jaxbe.getMessage(), jaxbe);
        }
    }
}
