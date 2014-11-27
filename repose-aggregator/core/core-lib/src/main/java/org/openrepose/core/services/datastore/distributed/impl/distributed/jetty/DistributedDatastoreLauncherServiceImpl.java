package org.openrepose.core.services.datastore.distributed.impl.distributed.jetty;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.Destroyable;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.proxy.RequestProxyService;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.datastore.distributed.impl.distributed.cluster.utils.AccessListDeterminator;
import org.openrepose.core.services.datastore.distributed.impl.distributed.cluster.utils.DistDatastoreClusterInterrogator;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.systemmodel.Service;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.services.datastore.DatastoreAccessControl;
import org.openrepose.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.DistributedDatastoreLauncherService;
import org.openrepose.core.services.datastore.distributed.config.DistributedDatastoreConfiguration;
import org.openrepose.core.services.datastore.distributed.config.Port;
import org.openrepose.services.datastore.distributed.ClusterView;
import org.openrepose.services.datastore.impl.distributed.ThreadSafeClusterView;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;
import org.openrepose.core.services.routing.RoutingService;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is where the dist datastore will be fired up if configured to
 * <p/>
 * This bean can be active all the time, it will only start up jetties and such if it detects itself in the <service></service>
 * section of the system model.
 * That same configuration update will be responsible for telling the jetties that are running what cluster members they have
 * <p/>
 * //TODO: I don't think this needs to implement an interface, it won't really have public methods
 * it's a launcher magic thing that should be fired up and stay running when spring turns on.
 * <p/>
 * TODO: this guy requires a NODE ID and a CLUSTER ID, we will have to make another spring context
 * There needs to be a core services context that runs on *all* nodes, and might need to be aware of the cluster (maybe?)
 * <p/>
 * Then there needs to be a different services context that can start up per-node, potentially multiple on a local node
 * So that means this guy will potentially be multiples of, but only within the external service (spring) context realm
 */
@Named
public class DistributedDatastoreLauncherServiceImpl implements Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreLauncherServiceImpl.class);

    public static final String SYSTEM_MODEL_XSD = "/META-INF/schema/system-model/system-model.xsd";
    public static final String DATASTORE_XSD = "/META-INF/schema/config/dist-datastore-configuration.xsd";

    public static final String CLUSTER_VIEW_SERVICE_NAME = "distributedDatastoreClusterView";
    public static final String DATASTORE_CONFIG = "dist-datastore.cfg.xml";
    public static final String SYSTEM_MODEL_CONFIG = "system-model.cfg.xml";
    private static final String DATASTORE_CONFIG_ERROR_REPORT = "DistDatastoreConfigError";
    private static final String SYSTEM_MODEL_CONFIG_ERROR_REPORT = "SystemModelConfigError";
    private final String DD_CONFIG_ISSUE_ID = "dist-datastore-config-issue";

    //TODO: I have way too many atomic References in here. We need to refactor this more
    private AtomicReference<DistributedDatastoreConfiguration> currentDDConfig = new AtomicReference<>();
    private AtomicReference<SystemModel> currentSystemModel = new AtomicReference<>();
    private AtomicReference<Integer> datastorePort = new AtomicReference<>();
    private AtomicReference<Server> jettyServer = new AtomicReference<>();
    private AtomicReference<ClusterView> clusterView = new AtomicReference<>();
    private AtomicReference<DatastoreAccessControl> datastoreAccessControl = new AtomicReference<>();

    private final RequestProxyService requestProxyService;
    private final DatastoreService datastoreService;
    private final String clusterId;
    private final String nodeId;
    private final ConfigurationService configurationService;
    private final HealthCheckServiceProxy healthCheckServiceProxy;

    private DDConfigListener ddConfigListener = new DDConfigListener();
    private SystemModelConfigListener systemModelConfigListener = new SystemModelConfigListener();


    @Inject
    public DistributedDatastoreLauncherServiceImpl(
            @Value(ReposeSpringProperties.CLUSTER_ID) String clusterId,
            @Value(ReposeSpringProperties.NODE_ID) String nodeId,
            ConfigurationService configurationService,
            RequestProxyService requestProxyService,
            DatastoreService datastoreService,
            RoutingService routingService, //TODO: why?
            HealthCheckService healthCheckService) {

        //TODO: something needs to keep track of what members of the cluster it's got, and it needs to be touchable from threads

        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.configurationService = configurationService;
        this.requestProxyService = requestProxyService;
        this.datastoreService = datastoreService;
        this.healthCheckServiceProxy = healthCheckService.register();
    }

    @PostConstruct
    public void init() {
        //Subscribe to the system model, if the service is configured, we'll need to start up more things
        URL systemModelXSD = getClass().getResource(SYSTEM_MODEL_XSD);
        configurationService.subscribeTo("", SYSTEM_MODEL_CONFIG, systemModelXSD, systemModelConfigListener, SystemModel.class);

        //Also subscribe to the dist-datastore configuration to pull up information on it
        URL datastoreXSD = getClass().getResource(DATASTORE_XSD);
        this.configurationService.subscribeTo("", DATASTORE_CONFIG, datastoreXSD, ddConfigListener, DistributedDatastoreConfiguration.class);


    }

    public void startDistributedDatastoreServlet() {
        if (jettyServer.get() == null && datastorePort.get() != null) { //Have to also have a datastorePort
            //TODO: need to assume that we've got a datastorePort now
            LOG.info("Starting Distributed Datastore Jetty Instance at port {}", datastorePort.get());

            DistributedDatastoreJettyServerBuilder builder = new DistributedDatastoreJettyServerBuilder(
                    datastorePort.get(),
                    clusterId,
                    datastoreService,
                    clusterView.get(),
                    datastoreAccessControl.get(),
                    requestProxyService);

            Server server = builder.newServer();
            try {
                server.start();
                server.setStopAtShutdown(true);

                jettyServer.set(server);
            } catch (Exception e) {
                LOG.error("Unable to start Distributed Datastore Jetty Instance at port {}", datastorePort.get(), e);
                try {
                    server.stop();
                } catch (Exception e1) {
                    LOG.error("Error stopping Distributed Datastore Jetty Instance after a failure", e1);
                }
            }
        }
    }

    public void stopDistributedDatastoreServlet() {
        if (jettyServer.get() != null && jettyServer.get().isStarted()) {
            LOG.info("Stopping Distributed Datastore Jetty Instance at port " + datastorePort.get());
            try {
                jettyServer.getAndSet(null).stop();
            } catch (Exception e) {
                LOG.error("Unable to stop Distributed Datastore Jetty Instance at port {} ", datastorePort.get(), e);
            }
        }
    }

    @PreDestroy
    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(DATASTORE_CONFIG, ddConfigListener);
        configurationService.unsubscribeFrom(SYSTEM_MODEL_CONFIG, systemModelConfigListener);

        //Shut this puppy down!
        stopDistributedDatastoreServlet();
    }

    private void triggerConfigUpdate() {
        if (currentSystemModel.get() != null && currentDDConfig.get() != null) {
            SystemModel systemModel = currentSystemModel.get();
            DistributedDatastoreConfiguration ddConfig = currentDDConfig.get();

            try {
                datastorePort.set(determinePort(ddConfig));

                if (!healthCheckServiceProxy.getReportIds().isEmpty()) {
                    healthCheckServiceProxy.resolveIssue(DD_CONFIG_ISSUE_ID);
                }

                //I have two configs, lets rock
                ReposeCluster cluster = findCluster(systemModel); //Get our cluster out of the system model

                boolean listed = serviceListed(cluster); //If we have a dd service for this cluster, and it's not running fire it up
                if (!(listed && jettyServer.get() == null)) { //TODO: maybe don't check this

                    //Create the ClusterView for this DD instance
                    setUpClusterView();

                    //There is no server, and we're listed in the system-model
                    startDistributedDatastoreServlet();
                    //In here we can update the cluster view, because the "startDDServlet" method can be hit multiple times
                    //Need to update the ClusterView for this guy

                    updateClusterView(); //This should also update the ACL
                    updateAccessList();
                }
                if (!listed && jettyServer.get() != null) {
                    //It's not listed any more, and we have a jetty server, so we need to stop things
                    stopDistributedDatastoreServlet();
                }

            } catch (Exception ex) {
                LOG.trace("Exception caught on an updated configuration", ex);
                healthCheckServiceProxy.reportIssue(DD_CONFIG_ISSUE_ID, "Dist-Datastore Configuration Issue:" + ex.getMessage(), Severity.BROKEN);
            }

        }
    }

    private void updateAccessList() {
        List<InetAddress> clusterMembers = new LinkedList<InetAddress>(); //This will never be null, because we'll always have a system model by now

        clusterMembers = AccessListDeterminator.getClusterMembers(currentSystemModel.get(), clusterId);

        DatastoreAccessControl hostACL = AccessListDeterminator.getAccessList(currentDDConfig.get(), clusterMembers);

        datastoreAccessControl.set(hostACL);
    }


    //TODO: this needs to go into the ClusterView so it can handle it's own mess
    private void updateClusterView() {
        List<InetSocketAddress> cacheSiblings = DistDatastoreClusterInterrogator.getClusterMembers(currentSystemModel.get(),
                currentDDConfig.get(),
                clusterId);

        clusterView.get().updateMembers(cacheSiblings);
    }

    //TODO: this needs to go away more
    private ClusterView setUpClusterView() {
        if (clusterView.get() == null) {
            List<Integer> ddPortList = new ArrayList<>();
            ddPortList.add(datastorePort.get());
            clusterView.set(new ThreadSafeClusterView(ddPortList));
        }
        return clusterView.get();
    }

    private class SystemModelConfigListener implements UpdateListener<SystemModel> {

        private boolean initialized = false;

        //When a servlet is running for a cluster, and the configuration for the system model changed, update it's cluster members
        //Should be a whole lot less complicated than before
        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            currentSystemModel.set(configurationObject);
            triggerConfigUpdate();
            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }


    private class DDConfigListener implements UpdateListener<DistributedDatastoreConfiguration> {
        private boolean initialized = false;

        @Override
        public void configurationUpdated(DistributedDatastoreConfiguration configurationObject) {
            currentDDConfig.set(configurationObject);
            triggerConfigUpdate();
            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

    }

    //Stupid jaxb TODO: replace with SystemModelInterrogator
    private ReposeCluster findCluster(SystemModel sysModel) {
        for (ReposeCluster cls : sysModel.getReposeCluster()) {
            if (cls.getId().equals(clusterId)) {
                return cls;
            }

        }
        return null;
    }

    //Stupid jaxb TODO: add to the SystemModelInterrogator
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

    /**
     * This is a utility method to find the port out of the jaxb object
     * TODO: move to the DDconfigInterrogator
     *
     * @param config
     * @return
     */
    private int determinePort(DistributedDatastoreConfiguration config) {
        int port = getDefaultPort(config);
        for (Port curPort : config.getPortConfig().getPort()) {
            if (curPort.getCluster().equalsIgnoreCase(clusterId)
                    && StringUtilities.nullSafeEqualsIgnoreCase(curPort.getNode(), nodeId)) { //TODO: MUST HAVE NODEID
                port = curPort.getPort();
                break;
            }
        }
        return port;
    }

    /**
     * A convienience method to get the default port if one isn't given (not sure why)
     * TODO: move to the DDConfigInterrogator
     *
     * @param config
     * @return
     */
    private int getDefaultPort(DistributedDatastoreConfiguration config) {
        int port = -1;

        for (Port curPort : config.getPortConfig().getPort()) {
            if (curPort.getCluster().equalsIgnoreCase(clusterId) && StringUtilities.nullSafeEqualsIgnoreCase(curPort.getNode(), "-1")) {
                port = curPort.getPort();
            }
        }

        return port;
    }


}
