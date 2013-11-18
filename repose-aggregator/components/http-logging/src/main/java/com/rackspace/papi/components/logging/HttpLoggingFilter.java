package com.rackspace.papi.components.logging;

import com.rackspace.papi.components.logging.config.HttpLoggingConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import java.io.IOException;
import java.net.URL;
import javax.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.util.logging.resources.logging;

/**
 *
 * @author jhopper
 */
public class HttpLoggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(HttpLoggingFilter.class);
    private static final String DEFAULT_CONFIG = "http-logging.cfg.xml";
    private String config;
    private ConfigurationService manager;
    private HttpLoggingHandlerFactory handlerFactory;

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
        handlerFactory = new HttpLoggingHandlerFactory();
        manager = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext().configurationService();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/http-logging-configuration.xsd");
        manager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, handlerFactory, HttpLoggingConfig.class);
    }
}
