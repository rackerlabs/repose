package com.rackspace.papi.filter;

import com.rackspace.papi.commons.config.manager.LockedConfigurationUpdater;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.filter.ApplicationContextAwareFilter;
import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.context.jndi.ContextAdapter;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.deploy.ApplicationDeploymentEvent;
import com.rackspace.papi.service.event.Event;
import com.rackspace.papi.service.event.listener.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
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
    private boolean firstInitialization;
    private List<FilterContext> filterChain;
    private ContextAdapter papiContext;
    private PowerProxy currentSystemModel;
    private FilterConfig filterConfig;

    public PowerFilter() {
        KeyedStackLock updateLock = new KeyedStackLock();
        Object updateKey = new Object();
        firstInitialization = true;

        filterChain = new LinkedList<FilterContext>();

        systemModelConfigurationListener = new LockedConfigurationUpdater<PowerProxy>(updateLock, updateKey) {

            final Object internalLock = new Object();

            // TODO:Review - There's got to be a better way of initializing PowerFilter. Maybe the app management service could be queryable.
            @Override
            public void onConfigurationUpdated(PowerProxy configurationObject) {
                currentSystemModel = configurationObject;

                // This event must be fired only after we have finished configuring the system.
                // This prevents a race condition illustrated below where the application
                // deployment event is caught but does nothing due to a null configuration
                synchronized (internalLock) {
                    if (firstInitialization) {
                        firstInitialization = false;
                        papiContext.eventService().newEvent(PowerFilterEvent.POWER_FILTER_INITIALIZED, System.currentTimeMillis());
                    } else {
                        final List<FilterContext> newFilterChain = new PowerFilterChainBuilder(filterConfig).build(papiContext.classLoader(), currentSystemModel);
                        updateFilterList(newFilterChain);
                    }
                }
            }
        };

        applicationDeploymentListener = new EventListener<ApplicationDeploymentEvent, String>() {

            @Override
            public void onEvent(Event<ApplicationDeploymentEvent, String> e) {
                LOG.info("Application collection has been modified. Application that changed: " + e.payload());

                if (currentSystemModel != null) {
                    final List<FilterContext> newFilterChain = new PowerFilterChainBuilder(filterConfig).build(papiContext.classLoader(), currentSystemModel);

                    updateFilterList(newFilterChain);
                }
            }
        };
    }
    
    // This is written like this in case requests are already processing against the
    // existing filterChain.  If that is the case we create a new one for the deployment
    // update but the old list stays in memory as the garbage collector won't clean
    // it up until all RequestFilterChainState objects are no longer referencing it.

    private synchronized void updateFilterList(List<FilterContext> newFilterChain) {
        this.filterChain = new LinkedList<FilterContext>(newFilterChain);
    }

    protected PowerProxy getCurrentSystemModel() {
        return currentSystemModel;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        this.filterConfig = filterConfig;

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
        final PowerFilterChain requestFilterChainState = new PowerFilterChain(Collections.unmodifiableList(this.filterChain), chain, filterConfig.getServletContext());

        mutableHttpResponse.setHeader(CommonHttpHeader.CONTENT_TYPE.headerKey(), mutableHttpRequest.getHeader(CommonHttpHeader.ACCEPT.headerKey()));

        try {
            requestFilterChainState.startFilterChain(mutableHttpRequest, mutableHttpResponse);
        } catch (IOException t) {
            mutableHttpResponse.setStatus(HttpStatusCode.BAD_GATEWAY.intValue());

            LOG.error("Exception encountered while processing filter chain", t);
        } catch (ServletException t) {
            mutableHttpResponse.setStatus(HttpStatusCode.BAD_GATEWAY.intValue());

            LOG.error("Exception encountered while processing filter chain", t);
        } finally {
            papiContext.responseMessageService().handle(mutableHttpRequest, mutableHttpResponse);

            mutableHttpResponse.commitBufferToServletOutputStream();
        }
    }
}