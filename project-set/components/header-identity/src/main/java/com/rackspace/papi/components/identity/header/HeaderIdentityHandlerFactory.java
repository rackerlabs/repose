package com.rackspace.papi.components.identity.header;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.identity.header.config.HeaderIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.components.identity.header.config.HttpHeader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HeaderIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderIdentityHandler> {


   private List<HttpHeader> sourceHeaders;

   public HeaderIdentityHandlerFactory() {
       sourceHeaders = new ArrayList<HttpHeader>();
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(HeaderIdentityConfig.class, new HeaderIdentityConfigurationListener());
         }
      };
   }

   private class HeaderIdentityConfigurationListener implements UpdateListener<HeaderIdentityConfig> {

      @Override
      public void configurationUpdated(HeaderIdentityConfig configurationObject) {
          sourceHeaders = configurationObject.getSourceHeaders().getHeader();
      }
   }

   @Override
   protected HeaderIdentityHandler buildHandler() {
      return new HeaderIdentityHandler(sourceHeaders);
   }
}
