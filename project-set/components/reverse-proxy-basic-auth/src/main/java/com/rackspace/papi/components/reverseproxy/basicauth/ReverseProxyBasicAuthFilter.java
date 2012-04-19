package com.rackspace.papi.components.reverseproxy.basicauth;


import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import java.io.IOException;
import javax.servlet.*;

public class ReverseProxyBasicAuthFilter implements Filter {

    private ReverseProxyBasicAuthHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("reverse-proxy-basic-auth.cfg.xml", handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        configurationManager = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new ReverseProxyBasicAuthHandlerFactory();

        configurationManager.subscribeTo("reverse-proxy-basic-auth.cfg.xml", handlerFactory, ReverseProxyBasicAuthConfig.class);
    }
}
