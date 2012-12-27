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
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("reposeValveControllerContext")
public class ReposeValveControllerContext implements ServiceContext<ControllerService> {

   protected SystemModel systemModel;
   private static final String SERVICE_NAME = "powerapi:/services/controller";
   private final ControllerService controllerService;
   private final ConfigurationService configurationManager;
   private final ServiceRegistry registry;
   private final SystemModelConfigurationListener systemModelConfigurationListener;
   private String configDir;
   private Set<String> curNodes  = new HashSet<String>();

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

         if (StringUtilities.isBlank(controllerService.getConfigDirectory())) {
            controllerService.setConfigDirectory(configDir);
         }

         systemModel = configurationObject;

         Map<String, Node> updatedSystem = new HashMap<String, Node>();
         
         for(ReposeCluster cluster: systemModel.getReposeCluster()){
            for(Node node: cluster.getNodes().getNode()){
               updatedSystem.put(cluster.getId()+node.getId(), node);
            }
         }
         //TODO: Determine which nodes in the system model are supposed to run in this server.
         
         curNodes = controllerService.getManagedInstances();
         controllerService.updateManagedInstances(getNodesToStart(updatedSystem),getNodesToShutdown(updatedSystem));
      }
   }
   
   //TODO: Method to determine which nodes in the system-model are to be launched on this server
   
   //TODO: Retrieve list of Nodes to be shutdown
   private List<String> getNodesToShutdown(Map<String,Node> nodes){
      
      List<String> shutDownNodes = new LinkedList<String>();
      
      for(String key: curNodes){
         if(!nodes.containsKey(key)){
            shutDownNodes.add(key);
         }
      }
      
      return shutDownNodes;
   }
   
   //TODO: Retrieve list of nodes to be started
   private Map<String, Node> getNodesToStart(Map<String, Node> newModel){
      Map<String, Node>  startUps = new HashMap<String,Node>();
      
      for(String key: newModel.keySet()){
         if(!curNodes.contains(key)){
            startUps.put(key, newModel.get(key));
         }
      }
      
      return startUps;
   }
}
