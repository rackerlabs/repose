package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;


public class TranslationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<TranslationHandler>  {
    private TranslationConfig config;
   
    public TranslationHandlerFactory() {
    }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {
         {
            put(TranslationConfig.class, new TranslationConfigurationListener());
         }
      };
   }

   @Override
   protected TranslationHandler buildHandler() {
      return new TranslationHandler(config);
   }
    
    private class TranslationConfigurationListener implements UpdateListener<TranslationConfig> {
       @Override
       public void configurationUpdated(TranslationConfig configurationObject) {
           config = configurationObject;
       }
    }
}
