package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.datastore.hash.HashRingDatastore;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.UUIDEncodingProvider;
import org.openrepose.components.datastore.config.DistributedDatastoreConfiguration;
import org.openrepose.components.datastore.config.HostAccessControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;

public class DatastoreFilterLogicHandlerFactory extends AbstractConfiguredFilterHandlerFactory<DatastoreFilterLogicHandler> {

   private static final Logger LOG = LoggerFactory.getLogger(DatastoreFilterLogicHandlerFactory.class);
   private static final DatastoreAccessControl DEFAULT_DATASTORE_ACL = new DatastoreAccessControl(Collections.EMPTY_LIST, true);
   
   private final MutableClusterView clusterView;
   private final HashRingDatastore hashRingDatastore;
   private DatastoreAccessControl hostACL;

   public DatastoreFilterLogicHandlerFactory(MutableClusterView clusterView, HashRingDatastore hashRingDatastore) {
      this.clusterView = clusterView;
      this.hashRingDatastore = hashRingDatastore;
      
      hostACL = DEFAULT_DATASTORE_ACL;
      
      LOG.info("By default, the distributed datastore component is configured to "
              + "start in allow-all mode meaning that any host can access, store "
              + "and delete cached objects. Please configure this component if "
              + "you wish to restrict access. This message may be ignored if you "
              + "have already configured this component.");
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      final Map<Class, UpdateListener<?>> listeners = new HashMap<Class, UpdateListener<?>>();
      listeners.put(SystemModel.class, new SystemModelUpdateListener());
      listeners.put(DistributedDatastoreConfiguration.class, new DistributedDatastoreConfigurationListener());

      return listeners;
   }

   protected void updateClusterMembers(SystemModel configuration) {
      try {
         final List<InetSocketAddress> cacheSiblings = new LinkedList<InetSocketAddress>();
         
         ReposeCluster domain = new SystemModelInterrogator(clusterView.getListenPorts()).getLocalServiceDomain(configuration);

         for (Node node : domain.getNodes().getNode()) {
            if (domain.getFilters() != null) {
               for (Filter f : domain.getFilters().getFilter()) {
                  if (f.getName().equals("dist-datastore")) {
                     
                     // TODO Model: Support https or other protocols
                     final InetAddress hostAddress = InetAddress.getByName(node.getHostname());
                     final InetSocketAddress hostSocketAddress = new InetSocketAddress(hostAddress, node.getHttpPort());

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

   private class DistributedDatastoreConfigurationListener implements UpdateListener<DistributedDatastoreConfiguration> {

      @Override
      public void configurationUpdated(DistributedDatastoreConfiguration configurationObject) {
         if (configurationObject.getAllowedHosts() != null) {
            final List<InetAddress> newHostList = new LinkedList<InetAddress>();
            
            for (HostAccessControl host : configurationObject.getAllowedHosts().getAllow()) {
               try {
                  final InetAddress hostAddress = InetAddress.getByName(host.getHost());
                  newHostList.add(hostAddress);
               } catch(UnknownHostException uhe) {
                  LOG.error("Unable to resolve name: " + host.getHost() + " - Ignoring this host. Reason: " + uhe.getMessage(), uhe);
               }
            }

            final boolean allowAll = configurationObject.getAllowedHosts().isAllowAll();
            
            if (allowAll) {
               LOG.info("The distributed datastore component is configured in allow-all mode meaning that any host can access, store and delete cached objects.");
            } else {
               LOG.info("The distributed datastore component has access controls configured meaning that only the configured hosts can access, store and delete cached objects.");
            }

            hostACL = new DatastoreAccessControl(newHostList, allowAll);
         }
      }
   }

   private class SystemModelUpdateListener implements UpdateListener<SystemModel> {

      @Override
      public void configurationUpdated(SystemModel configurationObject) {
         if (configurationObject == null) {
            LOG.error("Power Proxy configuration was null - please check your configurations and error logs");
            return;
         }

         updateClusterMembers(configurationObject);
      }
   }

   @Override
   protected DatastoreFilterLogicHandler buildHandler() {
      return new DatastoreFilterLogicHandler(UUIDEncodingProvider.getInstance(), hashRingDatastore, hostACL);
   }
}
