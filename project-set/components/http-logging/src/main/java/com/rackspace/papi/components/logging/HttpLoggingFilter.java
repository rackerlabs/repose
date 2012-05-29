package com.rackspace.papi.components.logging;

import com.rackspace.papi.components.logging.config.HttpLoggingConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import java.io.IOException;
import javax.servlet.*;

/**
 *
 * @author jhopper
 */
public class HttpLoggingFilter implements Filter {

    private static String DEFAULT_CONFIG = "http-logging.cfg.xml";
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
        handlerFactory = new HttpLoggingHandlerFactory();
        manager = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
        manager.subscribeTo(config, handlerFactory, HttpLoggingConfig.class);
    }
}
