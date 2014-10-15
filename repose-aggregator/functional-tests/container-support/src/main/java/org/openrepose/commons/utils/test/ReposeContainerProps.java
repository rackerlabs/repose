package org.openrepose.commons.utils.test;

public class ReposeContainerProps {

    String startPort;
    String war;
    String[] originServiceWars;

    public ReposeContainerProps(String startPort, String war, String... originServiceWars){
        this.startPort = startPort;
        this.war = war;
        this.originServiceWars = originServiceWars;

    }

    public String getStartPort() {
        return startPort;
    }

    public String getWar() {
        return war;
    }

    public String[] getOriginServiceWars(){
        return originServiceWars;
    }
}
