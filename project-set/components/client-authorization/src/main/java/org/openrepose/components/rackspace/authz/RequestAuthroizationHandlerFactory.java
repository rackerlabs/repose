package org.openrepose.components.rackspace.authz;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import org.openrepose.components.authz.rackspace.config.RackspaceAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestAuthroizationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RequestAuthroizationHandler> {

   private static final Logger LOG = LoggerFactory.getLogger(RequestAuthroizationHandlerFactory.class);
   private RackspaceAuthorization authorizationConfiguration;

   public RequestAuthroizationHandlerFactory() {
   }

   private class RoutingConfigurationListener implements UpdateListener<RackspaceAuthorization> {

      @Override
      public void configurationUpdated(RackspaceAuthorization configurationObject) {
         authorizationConfiguration = configurationObject;
      }
   }

   @Override
   protected RequestAuthroizationHandler buildHandler() {
      return new RequestAuthroizationHandler();
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
      updateListeners.put(RackspaceAuthorization.class, new RoutingConfigurationListener());

      return updateListeners;
   }
}
