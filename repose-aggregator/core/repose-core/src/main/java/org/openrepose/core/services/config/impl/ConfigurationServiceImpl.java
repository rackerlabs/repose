/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.config.impl;

import org.openrepose.commons.config.manager.ConfigurationUpdateManager;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.parser.common.TemplatingConfigurationParser;
import org.openrepose.commons.config.parser.jaxb.JaxbConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ConfigurationResourceResolver;
import org.openrepose.commons.config.resource.impl.FileDirectoryResourceResolver;
import org.apache.commons.lang3.StringUtils;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class uses configuration info to subscribe and unsubscribe from filters.
 */

@Named
public class ConfigurationServiceImpl implements ConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationServiceImpl.class);
    private final ConcurrentMap<ParserPoolKey, WeakReference<ConfigurationParser>> parserPoolCache;
    private final String configRoot;
    private ConfigurationUpdateManager updateManager;
    private ConfigurationResourceResolver resourceResolver;

    @Inject
    public ConfigurationServiceImpl(
            ConfigurationUpdateManager configurationUpdateManager,
            @Value(ReposeSpringProperties.CORE.CONFIG_ROOT) String configRoot
    ) {
        this.updateManager = configurationUpdateManager;
        this.configRoot = configRoot;
        parserPoolCache = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        LOG.debug("Loading configuration files from directory: {}", configRoot);

        //TODO: this should be validated somewhere else, so we can fail at startup sooner
        if (StringUtils.isBlank(configRoot)) {
            throw new PowerApiContextException("Power API requires a configuration directory in a spring property named " +
                    ReposeSpringProperties.CORE.CONFIG_ROOT);
        }

        setResourceResolver(new FileDirectoryResourceResolver(configRoot));
    }

    @Override
    public void destroy() {
        parserPoolCache.clear();
        updateManager.destroy();
    }

    @Override
    public ConfigurationResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }

    //Should not be part of the public facing interface.
    public void setResourceResolver(ConfigurationResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
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
        try {
            if (sendNotificationNow) {
                // Initial load of the cfg object
                listener.configurationUpdated(customParser.read(resource));
                //this marks the file as having been read already. DON'T REMOVE!!!!!!!
                resource.updated();
            }
        } catch (Exception ex) {
            // TODO:Refactor - Introduce a helper method so that this logic can be centralized and reused
            if (ex.getCause() instanceof FileNotFoundException) {
                LOG.error("An I/O error has occurred while processing resource {} that is used by filter specified in system-model.cfg.xml - Reason: {}", configurationName, ex.getCause().getMessage());
            } else {
                LOG.error("Configuration update error. Reason: {}", ex.getLocalizedMessage());
                LOG.trace("", ex);
            }
        } finally {
            updateManager.registerListener(listener, resource, customParser, filterName);
        }
    }

    @Override
    public void unsubscribeFrom(String configurationName, UpdateListener listener) {
        updateManager.unregisterListener(listener, resourceResolver.resolve(configurationName));
    }

    /**
     * Use the configuration class's classloader that was passed in. This should ensure that the JaxbContext knows how
     * to find the class.
     *
     * @param configurationClass
     * @param xsdStreamSource
     * @param <T>
     * @return
     */
    private <T> ConfigurationParser<T> getPooledJaxbConfigurationParser(Class<T> configurationClass, URL xsdStreamSource) {
        //The configuration class and the XSD stream source are the keys for finding a parser
        ParserPoolKey pk = new ParserPoolKey(configurationClass, xsdStreamSource);

        final WeakReference<ConfigurationParser> parserReference = parserPoolCache.get(pk);
        ConfigurationParser<T> parser = parserReference != null ? parserReference.get() : null;

        LOG.debug("Parser found from the reference is {}", parser);

        //Use the classloader of the desired marshalling destination
        ClassLoader loader = configurationClass.getClassLoader();

        if (parser == null) {
            LOG.debug("Creating new jaxbConfigurationParser for the given configuration class: {}", configurationClass);
            try {
                parser = new TemplatingConfigurationParser<>(new JaxbConfigurationParser<>(configurationClass, xsdStreamSource, loader));
            } catch (JAXBException e) {
                throw new ConfigurationServiceException("Failed to create a JAXB context for a configuration parser!", e);
            }

            parserPoolCache.put(pk, new WeakReference<>(parser));
        }

        return parser;
    }

    /**
     * Generated a class to contain Parser Pool Keys, because we've got a multi-object key
     */
    private class ParserPoolKey {
        private final Class clazz;
        private final URL xsdUrl;

        public ParserPoolKey(Class clazz, URL xsdUrl) {

            this.clazz = clazz;
            this.xsdUrl = xsdUrl;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ParserPoolKey that = (ParserPoolKey) o;

            if (clazz != null ? !clazz.equals(that.clazz) : that.clazz != null) {
                return false;
            }
            if (xsdUrl != null ? !xsdUrl.equals(that.xsdUrl) : that.xsdUrl != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = clazz != null ? clazz.hashCode() : 0;
            result = 31 * result + (xsdUrl != null ? xsdUrl.hashCode() : 0);
            return result;
        }
    }
}
