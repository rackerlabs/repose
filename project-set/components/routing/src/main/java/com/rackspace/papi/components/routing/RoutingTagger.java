package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.config.manager.LockedConfigurationUpdater;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;
import com.rackspace.papi.domain.HostUtilities;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import java.net.MalformedURLException;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingTagger extends AbstractFilterLogicHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingTagger.class);
    private final UpdateListener<PowerProxy> systemModelUpdateListener;
    private final KeyedStackLock configurationLock;
    private final Object updateKey, readKey;
    private PowerProxy systemModel;

    public RoutingTagger() {
        updateKey = new Object();
        readKey = new Object();

        configurationLock = new KeyedStackLock();

        systemModelUpdateListener = new LockedConfigurationUpdater<PowerProxy>(configurationLock, updateKey) {

            @Override
            protected void onConfigurationUpdated(PowerProxy configurationObject) {
                systemModel = configurationObject;
            }
        };
    }

    public UpdateListener<PowerProxy> getSystemModelUpdateListener() {
        return systemModelUpdateListener;
    }

    private PowerProxy getSystemModel() {
        configurationLock.lock(readKey);

        try {
            return systemModel;
        } finally {
            configurationLock.unlock(readKey);
        }
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);

        final String firstRoutingDestination = request.getHeader(PowerApiHeader.ROUTE_DESTINATION.headerKey());

        if (firstRoutingDestination == null) {
            final SystemModelInterrogator interrogator = new SystemModelInterrogator(getSystemModel());
            final Host nextRoutableHost = interrogator.getNextRoutableHost();

            try {
                myDirector.requestHeaderManager().putHeader(PowerApiHeader.ROUTE_DESTINATION.headerKey(), HostUtilities.asUrl(nextRoutableHost, request.getRequestURI()));
            } catch (MalformedURLException murle) {
                // TODO: Malformed URL Expcetions are unexpected and should return as a 502
                LOG.error(murle.getMessage(), murle);
                
                myDirector.setFilterAction(FilterAction.RETURN);
                myDirector.setResponseStatus(HttpStatusCode.BAD_GATEWAY);
            }
        }

        return myDirector;
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
        return super.handleResponse(request, response);
    }
}
