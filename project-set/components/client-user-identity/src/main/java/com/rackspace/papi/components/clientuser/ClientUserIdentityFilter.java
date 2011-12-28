package com.rackspace.papi.components.clientuser;


import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.clientuser.config.ClientUserIdentityConfig;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;

public class ClientUserIdentityFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientUserIdentityFilter.class);
    private ClientUserIdentityHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

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
                chain.doFilter(mutableHttpRequest, mutableHttpResponse);
                break;
        }
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("client-user-identity.cfg.xml", handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        configurationManager = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new ClientUserIdentityHandlerFactory();

        configurationManager.subscribeTo("client-user-identity.cfg.xml", handlerFactory, ClientUserIdentityConfig.class);
    }
}
