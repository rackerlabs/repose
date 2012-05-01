package com.rackspace.papi.components.unorm;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.http.normal.Normalizer;
import com.rackspace.papi.commons.util.http.normal.QueryStringNormalizer;
import com.rackspace.papi.commons.util.regex.RegexSelector;
import com.rackspace.papi.components.unorm.normalizer.MediaTypeNormalizer;
import com.rackspace.papi.components.unorm.normalizer.MultiInstanceWhiteListFactory;

import com.rackspace.papi.components.uri.normalization.config.Target;
import com.rackspace.papi.components.uri.normalization.config.UriFilterList;
import com.rackspace.papi.components.uri.normalization.config.UriNormalizationConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class UriNormalizationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<UriNormalizationHandler> {

   private RegexSelector<Normalizer<String>> queryStringNormalizers;
   private MediaTypeNormalizer mediaTypeNormalizer;

   private class UriNormalizationConfigurationListener implements UpdateListener<UriNormalizationConfig> {

      @Override
      public void configurationUpdated(UriNormalizationConfig configurationObject) {
         final UriFilterList uriFilterList = configurationObject.getUriFilters();
         final RegexSelector<Normalizer<String>> newNormalizers = new RegexSelector<Normalizer<String>>();

         if (uriFilterList != null) {
            for (Target target : uriFilterList.getTarget()) {
               final MultiInstanceWhiteListFactory whiteListFactory = new MultiInstanceWhiteListFactory(target.getWhitelist());
               newNormalizers.addPattern(target.getUriRegex(), new QueryStringNormalizer(whiteListFactory));
            }
         }

         queryStringNormalizers = newNormalizers;
         mediaTypeNormalizer = new MediaTypeNormalizer(configurationObject.getMediaVariants().getMediaType());
      }
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      final Map<Class, UpdateListener<?>> listenerMap = new HashMap<Class, UpdateListener<?>>();
      listenerMap.put(UriNormalizationConfig.class, new UriNormalizationConfigurationListener());

      return listenerMap;
   }

   @Override
   protected UriNormalizationHandler buildHandler() {
      return new UriNormalizationHandler(queryStringNormalizers, mediaTypeNormalizer);
   }
}
