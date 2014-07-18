package com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.datastore.distributed.ClusterView;
import com.rackspace.papi.components.datastore.impl.distributed.ThreadSafeClusterView;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.datastore.DatastoreAccessControl;
import com.rackspace.papi.service.datastore.distributed.config.DistributedDatastoreConfiguration;
import com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster.utils.AccessListDeterminator;
import com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster.utils.ClusterMemberDeterminator;
import com.rackspace.papi.service.healthcheck.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Component("distributedDatastoreServiceClusterContext")
public class DistributedDatastoreServiceClusterContext implements ServiceContext<DistributedDatastoreServiceClusterViewService> {

    private DistributedDatastoreServiceClusterViewService service;
    public static final String SERVICE_NAME = "distributedDatastoreClusterView";
    public static final String DEFAULT_CONFIG = "dist-datastore.cfg.xml";
    private static final String datastoreConfigHealthReport = "DistDatastoreConfigError";
    private static final String systemModelConfigHealthReport = "SystemModelConfigError";
    private final Object configLock = new Object();
    private DistributedDatastoreConfiguration curDistributedDatastoreConfiguration;
    private SystemModel curSystemModel;
    private DistributedDatastoreConfigurationListener distributedDatastoreConfigurationListener;
    private SystemModelUpdateListener systemModelUpdateListener;
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreServiceClusterContext.class);
    private DatastoreAccessControl hostACL;
    private ConfigurationService configurationManager;
    private ClusterView clusterView;
    private ReposeInstanceInfo reposeInstanceInfo;
    private ServiceRegistry registry;
    private HealthCheckServiceProxy healthCheckServiceProxy;

    @Autowired
    public DistributedDatastoreServiceClusterContext(@Qualifier("configurationManager") ConfigurationService configurationManager,
                                                     @Qualifier("clusterViewService") DistributedDatastoreServiceClusterViewService service,
                                                     @Qualifier("reposeInstanceInfo") ReposeInstanceInfo reposeInstanceInfo,
                                                     @Qualifier("serviceRegistry") ServiceRegistry registry,
                                                     @Qualifier("healthCheckService") HealthCheckService healthCheckService) {
        this.configurationManager = configurationManager;
        this.service = service;
        this.reposeInstanceInfo = reposeInstanceInfo;
        this.registry = registry;
        this.healthCheckServiceProxy = healthCheckService.register();
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public DistributedDatastoreServiceClusterViewService getService() {
        return service;
    }

    public void register() {
        if (registry != null) {
            registry.addService(this);
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

    /*
     * updates the hashring cluster view and the host access list
     */
    public void updateCluster() {

        updateClusterMembers();
        updateAccessList();

    }

    protected void updateClusterMembers() {


        List<InetSocketAddress> cacheSiblings = ClusterMemberDeterminator.getClusterMembers(curSystemModel, curDistributedDatastoreConfiguration, reposeInstanceInfo.getClusterId());
        service.updateClusterView(cacheSiblings);
    }

    private void updateAccessList() {

        synchronized (configLock) {
            List<InetAddress> clusterMembers = new LinkedList<InetAddress>();

            if (curSystemModel != null) {
                clusterMembers = AccessListDeterminator.getClusterMembers(curSystemModel, reposeInstanceInfo.getClusterId());
            }

            hostACL = AccessListDeterminator.getAccessList(curDistributedDatastoreConfiguration, clusterMembers);

            service.updateAccessList(hostACL);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        //Setting Initial Broken state.
        healthCheckServiceProxy.reportIssue(datastoreConfigHealthReport, "Dist Datastore Configuration Error", Severity.BROKEN);
        healthCheckServiceProxy.reportIssue(systemModelConfigHealthReport, "System Model Configuration Error", Severity.BROKEN);
        hostACL = new DatastoreAccessControl(Collections.EMPTY_LIST, false);
        String ddPort = sce.getServletContext().getInitParameter("datastoreServicePort");
        List<Integer> servicePorts = new ArrayList<Integer>();
        servicePorts.add(Integer.parseInt(ddPort));
        clusterView = new ThreadSafeClusterView(servicePorts);
        service.initialize(clusterView, hostACL);
        systemModelUpdateListener = new SystemModelUpdateListener();
        distributedDatastoreConfigurationListener = new DistributedDatastoreConfigurationListener();
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
        sce.getServletContext().setAttribute("ddClusterViewService", service);
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (configurationManager != null) {
            configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelUpdateListener);
            configurationManager.unsubscribeFrom(DEFAULT_CONFIG, distributedDatastoreConfigurationListener);
        }
    }
}
