package org.openrepose.core.valve.services.controller.impl;

import org.openrepose.core.valve.services.controller.ControllerService;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.net.NetUtilities;
import org.openrepose.commons.utils.regex.ExtractorResult;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ServiceContext;
import org.openrepose.core.servlet.InitParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ReposeValveControllerContext implements ServiceContext<ControllerService> {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeValveControllerContext.class);
    private SystemModel systemModel;
    private static final String SERVICE_NAME = "powerapi:/services/controller";
    private final ControllerService controllerService;
    private final ConfigurationService configurationManager;
    private final ServiceRegistry registry;
    private final SystemModelConfigurationListener systemModelConfigurationListener;
    private final ContainerConfigurationListener containerConfigurationListener;
    private String configDir;
    private boolean isInsecure;
    private Set<String> curNodes = new HashSet<String>();
    private boolean initialized = false;

    public ReposeValveControllerContext(
            ControllerService controllerService,
            ServiceRegistry registry,
            ConfigurationService configurationManager) {
        this.configurationManager = configurationManager;
        this.registry = registry;
        this.controllerService = controllerService;
        this.systemModelConfigurationListener = new SystemModelConfigurationListener();
        this.containerConfigurationListener = new ContainerConfigurationListener();
    }

    public void register() {
        if (registry != null) {
            registry.addService(this);
        }
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
        this.configDir = sce.getServletContext().getInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName());
        this.isInsecure = Boolean.parseBoolean(sce.getServletContext().getInitParameter(InitParameter.INSECURE.getParameterName()));
        controllerService.setConfigDirectory(configDir);
        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        URL containerXsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");
        configurationManager.subscribeTo("container.cfg.xml", containerXsdURL, containerConfigurationListener, ContainerConfiguration.class);
        configurationManager.subscribeTo("system-model.cfg.xml", xsdURL, systemModelConfigurationListener, SystemModel.class);
        register();

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelConfigurationListener);
        Set<String> instances = controllerService.getManagedInstances();
        controllerService.updateManagedInstances(null, instances);
        curNodes.clear();
    }

    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {

            if (configurationObject != null) {
                this.isInitialized = true;

                if (!systemModelConfigurationListener.isInitialized()) {
                    systemModelConfigurationListener.configurationUpdated(systemModel);
                }
            }
        }

        @Override
        public boolean isInitialized() {
            return this.isInitialized;
        }
    }

    private class SystemModelConfigurationListener implements UpdateListener<SystemModel> {

        @Override
        public void configurationUpdated(SystemModel configurationObject) {

            systemModel = configurationObject;

            if (containerConfigurationListener.isInitialized() && systemModel != null) {

                curNodes = controllerService.getManagedInstances();

                if (StringUtilities.isBlank(controllerService.getConfigDirectory())) {
                    controllerService.setConfigDirectory(configDir);
                }

                controllerService.setIsInsecure(isInsecure);


                Map<String, ExtractorResult<Node>> updatedSystem = getLocalReposeInstances(systemModel);


                controllerService.updateManagedInstances(getNodesToStart(updatedSystem), getNodesToShutdown(updatedSystem));

                checkDeployment();
                initialized = true;
            }
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }

    private Map<String, ExtractorResult<Node>> getLocalReposeInstances(SystemModel systemModel) {

        Map<String, ExtractorResult<Node>> updatedSystem = new HashMap<String, ExtractorResult<Node>>();

        for (ReposeCluster cluster : systemModel.getReposeCluster()) {
            for (Node node : cluster.getNodes().getNode()) {
                if (NetUtilities.isLocalHost(node.getHostname())) {
                    updatedSystem.put(cluster.getId() + node.getId() + node.getHostname() + node.getHttpPort() + node.getHttpsPort(), new ExtractorResult<Node>(cluster.getId(), node));
                }
            }
        }

        return updatedSystem;
    }

    private Set<String> getNodesToShutdown(Map<String, ExtractorResult<Node>> nodes) {

        Set<String> shutDownNodes = new HashSet<String>();

        for (String key : curNodes) {
            if (!nodes.containsKey(key)) {
                shutDownNodes.add(key);
            }
        }

        return shutDownNodes;
    }

    private Map<String, ExtractorResult<Node>> getNodesToStart(Map<String, ExtractorResult<Node>> newModel) {
        Map<String, ExtractorResult<Node>> startUps = new HashMap<String, ExtractorResult<Node>>();


        Set<Entry<String, ExtractorResult<Node>>> entrySet = newModel.entrySet();
        for (Entry<String, ExtractorResult<Node>> entry : entrySet) {
            if (!curNodes.contains(entry.getKey())) {
                startUps.put(entry.getKey(), entry.getValue());
            }
        }

        return startUps;
    }

    private void checkDeployment() {

        //no repose instances
        if (controllerService.getManagedInstances().isEmpty()) {
            LOG.warn("No Repose Instances started. Waiting for suitable update");
        }


    }
}
