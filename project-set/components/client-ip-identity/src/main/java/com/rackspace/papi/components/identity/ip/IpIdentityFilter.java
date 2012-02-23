package com.rackspace.papi.components.identity.ip;


import com.rackspace.papi.components.identity.ip.config.IpIdentityConfig;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import java.io.IOException;
import javax.servlet.*;

public class IpIdentityFilter implements Filter {

    private IpIdentityHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("ip-identity.cfg.xml", handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        configurationManager = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new IpIdentityHandlerFactory();

        configurationManager.subscribeTo("ip-identity.cfg.xml", handlerFactory, IpIdentityConfig.class);
    }
}
