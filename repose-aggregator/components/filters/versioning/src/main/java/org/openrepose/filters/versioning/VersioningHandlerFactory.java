package org.openrepose.filters.versioning;

import com.google.common.base.Optional;
import org.openrepose.core.systemmodel.Destination;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.domain.ServicePorts;
import org.openrepose.core.filter.SystemModelInterrogator;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;
import org.openrepose.filters.versioning.config.ServiceVersionMappingList;
import org.openrepose.filters.versioning.domain.ConfigurationData;
import org.openrepose.filters.versioning.util.ContentTransformer;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersioningHandlerFactory extends AbstractConfiguredFilterHandlerFactory<VersioningHandler> {
    private static final Logger LOG = LoggerFactory.getLogger(VersioningHandlerFactory.class);
    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";

    private final Map<String, ServiceVersionMapping> configuredMappings = new HashMap<String, ServiceVersionMapping>();
    private final Map<String, Destination> configuredHosts = new HashMap<String, Destination>();
    private final ContentTransformer transformer;
    private final MetricsService metricsService;

    private HealthCheckServiceProxy healthCheckServiceProxy;
    private ReposeCluster localDomain;
    private Node localHost;

    public VersioningHandlerFactory(ServicePorts ports, MetricsService metricsService, HealthCheckService healthCheckService) {
        this.metricsService = metricsService;

        this.healthCheckServiceProxy = healthCheckService.register();
        this.transformer = new ContentTransformer();
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(ServiceVersionMappingList.class, new VersioningConfigurationListener());
                put(SystemModel.class, new SystemModelConfigurationListener());
            }
        };
    }

    private class SystemModelConfigurationListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            //TODO: fix this to use a clusterID, and nodeId when doing actual spring stuff
            SystemModelInterrogator interrogator = new SystemModelInterrogator("","");
            Optional<ReposeCluster> cluster = interrogator.getLocalCluster(configurationObject);
            Optional<Node> node = interrogator.getLocalNode(configurationObject);

            if (cluster.isPresent() && node.isPresent()) {
                localDomain = cluster.get();
                localHost = node.get();

                List<Destination> destinations = new ArrayList<>();

                destinations.addAll(localDomain.getDestinations().getEndpoint());
                destinations.addAll(localDomain.getDestinations().getTarget());
                for (Destination powerApiHost : destinations) {
                    configuredHosts.put(powerApiHost.getId(), powerApiHost);
                }

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

    private class VersioningConfigurationListener implements UpdateListener<ServiceVersionMappingList> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ServiceVersionMappingList mappings) {
            configuredMappings.clear();

            for (ServiceVersionMapping mapping : mappings.getVersionMapping()) {
                configuredMappings.put(mapping.getId(), mapping);
            }

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected VersioningHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }

        final Map<String, ServiceVersionMapping> copiedVersioningMappings = new HashMap<>(configuredMappings);
        final Map<String, Destination> copiedHostDefinitions = new HashMap<>(configuredHosts);

        final ConfigurationData configData = new ConfigurationData(localDomain, localHost, copiedHostDefinitions, copiedVersioningMappings);

        return new VersioningHandler(configData, transformer, metricsService);
    }
}
