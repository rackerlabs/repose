package com.rackspace.papi.service.datastore.distributed.impl.distributed.jetty;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.Service;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.DistributedDatastoreLauncherService;
import com.rackspace.papi.service.datastore.distributed.config.DistributedDatastoreConfiguration;
import com.rackspace.papi.service.datastore.distributed.config.Port;
import com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster.DistributedDatastoreServiceClusterViewService;
import com.rackspace.papi.service.datastore.distributed.impl.distributed.servlet.DistributedDatastoreServletContextManager;
import com.rackspace.papi.service.healthcheck.*;
import com.rackspace.papi.service.routing.RoutingService;
import com.rackspace.papi.servlet.InitParameter;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.context.ServletContextAware;

import javax.inject.Named;
import javax.servlet.ServletContext;

import java.net.URL;

@Named
public class DistributedDatastoreLauncherServiceImpl implements DistributedDatastoreLauncherService, ServletContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreLauncherServiceImpl.class);
    private RoutingService routingService;
    private ServicePorts servicePorts;
    private SystemModelConfigurationListener systemModelConfigurationListener;
    private DistributedDatastoreJettyServerBuilder builder;
    private ConfigurationService configurationManager;
    private DistributedDatastoreConfiguration distributedDatastoreConfiguration;
    private ReposeInstanceInfo instanceInfo;
    private DistributedDatastoreConfigurationListener distributedDatastoreConfigurationListener;
    private int datastorePort;
    private final Object configLock = new Object();
    private DatastoreService datastoreService;
    private Server server;
    private String issueId, healthServiceUID;
    private DistributedDatastoreServletContextManager manager;
    private HealthCheckService healthCheckService;
    private String configDirectory;
    private boolean initialized = false;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private RequestProxyService requestProxyService;
    private DistributedDatastoreServiceClusterViewService clusterViewService;


    @Inject
    public DistributedDatastoreLauncherServiceImpl(DistributedDatastoreServletContextManager manager,
                                                   HealthCheckService healthCheckService,
                                                   ConfigurationService configurationManager,
                                                   @Qualifier("servicePorts") ServicePorts servicePorts,
                                                   RoutingService routingService,
                                                   DatastoreService datastoreService,
                                                   ReposeInstanceInfo instanceInfo,
                                                   RequestProxyService requestProxyService,
                                                   DistributedDatastoreServiceClusterViewService clusterViewService) {
        this.instanceInfo = instanceInfo;
        this.manager = manager;
        this.healthCheckService = healthCheckService;
        this.configurationManager = configurationManager;
        this.servicePorts = servicePorts;
        this.routingService = routingService;
        this.datastoreService = datastoreService;
        this.systemModelConfigurationListener = new SystemModelConfigurationListener();
        this.requestProxyService = requestProxyService;
        this.clusterViewService = clusterViewService;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        final String configProp = InitParameter.POWER_API_CONFIG_DIR.getParameterName();
        configDirectory = System.getProperty(configProp, servletContext.getInitParameter(configProp));
    }

    @PostConstruct
    public void afterPropertiesSet() {
        healthCheckServiceProxy = healthCheckService.register();
        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationManager.subscribeTo("system-model.cfg.xml", xsdURL, systemModelConfigurationListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        healthCheckServiceProxy.deregister();
        configurationManager.unsubscribeFrom("dist-datastore.cfg.xml", distributedDatastoreConfigurationListener);
        stopDistributedDatastoreServlet();
        configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelConfigurationListener);
    }

    @Override
    public void startDistributedDatastoreServlet() {
        server = builder.newServer(datastoreService, instanceInfo, clusterViewService.getAccessControl(), clusterViewService.getClusterView());
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
    public void initialize(ConfigurationService configurationService, ReposeInstanceInfo instanceInfo,
                           ServicePorts servicePorts, RoutingService routingService, String configDirectory) {

        this.configurationManager = configurationService;
        this.instanceInfo = instanceInfo;
        issueId = "disdatastore-config-issue";
        distributedDatastoreConfigurationListener = new DistributedDatastoreConfigurationListener();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/dist-datastore-configuration.xsd");
        configurationManager.subscribeTo("", "dist-datastore.cfg.xml", xsdURL, distributedDatastoreConfigurationListener, DistributedDatastoreConfiguration.class);
        builder = new DistributedDatastoreJettyServerBuilder(datastorePort, instanceInfo, configDirectory, manager, requestProxyService);


    }

    private class DistributedDatastoreConfigurationListener implements UpdateListener<DistributedDatastoreConfiguration> {

        private boolean initialized = false;

        @Override
        public void configurationUpdated(DistributedDatastoreConfiguration configurationObject) {
            synchronized (configLock) {
                distributedDatastoreConfiguration = configurationObject;
            }

            try {
                datastorePort = determinePort();
                initialized = true;
                if (!healthCheckServiceProxy.getReportIds().isEmpty()) {
                    healthCheckServiceProxy.resolveIssue(issueId);
                }
            } catch (Exception ex) {
                LOG.trace("Exception caught on an updated configuration", ex);
                reportError(ex.getMessage());
            }
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

        private void reportError(String message) {
            healthCheckServiceProxy.reportIssue(issueId,"Dist-Datastore Configuration Issue:" + message, Severity.BROKEN);
        }
    }

    private class SystemModelConfigurationListener implements UpdateListener<SystemModel> {

        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            ReposeCluster cluster = findCluster(configurationObject);

            boolean listed = serviceListed(cluster);
            if (listed && initialized == false) {
                //launch dist-datastore servlet!!! Pass down the datastore service
                initialize(configurationManager, instanceInfo, servicePorts, routingService, configDirectory);
                startDistributedDatastoreServlet();
                //TODO: this should tell the ServletContextManager ?
                //TODO: or maybe there shouldn't be a DistributedDatastoreServletContextManager
                initialized = true;
            } else if (!listed && initialized) { // case when someone has turned off an existing datastore
                stopDistributedDatastoreServlet();
            }
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        private ReposeCluster findCluster(SystemModel sysModel) {
            for (ReposeCluster cls : sysModel.getReposeCluster()) {
                if (cls.getId().equals(instanceInfo.getClusterId())) {
                    return cls;
                }

            }
            return null;
        }

        private boolean serviceListed(ReposeCluster cluster) {
            if (cluster.getServices() != null) {
                for (Service service : cluster.getServices().getService()) {
                    if (service.getName().equalsIgnoreCase("dist-datastore")) {
                        //launch dist-datastore servlet!!! Pass down the datastore service
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
