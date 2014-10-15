package org.openrepose.core.services.datastore.distributed.impl.distributed.jetty;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.domain.ReposeInstanceInfo;
import org.openrepose.core.domain.ServicePorts;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.DistributedDatastoreLauncherService;
import org.openrepose.core.services.datastore.distributed.config.DistributedDatastoreConfiguration;
import org.openrepose.core.services.datastore.distributed.config.Port;
import org.openrepose.core.services.datastore.distributed.impl.distributed.servlet.DistributedDatastoreServletContextManager;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;
import org.openrepose.core.services.routing.RoutingService;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component("distributedDatastoreLauncher")
public class DistributedDatastoreLauncherServiceImpl implements DistributedDatastoreLauncherService {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreLauncherServiceImpl.class);

    private final Object configLock = new Object();

    private int datastorePort;
    private Server server;
    private ReposeInstanceInfo instanceInfo;
    private DatastoreService datastoreService;
    private ConfigurationService configurationManager;
    private DistributedDatastoreJettyServerBuilder builder;
    private DistributedDatastoreServletContextManager manager;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private DistributedDatastoreConfiguration distributedDatastoreConfiguration;
    private DistributedDatastoreConfigurationListener distributedDatastoreConfigurationListener;

    @Autowired
    public DistributedDatastoreLauncherServiceImpl(DistributedDatastoreServletContextManager manager,
                                                   HealthCheckService healthCheckService) {
        this.manager = manager;
        this.healthCheckServiceProxy = healthCheckService.register();
    }

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
        if (configurationManager != null) {
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
        builder = new DistributedDatastoreJettyServerBuilder(datastorePort, instanceInfo, configDirectory, manager);
    }

    private class DistributedDatastoreConfigurationListener implements UpdateListener<DistributedDatastoreConfiguration> {
        private final String issueId = "dist-datastore-config-issue";

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
                healthCheckServiceProxy.reportIssue(issueId, "Dist-Datastore Configuration Issue:" + ex.getMessage(), Severity.BROKEN);
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
    }
}
