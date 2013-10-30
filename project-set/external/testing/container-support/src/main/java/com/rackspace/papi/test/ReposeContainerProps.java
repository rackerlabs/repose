package com.rackspace.papi.test;

public class ReposeContainerProps {

    String startPort;
    String stopPort;
    String war;

    public ReposeContainerProps(String startPort, String stopPort, String war) {
        this.startPort = startPort;
        this.stopPort = stopPort;
        this.war = war;
    }

    public String getStartPort() {
        return startPort;
    }

    public String getStopPort() {
        return stopPort;
    }

    public String getWar() {
        return war;
    }
}
