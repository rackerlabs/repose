package org.openrepose.core.services.context.impl;

import com.google.common.base.Optional;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.proxy.RequestProxyService;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.filter.SystemModelInterrogator;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ServiceContext;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;

@Component("requestProxyServiceContext")
@Lazy(true)
public class RequestProxyServiceContext implements ServiceContext<RequestProxyService> {
    private static final Logger LOG = LoggerFactory.getLogger(RequestProxyServiceContext.class);
    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";
    public static final String SERVICE_NAME = "powerapi:/services/proxy";

    private final ConfigurationService configurationManager;
    private final RequestProxyService proxyService;
    private final ServiceRegistry registry;
    private final ContainerConfigListener configListener;
    private final SystemModelInterrogator interrogator;
    private final SystemModelListener systemModelListener;
    private final HealthCheckService healthCheckService;

    private HealthCheckServiceProxy healthCheckServiceProxy;

    @Autowired
    public RequestProxyServiceContext(
            @Qualifier("requestProxyService") RequestProxyService proxyService,
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            @Qualifier("configurationManager") ConfigurationService configurationManager,
            @Qualifier("modelInterrogator") SystemModelInterrogator interrogator,
            @Qualifier("healthCheckService") HealthCheckService healthCheckService) {
        this.proxyService = proxyService;
        this.configurationManager = configurationManager;
        this.registry = registry;
        this.configListener = new ContainerConfigListener();
        this.systemModelListener = new SystemModelListener();
        this.interrogator = interrogator;
        this.healthCheckService = healthCheckService;
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
    public RequestProxyService getService() {
        return proxyService;
    }

    private class ContainerConfigListener implements UpdateListener<ContainerConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration config) {
            Integer connectionTimeout = config.getDeploymentConfig().getConnectionTimeout();
            Integer readTimeout = config.getDeploymentConfig().getReadTimeout();
            Integer proxyThreadPool = config.getDeploymentConfig().getProxyThreadPool();
            boolean requestLogging = config.getDeploymentConfig().isClientRequestLogging();
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    private class SystemModelListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel config) {
            Optional<ReposeCluster> localCluster = interrogator.getLocalCluster(config);

            if (localCluster.isPresent()) {
                proxyService.setRewriteHostHeader(localCluster.get().isRewriteHostHeader());
                isInitialized = true;

                healthCheckServiceProxy.resolveIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT);
            } else {
                LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
                healthCheckServiceProxy.reportIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT, "Unable to identify the " +
                        "local host in the system model - please check your system-model.cfg.xml", Severity.BROKEN);
            }
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        healthCheckServiceProxy= healthCheckService.register();

        configurationManager.subscribeTo("container.cfg.xml", configListener, ContainerConfiguration.class);
        configurationManager.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (configurationManager != null) {
            configurationManager.unsubscribeFrom("container.cfg.xml", configListener);
            configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
        }
    }
}
