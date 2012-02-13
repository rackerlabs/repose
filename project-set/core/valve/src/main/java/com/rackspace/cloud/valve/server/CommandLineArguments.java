package com.rackspace.cloud.valve.server;

import org.kohsuke.args4j.*;

public class CommandLineArguments {
    private static final String defaultPortInfo = "(Default is port 8080, range is 1024 to 49150)";
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";

    @Option(name = "-s", aliases = {"--shutdown-port"},
            usage = "The port used to communicate a shutdown to Power API Valve " + defaultPortInfo)
    private Integer stopPort = 8818;

    @Option(name = "-c", aliases = {"--config-file"},
            usage = "The location of the Power API Valve configuration file")
    private String configDirectory;

    //Note: I recommend keeping this an argument to stay inline with what people expect from a daemon script
    @Argument(usage = "Action to take - start | stop", required = true)
    private String action = ACTION_START;

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
}
