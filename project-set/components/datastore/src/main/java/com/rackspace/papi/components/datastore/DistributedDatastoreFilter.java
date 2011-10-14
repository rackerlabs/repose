package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.context.jndi.ContextAdapter;
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

public class DistributedDatastoreFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DistributedDatastoreFilter.class);
    
    private DatastoreFilterLogicHandlerFactory handler;

    @Override
    public void destroy() {
        
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap((HttpServletResponse) response);
        
        final FilterDirector director = handler.newHandler().handleRequest((HttpServletRequest) request, mutableHttpResponse);

        switch (director.getFilterAction()) {
            case PASS:
            case NOT_SET:
                chain.doFilter(request, response);
                break;

            case RETURN:
            case PROCESS_RESPONSE:
            case USE_MESSAGE_SERVICE:
                director.applyTo(httpResponse);     
                break;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final ContextAdapter contextAdapter = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext());
        
        handler = new DatastoreFilterLogicHandlerFactory(contextAdapter.datastoreService());
        contextAdapter.configurationService().subscribeTo("power-proxy.cfg.xml", handler, PowerProxy.class);
    }
}
