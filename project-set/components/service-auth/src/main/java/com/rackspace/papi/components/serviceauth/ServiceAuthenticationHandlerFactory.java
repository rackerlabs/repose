package com.rackspace.papi.components.serviceauth;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import org.slf4j.Logger;
import com.rackspace.papi.components.serviceauth.config.Credentials;
import com.rackspace.papi.components.serviceauth.config.ServiceAuthConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author jhopper
 */
public class ServiceAuthenticationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ServiceAuthenticationHandler> {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ServiceAuthenticationHandlerFactory.class);
   private String base64EncodedCredentials;

   public ServiceAuthenticationHandlerFactory() {
   }
   
   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {
         {
            put(ServiceAuthConfig.class, new ServiceAuthenticationConfigurationListener());
         }
      };
   }

   @Override
   protected ServiceAuthenticationHandler buildHandler() {
      return new ServiceAuthenticationHandler(base64EncodedCredentials);
   }

   private class ServiceAuthenticationConfigurationListener implements UpdateListener<ServiceAuthConfig> {
      @Override
      public void configurationUpdated(ServiceAuthConfig modifiedConfig) {
         if (modifiedConfig.getHttpBasic() != null) {
            final Credentials creds = modifiedConfig.getHttpBasic().getCredentials();
            final String combinedCredentials = creds.getUsername() + ":" + creds.getPassword();

            try {
               base64EncodedCredentials = "Basic " + new String(Base64.encodeBase64(combinedCredentials.getBytes("UTF-8")), "UTF-8");
            } catch (UnsupportedEncodingException uee) {
               LOG.error("Failed to update basic credentials. Reason: " + uee.getMessage(), uee);
            }
         } else {
            LOG.error("Please check your configuration for service authentication. It appears to be malformed.");
         }
      }
   }

}
