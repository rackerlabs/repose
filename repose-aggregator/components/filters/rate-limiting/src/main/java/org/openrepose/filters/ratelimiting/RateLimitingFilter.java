package org.openrepose.filters.ratelimiting;

import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.service.config.ConfigurationService;
import org.openrepose.core.service.context.ContextAdapter;
import org.openrepose.core.service.context.ServletContextHelper;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;
import org.slf4j.Logger;

import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

public class RateLimitingFilter implements Filter {

    private static final String DEFAULT_CONFIG = "rate-limiting.cfg.xml";
    private String config;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingFilter.class);
    private RateLimitingHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;
  
    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final ContextAdapter ctx = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext();
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);

        filterConfig.getFilterName();
        handlerFactory = new RateLimitingHandlerFactory(ctx.datastoreService());
        configurationManager = ctx.configurationService();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/rate-limiting-configuration.xsd");
        configurationManager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, handlerFactory, RateLimitingConfiguration.class);
              
       
    }
}
