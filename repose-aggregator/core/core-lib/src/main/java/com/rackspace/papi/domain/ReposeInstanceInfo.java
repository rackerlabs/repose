package com.rackspace.papi.domain;

import com.rackspace.papi.servlet.InitParameter;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is to replace some of the functionality that was in the PowerApiContextManager
 * Specifically around collecting the ClusterID, NodeID, and Port list, so that other beans can access it.
 * Should be used everywhere the "servicePorts" bean was used and the "instanceInfo" bean
 *  TODO: this should probably not be a named bean.
 *  I think it is used to contain the clusterID and the nodeID of this instance, and that's all....
 */
@Named
public class ReposeInstanceInfo implements ServletContextAware {

    private String clusterId;
    private String nodeId;

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
        //The other problem is that this might not work!
        if (clusterId == null) {
            clusterId = System.getProperty("repose-cluster-id");
        }

        nodeId = servletContext.getInitParameter(InitParameter.REPOSE_NODE_ID.getParameterName());
        if (nodeId == null) {
            nodeId = System.getProperty("repose-node-id");
        }
    }

    //TODO: it really bothers me that we set these in other places!
    //TODO: THIS SHOULD BE FIXED SOMEHOW
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getClusterId() {
        //TODO: this is probably the worst thing to do, but I'm working on it...
        clusterId = servletContext.getInitParameter(InitParameter.REPOSE_CLUSTER_ID.getParameterName());
        //If they were not set in the servlet context, get them from system properties
        //The other problem is that this might not work!
        if (clusterId == null) {
            clusterId = System.getProperty("repose-cluster-id");
        }
        return clusterId;
    }

    public String getNodeId() {
        nodeId = servletContext.getInitParameter(InitParameter.REPOSE_NODE_ID.getParameterName());
        if (nodeId == null) {
            nodeId = System.getProperty("repose-node-id");
        }
        return nodeId;
    }

}
