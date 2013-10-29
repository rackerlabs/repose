package com.rackspace.papi.test;

public abstract class ReposeContainer {

    protected String listenPort, stopPort, warLocation;

    public ReposeContainer(ReposeContainerProps props){
        this.listenPort = props.getStartPort();
        this.stopPort = props.getStopPort();
        this.warLocation = props.getWar();
    }

    protected abstract  void startRepose();

    protected  abstract void stopRepose();
}
