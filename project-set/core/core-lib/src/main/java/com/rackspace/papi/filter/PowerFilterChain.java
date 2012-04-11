package com.rackspace.papi.filter;

import com.rackspace.papi.filter.resource.ResourceMonitor;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;

import com.rackspace.papi.filter.logic.DispatchPathBuilder;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationDomain;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.ServiceDomain;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.routing.RoutingService;
import com.sun.jersey.api.client.ClientHandlerException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletResponse;

/**
 * @author fran
 *
 * Cases to handle/test: 1. There are no filters in our chain but some in container's 2. There are filters in our chain
 * and in container's 3. There are no filters in our chain or container's 4. There are filters in our chain but none in
 * container's 5. If one of our filters breaks out of the chain (i.e. it doesn't call doFilter), then we shouldn't call
 * doFilter on the container's filter chain. 6. If one of the container's filters breaks out of the chain then our chain
 * should unwind correctly
 *
 */
public class PowerFilterChain implements FilterChain {

   private static final Logger LOG = LoggerFactory.getLogger(PowerFilterChain.class);
   private final ResourceMonitor resourceMonitor;
   private final List<FilterContext> filterChainCopy;
   private final FilterChain containerFilterChain;
   private final ClassLoader containerClassLoader;
   private final ServletContext context;
   private final ServiceDomain domain;
   private final Map<String, Destination> destinations;
   private final RoutingService routingService;
   private int position;

   public PowerFilterChain(ServiceDomain domain, List<FilterContext> filterChainCopy, FilterChain containerFilterChain, ServletContext context, ResourceMonitor resourceMontior) {
      this.filterChainCopy = new LinkedList<FilterContext>(filterChainCopy);
      this.containerFilterChain = containerFilterChain;
      this.context = context;
      this.containerClassLoader = Thread.currentThread().getContextClassLoader();
      this.resourceMonitor = resourceMontior;
      this.domain = domain;
      this.routingService = ServletContextHelper.getPowerApiContext(context).routingService();
      destinations = new HashMap<String, Destination>();

      if (domain.getDestinations() != null) {
         addDestinations(domain.getDestinations().getEndpoint());
         addDestinations(domain.getDestinations().getTargetDomain());
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

   @Override
   public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
      final Thread currentThread = Thread.currentThread();
      final ClassLoader previousClassLoader = currentThread.getContextClassLoader();

      if (position < filterChainCopy.size()) {
         final FilterContext nextFilterContext = filterChainCopy.get(position++);
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
   
   private String buildUrl(String protocol, String hostname, int port, String baseUri, String uri) {
      StringBuilder builder = new StringBuilder();
      
      builder.append(protocol).append("://");
      builder.append(hostname);
      builder.append(":").append(port);
      builder.append(baseUri);
      
      return builder.toString();
   }

   private void route(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
      //final String routeDestination = ((HttpServletRequest) servletRequest).getHeader(PowerApiHeader.NEXT_ROUTE.toString());
      
      String routeDestination = "";
      MutableHttpServletRequest mutableRequest = (MutableHttpServletRequest) servletRequest;
      RouteDestination destination = mutableRequest.getDestination();

      // TODO Model: This is very rough route determination code that doesn't support
      // host and port defaults or internal dispatching.  Needs to be refactored and cleaned up!
      if (destination != null) {
         Destination d = destinations.get(destination.getDestinationId());
         
         if (d instanceof DestinationEndpoint) {
            DestinationEndpoint endpoint = (DestinationEndpoint)d;
            routeDestination = buildUrl(endpoint.getProtocol(), endpoint.getHostname(), endpoint.getPort(), endpoint.getRootPath(), destination.getUri());
         } else if (d instanceof DestinationDomain) {
            DestinationDomain destDomain = (DestinationDomain)d;
            DomainNode node = routingService.getRoutableNode(destDomain.getId());
            routeDestination = buildUrl(destDomain.getProtocol(), node.getHostname(), node.getHttpPort(), destDomain.getRootPath(), destination.getUri());
         }
      }
      
      //DomainNode routableNode = routingService.getRoutableNode(dest.getDestinationId());



      if (!StringUtilities.isBlank(routeDestination)) {
         // According to the Java 6 javadocs the routeDestination passed into getContext:
         // "The given path [routeDestination] must begin with /, is interpreted relative to the server's document root
         // and is matched against the context roots of other web applications hosted on this container."
         final ServletContext targetContext = context.getContext(routeDestination);

         if (targetContext != null) {
            final RequestDispatcher dispatcher = targetContext.getRequestDispatcher(
                    new DispatchPathBuilder(servletRequest, routeDestination).build());

            if (dispatcher != null) {
               LOG.debug("Attempting to route to " + routeDestination);
               LOG.debug("Request URI: " + ((HttpServletRequest) servletRequest).getRequestURI());
               LOG.debug("Context path = " + targetContext.getContextPath());

               try {
                  dispatcher.forward(servletRequest, servletResponse);
               } catch (ClientHandlerException e) {
                  LOG.error("Connection Refused to " + routeDestination + " " + e.getMessage(), e);
                  ((HttpServletResponse) servletResponse).setStatus(HttpStatusCode.SERVICE_UNAVAIL.intValue());
               }
            }
         }
      }
   }
}
