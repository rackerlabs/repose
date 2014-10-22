package org.openrepose.core.services.context.impl;

import com.google.common.base.Optional;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.domain.ServicePorts;
import org.openrepose.core.filter.SystemModelInterrogator;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ServiceContext;
import org.openrepose.core.services.context.ServletContextHelper;
import org.openrepose.core.services.headers.request.RequestHeaderService;
import org.openrepose.core.services.headers.request.ViaRequestHeaderBuilder;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;

@Component("requestHeaderServiceContext")
public class RequestHeaderServiceContext implements ServiceContext<RequestHeaderService> {
    private static final Logger LOG = LoggerFactory.getLogger(RequestHeaderServiceContext.class);
    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";
    public static final String SERVICE_NAME = "powerapi:/services/request_header";

    private final RequestHeaderService requestHeaderService;
    private final ServiceRegistry registry;
    private final ConfigurationService configurationManager;
    private final ContainerConfigurationListener configurationListener;
    private final SystemModelListener systemModelListener;
    private final HealthCheckService healthCheckService;
    private final String clusterId;
    private final String nodeId;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private ServicePorts ports;
    private String reposeVersion = "";
    private String viaReceivedBy = "";
    private String hostname = "Repose";

    @Autowired
    public RequestHeaderServiceContext(@Qualifier("requestHeaderService") RequestHeaderService requestHeaderService,
                                       @Qualifier("serviceRegistry") ServiceRegistry registry,
                                       @Qualifier("configurationManager") ConfigurationService configurationManager,
                                       @Qualifier("healthCheckService") HealthCheckService healthCheckService,
                                       @Value(ReposeSpringProperties.CLUSTER_ID) String clusterId,
                                       @Value(ReposeSpringProperties.NODE_ID) String nodeId
                                       ) {
        this.requestHeaderService = requestHeaderService;
        this.registry = registry;
        this.configurationManager = configurationManager;
        this.healthCheckService = healthCheckService;
        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.configurationListener = new ContainerConfigurationListener();
        this.systemModelListener = new SystemModelListener();
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
    public RequestHeaderService getService() {
        return requestHeaderService;
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ports = ServletContextHelper.getInstance(servletContextEvent.getServletContext()).getServerPorts();
        reposeVersion = ServletContextHelper.getInstance(servletContextEvent.getServletContext()).getPowerApiContext().getReposeVersion();

        healthCheckServiceProxy = healthCheckService.register();

        configurationManager.subscribeTo("container.cfg.xml", configurationListener, ContainerConfiguration.class);
        configurationManager.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        configurationManager.unsubscribeFrom("container.cfg.xml", configurationListener);
        configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
    }

    /**
     * Listens for updates to the container.cfg.xml file which holds the via
     * header receivedBy value.
     */
    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {

            if (configurationObject.getDeploymentConfig() != null) {
                viaReceivedBy = configurationObject.getDeploymentConfig().getVia();

                final ViaRequestHeaderBuilder viaBuilder = new ViaRequestHeaderBuilder(reposeVersion, viaReceivedBy, hostname);
                requestHeaderService.updateConfig(viaBuilder);
            }
            isInitialized = true;

        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    /**
     * Listens for updates to the system-model.cfg.xml file which holds the
     * hostname.
     */
    private class SystemModelListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel systemModel) {

            final SystemModelInterrogator interrogator = new SystemModelInterrogator(clusterId, nodeId);
            Optional<Node> ln = interrogator.getLocalNode(systemModel);

            if (ln.isPresent()) {
                hostname = ln.get().getHostname();

                final ViaRequestHeaderBuilder viaBuilder = new ViaRequestHeaderBuilder(reposeVersion, viaReceivedBy, hostname);
                requestHeaderService.updateConfig(viaBuilder);
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
}
