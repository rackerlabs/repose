package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.auth.AuthModule;
import com.rackspace.papi.commons.config.manager.UpdateListener;

import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
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
            authenticationModule = getAuth1_1Handler(modifiedConfig);
        } else if (modifiedConfig.getOpenstackAuth() != null) {
            authenticationModule = getAuth2_0Handler(modifiedConfig);
        } else if (modifiedConfig.getHttpBasicAuth() != null) {
            // TODO: Create handler for HttpBasic
            authenticationModule = null;
        } else {
            LOG.error("Authentication module is not understood or supported. Please check your configuration.");
        }
      }
   }

   private AuthModule getAuth1_1Handler(ClientAuthConfig config) {
       return new com.rackspace.papi.components.clientauth.rackspace.v1_1.RackspaceAuthenticationHandler(config.getRackspaceAuth());
   }

   private AuthModule getAuth2_0Handler(ClientAuthConfig config) {
       return new com.rackspace.papi.components.clientauth.openstack.v1_0.OpenStackAuthenticationHandler(config.getOpenstackAuth());            
   }   

   protected AuthModule buildHandler() {
      return authenticationModule;
   }

}
