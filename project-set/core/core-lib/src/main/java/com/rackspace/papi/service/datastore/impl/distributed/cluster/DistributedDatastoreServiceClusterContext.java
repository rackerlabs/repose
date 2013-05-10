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
import com.rackspace.papi.service.datastore.impl.distributed.DatastoreAccessControl;
import com.rackspace.papi.service.datastore.impl.distributed.cluster.utils.AccessListDeterminator;
import com.rackspace.papi.service.datastore.impl.distributed.cluster.utils.ClusterMemberDeterminator;
import com.rackspace.papi.service.datastore.impl.distributed.config.DistributedDatastoreConfiguration;
import com.rackspace.papi.service.datastore.impl.distributed.config.HostAccessControl;
import com.rackspace.papi.service.datastore.impl.distributed.config.Port;
import java.net.Inet4Address;
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
           @Qualifier("serviceRegistry") ServiceRegistry registry) {
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
            if (curDistributedDatastoreConfiguration != null) {
               isInitialized = true;

               if (systemModelUpdateListener.isInitialized()) {
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
            if (curSystemModel != null) {
               isInitialized = true;

               if (distributedDatastoreConfigurationListener.isInitialized()) {
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
   public void updateCluster() {

      updateClusterMembers();
      updateAccessList();

   }

   protected void updateClusterMembers() {


      List<InetSocketAddress> cacheSiblings = ClusterMemberDeterminator.getClusterMembers(curSystemModel, curDistributedDatastoreConfiguration, reposeInstanceInfo.getClusterId());
      service.updateClusterView(cacheSiblings);
   }

   private void updateAccessList() {

      synchronized (configLock) {
         List<InetAddress> clusterMembers = new LinkedList<InetAddress>();

         if (curSystemModel != null) {
            clusterMembers = AccessListDeterminator.getClusterMembers(curSystemModel, reposeInstanceInfo.getClusterId());
         }

         hostACL = AccessListDeterminator.getAccessList(curDistributedDatastoreConfiguration, clusterMembers);
         
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
      configurationManager.subscribeTo("system-model.cfg.xml", xsdURL, systemModelUpdateListener, SystemModel.class);

      URL dXsdURL = getClass().getResource("/META-INF/schema/system-model/dist-datastore-configuration.xsd");
      configurationManager.subscribeTo("dist-datastore.cfg.xml", dXsdURL, distributedDatastoreConfigurationListener, DistributedDatastoreConfiguration.class);

      sce.getServletContext().setAttribute("ddClusterViewService", service);
      register();
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      if (configurationManager != null) {
         configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelUpdateListener);
         configurationManager.unsubscribeFrom("dist-datastore.cfg.xml", distributedDatastoreConfigurationListener);
      }
   }
}
