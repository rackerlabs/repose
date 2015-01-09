package org.openrepose.nodeservice.request;

import com.google.common.base.Optional;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.filter.SystemModelInterrogator;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.headers.common.ViaHeaderBuilder;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.core.services.healthcheck.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class RequestHeaderServiceImpl implements RequestHeaderService {

    private static final Logger LOG = LoggerFactory.getLogger(RequestHeaderServiceImpl.class);

    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";

    private final ContainerConfigurationListener containerConfigurationListener;
    private final SystemModelListener systemModelListener;
    private final ConfigurationService configurationService;
    private final HealthCheckService healthCheckService;
    private final String clusterId;
    private final String nodeId;
    private final String reposeVersion;

    private String hostname;
    private String viaReceivedBy;
    private ViaHeaderBuilder viaHeaderBuilder;
    private HealthCheckServiceProxy healthCheckServiceProxy;

    @Inject
    public RequestHeaderServiceImpl(ConfigurationService configurationService,
                                    HealthCheckService healthCheckService,
                                    @Value(ReposeSpringProperties.NODE.CLUSTER_ID) String clusterId,
                                    @Value(ReposeSpringProperties.NODE.NODE_ID) String nodeId,
                                    @Value(ReposeSpringProperties.CORE.REPOSE_VERSION) String reposeVersion) {
        this.configurationService = configurationService;
        this.healthCheckService = healthCheckService;
        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.reposeVersion = reposeVersion;

        this.containerConfigurationListener = new ContainerConfigurationListener();
        this.systemModelListener = new SystemModelListener();
    }

    @PostConstruct
    public void init() {
        healthCheckServiceProxy = healthCheckService.register();

        configurationService.subscribeTo("container.cfg.xml", containerConfigurationListener, ContainerConfiguration.class);
        configurationService.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        configurationService.unsubscribeFrom("container.cfg.xml", containerConfigurationListener);
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
        healthCheckServiceProxy.resolveIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT);
    }

    public synchronized void updateConfig(ViaHeaderBuilder viaHeaderBuilder) {
        this.viaHeaderBuilder = viaHeaderBuilder;
    }

    @Override
    public void setXForwardedFor(MutableHttpServletRequest request) {
        request.addHeader(CommonHttpHeader.X_FORWARDED_FOR.toString(), request.getRemoteAddr());
    }

    @Override
    public void setVia(MutableHttpServletRequest request) {
        final String via = viaHeaderBuilder.buildVia(request);
        request.addHeader(CommonHttpHeader.VIA.toString(), via);
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
                updateConfig(viaBuilder);
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
                updateConfig(viaBuilder);
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
