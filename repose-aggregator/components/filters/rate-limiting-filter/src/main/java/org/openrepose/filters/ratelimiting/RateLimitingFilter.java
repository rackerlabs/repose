/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.ratelimiting;

import com.google.common.base.Optional;

import org.openrepose.commons.config.manager.UpdateFailedException;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.servlet.filter.FilterAction;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.commons.utils.servlet.http.ResponseMode;
import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.event.common.EventService;
import org.openrepose.core.services.ratelimit.RateLimitingService;
import org.openrepose.core.services.ratelimit.RateLimitingServiceFactory;
import org.openrepose.core.services.ratelimit.cache.ManagedRateLimitCache;
import org.openrepose.core.services.ratelimit.config.DatastoreType;
import org.openrepose.core.services.ratelimit.config.RateLimitingConfiguration;
import org.openrepose.filters.ratelimiting.write.ActiveLimitsWriter;
import org.openrepose.filters.ratelimiting.write.CombinedLimitsWriter;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Named
public class RateLimitingFilter implements Filter, UpdateListener<RateLimitingConfiguration> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String DEFAULT_CONFIG = "rate-limiting.cfg.xml";
    private static final String SCHEMA_FILE_NAME = "/META-INF/schema/config/rate-limiting-configuration.xsd";
    private static final String DEFAULT_DATASTORE_NAME = "local/default";

    private final ConfigurationService configurationService;
    private String configFilename;

    private final DatastoreService datastoreService;
    private final EventService eventService;
    private final AtomicReference<RateLimitingConfig> config = new AtomicReference<>();
    private boolean initialized = false;

    @Inject
    public RateLimitingFilter(
            DatastoreService datastoreService,
            ConfigurationService configurationService,
            EventService eventService) {
        this.datastoreService = datastoreService;
        this.configurationService = configurationService;
        this.eventService = eventService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.trace("Rate Limiting filter initializing...");
        configFilename = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);

        LOG.info("Initializing Rate Limiting filter using config {}", configFilename);
        URL xsdURL = getClass().getResource(SCHEMA_FILE_NAME);
        configurationService.subscribeTo(filterConfig.getFilterName(), configFilename, xsdURL, this, RateLimitingConfiguration.class);

        LOG.trace("Rate Limiting filter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!initialized) {
            LOG.error("Rate Limiting filter has not yet initialized...");
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else {
            HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper((HttpServletRequest) request);
            HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(
                    (HttpServletResponse) response, ResponseMode.MUTABLE, ResponseMode.MUTABLE);

            RateLimitingHandler handler = buildHandler();
            FilterAction filterAction = handler.handleRequest(wrappedRequest, wrappedResponse);
            switch (filterAction) {
                case RETURN:
                    break; // no further processing
                case PASS:
                    chain.doFilter(wrappedRequest, wrappedResponse);
                    break;
                case PROCESS_RESPONSE:
                    chain.doFilter(wrappedRequest, wrappedResponse);
                    handler.handleResponse(wrappedRequest, wrappedResponse);
                    break;
                default:
                    LOG.error("Unexpected internal filter state");
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            wrappedResponse.commitToResponse();
        }
    }

    @Override
    public void destroy() {
        LOG.trace("Rate Limiting filter destroying...");
        configurationService.unsubscribeFrom(configFilename, this);
        LOG.trace("Rate Limiting filter destroyed.");
    }

    @Override
    public void configurationUpdated(RateLimitingConfiguration configurationObject) throws UpdateFailedException {
        RateLimitingConfig newConfig = new RateLimitingConfig();
        newConfig.rateLimitingService = RateLimitingServiceFactory.createRateLimitingService(
                new ManagedRateLimitCache(getDatastore(configurationObject.getDatastore())), configurationObject);
        newConfig.describeLimitsUriRegex = configurationObject.getRequestEndpoint() != null ?
                Optional.of(Pattern.compile(configurationObject.getRequestEndpoint().getUriRegex())) :
                Optional.<Pattern>absent();
        newConfig.includeAbsoluteLimits = configurationObject.getRequestEndpoint() != null &&
                configurationObject.getRequestEndpoint().isIncludeAbsoluteLimits();
        newConfig.isOverLimit429ResponseCode = configurationObject.isOverLimit429ResponseCode();
        newConfig.datastoreWarnLimit = configurationObject.getDatastoreWarnLimit().intValue();

        config.set(newConfig);
        initialized = true;
    }

    /**
     * For now, carry over the old behavior of building a new handler per request.
     */
    private RateLimitingHandler buildHandler() {
        // snapshot of current config
        RateLimitingConfig currentConfig = config.get();

        return new RateLimitingHandler(
                new RateLimitingServiceHelper(currentConfig.rateLimitingService, new ActiveLimitsWriter(), new CombinedLimitsWriter()),
                eventService,
                currentConfig.includeAbsoluteLimits,
                currentConfig.describeLimitsUriRegex,
                currentConfig.isOverLimit429ResponseCode,
                currentConfig.datastoreWarnLimit);
    }

    private Datastore getDatastore(DatastoreType datastoreType) {
        String requestedDatastore = datastoreType.value();
        if (StringUtilities.isNotBlank(requestedDatastore)) {
            LOG.info("Requesting datastore {}", datastoreType);

            if (requestedDatastore.equals(DEFAULT_DATASTORE_NAME)) {
                LOG.info("Using requested datastore {}", requestedDatastore);
                return datastoreService.getDefaultDatastore();
            }

            Datastore datastore = datastoreService.getDatastore(requestedDatastore);
            if (datastore != null) {
                LOG.info("Using requested datastore {}", requestedDatastore);
                return datastore;
            } else {
                LOG.warn("Requested datastore not found");
            }
        }

        Datastore targetDatastore = datastoreService.getDistributedDatastore();
        if (targetDatastore != null) {
            LOG.info("Using distributed datastore {}", targetDatastore.getName());
        } else {
            LOG.warn("There were no distributed datastore managers available. Clustering for rate-limiting will be disabled.");
            targetDatastore = datastoreService.getDefaultDatastore();
        }
        return targetDatastore;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    private static class RateLimitingConfig {
        public RateLimitingService rateLimitingService;
        public Optional<Pattern> describeLimitsUriRegex;
        public boolean includeAbsoluteLimits;
        public boolean isOverLimit429ResponseCode;
        public int datastoreWarnLimit;
    }
}
