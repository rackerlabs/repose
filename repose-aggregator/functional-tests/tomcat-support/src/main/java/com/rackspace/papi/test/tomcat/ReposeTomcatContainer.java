package com.rackspace.papi.test.tomcat;

import com.rackspace.papi.test.ContainerMonitorThread;
import com.rackspace.papi.test.ReposeContainer;
import com.rackspace.papi.test.ReposeContainerProps;
import com.rackspace.papi.test.mocks.util.MocksUtil;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.ServletException;

public class ReposeTomcatContainer extends ReposeContainer {

    private Tomcat tomcat;
    protected ContainerMonitorThread monitor;
    private static final String BASE_DIRECTORY = System.getProperty("java.io.tmpdir");


    public ReposeTomcatContainer(ReposeContainerProps props) throws ServletException {
        super(props);
        tomcat = new Tomcat();
        tomcat.setBaseDir(BASE_DIRECTORY);
        tomcat.setPort(Integer.parseInt(listenPort));
        tomcat.getHost().setAutoDeploy(true);
        tomcat.getHost().setDeployOnStartup(true);
        tomcat.addWebapp("/", warLocation).setCrossContext(true);

        if(props.getOriginServiceWars() != null && props.getOriginServiceWars().length != 0){

            for(String originService: props.getOriginServiceWars()){
                tomcat.addWebapp("/"+ MocksUtil.getServletPath(originService), originService);
            }
        }

        monitor = new ContainerMonitorThread(this, Integer.parseInt(stopPort));
    }

    @Override
    protected void startRepose() {
        try {
            monitor.start();
            tomcat.start();
            System.out.println("Tomcat Container Running");
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            System.err.println("Unable To Start Tomcat Server");
        }
    }

    @Override
    protected void stopRepose() {
        try {
            System.out.println("Stopping Tomcat Server");
            tomcat.stop();
            tomcat.getServer().stop();
        } catch (LifecycleException e) {
            System.err.println("Error stopping Repose Tomcat");
        }
    }
}
