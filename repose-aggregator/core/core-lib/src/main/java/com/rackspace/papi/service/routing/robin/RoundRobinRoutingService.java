package com.rackspace.papi.service.routing.robin;

import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.routing.RoutingService;
import com.rackspace.papi.servlet.InitParameter;
import org.openrepose.core.service.config.ConfigurationService;
import org.openrepose.core.service.config.manager.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Named
public class RoundRobinRoutingService implements RoutingService, ServletContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(RoundRobinRoutingService.class);
    private final ReposeInstanceInfo reposeInstanceInfo;
    private Clusters domains;
    private SystemModel config;
    private final ConfigurationService configurationManager;
    private final PowerApiConfigListener configurationListener;
    private ServletContext servletContext;


    @Inject
    public RoundRobinRoutingService(
            ReposeInstanceInfo reposeInstanceInfo,
            ConfigurationService configurationManager) {
        this.configurationManager = configurationManager;
        this.configurationListener = new PowerApiConfigListener();
        this.reposeInstanceInfo = reposeInstanceInfo;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
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

    //TODO: WHAT IS THIS MADNESS!?!?
    //WHAT ARE THESE PORTS FOR??!?
    private class PowerApiConfigListener implements UpdateListener<SystemModel> {
        private boolean isInitialized = false;

        //So this collects poerts from all nodes, and just throws them in a list without associating what node they're with?
        // WHAT MADNESS IS THIS
        private List<Port> determinePorts(final Node reposeNode) {
            //This will replace all the ports, and I don't know how this is not a problem.
            final ArrayList<Port> ports = new ArrayList<>();

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
                final String clusterId = System.getProperty(clusterIdParam, servletContext.getInitParameter(clusterIdParam));
                final String nodeId = System.getProperty(nodeIdParam, servletContext.getInitParameter(nodeIdParam));

                LOG.info("Determining ports for repose under cluster: " + clusterId);
                //TODO: This is super jank, because it clobbers ports and then adds them all, but it's what it always did
                reposeInstanceInfo.getPorts().clear();
                reposeInstanceInfo.getPorts().addAll(determinePorts(determineReposeNode(clusterId, nodeId)));
            }
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
