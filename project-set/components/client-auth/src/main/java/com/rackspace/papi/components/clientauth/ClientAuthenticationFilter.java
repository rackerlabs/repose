package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
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
public class ClientAuthenticationFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientAuthenticationFilter.class);
    private ClientAuthenticationHandler handler;
    private ConfigurationService configurationManager;

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("client-auth-n.cfg.xml", handler.getClientAuthenticationConfigurationListener());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

        final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
        final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap((HttpServletResponse) response);

        final FilterDirector director = handler.handleRequest(mutableHttpRequest, mutableHttpResponse);

        if (director.requestHeaderManager().hasHeaders()) {
            director.requestHeaderManager().applyTo(mutableHttpRequest);
        }

        if (director.responseHeaderManager().hasHeaders()) {
            director.responseHeaderManager().applyTo(mutableHttpResponse);
        }

        switch (director.getFilterAction()) {
            case PASS:
                chain.doFilter(mutableHttpRequest, mutableHttpResponse);
                handler.handleResponse(mutableHttpRequest, mutableHttpResponse);
                break;

            default:
                mutableHttpResponse.setStatus(director.getResponseStatus().intValue());
                break;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        handler = new ClientAuthenticationHandler();
        ServletContext servletContext = filterConfig.getServletContext();
        configurationManager = ServletContextHelper.getPowerApiContext(servletContext).configurationService();

        configurationManager.subscribeTo("client-auth-n.cfg.xml", handler.getClientAuthenticationConfigurationListener(), ClientAuthConfig.class);
    }
}
