package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.datastore.hash.HashRingDatastoreManager;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.context.jndi.ContextAdapter;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.slf4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 * @author jhopper
 */
public class RateLimitingFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingFilter.class);
    private RateLimitingHandlerFactory handlerFactory;

    @Override
    public void destroy() {
        // TODO: In order to unsubscribe from configuration updates we need access to the configuration manager.
        // Do we want to keep the ConfigurationManager as a private variable that gets set on initialization?
//        configurationManager().unsubscribeFrom("rate-limiting.cfg.xml", handler.getRateLimitingConfigurationListener());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

        final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap((HttpServletResponse) response);
        final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);

        FilterDirector director = handlerFactory.newHandler().handleRequest(mutableHttpRequest, mutableHttpResponse);
        
        director.applyTo(mutableHttpRequest);
        
        switch (director.getFilterAction()) {
            case PASS:
                chain.doFilter(request, response);
                break;

            case PROCESS_RESPONSE:
                chain.doFilter(request, response);
                director = handlerFactory.newHandler().handleResponse(mutableHttpRequest, mutableHttpResponse);

            case RETURN:
                director.applyTo(mutableHttpResponse);
                break;
        }
    }

    private Datastore getDatastore(DatastoreService datastoreService) {
        Datastore papiDistributed;

        papiDistributed = datastoreService.getDatastore(HashRingDatastoreManager.DATASTORE_MANAGER_NAME + "/default");

        if (papiDistributed != null) {
            return papiDistributed;
        }

        LOG.warn("Default PAPI Distributed Datastore not found! Clustering for rate-limiting will be disabled.");

        return datastoreService.defaultDatastore();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final ContextAdapter ctx = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext());

        handlerFactory = new RateLimitingHandlerFactory(getDatastore(ctx.datastoreService()));

        ctx.configurationService().subscribeTo("rate-limiting.cfg.xml", handlerFactory, RateLimitingConfiguration.class);
    }
}
