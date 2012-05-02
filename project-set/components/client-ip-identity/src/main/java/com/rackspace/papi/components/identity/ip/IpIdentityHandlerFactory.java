package com.rackspace.papi.components.identity.ip;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.net.IpAddressRange;
import com.rackspace.papi.components.identity.ip.config.IpIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;

public class IpIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<IpIdentityHandler> {

   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(IpIdentityHandlerFactory.class);
   private IpIdentityConfig config;
   private List<IpAddressRange> whitelist;

   public IpIdentityHandlerFactory() {
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {

         {
            put(IpIdentityConfig.class, new ClientIpIdentityConfigurationListener());
         }
      };
   }

   private class ClientIpIdentityConfigurationListener implements UpdateListener<IpIdentityConfig> {

      @Override
      public void configurationUpdated(IpIdentityConfig configurationObject) {
         config = configurationObject;
         whitelist = new ArrayList<IpAddressRange>();
         if (config.getWhiteList() != null) {
            for (String address : config.getWhiteList().getIpAddress()) {
               try {
                  whitelist.add(new IpAddressRange(address));
               } catch (UnknownHostException ex) {
                  LOG.warn("Invalid IP address specified in white list: " + address);
               }
            }
         }
      }
   }

   @Override
   protected IpIdentityHandler buildHandler() {
      return new IpIdentityHandler(config, whitelist);
   }
}
