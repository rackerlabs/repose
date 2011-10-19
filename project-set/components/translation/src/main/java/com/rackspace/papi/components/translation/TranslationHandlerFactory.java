package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.translation.config.TransformerType;
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

        Transformer saxonTransformer = new TransformerImpl(TransformerType.NET_SF_SAXON_TRANSFORMER_FACTORY_IMPL.value(), null);
        Transformer stxTransformer = new TransformerImpl(TransformerType.NET_SF_JOOST_TRAX_TRANSFORMER_FACTORY_IMPL.value(), null);

        transformers.put(TransformerType.NET_SF_SAXON_TRANSFORMER_FACTORY_IMPL.value(), saxonTransformer);
        transformers.put(TransformerType.NET_SF_JOOST_TRAX_TRANSFORMER_FACTORY_IMPL.value(), stxTransformer);
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
       }
    }
}
