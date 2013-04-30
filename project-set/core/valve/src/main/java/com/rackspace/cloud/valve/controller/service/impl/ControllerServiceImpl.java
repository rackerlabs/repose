package com.rackspace.cloud.valve.controller.service.impl;

import com.rackspace.cloud.valve.controller.service.ControllerService;
import com.rackspace.cloud.valve.jetty.ValveJettyServerBuilder;
import com.rackspace.papi.commons.config.ConfigurationResourceException;
import com.rackspace.papi.commons.config.parser.ConfigurationParserFactory;
import com.rackspace.papi.commons.config.parser.jaxb.JaxbConfigurationParser;
import com.rackspace.papi.commons.config.resource.impl.BufferedURLConfigurationResource;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.SslConfiguration;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Node;
import org.eclipse.jetty.server.Server;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component("controllerService")
public class ControllerServiceImpl implements ControllerService {

   //TODO: Find a better way than using a ConcurrentHashMap for this.
   private Map<String, Server> managedServers = new ConcurrentHashMap<String, Server>();
   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ControllerServiceImpl.class);
    private static final String REPOSE_NODE = "Repose node ";
   private String configDir;
   private String connectionFramework;
   private boolean isInsecure;

   @Override
   public Set<String> getManagedInstances() {
      return managedServers.keySet();

   }

   @Override
   public synchronized void updateManagedInstances(Map<String, ExtractorResult<Node>> updatedInstances, Set<String> nodesToStop) {

      if (!nodesToStop.isEmpty()) {
         stopServers(nodesToStop);
      }
      if (updatedInstances != null && !updatedInstances.isEmpty()) {
         startValveServers(updatedInstances);
      }
   }

   private void startValveServers(Map<String, ExtractorResult<Node>> updatedInstances) {

      final Set<Entry<String, ExtractorResult<Node>>> entrySet = updatedInstances.entrySet();

      for (Entry<String, ExtractorResult<Node>> entry : entrySet) {

         Node curNode = entry.getValue().getKey();

         List<Port> ports = getNodePorts(curNode);



         Server serverInstance = new ValveJettyServerBuilder(configDir, ports, validateSsl(curNode), connectionFramework, isInsecure, entry.getValue().getResult(), curNode.getId()).newServer();
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
         logReposeLaunch(ports);
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
            LOG.error("Unable to shutdown server: " + key + ": " + e.getMessage(), e);
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

   @Override
   public void setIsInsecure(boolean isInsecure) {
      this.isInsecure = isInsecure;
   }

   @Override
   public Boolean isInsecure() {
      return isInsecure;
   }

   private SslConfiguration validateSsl(Node node) {
      SslConfiguration sslConfiguration = null;

      if (node.getHttpsPort() != 0) {

         try{
         sslConfiguration = readSslConfiguration(configDir);
         }catch(MalformedURLException e){
            LOG.error("Unable to build path to SSL configuration: " + e.getMessage(), e);
         }

         if (sslConfiguration == null) {
            throw new ConfigurationResourceException(REPOSE_NODE + node.getId() + " is configured to run on https but the ssl configuration is not in container.cfg.xml.");
         }

         if (sslConfiguration.getKeystoreFilename() == null) {
            throw new ConfigurationResourceException(REPOSE_NODE + node.getId() + " is configured to run on https but the ssl keystore filename is not in container.cfg.xml.");
         }

         if (sslConfiguration.getKeystorePassword() == null) {
            throw new ConfigurationResourceException(REPOSE_NODE + node.getId() + " is configured to run on https but the ssl keystore password is not in container.cfg.xml.");
         }

         if (sslConfiguration.getKeyPassword() == null) {
            throw new ConfigurationResourceException(REPOSE_NODE + node.getId() + " is configured to run on https but the ssl key password is not in container.cfg.xml.");
         }
      }

      return sslConfiguration;
   }

   private SslConfiguration readSslConfiguration(String cfgRoot) throws MalformedURLException {
      final URL configurationLocation = new URL("file://" + cfgRoot + File.separator + "container.cfg.xml");
      final JaxbConfigurationParser<ContainerConfiguration> containerConfigParser = ConfigurationParserFactory.getXmlConfigurationParser(ContainerConfiguration.class, null);
      final ContainerConfiguration cfg = containerConfigParser.read(new BufferedURLConfigurationResource(configurationLocation));

      if (cfg != null && cfg.getDeploymentConfig() != null) {
         return cfg.getDeploymentConfig().getSslConfiguration();
      }

      throw new ConfigurationResourceException("Container configuration is not valid. Please check your configuration.");
   }
   
   private void logReposeLaunch(List<Port> ports){
      
      for(Port port: ports){
         LOG.info("Repose node listening on " + port.getProtocol() + " on port " + port.getPort());
      }
      
   }
}
