package org.openrepose.core.services.routing.robin;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.domain.Port;
import org.openrepose.core.domain.ServicePorts;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.routing.RoutingService;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.systemmodel.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;

import static org.openrepose.core.spring.ReposeSpringProperties.CLUSTER_ID;
import static org.openrepose.core.spring.ReposeSpringProperties.NODE_ID;

@Named
public class RoundRobinRoutingService implements RoutingService {
    private static final Logger LOG = LoggerFactory.getLogger(RoundRobinRoutingService.class);
    private ConfigurationService configurationService;
    private PowerApiConfigListener configListener;
    private ServicePorts servicePorts;
    private String clusterId, nodeId;
    private Clusters domains;

    @Inject
    public RoundRobinRoutingService(ConfigurationService configurationService,
                                    @Value(NODE_ID) String nodeId,
                                    @Value(CLUSTER_ID) String clusterId) {
        configListener = new PowerApiConfigListener();
        this.configurationService = configurationService;
        this.servicePorts = servicePorts;
        this.clusterId = clusterId;
        this.nodeId = nodeId;

    }

    @PostConstruct
    public void afterPropertiesSet() {
        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationService.subscribeTo("system-model.cfg.xml", xsdURL, configListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        if (configurationService != null) {
            configurationService.unsubscribeFrom("system-model.cfg.xml", configListener);
        }
    }

    @Override
    public void setSystemModel(SystemModel config) {
        this.domains = new Clusters(config);
    }

    @Override
    public Node getRoutableNode(String domainId) {
        ClusterWrapper domain = domains.getDomain(domainId);
        if (domain != null) {
            return domain.getNextNode();
        }

        return null;
    }

    private class PowerApiConfigListener implements UpdateListener<SystemModel> {

        private SystemModel config;
        private boolean isInitialized = false;

        private ServicePorts determinePorts(Node reposeNode) {
            ServicePorts ports = new ServicePorts();

            if (reposeNode != null) {
                if (reposeNode.getHttpPort() != 0) {
                    ports.add(new Port("http", reposeNode.getHttpPort()));
                } else {
                    LOG.error("Http service port not specified for Repose Node " + reposeNode.getId());
                }

                if (reposeNode.getHttpsPort() != 0) {
                    ports.add(new Port("https", reposeNode.getHttpsPort()));
                } else {
                    LOG.info("Https service port not specified for Repose Node " + reposeNode.getId());
                }
            }

            return ports;
        }

        private Node determineReposeNode(String clusterId, String nodeId) {

            for (ReposeCluster cluster : config.getReposeCluster()) {

                if (cluster.getId().equals(clusterId)) {
                    for (Node node : cluster.getNodes().getNode()) {
                        if (node.getId().equals(nodeId)) {
                            return node;
                        }
                    }
                }
            }

            return null;
        }

        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            config = configurationObject;
            setSystemModel(config);


            if (!isInitialized()) {
                LOG.info("Determining ports for repose under cluster: " + clusterId);
                ServicePorts ports = determinePorts(determineReposeNode(clusterId, nodeId));
                servicePorts.clear();
                servicePorts.addAll(ports);
            }
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

}
