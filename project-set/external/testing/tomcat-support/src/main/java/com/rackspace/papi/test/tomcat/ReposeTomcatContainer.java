package com.rackspace.papi.test.tomcat;

import com.rackspace.papi.test.ContainerMonitorThread;
import com.rackspace.papi.test.ReposeContainer;
import com.rackspace.papi.test.ReposeContainerProps;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.ServletException;

public class ReposeTomcatContainer extends ReposeContainer {

    private Tomcat tomcat;
    protected ContainerMonitorThread monitor;


    public ReposeTomcatContainer(ReposeContainerProps props) throws ServletException {
        super(props);
        tomcat = new Tomcat();
        tomcat.setPort(Integer.parseInt(listenPort));
        tomcat.getHost().setAutoDeploy(true);
        tomcat.getHost().setDeployOnStartup(true);
        tomcat.addWebapp("/", warLocation);
        monitor = new ContainerMonitorThread(this, Integer.parseInt(stopPort));


    }

    @Override
    protected void startRepose() {
        try {
            monitor.start();
            tomcat.start();
            tomcat.getServer().await();
            System.out.println("Tomcat Container Running");
        } catch (LifecycleException e) {
            System.err.println("Unable To Start Tomcat Server");
        }
    }

    @Override
    protected void stopRepose() {
        try {
            System.out.println("Stopping Tomcat Server");
            tomcat.stop();
        } catch (LifecycleException e) {
            System.err.println("Error stopping Repose Tomcat");
        }
    }
}
