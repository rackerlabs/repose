package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.auth.AuthModule;
import com.rackspace.papi.commons.config.manager.UpdateListener;

import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.components.clientauth.rackspace.RackspaceAuthenticationHandler;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

/**
 *
 * @author jhopper
 *
 * The purpose of this class is to handle client authentication. Multiple
 * authentication schemes may be used depending on the configuration. For
 * example, a Rackspace specific or Basic Http authentication.
 *
 */
public class ClientAuthenticationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<AuthModule> {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientAuthenticationHandlerFactory.class);
   private AuthModule authenticationModule;

   public ClientAuthenticationHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {
         {
            put(ClientAuthConfig.class, new ClientAuthConfigurationListener());
         }
      };
   }

   private class ClientAuthConfigurationListener implements UpdateListener<ClientAuthConfig> {
      @Override
      public void configurationUpdated(ClientAuthConfig modifiedConfig) {
         if (modifiedConfig.getRackspaceAuth() != null) {
            authenticationModule = new RackspaceAuthenticationHandler(modifiedConfig.getRackspaceAuth());
         } else if (modifiedConfig.getHttpBasicAuth() == null) {
            LOG.error("Authentication module is not understood or supported. Please check your configuration.");
         }
      }
   }
   

   protected AuthModule buildHandler() {
      return authenticationModule;
   }

}
