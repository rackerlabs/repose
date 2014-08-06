package com.rackspace.papi.components.service.authentication;


import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.service.config.ConfigurationService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class ServiceAuthFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ServiceAuthFilter.class);
    private static final String DEFAULT_CONFIG = "service-authentication.cfg.xml";
    private String config;
    private ServiceAuthHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;

    @Inject
    public ServiceAuthFilter(ConfigurationService configurationService) {
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
        handlerFactory = new ServiceAuthHandlerFactory();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/service-auth-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, ServiceAuthenticationConfig.class);
    }
}
