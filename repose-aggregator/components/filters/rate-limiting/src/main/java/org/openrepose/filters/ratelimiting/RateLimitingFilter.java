package org.openrepose.filters.ratelimiting;

import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.services.datastore.DatastoreService;
import org.openrepose.services.ratelimit.config.RateLimitingConfiguration;
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
