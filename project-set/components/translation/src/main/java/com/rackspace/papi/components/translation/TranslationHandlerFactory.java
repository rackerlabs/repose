package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.config.manager.UpdateListener;
//import com.rackspace.papi.components.translation.config.TransformerType;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import javax.xml.transform.TransformerFactory;
import java.util.HashMap;
import java.util.Map;


public class TranslationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<TranslationHandler>  {
    private TranslationConfig config;
    private final Map<String, Transformer> transformers;
    private final String configDirectory;
   
    public TranslationHandlerFactory(String configDirectory) {
        this.configDirectory = configDirectory;
        transformers = new HashMap<String, Transformer>();
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
      return new TranslationHandler(config, transformers, configDirectory);
   }
    
    private class TranslationConfigurationListener implements UpdateListener<TranslationConfig> {
       @Override
       public void configurationUpdated(TranslationConfig configurationObject) {
           config = configurationObject;
           // TODO: For the first version of translation update translation templates at this point.
           // We can pull a list of the translation files from the TranslationConfig object and
           // create the new Templates from there.
       }
    }

}
