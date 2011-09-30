package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.config.manager.LockedConfigurationUpdater;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;
import com.rackspace.papi.components.ratelimit.cache.ManagedRateLimitCache;
import com.rackspace.papi.components.ratelimit.cache.RateLimitCache;
import com.rackspace.papi.components.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.papi.components.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * @author jhopper
 */
public final class RateLimitingHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingHandler.class);
    
    private final UpdateListener<RateLimitingConfiguration> rateLimitingConfigurationListener;
    private final Map<String, Map<String, Pattern>> regexCache;
    private final KeyedStackLock updateLock;
    private final Object updateKey, readKey;
    private final RateLimitCache rateLimitCache;
    
    //Volatile
    private Pattern describeLimitsUriRegex;
    private RateLimitingConfiguration rateLimitingConfig;

    private final class RateLimitingConfigListener extends LockedConfigurationUpdater<RateLimitingConfiguration> {
        private RateLimitingConfigListener(KeyedStackLock updateLock, Object updateKey) {
            super(updateLock, updateKey);
        }

        @Override
        protected void onConfigurationUpdated(RateLimitingConfiguration configurationObject) {
            
            boolean defaultSet=false;
            
            regexCache.clear();

            for (ConfiguredLimitGroup limitGroup : configurationObject.getLimitGroup()) {
                final Map<String, Pattern> compiledRegexMap = new HashMap<String, Pattern>();
                
                // Makes sure that only the first limit group set to default is the only default group
                if(limitGroup.isDefault() && defaultSet==true){
                    limitGroup.setDefault(false);
                    LOG.warn("Rate-limiting Configuration has more than one default group set. Limit Group '"
                            + limitGroup.getId() + "' will not be set as a default limit group. Please update your configuration file.");
                }else if (limitGroup.isDefault()){
                    defaultSet = true;
                }

                for (ConfiguredRatelimit configuredLimitGroup : limitGroup.getLimit()) {
                    compiledRegexMap.put(configuredLimitGroup.getUri(), Pattern.compile(configuredLimitGroup.getUriRegex()));
                }

                regexCache.put(limitGroup.getId(), compiledRegexMap);
            }

            describeLimitsUriRegex = Pattern.compile(configurationObject.getRequestEndpoint().getUriRegex());
            rateLimitingConfig = configurationObject;

        }
    };

    public RateLimitingHandler(Datastore datastore) {
        rateLimitCache = new ManagedRateLimitCache(datastore);
        updateLock = new KeyedStackLock();
        readKey = new Object();
        updateKey = new Object();
        regexCache = new HashMap<String, Map<String, Pattern>>();
        rateLimitingConfigurationListener = new RateLimitingConfigListener(updateLock, updateKey);    
    }

    public UpdateListener<RateLimitingConfiguration> getRateLimitingConfigurationListener() {
        return rateLimitingConfigurationListener;
    }

    private RateLimiter newRateLimiter() {
        updateLock.lock(readKey);

        try {
            return new RateLimiter(rateLimitCache, rateLimitingConfig, regexCache);
        } finally {
            updateLock.unlock(readKey);
        }
    }

    public boolean requestHasExpectedHeaders(HttpServletRequest request) {
        return request.getHeader(PowerApiHeader.USER.headerKey()) != null;
    }

    private void describeLimitsForRequest(final FilterDirector director, HttpServletRequest request) {
        // Should we include the absolute limits from the service origin?
        if (rateLimitingConfig.getRequestEndpoint().isIncludeAbsoluteLimits()) {

            // Process the response on the way back up the filter chain
            director.setFilterAction(FilterAction.PROCESS_RESPONSE);
        } else {
            new RateLimiterResponse(rateLimitCache, rateLimitingConfig).writeActiveLimits(new RateLimitingRequestInfo(request), director);

            director.setFilterAction(FilterAction.RETURN);
            director.setResponseStatus(HttpStatusCode.OK);
        }
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request) {
        final FilterDirector director = new FilterDirectorImpl();

        // Does the request contain expected headers?
        if (requestHasExpectedHeaders(request)) {
            final String requestUri = request.getRequestURI();

            // We set the default action to PASS in this case since further
            // logic may or may not change the action and this request can now
            // be considered valid.
            director.setFilterAction(FilterAction.PASS);

            // Does the request match the configured getCurrentLimits API call endpoint?
            if (describeLimitsUriRegex.matcher(requestUri).matches()) {
                describeLimitsForRequest(director, request);
            } else {
                newRateLimiter().recordLimitedRequest(new RateLimitingRequestInfo(request), director);
            }
        } else {
            LOG.warn("Expected header: " + PowerApiHeader.USER.headerKey()
                    + " was not supplied in the request. Rate limiting requires this header to operate.");

            // Auto return a 401 if the request does not meet expectations
            director.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
            director.setFilterAction(FilterAction.RETURN);
        }

        return director;
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector director = new FilterDirectorImpl();

        new RateLimiterResponse(rateLimitCache, rateLimitingConfig).writeCombinedLimits(new RateLimitingRequestInfo(request), response, director);

        return director;
    }
}
