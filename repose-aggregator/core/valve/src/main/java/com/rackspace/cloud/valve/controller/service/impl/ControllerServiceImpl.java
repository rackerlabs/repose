package com.rackspace.cloud.valve.controller.service.impl;

import com.rackspace.cloud.valve.controller.service.ControllerService;
import com.rackspace.cloud.valve.jetty.ValveJettyServerBuilder;
import com.rackspace.papi.service.config.ConfigurationResourceException;
import org.openrepose.core.service.config.manager.UpdateListener;
import com.rackspace.papi.service.config.parser.ConfigurationParserFactory;
import com.rackspace.papi.service.config.parser.jaxb.JaxbConfigurationParser;
import com.rackspace.papi.service.config.resource.impl.BufferedURLConfigurationResource;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.net.NetUtilities;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.SslConfiguration;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import org.openrepose.core.service.config.ConfigurationService;
import com.rackspace.papi.servlet.InitParameter;
import org.eclipse.jetty.server.Server;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

import javax.inject.Inject;
import javax.inject.Named;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

@Named
public class ControllerServiceImpl implements ControllerService, ServletContextAware {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ControllerServiceImpl.class);
    private static final String REPOSE_NODE = "Repose node ";

    private ServletContext servletContext;
    private final ConfigurationService configurationService;
    private final SystemModelConfigurationListener systemModelConfigurationListener = new SystemModelConfigurationListener();
    private final ContainerConfigurationListener containerConfigurationListener = new ContainerConfigurationListener();

    private String configDir;
    private boolean isInsecure;
    private SystemModel systemModel;
    private boolean initialized = false;
    private Set<String> curNodes = new HashSet<>();
    private Map<String, Server> managedServers = new ConcurrentHashMap<>(); //TODO: Find a better way than using a ConcurrentHashMap for this

    @Inject
    public ControllerServiceImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }


    @PostConstruct
    public void afterPropertiesSet() {
        this.configDir = servletContext.getInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName());
        this.isInsecure = Boolean.parseBoolean(servletContext.getInitParameter(InitParameter.INSECURE.getParameterName()));
        setConfigDirectory(configDir);
        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        URL containerXsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");
        configurationService.subscribeTo("container.cfg.xml", containerXsdURL, containerConfigurationListener, ContainerConfiguration.class);
        configurationService.subscribeTo("system-model.cfg.xml", xsdURL, systemModelConfigurationListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelConfigurationListener);
        Set<String> instances = getManagedInstances();
        updateManagedInstances(null, instances);
        curNodes.clear();
    }

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

    @Override
    public Boolean reposeInstancesInitialized() {
        return managedServers.isEmpty();
    }

    @Override
    public void setConfigDirectory(String directory) {
        this.configDir = directory;
    }

    @Override
    public String getConfigDirectory() {
        return this.configDir;
    }

    @Override
    public void setIsInsecure(boolean isInsecure) {
        this.isInsecure = isInsecure;
    }

    @Override
    public Boolean isInsecure() {
        return isInsecure;
    }

    private void startValveServers(Map<String, ExtractorResult<Node>> updatedInstances) {
        final Set<Entry<String, ExtractorResult<Node>>> entrySet = updatedInstances.entrySet();

        for (Entry<String, ExtractorResult<Node>> entry : entrySet) {
            Node curNode = entry.getValue().getKey();

            //TODO: I think this is the first place ports are gotted
            //But they're done for each one.... wtf is an ExtractorResult
            List<Port> ports = getNodePorts(curNode);

            Server serverInstance = new ValveJettyServerBuilder(configDir, ports, validateSsl(curNode), isInsecure, entry.getValue().getResult(), curNode.getId()).newServer();
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

    private List<Port> getNodePorts(Node node) {
        List<Port> ports = new LinkedList<>();

        if (node.getHttpPort() != 0) {
            ports.add(new Port("Http", node.getHttpPort()));
        }
        if (node.getHttpsPort() != 0) {
            ports.add(new Port("Https", node.getHttpsPort()));
        }

        return ports;
    }

    private SslConfiguration validateSsl(Node node) {
        SslConfiguration sslConfiguration = null;

        if (node.getHttpsPort() != 0) {
            try {
                sslConfiguration = readSslConfiguration(configDir);
            } catch (MalformedURLException e) {
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

    private void logReposeLaunch(List<Port> ports) {
        for (Port port : ports) {
            LOG.info("Repose node listening on " + port.getProtocol() + " on port " + port.getPort());
        }
    }

    private Map<String, ExtractorResult<Node>> getLocalReposeInstances(SystemModel systemModel) {
        Map<String, ExtractorResult<Node>> updatedSystem = new HashMap<>();

        for (ReposeCluster cluster : systemModel.getReposeCluster()) {
            for (Node node : cluster.getNodes().getNode()) {
                if (NetUtilities.isLocalHost(node.getHostname())) {
                    updatedSystem.put(cluster.getId() + node.getId() + node.getHostname() + node.getHttpPort() + node.getHttpsPort(), new ExtractorResult<>(cluster.getId(), node));
                }
            }
        }

        return updatedSystem;
    }

    private Set<String> getNodesToShutdown(Map<String, ExtractorResult<Node>> nodes) {
        Set<String> shutDownNodes = new HashSet<>();

        for (String key : curNodes) {
            if (!nodes.containsKey(key)) {
                shutDownNodes.add(key);
            }
        }

        return shutDownNodes;
    }

    private Map<String, ExtractorResult<Node>> getNodesToStart(Map<String, ExtractorResult<Node>> newModel) {
        Map<String, ExtractorResult<Node>> startUps = new HashMap<>();

        Set<Entry<String, ExtractorResult<Node>>> entrySet = newModel.entrySet();
        for (Entry<String, ExtractorResult<Node>> entry : entrySet) {
            if (!curNodes.contains(entry.getKey())) {
                startUps.put(entry.getKey(), entry.getValue());
            }
        }

        return startUps;
    }

    private void checkDeployment() {
        if (getManagedInstances().isEmpty()) {
            LOG.warn("No Repose Instances started. Waiting for suitable update");
        }
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
                curNodes = getManagedInstances();

                if (StringUtilities.isBlank(getConfigDirectory())) {
                    setConfigDirectory(configDir);
                }

                setIsInsecure(isInsecure);

                //TODO: this is where the port information is collected the first time
                //GET LOCAL REPOSE INSTANCES
                // This is triggered off the system model. Might have to make this into something generic so that all things can read
                // the ports using a class
                Map<String, ExtractorResult<Node>> updatedSystem = getLocalReposeInstances(systemModel);
                updateManagedInstances(getNodesToStart(updatedSystem), getNodesToShutdown(updatedSystem));

                checkDeployment();
                initialized = true;
            }
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }
}
