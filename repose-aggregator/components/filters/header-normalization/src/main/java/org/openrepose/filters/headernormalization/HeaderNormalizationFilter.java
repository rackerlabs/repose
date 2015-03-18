/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.filters.headernormalization;

import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.filters.headernormalization.config.HeaderNormalizationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class HeaderNormalizationFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(HeaderNormalizationFilter.class);
    private static final String DEFAULT_CONFIG = "header-normalization.cfg.xml";
    private String config;
    private HeaderNormalizationHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;
    private final MetricsService metricsService;

    @Inject
    public HeaderNormalizationFilter(ConfigurationService configurationService,
                                     MetricsService metricsService) {
        this.configurationService = configurationService;
        this.metricsService = metricsService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new HeaderNormalizationHandlerFactory(metricsService);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/header-normalization-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, HeaderNormalizationConfig.class);
    }
}
