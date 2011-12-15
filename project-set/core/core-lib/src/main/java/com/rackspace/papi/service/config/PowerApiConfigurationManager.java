package com.rackspace.papi.service.config;

import com.rackspace.papi.commons.config.ConfigurationResourceException;
import com.rackspace.papi.commons.config.manager.ConfigurationUpdateManager;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.ConfigurationParser;
import com.rackspace.papi.commons.config.parser.ConfigurationParserFactory;
import com.rackspace.papi.commons.config.parser.ConfigurationParserType;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerApiConfigurationManager implements ConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(PowerApiConfigurationManager.class);
    private final Map<Class, WeakReference<ConfigurationParser>> parserLookaside;
    private ConfigurationUpdateManager updateManager;
    private ConfigurationResourceResolver resourceResolver;

    public PowerApiConfigurationManager() {
        parserLookaside = new HashMap<Class, WeakReference<ConfigurationParser>>();
    }

    public void destroy() {
        parserLookaside.clear();
        updateManager.destroy();
    }

    public void setResourceResolver(ConfigurationResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public void setUpdateManager(ConfigurationUpdateManager updateManager) {
        this.updateManager = updateManager;
    }

    @Override
    public <T> void subscribeTo(String configurationName, UpdateListener<T> listener, Class<T> configurationClass) {
       subscribeTo(configurationName, listener, getPooledJaxbConfigurationParser(configurationClass));
    }

   @Override
    public <T> void subscribeTo(String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser) {
        final ConfigurationResource resource = resourceResolver.resolve(configurationName);
        updateManager.registerListener(listener, resource, customParser);

        // Initial load of the cfg object
        try {
            listener.configurationUpdated(customParser.read(resource));
        } catch (Exception ex) {
            LOG.error("Configuration update error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void unsubscribeFrom(String configurationName, UpdateListener listener) {
        updateManager.unregisterListener(listener, resourceResolver.resolve(configurationName));
    }

    public <T> ConfigurationParser<T> getPooledJaxbConfigurationParser(Class<T> configurationClass) {
        final WeakReference<ConfigurationParser> parserReference = parserLookaside.get(configurationClass);
        ConfigurationParser<T> parser = parserReference != null ? parserReference.get() : null;

        if (parser == null) {
            try {
                parser = ConfigurationParserFactory.getXmlConfigurationParser(configurationClass);
            } catch (ConfigurationResourceException cre) {
                throw new ConfigurationServiceException("Failed to create a JAXB context for a configuration parser. Reason: " + cre.getMessage(), cre);
            }

            parserLookaside.put(configurationClass, new WeakReference<ConfigurationParser>(parser));
        }

        return parser;
    }
}
