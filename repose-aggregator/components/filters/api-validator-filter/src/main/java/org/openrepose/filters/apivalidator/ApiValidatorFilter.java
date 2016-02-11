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

import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration;
import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class ApiValidatorFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorFilter.class);
    private static final String DEFAULT_CONFIG = "validator.cfg.xml";
    private final ConfigurationService configurationService;
    private final MetricsService metricsService;
    private String config;
    private ApiValidatorHandlerFactory handlerFactory;
    private String configurationRoot;

    @Inject
    public ApiValidatorFilter(ConfigurationService configurationService,
                              MetricsService metricsService,
                              @Value(ReposeSpringProperties.CORE.CONFIG_ROOT) String configurationRoot) {
        this.configurationService = configurationService;
        this.metricsService = metricsService;
        this.configurationRoot = configurationRoot;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ApiValidatorHandler handler = handlerFactory.newHandler();
        if (handler != null) {
            handler.setFilterChain(chain);
        } else {
            LOG.error("Unable to build API validator handler");
        }
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handler);
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new ApiValidatorHandlerFactory(configurationService, configurationRoot, config, metricsService);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/validator-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, ValidatorConfiguration.class);
    }
}
