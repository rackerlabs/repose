package com.rackspace.papi.components.unorm;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.http.normal.QueryStringNormalizer;
import com.rackspace.papi.components.unorm.normalizer.MediaTypeNormalizer;
import com.rackspace.papi.components.unorm.normalizer.MultiInstanceWhiteListFactory;
import com.rackspace.papi.components.uri.normalization.config.HttpMethod;
import com.rackspace.papi.components.uri.normalization.config.Target;
import com.rackspace.papi.components.uri.normalization.config.UriFilterList;
import com.rackspace.papi.components.uri.normalization.config.UriNormalizationConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UriNormalizationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<UriNormalizationHandler> {

   private Collection<QueryParameterNormalizer> queryStringNormalizers;
   private MediaTypeNormalizer mediaTypeNormalizer;


 
   private class UriNormalizationConfigurationListener implements UpdateListener<UriNormalizationConfig> {
       boolean isIntialized=false;
 
       
      @Override
      public void configurationUpdated(UriNormalizationConfig configurationObject) {
         final UriFilterList uriFilterList = configurationObject.getUriFilters();
         final Map<String, QueryParameterNormalizer> newNormalizers = new HashMap<String, QueryParameterNormalizer>();

         if (uriFilterList != null) {
            for (Target target : uriFilterList.getTarget()) {
               boolean alphabetize = target.isAlphabetize();
               final MultiInstanceWhiteListFactory whiteListFactory = new MultiInstanceWhiteListFactory(target.getWhitelist());
               final QueryStringNormalizer normalizerInstance = new QueryStringNormalizer(whiteListFactory,alphabetize);
               if(target.getHttpMethods().isEmpty()){
                   target.getHttpMethods().add(HttpMethod.ALL);
               }
               for (HttpMethod method : target.getHttpMethods()) {
                  QueryParameterNormalizer methodScopedNormalizer = newNormalizers.get(method.name());
                  
                  if (methodScopedNormalizer == null) {
                     methodScopedNormalizer = new QueryParameterNormalizer(method);
                     newNormalizers.put(method.name(), methodScopedNormalizer);
                  }
                  
                  methodScopedNormalizer.getUriSelector().addPattern(target.getUriRegex(), normalizerInstance);
               }
            }
         }

         queryStringNormalizers = newNormalizers.values();
         if(configurationObject.getMediaVariants()!=null){
         mediaTypeNormalizer = new MediaTypeNormalizer(configurationObject.getMediaVariants().getMediaType());
         }
         isIntialized=true;
      }
      
     @Override
      public boolean isInitialized(){
          return isIntialized;
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
       if(!this.isInitialized()){
           return null;
       } 
       
         return new UriNormalizationHandler(queryStringNormalizers, mediaTypeNormalizer);
       
   }
}
