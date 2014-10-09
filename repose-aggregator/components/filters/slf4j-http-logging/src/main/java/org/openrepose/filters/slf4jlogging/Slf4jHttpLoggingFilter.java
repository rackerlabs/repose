package org.openrepose.filters.slf4jlogging;

import org.openrepose.filters.slf4jlogging.slf4jlogging.config.Slf4JHttpLoggingConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author jhopper
 */
public class Slf4jHttpLoggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(Slf4jHttpLoggingFilter.class);
    private static final String DEFAULT_CONFIG = "slf4j-http-logging.cfg.xml";
    private String config;
    private ConfigurationService manager;
    private Slf4jHttpLoggingHandlerFactory handlerFactory;

    @Override
    public void destroy() {
        manager.unsubscribeFrom(config, handlerFactory);
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
        manager = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext().configurationService();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/slf4j-http-logging-configuration.xsd");
        manager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, handlerFactory, Slf4JHttpLoggingConfig.class);
    }
}
