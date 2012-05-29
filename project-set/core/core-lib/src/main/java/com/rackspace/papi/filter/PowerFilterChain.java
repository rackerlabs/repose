package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;
import com.rackspace.papi.filter.logic.DispatchPathBuilder;
import com.rackspace.papi.filter.resource.ResourceMonitor;
import com.rackspace.papi.filter.routing.DestinationLocation;
import com.rackspace.papi.filter.routing.DestinationLocationBuilder;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.routing.RoutingService;
import com.sun.jersey.api.client.ClientHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author fran
 *
 * Cases to handle/test: 1. There are no filters in our chain but some in
 * container's 2. There are filters in our chain and in container's 3. There are
 * no filters in our chain or container's 4. There are filters in our chain but
 * none in container's 5. If one of our filters breaks out of the chain (i.e. it
 * doesn't call doFilter), then we shouldn't call doFilter on the container's
 * filter chain. 6. If one of the container's filters breaks out of the chain
 * then our chain should unwind correctly
 *
 */
public class PowerFilterChain implements FilterChain {

    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterChain.class);
    private final ResourceMonitor resourceMonitor;
    private final List<FilterContext> filterChainCopy;
    private final FilterChain containerFilterChain;
    private final ClassLoader containerClassLoader;
    private final ServletContext context;
    private final ReposeCluster domain;
    private final Node localhost;
    private final Map<String, Destination> destinations;
    private final RoutingService routingService;
    private int position;

    public PowerFilterChain(ReposeCluster domain, Node localhost, List<FilterContext> filterChainCopy, FilterChain containerFilterChain, ServletContext context, ResourceMonitor resourceMontior) {
        this.filterChainCopy = new LinkedList<FilterContext>(filterChainCopy);
        this.containerFilterChain = containerFilterChain;
        this.context = context;
        this.containerClassLoader = Thread.currentThread().getContextClassLoader();
        this.resourceMonitor = resourceMontior;
        this.domain = domain;
        this.localhost = localhost;
        this.routingService = ServletContextHelper.getInstance().getPowerApiContext(context).routingService();
        destinations = new HashMap<String, Destination>();

        if (domain != null && domain.getDestinations() != null) {
            addDestinations(domain.getDestinations().getEndpoint());
            addDestinations(domain.getDestinations().getTarget());
        }
    }

    private void addDestinations(List<? extends Destination> destList) {
        for (Destination dest : destList) {
            destinations.put(dest.getId(), dest);
        }
    }

    public void startFilterChain(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        resourceMonitor.use();

        try {
            doFilter(servletRequest, servletResponse);
        } finally {
            resourceMonitor.released();
        }
    }
    
    private List<FilterContext> getFilterChainForRequest(String uri) {
        List<FilterContext> filters = new LinkedList<FilterContext>();
        for (FilterContext filter: filterChainCopy) {
            if (filter.getUriPattern() == null || filter.getUriPattern().matcher(uri).matches()) {
                filters.add(filter);
            }
        }
        
        return filters;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest)servletRequest;
        final Thread currentThread = Thread.currentThread();
        final ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        final List<FilterContext> currentFilters = getFilterChainForRequest(request.getRequestURI());

        if (position < currentFilters.size()) {
            final FilterContext nextFilterContext = currentFilters.get(position++);
            final ClassLoader nextClassLoader = nextFilterContext.getFilterClassLoader();

            currentThread.setContextClassLoader(nextClassLoader);

            try {
                nextFilterContext.getFilter().doFilter(servletRequest, servletResponse, this);
            } catch (Exception ex) {
                LOG.error("Failure in filter: " + nextFilterContext.getFilter().getClass().getSimpleName()
                        + "  -  Reason: " + ex.getMessage(), ex);
            } finally {
                currentThread.setContextClassLoader(previousClassLoader);
            }
        } else {
            currentThread.setContextClassLoader(containerClassLoader);

            try {
                containerFilterChain.doFilter(servletRequest, servletResponse);
                route(servletRequest, servletResponse);
            } catch (Exception ex) {
                LOG.error("Failure in filter within container filter chain. Reason: " + ex.getMessage(), ex);
            } finally {
                currentThread.setContextClassLoader(previousClassLoader);
            }
        }
    }

    private void route(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException, URISyntaxException {
        DestinationLocation location = null;
        MutableHttpServletRequest mutableRequest = (MutableHttpServletRequest) servletRequest;
        RouteDestination destination = mutableRequest.getDestination();

        if (destination != null) {
            Destination dest = destinations.get(destination.getDestinationId());
            if (dest == null) {
                LOG.warn("Invalid routing destination specified: " + destination.getDestinationId() + " for domain: " + domain.getId());
                ((HttpServletResponse) servletResponse).setStatus(HttpStatusCode.SERVICE_UNAVAIL.intValue());
            } else {
                location = new DestinationLocationBuilder(
                        routingService,
                        localhost,
                        dest,
                        destination.getUri(),
                        mutableRequest).build();
            }
        }

        if (location != null) {
            // According to the Java 6 javadocs the routeDestination passed into getContext:
            // "The given path [routeDestination] must begin with /, is interpreted relative to the server's document root
            // and is matched against the context roots of other web applications hosted on this container."
            final ServletContext targetContext = context.getContext(location.getUri().toString());

            if (targetContext != null) {
                String uri = new DispatchPathBuilder(location.getUri().getPath(), targetContext.getContextPath()).build();
                final RequestDispatcher dispatcher = targetContext.getRequestDispatcher(uri);

                mutableRequest.setRequestUrl(new StringBuffer(location.getUri().toURL().toExternalForm()));
                mutableRequest.setRequestUri(location.getUri().getPath());
                if (dispatcher != null) {
                    LOG.debug("Attempting to route to " + location.getUri());
                    LOG.debug("Request URL: " + ((HttpServletRequest) servletRequest).getRequestURL());
                    LOG.debug("Request URI: " + ((HttpServletRequest) servletRequest).getRequestURI());
                    LOG.debug("Context path = " + targetContext.getContextPath());

                    try {
                        dispatcher.forward(servletRequest, servletResponse);
                    } catch (ClientHandlerException e) {
                        LOG.error("Connection Refused to " + location.getUri() + " " + e.getMessage(), e);
                        ((HttpServletResponse) servletResponse).setStatus(HttpStatusCode.SERVICE_UNAVAIL.intValue());
                    }
                }
            }
        }
    }
}
