package org.openrepose.components.rackspace.authz;

import com.rackspace.auth.openstack.ids.AuthenticationServiceClient;
import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import org.openrepose.components.authz.rackspace.config.AuthenticationServer;
import org.openrepose.components.authz.rackspace.config.RackspaceAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestAuthroizationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RequestAuthroizationHandler> {

   private static final Logger LOG = LoggerFactory.getLogger(RequestAuthroizationHandlerFactory.class);
   private RackspaceAuthorization authorizationConfiguration;
   private OpenStackAuthenticationService authenticationService;

   public RequestAuthroizationHandlerFactory() {
   }

   private class RoutingConfigurationListener implements UpdateListener<RackspaceAuthorization> {

      @Override
      public void configurationUpdated(RackspaceAuthorization configurationObject) {
         authorizationConfiguration = configurationObject;

         final AuthenticationServer serverInfo = authorizationConfiguration.getAuthenticationServer();

         if (serverInfo != null) {
            authenticationService = new AuthenticationServiceClient(serverInfo.getHref(), serverInfo.getUsername(), serverInfo.getPassword());
         }
      }
   }

   @Override
   protected RequestAuthroizationHandler buildHandler() {
      if (authenticationService == null) {
         LOG.error("Component has not been initialized yet. Please check your configurations.");
         throw new IllegalStateException("Component has not been initialized yet");
      }

      return new RequestAuthroizationHandler(authenticationService);
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
      updateListeners.put(RackspaceAuthorization.class, new RoutingConfigurationListener());

      return updateListeners;
   }
}
