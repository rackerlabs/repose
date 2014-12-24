package org.openrepose.core.services.config.impl;

import org.openrepose.commons.config.manager.ConfigurationUpdateManager;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.parser.jaxb.JaxbConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ConfigurationResourceResolver;
import org.openrepose.commons.config.resource.impl.FileDirectoryResourceResolver;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.servlet.PowerApiContextException;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBException;
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
    private ConfigurationUpdateManager updateManager;
    private ConfigurationResourceResolver resourceResolver;
    private final String configRoot;

    @Inject
    public ConfigurationServiceImpl(
            ConfigurationUpdateManager configurationUpdateManager,
            @Value(ReposeSpringProperties.CORE.CONFIG_ROOT) String configRoot
    ) {
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
                    ReposeSpringProperties.CORE.CONFIG_ROOT);
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
            } catch (Exception ex) {
                // TODO:Refactor - Introduce a helper method so that this logic can be centralized and reused
                if (ex.getCause() instanceof FileNotFoundException) {
                    LOG.error("An I/O error has occurred while processing resource {} that is used by filter specified in system-model.cfg.xml - Reason: {}", configurationName, ex.getCause().getMessage());
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

    /**
     * This goes through all the earclassloader contexts and tries to find a classloader that can marshal the jaxb stuff
     * Unfortunately it is not aware of changes to the artifacts and such, and so this will be a source of bugs in the future.
     * NOTE: when artifacts are replaced or reloaded or changed, this guy won't update stuff, it will be a source of bugs
     *
     * @param configurationClass
     * @param xsdStreamSource
     * @param <T>
     * @return
     */
    private <T> ConfigurationParser<T> getPooledJaxbConfigurationParser(Class<T> configurationClass, URL xsdStreamSource) {
        final WeakReference<ConfigurationParser> parserReference = parserLookaside.get(configurationClass);
        ConfigurationParser<T> parser = parserReference != null ? parserReference.get() : null;

        //Use the classloader of the desired marshalling destination
        ClassLoader loader = configurationClass.getClassLoader();

        if (parser == null) {
            try {
                parser = JaxbConfigurationParser.getXmlConfigurationParser(configurationClass, xsdStreamSource, loader);
            } catch (JAXBException e) {
                throw new ConfigurationServiceException("Failed to create a JAXB context for a configuration parser!", e);
            }

            parserLookaside.put(configurationClass, new WeakReference<ConfigurationParser>(parser));
        }

        return parser;
    }

}
