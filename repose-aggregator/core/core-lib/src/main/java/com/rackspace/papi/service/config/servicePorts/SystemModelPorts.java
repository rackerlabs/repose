package com.rackspace.papi.service.config.servicePorts;

import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import org.openrepose.core.service.config.ConfigurationService;
import org.openrepose.core.service.config.manager.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to subscribe to the systemmodel and trigger a callback when the ports are updated
 * This is to replace the ServicePorts problem, so that when stuff needs the service ports, they just get updated
 * when the system model changes.
 *
 * It only gets the ports for the specified cluster and node ID
 *
 * CANNOT be a named bean, because this needs to exist for each item that cares about the ServicePorts
 *
 * NOTE: this is probably a gross hack, and there's probably copy/pasta from other things, in order to get repose working
 * This should eventually go away with something that makes much more sense.
 */
public class SystemModelPorts {
    private static final Logger LOG = LoggerFactory.getLogger(SystemModelPorts.class);

    private final ConfigurationService configurationService;
    private final ReposeServicePortsAware rspa;
    private final String clusterId;
    private final String nodeId;
    private SystemModelConfigListener configListener;

    public SystemModelPorts(final ConfigurationService configurationService,
                            final ReposeServicePortsAware reposeServicePortsAware,
                            final String clusterId,
                            final String nodeId) {
        this.configurationService = configurationService;
        this.rspa = reposeServicePortsAware;
        this.clusterId = clusterId;
        this.nodeId = nodeId;

        //start listening to the thing on the configuration service
        configListener = new SystemModelConfigListener();
        final URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationService.subscribeTo("system-model.cfg.xml", xsdURL, configListener, SystemModel.class);
    }

    private class SystemModelConfigListener implements UpdateListener<SystemModel> {
        private boolean initialized = false;

        /**
         * Get the ports from the system model and return them as a list
         * @param reposeNode
         * @return
         */
        private List<Port> determinePorts(final Node reposeNode) {
            final ArrayList<Port> ports = new ArrayList<>();

            if (reposeNode != null) {
                if (reposeNode.getHttpPort() != 0) {
                    ports.add(new Port("http", reposeNode.getHttpPort()));
                } else {
                    LOG.error("Http service port not specified for Repose Node {}", reposeNode.getId());
                }

                if (reposeNode.getHttpsPort() != 0) {
                    ports.add(new Port("https", reposeNode.getHttpsPort()));
                } else {
                    LOG.info("Https service port not specified for Repose Node {}", reposeNode.getId());
                }
            }
            return ports;
        }

        private Node determineReposeNode(final SystemModel config) {
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
            if(!isInitialized()) {
                LOG.info("Determining ports from systemModel for cluster {} and node {}", clusterId, nodeId);

                //Make the callback to the thing that cares about new service ports
                rspa.updatedServicePorts(determinePorts(determineReposeNode(configurationObject)));
            }

            //TODO: that means this is only done when things are initialized, this might not be right
            //It's how everything else does it, but it might not be the right behavior here....
            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }

    /**
     * Something should call this when their bean dies
     */
    public void destroy() {
        configurationService.unsubscribeFrom("system-model.cfg.xml", configListener);
    }

}
