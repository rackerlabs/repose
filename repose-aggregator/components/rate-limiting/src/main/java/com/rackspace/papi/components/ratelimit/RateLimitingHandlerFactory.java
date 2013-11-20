package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;

import com.rackspace.papi.components.ratelimit.write.ActiveLimitsWriter;
import com.rackspace.papi.components.ratelimit.write.CombinedLimitsWriter;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;

import com.rackspace.repose.service.ratelimit.RateLimitingServiceFactory;
import com.rackspace.repose.service.ratelimit.cache.ManagedRateLimitCache;
import com.rackspace.repose.service.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.config.DatastoreType;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.repose.service.ratelimit.RateLimitingService;

import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;

/* Responsible for creating rate limit handlers that provide datastoreservice and listener to rate limit configuration */
public class RateLimitingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RateLimitingHandler> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingHandlerFactory.class);
    private RateLimitCache rateLimitCache;
    //Volatile
    private Pattern describeLimitsUriRegex;
    private RateLimitingConfiguration rateLimitingConfig;
    private RateLimitingService service;
    private final DatastoreService datastoreService;

    public RateLimitingHandlerFactory(DatastoreService datastoreService) {
        this.datastoreService = datastoreService;

    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> listenerMap = new HashMap<Class, UpdateListener<?>>();
        listenerMap.put(RateLimitingConfiguration.class, new RateLimitingConfigurationListener());

        return listenerMap;
    }

    private Datastore getDatastore(DatastoreType datastoreType) {
        Datastore targetDatastore;

        final Collection<DatastoreManager> distributedDatastores = datastoreService.availableDistributedDatastores();

        if (StringUtilities.isNotBlank(datastoreType.value())) {
            LOG.info("Requesting datastore " + datastoreType);
            DatastoreManager datastore = datastoreService.getDatastore(datastoreType.value());

            if (datastore != null) {
                LOG.info("Using requested datastore " + datastoreType);
                return datastore.getDatastore();
            }

            LOG.warn("Requested datastore not found");
        }

        if (!distributedDatastores.isEmpty()) {
            DatastoreManager manager = distributedDatastores.iterator().next();
            LOG.info("Using distributed datastore " + manager.getName());
            targetDatastore = manager.getDatastore();
        } else {
            LOG.warn("There were no distributed datastore managers available. Clustering for rate-limiting will be disabled.");
            targetDatastore = datastoreService.defaultDatastore().getDatastore();
        }

        return targetDatastore;
    }

    private class RateLimitingConfigurationListener implements UpdateListener<RateLimitingConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(RateLimitingConfiguration configurationObject) {

            rateLimitCache = new ManagedRateLimitCache(getDatastore(configurationObject.getDatastore()));

            service = RateLimitingServiceFactory.createRateLimitingService(rateLimitCache, configurationObject);

            describeLimitsUriRegex = Pattern.compile(configurationObject.getRequestEndpoint().getUriRegex());

            rateLimitingConfig = configurationObject;

            isInitialized = true;

        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected RateLimitingHandler buildHandler() {

        if (!this.isInitialized()) {
            return null;
        }

        final ActiveLimitsWriter activeLimitsWriter = new ActiveLimitsWriter();
        final CombinedLimitsWriter combinedLimitsWriter = new CombinedLimitsWriter();
        final RateLimitingServiceHelper serviceHelper = new RateLimitingServiceHelper(service, activeLimitsWriter, combinedLimitsWriter);
        boolean includeAbsoluteLimits = rateLimitingConfig.getRequestEndpoint().isIncludeAbsoluteLimits();

        return new RateLimitingHandler(serviceHelper, includeAbsoluteLimits, describeLimitsUriRegex, rateLimitingConfig.isOverLimit429ResponseCode(),rateLimitingConfig.getDatastoreWarnLimit().intValue());
    }
}
