package org.openrepose.commons.utils.test;

public abstract class ReposeContainer {

    protected String listenPort, warLocation;

    public ReposeContainer(ReposeContainerProps props){
        this.listenPort = props.getStartPort();
        this.warLocation = props.getWar();
    }

    protected abstract  void startRepose();

    protected  abstract void stopRepose();
}
