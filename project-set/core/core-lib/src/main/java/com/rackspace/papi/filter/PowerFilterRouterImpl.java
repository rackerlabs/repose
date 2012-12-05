package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.stream.ReadLimitReachedException;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;
import com.rackspace.papi.filter.logic.DispatchPathBuilder;
import com.rackspace.papi.filter.routing.DestinationLocation;
import com.rackspace.papi.filter.routing.DestinationLocationBuilder;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.service.headers.request.RequestHeaderService;
import com.rackspace.papi.service.headers.response.ResponseHeaderService;
import com.rackspace.papi.service.reporting.ReportingService;
import com.sun.jersey.api.client.ClientHandlerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("powerFilterRouter")
@Scope("prototype")
public class PowerFilterRouterImpl implements PowerFilterRouter {

    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterRouterImpl.class);
    private final Map<String, Destination> destinations;
    private final ReportingService reportingService;
    private final RequestHeaderService requestHeaderService;
    private final ResponseHeaderService responseHeaderService;
    private ServletContext context;
    private ReposeCluster domain;
    private String defaultDst;
    private final DestinationLocationBuilder locationBuilder;

    @Autowired
    public PowerFilterRouterImpl(
            @Qualifier("reportingService") ReportingService reportingService,
            @Qualifier("requestHeaderService") RequestHeaderService requestHeaderService,
            @Qualifier("responseHeaderService") ResponseHeaderService responseHeaderService,
            @Qualifier("destinationLocationBuilder") DestinationLocationBuilder locationBuilder) {
        LOG.info("Creating Repose Router");
        this.destinations = new HashMap<String, Destination>();
        this.reportingService = reportingService;
        this.responseHeaderService = responseHeaderService;
        this.requestHeaderService = requestHeaderService;
        this.locationBuilder = locationBuilder;
    }

    @Override
    public void initialize(ReposeCluster domain, Node localhost, ServletContext context, String defaultDst) throws PowerFilterChainException {
        if (localhost == null || domain == null) {
            throw new PowerFilterChainException("Domain and localhost cannot be null");
        }

        LOG.info("Initializing Repose Router");
        this.domain = domain;
        this.context = context;
        this.defaultDst = defaultDst;
        this.destinations.clear();
        this.locationBuilder.init(localhost);

        if (domain.getDestinations() != null) {
            addDestinations(domain.getDestinations().getEndpoint());
            addDestinations(domain.getDestinations().getTarget());
        }

    }

    private void addDestinations(List<? extends Destination> destList) {
        for (Destination dest : destList) {
            destinations.put(dest.getId(), dest);
        }
    }

    @Override
    public void route(MutableHttpServletRequest servletRequest, MutableHttpServletResponse servletResponse) throws IOException, ServletException, URISyntaxException {
        DestinationLocation location = null;

        if (!StringUtilities.isBlank(defaultDst)) {
            servletRequest.addDestination(defaultDst, servletRequest.getRequestURI(), -1);

        }
        RouteDestination routingDestination = servletRequest.getDestination();
        String rootPath = "";

        if (routingDestination != null) {
            Destination configDestinationElement = destinations.get(routingDestination.getDestinationId());
            if (configDestinationElement == null) {
                LOG.warn("Invalid routing destination specified: " + routingDestination.getDestinationId() + " for domain: " + domain.getId());
                ((HttpServletResponse) servletResponse).setStatus(HttpStatusCode.NOT_FOUND.intValue());
            } else {
                location = locationBuilder.build(configDestinationElement, routingDestination.getUri(), servletRequest);

                rootPath = configDestinationElement.getRootPath();
            }
        }

        if (location != null) {
            // According to the Java 6 javadocs the routeDestination passed into getContext:
            // "The given path [routeDestination] must begin with /, is interpreted relative to the server's document root
            // and is matched against the context roots of other web applications hosted on this container."
            final ServletContext targetContext = context.getContext(location.getUri().toString());

            if (targetContext != null) {
                // Capture this for Location header processing
                final HttpServletRequest originalRequest = (HttpServletRequest) servletRequest.getRequest();

                String uri = new DispatchPathBuilder(location.getUri().getPath(), targetContext.getContextPath()).build();
                final RequestDispatcher dispatcher = targetContext.getRequestDispatcher(uri);

                servletRequest.setRequestUrl(new StringBuffer(location.getUrl().toExternalForm()));
                servletRequest.setRequestUri(location.getUri().getPath());
                requestHeaderService.setVia(servletRequest);
                requestHeaderService.setXForwardedFor(servletRequest);
                if (dispatcher != null) {
                    LOG.debug("Attempting to route to " + location.getUri());
                    LOG.debug("Request URL: " + ((HttpServletRequest) servletRequest).getRequestURL());
                    LOG.debug("Request URI: " + ((HttpServletRequest) servletRequest).getRequestURI());
                    LOG.debug("Context path = " + targetContext.getContextPath());

                    final long startTime = System.currentTimeMillis();
                    try {
                        reportingService.incrementRequestCount(routingDestination.getDestinationId());
                        dispatcher.forward(servletRequest, servletResponse);
                        final long stopTime = System.currentTimeMillis();
                        reportingService.recordServiceResponse(routingDestination.getDestinationId(), servletResponse.getStatus(), (stopTime - startTime));
                        responseHeaderService.fixLocationHeader(originalRequest, servletResponse, routingDestination, location.getUri().toString(), rootPath);
                    } catch (ClientHandlerException e) {
                        if (e.getCause() instanceof ReadLimitReachedException) {
                            LOG.error("Error reading request content", e);
                            servletResponse.sendError(HttpStatusCode.REQUEST_ENTITY_TOO_LARGE.intValue(), "Error reading request content");
                            servletResponse.setLastException(e);
                        } else {
                            LOG.error("Connection Refused to " + location.getUri() + " " + e.getMessage(), e);
                            ((HttpServletResponse) servletResponse).setStatus(HttpStatusCode.SERVICE_UNAVAIL.intValue());
                        }
                    }

                }
            }
        }
    }
}
