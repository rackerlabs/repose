package com.rackspace.papi.components.identity.uri;


import com.rackspace.papi.components.identity.uri.config.UriIdentityConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class UriIdentityFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(UriIdentityFilter.class);
    private static final String DEFAULT_CONFIG = "uri-identity.cfg.xml";
    private String config;
    private UriIdentityHandlerFactory handlerFactory;
    private ConfigurationService configurationService;

    @Inject
    public UriIdentityFilter(ConfigurationService configurationService) {
        this.configurationService = configurationService;
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
        handlerFactory = new UriIdentityHandlerFactory();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/uri-identity-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, UriIdentityConfig.class);
    }
}
