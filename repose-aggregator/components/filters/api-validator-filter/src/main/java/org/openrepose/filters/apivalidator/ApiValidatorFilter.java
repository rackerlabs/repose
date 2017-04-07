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
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

@Named
public class ApiValidatorFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorFilter.class);
    private static final String DEFAULT_CONFIG = "validator.cfg.xml";
    private static final String SCHEMA_FILE_NAME = "/META-INF/schema/config/validator-configuration.xsd";

    private final ConfigurationService configurationService;
    private final Optional<MetricsService> metricsService;
    private String configFileName;
    private ApiValidatorHandlerFactory handlerFactory;
    private String configurationRoot;

    @Inject
    public ApiValidatorFilter(
            ConfigurationService configurationService,
            Optional<MetricsService> metricsService,
            @Value(ReposeSpringProperties.CORE.CONFIG_ROOT) String configurationRoot) {
        this.configurationService = configurationService;
        this.metricsService = metricsService;
        this.configurationRoot = configurationRoot;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.trace("API Validator filter initializing...");
        configFileName = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);

        LOG.info("Initializing API Validator filter using config " + configFileName);
        handlerFactory = new ApiValidatorHandlerFactory(configurationService, configurationRoot, configFileName, metricsService);
        URL xsdURL = getClass().getResource(SCHEMA_FILE_NAME);
        configurationService.subscribeTo(filterConfig.getFilterName(), configFileName, xsdURL, handlerFactory, ValidatorConfiguration.class);

        LOG.trace("API Validator filter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ApiValidatorHandler handler = handlerFactory.buildHandler();

        if (handler == null) {
            LOG.error("API Validator filter has not yet initialized... Please check your configuration files and your artifacts directory.");
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else {
            handler.doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        }
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(configFileName, handlerFactory);
    }
}
