package org.openrepose.core.valve;

import org.kohsuke.args4j.Option;

public class CommandLineArguments {
    private static final String DEFAULT_HTTP_PORT_INFO = "(Range is 1024 to 49150)";
    private static final String DEFAULT_HTTPS_PORT_INFO = "(Default is only run Repose on http if https port not specified, range is 1024 to 49150)";
    private static final boolean DEFAULT_INSECURE = false;

    @Option(name = "-p",  aliases = {"--http-port"},
            usage = "*DEPRECATED* Repose http port number " + DEFAULT_HTTP_PORT_INFO)
    private Integer httpPort;

    @Option(name = "-ps",  aliases = {"--https-port"},
            usage = "*DEPRECATED* Repose https port number " + DEFAULT_HTTPS_PORT_INFO)
    private Integer httpsPort;

    @Option(name = "-c",  aliases = {"--config-file"},
            usage = "The location of the Repose configuration file (Default config directory: /etc/repose) (java -jar repose-valve.jar -c /etc/repose/config)")
    private String configDirectory;

    @Option(name = "-k", aliases = {"--insecure"},
            usage = "Allows Repose to connect to SSL servers (e.g. auth, origin service) without certs, use this option if you have specified https port for node in system model configuration file. (Example: java -jar repose-valve.jar -c /etc/repose/config -k).")
    private Boolean insecure = DEFAULT_INSECURE;

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

    public String getConfigDirectory() {
        return configDirectory;
    }

    public void setConfigDirectory(String configDirectory) {
        this.configDirectory = configDirectory;
    }

    public Boolean getInsecure() {
        return insecure;
    }

    public void setInsecure(Boolean insecure) {
        this.insecure = insecure;
    }
}
