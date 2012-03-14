package com.rackspace.papi.components.translation;


import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.xproc.Pipeline;
import com.rackspace.papi.components.translation.xproc.PipelineException;
import com.rackspace.papi.components.translation.xproc.calabash.CalabashPipelineBuilder;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;


public class TranslationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<TranslationHandler>  {
    private TranslationConfig config;
    private Pool<Pipeline> requestPipelinePool;
    private Pool<Pipeline> responsePipelinePool;
   
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
      return new TranslationHandler(config, requestPipelinePool, responsePipelinePool);
   }
    
    private class TranslationConfigurationListener implements UpdateListener<TranslationConfig> {
       @Override
       public void configurationUpdated(TranslationConfig configurationObject) {
           config = configurationObject;
            requestPipelinePool = new GenericBlockingResourcePool<Pipeline>(new ConstructionStrategy<Pipeline>() {

               @Override
               public Pipeline construct() throws ResourceConstructionException {
                  try {
                   return new CalabashPipelineBuilder(false).build(config.getRequestTranslationProcess().getHref());

                  } catch (PipelineException ex) {
                     throw new ResourceConstructionException("Unable to build request pipeline.  Reason: " + ex.getMessage(), ex);
                  }
               }
            });
            responsePipelinePool = new GenericBlockingResourcePool<Pipeline>(new ConstructionStrategy<Pipeline>() {

               @Override
               public Pipeline construct() throws ResourceConstructionException {
                  try {
                   return new CalabashPipelineBuilder(false).build(config.getResponseTranslationProcess().getHref());

                  } catch (PipelineException ex) {
                     throw new ResourceConstructionException("Unable to build request pipeline. Reason: " + ex.getMessage(), ex);
                  }
               }
            });
       }
    }

}
