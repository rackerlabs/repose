package com.rackspace.papi.service.config;

import com.rackspace.papi.commons.config.manager.ConfigurationUpdateManager;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.ConfigurationObjectParser;
import com.rackspace.papi.commons.config.parser.jaxb.JaxbConfigurationObjectParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerApiConfigurationManager implements ConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(PowerApiConfigurationManager.class);
    private final Map<Class, WeakReference<ConfigurationObjectParser>> parserLookaside;
    private ConfigurationUpdateManager updateManager;
    private ConfigurationResourceResolver resourceResolver;

    public PowerApiConfigurationManager() {
        parserLookaside = new HashMap<Class, WeakReference<ConfigurationObjectParser>>();
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
        final ConfigurationObjectParser<T> parser = getJaxbConfigurationParser(configurationClass);
        final ConfigurationResource resource = resourceResolver.resolve(configurationName);

        updateManager.registerListener(listener, resource, parser);

        // Initial load of the cfg object
        try {
            listener.configurationUpdated(parser.read(resource));
        } catch (Exception ex) {
            LOG.error("Configuration update error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void unsubscribeFrom(String configurationName, UpdateListener listener) {
        updateManager.unregisterListener(listener, resourceResolver.resolve(configurationName));
    }

    public <T> ConfigurationObjectParser<T> getJaxbConfigurationParser(Class<T> configurationClass) {
        final WeakReference<ConfigurationObjectParser> parserReference = parserLookaside.get(configurationClass);
        ConfigurationObjectParser<T> parser = parserReference != null ? parserReference.get() : null;

        if (parser == null) {
            try {
                final JAXBContext jaxbCtx = JAXBContext.newInstance(configurationClass.getPackage().getName());
                parser = new JaxbConfigurationObjectParser<T>(configurationClass, jaxbCtx);
            } catch (JAXBException jaxbe) {
                throw new ConfigurationServiceException("Failed to create a JAXB context for a configuration parser. Reason: " + jaxbe.getMessage(), jaxbe);
            }

            parserLookaside.put(configurationClass, new WeakReference<ConfigurationObjectParser>(parser));
        }

        return parser;
    }
}
