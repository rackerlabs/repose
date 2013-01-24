package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.components.clientauth.common.AuthenticationHandler;
import com.rackspace.papi.components.clientauth.common.UriMatcher;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.components.clientauth.config.URIPattern;
import com.rackspace.papi.components.clientauth.config.WhiteList;
import com.rackspace.papi.components.clientauth.openstack.config.ClientMapping;
import com.rackspace.papi.components.clientauth.openstack.v1_0.OpenStackAuthenticationHandlerFactory;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping;
import com.rackspace.papi.components.clientauth.rackspace.v1_1.RackspaceAuthenticationHandlerFactory;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.service.datastore.Datastore;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * @author jhopper
 *
 * The purpose of this class is to handle client authentication. Multiple
 * authentication schemes may be used depending on the configuration. For
 * example, a Rackspace specific or Basic Http authentication.
 *
 */
public class ClientAuthenticationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<AuthenticationHandler> {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientAuthenticationHandlerFactory.class);
   private AuthenticationHandler authenticationModule;
   private KeyedRegexExtractor<String> accountRegexExtractor = new KeyedRegexExtractor<String>();
   private UriMatcher uriMatcher;
   private final Datastore datastore;

   public ClientAuthenticationHandlerFactory(Datastore datastore) {
      this.datastore = datastore;
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      final Map<Class, UpdateListener<?>> listenerMap = new HashMap<Class, UpdateListener<?>>();
      listenerMap.put(ClientAuthConfig.class, new ClientAuthConfigurationListener());

      return listenerMap;
   }

   private class ClientAuthConfigurationListener implements UpdateListener<ClientAuthConfig> {

       private boolean isInitialized = false;
      
      @Override
      public void configurationUpdated(ClientAuthConfig modifiedConfig) {

         updateUriMatcher(modifiedConfig.getWhiteList());         

         accountRegexExtractor.clear();
         if (modifiedConfig.getRackspaceAuth() != null) {
            authenticationModule = getRackspaceAuthHandler(modifiedConfig);
            for (AccountMapping accountMapping : modifiedConfig.getRackspaceAuth().getAccountMapping()) {
               accountRegexExtractor.addPattern(accountMapping.getIdRegex(), accountMapping.getType().value());
            }
         } else if (modifiedConfig.getOpenstackAuth() != null) {
            authenticationModule = getOpenStackAuthHandler(modifiedConfig);
            for (ClientMapping clientMapping : modifiedConfig.getOpenstackAuth().getClientMapping()) {
               accountRegexExtractor.addPattern(clientMapping.getIdRegex());
            }
         } else if (modifiedConfig.getHttpBasicAuth() != null) {
            // TODO: Create handler for HttpBasic
            authenticationModule = null;
         } else {
            LOG.error("Authentication module is not understood or supported. Please check your configuration.");
         }
         
          isInitialized = true;

      }
      
     @Override
      public boolean isInitialized(){
          return isInitialized;
      }
      
  
   }

   private void updateUriMatcher(WhiteList whiteList) {
      final List<Pattern> whiteListRegexPatterns = new ArrayList<Pattern>();

      if (whiteList != null) {
         for (URIPattern pattern : whiteList.getUriPattern()) {
            whiteListRegexPatterns.add(Pattern.compile(pattern.getUriRegex()));
         }
      }

      uriMatcher = new UriMatcher(whiteListRegexPatterns);
   }

   private AuthenticationHandler getRackspaceAuthHandler(ClientAuthConfig cfg) {
      return RackspaceAuthenticationHandlerFactory.newInstance(cfg, accountRegexExtractor, datastore, uriMatcher);
   }

   private AuthenticationHandler getOpenStackAuthHandler(ClientAuthConfig config) {
      return OpenStackAuthenticationHandlerFactory.newInstance(config, accountRegexExtractor, datastore, uriMatcher);
   }

   @Override
   protected AuthenticationHandler buildHandler() {
    if(!this.isInitialized()){
           return null;
       } 
      return authenticationModule;
   }
}
