package com.rackspace.papi.service.datastore.impl.distributed.jetty;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.DistributedDatastoreLauncherService;
import com.rackspace.papi.service.datastore.impl.distributed.config.DistributedDatastoreConfiguration;
import com.rackspace.papi.service.datastore.impl.distributed.config.Port;
import com.rackspace.papi.service.routing.RoutingService;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component("distributedDatastoreLauncher")
public class DistributedDatastoreLauncherServiceImpl implements DistributedDatastoreLauncherService {

   private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreLauncherServiceImpl.class);
   private DistributedDatastoreJettyServerBuilder builder;
   private ConfigurationService configurationManager;
   private DistributedDatastoreConfiguration distributedDatastoreConfiguration;
   private ReposeInstanceInfo instanceInfo;
   private DistributedDatastoreConfigurationListener distributedDatastoreConfigurationListener;
   private int datastorePort;
   private final Object configLock = new Object();
   private DatastoreService datastoreService;
   private Server server;

   @Override
   public void startDistributedDatastoreServlet() {
      server = builder.newServer(datastoreService, instanceInfo);
      try {
         LOG.info("Launching Datastore servlet on port: " + datastorePort);
         server.start();
         server.setStopAtShutdown(true);
      } catch (Exception e) {
         LOG.error("Unable to start Distributed Datastore Jetty Instance: " + e.getMessage(), e);
         if (server != null) {
            try {
               server.stop();
            } catch (Exception ex) {
               LOG.error("Error stopping server", ex);
            }
         }
      }
   }

   @Override
   public void stopDistributedDatastoreServlet() {
      LOG.info("Stopping Distributed Datastore listener at port " + datastorePort);

      if (server != null && server.isStarted()) {
          try {
              server.stop();
          } catch (Exception ex) {
              LOG.error("Unable to stop Distributed Datastore listener at port " + datastorePort, ex);
          }
      }
   }

   @Override
   public void destroy() {
      if(configurationManager != null){
         configurationManager.unsubscribeFrom("dist-datastore.cfg.xml", distributedDatastoreConfigurationListener);
      }
      stopDistributedDatastoreServlet();
   }
  

   @Override
   public void initialize(ConfigurationService configurationService, ReposeInstanceInfo instanceInfo, DatastoreService datastoreService,
           ServicePorts servicePorts, RoutingService routingService, String configDirectory) {

      this.configurationManager = configurationService;
      this.instanceInfo = instanceInfo;
      distributedDatastoreConfigurationListener = new DistributedDatastoreConfigurationListener();
      URL xsdURL = getClass().getResource("/META-INF/schema/config/dist-datastore-configuration.xsd");
      configurationManager.subscribeTo("", "dist-datastore.cfg.xml", xsdURL, distributedDatastoreConfigurationListener, DistributedDatastoreConfiguration.class);
      this.datastoreService = datastoreService;
      builder = new DistributedDatastoreJettyServerBuilder(datastorePort, instanceInfo, configDirectory);


   }

   private class DistributedDatastoreConfigurationListener implements UpdateListener<DistributedDatastoreConfiguration> {

      private boolean initialized = false;

      @Override
      public void configurationUpdated(DistributedDatastoreConfiguration configurationObject) {
         synchronized (configLock) {
            distributedDatastoreConfiguration = configurationObject;
         }

         //updateAccessList();
         datastorePort = determinePort();
         initialized = true;
      }

      @Override
      public boolean isInitialized() {
         return initialized;
      }

      private int determinePort() {
         int port = getDefaultPort();
         for (Port curPort : distributedDatastoreConfiguration.getPortConfig().getPort()) {
            if (curPort.getCluster().equalsIgnoreCase(instanceInfo.getClusterId())
                    && StringUtilities.nullSafeEqualsIgnoreCase(curPort.getNode(), instanceInfo.getNodeId())) {
               port = curPort.getPort();
               break;
            }
         }
         return port;
      }

      private int getDefaultPort() {
         int port = -1;

         for (Port curPort : distributedDatastoreConfiguration.getPortConfig().getPort()) {
            if (curPort.getCluster().equalsIgnoreCase(instanceInfo.getClusterId()) && StringUtilities.nullSafeEqualsIgnoreCase(curPort.getNode(), "-1")) {
               port = curPort.getPort();
            }
         }

         return port;
      }
   }
}
