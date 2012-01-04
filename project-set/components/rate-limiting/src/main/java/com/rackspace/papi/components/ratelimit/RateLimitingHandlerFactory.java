package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.ratelimit.cache.ManagedRateLimitCache;
import com.rackspace.papi.components.ratelimit.cache.RateLimitCache;
import com.rackspace.papi.components.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.papi.components.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
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
   
   //Volatile
   private Pattern describeLimitsUriRegex;
   private RateLimitingConfiguration rateLimitingConfig;

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
            if (limitGroup.isSetDefault() && defaultSet == true) {
               limitGroup.setDefault(false);
               LOG.warn("Rate-limiting Configuration has more than one default group set. Limit Group '"
                       + limitGroup.getId() + "' will not be set as a default limit group. Please update your configuration file.");
            } else if (limitGroup.isSetDefault()) {
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
   }

   public RateLimitingHandlerFactory(Datastore datastore) {
      rateLimitCache = new ManagedRateLimitCache(datastore);
      regexCache = new HashMap<String, Map<String, Pattern>>();
   }

   @Override
   protected RateLimitingHandler buildHandler() {
      return new RateLimitingHandler(regexCache, rateLimitCache, describeLimitsUriRegex, rateLimitingConfig);
   }
}
