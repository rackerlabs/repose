package com.rackspace.papi.components.identity.header;

import com.rackspace.papi.components.identity.header.config.HeaderIdentityConfig;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import java.io.IOException;
import javax.servlet.*;

public class HeaderIdentityFilter implements Filter {

    private HeaderIdentityHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("header-identity.cfg.xml", handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        configurationManager = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new HeaderIdentityHandlerFactory();

        configurationManager.subscribeTo("header-identity.cfg.xml", handlerFactory, HeaderIdentityConfig.class);
    }
}
