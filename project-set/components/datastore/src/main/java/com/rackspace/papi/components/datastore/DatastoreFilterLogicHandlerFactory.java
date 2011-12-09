package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DatastoreFilterLogicHandlerFactory extends AbstractConfiguredFilterHandlerFactory<DatastoreFilterLogicHandler> {

   private static final Logger LOG = LoggerFactory.getLogger(DatastoreFilterLogicHandlerFactory.class);

   private final MutableClusterView clusterView;
   private final HashedDatastore hashRingDatastore;
   
   private String lastLocalAddr;

   public DatastoreFilterLogicHandlerFactory(MutableClusterView clusterView, HashedDatastore hashRingDatastore) {
      this.clusterView = clusterView;
      this.hashRingDatastore = hashRingDatastore;
   }
   
   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      final Map<Class, UpdateListener<?>> listeners = new HashMap<Class, UpdateListener<?>>();
      listeners.put(PowerProxy.class, new SystemModelUpdateListener());
      
      return listeners;
   }

   protected void updateClusterMembers(PowerProxy configuration) {
      try {
         final List<InetSocketAddress> cacheSiblings = new LinkedList<InetSocketAddress>();

         for (Host hostInformation : configuration.getHost()) {
            if (hostInformation.getFilters() != null) {
               for (Filter f : hostInformation.getFilters().getFilter()) {
                  if (f.getName().equals("dist-datastore")) {
                     final InetAddress hostAddress = InetAddress.getByName(hostInformation.getHostname());
                     final InetSocketAddress hostSocketAddress = new InetSocketAddress(hostAddress, hostInformation.getServicePort());

                     cacheSiblings.add(hostSocketAddress);
                  }
               }
            }
         }

         clusterView.updateMembers(cacheSiblings.toArray(new InetSocketAddress[cacheSiblings.size()]));
      } catch (UnknownHostException uhe) {
         LOG.error(uhe.getMessage(), uhe);
      }
   }

   private class SystemModelUpdateListener implements UpdateListener<PowerProxy> {

      @Override
      public void configurationUpdated(PowerProxy configurationObject) {
         if (configurationObject == null) {
            LOG.error("Power Proxy configuration was null - please check your configurations and error logs");
            return;
         }

         updateClusterMembers(configurationObject);
      }
   }

   @Override
   protected DatastoreFilterLogicHandler buildHandler() {
      return new DatastoreFilterLogicHandler(clusterView, lastLocalAddr, hashRingDatastore);
   }
}
