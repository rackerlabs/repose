package org.openrepose.core.spring;

/**
 * Used to provide a single point of reference for our necessary spring properties
 */
public class ReposeSpringProperties {

    //TODO: These need to be available to the core context
    public static final String REPOSE_VERSION = "repose-version";
    public static final String CLUSTER_ID = "repose-cluster-id";
    public static final String CONFIG_ROOT = "powerapi-config-directory";
    public static final String INSECURE = "repose-insecurity";

    //TODO: This one is the only one that's node specific
    public static final String NODE_ID = "repose-node-id";

}
