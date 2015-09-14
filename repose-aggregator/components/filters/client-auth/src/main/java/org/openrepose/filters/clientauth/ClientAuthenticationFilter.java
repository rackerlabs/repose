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
package org.openrepose.filters.clientauth;

import org.openrepose.commons.config.manager.UpdateFailedException;
import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.filters.clientauth.config.ClientAuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
@Deprecated
public class ClientAuthenticationFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ClientAuthenticationFilter.class);
    private static final String DEFAULT_CONFIG = "client-auth-n.cfg.xml";
    private final DatastoreService datastoreService;
    private final ConfigurationService configurationService;
    private final HttpClientService httpClientService;
    private final AkkaServiceClient akkaServiceClient;
    private final String reposeVersion;
    private String config;
    private ClientAuthenticationHandlerFactory handlerFactory;
    private SystemModelConfigurationListener systemModelConfigurationListener;

    @Inject
    public ClientAuthenticationFilter(DatastoreService datastoreService,
                                      ConfigurationService configurationService,
                                      HttpClientService httpClientService,
                                      AkkaServiceClient akkaServiceClient,
                                      @Value(ReposeSpringProperties.CORE.REPOSE_VERSION) String reposeVersion) {
        this.datastoreService = datastoreService;
        this.httpClientService = httpClientService;
        this.akkaServiceClient = akkaServiceClient;
        this.configurationService = configurationService;
        this.reposeVersion = reposeVersion;
    }

    @Override
    public void destroy() {
        handlerFactory.stopFeeds();
        configurationService.unsubscribeFrom(config, handlerFactory);
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelConfigurationListener);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.warn("This filter is deprecated; use the keystone-v2 filter");
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new ClientAuthenticationHandlerFactory(datastoreService.getDefaultDatastore(), httpClientService, akkaServiceClient, reposeVersion);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/client-auth-n-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, ClientAuthConfig.class);
        URL smXsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        systemModelConfigurationListener = new SystemModelConfigurationListener();
        configurationService.subscribeTo("", "system-model.cfg.xml", smXsdURL, systemModelConfigurationListener, SystemModel.class);

    }

    private class SystemModelConfigurationListener implements org.openrepose.commons.config.manager.UpdateListener<SystemModel> {

        private boolean initialized = false;
        @Override
        public void configurationUpdated(SystemModel configurationObject) throws UpdateFailedException {
            handlerFactory.setOutboundTracing(configurationObject.isTracingHeader());
            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }
}
