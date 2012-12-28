package com.rackspace.cloud.valve.controller.service;

import com.rackspace.cloud.valve.jetty.ValveJettyServerBuilder;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Node;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jetty.server.Server;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("controllerService")
public class ControllerServiceImpl implements ControllerService {

   //TODO: Find a better way than using a ConcurrentHashMap for this.
   private Map<String, Server> managedServers = new ConcurrentHashMap<String, Server>();
   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ControllerServiceImpl.class);
   private String configDir;
   private String connectionFramework;

   @Override
   public Set<String> getManagedInstances() {
      return managedServers.keySet();

   }

   @Override
   public synchronized void updateManagedInstances(Map<String, Node> updatedInstances, Set<String> nodesToStop) {

      LOG.info("Here are the new nodes");


      if (!nodesToStop.isEmpty()) {
         stopServers(nodesToStop);
      }
      if (updatedInstances != null && !updatedInstances.isEmpty()) {
         startValveServers(updatedInstances);
      }
   }

   private void startValveServers(Map<String, Node> updatedInstances) {

      final Set<Entry<String, Node>> entrySet = updatedInstances.entrySet();

      for (Entry<String, Node> entry : entrySet) {

         Node curNode = entry.getValue();

         List<Port> ports = getNodePorts(curNode);


         Server serverInstance = new ValveJettyServerBuilder(configDir, ports, null, "", true).newServer();
         try {
            serverInstance.start();
            serverInstance.setStopAtShutdown(true);
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
         managedServers.put(entry.getKey(), serverInstance);
      }
   }

   private void stopServers(Set<String> nodesToStop) {

      for (String key : nodesToStop) {
         Server serverInstance = managedServers.get(key);
         try {
            serverInstance.stop();
            managedServers.remove(key);
         } catch (Exception e) {
            LOG.error("Unable to shutdown server: " + key);
         }
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
   public void setConnectionFramework(String framework) {
      this.connectionFramework = framework;
   }

   @Override
   public String getConfigDirectory() {
      return this.configDir;
   }

   @Override
   public String getConnectionFramework() {
      return this.connectionFramework;
   }

   private List<Port> getNodePorts(Node node) {

      List<Port> ports = new LinkedList<Port>();

      if (node.getHttpPort() != 0) {
         ports.add(new Port("Http", node.getHttpPort()));
      }
      if (node.getHttpsPort() != 0) {
         ports.add(new Port("Https", node.getHttpsPort()));
      }

      return ports;
   }
}
