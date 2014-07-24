package com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster;

import org.openrepose.core.service.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.components.datastore.distributed.ClusterView;
import com.rackspace.papi.components.datastore.impl.distributed.ThreadSafeClusterView;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.model.SystemModel;
import org.openrepose.core.service.config.ConfigurationService;
import com.rackspace.papi.service.datastore.DatastoreAccessControl;
import com.rackspace.papi.service.datastore.distributed.config.DistributedDatastoreConfiguration;
import com.rackspace.papi.service.datastore.distributed.config.Port;
import com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster.utils.AccessListDeterminator;
import com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster.utils.ClusterMemberDeterminator;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy;
import com.rackspace.papi.service.healthcheck.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Named
public class DistributedDatastoreServiceClusterViewServiceImpl implements DistributedDatastoreServiceClusterViewService,
        ServletContextAware {
    public static final String DEFAULT_CONFIG = "dist-datastore.cfg.xml";

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreServiceClusterViewServiceImpl.class);
    private static final String datastoreConfigHealthReport = "DistDatastoreConfigError";
    private static final String systemModelConfigHealthReport = "SystemModelConfigError";

    private final Object configLock = new Object();
    private final SystemModelUpdateListener systemModelUpdateListener = new SystemModelUpdateListener();
    private final DistributedDatastoreConfigurationListener distributedDatastoreConfigurationListener = new DistributedDatastoreConfigurationListener();

    private ClusterView clusterView;
    private SystemModel curSystemModel;
    private ServletContext servletContext;
    private DatastoreAccessControl accessControl;
    private ReposeInstanceInfo reposeInstanceInfo;
    private ConfigurationService configurationManager;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private DistributedDatastoreConfiguration curDistributedDatastoreConfiguration;
    private HealthCheckService healthCheckService;

    @Inject
    public DistributedDatastoreServiceClusterViewServiceImpl(ConfigurationService configurationManager,
                                                             ReposeInstanceInfo reposeInstanceInfo,
                                                             HealthCheckService healthCheckService) {
        this.configurationManager = configurationManager;
        this.reposeInstanceInfo = reposeInstanceInfo;
        this.healthCheckService = healthCheckService;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }


    @PostConstruct
    public void afterPropertiesSet() {
        this.healthCheckServiceProxy = healthCheckService.register();

        //Doing the monitoring for config should always happen
        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationManager.subscribeTo("system-model.cfg.xml", xsdURL, systemModelUpdateListener, SystemModel.class);
        URL dXsdURL = getClass().getResource("/META-INF/schema/config/dist-datastore-configuration.xsd");
        configurationManager.subscribeTo(DEFAULT_CONFIG, dXsdURL, distributedDatastoreConfigurationListener, DistributedDatastoreConfiguration.class);

    }

    @PreDestroy
    public void destroy() {
        healthCheckServiceProxy.deregister();
        if (configurationManager != null) {
            configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelUpdateListener);
            configurationManager.unsubscribeFrom(DEFAULT_CONFIG, distributedDatastoreConfigurationListener);
        }
    }


    /**
     * Called when the cluster view service is turned on, not when the spring bean is created.
     * TODO: call this when the cluster is turned on for the first time?
     */
    public void initialize(int ddPort) {
        LOG.info("ONE TIME initialization of Distributed Datastore Cluster View");

        //Setting Initial Broken state.
        healthCheckServiceProxy.reportIssue(datastoreConfigHealthReport, "Dist Datastore Configuration Error", Severity.BROKEN);
        healthCheckServiceProxy.reportIssue(systemModelConfigHealthReport, "System Model Configuration Error", Severity.BROKEN);
        accessControl = new DatastoreAccessControl(Collections.EMPTY_LIST, false);

        List<Integer> servicePorts = new ArrayList<Integer>();
        servicePorts.add(ddPort);
        clusterView = new ThreadSafeClusterView(servicePorts);

        //First time run should also update the cluster?
        updateCluster();

        //After something has been done, try to resolve issues...
        try {
            if (!distributedDatastoreConfigurationListener.isInitialized() && !configurationManager.getResourceResolver().resolve(DEFAULT_CONFIG).exists()) {
                healthCheckServiceProxy.resolveIssue(datastoreConfigHealthReport);
                healthCheckServiceProxy.resolveIssue(systemModelConfigHealthReport);
            }
        } catch (IOException e) {
            LOG.error("Unable to search for {}", DEFAULT_CONFIG, e);
        }
        //Adding it to the servlet context?
        servletContext.setAttribute("ddClusterViewService", this);

    }


    @Override
    public void updateClusterView(List<InetSocketAddress> cacheSiblings) {
        clusterView.updateMembers(cacheSiblings.toArray(new InetSocketAddress[cacheSiblings.size()]));
    }

    @Override
    public void updateAccessList(DatastoreAccessControl accessControl) {
        this.accessControl = accessControl;
    }

    @Override
    public ClusterView getClusterView() {
        return clusterView;
    }

    @Override
    public DatastoreAccessControl getAccessControl() {
        return accessControl;
    }

    /*
     * updates the hashring cluster view and the host access list
     */
    private void updateCluster() {
        updateClusterMembers();
        updateAccessList();
    }

    private void updateClusterMembers() {
        List<InetSocketAddress> cacheSiblings = ClusterMemberDeterminator.getClusterMembers(curSystemModel, curDistributedDatastoreConfiguration, reposeInstanceInfo.getClusterId());
        updateClusterView(cacheSiblings);
    }

    private void updateAccessList() {
        synchronized (configLock) {
            List<InetAddress> clusterMembers = new LinkedList<InetAddress>();

            if (curSystemModel != null) {
                clusterMembers = AccessListDeterminator.getClusterMembers(curSystemModel, reposeInstanceInfo.getClusterId());
            }

            accessControl = AccessListDeterminator.getAccessList(curDistributedDatastoreConfiguration, clusterMembers);

            updateAccessList(accessControl);
        }
    }

    private class DistributedDatastoreConfigurationListener implements UpdateListener<DistributedDatastoreConfiguration> {
        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(DistributedDatastoreConfiguration configurationObject) {
            synchronized (configLock) {
                curDistributedDatastoreConfiguration = configurationObject;
                if (curDistributedDatastoreConfiguration != null) {
                    if(!isInitialized) {
                        //I think this will trigger only the first time run
                        //Since nothing sets initialized to false...
                        initialize(determinePort());
                    }
                    isInitialized = true;

                    if (systemModelUpdateListener.isInitialized()) {
                        updateCluster();

                    }
                }
            }
            // After successful config update the error report will be removed
            healthCheckServiceProxy.resolveIssue(datastoreConfigHealthReport);
        }

        private int determinePort() {
            int port = getDefaultPort();
            for (Port curPort : curDistributedDatastoreConfiguration.getPortConfig().getPort()) {
                if (curPort.getCluster().equalsIgnoreCase(reposeInstanceInfo.getClusterId())
                        && StringUtilities.nullSafeEqualsIgnoreCase(curPort.getNode(), reposeInstanceInfo.getNodeId())) {
                    port = curPort.getPort();
                    break;
                }
            }
            return port;
        }

        private int getDefaultPort() {
            int port = -1;

            for (Port curPort : curDistributedDatastoreConfiguration.getPortConfig().getPort()) {
                if (curPort.getCluster().equalsIgnoreCase(reposeInstanceInfo.getClusterId()) && StringUtilities.nullSafeEqualsIgnoreCase(curPort.getNode(), "-1")) {
                    port = curPort.getPort();
                }
            }

            return port;
        }


        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    private class SystemModelUpdateListener implements UpdateListener<SystemModel> {
        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            synchronized (configLock) {
                curSystemModel = configurationObject;
                if (curSystemModel != null) {
                    this.isInitialized = true; //Whoo, that was probably a thread bug!

                    if (distributedDatastoreConfigurationListener.isInitialized()) {
                        updateCluster();
                    }
                }
            }
            // After successful config update the error report will be removed
            healthCheckServiceProxy.resolveIssue(systemModelConfigHealthReport);
        }

        @Override
        public boolean isInitialized() {
            return this.isInitialized;
        }
    }
}
