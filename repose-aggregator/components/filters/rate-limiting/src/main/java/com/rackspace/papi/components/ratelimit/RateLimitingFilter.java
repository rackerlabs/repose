package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.openrepose.core.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class RateLimitingFilter implements Filter {

    private static final String DEFAULT_CONFIG = "rate-limiting.cfg.xml";
    private String config;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingFilter.class);
    private RateLimitingHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;
    private final DatastoreService datastoreService;

    @Inject
    public RateLimitingFilter(
            DatastoreService datastoreService,
            ConfigurationService configurationService) {
        this.datastoreService = datastoreService;
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

        filterConfig.getFilterName();
        handlerFactory = new RateLimitingHandlerFactory(datastoreService);

        URL xsdURL = getClass().getResource("/META-INF/schema/config/rate-limiting-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, RateLimitingConfiguration.class);
    }
}
