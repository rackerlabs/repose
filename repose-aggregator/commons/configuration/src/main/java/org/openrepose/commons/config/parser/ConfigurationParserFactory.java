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

    //TODO: this is only exposed for testing methods!
    public static <T> ConfigurationParser<T> newConfigurationParser(ConfigurationParserType type, Class<T> configurationClass) throws JAXBException {
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

    /**
     * uses the current classloader to create a jaxb context. Used to not throw a jaxbexception but added that because it makes much more sense
     * @param configurationClass
     * @param xsdStreamSource
     * @param <T>
     * @return
     * @throws JAXBException
     */
    public static <T> JaxbConfigurationParser<T> getXmlConfigurationParser(Class<T> configurationClass, URL xsdStreamSource) throws JAXBException {
        final JAXBContext jaxbCtx = JAXBContext.newInstance(configurationClass.getPackage().getName());
        return new JaxbConfigurationParser<T>(configurationClass, jaxbCtx, xsdStreamSource);
    }

    /**
     * Creates a jaxb parser for a specific classloader.
     * Throws up the JAXB exception so that things know they have to handle it.
     *
     * @param configurationClass
     * @param xsdStreamSource
     * @param loader
     * @param <T>
     * @return
     * @throws JAXBException
     */
    public static <T> JaxbConfigurationParser<T> getXmlConfigurationParser(Class<T> configurationClass, URL xsdStreamSource, ClassLoader loader) throws JAXBException {
        final JAXBContext context = JAXBContext.newInstance(configurationClass.getPackage().getName(), loader);
        return new JaxbConfigurationParser<>(configurationClass, context, xsdStreamSource);
    }
}
