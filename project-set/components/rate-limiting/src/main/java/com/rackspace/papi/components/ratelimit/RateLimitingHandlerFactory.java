package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.config.manager.UpdateListener;

import com.rackspace.papi.components.ratelimit.write.ActiveLimitsWriter;
import com.rackspace.papi.components.ratelimit.write.CombinedLimitsWriter;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import com.rackspace.papi.service.datastore.Datastore;

import com.rackspace.repose.service.ratelimit.RateLimitingServiceFactory;
import com.rackspace.repose.service.ratelimit.cache.ManagedRateLimitCache;
import com.rackspace.repose.service.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.repose.service.ratelimit.RateLimitingService;

import java.util.*;
import java.util.regex.Pattern;

public class RateLimitingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RateLimitingHandler> {

    private final RateLimitCache rateLimitCache;
    //Volatile
    private Pattern describeLimitsUriRegex =  null;
    private RateLimitingConfiguration rateLimitingConfig;
    private RateLimitingService service;

    public RateLimitingHandlerFactory(Datastore datastore) {
        rateLimitCache = new ManagedRateLimitCache(datastore);
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> listenerMap = new HashMap<Class, UpdateListener<?>>();
        listenerMap.put(RateLimitingConfiguration.class, new RateLimitingConfigurationListener());

        return listenerMap;
    }

    private class RateLimitingConfigurationListener implements UpdateListener<RateLimitingConfiguration> {

        @Override
        public void configurationUpdated(RateLimitingConfiguration configurationObject) {
            service = RateLimitingServiceFactory.createRateLimitingService(rateLimitCache, configurationObject);

            if (configurationObject.getRequestEndpoint() != null) {
                describeLimitsUriRegex = Pattern.compile(configurationObject.getRequestEndpoint().getUriRegex());    
            }

            rateLimitingConfig = configurationObject;
        }
    }

    @Override
    protected RateLimitingHandler buildHandler() {

        final ActiveLimitsWriter activeLimitsWriter = new ActiveLimitsWriter();
        final CombinedLimitsWriter combinedLimitsWriter = new CombinedLimitsWriter();
        final RateLimitingServiceHelper serviceHelper = new RateLimitingServiceHelper(service, activeLimitsWriter, combinedLimitsWriter);
        boolean includeAbsoluteLimits = false;

        if (rateLimitingConfig.getRequestEndpoint() != null) {
            includeAbsoluteLimits = rateLimitingConfig.getRequestEndpoint().isIncludeAbsoluteLimits();
        }

        return new RateLimitingHandler(serviceHelper, includeAbsoluteLimits, rateLimitingConfig.isDelegation(), describeLimitsUriRegex);
    }
}
