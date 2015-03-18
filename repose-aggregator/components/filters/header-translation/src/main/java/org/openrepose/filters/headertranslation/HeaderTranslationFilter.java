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
package org.openrepose.filters.headertranslation;


import org.openrepose.filters.headertranslation.config.HeaderTranslationType;
import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class HeaderTranslationFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(HeaderTranslationFilter.class);
    private static final String DEFAULT_CONFIG = "header-translation.cfg.xml";
    private String config;
    private HeaderTranslationHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;

    @Inject
    public HeaderTranslationFilter(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new HeaderTranslationHandlerFactory();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/header-translation.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, HeaderTranslationType.class);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain).doFilter(
                handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(config, handlerFactory);
    }
}
