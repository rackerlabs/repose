package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.header.UserAgentExtractor;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;
import com.rackspace.papi.commons.util.net.NetUtilities;
import com.rackspace.papi.filter.logic.DispatchPathBuilder;
import com.rackspace.papi.filter.resource.ResourceMonitor;
import com.rackspace.papi.filter.routing.DestinationLocation;
import com.rackspace.papi.filter.routing.DestinationLocationBuilder;
import com.rackspace.papi.http.ProxyHeadersGenerator;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import com.rackspace.papi.service.routing.RoutingService;
import com.sun.jersey.api.client.ClientHandlerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fran
 *
 * Cases to handle/test: 1. There are no filters in our chain but some in container's 2. There are filters in our chain and in container's 3. There are no filters in our
 * chain or container's 4. There are filters in our chain but none in container's 5. If one of our filters breaks out of the chain (i.e. it doesn't call doFilter), then
 * we shouldn't call doFilter on the container's filter chain. 6. If one of the container's filters breaks out of the chain then our chain should unwind correctly
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
   private List<FilterContext> currentFilters;
   private ProxyHeadersGenerator proxyHeadersGenerator;
   private boolean trace;
   private int position;
   private long accumulatedTime;
   private long requestStart;

   public PowerFilterChain(ReposeCluster domain, Node localhost, List<FilterContext> filterChainCopy, FilterChain containerFilterChain, ServletContext context, ResourceMonitor resourceMontior) throws PowerFilterChainException {
      if (localhost == null || domain == null) {
         throw new PowerFilterChainException("Domain and localhost cannot be null");
      }

      this.filterChainCopy = new LinkedList<FilterContext>(filterChainCopy);
      this.containerFilterChain = containerFilterChain;
      this.context = context;
      this.containerClassLoader = Thread.currentThread().getContextClassLoader();
      this.resourceMonitor = resourceMontior;
      this.domain = domain;
      this.localhost = localhost;
      this.routingService = ServletContextHelper.getInstance().getPowerApiContext(context).routingService();
      this.proxyHeadersGenerator = new ProxyHeadersGenerator(ServletContextHelper.getInstance().getPowerApiContext(context).containerConfigurationService());
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

   /**
    * Find the filters that are applicable to this request based on the uri-regex specified for each filter and the current request uri.
    *
    * If a necessary filter is not available, then return an empty filter list.
    *
    * @param uri
    * @return
    */
   private List<FilterContext> getFilterChainForRequest(String uri) {
      List<FilterContext> filters = new LinkedList<FilterContext>();
      for (FilterContext filter : filterChainCopy) {
         if ((filter.getUriPattern() == null || filter.getUriPattern().matcher(uri).matches())) {
            filters.add(filter);
         }
      }

      return filters;
   }

   private boolean traceRequest(HttpServletRequest request) {
      return request.getHeader("X-Trace-Request") != null;
   }

   private void initChainForRequest(HttpServletRequest request) {
      requestStart = new Date().getTime();
      trace = traceRequest(request);
      currentFilters = getFilterChainForRequest(request.getRequestURI());
   }

   private boolean isCurrentFilterChainAvailable() {
      boolean result = true;

      for (FilterContext filter : currentFilters) {
         if (!filter.isFilterAvailable()) {
            LOG.warn("Filter is not available for processing requests: " + filter.getName());
         }
         result &= filter.isFilterAvailable();
      }

      return result;
   }

   private long traceEnter(MutableHttpServletResponse response, String filterName) {
      if (!trace) {
         return 0;
      }

      long time = new Date().getTime() - requestStart;
      //mutableHttpResponse.addHeader("X-" + filterName + "-Enter", String.valueOf(time));
      return time;
   }

   private void traceExit(MutableHttpServletResponse response, String filterName, long myStart) {
      if (!trace) {
         return;
      }
      long totalRequestTime = new Date().getTime() - requestStart;
      long myTime = totalRequestTime - myStart - accumulatedTime;
      accumulatedTime += myTime;
      //mutableHttpResponse.addHeader("X-" + filterName + "-Exit", String.valueOf(totalRequestTime));
      response.addHeader("X-" + filterName + "-Time", String.valueOf(myTime) + "ms");
   }

   private boolean isResponseOk(HttpServletResponse response) {
      return response.getStatus() < HttpStatusCode.INTERNAL_SERVER_ERROR.intValue();
   }

   @Override
   public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
      final HttpServletRequest request = (HttpServletRequest) servletRequest;
      final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) servletRequest);
      final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, (HttpServletResponse) servletResponse);
      final Thread currentThread = Thread.currentThread();
      final ClassLoader previousClassLoader = currentThread.getContextClassLoader();

      boolean filterChainAvailable = true;
      if (position == 0) {
         initChainForRequest(request);
         filterChainAvailable = isCurrentFilterChainAvailable();
         request.setAttribute("filterChainAvailableForRequest", filterChainAvailable);
      }


      if (filterChainAvailable && position < currentFilters.size()) {
         final FilterContext nextFilterContext = currentFilters.get(position++);
         final com.rackspace.papi.model.Filter filterConfig = nextFilterContext.getFilterConfig();
         final ClassLoader nextClassLoader = nextFilterContext.getFilterClassLoader();

         currentThread.setContextClassLoader(nextClassLoader);
         mutableHttpResponse.pushOutputStream();
         try {
            long start = traceEnter(mutableHttpResponse, filterConfig.getName());
            nextFilterContext.getFilter().doFilter(mutableHttpRequest, mutableHttpResponse, this);
            traceExit(mutableHttpResponse, filterConfig.getName(), start);
         } catch (Exception ex) {
            String filterName = nextFilterContext.getFilter().getClass().getSimpleName();
            LOG.error("Failure in filter: " + filterName + "  -  Reason: " + ex.getMessage(), ex);
         } finally {
            mutableHttpResponse.popOutputStream();
            currentThread.setContextClassLoader(previousClassLoader);
         }
      } else {
         currentThread.setContextClassLoader(containerClassLoader);

         try {
            if (isResponseOk(mutableHttpResponse)) {
               containerFilterChain.doFilter(mutableHttpRequest, mutableHttpResponse);
            }

            if (isResponseOk(mutableHttpResponse)) {
               proxyHeadersGenerator.setProxyHeaders(mutableHttpRequest);
               route(mutableHttpRequest, mutableHttpResponse);
               if (servletResponse != mutableHttpResponse) {
                  mutableHttpResponse.commitBufferToServletOutputStream();
               }
            }
         } catch (Exception ex) {
            LOG.error("Failure in filter within container filter chain. Reason: " + ex.getMessage(), ex);
            mutableHttpResponse.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());
            mutableHttpResponse.setLastException(ex);
         } finally {
            currentThread.setContextClassLoader(previousClassLoader);
         }
      }
   }

   private void route(MutableHttpServletRequest servletRequest, MutableHttpServletResponse servletResponse) throws IOException, ServletException, URISyntaxException {
      final String name = "route";
      long start = traceEnter(servletResponse, name);
      DestinationLocation location = null;
      RouteDestination destination = servletRequest.getDestination();

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
                    servletRequest).build();
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

            servletRequest.setRequestUrl(new StringBuffer(location.getUrl().toExternalForm()));
            servletRequest.setRequestUri(location.getUri().getPath());
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

      traceExit(servletResponse, name, start);
   }
}
