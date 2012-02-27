package com.rackspace.papi.filter;

import com.rackspace.papi.filter.resource.ResourceMonitor;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.PowerApiHeader;

import com.rackspace.papi.filter.logic.DispatchPathBuilder;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   private int position;

   public PowerFilterChain(List<FilterContext> filterChainCopy, FilterChain containerFilterChain, ServletContext context, ResourceMonitor resourceMontior) {
      this.filterChainCopy = new LinkedList<FilterContext>(filterChainCopy);
      this.containerFilterChain = containerFilterChain;
      this.context = context;
      this.containerClassLoader = Thread.currentThread().getContextClassLoader();
      this.resourceMonitor = resourceMontior;
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
            LOG.error("Failure in filter within container filter chain. Please debug.", ex);
         } finally {
            currentThread.setContextClassLoader(previousClassLoader);
         }
      }
   }

   private void route(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
      final String routeDestination = ((HttpServletRequest) servletRequest).getHeader(PowerApiHeader.NEXT_ROUTE.toString());

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
               LOG.debug("Request URI: " + ((HttpServletRequest)servletRequest).getRequestURI());
               LOG.debug("Context path = " + targetContext.getContextPath());
               dispatcher.forward(servletRequest, servletResponse);
            }
         }
      }
   }
}
