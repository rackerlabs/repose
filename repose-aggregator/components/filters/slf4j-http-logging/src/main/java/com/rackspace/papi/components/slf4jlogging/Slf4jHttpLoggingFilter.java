package com.rackspace.papi.components.slf4jlogging;

import com.rackspace.papi.components.slf4jlogging.config.Slf4JHttpLoggingConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.service.config.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class Slf4jHttpLoggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(Slf4jHttpLoggingFilter.class);
    private static final String DEFAULT_CONFIG = "slf4j-http-logging.cfg.xml";
    private String config;
    private final ConfigurationService configurationService;
    private Slf4jHttpLoggingHandlerFactory handlerFactory;

    @Inject
    public Slf4jHttpLoggingFilter(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new Slf4jHttpLoggingHandlerFactory();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/slf4j-http-logging-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, Slf4JHttpLoggingConfig.class);
    }
}
