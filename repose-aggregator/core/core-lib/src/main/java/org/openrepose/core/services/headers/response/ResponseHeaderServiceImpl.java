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
package org.openrepose.core.services.headers.response;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.RouteDestination;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.headers.common.ViaHeaderBuilder;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;

@Named
public class ResponseHeaderServiceImpl implements ResponseHeaderService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ResponseHeaderServiceImpl.class);

    private final String reposeVersion;
    private final ConfigurationService configurationService;
    private final ContainerConfigurationListener configurationListener;

    private ViaHeaderBuilder viaHeaderBuilder;
    private LocationHeaderBuilder locationHeaderBuilder;

    @Inject
    public ResponseHeaderServiceImpl(ConfigurationService configurationService,
                                     @Value(ReposeSpringProperties.CORE.REPOSE_VERSION) String reposeVersion) {
        this.configurationService = configurationService;
        this.configurationListener = new ContainerConfigurationListener();
        this.reposeVersion = reposeVersion;
    }

    @PostConstruct
    public void init() {
        URL xsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");

        configurationService.subscribeTo("container.cfg.xml", xsdURL, configurationListener, ContainerConfiguration.class);
    }

    @PreDestroy
    public void destroy() {
        configurationService.unsubscribeFrom("container.cfg.xml", configurationListener);
    }

    public synchronized void updateConfig(ViaHeaderBuilder viaHeaderBuilder, LocationHeaderBuilder locationHeaderBuilder) {
        this.viaHeaderBuilder = viaHeaderBuilder;
        this.locationHeaderBuilder = locationHeaderBuilder;
    }

    @Override
    public void setVia(MutableHttpServletRequest request, HttpServletResponse response) {
        final String existingVia = response.getHeader(CommonHttpHeader.VIA.toString());
        final String myVia = viaHeaderBuilder.buildVia(request);
        final String via = StringUtilities.isBlank(existingVia) ? myVia : existingVia + ", " + myVia;

        response.setHeader(CommonHttpHeader.VIA.name(), via);
    }

    @Override
    public void fixLocationHeader(HttpServletRequest originalRequest, HttpServletResponse response, RouteDestination destination, String destinationLocationUri, String proxiedRootContext) {
        String destinationUri = cleanPath(destinationLocationUri);
        if (!destinationUri.matches("^https?://.*")) {
            // local dispatch
            destinationUri = proxiedRootContext;
        }
        try {
            locationHeaderBuilder.setLocationHeader(originalRequest, response, destinationUri, destination.getContextRemoved(), proxiedRootContext);
        } catch (MalformedURLException ex) {
            LOG.warn("Invalid URL in location header processing", ex);
        }
    }

    private String cleanPath(String uri) {
        return uri == null ? "" : uri.split("\\?")[0];
    }

    /**
     * Listens for updates to the container.cfg.xml file which holds the via
     * header receivedBy value.
     */
    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {
            if (configurationObject.getDeploymentConfig() != null) {
                final String viaReceivedBy = configurationObject.getDeploymentConfig().getVia();

                final ViaResponseHeaderBuilder viaBuilder = new ViaResponseHeaderBuilder(reposeVersion, viaReceivedBy);
                final LocationHeaderBuilder locationBuilder = new LocationHeaderBuilder();
                updateConfig(viaBuilder, locationBuilder);
            }
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
