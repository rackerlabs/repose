package com.rackspace.papi.filter;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.stream.ReadLimitReachedException;
import com.rackspace.papi.commons.util.servlet.filter.ApplicationContextAwareFilter;
import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.deploy.ApplicationDeploymentEvent;
import com.rackspace.papi.service.event.PowerFilterEvent;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventListener;
import java.io.IOException;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rackspace.papi.service.headers.response.ResponseHeaderService;
import com.rackspace.papi.service.reporting.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerFilter extends ApplicationContextAwareFilter {

   private static final Logger LOG = LoggerFactory.getLogger(PowerFilter.class);
   private final EventListener<ApplicationDeploymentEvent, List<String>> applicationDeploymentListener;
   private final UpdateListener<SystemModel> systemModelConfigurationListener;
   private ServicePorts ports;
   private boolean firstInitialization;
   private PowerFilterChainBuilder powerFilterChainBuilder;
   private ContextAdapter papiContext;
   private SystemModel currentSystemModel;
   private ReposeCluster serviceDomain;
   private Node localHost;
   private FilterConfig filterConfig;
   private ReportingService reportingService;
   private ResponseHeaderService responseHeaderService;
   private Destination defaultDst;

   public PowerFilter() {
      firstInitialization = true;

      // Default to an empty filter chain so that artifact deployment doesn't gum up the works with a null pointer
      powerFilterChainBuilder = null;
      systemModelConfigurationListener = new SystemModelConfigListener();
      applicationDeploymentListener = new ApplicationDeploymentEventListener();
   }

   private class ApplicationDeploymentEventListener implements EventListener<ApplicationDeploymentEvent, List<String>> {

      @Override
      public void onEvent(Event<ApplicationDeploymentEvent, List<String>> e) {
         LOG.info("Application collection has been modified. Application that changed: " + e.payload());

         if (currentSystemModel != null) {
            SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);
            localHost = interrogator.getLocalHost(currentSystemModel);
            serviceDomain = interrogator.getLocalServiceDomain(currentSystemModel);
            defaultDst = interrogator.getDefaultDestination(currentSystemModel);
            final List<FilterContext> newFilterChain = new FilterContextInitializer(
                    filterConfig,
                    ServletContextHelper.getInstance().getApplicationContext(filterConfig.getServletContext())).buildFilterContexts(papiContext.classLoader(), serviceDomain, localHost);

            updateFilterChainBuilder(newFilterChain);
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
               localHost = interrogator.getLocalHost(currentSystemModel);
               serviceDomain = interrogator.getLocalServiceDomain(currentSystemModel);
               defaultDst = interrogator.getDefaultDestination(currentSystemModel);
               final List<FilterContext> newFilterChain = new FilterContextInitializer(
                       filterConfig,
                       ServletContextHelper.getInstance().getApplicationContext(filterConfig.getServletContext())).buildFilterContexts(papiContext.classLoader(), serviceDomain, localHost);
               updateFilterChainBuilder(newFilterChain);
            }
         }
      }
   }

   // This is written like this in case requests are already processing against the
   // existing filterChain.  If that is the case we create a new one for the deployment
   // update but the old list stays in memory as the garbage collector won't clean
   // it up until all RequestFilterChainState objects are no longer referencing it.
   private synchronized void updateFilterChainBuilder(List<FilterContext> newFilterChain) {
      if (powerFilterChainBuilder != null) {
         papiContext.filterChainGarbageCollectorService().reclaimDestroyable(powerFilterChainBuilder, powerFilterChainBuilder.getResourceConsumerMonitor());
      }
      try {
         powerFilterChainBuilder = new PowerFilterChainBuilder(serviceDomain, localHost, newFilterChain, filterConfig.getServletContext(), defaultDst.getId());
      } catch (PowerFilterChainException ex) {
         LOG.error("Unable to initialize filter chain builder", ex);
      }
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

      filterConfig.getServletContext().setAttribute("powerFilter", this);

      reportingService = papiContext.reportingService();
      responseHeaderService = papiContext.responseHeaderService();
   }

   @Override
   public void destroy() {
   }

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

      final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
      final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, (HttpServletResponse) response);

      if (powerFilterChainBuilder == null) {
         responseHeaderService.setVia(mutableHttpRequest, mutableHttpResponse);
         throw new ServletException("Filter chain has not been initialized");
      }

      try {
         final PowerFilterChain requestFilterChainState = powerFilterChainBuilder.newPowerFilterChain(chain);
         requestFilterChainState.startFilterChain(mutableHttpRequest, mutableHttpResponse);
      } catch (PowerFilterChainException ex) {
         LOG.warn("Error creating filter chain", ex);
         mutableHttpResponse.sendError(HttpStatusCode.SERVICE_UNAVAIL.intValue(), "Error creating filter chain");
         mutableHttpResponse.setLastException(ex);
      } catch (Exception ex) {
         LOG.error("Exception encountered while processing filter chain. Reason: " + ex.getMessage(), ex);
         mutableHttpResponse.sendError(HttpStatusCode.BAD_GATEWAY.intValue(), "Error processing request");
         mutableHttpResponse.setLastException(ex);
      } finally {
         // In the case where we pass/route the request, there is a chance that
         // the response will be committed by an underlying service, outside of repose
         if (!mutableHttpResponse.isCommitted()) {
            papiContext.responseMessageService().handle(mutableHttpRequest, mutableHttpResponse);
            responseHeaderService.setVia(mutableHttpRequest, mutableHttpResponse);
         }

         try {
            mutableHttpResponse.commitBufferToServletOutputStream();
         } catch (IOException ex) {
            LOG.error("Error committing output stream", ex);
         }
         reportingService.incrementReposeStatusCodeCount(((HttpServletResponse) response).getStatus());
      }
   }
}
