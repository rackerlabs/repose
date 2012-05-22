package com.rackspace.papi.components.service.authentication;


import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;

import javax.servlet.*;
import java.io.IOException;

public class ServiceAuthFilter implements Filter {

    private ServiceAuthHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("service-authentication.cfg.xml", handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        configurationManager = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new ServiceAuthHandlerFactory();

        configurationManager.subscribeTo("service-authentication.cfg.xml", handlerFactory, ServiceAuthenticationConfig.class);
    }
}
