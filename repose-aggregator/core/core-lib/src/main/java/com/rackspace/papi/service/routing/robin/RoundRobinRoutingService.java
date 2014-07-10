package com.rackspace.papi.service.routing.robin;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.routing.RoutingService;
import com.rackspace.papi.servlet.InitParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import java.net.URL;

@Component
public class RoundRobinRoutingService implements RoutingService, ServletContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(RoundRobinRoutingService.class);
    private Clusters domains;
    private SystemModel config;
    private final ConfigurationService configurationManager;
    private final PowerApiConfigListener configurationListener;
    private final ServicePorts servicePorts;
    private ServletContext ctx;


    @Autowired
    public RoundRobinRoutingService(final ServicePorts servicePorts, final ConfigurationService configurationManager) {
        this.configurationManager = configurationManager;
        this.configurationListener = new PowerApiConfigListener();
        this.servicePorts = servicePorts;
    }

    @Override
    public void setServletContext(final ServletContext servletContext) {
        this.ctx = servletContext;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        final URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationManager.subscribeTo("system-model.cfg.xml", xsdURL, configurationListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        if (configurationManager != null) {
            configurationManager.unsubscribeFrom("system-model.cfg.xml", configurationListener);
        }
    }

    @Override
    public void setSystemModel(final SystemModel config) {
        this.domains = new Clusters(config);
    }

    @Override
    public Node getRoutableNode(final String domainId) {
        final Node rtn;
        final ClusterWrapper domain = domains.getDomain(domainId);
        if (domain != null) {
            rtn = domain.getNextNode();
        } else {
            rtn = null;
        }
        return rtn;
    }

    private class PowerApiConfigListener implements UpdateListener<SystemModel> {
        private boolean isInitialized = false;

        private ServicePorts determinePorts(final Node reposeNode) {
            final ServicePorts ports = new ServicePorts();

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

        private Node determineReposeNode(final String clusterId, final String nodeId) {
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
        public void configurationUpdated(final SystemModel configurationObject) {
            config = configurationObject;
            setSystemModel(config);

            if (!isInitialized()) {
                final String clusterIdParam = InitParameter.REPOSE_CLUSTER_ID.getParameterName();
                final String nodeIdParam = InitParameter.REPOSE_NODE_ID.getParameterName();
                final String clusterId = System.getProperty(clusterIdParam, ctx.getInitParameter(clusterIdParam));
                final String nodeId = System.getProperty(nodeIdParam, ctx.getInitParameter(nodeIdParam));
                LOG.info("Determining ports for repose under cluster: " + clusterId);
                final ServicePorts ports = determinePorts(determineReposeNode(clusterId, nodeId));
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
