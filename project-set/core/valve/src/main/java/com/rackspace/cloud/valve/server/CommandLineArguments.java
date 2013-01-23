package com.rackspace.cloud.valve.server;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class CommandLineArguments {
    private static final String DEFAULT_HTTP_PORT_INFO = "(Range is 1024 to 49150)";
    private static final String DEFAULT_HTTPS_PORT_INFO = "(Default is only run Repose on http if https port not specified, range is 1024 to 49150)";
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    private static final int DEFAULT_STOP_PORT = 8818;
    private static final boolean DEFAULT_INSECURE = false;

    @Option(name = "-p", aliases = {"--http-port"},
            usage = "*DEPRECATED* Repose http port number " + DEFAULT_HTTP_PORT_INFO)
    private Integer httpPort;

    @Option(name = "-ps", aliases = {"--https-port"},
            usage = "*DEPRECATED* Repose https port number " + DEFAULT_HTTPS_PORT_INFO)
    private Integer httpsPort;

    @Option(name = "-s", aliases = {"--shutdown-port"},
            usage = "The port used to communicate a shutdown to Repose " + DEFAULT_HTTP_PORT_INFO)
    private Integer stopPort = DEFAULT_STOP_PORT;

    @Option(name = "-c", aliases = {"--config-file"},
            usage = "The location of the Repose configuration file")
    private String configDirectory;

    @Option(name = "-cf", aliases = {"--connection-framework"},
            usage = "The http connection framework. Available values are jersey, ning, apache.")
    private String connectionFramework;

    @Option(name = "-k", aliases = {"--insecure"},
            usage = "Allows Repose to connect to SSL servers (e.g. auth, origin service) without certs.")
    private Boolean insecure = DEFAULT_INSECURE;

    @Argument(usage = "Action to take - start | stop", required = true)
    private String action = ACTION_START;

    @Deprecated
    public Integer getHttpPort() {
        return httpPort;
    }

    @Deprecated
    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    @Deprecated
    public Integer getHttpsPort() {
        return httpsPort;
    }

    @Deprecated
    public void setHttpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
    }

    public Integer getStopPort() {
        return stopPort;
    }

    public void setStopPort(Integer stopport) {
        this.stopPort = stopport;
    }

    public String getConfigDirectory() {
        return configDirectory;
    }

    public void setConfigDirectory(String configDirectory) {
        this.configDirectory = configDirectory;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getConnectionFramework() {
        return connectionFramework;
    }

    public void setConnectionFramework(String connectionFramework) {
        this.connectionFramework = connectionFramework;
    }

    public Boolean getInsecure() {
        return insecure;
    }

    public void setInsecure(Boolean insecure) {
        this.insecure = insecure;
    }
}
