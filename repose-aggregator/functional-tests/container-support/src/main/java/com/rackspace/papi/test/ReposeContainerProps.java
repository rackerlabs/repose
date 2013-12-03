package com.rackspace.papi.test;

public class ReposeContainerProps {

    String startPort;
    String stopPort;
    String war;
    String[] originServiceWars;

    public ReposeContainerProps(String startPort, String stopPort, String war, String... originServiceWars){
        this.startPort = startPort;
        this.stopPort = stopPort;
        this.war = war;
        this.originServiceWars = originServiceWars;

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

    public String[] getOriginServiceWars(){
        return originServiceWars;
    }
}
