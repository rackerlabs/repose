package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.domain.Port;
import com.rackspace.papi.servlet.InitParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is to replace some of the functionality that was in the PowerApiContextManager
 * Specifically around collecting the ClusterID, NodeID, and Port list, so that other beans can access it.
 * Should be used everywhere the "servicePorts" bean was used and the "instanceInfo" bean
 */
@Component
public class ReposeInfo implements ServletContextAware {

    public static final String PORT_LIST_ATTRIBUTE = "org.openrepose.server.PortList";

    private String clusterId;
    private String nodeId;
    private List<Port> ports;

    private ServletContext servletContext;

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        //Get the thingies out of the servlet context and hook them up in here.
        clusterId = servletContext.getInitParameter(InitParameter.REPOSE_CLUSTER_ID.getParameterName());
        //If they were not set in the servlet context, get them from system properties
        if(clusterId == null) {
            clusterId = System.getProperty("repose-cluster-id");
        }

        nodeId = servletContext.getInitParameter(InitParameter.REPOSE_NODE_ID.getParameterName());
        if(nodeId == null) {
            nodeId = System.getProperty("repose-node-id");
        }

        // If ports is null, oh well. Nothing creates them if they're null
        ports = (List<Port>) servletContext.getAttribute(PORT_LIST_ATTRIBUTE);
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public List<Port> getPorts() {
        return ports;
    }

    /**
     * Just a convenience method to replace the one in ServicePorts
     * @return A list of the port numbers only
     */
    public List<Integer> getPortNumbers() {
        List<Integer> portNumbers = new ArrayList<Integer>();

        for (Port port: ports) {
            portNumbers.add(port.getPort());
        }

        return portNumbers;
    }
}
