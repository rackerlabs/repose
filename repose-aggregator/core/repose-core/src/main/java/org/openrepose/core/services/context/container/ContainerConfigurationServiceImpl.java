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
package org.openrepose.core.services.context.container;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.container.config.DeploymentConfiguration;
import org.openrepose.core.services.config.ConfigurationService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.Optional;

/**
 * This service has been deprecated. Use
 * {@link org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService} instead.
 */
@Named
@Deprecated
public class ContainerConfigurationServiceImpl implements ContainerConfigurationService {

    private final ConfigurationService configurationService;
    private final ContainerConfigurationListener containerConfigurationListener;

    private String viaValue;
    private Long contentBodyReadLimit;

    @Inject
    public ContainerConfigurationServiceImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
        this.containerConfigurationListener = new ContainerConfigurationListener();
    }

    @PostConstruct
    public void init() {
        URL xsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");
        configurationService.subscribeTo("container.cfg.xml", xsdURL, containerConfigurationListener, ContainerConfiguration.class);
    }

    @PreDestroy
    public void destroy() {
        if (configurationService != null) {
            configurationService.unsubscribeFrom("container.cfg.xml", containerConfigurationListener);
        }
    }

    @Override
    public String getVia() {
        return viaValue;
    }

    @Override
    public Optional<Long> getContentBodyReadLimit() {
        return Optional.ofNullable(contentBodyReadLimit);
    }

    /**
     * Listens for updates to the container.cfg.xml file which holds the
     * location of the log properties file.
     */
    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {
            DeploymentConfiguration deployConfig = configurationObject.getDeploymentConfig();

            viaValue = deployConfig.getVia();
            contentBodyReadLimit = deployConfig.getContentBodyReadLimit();

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
