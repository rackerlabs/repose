package com.rackspace.papi.components.hnorm;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.HeaderFilterList;
import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.HeaderNormalizationConfig;
import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.Target;

import java.util.HashMap;
import java.util.Map;

public class HeaderNormalizationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderNormalizationHandler> {


   public HeaderNormalizationHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {
         {
            put(HeaderNormalizationConfig.class, new ContentNormalizationConfigurationListener());
         }
      };
   }

   private class ContentNormalizationConfigurationListener implements UpdateListener<HeaderNormalizationConfig> {
      @Override
      public void configurationUpdated(HeaderNormalizationConfig configurationObject) {
         final HeaderFilterList filterList = configurationObject.getHeaderFilters();

         if (filterList != null) {
            for (Target target : filterList.getTarget()) {
               // TODO: Build objects with pre-compiled regexes and pass those to the Handler
            }
         }
      }
   }

   @Override
   protected HeaderNormalizationHandler buildHandler() {
      return new HeaderNormalizationHandler();
   }
}
