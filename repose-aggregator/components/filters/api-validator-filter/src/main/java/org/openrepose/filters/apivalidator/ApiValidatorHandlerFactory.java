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
package org.openrepose.filters.apivalidator;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.generic.GenericResourceConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * This class uses the <a href="http://en.wikipedia.org/wiki/Factory_method_pattern">factory pattern</a> to construct
 * a handler from the configuration files.
 * <p/>
 * This class and ApiValidatorWadlListener are classes which re-initializes this handler factory
 * when the WADL or configuration files are changed.
 */
public class ApiValidatorHandlerFactory implements UpdateListener<ValidatorConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorHandlerFactory.class);
    private final ConfigurationService configurationService;
    private final ApiValidatorWadlListener wadlListener;
    private final Object wadlLock = new Object();
    private final Object configLock = new Object();
    private final String configRoot;
    private final String config;
    private final Optional<MetricsService> metricsService;
    private ValidatorConfiguration validatorConfiguration;
    private ValidatorInfo defaultValidator;
    private List<ValidatorInfo> validators;
    private volatile boolean initialized = false;
    private boolean multiRoleMatch = false;
    private boolean delegatingMode;

    public ApiValidatorHandlerFactory(
            ConfigurationService configurationService,
            String configurationRoot,
            String config,
            Optional<MetricsService> metricsService) {
        this.configurationService = configurationService;
        this.configRoot = configurationRoot;
        this.config = config;
        this.metricsService = metricsService;
        this.wadlListener = new ApiValidatorWadlListener();
    }

    public ApiValidatorHandler buildHandler() {
        synchronized (configLock) {
            initialize();
            if (!initialized) {
                return null;
            }
            return new ApiValidatorHandler(defaultValidator, validators, multiRoleMatch, delegatingMode, metricsService);
        }
    }

    @Override
    public void configurationUpdated(ValidatorConfiguration configurationObject) {
        synchronized (configLock) {
            validatorConfiguration = configurationObject;
            unsubscribeAllWadlListeners();
            initialize();
            initialized = true;
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    private void initialize() {
        synchronized (wadlLock) {
            if (initialized || validatorConfiguration == null) {
                return;
            }

            ValidatorConfigurator validatorConfigurator = new ValidatorConfigurator(validatorConfiguration, configRoot, config);

            multiRoleMatch = validatorConfiguration.isMultiRoleMatch();
            defaultValidator = validatorConfigurator.getDefaultValidator();
            validators = validatorConfigurator.getValidators();
            delegatingMode = validatorConfiguration.getDelegating() != null;

            for (ValidatorInfo validator : validators) {
                LOG.debug("Adding listener for {} : {}", validator.getName(), validator.getUri());
                addWadlListener(validator.getUri());
            }

            initialized = true;
        }
    }

    private void addWadlListener(String wadl) {
        if (wadl == null) {
            return;
        }
        //TODO: what if it's already subscribed? How do we know this?
        LOG.info("Watching WADL: " + wadl);
        configurationService.subscribeTo("api-validator", wadl, wadlListener, new GenericResourceConfigurationParser());
    }

    private void unsubscribeAllWadlListeners() {
        synchronized (wadlLock) {
            initialized = false;
            if (validators == null) {
                return;
            }

            for (ValidatorInfo info : validators) {
                if (StringUtilities.isNotBlank(info.getUri())) {
                    configurationService.unsubscribeFrom(info.getUri(), wadlListener);
                }
                if (info.getValidator() != null) {
                    LOG.debug("DESTROYING VALIDATOR: {}", info.getName());
                    info.getValidator().destroy();
                }
            }
        }
    }

    ApiValidatorWadlListener getWadlListener() {
        return wadlListener;
    }

    void setValidators(List<ValidatorInfo> validators) {
        this.validators = validators;
    }

    String getWadlPath(String uri) {
        return new File(configRoot, uri).toURI().toString();
    }

    public class ApiValidatorWadlListener implements UpdateListener<ConfigurationResource> {
        private boolean isInitialized = false;

        private String getNormalizedPath(String uri) {
            String path = uri;
            try {
                path = new URL(uri).toString();
            } catch (MalformedURLException ex) {
                LOG.warn("Invalid URL: " + uri, ex);
            }
            return path;
        }

        @Override
        public void configurationUpdated(ConfigurationResource config) {
            LOG.info("WADL file changed: " + config.name());

            synchronized (wadlLock) {
                if (validators == null) {
                    return;
                }
                boolean validatorForWADLFound = false;
                boolean allReloadedWADL = true;

                for (ValidatorInfo info : validators) {
                    LOG.debug("Checking config for validator: {}", info.getName());
                    if (info.getUri() != null && getNormalizedPath(info.getUri()).equals(config.name())) {
                        LOG.debug("REINIT validator: {}", info.getName());
                        allReloadedWADL = info.reinitValidator() && allReloadedWADL;
                        validatorForWADLFound = true;
                    }
                }

                if (!validatorForWADLFound) {
                    LOG.debug("Didn't match a particular config, so reinit *all* the validators");
                    for (ValidatorInfo info : validators) {
                        LOG.debug("REINIT valdiator: {}", info.getName());
                        info.reinitValidator();
                    }
                }

                isInitialized = allReloadedWADL;
            }
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
