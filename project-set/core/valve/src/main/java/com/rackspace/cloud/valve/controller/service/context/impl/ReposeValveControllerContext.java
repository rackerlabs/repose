package com.rackspace.cloud.valve.controller.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.cloud.valve.controller.service.ControllerService;
import com.rackspace.papi.commons.util.net.NetUtilities;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContextEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("reposeValveControllerContext")
public class ReposeValveControllerContext implements ServiceContext<ControllerService> {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeValveControllerContext.class);
   protected SystemModel systemModel;
   private static final String SERVICE_NAME = "powerapi:/services/controller";
   private final ControllerService controllerService;
   private final ConfigurationService configurationManager;
   private final ServiceRegistry registry;
   private final SystemModelConfigurationListener systemModelConfigurationListener;
   private String configDir;
   private String connectionFramework;
   private Set<String> curNodes = new HashSet<String>();
   private boolean initialized = false;

   @Autowired
   public ReposeValveControllerContext(
           @Qualifier("controllerService") ControllerService controllerService,
           @Qualifier("serviceRegistry") ServiceRegistry registry,
           @Qualifier("configurationManager") ConfigurationService configurationManager) {
      this.configurationManager = configurationManager;
      this.registry = registry;
      this.controllerService = controllerService;
      this.systemModelConfigurationListener = new SystemModelConfigurationListener();
   }

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public ControllerService getService() {
      return this.controllerService;
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      this.configDir = sce.getServletContext().getInitParameter("powerapi-config-directory");
      this.connectionFramework = sce.getServletContext().getInitParameter("connection-framework");
      controllerService.setConfigDirectory(configDir);
      URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
      configurationManager.subscribeTo("system-model.cfg.xml", xsdURL, systemModelConfigurationListener, SystemModel.class);


   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelConfigurationListener);
   }

   private class SystemModelConfigurationListener implements UpdateListener<SystemModel> {

      @Override
      public void configurationUpdated(SystemModel configurationObject) {
         
         curNodes = controllerService.getManagedInstances();
         
         if (StringUtilities.isBlank(controllerService.getConfigDirectory())) {
            controllerService.setConfigDirectory(configDir);
         }

         if (StringUtilities.isBlank(controllerService.getConnectionFramework())) {
            controllerService.setConnectionFramework(connectionFramework);
         }

         systemModel = configurationObject;

         Map<String, Node> updatedSystem = getLocalReposeInstances(systemModel);
         
         
         controllerService.updateManagedInstances(getNodesToStart(updatedSystem), getNodesToShutdown(updatedSystem));
         
         checkDeployment();
         initialized = true;
      }

      @Override
      public boolean isInitialized() {
         return initialized;
      }
   }

   private Map<String, Node> getLocalReposeInstances(SystemModel systemModel){
      
      Map<String, Node> updatedSystem = new HashMap<String, Node>();

         for (ReposeCluster cluster : systemModel.getReposeCluster()) {
            for (Node node : cluster.getNodes().getNode()) {
               if (NetUtilities.isLocalHost(node.getHostname())) {
                  updatedSystem.put(cluster.getId() + node.getId(), node);
               }
            }
         }
         
         return updatedSystem;
   }
   
   private List<String> getNodesToShutdown(Map<String, Node> nodes) {

      List<String> shutDownNodes = new LinkedList<String>();

      for (String key : curNodes) {
         if (!nodes.containsKey(key)) {
            shutDownNodes.add(key);
         }
      }

      return shutDownNodes;
   }

   private Map<String, Node> getNodesToStart(Map<String, Node> newModel) {
      Map<String, Node> startUps = new HashMap<String, Node>();

      for (String key : newModel.keySet()) {
         if (!curNodes.contains(key)) {
            startUps.put(key, newModel.get(key));
         }
      }

      return startUps;
   }
   
   private void checkDeployment(){
      
      //no repose instances
      if(controllerService.getManagedInstances().isEmpty()){
         LOG.warn("No Repose Instances started. Waiting for suitable update");
      }
      
      
   }
}
