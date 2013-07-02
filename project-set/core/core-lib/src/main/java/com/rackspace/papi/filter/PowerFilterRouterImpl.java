package com.rackspace.papi.filter;

import com.rackspace.papi.RequestTimeout;
import com.rackspace.papi.ResponseCode;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.stream.ReadLimitReachedException;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;
import com.rackspace.papi.filter.logic.DispatchPathBuilder;
import com.rackspace.papi.filter.routing.DestinationLocation;
import com.rackspace.papi.filter.routing.DestinationLocationBuilder;
import com.rackspace.papi.model.*;
import com.rackspace.papi.service.headers.request.RequestHeaderService;
import com.rackspace.papi.service.headers.response.ResponseHeaderService;
import com.rackspace.papi.service.reporting.ReportingService;
import com.rackspace.papi.service.reporting.metrics.MeterByCategory;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.service.reporting.metrics.impl.MeterByCategorySum;
import com.sun.jersey.api.client.ClientHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class routes a request to the appropriate endpoint specified in system-model.cfg.xml and receives
 * a response.
 * <p>
 * The final URI is constructed from the following information:
 *   - TODO
 * <p>
 * This class also instruments the response codes coming from the endpoint.
 */
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
    private Map<String, MeterByCategory> mapResponseCodes = new HashMap<String, MeterByCategory>();
    private Map<String, MeterByCategory> mapRequestTimeouts = new HashMap<String, MeterByCategory>();
    private MeterByCategory mbcAllResponse;
    private MeterByCategory mbcAllTimeouts;

    private MetricsService metricsService;

    @Autowired
    public PowerFilterRouterImpl(
          @Qualifier("metricsService") MetricsService metricsService,
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
        this.metricsService = metricsService;
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

        mbcAllResponse = metricsService.newMeterByCategory( ResponseCode.class,
                                                            "All Endpoints",
                                                            "Response Codes",
                                                            TimeUnit.SECONDS );
        mbcAllTimeouts = metricsService.newMeterByCategory( RequestTimeout.class,
                                                             "TimeoutToOrigin",
                                                             "Request Timeout",
                                                             TimeUnit.SECONDS );
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

        Destination configDestinationElement = null;

        if (routingDestination != null) {
            configDestinationElement = destinations.get(routingDestination.getDestinationId());
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

                        // track response code for endpoint & across all endpoints
                        String endpoint = getEndpoint( configDestinationElement, location );
                        MeterByCategory mbc = verifyGet( endpoint );
                        MeterByCategory mbcTimeout = getTimeoutMeter( endpoint );

                        PowerFilter.markResponseCodeHelper( mbc, servletResponse.getStatus(), LOG, endpoint );
                        PowerFilter.markResponseCodeHelper( mbcAllResponse, servletResponse.getStatus(), LOG, MeterByCategorySum.ALL );
                        markRequestTimeoutHelper( mbcTimeout, servletResponse.getStatus(), endpoint );
                        markRequestTimeoutHelper( mbcAllTimeouts, servletResponse.getStatus(), "All Endpoints" );

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

    private String getEndpoint( Destination dest, DestinationLocation location ) {

        StringBuilder sb = new StringBuilder();

        sb.append( location.getUri().getHost() + ":" + location.getUri().getPort() );

        if (dest instanceof DestinationEndpoint ) {

            sb.append( ((DestinationEndpoint)dest).getRootPath() );
        }
        else if (dest instanceof DestinationCluster ) {

            sb.append( ((DestinationCluster)dest).getRootPath() );
        }
        else {
            throw new IllegalArgumentException( "Unknown destination type: " + dest.getClass().getName() );
        }

        return sb.toString();
    }

    private MeterByCategory verifyGet( String endpoint ) {
        if( !mapResponseCodes.containsKey( endpoint ) ) {
            synchronized ( mapResponseCodes ) {


                if( !mapResponseCodes.containsKey( endpoint ) ) {

                    mapResponseCodes.put( endpoint, metricsService.newMeterByCategory( ResponseCode.class,
                                                                                       endpoint,
                                                                                       "Response Codes",
                                                                                       TimeUnit.SECONDS ) );
                }
            }
        }

        return mapResponseCodes.get( endpoint );
    }

    private MeterByCategory getTimeoutMeter( String endpoint ) {
        if( !mapRequestTimeouts.containsKey( endpoint ) ) {
            synchronized ( mapRequestTimeouts ) {
                if( !mapRequestTimeouts.containsKey( endpoint ) ) {
                    mapRequestTimeouts.put( endpoint, metricsService.newMeterByCategory( RequestTimeout.class,
                            "TimeoutToOrigin",
                            "Request Timeout",
                            TimeUnit.SECONDS ) );
                }
            }
        }

        return mapRequestTimeouts.get( endpoint );
    }

    public void markRequestTimeoutHelper( MeterByCategory mbc, int responseCode, String endpoint ) {
        assert mbc != null;
        assert endpoint != null;

        if ( responseCode == 408 ) {
            mbc.mark( endpoint );
        }
    }
}
