package org.openrepose.nodeservice.distributed.jetty;

import com.google.common.base.Optional;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.encoding.UUIDEncodingProvider;
import org.openrepose.core.services.RequestProxyService;
import org.openrepose.core.filter.SystemModelInterrogator;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.datastore.distributed.config.DistributedDatastoreConfiguration;
import org.openrepose.nodeservice.distributed.cluster.utils.AccessListDeterminator;
import org.openrepose.nodeservice.distributed.cluster.utils.ClusterMemberDeterminator;
import org.openrepose.nodeservice.distributed.servlet.DistributedDatastoreServlet;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.core.services.datastore.DatastoreAccessControl;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.distributed.ClusterConfiguration;
import org.openrepose.core.services.datastore.impl.distributed.ThreadSafeClusterView;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

@Named
public class DistributedDatastoreLauncherService {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreLauncherService.class);

    private final String clusterId;
    private final String nodeId;
    private final ConfigurationService configurationService;
    private final RequestProxyService requestProxyService;
    private final SystemModelListener systemModelListener = new SystemModelListener();

    private volatile boolean isRunning = false;
    private final Object heartbeatLock = new Object();

    private final DatastoreService datastoreService;
    private Optional<DistributedDatastoreServer> ddServer = Optional.absent();
    private DistributedDatastoreServlet ddServlet = null;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private DistributedDatastoreConfigurationListener ddConfigListener;

    private final AtomicReference<SystemModel> currentSystemModel = new AtomicReference<>();
    private final AtomicReference<DistributedDatastoreConfiguration> currentDDConfig = new AtomicReference<>();

    private final String DD_PORT_CONFIG_ISSUE = "dist-datastore-config-issue"; //ONLY triggered if it couldn't find the port


    @Inject
    public DistributedDatastoreLauncherService(
            @Value(ReposeSpringProperties.NODE.CLUSTER_ID) String clusterId,
            @Value(ReposeSpringProperties.NODE.NODE_ID) String nodeId,
            DatastoreService datastoreService,
            ConfigurationService configurationService,
            HealthCheckService healthCheckService,
            RequestProxyService requestProxyService) {

        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.datastoreService = datastoreService;
        this.configurationService = configurationService;
        this.requestProxyService = requestProxyService;
        this.healthCheckServiceProxy = healthCheckService.register();
    }

    private void startDistributedDatastore() {
        isRunning = true; //Note that we're alive now

        //Start listening to the dd config, so we can update our service with more stuff
        ddConfigListener = new DistributedDatastoreConfigurationListener();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/dist-datastore-configuration.xsd");
        this.configurationService.subscribeTo("", "dist-datastore.cfg.xml",
                xsdURL,
                ddConfigListener,
                DistributedDatastoreConfiguration.class);

        //We cannot start the server up completely until we've got a port, which comes from the config

        //Nothing happens here, because we have to wait on a configuration Pulse to have the necessary data to start
        //up a dist datastore.
    }

    private void stopDistributedDatastore() {
        isRunning = false;

        configurationService.unsubscribeFrom("dist-datastore.cfg.xml", ddConfigListener);

        synchronized (heartbeatLock) {
            if (ddServer.isPresent()) {
                DistributedDatastoreServer server = ddServer.get();
                LOG.info("Stopping Distributed Datastore listener at port {} ", server.getPort());

                try {
                    server.stop();
                } catch (Exception e) {
                    LOG.error("Unable to stop Distributed Datastore listener at port {}", server.getPort(), e);
                }
                ddServer = Optional.absent();
            }
        }
    }

    private void configurationHeartbeat() {
        synchronized (heartbeatLock) {
            if (currentDDConfig.get() != null && currentSystemModel.get() != null && isRunning) {
                DistributedDatastoreConfiguration ddConfig = currentDDConfig.get();
                SystemModel systemModel = currentSystemModel.get();

                //We have configuration options!
                //extract data and do things with it?
                int ddPort = ClusterMemberDeterminator.getNodeDDPort(ddConfig, clusterId, nodeId);
                if (ddPort == -1) {
                    LOG.error("Distributed datastore port is not defined, fell back to -1");
                    healthCheckServiceProxy.reportIssue(DD_PORT_CONFIG_ISSUE, "Dist-Datastore Configuration Issue: ddPort not defined", Severity.BROKEN);
                    return;
                }
                //Make sure this time through that if there was an issue, it's resolved
                healthCheckServiceProxy.resolveIssue(DD_PORT_CONFIG_ISSUE);

                //If there's no server running, fire one up, because we should have a server
                if (!ddServer.isPresent()) {
                    ClusterConfiguration configuration = new ClusterConfiguration(requestProxyService,
                            UUIDEncodingProvider.getInstance(),
                            ThreadSafeClusterView.singlePortClusterView(ddPort));

                    //ddServlet provides a way to get a hold of the ClusterView now and the ACL, like it should
                    ddServlet = new DistributedDatastoreServlet(datastoreService,
                            configuration,
                            new DatastoreAccessControl(Collections.EMPTY_LIST, false));

                    DistributedDatastoreServer server = new DistributedDatastoreServer(clusterId, nodeId, ddServlet);
                    this.ddServer = Optional.of(server);

                    //Make sure the server is running now -- the dist datastore is up
                    try {
                        LOG.info("Starting Distributed Datastore listener on port {} ", ddPort);
                        server.runServer(ddPort);
                    } catch (Exception e) {
                        LOG.error("Unable to start Distributed Datastore Server instance on {}", ddPort, e);
                    }
                }

                //Have a port, now determine if we have a server running on that port.
                //If we have a port that's different than the other port, restart it on the new port
                if (ddServer.isPresent()) {
                    try {
                        ddServer.get().runServer(ddPort);
                    } catch (Exception e) {
                        LOG.error("Failure ensuring Distributed Datastore on port {}", ddPort, e);
                    }

                    //Update the Servlet ClusterView and ACL
                    ddServlet.getClusterView().updateMembers(
                            ClusterMemberDeterminator.getClusterMembers(systemModel, ddConfig, clusterId)
                    );
                    ddServlet.updateAcl(
                            AccessListDeterminator.getAccessList(ddConfig, AccessListDeterminator.getClusterMembers(systemModel, clusterId))
                    );
                }
            }
        }
    }

    @PreDestroy
    public void destroy() {
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelListener);

        stopDistributedDatastore();
    }

    @PostConstruct
    public void initialize() {
        //Subscribe to the system model to know if we even want to turn on...
        // (If we do want to turn on, we should probably only then subscribe to the ddconfig)
        URL systemModelXSD = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationService.subscribeTo("system-model.cfg.xml", systemModelXSD, systemModelListener, SystemModel.class);
        //If and only if we're going to be turned on, should we subscribe to the other one
    }

    private class DistributedDatastoreConfigurationListener implements UpdateListener<DistributedDatastoreConfiguration> {
        private boolean initialized = false;

        @Override
        public void configurationUpdated(DistributedDatastoreConfiguration configurationObject) {
            currentDDConfig.set(configurationObject);
            initialized = true;
            configurationHeartbeat();
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }

    private class SystemModelListener implements UpdateListener<SystemModel> {

        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            SystemModelInterrogator smi = new SystemModelInterrogator(clusterId, nodeId);
            Optional<ReposeCluster> clusterOption = smi.getLocalCluster(configurationObject);
            if (clusterOption.isPresent()) {

                boolean listed = smi.getServiceForCluster(configurationObject, "dist-datastore").isPresent();

                if (listed && !isRunning) {
                    startDistributedDatastore();
                } else if (!listed && isRunning) {
                    stopDistributedDatastore();
                }
            }

            currentSystemModel.set(configurationObject);
            configurationHeartbeat();
        }

        @Override
        public boolean isInitialized() {
            return isRunning;
        }
    }
}
