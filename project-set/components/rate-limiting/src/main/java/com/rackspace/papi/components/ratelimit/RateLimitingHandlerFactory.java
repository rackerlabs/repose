package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.ratelimit.cache.ManagedRateLimitCache;
import com.rackspace.papi.components.ratelimit.cache.RateLimitCache;
import com.rackspace.papi.components.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.papi.components.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.papi.components.ratelimit.util.RateLimitKeyGenerator;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.service.datastore.Datastore;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * @author jhopper
 */
public final class RateLimitingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RateLimitingHandler> {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingHandlerFactory.class);
   
   private final Map<String, Map<String, Pattern>> regexCache;
   private final RateLimitCache rateLimitCache;
   private final RateLimiterBuilder rateLimiterBuilder;
   
   //Volatile
   private Pattern describeLimitsUriRegex;
   private RateLimitingConfiguration rateLimitingConfig;

   public RateLimitingHandlerFactory(Datastore datastore) {
      this(datastore, DefaultRateLimiterBuilder.getInstance());
   }

   public RateLimitingHandlerFactory(Datastore datastore, RateLimiterBuilder rateLimiterBuilder) {
      rateLimitCache = new ManagedRateLimitCache(datastore);
      regexCache = new HashMap<String, Map<String, Pattern>>();
      
      this.rateLimiterBuilder = rateLimiterBuilder;
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
         boolean defaultSet = false;

         regexCache.clear();

         for (ConfiguredLimitGroup limitGroup : configurationObject.getLimitGroup()) {
            final Map<String, Pattern> compiledRegexMap = new HashMap<String, Pattern>();

            // Makes sure that only the first limit group set to default is the only default group
            if (limitGroup.isDefault() && defaultSet == true) {
               limitGroup.setDefault(false);

               LOG.warn("Rate-limiting Configuration has more than one default group set. Limit Group '"
                       + limitGroup.getId() + "' will not be set as a default limit group. Please update your configuration file.");
            } else if (limitGroup.isDefault()) {
               defaultSet = true;
            }

            for (ConfiguredRatelimit configuredRatelimit : limitGroup.getLimit()) {
               compiledRegexMap.put(RateLimitKeyGenerator.createMapKey(configuredRatelimit), Pattern.compile(configuredRatelimit.getUriRegex()));
            }

            regexCache.put(limitGroup.getId(), compiledRegexMap);
         }

         describeLimitsUriRegex = Pattern.compile(configurationObject.getRequestEndpoint().getUriRegex());
         rateLimitingConfig = configurationObject;
      }
   }

   @Override
   protected RateLimitingHandler buildHandler() {
      return new RateLimitingHandler(regexCache, rateLimitCache, describeLimitsUriRegex, rateLimitingConfig, rateLimiterBuilder);
   }
}
