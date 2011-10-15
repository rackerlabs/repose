package com.rackspace.papi.components.serviceauth;

import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.serviceauth.config.ServiceAuthConfig;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 * @author jhopper
 */
public class ServiceAuthenticationFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ServiceAuthenticationFilter.class);
    private ServiceAuthenticationHandlerFactory handlerFactory;

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

        final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
        final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap((HttpServletResponse) response);
        
        handlerFactory.newHandler().handleRequest(mutableHttpRequest, mutableHttpResponse);

        //Continue on
        chain.doFilter(mutableHttpRequest, mutableHttpResponse);
        handlerFactory.newHandler().handleResponse(mutableHttpRequest, mutableHttpResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        handlerFactory = new ServiceAuthenticationHandlerFactory();
        final ConfigurationService manager = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();

        manager.subscribeTo("service-auth.cfg.xml", handlerFactory, ServiceAuthConfig.class);
    }
}
