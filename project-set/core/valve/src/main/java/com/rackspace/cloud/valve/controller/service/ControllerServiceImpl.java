package com.rackspace.cloud.valve.controller.service;

import com.rackspace.cloud.valve.jetty.ValveJettyServerBuilder;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Node;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jetty.server.Server;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("controllerService")
public class ControllerServiceImpl implements ControllerService {

   private Map<String, Server> managedServers = new HashMap<String, Server>();
   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ControllerServiceImpl.class);
   private String configDir;
   private String connectionFramework;

   @Override
   public Set<String> getManagedInstances() {
      return managedServers.keySet();

   }

   @Override
   public synchronized void updateManagedInstances(Map<String, Node> updatedInstances, List<String> nodesToStop) {

      LOG.info("Here are the new nodes");

      //Stopping Servers
      for (String key : nodesToStop) {
         Server serverInstance = managedServers.get(key);
         try {
            serverInstance.stop();
            managedServers.remove(key);
         } catch (Exception e) {
            LOG.error("Unable to shutdown server: " + key);
         }
      }


      for (String key : updatedInstances.keySet()) {
         Node curNode = updatedInstances.get(key);
         List<Port> ports = new LinkedList<Port>();

         if (curNode.getHttpPort() != 0) {
            ports.add(new Port("Http", curNode.getHttpPort()));
         }
         if (curNode.getHttpsPort() != 0) {
            ports.add(new Port("Https", curNode.getHttpsPort()));
         }
         Server serverInstance = new ValveJettyServerBuilder(configDir, ports, null, "", true).newServer();
         try {
            serverInstance.start();
         } catch (Exception e) {
            LOG.error("Repose Node with Id " + curNode.getId() + " could not be started: " + e.getMessage(), e);
            if (serverInstance != null) {
               try {
                  serverInstance.stop();
               } catch (Exception ex) {
                  LOG.error("Error stopping server", ex);
               }
            }
         }
         managedServers.put(key, serverInstance);
      }

   }

   @Override
   public Boolean reposeInstancesInitialized() {
      return managedServers.isEmpty();
   }

   @Override
   public void setConfigDirectory(String directory) {
      this.configDir = directory;
   }
   
   @Override
   public void setConnectionFramework(String framework){
      this.connectionFramework = framework;
   }

   @Override
   public String getConfigDirectory() {
      return this.configDir;
   }
   
   @Override
   public String getConnectionFramework(){
      return this.connectionFramework;
   }
}
