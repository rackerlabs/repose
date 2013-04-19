package com.rackspace.papi.service.datastore.impl.distributed.cluster;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.cluster.ThreadSafeClusterView;
import com.rackspace.repose.service.distributeddatastore.DatastoreAccessControl;
import com.rackspace.repose.service.distributeddatastore.config.DistributedDatastoreConfiguration;
import com.rackspace.repose.service.distributeddatastore.config.HostAccessControl;
import com.rackspace.repose.service.distributeddatastore.config.Port;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletContextEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("distributedDatastoreServiceClusterContext")
public class DistributedDatastoreServiceClusterContext implements ServiceContext<DistributedDatastoreServiceClusterViewService> {

   private DistributedDatastoreServiceClusterViewService service;
   public static final String SERVICE_NAME = "distributedDatastoreClusterView";
   private final Object configLock = new Object();
   private DistributedDatastoreConfiguration curDistributedDatastoreConfiguration;
   private SystemModel curSystemModel;
   private DistributedDatastoreConfigurationListener distributedDatastoreConfigurationListener;
   private SystemModelUpdateListener systemModelUpdateListener;
   private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreServiceClusterContext.class);
   private DatastoreAccessControl hostACL;
   private ConfigurationService configurationManager;
   private MutableClusterView clusterView;
   private ReposeInstanceInfo reposeInstanceInfo;
   private ServiceRegistry registry;
   
   @Autowired
   public DistributedDatastoreServiceClusterContext(@Qualifier("configurationManager") ConfigurationService configurationManager,
   @Qualifier("clusterViewService") DistributedDatastoreServiceClusterViewService service,
   @Qualifier("reposeInstanceInfo") ReposeInstanceInfo reposeInstanceInfo,
   @Qualifier("serviceRegistry") ServiceRegistry registry){
      this.configurationManager = configurationManager;
      this.service = service;
      this.reposeInstanceInfo = reposeInstanceInfo;
      this.registry = registry;
   }
   
   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public DistributedDatastoreServiceClusterViewService getService() {
      return service;
   }
   
   public void register() {
      if (registry != null) {
         registry.addService(this);
      }
   }
   
   private class DistributedDatastoreConfigurationListener implements UpdateListener<DistributedDatastoreConfiguration> {

      private boolean isInitialized = false;

      @Override
      public void configurationUpdated(DistributedDatastoreConfiguration configurationObject) {

         synchronized (configLock) {
            curDistributedDatastoreConfiguration = configurationObject;
            if(curDistributedDatastoreConfiguration != null){
               isInitialized = true;
               
               if(systemModelUpdateListener.isInitialized()){
                  updateCluster();
               }
            }
         }
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
            if(curSystemModel != null){
               isInitialized = true;
               
               if(distributedDatastoreConfigurationListener.isInitialized()){
                  updateCluster();
               }
            }
         }
      }

      @Override
      public boolean isInitialized() {
         return isInitialized;
      }
   }
   
   /*
    * updates the hashring cluster view and the host access list
    */
   public void updateCluster(){
      
      updateClusterMembers();
      updateAccessList();
      
   }
   
   protected void updateClusterMembers() {
      try {
         final List<InetSocketAddress> cacheSiblings = new LinkedList<InetSocketAddress>();

         ReposeCluster cluster = getCurrentCluster(curSystemModel.getReposeCluster(), reposeInstanceInfo.getClusterId());

         //Adding all members of the current Repose Cluster to clusterView
         if (cluster != null) {
            for (Node node : cluster.getNodes().getNode()) {

               final InetAddress hostAddress = InetAddress.getByName(node.getHostname());
               final int port = getNodeDDPort(cluster.getId(), node.getId());
               final InetSocketAddress hostSocketAddress = new InetSocketAddress(hostAddress, port);
               cacheSiblings.add(hostSocketAddress);
            }
         }

         service.updateClusterView(cacheSiblings);
      } catch (UnknownHostException uhe) {
         LOG.error(uhe.getMessage(), uhe);
      }
   }
   
   private int getNodeDDPort(String clusterId, String nodeId){
      
      int port = -1;
         for (Port curPort : curDistributedDatastoreConfiguration.getPortConfig().getPort()) {
            if (curPort.getCluster().equalsIgnoreCase(clusterId)) {
               port = curPort.getPort();
               if (curPort.getNode().equalsIgnoreCase(nodeId)) {
                  break;
               }
            }
         }
         return port;
   }
   
   private List<InetAddress> getClusterMembers() {

      ReposeCluster cluster = getCurrentCluster(curSystemModel.getReposeCluster(), reposeInstanceInfo.getClusterId());
      final List<InetAddress> reposeClusterMembers = new LinkedList<InetAddress>();

      for (Node node : cluster.getNodes().getNode()) {
         try {
            final InetAddress hostAddress = InetAddress.getByName(node.getHostname());
            reposeClusterMembers.add(hostAddress);
         } catch (UnknownHostException e) {
            LOG.warn("Unable to resolve host: " + node.getHostname() + "for Node " + node.getId() + " in Repose Cluster " + reposeInstanceInfo.getClusterId());
         }

      }

      return reposeClusterMembers;
   }
   
   private ReposeCluster getCurrentCluster(List<ReposeCluster> clusters, String clusterId) {

      for (ReposeCluster cluster : clusters) {

         if (StringUtilities.nullSafeEquals(clusterId, cluster.getId())) {
            return cluster;
         }
      }

      return null;
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
         hostACL =new DatastoreAccessControl(hostAccessList, allowAll);
         service.updateAccessList(hostACL);
      }
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      
      hostACL = new DatastoreAccessControl(Collections.EMPTY_LIST, false);
      String ddPort = sce.getServletContext().getInitParameter("datastoreServicePort");
      ServicePorts servicePorts = new ServicePorts();
      servicePorts.add(new com.rackspace.papi.domain.Port("http", Integer.parseInt(ddPort)));
      clusterView = new ThreadSafeClusterView(servicePorts);
      service.initialize(clusterView, hostACL);
      systemModelUpdateListener = new SystemModelUpdateListener();
      distributedDatastoreConfigurationListener = new DistributedDatastoreConfigurationListener();
      URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
      configurationManager.subscribeTo("system-model.cfg.xml",xsdURL, systemModelUpdateListener, SystemModel.class);
      
      URL dXsdURL = getClass().getResource("/META-INF/schema/system-model/dist-datastore-configuration.xsd");
      configurationManager.subscribeTo("dist-datastore.cfg.xml",dXsdURL, distributedDatastoreConfigurationListener, DistributedDatastoreConfiguration.class);
      
      sce.getServletContext().setAttribute("ddClusterViewService", service);
      register();
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      if(configurationManager != null){
         configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelUpdateListener);
         configurationManager.unsubscribeFrom("dist-datastore.cfg.xml", distributedDatastoreConfigurationListener);
      }
   }
   
}
