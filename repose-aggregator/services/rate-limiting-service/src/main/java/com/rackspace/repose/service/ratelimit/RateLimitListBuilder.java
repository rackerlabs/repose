package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.RateLimit;
import com.rackspace.repose.service.limits.schema.RateLimitList;
import com.rackspace.repose.service.limits.schema.ResourceRateLimits;
import com.rackspace.repose.service.ratelimit.cache.CachedRateLimit;
import com.rackspace.repose.service.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import org.slf4j.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.*;

public class RateLimitListBuilder {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitListBuilder.class);
    private static final DatatypeFactory DATATYPE_FACTORY;
    
    // TODO: Clean this up with a static initializer method
    static {
        DatatypeFactory factory = null;
        
        try {
            factory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException dce) {
            LOG.error("Unable to create a new DatatypeFactory instance. Reason: " + dce.getMessage(), dce);
        } finally {
            DATATYPE_FACTORY = factory;
        }
    }
    
    private final Map<String, CachedRateLimit> cachedRateLimits;
    private final Map<String, ResourceRateLimits> liveRateLimitMap;
    private final List<ConfiguredLimitGroup> configuredLimitGroups;

    //TODO: Remove this after refactoring tests
    private static List<ConfiguredLimitGroup> asList(ConfiguredLimitGroup group) {
        final List<ConfiguredLimitGroup> list = new LinkedList<ConfiguredLimitGroup>();
        list.add(group);
        
        return list;
    }
    
    public RateLimitListBuilder(Map<String, CachedRateLimit> cachedRateLimits, ConfiguredLimitGroup configuredLimitGroup) {
        this(cachedRateLimits, asList(configuredLimitGroup));
    }

    public RateLimitListBuilder(Map<String, CachedRateLimit> cachedRateLimits, List<ConfiguredLimitGroup> configuredLimitGroups) {
        this.cachedRateLimits = cachedRateLimits;
        this.configuredLimitGroups = configuredLimitGroups;

        liveRateLimitMap = new HashMap<String, ResourceRateLimits>();
    }

    public RateLimitList toRateLimitList() {
        if (DATATYPE_FACTORY == null) {
            throw new IllegalStateException("DatatypeFactory for producing limits responses is not set. This is a runtime error.");
        }
        
        final RateLimitList rateLimitList = new RateLimitList();

        final Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));

        // Process limit returns for each limit group the client's role belongs to
        for (ConfiguredLimitGroup configuredLimitGroup : configuredLimitGroups) {
            for (ConfiguredRatelimit configuredRateLimit : configuredLimitGroup.getLimit()) {
                final CachedRateLimit cachedLimit = getCachedRateLimitFromSet(configuredRateLimit.getUriRegex(), cachedRateLimits.values());
                
                processLiveRateLimits(configuredRateLimit, cal, cachedLimit);
            }
        }

        for (ResourceRateLimits resourceScopedLimits : liveRateLimitMap.values()) {
            rateLimitList.getRate().add(resourceScopedLimits);
        }

        return rateLimitList;
    }
    
    private CachedRateLimit getCachedRateLimitFromSet(String uriRegex, Collection<CachedRateLimit> limitSet) {
        final int uriRegexHashCode = uriRegex.hashCode();
        
        for (CachedRateLimit cachedRateLimit : limitSet) {
            if (cachedRateLimit.getRegexHashcode() == uriRegexHashCode) {
                return cachedRateLimit;
            }
        }
        
        return null;
    }

    private void processLiveRateLimits(ConfiguredRatelimit configuredRateLimit, Calendar cal, CachedRateLimit cachedLimit) {
        for (HttpMethod method : configuredRateLimit.getHttpMethods()) {
            final RateLimit limit = new RateLimit();

            limit.setValue(configuredRateLimit.getValue());
            limit.setUnit(configuredRateLimit.getUnit());
            limit.setVerb(method);

            long now = System.currentTimeMillis(), earliestExpirationDate = now;
            int remainingRequests = configuredRateLimit.getValue();

            if (cachedLimit != null) {
                earliestExpirationDate = cachedLimit.getEarliestExpirationTime(method);
                remainingRequests = configuredRateLimit.getValue() - cachedLimit.amount(method);
            }

            cal.setTimeInMillis(earliestExpirationDate);

            limit.setRemaining(remainingRequests);
            limit.setNextAvailable(DATATYPE_FACTORY.newXMLGregorianCalendar((GregorianCalendar) cal));

            ResourceRateLimits rateLimits = liveRateLimitMap.get(configuredRateLimit.getUriRegex());

            if (rateLimits == null) {
                rateLimits = new ResourceRateLimits();
                rateLimits.setRegex(configuredRateLimit.getUriRegex());
                rateLimits.setUri(configuredRateLimit.getUri());

                liveRateLimitMap.put(configuredRateLimit.getUriRegex(), rateLimits);
            }

            rateLimits.getLimit().add(limit);
        }
    }
}
