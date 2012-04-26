package com.rackspace.papi.components.unorm;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.unorm.normalizer.MediaTypeNormalizer;

import com.rackspace.papi.components.uri.normalization.config.Target;
import com.rackspace.papi.components.uri.normalization.config.UriFilterList;
import com.rackspace.papi.components.uri.normalization.config.UriNormalizationConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class UriNormalizationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<UriNormalizationHandler> {

   private MediaTypeNormalizer mediaTypeNormalizer;

   public UriNormalizationHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {
         {
            put(UriNormalizationConfig.class, new UriNormalizationConfigurationListener());
         }
      };
   }

   private class UriNormalizationConfigurationListener implements UpdateListener<UriNormalizationConfig> {
      @Override
      public void configurationUpdated(UriNormalizationConfig configurationObject) {

         final UriFilterList uriFilterList = configurationObject.getUriFilters();
         if (uriFilterList != null) {
            for (Target target : uriFilterList.getTarget()) {
               // TODO: Create object with pre-compiled regexes and pass that into the handler       
            }
         }
      }
   }
   
   @Override
   protected UriNormalizationHandler buildHandler() {
      return new UriNormalizationHandler(mediaTypeNormalizer);
   }
   
}
