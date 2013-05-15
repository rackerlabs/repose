package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.components.datastore.hash.HashRingDatastore;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.*;
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
   private final DatastoreAccessControl defaultDatastoreACL = new DatastoreAccessControl(Collections.EMPTY_LIST, false);
   private final MutableClusterView clusterView;
   private final HashRingDatastore hashRingDatastore;
   private DatastoreAccessControl hostACL;
   private ReposeInstanceInfo instanceInfo;
   private SystemModel curSystemModel;
   private DistributedDatastoreConfiguration curDistributedDatastoreConfiguration;
   private final Object configLock = new Object();

   public DatastoreFilterLogicHandlerFactory(MutableClusterView clusterView, HashRingDatastore hashRingDatastore, ReposeInstanceInfo instanceInfo) {
      this.clusterView = clusterView;
      this.hashRingDatastore = hashRingDatastore;
      this.instanceInfo = instanceInfo;

      hostACL = defaultDatastoreACL;

      LOG.info("By default, the distributed datastore component is configured to"
              + " start in restricted mode. Meaning that only members of the current"
              + " Repose nodes cluster will be able to access, store, and delete cached objects."
              + " Please configure this component if you wish to grant access to other hosts. This "
              + "message may be ignored if you have already configured this component");
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      final Map<Class, UpdateListener<?>> listeners = new HashMap<Class, UpdateListener<?>>();
      listeners.put(SystemModel.class, new SystemModelUpdateListener());
      listeners.put(DistributedDatastoreConfiguration.class, new DistributedDatastoreConfigurationListener());

      return listeners;
   }

   //TODO: Move this logic to a utility class. Maybe new SystemModel Interrogator?
   private ReposeCluster getCurrentCluster(List<ReposeCluster> clusters, String clusterId) {

      for (ReposeCluster cluster : clusters) {

         if (StringUtilities.nullSafeEquals(clusterId, cluster.getId())) {
            return cluster;
         }
      }

      return null;
   }

   protected void updateClusterMembers(SystemModel configuration) {// this config will have all the info from the system model cfg
      try {
         final List<InetSocketAddress> cacheSiblings = new LinkedList<InetSocketAddress>();

         ReposeCluster cluster = getCurrentCluster(configuration.getReposeCluster(), instanceInfo.getClusterId());

         //Adding all members of the current Repose Cluster to clusterView
         if (cluster != null) {

             //Make sure the DD service and filter are not running at the same time.
             checkForDDFilterAndService(cluster);

             for (Node node : cluster.getNodes().getNode()) {

               final InetAddress hostAddress = InetAddress.getByName(node.getHostname());
               final InetSocketAddress hostSocketAddress = new InetSocketAddress(hostAddress, node.getHttpPort());
               cacheSiblings.add(hostSocketAddress);
            }
         }

         clusterView.updateMembers(cacheSiblings.toArray(new InetSocketAddress[cacheSiblings.size()]));
      } catch (UnknownHostException uhe) {
         LOG.error(uhe.getMessage(), uhe);
      }
   }

    private void checkForDDFilterAndService(ReposeCluster cluster) {

        //Check for both dd filter and dd service.
        Boolean ddFilterPresent = false;
        Boolean ddServicePresent = false;

        if (cluster.getFilters() != null && cluster.getFilters().getFilter() != null) {
            for (Filter filter : cluster.getFilters().getFilter()) {
                if (filter.getName() != null && filter.getName().equals("dist-datastore")) {
                    ddFilterPresent = true;
                }
            }
        }

        if (cluster.getServices() != null && cluster.getServices().getService() != null) {
            for (Service service : cluster.getServices().getService()) {
                if (service.getName() != null && service.getName().equals("distributed-datastore")) {
                    ddServicePresent = true;
                }
            }
        }

        //If both are present throw a clear error.
        if (ddFilterPresent == true && ddServicePresent == true) {
            throw new IllegalArgumentException(
                    "The distributed datastore filter and service can not be used at the same time, " +
                            "within the same cluster. Please check your configuration.");
        }
    }

    private class DistributedDatastoreConfigurationListener implements UpdateListener<DistributedDatastoreConfiguration> {

      private boolean isInitialized = false;

      @Override
      public void configurationUpdated(DistributedDatastoreConfiguration configurationObject) {

         synchronized (configLock) {
            curDistributedDatastoreConfiguration = configurationObject;
         }
         updateAccessList();

         isInitialized = true;
      }

      @Override
      public boolean isInitialized() {
         return isInitialized;
      }
   }

   private class SystemModelUpdateListener implements UpdateListener<SystemModel> {

      private boolean isInitialized = false;

      @Override
      public void configurationUpdated(SystemModel configurationObject) {

         synchronized (configLock) {
            curSystemModel = configurationObject;
         }
         if (curSystemModel == null) {
            LOG.error("Power Proxy configuration was null - please check your configurations and error logs");
            return;
         }

         updateClusterMembers(configurationObject);
         updateAccessList();
         isInitialized = true;
      }

      @Override
      public boolean isInitialized() {
         return isInitialized;
      }
   }

   @Override
   protected DatastoreFilterLogicHandler buildHandler() {

      if (!this.isInitialized()) {
         return null;
      }
      synchronized (configLock) {
         return new DatastoreFilterLogicHandler(UUIDEncodingProvider.getInstance(), hashRingDatastore, hostACL);
      }
   }

   private List<InetAddress> getClusterMembers() {

      ReposeCluster cluster = getCurrentCluster(curSystemModel.getReposeCluster(), instanceInfo.getClusterId());
      final List<InetAddress> reposeClusterMembers = new LinkedList<InetAddress>();

      for (Node node : cluster.getNodes().getNode()) {
         try {
            final InetAddress hostAddress = InetAddress.getByName(node.getHostname());
            reposeClusterMembers.add(hostAddress);
         } catch (UnknownHostException e) {
            LOG.warn("Unable to resolve host: " + node.getHostname() + "for Node " + node.getId() + " in Repose Cluster " + instanceInfo.getClusterId());
         }

      }

      return reposeClusterMembers;
   }

   private List<InetAddress> getConfiguredAllowedHosts() {

      final List<InetAddress> configuredAllowedHosts = new LinkedList<InetAddress>();

      for (HostAccessControl host : curDistributedDatastoreConfiguration.getAllowedHosts().getAllow()) {
         try {
            final InetAddress hostAddress = InetAddress.getByName(host.getHost());
            configuredAllowedHosts.add(hostAddress);
         } catch (UnknownHostException e) {
            LOG.warn("Unable to resolve host: " + host.getHost());
         }
      }

      return configuredAllowedHosts;
   }

   private void updateAccessList() {

      synchronized (configLock) {
         List<InetAddress> hostAccessList = new LinkedList<InetAddress>();
         boolean allowAll = false;
         if (curSystemModel != null) {
            hostAccessList.addAll(getClusterMembers());
         }
         if (curDistributedDatastoreConfiguration != null) {

            allowAll = curDistributedDatastoreConfiguration.getAllowedHosts().isAllowAll();
            hostAccessList.addAll(getConfiguredAllowedHosts());
         }

         if (allowAll) {
            LOG.info("The distributed datastore component is configured in allow-all mode meaning that any host can access, store and delete cached objects.");
         } else {
            LOG.info("The distributed datastore component has access controls configured meaning that only the configured hosts and cluster members "
                    + "can access, store and delete cached objects.");
         }
         LOG.debug("Allowed Hosts: " + hostAccessList.toString());
         hostACL = new DatastoreAccessControl(hostAccessList, allowAll);
      }
   }
}
