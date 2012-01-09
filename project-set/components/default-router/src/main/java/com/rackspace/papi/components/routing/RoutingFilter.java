package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.model.PowerProxy;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

public class RoutingFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RoutingFilter.class);
    private RoutingHandlerFactory handlerFactory;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

        final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
        final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap((HttpServletResponse) response);

        final FilterDirector director = handlerFactory.newHandler().handleRequest(mutableHttpRequest, mutableHttpResponse);

        director.applyTo(mutableHttpRequest);

        switch (director.getFilterAction()) {
            case RETURN:
                break;

            case PASS:
                chain.doFilter(mutableHttpRequest, response);
                break;
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final ConfigurationService manager = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new RoutingHandlerFactory(ServletContextHelper.getServerPort(filterConfig.getServletContext()));

        manager.subscribeTo("power-proxy.cfg.xml", handlerFactory, PowerProxy.class);
    }
}
