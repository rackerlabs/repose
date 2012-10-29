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
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.headers.request.RequestHeaderService;
import com.rackspace.papi.service.headers.response.ResponseHeaderService;
import com.rackspace.papi.service.reporting.ReportingService;
import com.rackspace.papi.service.routing.RoutingService;
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

public class PowerFilterRouterImpl implements PowerFilterRouter {

   private static final Logger LOG = LoggerFactory.getLogger(PowerFilterRouterImpl.class);
   private final Map<String, Destination> destinations;
   private final Node localhost;
   private final RoutingService routingService;
   private final ReportingService reportingService;
   private final RequestHeaderService requestHeaderService;
   private final ResponseHeaderService responseHeaderService;
   private final ServletContext context;
   private final ReposeCluster domain;
   private final String defaultDst;

   public PowerFilterRouterImpl(ReposeCluster domain, Node localhost, ServletContext context, String defaultDst) throws PowerFilterChainException {
      if (localhost == null || domain == null) {
         throw new PowerFilterChainException("Domain and localhost cannot be null");
      }

      this.domain = domain;
      this.localhost = localhost;
      this.routingService = getRoutingService(context);
      this.reportingService = getReportingService(context);
      this.requestHeaderService = getRequestHeaderService(context);
      this.responseHeaderService = getResponseHeaderService(context);
      this.context = context;
      this.defaultDst = defaultDst;
      destinations = new HashMap<String, Destination>();

      if (domain.getDestinations() != null) {
         addDestinations(domain.getDestinations().getEndpoint());
         addDestinations(domain.getDestinations().getTarget());
      }

   }

   private ReportingService getReportingService(ServletContext servletContext) {
      return ServletContextHelper.getInstance().getPowerApiContext(servletContext).reportingService();
   }

   private RequestHeaderService getRequestHeaderService(ServletContext servletContext) {
      return ServletContextHelper.getInstance().getPowerApiContext(servletContext).requestHeaderService();
   }

   private ResponseHeaderService getResponseHeaderService(ServletContext servletContext) {
      return ServletContextHelper.getInstance().getPowerApiContext(servletContext).responseHeaderService();
   }

   private RoutingService getRoutingService(ServletContext servletContext) {
      return ServletContextHelper.getInstance().getPowerApiContext(servletContext).routingService();
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
      RouteDestination destination = servletRequest.getDestination();
      String rootPath = "";

      if (destination != null) {
         Destination dest = destinations.get(destination.getDestinationId());
         if (dest == null) {
            LOG.warn("Invalid routing destination specified: " + destination.getDestinationId() + " for domain: " + domain.getId());
            ((HttpServletResponse) servletResponse).setStatus(HttpStatusCode.NOT_FOUND.intValue());
         } else {
            location = new DestinationLocationBuilder(
                    routingService,
                    localhost,
                    dest,
                    destination.getUri(),
                    servletRequest).build();

            rootPath = dest.getRootPath();
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

               try {
                  reportingService.incrementRequestCount(destination.getDestinationId());
                  final long startTime = System.currentTimeMillis();
                  dispatcher.forward(servletRequest, servletResponse);

                  final long stopTime = System.currentTimeMillis();
                  reportingService.accumulateResponseTime(destination.getDestinationId(), (stopTime - startTime));
                  reportingService.incrementResponseCount(destination.getDestinationId());
                  reportingService.incrementDestinationStatusCodeCount(destination.getDestinationId(), servletResponse.getStatus());


                  responseHeaderService.fixLocationHeader(originalRequest, servletResponse, destination, location.getUri().toString(), rootPath);
               } catch (ClientHandlerException e) {
                  if ("ReadLimitReachedException".equals(e.getCause().getClass().getSimpleName())) {
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
