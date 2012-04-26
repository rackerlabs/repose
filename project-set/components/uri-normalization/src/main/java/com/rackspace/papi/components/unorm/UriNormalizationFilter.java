package com.rackspace.papi.components.unorm;

import com.rackspace.papi.components.uri.normalization.config.UriNormalizationConfig;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class UriNormalizationFilter implements Filter {

    private UriNormalizationHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("uri-normalization.cfg.xml", handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        configurationManager = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new UriNormalizationHandlerFactory();

        configurationManager.subscribeTo("uri-normalization.cfg.xml", handlerFactory, UriNormalizationConfig.class);
    }
}
