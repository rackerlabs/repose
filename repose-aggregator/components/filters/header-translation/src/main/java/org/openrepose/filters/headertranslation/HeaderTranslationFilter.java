package org.openrepose.filters.headertranslation;


import org.openrepose.filters.headertranslation.config.HeaderTranslationType;
import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ServletContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

public class HeaderTranslationFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(HeaderTranslationFilter.class);
    private static final String DEFAULT_CONFIG = "header-translation.cfg.xml";
    private String config;
    private HeaderTranslationHandlerFactory headerTranslationHandlerFactory;
    private ConfigurationService configurationService;


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        configurationService = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext().configurationService();
        headerTranslationHandlerFactory = new HeaderTranslationHandlerFactory();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/header-translation.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, headerTranslationHandlerFactory, HeaderTranslationType.class);

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain).doFilter(
                headerTranslationHandlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(config, headerTranslationHandlerFactory);
    }
}
