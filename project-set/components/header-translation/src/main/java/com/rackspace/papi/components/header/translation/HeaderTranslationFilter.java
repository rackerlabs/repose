package com.rackspace.papi.components.header.translation;


import com.rackspace.papi.components.header.translation.config.HeaderTranslationType;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
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
