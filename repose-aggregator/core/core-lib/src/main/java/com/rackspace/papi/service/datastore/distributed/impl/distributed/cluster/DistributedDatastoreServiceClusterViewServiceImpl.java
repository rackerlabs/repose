package com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.datastore.distributed.ClusterView;
import com.rackspace.papi.components.datastore.impl.distributed.ThreadSafeClusterView;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.datastore.DatastoreAccessControl;
import com.rackspace.papi.service.datastore.distributed.config.DistributedDatastoreConfiguration;
import com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster.utils.AccessListDeterminator;
import com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster.utils.ClusterMemberDeterminator;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.healthcheck.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Component
public class DistributedDatastoreServiceClusterViewServiceImpl implements DistributedDatastoreServiceClusterViewService {
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
    private DatastoreAccessControl hostACL;
    private DatastoreAccessControl accessControl;
    private ReposeInstanceInfo reposeInstanceInfo;
    private ConfigurationService configurationManager;
    private HealthCheckService.HealthCheckServiceProxy healthCheckServiceProxy;
    private DistributedDatastoreConfiguration curDistributedDatastoreConfiguration;

    @Autowired
    public DistributedDatastoreServiceClusterViewServiceImpl(ServletContext servletContext,
                                                             ConfigurationService configurationManager,
                                                             ReposeInstanceInfo reposeInstanceInfo,
                                                             HealthCheckService healthCheckService) {
        this.servletContext = servletContext;
        this.configurationManager = configurationManager;
        this.reposeInstanceInfo = reposeInstanceInfo;
        this.healthCheckServiceProxy = healthCheckService.register(DistributedDatastoreServiceClusterViewServiceImpl.class);
    }

    @PostConstruct
    public void afterPropertiesSet() {
        //Setting Initial Broken state.
        healthCheckServiceProxy.reportIssue(datastoreConfigHealthReport, "Dist Datastore Configuration Error", Severity.BROKEN);
        healthCheckServiceProxy.reportIssue(systemModelConfigHealthReport, "System Model Configuration Error", Severity.BROKEN);
        hostACL = new DatastoreAccessControl(Collections.EMPTY_LIST, false);
        String ddPort = servletContext.getInitParameter("datastoreServicePort");
        List<Integer> servicePorts = new ArrayList<Integer>();
        servicePorts.add(Integer.parseInt(ddPort));
        clusterView = new ThreadSafeClusterView(servicePorts);
        initialize(clusterView, hostACL);
        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationManager.subscribeTo("system-model.cfg.xml", xsdURL, systemModelUpdateListener, SystemModel.class);
        URL dXsdURL = getClass().getResource("/META-INF/schema/config/dist-datastore-configuration.xsd");
        configurationManager.subscribeTo(DEFAULT_CONFIG, dXsdURL, distributedDatastoreConfigurationListener, DistributedDatastoreConfiguration.class);

        try {
            if (!distributedDatastoreConfigurationListener.isInitialized() && !configurationManager.getResourceResolver().resolve(DEFAULT_CONFIG).exists()) {
                healthCheckServiceProxy.resolveIssue(datastoreConfigHealthReport);
                healthCheckServiceProxy.resolveIssue(systemModelConfigHealthReport);
            }
        } catch (IOException e) {
            LOG.error("Unable to search for {}", DEFAULT_CONFIG, e);
        }
        servletContext.setAttribute("ddClusterViewService", this);
    }

    @PreDestroy
    public void destroy() {
        if (configurationManager != null) {
            configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelUpdateListener);
            configurationManager.unsubscribeFrom(DEFAULT_CONFIG, distributedDatastoreConfigurationListener);
        }
    }

    @Override
    public void initialize(ClusterView clusterView, DatastoreAccessControl accessControl) {
        this.clusterView = clusterView;
        this.accessControl = accessControl;
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

            hostACL = AccessListDeterminator.getAccessList(curDistributedDatastoreConfiguration, clusterMembers);

            updateAccessList(hostACL);
        }
    }

    private class DistributedDatastoreConfigurationListener implements UpdateListener<DistributedDatastoreConfiguration> {
        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(DistributedDatastoreConfiguration configurationObject) {
            synchronized (configLock) {
                curDistributedDatastoreConfiguration = configurationObject;
                if (curDistributedDatastoreConfiguration != null) {
                    isInitialized = true;

                    if (systemModelUpdateListener.isInitialized()) {
                        updateCluster();
                    }
                }
            }
            // After successful config update the error report will be removed
            healthCheckServiceProxy.resolveIssue(datastoreConfigHealthReport);
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
                    isInitialized = true;

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
            return isInitialized;
        }
    }
}
