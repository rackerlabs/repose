package com.rackspace.papi.filter;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.filter.ApplicationContextAwareFilter;
import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.deploy.ApplicationDeploymentEvent;
import com.rackspace.papi.service.event.PowerFilterEvent;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PowerFilter extends ApplicationContextAwareFilter {

   private static final Logger LOG = LoggerFactory.getLogger(PowerFilter.class);
   private final EventListener<ApplicationDeploymentEvent, String> applicationDeploymentListener;
   private final UpdateListener<SystemModel> systemModelConfigurationListener;
   private ServicePorts ports;
   private boolean firstInitialization;
   private PowerFilterChainBuilder powerFilterChainBuilder;
   private ContextAdapter papiContext;
   private SystemModel currentSystemModel;
   private FilterConfig filterConfig;

   public PowerFilter() {
      firstInitialization = true;

      // Default to an empty filter chain so that artifact deployment doesn't gum up the works with a null pointer
      powerFilterChainBuilder = new PowerFilterChainBuilder(null, null, Collections.EMPTY_LIST);
      systemModelConfigurationListener = new SystemModelConfigListener();
      applicationDeploymentListener = new ApplicationDeploymentEventListener();
   }

   private class ApplicationDeploymentEventListener implements EventListener<ApplicationDeploymentEvent, String> {

      @Override
      public void onEvent(Event<ApplicationDeploymentEvent, String> e) {
         LOG.info("Application collection has been modified. Application that changed: " + e.payload());

         if (currentSystemModel != null) {
            SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);
            final List<FilterContext> newFilterChain = new FilterContextInitializer(
                    filterConfig, 
                    ServletContextHelper.getInstance().getApplicationContext(filterConfig.getServletContext())
                    ).buildFilterContexts(papiContext.classLoader(), currentSystemModel, ports);

            updateFilterChainBuilder(
                    interrogator.getLocalServiceDomain(currentSystemModel), 
                    interrogator.getLocalHost(currentSystemModel), 
                    newFilterChain);
         }
      }
   }

   private class SystemModelConfigListener implements UpdateListener<SystemModel> {

      private final Object internalLock = new Object();

      // TODO:Review - There's got to be a better way of initializing PowerFilter. Maybe the app management service could be queryable.
      @Override
      public void configurationUpdated(SystemModel configurationObject) {
         currentSystemModel = configurationObject;

         // This event must be fired only after we have finished configuring the system.
         // This prevents a race condition illustrated below where the application
         // deployment event is caught but does nothing due to a null configuration
         synchronized (internalLock) {
            if (firstInitialization) {
               firstInitialization = false;

               papiContext.eventService().newEvent(PowerFilterEvent.POWER_FILTER_CONFIGURED, System.currentTimeMillis());
            } else {
               SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);
               final List<FilterContext> newFilterChain = new FilterContextInitializer(
                       filterConfig, 
                       ServletContextHelper.getInstance().getApplicationContext(filterConfig.getServletContext())
                       ).buildFilterContexts(papiContext.classLoader(), currentSystemModel, ports);
               updateFilterChainBuilder(
                       interrogator.getLocalServiceDomain(currentSystemModel), 
                       interrogator.getLocalHost(currentSystemModel), 
                       newFilterChain);
            }
         }
      }
   }

   // This is written like this in case requests are already processing against the
   // existing filterChain.  If that is the case we create a new one for the deployment
   // update but the old list stays in memory as the garbage collector won't clean
   // it up until all RequestFilterChainState objects are no longer referencing it.
   private synchronized void updateFilterChainBuilder(ReposeCluster domain, Node localhost, List<FilterContext> newFilterChain) {
      if (powerFilterChainBuilder != null) {
         papiContext.filterChainGarbageCollectorService().reclaimDestroyable(powerFilterChainBuilder, powerFilterChainBuilder.getResourceConsumerMonitor());
      }

      powerFilterChainBuilder = new PowerFilterChainBuilder(domain, localhost, newFilterChain);
   }

   protected SystemModel getCurrentSystemModel() {
      return currentSystemModel;
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      super.init(filterConfig);
      this.filterConfig = filterConfig;

      ports = ServletContextHelper.getInstance().getServerPorts(filterConfig.getServletContext());
      papiContext = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext());

      papiContext.eventService().listen(applicationDeploymentListener, ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED);
      papiContext.configurationService().subscribeTo("system-model.cfg.xml", systemModelConfigurationListener, SystemModel.class);
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
//      mutableHttpResponse.setHeader(CommonHttpHeader.CONTENT_TYPE.toString(), mutableHttpRequest.getHeader(CommonHttpHeader.ACCEPT.toString()));

      try {
         requestFilterChainState.startFilterChain(mutableHttpRequest, mutableHttpResponse);
      } catch (RuntimeException ex) {
         mutableHttpResponse.setStatus(HttpStatusCode.BAD_GATEWAY.intValue());

         LOG.error("Exception encountered while processing filter chain. Reason: " + ex.getMessage(), ex);
      } catch (Exception ex) {
         mutableHttpResponse.setStatus(HttpStatusCode.BAD_GATEWAY.intValue());

         LOG.error("Exception encountered while processing filter chain. Reason: " + ex.getMessage(), ex);
      } finally {
         // In the case where we pass/route the request, there is a chance that
         // the response will be committed by an underlying service, outside of repose
         if (!mutableHttpResponse.isCommitted()) {
            papiContext.responseMessageService().handle(mutableHttpRequest, mutableHttpResponse);
         }

         mutableHttpResponse.commitBufferToServletOutputStream();
      }
   }
}