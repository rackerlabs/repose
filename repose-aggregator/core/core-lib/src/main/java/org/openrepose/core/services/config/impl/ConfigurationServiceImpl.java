package org.openrepose.core.services.config.impl;

import org.openrepose.commons.config.ConfigurationResourceException;
import org.openrepose.commons.config.manager.ConfigurationUpdateManager;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.ConfigurationParserFactory;
import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ConfigurationResourceResolver;
import org.openrepose.commons.config.resource.impl.FileDirectoryResourceResolver;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.jmx.ConfigurationInformation;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.event.common.EventService;
import org.openrepose.core.servlet.PowerApiContextException;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This class uses configuration info to subscribe and unsubscribe from filters.
 */

@Named
public class ConfigurationServiceImpl implements ConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationServiceImpl.class);
    private final Map<Class, WeakReference<ConfigurationParser>> parserLookaside;
    private final EventService eventService;
    private ConfigurationUpdateManager updateManager;
    private ConfigurationResourceResolver resourceResolver;
    private final ConfigurationInformation configurationInformation;
    private final String configRoot;

    //TODO: Need to add ConfigRoot to the coreSpringContext properties or something, not per-node properties
    @Inject
    public ConfigurationServiceImpl(
            EventService eventService,
            @Qualifier("reposeConfigurationInformation") ConfigurationInformation configurationInformation,
            ConfigurationUpdateManager configurationUpdateManager,
            @Value(ReposeSpringProperties.CONFIG_ROOT) String configRoot
    ) {
        this.eventService = eventService;
        this.configurationInformation = configurationInformation;
        setUpdateManager(configurationUpdateManager);
        this.configRoot = configRoot;
        parserLookaside = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        LOG.debug("Loading configuration files from directory: {}", configRoot);

        //TODO: this should be validated somewhere else, so we can fail at startup sooner
        if (StringUtilities.isBlank(configRoot)) {
            throw new PowerApiContextException("Power API requires a configuration directory in a spring property named " +
                    ReposeSpringProperties.CONFIG_ROOT);
        }

        setResourceResolver(new FileDirectoryResourceResolver(configRoot));
    }

    @Override
    public void destroy() {
        parserLookaside.clear();
        updateManager.destroy();
    }

    //Should not be part of the public facing interface.
    public void setResourceResolver(ConfigurationResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public ConfigurationResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }

    public void setUpdateManager(ConfigurationUpdateManager updateManager) {
        this.updateManager = updateManager;
    }

    @Override
    public <T> void subscribeTo(String configurationName, UpdateListener<T> listener, Class<T> configurationClass) {
        subscribeTo("", configurationName, listener, getPooledJaxbConfigurationParser(configurationClass, null), true);

    }

    @Override
    public <T> void subscribeTo(String filterName, String configurationName, UpdateListener<T> listener, Class<T> configurationClass) {
        subscribeTo(filterName, configurationName, listener, getPooledJaxbConfigurationParser(configurationClass, null), true);

    }

    @Override
    public <T> void subscribeTo(String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass) {
        subscribeTo("", configurationName, listener, getPooledJaxbConfigurationParser(configurationClass, xsdStreamSource), true);


    }

    @Override
    public <T> void subscribeTo(String filterName, String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass) {
        subscribeTo(filterName, configurationName, listener, getPooledJaxbConfigurationParser(configurationClass, xsdStreamSource), true);


    }


    @Override
    public <T> void subscribeTo(String filterName, String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser) {
        subscribeTo(filterName, configurationName, listener, customParser, true);
    }

    @Override
    public <T> void subscribeTo(String filterName, String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser, boolean sendNotificationNow) {
        final ConfigurationResource resource = resourceResolver.resolve(configurationName);
        updateManager.registerListener(listener, resource, customParser, filterName);
        if (sendNotificationNow) {
            // Initial load of the cfg object
            try {

                listener.configurationUpdated(customParser.read(resource));

                if (filterName != null && !filterName.isEmpty() && listener.isInitialized()) {
                    configurationInformation.setFilterLoadingInformation(filterName, listener.isInitialized(), resource);
                } else {
                    configurationInformation.setFilterLoadingFailedInformation(filterName, resource, "Failed loading File");
                }

            } catch (Exception ex) {
                if (filterName != null && !filterName.isEmpty()) {
                    configurationInformation.setFilterLoadingFailedInformation(filterName, resource, ex.getMessage());
                }
                // TODO:Refactor - Introduce a helper method so that this logic can be centralized and reused
                if (ex.getCause() instanceof FileNotFoundException) {
                    LOG.error("An I/O error has occurred while processing resource " + configurationName + " that is used by filter specified in system-model.cfg.xml - Reason: " + ex.getCause().getMessage());

                } else {
                    LOG.error("Configuration update error. Reason: {}", ex.getLocalizedMessage());
                    LOG.trace("", ex);
                }
            }
        }
    }

    @Override
    public void unsubscribeFrom(String configurationName, UpdateListener listener) {
        updateManager.unregisterListener(listener, resourceResolver.resolve(configurationName));
    }

    public <T> ConfigurationParser<T> getPooledJaxbConfigurationParser(Class<T> configurationClass, URL xsdStreamSource) {
        final WeakReference<ConfigurationParser> parserReference = parserLookaside.get(configurationClass);
        ConfigurationParser<T> parser = parserReference != null ? parserReference.get() : null;

        if (parser == null) {
            try {
                parser = ConfigurationParserFactory.getXmlConfigurationParser(configurationClass, xsdStreamSource);
            } catch (ConfigurationResourceException cre) {
                throw new ConfigurationServiceException("Failed to create a JAXB context for a configuration parser. Reason: " + cre.getMessage(), cre);
            }

            parserLookaside.put(configurationClass, new WeakReference<ConfigurationParser>(parser));
        }

        return parser;
    }

}
