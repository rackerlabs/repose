package com.rackspace.papi.components.identity.header;

import com.rackspace.papi.components.identity.header.config.HeaderIdentityConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;

import javax.servlet.*;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderIdentityFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(HeaderIdentityFilter.class);
    private static String DEFAULT_CONFIG = "header-identity.cfg.xml";
    private String config;
    private HeaderIdentityHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        configurationManager = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new HeaderIdentityHandlerFactory();

        configurationManager.subscribeTo(config, handlerFactory, HeaderIdentityConfig.class);
    }
}
