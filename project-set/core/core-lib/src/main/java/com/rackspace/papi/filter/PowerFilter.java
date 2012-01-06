package com.rackspace.papi.filter;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.filter.ApplicationContextAwareFilter;
import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.context.jndi.ContextAdapter;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.deploy.ApplicationDeploymentEvent;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.listener.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class PowerFilter extends ApplicationContextAwareFilter {

   private static final Logger LOG = LoggerFactory.getLogger(PowerFilter.class);
   private final EventListener<ApplicationDeploymentEvent, String> applicationDeploymentListener;
   private final UpdateListener<PowerProxy> systemModelConfigurationListener;
   private int port;
   
   private boolean firstInitialization;
   private PowerFilterChainBuilder powerFilterChainBuilder;
   private ContextAdapter papiContext;
   private PowerProxy currentSystemModel;
   private FilterConfig filterConfig;
   
   public PowerFilter() {
      firstInitialization = true;

      // Default to an empty filter chain so that artifact deployment doesn't gum up the works with a null pointer
      powerFilterChainBuilder = new PowerFilterChainBuilder(Collections.EMPTY_LIST);

      systemModelConfigurationListener = new UpdateListener<PowerProxy>() {

         private final Object internalLock = new Object();

         // TODO:Review - There's got to be a better way of initializing PowerFilter. Maybe the app management service could be queryable.
         @Override
         public void configurationUpdated(PowerProxy configurationObject) {
            currentSystemModel = configurationObject;

            // This event must be fired only after we have finished configuring the system.
            // This prevents a race condition illustrated below where the application
            // deployment event is caught but does nothing due to a null configuration
            synchronized (internalLock) {
               if (firstInitialization) {
                  firstInitialization = false;

                  papiContext.eventService().newEvent(PowerFilterEvent.POWER_FILTER_CONFIGURED, System.currentTimeMillis());
               } else {
                  final List<FilterContext> newFilterChain = new FilterContextInitializer(filterConfig).buildFilterContexts(papiContext.classLoader(), currentSystemModel, port);
                  updateFilterChainBuilder(newFilterChain);
               }
            }
         }
      };

      applicationDeploymentListener = new EventListener<ApplicationDeploymentEvent, String>() {

         @Override
         public void onEvent(Event<ApplicationDeploymentEvent, String> e) {
            LOG.info("Application collection has been modified. Application that changed: " + e.payload());

            if (currentSystemModel != null) {
               final List<FilterContext> newFilterChain = new FilterContextInitializer(filterConfig).buildFilterContexts(papiContext.classLoader(), currentSystemModel, port);

               updateFilterChainBuilder(newFilterChain);
            }
         }
      };
   }

   // This is written like this in case requests are already processing against the
   // existing filterChain.  If that is the case we create a new one for the deployment
   // update but the old list stays in memory as the garbage collector won't clean
   // it up until all RequestFilterChainState objects are no longer referencing it.
   private synchronized void updateFilterChainBuilder(List<FilterContext> newFilterChain) {
      if (powerFilterChainBuilder != null) {
         papiContext.filterChainGarbageCollectorService().reclaimDestroyable(powerFilterChainBuilder, powerFilterChainBuilder.getResourceConsumerMonitor());
      }

      powerFilterChainBuilder = new PowerFilterChainBuilder(newFilterChain);
   }

   protected PowerProxy getCurrentSystemModel() {
      return currentSystemModel;
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      super.init(filterConfig);
      this.filterConfig = filterConfig;
      
      port = ServletContextHelper.getServerPort(filterConfig.getServletContext());
      papiContext = ServletContextHelper.getPowerApiContext(filterConfig.getServletContext());
      
      papiContext.eventService().listen(applicationDeploymentListener, ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED);
      papiContext.configurationService().subscribeTo("power-proxy.cfg.xml", systemModelConfigurationListener, PowerProxy.class);
   }

   @Override
   public void destroy() {
   }

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

      final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
      final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap((HttpServletResponse) response);
      final PowerFilterChain requestFilterChainState = powerFilterChainBuilder.newPowerFilterChain(chain, filterConfig.getServletContext());

      // TODO:Review - Should this be set for all filters regardless of what they return?
//      mutableHttpResponse.setHeader(CommonHttpHeader.CONTENT_TYPE.getHeaderKey(), mutableHttpRequest.getHeader(CommonHttpHeader.ACCEPT.getHeaderKey()));

      try {
         requestFilterChainState.startFilterChain(mutableHttpRequest, mutableHttpResponse);
      } catch (Exception ex) {
         mutableHttpResponse.setStatus(HttpStatusCode.BAD_GATEWAY.intValue());

         LOG.error("Exception encountered while processing filter chain", ex);
      } finally {
         papiContext.responseMessageService().handle(mutableHttpRequest, mutableHttpResponse);

         mutableHttpResponse.commitBufferToServletOutputStream();
      }
   }
}